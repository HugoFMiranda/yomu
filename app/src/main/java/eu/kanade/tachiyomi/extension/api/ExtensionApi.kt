package eu.kanade.tachiyomi.extension.api

import android.content.Context
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.protobuf.ProtoBuf
import okio.BufferedSource
import okio.buffer
import okio.gzip
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.coroutines.cancellation.CancellationException

internal class ExtensionApi {

    private val json: Json by injectLazy()
    private val networkService: NetworkHelper by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()
    private val protoBuf = ProtoBuf

    suspend fun findExtensions(): List<Extension.Available> {
        return withIOContext {
            val repos = preferences.extensionRepos().get()
            if (repos.isEmpty()) {
                return@withIOContext emptyList()
            }
            val extensions = repos.flatMap { getExtensions(it) }

            if (extensions.isEmpty()) {
                throw Exception()
            }

            extensions
        }
    }

    private suspend fun getExtensions(repoBaseUrl: String): List<Extension.Available> {
        return try {
            val indexV2Url = getIndexV2Url(repoBaseUrl)
            val extensions = if (indexV2Url != null) {
                getStoreExtensions(indexV2Url, repoBaseUrl)
            } else {
                getLegacyExtensions(repoBaseUrl)
            }
            extensions.filterSupportedLibVersion()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Timber.e(e, "Failed to get extensions from $repoBaseUrl")
            emptyList()
        }
    }

    /**
     * Reads the store descriptor at `<repo>/repo.json` and returns the v2 index url it points at,
     * or null for repos that only publish the legacy `index.min.json`.
     */
    private suspend fun getIndexV2Url(repoBaseUrl: String): String? {
        return try {
            networkService.client
                .newCall(GET("$repoBaseUrl/repo.json"))
                .awaitSuccess()
                .body.source()
                .use { json.decodeFromBufferedSource<NetworkLegacyExtensionRepo>(it) }
                .indexV2
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Timber.d("No v2 index for $repoBaseUrl (${e.message})")
            null
        }
    }

    private suspend fun getStoreExtensions(indexUrl: String, repoBaseUrl: String): List<Extension.Available> {
        val store = fetchStore(indexUrl)
        val extensionList = store.extensionList
            ?: store.extensionListUrl?.let { fetchExtensionList(it) }
            ?: throw Exception("Store index $indexUrl has no extension list")

        return extensionList.toAvailableExtensions(repoBaseUrl)
    }

    private suspend fun getLegacyExtensions(repoBaseUrl: String): List<Extension.Available> {
        val response = networkService.client
            .newCall(GET("$repoBaseUrl/index.min.json"))
            .awaitSuccess()

        return with(json) {
            response
                .parseAs<List<NetworkLegacyExtension>>()
                .map { it.toAvailableExtension(repoBaseUrl) }
        }
    }

    private suspend fun fetchStore(url: String): NetworkExtensionStore {
        val response = networkService.client.newCall(GET(url)).awaitSuccess()
        return response.body.source().decompressIfGzipped().use { source ->
            if (source.isJsonObject()) {
                json.decodeFromBufferedSource<NetworkExtensionStore>(source)
            } else {
                protoBuf.decodeFromByteArray<NetworkExtensionStore>(source.readByteArray())
            }
        }
    }

    private suspend fun fetchExtensionList(url: String): NetworkExtensionStore.ExtensionList {
        val response = networkService.client.newCall(GET(url)).awaitSuccess()
        return response.body.source().decompressIfGzipped().use { source ->
            if (source.isJsonObject()) {
                json.decodeFromBufferedSource<NetworkExtensionStore.ExtensionList>(source)
            } else {
                protoBuf.decodeFromByteArray<NetworkExtensionStore.ExtensionList>(source.readByteArray())
            }
        }
    }

    suspend fun checkForUpdates(context: Context, prefetchedExtensions: List<Extension.Available>? = null): List<Extension.Available> {
        return withIOContext {
            val extensions = prefetchedExtensions ?: findExtensions()

            val extensionManager: ExtensionManager = Injekt.get()
            val installedExtensions = extensionManager.installedExtensionsFlow.value.ifEmpty {
                ExtensionLoader.loadExtensionAsync(context)
                    .filterIsInstance<LoadResult.Success>()
                    .map { it.extension }
            }

            val extensionsWithUpdate = mutableListOf<Extension.Available>()
            for (installedExt in installedExtensions) {
                val pkgName = installedExt.pkgName
                val availableExt = extensions.find { it.pkgName == pkgName } ?: continue
                val hasUpdatedVer = availableExt.versionCode > installedExt.versionCode
                val hasUpdatedLib = availableExt.libVersion > installedExt.libVersion
                val hasUpdate = hasUpdatedVer || hasUpdatedLib
                if (hasUpdate) {
                    extensionsWithUpdate.add(availableExt)
                }
            }

            extensionsWithUpdate
        }
    }

    fun getApkUrl(extension: ExtensionManager.ExtensionInfo): String {
        return extension.apkUrl
    }

    private fun List<Extension.Available>.filterSupportedLibVersion(): List<Extension.Available> {
        return filter { it.libVersion >= ExtensionLoader.LIB_VERSION_MIN && it.libVersion <= ExtensionLoader.LIB_VERSION_MAX }
    }

    private fun BufferedSource.isJsonObject(): Boolean {
        return peek().readByte() == JSON_OBJECT_PREFIX
    }

    private fun BufferedSource.decompressIfGzipped(): BufferedSource {
        val isGzip = peek().use { peeked ->
            try {
                (peeked.readShort().toInt() and 0xFFFF) == GZIP_MAGIC
            } catch (_: Exception) {
                false
            }
        }

        return if (isGzip) gzip().buffer() else this
    }

    companion object {
        private const val JSON_OBJECT_PREFIX: Byte = 0x7B
        private const val GZIP_MAGIC = 0x1F8B
    }
}

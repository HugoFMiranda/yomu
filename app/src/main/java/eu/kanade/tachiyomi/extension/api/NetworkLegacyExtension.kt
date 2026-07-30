package eu.kanade.tachiyomi.extension.api

import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.serialization.Serializable

/**
 * Entry of the legacy `index.min.json` index. Apk and icon urls are built from the repo base url,
 * unlike the v2 index which ships absolute urls.
 */
@Serializable
data class NetworkLegacyExtension(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val sources: List<Extension.AvailableSource>?,
) {
    fun toAvailableExtension(repoUrl: String): Extension.Available {
        return Extension.Available(
            name = name.substringAfter("Tachiyomi: "),
            pkgName = pkg,
            versionName = version,
            versionCode = code,
            libVersion = version.substringBeforeLast('.').toDouble(),
            lang = lang,
            isNsfw = nsfw == 1,
            apkUrl = "$repoUrl/apk/$apk",
            iconUrl = "$repoUrl/icon/$pkg.png",
            sources = sources ?: emptyList(),
            repoUrl = repoUrl,
        )
    }
}

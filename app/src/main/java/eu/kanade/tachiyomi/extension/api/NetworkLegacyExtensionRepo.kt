package eu.kanade.tachiyomi.extension.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Store descriptor served at `<repo>/repo.json`. Repos that migrated to the v2 index point at it
 * through [indexV2]; older repos only carry [meta] and keep publishing `index.min.json`.
 */
@Serializable
data class NetworkLegacyExtensionRepo(
    @SerialName("index_v2")
    val indexV2: String? = null,
    val meta: Meta? = null,
) {
    @Serializable
    data class Meta(
        val name: String,
        val shortName: String? = null,
        val website: String = "",
        val signingKeyFingerprint: String = "",
    )
}

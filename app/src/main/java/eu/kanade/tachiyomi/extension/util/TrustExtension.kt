package eu.kanade.tachiyomi.extension.util

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrustExtension(
    private val preferences: PreferencesHelper = Injekt.get(),
) {

    fun isTrusted(pkgInfo: PackageInfo, signatureHash: String): Boolean {
        val key = "${pkgInfo.packageName}:${PackageInfoCompat.getLongVersionCode(pkgInfo)}:$signatureHash"
        return key in preferences.trustedExtensions().get()
    }

    /**
     * Returns true if [pkgName] was previously trusted under a *different* signature than
     * [signatureHash]. This is a stronger signal than a plain first-time "untrusted" prompt: the
     * extension's signing certificate changed since the user last approved it, which is what a
     * compromised repo re-signing/swapping a known extension would look like.
     */
    fun hasSignatureChanged(pkgName: String, signatureHash: String): Boolean {
        return preferences.trustedExtensions().get().any {
            val parts = it.split(":")
            parts.size == 3 && parts[0] == pkgName && parts[2] != signatureHash
        }
    }

    fun trust(pkgName: String, versionCode: Long, signatureHash: String) {
        preferences.trustedExtensions().let { exts ->
            // Remove previously trusted versions
            val removed = exts.get().filterNot { it.startsWith("$pkgName:") }.toMutableSet()

            removed += "$pkgName:$versionCode:$signatureHash"
            exts.set(removed)
        }
    }

    fun revokeAll() {
        preferences.trustedExtensions().delete()
    }
}

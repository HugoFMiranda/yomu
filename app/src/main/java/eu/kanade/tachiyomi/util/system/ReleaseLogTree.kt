package eu.kanade.tachiyomi.util.system

import android.util.Log
import timber.log.Timber

/**
 * Forwards warnings and errors to logcat in release builds, where no [Timber.DebugTree] is planted.
 * Without it every `Timber.e` is a no-op, so the crash log dump — which reads `logcat *:E` — comes
 * back empty even when a source failed. Anything below WARN is dropped to keep the log usable.
 */
class ReleaseLogTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Log.println(priority, tag ?: DEFAULT_TAG, message)
        if (t != null) {
            Log.println(priority, tag ?: DEFAULT_TAG, Log.getStackTraceString(t))
        }
    }

    private companion object {
        const val DEFAULT_TAG = "Yomu"
    }
}

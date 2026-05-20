package dev.karipap.app.updater

import dev.karipap.app.BuildConfig

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val tag: String,
    val apk: String,
    val changelog: String
) {
    val downloadUrl: String
        get() {
            if (apk.startsWith("https://") || apk.startsWith("http://")) return apk
            val base = BuildConfig.UPDATE_DOWNLOAD_BASE_URL.trimEnd('/')
            return if (base.isEmpty()) "" else "$base/$tag/$apk"
        }
}

package com.firstlimo.finance.data.model

import com.google.gson.annotations.SerializedName

/**
 * تمثيل نموذج الإصدار من GitHub Releases API أو ملف update.json
 */
data class GitHubRelease(
    @SerializedName("tag_name")
    val tagName: String = "",
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("body")
    val body: String = "",

    @SerializedName("releaseNotes")
    val releaseNotes: String? = null,
    
    @SerializedName("html_url")
    val htmlUrl: String = "",
    
    @SerializedName("assets")
    val assets: List<ReleaseAsset> = emptyList(),

    @SerializedName("versionName")
    val versionName: String? = null,

    @SerializedName("downloadUrl")
    val directDownloadUrl: String? = null
) {
    // استخراج رابط تنزيل ملف الـ APK المباشر من الأصول أو من update.json
    fun getApkDownloadUrl(): String? {
        if (!directDownloadUrl.isNullOrBlank()) return directDownloadUrl
        val apkAsset = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        return apkAsset?.browserDownloadUrl
    }

    // استخراج رقم الإصدار النظيف بدون حرف 'v' إن وجد
    fun getCleanVersion(): String {
        if (!versionName.isNullOrBlank()) return versionName.removePrefix("v").removePrefix("V").trim()
        return tagName.removePrefix("v").removePrefix("V").trim()
    }

    // الحصول على وصف التحديث
    fun getDisplayNotes(): String {
        return when {
            !releaseNotes.isNullOrBlank() -> releaseNotes
            body.isNotBlank() -> body
            else -> "تحديث جديد يتضمن تحسينات وإصلاحات عامة."
        }
    }
}

data class ReleaseAsset(
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("size")
    val size: Long = 0L,
    
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String = ""
)

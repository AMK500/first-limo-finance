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

    // استخراج رقم الإصدار النظيف
    fun getCleanVersion(): String {
        val raw = when {
            !versionName.isNullOrBlank() -> versionName
            tagName.isNotBlank() -> tagName
            name.isNotBlank() -> name
            else -> "1.3.0"
        }
        return raw.removePrefix("v").removePrefix("V").trim()
    }

    // رقم الإصدار للعرض
    fun getDisplayVersion(): String {
        val clean = getCleanVersion()
        return "v$clean"
    }

    // وصف التحديث وملاحظات الإصدار
    fun getDisplayNotes(): String {
        return when {
            !releaseNotes.isNullOrBlank() -> releaseNotes.trim()
            body.isNotBlank() -> body.trim()
            name.isNotBlank() -> name.trim()
            else -> "تحديث جديد يتضمن الأيقونة الرسمية وثيم دارك مود فاخر وحماية دائمة للبيانات."
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

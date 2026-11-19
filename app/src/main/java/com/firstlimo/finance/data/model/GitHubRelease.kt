package com.firstlimo.finance.data.model

import com.google.gson.annotations.SerializedName

/**
 * تمثيل نموذج الإصدار من GitHub Releases API
 */
data class GitHubRelease(
    @SerializedName("tag_name")
    val tagName: String = "",
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("body")
    val body: String = "",
    
    @SerializedName("html_url")
    val htmlUrl: String = "",
    
    @SerializedName("assets")
    val assets: List<ReleaseAsset> = emptyList()
) {
    // استخراج رابط تنزيل ملف الـ APK المباشر من الأصول
    fun getApkDownloadUrl(): String? {
        val apkAsset = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        return apkAsset?.browserDownloadUrl
    }

    // استخراج رقم الإصدار النظيف بدون حرف 'v' إن وجد
    fun getCleanVersion(): String {
        return tagName.removePrefix("v").removePrefix("V").trim()
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

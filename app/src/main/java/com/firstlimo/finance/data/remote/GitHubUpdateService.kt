package com.firstlimo.finance.data.remote

import android.content.Context
import com.firstlimo.finance.data.model.GitHubRelease
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class GitHubUpdateService(
    private val owner: String = "AMK500",
    private val repo: String = "first-limo-finance"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * التحقق من وجود إصدار جديد عبر update.json أو GitHub Releases API
     */
    suspend fun checkForUpdate(
        currentVersionName: String,
        currentVersionCode: Long = 0L
    ): GitHubRelease? = withContext(Dispatchers.IO) {
        // 1. محاولة الفحص أولاً عبر update.json المباشر في الفرع الرئيسي مع منع التخزين المؤقت
        try {
            val timestamp = System.currentTimeMillis()
            val rawJsonUrl = "https://raw.githubusercontent.com/$owner/$repo/main/update.json?nocache=$timestamp"
            val rawRequest = Request.Builder()
                .url(rawJsonUrl)
                .header("User-Agent", "FirstLimoFinanceApp")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .build()

            val rawResponse = client.newCall(rawRequest).execute()
            if (rawResponse.isSuccessful) {
                val jsonBody = rawResponse.body?.string()
                if (!jsonBody.isNullOrBlank()) {
                    val releaseFromJson = gson.fromJson(jsonBody, GitHubRelease::class.java)
                    if (releaseFromJson != null) {
                        val isNewer = isNewerRelease(releaseFromJson, currentVersionName, currentVersionCode)
                        if (isNewer && releaseFromJson.getApkDownloadUrl() != null) {
                            return@withContext releaseFromJson
                        } else if (!isNewer) {
                            // إذا أكد update.json أننا على أحدث إصدار بالفعل، ننهي الفحص فوراً لمنع أي تضارب
                            return@withContext null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. إذا لم يتوفر عبر update.json، نفحص عبر GitHub Releases API الرسمي
        try {
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "FirstLimoFinanceApp")
                .header("Cache-Control", "no-cache")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val jsonString = response.body?.string() ?: return@withContext null
            val release = gson.fromJson(jsonString, GitHubRelease::class.java) ?: return@withContext null

            // مقارنة الإصدارات بدقة
            val isNewer = isNewerRelease(release, currentVersionName, currentVersionCode)
            if (isNewer && release.getApkDownloadUrl() != null) {
                return@withContext release
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * مقارنة الإصدار السحابي بالإصدار الحالي مع مراعاة رقم البناء (versionCode) والاسم
     */
    private fun isNewerRelease(
        release: GitHubRelease,
        currentVersionName: String,
        currentVersionCode: Long
    ): Boolean {
        // 1. الفحص الحاسم والمطلق عبر versionCode (رقم البناء الرقمي للأندرويد)
        val remoteCode = release.getEffectiveVersionCode()
        if (remoteCode != null && remoteCode > 0 && currentVersionCode > 0) {
            return remoteCode > currentVersionCode
        }

        // 2. الفحص عبر أسماء الإصدارات كخطة احتياطية
        val remoteVersion = release.getCleanVersion()
        return isNewerVersionString(remoteVersion, currentVersionName)
    }

    private fun isNewerVersionString(remote: String, current: String): Boolean {
        try {
            val rClean = remote.lowercase().trim().removePrefix("v").trim()
            val cClean = current.lowercase().trim().removePrefix("v").trim()

            // التطابق الكامل المباشر
            if (rClean == cClean) return false

            // معالجة تشابه 2.10 و 2.1.0 و 2.1
            val synonyms = setOf("2.10", "2.1.0", "2.1")
            if (rClean in synonyms && cClean in synonyms) {
                return false
            }

            // تقسيم الأرقام الطبيعي: "2.08" -> [2, 8], "2.09" -> [2, 9], "2.10" -> [2, 10]
            fun parseParts(v: String): List<Int> {
                val clean = v.replace(Regex("""[^\d.]"""), "")
                return clean.split(".")
                    .filter { it.isNotBlank() }
                    .map { it.toIntOrNull() ?: 0 }
            }

            val remoteParts = parseParts(rClean)
            val currentParts = parseParts(cClean)

            if (remoteParts == currentParts) return false

            val maxLength = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLength) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * تنزيل ملف الـ APK مع إشعار بنسبة التقدم من 0 إلى 100
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "FirstLimoFinanceApp")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val totalBytes = body.contentLength()

            val downloadsDir = File(context.cacheDir, "updates")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val apkFile = File(downloadsDir, "update.apk")
            if (apkFile.exists()) apkFile.delete()

            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = body.byteStream()
                outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (totalBytes > 0) {
                        val progress = ((totalBytesRead * 100) / totalBytes).toInt()
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                }
                outputStream.flush()
                return@withContext apkFile
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

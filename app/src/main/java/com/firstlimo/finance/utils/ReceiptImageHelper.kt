package com.firstlimo.finance.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ReceiptImageHelper {

    /**
     * حفظ صورة الإيصال المختارة من المعرض أو الكاميرا في مجلد التطبيق الداخلي
     */
    fun saveReceiptImage(context: Context, sourceUri: Uri): String? {
        return try {
            val receiptsDir = File(context.filesDir, "receipts")
            if (!receiptsDir.exists()) receiptsDir.mkdirs()

            val fileName = "receipt_${UUID.randomUUID()}.jpg"
            val targetFile = File(receiptsDir, fileName)

            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val outputStream = FileOutputStream(targetFile)
                // ضغط الصورة إلى JPEG بجودة 85% لتقليل المساحة وسرعة العرض
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                outputStream.flush()
                outputStream.close()
                targetFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

package com.firstlimo.finance.data.model

import java.util.UUID

/**
 * تمثيل العملية المالية المنفصلة مع دعم إرفاق صورة الإيصال أو الفاتورة
 */
data class FinancialTransaction(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val type: TransactionType,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = "",
    val timeString: String = "",
    val notes: String = "",
    val receiptImagePath: String? = null, // مسار صورة الإيصال / الفاتورة
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val isSynced: Boolean = false
) {
    // التحويل إلى Map لـ Firestore
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "type" to type.name,
            "amount" to amount,
            "timestamp" to timestamp,
            "dateString" to dateString,
            "timeString" to timeString,
            "notes" to notes,
            "receiptImagePath" to receiptImagePath,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "isDeleted" to isDeleted
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): FinancialTransaction {
            return FinancialTransaction(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                userId = map["userId"] as? String ?: "",
                type = TransactionType.fromString(map["type"] as? String ?: "COLLECTION"),
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                dateString = map["dateString"] as? String ?: "",
                timeString = map["timeString"] as? String ?: "",
                notes = map["notes"] as? String ?: "",
                receiptImagePath = map["receiptImagePath"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isDeleted = map["isDeleted"] as? Boolean ?: false,
                isSynced = true
            )
        }
    }
}

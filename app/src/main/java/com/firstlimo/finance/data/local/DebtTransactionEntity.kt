package com.firstlimo.finance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class DebtType(val titleArabic: String) {
    TOOK("أخذت"), // قبضت / استلمت (أخضر)
    GAVE("أعطيت")  // دفعت / سلفة / مصروف (أحمر)
}

@Entity(tableName = "debt_transactions")
data class DebtTransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val customerId: String,
    val type: String, // "TOOK" or "GAVE"
    val amount: Double,
    val dateString: String,
    val timeString: String,
    val notes: String = "",
    val receiptImagePath: String? = null,
    val linkedFinancialTransactionId: String? = null, // للربط التلقائي بجدول المالية
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

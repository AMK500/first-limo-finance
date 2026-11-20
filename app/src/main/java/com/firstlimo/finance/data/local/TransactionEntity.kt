package com.firstlimo.finance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val type: String,
    val amount: Double,
    val timestamp: Long,
    val dateString: String,
    val timeString: String,
    val notes: String,
    val receiptImagePath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val isSynced: Boolean
) {
    fun toDomain(): FinancialTransaction {
        return FinancialTransaction(
            id = id,
            userId = userId,
            type = TransactionType.fromString(type),
            amount = amount,
            timestamp = timestamp,
            dateString = dateString,
            timeString = timeString,
            notes = notes,
            receiptImagePath = receiptImagePath,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = isDeleted,
            isSynced = isSynced
        )
    }

    companion object {
        fun fromDomain(domain: FinancialTransaction): TransactionEntity {
            return TransactionEntity(
                id = domain.id,
                userId = domain.userId,
                type = domain.type.name,
                amount = domain.amount,
                timestamp = domain.timestamp,
                dateString = domain.dateString,
                timeString = domain.timeString,
                notes = domain.notes,
                receiptImagePath = domain.receiptImagePath,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
                isDeleted = domain.isDeleted,
                isSynced = domain.isSynced
            )
        }
    }
}

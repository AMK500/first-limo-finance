package com.firstlimo.finance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String, // الحقل الوحيد الإجباري
    val phone: String? = null, // اختياري
    val address: String? = null, // اختياري
    val notes: String? = null, // اختياري
    val isSystem: Boolean = false, // true لعميل فرست ليموزين الأساسي
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

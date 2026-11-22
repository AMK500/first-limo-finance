package com.firstlimo.finance.data.model

import com.firstlimo.finance.data.local.CustomerEntity

data class CustomerWithBalance(
    val customer: CustomerEntity,
    val totalTook: Double, // إجمالي أخذت (لي)
    val totalGave: Double, // إجمالي أعطيت (عليّ)
    val netBalance: Double, // الفارق: أخذت - أعطيت
    val lastTransactionDate: String?,
    val transactionsCount: Int
)

data class DebtsSummary(
    val totalTookAll: Double = 0.0, // إجمالي أخذت لكل العملاء
    val totalGaveAll: Double = 0.0, // إجمالي أعطيت لكل العملاء
    val netBalanceAll: Double = 0.0,
    val customersCount: Int = 0
)

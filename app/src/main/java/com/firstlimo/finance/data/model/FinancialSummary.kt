package com.firstlimo.finance.data.model

/**
 * الملخص المالي الإجمالي لكافة الحسابات
 */
data class FinancialSummary(
    val totalCollection: Double = 0.0,      // إجمالي التحصيل
    val totalExpense: Double = 0.0,         // إجمالي المصاريف
    val totalCommission: Double = 0.0,      // إجمالي الكوميسيون
    val netLimoBalance: Double = 0.0,       // صافي حساب فرست ليموزين
    val totalPersonalCash: Double = 0.0,    // إجمالي النقدية الشخصية
    val totalOverallLiquidity: Double = 0.0 // إجمالي الأموال في الحسابين معاً
) {
    companion object {
        /**
         * حساب الملخص المالي بناءً على القواعد والمعادلات المطلوبة:
         * 1. رصيد فرست ليموزين = التحصيل - المصاريف - الكوميسيون
         * 2. رصيد النقدية الشخصية = الكوميسيون + إيداعات شخصية - مصاريف شخصية
         * 3. إجمالي الحسابين = رصيد فرست ليموزين + النقدية الشخصية
         */
        fun calculate(transactions: List<FinancialTransaction>): FinancialSummary {
            val activeTransactions = transactions.filter { !it.isDeleted }

            var collections = 0.0
            var expenses = 0.0
            var commissions = 0.0
            var personalDeposits = 0.0
            var personalExpenses = 0.0

            for (tx in activeTransactions) {
                when (tx.type) {
                    TransactionType.COLLECTION -> collections += tx.amount
                    TransactionType.EXPENSE -> expenses += tx.amount
                    TransactionType.COMMISSION -> commissions += tx.amount
                    TransactionType.PERSONAL_DEPOSIT -> personalDeposits += tx.amount
                    TransactionType.PERSONAL_EXPENSE -> personalExpenses += tx.amount
                }
            }

            // صافي فرست ليموزين: التحصيل - المصاريف - الكوميسيون
            val netLimo = collections - expenses - commissions

            // إجمالي النقدية: الكوميسيون (المضاف تلقائياً) + الإيداعات الشخصية - المصاريف الشخصية
            val personalCash = commissions + personalDeposits - personalExpenses

            // إجمالي السيولة في الحسابين معاً
            val totalLiquidity = netLimo + personalCash

            return FinancialSummary(
                totalCollection = collections,
                totalExpense = expenses,
                totalCommission = commissions,
                netLimoBalance = netLimo,
                totalPersonalCash = personalCash,
                totalOverallLiquidity = totalLiquidity
            )
        }
    }
}

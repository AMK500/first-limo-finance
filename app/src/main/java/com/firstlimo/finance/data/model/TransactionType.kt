package com.firstlimo.finance.data.model

/**
 * أنواع العمليات المالية في التطبيق
 */
enum class TransactionType(
    val titleArabic: String,
    val description: String,
    val isPositiveForLimo: Boolean?,
    val isPositiveForPersonal: Boolean?
) {
    // 1. تحصيل فرست ليموزين: يضاف إلى حساب فرست ليموزين
    COLLECTION(
        titleArabic = "تحصيل فرست ليموزين",
        description = "مبلغ محصل يضاف لحساب الشركة",
        isPositiveForLimo = true,
        isPositiveForPersonal = null
    ),

    // 2. مصاريف فرست ليموزين: تخصم من رصيد فرست ليموزين
    EXPENSE(
        titleArabic = "مصروف فرست ليموزين",
        description = "مصاريف تخص الشركة وتخصم من حسابها",
        isPositiveForLimo = false,
        isPositiveForPersonal = null
    ),

    // 3. الكوميسيون (راتب السائق): يخصم من فرست ليموزين ويضاف تلقائياً للنقدية الشخصية
    COMMISSION(
        titleArabic = "كوميسيون (دخل شخصي)",
        description = "راتب السائق: يخصم من الشركة ويضاف تلقائياً للنقدية الشخصية",
        isPositiveForLimo = false,
        isPositiveForPersonal = true
    ),

    // 4. إيداع نقدي شخصي: يضاف إلى النقدية الشخصية فقط
    PERSONAL_DEPOSIT(
        titleArabic = "إيداع نقدي شخصي",
        description = "أموال شخصية مضافة للنقدية",
        isPositiveForLimo = null,
        isPositiveForPersonal = true
    ),

    // 5. مصروف نقدي شخصي: يخصم من النقدية الشخصية فقط
    PERSONAL_EXPENSE(
        titleArabic = "مصروف نقدي شخصي",
        description = "مصروفات شخصية تخصم من النقدية",
        isPositiveForLimo = null,
        isPositiveForPersonal = false
    );

    companion object {
        fun fromString(type: String): TransactionType {
            return try {
                valueOf(type)
            } catch (e: Exception) {
                COLLECTION
            }
        }
    }
}

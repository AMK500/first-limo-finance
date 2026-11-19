package com.firstlimo.finance.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)

    fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }

    fun getCurrentTime(): String {
        return timeFormat.format(Date())
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }
}

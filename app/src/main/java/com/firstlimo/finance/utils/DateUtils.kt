package com.firstlimo.finance.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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

    fun isToday(dateString: String): Boolean {
        return dateString == getCurrentDate()
    }

    fun isWithinDays(dateString: String, days: Int): Boolean {
        return try {
            val date = dateFormat.parse(dateString) ?: return false
            val now = Date()
            val diffMs = now.time - date.time
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
            diffDays in 0..days
        } catch (_: Exception) {
            false
        }
    }

    /**
     * هل التاريخ في الماضي (مستحق السداد وتجاوز موعده)
     */
    fun isPastDate(dateString: String): Boolean {
        return try {
            val date = dateFormat.parse(dateString) ?: return false
            val today = dateFormat.parse(getCurrentDate()) ?: return false
            date.before(today)
        } catch (_: Exception) {
            false
        }
    }
}

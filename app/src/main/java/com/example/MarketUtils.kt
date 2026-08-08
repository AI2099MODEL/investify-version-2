package com.example

import java.util.Calendar
import java.util.TimeZone

object MarketUtils {
    /**
     * Checks whether the Indian stock market (NSE/BSE) is currently open for live trading.
     * Market hours: Monday to Friday, 09:15 AM IST to 03:30 PM IST (555 to 930 minutes).
     */
    fun isMarketOpen(): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }
        if (isMarketHoliday(cal)) {
            return false
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute
        return timeInMinutes in 555..930
    }

    fun isMarketHoliday(cal: Calendar): Boolean {
        val month = cal.get(Calendar.MONTH) // 0-indexed (0 is Jan, 11 is Dec)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        
        // Fixed NSE Holidays
        if (month == Calendar.JANUARY && day == 26) return true // Republic Day
        if (month == Calendar.MAY && day == 1) return true // Maharashtra Day
        if (month == Calendar.AUGUST && day == 15) return true // Independence Day
        if (month == Calendar.OCTOBER && day == 2) return true // Gandhi Jayanti
        if (month == Calendar.DECEMBER && day == 25) return true // Christmas
        
        // Common Indian Market Holidays for 2026
        if (month == Calendar.MARCH && day == 3) return true // Holi
        if (month == Calendar.MARCH && day == 20) return true // Ramzan Id
        if (month == Calendar.APRIL && day == 3) return true // Good Friday
        if (month == Calendar.APRIL && day == 14) return true // Ambedkar Jayanti
        if (month == Calendar.MAY && day == 27) return true // Bakri Id
        if (month == Calendar.JUNE && day == 26) return true // Muharram
        if (month == Calendar.SEPTEMBER && day == 14) return true // Ganesh Chaturthi
        if (month == Calendar.OCTOBER && day == 20) return true // Dussehra
        if (month == Calendar.NOVEMBER && day == 8) return true // Diwali
        if (month == Calendar.NOVEMBER && day == 24) return true // Gurunanak Jayanti

        return false
    }

    fun getMarketStatusText(): String {
        return if (isMarketOpen()) "LIVE MARKET" else "MARKET CLOSED"
    }
}

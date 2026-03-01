package com.laileme.app.ui.components

import java.util.Calendar

object LunarCalendar {

    private val LUNAR_INFO = longArrayOf(
        0x04bd8,0x04ae0,0x0a570,0x054d5,0x0d260,0x0d950,0x16554,0x056a0,0x09ad0,0x055d2,
        0x04ae0,0x0a5b6,0x0a4d0,0x0d250,0x1d255,0x0b540,0x0d6a0,0x0ada2,0x095b0,0x14977,
        0x04970,0x0a4b0,0x0b4b5,0x06a50,0x06d40,0x1ab54,0x02b60,0x09570,0x052f2,0x04970,
        0x06566,0x0d4a0,0x0ea50,0x06e95,0x05ad0,0x02b60,0x186e3,0x092e0,0x1c8d7,0x0c950,
        0x0d4a0,0x1d8a6,0x0b550,0x056a0,0x1a5b4,0x025d0,0x092d0,0x0d2b2,0x0a950,0x0b557,
        0x06ca0,0x0b550,0x15355,0x04da0,0x0a5b0,0x14573,0x052b0,0x0a9a8,0x0e950,0x06aa0,
        0x0aea6,0x0ab50,0x04b60,0x0aae4,0x0a570,0x05260,0x0f263,0x0d950,0x05b57,0x056a0,
        0x096d0,0x04dd5,0x04ad0,0x0a4d0,0x0d4d4,0x0d250,0x0d558,0x0b540,0x0b6a0,0x195a6,
        0x095b0,0x049b0,0x0a974,0x0a4b0,0x0b27a,0x06a50,0x06d40,0x0af46,0x0ab60,0x09570,
        0x04af5,0x04970,0x064b0,0x074a3,0x0ea50,0x06b58,0x05ac0,0x0ab60,0x096d5,0x092e0,
        0x0c960,0x0d954,0x0d4a0,0x0da50,0x07552,0x056a0,0x0abb7,0x025d0,0x092d0,0x0cab5,
        0x0a950,0x0b4a0,0x0baa4,0x0ad50,0x055d9,0x04ba0,0x0a5b0,0x15176,0x052b0,0x0a930,
        0x07954,0x06aa0,0x0ad50,0x05b52,0x04b60,0x0a6e6,0x0a4e0,0x0d260,0x0ea65,0x0d530,
        0x05aa0,0x076a3,0x096d0,0x04afb,0x04ad0,0x0a4d0,0x1d0b6,0x0d250,0x0d520,0x0dd45,
        0x0b5a0,0x056d0,0x055b2,0x049b0,0x0a577,0x0a4b0,0x0aa50,0x1b255,0x06d20,0x0ada0,
        0x14b63,0x09370,0x049f8,0x04970,0x064b0,0x168a6,0x0ea50,0x06b20,0x1a6c4,0x0aae0,
        0x092e0,0x0d2e3,0x0c960,0x0d557,0x0d4a0,0x0da50,0x05d55,0x056a0,0x0a6d0,0x055d4,
        0x052d0,0x0a9b8,0x0a950,0x0b4a0,0x0b6a6,0x0ad50,0x055a0,0x0aba4,0x0a5b0,0x052b0,
        0x0b273,0x06930,0x07337,0x06aa0,0x0ad50,0x14b55,0x04b60,0x0a570,0x054e4,0x0d160,
        0x0e968,0x0d520,0x0daa0,0x16aa6,0x056d0,0x04ae0,0x0a9d4,0x0a4d0,0x0d150,0x0f252,
        0x0d520
    )

    private val LUNAR_MONTH = arrayOf("正","二","三","四","五","六","七","八","九","十","冬","腊")
    private val LUNAR_DAY = arrayOf(
        "初一","初二","初三","初四","初五","初六","初七","初八","初九","初十",
        "十一","十二","十三","十四","十五","十六","十七","十八","十九","二十",
        "廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十"
    )

    private val SOLAR_FESTIVALS = mapOf(
        "01-01" to "元旦", "02-14" to "情人节", "03-08" to "妇女节",
        "03-12" to "植树节", "04-01" to "愚人节", "05-01" to "劳动节",
        "05-04" to "青年节", "06-01" to "儿童节", "07-01" to "建党节",
        "08-01" to "建军节", "09-10" to "教师节", "10-01" to "国庆节",
        "10-31" to "万圣节", "11-11" to "光棍节", "12-24" to "平安夜",
        "12-25" to "圣诞节"
    )

    private val LUNAR_FESTIVALS = mapOf(
        "01-01" to "春节", "01-15" to "元宵", "02-02" to "龙抬头",
        "05-05" to "端午", "07-07" to "七夕", "07-15" to "中元",
        "08-15" to "中秋", "09-09" to "重阳", "12-08" to "腊八",
        "12-30" to "除夕", "12-29" to "除夕"
    )

    data class LunarDate(val year: Int, val month: Int, val day: Int, val isLeap: Boolean)
    data class LunarInfo(val lunarText: String, val isHoliday: Boolean)

    private fun lunarYearDays(y: Int): Int {
        val idx = y - 1900
        if (idx < 0 || idx >= LUNAR_INFO.size) return 348
        var sum = 348
        val info = LUNAR_INFO[idx]
        var bit = 0x8000L
        while (bit >= 0x10L) {
            if (info and bit != 0L) sum += 1
            bit = bit shr 1
        }
        return sum + leapMonthDays(y)
    }

    private fun leapMonth(y: Int): Int {
        val idx = y - 1900
        if (idx < 0 || idx >= LUNAR_INFO.size) return 0
        return (LUNAR_INFO[idx] and 0xfL).toInt()
    }

    private fun leapMonthDays(y: Int): Int {
        if (leapMonth(y) == 0) return 0
        val idx = y - 1900
        if (idx < 0 || idx >= LUNAR_INFO.size) return 0
        return if (LUNAR_INFO[idx] and 0x10000L != 0L) 30 else 29
    }

    private fun lunarMonthDays(y: Int, m: Int): Int {
        val idx = y - 1900
        if (idx < 0 || idx >= LUNAR_INFO.size) return 29
        return if (LUNAR_INFO[idx] and (0x10000L shr m) != 0L) 30 else 29
    }

    fun solarToLunar(year: Int, month: Int, day: Int): LunarDate {
        val baseDate = Calendar.getInstance().apply {
            set(1900, 0, 31, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        val target = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        var offset = ((target.timeInMillis - baseDate.timeInMillis) / 86400000L).toInt()
        if (offset < 0) return LunarDate(1900, 1, 1, false)

        var lunarYear = 1900
        while (lunarYear < 2101) {
            val d = lunarYearDays(lunarYear)
            if (offset < d) break
            offset -= d
            lunarYear++
        }

        val lm = leapMonth(lunarYear)
        var isLeap = false
        var lunarMonth = 1

        for (m in 1..12) {
            if (lm > 0 && m == lm + 1 && !isLeap) {
                isLeap = true
                val d = leapMonthDays(lunarYear)
                if (offset < d) { lunarMonth = m - 1; break }
                offset -= d
                isLeap = false
            }
            val d = lunarMonthDays(lunarYear, m)
            if (offset < d) { lunarMonth = m; break }
            offset -= d
        }

        return LunarDate(lunarYear, lunarMonth, offset + 1, isLeap)
    }

    fun getLunarInfo(year: Int, month: Int, day: Int): LunarInfo {
        val solarKey = String.format("%02d-%02d", month, day)
        SOLAR_FESTIVALS[solarKey]?.let { return LunarInfo(it, true) }

        val lunar = solarToLunar(year, month, day)
        val lunarKey = String.format("%02d-%02d", lunar.month, lunar.day)
        LUNAR_FESTIVALS[lunarKey]?.let { return LunarInfo(it, true) }

        // 母亲节(5月第2个周日)
        if (month == 5) {
            val cal = Calendar.getInstance().apply { set(year, 4, 1) }
            var cnt = 0
            for (d in 1..31) {
                cal.set(Calendar.DAY_OF_MONTH, d)
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    cnt++; if (cnt == 2 && d == day) return LunarInfo("母亲节", true)
                }
            }
        }
        // 父亲节(6月第3个周日)
        if (month == 6) {
            val cal = Calendar.getInstance().apply { set(year, 5, 1) }
            var cnt = 0
            for (d in 1..30) {
                cal.set(Calendar.DAY_OF_MONTH, d)
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    cnt++; if (cnt == 3 && d == day) return LunarInfo("父亲节", true)
                }
            }
        }
        // 感恩节(11月第4个周四)
        if (month == 11) {
            val cal = Calendar.getInstance().apply { set(year, 10, 1) }
            var cnt = 0
            for (d in 1..30) {
                cal.set(Calendar.DAY_OF_MONTH, d)
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY) {
                    cnt++; if (cnt == 4 && d == day) return LunarInfo("感恩节", true)
                }
            }
        }

        return if (lunar.day == 1) {
            val prefix = if (lunar.isLeap) "闰" else ""
            LunarInfo("${prefix}${LUNAR_MONTH[lunar.month - 1]}月", false)
        } else {
            LunarInfo(LUNAR_DAY[lunar.day - 1], false)
        }
    }
}

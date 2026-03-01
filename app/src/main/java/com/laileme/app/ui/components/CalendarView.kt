package com.laileme.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.data.entity.PeriodRecord
import com.laileme.app.ui.normalizeDate
import com.laileme.app.ui.daysBetween
import com.laileme.app.ui.theme.*
import java.util.*

// ──────────────── 数据类 ────────────────

data class DayInfo(
    val day: Int?,
    val date: Long?,
    val isPeriod: Boolean = false,
    val isPredictPeriod: Boolean = false,
    val isOvulation: Boolean = false,
    val isFertile: Boolean = false,
    val isRecordOvulation: Boolean = false,
    val isPredictOvulation: Boolean = false,
    val isToday: Boolean = false,
    val isWeekend: Boolean = false,
    val lunarText: String = "",
    val isHoliday: Boolean = false,
    val hasIntimacy: Boolean = false
)

private data class PeriodStatus(
    val isPeriod: Boolean = false,
    val isPredictPeriod: Boolean = false,
    val isOvulation: Boolean = false,
    val isFertile: Boolean = false
)

// ──────────────── 主组件 ────────────────

@Composable
fun CalendarView(
    currentMonth: Calendar,
    records: List<PeriodRecord>,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onMonthChange: (Int) -> Unit,
    intimacyDates: Set<Long> = emptySet()
) {
    val monthNames = arrayOf(
        "1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月"
    )
    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(modifier = Modifier.fillMaxWidth()) {
        // 月份标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f))
                    .clickable { onMonthChange(-1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "上个月",
                    modifier = Modifier.size(18.dp),
                    tint = PrimaryPink
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${currentMonth.get(Calendar.YEAR)}年",
                    fontSize = 11.sp, color = TextSecondary
                )
                Text(
                    monthNames[currentMonth.get(Calendar.MONTH)],
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f))
                    .clickable { onMonthChange(1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "下个月",
                    modifier = Modifier.size(18.dp),
                    tint = PrimaryPink
                )
            }
        }

        // 星期标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            weekDays.forEachIndexed { index, day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (index >= 5) WeekendText else TextSecondary
                )
            }
        }

        // 日期网格（固定6行，保证每月卡片高度一致）
        val days = buildDaysInMonth(currentMonth, records, intimacyDates)
        val paddedDays = days + List((42 - days.size).coerceAtLeast(0)) { DayInfo(null, null) }
        paddedDays.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                week.forEach { dayInfo ->
                    DayCell(
                        dayInfo = dayInfo,
                        isSelected = dayInfo.date?.let { isSameDay(it, selectedDate) } ?: false,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ──────────────── 日期格子 ────────────────

@Composable
private fun DayCell(
    dayInfo: DayInfo,
    isSelected: Boolean,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (dayInfo.day != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            dayInfo.isToday -> TodayGreen
                            dayInfo.isPeriod -> PeriodRed.copy(alpha = 0.2f)
                            dayInfo.isPredictPeriod -> PredictPeriod.copy(alpha = 0.3f)
                            dayInfo.isOvulation -> OvulationOrange.copy(alpha = 0.3f)
                            dayInfo.isFertile -> FertileGreen.copy(alpha = 0.25f)
                            isSelected -> PrimaryPink.copy(alpha = 0.1f)
                            else -> Color.Transparent
                        }
                    )
                    .then(
                        if (isSelected && !dayInfo.isToday)
                            Modifier.border(1.dp, PrimaryPink, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .clickable { dayInfo.date?.let { onDateSelected(it) } },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 顶部标记（数字上方）
                    if (dayInfo.hasIntimacy && !dayInfo.isToday) {
                        // 爱心标记：默认大红色，经期重叠时靛蓝爱心+红点
                        if (dayInfo.isPeriod) {
                            Box(
                                modifier = Modifier.size(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = Color(0xFF5C6BC0)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(PeriodRed)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = Color(0xFFE53935)
                            )
                        }
                    } else when {
                        dayInfo.isPeriod && !dayInfo.isToday -> Box(
                            modifier = Modifier.size(4.dp).clip(CircleShape).background(PeriodRed)
                        )
                        dayInfo.isFertile && !dayInfo.isToday -> Box(
                            modifier = Modifier.size(4.dp).clip(CircleShape).background(FertileGreen)
                        )
                        dayInfo.isOvulation && !dayInfo.isToday -> Box(
                            modifier = Modifier.size(4.dp).clip(CircleShape).background(OvulationOrange)
                        )
                        else -> Spacer(modifier = Modifier.height(4.dp))
                    }
                    // 日期数字
                    Text(
                        text = if (dayInfo.isToday) "今" else dayInfo.day.toString(),
                        fontSize = if (dayInfo.lunarText.isNotEmpty()) 10.sp else 12.sp,
                        fontWeight = if (dayInfo.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            dayInfo.isToday -> Color.White
                            dayInfo.isWeekend -> WeekendText
                            else -> TextPrimary
                        },
                        lineHeight = 11.sp
                    )
                    // 农历/节日小字（框内数字下方）
                    if (dayInfo.lunarText.isNotEmpty()) {
                        Text(
                            text = dayInfo.lunarText,
                            fontSize = 6.sp,
                            color = when {
                                dayInfo.isToday -> Color.White.copy(alpha = 0.85f)
                                dayInfo.isHoliday -> PeriodRed
                                else -> TextSecondary
                            },
                            fontWeight = if (dayInfo.isHoliday) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            lineHeight = 7.sp
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.size(32.dp))
        }
    }
}

// ──────────────── 构建月份日期 ────────────────

private fun buildDaysInMonth(calendar: Calendar, records: List<PeriodRecord>, intimacyDates: Set<Long> = emptySet()): List<DayInfo> {
    val result = mutableListOf<DayInfo>()
    val cal = calendar.clone() as Calendar

    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    // 周一=0 ... 周日=6
    var firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - 2
    if (firstDayOffset < 0) firstDayOffset += 7

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val todayMs = normalizeDate(System.currentTimeMillis())

    // 填充月初空白
    repeat(firstDayOffset) { result.add(DayInfo(null, null)) }

    val solarYear = cal.get(Calendar.YEAR)
    val solarMonth = cal.get(Calendar.MONTH) + 1

    for (day in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        val dateMs = normalizeDate(cal.timeInMillis)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
        val isToday = dateMs == todayMs
        val status = calcPeriodStatus(dateMs, records)
        val lunarInfo = LunarCalendar.getLunarInfo(solarYear, solarMonth, day)

        result.add(
            DayInfo(
                day = day,
                date = dateMs,
                isPeriod = status.isPeriod,
                isPredictPeriod = status.isPredictPeriod,
                isOvulation = status.isOvulation,
                isFertile = status.isFertile,
                isToday = isToday,
                isWeekend = isWeekend,
                lunarText = lunarInfo.lunarText,
                isHoliday = lunarInfo.isHoliday,
                hasIntimacy = dateMs in intimacyDates
            )
        )
    }
    return result
}

// ──────────────── 经期状态判定 ────────────────

private fun calcPeriodStatus(dateMs: Long, records: List<PeriodRecord>): PeriodStatus {
    if (records.isEmpty()) return PeriodStatus()

    // 1) 检查是否落在任何一条"已记录"的经期范围内
    for (record in records) {
        val startMs = normalizeDate(record.startDate)
        val endMs = if (record.endDate != null) {
            normalizeDate(record.endDate)
        } else {
            // 没有 endDate（活跃经期）→ 延续到今天或预计结束日的较大值
            // 用户未手动结束前，一直标记为经期
            val todayMs2 = normalizeDate(System.currentTimeMillis())
            maxOf(startMs + (record.periodLength - 1) * 24L * 60 * 60 * 1000, todayMs2)
        }
        if (dateMs in startMs..endMs) {
            return PeriodStatus(isPeriod = true)
        }
    }

    // 2) 检查是否有已完成的记录（有结束日期的）
    val hasCompletedRecord = records.any { it.endDate != null }

    // 如果没有任何已完成的记录，说明是第一次使用，不做任何预测
    // 避免在没有真实数据时显示易孕期/排卵期造成混乱
    if (!hasCompletedRecord) {
        return PeriodStatus()
    }

    // 3) 用最新已完成记录做未来预测（而非可能未完成的活跃记录）
    val latestCompleted = records.firstOrNull { it.endDate != null } ?: return PeriodStatus()
    val startMs = normalizeDate(latestCompleted.startDate)
    val diff = daysBetween(startMs, dateMs)
    if (diff < 0) return PeriodStatus()  // 日期在记录之前

    val cycleLen = latestCompleted.cycleLength
    val periodLen = latestCompleted.periodLength

    // 周期长度异常时不预测
    if (cycleLen <= 0 || periodLen <= 0) return PeriodStatus()

    val dayInCycle = diff % cycleLen
    // 排卵日 = 周期长度 - 14（更符合医学）
    val ovulationDay = (cycleLen - 14).coerceAtLeast(periodLen + 1)
    // 易孕期 = 排卵日前5天 ~ 排卵日后1天
    val fertileStart = (ovulationDay - 5).coerceAtLeast(periodLen)
    val fertileEnd = (ovulationDay + 1).coerceAtMost(cycleLen - 1)

    // 第一个周期内的经期已由上面精确匹配处理，跳过经期判断
    if (diff < cycleLen) {
        return when {
            dayInCycle == ovulationDay -> PeriodStatus(isOvulation = true)
            dayInCycle in fertileStart..fertileEnd -> PeriodStatus(isFertile = true)
            else -> PeriodStatus()
        }
    }

    // 后续周期的预测
    return when {
        dayInCycle < periodLen -> PeriodStatus(isPredictPeriod = true)
        dayInCycle == ovulationDay -> PeriodStatus(isOvulation = true)
        dayInCycle in fertileStart..fertileEnd -> PeriodStatus(isFertile = true)
        else -> PeriodStatus()
    }
}

// ──────────────── 工具函数 ────────────────

private fun isSameDay(a: Long, b: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = a }
    val c2 = Calendar.getInstance().apply { timeInMillis = b }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
            && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

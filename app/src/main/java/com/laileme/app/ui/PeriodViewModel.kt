package com.laileme.app.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.laileme.app.data.AppDatabase
import com.laileme.app.data.AuthManager
import com.laileme.app.data.PartnerManager
import com.laileme.app.data.SyncManager
import com.laileme.app.data.entity.DiaryEntry
import com.laileme.app.data.entity.PeriodRecord
import com.laileme.app.data.entity.SleepRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class PeriodUiState(
    val records: List<PeriodRecord> = emptyList(),
    val latestRecord: PeriodRecord? = null,
    val selectedDate: Long = normalizeDate(System.currentTimeMillis()),
    val currentMonthYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val currentMonthMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val isInPeriod: Boolean = false,
    val hasActivePeriod: Boolean = false,
    val daysUntilPeriodEnd: Int = 0,
    val daysUntilNextPeriod: Int = 0,
    val currentPhase: String = "请记录经期",
    val cycleDay: Int = 0,
    val cycleProgress: Float = 0f,
    val periodProgress: Float = 0f,
    val isFirstRecord: Boolean = false,  // 是否是首次记录（无已完成记录）
    val hasSetup: Boolean = false,
    val trackingMode: String = "auto", // "auto" = 自动推算, "manual" = 手动输入
    val savedCycleLength: Int = 28,
    val savedPeriodLength: Int = 5
) {
    val currentMonth: Calendar
        get() = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentMonthYear)
            set(Calendar.MONTH, currentMonthMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
}

/** 将任意时间戳归一化到当天 00:00:00.000 */
fun normalizeDate(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

/** 计算两个归一化日期之间的天数差 */
fun daysBetween(startMs: Long, endMs: Long): Int {
    return ((endMs - startMs) / (24L * 60 * 60 * 1000)).toInt()
}

class PeriodViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.periodDao()
    private val diaryDao = db.diaryDao()
    private val prefs = application.getSharedPreferences("laileme_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(PeriodUiState())
    val uiState: StateFlow<PeriodUiState> = _uiState.asStateFlow()

    private val _currentDiary = MutableStateFlow<DiaryEntry?>(null)
    val currentDiary: StateFlow<DiaryEntry?> = _currentDiary.asStateFlow()

    private var partnerUpdateReceiver: BroadcastReceiver? = null

    /** 获取当前token */
    private fun getToken(): String {
        return try {
            val field = SyncManager::class.java.getDeclaredField("token")
            field.isAccessible = true
            field.get(null) as? String ?: ""
        } catch (e: Exception) { "" }
    }

    init {
        // 从 SharedPreferences 读取设置
        val hasSetup = prefs.getBoolean("has_setup", false)
        val trackingMode = prefs.getString("tracking_mode", "auto") ?: "auto"
        val savedCycle = prefs.getInt("cycle_length", 28)
        val savedPeriod = prefs.getInt("period_length", 5)
        _uiState.update {
            it.copy(
                hasSetup = hasSetup,
                trackingMode = trackingMode,
                savedCycleLength = savedCycle,
                savedPeriodLength = savedPeriod
            )
        }

        viewModelScope.launch {
            dao.getAllRecords().collect { records ->
                val latest = records.firstOrNull()
                recalculate(records, latest)
            }
        }
        // 监听选中日期变化，加载对应日记
        viewModelScope.launch {
            _uiState.map { it.selectedDate }
                .distinctUntilChanged()
                .collectLatest { date ->
                    diaryDao.getEntryByDate(date).collect { entry ->
                        _currentDiary.value = entry
                    }
                }
        }

        // 男性用户：自动拉取伴侣经期数据存入本地数据库
        viewModelScope.launch {
            try {
                val user = AuthManager.userState.value
                if (user != null && user.gender == "male") {
                    val token = getToken()
                    if (token.isNotEmpty()) {
                        syncPartnerData(token)
                    }
                    // 也监听 userState 变化（登录后触发）
                    AuthManager.userState.filterNotNull()
                        .filter { it.gender == "male" }
                        .collectLatest {
                            val t = getToken()
                            if (t.isNotEmpty()) {
                                syncPartnerData(t)
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e("PeriodViewModel", "伴侣数据同步检查失败", e)
            }
        }

        // 注册广播接收器：伴侣数据更新时自动重新拉取
        partnerUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.i("PeriodViewModel", "收到伴侣数据更新广播，开始重新同步...")
                viewModelScope.launch {
                    try {
                        val t = getToken()
                        if (t.isNotEmpty()) {
                            syncPartnerData(t)
                        }
                    } catch (e: Exception) {
                        Log.e("PeriodViewModel", "广播触发伴侣同步失败", e)
                    }
                }
            }
        }
        val filter = IntentFilter("com.laileme.app.PARTNER_DATA_UPDATED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(partnerUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(partnerUpdateReceiver, filter)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            partnerUpdateReceiver?.let {
                getApplication<Application>().unregisterReceiver(it)
            }
        } catch (e: Exception) {
            Log.e("PeriodViewModel", "unregister receiver err", e)
        }
    }

    /** 从服务器拉取伴侣经期数据并存入本地数据库 */
    suspend fun syncPartnerData(token: String) {
        try {
            val partnerData = PartnerManager.getPartnerData(token)
            if (partnerData != null && partnerData.periodRecords.isNotEmpty()) {
                // 清空旧数据再写入（男性自己不记录，全部是伴侣的）
                val existingRecords = dao.getAllList()
                if (existingRecords.isNotEmpty()) {
                    dao.deleteAll(existingRecords)
                }

                // 将伴侣经期记录写入本地数据库
                for (p in partnerData.periodRecords) {
                    val record = PeriodRecord(
                        startDate = p.optLong("startDate", 0L),
                        endDate = if (p.isNull("endDate")) null else p.optLong("endDate"),
                        cycleLength = p.optInt("cycleLength", 28),
                        periodLength = p.optInt("periodLength", 5),
                        symptoms = p.optString("symptoms", ""),
                        mood = p.optString("mood", ""),
                        notes = p.optString("notes", "")
                    )
                    if (record.startDate > 0) {
                        dao.insert(record)
                    }
                }

                // 写入伴侣的睡眠数据
                val sleepDao = db.sleepDao()
                for (s in partnerData.sleepRecords) {
                    val sleepRecord = SleepRecord(
                        date = s.optString("date", ""),
                        bedtime = s.optString("bedtime", ""),
                        waketime = s.optString("waketime", "")
                    )
                    if (sleepRecord.date.isNotEmpty()) {
                        sleepDao.insert(sleepRecord)
                    }
                }

                // 写入伴侣的习惯打卡数据到 discover prefs
                if (partnerData.habitCheckins.isNotEmpty()) {
                    val discoverPrefs = getApplication<android.app.Application>()
                        .getSharedPreferences("laileme_discover", android.content.Context.MODE_PRIVATE)
                    discoverPrefs.edit()
                        .putString("partner_habit_checkins", partnerData.habitCheckins)
                        .putString("partner_habits", partnerData.habits)
                        .apply()
                    Log.i("PeriodViewModel", "伴侣习惯打卡数据已保存")
                }

                Log.i("PeriodViewModel", "伴侣数据同步成功: ${partnerData.periodRecords.size}条经期, ${partnerData.sleepRecords.size}条睡眠")
            } else if (partnerData != null) {
                // 没有经期数据但可能有习惯打卡数据
                if (partnerData.habitCheckins.isNotEmpty()) {
                    val discoverPrefs = getApplication<android.app.Application>()
                        .getSharedPreferences("laileme_discover", android.content.Context.MODE_PRIVATE)
                    discoverPrefs.edit()
                        .putString("partner_habit_checkins", partnerData.habitCheckins)
                        .putString("partner_habits", partnerData.habits)
                        .apply()
                    Log.i("PeriodViewModel", "伴侣习惯打卡数据已保存（无经期数据）")
                }
                Log.i("PeriodViewModel", "伴侣无经期数据")
            } else {
                Log.i("PeriodViewModel", "伴侣未绑定或无数据")
            }
        } catch (e: Exception) {
            Log.e("PeriodViewModel", "伴侣数据同步失败", e)
        }
    }

    /** 保存周期设置（首次设置 or 设置页修改） */
    fun saveCycleSettings(cycleLength: Int, periodLength: Int) {
        val cycle = cycleLength.coerceIn(15, 60)
        val period = periodLength.coerceIn(1, 15)
        prefs.edit()
            .putBoolean("has_setup", true)
            .putInt("cycle_length", cycle)
            .putInt("period_length", period)
            .apply()
        _uiState.update {
            it.copy(hasSetup = true, savedCycleLength = cycle, savedPeriodLength = period)
        }
        SyncManager.triggerImmediateSync()
    }

    /** 保存记录模式（auto=自动推算 / manual=手动输入） */
    fun saveTrackingMode(mode: String) {
        prefs.edit()
            .putString("tracking_mode", mode)
            .putBoolean("has_setup", true)
            .apply()
        _uiState.update { it.copy(trackingMode = mode, hasSetup = true) }
        SyncManager.triggerImmediateSync()
    }

    // ─────────────── 核心计算 ───────────────

    private fun recalculate(records: List<PeriodRecord>, latest: PeriodRecord?) {
        if (latest == null) {
            _uiState.update {
                it.copy(
                    records = records,
                    latestRecord = null,
                    isInPeriod = false,
                    hasActivePeriod = false,
                    daysUntilPeriodEnd = 0,
                    daysUntilNextPeriod = 0,
                    currentPhase = "请记录经期",
                    cycleDay = 0,
                    cycleProgress = 0f,
                    periodProgress = 0f
                )
            }
            return
        }

        val todayMs = normalizeDate(System.currentTimeMillis())
        val startMs = normalizeDate(latest.startDate)
        val daysSinceStart = daysBetween(startMs, todayMs)

        // 经期实际结束日（含当天）
        val periodEndMs = if (latest.endDate != null) {
            normalizeDate(latest.endDate)
        } else {
            // 用户未手动结束经期时，不根据periodLength自动结束
            // 经期持续到今天或预计结束日的较大值
            maxOf(startMs + (latest.periodLength - 1) * 24L * 60 * 60 * 1000, todayMs)
        }

        // 有没有"活跃"（未结束）的经期 => 决定"结束"按钮是否可点
        val hasActivePeriod = latest.endDate == null

        // 今天是否在经期范围内：只有未手动结束时才算经期中
        val isInPeriod = hasActivePeriod && todayMs in startMs..periodEndMs

        // 简化：只在第一个周期内精确判断，后续周期用预测
        val cycleLength = latest.cycleLength
        val periodLength = latest.periodLength

        // 当前周期第几天（从1开始）
        val cycleDay = if (daysSinceStart >= 0) {
            (daysSinceStart % cycleLength) + 1
        } else 0

        // 距经期结束天数
        val daysUntilPeriodEnd = if (isInPeriod) {
            daysBetween(todayMs, periodEndMs).coerceAtLeast(0)
        } else 0

        // 距下次经期天数
        val nextPeriodStartMs = if (daysSinceStart < cycleLength) {
            startMs + cycleLength * 24L * 60 * 60 * 1000
        } else {
            // 已过多个周期，计算最近的下一个周期开始
            val cyclesPassed = daysSinceStart / cycleLength
            startMs + (cyclesPassed + 1) * cycleLength * 24L * 60 * 60 * 1000
        }
        val daysUntilNextPeriod = if (isInPeriod) 0 else {
            daysBetween(todayMs, nextPeriodStartMs).coerceAtLeast(0)
        }

        val effectiveDay = if (daysSinceStart >= 0) daysSinceStart % cycleLength else 0

        val phase = when {
            daysSinceStart < 0 -> "等待中"
            isInPeriod -> "经期第${daysBetween(startMs, todayMs) + 1}天"
            effectiveDay < cycleLength / 2 - 2 -> "安全期"
            effectiveDay < cycleLength / 2 + 2 -> "排卵期"
            effectiveDay < cycleLength - 3 -> "黄体期"
            else -> "经期将至"
        }

        val cycleProgress = if (cycleLength > 0) effectiveDay.toFloat() / cycleLength else 0f
        val periodProgress = if (isInPeriod && periodLength > 0) {
            val raw = daysBetween(startMs, todayMs).toFloat() / periodLength
            if (raw > 0.9f) 0.9f else raw  // 超过预设天数后保持90%
        } else 0f

        _uiState.update {
                // 需要至少2条已完成记录才能推算出真正的周期长度
                val completedCount = records.count { it.endDate != null }
                it.copy(
                    records = records,
                    latestRecord = latest,
                    isInPeriod = isInPeriod,
                    hasActivePeriod = hasActivePeriod,
                    daysUntilPeriodEnd = daysUntilPeriodEnd,
                    daysUntilNextPeriod = daysUntilNextPeriod,
                    currentPhase = phase,
                    cycleDay = cycleDay,
                    cycleProgress = cycleProgress,
                    periodProgress = periodProgress,
                    isFirstRecord = completedCount < 2
                )
        }
    }

    // ─────────────── 用户操作 ───────────────

    /**
     * 记录经期开始
     * - 自动推算模式：根据上一次已完成记录计算周期长度
     * - 手动输入模式：使用 SharedPreferences 中保存的固定值
     */
    fun addPeriodRecord(startDate: Long) {
        viewModelScope.launch {
            // 如果已有未结束的记录，不允许再创建新的
            val activeRecords = dao.getActiveRecords()
            if (activeRecords.isNotEmpty()) {
                return@launch
            }

            val normalized = normalizeDate(startDate)
            val state = _uiState.value

            // 检查最近一条已完成的记录
            val lastCompleted = dao.getLatestCompletedRecord()
            if (lastCompleted != null) {
                val lastStartNorm = normalizeDate(lastCompleted.startDate)
                val daysSinceLast = daysBetween(lastStartNorm, normalized)

                // 距上次结束日期的天数
                val lastEndNorm = if (lastCompleted.endDate != null) normalizeDate(lastCompleted.endDate) else lastStartNorm
                val daysSinceEnd = daysBetween(lastEndNorm, normalized)

                // 重新激活条件（视为同一次经期，而非新周期）：
                // 1. 距上次开始不超过经期天数+2天（还在同一个经期窗口内），或
                // 2. 距上次结束不超过3天（刚结束就又来了）
                // 防止经期中途误点"结束"再"开始"导致创建极短周期记录
                if (daysSinceLast < lastCompleted.periodLength + 2 || daysSinceEnd < 3) {
                    // 恢复经期天数为用户设置的默认值（结束时可能被改成了1天）
                    dao.update(lastCompleted.copy(
                        endDate = null,
                        periodLength = state.savedPeriodLength
                    ))
                    SyncManager.triggerImmediateSync()
                    return@launch
                }
            }

            val cycleLen: Int
            val periodLen: Int

            if (state.trackingMode == "auto") {
                // 自动推算模式：根据历史记录计算
                if (lastCompleted != null) {
                    // 有已完成的记录 → 用两次开始日期的间隔作为周期长度
                    val lastStart = normalizeDate(lastCompleted.startDate)
                    val calculatedCycle = daysBetween(lastStart, normalized)
                    cycleLen = calculatedCycle.coerceIn(15, 60)
                    // 经期天数用上次的实际值
                    periodLen = lastCompleted.periodLength
                } else {
                    // 首次记录，没有历史数据，用默认值
                    cycleLen = state.savedCycleLength
                    periodLen = state.savedPeriodLength
                }
            } else {
                // 手动输入模式：使用用户设定的固定值
                cycleLen = state.savedCycleLength
                periodLen = state.savedPeriodLength
            }

            dao.insert(
                PeriodRecord(
                    startDate = normalized,
                    cycleLength = cycleLen,
                    periodLength = periodLen
                )
            )
            SyncManager.triggerImmediateSync()
        }
    }

    /** 记录经期结束 — 结束所有活跃记录 */
    fun endPeriod(endDate: Long) {
        viewModelScope.launch {
            val activeRecords = dao.getActiveRecords()
            if (activeRecords.isEmpty()) return@launch  // 没有活跃的经期

            val normalizedEnd = normalizeDate(endDate)

            // 只保留最新的一条活跃记录并结束它，多余的重复记录删掉
            val primary = activeRecords.first()
            if (activeRecords.size > 1) {
                dao.deleteAll(activeRecords.drop(1))
            }

            val normalizedStart = normalizeDate(primary.startDate)

            // 结束日期不能早于开始日期，如果早于就用开始日期当天
            val finalEnd = if (normalizedEnd < normalizedStart) normalizedStart else normalizedEnd
            val actualPeriodLength = (daysBetween(normalizedStart, finalEnd) + 1).coerceIn(1, 15)

            dao.update(
                primary.copy(
                    endDate = finalEnd,
                    periodLength = actualPeriodLength
                )
            )
            SyncManager.triggerImmediateSync()
        }
    }

    fun updateSelectedDate(date: Long) {
        _uiState.update { it.copy(selectedDate = normalizeDate(date)) }
    }

    fun changeMonth(delta: Int) {
        _uiState.update {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, it.currentMonthYear)
                set(Calendar.MONTH, it.currentMonthMonth)
                add(Calendar.MONTH, delta)
            }
            it.copy(
                currentMonthYear = cal.get(Calendar.YEAR),
                currentMonthMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun deleteRecord(record: PeriodRecord) {
        viewModelScope.launch {
            dao.delete(record)
            SyncManager.triggerImmediateSync()
        }
    }

    /** 重置当前经期记录（删除最新的活跃/最近记录） */
    fun resetLatestRecord() {
        viewModelScope.launch {
            // 优先删除活跃记录，没有的话删最近已完成的
            val active = dao.getActiveRecords()
            if (active.isNotEmpty()) {
                dao.deleteAll(active)
            } else {
                val latest = dao.getLatestCompletedRecord()
                if (latest != null) dao.delete(latest)
            }
            SyncManager.triggerImmediateSync()
            // Flow 会自动触发状态刷新
        }
    }

    // ─────────────── 日记操作 ───────────────

    fun saveDiary(entry: DiaryEntry) {
        viewModelScope.launch {
            val normalizedDate = normalizeDate(entry.date)
            val existing = diaryDao.getEntryByDateOnce(normalizedDate)
            if (existing != null) {
                diaryDao.update(entry.copy(id = existing.id, date = normalizedDate))
            } else {
                diaryDao.insert(entry.copy(date = normalizedDate))
            }
            SyncManager.triggerImmediateSync()
        }
    }
}

package com.laileme.app.ui

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 全局番茄钟状态（单例）
 * DiscoverScreen 和 FocusFullScreenActivity 共享同一份状态
 */
object FocusTimerState {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _isBreak = MutableStateFlow(false)
    val isBreak: StateFlow<Boolean> = _isBreak

    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private val _completedToday = MutableStateFlow(0)
    val completedToday: StateFlow<Int> = _completedToday

    private val _totalFocusMinutesToday = MutableStateFlow(0)
    val totalFocusMinutesToday: StateFlow<Int> = _totalFocusMinutesToday

    private val _focusDuration = MutableStateFlow(25)
    val focusDuration: StateFlow<Int> = _focusDuration

    private val _breakDuration = MutableStateFlow(5)
    val breakDuration: StateFlow<Int> = _breakDuration

    private var timerJob: Job? = null
    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("laileme_discover", Context.MODE_PRIVATE)
        val p = prefs!!
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val savedDate = p.getString("focus_date", "") ?: ""

        _focusDuration.value = p.getInt("focus_duration", 25)
        _breakDuration.value = p.getInt("break_duration", 5)
        _remainingSeconds.value = _focusDuration.value * 60
        _completedToday.value = if (savedDate == today) p.getInt("focus_completed", 0) else 0
        _totalFocusMinutesToday.value = if (savedDate == today) p.getInt("focus_total_minutes", 0) else 0
    }

    fun start() {
        _isRunning.value = true
        _isPaused.value = false
        _isBreak.value = false
        _remainingSeconds.value = _focusDuration.value * 60
        startCountdown()
    }

    fun pause() {
        _isPaused.value = true
        timerJob?.cancel()
    }

    fun resume() {
        _isPaused.value = false
        startCountdown()
    }

    fun stop() {
        timerJob?.cancel()
        _isRunning.value = false
        _isPaused.value = false
        _isBreak.value = false
        _remainingSeconds.value = _focusDuration.value * 60
    }

    fun togglePause() {
        if (_isPaused.value) resume() else pause()
    }

    fun updateSettings(focus: Int, breakMin: Int) {
        _focusDuration.value = focus
        _breakDuration.value = breakMin
        if (!_isRunning.value) {
            _remainingSeconds.value = focus * 60
        }
        prefs?.edit()
            ?.putInt("focus_duration", focus)
            ?.putInt("break_duration", breakMin)
            ?.apply()
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000L)
                if (!_isRunning.value || _isPaused.value) break
                _remainingSeconds.value--
            }
            if (_remainingSeconds.value <= 0 && _isRunning.value) {
                onTimerComplete()
            }
        }
    }

    private fun onTimerComplete() {
        if (!_isBreak.value) {
            // 专注结束 → 记录
            _completedToday.value++
            _totalFocusMinutesToday.value += _focusDuration.value
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            prefs?.edit()
                ?.putString("focus_date", today)
                ?.putInt("focus_completed", _completedToday.value)
                ?.putInt("focus_total_minutes", _totalFocusMinutesToday.value)
                ?.apply()
            // 切到休息
            _isBreak.value = true
            _remainingSeconds.value = _breakDuration.value * 60
            startCountdown()
        } else {
            // 休息结束
            _isBreak.value = false
            _remainingSeconds.value = _focusDuration.value * 60
            _isRunning.value = false
            _isPaused.value = false
        }
    }
}

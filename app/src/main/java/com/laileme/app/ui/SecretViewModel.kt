package com.laileme.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.laileme.app.data.AppDatabase
import com.laileme.app.data.entity.SecretRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import com.laileme.app.ui.normalizeDate

data class SecretUiState(
    val records: List<SecretRecord> = emptyList(),
    val selectedDate: Long = normalizeDate(System.currentTimeMillis()),
    val currentMonthYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val currentMonthMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val currentRecord: SecretRecord? = null,
    val defaultHadSex: Boolean = false   // 继承最近一次记录的爱爱状态
) {
    val currentMonth: Calendar
        get() = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentMonthYear)
            set(Calendar.MONTH, currentMonthMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
}

class SecretViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.secretDao()

    private val _uiState = MutableStateFlow(SecretUiState())
    val uiState: StateFlow<SecretUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAll().collect { records ->
                _uiState.value = _uiState.value.copy(records = records)
                loadRecordForSelectedDate()
            }
        }
    }

    fun updateSelectedDate(date: Long) {
        _uiState.value = _uiState.value.copy(selectedDate = normalizeDate(date))
        loadRecordForSelectedDate()
    }

    fun changeMonth(offset: Int) {
        val cal = _uiState.value.currentMonth.apply {
            add(Calendar.MONTH, offset)
        }
        _uiState.value = _uiState.value.copy(
            currentMonthYear = cal.get(Calendar.YEAR),
            currentMonthMonth = cal.get(Calendar.MONTH)
        )
    }

    private fun loadRecordForSelectedDate() {
        val state = _uiState.value
        val r = state.records.find { it.date == state.selectedDate }
        // 只要历史上有任何一次爱爱记录，所有未记录的日期都默认继承开启状态
        val latestHadSex = state.records.any { it.hadSex }
        _uiState.value = state.copy(currentRecord = r, defaultHadSex = latestHadSex)
    }

    fun saveRecord(record: SecretRecord) {
        viewModelScope.launch {
            dao.insert(record)
        }
    }
    
    fun deleteRecordForDate(date: Long) {
        viewModelScope.launch {
            dao.deleteByDate(date)
        }
    }
}
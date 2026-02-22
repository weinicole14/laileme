package com.laileme.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_records")
data class PeriodRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDate: Long,  // 开始日期时间戳
    val endDate: Long? = null,  // 结束日期时间戳
    val cycleLength: Int = 28,  // 周期长度
    val periodLength: Int = 5,  // 经期长度
    val symptoms: String = "",  // 症状记录
    val mood: String = "",  // 情绪记录
    val notes: String = ""  // 备注
)

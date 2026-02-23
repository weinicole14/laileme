package com.laileme.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey
    val date: String,           // "yyyy-MM-dd" 每天一条记录
    val bedtime: String = "",   // "HH:mm" 入睡时间
    val waketime: String = ""   // "HH:mm" 起床时间
)

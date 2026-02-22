package com.laileme.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,            // 归一化日期时间戳
    val mood: String = "",     // 心情 emoji
    val symptoms: String = "", // 症状
    val notes: String = ""     // 日记内容
)

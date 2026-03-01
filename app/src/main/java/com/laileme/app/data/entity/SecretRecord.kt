package com.laileme.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secret_records")
data class SecretRecord(
    @PrimaryKey
    val date: Long,                  // 归一化日期时间戳
    val hadSex: Boolean = false,     // 是否有爱爱
    val protection: String = "",     // 避孕方式: none, condom, pill, iud, safe_period, other
    val feeling: Int = 0,            // 体验评分: 0=未评, 1-5星
    val mood: String = "",           // 伴随心情
    val notes: String = ""           // 额外的私密日记文字
)
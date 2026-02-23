package com.laileme.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,            // 归一化日期时间戳
    val mood: String = "",     // 心情
    val symptoms: String = "", // 症状（兼容旧版）
    val notes: String = "",    // 日记内容
    // ── 身体状态字段 ──
    val flowLevel: Int = 0,         // 月经量: 0=未记录, 1=少, 2=中, 3=多
    val flowColor: String = "",     // 颜色: light_red, red, dark_red, brown, black
    val painLevel: Int = 0,         // 疼痛程度: 0=无, 1=轻微, 2=中等, 3=较重, 4=严重
    val breastPain: Int = 0,        // 胸部胀痛: 0=无, 1=轻微, 2=明显, 3=严重
    val digestive: Int = 0,         // 肠胃: 0=正常, 1=轻微不适, 2=腹泻, 3=便秘
    val backPain: Int = 0,          // 腰腹痛: 0=无, 1=轻微, 2=明显, 3=严重
    val headache: Int = 0,          // 头痛: 0=无, 1=轻微, 2=明显, 3=严重
    val fatigue: Int = 0,           // 疲劳: 0=精力充沛, 1=正常, 2=有点累, 3=很疲惫
    val skinCondition: String = "", // 皮肤: good, normal, oily, acne, dry
    val temperature: String = "",   // 体温（字符串形式存储）
    val appetite: Int = 0,          // 食欲: 0=正常, 1=增加, 2=减少
    val discharge: String = ""      // 分泌物: none, clear, white, yellow, sticky
)

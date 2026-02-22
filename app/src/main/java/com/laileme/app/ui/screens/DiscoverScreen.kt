package com.laileme.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laileme.app.ui.theme.*

@Composable
fun DiscoverScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
            .padding(bottom = 60.dp)
    ) {
        Text(
            text = "发现",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 健康小贴士
        TipCard(
            icon = Icons.Outlined.Lightbulb,
            title = "今日小贴士",
            content = "经期前多补充铁元素，可以吃些红枣、菠菜等食物哦~",
            iconColor = AccentOrange
        )

        Spacer(modifier = Modifier.height(12.dp))

        TipCard(
            icon = Icons.Outlined.SelfImprovement,
            title = "放松心情",
            content = "适当的瑜伽和冥想可以缓解经期不适感",
            iconColor = AccentTeal
        )

        Spacer(modifier = Modifier.height(12.dp))

        TipCard(
            icon = Icons.Outlined.LocalCafe,
            title = "暖心推荐",
            content = "红糖姜茶是经期的好伙伴，暖宫又舒适",
            iconColor = PrimaryPink
        )

        Spacer(modifier = Modifier.height(12.dp))

        TipCard(
            icon = Icons.Outlined.Bedtime,
            title = "充足睡眠",
            content = "保持规律作息，每天7-8小时睡眠有助于调节周期",
            iconColor = AccentBlue
        )
    }
}

@Composable
private fun TipCard(icon: ImageVector, title: String, content: String, iconColor: Color = PrimaryPink) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = iconColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

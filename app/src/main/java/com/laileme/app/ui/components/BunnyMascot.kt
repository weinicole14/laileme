package com.laileme.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun BunnyMascot(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(70.dp, 84.dp)) {
        val width = size.width
        val height = size.height

        // 左耳
        val leftEarPath = Path().apply {
            moveTo(width * 0.25f, height * 0.45f)
            quadraticBezierTo(width * 0.15f, height * 0.1f, width * 0.3f, height * 0.05f)
            quadraticBezierTo(width * 0.4f, height * 0.1f, width * 0.35f, height * 0.45f)
            close()
        }
        drawPath(leftEarPath, Color(0xFFFFF5F5))

        // 左耳内部粉色
        val leftEarInnerPath = Path().apply {
            moveTo(width * 0.27f, height * 0.42f)
            quadraticBezierTo(width * 0.2f, height * 0.15f, width * 0.3f, height * 0.1f)
            quadraticBezierTo(width * 0.37f, height * 0.15f, width * 0.33f, height * 0.42f)
            close()
        }
        drawPath(leftEarInnerPath, Color(0xFFFFB6C1))

        // 右耳
        val rightEarPath = Path().apply {
            moveTo(width * 0.65f, height * 0.45f)
            quadraticBezierTo(width * 0.6f, height * 0.1f, width * 0.7f, height * 0.05f)
            quadraticBezierTo(width * 0.85f, height * 0.1f, width * 0.75f, height * 0.45f)
            close()
        }
        drawPath(rightEarPath, Color(0xFFFFF5F5))

        // 右耳内部粉色
        val rightEarInnerPath = Path().apply {
            moveTo(width * 0.67f, height * 0.42f)
            quadraticBezierTo(width * 0.63f, height * 0.15f, width * 0.7f, height * 0.1f)
            quadraticBezierTo(width * 0.8f, height * 0.15f, width * 0.73f, height * 0.42f)
            close()
        }
        drawPath(rightEarInnerPath, Color(0xFFFFB6C1))

        // 头部（椭圆）
        drawOval(
            color = Color(0xFFFFF5F5),
            topLeft = Offset(width * 0.15f, height * 0.35f),
            size = Size(width * 0.7f, height * 0.55f)
        )

        // 左眼
        drawCircle(
            color = Color(0xFF2D3436),
            radius = width * 0.04f,
            center = Offset(width * 0.35f, height * 0.55f)
        )

        // 右眼
        drawCircle(
            color = Color(0xFF2D3436),
            radius = width * 0.04f,
            center = Offset(width * 0.65f, height * 0.55f)
        )

        // 眼睛高光
        drawCircle(
            color = Color.White,
            radius = width * 0.015f,
            center = Offset(width * 0.36f, height * 0.54f)
        )
        drawCircle(
            color = Color.White,
            radius = width * 0.015f,
            center = Offset(width * 0.66f, height * 0.54f)
        )

        // 鼻子
        drawCircle(
            color = Color(0xFFFFB6C1),
            radius = width * 0.03f,
            center = Offset(width * 0.5f, height * 0.65f)
        )

        // 嘴巴
        val mouthPath = Path().apply {
            moveTo(width * 0.5f, height * 0.68f)
            quadraticBezierTo(width * 0.42f, height * 0.75f, width * 0.45f, height * 0.72f)
        }
        drawPath(mouthPath, Color(0xFFFFB6C1))

        val mouthPath2 = Path().apply {
            moveTo(width * 0.5f, height * 0.68f)
            quadraticBezierTo(width * 0.58f, height * 0.75f, width * 0.55f, height * 0.72f)
        }
        drawPath(mouthPath2, Color(0xFFFFB6C1))

        // 腮红
        drawCircle(
            color = Color(0xFFFFB6C1).copy(alpha = 0.4f),
            radius = width * 0.06f,
            center = Offset(width * 0.22f, height * 0.65f)
        )
        drawCircle(
            color = Color(0xFFFFB6C1).copy(alpha = 0.4f),
            radius = width * 0.06f,
            center = Offset(width * 0.78f, height * 0.65f)
        )

        // 前爪
        drawOval(
            color = Color(0xFFFFF5F5),
            topLeft = Offset(width * 0.2f, height * 0.8f),
            size = Size(width * 0.2f, height * 0.12f)
        )
        drawOval(
            color = Color(0xFFFFF5F5),
            topLeft = Offset(width * 0.6f, height * 0.8f),
            size = Size(width * 0.2f, height * 0.12f)
        )
    }
}

/**
 * 趴着的兔子吉祥物 - 趴在边缘上，两只小爪子挂下来
 */
@Composable
fun BunnyMascotLying(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(80.dp, 56.dp)) {
        val w = size.width
        val h = size.height

        // 身体（大椭圆，偏右后方，水平躺着）
        drawOval(
            color = Color(0xFFFFF5F5),
            topLeft = Offset(w * 0.35f, h * 0.15f),
            size = Size(w * 0.6f, h * 0.5f)
        )

        // 左耳
        val leftEar = Path().apply {
            moveTo(w * 0.18f, h * 0.38f)
            quadraticBezierTo(w * 0.08f, h * 0.02f, w * 0.22f, h * 0.0f)
            quadraticBezierTo(w * 0.32f, h * 0.05f, w * 0.28f, h * 0.38f)
            close()
        }
        drawPath(leftEar, Color(0xFFFFF5F5))
        // 左耳内粉
        val leftEarInner = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            quadraticBezierTo(w * 0.13f, h * 0.08f, w * 0.22f, h * 0.05f)
            quadraticBezierTo(w * 0.29f, h * 0.1f, w * 0.26f, h * 0.35f)
            close()
        }
        drawPath(leftEarInner, Color(0xFFFFB6C1))

        // 右耳
        val rightEar = Path().apply {
            moveTo(w * 0.32f, h * 0.38f)
            quadraticBezierTo(w * 0.28f, h * 0.05f, w * 0.38f, h * 0.0f)
            quadraticBezierTo(w * 0.48f, h * 0.05f, w * 0.42f, h * 0.38f)
            close()
        }
        drawPath(rightEar, Color(0xFFFFF5F5))
        // 右耳内粉
        val rightEarInner = Path().apply {
            moveTo(w * 0.34f, h * 0.35f)
            quadraticBezierTo(w * 0.31f, h * 0.1f, w * 0.38f, h * 0.05f)
            quadraticBezierTo(w * 0.45f, h * 0.1f, w * 0.4f, h * 0.35f)
            close()
        }
        drawPath(rightEarInner, Color(0xFFFFB6C1))

        // 头部（圆形，偏左前方）
        drawOval(
            color = Color(0xFFFFF5F5),
            topLeft = Offset(w * 0.1f, h * 0.25f),
            size = Size(w * 0.4f, h * 0.45f)
        )

        // 左眼
        drawCircle(
            color = Color(0xFF2D3436),
            radius = w * 0.025f,
            center = Offset(w * 0.22f, h * 0.42f)
        )
        // 右眼
        drawCircle(
            color = Color(0xFF2D3436),
            radius = w * 0.025f,
            center = Offset(w * 0.38f, h * 0.42f)
        )
        // 眼睛高光
        drawCircle(Color.White, w * 0.01f, Offset(w * 0.225f, h * 0.415f))
        drawCircle(Color.White, w * 0.01f, Offset(w * 0.385f, h * 0.415f))

        // 鼻子
        drawCircle(
            color = Color(0xFFFFB6C1),
            radius = w * 0.02f,
            center = Offset(w * 0.3f, h * 0.5f)
        )

        // 嘴巴
        val mouth1 = Path().apply {
            moveTo(w * 0.3f, h * 0.52f)
            quadraticBezierTo(w * 0.25f, h * 0.57f, w * 0.27f, h * 0.55f)
        }
        drawPath(mouth1, Color(0xFFFFB6C1))
        val mouth2 = Path().apply {
            moveTo(w * 0.3f, h * 0.52f)
            quadraticBezierTo(w * 0.35f, h * 0.57f, w * 0.33f, h * 0.55f)
        }
        drawPath(mouth2, Color(0xFFFFB6C1))

        // 腮红
        drawCircle(Color(0xFFFFB6C1).copy(alpha = 0.4f), w * 0.035f, Offset(w * 0.14f, h * 0.5f))
        drawCircle(Color(0xFFFFB6C1).copy(alpha = 0.4f), w * 0.035f, Offset(w * 0.46f, h * 0.5f))

        // 两只前爪 - 挂在边缘下面
        drawOval(
            color = Color(0xFFFFF5F5),
            topLeft = Offset(w * 0.15f, h * 0.65f),
            size = Size(w * 0.13f, h * 0.22f)
        )
        drawOval(
            color = Color(0xFFFFF5F5),
            topLeft = Offset(w * 0.33f, h * 0.65f),
            size = Size(w * 0.13f, h * 0.22f)
        )

        // 小尾巴（圆球在身体后方）
        drawCircle(
            color = Color(0xFFFFF5F5),
            radius = w * 0.05f,
            center = Offset(w * 0.92f, h * 0.3f)
        )
    }
}

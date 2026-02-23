package com.laileme.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.laileme.app.data.AuthManager
import com.laileme.app.data.AuthResult
import com.laileme.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }  // true=登录, false=注册
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // 用户协议弹窗
    if (showTermsDialog) {
        AgreementDialog(
            title = "用户协议",
            content = userAgreementText,
            onDismiss = { showTermsDialog = false }
        )
    }

    // 隐私政策弹窗
    if (showPrivacyDialog) {
        AgreementDialog(
            title = "隐私政策",
            content = privacyPolicyText,
            onDismiss = { showPrivacyDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // 顶部返回栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPink.copy(alpha = 0.1f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "关闭",
                    modifier = Modifier.size(20.dp),
                    tint = PrimaryPink
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isLoginMode) "登录" else "注册",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo / 头像区域
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(PrimaryPink.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = PrimaryPink
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "来了么",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPink
            )
            Text(
                text = if (isLoginMode) "登录你的账号" else "创建新账号",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 错误提示
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PeriodRed.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = PeriodRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            errorMessage ?: "",
                            fontSize = 13.sp,
                            color = PeriodRed
                        )
                    }
                }
            }

            // 用户名输入
            LoginTextField(
                value = username,
                onValueChange = { username = it; errorMessage = null },
                label = "用户名",
                leadingIcon = Icons.Outlined.Person
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 注册模式：昵称
            AnimatedVisibility(visible = !isLoginMode) {
                Column {
                    LoginTextField(
                        value = nickname,
                        onValueChange = { nickname = it; errorMessage = null },
                        label = "昵称（可选）",
                        leadingIcon = Icons.Outlined.Badge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 密码输入
            LoginTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = "密码",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePassword = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 注册模式：确认密码
            AnimatedVisibility(visible = !isLoginMode) {
                Column {
                    LoginTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = "确认密码",
                        leadingIcon = Icons.Outlined.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 用户协议勾选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it },
                    modifier = Modifier.size(20.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryPink,
                        uncheckedColor = TextHint,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = TextSecondary, fontSize = 12.sp)) {
                            append("我已阅读并同意")
                        }
                        withStyle(SpanStyle(
                            color = PrimaryPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append("《用户协议》")
                        }
                        withStyle(SpanStyle(color = TextSecondary, fontSize = 12.sp)) {
                            append("和")
                        }
                        withStyle(SpanStyle(
                            color = PrimaryPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append("《隐私政策》")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 可点击的协议链接（单独一行，方便点击）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "查看用户协议",
                    fontSize = 11.sp,
                    color = PrimaryPink.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { showTermsDialog = true }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "查看隐私政策",
                    fontSize = 11.sp,
                    color = PrimaryPink.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { showPrivacyDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 登录/注册按钮
            Button(
                onClick = {
                    if (!agreedToTerms) {
                        errorMessage = "请先阅读并同意用户协议和隐私政策"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        errorMessage = null

                        if (isLoginMode) {
                            when (val result = AuthManager.login(username, password)) {
                                is AuthResult.Success -> onLoginSuccess()
                                is AuthResult.Error -> errorMessage = result.message
                            }
                        } else {
                            if (password != confirmPassword) {
                                errorMessage = "两次密码不一致"
                            } else {
                                when (val result = AuthManager.register(username, password, nickname)) {
                                    is AuthResult.Success -> onLoginSuccess()
                                    is AuthResult.Error -> errorMessage = result.message
                                }
                            }
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPink,
                    disabledContainerColor = PrimaryPink.copy(alpha = 0.4f)
                ),
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank() && agreedToTerms
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isLoginMode) Icons.Outlined.Login else Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isLoginMode) "登 录" else "注 册",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 切换登录/注册
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isLoginMode) "还没有账号？" else "已有账号？",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    if (isLoginMode) "立即注册" else "去登录",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPink,
                    modifier = Modifier.clickable {
                        isLoginMode = !isLoginMode
                        errorMessage = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ============ 协议弹窗组件 ============
@Composable
private fun AgreementDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .heightIn(max = 520.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "关闭",
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onDismiss),
                    tint = TextHint
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = content,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("我已知晓", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ============ 协议文本内容 ============
private val userAgreementText = """
来了么 用户协议

更新日期：2025年6月

欢迎使用「来了么」应用（以下简称"本应用"）。在使用本应用之前，请您仔细阅读以下条款。

一、服务说明
本应用为用户提供经期管理、睡眠记录、日记等健康生活辅助功能。本应用的所有功能仅供参考，不构成任何医疗建议。

二、账号注册与使用
1. 用户注册时应提供真实、准确的信息。
2. 用户应妥善保管账号密码，因用户保管不善导致的损失由用户自行承担。
3. 用户不得将账号转让、出借给他人使用。

三、用户行为规范
用户在使用本应用时，不得：
1. 利用本应用从事违法违规活动；
2. 干扰、破坏本应用的正常运行；
3. 未经授权访问本应用的系统或数据；
4. 发布任何违法、有害、威胁、滥用、骚扰、侵权的内容。

四、知识产权
本应用的所有内容，包括但不限于文字、图片、界面设计、程序代码等，均受知识产权法律保护，未经许可不得复制、修改或传播。

五、免责声明
1. 本应用提供的健康数据记录功能仅供参考，不能替代专业医疗诊断。
2. 因不可抗力、系统维护等原因导致的服务中断，本应用不承担责任。
3. 用户因违反本协议导致的任何损失，由用户自行承担。

六、协议修改
本应用有权在必要时修改本协议，修改后的协议将在应用内公布。用户继续使用本应用即视为同意修改后的协议。

七、联系方式
如有任何疑问，请通过应用内的反馈渠道与我们联系。
""".trimIndent()

private val privacyPolicyText = """
来了么 隐私政策

更新日期：2025年6月

我们深知个人信息对您的重要性，我们将按照法律法规要求，采取相应安全措施，保护您的个人信息安全。

一、我们收集的信息
为提供服务，我们可能收集以下信息：
1. 账号信息：用户名、昵称、密码（加密存储）；
2. 健康数据：经期记录、睡眠记录等您主动输入的数据；
3. 日记内容：您在日记功能中记录的文字内容；
4. 设备信息：设备型号、操作系统版本（仅用于适配和问题排查）。

二、信息的使用
我们收集的信息仅用于：
1. 提供和改善应用功能；
2. 保障账号安全；
3. 进行必要的数据分析以优化服务。

三、信息的存储与保护
1. 您的数据存储在安全的服务器上，我们采用业界通行的加密技术保护数据传输安全；
2. 密码经过加密处理存储，任何人（包括我们）都无法查看您的明文密码；
3. 我们会定期备份数据，防止数据丢失。

四、信息共享
我们承诺：
1. 未经您的同意，不会向第三方共享、出售您的个人信息；
2. 除非法律法规要求或政府机关依法要求，我们不会披露您的个人信息；
3. 我们不会将您的健康数据用于商业推广。

五、您的权利
您有权：
1. 查看、修改您的个人信息；
2. 删除您的账号及相关数据；
3. 撤回对本隐私政策的同意（撤回后将无法继续使用服务）。

六、未成年人保护
如果您是未满14周岁的未成年人，请在监护人的陪同下阅读本隐私政策，并在取得监护人同意后使用本应用。

七、隐私政策更新
我们可能会适时更新本隐私政策，更新后的政策将在应用内通知。

八、联系我们
如您对本隐私政策有任何疑问，请通过应用内反馈渠道联系我们。
""".trimIndent()

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        leadingIcon = {
            Icon(
                leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PrimaryPink.copy(alpha = 0.6f)
            )
        },
        trailingIcon = if (isPassword) {
            {
                Icon(
                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onTogglePassword?.invoke() },
                    tint = TextHint
                )
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryPink,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedLabelColor = PrimaryPink
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
    )
}

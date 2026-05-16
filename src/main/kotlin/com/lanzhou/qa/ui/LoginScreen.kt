package com.lanzhou.qa.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanzhou.qa.config.User
import com.lanzhou.qa.config.UserRole
import com.lanzhou.qa.database.DatabaseManager
import java.io.File
import java.util.Properties

// 国潮古风配色 - 蓝黄系列
private val BlueMain = Color(0xFF0D47A1)
private val BlueBright = Color(0xFF1565C0)
private val BlueLight = Color(0xFF42A5F5)
private val GoldMain = Color(0xFFFFC107)
private val GoldLight = Color(0xFFFFD54F)
private val CreamWhite = Color(0xFFFFF8E1)

private val rememberFile = File("remember_me.properties")

private fun loadRemembered(): Pair<String, String> {
    return try {
        if (rememberFile.exists()) {
            val props = Properties()
            rememberFile.inputStream().use { props.load(it) }
            Pair(props.getProperty("username", ""), props.getProperty("password", ""))
        } else {
            Pair("", "")
        }
    } catch (_: Exception) {
        Pair("", "")
    }
}

private fun saveRemembered(username: String, password: String) {
    try {
        val props = Properties()
        props.setProperty("username", username)
        props.setProperty("password", password)
        rememberFile.outputStream().use { props.store(it, null) }
    } catch (_: Exception) {}
}

private fun clearRemembered() {
    try { rememberFile.delete() } catch (_: Exception) {}
}

private fun loadCoverImage(): ImageBitmap? {
    val extensions = listOf("png", "jpg", "jpeg", "bmp", "webp")
    for (ext in extensions) {
        val stream = Thread.currentThread().contextClassLoader?.getResourceAsStream("cover.$ext")
        if (stream != null) {
            return try {
                loadImageBitmap(stream)
            } catch (_: Exception) {
                null
            } finally {
                stream.close()
            }
        }
    }
    return null
}

@Composable
private fun AncientStyleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = GoldMain, fontSize = 13.sp) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = modifier,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldMain,
            unfocusedBorderColor = GoldMain.copy(alpha = 0.5f),
            focusedTextColor = CreamWhite,
            unfocusedTextColor = CreamWhite,
            cursorColor = GoldLight,
            focusedContainerColor = Color(0x30000000),
            unfocusedContainerColor = Color(0x20000000),
            disabledTextColor = CreamWhite.copy(alpha = 0.5f)
        ),
        textStyle = TextStyle(fontSize = 14.sp)
    )
}

@Composable
fun LoginScreen(
    databaseManager: DatabaseManager,
    onLoginSuccess: (User) -> Unit
) {
    val remembered = remember { loadRemembered() }
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf(remembered.first) }
    var password by remember { mutableStateOf(remembered.second) }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.TOURIST) }
    var roleExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(remembered.first.isNotEmpty()) }

    val dbAvailable = remember { databaseManager.isInitialized() }
    val coverImage = remember { loadCoverImage() }

    val borderGradient = Brush.verticalGradient(
        colors = listOf(GoldLight, BlueLight, BlueBright, BlueLight, GoldLight)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (coverImage != null) {
            Image(
                bitmap = coverImage,
                contentDescription = "背景",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(borderGradient)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x660A1628),
                                    Color(0x660D1F3C),
                                    Color(0x660A1628)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 36.dp, vertical = 32.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("━━━ ✿ ━━━", color = GoldMain, fontSize = 14.sp, letterSpacing = 4.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "兰州旅游问答系统",
                            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GoldLight, letterSpacing = 6.sp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isLoginMode) "—  登  录  —" else "—  注  册  —",
                            style = TextStyle(fontSize = 15.sp, color = GoldMain, letterSpacing = 8.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("━━━━━━━━━━━━", color = GoldMain.copy(alpha = 0.4f), fontSize = 10.sp, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(20.dp))

                        if (!dbAvailable) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .background(BlueMain.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Text("⚠ 数据库未启用，请在 config.json 中启用数据库模式", color = GoldLight, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        AncientStyleTextField(
                            value = username,
                            onValueChange = { username = it; errorMessage = ""; successMessage = "" },
                            label = "用 户 名",
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        AncientStyleTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = ""; successMessage = "" },
                            label = "密    码",
                            isPassword = true,
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // 记住密码
                        if (isLoginMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = GoldMain,
                                        uncheckedColor = GoldMain.copy(alpha = 0.5f),
                                        checkmarkColor = BlueMain
                                    )
                                )
                                Text(
                                    text = "记住密码",
                                    color = GoldMain.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // 注册模式
                        if (!isLoginMode) {
                            AncientStyleTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = ""; successMessage = "" },
                                label = "确认密码",
                                isPassword = true,
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("身  份：", color = GoldMain, fontSize = 13.sp, letterSpacing = 2.sp, modifier = Modifier.width(65.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { roleExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isLoading,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                                    ) { Text(selectedRole.label, color = GoldLight) }
                                    DropdownMenu(
                                        expanded = roleExpanded,
                                        onDismissRequest = { roleExpanded = false },
                                        modifier = Modifier.background(Color(0x660D1F3C))
                                    ) {
                                        UserRole.values().forEach { role ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "${role.label}  ${when (role) { UserRole.TOURIST -> "·问答查看"; UserRole.ADMIN -> "·管理用户知识库"; UserRole.SUPER_ADMIN -> "·全部权限" }}",
                                                        color = CreamWhite, fontSize = 13.sp
                                                    )
                                                },
                                                onClick = { selectedRole = role; roleExpanded = false }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        if (errorMessage.isNotEmpty()) {
                            Text(errorMessage, color = Color(0xFFFF6B6B), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
                        }
                        if (successMessage.isNotEmpty()) {
                            Text(successMessage, color = GoldLight, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
                        }

                        // 登录/注册按钮
                        Button(
                            onClick = {
                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "用户名和密码不能为空"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = ""
                                successMessage = ""

                                if (isLoginMode) {
                                    val user = databaseManager.loginUser(username, password)
                                    if (user != null) {
                                        if (rememberMe) saveRemembered(username, password) else clearRemembered()
                                        onLoginSuccess(user)
                                    } else {
                                        errorMessage = "用户名或密码错误"
                                        isLoading = false
                                    }
                                } else {
                                    if (password != confirmPassword) {
                                        errorMessage = "两次密码输入不一致"
                                        isLoading = false
                                        return@Button
                                    }
                                    val (success, msg) = databaseManager.registerUser(username, password, selectedRole)
                                    if (success) {
                                        successMessage = "注册成功，请登录"
                                        isLoginMode = true
                                        password = ""
                                        confirmPassword = ""
                                    } else {
                                        errorMessage = msg
                                    }
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            enabled = !isLoading && dbAvailable,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlueBright, contentColor = GoldLight,
                                disabledContainerColor = BlueBright.copy(alpha = 0.4f), disabledContentColor = GoldLight.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GoldLight, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isLoginMode) "登  录" else "注  册", fontSize = 15.sp, letterSpacing = 8.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = {
                                isLoginMode = !isLoginMode
                                errorMessage = ""; successMessage = ""
                                password = ""; confirmPassword = ""
                            },
                            enabled = !isLoading
                        ) {
                            Text(if (isLoginMode) "尚无账号？点击注册" else "已有账号？点击登录", color = GoldMain.copy(alpha = 0.8f), fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("管理员初始账户：superadmin / 111111", color = BlueMain.copy(alpha = 0.7f), fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("━━━ ✿ ━━━", color = GoldMain.copy(alpha = 0.5f), fontSize = 14.sp, letterSpacing = 4.sp)
                    }
                }
            }
        }
    }
}

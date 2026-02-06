package com.lanzhou.qa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.lanzhou.qa.service.QAService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主入口 - GUI 版本
 * 使用 Compose Desktop 创建图形界面
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "兰州旅游知识问答系统 - RAG版",
        state = rememberWindowState(width = 800.dp, height = 700.dp)
    ) {
        App()
    }
}

@Composable
fun App() {
    var isInitialized by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var service by remember { mutableStateOf<QAService?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            service = QAService()
            stats = service!!.getStats()
            isInitialized = true
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Header()

            if (!isInitialized) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                MainContent(service!!, stats)
            }
        }
    }
}

@Composable
fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "兰州旅游知识问答系统",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "RAG版 - 基于知识库的智能问答",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun MainContent(service: QAService, stats: Map<String, Int>) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var isAsking by remember { mutableStateOf(false) }
    var currentSource by remember { mutableStateOf(stats["source"] ?: 0) }
    var reloadMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val sourceText = if (currentSource == 1) "数据库" else "本地JSON"

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 统计信息面板
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📊 系统信息", style = MaterialTheme.typography.titleMedium)

                    // 数据源选择按钮
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val success = service.reloadKnowledgeBase(0)
                                        currentSource = 0
                                        reloadMessage = if (success) "✅ 已切换到本地JSON" else "❌ 切换失败"
                                    }
                                }
                            },
                            colors = if (currentSource == 0) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("JSON", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val success = service.reloadKnowledgeBase(1)
                                        currentSource = 1
                                        reloadMessage = if (success) "✅ 已切换到数据库" else "❌ 切换失败，请检查数据库配置"
                                    }
                                }
                            },
                            colors = if (currentSource == 1) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("数据库", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("当前来源: $sourceText")
                    Text("知识条目: ${stats["totalItems"]} 条")
                    Text("分类数量: ${stats["categories"]} 类")
                }

                if (reloadMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reloadMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reloadMessage.contains("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // 问题输入区域
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("请输入您的问题") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !isAsking
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 测试API连接按钮
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val testResult = service.testApiConnection()
                                    answer = if (testResult) {
                                        "✅ API连接测试成功！MIMO API可以正常访问。"
                                    } else {
                                        "❌ API连接测试失败！请检查：\n1. 网络连接是否正常\n2. API地址是否正确\n3. API密钥是否有效"
                                    }
                                }
                            }
                        },
                        enabled = !isAsking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("测试API连接")
                    }

                    // 提问按钮
                    Button(
                        onClick = {
                            if (question.isNotBlank()) {
                                isAsking = true
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        answer = service.askQuestion(question)
                                    }
                                    isAsking = false
                                }
                            }
                        },
                        enabled = !isAsking && question.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isAsking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("提问")
                        }
                    }
                }
            }
        }

        // 回答显示区域
        if (answer.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "💡 AI 回答",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

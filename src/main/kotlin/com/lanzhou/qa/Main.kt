package com.lanzhou.qa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.lanzhou.qa.config.LanguageManager
import com.lanzhou.qa.service.QAService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主入口 - GUI 版本
 * 使用 Compose Desktop 创建图形界面
 */
fun main() = application {
    var isFullscreen by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = ::exitApplication,
        title = LanguageManager.getUIStrings().title,
        state = rememberWindowState(
            width = if (isFullscreen) 1920.dp else 1200.dp,
            height = if (isFullscreen) 1080.dp else 800.dp
        )
    ) {
        App(isFullscreen) { isFullscreen = it }
    }
}

@Composable
fun App(isFullscreen: Boolean, onFullscreenChange: (Boolean) -> Unit) {
    var isInitialized by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var service by remember { mutableStateOf<QAService?>(null) }
    var currentLanguage by remember { mutableStateOf(LanguageManager.getCurrentLanguageCode()) }

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
            Header(currentLanguage, isFullscreen, onFullscreenChange) { newLanguage ->
                currentLanguage = newLanguage
            }

            if (!isInitialized) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                MainContent(service!!, stats, currentLanguage)
            }
        }
    }
}

@Composable
fun Header(currentLanguage: String, isFullscreen: Boolean, onFullscreenChange: (Boolean) -> Unit, onLanguageChange: (String) -> Unit) {
    val uiStrings = LanguageManager.getUIStrings()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = uiStrings.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = uiStrings.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // 控制按钮区域
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 全屏按钮
                OutlinedButton(
                    onClick = { onFullscreenChange(!isFullscreen) },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(if (isFullscreen) "⛶" else "⛶")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFullscreen) "退出全屏" else "全屏")
                }

                // 语言选择器
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("🌐")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(LanguageManager.getLanguageName(currentLanguage))
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        LanguageManager.getAvailableLanguages().forEach { code ->
                            DropdownMenuItem(
                                text = { Text(LanguageManager.getLanguageName(code)) },
                                onClick = {
                                    LanguageManager.setCurrentLanguage(code)
                                    onLanguageChange(code)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun MainContent(service: QAService, stats: Map<String, Int>, currentLanguage: String) {
    val uiStrings = LanguageManager.getUIStrings()
    var selectedTab by remember { mutableStateOf(0) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var isAsking by remember { mutableStateOf(false) }
    var currentSource by remember { mutableStateOf(stats["source"] ?: 0) }
    var reloadMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val sourceText = if (currentSource == 1) uiStrings.database_mode else uiStrings.json_mode

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 统计信息面板
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📊 ${uiStrings.stats}", style = MaterialTheme.typography.titleMedium)

                    // 数据源选择按钮
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val success = service.reloadKnowledgeBase(0)
                                        currentSource = 0
                                        reloadMessage = if (success) uiStrings.switch_to_json_success else "❌ 切换失败"
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
                            Text(uiStrings.json_mode, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val success = service.reloadKnowledgeBase(1)
                                        currentSource = 1
                                        reloadMessage = if (success) uiStrings.switch_to_database_success else uiStrings.switch_to_database_failed
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
                            Text(uiStrings.database_mode, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${uiStrings.data_source}: $sourceText", style = MaterialTheme.typography.bodySmall)
                    Text("${uiStrings.total_items}: ${stats["totalItems"]} ${uiStrings.items}", style = MaterialTheme.typography.bodySmall)
                    Text("${uiStrings.categories}: ${stats["categories"]} 类", style = MaterialTheme.typography.bodySmall)
                    if (currentSource == 1) {
                        Text("${uiStrings.db_qa}: ${stats["db_qa_pairs"] ?: 0} ${uiStrings.items}", style = MaterialTheme.typography.bodySmall)
                        Text("${uiStrings.history}: ${stats["db_chat_history"] ?: 0} ${uiStrings.items}", style = MaterialTheme.typography.bodySmall)
                    }
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

        // 选项卡
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(uiStrings.question) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(uiStrings.knowledge_base) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(uiStrings.chat_history) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text(uiStrings.stats) }
            )
        }

        // 选项卡内容
        when (selectedTab) {
            0 -> QATab(service, question, { question = it }, { newAnswer -> answer = newAnswer }, answer, isAsking, { isAsking = it }, currentLanguage)
            1 -> KnowledgeTab(service, currentLanguage)
            2 -> ChatHistoryTab(service, currentLanguage)
            3 -> StatsTab(service, stats, currentLanguage)
        }
    }
}

/**
 * 问答选项卡
 */
@Composable
fun QATab(
    service: QAService,
    question: String,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    answer: String,
    isAsking: Boolean,
    onAskingChange: (Boolean) -> Unit,
    currentLanguage: String
) {
    val uiStrings = LanguageManager.getUIStrings()
    val actualScope = rememberCoroutineScope()
    var testResult by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 问题输入区域
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = onQuestionChange,
                    label = { Text(uiStrings.question_label) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
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
                            actualScope.launch {
                                withContext(Dispatchers.IO) {
                                    val success = service.testApiConnection()
                                    testResult = if (success) {
                                        uiStrings.test_api_success
                                    } else {
                                        uiStrings.test_api_failed
                                    }
                                }
                            }
                        },
                        enabled = !isAsking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(uiStrings.test_api)
                    }

                    // 测试数据库连接按钮
                    OutlinedButton(
                        onClick = {
                            actualScope.launch {
                                withContext(Dispatchers.IO) {
                                    val success = service.testDatabaseConnection()
                                    testResult = if (success) {
                                        uiStrings.test_db_success
                                    } else {
                                        uiStrings.test_db_failed
                                    }
                                }
                            }
                        },
                        enabled = !isAsking,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(uiStrings.test_database)
                    }

                    // 提问按钮
                    Button(
                        onClick = {
                            if (question.isNotBlank()) {
                                onAskingChange(true)
                                actualScope.launch {
                                    withContext(Dispatchers.IO) {
                                        val response = service.askQuestion(question)
                                        onAnswerChange(response)
                                    }
                                    onAskingChange(false)
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
                            Text(uiStrings.ask_ai)
                        }
                    }
                }
            }
        }

        // 测试结果显示区域
        if (testResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (testResult.contains("✅")) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "🔧 ${uiStrings.test_result}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (testResult.contains("✅")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testResult,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 回答显示区域
        if (answer.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "💡 ${uiStrings.ai_answer}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * 知识库浏览选项卡
 */
@Composable
fun KnowledgeTab(service: QAService, currentLanguage: String) {
    val uiStrings = LanguageManager.getUIStrings()
    var allKnowledge by remember { mutableStateOf(emptyList<com.lanzhou.qa.model.KnowledgeItem>()) }
    var filteredKnowledge by remember { mutableStateOf(emptyList<com.lanzhou.qa.model.KnowledgeItem>()) }
    var searchKeyword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            allKnowledge = service.getAllKnowledge()
            filteredKnowledge = allKnowledge
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // 搜索栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = { keyword ->
                        searchKeyword = keyword
                        filteredKnowledge = if (keyword.isBlank()) {
                            allKnowledge
                        } else {
                            allKnowledge.filter { item ->
                                item.question.contains(keyword, ignoreCase = true) ||
                                item.answer.contains(keyword, ignoreCase = true) ||
                                item.category.contains(keyword, ignoreCase = true)
                            }
                        }
                    },
                    label = { Text(uiStrings.search_knowledge) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, uiStrings.search_knowledge) }
                )
                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                allKnowledge = service.getAllKnowledge()
                                filteredKnowledge = allKnowledge
                                searchKeyword = ""
                            }
                        }
                    }
                ) {
                    Text(uiStrings.refresh, fontSize = 12.sp)
                }
            }

            // 结果统计
            Text(
                text = LanguageManager.formatString(uiStrings.total_records, "count" to filteredKnowledge.size),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 知识列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredKnowledge) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "❓ ${item.question}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.answer,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📚 ${item.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "${uiStrings.id}: ${item.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 聊天历史选项卡
 */
@Composable
fun ChatHistoryTab(service: QAService, currentLanguage: String) {
    val uiStrings = LanguageManager.getUIStrings()
    var chatHistory by remember { mutableStateOf(emptyList<com.lanzhou.qa.config.ChatHistory>()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            chatHistory = service.getChatHistory(100)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (chatHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiStrings.no_chat_history,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LanguageManager.formatString(uiStrings.total_records, "count" to chatHistory.size),
                    style = MaterialTheme.typography.titleSmall
                )
                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                chatHistory = service.getChatHistory(100)
                            }
                        }
                    }
                ) {
                    Text(uiStrings.refresh, fontSize = 12.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatHistory) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "🙋 ${uiStrings.question}: ${record.question}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🤖 ${uiStrings.answer}: ${record.answer}",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⏰ ${record.timestamp}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 统计信息选项卡
 */
@Composable
fun StatsTab(service: QAService, stats: Map<String, Int>, currentLanguage: String) {
    val uiStrings = LanguageManager.getUIStrings()
    var currentStats by remember { mutableStateOf(stats) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 刷新按钮
        Button(
            onClick = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        currentStats = service.getStats()
                    }
                }
            },
            modifier = Modifier.align(Alignment.End).padding(bottom = 12.dp)
        ) {
            Text(uiStrings.refresh)
        }

        // 基础统计
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📊 ${uiStrings.basic_stats}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                StatsRow(uiStrings.total_items, currentStats["totalItems"]?.toString() ?: "0")
                StatsRow(uiStrings.categories, currentStats["categories"]?.toString() ?: "0")
                StatsRow(uiStrings.data_source, if (currentStats["source"] == 1) uiStrings.database_mode else uiStrings.json_mode)
            }
        }

        // 数据库统计
        if (currentStats["source"] == 1) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🗄️ ${uiStrings.db_stats}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    StatsRow(uiStrings.db_qa_pairs, currentStats["db_qa_pairs"]?.toString() ?: "0")
                    StatsRow(uiStrings.db_chat_history, currentStats["db_chat_history"]?.toString() ?: "0")
                }
            }
        }

        // 分类统计
        val categoryStats = currentStats.filter { it.key.startsWith("category_") }
        if (categoryStats.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📚 ${uiStrings.category_stats}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    categoryStats.forEach { (key, value) ->
                        val categoryName = key.removePrefix("category_")
                        StatsRow(categoryName, "$value ${uiStrings.items}")
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

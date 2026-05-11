package com.lanzhou.qa

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import java.net.URL
import kotlin.math.floor
import kotlin.math.min
import com.lanzhou.qa.config.LanguageManager
import com.lanzhou.qa.service.QAService
import com.lanzhou.qa.ui.AudioWaveform
import com.lanzhou.qa.ui.DonutChart
import com.lanzhou.qa.ui.HorizontalBarChart
import com.lanzhou.qa.ui.StatCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AnswerBlock

data class TextBlock(val text: String) : AnswerBlock

data class ImageBlock(val bitmap: ImageBitmap, val description: String = "") : AnswerBlock

/**
 * 主入口 - GUI 版本
 * 使用 Compose Desktop 创建图形界面
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = LanguageManager.getUIStrings().title,
        state = rememberWindowState(
            width = 1200.dp,
            height = 800.dp
        )
    ) {
        App()
    }
}

@Composable
fun App() {
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
            Header(currentLanguage) { newLanguage ->
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
fun Header(currentLanguage: String, onLanguageChange: (String) -> Unit) {
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
    var imageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageBitmaps by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var isGeneratingImage by remember { mutableStateOf(false) }
    var imageMessage by remember { mutableStateOf("") }
    var imageProgressText by remember { mutableStateOf("") }
    var imageLoadedCount by remember { mutableStateOf(0) }
    var imageTotalCount by remember { mutableStateOf(0) }
    var followUpSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastProcessedAnswer by remember { mutableStateOf("") }
    var isAsking by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var currentSource by remember { mutableStateOf(stats["source"] ?: 0) }
    var reloadMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun generateAndLoadImages(answerText: String) {
        isGeneratingImage = true
        imageMessage = ""
        imageProgressText = "正在生成图片..."
        imageUrls = emptyList()
        imageBitmaps = emptyList()
        imageLoadedCount = 0
        imageTotalCount = 0

        val imageCount = calculateRequiredImageCount(answerText)
        val paths = withContext(Dispatchers.IO) {
            service.generateImagesFromAnswer(answerText, imageCount)
        }

        imageTotalCount = paths.size
        if (paths.isEmpty()) {
            imageMessage = uiStrings.image_generation_failed
            imageProgressText = ""
        } else {
            imageUrls = paths
            val loaded = mutableListOf<ImageBitmap>()

            paths.forEachIndexed { index, path ->
                imageProgressText = "正在加载图片 ${index + 1}/${paths.size}"
                imageLoadedCount = index + 1

                val bitmap = withContext(Dispatchers.IO) {
                    loadImageBitmapFromPath(path)
                }

                if (bitmap != null) {
                    loaded.add(bitmap)
                }
            }

            imageBitmaps = loaded

            if (imageBitmaps.isEmpty()) {
                imageMessage = uiStrings.no_generated_image
            } else {
                imageProgressText = "已加载 ${imageBitmaps.size}/${imageTotalCount} 张图片"
            }
        }

        isGeneratingImage = false
    }

    LaunchedEffect(answer) {
        if (answer.isNotBlank() && answer != lastProcessedAnswer) {
            imageMessage = ""
            imageProgressText = ""
            imageLoadedCount = 0
            imageTotalCount = 0
            imageBitmaps = emptyList()
            imageUrls = emptyList()
            followUpSuggestions = buildFollowUpSuggestions(answer)
            lastProcessedAnswer = answer
        }
    }

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
                    Text("${uiStrings.categories}: ${stats["categories"]}", style = MaterialTheme.typography.bodySmall)
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

    val onQuestionChange = remember { { newQuestion: String -> question = newQuestion } }
    val onAnswerChange = remember { { newAnswer: String -> answer = newAnswer } }
    val onAskingChange = remember { { newAsking: Boolean -> isAsking = newAsking } }

    // 选项卡内容
    when (selectedTab) {
        0 -> QATab(
            service = service,
            question = question,
            onQuestionChange = onQuestionChange,
            onAnswerChange = onAnswerChange,
            answer = answer,
            isAsking = isAsking,
            onAskingChange = onAskingChange,
            currentLanguage = currentLanguage,
            imageBitmaps = imageBitmaps,
            isGeneratingImage = isGeneratingImage,
            imageMessage = imageMessage,
            imageProgressText = imageProgressText,
            imageLoadedCount = imageLoadedCount,
            imageTotalCount = imageTotalCount,
            followUpSuggestions = followUpSuggestions,
            onGenerateImages = { scope.launch { generateAndLoadImages(answer) } },
            isSpeaking = isSpeaking,
            onSpeakingChange = { isSpeaking = it }
        )
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
    currentLanguage: String,
    imageBitmaps: List<ImageBitmap>,
    isGeneratingImage: Boolean,
    imageMessage: String,
    imageProgressText: String,
    imageLoadedCount: Int,
    imageTotalCount: Int,
    followUpSuggestions: List<String>,
    onGenerateImages: () -> Unit,
    isSpeaking: Boolean,
    onSpeakingChange: (Boolean) -> Unit
) {
    val uiStrings = LanguageManager.getUIStrings()
    val actualScope = rememberCoroutineScope()
    var testResult by remember { mutableStateOf("") }
    var waveformAmplitudes by remember { mutableStateOf(List(32) { 0f }) }

    val sendQuestionImmediately: (String) -> Unit = { promptText ->
        if (promptText.isNotBlank()) {
            onQuestionChange(promptText)
            onAskingChange(true)
            actualScope.launch {
                withContext(Dispatchers.IO) {
                    val response = service.askQuestion(promptText)
                    onAnswerChange(response)
                }
                onAskingChange(false)
            }
        }
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp)
                .padding(12.dp)
                .verticalScroll(scrollState)
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
        if (answer.isNotBlank()) {
            val answerBlocks = buildAnswerBlocks(answer, imageBitmaps)

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

                    if (isGeneratingImage) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = imageProgressText.ifBlank { "正在生成或加载图片..." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    answerBlocks.forEach { block ->
                        when (block) {
                            is TextBlock -> {
                                Text(
                                    text = block.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            is ImageBlock -> {
                                Column(
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Image(
                                            bitmap = block.bitmap,
                                            contentDescription = block.description,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 180.dp)
                                                .aspectRatio(block.bitmap.width.toFloat() / block.bitmap.height.toFloat())
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (followUpSuggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "相关推荐",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            followUpSuggestions.forEach { suggestion ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clickable(enabled = !isAsking) { sendQuestionImmediately(suggestion) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (isSpeaking) {
                        AudioWaveform(
                            amplitudes = waveformAmplitudes,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            primaryColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 音色选择区域
                    var voiceExpanded by remember { mutableStateOf(false) }
                    val presets = remember { service.getVoicePresets() }
                    var selectedPresetName by remember {
                        mutableStateOf(
                            presets.find { it.style == service.getVoiceStyle() }?.name ?: presets.first().name
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("音色:", style = MaterialTheme.typography.bodyMedium)
                        Box {
                            OutlinedButton(onClick = { voiceExpanded = true }) {
                                Text(selectedPresetName)
                            }
                            DropdownMenu(
                                expanded = voiceExpanded,
                                onDismissRequest = { voiceExpanded = false }
                            ) {
                                presets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset.name) },
                                        onClick = {
                                            selectedPresetName = preset.name
                                            service.setVoiceStyle(preset.style)
                                            service.setVoiceSeed(preset.seed)
                                            voiceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        // 朗读按钮
                        if (isSpeaking) {
                            Button(
                                onClick = {
                                    service.stopSpeaking()
                                    service.onAmplitudeUpdate = null
                                    waveformAmplitudes = List(32) { 0f }
                                    onSpeakingChange(false)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("⏹ 停止朗读")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    if (answer.isNotBlank()) {
                                        onSpeakingChange(true)
                                        service.onAmplitudeUpdate = { amplitudes ->
                                            waveformAmplitudes = amplitudes
                                        }
                                        actualScope.launch {
                                            withContext(Dispatchers.IO) {
                                                service.speakText(answer)
                                            }
                                            service.onAmplitudeUpdate = null
                                            waveformAmplitudes = List(32) { 0f }
                                            onSpeakingChange(false)
                                        }
                                    }
                                },
                                enabled = answer.isNotBlank()
                            ) {
                                Text("🔊 朗读")
                            }
                        }

                        Button(
                            onClick = {
                                if (answer.isNotBlank()) {
                                    onGenerateImages()
                                }
                            },
                            enabled = !isGeneratingImage,
                        ) {
                            if (isGeneratingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(uiStrings.generate_image)
                            }
                        }
                    }
                }
            }

            if (imageMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = imageMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

fun loadImageBitmapFromPath(pathOrUrl: String): ImageBitmap? {
    return try {
        val stream = if (pathOrUrl.startsWith("http")) {
            URL(pathOrUrl).openStream()
        } else {
            File(pathOrUrl).inputStream()
        }
        stream.use { androidx.compose.ui.res.loadImageBitmap(it) }
    } catch (e: Exception) {
        println("⚠️ 加载图像失败: ${e.message}")
        null
    }
}

fun buildAnswerBlocks(answer: String, imageBitmaps: List<ImageBitmap>): List<AnswerBlock> {
    if (answer.isBlank()) return emptyList()

    val paragraphs = answer
        .split(Regex("\\r?\\n\\r?\\n"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val textBlocks: List<AnswerBlock> = if (paragraphs.isEmpty()) {
        listOf(TextBlock(answer))
    } else {
        paragraphs.map { TextBlock(it) }
    }

    val imageBlocks: List<AnswerBlock> = imageBitmaps.map { ImageBlock(it, generateImageDescription(it)) }

    return textBlocks + imageBlocks
}

fun calculateRequiredImageCount(answer: String): Int {
    if (answer.isBlank()) return 0

    val paragraphs = answer
        .split(Regex("\\r?\\n\\r?\\n"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val keywordMap = mapOf(
        "牛肉面" to true, "手抓羊肉" to true, "黄河" to true, "水车" to true,
        "拉面" to true, "羊肉" to true, "夜市" to true, "白塔山" to true,
        "中山桥" to true, "五泉山" to true, "甘肃省博物馆" to true,
        "兰州大学" to true, "西关" to true, "张掖路步行街" to true,
        "甜品" to true, "烤肉" to true, "酿皮" to true, "灰豆子" to true,
        "浆水面" to true, "炒面片" to true, "丹霞" to true, "敦煌" to true,
        "兰山" to true, "铜奔马" to true, "黄河母亲" to true,
        "羊皮筏子" to true, "百合" to true, "玫瑰" to true,
        "刻葫芦" to true, "三炮台" to true, "青城古镇" to true,
        "河口古镇" to true, "兴隆山" to true, "什川" to true,
        "黄河石林" to true, "甘南" to true, "拉卜楞寺" to true
    )

    var keywordCount = 0
    for (paragraph in paragraphs) {
        for ((keyword, _) in keywordMap) {
            if (paragraph.contains(keyword)) {
                keywordCount++
                break
            }
        }
    }

    // 返回关键词段落数量，最多不超过2张图片，最少为1张
    return maxOf(1, minOf(keywordCount, paragraphs.size, 2))
}

fun buildFollowUpSuggestions(answer: String): List<String> {
    if (answer.isBlank()) return emptyList()

    val keywordPrompts = mapOf(
        "牛肉面" to listOf("兰州牛肉面哪家最正宗？", "兰州牛肉面的历史有多久？"),
        "手抓羊肉" to listOf("手抓羊肉哪里好吃？", "手抓羊肉的正确吃法？"),
        "黄河" to listOf("黄河风情线有多长？", "黄河游船在哪里坐？"),
        "水车" to listOf("兰州水车的历史是什么？", "水车博览园怎么样？"),
        "拉面" to listOf("兰州拉面和牛肉面有什么区别？", "拉面师傅是怎么培训的？"),
        "夜市" to listOf("兰州夜市推荐有哪些？", "正宁路夜市必吃的小吃？"),
        "酿皮" to listOf("兰州酿皮和凉面有什么区别？", "酿皮哪家好吃？"),
        "灰豆子" to listOf("兰州灰豆子是什么？", "灰豆子怎么做的？"),
        "白塔山" to listOf("白塔山有什么传说？", "白塔山怎么上去？"),
        "中山桥" to listOf("黄河铁桥的历史？", "中山桥夜景怎么样？"),
        "博物馆" to listOf("甘肃省博物馆有什么镇馆之宝？", "兰州有什么博物馆值得参观？"),
        "兰山" to listOf("兰山公园怎么去？", "兰山夜景好看吗？"),
        "丹霞" to listOf("兰州到张掖丹霞怎么去？", "丹霞地貌什么时候去最美？"),
        "敦煌" to listOf("兰州到敦煌怎么去？", "河西走廊怎么安排行程？"),
        "甘南" to listOf("兰州到甘南怎么去？", "甘南有什么好玩的？"),
        "三炮台" to listOf("三炮台茶是什么？", "兰州哪里喝三炮台？"),
        "百合" to listOf("兰州百合有什么特点？", "百合在哪里买正宗？"),
        "温泉" to listOf("兰州有什么温泉？", "冬天兰州有什么玩的？")
    )

    val suggestions = mutableListOf<String>()
    for ((keyword, prompts) in keywordPrompts) {
        if (answer.contains(keyword, ignoreCase = true)) {
            suggestions += prompts
        }
    }

    if (suggestions.isEmpty()) {
        suggestions += listOf(
            "兰州有什么免费景点？",
            "兰州美食推荐",
            "兰州两日游怎么安排？",
            "兰州有什么特产可以带回家？",
            "兰州的最佳旅游时间？"
        )
    }

    return suggestions.distinct().take(5)
}

fun generateImageDescription(bitmap: ImageBitmap): String {
    // 根据图片尺寸生成通用描述
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    return when {
        aspectRatio > 1.5 -> "横版风景图片"
        aspectRatio < 0.7 -> "竖版美食图片"
        else -> "方形场景图片"
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

        // 数字卡片行
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = currentStats["totalItems"]?.toString() ?: "0",
                label = uiStrings.total_items,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary
            )
            StatCard(
                value = currentStats["categories"]?.toString() ?: "0",
                label = uiStrings.categories,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.tertiary
            )
            StatCard(
                value = if (currentStats["source"] == 1) "DB" else "JSON",
                label = uiStrings.data_source,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // 分类数据
        val categoryStats = currentStats.filter { it.key.startsWith("category_") }
            .map { (key, value) -> key.removePrefix("category_") to value }
            .sortedByDescending { it.second }

        if (categoryStats.isNotEmpty()) {
            // 环形图
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "分类占比",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DonutChart(
                        data = categoryStats,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 柱状图
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "分类详情",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalBarChart(
                        data = categoryStats,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 数据库统计
        if (currentStats["source"] == 1) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "数据库统计",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatsRow(uiStrings.db_qa_pairs, currentStats["db_qa_pairs"]?.toString() ?: "0")
                    StatsRow(uiStrings.db_chat_history, currentStats["db_chat_history"]?.toString() ?: "0")
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

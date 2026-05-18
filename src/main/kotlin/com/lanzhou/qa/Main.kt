package com.lanzhou.qa

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.lanzhou.qa.config.User
import com.lanzhou.qa.config.UserRole
import com.lanzhou.qa.service.QAService
import com.lanzhou.qa.ui.AudioWaveform
import com.lanzhou.qa.ui.LoginScreen
import com.lanzhou.qa.ui.DonutChart
import com.lanzhou.qa.ui.HorizontalBarChart
import com.lanzhou.qa.ui.StatCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 国潮古风配色 - 蓝黄系列（与登录页一致）
private val BlueMain = Color(0xFF0D47A1)
private val BlueBright = Color(0xFF1565C0)
private val BlueLight = Color(0xFF42A5F5)
private val GoldMain = Color(0xFFFFC107)
private val GoldLight = Color(0xFFFFD54F)
private val CreamWhite = Color(0xFFFFF8E1)
private val DarkBg = Color(0xCC0A1628)

// 加载卡片图片（从 resources 目录），失败时返回 null
private fun loadCardImage(path: String): ImageBitmap? {
    return try {
        val stream = Thread.currentThread().contextClassLoader?.getResourceAsStream(path) ?: return null
        stream.use { loadImageBitmap(it) }
    } catch (_: Exception) { null }
}

// 卡片图片数据：资源路径 -> 备用渐变色
data class CardImage(val resourcePath: String, val fallbackTop: Color, val fallbackBottom: Color)

private val cardImages = mapOf(
    // 景点
    "白塔山公园" to CardImage("cards/baitashan.jpg", Color(0xFF1A5276), Color(0xFF2E86C1)),
    "中山桥" to CardImage("cards/zhongshanqiao.jpg", Color(0xFF6C3483), Color(0xFFAF7AC5)),
    "五泉山公园" to CardImage("cards/wuquanshan.jpg", Color(0xFF1E8449), Color(0xFF52BE80)),
    "甘肃省博物馆" to CardImage("cards/bowuguan.jpg", Color(0xFF935116), Color(0xFFE67E22)),
    "水车博览园" to CardImage("cards/shuiche.jpg", Color(0xFF1A5276), Color(0xFF5DADE2)),
    "黄河母亲雕塑" to CardImage("cards/huanghemuqin.jpg", Color(0xFF922B21), Color(0xFFEC7063)),
    "兰山公园" to CardImage("cards/lanshan.jpg", Color(0xFF196F3D), Color(0xFF58D68D)),
    "兴隆山国家级自然保护区" to CardImage("cards/xinglongshan.jpg", Color(0xFF0E6655), Color(0xFF48C9B0)),
    "什川古梨园" to CardImage("cards/shichuan.jpg", Color(0xFF7D6608), Color(0xFFF4D03F)),
    "青城古镇" to CardImage("cards/qingcheng.jpg", Color(0xFF6E2C00), Color(0xFFDC7633)),
    "吐鲁沟国家森林公园" to CardImage("cards/tulugou.jpg", Color(0xFF0B5345), Color(0xFF1ABC9C)),
    "兰州老街" to CardImage("cards/laojie.jpg", Color(0xFF784212), Color(0xFFD4A06A)),
    "黄河风情线" to CardImage("cards/huanghefengqing.jpg", Color(0xFF1B4F72), Color(0xFF5499C7)),
    // 美食
    "兰州牛肉面" to CardImage("cards/niuroumian.jpg", Color(0xFF935116), Color(0xFFF5B041)),
    "手抓羊肉" to CardImage("cards/shouzhuayangrou.jpg", Color(0xFF922B21), Color(0xFFF1948A)),
    "灰豆子" to CardImage("cards/huidouzi.jpg", Color(0xFF6C3483), Color(0xFFBB8FCE)),
    "酿皮子" to CardImage("cards/niangpizi.jpg", Color(0xFFCA6F1E), Color(0xFFF5CBA7)),
    "牛奶鸡蛋醪糟" to CardImage("cards/naunaijiu.jpg", Color(0xFFD4AC0D), Color(0xFFF9E79F)),
    "烤羊肉串" to CardImage("cards/kaoyangrouchuan.jpg", Color(0xFFA93226), Color(0xFFF1948A)),
    "浆水面" to CardImage("cards/jiangshuimian.jpg", Color(0xFF117A65), Color(0xFF76D7C4)),
    "热冬果" to CardImage("cards/redongguo.jpg", Color(0xFF884EA0), Color(0xFFD2B4DE)),
    "甜醅子" to CardImage("cards/tianpeizi.jpg", Color(0xFFB7950B), Color(0xFFF7DC6F)),
    "三泡台盖碗茶" to CardImage("cards/sanpaotai.jpg", Color(0xFF1A5276), Color(0xFF85C1E9)),
    "黄河啤酒" to CardImage("cards/huanghepijiu.jpg", Color(0xFF196F3D), Color(0xFF82E0AA)),
    // 攻略
    "黄河风情线一日游" to CardImage("cards/yiriyou.jpg", Color(0xFF1B4F72), Color(0xFF5499C7)),
    "五泉山探秘之旅" to CardImage("cards/wuquanshan2.jpg", Color(0xFF1E8449), Color(0xFF52BE80)),
    "白塔山登高望远" to CardImage("cards/baitashan2.jpg", Color(0xFF1A5276), Color(0xFF2E86C1)),
    "兰州老街文化之旅" to CardImage("cards/laojie2.jpg", Color(0xFF784212), Color(0xFFD4A06A)),
    "什川古梨园赏花" to CardImage("cards/shichuan2.jpg", Color(0xFF7D6608), Color(0xFFF4D03F)),
    "青城古镇深度游" to CardImage("cards/qingcheng2.jpg", Color(0xFF6E2C00), Color(0xFFDC7633)),
    "兴隆山森林之旅" to CardImage("cards/xinglongshan2.jpg", Color(0xFF0E6655), Color(0xFF48C9B0)),
    "兰州三日游攻略" to CardImage("cards/sanriyou.jpg", Color(0xFF6C3483), Color(0xFFAF7AC5)),
    "吐鲁沟探险之旅" to CardImage("cards/tulugou2.jpg", Color(0xFF0B5345), Color(0xFF1ABC9C)),
    "甘肃省博物馆文化之旅" to CardImage("cards/bowuguan2.jpg", Color(0xFF935116), Color(0xFFE67E22)),
    // 出行
    "飞机出行" to CardImage("cards/feiji.jpg", Color(0xFF1A5276), Color(0xFF5DADE2)),
    "高铁出行" to CardImage("cards/gaotie.jpg", Color(0xFF2E4053), Color(0xFF85929E)),
    "市内公交" to CardImage("cards/gongjiao.jpg", Color(0xFF117A65), Color(0xFF76D7C4)),
    "出租车/网约车" to CardImage("cards/dache.jpg", Color(0xFF7D6608), Color(0xFFF4D03F)),
    "最佳旅游季节" to CardImage("cards/jijie.jpg", Color(0xFF196F3D), Color(0xFF82E0AA)),
    "气候特点" to CardImage("cards/qihou.jpg", Color(0xFF2471A3), Color(0xFF85C1E9)),
    "穿衣建议" to CardImage("cards/chuanyi.jpg", Color(0xFF884EA0), Color(0xFFD2B4DE)),
    "自驾路线" to CardImage("cards/zijia.jpg", Color(0xFF935116), Color(0xFFE67E22)),
    "住宿推荐区域" to CardImage("cards/zhusu.jpg", Color(0xFF6C3483), Color(0xFFBB8FCE)),
    "安全与健康" to CardImage("cards/anquan.jpg", Color(0xFF922B21), Color(0xFFEC7063))
)

// 自定义 Material3 配色方案（蓝金系列，替换默认紫色）
private val AppColorScheme = darkColorScheme(
    primary = GoldLight,
    onPrimary = BlueMain,
    primaryContainer = Color(0x331565C0),
    onPrimaryContainer = GoldLight,
    secondary = BlueLight,
    onSecondary = CreamWhite,
    secondaryContainer = Color(0x2242A5F5),
    onSecondaryContainer = CreamWhite,
    tertiary = GoldMain,
    onTertiary = BlueMain,
    error = Color(0xFFFF6B6B),
    onError = CreamWhite,
    errorContainer = Color(0x33FF6B6B),
    onErrorContainer = Color(0xFFFF6B6B),
    background = Color(0xFF1A2744),
    onBackground = CreamWhite,
    surface = Color(0xFF1E2D4A),
    onSurface = CreamWhite,
    surfaceVariant = Color(0x22FFFFFF),
    onSurfaceVariant = CreamWhite.copy(alpha = 0.8f),
    outline = GoldMain.copy(alpha = 0.5f),
    outlineVariant = BlueLight.copy(alpha = 0.3f)
)


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
    var currentUser by remember { mutableStateOf<User?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    // 根据角色校正 tab，防止停留在无权限页面
    val validTab = remember(selectedTab, currentUser?.role) {
        val role = currentUser?.role ?: UserRole.TOURIST
        when {
            selectedTab < 0 -> 0
            selectedTab <= 7 -> selectedTab // 所有用户可见
            selectedTab in 8..9 && (role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN) -> selectedTab
            selectedTab == 10 && role == UserRole.SUPER_ADMIN -> selectedTab
            else -> 0
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            service = QAService()
            stats = service!!.getStats()
            isInitialized = true
        }
    }

    MaterialTheme(colorScheme = AppColorScheme) {
        if (!isInitialized) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (currentUser == null) {
            val dbManager = service!!.getDatabaseManager()
            if (dbManager != null) {
                LoginScreen(
                    databaseManager = dbManager,
                    onLoginSuccess = { user -> currentUser = user }
                )
            } else {
                currentUser = User(id = 0, username = "guest", role = UserRole.TOURIST)
            }
        } else {
            // 纯蓝色渐变背景 + 侧边栏布局
            Row(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1A2744))
            ) {
                // 左侧边栏
                SideBar(
                    currentUser = currentUser!!,
                    currentLanguage = currentLanguage,
                    selectedTab = validTab,
                    onTabChange = { selectedTab = it },
                    onLanguageChange = { newLanguage -> currentLanguage = newLanguage },
                    onLogout = { currentUser = null; selectedTab = 0 }
                )
                // 右侧内容
                MainContent(service!!, stats, currentLanguage, currentUser!!, validTab)
            }
        }
    }
}

@Composable
fun SideBar(currentUser: User, currentLanguage: String, selectedTab: Int, onTabChange: (Int) -> Unit, onLanguageChange: (String) -> Unit, onLogout: () -> Unit) {
    val uiStrings = LanguageManager.getUIStrings()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F1B30), Color(0xFF141E33), Color(0xFF0F1B30))
                )
            )
            .padding(vertical = 16.dp, horizontal = 12.dp)
    ) {
        // 系统标题
        Text(
            text = "✿",
            color = GoldMain,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "兰州旅游问答系统",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight, letterSpacing = 3.sp),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = uiStrings.subtitle,
            style = TextStyle(fontSize = 10.sp, color = GoldMain.copy(alpha = 0.6f), letterSpacing = 1.sp),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 分割线
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(colors = listOf(Color.Transparent, GoldMain.copy(alpha = 0.4f), Color.Transparent)))
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 导航项
        val navItems = mutableListOf(
            NavItem("❓ ${uiStrings.question}", 0),
            NavItem("📚 ${uiStrings.knowledge_base}", 1),
            NavItem("🗺️ 旅游攻略", 2),
            NavItem("🏔️ 景点大全", 3),
            NavItem("🍜 兰州美食", 4),
            NavItem("🚌 出行指南", 5),
            NavItem("💬 ${uiStrings.chat_history}", 6),
            NavItem("📊 ${uiStrings.stats}", 7),
        )
        if (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.SUPER_ADMIN) {
            navItems.add(NavItem("✏️ 知识库管理", 8))
            navItems.add(NavItem("👥 用户管理", 9))
        }
        if (currentUser.role == UserRole.SUPER_ADMIN) {
            navItems.add(NavItem("⚙️ 系统配置", 10))
        }

        navItems.forEach { item ->
            val isSelected = selectedTab == item.index
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0x3342A5F5) else Color.Transparent)
                    .clickable { onTabChange(item.index) }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = item.label,
                    color = if (isSelected) GoldLight else CreamWhite.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // 分割线
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(colors = listOf(Color.Transparent, GoldMain.copy(alpha = 0.4f), Color.Transparent)))
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 用户信息
        Text(
            text = "👤 ${currentUser.username}",
            color = CreamWhite,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = currentUser.role.label,
            color = GoldMain.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 语言选择器
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldMain),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("🌐 ${LanguageManager.getLanguageName(currentLanguage)}", fontSize = 11.sp, color = GoldMain)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xDD0D1F3C))
            ) {
                LanguageManager.getAvailableLanguages().forEach { code ->
                    DropdownMenuItem(
                        text = { Text(LanguageManager.getLanguageName(code), color = CreamWhite, fontSize = 12.sp) },
                        onClick = {
                            LanguageManager.setCurrentLanguage(code)
                            onLanguageChange(code)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 退出按钮
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFF6B6B),
                containerColor = Color(0x22FF6B6B)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("退出登录", fontSize = 12.sp, color = Color(0xFFFF6B6B))
        }
    }
}

data class NavItem(val label: String, val index: Int)

@Composable
fun MainContent(service: QAService, stats: Map<String, Int>, currentLanguage: String, currentUser: User, selectedTab: Int) {
    val uiStrings = LanguageManager.getUIStrings()

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
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 统计信息面板
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x22FFFFFF))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${uiStrings.data_source}: $sourceText", color = CreamWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                Text("${uiStrings.total_items}: ${stats["totalItems"]} ${uiStrings.items}", color = CreamWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                Text("${uiStrings.categories}: ${stats["categories"]}", color = CreamWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                if (currentSource == 1) {
                    Text("${uiStrings.db_qa}: ${stats["db_qa_pairs"] ?: 0} ${uiStrings.items}", color = CreamWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    Text("${uiStrings.history}: ${stats["db_chat_history"] ?: 0} ${uiStrings.items}", color = CreamWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (reloadMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reloadMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (reloadMessage.contains("✅")) GoldLight else Color(0xFFFF6B6B)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

    val onQuestionChange = remember { { newQuestion: String -> question = newQuestion } }
    val onAnswerChange = remember { { newAnswer: String -> answer = newAnswer } }
    val onAskingChange = remember { { newAsking: Boolean -> isAsking = newAsking } }

    // 选项卡内容 - weight(1f) 填满剩余空间
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
    when (selectedTab) {
        0 -> QATab(
            service = service,
            currentUser = currentUser,
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
        1 -> KnowledgeTab(service, currentLanguage, currentUser)
        2 -> TravelGuideTab(service)
        3 -> AttractionsTab(service)
        4 -> FoodTab(service)
        5 -> TravelTipsTab(service)
        6 -> ChatHistoryTab(service, currentLanguage)
        7 -> StatsTab(service, stats, currentLanguage)
        8 -> KnowledgeManagementTab(service, currentUser)
        9 -> UserManagementTab(service, currentUser)
        10 -> SystemConfigTab(service, currentSource) { newSource -> currentSource = newSource }
    }
    }
    }
}

/**
 * 问答选项卡
 */
@Composable
fun QATab(
    service: QAService,
    currentUser: User,
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
        // 角色欢迎提示
        val roleWelcome = when (currentUser.role) {
            UserRole.TOURIST -> "欢迎使用兰州旅游问答系统，您可以浏览知识库和提问"
            UserRole.ADMIN -> "管理员 ${currentUser.username}，您可以管理知识库和普通用户"
            UserRole.SUPER_ADMIN -> "超级管理员 ${currentUser.username}，您拥有系统全部权限"
        }
        Text(
            text = roleWelcome,
            color = GoldMain.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 快捷提问卡片
        val quickTopics = listOf(
            Triple("🏔️", "兰州景点", "兰州有哪些著名景点推荐？"),
            Triple("🍜", "兰州美食", "兰州有什么特色美食？"),
            Triple("🌤️", "兰州天气", "兰州的气候怎么样？最佳旅游时间是什么时候？"),
            Triple("🗺️", "兰州路线", "如何规划兰州三日游路线？"),
            Triple("🏨", "住宿交通", "兰州有哪些推荐的住宿和交通方式？"),
            Triple("🎭", "兰州文化", "兰州有哪些民俗文化和特色节庆活动？"),
            Triple("🛒", "兰州特产", "兰州有什么特产可以带回家？"),
            Triple("📍", "周边游玩", "兰州周边有什么值得一去的地方？")
        )

        Text(
            text = "快捷提问",
            color = GoldLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // 两列网格
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickTopics.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowItems.forEach { (icon, title, query) ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isAsking) {
                                    onQuestionChange(query)
                                    sendQuestionImmediately(query)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0x15FFFFFF)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "$icon $title",
                                    color = GoldLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = query,
                                    color = CreamWhite.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    // 补齐空位
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

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
fun KnowledgeTab(service: QAService, currentLanguage: String, currentUser: User) {
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LanguageManager.formatString(uiStrings.total_records, "count" to filteredKnowledge.size),
                    style = MaterialTheme.typography.bodySmall
                )
                if (currentUser.role == UserRole.TOURIST) {
                    Text(
                        text = "📖 只读模式",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldMain.copy(alpha = 0.6f)
                    )
                }
            }

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

/**
 * 旅游攻略 - 独立展示页面
 */
@Composable
fun TravelGuideTab(service: QAService) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("全部") }
    var selectedItem by remember { mutableStateOf<Map<String, String>?>(null) }

    val allGuides = listOf(
        mapOf("name" to "黄河风情线一日游", "type" to "一日游", "desc" to "从中山桥出发，沿黄河风情线漫步，途经水车博览园、黄河母亲雕塑、绿色希望雕塑。全长约10公里，可步行或骑行。沿途可观赏黄河两岸风光，感受兰州黄河文化的独特魅力。傍晚时分夕阳映照黄河，别有一番风味。", "tips" to "建议下午3点出发，走到水车博览园时正好日落。全程约3小时。"),
        mapOf("name" to "五泉山探秘之旅", "type" to "文化游", "desc" to "五泉山因五眼清泉得名，是兰州佛教文化圣地。山上崇庆寺、嘛呢寺、地藏寺等古建筑群错落有致。登顶可俯瞰兰州全城，山下有兰州动物园适合亲子游。春季花开、秋季红叶时节最美。", "tips" to "建议上午前往，光线适合拍照。园内有索道可直达山顶。"),
        mapOf("name" to "白塔山登高望远", "type" to "登山游", "desc" to "白塔山海拔约1700米，山顶白塔寺始建于元代。登山步道约20分钟可达山顶，沿途可欣赏古建筑群。山顶是观赏黄河铁桥和兰州城区全景的最佳位置。白塔通高17米，造型别致，是兰州的标志性建筑。", "tips" to "推荐傍晚登山，可以同时欣赏日落和城市灯光。山脚下有中山桥美食街。"),
        mapOf("name" to "兰州老街文化之旅", "type" to "文化游", "desc" to "兰州老街是仿古商业街，采用明清建筑风格。汇集兰州特色小吃（灰豆子、酿皮子、甜醅子）、非遗手工艺品（黄河石、剪纸）、传统文化体验（秦腔表演、皮影戏）。夜晚灯火辉煌，是最能感受老兰州烟火气息的地方。", "tips" to "周末晚上有民俗表演，小吃推荐灰豆子和酿皮子。"),
        mapOf("name" to "什川古梨园赏花", "type" to "生态游", "desc" to "什川古梨园有千年古梨树9000余株，其中200余株树龄超400年。每年4月中旬梨花盛开，漫山遍野如雪海，是兰州春季最壮观的自然景观。秋季（9-10月）硕果累累，可体验采摘乐趣。被认定为'中国重要农业文化遗产'。", "tips" to "4月中旬梨花节期间人多，建议提前预约农家乐。距市区约30公里。"),
        mapOf("name" to "青城古镇深度游", "type" to "古镇游", "desc" to "青城古镇有2000多年历史，保存完好的明清古民居60多处。高家祠堂、青城书院、城隍庙等古建筑见证了古镇的辉煌。手工陈醋酿造技艺是省级非遗，长面是当地特色主食。古镇民风淳朴，是体验西北慢生活的好去处。", "tips" to "建议住一晚，清晨的古镇最有味道。距市区约100公里，自驾约2小时。"),
        mapOf("name" to "兴隆山森林之旅", "type" to "自然游", "desc" to "兴隆山被誉为'陇右名山'，海拔约3000米，森林覆盖率超80%。山中云龙桥、卧桥等景点古朴典雅，夏季绿树成荫、溪水潺潺，是兰州人首选的避暑胜地。山上有成吉思汗纪念馆，冬季雾凇雪景也别具特色。", "tips" to "夏季最佳，全程约4-5小时。距市区45公里，建议自驾。"),
        mapOf("name" to "兰州三日游攻略", "type" to "综合游", "desc" to "Day1：中山桥→白塔山→黄河风情线→兰州老街（感受黄河文化和城市夜景）；Day2：五泉山→甘肃省博物馆→正宁路夜市（了解历史文化和品尝美食）；Day3：什川古梨园或兴隆山（亲近自然）。这条路线涵盖了兰州的自然、人文、美食精华。", "tips" to "三天预算约500-800元（不含住宿）。建议住在城关区，交通便利。"),
        mapOf("name" to "吐鲁沟探险之旅", "type" to "探险游", "desc" to "吐鲁沟地处祁连山东麓，总面积5848公顷。沟内奇峰林立、怪石嶙峋、林木茂密、溪流清澈，有天窗眼、藏龙卧虎石、驼峰岭等自然奇观。森林覆盖率达79.2%，是西北罕见的原始森林景观。适合户外徒步和摄影爱好者。", "tips" to "全程徒步约5-6小时，建议穿防滑鞋。距市区150公里，建议包车。"),
        mapOf("name" to "甘肃省博物馆文化之旅", "type" to "文化游", "desc" to "甘肃省博物馆是国家一级博物馆，馆藏文物约35万件。必看：马踏飞燕铜奔马（中国旅游标志）、铜奔马仪仗队、彩绘木独角兽、人形彩陶罐。丝绸之路展厅展示了两千多年前东西方文明交流的辉煌。彩陶展厅收藏了马家窑文化、齐家文化的精美彩陶。", "tips" to "建议提前3天预约，每天限流3000人。语音导览20元，参观约需3小时。")
    )

    val types = listOf("全部") + allGuides.map { it["type"]!! }.distinct()
    val filtered = allGuides.filter { item ->
        (selectedType == "全部" || item["type"] == selectedType) &&
        (searchQuery.isBlank() || item["name"]!!.contains(searchQuery, ignoreCase = true))
    }

    if (selectedItem != null) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GoldMain).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedItem!!["name"]!!, color = BlueMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("📂 ${selectedItem!!["type"]}", color = BlueMain.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("攻略类型", color = GoldMain, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                        Text(selectedItem!!["type"] ?: "", fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("攻略详情", color = GoldMain, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(selectedItem!!["desc"] ?: "", fontSize = 13.sp, lineHeight = 20.sp)
                    if (selectedItem!!["tips"] != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("贴士", color = GoldMain, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(selectedItem!!["tips"] ?: "", fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { selectedItem = null }) { Text("返回列表", fontSize = 12.sp) }
        }
    } else {
        // 列表页
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text("🗺️ 旅游攻略", style = MaterialTheme.typography.titleMedium, color = GoldLight, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // 搜索栏
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    label = { Text("攻略名称", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = GoldMain, contentColor = BlueMain)) {
                    Text("🔍 查询", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            // 分类筛选
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                types.forEach { type ->
                    OutlinedButton(
                        onClick = { selectedType = type },
                        colors = if (selectedType == type) ButtonDefaults.outlinedButtonColors(containerColor = GoldMain, contentColor = BlueMain)
                                 else ButtonDefaults.outlinedButtonColors(contentColor = CreamWhite),
                        modifier = Modifier.height(36.dp)
                    ) { Text(type, fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))

            // 卡片网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered.size) { index ->
                    val item = filtered[index]
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedItem = item },
                        colors = CardDefaults.cardColors(containerColor = Color(0x15FFFFFF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            val imgName = item["name"] ?: ""
                            val ci = cardImages[imgName]
                            val img = if (ci != null) loadCardImage(ci.resourcePath) else null
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(
                                Brush.verticalGradient(listOf(
                                    ci?.fallbackTop ?: Color(0x330D47A1),
                                    ci?.fallbackBottom ?: Color(0x330D47A1)
                                ))
                            )) {
                                if (img != null) {
                                    Image(bitmap = img, contentDescription = imgName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item["name"] ?: "", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CreamWhite, maxLines = 1)
                                Text(item["type"] ?: "", fontSize = 12.sp, color = GoldMain.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 景点大全 - 独立展示页面
 */
@Composable
fun AttractionsTab(service: QAService) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("全部") }
    var selectedItem by remember { mutableStateOf<Map<String, String>?>(null) }

    val allAttractions = listOf(
        mapOf("name" to "白塔山公园", "type" to "山", "price" to "免费", "hours" to "全天开放", "address" to "兰州市城关区白塔山1号", "history" to "白塔山因山顶有白塔寺而得名，白塔始建于元代（1391年），由明代重修。山上有三星殿、法雨寺、白塔寺等古建筑群，共三十余座殿宇。白塔通高约17米，为八角七级楼阁式实心砖塔，外敷白灰，塔顶为绿色琉璃宝瓶，造型别致。登至山顶可远眺黄河铁桥和兰州城区全景，是兰州标志性景点之一。", "activity" to "登山远眺、参观白塔寺古建筑群、俯瞰黄河铁桥全景、摄影留念", "tips" to "建议傍晚前往，夕阳下的黄河景色绝美。山脚下有小吃摊，可以品尝兰州特色小吃。", "icon" to "🏔️"),
        mapOf("name" to "中山桥", "type" to "古城", "price" to "免费", "hours" to "全天开放", "address" to "兰州市城关区南滨河东路", "history" to "中山桥建于1907年（清光绪三十三年），1909年竣工通车，是黄河上第一座近代桥梁，由德国泰来洋行承建，名为'黄河铁桥'。1942年为纪念孙中山先生改名'中山桥'。桥长234米，宽7.5米，共5孔。桥上建有铁桥纪念塔，2004年改为步行桥，成为兰州最著名的地标建筑。2006年列为全国重点文物保护单位。", "activity" to "漫步铁桥、观赏黄河夜景（灯光秀）、参观铁桥纪念塔、拍摄城市风光", "tips" to "夜晚的中山桥灯光璀璨，是兰州最佳夜景观赏地。桥两头有铁索铁桥可拍照。", "icon" to "🌉"),
        mapOf("name" to "五泉山公园", "type" to "公园", "price" to "免费", "hours" to "06:00-20:00", "address" to "兰州市城关区五泉南路", "history" to "五泉山因山上有五眼清泉而得名，五泉分别为甘露泉、掬月泉、摸子泉、惠泉、蒙泉。传说汉武帝时霍去病西征驻军此地，士兵口渴无水，将军以鞭击地涌出五泉。始建于西汉，山上现有崇庆寺、千佛阁、嘛呢寺、地藏寺等古建筑群，是兰州佛教文化圣地。", "activity" to "观赏古建筑群、登高望远、品泉水、参观兰州动物园", "tips" to "公园面积较大，建议预留2-3小时。春季花开时节最美。", "icon" to "⛲"),
        mapOf("name" to "甘肃省博物馆", "type" to "博物馆", "price" to "免费（需预约）", "hours" to "09:00-17:00（16:00停止入馆，周一闭馆）", "address" to "兰州市七里河区西津西路3号", "history" to "甘肃省博物馆始建于1939年，是国家一级博物馆。馆藏文物约35万件，以新石器时代彩陶、汉简、丝绸之路文物为特色。镇馆之宝为'马踏飞燕'铜奔马（东汉），是中国旅游标志。此外还有铜奔马仪仗队、彩绘木独角兽、人形彩陶罐等国宝级文物。", "activity" to "参观丝绸之路展厅、彩陶展厅、马踏飞燕铜奔马、佛教艺术展", "tips" to "建议提前3天预约，每天限流3000人。建议租借语音导览器，参观约需3小时。", "icon" to "🏛️"),
        mapOf("name" to "水车博览园", "type" to "公园", "price" to "10元", "hours" to "08:00-18:00", "address" to "兰州市城关区南滨河东路", "history" to "兰州水车起源于明嘉靖年间（1556年），由兰州人段续创造。博览园内展示多架巨型兰州水车，其中最大的直径达16米，利用黄河水流冲击水轮旋转提水灌溉。2005年建成开放，占地约400亩，展示了兰州黄河灌溉技术的发展历程。水车是兰州的城市标志之一，也是兰州的象征。", "activity" to "观看巨型水车运转、体验黄河灌溉文化、参观水车历史文化展、游览黄河风情线", "tips" to "园内可以近距离观看水车提水过程，夏天有喷雾降温区域。", "icon" to "🎡"),
        mapOf("name" to "黄河母亲雕塑", "type" to "雕塑", "price" to "免费", "hours" to "全天开放", "address" to "兰州市城关区南滨河中路", "history" to "黄河母亲雕塑由雕塑家何鄂于1986年创作，1987年落成。雕塑全长6米，高2.6米，由花岗岩雕成。作品展现了一位母亲侧卧怀抱婴儿的形象，象征中华民族如黄河般生生不息、源远流长。1997年被列为全国重点保护文物，是兰州最具代表性的城市雕塑和城市名片。", "activity" to "观赏雕塑艺术、沿黄河风情线散步、欣赏黄河落日", "tips" to "傍晚时分拍照效果最佳，可以与雕塑合影留念。", "icon" to "🗿"),
        mapOf("name" to "兰山公园", "type" to "公园", "price" to "免费", "hours" to "全天开放（缆车08:00-17:30）", "address" to "兰州市城关区兰山", "history" to "兰山位于兰州南郊皋兰山上，海拔约2129米，是俯瞰兰州全城的最佳位置。山上建有兰山公园，拥有索道缆车、山顶观景台、农家乐等设施。夜晚可观赏兰州万家灯火的城市夜景，被誉为'兰州的观景台'。", "activity" to "登高俯瞰兰州全景、乘坐缆车、观赏城市夜景、品尝山顶农家乐", "tips" to "推荐下午4点上山，等到日落时分欣赏城市灯光渐起的美景。有索道可直达山顶。", "icon" to "🌳"),
        mapOf("name" to "兴隆山国家级自然保护区", "type" to "自然", "price" to "40元", "hours" to "08:00-18:00", "address" to "兰州市榆中县", "history" to "兴隆山被誉为'陇右名山'，海拔约3000米，森林覆盖率高达80%以上。山中古木参天，溪流潺潺，有云龙桥、卧桥、大峡等自然景观。元代成吉思汗曾在此驻军，山上有成吉思汗纪念馆。兴隆山也是兰州人夏季避暑胜地，冬天可赏雾凇雪景。", "activity" to "森林徒步、避暑纳凉、参观成吉思汗纪念馆、云龙桥观光", "tips" to "夏季是最佳游览时间，注意保暖。建议带登山装备，全程约需4-5小时。距市区约45公里。", "icon" to "🌲"),
        mapOf("name" to "什川古梨园", "type" to "自然", "price" to "免费", "hours" to "全天开放", "address" to "兰州市皋兰县什川镇", "history" to "什川古梨园被誉为'世界第一古梨园'，园内有百年以上古梨树9000余株，其中树龄超过400年的有200余株。最古老的梨树'梨树王'树龄已达440余年，至今仍然硕果累累。春季（4月中旬）梨花盛开如雪海，秋季（9-10月）硕果累累，是兰州独特的生态奇观。2014年被农业部认定为'中国重要农业文化遗产'。", "activity" to "春季赏梨花、秋季采摘果实、体验农家乐、品尝鲜梨和梨膏", "tips" to "4月中旬梨花节期间人多，建议提前预约农家乐。距市区约30公里，自驾约40分钟。", "icon" to "🍐"),
        mapOf("name" to "青城古镇", "type" to "古城", "price" to "免费", "hours" to "全天开放", "address" to "兰州市榆中县青城镇", "history" to "青城古镇始建于汉代，有2000多年历史，是古丝绸之路上的重镇。镇内保存完好的明清古民居有60多处，还有高家祠堂、青城书院、城隍庙等古建筑。青城以手工陈醋和长面闻名，青城陈醋酿造技艺是甘肃省非物质文化遗产。古镇民风淳朴，是体验西北古镇文化的好去处。", "activity" to "参观明清古民居、游览高家祠堂和青城书院、品尝长面和陈醋、体验古镇慢生活", "tips" to "建议住一晚，清晨的古镇最有味道。距市区约100公里，自驾约2小时。", "icon" to "🏯"),
        mapOf("name" to "吐鲁沟国家森林公园", "type" to "自然", "price" to "50元", "hours" to "08:00-17:30", "address" to "兰州市永登县连城林区", "history" to "吐鲁沟地处祁连山东麓，总面积5848公顷，是西北罕见的原始森林景观。沟内奇峰林立、怪石嶙峋、林木茂密、溪流清澈，有天窗眼、藏龙卧虎石、驼峰岭等自然奇观。森林覆盖率达79.2%，是天然的森林博物馆。2000年评为国家森林公园。", "activity" to "原始森林徒步、观赏奇峰怪石、森林探险、摄影采风", "tips" to "全程徒步约需5-6小时，建议穿防滑鞋。距市区约150公里，建议包车或自驾前往。", "icon" to "🏔️"),
        mapOf("name" to "兰州老街", "type" to "古城", "price" to "免费", "hours" to "全天开放（商铺10:00-22:00）", "address" to "兰州市七里河区西津西路", "history" to "兰州老街是一条仿古商业街，集中展示了兰州的历史文化和民俗风情。街区内建筑采用明清风格，青砖灰瓦，飞檐翘角。汇集了兰州特色小吃、非遗手工艺品、传统文化体验等，是感受老兰州烟火气息的好去处。", "activity" to "品尝兰州特色小吃、体验非遗文化、观赏民俗表演、购买手工纪念品", "tips" to "晚上更热闹，周末有民俗表演。小吃推荐：灰豆子、酿皮子、甜醅子。", "icon" to "🏯"),
        mapOf("name" to "黄河风情线", "type" to "自然", "price" to "免费", "hours" to "全天开放", "address" to "兰州市城关区南滨河路沿线", "history" to "黄河风情线全长约10公里，是兰州市最重要的城市景观带。沿河修建了步行道、自行车道、绿化带，串联了中山桥、黄河母亲、水车博览园、绿色希望雕塑等多个景点。是兰州市民休闲散步的主要场所，也是游客感受黄河文化的最佳路线。", "activity" to "沿河散步、骑行观光、赏黄河日落、参观沿线景点", "tips" to "建议下午4-6点前往，夕阳下的黄河非常美丽。可租用共享单车骑行。", "icon" to "🌊")
    )

    val types = listOf("全部") + allAttractions.map { it["type"]!! }.distinct()
    val filtered = allAttractions.filter { item ->
        (selectedType == "全部" || item["type"] == selectedType) &&
        (searchQuery.isBlank() || item["name"]!!.contains(searchQuery, ignoreCase = true))
    }

    if (selectedItem != null) {
        // 详情页
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GoldMain).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedItem!!["name"]!!, color = BlueMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⭐ 收藏 (0)", color = BlueMain.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text("👁 访问量: 17", color = BlueMain.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf("type" to "景点类型", "price" to "门票价格", "hours" to "开放时间", "address" to "景点地址", "history" to "历史背景", "activity" to "特色活动", "tips" to "游览贴士").forEach { (key, label) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(label, color = GoldMain, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                            Text(selectedItem!![key] ?: "", fontSize = 13.sp, lineHeight = 18.sp)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x11FFFFFF)))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { selectedItem = null }) { Text("返回列表", fontSize = 12.sp) }
        }
    } else {
        // 列表页
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text("🏔️ 景点大全", style = MaterialTheme.typography.titleMedium, color = GoldLight, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // 搜索栏
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    label = { Text("景点名称", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = GoldMain, contentColor = BlueMain)) {
                    Text("🔍 查询", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            // 景点类型筛选
            Text("景点类型:", color = CreamWhite.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                types.forEach { type ->
                    OutlinedButton(
                        onClick = { selectedType = type },
                        colors = if (selectedType == type) ButtonDefaults.outlinedButtonColors(containerColor = GoldMain, contentColor = BlueMain)
                                 else ButtonDefaults.outlinedButtonColors(contentColor = CreamWhite),
                        modifier = Modifier.height(36.dp)
                    ) { Text(type, fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))

            // 卡片网格 (4列)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered.size) { index ->
                    val item = filtered[index]
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedItem = item },
                        colors = CardDefaults.cardColors(containerColor = Color(0x15FFFFFF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            val imgName = item["name"] ?: ""
                            val ci = cardImages[imgName]
                            val img = if (ci != null) loadCardImage(ci.resourcePath) else null
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(
                                Brush.verticalGradient(listOf(
                                    ci?.fallbackTop ?: Color(0x330D47A1),
                                    ci?.fallbackBottom ?: Color(0x330D47A1)
                                ))
                            )) {
                                if (img != null) {
                                    Image(bitmap = img, contentDescription = imgName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item["name"] ?: "", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CreamWhite, maxLines = 1)
                                Text(item["type"] ?: "", fontSize = 12.sp, color = GoldMain.copy(alpha = 0.8f))
                                Spacer(Modifier.height(2.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("👁 0", fontSize = 10.sp, color = CreamWhite.copy(alpha = 0.5f))
                                    Text("⭐ 0", fontSize = 10.sp, color = CreamWhite.copy(alpha = 0.5f))
                                    Text("👍 0", fontSize = 10.sp, color = CreamWhite.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 兰州美食 - 独立展示页面
 */
@Composable
fun FoodTab(service: QAService) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    var selectedItem by remember { mutableStateOf<Map<String, String>?>(null) }

    val allFood = listOf(
        mapOf("name" to "兰州牛肉面", "category" to "面食", "location" to "全市各处", "taste" to "鲜香", "rating" to "好评", "price" to "8-15元", "desc" to "兰州牛肉面是中国十大面条之一，始于清嘉庆年间（1799年），由国子监太学生马保子创制。讲究'一清二白三红四绿五黄'：汤清（牛骨汤）、萝卜白、辣椒油红、蒜苗香菜绿、面条黄亮。面条有毛细、细、二细、三细、韭叶、宽面等12种粗细可选。最佳品尝时间是早上，当地有'早起一碗面'的习俗。推荐品牌：马子禄、磨沟沿、舌尖尖。", "icon" to "🍜"),
        mapOf("name" to "手抓羊肉", "category" to "肉类", "location" to "小西湖一带", "taste" to "鲜嫩", "rating" to "好评", "price" to "40-60元/斤", "desc" to "选用甘肃靖远滩羊，肉质鲜嫩，膻味极轻。制作方法：将羊肉冷水下锅，加姜、葱、花椒等调料煮至八成熟，捞出切块装盘。食用时配以椒盐、蒜汁、辣酱。手抓羊肉是兰州人宴请宾客的必备菜，体现了西北人豪爽的饮食文化。推荐去小西湖民族餐厅品尝。", "icon" to "🍖"),
        mapOf("name" to "灰豆子", "category" to "甜品", "location" to "各小吃摊", "taste" to "香甜", "rating" to "好评", "price" to "5-8元", "desc" to "灰豆子是兰州传统甜品，以蓬灰（草木灰碱）和麻色豆（豌豆）为主料熬制。制作时将豌豆加蓬灰水慢火熬煮至豆子绵软起沙，加入白糖调味。成品色泽暗红，口感绵软香甜，冰镇后食用更佳。夏天消暑解渴，冬天暖胃暖心。正宁路夜市的老字号灰豆子最受欢迎。", "icon" to "🥣"),
        mapOf("name" to "酿皮子", "category" to "面食", "location" to "各小吃街", "taste" to "酸辣", "rating" to "好评", "price" to "6-10元", "desc" to "酿皮子是兰州特色凉皮，用面粉浆蒸制而成。制作时将面粉加水搅拌成面浆，上笼蒸熟后切条。食用时配以辣椒油、醋、蒜汁、芝麻酱、芥末等调料，口感爽滑筋道，酸辣开胃。是夏季消暑解腻的绝佳小食。好的酿皮子薄如纸、韧如丝，入口滑嫩。", "icon" to "🥘"),
        mapOf("name" to "牛奶鸡蛋醪糟", "category" to "甜品", "location" to "正宁路夜市", "taste" to "香甜", "rating" to "好评", "price" to "10-15元", "desc" to "将醪糟（甜酒酿）与鲜牛奶同煮，打入鸡蛋花，撒上芝麻、花生碎、枸杞。口感细腻顺滑，奶香与酒香完美融合。正宁路夜市的'老马家牛奶鸡蛋醪糟'是兰州最著名的夜市小吃之一，每碗现煮现卖，排队是常态。营养丰富，老少皆宜。", "icon" to "🥛"),
        mapOf("name" to "烤羊肉串", "category" to "烧烤", "location" to "各夜市", "taste" to "香辣", "rating" to "好评", "price" to "3-5元/串", "desc" to "选用新鲜羊肉切块穿串，撒上孜然粉、辣椒面、盐等调料，在炭火上烤制。好的烤串外焦里嫩、肥瘦相间、孜然香味浓郁。兰州烤串用的是甘肃本地羊肉，肉质紧实鲜美。搭配冰镇黄河啤酒，是兰州夜生活的标配。正宁路、南关等地夜市烤串最集中。", "icon" to "🍢"),
        mapOf("name" to "浆水面", "category" to "面食", "location" to "各面馆", "taste" to "酸爽", "rating" to "好评", "price" to "10-15元", "desc" to "浆水面以浆水（发酵面汤或芹菜汤）为汤底，配以手擀面。浆水是将面粉水或芹菜水自然发酵产生的酸味液体，具有独特的清爽口感。夏天吃一碗浆水面，酸爽开胃、消暑解热，是兰州人夏天的最爱。可搭配虎皮辣子、卤肉等小菜。", "icon" to "🍝"),
        mapOf("name" to "热冬果", "category" to "甜品", "location" to "冬季各摊点", "taste" to "甜蜜", "rating" to "好评", "price" to "8-12元", "desc" to "热冬果是兰州独特的冬季甜品。将软儿梨（一种冻梨）放入热水中加热解冻，果肉变得绵软如泥，汤汁甜蜜芳香。食用时连汤带果肉一起吃，暖胃驱寒。软儿梨经过冷冻和解冻后，果肉细胞破裂释放糖分，甜度大大增加。是兰州人冬季特有的美味。", "icon" to "🍐"),
        mapOf("name" to "三泡台盖碗茶", "category" to "饮品", "location" to "各茶楼", "taste" to "甘甜", "rating" to "好评", "price" to "15-30元", "desc" to "三泡台盖碗茶由桂圆、枸杞、冰糖、红枣、菊花、茶叶等八种配料在盖碗中冲泡而成。因碗盖、碗身、碗托三件套得名'三泡台'。冲泡后茶汤金黄透亮，口感甘甜醇厚，具有滋补养生功效。是兰州人待客的最高礼遇，喝法讲究'刮'碗盖、闻香、品味。", "icon" to "🍵"),
        mapOf("name" to "甜醅子", "category" to "甜品", "location" to "各小吃摊", "taste" to "酒香", "rating" to "好评", "price" to "5-8元", "desc" to "甜醅子是兰州传统甜品，将燕麦或青稞蒸熟后加入酒曲发酵而成。成品酸甜可口，带有淡淡的酒香，口感软糯。夏天加冰水食用清凉解暑，是兰州街头最常见的消暑小吃。制作简单但发酵工艺讲究，好的甜醅子酒香浓郁、甜度适中。", "icon" to "🍶"),
        mapOf("name" to "黄河啤酒", "category" to "饮品", "location" to "各大超市", "taste" to "清爽", "rating" to "好评", "price" to "5-8元", "desc" to "黄河啤酒是兰州本地著名啤酒品牌，建于1985年。采用优质大麦和祁连山雪水酿造，口感清爽，泡沫细腻。经典款黄河干啤和黄河冰纯最受欢迎。搭配烤串和牛肉面是兰州人的经典组合。在兰州的每个餐馆和夜市都能看到它的身影。", "icon" to "🍺")
    )

    val categories = listOf("全部") + allFood.map { it["category"]!! }.distinct()
    val filtered = allFood.filter { item ->
        (selectedCategory == "全部" || item["category"] == selectedCategory) &&
        (searchQuery.isBlank() || item["name"]!!.contains(searchQuery, ignoreCase = true))
    }

    if (selectedItem != null) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GoldMain).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedItem!!["name"]!!, color = BlueMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⭐ 收藏 (0)", color = BlueMain.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text("👁 访问量: 5", color = BlueMain.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf("category" to "美食分类", "location" to "推荐地点", "taste" to "口味", "price" to "参考价格", "rating" to "评价").forEach { (key, label) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(label, color = GoldMain, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                            Text(selectedItem!![key] ?: "", fontSize = 13.sp)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x11FFFFFF)))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("详细介绍", color = GoldMain, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(selectedItem!!["desc"] ?: "", fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { selectedItem = null }) { Text("返回列表", fontSize = 12.sp) }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text("🍜 兰州美食", style = MaterialTheme.typography.titleMedium, color = GoldLight, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // 搜索栏
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    label = { Text("美食名称", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = GoldMain, contentColor = BlueMain)) {
                    Text("🔍 查询", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            // 美食分类筛选
            Text("美食分类:", color = CreamWhite.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    OutlinedButton(
                        onClick = { selectedCategory = cat },
                        colors = if (selectedCategory == cat) ButtonDefaults.outlinedButtonColors(containerColor = GoldMain, contentColor = BlueMain)
                                 else ButtonDefaults.outlinedButtonColors(contentColor = CreamWhite),
                        modifier = Modifier.height(36.dp)
                    ) { Text(cat, fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))

            // 卡片网格 (4列)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered.size) { index ->
                    val item = filtered[index]
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedItem = item },
                        colors = CardDefaults.cardColors(containerColor = Color(0x15FFFFFF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            val imgName = item["name"] ?: ""
                            val ci = cardImages[imgName]
                            val img = if (ci != null) loadCardImage(ci.resourcePath) else null
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(
                                Brush.verticalGradient(listOf(
                                    ci?.fallbackTop ?: Color(0x330D47A1),
                                    ci?.fallbackBottom ?: Color(0x330D47A1)
                                ))
                            )) {
                                if (img != null) {
                                    Image(bitmap = img, contentDescription = imgName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item["name"] ?: "", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CreamWhite, maxLines = 1)
                                Text(item["category"] ?: "", fontSize = 12.sp, color = GoldMain.copy(alpha = 0.8f))
                                Spacer(Modifier.height(2.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("👁 0", fontSize = 10.sp, color = CreamWhite.copy(alpha = 0.5f))
                                    Text("⭐ 0", fontSize = 10.sp, color = CreamWhite.copy(alpha = 0.5f))
                                    Text("👍 0", fontSize = 10.sp, color = CreamWhite.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 出行指南 - 独立展示页面
 */
@Composable
fun TravelTipsTab(service: QAService) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    var selectedItem by remember { mutableStateOf<Map<String, String>?>(null) }

    val allTips = listOf(
        mapOf("name" to "飞机出行", "category" to "交通", "desc" to "兰州中川国际机场（IAXX: LHW），距市区约70公里。机场大巴单程约30元，约1小时到达市区；城际铁路（兰中城际）约33分钟到达兰州西站，票价约20元。国内航线覆盖全国主要城市，部分国际航线可达东南亚。机场T2航站楼设施完善。", "icon" to "✈️"),
        mapOf("name" to "高铁出行", "category" to "交通", "desc" to "兰州西站是西北高铁枢纽，可达：西安（约3.5h）、成都（约6h）、重庆（约6.5h）、西宁（约1.5h）、银川（约3h）、乌鲁木齐（兰新高铁约12h）。兰州站有普速列车。建议提前15天购票，节假日需尽早预订。", "icon" to "🚄"),
        mapOf("name" to "市内公交", "category" to "交通", "desc" to "兰州公交覆盖全市，票价1-2元。主要线路：1路（兰州火车站-西固）、137路（火车站-白塔山）、149路（火车站-五泉山）。支持支付宝/微信扫码乘车。BRT快速公交在BRT1号线（银安路-西站）通行。", "icon" to "🚌"),
        mapOf("name" to "出租车/网约车", "category" to "交通", "desc" to "出租车起步价7元/3公里，之后1.6元/公里。滴滴、高德等平台均可叫车。市区打车方便，但早晚高峰可能堵车。建议从火车站/机场使用网约车前往酒店更省心。", "icon" to "🚕"),
        mapOf("name" to "最佳旅游季节", "category" to "天气", "desc" to "5-10月为最佳旅游季节。6-8月（夏季）凉爽宜人，平均气温20-25度，是西北著名的避暑胜地。9-10月（秋季）天高气爽、瓜果飘香，是品尝鲜果的好时节。4月可赏什川梨花，11月-次年3月寒冷干燥但游客少。", "icon" to "🌤️"),
        mapOf("name" to "气候特点", "category" to "天气", "desc" to "兰州属温带大陆性气候，年均气温约10度，年降水量约327mm。特点是：冬无严寒（最低约-15度）、夏无酷暑（最高约35度）、昼夜温差大（10-15度）、降水少蒸发强、日照充足（年日照约2600小时）。", "icon" to "🌡️"),
        mapOf("name" to "穿衣建议", "category" to "天气", "desc" to "夏季：短袖+薄外套（防晒+早晚温差），必备防晒霜和墨镜。春秋季：长袖+厚外套，早晚较凉。冬季：羽绒服+保暖内衣，室内有暖气。紫外线全年强烈，建议SPF50+防晒霜。", "icon" to "👕"),
        mapOf("name" to "自驾路线", "category" to "交通", "desc" to "多条高速交汇：连霍高速（东西向）、京藏高速（南北向）、兰海高速。周边自驾推荐：兰州-什川（30km，约40min）、兰州-兴隆山（45km，约50min）、兰州-青城（100km，约2h）。市区停车较方便，景点附近有停车场。", "icon" to "🚗"),
        mapOf("name" to "住宿推荐区域", "category" to "住宿", "desc" to "城关区（市中心）：交通便利，靠近中山桥、正宁路夜市，价格150-400元/晚。七里河区：靠近甘肃省博物馆、兰州西站，商务酒店较多。黄河边酒店：可赏黄河夜景，价格略高。建议住城关区，出行最方便。", "icon" to "🏨"),
        mapOf("name" to "安全与健康", "category" to "安全", "desc" to "兰州治安良好，是全国治安满意度较高的城市。注意事项：1.紫外线强烈，注意防晒。2.气候干燥，多喝水补涂润唇膏。3.海拔约1500米，一般无高原反应。4.夜间出行注意安全，正宁路夜市较安全。5.饮食偏西北口味，不习惯可提前备胃药。", "icon" to "⚠️")
    )

    val categories = listOf("全部") + allTips.map { it["category"]!! }.distinct()
    val filtered = allTips.filter { item ->
        (selectedCategory == "全部" || item["category"] == selectedCategory) &&
        (searchQuery.isBlank() || item["name"]!!.contains(searchQuery, ignoreCase = true))
    }

    if (selectedItem != null) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GoldMain).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedItem!!["name"]!!, color = BlueMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("类别", color = GoldMain, fontSize = 13.sp, modifier = Modifier.width(60.dp))
                        Text(selectedItem!!["category"] ?: "", fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("详细介绍", color = GoldMain, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(selectedItem!!["desc"] ?: "", fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { selectedItem = null }) { Text("返回列表", fontSize = 12.sp) }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text("🚌 出行指南", style = MaterialTheme.typography.titleMedium, color = GoldLight, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    label = { Text("搜索", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = GoldMain, contentColor = BlueMain)) {
                    Text("🔍 查询", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    OutlinedButton(
                        onClick = { selectedCategory = cat },
                        colors = if (selectedCategory == cat) ButtonDefaults.outlinedButtonColors(containerColor = GoldMain, contentColor = BlueMain)
                                 else ButtonDefaults.outlinedButtonColors(contentColor = CreamWhite),
                        modifier = Modifier.height(36.dp)
                    ) { Text(cat, fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered.size) { index ->
                    val item = filtered[index]
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedItem = item },
                        colors = CardDefaults.cardColors(containerColor = Color(0x15FFFFFF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            val imgName = item["name"] ?: ""
                            val ci = cardImages[imgName]
                            val img = if (ci != null) loadCardImage(ci.resourcePath) else null
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(
                                Brush.verticalGradient(listOf(
                                    ci?.fallbackTop ?: Color(0x330D47A1),
                                    ci?.fallbackBottom ?: Color(0x330D47A1)
                                ))
                            )) {
                                if (img != null) {
                                    Image(bitmap = img, contentDescription = imgName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item["name"] ?: "", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CreamWhite, maxLines = 1)
                                Text(item["category"] ?: "", fontSize = 12.sp, color = GoldMain.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 用户管理选项卡（管理员和超级管理员可用）
 * 管理员：管理普通用户（禁用/启用/重置密码/删除）
 * 超级管理员：管理所有用户（含管理员账号的创建/删除/角色修改）
 */
@Composable
fun UserManagementTab(service: QAService, currentUser: User) {
    var users by remember { mutableStateOf(emptyList<User>()) }
    var isLoading by remember { mutableStateOf(true) }
    var actionMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val dbManager = service.getDatabaseManager()
    val isSuperAdmin = currentUser.role == UserRole.SUPER_ADMIN

    // 超级管理员相关状态
    var showCreateAdmin by remember { mutableStateOf(false) }
    var newAdminName by remember { mutableStateOf("") }
    var newAdminPwd by remember { mutableStateOf("") }

    // 重置密码弹窗
    var resetPwdUser by remember { mutableStateOf<User?>(null) }
    var resetPwdValue by remember { mutableStateOf("") }

    fun reloadUsers() {
        isLoading = true
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                users = dbManager?.getAllUsers() ?: emptyList()
                isLoading = false
            }
        }
    }

    fun msg(text: String) { actionMessage = text }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            users = dbManager?.getAllUsers() ?: emptyList()
            isLoading = false
        }
    }

    // 管理员只能看普通用户，超级管理员看所有用户
    val visibleUsers = if (isSuperAdmin) users else users.filter { it.role == UserRole.TOURIST }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "共 ${visibleUsers.size} 个${if (isSuperAdmin) "" else "普通"}用户",
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isSuperAdmin) {
                    Button(onClick = { showCreateAdmin = !showCreateAdmin }) {
                        Text(if (showCreateAdmin) "收起" else "创建管理员", fontSize = 12.sp)
                    }
                }
                Button(onClick = { reloadUsers() }) {
                    Text("刷新", fontSize = 12.sp)
                }
            }
        }

        // 超级管理员：创建管理员表单
        if (showCreateAdmin && isSuperAdmin) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("创建新管理员", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newAdminName,
                            onValueChange = { newAdminName = it },
                            label = { Text("用户名") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newAdminPwd,
                            onValueChange = { newAdminPwd = it },
                            label = { Text("密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            if (newAdminName.isBlank() || newAdminPwd.isBlank()) {
                                msg("❌ 用户名和密码不能为空")
                                return@Button
                            }
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val (success, msgText) = dbManager?.registerUser(newAdminName, newAdminPwd, UserRole.ADMIN) ?: Pair(false, "数据库未连接")
                                    msg(if (success) "✅ 管理员 $newAdminName 创建成功" else "❌ $msgText")
                                    if (success) {
                                        newAdminName = ""
                                        newAdminPwd = ""
                                        showCreateAdmin = false
                                        reloadUsers()
                                    }
                                }
                            }
                        }) {
                            Text("创建", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 重置密码弹窗
        if (resetPwdUser != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("重置密码 - ${resetPwdUser!!.username}", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = resetPwdValue,
                            onValueChange = { resetPwdValue = it },
                            label = { Text("新密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val success = dbManager?.resetPassword(resetPwdUser!!.id, resetPwdValue) ?: false
                                    msg(if (success) "✅ 密码已重置" else "❌ 重置失败（密码至少6位）")
                                    if (success) {
                                        resetPwdUser = null
                                        resetPwdValue = ""
                                    }
                                }
                            }
                        }) {
                            Text("确认", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { resetPwdUser = null; resetPwdValue = "" }) {
                            Text("取消", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 操作消息
        if (actionMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (actionMessage.contains("✅"))
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = actionMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (visibleUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无用户数据", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleUsers) { user ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.username,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (!user.enabled) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "[已禁用]",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    Text(
                                        text = "角色: ${user.role.label}  |  ID: ${user.id}${if (user.createdAt.isNotEmpty()) "  |  注册: ${user.createdAt}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 超级管理员：修改角色（不能修改自己的角色）
                                if (isSuperAdmin && user.id != currentUser.id) {
                                    var roleExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(onClick = { roleExpanded = true }, modifier = Modifier.height(36.dp)) {
                                            Text(user.role.label, fontSize = 12.sp)
                                        }
                                        DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                            UserRole.values().forEach { role ->
                                                DropdownMenuItem(
                                                    text = { Text(role.label) },
                                                    onClick = {
                                                        roleExpanded = false
                                                        if (role != user.role) {
                                                            coroutineScope.launch {
                                                                withContext(Dispatchers.IO) {
                                                                    val success = dbManager?.updateUserRole(user.id, role) ?: false
                                                                    msg(if (success) "✅ ${user.username} 角色已改为 ${role.label}" else "❌ 修改失败")
                                                                    if (success) reloadUsers()
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 禁用/启用（不能操作自己和超级管理员）
                                if (user.id != currentUser.id && (isSuperAdmin || user.role == UserRole.TOURIST)) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    val success = if (user.enabled) dbManager?.disableUser(user.id) else dbManager?.enableUser(user.id)
                                                    msg(if (success == true) "✅ ${user.username} 已${if (user.enabled) "禁用" else "启用"}" else "❌ 操作失败")
                                                    if (success == true) reloadUsers()
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(36.dp),
                                        colors = if (user.enabled) {
                                            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        } else {
                                            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                        }
                                    ) {
                                        Text(if (user.enabled) "禁用" else "启用", fontSize = 12.sp)
                                    }
                                }

                                // 重置密码
                                OutlinedButton(
                                    onClick = { resetPwdUser = user; resetPwdValue = "" },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("重置密码", fontSize = 12.sp)
                                }

                                // 删除（不能删除自己，不能删除唯一的超级管理员）
                                if (user.id != currentUser.id && (isSuperAdmin || user.role == UserRole.TOURIST)) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    val success = dbManager?.deleteUser(user.id) ?: false
                                                    msg(if (success) "✅ 已删除 ${user.username}" else "❌ 删除失败")
                                                    if (success) reloadUsers()
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(36.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("删除", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 知识库管理选项卡（管理员和超级管理员可用）
 * 支持新增/编辑/删除知识条目
 */
@Composable
fun KnowledgeManagementTab(service: QAService, currentUser: User) {
    var allKnowledge by remember { mutableStateOf(emptyList<com.lanzhou.qa.model.KnowledgeItem>()) }
    var filteredKnowledge by remember { mutableStateOf(emptyList<com.lanzhou.qa.model.KnowledgeItem>()) }
    var searchKeyword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var actionMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    var showForm by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<com.lanzhou.qa.model.KnowledgeItem?>(null) }
    var formQuestion by remember { mutableStateOf("") }
    var formAnswer by remember { mutableStateOf("") }
    var formCategory by remember { mutableStateOf("") }

    fun reload() {
        isLoading = true
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                allKnowledge = service.getAllKnowledge()
                filteredKnowledge = if (searchKeyword.isBlank()) allKnowledge else allKnowledge.filter {
                    it.question.contains(searchKeyword, ignoreCase = true) || it.answer.contains(searchKeyword, ignoreCase = true)
                }
                isLoading = false
            }
        }
    }

    fun openEditForm(item: com.lanzhou.qa.model.KnowledgeItem) {
        editingItem = item
        formQuestion = item.question
        formAnswer = item.answer
        formCategory = item.category
        showForm = true
    }

    fun openAddForm() {
        editingItem = null
        formQuestion = ""
        formAnswer = ""
        formCategory = ""
        showForm = true
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            allKnowledge = service.getAllKnowledge()
            filteredKnowledge = allKnowledge
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { keyword ->
                    searchKeyword = keyword
                    filteredKnowledge = if (keyword.isBlank()) allKnowledge else allKnowledge.filter {
                        it.question.contains(keyword, ignoreCase = true) || it.answer.contains(keyword, ignoreCase = true) || it.category.contains(keyword, ignoreCase = true)
                    }
                },
                label = { Text("搜索...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(onClick = { openAddForm() }) { Text("新增条目", fontSize = 12.sp) }
            Button(onClick = { reload() }) { Text("刷新", fontSize = 12.sp) }
        }

        Text("共 ${filteredKnowledge.size} 条知识", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))

        // 新增/编辑表单
        if (showForm) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = if (editingItem != null) "编辑知识条目 #${editingItem!!.id}" else "新增知识条目", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = formQuestion, onValueChange = { formQuestion = it }, label = { Text("问题") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = formAnswer, onValueChange = { formAnswer = it }, label = { Text("回答") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = formCategory, onValueChange = { formCategory = it }, label = { Text("分类") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (formQuestion.isBlank() || formAnswer.isBlank()) { actionMessage = "❌ 问题和回答不能为空"; return@Button }
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val success = if (editingItem != null) service.updateKnowledge(editingItem!!.id, formQuestion, formAnswer, formCategory)
                                        else service.addKnowledge(formQuestion, formAnswer, formCategory)
                                    actionMessage = if (success) (if (editingItem != null) "✅ 条目已更新" else "✅ 新增成功") else "❌ 操作失败（需数据库模式）"
                                    if (success) { showForm = false; reload() }
                                }
                            }
                        }) { Text(if (editingItem != null) "保存" else "添加") }
                        OutlinedButton(onClick = { showForm = false }) { Text("取消") }
                    }
                }
            }
        }

        if (actionMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = if (actionMessage.contains("✅")) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)
            ) { Text(text = actionMessage, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp)) }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredKnowledge) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "❓ ${item.question}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                Text(text = "📚 ${item.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = item.answer, style = MaterialTheme.typography.bodySmall, lineHeight = 16.sp, maxLines = 3)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { openEditForm(item) }, modifier = Modifier.height(34.dp)) { Text("编辑", fontSize = 11.sp) }
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                val success = service.deleteKnowledge(item.id)
                                                actionMessage = if (success) "✅ 已删除 #${item.id}" else "❌ 删除失败（需数据库模式）"
                                                if (success) reload()
                                            }
                                        }
                                    },
                                    modifier = Modifier.height(34.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) { Text("删除", fontSize = 11.sp) }
                                Text(text = "ID: ${item.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.CenterVertically))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 系统配置选项卡（仅超级管理员可用）
 */
@Composable
fun SystemConfigTab(service: QAService, currentSource: Int, onSourceChange: (Int) -> Unit) {
    var sourceState by remember { mutableStateOf(currentSource) }
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "数据源配置", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "选择知识库数据来源，切换后立即生效", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).clickable {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val success = service.reloadKnowledgeBase(0)
                                    if (success) { sourceState = 0; onSourceChange(0) }
                                    message = if (success) "✅ 已切换到本地JSON模式" else "❌ 切换失败"
                                }
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = if (sourceState == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("JSON模式", style = MaterialTheme.typography.titleSmall)
                            Text("本地文件读取", style = MaterialTheme.typography.bodySmall)
                            if (sourceState == 0) { Text("当前使用中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).clickable {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val success = service.reloadKnowledgeBase(1)
                                    if (success) { sourceState = 1; onSourceChange(1) }
                                    message = if (success) "✅ 已切换到数据库模式" else "❌ 切换失败，请检查数据库配置"
                                }
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = if (sourceState == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("数据库模式", style = MaterialTheme.typography.titleSmall)
                            Text("MariaDB/MySQL", style = MaterialTheme.typography.bodySmall)
                            if (sourceState == 1) { Text("当前使用中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }
        }
        if (message.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (message.contains("✅")) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)
            ) { Text(text = message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp)) }
        }
    }
}
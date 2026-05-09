package com.lanzhou.qa.service

import com.lanzhou.qa.api.MIMOClient
import com.lanzhou.qa.api.TTSClient
import com.lanzhou.qa.config.ChatHistory
import com.lanzhou.qa.config.ConfigManager
import com.lanzhou.qa.config.LanguageManager
import com.lanzhou.qa.database.DatabaseManager
import com.lanzhou.qa.api.ImageClient
import com.lanzhou.qa.embedding.EmbeddingModel
import com.lanzhou.qa.model.ImageConfig
import com.lanzhou.qa.model.KnowledgeItem
import com.lanzhou.qa.rag.RAGRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * QA服务 - 整合RAG和MIMO API
 * 支持双模式：数据库 + 本地JSON
 */
class QAService {

    private val config = ConfigManager.loadConfig()
    private val databaseConfig = ConfigManager.loadDatabaseConfig()
    private val databaseManager = DatabaseManager(databaseConfig)

    // 当前数据源类型：0 = JSON, 1 = 数据库
    private var currentDataSource: Int = if (databaseConfig.enabled) 1 else 0

    // 知识库来源：数据库优先，JSON作为备用
    private var knowledgeItems: List<KnowledgeItem> = emptyList()

    private val embeddingModel = EmbeddingModel(config.embedding.dimension)
    private var ragRetriever: RAGRetriever? = null
    private val mimoClient = MIMOClient(config.api)
    private val imageClient = ImageClient(config.image)
    private val ttsClient = TTSClient(config.tts)

    // JSON聊天历史存储
    private val jsonHistoryFile = File("chat_history.json")
    private val jsonHistoryLock = Any()

    init {
        // 初始化时加载默认数据源
        reloadKnowledgeBase(currentDataSource)
    }

    /**
     * JSON聊天历史数据类
     */
    @Serializable
    private data class JSONChatHistory(
        val history: List<JSONChatRecord>
    )

    @Serializable
    private data class JSONChatRecord(
        val question: String,
        val answer: String,
        val timestamp: String
    )

    /**
     * 重新加载知识库（支持动态切换数据源）
     */
    fun reloadKnowledgeBase(sourceType: Int): Boolean {
        currentDataSource = sourceType
        println("🔄 切换数据源到: ${if (sourceType == 1) "数据库" else "本地JSON"}")

        knowledgeItems = when (sourceType) {
            1 -> {
                // 数据库模式
                if (databaseManager.initialize()) {
                    val qaPairs = databaseManager.getAllQAPairs()
                    println("✅ 从数据库加载了 ${qaPairs.size} 条知识")
                    qaPairs.map { KnowledgeItem(it.id, it.question, it.answer, "数据库") }
                } else {
                    println("⚠️ 数据库连接失败，回退到JSON模式")
                    loadFromJson()
                }
            }
            else -> {
                // JSON模式
                loadFromJson()
            }
        }

        // 重新创建RAG检索器
        ragRetriever = RAGRetriever(
            knowledgeItems = knowledgeItems,
            embeddingModel = embeddingModel,
            topK = config.system.top_k
        )

        return knowledgeItems.isNotEmpty()
    }

    /**
     * 获取当前数据源类型
     */
    fun getCurrentDataSource(): Int = currentDataSource

    /**
     * 获取当前数据源名称
     */
    fun getCurrentDataSourceName(): String = if (currentDataSource == 1) "数据库" else "本地JSON"

    private fun loadFromJson(): List<KnowledgeItem> {
        return try {
            val knowledgeBase = ConfigManager.loadKnowledgeBase()
            println("✅ 从JSON加载了 ${knowledgeBase.knowledge_base.size} 条知识")
            knowledgeBase.knowledge_base
        } catch (e: Exception) {
            println("❌ 加载JSON知识库失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 处理用户问题
     */
    suspend fun askQuestion(question: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = ragRetriever
                if (retriever == null || knowledgeItems.isEmpty()) {
                    return@withContext "❌ 知识库未加载，请检查数据源配置"
                }

                // 1. 检索相关知识
                val retrievalResults = retriever.retrieve(question)

                // 2. 构建上下文
                val context = retriever.buildContext(retrievalResults)

                // 3. 构建RAG提示词（使用当前语言的系统提示词）
                val prompt = retriever.buildPrompt(
                    question = question,
                    context = context,
                    systemPrompt = LanguageManager.getSystemPrompt()
                )

                // 4. 调用MIMO API
                val answer = mimoClient.call(prompt)

                // 5. 异步保存聊天历史（如果启用数据库）
                if (databaseConfig.enabled && databaseManager.isInitialized()) {
                    saveChatHistoryAsync(question, answer)
                }

                answer
            } catch (e: Exception) {
                "处理问题时发生错误: ${e.message}"
            }
        }
    }

    /**
     * 根据回答生成智能图像
     */
    suspend fun generateImagesFromAnswer(answer: String, count: Int = 1): List<String> {
        return withContext(Dispatchers.IO) {
            if (answer.isBlank()) {
                return@withContext emptyList<String>()
            }

            val imagePrompt = buildImagePrompt(answer)
            val imagePaths = imageClient.generate(imagePrompt, count)

            if (imagePaths.isEmpty()) {
                println("⚠️ 未生成到图像结果，检查 image 配置和 API 是否可用")
            }
            imagePaths
        }
    }

    private fun buildImagePrompt(answer: String): String {
        return "Generate a high-quality, realistic image based on the following description: $answer"
    }

    /**
     * 异步保存聊天历史
     */
    private fun saveChatHistoryAsync(question: String, answer: String) {
        try {
            // 在后台线程保存，不阻塞主流程
            Thread {
                try {
                    if (databaseConfig.enabled && databaseManager.isInitialized()) {
                        // 保存到数据库
                        databaseManager.saveChatHistory(question, answer)
                    } else {
                        // 保存到JSON文件
                        saveChatHistoryToJSON(question, answer)
                    }
                } catch (e: Exception) {
                    println("⚠️ 保存聊天历史失败: ${e.message}")
                }
            }.start()
        } catch (e: Exception) {
            println("⚠️ 启动保存聊天历史线程失败: ${e.message}")
        }
    }

    /**
     * 保存聊天历史到JSON文件
     */
    private fun saveChatHistoryToJSON(question: String, answer: String) {
        synchronized(jsonHistoryLock) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                // 读取现有历史
                val historyData = if (jsonHistoryFile.exists()) {
                    val content = jsonHistoryFile.readText()
                    json.decodeFromString<JSONChatHistory>(content)
                } else {
                    JSONChatHistory(emptyList())
                }

                // 添加新记录
                val newRecord = JSONChatRecord(question, answer, timestamp)
                val updatedHistory = historyData.history + newRecord

                // 限制历史记录数量（最多100条）
                val limitedHistory = updatedHistory.takeLast(100)

                // 写入文件
                val updatedData = JSONChatHistory(limitedHistory)
                val jsonString = Json.encodeToString<JSONChatHistory>(updatedData)
                jsonHistoryFile.writeText(jsonString)

                println("✅ 聊天历史已保存到JSON: ${jsonHistoryFile.absolutePath}")
            } catch (e: Exception) {
                println("⚠️ 保存聊天历史到JSON失败: ${e.message}")
            }
        }
    }

    /**
     * 从JSON文件加载聊天历史
     */
    private fun loadChatHistoryFromJSON(limit: Int): List<ChatHistory> {
        synchronized(jsonHistoryLock) {
            try {
                if (!jsonHistoryFile.exists()) {
                    return emptyList()
                }

                val json = Json { ignoreUnknownKeys = true }
                val content = jsonHistoryFile.readText()
                val historyData = json.decodeFromString<JSONChatHistory>(content)

                return historyData.history.takeLast(limit).map { record ->
                    ChatHistory(
                        id = 0, // JSON模式没有ID
                        question = record.question,
                        answer = record.answer,
                        timestamp = record.timestamp
                    )
                }
            } catch (e: Exception) {
                println("⚠️ 加载聊天历史从JSON失败: ${e.message}")
                return emptyList()
            }
        }
    }

    /**
     * 获取所有知识项
     */
    fun getAllKnowledge(): List<KnowledgeItem> {
        return knowledgeItems
    }

    /**
     * 搜索知识
     */
    fun searchKnowledge(keyword: String): List<KnowledgeItem> {
        return knowledgeItems.filter { item ->
            item.question.contains(keyword, ignoreCase = true) ||
            item.answer.contains(keyword, ignoreCase = true) ||
            item.category.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 测试API连接
     */
    suspend fun testApiConnection(): Boolean {
        return mimoClient.testConnection()
    }

    /**
     * 测试数据库连接
     * 在JSON模式下测试JSON文件读取，在数据库模式下测试数据库连接
     */
    fun testDatabaseConnection(): Boolean {
        return if (databaseConfig.enabled) {
            // 数据库模式 - 测试数据库连接
            databaseManager.testConnection()
        } else {
            // JSON模式 - 测试JSON文件读取
            try {
                val knowledgeBase = ConfigManager.loadKnowledgeBase()
                knowledgeBase.knowledge_base.isNotEmpty()
            } catch (e: Exception) {
                println("❌ JSON文件读取失败: ${e.message}")
                false
            }
        }
    }

    /**
     * 获取统计信息
     */
    fun getStats(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()

        // 基础统计
        stats["totalItems"] = knowledgeItems.size
        stats["categories"] = knowledgeItems.groupingBy { it.category }.eachCount().size

        // 当前数据源
        stats["source"] = currentDataSource

        // 数据库统计（如果启用）
        if (currentDataSource == 1 && databaseManager.isInitialized()) {
            val dbStats = databaseManager.getStats()
            stats["db_qa_pairs"] = dbStats["qa_pairs_count"] ?: 0
            stats["db_chat_history"] = dbStats["chat_history_count"] ?: 0
        }

        // 按分类统计
        val categoryCount = knowledgeItems.groupingBy { it.category }.eachCount()
        stats.putAll(categoryCount.mapKeys { "category_${it.key}" })

        return stats
    }

    /**
     * 获取聊天历史
     */
    fun getChatHistory(limit: Int = 50): List<ChatHistory> {
        return if (databaseConfig.enabled && databaseManager.isInitialized()) {
            databaseManager.getChatHistory(limit)
        } else {
            loadChatHistoryFromJSON(limit)
        }
    }

    /**
     * 朗读文本
     */
    suspend fun speakText(text: String): Boolean {
        return withContext(Dispatchers.IO) {
            ttsClient.speak(text)
        }
    }

    var onAmplitudeUpdate: ((List<Float>) -> Unit)?
        get() = ttsClient.onAmplitudeUpdate
        set(value) { ttsClient.onAmplitudeUpdate = value }

    /**
     * 停止朗读
     */
    fun stopSpeaking() {
        ttsClient.stop()
    }

    /**
     * 是否正在朗读
     */
    fun isSpeaking(): Boolean = ttsClient.isSpeaking()

    /**
     * 关闭数据库连接和客户端
     */
    fun close() {
        if (databaseConfig.enabled) {
            databaseManager.close()
        }
        // 关闭MIMO客户端
        mimoClient.close()
        ttsClient.close()
    }
}

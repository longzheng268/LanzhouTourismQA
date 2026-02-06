package com.lanzhou.qa.service

import com.lanzhou.qa.api.MIMOClient
import com.lanzhou.qa.config.ChatHistory
import com.lanzhou.qa.config.ConfigManager
import com.lanzhou.qa.database.DatabaseManager
import com.lanzhou.qa.embedding.EmbeddingModel
import com.lanzhou.qa.model.KnowledgeItem
import com.lanzhou.qa.rag.RAGRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    init {
        // 初始化时加载默认数据源
        reloadKnowledgeBase(currentDataSource)
    }

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

                // 3. 构建RAG提示词
                val prompt = retriever.buildPrompt(
                    question = question,
                    context = context,
                    systemPrompt = config.system.prompt_template
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
     * 异步保存聊天历史
     */
    private fun saveChatHistoryAsync(question: String, answer: String) {
        try {
            // 在后台线程保存，不阻塞主流程
            Thread {
                try {
                    databaseManager.saveChatHistory(question, answer)
                } catch (e: Exception) {
                    println("⚠️ 保存聊天历史失败: ${e.message}")
                }
            }.start()
        } catch (e: Exception) {
            println("⚠️ 启动保存聊天历史线程失败: ${e.message}")
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
     */
    fun testDatabaseConnection(): Boolean {
        return if (databaseConfig.enabled) {
            databaseManager.testConnection()
        } else {
            false
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
            emptyList()
        }
    }

    /**
     * 关闭数据库连接和客户端
     */
    fun close() {
        if (databaseConfig.enabled) {
            databaseManager.close()
        }
        // 关闭MIMO客户端
        mimoClient.close()
    }
}

package com.lanzhou.qa.config

import kotlinx.serialization.json.Json
import java.io.File

/**
 * 语言管理器 - 负责加载和管理多语言配置
 */
object LanguageManager {
    private val config: LanguageConfig by lazy {
        loadConfig()
    }

    private var currentLanguageCode: String = "zh"

    /**
     * 加载语言配置文件
     */
    private fun loadConfig(): LanguageConfig {
        val configFile = File("src/main/resources/languages.json")
        val json = Json { ignoreUnknownKeys = true }

        return if (configFile.exists()) {
            val jsonString = configFile.readText()
            json.decodeFromString(LanguageConfig.serializer(), jsonString)
        } else {
            // Fallback to default Chinese configuration
            LanguageConfig(
                languages = mapOf(
                    "zh" to Language(
                        name = "中文",
                        code = "zh",
                        ui = UIStrings(
                            title = "兰州旅游知识问答系统",
                            subtitle = "RAG版 - 基于知识库的智能问答",
                            question_label = "请输入您的问题",
                            test_api = "测试API",
                            test_database = "测试数据库",
                            ask_ai = "提问 AI",
                            test_result = "测试结果",
                            ai_answer = "AI 回答",
                            knowledge_base = "知识库",
                            chat_history = "聊天历史",
                            stats = "统计",
                            data_source = "数据源",
                            total_items = "知识总条目",
                            categories = "分类数量",
                            db_qa_pairs = "数据库QA对",
                            db_chat_history = "聊天历史",
                            json_mode = "JSON模式",
                            database_mode = "数据库模式",
                            switch_to_json = "切换到JSON",
                            switch_to_database = "切换到数据库",
                            search_knowledge = "搜索知识...",
                            refresh = "刷新",
                            no_chat_history = "暂无聊天历史",
                            total_records = "共 {count} 条聊天记录",
                            basic_stats = "基础统计",
                            db_stats = "数据库统计",
                            category_stats = "分类统计",
                            test_api_success = "✅ API连接测试成功",
                            test_api_failed = "❌ API连接测试失败",
                            test_db_success = "✅ 数据库连接测试成功",
                            test_db_failed = "❌ 数据库连接测试失败（可能未启用数据库模式）",
                            switch_to_json_success = "✅ 已切换到本地JSON",
                            switch_to_database_success = "✅ 已切换到数据库",
                            switch_to_database_failed = "❌ 切换失败，请检查数据库配置",
                            knowledge_search = "搜索知识",
                            question = "问题",
                            answer = "回答",
                            timestamp = "时间",
                            category = "分类",
                            id = "ID",
                            loading = "加载中...",
                            source = "来源",
                            items = "条目",
                            db_qa = "DB-QA",
                            history = "历史"
                        ),
                        ai = AIStrings(
                            system_prompt = "你是一个专业的兰州旅游专家，精通兰州的地理、景点、美食、文化、历史等各个方面。请用中文回答用户的问题。",
                            test_prompt = "测试连接"
                        )
                    )
                ),
                default_language = "zh"
            )
        }
    }

    /**
     * 获取当前语言代码
     */
    fun getCurrentLanguageCode(): String {
        return currentLanguageCode
    }

    /**
     * 设置当前语言
     */
    fun setCurrentLanguage(code: String) {
        if (config.languages.containsKey(code)) {
            currentLanguageCode = code
        }
    }

    /**
     * 获取当前语言的所有语言代码
     */
    fun getAvailableLanguages(): List<String> {
        return config.languages.keys.toList()
    }

    /**
     * 获取语言名称
     */
    fun getLanguageName(code: String): String {
        return config.languages[code]?.name ?: code
    }

    /**
     * 获取当前语言的UI字符串
     */
    fun getUIStrings(): UIStrings {
        return config.languages[currentLanguageCode]?.ui
            ?: config.languages[config.default_language]?.ui
            ?: throw IllegalStateException("No UI strings available for language $currentLanguageCode")
    }

    /**
     * 获取当前语言的AI字符串
     */
    fun getAIStrings(): AIStrings {
        return config.languages[currentLanguageCode]?.ai
            ?: config.languages[config.default_language]?.ai
            ?: throw IllegalStateException("No AI strings available for language $currentLanguageCode")
    }

    /**
     * 获取系统提示词（根据当前语言）
     */
    fun getSystemPrompt(): String {
        return getAIStrings().system_prompt
    }

    /**
     * 获取测试提示词（根据当前语言）
     */
    fun getTestPrompt(): String {
        return getAIStrings().test_prompt
    }

    /**
     * 格式化带占位符的字符串
     */
    fun formatString(template: String, vararg args: Pair<String, Any>): String {
        var result = template
        for ((key, value) in args) {
            result = result.replace("{$key}", value.toString())
        }
        return result
    }

    /**
     * 获取当前语言的UI字符串（带格式化）
     */
    fun getUIString(key: String, vararg args: Pair<String, Any>): String {
        val uiStrings = getUIStrings()
        return when (key) {
            "total_records" -> formatString(uiStrings.total_records, *args)
            else -> uiStrings.title // Fallback
        }
    }
}

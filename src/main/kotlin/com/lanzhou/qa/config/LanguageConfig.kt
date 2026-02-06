package com.lanzhou.qa.config

import kotlinx.serialization.Serializable

/**
 * 语言配置模型
 */
@Serializable
data class LanguageConfig(
    val languages: Map<String, Language>,
    val default_language: String = "zh"
)

@Serializable
data class Language(
    val name: String,
    val code: String,
    val ui: UIStrings,
    val ai: AIStrings
)

@Serializable
data class UIStrings(
    val title: String,
    val subtitle: String,
    val question_label: String,
    val test_api: String,
    val test_database: String,
    val ask_ai: String,
    val test_result: String,
    val ai_answer: String,
    val knowledge_base: String,
    val chat_history: String,
    val stats: String,
    val data_source: String,
    val total_items: String,
    val categories: String,
    val db_qa_pairs: String,
    val db_chat_history: String,
    val json_mode: String,
    val database_mode: String,
    val switch_to_json: String,
    val switch_to_database: String,
    val search_knowledge: String,
    val refresh: String,
    val no_chat_history: String,
    val total_records: String,
    val basic_stats: String,
    val db_stats: String,
    val category_stats: String,
    val test_api_success: String,
    val test_api_failed: String,
    val test_db_success: String,
    val test_db_failed: String,
    val switch_to_json_success: String,
    val switch_to_database_success: String,
    val switch_to_database_failed: String,
    val knowledge_search: String,
    val question: String,
    val answer: String,
    val timestamp: String,
    val category: String,
    val id: String,
    val loading: String,
    val source: String,
    val items: String,
    val db_qa: String,
    val history: String
)

@Serializable
data class AIStrings(
    val system_prompt: String,
    val test_prompt: String
)
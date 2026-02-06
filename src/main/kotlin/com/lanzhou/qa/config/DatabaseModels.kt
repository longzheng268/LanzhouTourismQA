package com.lanzhou.qa.config

import kotlinx.serialization.Serializable

/**
 * 数据库表结构定义（与SQL脚本完全一致）
 */

// qa_pairs 表对应的实体类
@Serializable
data class QAPair(
    val id: Int,
    val question: String,
    val answer: String
)

// chat_history 表对应的实体类
@Serializable
data class ChatHistory(
    val id: Int,
    val question: String,
    val answer: String,
    val timestamp: String
)

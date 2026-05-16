package com.lanzhou.qa.config

import kotlinx.serialization.Serializable

/**
 * 用户角色枚举
 */
enum class UserRole(val label: String) {
    TOURIST("游客"),
    ADMIN("管理员"),
    SUPER_ADMIN("超级管理员")
}

/**
 * 用户数据模型
 */
data class User(
    val id: Int,
    val username: String,
    val role: UserRole,
    val enabled: Boolean = true,
    val createdAt: String = ""
)

/**
 * 数据库表结构定义（与SQL脚本完全一致）
 */

// qa_pairs 表对应的实体类
@Serializable
data class QAPair(
    val id: Int,
    val question: String,
    val answer: String,
    val category: String = ""
)

// chat_history 表对应的实体类
@Serializable
data class ChatHistory(
    val id: Int,
    val question: String,
    val answer: String,
    val timestamp: String
)

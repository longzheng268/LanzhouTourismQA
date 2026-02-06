package com.lanzhou.qa.model

import kotlinx.serialization.Serializable

// 知识库数据模型
@Serializable
data class KnowledgeItem(
    val id: Int,
    val question: String,
    val answer: String,
    val category: String
)

@Serializable
data class KnowledgeBase(
    val knowledge_base: List<KnowledgeItem>
)

// 配置模型
@Serializable
data class ApiConfig(
    val api_url: String,
    val api_key: String,
    val model: String,
    val temperature: Double,
    val max_tokens: Int
)

@Serializable
data class SystemConfig(
    val prompt_template: String,
    val top_k: Int
)

@Serializable
data class EmbeddingConfig(
    val model: String,
    val dimension: Int
)

// 数据库配置
@Serializable
data class DatabaseConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 3306,
    val database: String = "",
    val username: String = "",
    val password: String = "",
    val maximumPoolSize: Int = 10,
    val minimumIdle: Int = 2,
    val connectionTimeout: Int = 30000
)

@Serializable
data class AppConfig(
    val api: ApiConfig,
    val system: SystemConfig,
    val embedding: EmbeddingConfig,
    val database: DatabaseConfig
)

// MIMO API 请求/响应模型
@Serializable
data class MIMORequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1000
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class MIMOResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String?
)

// 向量模型
data class Vector(
    val values: List<Double>,
    val metadata: VectorMetadata
)

data class VectorMetadata(
    val id: Int,
    val question: String,
    val answer: String,
    val category: String
)

// 检索结果
data class RetrievalResult(
    val item: KnowledgeItem,
    val similarity: Double
)

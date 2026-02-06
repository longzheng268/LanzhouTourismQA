package com.lanzhou.qa.config

import com.lanzhou.qa.model.AppConfig
import com.lanzhou.qa.model.DatabaseConfig
import com.lanzhou.qa.model.KnowledgeBase
import kotlinx.serialization.json.Json
import java.io.File

object ConfigManager {
    private val json = Json { ignoreUnknownKeys = true }

    private const val CONFIG_PATH = "config.json"
    private const val KNOWLEDGE_PATH = "knowledge_base.json"

    fun loadConfig(): AppConfig {
        val configFile = File(CONFIG_PATH)
        return if (configFile.exists()) {
            val content = configFile.readText()
            json.decodeFromString<AppConfig>(content)
        } else {
            // 从资源文件加载
            val resource = this::class.java.classLoader.getResource(CONFIG_PATH)
            if (resource != null) {
                val content = resource.readText()
                json.decodeFromString<AppConfig>(content)
            } else {
                throw RuntimeException("配置文件未找到: $CONFIG_PATH")
            }
        }
    }

    fun loadDatabaseConfig(): DatabaseConfig {
        return try {
            val config = loadConfig()
            config.database
        } catch (e: Exception) {
            // 如果配置文件没有数据库配置，返回默认配置
            DatabaseConfig()
        }
    }

    fun loadKnowledgeBase(): KnowledgeBase {
        val knowledgeFile = File(KNOWLEDGE_PATH)
        return if (knowledgeFile.exists()) {
            val content = knowledgeFile.readText()
            json.decodeFromString<KnowledgeBase>(content)
        } else {
            // 从资源文件加载
            val resource = this::class.java.classLoader.getResource(KNOWLEDGE_PATH)
            if (resource != null) {
                val content = resource.readText()
                json.decodeFromString<KnowledgeBase>(content)
            } else {
                throw RuntimeException("知识库文件未找到: $KNOWLEDGE_PATH")
            }
        }
    }
}

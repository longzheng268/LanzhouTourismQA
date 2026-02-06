package com.lanzhou.qa.api

import com.lanzhou.qa.model.ApiConfig
import com.lanzhou.qa.model.MIMORequest
import com.lanzhou.qa.model.Message
import com.lanzhou.qa.model.MIMOResponse
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * MIMO API客户端
 */
class MIMOClient(private val config: ApiConfig) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    /**
     * 调用MIMO API
     */
    suspend fun call(prompt: String): String {
        val messages = listOf(
            Message(role = "system", content = "你是一个专业的兰州旅游专家"),
            Message(role = "user", content = prompt)
        )

        val request = MIMORequest(
            model = config.model,
            messages = messages,
            temperature = config.temperature,
            max_tokens = config.max_tokens
        )

        return try {
            println("🔄 正在调用MIMO API: ${config.api_url}")
            println("🔄 使用模型: ${config.model}")

            val response: HttpResponse = client.post(config.api_url) {
                // 尝试两种认证方式：api-key 和 Authorization Bearer
                header("api-key", config.api_key)
                header("Authorization", "Bearer ${config.api_key}")
                header("Content-Type", "application/json")
                setBody(request)
            }

            println("✅ API响应状态: ${response.status.value}")

            if (response.status.value == 200) {
                val mimoResponse = response.body<MIMOResponse>()
                val answer = mimoResponse.choices.firstOrNull()?.message?.content ?: "未获取到有效回答"
                println("✅ 成功获取回答，长度: ${answer.length}")
                answer
            } else {
                val errorBody = response.bodyAsText()
                println("❌ API调用失败: ${response.status.value} - $errorBody")
                "API调用失败: ${response.status.value} - $errorBody"
            }
        } catch (e: Exception) {
            println("❌ 调用MIMO API时发生错误: ${e.javaClass.simpleName} - ${e.message}")
            println("❌ 详细错误信息: ${e.stackTraceToString()}")
            "调用MIMO API时发生错误: ${e.javaClass.simpleName} - ${e.message}"
        }
    }

    /**
     * 测试API连接
     */
    suspend fun testConnection(): Boolean {
        println("🔄 开始测试MIMO API连接...")
        println("🔄 API地址: ${config.api_url}")
        println("🔄 模型: ${config.model}")

        return try {
            val testPrompt = "测试连接"
            val result = call(testPrompt)
            val success = !result.contains("错误") && !result.contains("失败")

            if (success) {
                println("✅ API连接测试成功")
            } else {
                println("❌ API连接测试失败: $result")
            }

            success
        } catch (e: Exception) {
            println("❌ API连接测试异常: ${e.javaClass.simpleName} - ${e.message}")
            false
        }
    }

    /**
     * 关闭客户端
     */
    fun close() {
        try {
            client.close()
            println("✅ MIMO客户端已关闭")
        } catch (e: Exception) {
            println("⚠️ 关闭MIMO客户端时出错: ${e.message}")
        }
    }
}

package com.lanzhou.qa.api

import com.lanzhou.qa.model.ImageConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO

@Serializable
data class ImageGenerateRequest(
    val model: String,
    val prompt: String,
    val n: Int = 1,
    val size: String = "512x512",
    val response_format: String = "url"
)

@Serializable
data class ImageData(
    val url: String? = null,
    val b64_json: String? = null
)

@Serializable
data class ImageGenerateResponse(
    val created: Long,
    val data: List<ImageData>
)

class ImageClient(private val config: ImageConfig) {

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = config.request_timeout_ms
            endpoint {
                connectTimeout = config.request_timeout_ms
                socketTimeout = config.request_timeout_ms
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    suspend fun generate(prompt: String, count: Int = 1): List<String> {
        if (!config.enabled) {
            println("⚠️ 图像生成已禁用")
            return emptyList()
        }
        if (config.image_api_url.isBlank()) {
            println("❌ 图像 API URL 未配置")
            return emptyList()
        }

        val request = ImageGenerateRequest(
            model = config.model.ifBlank { "default" },
            prompt = prompt,
            n = count,
            size = config.size,
            response_format = config.response_format
        )

        println("🖼️ 发送图像生成请求:")
        println("   API: ${config.image_api_url}")
        println("   模型: ${request.model}")
        println("   数量: ${request.n}")
        println("   提示词: ${prompt.take(100)}...")

        return try {
            val response: HttpResponse = client.post(config.image_api_url) {
                header("api-key", config.image_api_key)
                header("Authorization", "Bearer ${config.image_api_key}")
                header("Content-Type", "application/json")
                setBody(request)
            }

            println("📊 API 响应状态: ${response.status.value}")
            if (response.status.value == 401) {
                println("❌ 授权失败：请检查 API Key 是否正确，或者权限是否被拒绝")
            }
            if (response.status.value == 403) {
                println("❌ 访问被拒绝：请检查 API Key 权限和访问路径是否正确")
            }

            if (response.status.value == 200) {
                val imageResponse = response.body<ImageGenerateResponse>()
                println("✅ 获得 ${imageResponse.data.size} 张图像")
                imageResponse.data.mapNotNull { imageData ->
                    imageData.url ?: imageData.b64_json?.let { saveBase64Image(it) }
                }
            } else {
                val responseBody = response.bodyAsText()
                println("❌ 图像生成 API 调用失败")
                println("   状态码: ${response.status.value}")
                println("   响应: ${responseBody.take(500)}")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ 图像生成异常: ${e.javaClass.simpleName}")
            println("   原因: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveBase64Image(base64: String): String? {
        return try {
            val bytes = Base64.getDecoder().decode(base64)
            val tempFile = File.createTempFile("qa_image_", ".png")
            tempFile.writeBytes(bytes)
            tempFile.absolutePath
        } catch (e: Exception) {
            println("⚠️ 保存图像文件失败: ${e.message}")
            null
        }
    }

    fun close() {
        try {
            client.close()
        } catch (e: Exception) {
            println("⚠️ 关闭图像客户端失败: ${e.message}")
        }
    }
}

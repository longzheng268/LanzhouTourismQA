package com.lanzhou.qa.api

import com.lanzhou.qa.config.ConfigManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertFalse

class ImageClientTest {
    @Test
    fun `image api should return at least one image url or base64`() {
        val config = ConfigManager.loadConfig().image
        val client = ImageClient(config)
        val prompt = "测试生成兰州美食场景图像"
        val result = kotlinx.coroutines.runBlocking {
            client.generate(prompt, 1)
        }

        println("Image API result count: ${result.size}")
        result.forEach { println("Image result: $it") }

        assertFalse(result.isEmpty(), "图像 API 未返回任何结果，请检查配置或网络")
    }
}
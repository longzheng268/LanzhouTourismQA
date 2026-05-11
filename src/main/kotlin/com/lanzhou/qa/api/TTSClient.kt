package com.lanzhou.qa.api

import com.lanzhou.qa.model.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.sound.sampled.*
import java.text.SimpleDateFormat
import java.util.*

class TTSClient(private val config: TTSConfig) {

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = config.request_timeout_ms
            endpoint {
                connectTimeout = config.request_timeout_ms
                socketTimeout = config.request_timeout_ms
            }
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    var currentVoiceStyle: String = config.voice_style
    var currentSeed: Int = 0

    @Volatile
    private var isPlaying = false

    @Volatile
    private var shouldStop = false

    var onAmplitudeUpdate: ((List<Float>) -> Unit)? = null

    private fun computeAmplitudes(buffer: ByteArray, bytesRead: Int, channels: Int = 1, barsCount: Int = 32): List<Float> {
        val bytesPerSample = 2 * channels
        val totalSamples = bytesRead / bytesPerSample
        val samplesPerBar = totalSamples / barsCount
        if (samplesPerBar <= 0) return List(barsCount) { 0f }

        return List(barsCount) { barIndex ->
            var sum = 0.0
            val startSample = barIndex * samplesPerBar
            for (i in 0 until samplesPerBar) {
                val sampleIndex = (startSample + i) * bytesPerSample
                if (sampleIndex + 1 >= bytesRead) break
                val sample = (buffer[sampleIndex].toInt() and 0xFF) or
                        (buffer[sampleIndex + 1].toInt() shl 8)
                val normalized = sample.toShort().toFloat() / Short.MAX_VALUE
                sum += normalized * normalized
            }
            kotlin.math.sqrt(sum / samplesPerBar).toFloat().coerceIn(0f, 1f)
        }
    }

    suspend fun speak(text: String): Boolean {
        if (!config.enabled || config.app_id.isBlank()) {
            println("⚠️ TTS 未启用或 app_id 未配置, enabled=${config.enabled}, app_id='${config.app_id}'")
            return false
        }

        println("🔊 TTS speak() 被调用, 文本长度: ${text.length}, vcn: $currentVoiceStyle")
        shouldStop = false
        isPlaying = true

        return try {
            val taskId = createTask(text)
            if (taskId != null) {
                val audioUrl = queryTask(taskId)
                if (audioUrl != null) {
                    val audioBytes = downloadAudio(audioUrl)
                    if (audioBytes != null && !shouldStop) {
                        playAudioData(audioBytes)
                    } else {
                        isPlaying = false
                        false
                    }
                } else {
                    isPlaying = false
                    false
                }
            } else {
                isPlaying = false
                false
            }
        } catch (e: Exception) {
            println("❌ TTS 异常: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            isPlaying = false
            false
        }
    }

    private suspend fun createTask(text: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val host = "api-dx.xf-yun.com"
                val path = "/v1/private/dts_create"
                val date = getRFC1123Date()
                val authUrl = buildAuthUrl(host, path, date)

                val encodedText = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))

                val requestBody = buildJsonObject {
                    putJsonObject("header") {
                        put("app_id", config.app_id)
                    }
                    putJsonObject("parameter") {
                        putJsonObject("dts") {
                            put("vcn", currentVoiceStyle)
                            put("language", "zh")
                            put("speed", 50)
                            put("volume", 50)
                            put("pitch", 50)
                            put("rhy", 0)
                            putJsonObject("audio") {
                                put("encoding", "raw")
                                put("sample_rate", 16000)
                                put("channels", 1)
                                put("bit_depth", 16)
                            }
                            putJsonObject("pybuf") {
                                put("encoding", "utf8")
                                put("compress", "raw")
                                put("format", "plain")
                            }
                        }
                    }
                    putJsonObject("payload") {
                        putJsonObject("text") {
                            put("encoding", "utf8")
                            put("compress", "raw")
                            put("format", "plain")
                            put("text", encodedText)
                        }
                    }
                }

                println("🔊 TTS 创建任务请求: $authUrl")

                val response = client.post(authUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toString())
                }

                val responseText = response.bodyAsText()
                println("🔊 创建任务响应: $responseText")

                val responseJson = json.parseToJsonElement(responseText).jsonObject
                val code = responseJson["header"]?.jsonObject?.get("code")?.jsonPrimitive?.int
                val taskId = responseJson["header"]?.jsonObject?.get("task_id")?.jsonPrimitive?.content

                if (code == 0 && taskId != null) {
                    println("✅ 任务创建成功: $taskId")
                    taskId
                } else {
                    println("❌ 任务创建失败: code=$code")
                    null
                }
            } catch (e: Exception) {
                println("❌ 创建任务异常: ${e.message}")
                null
            }
        }
    }

    private suspend fun queryTask(taskId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val host = "api-dx.xf-yun.com"
                val path = "/v1/private/dts_query"

                for (attempt in 1..30) {
                    delay(1000)

                    val date = getRFC1123Date()
                    val authUrl = buildAuthUrl(host, path, date)

                    val requestBody = buildJsonObject {
                        putJsonObject("header") {
                            put("app_id", config.app_id)
                            put("task_id", taskId)
                        }
                    }

                    println("🔊 查询任务 (第 $attempt 次): $taskId")

                    val response = client.post(authUrl) {
                        contentType(ContentType.Application.Json)
                        setBody(requestBody.toString())
                    }

                    val responseText = response.bodyAsText()
                    val responseJson = json.parseToJsonElement(responseText).jsonObject
                    val code = responseJson["header"]?.jsonObject?.get("code")?.jsonPrimitive?.int
                    val taskStatus = responseJson["header"]?.jsonObject?.get("task_status")?.jsonPrimitive?.content

                    if (code == 0) {
                        if (taskStatus == "5") {
                            val audioBase64 = responseJson["payload"]?.jsonObject?.get("audio")?.jsonObject?.get("audio")?.jsonPrimitive?.content
                            if (audioBase64 != null) {
                                val audioUrl = String(Base64.getDecoder().decode(audioBase64), Charsets.UTF_8)
                                println("✅ 任务完成，音频URL: $audioUrl")
                                return@withContext audioUrl
                            }
                        } else {
                            println("⏳ 任务处理中，状态: $taskStatus")
                        }
                    } else {
                        println("❌ 查询失败: code=$code")
                    }
                }

                println("❌ 任务查询超时")
                null
            } catch (e: Exception) {
                println("❌ 查询任务异常: ${e.message}")
                null
            }
        }
    }

    private suspend fun downloadAudio(audioUrl: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                println("🔊 下载音频: $audioUrl")
                val response = client.get(audioUrl)
                val audioBytes = response.readBytes()
                println("🔊 音频下载完成: ${audioBytes.size} 字节")
                audioBytes
            } catch (e: Exception) {
                println("❌ 下载音频异常: ${e.message}")
                null
            }
        }
    }

    private fun playAudioData(audioBytes: ByteArray): Boolean {
        return try {
            // 先尝试用 AudioSystem 直接播放（WAV/MP3等格式）
            try {
                val audioStream = AudioSystem.getAudioInputStream(audioBytes.inputStream())
                val format = audioStream.format
                println("🔊 音频格式: ${format.sampleRate}Hz, ${format.sampleSizeInBits}bit, ${format.channels}ch")
                val info = DataLine.Info(SourceDataLine::class.java, format)
                val line = AudioSystem.getLine(info) as SourceDataLine
                line.open(format)
                line.start()
                println("🔊 开始播放音频...")
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (audioStream.read(buffer).also { bytesRead = it } != -1 && !shouldStop) {
                    line.write(buffer, 0, bytesRead)
                    onAmplitudeUpdate?.invoke(computeAmplitudes(buffer, bytesRead, format.channels))
                }
                if (!shouldStop) line.drain()
                line.stop()
                line.close()
                audioStream.close()
                println("🔊 播放完成")
                isPlaying = false
                return true
            } catch (e: Exception) {
                println("⚠️ AudioSystem 播放失败，尝试 PCM 方式: ${e.message}")
            }

            // 降级：当作 PCM raw 数据播放（16bit, 16000Hz, mono）
            val format = AudioFormat(16000f, 16, 1, true, false)
            val info = DataLine.Info(SourceDataLine::class.java, format)
            val line = AudioSystem.getLine(info) as SourceDataLine
            line.open(format)
            line.start()
            println("🔊 PCM 方式播放, 数据量: ${audioBytes.size} 字节")
            var offset = 0
            val chunkSize = 4096
            while (offset < audioBytes.size && !shouldStop) {
                val len = minOf(chunkSize, audioBytes.size - offset)
                line.write(audioBytes, offset, len)
                onAmplitudeUpdate?.invoke(computeAmplitudes(audioBytes.copyOfRange(offset, offset + len), len))
                offset += len
            }
            if (!shouldStop) line.drain()
            line.stop()
            line.close()
            println("🔊 播放完成")
            isPlaying = false
            true
        } catch (e: Exception) {
            println("❌ 播放异常: ${e.message}")
            isPlaying = false
            false
        }
    }

    private fun buildAuthUrl(host: String, path: String, date: String): String {
        val authParams = buildAuthParams(host, path, date)
        return "https://$host$path?${authParams.entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" }}"
    }

    private fun buildAuthParams(host: String, path: String, date: String): Map<String, String> {
        val signatureOrigin = "host: $host\ndate: $date\nPOST $path HTTP/1.1"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(config.api_secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature = Base64.getEncoder().encodeToString(mac.doFinal(signatureOrigin.toByteArray(Charsets.UTF_8)))

        val authorizationOrigin = "api_key=\"${config.api_key}\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"$signature\""
        val authorization = Base64.getEncoder().encodeToString(authorizationOrigin.toByteArray(Charsets.UTF_8))

        return mapOf(
            "host" to host,
            "date" to date,
            "authorization" to authorization
        )
    }

    private fun getRFC1123Date(): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(Date())
    }

    fun stop() {
        shouldStop = true
        onAmplitudeUpdate?.invoke(List(32) { 0f })
        println("⏹️ TTS 停止")
    }

    fun isSpeaking(): Boolean = isPlaying

    fun close() {
        stop()
        try { client.close() } catch (_: Exception) {}
    }
}

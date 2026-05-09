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
import java.util.Base64
import javax.sound.sampled.*

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
        if (!config.enabled || config.api_url.isBlank()) {
            println("⚠️ TTS 未启用或 URL 未配置")
            return false
        }

        shouldStop = false
        isPlaying = true

        return try {
            // 先尝试非流式调用（更可靠），失败再尝试流式
            val success = speakNonStream(text)
            if (!success) {
                println("🔊 非流式调用失败，尝试流式调用...")
                speakStream(text)
            } else {
                success
            }
        } catch (e: Exception) {
            println("❌ TTS 异常: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            isPlaying = false
            false
        }
    }

    private suspend fun speakNonStream(text: String): Boolean {
        val requestBody = buildJsonObject {
            put("model", config.model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", config.voice_style)
                }
                addJsonObject {
                    put("role", "assistant")
                    put("content", text)
                }
            }
            putJsonObject("audio") {
                put("format", "wav")
            }
            put("stream", false)
        }

        val requestJson = requestBody.toString()
        println("🔊 TTS 非流式请求: ${config.api_url}")
        println("🔊 模型: ${config.model}")

        val response = client.post(config.api_url) {
            header("api-key", config.api_key)
            header("Authorization", "Bearer ${config.api_key}")
            contentType(ContentType.Application.Json)
            setBody(requestJson)
        }

        println("🔊 TTS 响应状态: ${response.status}")

        if (response.status.value != 200) {
            val errorBody = response.bodyAsText()
            println("❌ TTS API 失败: $errorBody")
            isPlaying = false
            return false
        }

        val responseText = response.bodyAsText()
        println("🔊 响应长度: ${responseText.length} 字符")

        return try {
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            val choices = responseJson["choices"]?.jsonArray
            val message = choices?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            val audio = message?.get("audio")?.jsonObject
            val audioDataBase64 = audio?.get("data")?.jsonPrimitive?.content

            if (audioDataBase64 != null) {
                println("🔊 收到音频数据: ${audioDataBase64.length} 字符 base64")
                val audioBytes = Base64.getDecoder().decode(audioDataBase64)
                println("🔊 解码后: ${audioBytes.size} 字节")

                if (shouldStop) {
                    isPlaying = false
                    return false
                }

                // WAV 格式可以直接用 AudioSystem 播放
                val success = playWavData(audioBytes)
                isPlaying = false
                return success
            } else {
                println("❌ 响应中未找到 audio.data 字段")
                println("🔊 [调试] 响应结构: ${responseText.take(500)}")
                isPlaying = false
                false
            }
        } catch (e: Exception) {
            println("❌ 解析非流式响应失败: ${e.message}")
            println("🔊 [调试] 响应内容: ${responseText.take(500)}")
            isPlaying = false
            false
        }
    }

    private suspend fun speakStream(text: String): Boolean {
        val requestBody = buildJsonObject {
            put("model", config.model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", config.voice_style)
                }
                addJsonObject {
                    put("role", "assistant")
                    put("content", text)
                }
            }
            putJsonObject("audio") {
                put("format", "pcm16")
            }
            put("stream", true)
        }

        val requestJson = requestBody.toString()
        println("🔊 TTS 流式请求: ${config.api_url}")

        val statement = client.preparePost(config.api_url) {
            header("api-key", config.api_key)
            header("Authorization", "Bearer ${config.api_key}")
            header("Content-Type", "application/json")
            header("Accept", "text/event-stream")
            setBody(requestJson)
        }

        return statement.execute { response ->
            println("🔊 TTS 流式响应状态: ${response.status}")

            if (response.status.value != 200) {
                val errorBody = response.bodyAsText()
                println("❌ TTS 流式 API 失败: $errorBody")
                isPlaying = false
                return@execute false
            }

            val allPcmData = ByteArrayOutputStream()
            val channel = response.bodyAsChannel()
            val lineBuffer = StringBuilder()
            var chunkCount = 0
            var totalAudioBytes = 0

            while (!channel.isClosedForRead && !shouldStop) {
                val buf = ByteArray(8192)
                val bytesRead = channel.readAvailable(buf, 0, buf.size)
                if (bytesRead <= 0) continue

                val decoded = String(buf, 0, bytesRead)
                lineBuffer.append(decoded)

                while (true) {
                    val lineEnd = findLineEnd(lineBuffer)
                    if (lineEnd == -1) break

                    val line = lineBuffer.substring(0, lineEnd).trim()
                    lineBuffer.delete(0, lineEnd + 1)

                    if (line.isEmpty()) continue
                    if (line == "data: [DONE]") break

                    if (line.startsWith("data: ")) {
                        val data = line.substring(6)
                        chunkCount++

                        try {
                            val chunkJson = json.parseToJsonElement(data).jsonObject
                            val choices = chunkJson["choices"]?.jsonArray
                            val delta = choices?.firstOrNull()?.jsonObject?.get("delta")?.jsonObject
                            val audio = delta?.get("audio")?.jsonObject
                            val audioData = audio?.get("data")?.jsonPrimitive?.content

                            if (audioData != null) {
                                val pcmBytes = Base64.getDecoder().decode(audioData)
                                allPcmData.write(pcmBytes)
                                totalAudioBytes += pcmBytes.size
                            } else if (chunkCount <= 3) {
                                println("🔊 [调试] chunk #$chunkCount: ${data.take(200)}")
                            }
                        } catch (e: Exception) {
                            if (chunkCount <= 3) {
                                println("⚠️ [调试] chunk #$chunkCount 解析失败: ${e.message}")
                            }
                        }
                    }
                }
            }

            println("🔊 流读取完毕: ${chunkCount} 个 chunk, 共 ${totalAudioBytes} 字节 PCM")

            if (totalAudioBytes == 0) {
                println("❌ 未收到任何音频数据")
                isPlaying = false
                return@execute false
            }

            val pcmData = allPcmData.toByteArray()
            if (shouldStop) {
                isPlaying = false
                return@execute false
            }

            val success = playPcmData(pcmData)
            isPlaying = false
            success
        }
    }

    private fun playWavData(wavBytes: ByteArray): Boolean {
        return try {
            val audioStream = AudioSystem.getAudioInputStream(wavBytes.inputStream())
            val format = audioStream.format
            println("🔊 WAV 格式: ${format.sampleRate}Hz, ${format.sampleSizeInBits}bit, ${format.channels}ch")

            val info = DataLine.Info(SourceDataLine::class.java, format)
            val line = AudioSystem.getLine(info) as SourceDataLine
            line.open(format)
            line.start()
            println("🔊 开始播放 WAV 音频...")

            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (audioStream.read(buffer).also { bytesRead = it } != -1 && !shouldStop) {
                line.write(buffer, 0, bytesRead)
                onAmplitudeUpdate?.invoke(computeAmplitudes(buffer, bytesRead, format.channels))
            }

            if (!shouldStop) {
                line.drain()
            }
            line.stop()
            line.close()
            audioStream.close()
            println("🔊 播放完成")
            true
        } catch (e: Exception) {
            println("❌ WAV 播放异常: ${e.message}, 尝试 PCM 方式...")
            playPcmData(wavBytes)
        }
    }

    private fun playPcmData(pcmData: ByteArray): Boolean {
        // 常见 TTS 采样率，从最可能的开始尝试
        val sampleRates = listOf(24000f, 16000f, 22050f, 44100f, 48000f)

        for (sampleRate in sampleRates) {
            val format = AudioFormat(sampleRate, 16, 1, true, false)
            val info = DataLine.Info(SourceDataLine::class.java, format)

            if (AudioSystem.isLineSupported(info)) {
                println("🔊 使用采样率: ${sampleRate}Hz, 数据量: ${pcmData.size} 字节, 时长约: ${pcmData.size / (sampleRate * 2)} 秒")
                return playWithFormat(pcmData, format)
            } else {
                println("⚠️ 采样率 ${sampleRate}Hz 不支持，尝试下一个...")
            }
        }

        println("❌ 所有采样率都不支持，尝试保存为 WAV 文件播放")
        return playAsWav(pcmData)
    }

    /**
     * 使用 SourceDataLine 播放
     */
    private fun playWithFormat(pcmData: ByteArray, format: AudioFormat): Boolean {
        var line: SourceDataLine? = null
        return try {
            line = AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine
            line.open(format)
            line.start()
            println("🔊 开始播放音频...")

            // 分块写入，以便能响应停止
            val chunkSize = 4096
            var offset = 0
            while (offset < pcmData.size && !shouldStop) {
                val len = minOf(chunkSize, pcmData.size - offset)
                line.write(pcmData, offset, len)
                onAmplitudeUpdate?.invoke(computeAmplitudes(pcmData.copyOfRange(offset, offset + len), len))
                offset += len
            }

            if (!shouldStop) {
                line.drain()
            }
            line.stop()
            line.close()
            println("🔊 播放完成")
            true
        } catch (e: Exception) {
            println("❌ 播放异常: ${e.message}")
            try { line?.close() } catch (_: Exception) {}
            false
        }
    }

    /**
     * 降级方案：保存为 WAV 文件然后用系统播放器打开
     */
    private fun playAsWav(pcmData: ByteArray): Boolean {
        return try {
            // 尝试多个采样率写 WAV
            val sampleRates = listOf(24000f, 16000f, 22050f)
            for (sampleRate in sampleRates) {
                try {
                    val format = AudioFormat(sampleRate, 16, 1, true, false)
                    val wavFile = File.createTempFile("tts_audio_", ".wav")
                    writeWavFile(wavFile, pcmData, format)
                    println("🔊 WAV 文件已保存: ${wavFile.absolutePath}")

                    // 尝试用系统默认播放器打开
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(wavFile)
                        println("🔊 已用系统播放器打开 WAV 文件")
                        // 等待播放
                        val durationMs = (pcmData.size / (sampleRate * 2) * 1000).toLong()
                        Thread.sleep(minOf(durationMs + 1000, 60000))
                        return true
                    }
                } catch (e: Exception) {
                    println("⚠️ WAV 采样率 ${sampleRate} 失败: ${e.message}")
                }
            }
            false
        } catch (e: Exception) {
            println("❌ WAV 降级播放失败: ${e.message}")
            false
        }
    }

    /**
     * 将 PCM 数据写入 WAV 文件
     */
    private fun writeWavFile(file: File, pcmData: ByteArray, format: AudioFormat) {
        val audioInputStream = AudioInputStream(
            pcmData.inputStream(),
            format,
            pcmData.size.toLong() / format.frameSize
        )
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file)
        audioInputStream.close()
    }

    /**
     * 在 StringBuilder 中查找行结束符 (\n 或 \r\n)
     */
    private fun findLineEnd(sb: StringBuilder): Int {
        for (i in 0 until sb.length) {
            if (sb[i] == '\n') return i
        }
        return -1
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

package com.lanzhou.qa.embedding

import com.lanzhou.qa.model.Vector
import com.lanzhou.qa.model.VectorMetadata
import com.lanzhou.qa.model.KnowledgeItem
import kotlin.math.sqrt

/**
 * 嵌入模型 - 将文本转换为向量
 * 使用TF-IDF + 词频统计的简单方法，避免外部依赖
 */
class EmbeddingModel(private val dimension: Int = 384) {

    private val vocabulary = mutableSetOf<String>()
    private val idfCache = mutableMapOf<String, Double>()

    /**
     * 构建词汇表（预处理阶段）
     */
    fun buildVocabulary(items: List<KnowledgeItem>) {
        items.forEach { item ->
            vocabulary.addAll(tokenize(item.question))
            vocabulary.addAll(tokenize(item.answer))
        }
        // 计算IDF
        calculateIDF(items)
    }

    /**
     * 文本分词
     */
    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[\\s\\p{Punct}]+"), " ")
            .split(" ")
            .filter { it.isNotBlank() && it.length > 1 }
    }

    /**
     * 计算IDF
     */
    private fun calculateIDF(items: List<KnowledgeItem>) {
        val docCount = items.size.toDouble()
        val termDocCount = mutableMapOf<String, Int>()

        items.forEach { item ->
            val terms = tokenize(item.question + " " + item.answer).toSet()
            terms.forEach { term ->
                termDocCount[term] = termDocCount.getOrDefault(term, 0) + 1
            }
        }

        termDocCount.forEach { (term, count) ->
            idfCache[term] = Math.log(docCount / count)
        }
    }

    /**
     * 将文本转换为向量
     */
    fun embed(text: String): Vector {
        val tokens = tokenize(text)
        val tf = mutableMapOf<String, Double>()

        // 计算TF
        tokens.forEach { token ->
            tf[token] = tf.getOrDefault(token, 0.0) + 1.0
        }

        // 归一化TF
        val maxTf = tf.values.maxOrNull() ?: 1.0
        tf.replaceAll { _, v -> v / maxTf }

        // 构建向量（TF-IDF）
        val vector = DoubleArray(dimension) { 0.0 }

        tokens.forEachIndexed { index, token ->
            if (index < dimension) {
                val tfValue = tf[token] ?: 0.0
                val idfValue = idfCache[token] ?: 0.0
                vector[index] = tfValue * idfValue
            }
        }

        // 归一化向量
        val norm = sqrt(vector.sumOf { it * it })
        val normalizedVector = if (norm > 0) {
            vector.map { it / norm }
        } else {
            vector.toList()
        }

        return Vector(normalizedVector, VectorMetadata(0, "", "", ""))
    }

    /**
     * 将知识项转换为向量
     */
    fun embed(item: KnowledgeItem): Vector {
        val text = "${item.question} ${item.answer}"
        val vector = embed(text)
        return vector.copy(metadata = VectorMetadata(item.id, item.question, item.answer, item.category))
    }

    /**
     * 计算余弦相似度
     */
    fun cosineSimilarity(v1: Vector, v2: Vector): Double {
        var sum = 0.0
        for (i in v1.values.indices) {
            sum += v1.values[i] * v2.values[i]
        }
        return sum
    }
}

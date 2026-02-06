package com.lanzhou.qa.rag

import com.lanzhou.qa.embedding.EmbeddingModel
import com.lanzhou.qa.model.KnowledgeItem
import com.lanzhou.qa.model.RetrievalResult
import com.lanzhou.qa.model.Vector

/**
 * RAG检索器 - 基于向量相似度检索相关知识
 */
class RAGRetriever(
    private val knowledgeItems: List<KnowledgeItem>,
    private val embeddingModel: EmbeddingModel,
    private val topK: Int = 5
) {

    private val knowledgeVectors: List<Vector>

    init {
        // 预处理：将所有知识项转换为向量
        embeddingModel.buildVocabulary(knowledgeItems)
        knowledgeVectors = knowledgeItems.map { item ->
            embeddingModel.embed(item)
        }
    }

    /**
     * 检索与问题最相关的知识
     */
    fun retrieve(question: String): List<RetrievalResult> {
        val questionVector = embeddingModel.embed(question)

        // 计算所有知识项的相似度
        val similarities = knowledgeVectors.map { knowledgeVector ->
            val similarity = embeddingModel.cosineSimilarity(questionVector, knowledgeVector)
            RetrievalResult(
                item = KnowledgeItem(
                    id = knowledgeVector.metadata.id,
                    question = knowledgeVector.metadata.question,
                    answer = knowledgeVector.metadata.answer,
                    category = knowledgeVector.metadata.category
                ),
                similarity = similarity
            )
        }

        // 按相似度排序，返回TopK
        return similarities
            .sortedByDescending { it.similarity }
            .take(topK)
    }

    /**
     * 构建上下文文本
     */
    fun buildContext(retrievalResults: List<RetrievalResult>): String {
        if (retrievalResults.isEmpty()) {
            return "知识库中暂无相关信息。"
        }

        return buildString {
            appendLine("=== 相关知识 ===")
            retrievalResults.forEachIndexed { index, result ->
                appendLine("${index + 1}. 【${result.item.category}】${result.item.question}")
                appendLine("   答案：${result.item.answer}")
                appendLine("   相似度：${String.format("%.3f", result.similarity)}")
                appendLine()
            }
        }
    }

    /**
     * 构建RAG提示词
     */
    fun buildPrompt(question: String, context: String, systemPrompt: String): String {
        return """
            $systemPrompt

            === 知识上下文 ===
            $context

            === 用户问题 ===
            $question

            === 要求 ===
            1. 请基于知识上下文回答用户问题
            2. 回答要准确、专业、友好
            3. 如果知识库中没有相关信息，请基于你的专业知识提供回答
            4. 回答请使用中文
            5. 请详细解释，不要过于简短
        """.trimIndent()
    }
}

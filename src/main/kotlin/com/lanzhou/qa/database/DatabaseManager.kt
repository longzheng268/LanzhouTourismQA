package com.lanzhou.qa.database

import com.lanzhou.qa.config.QAPair
import com.lanzhou.qa.config.ChatHistory
import com.lanzhou.qa.model.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.SQLException

/**
 * 数据库管理器
 * 与SQL脚本 setup_database_fixed.sql 完全兼容
 *
 * 数据库结构：
 * - 数据库名: qa_database
 * - 表1: qa_pairs (id, question, answer, created_at)
 * - 表2: chat_history (id, question, answer, timestamp)
 */
class DatabaseManager(private val config: DatabaseConfig) {

    private var dataSource: HikariDataSource? = null
    private var initialized = false

    /**
     * 初始化数据库连接
     * 支持MySQL和MariaDB
     */
    fun initialize(): Boolean {
        if (!config.enabled) {
            println("数据库模式未启用，使用本地JSON")
            return false
        }

        return try {
            val hikariConfig = HikariConfig().apply {
                // 尝试MariaDB驱动（优先）
                try {
                    Class.forName("org.mariadb.jdbc.Driver")
                    jdbcUrl = "jdbc:mariadb://${config.host}:${config.port}/${config.database}?useSSL=false"
                    driverClassName = "org.mariadb.jdbc.Driver"
                    println("ℹ️ 使用MariaDB驱动")
                } catch (e: ClassNotFoundException) {
                    // MariaDB驱动不存在，尝试MySQL驱动
                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver")
                        jdbcUrl = "jdbc:mysql://${config.host}:${config.port}/${config.database}?useSSL=false&serverTimezone=UTC"
                        driverClassName = "com.mysql.cj.jdbc.Driver"
                        println("ℹ️ 使用MySQL驱动")
                    } catch (e2: ClassNotFoundException) {
                        println("❌ 未找到数据库驱动，请确保MariaDB或MySQL驱动已添加到classpath")
                        return false
                    }
                }

                username = config.username
                password = config.password

                // 连接池配置
                maximumPoolSize = config.maximumPoolSize
                minimumIdle = config.minimumIdle
                connectionTimeout = config.connectionTimeout.toLong()
                idleTimeout = 600000
                maxLifetime = 1800000

                // 性能优化
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")
            }

            dataSource = HikariDataSource(hikariConfig)
            initialized = true
            println("✅ 数据库连接成功: ${config.host}:${config.port}/${config.database}")
            true
        } catch (e: Exception) {
            println("❌ 数据库连接失败: ${e.message}")
            e.printStackTrace()
            initialized = false
            false
        }
    }

    /**
     * 获取数据库连接
     */
    fun getConnection(): Connection? {
        if (!initialized || !config.enabled) return null
        return try {
            dataSource?.connection
        } catch (e: SQLException) {
            println("❌ 获取连接失败: ${e.message}")
            null
        }
    }

    /**
     * 测试数据库连接
     */
    fun testConnection(): Boolean {
        return try {
            getConnection()?.use { conn ->
                val stmt = conn.createStatement()
                stmt.use {
                    it.executeQuery("SELECT 1").use { rs ->
                        rs.next()
                    }
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 连接测试失败: ${e.message}")
            false
        }
    }

    /**
     * 查询所有问答对（与SQL脚本中的qa_pairs表兼容）
     */
    fun getAllQAPairs(): List<QAPair> {
        if (!initialized || !config.enabled) return emptyList()

        val qaPairs = mutableListOf<QAPair>()
        val sql = "SELECT id, question, answer FROM qa_database.qa_pairs ORDER BY id"

        try {
            getConnection()?.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery(sql).use { rs ->
                        while (rs.next()) {
                            qaPairs.add(
                                QAPair(
                                    id = rs.getInt("id"),
                                    question = rs.getString("question"),
                                    answer = rs.getString("answer")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ 查询qa_pairs失败: ${e.message}")
        }

        return qaPairs
    }

    /**
     * 查询指定数量的问答对（用于分页或限制）
     */
    fun getQAPairs(limit: Int): List<QAPair> {
        if (!initialized || !config.enabled) return emptyList()

        val qaPairs = mutableListOf<QAPair>()
        val sql = "SELECT id, question, answer FROM qa_database.qa_pairs ORDER BY id LIMIT ?"

        try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setInt(1, limit)
                    pstmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            qaPairs.add(
                                QAPair(
                                    id = rs.getInt("id"),
                                    question = rs.getString("question"),
                                    answer = rs.getString("answer")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ 查询qa_pairs(limit)失败: ${e.message}")
        }

        return qaPairs
    }

    /**
     * 搜索问答对（根据关键词）
     */
    fun searchQAPairs(keyword: String): List<QAPair> {
        if (!initialized || !config.enabled) return emptyList()

        val qaPairs = mutableListOf<QAPair>()
        val sql = """
            SELECT id, question, answer
            FROM qa_database.qa_pairs
            WHERE question LIKE ? OR answer LIKE ?
            ORDER BY id
        """.trimIndent()

        try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    val pattern = "%$keyword%"
                    pstmt.setString(1, pattern)
                    pstmt.setString(2, pattern)
                    pstmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            qaPairs.add(
                                QAPair(
                                    id = rs.getInt("id"),
                                    question = rs.getString("question"),
                                    answer = rs.getString("answer")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ 搜索qa_pairs失败: ${e.message}")
        }

        return qaPairs
    }

    /**
     * 保存聊天历史（与SQL脚本中的chat_history表兼容）
     */
    fun saveChatHistory(question: String, answer: String): Boolean {
        if (!initialized || !config.enabled) return false

        val sql = "INSERT INTO qa_database.chat_history (question, answer) VALUES (?, ?)"

        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, question)
                    pstmt.setString(2, answer)
                    pstmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 保存聊天历史失败: ${e.message}")
            false
        }
    }

    /**
     * 查询聊天历史
     */
    fun getChatHistory(limit: Int = 50): List<ChatHistory> {
        if (!initialized || !config.enabled) return emptyList()

        val history = mutableListOf<ChatHistory>()
        val sql = """
            SELECT id, question, answer, timestamp
            FROM qa_database.chat_history
            ORDER BY timestamp DESC
            LIMIT ?
        """.trimIndent()

        try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setInt(1, limit)
                    pstmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            history.add(
                                ChatHistory(
                                    id = rs.getInt("id"),
                                    question = rs.getString("question"),
                                    answer = rs.getString("answer"),
                                    timestamp = rs.getTimestamp("timestamp").toString()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ 查询聊天历史失败: ${e.message}")
        }

        return history
    }

    /**
     * 插入示例数据（用于初始化）
     */
    fun insertSampleData(): Boolean {
        if (!initialized || !config.enabled) return false

        val sql = """
            INSERT IGNORE INTO qa_database.qa_pairs (question, answer)
            VALUES (?, ?)
        """.trimIndent()

        val samples = listOf(
            Pair("什么是继续教育？", "继续教育是指已经脱离正规教育，已参加工作和负有成人责任的人所接受的各种各样的教育。"),
            Pair("继续教育的目的是什么？", "继续教育的主要目的是更新知识、提高技能、适应职业变化、促进个人发展。"),
            Pair("继续教育有哪些形式？", "继续教育包括培训课程、在线学习、研讨会、工作坊、自学等多种形式。")
        )

        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    samples.forEach { (question, answer) ->
                        pstmt.setString(1, question)
                        pstmt.setString(2, answer)
                        pstmt.addBatch()
                    }
                    val results = pstmt.executeBatch()
                    results.sum() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 插入示例数据失败: ${e.message}")
            false
        }
    }

    /**
     * 获取统计信息
     */
    fun getStats(): Map<String, Int> {
        if (!initialized || !config.enabled) return emptyMap()

        val stats = mutableMapOf<String, Int>()

        try {
            getConnection()?.use { conn ->
                // qa_pairs数量
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT COUNT(*) as count FROM qa_database.qa_pairs").use { rs ->
                        if (rs.next()) {
                            stats["qa_pairs_count"] = rs.getInt("count")
                        }
                    }
                }

                // chat_history数量
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT COUNT(*) as count FROM qa_database.chat_history").use { rs ->
                        if (rs.next()) {
                            stats["chat_history_count"] = rs.getInt("count")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ 获取统计信息失败: ${e.message}")
        }

        return stats
    }

    /**
     * 关闭数据库连接
     */
    fun close() {
        try {
            dataSource?.close()
            initialized = false
            println("✅ 数据库连接已关闭")
        } catch (e: Exception) {
            println("❌ 关闭数据库连接失败: ${e.message}")
        }
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized && config.enabled
}

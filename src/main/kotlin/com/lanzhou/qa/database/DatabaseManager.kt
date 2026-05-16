package com.lanzhou.qa.database

import com.lanzhou.qa.config.QAPair
import com.lanzhou.qa.config.ChatHistory
import com.lanzhou.qa.config.User
import com.lanzhou.qa.config.UserRole
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
                    jdbcUrl = "jdbc:mariadb://${config.host}:${config.port}/${config.database}?useSSL=false&allowPublicKeyRetrieval=true"
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
            // 自动创建用户表并初始化管理员
            ensureUsersTable()
            initDefaultAdmin()
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
        if (!config.enabled) {
            println("❌ 数据库未启用，请在 config.json 中设置 database.enabled = true")
            return false
        }

        if (!initialized) {
            println("⚠️ 数据库未初始化，正在尝试连接...")
            val result = initialize()
            if (!result) {
                return false
            }
        }

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

        // 先尝试带 category 查询，失败则回退到不带 category
        val sqlWithCategory = "SELECT id, question, answer, category FROM qa_database.qa_pairs ORDER BY id"
        val sqlWithoutCategory = "SELECT id, question, answer FROM qa_database.qa_pairs ORDER BY id"

        try {
            getConnection()?.use { conn ->
                conn.createStatement().use { stmt ->
                    try {
                        stmt.executeQuery(sqlWithCategory).use { rs ->
                            while (rs.next()) {
                                qaPairs.add(
                                    QAPair(
                                        id = rs.getInt("id"),
                                        question = rs.getString("question"),
                                        answer = rs.getString("answer"),
                                        category = rs.getString("category") ?: ""
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {
                        // category 列不存在，回退
                        stmt.executeQuery(sqlWithoutCategory).use { rs ->
                            while (rs.next()) {
                                qaPairs.add(
                                    QAPair(
                                        id = rs.getInt("id"),
                                        question = rs.getString("question"),
                                        answer = rs.getString("answer"),
                                        category = ""
                                    )
                                )
                            }
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
     * 从JSON知识库同步数据到数据库
     */
    fun syncFromKnowledgeBase(items: List<com.lanzhou.qa.model.KnowledgeItem>): Int {
        if (!initialized || !config.enabled) return 0

        var synced = 0
        // 确保 category 列存在（兼容 MySQL 8 不支持 IF NOT EXISTS 的情况）
        try {
            getConnection()?.use { conn ->
                val hasCategoryColumn = try {
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT category FROM qa_database.qa_pairs LIMIT 1").close()
                    }
                    true
                } catch (_: Exception) { false }

                if (!hasCategoryColumn) {
                    conn.createStatement().use { stmt ->
                        stmt.executeUpdate("ALTER TABLE qa_database.qa_pairs ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT ''")
                    }
                }
            }
        } catch (_: Exception) {}

        val sql = "REPLACE INTO qa_database.qa_pairs (id, question, answer, category) VALUES (?, ?, ?, ?)"

        try {
            getConnection()?.use { conn ->
                conn.autoCommit = false
                conn.prepareStatement(sql).use { pstmt ->
                    for ((index, item) in items.withIndex()) {
                        pstmt.setInt(1, item.id)
                        pstmt.setString(2, item.question)
                        pstmt.setString(3, item.answer)
                        pstmt.setString(4, item.category)
                        pstmt.addBatch()
                        synced++
                        if ((index + 1) % 500 == 0) {
                            pstmt.executeBatch()
                        }
                    }
                    pstmt.executeBatch()
                }
                conn.commit()
                conn.autoCommit = true
            }
            println("✅ 同步了 $synced 条知识到数据库（含分类信息）")
        } catch (e: Exception) {
            println("❌ 同步知识库到数据库失败: ${e.message}")
            synced = 0
        }

        return synced
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

    // ==================== 用户管理 ====================

    /**
     * 确保 users 表存在
     */
    fun ensureUsersTable(): Boolean {
        if (!initialized || !config.enabled) return false

        val sql = """
            CREATE TABLE IF NOT EXISTS qa_database.users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password VARCHAR(100) NOT NULL,
                role VARCHAR(20) NOT NULL DEFAULT 'TOURIST',
                enabled BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        return try {
            getConnection()?.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(sql)
                }
            }
            println("✅ users 表就绪")
            // 兼容旧表：自动补全 enabled 列
            try {
                getConnection()?.use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT enabled FROM qa_database.users LIMIT 1").close()
                    }
                }
            } catch (_: Exception) {
                try {
                    getConnection()?.use { conn ->
                        conn.createStatement().use { stmt ->
                            stmt.executeUpdate("ALTER TABLE qa_database.users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE")
                        }
                    }
                    println("✅ 已补全 users.enabled 列")
                } catch (e: Exception) {
                    println("⚠️ 补全 enabled 列失败: ${e.message}")
                }
            }
            true
        } catch (e: Exception) {
            println("❌ 创建 users 表失败: ${e.message}")
            false
        }
    }

    /**
     * 初始化默认超级管理员账户
     */
    fun initDefaultAdmin(): Boolean {
        if (!initialized || !config.enabled) return false

        val sql = "INSERT IGNORE INTO qa_database.users (username, password, role) VALUES (?, ?, ?)"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, "superadmin")
                    pstmt.setString(2, "111111")
                    pstmt.setString(3, UserRole.SUPER_ADMIN.name)
                    pstmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 初始化默认管理员失败: ${e.message}")
            false
        }
    }

    /**
     * 注册用户
     */
    fun registerUser(username: String, password: String, role: UserRole): Pair<Boolean, String> {
        if (!initialized || !config.enabled) return Pair(false, "数据库未启用")

        if (username.isBlank() || password.isBlank()) {
            return Pair(false, "用户名和密码不能为空")
        }
        if (username.length < 2 || username.length > 50) {
            return Pair(false, "用户名长度需在2-50之间")
        }
        if (password.length < 6) {
            return Pair(false, "密码长度不能少于6位")
        }

        // 检查用户名是否已存在
        val checkSql = "SELECT COUNT(*) FROM qa_database.users WHERE username = ?"
        try {
            getConnection()?.use { conn ->
                conn.prepareStatement(checkSql).use { pstmt ->
                    pstmt.setString(1, username)
                    pstmt.executeQuery().use { rs ->
                        if (rs.next() && rs.getInt(1) > 0) {
                            return Pair(false, "用户名已存在")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return Pair(false, "查询用户失败: ${e.message}")
        }

        val sql = "INSERT INTO qa_database.users (username, password, role) VALUES (?, ?, ?)"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, username)
                    pstmt.setString(2, password)
                    pstmt.setString(3, role.name)
                    val result = pstmt.executeUpdate() > 0
                    if (result) {
                        println("✅ 用户注册成功: $username (${role.label})")
                        Pair(true, "注册成功")
                    } else {
                        Pair(false, "注册失败")
                    }
                }
            } ?: Pair(false, "数据库连接失败")
        } catch (e: Exception) {
            Pair(false, "注册失败: ${e.message}")
        }
    }

    /**
     * 用户登录验证
     */
    fun loginUser(username: String, password: String): User? {
        if (!initialized || !config.enabled) return null

        val sql = "SELECT id, username, role, created_at FROM qa_database.users WHERE username = ? AND password = ?"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, username)
                    pstmt.setString(2, password)
                    pstmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            User(
                                id = rs.getInt("id"),
                                username = rs.getString("username"),
                                role = try {
                                    UserRole.valueOf(rs.getString("role"))
                                } catch (_: Exception) {
                                    UserRole.TOURIST
                                },
                                createdAt = rs.getTimestamp("created_at")?.toString() ?: ""
                            )
                        } else {
                            null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ 登录失败: ${e.message}")
            null
        }
    }

    /**
     * 获取所有用户
     */
    fun getAllUsers(): List<User> {
        if (!initialized || !config.enabled) return emptyList()

        val users = mutableListOf<User>()
        // 先尝试带 enabled 查询，失败则回退
        val sqlWithEnabled = "SELECT id, username, role, enabled, created_at FROM qa_database.users ORDER BY id"
        val sqlWithoutEnabled = "SELECT id, username, role, created_at FROM qa_database.users ORDER BY id"
        try {
            getConnection()?.use { conn ->
                conn.createStatement().use { stmt ->
                    try {
                        stmt.executeQuery(sqlWithEnabled).use { rs ->
                            while (rs.next()) {
                                users.add(
                                    User(
                                        id = rs.getInt("id"),
                                        username = rs.getString("username"),
                                        role = try { UserRole.valueOf(rs.getString("role")) } catch (_: Exception) { UserRole.TOURIST },
                                        enabled = rs.getBoolean("enabled"),
                                        createdAt = rs.getTimestamp("created_at")?.toString() ?: ""
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {
                        stmt.executeQuery(sqlWithoutEnabled).use { rs ->
                            while (rs.next()) {
                                users.add(
                                    User(
                                        id = rs.getInt("id"),
                                        username = rs.getString("username"),
                                        role = try { UserRole.valueOf(rs.getString("role")) } catch (_: Exception) { UserRole.TOURIST },
                                        enabled = true,
                                        createdAt = rs.getTimestamp("created_at")?.toString() ?: ""
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ 查询用户列表失败: ${e.message}")
        }
        return users
    }

    /**
     * 删除用户
     */
    fun deleteUser(userId: Int): Boolean {
        if (!initialized || !config.enabled) return false

        val sql = "DELETE FROM qa_database.users WHERE id = ?"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setInt(1, userId)
                    pstmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 删除用户失败: ${e.message}")
            false
        }
    }

    /**
     * 修改用户角色
     */
    fun updateUserRole(userId: Int, newRole: UserRole): Boolean {
        if (!initialized || !config.enabled) return false

        val sql = "UPDATE qa_database.users SET role = ? WHERE id = ?"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, newRole.name)
                    pstmt.setInt(2, userId)
                    pstmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 修改用户角色失败: ${e.message}")
            false
        }
    }

    /**
     * 禁用用户
     */
    fun disableUser(userId: Int): Boolean {
        if (!initialized || !config.enabled) return false
        val sql = "UPDATE qa_database.users SET enabled = FALSE WHERE id = ?"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setInt(1, userId)
                    pstmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 禁用用户失败: ${e.message}")
            false
        }
    }

    /**
     * 启用用户
     */
    fun enableUser(userId: Int): Boolean {
        if (!initialized || !config.enabled) return false
        val sql = "UPDATE qa_database.users SET enabled = TRUE WHERE id = ?"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setInt(1, userId)
                    pstmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 启用用户失败: ${e.message}")
            false
        }
    }

    /**
     * 重置用户密码
     */
    fun resetPassword(userId: Int, newPassword: String): Boolean {
        if (!initialized || !config.enabled) return false
        if (newPassword.length < 6) return false
        val sql = "UPDATE qa_database.users SET password = ? WHERE id = ?"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, newPassword)
                    pstmt.setInt(2, userId)
                    pstmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 重置密码失败: ${e.message}")
            false
        }
    }

    // ==================== 知识库管理 ====================

    /**
     * 新增知识条目
     */
    fun insertQAPair(question: String, answer: String, category: String): Boolean {
        if (!initialized || !config.enabled) return false
        val sql = "INSERT INTO qa_database.qa_pairs (question, answer, category) VALUES (?, ?, ?)"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, question)
                    pstmt.setString(2, answer)
                    pstmt.setString(3, category)
                    pstmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 新增知识条目失败: ${e.message}")
            false
        }
    }

    /**
     * 编辑知识条目
     */
    fun updateQAPair(id: Int, question: String, answer: String, category: String): Boolean {
        if (!initialized || !config.enabled) return false
        val sql = "UPDATE qa_database.qa_pairs SET question = ?, answer = ?, category = ? WHERE id = ?"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, question)
                    pstmt.setString(2, answer)
                    pstmt.setString(3, category)
                    pstmt.setInt(4, id)
                    pstmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 编辑知识条目失败: ${e.message}")
            false
        }
    }

    /**
     * 删除知识条目
     */
    fun deleteQAPair(id: Int): Boolean {
        if (!initialized || !config.enabled) return false
        val sql = "DELETE FROM qa_database.qa_pairs WHERE id = ?"
        return try {
            getConnection()?.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setInt(1, id)
                    pstmt.executeUpdate() > 0
                }
            } ?: false
        } catch (e: Exception) {
            println("❌ 删除知识条目失败: ${e.message}")
            false
        }
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

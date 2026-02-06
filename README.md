# 兰州旅游知识问答系统 (RAG版)

基于Kotlin + Compose Desktop开发的RAG知识问答系统，专为兰州旅游知识问答设计。

## 🚀 快速开始

### 1. 配置API密钥

复制示例配置文件：
```bash
cp src/main/resources/config.json.example src/main/resources/config.json
```

然后编辑 `src/main/resources/config.json` 文件，填入你的API密钥：
```json
{
  "api": {
    "api_key": "YOUR_API_KEY_HERE"  // 替换为你的API密钥
  }
}
```

### 2. 运行项目

**开发模式**（推荐）：
```bash
./gradlew run
```

**构建并运行**：
```bash
./gradlew build
./gradlew run
```

## 🎯 项目特点

- ✅ RAG架构（检索增强生成）
- ✅ 本地知识库（25条兰州旅游知识）
- ✅ TF-IDF向量化（无需外部API）
- ✅ Material Design 3界面
- ✅ **多平台支持**：Windows、Linux、macOS
- ✅ **双模式支持**：数据库 + 本地JSON
- ✅ **聊天历史**：自动保存到数据库

## 📁 项目结构

```
LanzhouTourismQA/
├── src/main/kotlin/          # Kotlin源代码
├── src/main/resources/       # 配置和知识库
│   ├── config.json.example   # 配置文件模板
│   └── knowledge_base.json   # 本地知识库
├── build.gradle.kts          # Gradle构建配置
├── .gitignore               # Git忽略文件
├── SETUP.md                 # 配置说明文档
├── README.md                # 本文件
└── 使用文档.md              # 完整使用指南
```

## 🔧 常用命令

| 命令 | 说明 |
|------|------|
| `./gradlew run` | 运行程序 |
| `./gradlew build` | 构建项目 |
| `./gradlew clean` | 清理构建缓存 |

## 📚 更多信息

- 配置说明：`SETUP.md`
- 详细使用指南：`使用文档.md`
- 数据库配置：`数据库初始化说明.md`
- 技术架构：查看`使用文档.md`的"技术架构"章节
- 故障排除：查看`使用文档.md`的"故障排除"章节

## 🗄️ 数据库模式

**默认**：JSON模式（无需配置，开箱即用）

**可选**：数据库模式（需MariaDB服务器）
- 修改 `config.json` 启用数据库
- 支持数据持久化
- 自动保存聊天历史
- 与C++版本数据库完全兼容

## 🔒 安全说明

- **API密钥保护**：`.gitignore` 已配置，确保 `config.json` 不会被提交到版本控制
- **数据库密码**：建议使用环境变量存储敏感信息
- **配置模板**：使用 `config.json.example` 作为配置模板

---

**版本**：1.0.0
**状态**：✅ 完成并可直接使用
**最后更新**：2026-02-06

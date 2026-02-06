# 项目配置说明

## 🔧 配置步骤

### 1. 配置API密钥

复制示例配置文件：
```bash
cp src/main/resources/config.json.example src/main/resources/config.json
```

然后编辑 `src/main/resources/config.json` 文件，填入你的API密钥：

```json
{
  "api": {
    "api_url": "https://api.xiaomimimo.com/v1/chat/completions",
    "api_key": "YOUR_API_KEY_HERE",  // 替换为你的API密钥
    "model": "mimo-v2-flash",
    "temperature": 0.7,
    "max_tokens": 1000
  }
}
```

### 2. 数据库配置（可选）

默认使用本地JSON模式，无需数据库配置。

如果需要使用数据库模式，请修改配置文件中的数据库部分：

```json
{
  "database": {
    "enabled": true,  // 启用数据库模式
    "host": "localhost",
    "port": 3306,
    "database": "qa_database",
    "username": "root",
    "password": "YOUR_DATABASE_PASSWORD",
    "maximumPoolSize": 10,
    "minimumIdle": 2,
    "connectionTimeout": 30000
  }
}
```

### 3. 运行项目

**开发模式**：
```bash
./gradlew run
```

**构建并运行**：
```bash
./gradlew build
./gradlew run
```

## ⚠️ 安全注意事项

1. **不要提交敏感信息**：`.gitignore` 文件已配置，确保 `config.json` 不会被提交到版本控制
2. **使用环境变量**（可选）：对于生产环境，建议使用环境变量存储敏感信息
3. **定期更新API密钥**：定期轮换API密钥以提高安全性

## 📁 文件说明

- `config.json.example` - 配置文件模板（示例）
- `config.json` - 实际配置文件（需要手动创建，不会被版本控制）
- `.gitignore` - 版本控制忽略文件列表

## 🔍 故障排除

### API连接失败
- 检查API密钥是否正确
- 确认网络连接正常
- 验证API地址是否可访问

### 数据库连接失败
- 确认数据库服务正在运行
- 检查数据库配置（主机、端口、用户名、密码）
- 确认数据库已创建并包含必要的表

### Skiko库加载失败
- 确保系统满足最低要求
- 检查Java版本（推荐Java 20+）
- 清理构建缓存：`./gradlew clean`

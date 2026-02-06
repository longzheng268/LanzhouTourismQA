# 隐私保护说明

本项目已采取以下措施保护敏感信息：

## 🔒 已实施的隐私保护措施

### 1. Git忽略配置 (.gitignore)
- ✅ `src/main/resources/config.json` 已被忽略
- ✅ 所有包含敏感信息的文件都被排除在版本控制之外
- ✅ API密钥、数据库密码等凭证文件不会被提交

### 2. 配置文件分离
- ✅ `config.json.example` - 配置文件模板（已提交到仓库）
- ✅ `config.json` - 实际配置文件（被.gitignore忽略）

### 3. 敏感信息替换
原始配置文件中的敏感信息已替换为占位符：
```json
{
  "api": {
    "api_key": "YOUR_API_KEY_HERE"  // 替换为你的API密钥
  },
  "database": {
    "password": "YOUR_DATABASE_PASSWORD"  // 替换为你的数据库密码
  }
}
```

## 📋 如何配置项目

### 第一次使用
```bash
# 1. 复制配置文件模板
cp src/main/resources/config.json.example src/main/resources/config.json

# 2. 编辑配置文件，填入你的API密钥和数据库密码
# 使用文本编辑器打开 src/main/resources/config.json

# 3. 运行项目
./gradlew run
```

### 更新配置
如果你需要更新配置：
```bash
# 直接编辑配置文件
# 或者重新复制模板并填入新配置
```

## ⚠️ 重要提醒

### 不要提交敏感信息
- **永远不要**将 `config.json` 提交到版本控制
- **永远不要**将 `.env` 文件提交到版本控制
- **永远不要**在代码中硬编码API密钥或密码

### 安全最佳实践
1. **使用环境变量**（推荐用于生产环境）：
   ```bash
   export API_KEY="your-api-key-here"
   export DB_PASSWORD="your-db-password-here"
   ```

2. **定期轮换密钥**：
   - 定期更换API密钥
   - 定期更换数据库密码

3. **使用.gitignore**：
   - 确保所有敏感文件都被正确忽略
   - 定期检查.gitignore配置

## 🚨 如果意外提交了敏感信息

如果意外将敏感信息提交到了版本控制：

1. **立即撤销提交**：
   ```bash
   git reset HEAD~1
   ```

2. **从Git历史中删除敏感信息**（如果已经推送到远程）：
   ```bash
   git filter-branch --force --index-filter \
     'git rm --cached --ignore-unmatch src/main/resources/config.json' \
     --prune-empty --tag-name-filter cat -- --all
   ```

3. **更改所有相关密钥**：
   - 更改API密钥
   - 更改数据库密码
   - 更改所有相关凭证

## 📞 安全问题报告

如果发现安全问题或隐私泄露，请立即：
1. 更改相关密钥
2. 联系项目维护者
3. 在GitHub上创建安全问题（如果适用）

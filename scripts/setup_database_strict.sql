-- ============================================
-- 兰州旅游知识问答系统 - 数据库初始化脚本
-- 语法严格版 - 避免重复创建表
-- ============================================

-- 设置严格模式（包含所有严格模式选项）
SET SQL_MODE = 'STRICT_TRANS_TABLES,STRICT_ALL_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS qa_database
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 切换到数据库
USE qa_database;

-- 删除旧表（如果存在）以确保使用最新结构
DROP TABLE IF EXISTS chat_history;
DROP TABLE IF EXISTS qa_pairs;

-- 创建 qa_pairs 表（知识库）- 包含 category 列
CREATE TABLE qa_pairs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_question (question(255)),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建 chat_history 表（聊天历史）
CREATE TABLE chat_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入基础兰州旅游知识数据（使用 INSERT IGNORE 避免重复）
-- 注意：完整的2000条知识库数据通过应用程序启动时自动从 knowledge_base.json 同步到数据库
-- 以下仅为初始化基础数据，确保数据库不为空
INSERT IGNORE INTO qa_pairs (id, question, answer, category) VALUES
(1, '兰州在哪里？', '兰州是甘肃省省会，位于中国西北部，黄河上游。地理坐标为东经103°30''，北纬36°03''，是丝绸之路重镇。', '地理'),
(2, '兰州有什么著名景点？', '兰州著名景点包括：1) 白塔山公园 - 黄河岸边的标志性景点；2) 兰山公园 - 俯瞰兰州全景；3) 五泉山公园 - 因五眼泉水得名；4) 黄河铁桥（中山桥）- 百年历史；5) 水车博览园 - 展示古代灌溉技术；6) 甘肃省博物馆 - 藏有丰富文物。', '景点'),
(3, '兰州牛肉面有什么特点？', '兰州牛肉面讲究"一清二白三红四绿五黄"：汤清（一清）、萝卜白（二白）、辣椒油红（三红）、香菜蒜苗绿（四绿）、面条黄亮（五黄）。面条有多种粗细可选：毛细、细、二细、韭叶、宽面等。', '美食'),
(4, '兰州的最佳旅游时间是什么时候？', '兰州最佳旅游时间是5-10月。夏季（6-8月）气候凉爽，是避暑胜地；秋季（9-10月）天高气爽，瓜果飘香。春季多风沙，冬季寒冷干燥。', '地理'),
(5, '如何到达兰州？', '1) 飞机：兰州中川国际机场，有国内外航线；2) 高铁：兰州西站、兰州站，连接全国高铁网；3) 普通火车：兰州站；4) 自驾：连霍高速、京藏高速等多条高速交汇。', '交通'),
(6, '兰州有哪些特色美食？', '兰州特色美食：1) 兰州牛肉面（必吃）；2) 手抓羊肉；3) 灰豆子（甜品）；4) 酿皮子；5) 热冬果；6) 牛奶鸡蛋醪糟；7) 烤羊肉串；8) 浆水面。', '美食'),
(7, '兰州黄河铁桥的历史？', '黄河铁桥（中山桥）建于1909年，是黄河上第一座铁桥，由德国商人承建。桥长234米，宽7.5米。初名"黄河铁桥"，1942年为纪念孙中山先生改名"中山桥"。是全国重点文物保护单位。', '历史'),
(8, '兰州的气候特点是什么？', '兰州属于温带大陆性气候，特点：1) 冬季寒冷干燥，夏季凉爽；2) 昼夜温差大；3) 降水少，蒸发强；4) 日照充足；5) 多风沙。年均气温10℃左右，7月最热，1月最冷。', '地理'),
(9, '兰州有什么特产可以带回家？', '兰州特产：1) 兰州百合（甜百合）；2) 苦水玫瑰；3) 白兰瓜；4) 黑瓜子；5) 三炮台茶；6) 牛肉面调料包；7) 甘草杏；8) 软儿梨。', '购物'),
(10, '兰州周边有什么值得一去的地方？', '兰州周边景点：1) 兴隆山（国家级森林公园）；2) 吐鲁沟森林公园；3) 什川古梨园；4) 青城古镇；5) 河口古镇；6) 临夏（民族风情）；7) 夏河拉卜楞寺（藏传佛教）。', '景点');

-- 验证数据插入
SELECT '数据库初始化完成' AS status;
SELECT COUNT(*) AS qa_pairs_count FROM qa_pairs;
SELECT COUNT(*) AS chat_history_count FROM chat_history;

-- 显示表结构信息
SHOW TABLES;


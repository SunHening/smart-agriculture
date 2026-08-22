-- ========================================
-- 增量迁移 v2：模块重构新增表
-- 执行: source backend/src/main/resources/migrate_v2.sql
-- ========================================
USE smart_agriculture;

-- 1. 检测模型/参数配置表
CREATE TABLE IF NOT EXISTS model_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    model_type VARCHAR(50) NOT NULL DEFAULT 'vit_b' COMMENT '模型类型 vit_b/vit_l/vit_h',
    checkpoint_path VARCHAR(500) COMMENT '权重文件路径(空则自动下载)',
    device VARCHAR(20) DEFAULT 'cpu' COMMENT 'cpu/cuda',
    model_params TEXT COMMENT '模型参数JSON',
    gsd_default DECIMAL(10,4) DEFAULT 1.2 COMMENT '默认地面采样距离m/px',
    min_area_threshold INT DEFAULT 200 COMMENT '最小地块面积(像素)',
    confidence_threshold DECIMAL(5,4) DEFAULT 0.5 COMMENT '置信度阈值',
    status INT DEFAULT 1 COMMENT '1启用 0禁用',
    is_active INT DEFAULT 0 COMMENT '是否当前生效 1是0否',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '检测模型/参数配置表';

-- 2. 遥感/检测数据集表
CREATE TABLE IF NOT EXISTS dataset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '数据集ID',
    dataset_name VARCHAR(100) NOT NULL COMMENT '数据集名称',
    dataset_type VARCHAR(30) NOT NULL DEFAULT 'REMOTE_SENSING' COMMENT 'REMOTE_SENSING/DRONE',
    description VARCHAR(500) COMMENT '描述',
    image_count INT DEFAULT 0 COMMENT '影像数量',
    storage_path VARCHAR(500) COMMENT '存储路径',
    status INT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '遥感/检测数据集表';

-- 3. AI 模型/提示词参数配置表
CREATE TABLE IF NOT EXISTS ai_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    provider VARCHAR(30) NOT NULL DEFAULT 'deepseek' COMMENT '服务商 deepseek/openai',
    api_key VARCHAR(200) COMMENT 'API Key',
    base_url VARCHAR(300) DEFAULT 'https://api.deepseek.com/v1' COMMENT 'API 端点',
    model_name VARCHAR(100) DEFAULT 'deepseek-chat' COMMENT '模型名',
    temperature DECIMAL(3,2) DEFAULT 0.7 COMMENT '温度',
    max_tokens INT DEFAULT 2000 COMMENT '最大token',
    system_prompt TEXT COMMENT '系统提示词',
    status INT DEFAULT 1 COMMENT '1启用 0禁用',
    is_active INT DEFAULT 0 COMMENT '是否当前生效 1是0否',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'AI模型/提示词参数配置表';

-- 4. 提示词模板表
CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_code VARCHAR(100) COMMENT '模板编码',
    content TEXT COMMENT '模板内容',
    description VARCHAR(500) COMMENT '描述',
    status INT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '提示词模板表';

-- 5. 知识库/常见问题表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '知识ID',
    question VARCHAR(500) NOT NULL COMMENT '问题',
    answer TEXT COMMENT '答案',
    category VARCHAR(100) COMMENT '分类',
    status INT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '知识库/常见问题表';

-- ============ 种子数据 ============

-- 默认生效的 SAM 模型配置
INSERT INTO model_config (config_name, model_type, checkpoint_path, device, model_params, gsd_default, min_area_threshold, confidence_threshold, status, is_active)
SELECT 'SAM-Geo vit_b 默认配置', 'vit_b', NULL, 'cpu', '{}', 1.2, 200, 0.5, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM model_config WHERE is_active = 1);

-- 默认 AI 配置（DeepSeek，需在后台填写 API Key）
INSERT INTO ai_config (provider, api_key, base_url, model_name, temperature, max_tokens, system_prompt, status, is_active)
SELECT 'deepseek', '', 'https://api.deepseek.com/v1', 'deepseek-chat', 0.70, 2000, '你是智慧农业管理系统的智能助手，请用简洁专业的中文回答关于农田面积检测、遥感影像、无人机航拍、病虫害与长势分析的问题。', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM ai_config WHERE is_active = 1);

-- 默认知识库条目（仅首次插入）
INSERT INTO knowledge_base (question, answer, category)
SELECT '如何使用农田面积检测功能？', '在「地块管理」页面通过地图框选区域并点击检测，或在地块档案中上传遥感/航拍影像，系统将自动提取农田边界并计算面积。', '使用指南'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base);

INSERT INTO knowledge_base (question, answer, category)
SELECT '系统支持哪些影像类型？', '支持遥感卫星影像和无人机航拍图像，格式包括 PNG、JPG、GeoTIFF 等，建议分辨率优于 1m/像素。', '使用指南'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base);

-- ========================================
-- 增量迁移：新增农田检测系统相关表
-- 直接在 MySQL 客户端执行: source backend/src/main/resources/migrate.sql
-- ========================================
USE smart_agriculture;

-- 1. 更新农田表（新增检测相关列，若已存在则忽略报错）
-- 若报错 Duplicate column 说明列已存在，可忽略继续执行
ALTER TABLE farm_field ADD COLUMN detected_area DECIMAL(10,4) COMMENT '检测面积(亩)';
ALTER TABLE farm_field ADD COLUMN longitude DOUBLE COMMENT '经度';
ALTER TABLE farm_field ADD COLUMN latitude DOUBLE COMMENT '纬度';
ALTER TABLE farm_field ADD COLUMN boundary_geojson MEDIUMTEXT COMMENT '边界多边形GeoJSON';
ALTER TABLE farm_field ADD COLUMN last_detection_id BIGINT COMMENT '关联最近检测结果ID';

-- 2. 影像任务表
CREATE TABLE IF NOT EXISTS image_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    field_id BIGINT COMMENT '关联农田ID',
    image_type VARCHAR(30) NOT NULL COMMENT '遥感/无人机',
    original_filename VARCHAR(300) COMMENT '原始文件名',
    image_path VARCHAR(500) COMMENT '存储路径',
    resolution DECIMAL(10,4) COMMENT '分辨率m/pixel',
    gsd DECIMAL(10,4) COMMENT '地面采样距离GSD',
    crs VARCHAR(50) COMMENT '坐标参考系',
    longitude DOUBLE COMMENT '中心经度',
    latitude DOUBLE COMMENT '中心纬度',
    image_width INT COMMENT '宽度像素',
    image_height INT COMMENT '高度像素',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/COMPLETED/FAILED',
    error_msg VARCHAR(500) COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '影像任务表';

-- 3. 检测结果表
CREATE TABLE IF NOT EXISTS detection_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '结果ID',
    image_task_id BIGINT NOT NULL COMMENT '关联影像任务ID',
    field_id BIGINT COMMENT '关联农田ID',
    boundary_geojson MEDIUMTEXT COMMENT '边界多边形GeoJSON',
    detected_area DECIMAL(12,4) COMMENT '检测面积(亩)',
    confidence DECIMAL(5,4) COMMENT '置信度0~1',
    coverage_rate DECIMAL(5,4) COMMENT '农田覆盖率',
    mask_image_path VARCHAR(500) COMMENT '分割掩码图路径',
    overlay_image_path VARCHAR(500) COMMENT '叠加可视化图路径',
    processing_time_ms BIGINT COMMENT '处理耗时ms',
    model_name VARCHAR(100) COMMENT '模型名称',
    model_version VARCHAR(50) COMMENT '模型版本',
    status VARCHAR(20) DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '检测结果表';

-- 4. 智能客服会话表
CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) DEFAULT '智能客服会话' COMMENT '会话标题',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '智能客服会话表';

-- 5. 智能客服消息表
CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '关联会话ID',
    sender VARCHAR(10) NOT NULL COMMENT '发送者USER/AI',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type VARCHAR(20) DEFAULT 'TEXT' COMMENT 'TEXT/IMAGE',
    provider VARCHAR(50) DEFAULT 'default' COMMENT 'AI服务商标识',
    model_name VARCHAR(50) COMMENT 'AI模型名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '智能客服消息表';

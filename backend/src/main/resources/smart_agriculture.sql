-- ========================================
-- 智能化农田面积检测系统数据库
-- ========================================
CREATE DATABASE IF NOT EXISTS smart_agriculture DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE smart_agriculture;

-- 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(500) COMMENT '头像',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户表';

-- 农田表（改造后 — 支持影像检测结果）
DROP TABLE IF EXISTS farm_field;
CREATE TABLE farm_field (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '农田ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    field_name VARCHAR(100) NOT NULL COMMENT '农田名称',
    area DECIMAL(10,4) COMMENT '面积(亩)',
    detected_area DECIMAL(10,4) COMMENT '检测面积(亩) — 来自影像自动检测',
    location VARCHAR(200) COMMENT '位置描述',
    longitude DOUBLE COMMENT '经度',
    latitude DOUBLE COMMENT '纬度',
    soil_type VARCHAR(50) COMMENT '土壤类型',
    status INT DEFAULT 1 COMMENT '状态: 1正常 0闲置',
    cover_image VARCHAR(500) COMMENT '封面图片',
    boundary_geojson MEDIUMTEXT COMMENT '边界多边形 GeoJSON',
    last_detection_id BIGINT COMMENT '关联的最近检测结果ID',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '农田表';

-- 影像任务表
DROP TABLE IF EXISTS image_task;
CREATE TABLE image_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    field_id BIGINT COMMENT '关联农田ID',
    image_type VARCHAR(30) NOT NULL COMMENT '影像类型: REMOTE_SENSING/DRONE',
    original_filename VARCHAR(300) COMMENT '原始文件名',
    image_path VARCHAR(500) COMMENT '服务器存储路径',
    resolution DECIMAL(10,4) COMMENT '分辨率(m/pixel)',
    gsd DECIMAL(10,4) COMMENT '地面采样距离 GSD(m/pixel)',
    crs VARCHAR(50) COMMENT '坐标参考系 CRS(如 EPSG:4326)',
    longitude DOUBLE COMMENT '影像中心经度',
    latitude DOUBLE COMMENT '影像中心纬度',
    image_width INT COMMENT '影像宽度(像素)',
    image_height INT COMMENT '影像高度(像素)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/COMPLETED/FAILED',
    error_msg VARCHAR(500) COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '影像任务表';

-- 检测结果表
DROP TABLE IF EXISTS detection_result;
CREATE TABLE detection_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '结果ID',
    image_task_id BIGINT NOT NULL COMMENT '关联影像任务ID',
    field_id BIGINT COMMENT '关联农田ID',
    boundary_geojson MEDIUMTEXT COMMENT '边界多边形 GeoJSON',
    detected_area DECIMAL(12,4) COMMENT '检测面积(亩)',
    confidence DECIMAL(5,4) COMMENT '置信度(0~1)',
    coverage_rate DECIMAL(5,4) COMMENT '农田覆盖率',
    mask_image_path VARCHAR(500) COMMENT '分割掩码图片路径',
    overlay_image_path VARCHAR(500) COMMENT '叠加可视化图片路径',
    processing_time_ms BIGINT COMMENT '处理耗时(毫秒)',
    model_name VARCHAR(100) COMMENT '模型名称',
    model_version VARCHAR(50) COMMENT '模型版本',
    status VARCHAR(20) DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '检测结果表';

-- 智能客服会话表
DROP TABLE IF EXISTS ai_conversation;
CREATE TABLE ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) DEFAULT '智能客服会话' COMMENT '会话标题',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '智能客服会话表';

-- 智能客服消息表
DROP TABLE IF EXISTS ai_message;
CREATE TABLE ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '关联会话ID',
    sender VARCHAR(10) NOT NULL COMMENT '发送者: USER/AI',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type VARCHAR(20) DEFAULT 'TEXT' COMMENT '消息类型: TEXT/IMAGE',
    provider VARCHAR(50) DEFAULT 'default' COMMENT 'AI服务商标识',
    model_name VARCHAR(50) COMMENT 'AI模型名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '智能客服消息表';

-- ==================== 初始化数据 ====================

-- 管理员 (密码: admin123)
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
('admin', 'admin123', '系统管理员', 'ADMIN', 1);

-- 普通用户 (密码: 123456)
INSERT INTO sys_user (username, password, real_name, phone, role, status) VALUES
('zhangsan', '123456', '张三', '13800138000', 'USER', 1);

-- 农田数据
INSERT INTO farm_field (user_id, field_name, area, location, longitude, latitude, soil_type, status) VALUES
(2, '东区1号田', 5.50, '村东200米', 116.4074, 39.9042, '壤土', 1),
(2, '西区2号田', 3.20, '村西500米', 116.3982, 39.9015, '黏土', 1),
(2, '南区试验田', 2.00, '村南温室', 116.4150, 39.9008, '砂壤土', 1);

# 智慧农业管理系统 - 系统说明文档

## 一、系统概述

智慧农业管理系统是一个基于 SpringBoot + Vue3 + MySQL 的前后端分离 Web 应用，融合物联网环境感知、数据可视化分析与 AI 智能辅助决策，面向农业生产全流程的数字化管理平台。

系统采用双角色权限设计，管理员端侧重后台数据管理与审批，用户端侧重日常农事操作与交互体验，两端界面风格完全独立。

---

## 二、技术栈

### 2.1 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架 |
| Spring Security | 2.7.x | 安全认证与权限控制 |
| MyBatis Plus | 3.5.3.1 | ORM 持久层框架 |
| MySQL | 8.0+ | 关系型数据库 |
| JWT (jjwt) | 0.9.1 | 无状态 Token 认证 |
| Hutool | 5.8.18 | Java 工具类库 |
| EasyExcel | 3.3.2 | Excel 导出 |
| Lombok | 1.18.28 | 简化实体类代码 |
| Maven | 3.6+ | 项目构建管理 |
| JDK | 1.8 | Java 运行环境 |

### 2.2 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.3.4 | 渐进式前端框架 |
| Vite | 4.4.9 | 构建工具 |
| Element Plus | 2.3.14 | UI 组件库 |
| ECharts | 5.4.3 | 数据可视化图表 |
| Pinia | 2.1.6 | 状态管理 |
| Vue Router | 4.2.4 | 路由管理 |
| Axios | 1.5.0 | HTTP 请求库 |
| Sass | 1.66.1 | CSS 预处理器 |

### 2.3 开发工具

| 工具 | 用途 |
|------|------|
| IntelliJ IDEA / VS Code | 后端/前端开发 |
| Navicat / DataGrip | 数据库管理 |
| Postman | 接口调试 |
| Git | 版本控制 |
| Node.js 16+ | 前端运行环境 |

---

## 三、系统架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        浏览器端                              │
├──────────────────────────┬──────────────────────────────────┤
│     管理员端 (Admin)      │         用户端 (User)            │
│  侧边栏 + 数据管理        │    顶部导航 + 工作台风格          │
│  数据大屏指挥舱           │    AI 农业助手                   │
├──────────────────────────┴──────────────────────────────────┤
│                    Vue3 + Element Plus + ECharts             │
├─────────────────────────────────────────────────────────────┤
│                    Axios HTTP (RESTful API)                  │
├─────────────────────────────────────────────────────────────┤
│                    Vite Dev Proxy / Nginx                    │
├─────────────────────────────────────────────────────────────┤
│                Spring Boot (Controller 层)                   │
│                Spring Security + JWT Filter                  │
├─────────────────────────────────────────────────────────────┤
│                Service 业务逻辑层                             │
├─────────────────────────────────────────────────────────────┤
│                MyBatis Plus (Mapper 层)                      │
├─────────────────────────────────────────────────────────────┤
│                    MySQL 8.0 数据库                          │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 后端分层架构

```
com.smart.agriculture
├── config/              # 配置类（Security、MyBatis、CORS、定时任务）
├── controller/          # 控制器层（RESTful API 接口）
├── service/             # 业务逻辑层（接口）
│   └── impl/           # 业务逻辑层（实现）
├── mapper/              # 数据访问层（MyBatis Mapper）
├── entity/              # 实体类（对应数据库表）
├── common/              # 公共类（统一响应、异常处理、常量）
└── utils/               # 工具类（JWT 工具）
```

### 3.3 前端目录结构

```
frontend/src/
├── api/                 # 接口封装（Axios 请求）
├── assets/              # 静态资源（全局样式）
├── components/          # 公共组件（AI 助手）
├── router/              # 路由配置
├── store/               # Pinia 状态管理
└── views/
    ├── login/           # 登录页
    ├── screen/          # 智慧农业座舱大屏
    ├── layout/          # 管理员端布局
    ├── dashboard/       # 管理员-数据概览
    ├── user/            # 管理员-用户管理
    ├── field/           # 管理员-农田管理
    ├── crop/            # 管理员-作物管理
    ├── planting/        # 管理员-种植记录
    ├── environment/     # 管理员-环境监测
    ├── task/            # 管理员-农事任务
    ├── material/        # 管理员-农资管理
    ├── material-apply/  # 管理员-领用审批
    ├── harvest/         # 管理员-收获记录
    ├── notice/          # 管理员-公告管理
    └── user-portal/     # 用户端（独立布局）
        ├── layout.vue   # 用户端顶部导航布局
        ├── home/        # 用户-工作台
        ├── field/       # 用户-我的农田
        ├── planting/    # 用户-种植管理
        ├── environment/ # 用户-环境监测
        ├── task/        # 用户-农事任务
        ├── material/    # 用户-农资领用
        ├── harvest/     # 用户-收获记录
        ├── notice/      # 用户-系统公告
        └── profile/     # 用户-个人中心
```

---

## 四、功能模块说明

### 4.1 系统角色与权限

| 角色 | 路由前缀 | 布局风格 | 核心职责 |
|------|---------|---------|---------|
| 管理员 | /admin | 侧边栏 + 主内容区 | 数据管理、审批、全局监控 |
| 普通用户 | /home | 顶部导航 + 工作台 | 日常农事操作、数据查看 |

### 4.2 管理员端功能

| 模块 | 功能描述 |
|------|---------|
| 智慧座舱 | 登录后首页，农业产业大数据指挥舱可视化大屏 |
| 数据概览 | 统计卡片、收益趋势图、作物分布饼图、农资环状图、任务玫瑰图、土壤分布图、面积对比图、库存预警 |
| 用户管理 | 用户列表、状态启禁用、角色筛选、删除 |
| 农田管理 | 所有用户农田 CRUD、分页搜索 |
| 作物管理 | 作物信息库 CRUD、分类筛选 |
| 种植记录 | 全局种植记录查看、状态筛选 |
| 环境监测 | 选择任意农田查看实时数据、24小时趋势图表、历史数据分页 |
| 农事任务 | 全局任务管理、状态统计 |
| 农资管理 | 农资 CRUD、入库/出库操作、库存预警、图片管理 |
| 领用审批 | 查看用户领用申请、通过（自动出库）/拒绝（附原因） |
| 收获记录 | 全局收获数据、收益统计 |
| 公告管理 | 发布/编辑/删除系统公告 |

### 4.3 用户端功能

| 模块 | 功能描述 |
|------|---------|
| 工作台 | 欢迎语、实时指标、环境数据、待办任务、最新公告、快捷入口 |
| 我的农田 | 卡片式展示（含封面图片）、添加/编辑/删除、图片上传 |
| 种植管理 | Tab 切换状态、种植进度条、新增/编辑种植记录 |
| 环境监测 | 实时六项指标、温湿度趋势图、土壤湿度趋势图 |
| 农事任务 | 任务统计、状态切换、新建/开始/完成/删除 |
| 农资领用 | 浏览农资（含封面图片）、提交领用申请、查看申请状态 |
| 收获记录 | 收益统计面板、收获记录表格、新增/编辑 |
| 系统公告 | 公告列表、点击查看详情 |
| 个人中心 | 头像上传、信息修改、密码修改 |
| AI 助手 | 悬浮机器人、农业知识问答、快捷问题、打字动画 |

### 4.4 系统亮点功能

| 亮点 | 说明 |
|------|------|
| 双角色差异化 UI | 管理员侧边栏后台风格，用户端工作台交互风格，完全独立 |
| 智慧座舱大屏 | 管理员登录首页为数据指挥舱，全屏可视化展示 |
| 环境数据模拟 | 定时任务自动生成传感器数据，启动时生成24小时历史数据 |
| 数据可视化 | ECharts 实现柱状图、折线图、饼图、环状图、玫瑰图等 |
| 农资领用审批流 | 用户申请 → 管理员审批 → 自动出库，体现业务流转 |
| AI 农业助手 | 模拟智能对话，内置农业知识库，关键词匹配回答 |
| 科技感登录页 | Canvas 粒子连线动画，体现 AI 赋能主题 |
| 文件上传 | 农田封面、农资图片、用户头像上传 |
| JWT 无状态认证 | Token 鉴权，前后端完全分离 |
| 库存预警 | 农资库存低于阈值自动预警提示 |

---

## 五、数据库设计

### 5.1 数据库表清单

| 序号 | 表名 | 说明 |
|------|------|------|
| 1 | sys_user | 用户表 |
| 2 | farm_field | 农田表 |
| 3 | farm_crop | 作物信息表 |
| 4 | farm_planting | 种植记录表 |
| 5 | farm_environment | 环境监测数据表 |
| 6 | farm_task | 农事任务表 |
| 7 | farm_material | 农资表 |
| 8 | farm_material_record | 农资出入库记录表 |
| 9 | farm_material_apply | 农资领用申请表 |
| 10 | farm_harvest | 收获记录表 |
| 11 | sys_notice | 系统公告表 |
| 12 | sys_log | 操作日志表 |
| 13 | farm_alert_rule | 环境预警规则表 |
| 14 | farm_alert_record | 预警记录表 |

### 5.2 核心表关系

```
sys_user (1) ──── (N) farm_field
sys_user (1) ──── (N) farm_planting
sys_user (1) ──── (N) farm_task
sys_user (1) ──── (N) farm_harvest
sys_user (1) ──── (N) farm_material_apply
farm_field (1) ──── (N) farm_environment
farm_field (1) ──── (N) farm_planting
farm_crop (1) ──── (N) farm_planting
farm_material (1) ──── (N) farm_material_apply
farm_material (1) ──── (N) farm_material_record
```

---

## 六、API 接口设计

### 6.1 接口规范

- 基础路径：`/api`
- 认证方式：Bearer Token（JWT）
- 响应格式：`{ code: 200, message: "操作成功", data: {} }`
- 分页参数：`pageNum`（页码）、`pageSize`（每页条数）

### 6.2 主要接口列表

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 认证 | POST | /api/auth/login | 登录 |
| 认证 | POST | /api/auth/register | 注册 |
| 用户 | GET | /api/user/info | 获取当前用户信息 |
| 用户 | GET | /api/user/page | 用户分页列表 |
| 农田 | GET | /api/field/list | 农田列表 |
| 农田 | POST | /api/field | 新增农田 |
| 作物 | GET | /api/crop/page | 作物分页 |
| 种植 | GET | /api/planting/page | 种植记录分页 |
| 环境 | GET | /api/environment/latest/{fieldId} | 最新环境数据 |
| 环境 | GET | /api/environment/recent/{fieldId} | 24小时趋势数据 |
| 任务 | GET | /api/task/page | 任务分页 |
| 任务 | PUT | /api/task/complete/{id} | 完成任务 |
| 农资 | GET | /api/material/page | 农资分页 |
| 农资 | POST | /api/material/in | 入库 |
| 农资 | POST | /api/material/out | 出库 |
| 领用 | POST | /api/material-apply | 提交领用申请 |
| 领用 | PUT | /api/material-apply/approve/{id} | 审批通过 |
| 领用 | PUT | /api/material-apply/reject/{id} | 审批拒绝 |
| 收获 | GET | /api/harvest/statistics | 收益统计 |
| 公告 | GET | /api/notice/page | 公告分页 |
| 大屏 | GET | /api/dashboard/overview | 数据概览统计 |
| 上传 | POST | /api/upload | 文件上传 |

---

## 七、本地部署指南

### 7.1 环境要求

| 环境 | 版本要求 |
|------|---------|
| JDK | 1.8+ |
| Maven | 3.6+ |
| Node.js | 16+ |
| npm | 8+ |
| MySQL | 8.0+ |

### 7.2 数据库初始化

1. 启动 MySQL 服务

2. 登录 MySQL 并执行主脚本：
```sql
mysql -u root -p
SOURCE D:/Learning/农业/smart-agriculture/backend/src/main/resources/smart_agriculture.sql;
```

3. 执行样本数据脚本：
```sql
USE smart_agriculture;
SOURCE D:/Learning/农业/smart-agriculture/backend/src/main/resources/sample_data.sql;
```

4. 执行环境监测数据脚本（按顺序）：
```sql
SOURCE D:/Learning/农业/smart-agriculture/backend/src/main/resources/env_data_batch1.sql;
SOURCE D:/Learning/农业/smart-agriculture/backend/src/main/resources/env_data_batch2.sql;
SOURCE D:/Learning/农业/smart-agriculture/backend/src/main/resources/env_data_batch3.sql;
SOURCE D:/Learning/农业/smart-agriculture/backend/src/main/resources/env_data_batch4.sql;
SOURCE D:/Learning/农业/smart-agriculture/backend/src/main/resources/env_data_batch5.sql;
```

### 7.3 后端配置与启动

1. 修改数据库连接配置：

编辑 `backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_agriculture?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 你的MySQL密码
```

2. 如果不使用 Redis，注释掉 `pom.xml` 中的 Redis 依赖和 `application.yml` 中的 Redis 配置。

3. 编译并启动：
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

4. 启动成功后，后端运行在 `http://localhost:8088`

### 7.4 前端配置与启动

1. 安装依赖：
```bash
cd frontend
npm install
```

2. 启动开发服务器：
```bash
npm run dev
```

3. 启动成功后，前端运行在 `http://localhost:5173`

4. Vite 已配置代理，`/api` 和 `/uploads` 请求自动转发到后端 8088 端口。

### 7.5 访问系统

浏览器打开 `http://localhost:5173`

**默认账号：**

| 角色 | 用户名 | 密码 | 登录后首页 |
|------|--------|------|-----------|
| 管理员 | admin | admin123 | 智慧座舱大屏 → 管理后台 |
| 普通用户 | zhangsan | 123456 | 用户工作台 |
| 普通用户 | huangwei | 123456 | 用户工作台 |

（所有样本用户密码均为 `123456`）

### 7.6 生产部署（可选）

1. 前端打包：
```bash
cd frontend
npm run build
```
生成 `dist/` 目录，部署到 Nginx。

2. 后端打包：
```bash
cd backend
mvn clean package -DskipTests
```
生成 `target/agriculture-1.0.0.jar`，使用 `java -jar` 运行。

3. Nginx 配置示例：
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8088;
    }

    location /uploads {
        proxy_pass http://localhost:8088;
    }
}
```

---

## 八、项目文件清单

```
smart-agriculture/
├── backend/                          # 后端项目
│   ├── pom.xml                      # Maven 配置
│   └── src/main/
│       ├── java/com/smart/agriculture/
│       │   ├── SmartAgricultureApplication.java
│       │   ├── config/              # 5个配置类
│       │   ├── controller/          # 11个控制器
│       │   ├── entity/              # 13个实体类
│       │   ├── mapper/              # 13个Mapper接口
│       │   ├── service/             # 9个Service接口
│       │   │   └── impl/           # 9个Service实现
│       │   ├── common/              # 3个公共类
│       │   └── utils/               # 1个工具类
│       └── resources/
│           ├── application.yml      # 应用配置
│           ├── mapper/              # 3个Mapper XML
│           ├── smart_agriculture.sql # 主数据库脚本
│           ├── sample_data.sql      # 样本数据脚本
│           └── env_data_batch*.sql  # 环境数据脚本(5个)
├── frontend/                         # 前端项目
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   ├── public/                      # 静态资源
│   │   ├── dashboard-screen/       # 大屏页面资源
│   │   └── img/material/           # 农资占位图
│   └── src/
│       ├── main.js                  # 入口文件
│       ├── App.vue                  # 根组件
│       ├── api/                     # 2个API文件
│       ├── assets/                  # 全局样式
│       ├── components/              # AI助手组件
│       ├── router/                  # 路由配置
│       ├── store/                   # 状态管理
│       └── views/                   # 22个页面组件
├── Dashboard_Home/                   # 大屏源文件
└── README.md                         # 项目说明
```

---

## 九、常见问题

**Q: 启动后端报 Redis 连接失败？**
A: 如果未安装 Redis，在 `pom.xml` 中注释掉 `spring-boot-starter-data-redis` 依赖，并删除 `application.yml` 中的 redis 配置段。

**Q: 环境监测页面没有数据？**
A: 确保已执行 `env_data_batch*.sql` 脚本，且脚本中通过 `field_name` 动态获取的 ID 与实际数据库一致。

**Q: 上传的图片无法显示？**
A: 确认 `vite.config.js` 中已配置 `/uploads` 代理到后端 8088 端口。

**Q: 管理员端环境监测无数据？**
A: 后端 `/api/field/list` 接口对管理员返回所有农田，确认数据库中有农田和对应的环境数据。

---

*文档版本：v1.0*
*最后更新：2025年5月*

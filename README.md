# 智能化农田面积检测系统

基于遥感影像、无人机航拍与图像处理技术的农田面积自动检测系统。

## 项目简介

本项目面向智慧农业应用场景，研究基于遥感影像、无人机航拍与图像处理技术的农田面积自动检测方法，构建一套智能化农田面积检测系统。项目结合计算机视觉、深度学习和地理信息处理技术，实现农田区域自动识别、边界提取以及面积计算，提高农田面积测量的自动化程度与精度。

## 技术栈

### 后端
- Spring Boot 2.7.18
- Spring Security + JWT
- MyBatis Plus 3.5.3
- MySQL 8.0

### AI 检测服务（预留接口）
- Python Flask/FastAPI
- PyTorch / ONNX Runtime
- U-Net / DeepLabV3+ 语义分割
- OpenCV + GDAL 图像处理

### 前端
- Vue 3 + Composition API
- Vite 4
- Element Plus
- ECharts 5
- Pinia + Axios

## 核心功能

| 模块 | 功能 |
|------|------|
| 影像上传 | 支持遥感影像(GeoTIFF)和无人机航拍图像上传 |
| 农田检测 | 基于深度学习自动识别农田区域 |
| 边界提取 | 语义分割 → 轮廓提取 → GeoJSON 多边形 |
| 面积计算 | 像素坐标→地理坐标转换，自动面积核算 |
| 结果可视化 | 原图+掩码叠加、边界展示、面积标注 |
| 智能客服 | AI 对话交互，解答检测相关问题 |

## 系统功能模块

| 模块 | 功能 |
|------|------|
| 用户管理 | 注册/登录/角色权限/状态管理 |
| 农田管理 | 农田CRUD/坐标信息/检测面积对比 |
| 影像检测 | 遥感/无人机影像上传/自动检测/结果查看 |
| 检测概览 | 检测统计/成功率/状态分布 |
| 智能客服 | AI对话/检测咨询/接口预留 |
| 数据大屏 | 检测结果大屏展示 |

## 快速开始

### 1. 数据库初始化

```sql
-- 执行SQL脚本
source backend/src/main/resources/smart_agriculture.sql
```

### 2. 修改后端配置

编辑 `backend/src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_agriculture
    username: root
    password: 你的密码
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端运行在 http://localhost:8088

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 http://localhost:5173

### 5. 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 普通用户 | zhangsan | 123456 |

## 项目结构

```
smart-agriculture/
├── backend/                          # Spring Boot 后端
│   └── src/main/java/com/smart/agriculture/
│       ├── common/                   # 公共类、AI服务商接口
│       ├── config/                   # Security、JWT、MyBatis Plus配置
│       ├── controller/               # 控制器
│       │   ├── AuthController        # 认证
│       │   ├── UserController        # 用户管理
│       │   ├── FarmFieldController   # 农田管理
│       │   ├── ImageTaskController   # 影像检测
│       │   ├── AiCustomerServiceController  # 智能客服
│       │   └── DashboardController   # 检测概览
│       ├── entity/                   # 实体类
│       ├── mapper/                   # MyBatis Plus Mapper
│       └── service/                  # 业务逻辑层
├── frontend/                         # Vue3 前端
│   └── src/
│       ├── api/                      # 接口封装
│       ├── router/                   # 路由配置
│       ├── store/                    # Pinia 状态管理
│       └── views/                    # 页面组件
│           ├── login/                # 登录
│           ├── layout/               # 管理端布局
│           ├── dashboard/            # 检测概览
│           ├── user/                 # 用户管理
│           ├── field/                # 农田管理
│           ├── detection/            # 影像检测(列表+详情)
│           ├── customer-service/     # 智能客服
│           ├── screen/               # 数据大屏
│           └── user-portal/          # 用户端(工作台/农田/检测/客服/个人中心)
└── README.md
```

## AI 服务商接入（预留接口）

智能客服模块预留了 AI 服务商接入接口 `AiProvider`，支持：

- **DeepSeek API** — 实现类 `DeepSeekAiProvider`（已创建，待配置 API Key）
- **OpenAI API** — 实现 `AiProvider` 接口即可接入
- **本地模型** — 可实现 `AiProvider` 接口对接本地模型

配置方式：实现 `AiProvider` 接口，在 `AiCustomerServiceImpl` 中注入对应 Provider 即可切换。

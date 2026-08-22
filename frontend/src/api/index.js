import request from './request'

// ==================== 认证相关 ====================
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)

// ==================== 用户相关 ====================
export const getUserInfo = () => request.get('/user/info')
export const updateUser = (data) => request.put('/user/update', data)
export const updatePassword = (data) => request.put('/user/password', data)
export const getUserPage = (params) => request.get('/user/page', { params })
export const updateUserStatus = (id, status) => request.put(`/user/status/${id}`, null, { params: { status } })
export const deleteUser = (id) => request.delete(`/user/${id}`)

// ==================== 农田相关 ====================
export const getFieldPage = (params) => request.get('/field/page', { params })
export const getFieldList = () => request.get('/field/list')
export const addField = (data) => request.post('/field', data)
export const updateField = (data) => request.put('/field', data)
export const deleteField = (id) => request.delete(`/field/${id}`)

// ==================== 影像检测相关 ====================
export const submitDetection = (data) => request.post('/detection/submit', data)
export const getDetectionPage = (params) => request.get('/detection/page', { params })
export const getDetectionDetail = (id) => request.get(`/detection/${id}`)
export const getDetectionRecords = (params) => request.get('/detection/records', { params })
export const reDetect = (id) => request.post(`/detection/re-detect/${id}`)
export const deleteDetection = (id) => request.delete(`/detection/${id}`)

// ==================== SAM-Geo 地图检测相关 ====================
// SAM 首次推理需下载 vit_b 权重(375MB) + CPU 分割，耗时长，单独放宽超时
export const samGeoDetect = (data) => request.post('/sam-geo/detect', data, { timeout: 600000 })
export const samGeoDetectImage = (data) => request.post('/sam-geo/detect-image', data, { timeout: 600000 })
export const samGeoHealth = () => request.get('/sam-geo/health')

// ==================== 智能问答相关 ====================
export const sendChatMessage = (data) => request.post('/customer-service/chat', data)
export const getChatHistory = (params) => request.get('/customer-service/history', { params })
export const clearChatHistory = (params) => request.delete('/customer-service/history', { params })

// ==================== 数据看板 ====================
export const getDashboardOverview = () => request.get('/dashboard/overview')
export const getDashboardTasks = () => request.get('/dashboard/tasks')

// ==================== 检测模型配置 ====================
export const getModelConfigList = () => request.get('/model-config/list')
export const getActiveModelConfig = () => request.get('/model-config/active')
export const addModelConfig = (data) => request.post('/model-config', data)
export const updateModelConfig = (data) => request.put('/model-config', data)
export const deleteModelConfig = (id) => request.delete(`/model-config/${id}`)

// ==================== 数据集 ====================
export const getDatasetList = () => request.get('/dataset/list')
export const addDataset = (data) => request.post('/dataset', data)
export const updateDataset = (data) => request.put('/dataset', data)
export const deleteDataset = (id) => request.delete(`/dataset/${id}`)

// ==================== AI 配置 ====================
export const getAiConfigList = () => request.get('/ai-config/list')
export const getActiveAiConfig = () => request.get('/ai-config/active')
export const addAiConfig = (data) => request.post('/ai-config', data)
export const updateAiConfig = (data) => request.put('/ai-config', data)
export const deleteAiConfig = (id) => request.delete(`/ai-config/${id}`)

// ==================== 提示词模板 ====================
export const getPromptTemplateList = () => request.get('/prompt-template/list')
export const addPromptTemplate = (data) => request.post('/prompt-template', data)
export const updatePromptTemplate = (data) => request.put('/prompt-template', data)
export const deletePromptTemplate = (id) => request.delete(`/prompt-template/${id}`)

// ==================== 知识库 ====================
export const getKnowledgeList = () => request.get('/knowledge/list')
export const searchKnowledge = (keyword) => request.get('/knowledge/search', { params: { keyword } })
export const addKnowledge = (data) => request.post('/knowledge', data)
export const updateKnowledge = (data) => request.put('/knowledge', data)
export const deleteKnowledge = (id) => request.delete(`/knowledge/${id}`)

// ==================== 文件上传 ====================
export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

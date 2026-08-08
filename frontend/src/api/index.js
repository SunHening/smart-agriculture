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
// 上传影像发起检测
export const submitDetection = (data) => request.post('/detection/submit', data)
// 获取检测任务列表
export const getDetectionPage = (params) => request.get('/detection/page', { params })
// 获取检测详情
export const getDetectionDetail = (id) => request.get(`/detection/${id}`)
// 重新检测
export const reDetect = (id) => request.post(`/detection/re-detect/${id}`)
// 删除检测记录
export const deleteDetection = (id) => request.delete(`/detection/${id}`)

// ==================== SAM-Geo 地图检测相关 ====================
// 地图框选检测
export const samGeoDetect = (data) => request.post('/sam-geo/detect', data)
// SAM-Geo 服务健康检查
export const samGeoHealth = () => request.get('/sam-geo/health')

// ==================== 智能客服相关 ====================
// 发送消息
export const sendChatMessage = (data) => request.post('/customer-service/chat', data)
// 获取历史消息
export const getChatHistory = (params) => request.get('/customer-service/history', { params })
// 清除历史
export const clearChatHistory = () => request.delete('/customer-service/history')

// ==================== 数据大屏 ====================
export const getDashboardOverview = () => request.get('/dashboard/overview')

// ==================== 文件上传 ====================
export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

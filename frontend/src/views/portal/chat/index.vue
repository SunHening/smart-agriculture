<template>
  <div class="cs-container">
    <el-card class="cs-card" :body-style="{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column', padding: 0 }">
      <!-- 头部 -->
      <div class="cs-header">
        <div class="cs-header-left">
          <span class="cs-avatar">🤖</span>
          <div>
            <div class="cs-title">AI 问答</div>
            <div class="cs-subtitle">{{ activeModel ? activeModel : '农田检测助手' }}</div>
          </div>
        </div>
        <el-button text @click="handleClear">清除历史</el-button>
      </div>

      <!-- 消息列表 -->
      <div class="cs-body" ref="bodyRef">
        <div v-for="(msg, i) in messages" :key="i" class="cs-message" :class="msg.sender === 'USER' ? 'user' : 'ai'">
          <div class="cs-bubble">
            <div class="cs-text">{{ msg.content }}</div>
            <div class="cs-time">{{ msg.createTime }}</div>
          </div>
        </div>
        <div v-if="loading" class="cs-message ai">
          <div class="cs-bubble typing">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <!-- 快捷提问 -->
      <div class="cs-quick" v-if="messages.length === 0">
        <span class="quick-tag" v-for="q in quickQuestions" :key="q" @click="send(q)">{{ q }}</span>
      </div>

      <!-- 输入区 -->
      <div class="cs-footer">
        <el-input v-model="text" placeholder="输入您的问题..." @keyup.enter="send(text)" :disabled="loading" clearable>
          <template #append>
            <el-button :icon="Promotion" @click="send(text)" :disabled="!text.trim() || loading" />
          </template>
        </el-input>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { sendChatMessage, getChatHistory, clearChatHistory, getActiveAiConfig } from '../../../api'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const text = ref('')
const loading = ref(false)
const messages = ref([])
const bodyRef = ref(null)
const activeModel = ref('')

const quickQuestions = [
  '如何使用面积检测功能？',
  '支持哪些类型的遥感影像？',
  '无人机拍摄需要注意什么？',
  '边界提取的精度如何？',
  '病虫害分析是怎么做的？'
]

const scrollBottom = () => {
  nextTick(() => {
    if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  })
}

const send = async (msg) => {
  const content = msg?.trim?.() ?? text.value.trim()
  if (!content || loading.value) return
  text.value = ''
  loading.value = true

  messages.value.push({ sender: 'USER', content, createTime: new Date().toLocaleTimeString() })
  scrollBottom()

  try {
    const res = await sendChatMessage({ userId: user.id, message: content })
    const data = res.data
    messages.value.push(data.aiMessage)
    if (data.modelName) activeModel.value = `${data.provider || ''} / ${data.modelName}`
    scrollBottom()
  } catch {
    ElMessage.error('发送失败，请重试')
  } finally {
    loading.value = false
  }
}

const handleClear = async () => {
  await clearChatHistory({ userId: user.id })
  messages.value = []
  ElMessage.success('历史已清除')
}

const loadHistory = async () => {
  try {
    const res = await getChatHistory({ userId: user.id, pageNum: 1, pageSize: 100 })
    if (res.data?.records) messages.value = res.data.records
  } catch { /* ignore */ }
}

const loadActiveModel = async () => {
  try {
    const res = await getActiveAiConfig()
    if (res.data) activeModel.value = `${res.data.provider || 'AI'} / ${res.data.modelName || '未配置'}`
  } catch { /* ignore */ }
}

onMounted(() => {
  loadHistory()
  loadActiveModel()
})
</script>

<style scoped>
.cs-container { width: 100%; }
.cs-card { border-radius: 12px; overflow: hidden; }

.cs-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, #1b5e20, #2e7d32);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.cs-header-left { display: flex; align-items: center; gap: 10px; }
.cs-avatar { width: 40px; height: 40px; background: rgba(255,255,255,0.2); border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 22px; }
.cs-title { font-size: 16px; font-weight: 600; color: #fff; }
.cs-subtitle { font-size: 12px; color: rgba(255,255,255,0.7); }

.cs-body { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }

.cs-message { display: flex; max-width: 75%; }
.cs-message.user { align-self: flex-end; }
.cs-message.ai { align-self: flex-start; }
.cs-bubble { padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; }
.cs-message.user .cs-bubble { background: #2e7d32; color: #fff; border-bottom-right-radius: 4px; }
.cs-message.ai .cs-bubble { background: #f5f7fa; color: #333; border-bottom-left-radius: 4px; }
.cs-time { font-size: 11px; margin-top: 4px; opacity: 0.6; }
.cs-message.user .cs-time { text-align: right; }

.typing { display: flex; gap: 4px; padding: 8px 14px; }
.typing span { width: 6px; height: 6px; background: #999; border-radius: 50%; animation: typingDot 1.4s infinite; }
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typingDot { 0%,60%,100% { transform: translateY(0); opacity: 0.4; } 30% { transform: translateY(-4px); opacity: 1; } }

.cs-quick { padding: 10px 20px; border-top: 1px solid #f5f5f5; display: flex; flex-wrap: wrap; gap: 8px; flex-shrink: 0; }
.quick-tag { font-size: 12px; padding: 4px 12px; background: #f0f9eb; color: #43a047; border-radius: 12px; cursor: pointer; transition: all 0.2s; }
.quick-tag:hover { background: #43a047; color: #fff; }

.cs-footer { padding: 12px 20px; border-top: 1px solid #f0f0f0; flex-shrink: 0; }
.cs-footer :deep(.el-input-group__append) { background: #43a047; border-color: #43a047; color: #fff; }
</style>

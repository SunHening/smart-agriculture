<template>
  <!-- 悬浮按钮 -->
  <div class="ai-assistant">
    <transition name="chat-fade">
      <div v-show="isOpen" class="chat-panel">
        <div class="chat-header">
          <div class="chat-header-left">
            <div class="ai-avatar-small">🤖</div>
            <div>
              <div class="chat-title">农业AI助手</div>
              <div class="chat-status"><span class="status-dot"></span>在线</div>
            </div>
          </div>
          <el-icon class="close-btn" @click="isOpen = false"><Close /></el-icon>
        </div>

        <div class="chat-body" ref="chatBodyRef">
          <div v-for="(msg, index) in messages" :key="index" class="chat-message" :class="msg.role">
            <div class="msg-avatar">
              <span v-if="msg.role === 'ai'">🤖</span>
              <span v-else>👤</span>
            </div>
            <div class="msg-bubble">
              <div class="msg-text" v-html="msg.content"></div>
              <div class="msg-time">{{ msg.time }}</div>
            </div>
          </div>
          <div v-if="isTyping" class="chat-message ai">
            <div class="msg-avatar"><span>🤖</span></div>
            <div class="msg-bubble">
              <div class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 快捷问题 -->
        <div class="quick-questions" v-if="messages.length <= 1">
          <div class="quick-label">试试问我：</div>
          <div class="quick-list">
            <span class="quick-item" v-for="q in quickQuestions" :key="q" @click="sendMessage(q)">{{ q }}</span>
          </div>
        </div>

        <div class="chat-footer">
          <el-input
            v-model="inputText"
            placeholder="输入农业相关问题..."
            @keyup.enter="sendMessage(inputText)"
            :disabled="isTyping"
          >
            <template #append>
              <el-button :icon="Promotion" @click="sendMessage(inputText)" :disabled="!inputText.trim() || isTyping" />
            </template>
          </el-input>
        </div>
      </div>
    </transition>

    <!-- 悬浮触发按钮 -->
    <div class="float-btn" @click="isOpen = !isOpen" :class="{ active: isOpen }">
      <div class="float-btn-inner">
        <span class="robot-icon">🤖</span>
        <span class="btn-pulse"></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const isOpen = ref(false)
const inputText = ref('')
const isTyping = ref(false)
const chatBodyRef = ref(null)

const messages = ref([
  {
    role: 'ai',
    content: '您好！我是智慧农业AI助手 🌱<br>我可以帮您解答农业种植、病虫害防治、施肥灌溉等方面的问题。请问有什么可以帮您的？',
    time: getCurrentTime()
  }
])

const quickQuestions = [
  '水稻什么时候施肥最好？',
  '番茄叶子发黄怎么办？',
  '如何预防草莓灰霉病？',
  '玉米的最佳种植密度？'
]

// 模拟AI知识库
const knowledgeBase = {
  '施肥': {
    keywords: ['施肥', '肥料', '追肥', '底肥'],
    answers: [
      '关于施肥建议：<br><br>🌾 <b>水稻施肥</b>：分蘖期追施氮肥（每亩尿素8-10kg），孕穗期施穗肥（每亩复合肥15kg）。<br><br>🍅 <b>番茄施肥</b>：定植后7天施提苗肥，开花坐果期每隔15天追施一次复合肥。<br><br>💡 <b>通用原则</b>：基肥为主、追肥为辅；有机肥与化肥配合使用；少量多次，避免烧根。'
    ]
  },
  '浇水': {
    keywords: ['浇水', '灌溉', '水分', '干旱', '水量'],
    answers: [
      '灌溉管理建议：<br><br>💧 <b>灌溉时间</b>：建议早晨或傍晚灌溉，避免中午高温蒸发。<br><br>📊 <b>土壤湿度</b>：保持土壤湿度在60%-80%为宜，可通过系统环境监测模块实时查看。<br><br>🌱 <b>不同作物需水量</b>：<br>- 水稻：需保持水层3-5cm<br>- 蔬菜：保持土壤湿润即可<br>- 果树：深浇少浇，每次浇透'
    ]
  },
  '病虫害': {
    keywords: ['病虫害', '虫', '病', '发黄', '枯萎', '斑点', '防治', '灰霉', '飞虱'],
    answers: [
      '病虫害防治建议：<br><br>🔍 <b>常见症状判断</b>：<br>- 叶片发黄：可能缺氮或根系受损<br>- 叶片有斑点：可能是真菌性病害<br>- 叶片卷曲：可能有蚜虫或病毒病<br><br>🛡️ <b>防治原则</b>：<br>1. 预防为主，综合防治<br>2. 优先使用生物农药（如阿维菌素）<br>3. 注意安全间隔期<br>4. 轮换用药，避免抗药性<br><br>💊 <b>推荐药剂</b>：<br>- 杀虫：吡虫啉、阿维菌素<br>- 杀菌：多菌灵、百菌清<br>- 除草：草甘膦（注意避开作物）'
    ]
  },
  '种植': {
    keywords: ['种植', '播种', '密度', '间距', '什么时候种'],
    answers: [
      '种植技术要点：<br><br>📅 <b>播种时间</b>：<br>- 水稻：南方3-4月，北方5月<br>- 玉米：春玉米4月，夏玉米6月<br>- 番茄：大棚全年可种，露地3-4月<br><br>📏 <b>种植密度</b>：<br>- 水稻：行距25-30cm，株距15-20cm<br>- 玉米：行距60cm，株距25-30cm（每亩4000-4500株）<br>- 番茄：行距50-60cm，株距35-40cm<br><br>🌡️ <b>温度要求</b>：大多数作物发芽适温15-25℃，生长适温20-30℃。'
    ]
  },
  '草莓': {
    keywords: ['草莓'],
    answers: [
      '草莓种植管理：<br><br>🍓 <b>种植要点</b>：<br>- 适宜温度：15-25℃，怕高温<br>- 土壤：偏酸性（pH 5.5-6.5），排水良好<br>- 定植深度：不埋心、不露根<br><br>💧 <b>水肥管理</b>：<br>- 滴灌为主，保持土壤湿润<br>- 花期减少浇水，果期增加钾肥<br><br>🛡️ <b>常见病害</b>：<br>- 灰霉病：通风降湿 + 腐霉利喷雾<br>- 白粉病：控制湿度 + 三唑酮防治<br>- 炭疽病：高温多雨季节注意预防'
    ]
  },
  '温度': {
    keywords: ['温度', '高温', '低温', '冻害', '热'],
    answers: [
      '温度管理建议：<br><br>🌡️ <b>高温应对</b>：<br>- 遮阳网覆盖（遮光率50-70%）<br>- 增加灌溉频次，叶面喷水降温<br>- 大棚及时通风换气<br><br>❄️ <b>低温防护</b>：<br>- 覆盖地膜保温<br>- 大棚加装保温被<br>- 提前灌水蓄热<br><br>📊 您可以在系统的"环境监测"模块实时查看各农田温度数据，设置预警阈值自动提醒。'
    ]
  }
}

const defaultAnswers = [
  '这是一个很好的问题！根据我的农业知识库分析：<br><br>建议您关注以下几点：<br>1. 根据当地气候条件合理安排农事<br>2. 定期查看系统环境监测数据<br>3. 做好病虫害预防工作<br><br>如需更详细的指导，建议咨询当地农技站专家。您还可以在系统中记录种植日志，方便后续分析优化。',
  '感谢您的提问！基于智慧农业大数据分析：<br><br>🔬 <b>建议方案</b>：<br>1. 利用系统环境监测功能，实时掌握农田状况<br>2. 根据作物生长周期合理安排施肥灌溉<br>3. 建立农事任务提醒，不错过关键农时<br><br>您可以在"农事任务"模块创建定期提醒，系统会按时通知您执行相关操作。',
  '根据农业专家知识库，为您提供以下参考：<br><br>🌱 现代农业讲究精准管理，建议：<br>- 定期检测土壤养分状况<br>- 根据作物需求精准施肥<br>- 合理轮作，改善土壤结构<br>- 利用系统数据辅助决策<br><br>如果您能提供更具体的作物品种和问题描述，我可以给出更精准的建议！'
]

function getCurrentTime() {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
}

function findAnswer(text) {
  for (const category of Object.values(knowledgeBase)) {
    if (category.keywords.some(kw => text.includes(kw))) {
      return category.answers[Math.floor(Math.random() * category.answers.length)]
    }
  }
  return defaultAnswers[Math.floor(Math.random() * defaultAnswers.length)]
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

const sendMessage = async (text) => {
  if (!text || !text.trim()) return
  const userMsg = text.trim()
  inputText.value = ''

  messages.value.push({
    role: 'user',
    content: userMsg,
    time: getCurrentTime()
  })
  scrollToBottom()

  // 模拟AI思考
  isTyping.value = true
  scrollToBottom()

  const delay = 800 + Math.random() * 1200
  setTimeout(() => {
    isTyping.value = false
    const answer = findAnswer(userMsg)
    messages.value.push({
      role: 'ai',
      content: answer,
      time: getCurrentTime()
    })
    scrollToBottom()
  }, delay)
}
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
}

/* 悬浮按钮 */
.float-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #43a047, #2e7d32);
  box-shadow: 0 4px 16px rgba(46, 125, 50, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}
.float-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px rgba(46, 125, 50, 0.5);
}
.float-btn.active {
  transform: scale(0.95);
}
.float-btn-inner {
  position: relative;
}
.robot-icon {
  font-size: 26px;
}
.btn-pulse {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 12px;
  height: 12px;
  background: #4caf50;
  border-radius: 50%;
  border: 2px solid #fff;
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(76, 175, 80, 0.6); }
  70% { box-shadow: 0 0 0 8px rgba(76, 175, 80, 0); }
  100% { box-shadow: 0 0 0 0 rgba(76, 175, 80, 0); }
}

/* 聊天面板 */
.chat-panel {
  position: absolute;
  bottom: 70px;
  right: 0;
  width: 380px;
  height: 540px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, #2e7d32, #1b5e20);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chat-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ai-avatar-small {
  width: 36px;
  height: 36px;
  background: rgba(255,255,255,0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.chat-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}
.chat-status {
  font-size: 12px;
  color: rgba(255,255,255,0.7);
  display: flex;
  align-items: center;
  gap: 4px;
}
.status-dot {
  width: 6px;
  height: 6px;
  background: #69f0ae;
  border-radius: 50%;
}
.close-btn {
  color: rgba(255,255,255,0.8);
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: background 0.2s;
}
.close-btn:hover {
  background: rgba(255,255,255,0.15);
}

/* 聊天内容 */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-message {
  display: flex;
  gap: 8px;
  max-width: 85%;
}
.chat-message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.chat-message.ai .msg-avatar {
  background: #e8f5e9;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.7;
  color: #333;
}
.chat-message.ai .msg-bubble {
  background: #f5f7fa;
  border-top-left-radius: 4px;
}
.chat-message.user .msg-bubble {
  background: #2e7d32;
  color: #fff;
  border-top-right-radius: 4px;
}
.msg-time {
  font-size: 11px;
  color: #bbb;
  margin-top: 4px;
}
.chat-message.user .msg-time {
  color: rgba(255,255,255,0.6);
  text-align: right;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.typing-indicator span {
  width: 6px;
  height: 6px;
  background: #999;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* 快捷问题 */
.quick-questions {
  padding: 8px 16px 12px;
  border-top: 1px solid #f5f5f5;
}
.quick-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}
.quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.quick-item {
  font-size: 12px;
  padding: 5px 10px;
  background: #f0f9eb;
  color: #43a047;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e8f5e9;
}
.quick-item:hover {
  background: #43a047;
  color: #fff;
  border-color: #43a047;
}

/* 输入区 */
.chat-footer {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
}
.chat-footer :deep(.el-input-group__append) {
  background: #43a047;
  border-color: #43a047;
  color: #fff;
  padding: 0 14px;
}
.chat-footer :deep(.el-input__wrapper) {
  border-radius: 8px 0 0 8px;
}

/* 动画 */
.chat-fade-enter-active {
  animation: chatIn 0.3s ease;
}
.chat-fade-leave-active {
  animation: chatIn 0.2s ease reverse;
}
@keyframes chatIn {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>

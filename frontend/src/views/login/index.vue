<template>
  <div class="login-page">
    <!-- 左侧品牌区域 -->
    <div class="login-left">
      <!-- 动态粒子背景 -->
      <canvas ref="canvasRef" class="particle-canvas"></canvas>

      <div class="brand-content">
        <div class="brand-logo">
          <div class="logo-icon">🛰️</div>
          <h1>智能化农田面积检测系统</h1>
        </div>
        <p class="brand-slogan">遥感影像 · 无人机航拍 · AI智能识别</p>
        <div class="brand-desc">
          基于计算机视觉与深度学习技术，<br>实现农田区域自动识别、边界提取与面积精准计算
        </div>
        <div class="feature-list">
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
            </div>
            <div class="feature-text">
              <span class="feature-title">遥感/无人机影像处理</span>
              <span class="feature-sub">支持卫星遥感与无人机航拍影像上传分析</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
            </div>
            <div class="feature-text">
              <span class="feature-title">深度学习智能识别</span>
              <span class="feature-sub">U-Net / DeepLabV3+ 语义分割自动提取农田</span>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 22 8.5 22 15.5 12 22 2 15.5 2 8.5 12 2"/><line x1="12" y1="22" x2="12" y2="15.5"/><polyline points="22 8.5 12 15.5 2 8.5"/></svg>
            </div>
            <div class="feature-text">
              <span class="feature-title">精准面积计算</span>
              <span class="feature-sub">像素→地理坐标转换，高精度农田面积自动核算</span>
            </div>
          </div>
        </div>
      </div>

      <div class="brand-footer">
        <span>© 2026 Smart Agriculture · Remote Sensing &amp; Drone Detection</span>
      </div>
    </div>

    <!-- 右侧表单区域 -->
    <div class="login-right">
      <div class="form-container">
        <div class="form-header">
          <h2>{{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}</h2>
          <p>{{ activeTab === 'login' ? '登录您的账号以继续使用系统' : '注册新账号，开始农田面积检测' }}</p>
        </div>

        <!-- 登录表单 -->
        <div v-show="activeTab === 'login'">
          <el-form :model="loginForm" :rules="loginRules" ref="loginRef" size="large">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" @click="handleLogin" :loading="loading">登 录</el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>还没有账号？</span>
            <a @click="activeTab = 'register'">立即注册</a>
          </div>
        </div>

        <!-- 注册表单 -->
        <div v-show="activeTab === 'register'">
          <el-form :model="registerForm" :rules="registerRules" ref="registerRef" size="large">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码（至少6位）" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" prefix-icon="UserFilled" />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="请输入手机号" prefix-icon="Phone" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" @click="handleRegister" :loading="loading">注 册</el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>已有账号？</span>
            <a @click="activeTab = 'login'">返回登录</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { login, register } from '../../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('login')
const loading = ref(false)
const loginRef = ref(null)
const registerRef = ref(null)
const canvasRef = ref(null)

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', realName: '', phone: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

const handleLogin = async () => {
  await loginRef.value.validate()
  loading.value = true
  try {
    const res = await login(loginForm.value)
    userStore.setToken(res.data.token)
    userStore.setUser(res.data.user)
    ElMessage.success('登录成功')
    if (res.data.user.role === 'ADMIN') {
      router.push('/admin/dashboard')
    } else {
      router.push('/home/workbench')
    }
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  await registerRef.value.validate()
  loading.value = true
  try {
    await register(registerForm.value)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
  } finally {
    loading.value = false
  }
}

// 粒子动画
let animationId = null
onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let width = canvas.width = canvas.offsetWidth
  let height = canvas.height = canvas.offsetHeight

  const particles = []
  const particleCount = 60
  const connectionDistance = 120
  const colors = ['rgba(102,187,106,', 'rgba(129,199,132,', 'rgba(76,175,80,', 'rgba(165,214,167,']

  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * 0.6,
      vy: (Math.random() - 0.5) * 0.6,
      radius: Math.random() * 2 + 1,
      color: colors[Math.floor(Math.random() * colors.length)]
    })
  }

  const animate = () => {
    ctx.clearRect(0, 0, width, height)

    particles.forEach((p, i) => {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > width) p.vx *= -1
      if (p.y < 0 || p.y > height) p.vy *= -1

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = p.color + '0.7)'
      ctx.fill()

      for (let j = i + 1; j < particles.length; j++) {
        const dx = p.x - particles[j].x
        const dy = p.y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < connectionDistance) {
          ctx.beginPath()
          ctx.moveTo(p.x, p.y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(102,187,106,${0.15 * (1 - dist / connectionDistance)})`
          ctx.lineWidth = 0.8
          ctx.stroke()
        }
      }
    })

    animationId = requestAnimationFrame(animate)
  }

  animate()

  const handleResize = () => {
    width = canvas.width = canvas.offsetWidth
    height = canvas.height = canvas.offsetHeight
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
})
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
}

/* 左侧品牌区 */
.login-left {
  flex: 1;
  background: linear-gradient(160deg, #0d1f17 0%, #1a3a2a 40%, #142e22 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 520px;
  padding: 0 40px;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-bottom: 16px;
}
.logo-icon {
  font-size: 48px;
}
.brand-logo h1 {
  font-size: 30px;
  color: #fff;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0;
}

.brand-slogan {
  font-size: 17px;
  color: rgba(255,255,255,0.85);
  margin: 0 0 12px;
  font-weight: 300;
  letter-spacing: 2px;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255,255,255,0.5);
  line-height: 1.8;
  margin-bottom: 48px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  text-align: left;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  backdrop-filter: blur(4px);
  transition: all 0.3s;
}
.feature-item:hover {
  background: rgba(255,255,255,0.08);
  border-color: rgba(102,187,106,0.3);
  transform: translateX(4px);
}
.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(102,187,106,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #66bb6a;
  flex-shrink: 0;
}
.feature-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.feature-title {
  font-size: 15px;
  color: rgba(255,255,255,0.9);
  font-weight: 500;
}
.feature-sub {
  font-size: 13px;
  color: rgba(255,255,255,0.45);
}

.brand-footer {
  position: absolute;
  bottom: 28px;
  z-index: 1;
  color: rgba(255,255,255,0.3);
  font-size: 12px;
  letter-spacing: 0.5px;
}

/* 右侧表单区 */
.login-right {
  width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.form-container {
  width: 360px;
}
.form-header {
  margin-bottom: 40px;
}
.form-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 10px;
}
.form-header p {
  font-size: 15px;
  color: #86909c;
  margin: 0;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 10px;
  background: linear-gradient(135deg, #2e7d32 0%, #1b5e20 100%);
  border: none;
  letter-spacing: 4px;
}
.submit-btn:hover {
  background: linear-gradient(135deg, #43a047 0%, #2e7d32 100%);
}

.form-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #86909c;
}
.form-footer a {
  color: #2e7d32;
  font-weight: 600;
  cursor: pointer;
  margin-left: 4px;
  transition: color 0.2s;
}
.form-footer a:hover {
  color: #43a047;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
  box-shadow: 0 0 0 1px #e8e8e8 inset;
  transition: all 0.2s;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(46,125,50,0.2) inset;
}
:deep(.el-form-item) {
  margin-bottom: 24px;
}
</style>

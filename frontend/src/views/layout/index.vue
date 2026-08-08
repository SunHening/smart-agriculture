<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '240px'" class="admin-aside">
      <div class="aside-header">
        <div class="logo-area">
          <span class="logo-icon">🌾</span>
          <transition name="fade">
            <span v-show="!isCollapse" class="logo-text">智慧农业</span>
          </transition>
        </div>
      </div>
      <el-scrollbar class="menu-scrollbar">
        <el-menu
          :default-active="$route.path"
          router
          :collapse="isCollapse"
          :collapse-transition="false"
          background-color="transparent"
          text-color="rgba(255,255,255,0.7)"
          active-text-color="#fff"
        >
          <template v-for="item in menuList" :key="item.path">
            <el-menu-item :index="'/admin/' + item.path" class="admin-menu-item">
              <el-icon><component :is="item.meta.icon" /></el-icon>
              <template #title>{{ item.meta.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <!-- 主区域 -->
    <el-container class="admin-main-container">
      <!-- 顶栏 -->
      <header class="admin-header">
        <div class="header-left">
          <div class="collapse-trigger" @click="isCollapse = !isCollapse">
            <el-icon :size="18"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <div class="header-action">
            <el-tooltip content="全屏" placement="bottom">
              <el-icon :size="18" class="action-icon" @click="toggleFullscreen"><FullScreen /></el-icon>
            </el-tooltip>
          </div>
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-block">
              <el-avatar :size="32" :src="userStore.user.avatar" icon="UserFilled" />
              <div class="user-meta">
                <span class="user-name">{{ userStore.user.realName || userStore.user.username }}</span>
                <span class="user-role">管理员</span>
              </div>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password"><el-icon><Lock /></el-icon>修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="admin-content">
        <router-view />
      </main>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="400px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePwd">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { updatePassword } from '../../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const pwdVisible = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '' })

const menuList = computed(() => {
  const adminRoute = router.options.routes.find(r => r.path === '/admin')
  return adminRoute?.children || []
})

const handleCommand = (command) => {
  if (command === 'password') {
    pwdForm.value = { oldPassword: '', newPassword: '' }
    pwdVisible.value = true
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }
}

const handleChangePwd = async () => {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  try {
    await updatePassword(pwdForm.value)
    ElMessage.success('密码修改成功')
    pwdVisible.value = false
  } catch { ElMessage.error('修改失败') }
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏 */
.admin-aside {
  background: rgb(26, 46, 26);
  display: flex;
  flex-direction: column;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.aside-header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  white-space: nowrap;
}
.logo-icon {
  font-size: 26px;
  flex-shrink: 0;
}
.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}

.menu-scrollbar {
  flex: 1;
  padding: 8px 0;
}

.admin-menu-item {
  margin: 2px 8px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}
.admin-menu-item:hover {
  background: rgba(255,255,255,0.06) !important;
}
.admin-menu-item.is-active {
  background: rgba(67, 160, 71, 0.15) !important;
  color: #fff !important;
}

:deep(.el-menu) {
  border-right: none;
}
:deep(.el-menu--collapse .admin-menu-item) {
  margin: 2px 8px;
}

/* 顶栏 */
.admin-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.collapse-trigger {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.collapse-trigger:hover {
  background: #f5f5f5;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-action {
  display: flex;
  align-items: center;
}
.action-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}
.action-icon:hover {
  background: #f5f5f5;
  color: #333;
}

.user-block {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.user-block:hover {
  background: #f5f5f5;
}
.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.user-name {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a1a;
}
.user-role {
  font-size: 11px;
  color: #999;
}

/* 内容区 */
.admin-main-container {
  flex-direction: column;
  overflow: hidden;
}
.admin-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f7f8fa;
}

/* 过渡动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>

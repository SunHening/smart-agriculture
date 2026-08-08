<template>
  <el-container class="portal-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="portal-aside">
      <div class="aside-header">
        <span class="logo-icon">🌾</span>
        <span class="logo-text">农田检测系统</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,0.7)"
        active-text-color="#fff"
      >
        <el-menu-item v-for="tab in tabs" :key="tab.path" :index="tab.path" class="portal-menu-item">
          <el-icon><component :is="tab.icon" /></el-icon>
          <template #title>{{ tab.label }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主区域 -->
    <el-container class="portal-main">
      <!-- 顶栏 -->
      <header class="portal-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown @command="handleCommand" trigger="click">
          <div class="user-block">
            <el-avatar :size="32" icon="UserFilled" />
            <div class="user-meta">
              <span class="user-name">{{ userStore.user.realName || userStore.user.username }}</span>
              <span class="user-role">用户</span>
            </div>
            <el-icon :size="12"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <!-- 内容区 -->
      <main class="portal-body">
        <router-view />
      </main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const tabs = [
  { path: '/home/workbench', label: '工作台', icon: 'HomeFilled' },
  { path: '/home/field', label: '我的农田', icon: 'MapLocation' },
  { path: '/home/detection', label: '影像检测', icon: 'Picture' },
  { path: '/home/map', label: '地图标注', icon: 'MapLocation' },
  { path: '/home/customer-service', label: '智能客服', icon: 'ChatDotRound' },
  { path: '/home/profile', label: '个人中心', icon: 'User' }
]

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  } else if (cmd === 'profile') {
    router.push('/home/profile')
  } else if (cmd === 'password') {
    router.push('/home/profile')
  }
}
</script>

<style scoped>
.portal-layout {
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏 */
.portal-aside {
  background: linear-gradient(180deg, #1a2e1a 0%, #142614 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.aside-header {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.logo-icon {
  font-size: 24px;
  flex-shrink: 0;
}
.logo-text {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
  white-space: nowrap;
}

.portal-menu-item {
  margin: 4px 10px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}
.portal-menu-item:hover {
  background: rgba(255,255,255,0.06) !important;
}
.portal-menu-item.is-active {
  background: rgba(67, 160, 71, 0.2) !important;
  color: #fff !important;
}

:deep(.el-menu) {
  border-right: none;
}

/* 主区域 */
.portal-main {
  flex-direction: column;
  overflow: hidden;
}

.portal-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
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

.portal-body {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: #f7f8fa;
}
</style>

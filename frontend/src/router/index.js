import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue')
  },
  // ========== 管理端路由 ==========
  {
    path: '/admin',
    component: () => import('../views/admin/layout/index.vue'),
    redirect: '/admin/dashboard',
    meta: { role: 'ADMIN' },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/admin/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'DataBoard' }
      },
      {
        path: 'user',
        name: 'AdminUser',
        component: () => import('../views/admin/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'task',
        name: 'AdminTask',
        component: () => import('../views/admin/task/index.vue'),
        meta: { title: '任务监控', icon: 'Picture' }
      },
      {
        path: 'model',
        name: 'AdminModel',
        component: () => import('../views/admin/model/index.vue'),
        meta: { title: '模型配置', icon: 'Cpu' }
      },
      {
        path: 'ai-config',
        name: 'AdminAiConfig',
        component: () => import('../views/admin/ai-config/index.vue'),
        meta: { title: 'AI 配置', icon: 'MagicStick' }
      },
      {
        path: 'knowledge',
        name: 'AdminKnowledge',
        component: () => import('../views/admin/knowledge/index.vue'),
        meta: { title: '知识库', icon: 'Collection' }
      }
    ]
  },
  // ========== 用户端路由 ==========
  {
    path: '/home',
    component: () => import('../views/portal/layout.vue'),
    redirect: '/home/overview',
    meta: { role: 'USER' },
    children: [
      {
        path: 'overview',
        name: 'UserOverview',
        component: () => import('../views/portal/overview/index.vue'),
        meta: { title: '农场概览' }
      },
      {
        path: 'field',
        name: 'UserField',
        component: () => import('../views/portal/field/index.vue'),
        meta: { title: '地块管理' }
      },
      {
        path: 'tianditu',
        name: 'UserTianditu',
        component: () => import('../views/portal/tianditu/index.vue'),
        meta: { title: '天地图检测' }
      },
      {
        path: 'upload',
        name: 'UserUpload',
        component: () => import('../views/portal/upload/index.vue'),
        meta: { title: '上传影像' }
      },
      {
        path: 'record',
        name: 'UserRecord',
        component: () => import('../views/portal/record/index.vue'),
        meta: { title: '检测记录' }
      },
      {
        path: 'chat',
        name: 'UserChat',
        component: () => import('../views/portal/chat/index.vue'),
        meta: { title: 'AI 问答' }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('../views/portal/profile/index.vue'),
        meta: { title: '个人中心' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || '{}')

  if (to.path === '/login') {
    next()
    return
  }

  if (!token) {
    next('/login')
    return
  }

  // 根路径重定向
  if (to.path === '/') {
    next(user.role === 'ADMIN' ? '/admin/dashboard' : '/home/overview')
    return
  }

  // 权限校验
  if (to.path.startsWith('/admin') && user.role !== 'ADMIN') {
    next('/home/overview')
    return
  }
  if (to.path.startsWith('/home') && user.role === 'ADMIN') {
    next('/admin/dashboard')
    return
  }

  next()
})

export default router

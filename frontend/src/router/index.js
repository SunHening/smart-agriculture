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
    component: () => import('../views/layout/index.vue'),
    redirect: '/admin/dashboard',
    meta: { role: 'ADMIN' },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { title: '检测概览', icon: 'DataBoard' }
      },
      {
        path: 'user',
        name: 'AdminUser',
        component: () => import('../views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'field',
        name: 'AdminField',
        component: () => import('../views/field/index.vue'),
        meta: { title: '农田管理', icon: 'MapLocation' }
      },
      {
        path: 'detection',
        name: 'AdminDetection',
        component: () => import('../views/detection/index.vue'),
        meta: { title: '影像检测', icon: 'Picture' }
      },
      {
        path: 'map',
        name: 'AdminMap',
        component: () => import('../views/map/index.vue'),
        meta: { title: '地图标注', icon: 'MapLocation' }
      },
      {
        path: 'customer-service',
        name: 'AdminCustomerService',
        component: () => import('../views/customer-service/index.vue'),
        meta: { title: '智能客服', icon: 'ChatDotRound' }
      }
    ]
  },
  // ========== 用户端路由 ==========
  {
    path: '/home',
    component: () => import('../views/user-portal/layout.vue'),
    redirect: '/home/workbench',
    meta: { role: 'USER' },
    children: [
      {
        path: 'workbench',
        name: 'UserWorkbench',
        component: () => import('../views/user-portal/home/index.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'field',
        name: 'UserField',
        component: () => import('../views/user-portal/field/index.vue'),
        meta: { title: '我的农田' }
      },
      {
        path: 'detection',
        name: 'UserDetection',
        component: () => import('../views/detection/index.vue'),
        meta: { title: '影像检测' }
      },
      {
        path: 'map',
        name: 'UserMap',
        component: () => import('../views/map/index.vue'),
        meta: { title: '地图标注' }
      },
      {
        path: 'customer-service',
        name: 'UserCustomerService',
        component: () => import('../views/customer-service/index.vue'),
        meta: { title: '智能客服' }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('../views/user-portal/profile/index.vue'),
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
    next(user.role === 'ADMIN' ? '/admin/dashboard' : '/home/workbench')
    return
  }

  // 权限校验
  if (to.path.startsWith('/admin') && user.role !== 'ADMIN') {
    next('/home/workbench')
    return
  }
  if (to.path.startsWith('/home') && user.role === 'ADMIN') {
    next('/admin/dashboard')
    return
  }

  next()
})

export default router

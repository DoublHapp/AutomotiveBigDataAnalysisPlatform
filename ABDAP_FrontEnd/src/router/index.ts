import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useUserStore } from '@/stores/UserStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect:'/Auth'
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },

    {
      path: '/Auth',
      name: 'Auth',
      component: () => import('../views/Auth.vue'),
      meta: { 
        title: '登录', 
        requiresAuth: false // 不需要登录验证
      }
    },
    // 404 页面
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('../views/404View.vue'),
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 如果用户未登录且访问的不是登录页面
  if (!userStore.isLoggedIn && to.name !== 'Auth') {
    next({ name: 'Auth' })
  } else {
    next()
  }
})

export default router

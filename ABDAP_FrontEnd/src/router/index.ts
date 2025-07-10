import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/Auth',
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
        title: '登录注册',
        requiresAuth: false, // 不需要登录验证
      },
    },
    // 404 页面
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('../views/404View.vue'),
    },

    // 主要的应用布局路由
    {
      path: '/app',
      component: () => import('../layouts/LayoutView.vue'),
      meta: {
        requiresAuth: true,
      },
      children: [
        // 销售经理 (SalesManager) 页面
        {
          path: 'SaleTotal',
          name: 'SaleTotal',
          component: () => import('../views/SaleTotal.vue'),
          meta: {
            title: '销售统计总览',
            requiresAuth: true,
            roles: ['SalesManager'],
          },
        },
        {
          path: 'CarPurchasesHeatMap',
          name: 'CarPurchasesHeatMap',
          component: () => import('../views/CarPurchasesHeatMap.vue'),
          meta: {
            title: '购车热区地图',
            requiresAuth: true,
            roles: ['SalesManager'],
          },
        },
        {
          path: 'TimeSeries',
          name: 'TimeSeries',
          component: () => import('../views/TimeSeries.vue'),
          meta: {
            title: '时间序列可视化',
            requiresAuth: true,
            roles: ['SalesManager'],
          },
        },
        {
          path: 'SalesForecast',
          name: 'SalesForecast',
          component: () => import('../views/SalesForecast.vue'),
          meta: {
            title: '多维度销售预测',
            requiresAuth: true,
            roles: ['SalesManager'],
          },
        },
        {
          path: 'ModelSalesForcast',
          name: 'ModelSalesForcast',
          component: () => import('../views/ModelSalesForecast.vue'),
          meta: {
            title: '车型销售预测',
            requiresAuth: true,
            roles: ['SalesManager'],
          },
        },
        {
          path: 'RegionSalesForcast',
          name: 'RegionSalesForcast',
          component: () => import('../views/RegionSalesForecast.vue'),
          meta: {
            title: '区域销售预测',
            requiresAuth: true,
            roles: ['SalesManager'],
          },
        },
        // 消费者 (Customer) 页面
        {
          path: 'TopCarModelList',
          name: 'TopCarModelList',
          component: () => import('../views/TopCarModelList.vue'),
          meta: {
            title: '热门车型排行榜',
            requiresAuth: true,
            roles: ['Customer'],
          },
        },
        {
          path: 'VehicleModelCompAnalysis',
          name: 'VehicleModelCompAnalysis',
          component: () => import('../views/VehicleModelCompAnalysis .vue'),
          meta: {
            title: '车型对比分析',
            requiresAuth: true,
            roles: ['Customer'],
          },
        },
        {
          path: 'Recommendation',
          name: 'Recommendation',
          component: () => import('../views/Recommendation.vue'),
          meta: {
            title: '购车推荐分析',
            requiresAuth: true,
            roles: ['Customer'],
          },
        },
        {
          path: 'FuelConsList',
          name: 'FuelConsList',
          component: () => import('../views/FuelConsList.vue'),
          meta: {
            title: '油耗榜单',
            requiresAuth: true,
            roles: ['Customer'],
          },
        },
        // {
        //   path: 'Evaluative',
        //   name: 'Evaluative',
        //   component: () => import('../views/Evaluative.vue'),
        //   meta: {
        //     title: '口碑聚合分析',
        //     requiresAuth: true,
        //     roles: ['Customer'],
        //   },
        // },
        // 产品经理 (ProductManager) 页面
        //  {
        //   path: 'VehicleConfiguration',
        //   name: 'VehicleConfiguration',
        //   component: () => import('../views/VehicleConfiguration.vue'),
        //   meta: {
        //     title: '车辆配置热度分析',
        //     requiresAuth: true,
        //     roles: ['ProductManager'],
        //   },
        // },

        // {
        //    path: '/app/CompetitiveProductComp',
        //    name: 'CompetitiveProductComp',
        //    component: () => import('@/views/CompetitiveProductComp.vue'),
        //   meta: {
        //   title: '竞品对比分析',
        //   requiresAuth:true,
        //   roles: ['ProductManager']
        //   }
        //  },
      ],
    },

    // 重定向旧路由到新路由
    {
      path: '/SaleTotal',
      redirect: '/app/SaleTotal',
    },
    {
      path: '/TopCarModelList',
      redirect: '/app/TopCarModelList',
    },
    // {
    //   path: '/VehicleConfiguration',
    //   redirect: '/app/VehicleConfiguration',
    // },
    {
      path: '/CarPurchasesHeatMap',
      redirect: '/app/CarPurchasesHeatMap',
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 检查用户登录状态
  const userInfo = localStorage.getItem('userInfo')
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true'

  console.log('路由守卫检查:', {
    to: to.path,
    isLoggedIn,
    userInfo: userInfo ? JSON.parse(userInfo) : null,
    requiresAuth: to.meta?.requiresAuth,
  })

  // 如果页面需要认证但用户未登录
  if (to.meta?.requiresAuth && !isLoggedIn) {
    console.log('需要登录，跳转到登录页面')
    next({ name: 'Auth' })
    return
  }

  // 如果用户已登录且访问登录页面，重定向到对应角色的首页
  if (isLoggedIn && to.name === 'Auth') {
    if (userInfo) {
      const user = JSON.parse(userInfo)
      const roleDefaultRoutes: Record<string, string> = {
        SalesManager: '/SaleTotal',
        Customer: '/TopCarModelList',
        // ProductManager: '/VehicleConfiguration',
      }
      const defaultRoute = roleDefaultRoutes[user.role] || '/TopCarModelList'
      console.log('用户已登录，重定向到:', defaultRoute)
      next(defaultRoute)
      return
    }
  }

  // 角色权限检查
  if (
    to.meta?.requiresAuth &&
    to.meta?.roles &&
    Array.isArray(to.meta.roles) &&
    isLoggedIn &&
    userInfo
  ) {
    const user = JSON.parse(userInfo)
    const userRole = user.role

    // 修复类型错误：确保 roles 是数组且 userRole 存在
    if (!userRole || !to.meta.roles.includes(userRole)) {
      console.log('用户角色不匹配，重定向到默认页面')
      // 用户角色不匹配，重定向到用户的默认页面
      const roleDefaultRoutes: Record<string, string> = {
        SalesManager: '/SaleTotal',
        Customer: '/TopCarModelList',
        // ProductManager: '/VehicleConfiguration',
      }
      const defaultRoute = roleDefaultRoutes[userRole] || '/Auth'
      next(defaultRoute)
      return
    }
  }

  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - ABDAP 汽车大数据分析平台`
  }

  console.log('路由守卫通过，继续导航')
  next()
})

export default router

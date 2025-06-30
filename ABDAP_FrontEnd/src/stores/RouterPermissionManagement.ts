import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { RouteRecordRaw } from 'vue-router'

export interface MenuRoute {
  path: string
  name: string
  component?: string
  meta?: {
    title: string
    icon?: string
    permissions?: string[]
    roles?: string[]
    hidden?: boolean
    keepAlive?: boolean
  }
  children?: MenuRoute[]
}

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([])
  const menuRoutes = ref<MenuRoute[]>([])
  const isRoutesLoaded = ref(false)

  // 静态路由（无需权限）
  const constantRoutes: RouteRecordRaw[] = [
    {
      path: '/Auth',
      name: 'Auth',
      component: () => import('@/views/Auth.vue'),
      meta: { title: '登录', hidden: true },
    },
    {
      path: '/404',
      name: 'NotFound',
      component: () => import('@/views/404View.vue'),
      meta: { title: '页面未找到', hidden: true },
    },
  ]

  // 异步路由（需要权限控制）
  const asyncRoutes: MenuRoute[] = [
    {
      path: '/',
      name: 'Layout',
      component: 'Layout',
      meta: { title: '首页', icon: 'House' },
      children: [
        {
          path: '/SaleTotal',
          name: 'SaleTotal',
          component: 'views/SaleTotal',
          meta: {
            title: '销售总览',
            icon: 'TrendCharts',
            permissions: ['SaleTotal:view'],
          },
        },
        {
          path: '/TopCarModelList',
          name: 'TopCarModelList',
          component: 'views/TopCarModelList',
          meta: {
            title: '热门车型',
            icon: 'Star',
            permissions: ['TopCarModelList:view'],
          },
        },
      ],
    },
  ]

  // 根据权限过滤路由
  const filterAsyncRoutes = (
    routes: MenuRoute[],
    permissions: string[],
    roles: string[],
  ): MenuRoute[] => {
    const filteredRoutes: MenuRoute[] = []

    routes.forEach((route) => {
      const tempRoute = { ...route }

      // 检查权限
      if (hasPermission(tempRoute, permissions, roles)) {
        // 递归过滤子路由
        if (tempRoute.children) {
          tempRoute.children = filterAsyncRoutes(tempRoute.children, permissions, roles)
        }
        filteredRoutes.push(tempRoute)
      }
    })

    return filteredRoutes
  }

  // 检查是否有权限访问路由
  const hasPermission = (route: MenuRoute, permissions: string[], roles: string[]): boolean => {
    // 如果路由指定了角色要求
    if (route.meta?.roles) {
      return route.meta.roles.some((role) => roles.includes(role))
    }

    // 如果路由指定了权限要求
    if (route.meta?.permissions) {
      return route.meta.permissions.some((permission) => permissions.includes(permission))
    }

    // 没有指定权限要求的路由默认可访问
    return true
  }

  // 动态组件导入 - 修复文件路径
  const componentMap: Record<string, () => Promise<any>> = {
    Layout: () => import('@/layouts/LayoutView.vue'),
    'views/SaleTotal': () => import('@/views/SaleTotal.vue'),
    'views/TopCarModelList': () => import('@/views/TopCarModelList.vue'),
  }

  // 将 MenuRoute 转换为 RouteRecordRaw
  const transformRoutes = (menuRoutes: MenuRoute[]): RouteRecordRaw[] => {
    return menuRoutes.map((route) => {
      const routeRecord: RouteRecordRaw = {
        path: route.path,
        name: route.name,
        component: route.component ? componentMap[route.component] : undefined,
        meta: route.meta,
        children: route.children ? transformRoutes(route.children) : undefined,
      }
      return routeRecord
    })
  }

  // 生成动态路由
  const generateRoutes = (permissions: string[], roles: string[]) => {
    const accessedRoutes = filterAsyncRoutes(asyncRoutes, permissions, roles)
    const transformedRoutes = transformRoutes(accessedRoutes)

    routes.value = constantRoutes.concat(transformedRoutes)
    menuRoutes.value = accessedRoutes
    isRoutesLoaded.value = true

    return transformedRoutes
  }

  // 重置路由
  const resetRoutes = () => {
    routes.value = []
    menuRoutes.value = []
    isRoutesLoaded.value = false
  }

  return {
    routes,
    menuRoutes,
    isRoutesLoaded,
    constantRoutes,
    generateRoutes,
    resetRoutes,
  }
})
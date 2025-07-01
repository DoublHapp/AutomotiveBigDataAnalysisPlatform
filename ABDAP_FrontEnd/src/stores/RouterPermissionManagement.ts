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

  // 基础菜单配置
  const baseMenus: MenuRoute[] = [
    {
      path: '/app/SaleTotal',
      name: 'SaleTotal',
      meta: {
        title: '销售总览',
        icon: 'TrendCharts',
        roles: ['SalesManager'],
      },
    },
    {
      path: '/app/TopCarModelList',
      name: 'TopCarModelList',
      meta: {
        title: '热门车型',
        icon: 'Star',
        roles: ['SalesManager', 'Customer'],
      },
    },
    {
      path: '/app/VehicleConfiguration',
      name: 'VehicleConfiguration',
      meta: {
        title: '车辆配置',
        icon: 'Setting',
        roles: ['ProductManager'],
      },
    },
    {
      path: '/app/CarPurchasesHeatMap',
      name: 'CarPurchasesHeatMap',
      meta: {
        title: '购车热力图',
        icon: 'Location',
        roles: ['Customer'],
      },
    },
    {
      path: '/app/SalesForecast',
      name: 'SalesForecast',
      meta: {
        title: '销售预测',
        icon: 'DataAnalysis',
        roles: ['SalesManager'],
      },
    },
    {
      path: '/app/Recommendation',
      name: 'Recommendation',
      meta: {
        title: '购车推荐',
        icon: 'Star',
        roles: ['Customer'],
      },
    },
  ]

  // 根据用户角色生成菜单
  const generateMenus = (userRole: string) => {
    const filteredMenus = baseMenus.filter((menu) => {
      return menu.meta?.roles?.includes(userRole)
    })

    menuRoutes.value = filteredMenus
    isRoutesLoaded.value = true

    return filteredMenus
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
    generateMenus,
    resetRoutes,
  }
})

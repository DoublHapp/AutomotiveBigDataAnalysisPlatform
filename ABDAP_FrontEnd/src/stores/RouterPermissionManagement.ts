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

  // 基础菜单配置(按角色分组)
  const baseMenus: MenuRoute[] = [
    // 销售经理 (SalesManager) 菜单
    {
      path: '/app/SaleTotal',
      name: 'SaleTotal',
      meta: {
        title: '销售统计总览',
        icon: 'TrendCharts',
        roles: ['SalesManager'],
        keepAlive: true,
      },
    },
    {
      path: '/app/CarPurchasesHeatMap',
      name: 'CarPurchasesHeatMap',
      meta: {
        title: '购车热区地图展示',
        icon: 'Location',
        roles: ['SalesManager'],
        keepAlive: true,
      },
    },
    {
      path: '/app/TimeSeries',
      name: 'TimeSeries',
      meta: {
        title: '时间序列可视化',
        icon: 'Timer',
        roles: ['SalesManager'],
        keepAlive: true,
      },
    },
    {
      path: '/app/SalesForecast',
      name: 'SalesForecast',
      meta: {
        title: '多维度销售预测',
        icon: 'DataAnalysis',
        roles: ['SalesManager'],
        keepAlive: true,
      },
    },
    {
      path: '/app/ModelSalesForcast',
      name: 'ModelSalesForcast',
      meta: {
        title: '车型销售预测',
        icon: 'DataLine',
        roles: ['SalesManager'],
        keepAlive: true,
      },
    },
    {
      path: '/app/RegionSalesForcast',
      name: 'RegionSalesForcast',
      meta: {
        title: '区域销售预测',
        icon: 'MapLocation',
        roles: ['SalesManager'],
        keepAlive: true,
      },
    },
    //消费者(Customer) 菜单
    {
      path: '/app/TopCarModelList',
      name: 'TopCarModelList',
      meta: {
        title: '热门车型排行榜',
        icon: 'Star',
        roles: ['Customer'],
        keepAlive: true,
      },
    },
    {
      path: '/app/VehicleModelCompAnalysis',
      name: 'VehicleModelCompAnalysis',
      meta: {
        title: '车型对比分析',
        icon: 'Tools',
        roles: ['Customer'],
        keepAlive: true,
      },
    },
    {
      path: '/app/Recommendation',
      name: 'Recommendation',
      meta: {
        title: '购车推荐分析',
        icon: 'ChatLineRound',
        roles: ['Customer'],
        keepAlive: true,
      },
    },
    {
      path: '/app/FuelConsList',
      name: 'FuelConsList',
      meta: {
        title: '油耗榜单',
        icon: 'GasPump',
        roles: ['Customer'],
        keepAlive: true,
      },
    },
    {
      path: '/app/Evaluative',
      name: 'Evaluative',
      meta: {
        title: '口碑聚合分析',
        icon: 'ChatDotRound',
        roles: ['Customer'],
        keepAlive: true,
      },
    },

    //产品经理 (ProductManager) 菜单
    {
      path: '/app/VehicleConfiguration',
      name: 'VehicleConfiguration',
      meta: {
        title: '车辆配置热度分析',
        icon: 'Setting',
        roles: ['ProductManager'],
        keepAlive: true,
      },
    },
    {
      path: '/app/CompetitiveProductComp',
      name: 'CompetitiveProductComp',
      meta: {
        title: '竞品对比分析',
        icon: 'DataBoard',
        roles: ['ProductManager'],
        keepAlive: true,
      },
    },
  ]

  // 根据用户角色生成菜单
  const generateMenus = (userRole: string) => {
    console.log('生成菜单，用户角色:', userRole)
    const filteredMenus = baseMenus.filter((menu) => {
      return menu.meta?.roles?.includes(userRole)
    })

    menuRoutes.value = filteredMenus
    isRoutesLoaded.value = true

    return filteredMenus
  }

  // 根据角色获取默认首页
  const getDefaultRoute = (userRole: string): string => {
    const defaultRoutes: Record<string, string> = {
      SalesManager: '/app/SaleTotal',
      Customer: '/app/TopCarModelList',
      ProductManager: '/app/VehicleConfiguration',
    }
    return defaultRoutes[userRole] || '/app/SaleTotal'
  }

  // 检查用户是否有权限访问指定路由
  const hasPermission = (userRole: string, routePath: string): boolean => {
    const targetMenu = baseMenus.find(menu => menu.path === routePath)
    return targetMenu?.meta?.roles?.includes(userRole) || false
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
    getDefaultRoute,
    hasPermission,
    resetRoutes,
  }
})

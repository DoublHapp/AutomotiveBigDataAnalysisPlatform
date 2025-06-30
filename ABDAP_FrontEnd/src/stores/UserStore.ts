import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export interface UserInfo {
  id: number
  username: string
  roles: string[]
  permissions: string[]
}

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const token = ref<string>('')

  const isLoggedIn = computed(() => !!token.value)
  const userRoles = computed(() => userInfo.value?.roles || [])
  const userPermissions = computed(() => userInfo.value?.permissions || [])

  // 模拟登录
  const login = async (username: string, password: string) => {
    // 模拟API调用
    await new Promise((resolve) => setTimeout(resolve, 1000))

    // 验证用户凭据并返回用户信息
    let mockUser: UserInfo | null = null

    if (username === 'DB' && password === '123') {
      mockUser = {
        id: 1,
        username: 'DB',
        roles: ['SalesManager'],
        permissions: ['Auth:view', 'SaleTotal:view'],
      }
    } else if (username === 'LJJ' && password === '1234') {
      mockUser = {
        id: 2,
        username: 'LJJ',
        roles: ['Customer'],
        permissions: ['Auth:view', 'TopCarModelList:view'],
      }
    }

    if (!mockUser) {
      throw new Error('用户名或密码错误')
    }

    userInfo.value = mockUser
    token.value = `mock-token-${mockUser.username}-${Date.now()}`

    return mockUser
  }

  const logout = () => {
    userInfo.value = null
    token.value = ''
  }

  const hasPermission = (permission: string) => {
    return userPermissions.value.includes(permission)
  }

  const hasRole = (role: string) => {
    return userRoles.value.includes(role)
  }

  return {
    userInfo,
    token,
    isLoggedIn,
    userRoles,
    userPermissions,
    login,
    logout,
    hasPermission,
    hasRole,
  }
})

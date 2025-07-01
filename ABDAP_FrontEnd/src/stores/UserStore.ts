import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export interface UserInfo {
  user_id: number // 改为匹配后端响应
  username: string
  role: string // 改为单个角色字符串
  permissions: string[]
}

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const token = ref<string>('')

  const isLoggedIn = computed(() => !!userInfo.value && !!token.value)
  const userRole = computed(() => userInfo.value?.role || '')
  const userPermissions = computed(() => userInfo.value?.permissions || [])

  // 从 localStorage 初始化用户信息
  const initFromStorage = () => {
    const storedUserInfo = localStorage.getItem('userInfo')
    const storedIsLoggedIn = localStorage.getItem('isLoggedIn')

    if (storedUserInfo && storedIsLoggedIn === 'true') {
      userInfo.value = JSON.parse(storedUserInfo)
      token.value = `token-${userInfo.value?.username}-${Date.now()}`
    }
  }

  // 登录
  const login = async (userData: UserInfo) => {
    userInfo.value = userData
    token.value = `token-${userData.username}-${Date.now()}`

    // 同步到 localStorage
    localStorage.setItem('userInfo', JSON.stringify(userData))
    localStorage.setItem('isLoggedIn', 'true')

    return userData
  }

  // 登出
  const logout = () => {
    userInfo.value = null
    token.value = ''

    // 清除 localStorage
    localStorage.removeItem('userInfo')
    localStorage.removeItem('isLoggedIn')
  }

  const hasPermission = (permission: string) => {
    return userPermissions.value.includes(permission)
  }

  const hasRole = (role: string) => {
    return userRole.value === role
  }

  return {
    userInfo,
    token,
    isLoggedIn,
    userRole,
    userPermissions,
    initFromStorage,
    login,
    logout,
    hasPermission,
    hasRole,
  }
})

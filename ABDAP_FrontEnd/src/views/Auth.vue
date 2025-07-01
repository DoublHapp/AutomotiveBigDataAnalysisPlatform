<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { User, Lock, Operation } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/UserStore'
import { usePermissionStore } from '@/stores/RouterPermissionManagement'

// 使用 Operation 图标替代 Car（或者使用其他合适的图标）
const CarButton = Operation

const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: 'DB',
  password: '123',
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 登录
        const userInfo = await userStore.login(loginForm.username, loginForm.password)

        // 生成动态路由
        const accessRoutes = permissionStore.generateRoutes(userInfo.permissions, userInfo.roles)

        // 动态添加路由
        accessRoutes.forEach((route) => {
          router.addRoute(route)
        })

        // 添加404路由到最后
        router.addRoute({
          path: '/:pathMatch(.*)*',
          redirect: '/404',
        })

        ElMessage.success('登录成功')

        // 根据用户权限跳转到合适的页面
        if (userInfo.permissions.includes('SaleTotal:view')) {
          router.push('/SaleTotal')
        } else if (userInfo.permissions.includes('TopCarModelList:view')) {
          router.push('/TopCarModelList')
        } else {
          router.push('/')
        }
      } catch (error) {
        ElMessage.error('登录失败，请检查用户名和密码')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <el-icon :size="40" color="#409EFF">
            <CarButton />
          </el-icon>
          <h2>汽车大数据分析平台</h2>
        </div>
      </template>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="demo-accounts">
        <h4>演示账号：</h4>
        <p>DB / 123 (销售经理权限)</p>
        <p>LJJ / 1234 (客户权限)</p>
      </div>

      <template #footer>
        <dive class="more-options">
          <el-button type="text" @click="router.push('/register')">注册新用户</el-button>
          <el-button type="text" @click="router.push('/forgot-password')">忘记密码</el-button>
        </dive>
      </template>
    </el-card>
  </div>
</template>


<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.login-header h2 {
  margin: 0;
  color: #333;
  font-weight: 600;
}

.login-form {
  margin-top: 20px;
}

.login-button {
  width: 100%;
}

.demo-accounts {
  margin-top: 20px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.demo-accounts h4 {
  margin: 0 0 8px 0;
  color: #666;
  font-size: 14px;
}

.demo-accounts p {
  margin: 4px 0;
  color: #888;
  font-size: 12px;
}

.more-options {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
}
</style>

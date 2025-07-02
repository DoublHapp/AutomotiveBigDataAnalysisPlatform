<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/UserStore'
import { usePermissionStore } from '@/stores/RouterPermissionManagement'
import {
  User,
  Lock,
  Operation,
  Right,
  Avatar,
  TrendCharts,
  DataAnalysis,
  Monitor,
} from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

// 表单引用
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

// 状态管理
const isLogin = ref(true)
const loading = ref(false)
const rememberMe = ref(false)
const agreeTerms = ref(false)

// 登录表单
const loginForm = reactive({
  username: '',
  password: '',
})

// 注册表单
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  role: '',
})

// 验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, message: '密码长度至少 3 个字符', trigger: 'blur' },
  ],
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, message: '密码长度至少 3 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

// 角色权限映射
const rolePermissions = {
  SalesManager: [
    'SaleTotal:view',
    'CarPurchasesHeatMap:view',
    'TimeSeries:view',
    'SalesForecast:view',
    'ModelSalesForcast:view',
    'RegionSalesForcast:view'
  ],
  Customer: [
    'TopCarModelList:view',
    'VehicleModelCompAnalysis:view',
    'Recommendation:view',
    'FuelConsList:view',
    'Evaluative:view'
  ],
  ProductManager: [
    'VehicleConfiguration:view',
    'VehicleModelCompAnalysis:view'
  ],
}

// 角色默认路由
const roleDefaultRoutes = {
  SalesManager: '/app/SaleTotal',
  Customer: '/app/TopCarModelList',
  ProductManager: '/app/VehicleConfiguration',
}

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        console.log('发送登录请求:', {
          username: loginForm.username,
          password: loginForm.password,
        })

        // 调用登录API
        const response = await axios.post('/api/auth/login', {
          username: loginForm.username,
          password: loginForm.password,
        })

        console.log('登录响应完整数据:', response.data)

        if (response.data.status === 1) {
          // 登录成功
          ElMessage.success(response.data.msg || '登录成功')

          // 存储用户信息
          const userInfo = response.data.data

          console.log('用户信息:', userInfo)

          // 确认用户信息包含必要字段
          if (!userInfo || !userInfo.role) {
            console.error('用户信息缺失必要字段:', userInfo)
            ElMessage.error('登录响应数据异常')
            return
          }

          // 使用 Store 管理用户状态
          await userStore.login(userInfo)

          // 生成菜单
          permissionStore.generateMenus(userInfo.role)

          // 根据角色跳转到对应页面
          const defaultRoute = roleDefaultRoutes[userInfo.role as keyof typeof roleDefaultRoutes]
          await router.push(defaultRoute)
          console.log('准备跳转到:', defaultRoute, '用户角色:', userInfo.role)

          // 使用 setTimeout 确保状态更新完成
          setTimeout(async () => {
            try {
              await router.push(defaultRoute)
              console.log('跳转完成到:', defaultRoute)
            } catch (routerError) {
              console.error('路由跳转失败:', routerError)
              ElMessage.error('页面跳转失败')
            }
          }, 100)
        } else {
          // 登录失败
          console.error('登录失败，服务器返回:', response.data)
          ElMessage.error(response.data.msg || '登录失败')
        }
      } catch (error: any) {
        console.error('登录API调用失败:', error)
        if (error.response) {
          console.error('错误响应:', error.response.data)
          ElMessage.error(error.response.data.msg || '登录失败')
        } else {
          console.error('网络错误，使用模拟登录:', error)
          // 网络错误时使用模拟登录
          await mockLogin()
        }
      } finally {
        loading.value = false
      }
    }
  })
}

// 模拟登录（用于开发测试）
const mockLogin = async () => {
  const { username, password } = loginForm

  // 模拟用户数据
  const mockUsers = {
    DB: { password: '123456', role: 'SalesManager', user_id: 1 },
    LJJ: { password: '123456', role: 'Customer', user_id: 2 },
    PM: { password: '123456', role: 'ProductManager', user_id: 3 },
  }

  const user = mockUsers[username as keyof typeof mockUsers]

  if (!user) {
    ElMessage.error('该账号不存在，请注册')
    return
  }

  if (user.password !== password) {
    ElMessage.error('密码错误，请重新输入')
    return
  }

  // 模拟成功响应
  const userInfo = {
    user_id: user.user_id,
    username: username,
    role: user.role,
    permissions: rolePermissions[user.role as keyof typeof rolePermissions],
  }

  localStorage.setItem('userInfo', JSON.stringify(userInfo))
  localStorage.setItem('isLoggedIn', 'true')

  ElMessage.success('登录成功')

  // 根据角色跳转
  const defaultRoute = roleDefaultRoutes[user.role as keyof typeof roleDefaultRoutes]
  router.push(defaultRoute)
}

// 注册处理
const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 调用注册API
        const response = await axios.post('/api/auth/register', {
          username: registerForm.username,
          password: registerForm.password,
          role: registerForm.role,
        })

        if (response.data.status === 1) {
          // 注册成功
          ElMessage.success(response.data.msg)

          // 切换到登录模式并填充用户名
          isLogin.value = true
          loginForm.username = registerForm.username

          // 清空注册表单
          resetRegisterForm()
        } else {
          // 注册失败
          ElMessage.error(response.data.msg)
        }
      } catch (error: any) {
        console.error('注册API调用失败:', error)
        ElMessage.error('注册失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
}

// 快速登录（演示账号）
const quickLogin = (username: string, password: string, role: string) => {
  loginForm.username = username
  loginForm.password = password
  isLogin.value = true
  handleLogin()
}

// 切换到登录
const switchToLogin = () => {
  isLogin.value = true
  resetForms()
}

// 切换到注册
const switchToRegister = () => {
  isLogin.value = false
  resetForms()
}

// 重置表单
const resetForms = () => {
  loginFormRef.value?.resetFields()
  registerFormRef.value?.resetFields()

  Object.assign(loginForm, {
    username: '',
    password: '',
  })

  resetRegisterForm()
}

const resetRegisterForm = () => {
  Object.assign(registerForm, {
    username: '',
    password: '',
    confirmPassword: '',
    role: '',
  })
  agreeTerms.value = false
}

// 忘记密码
const showForgotPassword = () => {
  ElMessageBox.alert('请联系管理员重置密码', '忘记密码', {
    confirmButtonText: '确定',
  })
}

// 显示用户协议
const showTerms = () => {
  ElMessageBox.alert(
    '这里是用户协议的内容...\n\n1. 用户应当遵守相关法律法规\n2. 禁止进行任何违法违规行为\n3. 保护个人账号信息安全\n4. 不同角色用户只能访问对应权限的功能',
    '用户协议',
    {
      confirmButtonText: '我已阅读',
    },
  )
}
</script>





<template>
  <div class="auth-container">
    <div class="auth-background">
      <div class="bg-decoration">
        <div class="decoration-circle circle-1"></div>
        <div class="decoration-circle circle-2"></div>
        <div class="decoration-circle circle-3"></div>
      </div>
    </div>

    <div class="auth-content">
      <!-- 左侧信息展示 -->
      <div class="auth-info">
        <div class="info-content">
          <h1>汽车大数据分析平台</h1>
          <h2>ABDAP - Automotive Big Data Analysis Platform</h2>
          <p class="description">基于先进的大数据技术，为汽车行业提供全面的数据分析和洞察服务</p>
          <div class="features">
            <div class="feature-item">
              <el-icon><TrendCharts /></el-icon>
              <span>销售数据分析</span>
            </div>
            <div class="feature-item">
              <el-icon><Operation /></el-icon>
              <span>车型热度排行</span>
            </div>
            <div class="feature-item">
              <el-icon><DataAnalysis /></el-icon>
              <span>市场趋势预测</span>
            </div>
            <div class="feature-item">
              <el-icon><Monitor /></el-icon>
              <span>定时数据监控</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录注册表单 -->
      <div class="auth-form-container">
        <el-card class="auth-card" shadow="always">
          <template #header>
            <div class="auth-header">
              <el-icon :size="40" color="#409EFF">
                <Operation />
              </el-icon>
              <h3>{{ isLogin ? '登录系统' : '注册账号' }}</h3>
              <p>{{ isLogin ? '欢迎回来！请输入您的账号信息' : '创建新账号开始使用' }}</p>
            </div>
          </template>

          <!-- 切换登录/注册 -->
          <div class="auth-switch">
            <el-button
              :type="isLogin ? 'primary' : 'default'"
              @click="switchToLogin"
              class="switch-btn"
            >
              登录
            </el-button>
            <el-button
              :type="!isLogin ? 'primary' : 'default'"
              @click="switchToRegister"
              class="switch-btn"
            >
              注册
            </el-button>
          </div>

          <!-- 登录表单 -->
          <el-form
            v-if="isLogin"
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="auth-form"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                size="large"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                clearable
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item>
              <div class="form-options">
                <el-checkbox v-model="rememberMe">记住我</el-checkbox>
                <el-link type="primary" @click="showForgotPassword">忘记密码？</el-link>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="auth-submit-btn"
                :loading="loading"
                @click="handleLogin"
              >
                <el-icon v-if="!loading"><Right /></el-icon>
                {{ loading ? '登录中...' : '立即登录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 注册表单 -->
          <el-form
            v-else
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            class="auth-form"
          >
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名"
                size="large"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请确认密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item prop="role">
              <el-select
                v-model="registerForm.role"
                placeholder="请选择角色"
                size="large"
                style="width: 100%"
              >
                <el-option label="销售经理" value="SalesManager">
                  <div class="role-option">
                    <span>销售经理</span>
                    <small>可查看销售总览、车型排行等销售数据</small>
                  </div>
                </el-option>
                <el-option label="消费者" value="Customer">
                  <div class="role-option">
                    <span>消费者</span>
                    <small>可查看热门车型、购车推荐等信息</small>
                  </div>
                </el-option>
                <el-option label="产品经理" value="ProductManager">
                  <div class="role-option">
                    <span>产品经理</span>
                    <small>可查看产品配置、对比分析等功能</small>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item>
              <div class="form-options">
                <el-checkbox v-model="agreeTerms">
                  我已阅读并同意
                  <el-link type="primary" @click="showTerms">《用户协议》</el-link>
                </el-checkbox>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="auth-submit-btn"
                :loading="loading"
                :disabled="!agreeTerms"
                @click="handleRegister"
              >
                <el-icon v-if="!loading"><Right /></el-icon>
                {{ loading ? '注册中...' : '立即注册' }}
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 演示账号信息 -->
          <div class="demo-accounts">
            <el-divider>演示账号</el-divider>
            <div class="demo-item" @click="quickLogin('DB', '123', 'SalesManager')">
              <el-icon><Avatar /></el-icon>
              <div>
                <p><strong>DB</strong> - 销售经理</p>
                <small>可查看销售总览数据</small>
              </div>
            </div>
            <div class="demo-item" @click="quickLogin('LJJ', '1234', 'Customer')">
              <el-icon><Avatar /></el-icon>
              <div>
                <p><strong>LJJ</strong> - 消费者</p>
                <small>可查看热门车型排行</small>
              </div>
            </div>
            <div class="demo-item" @click="quickLogin('PM', '123456', 'ProductManager')">
              <el-icon><Avatar /></el-icon>
              <div>
                <p><strong>PM</strong> - 产品经理</p>
                <small>可查看产品配置分析</small>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>



<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  position: relative;
  overflow: hidden;
}

.auth-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 0;
}

.bg-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 200px;
  height: 200px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.circle-2 {
  width: 300px;
  height: 300px;
  top: 60%;
  right: 10%;
  animation-delay: 2s;
}

.circle-3 {
  width: 150px;
  height: 150px;
  bottom: 10%;
  left: 50%;
  animation-delay: 4s;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-20px);
  }
}

.auth-content {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  min-height: 100vh;
}

.auth-info {
  flex: 0 0 45%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px; /* 减少内边距 */
  color: white;
}

.info-content h1 {
  font-size: 42px; /* 稍微减小字体 */
  font-weight: 700;
  margin-bottom: 16px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.info-content h2 {
  font-size: 22px; /* 稍微减小字体 */
  font-weight: 400;
  margin-bottom: 32px;
  opacity: 0.9;
}

.description {
  font-size: 16px; /* 稍微减小字体 */
  line-height: 1.6;
  margin-bottom: 40px; /* 减少边距 */
  opacity: 0.8;
}

.features {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px; /* 减少间距 */
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px; /* 减小字体 */
  opacity: 0.9;
}

.feature-item .el-icon {
  font-size: 20px; /* 减小图标 */
}

.auth-form-container {
  flex: 0 0 55%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px; /* 减少内边距，为卡片留出更多空间 */
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.auth-card {
  width: 100%; /* 占据容器的全部宽度 */
  height: 100%; /* 占据容器的全部高度 */
  max-width: none; /* 移除最大宽度限制 */
  max-height: 90vh; /* 设置最大高度避免超出视窗 */
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

/* 修改卡片内容布局，使其充分利用空间 */
.auth-card .el-card__header {
  flex-shrink: 0; /* 头部不缩放 */
  padding: 32px; /* 增加头部内边距 */
}

.auth-card .el-card__body {
  flex: 1; /* 主体内容占据剩余空间 */
  padding: 0 32px 32px 32px; /* 增加内边距 */
  display: flex;
  flex-direction: column;
  overflow-y: auto; /* 内容过多时可滚动 */
}

.auth-header {
  text-align: center;
  padding: 0; /* 移除默认内边距，由卡片头部控制 */
}

.auth-header h3 {
  margin: 16px 0 8px 0;
  font-size: 28px; /* 增大标题字体 */
  color: #303133;
}

.auth-header p {
  margin: 0;
  color: #909399;
  font-size: 16px; /* 增大字体 */
}

.auth-switch {
  display: flex;
  margin-bottom: 32px;
  background: #f5f7fa;
  border-radius: 10px; /* 增大圆角 */
  padding: 8px; /* 增加内边距 */
}

.switch-btn {
  flex: 1;
  border: none;
  background: transparent;
  border-radius: 8px; /* 增大圆角 */
  height: 48px; /* 增加按钮高度 */
  font-size: 16px; /* 增大字体 */
  font-weight: 500;
  transition: all 0.3s ease;
}

.switch-btn.el-button--primary {
  background: #409eff;
  color: white;
}

.auth-form {
  margin-top: 24px;
  flex: 1; /* 表单占据剩余空间 */
}

/* 增大表单项间距 */
.auth-form .el-form-item {
  margin-bottom: 28px; /* 增加间距 */
}

/* 增大输入框 */
.auth-form .el-input__wrapper {
  min-height: 52px; /* 增加输入框高度 */
  font-size: 16px; /* 增大字体 */
}

/* 增大选择框 */
.auth-form .el-select .el-input__wrapper {
  min-height: 52px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-top: 8px;
}

.auth-submit-btn {
  width: 100%;
  height: 56px; /* 增加按钮高度 */
  font-size: 18px; /* 增大字体 */
  font-weight: 600;
  border-radius: 10px; /* 增大圆角 */
  margin-top: 16px; /* 增加上边距 */
}

.role-option {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 0; /* 增加内边距 */
}

.role-option small {
  color: #909399;
  font-size: 13px; /* 稍微增大字体 */
}

.demo-accounts {
  margin-top: 40px; /* 增加上边距 */
  flex-shrink: 0; /* 演示账号区域不缩放 */
}

.demo-item {
  display: flex;
  align-items: center;
  gap: 16px; /* 增加间距 */
  padding: 16px 20px; /* 增加内边距 */
  border-radius: 10px; /* 增大圆角 */
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 12px;
  border: 1px solid transparent;
}

.demo-item:hover {
  background: #f0f9ff;
  border-color: #409eff;
  transform: translateY(-2px); /* 增加悬停效果 */
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.demo-item .el-icon {
  font-size: 32px; /* 增大图标 */
  color: #409eff;
}

.demo-item p {
  margin: 0 0 4px 0;
  font-size: 16px; /* 增大字体 */
  color: #303133;
}

.demo-item small {
  color: #909399;
  font-size: 14px; /* 增大字体 */
}

/* 响应式设计优化 */
@media (max-width: 1200px) {
  .auth-info {
    flex: 0 0 40%;
  }

  .auth-form-container {
    flex: 0 0 60%;
  }

  .info-content h1 {
    font-size: 36px;
  }

  .info-content h2 {
    font-size: 20px;
  }
}

@media (max-width: 1024px) {
  .auth-info {
    display: none;
  }

  .auth-form-container {
    flex: 1;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 30px; /* 在全屏时减少内边距 */
  }

  .auth-card {
    max-height: 95vh; /* 在平板上调整最大高度 */
  }
}

@media (max-width: 768px) {
  .auth-form-container {
    padding: 20px;
  }

  .auth-card {
    max-height: 100vh; /* 在手机上使用全部高度 */
    border-radius: 0; /* 在小屏幕上移除圆角 */
  }

  .auth-card .el-card__header,
  .auth-card .el-card__body {
    padding: 24px; /* 在小屏幕上减少内边距 */
  }

  .auth-header h3 {
    font-size: 24px;
  }

  .auth-submit-btn {
    height: 50px;
    font-size: 16px;
  }

  .demo-item {
    padding: 14px 16px;
    gap: 14px;
  }

  .demo-item .el-icon {
    font-size: 28px;
  }
}

@media (max-width: 480px) {
  .auth-form-container {
    padding: 12px;
  }

  .auth-card .el-card__header,
  .auth-card .el-card__body {
    padding: 20px;
  }

  .auth-header h3 {
    font-size: 22px;
  }

  .switch-btn {
    height: 44px;
    font-size: 15px;
  }

  .auth-form .el-input__wrapper {
    min-height: 48px;
  }

  .auth-submit-btn {
    height: 48px;
    font-size: 15px;
  }
}
</style>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  ArrowRight,
  ArrowLeft,
  Sort,
  Money,
  Star,
  Trophy,
  Medal,
  Check,
  List,
  DataBoard,
  Close,
  Guide,
  TrendCharts,
  Lightning,
  MagicStick,
  OfficeBuilding,
  Lock,
  Monitor,
  Loading,
  Connection,
} from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

// 接口定义
interface QuestionnaireData {
  // 基础需求
  budget: string
  bodyTypes: string[]
  energyType: string
  passengers: string
  
  // 个性偏好
  brandPreference: string[]
  dailyMileage: string
  driveType: string
}

interface PriorityFactor {
  id: string
  label: string
  description: string
  icon: any
  weight: number
}

interface CarModel {
  id: number
  brand: string
  name: string
  type: string
  engine: string
  transmission: string
  priceRange: string
  image: string
  isHot?: boolean
  highlight?: string
  matchScore?: number
  advantages?: Advantage[]
  reasons?: string[]
  confidence?: number
}

interface Advantage {
  label: string
  description: string
  icon: any
  score: number
}

interface RecommendationResult {
  recommendations: CarModel[]
  primaryRecommendation: CarModel
  alternatives: {
    budget: CarModel[]
    luxury: CarModel[]
    practical: CarModel[]
  }
  matchScore: number
  totalCandidates: number
  analysisTime: number
}

interface PurchaseChannel {
  type: string
  name: string
  advantage: string
  icon: any
  recommended: boolean
}

interface ChecklistItem {
  id: string
  text: string
  checked: boolean
}

// 响应式数据
const loading = ref(false)
const analyzing = ref(false)
const currentStep = ref(1)
const analysisProgress = ref(0)
const currentAnalysisStep = ref('')

// 问卷数据
const questionnaireData = ref<QuestionnaireData>({
  budget: '',
  bodyTypes: [],
  energyType: '',
  passengers: '',
  brandPreference: [],
  dailyMileage: '',
  driveType: ''
})

// 优先级排序
const priorityFactors = ref<PriorityFactor[]>([
  {
    id: 'fuelEconomy',
    label: '油耗/电耗',
    description: '燃油经济性或电能消耗效率',
    icon: MagicStick,
    weight: 30
  },
  {
    id: 'comfort',
    label: '舒适性',
    description: '驾乘舒适度和便利性配置',
    icon: Star,
    weight: 25
  },
  {
    id: 'space',
    label: '空间',
    description: '车内空间大小和储物能力',
    icon: OfficeBuilding,
    weight: 20
  },
  {
    id: 'performance',
    label: '动力性能',
    description: '加速性能和动力响应',
    icon: Lightning,
    weight: 15
  },
  {
    id: 'intelligence',
    label: '智能配置',
    description: '科技配置和智能驾驶功能',
    icon: Monitor,
    weight: 10
  }
])

// 推荐结果
const recommendationResult = ref<RecommendationResult | null>(null)
const showAllAlternatives = ref(false)
const activeAlternativeTab = ref('budget')

// 对比工具
const comparisonList = ref<CarModel[]>([])

// 购买建议
const checkedItems = ref<string[]>([])
const purchaseChecklist = ref<ChecklistItem[]>([
  { id: 'budget', text: '确认预算和贷款方案', checked: false },
  { id: 'insurance', text: '了解保险费用和政策', checked: false },
  { id: 'maintenance', text: '查询维保政策和费用', checked: false },
  { id: 'test_drive', text: '预约试驾体验', checked: false },
  { id: 'negotiate', text: '准备购车谈判要点', checked: false },
  { id: 'documents', text: '准备购车所需证件', checked: false }
])

// 图表相关
const primaryRadarChart = ref<HTMLDivElement>()
let primaryRadarChartInstance: echarts.ECharts | null = null

// 计算属性
const getCandidateCount = computed(() => {
  // 模拟根据筛选条件计算候选车型数量
  let count = 2000
  
  if (questionnaireData.value.budget !== 'unlimited' && questionnaireData.value.budget) {
    count = Math.floor(count * 0.3)
  }
  
  if (questionnaireData.value.bodyTypes.length > 0 && !questionnaireData.value.bodyTypes.includes('unlimited')) {
    count = Math.floor(count * 0.4)
  }
  
  if (questionnaireData.value.energyType !== 'unlimited' && questionnaireData.value.energyType) {
    count = Math.floor(count * 0.6)
  }
  
  return Math.max(count, 50)
})

const getPriceRange = computed(() => {
  const budget = questionnaireData.value.budget
  const ranges = {
    'under10': '5-10万',
    '10-20': '10-20万',
    '20-30': '20-30万',
    '30-50': '30-50万',
    'over50': '50万以上',
    'unlimited': '全价格段'
  }
  return ranges[budget] || '全价格段'
})

const getPopularBrands = computed(() => {
  const selected = questionnaireData.value.brandPreference
  if (selected.includes('none') || selected.length === 0) {
    return '特斯拉、比亚迪、理想'
  }
  return selected.slice(0, 3).map(brand => {
    const brandMap = {
      'tesla': '特斯拉',
      'byd': '比亚迪',
      'nio': '蔚来',
      'bmw': '宝马',
      'mercedes': '奔驰',
      'audi': '奥迪'
    }
    return brandMap[brand] || brand
  }).join('、')
})

// 工具函数
const isStep1Valid = () => {
  return questionnaireData.value.budget && 
         questionnaireData.value.bodyTypes.length > 0 && 
         questionnaireData.value.energyType && 
         questionnaireData.value.passengers
}

const getSmartHints = () => {
  const hints = []
  
  if (questionnaireData.value.budget === 'under10') {
    hints.push('建议关注新能源车型，享受购车补贴优惠')
  }
  
  if (questionnaireData.value.bodyTypes.includes('suv')) {
    hints.push('SUV车型空间更大，适合家庭出行需求')
  }
  
  if (questionnaireData.value.energyType === 'electric') {
    hints.push('纯电动车型使用成本更低，适合城市通勤')
  }
  
  return hints
}

const getScenarioTagType = () => {
  if (questionnaireData.value.passengers === '7+') return 'success'
  if (questionnaireData.value.energyType === 'electric') return 'primary'
  if (questionnaireData.value.budget === 'over50') return 'warning'
  return 'info'
}

const getUserScenarioLabel = () => {
  if (questionnaireData.value.passengers === '7+') return '家庭用户'
  if (questionnaireData.value.energyType === 'electric') return '环保用户'
  if (questionnaireData.value.budget === 'over50') return '豪华用户'
  return '综合用户'
}

const getAnalysisTime = () => {
  return recommendationResult.value?.analysisTime || 2.3
}

const getPurchaseTimingAdvice = () => {
  const month = new Date().getMonth() + 1
  if (month >= 11 || month <= 2) {
    return '年底促销季，优惠力度较大，建议抓住机会'
  } else if (month >= 3 && month <= 5) {
    return '春季新车上市期，可关注新款车型'
  } else {
    return '淡季购车，谈判空间相对较大'
  }
}

const getRecommendedChannels = (): PurchaseChannel[] => {
  return [
    {
      type: 'dealer',
      name: '4S店',
      advantage: '服务保障全面，售后有保证',
      icon: OfficeBuilding,
      recommended: true
    },
    {
      type: 'online',
      name: '官方商城',
      advantage: '价格透明，购车流程便捷',
      icon: Monitor,
      recommended: true
    },
    {
      type: 'supermarket',
      name: '汽车超市',
      advantage: '多品牌对比，价格有优势',
      icon: Star,
      recommended: false
    }
  ]
}

// 事件处理函数
const nextStep = () => {
  if (currentStep.value < 4) {
    currentStep.value++
  }
}

const prevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

const updatePriorityWeights = () => {
  // 根据排序位置计算权重
  const totalFactors = priorityFactors.value.length
  priorityFactors.value.forEach((factor, index) => {
    // 第1位：30%，第2位：25%，第3位：20%，第4位：15%，第5位：10%
    const weights = [30, 25, 20, 15, 10]
    factor.weight = weights[index] || 5
  })
}

const generateRecommendation = async () => {
  analyzing.value = true
  analysisProgress.value = 0
  
  try {
    // 模拟AI分析过程
    const steps = [
      '正在分析您的需求偏好...',
      '正在匹配车型数据库...',
      '正在计算匹配度评分...',
      '正在生成个性化推荐...',
      '正在优化推荐结果...'
    ]
    
    for (let i = 0; i < steps.length; i++) {
      currentAnalysisStep.value = steps[i]
      analysisProgress.value = (i + 1) * 20
      await new Promise(resolve => setTimeout(resolve, 800))
    }
    
    // 调用推荐API
    await fetchRecommendation()
    
    currentStep.value = 4
    await nextTick()
    await initPrimaryRadarChart()
    
  } catch (error) {
    console.error('生成推荐失败:', error)
    ElMessage.error('推荐生成失败，请稍后重试')
  } finally {
    analyzing.value = false
    analysisProgress.value = 100
  }
}

const fetchRecommendation = async () => {
  try {
    const response = await axios.post('/api/recommendation/generate', {
      questionnaire: questionnaireData.value,
      priorities: priorityFactors.value
    })
    
    if (response.data.status === 1) {
      recommendationResult.value = response.data.data
    } else {
      throw new Error('API响应失败')
    }
  } catch (error) {
    console.error('获取推荐失败:', error)
    // 使用模拟数据
    recommendationResult.value = generateMockRecommendation()
  }
}

const generateMockRecommendation = (): RecommendationResult => {
  const mockCars = [
    {
      id: 1,
      brand: 'Tesla',
      name: 'Model Y',
      type: 'SUV',
      engine: '双电机',
      transmission: '单速变速箱',
      priceRange: '26.39-36.39万',
      image: 'https://picsum.photos/400/300?random=1',
      isHot: true,
      confidence: 92,
      matchScore: 94,
      highlight: '续航优秀',
      advantages: [
        {
          label: '续航里程',
          description: 'CLTC续航594km，满足长途需求',
          icon: Lightning,
          score: 95
        },
        {
          label: '科技配置',
          description: '自动驾驶、OTA升级等领先科技',
          icon: Monitor,
          score: 98
        },
        {
          label: '品牌价值',
          description: '全球电动车领导品牌',
          icon: Star,
          score: 96
        }
      ],
      reasons: [
        '与您的预算区间完美匹配',
        '纯电动符合您的环保理念',
        'SUV车型满足空间需求',
        '智能配置符合您的优先级'
      ]
    },
    {
      id: 2,
      brand: '比亚迪',
      name: '宋PLUS DM-i',
      type: 'SUV',
      engine: '1.5L混动',
      transmission: 'E-CVT',
      priceRange: '15.48-21.68万',
      image: 'https://picsum.photos/400/300?random=2',
      matchScore: 89,
      highlight: '性价比高'
    },
    {
      id: 3,
      brand: '理想',
      name: 'L7',
      type: 'SUV',
      engine: '增程式',
      transmission: '单速',
      priceRange: '31.98-37.98万',
      image: 'https://picsum.photos/400/300?random=3',
      matchScore: 87,
      highlight: '空间宽敞'
    }
  ]
  
  return {
    recommendations: mockCars,
    primaryRecommendation: mockCars[0],
    alternatives: {
      budget: [mockCars[1]],
      luxury: [mockCars[2]],
      practical: [mockCars[1]]
    },
    matchScore: 94,
    totalCandidates: getCandidateCount.value,
    analysisTime: 2.3
  }
}

const initPrimaryRadarChart = async () => {
  if (!primaryRadarChart.value || !recommendationResult.value) return
  
  await nextTick()
  
  if (primaryRadarChartInstance) {
    primaryRadarChartInstance.dispose()
  }
  
  primaryRadarChartInstance = echarts.init(primaryRadarChart.value)
  
  const option = {
    radar: {
      indicator: [
        { name: '油耗/电耗', max: 100 },
        { name: '舒适性', max: 100 },
        { name: '空间', max: 100 },
        { name: '动力性能', max: 100 },
        { name: '智能配置', max: 100 }
      ],
      radius: '70%'
    },
    series: [{
      type: 'radar',
      data: [{
        value: [95, 88, 85, 90, 98],
        name: '匹配度',
        areaStyle: {
          color: 'rgba(79, 172, 254, 0.3)'
        },
        lineStyle: {
          color: '#4facfe'
        }
      }]
    }]
  }
  
  primaryRadarChartInstance.setOption(option)
}

// 对比工具相关
const addToComparison = (model: CarModel) => {
  if (comparisonList.value.length >= 3) {
    ElMessage.warning('最多只能对比3款车型')
    return
  }
  
  if (comparisonList.value.find(item => item.id === model.id)) {
    ElMessage.warning('该车型已在对比列表中')
    return
  }
  
  comparisonList.value.push(model)
  ElMessage.success(`${model.brand} ${model.name} 已加入对比`)
}

const removeFromComparison = (modelId: number) => {
  const index = comparisonList.value.findIndex(item => item.id === modelId)
  if (index > -1) {
    const model = comparisonList.value[index]
    comparisonList.value.splice(index, 1)
    ElMessage.success(`${model.brand} ${model.name} 已移出对比`)
  }
}

const clearComparison = () => {
  comparisonList.value = []
  ElMessage.info('对比列表已清空')
}

const startDetailedComparison = () => {
  if (comparisonList.value.length < 2) {
    ElMessage.warning('至少需要2款车型才能开始对比')
    return
  }
  
  const modelIds = comparisonList.value.map(model => model.id).join(',')
  router.push({
    name: 'VehicleModelCompAnalysis',
    query: { models: modelIds }
  })
}

// 页面操作
const resetQuestionnaire = () => {
  ElMessageBox.confirm('确定要重新开始推荐吗？当前进度将会丢失。', '确认操作', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    currentStep.value = 1
    questionnaireData.value = {
      budget: '',
      bodyTypes: [],
      energyType: '',
      passengers: '',
      brandPreference: [],
      dailyMileage: '',
      driveType: ''
    }
    recommendationResult.value = null
    comparisonList.value = []
    ElMessage.success('已重置，请重新填写问卷')
  }).catch(() => {
    // 用户取消
  })
}

const exportRecommendation = () => {
  if (!recommendationResult.value) {
    ElMessage.warning('暂无推荐结果可导出')
    return
  }
  
  const content = [
    '购车推荐报告',
    `生成时间: ${new Date().toLocaleString()}`,
    `用户类型: ${getUserScenarioLabel()}`,
    '',
    '主要推荐:',
    `${recommendationResult.value.primaryRecommendation.brand} ${recommendationResult.value.primaryRecommendation.name}`,
    `匹配度: ${recommendationResult.value.primaryRecommendation.matchScore}%`,
    `价格: ${recommendationResult.value.primaryRecommendation.priceRange}`,
    '',
    '推荐理由:',
    ...recommendationResult.value.primaryRecommendation.reasons || []
  ].join('\n')
  
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `购车推荐报告_${new Date().toISOString().slice(0, 10)}.txt`
  link.click()
  
  ElMessage.success('推荐报告已导出')
}

const saveRecommendation = () => {
  if (!recommendationResult.value) return
  
  const recommendationId = Date.now()
  localStorage.setItem(`recommendation_${recommendationId}`, JSON.stringify({
    questionnaire: questionnaireData.value,
    priorities: priorityFactors.value,
    result: recommendationResult.value,
    timestamp: Date.now()
  }))
  
  ElMessage.success('推荐结果已保存到本地')
}

const shareRecommendation = () => {
  if (!recommendationResult.value) return
  
  const shareText = `我通过AI智能推荐找到了最适合的车型：${recommendationResult.value.primaryRecommendation.brand} ${recommendationResult.value.primaryRecommendation.name}，匹配度${recommendationResult.value.primaryRecommendation.matchScore}%！`
  
  if (navigator.share) {
    navigator.share({
      title: '智能购车推荐',
      text: shareText,
      url: window.location.href
    })
  } else {
    navigator.clipboard.writeText(shareText).then(() => {
      ElMessage.success('推荐内容已复制到剪贴板')
    }).catch(() => {
      ElMessage.error('分享失败')
    })
  }
}

const restartQuestionnaire = () => {
  resetQuestionnaire()
}

const viewModelDetails = (model: CarModel) => {
  ElMessage.info(`查看 ${model.brand} ${model.name} 详情功能开发中...`)
}

const bookTestDrive = (model: CarModel) => {
  ElMessage.info(`预约 ${model.brand} ${model.name} 试驾功能开发中...`)
}

// 窗口大小调整
const handleResize = () => {
  if (primaryRadarChartInstance) {
    primaryRadarChartInstance.resize()
  }
}

// 监听器
watch(priorityFactors, () => {
  updatePriorityWeights()
}, { deep: true })

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用智能购车推荐系统！')
  
  // 检查URL参数
  const step = route.query.step as string
  if (step && parseInt(step) >= 1 && parseInt(step) <= 4) {
    currentStep.value = parseInt(step)
  }
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  
  if (primaryRadarChartInstance) {
    primaryRadarChartInstance.dispose()
  }
})
</script>

<template>
  <div class="recommendation-analysis">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>智能购车推荐</h2>
          <p>基于个性化需求的科学推荐，3分钟找到最适合您的车型</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="resetQuestionnaire" v-if="currentStep > 1">
            重新开始
          </el-button>
          <el-button type="success" :icon="Download" @click="exportRecommendation" :disabled="!recommendationResult">
            导出推荐
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 进度指示器 -->
    <el-card shadow="never" class="progress-card" v-if="currentStep <= 3">
      <div class="progress-content">
        <el-steps :active="currentStep - 1" finish-status="success" align-center>
          <el-step title="基础需求" description="预算·车型·用途"></el-step>
          <el-step title="个性偏好" description="品牌·配置·驱动"></el-step>
          <el-step title="优先级设置" description="重要因素排序"></el-step>
        </el-steps>
        <div class="progress-stats">
          <span class="step-info">第 {{ currentStep }} 步，共 3 步</span>
          <span class="time-estimate">预计还需 {{ (4 - currentStep) * 1 }} 分钟</span>
        </div>
      </div>
    </el-card>

    <!-- 智能问卷区域 -->
    <div class="questionnaire-section" v-if="currentStep <= 3">
      <!-- 第一步：基础需求 -->
      <el-card shadow="never" class="questionnaire-card" v-if="currentStep === 1">
        <template #header>
          <div class="questionnaire-header">
            <el-icon><Money /></el-icon>
            <span>基础购车需求</span>
            <el-tag type="primary">必填信息</el-tag>
          </div>
        </template>

        <div class="questionnaire-content">
          <el-form :model="questionnaireData" label-width="120px" size="large">
            <!-- 预算范围 -->
            <el-form-item label="预算范围" required>
              <el-radio-group v-model="questionnaireData.budget" class="budget-options">
                <el-radio value="unlimited">无限制</el-radio>
                <el-radio value="under10">10万以下</el-radio>
                <el-radio value="10-20">10-20万</el-radio>
                <el-radio value="20-30">20-30万</el-radio>
                <el-radio value="30-50">30-50万</el-radio>
                <el-radio value="over50">50万以上</el-radio>
              </el-radio-group>
              <div class="live-preview" v-if="questionnaireData.budget">
                <span class="preview-label">符合条件车型：</span>
                <span class="preview-count">{{ getCandidateCount() }} 款</span>
              </div>
            </el-form-item>

            <!-- 外观偏好 -->
            <el-form-item label="外观偏好" required>
              <el-checkbox-group v-model="questionnaireData.bodyTypes" class="body-type-options">
                <el-checkbox value="unlimited">无限制</el-checkbox>
                <el-checkbox value="sedan">轿车</el-checkbox>
                <el-checkbox value="suv">SUV</el-checkbox>
                <el-checkbox value="mpv">MPV</el-checkbox>
                <el-checkbox value="pickup">皮卡</el-checkbox>
                <el-checkbox value="sports">跑车</el-checkbox>
                <el-checkbox value="crossover">跨界车</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <!-- 能源偏好 -->
            <el-form-item label="能源偏好" required>
              <el-radio-group v-model="questionnaireData.energyType" class="energy-options">
                <el-radio value="unlimited">无限制</el-radio>
                <el-radio value="gasoline">燃油车</el-radio>
                <el-radio value="electric">纯电动</el-radio>
                <el-radio value="hybrid">混动/插混</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 乘坐人数 -->
            <el-form-item label="乘坐人数" required>
              <el-radio-group v-model="questionnaireData.passengers" class="passenger-options">
                <el-radio value="unlimited">无限制</el-radio>
                <el-radio value="1-2">1-2人</el-radio>
                <el-radio value="3-4">3-4人</el-radio>
                <el-radio value="5">5人</el-radio>
                <el-radio value="7+">7人及以上</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>

          <!-- 智能提示 -->
          <div class="smart-hints" v-if="getSmartHints().length > 0">
            <h4>💡 智能建议</h4>
            <ul class="hints-list">
              <li v-for="hint in getSmartHints()" :key="hint">{{ hint }}</li>
            </ul>
          </div>
        </div>

        <div class="questionnaire-actions">
          <el-button type="primary" size="large" @click="nextStep" :disabled="!isStep1Valid()">
            下一步：个性偏好
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>

      <!-- 第二步：个性偏好 -->
      <el-card shadow="never" class="questionnaire-card" v-if="currentStep === 2">
        <template #header>
          <div class="questionnaire-header">
            <el-icon><Star /></el-icon>
            <span>个性化偏好设置</span>
            <el-tag type="warning">选填信息</el-tag>
          </div>
        </template>

        <div class="questionnaire-content">
          <el-form :model="questionnaireData" label-width="120px" size="large">
            <!-- 品牌倾向 -->
            <el-form-item label="品牌倾向">
              <el-checkbox-group v-model="questionnaireData.brandPreference" class="brand-options">
                <el-checkbox value="none">无偏好</el-checkbox>
                <el-checkbox value="mercedes">奔驰</el-checkbox>
                <el-checkbox value="bmw">宝马</el-checkbox>
                <el-checkbox value="audi">奥迪</el-checkbox>
                <el-checkbox value="tesla">特斯拉</el-checkbox>
                <el-checkbox value="byd">比亚迪</el-checkbox>
                <el-checkbox value="nio">蔚来</el-checkbox>
                <el-checkbox value="xiaopeng">小鹏</el-checkbox>
                <el-checkbox value="lixiang">理想</el-checkbox>
                <el-checkbox value="toyota">丰田</el-checkbox>
                <el-checkbox value="honda">本田</el-checkbox>
                <el-checkbox value="volkswagen">大众</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <!-- 日均行驶里程 -->
            <el-form-item label="日均里程">
              <el-radio-group v-model="questionnaireData.dailyMileage" class="mileage-options">
                <el-radio value="unlimited">无限制</el-radio>
                <el-radio value="under100">100km以内</el-radio>
                <el-radio value="100-200">100-200km</el-radio>
                <el-radio value="200-300">200-300km</el-radio>
                <el-radio value="over300">300km以上</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 驱动形式偏好 -->
            <el-form-item label="驱动形式">
              <el-radio-group v-model="questionnaireData.driveType" class="drive-options">
                <el-radio value="none">无特别要求</el-radio>
                <el-radio value="fwd">前驱</el-radio>
                <el-radio value="rwd">后驱</el-radio>
                <el-radio value="awd">四驱</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>

          <!-- 实时更新的筛选结果 -->
          <div class="filter-preview">
            <h4>🎯 筛选预览</h4>
            <div class="preview-stats">
              <div class="stat-item">
                <span class="stat-label">符合条件</span>
                <span class="stat-value">{{ getCandidateCount() }} 款</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">价格区间</span>
                <span class="stat-value">{{ getPriceRange() }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">热门品牌</span>
                <span class="stat-value">{{ getPopularBrands() }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="questionnaire-actions">
          <el-button size="large" @click="prevStep">
            <el-icon><ArrowLeft /></el-icon>
            上一步
          </el-button>
          <el-button type="primary" size="large" @click="nextStep">
            下一步：优先级设置
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>

      <!-- 第三步：优先级排序 -->
      <el-card shadow="never" class="questionnaire-card" v-if="currentStep === 3">
        <template #header>
          <div class="questionnaire-header">
            <el-icon><Sort /></el-icon>
            <span>购车优先级排序</span>
            <el-tag type="success">关键步骤</el-tag>
          </div>
        </template>

        <div class="questionnaire-content">
          <div class="priority-instruction">
            <h4>📋 请拖拽排序以下因素的重要程度（1最重要，5最不重要）</h4>
            <p>您的排序将直接影响推荐结果的准确性</p>
          </div>

          <div class="priority-sorting">
            <draggable 
              v-model="priorityFactors" 
              item-key="id" 
              class="priority-list"
              @change="updatePriorityWeights"
            >
              <template #item="{ element, index }">
                <div class="priority-item" :class="`priority-${index + 1}`">
                  <div class="priority-rank">{{ index + 1 }}</div>
                  <div class="priority-info">
                    <el-icon>
                      <component :is="element.icon" />
                    </el-icon>
                    <div class="priority-details">
                      <h5>{{ element.label }}</h5>
                      <p>{{ element.description }}</p>
                    </div>
                  </div>
                  <div class="priority-weight">{{ element.weight }}%</div>
                  <div class="drag-handle">
                    <el-icon><Sort /></el-icon>
                  </div>
                </div>
              </template>
            </draggable>
          </div>

          <!-- 权重可视化 -->
          <div class="weight-visualization">
            <h4>📊 权重分布预览</h4>
            <div class="weight-bars">
              <div 
                v-for="factor in priorityFactors" 
                :key="factor.id"
                class="weight-bar"
              >
                <span class="bar-label">{{ factor.label }}</span>
                <div class="bar-container">
                  <div 
                    class="bar-fill" 
                    :style="{ width: factor.weight + '%' }"
                    :class="`bar-${factor.id}`"
                  ></div>
                </div>
                <span class="bar-value">{{ factor.weight }}%</span>
              </div>
            </div>
          </div>
        </div>

        <div class="questionnaire-actions">
          <el-button size="large" @click="prevStep">
            <el-icon><ArrowLeft /></el-icon>
            上一步
          </el-button>
          <el-button 
            type="primary" 
            size="large" 
            @click="generateRecommendation"
            :loading="analyzing"
          >
            <el-icon><Connection/></el-icon>
            生成专属推荐
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 推荐结果展示区 -->
    <div class="recommendation-results" v-if="currentStep === 4 && recommendationResult">
      <!-- 推荐摘要卡片 -->
      <el-card shadow="never" class="summary-card">
        <template #header>
          <div class="summary-header">
            <el-icon><Trophy /></el-icon>
            <span>专属推荐结果</span>
            <el-tag :type="getScenarioTagType()">{{ getUserScenarioLabel() }}</el-tag>
          </div>
        </template>

        <div class="summary-content">
          <div class="summary-stats">
            <div class="stat-card">
              <div class="stat-number">{{ recommendationResult.recommendations.length }}</div>
              <div class="stat-label">推荐车型</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ recommendationResult.matchScore }}%</div>
              <div class="stat-label">匹配度</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ recommendationResult.totalCandidates }}</div>
              <div class="stat-label">候选车型</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ getAnalysisTime() }}s</div>
              <div class="stat-label">分析耗时</div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 主推荐车型 -->
      <el-card shadow="never" class="primary-recommendation-card">
        <template #header>
          <div class="primary-header">
            <el-icon><Medal /></el-icon>
            <span>最佳推荐</span>
            <div class="confidence-badge">
              <span>置信度 {{ recommendationResult.primaryRecommendation.confidence }}%</span>
            </div>
          </div>
        </template>

        <div class="primary-content">
          <div class="primary-model">
            <div class="model-showcase">
              <img 
                :src="recommendationResult.primaryRecommendation.image" 
                :alt="recommendationResult.primaryRecommendation.name"
                class="showcase-image"
              />
              <div class="model-badges">
                <el-tag type="success" size="large">
                  <el-icon><Check /></el-icon>
                  最佳匹配
                </el-tag>
                <el-tag v-if="recommendationResult.primaryRecommendation.isHot" type="danger">
                  HOT
                </el-tag>
              </div>
            </div>
            <div class="model-details">
              <h3>{{ recommendationResult.primaryRecommendation.brand }} {{ recommendationResult.primaryRecommendation.name }}</h3>
              <p class="model-price">{{ recommendationResult.primaryRecommendation.priceRange }}</p>
              <div class="model-specs">
                <el-tag size="small">{{ recommendationResult.primaryRecommendation.type }}</el-tag>
                <el-tag size="small" type="success">{{ recommendationResult.primaryRecommendation.engine }}</el-tag>
                <el-tag size="small" type="warning">{{ recommendationResult.primaryRecommendation.transmission }}</el-tag>
              </div>
              
              <!-- 匹配度雷达图 -->
              <div class="match-radar">
                <h4>匹配度分析</h4>
                <div ref="primaryRadarChart" class="radar-chart"></div>
              </div>
            </div>
          </div>

          <!-- 推荐理由 -->
          <div class="recommendation-reasons">
            <h4>🎯 推荐理由</h4>
            <ul class="reasons-list">
              <li v-for="reason in recommendationResult.primaryRecommendation.reasons" :key="reason">
                <el-icon><Check /></el-icon>
                {{ reason }}
              </li>
            </ul>
          </div>

          <!-- 核心优势 -->
          <div class="core-advantages">
            <h4>⭐ 核心优势</h4>
            <div class="advantages-grid">
              <div 
                v-for="advantage in recommendationResult.primaryRecommendation.advantages" 
                :key="advantage.label"
                class="advantage-item"
              >
                <el-icon>
                  <component :is="advantage.icon" />
                </el-icon>
                <div class="advantage-info">
                  <h5>{{ advantage.label }}</h5>
                  <p>{{ advantage.description }}</p>
                </div>
                <div class="advantage-score">{{ advantage.score }}/100</div>
              </div>
            </div>
          </div>
        </div>

        <div class="primary-actions">
          <el-button type="primary" size="large" @click="viewModelDetails(recommendationResult.primaryRecommendation)">
            查看详情
          </el-button>
          <el-button size="large" @click="addToComparison(recommendationResult.primaryRecommendation)">
            加入对比
          </el-button>
          <el-button size="large" @click="bookTestDrive(recommendationResult.primaryRecommendation)">
            预约试驾
          </el-button>
        </div>
      </el-card>

      <!-- 备选推荐 -->
      <el-card shadow="never" class="alternatives-card">
        <template #header>
          <div class="alternatives-header">
            <el-icon><List /></el-icon>
            <span>备选推荐</span>
            <el-button size="small" type="text" @click="showAllAlternatives = !showAllAlternatives">
              {{ showAllAlternatives ? '收起' : '查看全部' }}
            </el-button>
          </div>
        </template>

        <div class="alternatives-content">
          <div class="alternatives-tabs">
            <el-tabs v-model="activeAlternativeTab" type="card">
              <el-tab-pane label="性价比推荐" name="budget">
                <div class="alternative-group">
                  <div 
                    v-for="model in recommendationResult.alternatives.budget" 
                    :key="model.id"
                    class="alternative-item"
                  >
                    <img :src="model.image" :alt="model.name" class="alternative-image" />
                    <div class="alternative-info">
                      <h4>{{ model.brand }} {{ model.name }}</h4>
                      <p class="alternative-price">{{ model.priceRange }}</p>
                      <div class="alternative-highlight">
                        <el-tag size="small" type="success">{{ model.highlight }}</el-tag>
                      </div>
                      <div class="match-score">
                        <span>匹配度：{{ model.matchScore }}%</span>
                        <el-progress :percentage="model.matchScore" :show-text="false" />
                      </div>
                    </div>
                    <div class="alternative-actions">
                      <el-button size="small" @click="viewModelDetails(model)">详情</el-button>
                      <el-button size="small" type="primary" @click="addToComparison(model)">对比</el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="豪华配置" name="luxury">
                <div class="alternative-group">
                  <div 
                    v-for="model in recommendationResult.alternatives.luxury" 
                    :key="model.id"
                    class="alternative-item"
                  >
                    <img :src="model.image" :alt="model.name" class="alternative-image" />
                    <div class="alternative-info">
                      <h4>{{ model.brand }} {{ model.name }}</h4>
                      <p class="alternative-price">{{ model.priceRange }}</p>
                      <div class="alternative-highlight">
                        <el-tag size="small" type="warning">{{ model.highlight }}</el-tag>
                      </div>
                      <div class="match-score">
                        <span>匹配度：{{ model.matchScore }}%</span>
                        <el-progress :percentage="model.matchScore" :show-text="false" />
                      </div>
                    </div>
                    <div class="alternative-actions">
                      <el-button size="small" @click="viewModelDetails(model)">详情</el-button>
                      <el-button size="small" type="primary" @click="addToComparison(model)">对比</el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="实用首选" name="practical">
                <div class="alternative-group">
                  <div 
                    v-for="model in recommendationResult.alternatives.practical" 
                    :key="model.id"
                    class="alternative-item"
                  >
                    <img :src="model.image" :alt="model.name" class="alternative-image" />
                    <div class="alternative-info">
                      <h4>{{ model.brand }} {{ model.name }}</h4>
                      <p class="alternative-price">{{ model.priceRange }}</p>
                      <div class="alternative-highlight">
                        <el-tag size="small" type="info">{{ model.highlight }}</el-tag>
                      </div>
                      <div class="match-score">
                        <span>匹配度：{{ model.matchScore }}%</span>
                        <el-progress :percentage="model.matchScore" :show-text="false" />
                      </div>
                    </div>
                    <div class="alternative-actions">
                      <el-button size="small" @click="viewModelDetails(model)">详情</el-button>
                      <el-button size="small" type="primary" @click="addToComparison(model)">对比</el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </el-card>

      <!-- 对比分析工具 -->
      <el-card shadow="never" class="comparison-tool-card" v-if="comparisonList.length > 0">
        <template #header>
          <div class="comparison-header">
            <el-icon><DataBoard /></el-icon>
            <span>快速对比 ({{ comparisonList.length }}/3)</span>
            <div class="comparison-actions">
              <el-button size="small" @click="clearComparison">清空</el-button>
              <el-button size="small" type="primary" @click="startDetailedComparison" :disabled="comparisonList.length < 2">
                开始对比
              </el-button>
            </div>
          </div>
        </template>

        <div class="comparison-content">
          <div class="comparison-models">
            <div v-for="model in comparisonList" :key="model.id" class="comparison-model">
              <img :src="model.image" :alt="model.name" class="comparison-image" />
              <div class="comparison-info">
                <h5>{{ model.brand }} {{ model.name }}</h5>
                <p>{{ model.priceRange }}</p>
              </div>
              <el-button size="small" type="danger" :icon="Close" @click="removeFromComparison(model.id)" />
            </div>
          </div>
        </div>
      </el-card>

      <!-- 购买建议 -->
      <el-card shadow="never" class="purchase-advice-card">
        <template #header>
          <div class="advice-header">
            <el-icon><Guide /></el-icon>
            <span>购买建议</span>
          </div>
        </template>

        <div class="advice-content">
          <div class="advice-sections">
            <div class="advice-section">
              <h4>💰 购车时机建议</h4>
              <div class="timing-advice">
                <div class="advice-item">
                  <el-icon><TrendCharts /></el-icon>
                  <div class="advice-text">
                    <h5>市场趋势</h5>
                    <p>{{ getPurchaseTimingAdvice() }}</p>
                  </div>
                </div>
              </div>
            </div>

            <div class="advice-section">
              <h4>🏪 购车渠道建议</h4>
              <div class="channel-advice">
                <div class="channel-options">
                  <div class="channel-item" v-for="channel in getRecommendedChannels()" :key="channel.type">
                    <el-icon>
                      <component :is="channel.icon" />
                    </el-icon>
                    <div class="channel-info">
                      <h5>{{ channel.name }}</h5>
                      <p>{{ channel.advantage }}</p>
                    </div>
                    <el-tag :type="channel.recommended ? 'success' : 'info'">
                      {{ channel.recommended ? '推荐' : '可选' }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>

            <div class="advice-section">
              <h4>📋 购车清单</h4>
              <div class="checklist">
                <el-checkbox-group v-model="checkedItems" class="checklist-items">
                  <el-checkbox v-for="item in purchaseChecklist" :key="item.id" :value="item.id">
                    {{ item.text }}
                  </el-checkbox>
                </el-checkbox-group>
              </div>
            </div>
          </div>
        </div>

        <div class="advice-actions">
          <el-button type="primary" size="large" @click="saveRecommendation">
            保存推荐结果
          </el-button>
          <el-button size="large" @click="shareRecommendation">
            分享给朋友
          </el-button>
          <el-button size="large" @click="restartQuestionnaire">
            重新推荐
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 加载状态 -->
    <div class="loading-overlay" v-if="analyzing">
      <el-card shadow="never" class="loading-card">
        <div class="loading-content">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <h3>AI正在分析您的需求...</h3>
          <p>{{ currentAnalysisStep }}</p>
          <el-progress :percentage="analysisProgress" :show-text="false" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
/* 整体布局 */
.recommendation-analysis {
  padding: 0 16px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 页面头部样式 */
.page-header {
  margin-bottom: 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.header-left h2 {
  margin: 0 0 8px 0;
  color: white;
  font-size: 32px;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.header-actions .el-button {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  backdrop-filter: blur(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 500;
}

.header-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
}

/* 进度指示器 */
.progress-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.progress-content {
  padding: 24px;
}

.progress-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e8eaed;
}

.step-info {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.time-estimate {
  font-size: 12px;
  color: #909399;
}

/* 问卷卡片样式 */
.questionnaire-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.questionnaire-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.questionnaire-content {
  padding: 24px;
}

.questionnaire-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  border-top: 1px solid #e8eaed;
  background: #f8fafb;
}

/* 表单选项样式 */
.budget-options,
.body-type-options,
.energy-options,
.passenger-options,
.brand-options,
.mileage-options,
.drive-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
  margin-top: 8px;
}

.budget-options .el-radio,
.energy-options .el-radio,
.passenger-options .el-radio,
.mileage-options .el-radio,
.drive-options .el-radio {
  margin-right: 0;
  padding: 12px 16px;
  background: #f8fafb;
  border: 2px solid #e8eaed;
  border-radius: 12px;
  transition: all 0.3s ease;
  width: 100%;
  text-align: center;
}

.budget-options .el-radio.is-checked,
.energy-options .el-radio.is-checked,
.passenger-options .el-radio.is-checked,
.mileage-options .el-radio.is-checked,
.drive-options .el-radio.is-checked {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
  border-color: #4facfe;
  box-shadow: 0 4px 15px rgba(79, 172, 254, 0.3);
}

.body-type-options .el-checkbox,
.brand-options .el-checkbox {
  margin-right: 0;
  padding: 10px 14px;
  background: #f8fafb;
  border: 2px solid #e8eaed;
  border-radius: 10px;
  transition: all 0.3s ease;
  width: 100%;
  text-align: center;
}

.body-type-options .el-checkbox.is-checked,
.brand-options .el-checkbox.is-checked {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
  border-color: #4facfe;
  box-shadow: 0 4px 15px rgba(79, 172, 254, 0.3);
}

/* 实时预览 */
.live-preview {
  margin-top: 12px;
  padding: 8px 12px;
  background: #e8f4fd;
  border-radius: 8px;
  font-size: 14px;
  color: #4facfe;
}

.preview-label {
  font-weight: 500;
}

.preview-count {
  font-weight: 700;
  color: #1976d2;
}

/* 筛选预览 */
.filter-preview {
  margin-top: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.filter-preview h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.preview-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #4facfe;
}

/* 智能提示 */
.smart-hints {
  margin-top: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
  border-radius: 12px;
  border-left: 4px solid #ffd700;
}

.smart-hints h4 {
  margin: 0 0 12px 0;
  color: #b8860b;
  font-size: 16px;
  font-weight: 600;
}

.hints-list {
  margin: 0;
  padding-left: 20px;
  list-style: none;
}

.hints-list li {
  margin-bottom: 8px;
  color: #8b6914;
  font-size: 14px;
  position: relative;
}

.hints-list li::before {
  content: '💡';
  margin-right: 8px;
}

/* 优先级排序 */
.priority-instruction {
  margin-bottom: 24px;
  text-align: center;
}

.priority-instruction h4 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 600;
}

.priority-instruction p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.priority-sorting {
  margin-bottom: 32px;
}

.priority-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.priority-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border: 2px solid #e8eaed;
  border-radius: 16px;
  transition: all 0.3s ease;
  cursor: move;
}

.priority-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.priority-item.priority-1 {
  border-color: #ffd700;
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
}

.priority-item.priority-2 {
  border-color: #c0c4cc;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
}

.priority-rank {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  color: white;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  box-shadow: 0 4px 15px rgba(79, 172, 254, 0.3);
}

.priority-item.priority-1 .priority-rank {
  background: linear-gradient(135deg, #ffd700 0%, #ffb300 100%);
  box-shadow: 0 4px 15px rgba(255, 215, 0, 0.3);
}

.priority-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.priority-info .el-icon {
  font-size: 24px;
  color: #4facfe;
}

.priority-details h5 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.priority-details p {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.4;
}

.priority-weight {
  font-size: 18px;
  font-weight: 700;
  color: #4facfe;
  min-width: 60px;
  text-align: center;
}

.drag-handle {
  cursor: move;
  color: #c0c4cc;
  font-size: 20px;
}

.drag-handle:hover {
  color: #4facfe;
}

/* 权重可视化 */
.weight-visualization {
  margin-top: 24px;
  padding: 20px;
  background: #f8fafb;
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.weight-visualization h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.weight-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.weight-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-label {
  min-width: 80px;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.bar-container {
  flex: 1;
  height: 8px;
  background: #e8eaed;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.bar-fuelEconomy {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.bar-comfort {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.bar-space {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.bar-performance {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.bar-intelligence {
  background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%);
}

.bar-value {
  min-width: 40px;
  font-size: 14px;
  font-weight: 600;
  color: #4facfe;
  text-align: right;
}

/* 推荐结果区域 */
.recommendation-results {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 摘要卡片 */
.summary-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 20px;
  padding: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #4facfe;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* 主推荐卡片 */
.primary-recommendation-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 2px solid #ffd700;
  overflow: hidden;
  background: linear-gradient(135deg, #fff9e6 0%, #ffffff 100%);
}

.primary-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.confidence-badge {
  margin-left: auto;
  padding: 6px 12px;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.primary-content {
  padding: 24px;
}

.primary-model {
  display: flex;
  gap: 24px;
  margin-bottom: 32px;
}

.model-showcase {
  position: relative;
  flex-shrink: 0;
}

.showcase-image {
  width: 300px;
  height: 200px;
  object-fit: cover;
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.model-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.model-details {
  flex: 1;
}

.model-details h3 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.model-price {
  margin: 0 0 16px 0;
  font-size: 20px;
  font-weight: 600;
  color: #f56c6c;
}

.model-specs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

/* 匹配度雷达图 */
.match-radar {
  margin-top: 24px;
}

.match-radar h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.radar-chart {
  height: 250px;
  width: 100%;
}

/* 推荐理由 */
.recommendation-reasons {
  margin-bottom: 32px;
}

.recommendation-reasons h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 600;
}

.reasons-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.reasons-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: 10px;
  color: #1976d2;
  font-size: 14px;
  line-height: 1.5;
}

.reasons-list li .el-icon {
  color: #67c23a;
  font-size: 16px;
}

/* 核心优势 */
.core-advantages h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 600;
}

.advantages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.advantage-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.advantage-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.advantage-item .el-icon {
  font-size: 24px;
  color: #4facfe;
}

.advantage-info {
  flex: 1;
}

.advantage-info h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.advantage-info p {
  margin: 0;
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
}

.advantage-score {
  font-size: 16px;
  font-weight: 700;
  color: #67c23a;
}

/* 主推荐操作按钮 */
.primary-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

/* 备选推荐 */
.alternatives-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.alternatives-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.alternatives-content {
  padding: 20px;
}

.alternative-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.alternative-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.alternative-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.alternative-image {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.alternative-info {
  flex: 1;
}

.alternative-info h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.alternative-price {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
}

.alternative-highlight {
  margin-bottom: 12px;
}

.match-score {
  font-size: 12px;
  color: #606266;
}

.match-score .el-progress {
  margin-top: 4px;
}

.alternative-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-self: flex-start;
}

/* 对比工具 */
.comparison-tool-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #4facfe;
  overflow: hidden;
}

.comparison-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #4facfe;
  font-size: 16px;
}

.comparison-actions {
  display: flex;
  gap: 8px;
}

.comparison-content {
  padding: 20px;
}

.comparison-models {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.comparison-model {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: 12px;
  border: 1px solid #4facfe;
}

.comparison-image {
  width: 60px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
}

.comparison-info h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.comparison-info p {
  margin: 0;
  font-size: 12px;
  color: #606266;
}

/* 购买建议 */
.purchase-advice-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.advice-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.advice-content {
  padding: 24px;
}

.advice-sections {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.advice-section h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.timing-advice,
.channel-advice {
  padding: 20px;
  background: #f8fafb;
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.advice-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.advice-item .el-icon {
  font-size: 24px;
  color: #4facfe;
  margin-top: 2px;
}

.advice-text h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.advice-text p {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.channel-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.channel-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.channel-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.channel-item .el-icon {
  font-size: 20px;
  color: #4facfe;
}

.channel-info {
  flex: 1;
}

.channel-info h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.channel-info p {
  margin: 0;
  font-size: 12px;
  color: #606266;
}

.checklist {
  padding: 20px;
  background: #f8fafb;
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.checklist-items {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.checklist-items .el-checkbox {
  padding: 12px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.checklist-items .el-checkbox:hover {
  border-color: #4facfe;
}

.checklist-items .el-checkbox.is-checked {
  background: #f0f9ff;
  border-color: #4facfe;
}

.advice-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  border-top: 1px solid #e8eaed;
  background: #f8fafb;
}

/* 加载状态 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.loading-card {
  width: 400px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.loading-content {
  text-align: center;
  padding: 40px 24px;
}

.loading-icon {
  font-size: 48px;
  color: #4facfe;
  margin-bottom: 16px;
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loading-content h3 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 600;
}

.loading-content p {
  margin: 0 0 20px 0;
  color: #606266;
  font-size: 14px;
}

/* Element Plus 组件样式优化 */
.el-card {
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.el-button {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.el-button:hover {
  transform: translateY(-1px);
}

.el-progress-bar__outer {
  border-radius: 4px;
}

.el-progress-bar__inner {
  border-radius: 4px;
}

.el-tabs__nav-wrap::after {
  display: none;
}

.el-tabs__item {
  border-radius: 8px 8px 0 0;
  font-weight: 500;
}

.el-steps .el-step__line {
  border-radius: 2px;
}

/* 动画效果 */
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.questionnaire-card,
.summary-card,
.primary-recommendation-card,
.alternatives-card {
  animation: slideInUp 0.6s ease-out;
}

.alternative-item {
  animation: slideInUp 0.4s ease-out;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .primary-model {
    flex-direction: column;
    gap: 20px;
  }

  .showcase-image {
    width: 100%;
    max-width: 400px;
    margin: 0 auto;
  }

  .advantages-grid {
    grid-template-columns: 1fr;
  }

  .alternative-group {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .recommendation-analysis {
    padding: 0 8px;
  }

  .header-content {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .header-actions {
    justify-content: center;
  }

  .header-left h2 {
    font-size: 24px;
    text-align: center;
  }

  .header-left p {
    text-align: center;
  }

  .progress-stats {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }

  .budget-options,
  .body-type-options,
  .energy-options,
  .passenger-options,
  .brand-options,
  .mileage-options,
  .drive-options {
    grid-template-columns: 1fr;
  }

  .preview-stats {
    grid-template-columns: 1fr;
  }

  .summary-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .primary-actions {
    flex-direction: column;
  }

  .advice-actions {
    flex-direction: column;
  }

  .comparison-models {
    flex-direction: column;
  }

  .channel-options {
    gap: 12px;
  }

  .checklist-items {
    grid-template-columns: 1fr;
  }

  .loading-card {
    width: 320px;
  }
}

@media (max-width: 480px) {
  .questionnaire-actions {
    flex-direction: column;
  }

  .questionnaire-actions .el-button {
    width: 100%;
  }

  .alternative-item {
    flex-direction: column;
    text-align: center;
  }

  .alternative-actions {
    flex-direction: row;
    justify-content: center;
  }

  .comparison-model {
    justify-content: space-between;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .recommendation-analysis {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .questionnaire-card,
  .summary-card,
  .primary-recommendation-card,
  .alternatives-card,
  .comparison-tool-card,
  .purchase-advice-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .alternative-item,
  .advantage-item,
  .channel-item {
    background: #2d2d2d;
    border-color: #404040;
  }

  .filter-preview,
  .smart-hints,
  .weight-visualization,
  .timing-advice,
  .channel-advice,
  .checklist {
    background: #363636;
    border-color: #505050;
  }
}

/* 无障碍设计 */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* 聚焦样式 */
.el-button:focus-visible,
.el-radio:focus-visible,
.el-checkbox:focus-visible,
.priority-item:focus-visible {
  outline: 2px solid #4facfe;
  outline-offset: 2px;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f0f2f5;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8abb2;
}
</style>
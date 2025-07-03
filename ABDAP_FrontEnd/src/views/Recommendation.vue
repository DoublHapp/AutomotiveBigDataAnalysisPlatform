<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  Money,
  OfficeBuilding,
  Lightning,
  Star,
  Guide,
  TrendCharts,
  Trophy,
  Medal,
  Check,
  List,
  DataBoard,
  Close,
  Loading,
  Monitor,
  MagicStick,
  Lock
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()

// 接口定义
interface QuestionnaireData {
  budget: string
  bodyTypes: string[]
  energyType: string
  passengers: string
  brandPreference: string[]
  primaryUsage: string
  dailyMileage: string
}

interface RecommendationResult {
  recommendations: CarModel[]
  primaryRecommendation: PrimaryRecommendation
  alternatives: {
    budget: CarModel[]
    luxury: CarModel[]
    practical: CarModel[]
  }
  matchScore: number
  totalCandidates: number
  analysisTime: number
}

interface PrimaryRecommendation {
  id: number
  brand: string
  name: string
  priceRange: string
  type: string
  engine: string
  transmission: string
  image: string
  confidence: number
  isHot: boolean
  reasons: string[]
  advantages: Advantage[]
}

interface Advantage {
  label: string
  description: string
  score: number
  icon: string
}

interface CarModel {
  id: number
  brand: string
  name: string
  priceRange: string
  type: string
  engine: string
  transmission: string
  image: string
  highlight: string
  matchScore: number
}

interface FilterOption {
  value: string
  label: string
  desc?: string
  icon?: string
  logo?: string
}

interface ChannelRecommendation {
  type: string
  name: string
  advantage: string
  icon: string
  recommended: boolean
}

interface ChecklistItem {
  id: string
  text: string
}

// 响应式数据
const analyzing = ref(false)
const currentAnalysisStep = ref('')
const analysisProgress = ref(0)

// 问卷数据
const questionnaireData = ref<QuestionnaireData>({
  budget: '',
  bodyTypes: [],
  energyType: '',
  passengers: '',
  brandPreference: [],
  primaryUsage: '',
  dailyMileage: ''
})

// 推荐结果
const recommendationResult = ref<RecommendationResult | null>(null)
const showAllAlternatives = ref(false)
const activeAlternativeTab = ref('budget')

// 对比功能
const comparisonList = ref<CarModel[]>([])

// 购买建议
const checkedItems = ref<string[]>([])

// 图表实例
const primaryRadarChart = ref<HTMLDivElement>()
let primaryRadarChartInstance: echarts.ECharts | null = null

// 筛选选项配置
const budgetOptions = ref<FilterOption[]>([
  { value: 'under10', label: '10万以下', desc: '经济实用' },
  { value: '10-20', label: '10-20万', desc: '主流选择' },
  { value: '20-30', label: '20-30万', desc: '品质升级' },
  { value: '30-50', label: '30-50万', desc: '豪华体验' },
  { value: 'over50', label: '50万以上', desc: '顶级品质' },
  { value: 'unlimited', label: '预算充足', desc: '不限价格' }
])

const bodyTypeOptions = ref<FilterOption[]>([
  { value: 'sedan', label: '轿车', desc: '商务优雅', icon: 'Monitor' },
  { value: 'suv', label: 'SUV', desc: '空间宽敞', icon: 'OfficeBuilding' },
  { value: 'mpv', label: 'MPV', desc: '家庭首选', icon: 'Star' },
  { value: 'coupe', label: '跑车', desc: '运动激情', icon: 'Lightning' },
  { value: 'hatchback', label: '两厢车', desc: '城市便利', icon: 'Monitor' },
  { value: 'pickup', label: '皮卡', desc: '实用多能', icon: 'OfficeBuilding' }
])

const energyTypeOptions = ref<FilterOption[]>([
  { value: 'gasoline', label: '燃油车', desc: '成熟可靠', icon: 'MagicStick' },
  { value: 'electric', label: '纯电动', desc: '环保节能', icon: 'Lightning' },
  { value: 'hybrid', label: '混合动力', desc: '省油环保', icon: 'Monitor' },
  { value: 'phev', label: '插电混动', desc: '兼顾油电', icon: 'Star' },
  { value: 'unlimited', label: '不限类型', desc: '全部考虑', icon: 'Star' }
])

const passengerOptions = ref<FilterOption[]>([
  { value: '1-2', label: '1-2人', desc: '个人通勤' },
  { value: '3-4', label: '3-4人', desc: '小家庭' },
  { value: '5', label: '5人', desc: '标准家庭' },
  { value: '7+', label: '7人+', desc: '大家庭' },
  { value: 'unlimited', label: '不限人数', desc: '灵活需求' }
])

const brandOptions = ref<FilterOption[]>([
  { value: 'tesla', label: '特斯拉', logo: 'https://via.placeholder.com/32x32?text=T' },
  { value: 'byd', label: '比亚迪', logo: 'https://via.placeholder.com/32x32?text=B' },
  { value: 'nio', label: '蔚来', logo: 'https://via.placeholder.com/32x32?text=N' },
  { value: 'bmw', label: '宝马', logo: 'https://via.placeholder.com/32x32?text=BMW' },
  { value: 'audi', label: '奥迪', logo: 'https://via.placeholder.com/32x32?text=A' },
  { value: 'mercedes', label: '奔驰', logo: 'https://via.placeholder.com/32x32?text=M' },
  { value: 'toyota', label: '丰田', logo: 'https://via.placeholder.com/32x32?text=T' },
  { value: 'volkswagen', label: '大众', logo: 'https://via.placeholder.com/32x32?text=V' }
])

const usageOptions = ref<FilterOption[]>([
  { value: 'commute', label: '城市通勤', desc: '日常代步', icon: 'Monitor' },
  { value: 'family', label: '家庭出行', desc: '全家使用', icon: 'Star' },
  { value: 'business', label: '商务接待', desc: '工作需要', icon: 'OfficeBuilding' },
  { value: 'leisure', label: '休闲娱乐', desc: '周末出游', icon: 'TrendCharts' },
  { value: 'mixed', label: '综合使用', desc: '多种场景', icon: 'Guide' }
])

const mileageOptions = ref<FilterOption[]>([
  { value: 'under50', label: '50km以内', desc: '城市代步' },
  { value: '50-100', label: '50-100km', desc: '市内通勤' },
  { value: '100-200', label: '100-200km', desc: '跨区出行' },
  { value: 'over200', label: '200km以上', desc: '长途需求' },
  { value: 'unlimited', label: '不确定', desc: '视情况而定' }
])

// 计算属性
const getCandidateCount = computed(() => {
  if (!isStep1Valid()) return 0

  let count = 50 // 基础车型数量

  // 根据预算调整
  if (questionnaireData.value.budget === 'under10') count = Math.floor(count * 0.3)
  else if (questionnaireData.value.budget === 'over50') count = Math.floor(count * 0.2)

  // 根据车型类别调整
  if (questionnaireData.value.bodyTypes.length === 1) count = Math.floor(count * 0.4)

  // 根据能源类型调整
  if (questionnaireData.value.energyType === 'electric') count = Math.floor(count * 0.3)

  return Math.max(1, count)
})

const getPriceRange = computed(() => {
  const budget = questionnaireData.value.budget
  const ranges = {
    'under10': '10万以下',
    '10-20': '10-20万',
    '20-30': '20-30万',
    '30-50': '30-50万',
    'over50': '50万以上',
    'unlimited': '不限'
  }
  return ranges[budget] || '请选择预算'
})

const getPopularBrands = computed(() => {
  if (questionnaireData.value.budget === 'over50') return '奔驰 宝马 奥迪'
  if (questionnaireData.value.energyType === 'electric') return '特斯拉 蔚来 比亚迪'
  return '丰田 大众 本田'
})

const purchaseChecklist = computed<ChecklistItem[]>(() => [
  { id: 'insurance', text: '了解保险方案和费用' },
  { id: 'financing', text: '确认贷款方案和利率' },
  { id: 'license', text: '准备上牌所需材料' },
  { id: 'maintenance', text: '了解保养政策和费用' },
  { id: 'testdrive', text: '预约试驾体验' },
  { id: 'contract', text: '仔细阅读购车合同' }
])

// 筛选交互函数
const selectBudget = (value: string) => {
  questionnaireData.value.budget = value
  ElMessage.info(`已选择预算: ${budgetOptions.value.find(opt => opt.value === value)?.label}`)
}

const toggleBodyType = (value: string) => {
  const index = questionnaireData.value.bodyTypes.indexOf(value)
  if (index > -1) {
    questionnaireData.value.bodyTypes.splice(index, 1)
  } else {
    if (value === 'unlimited') {
      questionnaireData.value.bodyTypes = ['unlimited']
    } else {
      questionnaireData.value.bodyTypes = questionnaireData.value.bodyTypes.filter(t => t !== 'unlimited')
      questionnaireData.value.bodyTypes.push(value)
    }
  }
}

const selectEnergyType = (value: string) => {
  questionnaireData.value.energyType = value
}

const selectPassengers = (value: string) => {
  questionnaireData.value.passengers = value
}

const toggleBrandPreference = (value: string) => {
  const index = questionnaireData.value.brandPreference.indexOf(value)
  if (index > -1) {
    questionnaireData.value.brandPreference.splice(index, 1)
  } else {
    questionnaireData.value.brandPreference.push(value)
  }
}

const selectPrimaryUsage = (value: string) => {
  questionnaireData.value.primaryUsage = value
}

const selectDailyMileage = (value: string) => {
  questionnaireData.value.dailyMileage = value
}

// 验证函数
const isStep1Valid = () => {
  return questionnaireData.value.budget &&
         questionnaireData.value.bodyTypes.length > 0 &&
         questionnaireData.value.energyType &&
         questionnaireData.value.passengers
}

const getEstimatedTime = () => {
  return isStep1Valid() ? '2-3秒' : '请完善筛选条件'
}

// 智能提示
const getSmartHints = () => {
  const hints: string[] = []

  if (questionnaireData.value.budget === 'under10' && questionnaireData.value.energyType === 'electric') {
    hints.push('💡 10万以下的电动车选择较少，建议考虑混合动力车型')
  }

  if (questionnaireData.value.passengers === '7+' && questionnaireData.value.bodyTypes.includes('sedan')) {
    hints.push('💡 7人以上乘坐建议选择MPV或大型SUV')
  }

  if (questionnaireData.value.dailyMileage === 'over200' && questionnaireData.value.energyType === 'electric') {
    hints.push('💡 长途出行建议选择续航里程较长的车型')
  }

  return hints
}

// 推荐生成
const generateRecommendation = async () => {
  if (!isStep1Valid()) {
    ElMessage.warning('请完善必填的筛选条件')
    return
  }

  analyzing.value = true
  analysisProgress.value = 0

  try {
    // 模拟AI分析过程
    currentAnalysisStep.value = '正在分析您的预算需求...'
    analysisProgress.value = 20
    await new Promise(resolve => setTimeout(resolve, 800))

    currentAnalysisStep.value = '正在匹配车型数据库...'
    analysisProgress.value = 40
    await new Promise(resolve => setTimeout(resolve, 800))

    currentAnalysisStep.value = '正在计算匹配度评分...'
    analysisProgress.value = 60
    await new Promise(resolve => setTimeout(resolve, 800))

    currentAnalysisStep.value = '正在生成个性化推荐...'
    analysisProgress.value = 80
    await new Promise(resolve => setTimeout(resolve, 800))

    currentAnalysisStep.value = '推荐生成完成！'
    analysisProgress.value = 100
    await new Promise(resolve => setTimeout(resolve, 500))

    // 生成模拟推荐结果
    recommendationResult.value = generateMockRecommendationResult()

    ElMessage.success('推荐生成成功！为您找到了最适合的车型')

    // 初始化雷达图
    await nextTick()
    initPrimaryRadarChart()

  } catch (error) {
    ElMessage.error('推荐生成失败，请重试')
  } finally {
    analyzing.value = false
  }
}

// 生成模拟推荐结果
const generateMockRecommendationResult = (): RecommendationResult => {
  const primaryRecommendation: PrimaryRecommendation = {
    id: 1,
    brand: '比亚迪',
    name: 'Han EV',
    priceRange: '22.98-32.98万',
    type: '中大型轿车',
    engine: '纯电动',
    transmission: '电动单速',
    image: 'https://picsum.photos/400/300?random=1',
    confidence: 95,
    isHot: true,
    reasons: [
      '完全符合您的预算要求',
      '新能源政策支持，使用成本低',
      '续航里程605km，满足日常需求',
      '安全配置齐全，获得C-NCAP五星评级',
      '品牌口碑良好，保值率较高'
    ],
    advantages: [
      { label: '续航能力', description: '605km超长续航', score: 92, icon: 'Lightning' },
      { label: '安全性能', description: 'C-NCAP五星安全', score: 95, icon: 'Lock' },
      { label: '科技配置', description: 'DiLink智能网联', score: 88, icon: 'Monitor' },
      { label: '性价比', description: '同级别价格优势', score: 90, icon: 'Money' }
    ]
  }

  const alternatives = {
    budget: [
      {
        id: 2,
        brand: '长安',
        name: 'UNI-V',
        priceRange: '10.89-13.99万',
        type: '紧凑型轿车',
        engine: '1.5T',
        transmission: '7挡双离合',
        image: 'https://picsum.photos/400/300?random=2',
        highlight: '高性价比',
        matchScore: 85
      },
      {
        id: 3,
        brand: '吉利',
        name: '帝豪L',
        priceRange: '8.98-12.98万',
        type: '紧凑型轿车',
        engine: '1.4T',
        transmission: 'CVT',
        image: 'https://picsum.photos/400/300?random=3',
        highlight: '经济实用',
        matchScore: 82
      }
    ],
    luxury: [
      {
        id: 4,
        brand: '奔驰',
        name: 'E级',
        priceRange: '43.99-64.29万',
        type: '中大型轿车',
        engine: '2.0T',
        transmission: '9挡手自一体',
        image: 'https://picsum.photos/400/300?random=4',
        highlight: '豪华配置',
        matchScore: 88
      },
      {
        id: 5,
        brand: '宝马',
        name: '5系',
        priceRange: '42.69-54.69万',
        type: '中大型轿车',
        engine: '2.0T',
        transmission: '8挡手自一体',
        image: 'https://picsum.photos/400/300?random=5',
        highlight: '运动豪华',
        matchScore: 86
      }
    ],
    practical: [
      {
        id: 6,
        brand: '本田',
        name: 'CR-V',
        priceRange: '18.59-26.39万',
        type: '紧凑型SUV',
        engine: '1.5T',
        transmission: 'CVT',
        image: 'https://picsum.photos/400/300?random=6',
        highlight: '空间实用',
        matchScore: 87
      },
      {
        id: 7,
        brand: '丰田',
        name: 'RAV4荣放',
        priceRange: '17.48-25.88万',
        type: '紧凑型SUV',
        engine: '2.0L',
        transmission: 'CVT',
        image: 'https://picsum.photos/400/300?random=7',
        highlight: '可靠耐用',
        matchScore: 84
      }
    ]
  }

  return {
    recommendations: [primaryRecommendation, ...alternatives.budget, ...alternatives.luxury, ...alternatives.practical],
    primaryRecommendation,
    alternatives,
    matchScore: 95,
    totalCandidates: getCandidateCount.value,
    analysisTime: 2.3
  }
}

// 初始化雷达图
const initPrimaryRadarChart = async () => {
  await nextTick()

  if (!primaryRadarChart.value) return

  primaryRadarChartInstance = echarts.init(primaryRadarChart.value)

  const option = {
    title: {
      text: '匹配度分析',
      left: 'center',
      textStyle: { fontSize: 14, color: '#1a1a1a' }
    },
    radar: {
      indicator: [
        { name: '预算匹配', max: 100 },
        { name: '功能需求', max: 100 },
        { name: '品牌偏好', max: 100 },
        { name: '使用场景', max: 100 },
        { name: '性价比', max: 100 },
        { name: '口碑评价', max: 100 }
      ],
      radius: 80,
      startAngle: 90
    },
    series: [{
      name: '匹配度',
      type: 'radar',
      data: [{
        value: [95, 88, 92, 90, 93, 89],
        name: '综合匹配度',
        areaStyle: {
          color: 'rgba(79, 172, 254, 0.3)'
        },
        lineStyle: {
          color: '#4facfe',
          width: 2
        },
        itemStyle: {
          color: '#4facfe'
        }
      }]
    }]
  }

  primaryRadarChartInstance.setOption(option)
}

// 场景标签
const getScenarioTagType = () => {
  const usage = questionnaireData.value.primaryUsage
  const typeMap = {
    'family': 'success',
    'business': 'warning',
    'commute': 'info',
    'leisure': 'primary'
  }
  return typeMap[usage] || 'info'
}

const getUserScenarioLabel = () => {
  const usage = questionnaireData.value.primaryUsage
  const labelMap = {
    'family': '家庭用车',
    'business': '商务用车',
    'commute': '通勤代步',
    'leisure': '休闲娱乐',
    'mixed': '综合使用'
  }
  return labelMap[usage] || '个性化推荐'
}

const getAnalysisTime = () => {
  return recommendationResult.value?.analysisTime.toFixed(1) || '0'
}

// 操作函数
const viewModelDetails = (model: CarModel | PrimaryRecommendation) => {
  ElMessage.info(`查看 ${model.brand} ${model.name} 详细信息`)
  // 跳转到详情页面
  router.push(`/car/${model.id}`)
}

const addToComparison = (model: CarModel | PrimaryRecommendation) => {
  if (comparisonList.value.length >= 3) {
    ElMessage.warning('最多只能对比3款车型')
    return
  }

  if (comparisonList.value.some(car => car.id === model.id)) {
    ElMessage.warning('该车型已在对比列表中')
    return
  }

  const carModel: CarModel = {
    id: model.id,
    brand: model.brand,
    name: model.name,
    priceRange: model.priceRange,
    type: model.type,
    engine: model.engine,
    transmission: model.transmission,
    image: model.image,
    highlight: 'highlight' in model ? model.highlight : '推荐车型',
    matchScore: 'matchScore' in model ? model.matchScore : 95
  }

  comparisonList.value.push(carModel)
  ElMessage.success(`${model.brand} ${model.name} 已加入对比`)
}

const removeFromComparison = (modelId: number) => {
  const index = comparisonList.value.findIndex(car => car.id === modelId)
  if (index > -1) {
    const removed = comparisonList.value.splice(index, 1)[0]
    ElMessage.info(`${removed.brand} ${removed.name} 已移出对比`)
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

  const modelIds = comparisonList.value.map(car => car.id).join(',')
  router.push({
    name: 'VehicleModelCompAnalysis',
    query: { models: modelIds }
  })
}

// 购买建议
const getPurchaseTimingAdvice = () => {
  const month = new Date().getMonth() + 1
  if (month >= 3 && month <= 5) {
    return '春季是购车淡季，优惠力度较大，建议近期购买'
  } else if (month >= 9 && month <= 11) {
    return '金九银十购车旺季，新车上市较多，可选择性强'
  }
  return '当前时期购车政策稳定，建议根据个人需求决定购买时机'
}

const getRecommendedChannels = (): ChannelRecommendation[] => [
  {
    type: 'official',
    name: '官方4S店',
    advantage: '正品保证，售后完善',
    icon: 'OfficeBuilding',
    recommended: true
  },
  {
    type: 'online',
    name: '线上直销',
    advantage: '价格透明，便捷高效',
    icon: 'Monitor',
    recommended: questionnaireData.value.energyType === 'electric'
  },
  {
    type: 'dealer',
    name: '经销商',
    advantage: '价格灵活，库存充足',
    icon: 'Star',
    recommended: false
  }
]

// 页面操作
const resetAllFilters = () => {
  questionnaireData.value = {
    budget: '',
    bodyTypes: [],
    energyType: '',
    passengers: '',
    brandPreference: [],
    primaryUsage: '',
    dailyMileage: ''
  }
  recommendationResult.value = null
  comparisonList.value = []
  ElMessage.info('筛选条件已重置')
}

const exportRecommendation = () => {
  if (!recommendationResult.value) {
    ElMessage.warning('暂无推荐结果可导出')
    return
  }

  const content = [
    '购车推荐报告',
    '=' * 20,
    `生成时间: ${new Date().toLocaleString()}`,
    `使用场景: ${getUserScenarioLabel()}`,
    `预算范围: ${getPriceRange.value}`,
    '',
    '最佳推荐:',
    `${recommendationResult.value.primaryRecommendation.brand} ${recommendationResult.value.primaryRecommendation.name}`,
    `价格: ${recommendationResult.value.primaryRecommendation.priceRange}`,
    `匹配度: ${recommendationResult.value.matchScore}%`,
    '',
    '推荐理由:',
    ...recommendationResult.value.primaryRecommendation.reasons.map(reason => `- ${reason}`),
    '',
    '备选推荐:',
    ...recommendationResult.value.alternatives.budget.map(car => `- ${car.brand} ${car.name} (${car.priceRange})`),
  ].join('\n')

  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = '购车推荐报告.txt'
  link.click()

  ElMessage.success('推荐报告已导出')
}

const saveRecommendation = () => {
  if (!recommendationResult.value) {
    ElMessage.warning('暂无推荐结果可保存')
    return
  }

  const saveData = {
    timestamp: Date.now(),
    questionnaire: questionnaireData.value,
    result: recommendationResult.value
  }

  localStorage.setItem(`recommendation_${Date.now()}`, JSON.stringify(saveData))
  ElMessage.success('推荐结果已保存到本地')
}

const shareRecommendation = () => {
  if (!recommendationResult.value) {
    ElMessage.warning('暂无推荐结果可分享')
    return
  }

  const shareText = `我通过AI智能推荐找到了最适合的车型：${recommendationResult.value.primaryRecommendation.brand} ${recommendationResult.value.primaryRecommendation.name}，匹配度${recommendationResult.value.matchScore}%！`

  if (navigator.share) {
    navigator.share({
      title: '购车推荐结果',
      text: shareText,
      url: window.location.href
    })
  } else {
    navigator.clipboard.writeText(shareText).then(() => {
      ElMessage.success('推荐结果已复制到剪贴板')
    }).catch(() => {
      ElMessage.error('分享失败')
    })
  }
}

const restartQuestionnaire = () => {
  ElMessageBox.confirm('确定要重新开始推荐吗？当前结果将被清除。', '确认重新推荐', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    resetAllFilters()
    ElMessage.success('已重置，请重新填写需求')
  }).catch(() => {
    // 用户取消
  })
}

// 窗口大小调整
const handleResize = () => {
  if (primaryRadarChartInstance) {
    primaryRadarChartInstance.resize()
  }
}

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用智能购车推荐！')

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
          <p>通过筛选条件快速找到最适合您的车型，让购车更简单</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="resetAllFilters">
            重置筛选
          </el-button>
          <el-button type="success" :icon="Download" @click="exportRecommendation" :disabled="!recommendationResult">
            导出推荐
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 筛选条件卡片 -->
    <el-card shadow="never" class="filter-card">
      <template #header>
        <div class="filter-header">
          <span>购车需求筛选</span>
          <div class="filter-stats">
            <el-tag type="primary">符合条件: {{ getCandidateCount }} 款</el-tag>
            <el-button size="small" type="primary" @click="generateRecommendation" :disabled="!isStep1Valid()" :loading="analyzing">
              生成推荐
            </el-button>
          </div>
        </div>
      </template>

      <div class="filter-content">
        <!-- 预算区间 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><Money /></el-icon>
            <span>预算区间</span>
            <span class="required">*</span>
          </div>
          <div class="filter-options budget-options">
            <div
              v-for="option in budgetOptions"
              :key="option.value"
              class="filter-option"
              :class="{ active: questionnaireData.budget === option.value }"
              @click="selectBudget(option.value)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc" v-if="option.desc">{{ option.desc }}</span>
            </div>
          </div>
        </div>

        <!-- 车型类别 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><OfficeBuilding /></el-icon>
            <span>车型类别</span>
            <span class="required">*</span>
          </div>
          <div class="filter-options body-type-options">
            <div
              v-for="option in bodyTypeOptions"
              :key="option.value"
              class="filter-option"
              :class="{ active: questionnaireData.bodyTypes.includes(option.value) }"
              @click="toggleBodyType(option.value)"
            >
              <el-icon><component :is="option.icon" /></el-icon>
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc">{{ option.desc }}</span>
            </div>
          </div>
        </div>

        <!-- 能源类型 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><Lightning /></el-icon>
            <span>能源类型</span>
            <span class="required">*</span>
          </div>
          <div class="filter-options energy-options">
            <div
              v-for="option in energyTypeOptions"
              :key="option.value"
              class="filter-option"
              :class="{ active: questionnaireData.energyType === option.value }"
              @click="selectEnergyType(option.value)"
            >
              <el-icon><component :is="option.icon" /></el-icon>
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc">{{ option.desc }}</span>
            </div>
          </div>
        </div>

        <!-- 乘坐人数 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><Star /></el-icon>
            <span>乘坐人数</span>
            <span class="required">*</span>
          </div>
          <div class="filter-options passenger-options">
            <div
              v-for="option in passengerOptions"
              :key="option.value"
              class="filter-option"
              :class="{ active: questionnaireData.passengers === option.value }"
              @click="selectPassengers(option.value)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc">{{ option.desc }}</span>
            </div>
          </div>
        </div>

        <!-- 品牌偏好 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><Star /></el-icon>
            <span>品牌偏好</span>
            <span class="optional">选填</span>
          </div>
          <div class="filter-options brand-options">
            <div
              v-for="option in brandOptions"
              :key="option.value"
              class="filter-option brand-option"
              :class="{ active: questionnaireData.brandPreference.includes(option.value) }"
              @click="toggleBrandPreference(option.value)"
            >
              <img :src="option.logo" :alt="option.label" class="brand-logo" />
              <span class="option-label">{{ option.label }}</span>
            </div>
          </div>
        </div>

        <!-- 主要用途 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><Guide /></el-icon>
            <span>主要用途</span>
            <span class="optional">选填</span>
          </div>
          <div class="filter-options usage-options">
            <div
              v-for="option in usageOptions"
              :key="option.value"
              class="filter-option"
              :class="{ active: questionnaireData.primaryUsage === option.value }"
              @click="selectPrimaryUsage(option.value)"
            >
              <el-icon><component :is="option.icon" /></el-icon>
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc">{{ option.desc }}</span>
            </div>
          </div>
        </div>

        <!-- 日均里程 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><TrendCharts /></el-icon>
            <span>日均里程</span>
            <span class="optional">选填</span>
          </div>
          <div class="filter-options mileage-options">
            <div
              v-for="option in mileageOptions"
              :key="option.value"
              class="filter-option"
              :class="{ active: questionnaireData.dailyMileage === option.value }"
              @click="selectDailyMileage(option.value)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc">{{ option.desc }}</span>
            </div>
          </div>
        </div>

        <!-- 实时预览统计 -->
        <div class="filter-preview">
          <h4>🎯 筛选预览</h4>
          <div class="preview-stats">
            <div class="stat-item">
              <span class="stat-label">符合条件</span>
              <span class="stat-value">{{ getCandidateCount }} 款</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">价格区间</span>
              <span class="stat-value">{{ getPriceRange }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">推荐品牌</span>
              <span class="stat-value">{{ getPopularBrands }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">预计分析时间</span>
              <span class="stat-value">{{ getEstimatedTime() }}</span>
            </div>
          </div>
        </div>

        <!-- 智能提示 -->
        <div class="smart-hints" v-if="getSmartHints().length > 0">
          <h4>💡 智能提示</h4>
          <ul class="hints-list">
            <li v-for="hint in getSmartHints()" :key="hint">{{ hint }}</li>
          </ul>
        </div>
      </div>
    </el-card>

    <!-- 推荐结果展示区 -->
    <div class="recommendation-results" v-if="recommendationResult">
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
  letter-spacing: -0.5px;
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

/* 筛选卡片样式 */
.filter-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.filter-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}

.filter-content {
  padding: 8px 0;
}

/* 筛选区块样式 */
.filter-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f2f5;
}

.filter-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.section-label .el-icon {
  color: #4facfe;
}

.required {
  color: #f56c6c;
  font-size: 12px;
  margin-left: 4px;
}

.optional {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}

/* 筛选选项样式 */
.filter-options {
  display: grid;
  gap: 12px;
}

.budget-options {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.body-type-options {
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.energy-options {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.passenger-options {
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
}

.brand-options {
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
}

.usage-options {
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.mileage-options {
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
}

.filter-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 12px;
  background: white;
  border: 2px solid #e8eaed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
  min-height: 80px;
}

.filter-option:hover {
  border-color: #4facfe;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(79, 172, 254, 0.2);
}

.filter-option.active {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border-color: #4facfe;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(79, 172, 254, 0.3);
}

.filter-option.active .option-desc {
  color: rgba(255, 255, 255, 0.9);
}

.filter-option .el-icon {
  font-size: 20px;
  margin-bottom: 8px;
  color: #4facfe;
}

.filter-option.active .el-icon {
  color: white;
}

.option-label {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  line-height: 1.2;
}

.option-desc {
  font-size: 11px;
  color: #909399;
  line-height: 1.3;
}

/* 品牌选项特殊样式 */
.brand-option {
  min-height: 90px;
}

.brand-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  margin-bottom: 8px;
  border-radius: 4px;
}

/* 筛选预览样式 */
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

/* 智能提示样式 */
.smart-hints {
  margin-top: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
  border-radius: 12px;
  border: 1px solid #ffd700;
}

.smart-hints h4 {
  margin: 0 0 12px 0;
  color: #e6a23c;
  font-size: 16px;
  font-weight: 600;
}

.hints-list {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.hints-list li {
  margin-bottom: 8px;
  color: #b8860b;
  font-size: 14px;
  line-height: 1.5;
}

/* 推荐结果区域 */
.recommendation-results {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 摘要卡片样式 */
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

.summary-content {
  padding: 8px 0;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #4facfe;
  margin-bottom: 8px;
  display: block;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* 主推荐卡片样式 */
.primary-recommendation-card {
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
  border: 2px solid #4facfe;
  overflow: hidden;
  position: relative;
}

.primary-recommendation-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
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
  padding: 4px 12px;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.primary-content {
  padding: 8px 0;
}

.primary-model {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.model-showcase {
  position: relative;
}

.showcase-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 12px;
}

.model-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.model-details h3 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.model-price {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #4facfe;
}

.model-specs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.match-radar {
  margin-top: 20px;
}

.match-radar h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.radar-chart {
  height: 200px;
  width: 100%;
}

/* 推荐理由样式 */
.recommendation-reasons {
  margin-bottom: 24px;
}

.recommendation-reasons h4 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.reasons-list {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.reasons-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.reasons-list li .el-icon {
  color: #67c23a;
  font-size: 16px;
  margin-top: 2px;
  flex-shrink: 0;
}

/* 核心优势样式 */
.core-advantages h4 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.advantages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.advantage-item {
  display: flex;
  align-items: center;
  gap: 12px;
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
  flex-shrink: 0;
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
  color: #909399;
  line-height: 1.3;
}

.advantage-score {
  font-size: 16px;
  font-weight: 700;
  color: #4facfe;
}

.primary-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  border-top: 1px solid #f0f2f5;
}

/* 备选推荐样式 */
.alternatives-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.alternatives-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.alternatives-content {
  padding: 8px 0;
}

.alternatives-tabs .el-tabs__header {
  margin: 0;
}

.alternative-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
  padding: 16px 0;
}

.alternative-item {
  display: flex;
  gap: 16px;
  padding: 16px;
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
  width: 100px;
  height: 70px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.alternative-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alternative-info h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.alternative-price {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #4facfe;
}

.alternative-highlight {
  margin: 4px 0;
}

.match-score {
  margin-top: auto;
}

.match-score span {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
  display: block;
}

.alternative-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-self: center;
}

/* 对比工具样式 */
.comparison-tool-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #4facfe;
  overflow: hidden;
}

.comparison-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.comparison-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.comparison-content {
  padding: 8px 0;
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
  padding: 12px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  min-width: 200px;
}

.comparison-image {
  width: 60px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
}

.comparison-info {
  flex: 1;
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

/* 购买建议样式 */
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
  padding: 8px 0;
}

.advice-sections {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.advice-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.timing-advice,
.channel-advice {
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
}

.advice-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.advice-item .el-icon {
  font-size: 20px;
  color: #4facfe;
  margin-top: 2px;
}

.advice-text h5 {
  margin: 0 0 4px 0;
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
  gap: 12px;
}

.channel-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 8px;
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
  color: #909399;
}

.checklist {
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
}

.checklist-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.advice-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  border-top: 1px solid #f0f2f5;
}

/* 加载状态样式 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-card {
  min-width: 400px;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}

.loading-content {
  text-align: center;
  padding: 40px 20px;
}

.loading-icon {
  font-size: 48px;
  color: #4facfe;
  margin-bottom: 16px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loading-content h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.loading-content p {
  margin: 0 0 20px 0;
  font-size: 14px;
  color: #606266;
}

/* Element Plus 组件样式优化 */
.el-card {
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.el-button {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.el-button--primary {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.3);
}

.el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(79, 172, 254, 0.4);
}

.el-button--success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.el-button--success:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(103, 194, 58, 0.4);
}

.el-tag {
  border-radius: 6px;
  font-weight: 500;
}

.el-progress__text {
  font-size: 12px !important;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .primary-model {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .advantages-grid {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }

  .alternative-group {
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
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
    flex-wrap: wrap;
  }

  .header-left h2 {
    font-size: 24px;
    text-align: center;
  }

  .header-left p {
    text-align: center;
  }

  .filter-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .budget-options,
  .body-type-options,
  .energy-options,
  .passenger-options,
  .brand-options,
  .usage-options,
  .mileage-options {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-option {
    min-height: 70px;
    padding: 12px 8px;
  }

  .preview-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .summary-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .primary-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .confidence-badge {
    margin-left: 0;
    text-align: center;
  }

  .primary-actions {
    flex-direction: column;
  }

  .alternatives-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .alternative-group {
    grid-template-columns: 1fr;
  }

  .alternative-item {
    flex-direction: column;
    text-align: center;
  }

  .alternative-image {
    width: 100%;
    height: 120px;
  }

  .alternative-actions {
    flex-direction: row;
    align-self: stretch;
  }

  .comparison-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .comparison-models {
    flex-direction: column;
  }

  .comparison-model {
    min-width: auto;
  }

  .advice-actions {
    flex-direction: column;
  }

  .channel-options {
    gap: 8px;
  }

  .channel-item {
    flex-direction: column;
    text-align: center;
  }

  .loading-card {
    min-width: 320px;
    margin: 0 16px;
  }
}

@media (max-width: 480px) {
  .budget-options,
  .body-type-options,
  .energy-options,
  .passenger-options,
  .brand-options,
  .usage-options,
  .mileage-options {
    grid-template-columns: 1fr;
  }

  .preview-stats,
  .summary-stats {
    grid-template-columns: 1fr;
  }

  .advantages-grid {
    grid-template-columns: 1fr;
  }

  .stat-number {
    font-size: 24px;
  }

  .header-left h2 {
    font-size: 20px;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .recommendation-analysis {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .filter-card,
  .summary-card,
  .primary-recommendation-card,
  .alternatives-card,
  .comparison-tool-card,
  .purchase-advice-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .filter-option,
  .stat-card,
  .advantage-item,
  .alternative-item,
  .comparison-model,
  .channel-item {
    background: #2d2d2d;
    border-color: #404040;
  }

  .filter-preview,
  .smart-hints,
  .timing-advice,
  .channel-advice,
  .checklist {
    background: #363636;
  }

  .loading-card {
    background: #2d2d2d;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .filter-option,
  .alternative-item,
  .comparison-model,
  .channel-item {
    border-width: 2px;
    border-color: #000;
  }

  .showcase-image,
  .alternative-image,
  .comparison-image {
    border: 2px solid #000;
  }
}

/* 打印样式 */
@media print {
  .recommendation-analysis {
    background: white !important;
  }

  .header-actions,
  .filter-stats,
  .primary-actions,
  .alternative-actions,
  .comparison-actions,
  .advice-actions {
    display: none !important;
  }

  .filter-card,
  .summary-card,
  .primary-recommendation-card,
  .alternatives-card,
  .purchase-advice-card {
    break-inside: avoid;
    margin-bottom: 12px;
    box-shadow: none !important;
    border: 1px solid #ccc !important;
  }

  .page-header {
    background: white !important;
    color: black !important;
    border: 1px solid #ccc !important;
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
.filter-option:focus-visible,
.alternative-item:focus-visible,
.comparison-model:focus-visible {
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

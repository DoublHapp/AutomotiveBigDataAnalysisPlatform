<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  MagicStick,
  RefreshRight,
  MoreFilled,
  View,
  Star,
  Share,
  Close,
  Money,
  Document,
  ArrowRight
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

// 接口定义
interface FuelConsModel {
  id: number
  brand: string
  name: string
  type: string
  engine: string
  transmission: string
  priceRange: string
  fuelType: string
  fuelConsumption: number
  cityConsumption: number
  highwayConsumption: number
  realWorldConsumption?: number
  electricRange?: number
  powerConsumption?: number
  image: string
  sampleSize?: number
  dataReliability: number
  economyScore: number
  isEcoChampion?: boolean
  isNew?: boolean
}

interface CalculatorForm {
  dailyMileage: number
  fuelPrice: number
  drivingStyle: string
  usageScenario: string
}

interface CostResults {
  dailyCost: string
  monthlyCost: string
  yearlyCost: string
}

interface FuelPrices {
  gasoline92: number
  gasoline95: number
  gasoline98: number
  diesel: number
}

interface EnergyPolicy {
  id: string
  title: string
  description: string
  status: string
  validUntil: string
}

// 响应式数据
const loading = ref(false)
const currentPage = ref(1)

// 筛选条件
const rankingType = ref('overall')
const vehicleType = ref('all')
const priceRange = ref('all')
const fuelType = ref('all')
const displayCount = ref(20)
const showRealWorldData = ref(false)

// 油耗排行数据
const fuelConsRankingList = ref<FuelConsModel[]>([])

// 个性化计算器
const calculatorForm = ref<CalculatorForm>({
  dailyMileage: 50,
  fuelPrice: 7.5,
  drivingStyle: 'normal',
  usageScenario: 'mixed'
})

const costResults = ref<CostResults>({
  dailyCost: '0.00',
  monthlyCost: '0.00',
  yearlyCost: '0.00'
})

const fuelSavingTips = ref<string[]>([])
const currentFuelPrice = ref(7.52)

// 对比工具
const comparisonList = ref<FuelConsModel[]>([])
const showCalculatorModal = ref(false)
const costComparisonChart = ref<HTMLDivElement>()
let costComparisonChartInstance: echarts.ECharts | null = null

// 车型详情
const showDetailDrawer = ref(false)
const selectedCarDetail = ref<FuelConsModel | null>(null)

// 实用工具数据
const fuelPrices = ref<FuelPrices>({
  gasoline92: 7.52,
  gasoline95: 8.03,
  gasoline98: 8.78,
  diesel: 7.15
})

const fuelPriceTrend = ref(0.15)
const fuelPriceUpdateTime = ref(new Date().toLocaleString())

const energyPolicies = ref<EnergyPolicy[]>([
  {
    id: '1',
    title: '新能源汽车购置补贴',
    description: '纯电动乘用车补贴最高1.26万元',
    status: 'active',
    validUntil: '2024.12.31'
  },
  {
    id: '2',
    title: '节能车减免购置税',
    description: '符合条件的节能车型减免车辆购置税',
    status: 'active',
    validUntil: '2025.12.31'
  }
])

// 计算属性
const paginatedRankingList = computed(() => {
  const start = (currentPage.value - 1) * displayCount.value
  const end = start + displayCount.value
  return fuelConsRankingList.value.slice(start, end)
})

// 工具函数
const getRankingTitle = () => {
  const titles = {
    overall: '综合油耗',
    city: '市区油耗',
    highway: '高速油耗',
    realWorld: '实测油耗',
    electric: '电耗'
  }
  return titles[rankingType.value] || '综合油耗'
}

const getFuelTypeColor = (type: string) => {
  const colors = {
    '燃油': 'warning',
    '纯电动': 'success',
    '混合动力': 'primary',
    '插电混动': 'info'
  }
  return colors[type] || 'info'
}

const getFuelLabel = (fuelType: string) => {
  if (fuelType === '纯电动') return '电耗'
  return '油耗'
}

const getFuelUnit = (fuelType: string) => {
  if (fuelType === '纯电动') return 'kWh/100km'
  return 'L/100km'
}

const formatFuelConsumption = (consumption: number, fuelType: string) => {
  if (!consumption) return '--'
  return consumption.toFixed(1)
}

const getFuelEfficiencyClass = (consumption: number, fuelType: string) => {
  if (fuelType === '纯电动') {
    if (consumption <= 15) return 'excellent'
    if (consumption <= 20) return 'good'
    return 'normal'
  } else {
    if (consumption <= 5) return 'excellent'
    if (consumption <= 7) return 'good'
    return 'normal'
  }
}

const getScoreClass = (score: number) => {
  if (score >= 90) return 'excellent'
  if (score >= 75) return 'good'
  if (score >= 60) return 'fair'
  return 'poor'
}

const isSelected = (modelId: number) => {
  return comparisonList.value.some(model => model.id === modelId)
}

// 成本计算相关
const calculateCost = () => {
  const { dailyMileage, fuelPrice, drivingStyle, usageScenario } = calculatorForm.value
  
  // 基础油耗 (L/100km)
  let baseFuelConsumption = 7.0
  
  // 驾驶风格调整
  const styleMultiplier = {
    eco: 0.85,
    normal: 1.0,
    aggressive: 1.2
  }
  
  // 使用场景调整
  const scenarioMultiplier = {
    city: 1.15,
    highway: 0.9,
    mixed: 1.0
  }
  
  const adjustedConsumption = baseFuelConsumption * 
    styleMultiplier[drivingStyle] * 
    scenarioMultiplier[usageScenario]
  
  const dailyCost = (dailyMileage / 100) * adjustedConsumption * fuelPrice
  const monthlyCost = dailyCost * 30
  const yearlyCost = dailyCost * 365
  
  costResults.value = {
    dailyCost: dailyCost.toFixed(2),
    monthlyCost: monthlyCost.toFixed(0),
    yearlyCost: yearlyCost.toFixed(0)
  }
  
  // 更新节油建议
  updateFuelSavingTips()
}

const updateFuelSavingTips = () => {
  const tips = []
  
  if (calculatorForm.value.drivingStyle === 'aggressive') {
    tips.push('建议采用温和驾驶方式，可节省15-20%燃油')
  }
  
  if (calculatorForm.value.usageScenario === 'city') {
    tips.push('城市驾驶建议使用ECO模式，减少急加速急刹车')
  }
  
  if (calculatorForm.value.dailyMileage > 100) {
    tips.push('长距离驾驶建议选择混动或纯电动车型')
  }
  
  tips.push('定期保养车辆，保持最佳燃油经济性')
  
  fuelSavingTips.value = tips
}

const useLivePrice = () => {
  calculatorForm.value.fuelPrice = currentFuelPrice.value
  calculateCost()
  ElMessage.success('已使用当前油价')
}

const calculateYearlyCost = (model: FuelConsModel) => {
  const yearlyMileage = 15000 // 年均1.5万公里
  const fuelPrice = fuelPrices.value.gasoline92
  
  if (model.fuelType === '纯电动') {
    const electricPrice = 0.6 // 电价 元/kWh
    const consumption = model.powerConsumption || model.fuelConsumption
    return Math.floor((yearlyMileage / 100) * consumption * electricPrice)
  } else {
    const consumption = showRealWorldData.value ? 
      (model.realWorldConsumption || model.fuelConsumption) : 
      model.fuelConsumption
    return Math.floor((yearlyMileage / 100) * consumption * fuelPrice)
  }
}

const calculatePerKmCost = (model: FuelConsModel) => {
  const yearlyCost = calculateYearlyCost(model)
  return (yearlyCost / 15000).toFixed(3)
}

const getCostComparison = (model: FuelConsModel, index: number) => {
  if (index === 0) return ''
  
  const firstModelCost = calculateYearlyCost(fuelConsRankingList.value[0])
  const currentModelCost = calculateYearlyCost(model)
  const difference = currentModelCost - firstModelCost
  
  return difference > 0 ? `+¥${difference}` : `¥${Math.abs(difference)}`
}

const getCostComparisonClass = (model: FuelConsModel, index: number) => {
  if (index === 0) return ''
  
  const firstModelCost = calculateYearlyCost(fuelConsRankingList.value[0])
  const currentModelCost = calculateYearlyCost(model)
  
  return currentModelCost > firstModelCost ? 'higher' : 'lower'
}

// 事件处理函数
const handleRankingChange = async () => {
  loading.value = true
  currentPage.value = 1
  try {
    await fetchFuelConsRankingData()
    ElMessage.success(`已切换到${getRankingTitle()}排行`)
  } catch (error) {
    ElMessage.error('排行榜切换失败')
  } finally {
    loading.value = false
  }
}

const handleFilterChange = async () => {
  loading.value = true
  currentPage.value = 1
  try {
    await fetchFuelConsRankingData()
    ElMessage.success('筛选条件已更新')
  } catch (error) {
    ElMessage.error('筛选更新失败')
  } finally {
    loading.value = false
  }
}

const handleDisplayCountChange = () => {
  currentPage.value = 1
  ElMessage.info(`显示数量已调整为TOP ${displayCount.value}`)
}

const handleDataSourceChange = () => {
  ElMessage.info(showRealWorldData.value ? '已切换到实测数据' : '已切换到官方数据')
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  document.querySelector('.ranking-card')?.scrollIntoView({ behavior: 'smooth' })
}

const resetFilters = async () => {
  rankingType.value = 'overall'
  vehicleType.value = 'all'
  priceRange.value = 'all'
  fuelType.value = 'all'
  displayCount.value = 20
  currentPage.value = 1
  
  await handleFilterChange()
  ElMessage.success('筛选条件已重置')
}

// 对比功能
const toggleComparison = (model: FuelConsModel) => {
  const index = comparisonList.value.findIndex(item => item.id === model.id)
  
  if (index > -1) {
    comparisonList.value.splice(index, 1)
    ElMessage.success(`${model.brand} ${model.name} 已移出对比`)
  } else {
    if (comparisonList.value.length >= 3) {
      ElMessage.warning('最多只能对比3款车型')
      return
    }
    comparisonList.value.push(model)
    ElMessage.success(`${model.brand} ${model.name} 已加入对比`)
  }
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

const startComparison = () => {
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

const getMostEfficientModel = () => {
  if (comparisonList.value.length === 0) {
    return { brand: '', name: '' }
  }
  
  return comparisonList.value.reduce((min, current) => {
    const minConsumption = showRealWorldData.value ? 
      (min.realWorldConsumption || min.fuelConsumption) : 
      min.fuelConsumption
    const currentConsumption = showRealWorldData.value ? 
      (current.realWorldConsumption || current.fuelConsumption) : 
      current.fuelConsumption
    
    return currentConsumption < minConsumption ? current : min
  })
}

const calculateMaxSavings = () => {
  if (comparisonList.value.length < 2) return 0
  
  const costs = comparisonList.value.map(model => calculateYearlyCost(model))
  const minCost = Math.min(...costs)
  const maxCost = Math.max(...costs)
  
  return maxCost - minCost
}

// 车型详情
const viewDetails = (model: FuelConsModel) => {
  selectedCarDetail.value = model
  showDetailDrawer.value = true
}

const addToComparison = (model: FuelConsModel) => {
  toggleComparison(model)
  showDetailDrawer.value = false
}

const addToWishlist = (model: FuelConsModel) => {
  ElMessage.success(`${model.brand} ${model.name} 已加入心愿单`)
}

const shareModel = (model: FuelConsModel) => {
  const shareUrl = `${window.location.origin}/fuel-cons/${model.id}`
  
  if (navigator.share) {
    navigator.share({
      title: `${model.brand} ${model.name} 油耗信息`,
      text: `查看这款车的详细油耗数据和经济性分析`,
      url: shareUrl
    })
  } else {
    navigator.clipboard.writeText(shareUrl).then(() => {
      ElMessage.success('分享链接已复制到剪贴板')
    }).catch(() => {
      ElMessage.error('分享失败')
    })
  }
}

// 对比计算器弹窗
const handleCalculatorClose = () => {
  showCalculatorModal.value = false
}

const exportComparisonReport = () => {
  if (comparisonList.value.length < 2) {
    ElMessage.warning('至少需要2款车型才能导出报告')
    return
  }
  
  const csvContent = [
    ['车型对比报告 - 油耗经济性分析'],
    ['生成时间', new Date().toLocaleString()],
    [''],
    ['车型', '品牌', '油耗(L/100km)', '年度成本(元)', '经济性评分'],
    ...comparisonList.value.map(model => [
      model.name,
      model.brand,
      formatFuelConsumption(model.fuelConsumption, model.fuelType),
      calculateYearlyCost(model),
      model.economyScore
    ])
  ].map(row => row.join(',')).join('\n')
  
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `油耗对比报告_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  
  ElMessage.success('对比报告已导出')
}

// 实用工具
const viewMorePolicies = () => {
  ElMessage.info('跳转到政策详情页面...')
}

// 页面操作
const refreshData = async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchFuelConsRankingData(),
      fetchFuelPrices(),
      fetchEnergyPolicies()
    ])
    ElMessage.success('数据已刷新')
  } catch (error) {
    ElMessage.error('数据刷新失败')
  } finally {
    loading.value = false
  }
}

const exportRanking = () => {
  if (fuelConsRankingList.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  
  const csvContent = [
    ['油耗排行榜'],
    ['排行类型', getRankingTitle()],
    ['生成时间', new Date().toLocaleString()],
    [''],
    ['排名', '车型', '品牌', '油耗', '价格区间', '经济性评分'],
    ...fuelConsRankingList.value.slice(0, displayCount.value).map((model, index) => [
      index + 1,
      model.name,
      model.brand,
      `${formatFuelConsumption(model.fuelConsumption, model.fuelType)} ${getFuelUnit(model.fuelType)}`,
      model.priceRange,
      model.economyScore
    ])
  ].map(row => row.join(',')).join('\n')
  
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${getRankingTitle()}排行榜_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  
  ElMessage.success('排行榜数据已导出')
}

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'https://via.placeholder.com/300x200?text=Car+Image'
}

// API调用函数
const fetchFuelConsRankingData = async () => {
  try {
    const params = {
      rankingType: rankingType.value,
      vehicleType: vehicleType.value,
      priceRange: priceRange.value,
      fuelType: fuelType.value,
      limit: 100
    }
    
    const response = await axios.get('/api/fuel-consumption/ranking', { params })
    if (response.data.status === 1) {
      fuelConsRankingList.value = response.data.data
    } else {
      throw new Error('API响应失败')
    }
  } catch (error) {
    console.error('获取油耗排行数据失败:', error)
    // 使用模拟数据
    fuelConsRankingList.value = generateMockFuelConsData()
  }
}

const fetchFuelPrices = async () => {
  try {
    const response = await axios.get('/api/fuel-prices/current')
    if (response.data.status === 1) {
      fuelPrices.value = response.data.data
      currentFuelPrice.value = response.data.data.gasoline92
    }
  } catch (error) {
    console.error('获取油价失败:', error)
  }
}

const fetchEnergyPolicies = async () => {
  try {
    const response = await axios.get('/api/energy-policies/current')
    if (response.data.status === 1) {
      energyPolicies.value = response.data.data
    }
  } catch (error) {
    console.error('获取政策信息失败:', error)
  }
}

// 模拟数据生成
const generateMockFuelConsData = (): FuelConsModel[] => {
  const brands = ['Tesla', '比亚迪', '理想', '小鹏', '蔚来', '奔驰', '宝马', '奥迪', '丰田', '本田']
  const types = ['轿车', 'SUV', 'MPV']
  const fuelTypes = ['燃油', '纯电动', '混合动力', '插电混动']
  
  return Array.from({ length: 50 }, (_, index) => {
    const brand = brands[index % brands.length]
    const fuelTypeItem = fuelTypes[index % fuelTypes.length]
    const isElectric = fuelTypeItem === '纯电动'
    
    return {
      id: index + 1,
      brand,
      name: `${brand}车型${index + 1}`,
      type: types[index % types.length],
      engine: isElectric ? '电动机' : `${(Math.random() * 2 + 1).toFixed(1)}T`,
      transmission: isElectric ? '单速变速箱' : 'CVT',
      priceRange: `${(Math.random() * 30 + 10).toFixed(0)}-${(Math.random() * 20 + 30).toFixed(0)}万`,
      fuelType: fuelTypeItem,
      fuelConsumption: isElectric ? 
        Math.random() * 10 + 12 : // 电耗 12-22 kWh/100km
        Math.random() * 5 + 4,   // 油耗 4-9 L/100km
      cityConsumption: isElectric ? 
        Math.random() * 8 + 15 : 
        Math.random() * 3 + 6,
      highwayConsumption: isElectric ? 
        Math.random() * 6 + 10 : 
        Math.random() * 2 + 4,
      realWorldConsumption: isElectric ? 
        Math.random() * 12 + 16 : 
        Math.random() * 4 + 5,
      powerConsumption: isElectric ? Math.random() * 10 + 12 : undefined,
      image: `https://picsum.photos/300/200?random=${index + 100}`,
      sampleSize: Math.floor(Math.random() * 500) + 100,
      dataReliability: Math.floor(Math.random() * 2) + 4, // 4-5星
      economyScore: Math.floor(Math.random() * 30) + 70, // 70-100分
      isEcoChampion: index < 5,
      isNew: Math.random() > 0.8
    }
  }).sort((a, b) => a.fuelConsumption - b.fuelConsumption)
}

// 图表初始化
const initCostComparisonChart = async () => {
  if (!costComparisonChart.value || comparisonList.value.length < 2) return
  
  await nextTick()
  
  if (costComparisonChartInstance) {
    costComparisonChartInstance.dispose()
  }
  
  costComparisonChartInstance = echarts.init(costComparisonChart.value)
  
  const data = comparisonList.value.map(model => ({
    name: `${model.brand} ${model.name}`,
    value: calculateYearlyCost(model)
  }))
  
  const option = {
    title: {
      text: '年度使用成本对比',
      left: 'center',
      textStyle: { fontSize: 14 }
    },
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>年度成本: ¥{c}'
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.name),
      axisLabel: { interval: 0, rotate: 30 }
    },
    yAxis: {
      type: 'value',
      name: '成本(元)',
      axisLabel: { formatter: '¥{value}' }
    },
    series: [{
      type: 'bar',
      data: data.map(item => item.value),
      itemStyle: {
        color: (params) => {
          const colors = ['#4facfe', '#00f2fe', '#43e97b']
          return colors[params.dataIndex % colors.length]
        }
      }
    }]
  }
  
  costComparisonChartInstance.setOption(option)
}

// 窗口大小调整
const handleResize = () => {
  if (costComparisonChartInstance) {
    costComparisonChartInstance.resize()
  }
}

// 监听器
watch([comparisonList], () => {
  if (showCalculatorModal.value && comparisonList.value.length >= 2) {
    nextTick(() => {
      initCostComparisonChart()
    })
  }
}, { deep: true })

watch([calculatorForm], () => {
  calculateCost()
}, { deep: true })

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用油耗榜单！')
  
  try {
    // 初始化计算器
    calculateCost()
    
    // 加载数据
    await Promise.all([
      fetchFuelConsRankingData(),
      fetchFuelPrices(),
      fetchEnergyPolicies()
    ])
    
    window.addEventListener('resize', handleResize)
  } catch (error) {
    console.error('页面初始化失败:', error)
    ElMessage.error('初始化失败，部分功能可能不可用')
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  
  if (costComparisonChartInstance) {
    costComparisonChartInstance.dispose()
  }
})
</script>

<template>
  <div class="fuel-cons-list">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>油耗/电耗榜单</h2>
          <p>权威燃油经济性数据，助您做出最经济的购车选择</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
          <el-button type="success" :icon="Download" @click="exportRanking" :disabled="!fuelConsRankingList.length">
            导出榜单
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 个性化计算器卡片 -->
    <el-card shadow="never" class="calculator-card">
      <template #header>
        <div class="calculator-header">
          <el-icon><MagicStick /></el-icon>
          <span>个性化用车成本计算器</span>
          <el-tag type="primary">智能计算</el-tag>
        </div>
      </template>

      <div class="calculator-content">
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <div class="calculator-inputs">
              <el-form :model="calculatorForm" label-width="120px" size="large">
                <el-form-item label="日均里程">
                  <el-input-number
                    v-model="calculatorForm.dailyMileage"
                    :min="1"
                    :max="1000"
                    placeholder="公里"
                    style="width: 100%"
                    @change="calculateCost"
                  />
                </el-form-item>

                <el-form-item label="当前油价">
                  <el-input-number
                    v-model="calculatorForm.fuelPrice"
                    :min="1"
                    :max="50"
                    :precision="2"
                    placeholder="元/升"
                    style="width: 100%"
                    @change="calculateCost"
                  />
                  <div class="live-price-tip">
                    <span class="tip-text">今日油价：¥{{ currentFuelPrice }}/升</span>
                    <el-button size="small" type="text" @click="useLivePrice">使用</el-button>
                  </div>
                </el-form-item>

                <el-form-item label="驾驶风格">
                  <el-radio-group v-model="calculatorForm.drivingStyle" @change="calculateCost">
                    <el-radio value="eco">节能</el-radio>
                    <el-radio value="normal">温和</el-radio>
                    <el-radio value="aggressive">激进</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="使用场景">
                  <el-radio-group v-model="calculatorForm.usageScenario" @change="calculateCost">
                    <el-radio value="city">城市通勤</el-radio>
                    <el-radio value="highway">高速长途</el-radio>
                    <el-radio value="mixed">混合路况</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-form>
            </div>
          </el-col>

          <el-col :xs="24" :md="12">
            <div class="calculator-results">
              <h4>🎯 个性化成本预估</h4>
              <div class="cost-cards">
                <div class="cost-card daily">
                  <div class="cost-label">日均成本</div>
                  <div class="cost-value">¥{{ costResults.dailyCost }}</div>
                </div>
                <div class="cost-card monthly">
                  <div class="cost-label">月度成本</div>
                  <div class="cost-value">¥{{ costResults.monthlyCost }}</div>
                </div>
                <div class="cost-card yearly">
                  <div class="cost-label">年度成本</div>
                  <div class="cost-value">¥{{ costResults.yearlyCost }}</div>
                </div>
              </div>

              <!-- 节油建议 -->
              <div class="fuel-saving-tips" v-if="fuelSavingTips.length > 0">
                <h5>💡 个性化节油建议</h5>
                <ul class="tips-list">
                  <li v-for="tip in fuelSavingTips" :key="tip">{{ tip }}</li>
                </ul>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 筛选和排行配置区 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-content">
        <el-row :gutter="16">
          <el-col :span="4">
            <el-form-item label="排行类型:">
              <el-select v-model="rankingType" @change="handleRankingChange">
                <el-option label="综合油耗" value="overall" />
                <el-option label="市区油耗" value="city" />
                <el-option label="高速油耗" value="highway" />
                <el-option label="实测油耗" value="realWorld" />
                <el-option label="电耗排行" value="electric" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="车型类别:">
              <el-select v-model="vehicleType" @change="handleFilterChange">
                <el-option label="全部车型" value="all" />
                <el-option label="轿车" value="sedan" />
                <el-option label="SUV" value="suv" />
                <el-option label="MPV" value="mpv" />
                <el-option label="跑车" value="sports" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="价格区间:">
              <el-select v-model="priceRange" @change="handleFilterChange">
                <el-option label="全部价格" value="all" />
                <el-option label="10万以下" value="under10" />
                <el-option label="10-20万" value="10-20" />
                <el-option label="20-30万" value="20-30" />
                <el-option label="30万以上" value="over30" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="燃料类型:">
              <el-select v-model="fuelType" @change="handleFilterChange">
                <el-option label="全部类型" value="all" />
                <el-option label="燃油车" value="gasoline" />
                <el-option label="纯电动" value="electric" />
                <el-option label="混合动力" value="hybrid" />
                <el-option label="插电混动" value="phev" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="显示数量:">
              <el-select v-model="displayCount" @change="handleDisplayCountChange">
                <el-option label="TOP 20" :value="20" />
                <el-option label="TOP 50" :value="50" />
                <el-option label="TOP 100" :value="100" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-button type="primary" @click="resetFilters" :icon="RefreshRight">
              重置筛选
            </el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 油耗排行榜主体 -->
    <el-card shadow="never" class="ranking-card">
      <template #header>
        <div class="ranking-header">
          <span>{{ getRankingTitle() }} TOP {{ displayCount }}</span>
          <div class="ranking-controls">
            <el-switch
              v-model="showRealWorldData"
              active-text="显示实测数据"
              inactive-text="显示官方数据"
              @change="handleDataSourceChange"
            />
            <el-button size="small" @click="showCalculatorModal = true" type="primary">
              对比计算
            </el-button>
          </div>
        </div>
      </template>

      <div class="ranking-list" v-loading="loading">
        <!-- 油耗排行项 -->
        <div
          v-for="(item, index) in paginatedRankingList"
          :key="item.id"
          class="ranking-item"
          :class="{
            'top-three': index < 3,
            'selected': isSelected(item.id),
            'eco-champion': item.fuelConsumption <= 5.0
          }"
          @click="viewDetails(item)"
        >
          <!-- 排名标识 -->
          <div class="rank-number" :class="`rank-${index + 1}`">
            {{ index + 1 }}
          </div>

          <!-- 车型图片 -->
          <div class="vehicle-image">
            <img :src="item.image" :alt="item.name" @error="handleImageError" />
            <div class="image-overlay" v-if="item.isEcoChampion || item.isNew">
              <el-tag v-if="item.isEcoChampion" type="success" size="small">ECO</el-tag>
              <el-tag v-if="item.isNew" type="primary" size="small">新款</el-tag>
            </div>
          </div>

          <!-- 车型信息 -->
          <div class="vehicle-info">
            <h3>{{ item.brand }} {{ item.name }}</h3>
            <p class="vehicle-specs">{{ item.type }} · {{ item.engine }} · {{ item.transmission }}</p>
            <div class="price-range">{{ item.priceRange }}</div>
            <div class="fuel-type-badge">
              <el-tag :type="getFuelTypeColor(item.fuelType)" size="small">
                {{ item.fuelType }}
              </el-tag>
            </div>
          </div>

          <!-- 油耗核心数据 -->
          <div class="fuel-data">
            <div class="main-fuel-consumption">
              <span class="fuel-label">{{ getFuelLabel(item.fuelType) }}</span>
              <span class="fuel-value" :class="getFuelEfficiencyClass(item.fuelConsumption, item.fuelType)">
                {{ formatFuelConsumption(item.fuelConsumption, item.fuelType) }}
              </span>
              <span class="fuel-unit">{{ getFuelUnit(item.fuelType) }}</span>
            </div>

            <!-- 详细油耗数据 -->
            <div class="detailed-fuel-data">
              <div class="fuel-scenario" v-if="item.cityConsumption">
                <span class="scenario-label">市区</span>
                <span class="scenario-value">{{ formatFuelConsumption(item.cityConsumption, item.fuelType) }}</span>
              </div>
              <div class="fuel-scenario" v-if="item.highwayConsumption">
                <span class="scenario-label">高速</span>
                <span class="scenario-value">{{ formatFuelConsumption(item.highwayConsumption, item.fuelType) }}</span>
              </div>
              <div class="fuel-scenario" v-if="item.realWorldConsumption">
                <span class="scenario-label">实测</span>
                <span class="scenario-value real-world">{{ formatFuelConsumption(item.realWorldConsumption, item.fuelType) }}</span>
              </div>
            </div>

            <!-- 数据可靠性 -->
            <div class="data-reliability" v-if="item.sampleSize">
              <el-tooltip :content="`基于 ${item.sampleSize} 个样本数据`" placement="top">
                <el-rate
                  v-model="item.dataReliability"
                  disabled
                  show-score
                  text-color="#ff9900"
                  score-template="{value}"
                  max="5"
                  size="small"
                />
              </el-tooltip>
            </div>
          </div>

          <!-- 经济性评估 -->
          <div class="economy-assessment">
            <div class="cost-preview">
              <div class="cost-item">
                <span class="cost-label">年油费预估</span>
                <span class="cost-value">¥{{ calculateYearlyCost(item) }}</span>
              </div>
              <div class="cost-comparison" v-if="index > 0">
                <span class="comparison-label">vs 第1名</span>
                <span class="comparison-value" :class="getCostComparisonClass(item, index)">
                  {{ getCostComparison(item, index) }}
                </span>
              </div>
            </div>

            <div class="economy-score">
              <span class="score-label">经济性评分</span>
              <div class="score-bar">
                <div 
                  class="score-fill" 
                  :style="{ width: item.economyScore + '%' }"
                  :class="getScoreClass(item.economyScore)"
                ></div>
              </div>
              <span class="score-text">{{ item.economyScore }}/100</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <el-button 
              size="small" 
              @click.stop="toggleComparison(item)"
              :type="isSelected(item.id) ? 'primary' : ''"
              :disabled="!isSelected(item.id) && comparisonList.length >= 3"
            >
              {{ isSelected(item.id) ? '已选' : '对比' }}
            </el-button>
            <el-dropdown @click.stop trigger="click" size="small">
              <el-button size="small" :icon="MoreFilled" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="viewDetails(item)">
                    <el-icon><View /></el-icon>
                    查看详情
                  </el-dropdown-item>
                  <el-dropdown-item @click="addToWishlist(item)">
                    <el-icon><Star /></el-icon>
                    加入心愿单
                  </el-dropdown-item>
                  <el-dropdown-item @click="shareModel(item)">
                    <el-icon><Share /></el-icon>
                    分享车型
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-if="!loading && fuelConsRankingList.length === 0" description="暂无符合条件的车型数据" />
      </div>

      <!-- 分页控件 -->
      <div class="pagination-wrapper" v-if="fuelConsRankingList.length > displayCount">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="displayCount"
          :total="fuelConsRankingList.length"
          layout="prev, pager, next, jumper, total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 快速对比工具 -->
    <el-card shadow="never" class="quick-compare-card" v-if="comparisonList.length > 0">
      <template #header>
        <div class="compare-header">
          <span>快速对比 ({{ comparisonList.length }}/3)</span>
          <div class="compare-actions">
            <el-button size="small" @click="clearComparison">清空对比</el-button>
            <el-button
              size="small"
              type="primary"
              @click="startComparison"
              :disabled="comparisonList.length < 2"
            >
              开始对比
            </el-button>
          </div>
        </div>
      </template>

      <div class="selected-models">
        <div v-for="model in comparisonList" :key="model.id" class="selected-model-item">
          <img :src="model.image" :alt="model.name" class="model-thumb" />
          <div class="model-info">
            <span class="model-name">{{ model.brand }} {{ model.name }}</span>
            <span class="model-fuel">{{ formatFuelConsumption(model.fuelConsumption, model.fuelType) }} {{ getFuelUnit(model.fuelType) }}</span>
          </div>
          <el-button
            size="small"
            type="danger"
            :icon="Close"
            @click="removeFromComparison(model.id)"
          />
        </div>
      </div>
    </el-card>

    <!-- 实用工具集 -->
    <el-row :gutter="20" class="utility-tools">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="utility-card">
          <template #header>
            <div class="utility-header">
              <el-icon><Money /></el-icon>
              <span>实时油价信息</span>
            </div>
          </template>
          <div class="fuel-price-info">
            <div class="price-display">
              <div class="main-price">
                <span class="price-label">92#汽油</span>
                <span class="price-value">¥{{ fuelPrices.gasoline92 }}</span>
                <span class="price-unit">/升</span>
              </div>
              <div class="price-list">
                <div class="price-item">
                  <span>95#汽油：¥{{ fuelPrices.gasoline95 }}/升</span>
                </div>
                <div class="price-item">
                  <span>98#汽油：¥{{ fuelPrices.gasoline98 }}/升</span>
                </div>
                <div class="price-item">
                  <span>0#柴油：¥{{ fuelPrices.diesel }}/升</span>
                </div>
              </div>
            </div>
            <div class="price-trend">
              <span class="trend-label">本月趋势：</span>
              <span class="trend-value" :class="fuelPriceTrend > 0 ? 'increase' : 'decrease'">
                {{ fuelPriceTrend > 0 ? '+' : '' }}{{ fuelPriceTrend.toFixed(2) }}元/升
              </span>
            </div>
            <div class="update-time">
              <span>更新时间：{{ fuelPriceUpdateTime }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="utility-card">
          <template #header>
            <div class="utility-header">
              <el-icon><Document /></el-icon>
              <span>节能政策信息</span>
            </div>
          </template>
          <div class="policy-info">
            <div class="policy-item" v-for="policy in energyPolicies" :key="policy.id">
              <div class="policy-title">{{ policy.title }}</div>
              <div class="policy-content">{{ policy.description }}</div>
              <div class="policy-meta">
                <el-tag :type="policy.status === 'active' ? 'success' : 'info'" size="small">
                  {{ policy.status === 'active' ? '进行中' : '即将开始' }}
                </el-tag>
                <span class="policy-date">{{ policy.validUntil }}</span>
              </div>
            </div>
            <div class="policy-more">
              <el-button size="small" type="text" @click="viewMorePolicies">
                查看更多政策 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 对比计算器弹窗 -->
    <el-dialog v-model="showCalculatorModal" title="多车型成本对比计算器" width="80%" :before-close="handleCalculatorClose">
      <div class="modal-calculator-content">
        <div class="calculator-models">
          <h4>选择对比车型（最多3款）</h4>
          <div class="model-selection">
            <div 
              v-for="model in comparisonList" 
              :key="model.id"
              class="calculator-model-item"
            >
              <img :src="model.image" :alt="model.name" class="calc-model-image" />
              <div class="calc-model-info">
                <h5>{{ model.brand }} {{ model.name }}</h5>
                <div class="calc-model-fuel">{{ formatFuelConsumption(model.fuelConsumption, model.fuelType) }} {{ getFuelUnit(model.fuelType) }}</div>
                <div class="calc-cost-result">
                  <span>年成本：¥{{ calculateYearlyCost(model) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="calculator-summary" v-if="comparisonList.length >= 2">
          <h4>成本对比总结</h4>
          <div class="summary-chart">
            <div ref="costComparisonChart" class="cost-chart-container"></div>
          </div>
          <div class="savings-analysis">
            <div class="savings-item">
              <span class="savings-label">最省油车型：</span>
              <span class="savings-value">{{ getMostEfficientModel().brand }} {{ getMostEfficientModel().name }}</span>
            </div>
            <div class="savings-item">
              <span class="savings-label">年节省金额：</span>
              <span class="savings-value">¥{{ calculateMaxSavings() }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCalculatorModal = false">关闭</el-button>
          <el-button type="primary" @click="exportComparisonReport" :disabled="comparisonList.length < 2">
            导出对比报告
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 车型详情抽屉 -->
    <el-drawer v-model="showDetailDrawer" title="车型详情" direction="rtl" size="50%">
      <div class="detail-content" v-if="selectedCarDetail">
        <!-- 车型详情内容 -->
        <div class="detail-header">
          <img :src="selectedCarDetail.image" :alt="selectedCarDetail.name" class="detail-image" />
          <div class="detail-basic">
            <h2>{{ selectedCarDetail.brand }} {{ selectedCarDetail.name }}</h2>
            <p class="detail-price">{{ selectedCarDetail.priceRange }}</p>
            <div class="detail-tags">
              <el-tag type="primary">{{ selectedCarDetail.type }}</el-tag>
              <el-tag :type="getFuelTypeColor(selectedCarDetail.fuelType)">{{ selectedCarDetail.fuelType }}</el-tag>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="detail-sections">
          <div class="detail-section">
            <h4>油耗表现</h4>
            <el-row :gutter="16">
              <el-col :span="8">
                <div class="detail-metric">
                  <span class="metric-label">综合油耗</span>
                  <span class="metric-value">{{ formatFuelConsumption(selectedCarDetail.fuelConsumption, selectedCarDetail.fuelType) }} {{ getFuelUnit(selectedCarDetail.fuelType) }}</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="detail-metric">
                  <span class="metric-label">市区油耗</span>
                  <span class="metric-value">{{ formatFuelConsumption(selectedCarDetail.cityConsumption, selectedCarDetail.fuelType) }} {{ getFuelUnit(selectedCarDetail.fuelType) }}</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="detail-metric">
                  <span class="metric-label">高速油耗</span>
                  <span class="metric-value">{{ formatFuelConsumption(selectedCarDetail.highwayConsumption, selectedCarDetail.fuelType) }} {{ getFuelUnit(selectedCarDetail.fuelType) }}</span>
                </div>
              </el-col>
            </el-row>
          </div>

          <div class="detail-section">
            <h4>成本分析</h4>
            <div class="cost-analysis">
              <div class="cost-breakdown">
                <div class="breakdown-item">
                  <span class="breakdown-label">年油费（按1.5万公里）</span>
                  <span class="breakdown-value">¥{{ calculateYearlyCost(selectedCarDetail) }}</span>
                </div>
                <div class="breakdown-item">
                  <span class="breakdown-label">每公里燃料成本</span>
                  <span class="breakdown-value">¥{{ calculatePerKmCost(selectedCarDetail) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-actions">
          <el-button type="primary" size="large" @click="addToComparison(selectedCarDetail)">
            加入对比
          </el-button>
          <el-button size="large" @click="addToWishlist(selectedCarDetail)">
            加入心愿单
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
/* 整体布局 */
.fuel-cons-list {
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

/* 个性化计算器卡片 */
.calculator-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.calculator-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.calculator-content {
  padding: 24px;
}

.calculator-inputs {
  padding-right: 20px;
}

.live-price-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 8px;
  font-size: 12px;
}

.tip-text {
  color: #4facfe;
  font-weight: 500;
}

.calculator-results {
  padding-left: 20px;
  border-left: 1px solid #e8eaed;
}

.calculator-results h4 {
  margin: 0 0 24px 0;
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 600;
}

.cost-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.cost-card {
  text-align: center;
  padding: 20px 16px;
  border-radius: 12px;
  transition: all 0.3s ease;
  border: 1px solid #e8eaed;
}

.cost-card.daily {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-color: #4facfe;
}

.cost-card.monthly {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-color: #67c23a;
}

.cost-card.yearly {
  background: linear-gradient(135deg, #fff7ed 0%, #fed7aa 100%);
  border-color: #e6a23c;
}

.cost-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.cost-label {
  display: block;
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.cost-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.fuel-saving-tips {
  padding: 16px;
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
  border-radius: 12px;
  border-left: 4px solid #ffd700;
}

.fuel-saving-tips h5 {
  margin: 0 0 12px 0;
  color: #b8860b;
  font-size: 16px;
  font-weight: 600;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  list-style: none;
}

.tips-list li {
  margin-bottom: 8px;
  color: #8b6914;
  font-size: 14px;
  position: relative;
  line-height: 1.4;
}

.tips-list li::before {
  content: '💡';
  margin-right: 8px;
}

/* 筛选配置卡片 */
.filter-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.filter-content {
  padding: 16px 0;
}

.filter-content .el-row {
  align-items: end;
}

.filter-content .el-form-item {
  margin-bottom: 8px;
}

.filter-content .el-form-item__label {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.filter-content .el-select {
  width: 100%;
}

/* 油耗排行榜主体 */
.ranking-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.ranking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.ranking-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ranking-list {
  padding: 8px 0;
}

.ranking-item {
  display: grid;
  grid-template-columns: auto 160px 1fr 200px 180px 120px;
  gap: 20px;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f2f5;
  transition: all 0.3s ease;
  cursor: pointer;
}

.ranking-item:hover {
  background: #f8fafb;
  transform: translateX(4px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.ranking-item.selected {
  background: #f0f9ff;
  border-color: #4facfe;
}

.ranking-item.top-three {
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
  border-left: 4px solid #ffd700;
}

.ranking-item.eco-champion {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-left: 4px solid #67c23a;
}

/* 排名标识 */
.rank-number {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  color: white;
  background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%);
  box-shadow: 0 4px 15px rgba(144, 147, 153, 0.3);
  flex-shrink: 0;
}

.rank-number.rank-1 {
  background: linear-gradient(135deg, #ffd700 0%, #ffb300 100%);
  box-shadow: 0 4px 15px rgba(255, 215, 0, 0.3);
}

.rank-number.rank-2 {
  background: linear-gradient(135deg, #c0c4cc 0%, #909399 100%);
  box-shadow: 0 4px 15px rgba(192, 196, 204, 0.3);
}

.rank-number.rank-3 {
  background: linear-gradient(135deg, #cd7f32 0%, #b8721e 100%);
  box-shadow: 0 4px 15px rgba(205, 127, 50, 0.3);
}

/* 车型图片 */
.vehicle-image {
  position: relative;
  flex-shrink: 0;
}

.vehicle-image img {
  width: 160px;
  height: 100px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.image-overlay {
  position: absolute;
  top: 8px;
  left: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 车型信息 */
.vehicle-info {
  flex: 1;
  min-width: 0;
}

.vehicle-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.3;
}

.vehicle-specs {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
}

.price-range {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
}

.fuel-type-badge {
  display: flex;
  gap: 8px;
}

/* 油耗核心数据 */
.fuel-data {
  text-align: center;
  min-width: 200px;
}

.main-fuel-consumption {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin-bottom: 12px;
}

.fuel-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.fuel-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.fuel-value.excellent {
  color: #67c23a;
}

.fuel-value.good {
  color: #4facfe;
}

.fuel-value.normal {
  color: #e6a23c;
}

.fuel-unit {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.detailed-fuel-data {
  display: flex;
  justify-content: space-around;
  margin-bottom: 12px;
}

.fuel-scenario {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.scenario-label {
  font-size: 10px;
  color: #909399;
}

.scenario-value {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.scenario-value.real-world {
  color: #f56c6c;
  font-weight: 700;
}

.data-reliability {
  display: flex;
  justify-content: center;
}

/* 经济性评估 */
.economy-assessment {
  min-width: 180px;
}

.cost-preview {
  margin-bottom: 12px;
}

.cost-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.cost-label {
  font-size: 12px;
  color: #909399;
}

.cost-value {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.cost-comparison {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.comparison-label {
  color: #909399;
}

.comparison-value.higher {
  color: #f56c6c;
}

.comparison-value.lower {
  color: #67c23a;
}

.economy-score {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.score-label {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.score-bar {
  height: 6px;
  background: #e8eaed;
  border-radius: 3px;
  overflow: hidden;
  position: relative;
}

.score-fill {
  height: 100%;
  border-radius: 3px;
  transition: all 0.3s ease;
}

.score-fill.excellent {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.score-fill.good {
  background: linear-gradient(135deg, #4facfe 0%, #66b1ff 100%);
}

.score-fill.fair {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.score-fill.poor {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.score-text {
  font-size: 11px;
  color: #4facfe;
  text-align: center;
  font-weight: 600;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 120px;
}

.action-buttons .el-button {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.action-buttons .el-dropdown {
  width: 100%;
}

.action-buttons .el-dropdown .el-button {
  width: 100%;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px 0 8px 0;
}

.pagination-wrapper .el-pagination {
  --el-pagination-button-color: #606266;
  --el-pagination-button-bg-color: white;
  --el-pagination-button-border-radius: 8px;
}

/* 快速对比工具卡片 */
.quick-compare-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #4facfe;
  overflow: hidden;
}

.compare-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #4facfe;
  font-size: 16px;
}

.compare-actions {
  display: flex;
  gap: 8px;
}

.selected-models {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  padding: 8px 0;
}

.selected-model-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: 12px;
  border: 1px solid #4facfe;
}

.model-thumb {
  width: 60px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
}

.model-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.model-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.model-fuel {
  font-size: 12px;
  color: #4facfe;
  font-weight: 500;
}

/* 实用工具集 */
.utility-tools {
  margin-bottom: 24px;
}

.utility-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.utility-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

/* 实时油价信息 */
.fuel-price-info {
  padding: 20px;
}

.price-display {
  margin-bottom: 16px;
}

.main-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.price-label {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.price-value {
  font-size: 28px;
  font-weight: 700;
  color: #f56c6c;
}

.price-unit {
  font-size: 14px;
  color: #909399;
}

.price-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.price-item {
  padding: 8px 12px;
  background: #f8fafb;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
}

.price-trend {
  margin-bottom: 8px;
  font-size: 14px;
}

.trend-label {
  color: #909399;
}

.trend-value.increase {
  color: #f56c6c;
  font-weight: 600;
}

.trend-value.decrease {
  color: #67c23a;
  font-weight: 600;
}

.update-time {
  font-size: 12px;
  color: #c0c4cc;
}

/* 节能政策信息 */
.policy-info {
  padding: 20px;
}

.policy-item {
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
  margin-bottom: 16px;
  border-left: 4px solid #4facfe;
}

.policy-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.policy-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 12px;
}

.policy-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.policy-date {
  font-size: 12px;
  color: #909399;
}

.policy-more {
  text-align: center;
}

/* 对比计算器弹窗 */
.modal-calculator-content {
  max-height: 70vh;
  overflow-y: auto;
}

.calculator-models {
  margin-bottom: 24px;
}

.calculator-models h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.model-selection {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.calculator-model-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  flex: 1;
  min-width: 240px;
}

.calc-model-image {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
}

.calc-model-info {
  flex: 1;
}

.calc-model-info h5 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.calc-model-fuel {
  font-size: 14px;
  color: #4facfe;
  margin-bottom: 8px;
}

.calc-cost-result {
  font-size: 12px;
  color: #909399;
}

.calculator-summary h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.cost-chart-container {
  height: 250px;
  width: 100%;
  margin-bottom: 16px;
}

.savings-analysis {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.savings-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: 8px;
}

.savings-label {
  font-size: 14px;
  color: #606266;
}

.savings-value {
  font-size: 16px;
  font-weight: 600;
  color: #4facfe;
}

/* 车型详情抽屉 */
.detail-content {
  padding: 16px;
}

.detail-header {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.detail-image {
  width: 200px;
  height: 120px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.detail-basic {
  flex: 1;
}

.detail-basic h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.detail-price {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #f56c6c;
}

.detail-tags {
  display: flex;
  gap: 8px;
}

.detail-sections {
  margin-bottom: 24px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.detail-metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: #f8fafb;
  border-radius: 8px;
}

.metric-label {
  font-size: 12px;
  color: #909399;
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.cost-analysis {
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
}

.cost-breakdown {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.breakdown-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #e8eaed;
}

.breakdown-item:last-child {
  border-bottom: none;
}

.breakdown-label {
  font-size: 14px;
  color: #606266;
}

.breakdown-value {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.detail-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
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

.el-form-item__label {
  font-weight: 500;
}

.el-select .el-input__wrapper {
  border-radius: 8px;
}

.el-input-number .el-input__wrapper {
  border-radius: 8px;
}

.el-radio-group .el-radio {
  margin-right: 16px;
}

.el-radio__label {
  font-weight: 500;
}

.el-switch__label {
  font-weight: 500;
}

.el-rate {
  --el-rate-fill-color: #ff9900;
}

.el-dialog__header {
  border-radius: 8px 8px 0 0;
}

.el-drawer__header {
  border-bottom: 1px solid #e8eaed;
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

.calculator-card,
.filter-card,
.ranking-card,
.quick-compare-card,
.utility-card {
  animation: slideInUp 0.6s ease-out;
}

.ranking-item {
  animation: slideInUp 0.4s ease-out;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .ranking-item {
    grid-template-columns: auto 140px 1fr 180px 160px 100px;
    gap: 16px;
  }

  .header-left h2 {
    font-size: 28px;
  }

  .cost-cards {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

@media (max-width: 1200px) {
  .ranking-item {
    grid-template-columns: auto 120px 1fr 160px 140px;
    gap: 12px;
  }

  .calculator-content {
    padding: 16px;
  }

  .calculator-inputs,
  .calculator-results {
    padding: 0;
  }

  .calculator-results {
    border-left: none;
    border-top: 1px solid #e8eaed;
    padding-top: 20px;
    margin-top: 20px;
  }

  .utility-tools .el-col {
    margin-bottom: 16px;
  }
}

@media (max-width: 768px) {
  .fuel-cons-list {
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

  .ranking-item {
    grid-template-columns: auto 1fr;
    gap: 16px;
    padding: 16px;
  }

  .rank-number {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }

  .vehicle-image img {
    width: 120px;
    height: 80px;
  }

  .fuel-value {
    font-size: 20px;
  }

  .filter-content .el-row {
    flex-direction: column;
    gap: 12px;
  }

  .ranking-controls {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .selected-models {
    flex-direction: column;
    gap: 12px;
  }

  .model-selection {
    flex-direction: column;
  }

  .detail-header {
    flex-direction: column;
    gap: 16px;
  }

  .detail-image {
    width: 100%;
    max-width: 300px;
    margin: 0 auto;
  }

  .detail-actions {
    flex-direction: column;
  }

  .savings-analysis {
    gap: 8px;
  }

  .savings-item {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
}

@media (max-width: 480px) {
  .calculator-models h4,
  .calculator-summary h4 {
    font-size: 14px;
  }

  .calculator-model-item {
    min-width: 100%;
  }

  .detail-basic h2 {
    font-size: 20px;
  }

  .price-display {
    text-align: center;
  }

  .main-price {
    justify-content: center;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .fuel-cons-list {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .calculator-card,
  .filter-card,
  .ranking-card,
  .quick-compare-card,
  .utility-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .ranking-item,
  .selected-model-item,
  .calculator-model-item {
    background: #2d2d2d;
    border-color: #404040;
  }

  .fuel-saving-tips,
  .cost-card,
  .policy-item {
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
.el-input__wrapper:focus-within,
.el-select:focus-within .el-input__wrapper,
.ranking-item:focus-visible {
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
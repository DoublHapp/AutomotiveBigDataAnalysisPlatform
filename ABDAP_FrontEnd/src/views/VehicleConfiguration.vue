<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  RefreshRight,
  TrendCharts,
  QuestionFilled,
  Location,
  ArrowRight,
  Money,
  Trophy,
  DataAnalysis,
  WarningFilled,
  Warning,
  InfoFilled,
  Check,
  Close,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

// 接口定义
interface FilterForm {
  priceRange: string
  vehicleType: string
  powerType: string
}

interface ConfigData {
  configName: string
  averageRate: number
  coverageCount: number
  trend: number
  description: string
  brandRanking: BrandRankingItem[]
}

interface BrandRankingItem {
  brand: string
  rate: number
}

interface RegionalPreference {
  id: number
  name: string
  preferenceScore: number
  growth: number
}

interface RegionalConfig {
  name: string
  regions: RegionDetail[]
}

interface RegionDetail {
  name: string
  level: string
  levelText: string
  equipmentRate: number
  demandIndex: number
  insight: string
}

interface RoiRankingItem {
  id: number
  name: string
  roi: number
  profitMargin: number
}

interface CompetitorComparisonData {
  configName: string
  category: string
  marketStandard: string
  competitors: Record<number, any>
  opportunity?: {
    level: string
    text: string
  }
}

interface Competitor {
  id: number
  name: string
  logo: string
}

interface GapItem {
  id: number
  name: string
  impact?: number
  potential?: number
}

interface TrendInsight {
  id: number
  type: string
  icon: any
  title: string
  description: string
  expectedGrowth: number
  impactLevel: string
  actionType: string
  actionText: string
}

interface CompetitorForm {
  brandId: number | null
  modelIds: number[]
}

interface Brand {
  id: number
  name: string
  logo: string
}

interface Model {
  id: number
  name: string
  priceRange: string
}

// 响应式数据
const loading = ref(false)
const analyzing = ref(false)

// 筛选条件
const filterForm = ref<FilterForm>({
  priceRange: 'all',
  vehicleType: 'all',
  powerType: 'all',
})

const analysisDimension = ref('heatmap')

// 配置热度数据
const heatmapData = ref<any[]>([])
const heatmapViewType = ref('rate')
const selectedConfigData = ref<ConfigData | null>(null)

// 地域分析数据
const selectedRegionLevel = ref('province')
const regionalPreferences = ref<RegionalPreference[]>([])
const selectedRegionalConfig = ref<RegionalConfig | null>(null)

// 盈利分析数据
const roiRanking = ref<RoiRankingItem[]>([])
const highValueConfigs = ref<string[]>(['智能驾驶辅助', '全景天窗', '无线充电'])
const costSensitiveConfigs = ref<string[]>(['空气悬架', '主动降噪', '后排娱乐'])
const opportunityConfigs = ref<string[]>(['座椅通风', 'HUD抬显', '氛围灯'])
const reviewConfigs = ref<string[]>(['按摩座椅', '香氛系统', '后排小桌板'])

// 竞品对标数据
const selectedCompetitorSet = ref('mainstream')
const competitorComparisonData = ref<CompetitorComparisonData[]>([])
const selectedCompetitors = ref<Competitor[]>([])
const criticalGaps = ref<GapItem[]>([])
const moderateGaps = ref<GapItem[]>([])
const opportunityGaps = ref<GapItem[]>([])
const trendInsights = ref<TrendInsight[]>([])

// 弹窗和表单
const showAddCompetitorDialog = ref(false)
const competitorForm = ref<CompetitorForm>({
  brandId: null,
  modelIds: [],
})
const availableBrands = ref<Brand[]>([])
const availableModels = ref<Model[]>([])

// 图表实例
let configHeatmapChartInstance: echarts.ECharts | null = null
let regionalChartInstance: echarts.ECharts | null = null
let profitChartInstance: echarts.ECharts | null = null
let costBenefitChartInstance: echarts.ECharts | null = null
let trendChartInstance: echarts.ECharts | null = null

// 计算属性
const getRegionLevelText = () => {
  const textMap = {
    province: '省级',
    cityLevel: '城市级别',
    economic: '经济区域',
  }
  return textMap[selectedRegionLevel.value] || '省级'
}

// 工具函数
const getConfigPopularityType = (rate: number) => {
  if (rate >= 80) return 'success'
  if (rate >= 60) return 'primary'
  if (rate >= 40) return 'warning'
  return 'danger'
}

const getConfigPopularityText = (rate: number) => {
  if (rate >= 80) return '热门配置'
  if (rate >= 60) return '主流配置'
  if (rate >= 40) return '小众配置'
  return '冷门配置'
}

const getTrendClass = (trend: number) => {
  if (trend > 5) return 'positive'
  if (trend < -5) return 'negative'
  return 'neutral'
}

const getTrendText = (trend: number) => {
  if (trend > 5) return `上升 +${trend.toFixed(1)}%`
  if (trend < -5) return `下降 ${trend.toFixed(1)}%`
  return '平稳'
}

const getRegionLevelTag = (level: string) => {
  const tagMap = {
    high: 'success',
    medium: 'warning',
    low: 'info',
  }
  return tagMap[level] || 'info'
}

const getRoiColor = (roi: number) => {
  if (roi >= 200) return '#67c23a'
  if (roi >= 150) return '#4facfe'
  if (roi >= 100) return '#e6a23c'
  return '#f56c6c'
}

const getConfigIcon = (category: string) => {
  const iconMap = {
    safety: WarningFilled,
    comfort: Check,
    technology: TrendCharts,
    performance: Trophy,
  }
  return iconMap[category] || Check
}

const getStandardType = (standard: string) => {
  const typeMap = {
    标配: 'success',
    选配: 'warning',
    高配: 'primary',
    无: 'info',
  }
  return typeMap[standard] || 'info'
}

const getConfigStatusClass = (status: any) => {
  if (!status) return 'status-none'
  if (status.level === 'standard') return 'status-standard'
  if (status.level === 'optional') return 'status-optional'
  if (status.level === 'premium') return 'status-premium'
  return 'status-none'
}

const getConfigStatusIcon = (status: any) => {
  if (!status) return Close
  if (status.level === 'standard') return Check
  if (status.level === 'optional') return Warning
  if (status.level === 'premium') return Trophy
  return Close
}

const getConfigLevelText = (status: any) => {
  if (!status) return '无'
  const levelMap = {
    standard: '标配',
    optional: '选配',
    premium: '高配',
  }
  return levelMap[status.level] || '无'
}

const getOpportunityType = (level: string) => {
  const typeMap = {
    high: 'success',
    medium: 'warning',
    low: 'info',
  }
  return typeMap[level] || 'info'
}

// 事件处理函数
const handleDimensionChange = async (dimension: string) => {
  analyzing.value = true

  try {
    switch (dimension) {
      case 'heatmap':
        await fetchHeatmapData()
        await nextTick()
        initConfigHeatmapChart()
        break
      case 'regional':
        await fetchRegionalData()
        await nextTick()
        initRegionalChart()
        break
      case 'profit':
        await fetchProfitData()
        await nextTick()
        initProfitChart()
        initCostBenefitChart()
        break
      case 'competitor':
        await fetchCompetitorData()
        await nextTick()
        initTrendChart()
        break
    }

    ElMessage.success('分析维度已切换')
  } catch (error) {
    ElMessage.error('切换失败')
  } finally {
    analyzing.value = false
  }
}

const resetFilters = () => {
  filterForm.value = {
    priceRange: 'all',
    vehicleType: 'all',
    powerType: 'all',
  }
  ElMessage.success('筛选条件已重置')
}

const applyFilters = async () => {
  analyzing.value = true

  try {
    await refreshCurrentDimensionData()
    ElMessage.success('筛选条件已应用')
  } catch (error) {
    ElMessage.error('应用筛选失败')
  } finally {
    analyzing.value = false
  }
}

const toggleHeatmapView = () => {
  heatmapViewType.value = heatmapViewType.value === 'rate' ? 'count' : 'rate'
  initConfigHeatmapChart()
}

const selectRegionalConfig = (config: RegionalPreference) => {
  selectedRegionalConfig.value = {
    name: config.name,
    regions: generateMockRegionDetails(config.name),
  }
}

const refreshProfitData = async () => {
  analyzing.value = true

  try {
    await fetchProfitData()
    await nextTick()
    initProfitChart()
    initCostBenefitChart()
    ElMessage.success('盈利数据已重新计算')
  } catch (error) {
    ElMessage.error('重新计算失败')
  } finally {
    analyzing.value = false
  }
}

const exportProfitReport = () => {
  if (roiRanking.value.length === 0) {
    ElMessage.warning('暂无盈利数据可导出')
    return
  }

  const content = [
    '配置盈利贡献分析报告',
    `生成时间: ${new Date().toLocaleString()}`,
    `筛选条件: ${filterForm.value.priceRange} | ${filterForm.value.vehicleType} | ${filterForm.value.powerType}`,
    '',
    'ROI排行榜:',
    ...roiRanking.value.map(
      (item, index) => `${index + 1}. ${item.name}: ROI ${item.roi}%, 利润率 ${item.profitMargin}%`,
    ),
    '',
    '配置分类建议:',
    '高价值配置: ' + highValueConfigs.value.join(', '),
    '成本敏感配置: ' + costSensitiveConfigs.value.join(', '),
    '市场机会配置: ' + opportunityConfigs.value.join(', '),
    '待评估配置: ' + reviewConfigs.value.join(', '),
  ].join('\n')

  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `配置盈利分析报告_${new Date().toISOString().slice(0, 10)}.txt`
  link.click()

  ElMessage.success('盈利报告已导出')
}

const addCompetitor = () => {
  showAddCompetitorDialog.value = true
}

const confirmAddCompetitor = () => {
  if (!competitorForm.value.brandId || competitorForm.value.modelIds.length === 0) {
    ElMessage.warning('请选择品牌和车型')
    return
  }

  const brand = availableBrands.value.find((b) => b.id === competitorForm.value.brandId)
  if (brand) {
    const newCompetitor: Competitor = {
      id: Date.now(),
      name: brand.name,
      logo: brand.logo,
    }

    selectedCompetitors.value.push(newCompetitor)
    showAddCompetitorDialog.value = false

    // 重置表单
    competitorForm.value = {
      brandId: null,
      modelIds: [],
    }

    ElMessage.success(`${brand.name} 已添加到对比`)
  }
}

const loadSampleData = async () => {
  loading.value = true

  try {
    heatmapData.value = generateMockHeatmapData()
    regionalPreferences.value = generateMockRegionalPreferences()
    roiRanking.value = generateMockRoiRanking()

    await nextTick()

    if (analysisDimension.value === 'heatmap') {
      initConfigHeatmapChart()
    }

    ElMessage.success('示例数据已加载')
  } catch (error) {
    ElMessage.error('加载示例数据失败')
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  loading.value = true

  try {
    await refreshCurrentDimensionData()
    ElMessage.success('数据已刷新')
  } catch (error) {
    ElMessage.error('数据刷新失败')
  } finally {
    loading.value = false
  }
}

const exportReport = () => {
  if (heatmapData.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  const content = [
    '车辆配置热度分析报告',
    `分析维度: ${getDimensionText()}`,
    `筛选条件: ${getFilterText()}`,
    `生成时间: ${new Date().toLocaleString()}`,
    '',
    '分析摘要:',
    getAnalysisSummary(),
    '',
    '详细数据:',
    getDetailedData(),
  ].join('\n')

  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `配置热度分析报告_${new Date().toISOString().slice(0, 10)}.txt`
  link.click()

  ElMessage.success('分析报告已导出')
}

// API调用函数
const fetchHeatmapData = async () => {
  try {
    const response = await axios.get('/api/configuration/heatmap', {
      params: filterForm.value,
    })

    if (response.data.status === 1) {
      heatmapData.value = response.data.data
    }
  } catch (error) {
    console.error('获取热力图数据失败:', error)
    heatmapData.value = generateMockHeatmapData()
  }
}

const fetchRegionalData = async () => {
  try {
    const response = await axios.get('/api/configuration/regional', {
      params: {
        ...filterForm.value,
        regionLevel: selectedRegionLevel.value,
      },
    })

    if (response.data.status === 1) {
      regionalPreferences.value = response.data.data
    }
  } catch (error) {
    console.error('获取地域数据失败:', error)
    regionalPreferences.value = generateMockRegionalPreferences()
  }
}

const fetchProfitData = async () => {
  try {
    const response = await axios.get('/api/configuration/profit', {
      params: filterForm.value,
    })

    if (response.data.status === 1) {
      roiRanking.value = response.data.data.roiRanking
      highValueConfigs.value = response.data.data.highValue
      costSensitiveConfigs.value = response.data.data.costSensitive
      opportunityConfigs.value = response.data.data.opportunity
      reviewConfigs.value = response.data.data.review
    }
  } catch (error) {
    console.error('获取盈利数据失败:', error)
    roiRanking.value = generateMockRoiRanking()
  }
}

const fetchCompetitorData = async () => {
  try {
    const response = await axios.get('/api/configuration/competitor', {
      params: {
        ...filterForm.value,
        competitorSet: selectedCompetitorSet.value,
      },
    })

    if (response.data.status === 1) {
      competitorComparisonData.value = response.data.data.comparison
      criticalGaps.value = response.data.data.criticalGaps
      moderateGaps.value = response.data.data.moderateGaps
      opportunityGaps.value = response.data.data.opportunityGaps
      trendInsights.value = response.data.data.trendInsights
      selectedCompetitors.value = response.data.data.competitors
    }
  } catch (error) {
    console.error('获取竞品数据失败:', error)
    competitorComparisonData.value = generateMockCompetitorData()
    criticalGaps.value = generateMockGaps('critical')
    moderateGaps.value = generateMockGaps('moderate')
    opportunityGaps.value = generateMockGaps('opportunity')
    trendInsights.value = generateMockTrendInsights()
    selectedCompetitors.value = generateMockCompetitors()
  }
}

const refreshCurrentDimensionData = async () => {
  switch (analysisDimension.value) {
    case 'heatmap':
      await fetchHeatmapData()
      break
    case 'regional':
      await fetchRegionalData()
      break
    case 'profit':
      await fetchProfitData()
      break
    case 'competitor':
      await fetchCompetitorData()
      break
  }
}

// 模拟数据生成函数
const generateMockHeatmapData = () => {
  const configs = [
    '智能驾驶',
    '全景天窗',
    '座椅加热',
    'HUD抬显',
    '无线充电',
    '氛围灯',
    '空气悬架',
    '主动降噪',
  ]
  const brands = ['特斯拉', 'BYD', 'NIO', 'XPeng', '理想', '奔驰', '宝马', '奥迪']

  return configs.map((config, i) => ({
    configName: config,
    averageRate: Math.floor(Math.random() * 60) + 40,
    coverageCount: Math.floor(Math.random() * 20) + 10,
    trend: (Math.random() - 0.5) * 20,
    description: `${config}在市场中的表现和用户接受度分析`,
    brandRanking: brands
      .map((brand) => ({
        brand,
        rate: Math.floor(Math.random() * 80) + 20,
      }))
      .sort((a, b) => b.rate - a.rate),
  }))
}

const generateMockRegionalPreferences = (): RegionalPreference[] => {
  const configs = [
    '智能驾驶',
    '全景天窗',
    '座椅加热',
    'HUD抬显',
    '无线充电',
    '氛围灯',
    '空气悬架',
    '主动降噪',
    '座椅通风',
    '后排娱乐',
  ]

  return configs.map((config, index) => ({
    id: index + 1,
    name: config,
    preferenceScore: Math.floor(Math.random() * 40) + 60,
    growth: (Math.random() - 0.3) * 30,
  }))
}

const generateMockRegionDetails = (configName: string): RegionDetail[] => {
  const regions = ['华东地区', '华南地区', '华北地区']

  return regions.map((region) => ({
    name: region,
    level: Math.random() > 0.5 ? 'high' : Math.random() > 0.5 ? 'medium' : 'low',
    levelText: Math.random() > 0.5 ? '高需求' : Math.random() > 0.5 ? '中需求' : '低需求',
    equipmentRate: Math.floor(Math.random() * 60) + 40,
    demandIndex: Math.floor(Math.random() * 40) + 60,
    insight: `${configName}在${region}的用户需求相对较高，建议重点推广`,
  }))
}

const generateMockRoiRanking = (): RoiRankingItem[] => {
  const configs = [
    '智能驾驶辅助',
    '全景天窗',
    '无线充电',
    '座椅加热',
    'HUD抬显',
    '氛围灯',
    '座椅通风',
    '主动降噪',
  ]

  return configs
    .map((config, index) => ({
      id: index + 1,
      name: config,
      roi: Math.floor(Math.random() * 200) + 100,
      profitMargin: Math.floor(Math.random() * 30) + 15,
    }))
    .sort((a, b) => b.roi - a.roi)
}

const generateMockCompetitorData = (): CompetitorComparisonData[] => {
  const configs = [
    { name: '自动驾驶辅助', category: 'safety' },
    { name: '全景天窗', category: 'comfort' },
    { name: '无线充电', category: 'technology' },
    { name: '座椅加热', category: 'comfort' },
    { name: 'HUD抬显', category: 'technology' },
  ]

  return configs.map((config) => ({
    configName: config.name,
    category: config.category,
    marketStandard: Math.random() > 0.5 ? '标配' : Math.random() > 0.5 ? '选配' : '无',
    competitors: {
      1: { level: 'standard' },
      2: { level: 'optional' },
      3: { level: 'premium' },
    },
    opportunity:
      Math.random() > 0.3
        ? {
            level: Math.random() > 0.5 ? 'high' : 'medium',
            text: Math.random() > 0.5 ? '差异化机会' : '成本优化机会',
          }
        : undefined,
  }))
}

const generateMockGaps = (type: string): GapItem[] => {
  const gapConfigs = {
    critical: ['高级辅助驾驶', '空气悬架'],
    moderate: ['座椅通风', '后排娱乐'],
    opportunity: ['香氛系统', '按摩座椅'],
  }

  return (
    gapConfigs[type]?.map((name, index) => ({
      id: index + 1,
      name,
      impact: type === 'opportunity' ? undefined : Math.floor(Math.random() * 40) + 60,
      potential: type === 'opportunity' ? Math.floor(Math.random() * 30) + 70 : undefined,
    })) || []
  )
}

const generateMockTrendInsights = (): TrendInsight[] => {
  return [
    {
      id: 1,
      type: 'positive',
      icon: TrendCharts,
      title: '智能化配置快速普及',
      description: '预计未来6个月智能驾驶配置装配率将提升25%',
      expectedGrowth: 25,
      impactLevel: '高',
      actionType: 'success',
      actionText: '抓住机遇',
    },
    {
      id: 2,
      type: 'warning',
      icon: Warning,
      title: '传统配置需求下降',
      description: 'CD播放器等传统配置需求持续走低',
      expectedGrowth: -15,
      impactLevel: '中',
      actionType: 'warning',
      actionText: '逐步淘汰',
    },
  ]
}

const generateMockCompetitors = (): Competitor[] => {
  return [
    { id: 1, name: '特斯拉', logo: 'https://via.placeholder.com/40x40?text=T' },
    { id: 2, name: 'BYD', logo: 'https://via.placeholder.com/40x40?text=B' },
    { id: 3, name: 'NIO', logo: 'https://via.placeholder.com/40x40?text=N' },
  ]
}

// 辅助函数
const getDimensionText = () => {
  const textMap = {
    heatmap: '配置热度矩阵',
    regional: '地域特征分析',
    profit: '盈利贡献分析',
    competitor: '竞品对标分析',
  }
  return textMap[analysisDimension.value] || '未知'
}

const getFilterText = () => {
  return `${filterForm.value.priceRange} | ${filterForm.value.vehicleType} | ${filterForm.value.powerType}`
}

const getAnalysisSummary = () => {
  switch (analysisDimension.value) {
    case 'heatmap':
      return '配置热度分析显示智能化配置受欢迎程度持续上升'
    case 'regional':
      return '地域分析表明一二线城市对高端配置需求更强'
    case 'profit':
      return 'ROI分析显示智能驾驶配置投入产出比最高'
    case 'competitor':
      return '竞品对标发现在智能化配置方面存在差异化机会'
    default:
      return ''
  }
}

const getDetailedData = () => {
  switch (analysisDimension.value) {
    case 'heatmap':
      return heatmapData.value.map((item) => `${item.configName}: ${item.averageRate}%`).join('\n')
    case 'profit':
      return roiRanking.value.map((item) => `${item.name}: ROI ${item.roi}%`).join('\n')
    default:
      return '详细数据请参考页面图表'
  }
}

// 图表初始化函数
const initConfigHeatmapChart = async () => {
  await nextTick()

  const chartElement = document.querySelector('.heatmap-chart') as HTMLElement
  if (!chartElement || heatmapData.value.length === 0) return

  if (configHeatmapChartInstance) {
    configHeatmapChartInstance.dispose()
  }

  configHeatmapChartInstance = echarts.init(chartElement)

  // 模拟热力图数据
  const configs = heatmapData.value.map((item) => item.configName)
  const brands = ['特斯拉', 'BYD', 'NIO', 'XPeng', '理想']
  const data: any[] = []

  configs.forEach((config, i) => {
    brands.forEach((brand, j) => {
      data.push([i, j, Math.floor(Math.random() * 100)])
    })
  })

  const option = {
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        return `${configs[params.data[0]]}<br/>${brands[params.data[1]]}<br/>装配率: ${params.data[2]}%`
      },
    },
    grid: {
      height: '70%',
      top: '10%',
    },
    xAxis: {
      type: 'category',
      data: configs,
      splitArea: {
        show: true,
      },
      axisLabel: {
        rotate: 45,
      },
    },
    yAxis: {
      type: 'category',
      data: brands,
      splitArea: {
        show: true,
      },
    },
    visualMap: {
      min: 0,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '5%',
      inRange: {
        color: ['#50a3ba', '#eac736', '#d94e5d'],
      },
    },
    series: [
      {
        name: '装配率',
        type: 'heatmap',
        data: data,
        label: {
          show: true,
          formatter: '{c}%',
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  }

  configHeatmapChartInstance.setOption(option)

  // 添加点击事件
  configHeatmapChartInstance.on('click', (params: any) => {
    const configName = configs[params.data[0]]
    const configData = heatmapData.value.find((item) => item.configName === configName)
    if (configData) {
      selectedConfigData.value = configData
    }
  })
}

const initRegionalChart = async () => {
  await nextTick()

  const chartElement = document.querySelector('.regional-chart') as HTMLElement
  if (!chartElement) return

  if (regionalChartInstance) {
    regionalChartInstance.dispose()
  }

  regionalChartInstance = echarts.init(chartElement)

  const option = {
    title: {
      text: '地域配置偏好热力图',
      left: 'center',
    },
    tooltip: {
      trigger: 'item',
    },
    series: [
      {
        type: 'map',
        map: 'china',
        data: [
          { name: '北京', value: 95 },
          { name: '上海', value: 92 },
          { name: '广东', value: 88 },
          { name: '浙江', value: 85 },
          { name: '江苏', value: 82 },
        ],
        nameMap: {
          China: '中国',
        },
      },
    ],
  }

  regionalChartInstance.setOption(option)
}

const initProfitChart = async () => {
  await nextTick()

  const chartElement = document.querySelector('.profit-chart') as HTMLElement
  if (!chartElement || roiRanking.value.length === 0) return

  if (profitChartInstance) {
    profitChartInstance.dispose()
  }

  profitChartInstance = echarts.init(chartElement)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}% ({d}%)',
    },
    series: [
      {
        name: '盈利贡献',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: roiRanking.value.slice(0, 6).map((item) => ({
          value: item.roi,
          name: item.name,
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  }

  profitChartInstance.setOption(option)
}

const initCostBenefitChart = async () => {
  await nextTick()

  const chartElement = document.querySelector('.matrix-chart') as HTMLElement
  if (!chartElement) return

  if (costBenefitChartInstance) {
    costBenefitChartInstance.dispose()
  }

  costBenefitChartInstance = echarts.init(chartElement)

  // 模拟成本效益矩阵数据
  const data = [
    [10, 85, '智能驾驶', 15],
    [25, 70, '全景天窗', 12],
    [35, 60, '座椅加热', 8],
    [50, 45, '空气悬架', 20],
    [60, 30, '按摩座椅', 25],
  ]

  const option = {
    xAxis: {
      name: '成本投入',
      nameLocation: 'middle',
      nameGap: 30,
    },
    yAxis: {
      name: '用户接受度',
      nameLocation: 'middle',
      nameGap: 50,
    },
    series: [
      {
        symbolSize: (data: any) => data[3],
        data: data,
        type: 'scatter',
      },
    ],
  }

  costBenefitChartInstance.setOption(option)
}

const initTrendChart = async () => {
  await nextTick()

  const chartElement = document.querySelector('.trend-chart') as HTMLElement
  if (!chartElement) return

  if (trendChartInstance) {
    trendChartInstance.dispose()
  }

  trendChartInstance = echarts.init(chartElement)

  const months = ['1月', '2月', '3月', '4月', '5月', '6月']
  const smartDriving = [45, 52, 58, 65, 72, 78]
  const panoramicRoof = [67, 68, 70, 71, 72, 73]

  const option = {
    title: {
      text: '配置趋势预测',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
    },
    legend: {
      data: ['智能驾驶', '全景天窗'],
      top: 30,
    },
    xAxis: {
      type: 'category',
      data: months,
    },
    yAxis: {
      type: 'value',
      name: '装配率(%)',
    },
    series: [
      {
        name: '智能驾驶',
        type: 'line',
        data: smartDriving,
        smooth: true,
        itemStyle: { color: '#4facfe' },
      },
      {
        name: '全景天窗',
        type: 'line',
        data: panoramicRoof,
        smooth: true,
        itemStyle: { color: '#67c23a' },
      },
    ],
  }

  trendChartInstance.setOption(option)
}

// 窗口大小调整
const handleResize = () => {
  const charts = [
    configHeatmapChartInstance,
    regionalChartInstance,
    profitChartInstance,
    costBenefitChartInstance,
    trendChartInstance,
  ]

  charts.forEach((chart) => {
    if (chart) {
      chart.resize()
    }
  })
}

// 监听器
watch([selectedRegionLevel], () => {
  if (analysisDimension.value === 'regional') {
    fetchRegionalData()
  }
})

watch([selectedCompetitorSet], () => {
  if (analysisDimension.value === 'competitor') {
    fetchCompetitorData()
  }
})

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用车辆配置热度分析！')

  try {
    // 初始化数据
    availableBrands.value = [
      { id: 1, name: '特斯拉', logo: 'https://via.placeholder.com/40x40?text=T' },
      { id: 2, name: 'BYD', logo: 'https://via.placeholder.com/40x40?text=B' },
      { id: 3, name: 'NIO', logo: 'https://via.placeholder.com/40x40?text=N' },
    ]

    availableModels.value = [
      { id: 1, name: 'Model 3', priceRange: '26-34万' },
      { id: 2, name: '汉EV', priceRange: '21-28万' },
      { id: 3, name: 'ES6', priceRange: '36-52万' },
    ]

    // 加载默认数据
    await loadSampleData()

    window.addEventListener('resize', handleResize)
  } catch (error) {
    console.error('页面初始化失败:', error)
    ElMessage.error('初始化失败，部分功能可能不可用')
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)

  // 销毁图表实例
  const charts = [
    configHeatmapChartInstance,
    regionalChartInstance,
    profitChartInstance,
    costBenefitChartInstance,
    trendChartInstance,
  ]

  charts.forEach((chart) => {
    if (chart) {
      chart.dispose()
    }
  })
})
</script>

<template>
  <div class="vehicle-configuration">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>车辆配置热度分析</h2>
          <p>基于市场数据的配置策略制定与竞品差异化分析</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
          <el-button
            type="success"
            :icon="Download"
            @click="exportReport"
            :disabled="!heatmapData.length"
          >
            导出报告
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 筛选控制区 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-content">
        <div class="filter-left">
          <el-form :inline="true" :model="filterForm" class="filter-form">
            <el-form-item label="价格区间:">
              <el-select
                v-model="filterForm.priceRange"
                placeholder="选择价格区间"
                style="width: 150px"
              >
                <el-option label="全部价位" value="all" />
                <el-option label="10万以下" value="under10" />
                <el-option label="10-20万" value="10-20" />
                <el-option label="20-30万" value="20-30" />
                <el-option label="30-50万" value="30-50" />
                <el-option label="50万以上" value="over50" />
              </el-select>
            </el-form-item>
            <el-form-item label="车型类别:">
              <el-select
                v-model="filterForm.vehicleType"
                placeholder="选择车型"
                style="width: 120px"
              >
                <el-option label="全部车型" value="all" />
                <el-option label="轿车" value="sedan" />
                <el-option label="SUV" value="suv" />
                <el-option label="MPV" value="mpv" />
                <el-option label="跑车" value="sports" />
              </el-select>
            </el-form-item>
            <el-form-item label="能源类型:">
              <el-select v-model="filterForm.powerType" placeholder="选择能源" style="width: 120px">
                <el-option label="全部类型" value="all" />
                <el-option label="燃油车" value="ice" />
                <el-option label="纯电动" value="bev" />
                <el-option label="混合动力" value="hev" />
                <el-option label="插电混动" value="phev" />
              </el-select>
            </el-form-item>
            <el-form-item label="分析维度:">
              <el-radio-group v-model="analysisDimension" @change="handleDimensionChange">
                <el-radio-button label="heatmap">配置热度</el-radio-button>
                <el-radio-button label="regional">地域分析</el-radio-button>
                <el-radio-button label="profit">盈利分析</el-radio-button>
                <el-radio-button label="competitor">竞品对标</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>
        <div class="filter-right">
          <el-button @click="resetFilters" :icon="RefreshRight">重置筛选</el-button>
          <el-button type="primary" @click="applyFilters" :loading="analyzing">应用筛选</el-button>
        </div>
      </div>
    </el-card>

    <!-- 配置热度矩阵分析 -->
    <el-card shadow="never" class="heatmap-card" v-show="analysisDimension === 'heatmap'">
      <template #header>
        <div class="card-header">
          <el-icon><TrendCharts /></el-icon>
          <span>配置热度矩阵分析</span>
          <div class="header-controls">
            <el-tooltip content="配置装配率越高，颜色越深" placement="top">
              <el-button size="small" type="text" :icon="QuestionFilled">图例说明</el-button>
            </el-tooltip>
            <el-button size="small" @click="toggleHeatmapView">
              {{ heatmapViewType === 'rate' ? '切换到数量视图' : '切换到比例视图' }}
            </el-button>
          </div>
        </div>
      </template>

      <div class="heatmap-content">
        <!-- 热力图容器 -->
        <div class="heatmap-container">
          <div ref="configHeatmapChart" class="heatmap-chart" v-loading="loading"></div>
        </div>

        <!-- 配置详情面板 -->
        <div class="config-details-panel" v-if="selectedConfigData">
          <div class="detail-header">
            <h4>{{ selectedConfigData.configName }}</h4>
            <el-tag :type="getConfigPopularityType(selectedConfigData.averageRate)">
              {{ getConfigPopularityText(selectedConfigData.averageRate) }}
            </el-tag>
          </div>
          <div class="detail-metrics">
            <div class="metric-item">
              <span class="metric-label">平均装配率</span>
              <span class="metric-value">{{ selectedConfigData.averageRate }}%</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">覆盖车型</span>
              <span class="metric-value">{{ selectedConfigData.coverageCount }}款</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">市场趋势</span>
              <span class="metric-value" :class="getTrendClass(selectedConfigData.trend)">
                {{ getTrendText(selectedConfigData.trend) }}
              </span>
            </div>
          </div>
          <div class="detail-description">
            <p>{{ selectedConfigData.description }}</p>
          </div>
          <div class="brand-ranking">
            <h5>品牌装配率排行</h5>
            <div class="ranking-list">
              <div
                v-for="(item, index) in selectedConfigData.brandRanking"
                :key="item.brand"
                class="ranking-item"
              >
                <span class="rank">{{ index + 1 }}</span>
                <span class="brand">{{ item.brand }}</span>
                <div class="rate-bar">
                  <div class="rate-fill" :style="{ width: item.rate + '%' }"></div>
                </div>
                <span class="rate-text">{{ item.rate }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 地域特征分析 -->
    <el-card shadow="never" class="regional-card" v-show="analysisDimension === 'regional'">
      <template #header>
        <div class="card-header">
          <el-icon><Location /></el-icon>
          <span>地域配置偏好分析</span>
          <div class="header-controls">
            <el-select v-model="selectedRegionLevel" size="small" style="width: 120px">
              <el-option label="省级分析" value="province" />
              <el-option label="城市级别" value="cityLevel" />
              <el-option label="经济区域" value="economic" />
            </el-select>
          </div>
        </div>
      </template>

      <div class="regional-content">
        <el-row :gutter="20">
          <el-col :span="16">
            <!-- 地域热力图 -->
            <div class="regional-heatmap">
              <div ref="regionalChart" class="regional-chart" v-loading="analyzing"></div>
            </div>
          </el-col>
          <el-col :span="8">
            <!-- 地域配置偏好排行 -->
            <div class="regional-ranking">
              <h4>{{ getRegionLevelText() }}配置偏好TOP10</h4>
              <div class="preference-list">
                <div
                  v-for="(config, index) in regionalPreferences"
                  :key="config.id"
                  class="preference-item"
                  @click="selectRegionalConfig(config)"
                >
                  <div class="preference-rank">{{ index + 1 }}</div>
                  <div class="preference-info">
                    <h5>{{ config.name }}</h5>
                    <div class="preference-stats">
                      <span class="preference-rate">偏好度: {{ config.preferenceScore }}%</span>
                      <span class="preference-growth" :class="getTrendClass(config.growth)">
                        {{ config.growth >= 0 ? '+' : '' }}{{ config.growth }}%
                      </span>
                    </div>
                  </div>
                  <div class="preference-icon">
                    <el-icon><ArrowRight /></el-icon>
                  </div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 选中配置的地域分布 -->
        <div class="regional-distribution" v-if="selectedRegionalConfig">
          <h4>{{ selectedRegionalConfig.name }} - 地域分布分析</h4>
          <el-row :gutter="16">
            <el-col :span="8" v-for="region in selectedRegionalConfig.regions" :key="region.name">
              <div class="region-card">
                <div class="region-header">
                  <span class="region-name">{{ region.name }}</span>
                  <el-tag size="small" :type="getRegionLevelTag(region.level)">
                    {{ region.levelText }}
                  </el-tag>
                </div>
                <div class="region-metrics">
                  <div class="region-rate">
                    <span class="label">装配率</span>
                    <span class="value">{{ region.equipmentRate }}%</span>
                  </div>
                  <div class="region-demand">
                    <span class="label">需求指数</span>
                    <span class="value">{{ region.demandIndex }}</span>
                  </div>
                </div>
                <div class="region-insight">
                  <p>{{ region.insight }}</p>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>

    <!-- 盈利贡献分析 -->
    <el-card shadow="never" class="profit-card" v-show="analysisDimension === 'profit'">
      <template #header>
        <div class="card-header">
          <el-icon><Money /></el-icon>
          <span>配置盈利贡献分析</span>
          <div class="header-controls">
            <el-button size="small" @click="refreshProfitData" :loading="analyzing">
              重新计算
            </el-button>
            <el-button size="small" type="primary" @click="exportProfitReport">
              导出盈利报告
            </el-button>
          </div>
        </div>
      </template>

      <div class="profit-content">
        <el-row :gutter="20">
          <!-- ROI排行榜 -->
          <el-col :span="12">
            <div class="roi-ranking">
              <h4>配置ROI排行榜</h4>
              <div class="roi-list">
                <div
                  v-for="(config, index) in roiRanking"
                  :key="config.id"
                  class="roi-item"
                  :class="{ 'top-performer': index < 3 }"
                >
                  <div class="roi-rank">
                    <span class="rank-number">{{ index + 1 }}</span>
                    <el-icon v-if="index === 0" class="crown-icon"><Trophy /></el-icon>
                  </div>
                  <div class="roi-info">
                    <h5>{{ config.name }}</h5>
                    <div class="roi-metrics">
                      <span class="roi-value">ROI: {{ config.roi }}%</span>
                      <span class="profit-margin">利润率: {{ config.profitMargin }}%</span>
                    </div>
                  </div>
                  <div class="roi-chart">
                    <el-progress
                      :percentage="config.roi"
                      :show-text="false"
                      :stroke-width="8"
                      :color="getRoiColor(config.roi)"
                    />
                  </div>
                </div>
              </div>
            </div>
          </el-col>

          <!-- 盈利贡献图表 -->
          <el-col :span="12">
            <div class="profit-chart-container">
              <h4>配置盈利贡献分布</h4>
              <div ref="profitChart" class="profit-chart" v-loading="analyzing"></div>
            </div>
          </el-col>
        </el-row>

        <!-- 配置成本效益矩阵 -->
        <div class="cost-benefit-matrix">
          <h4>配置成本效益矩阵分析</h4>
          <div ref="costBenefitChart" class="matrix-chart" v-loading="analyzing"></div>
          <div class="matrix-quadrants">
            <div class="quadrant-info">
              <div class="quadrant high-value">
                <h5>高价值配置</h5>
                <p>成本合理，用户付费意愿高</p>
                <ul>
                  <li v-for="config in highValueConfigs" :key="config">{{ config }}</li>
                </ul>
              </div>
              <div class="quadrant cost-sensitive">
                <h5>成本敏感配置</h5>
                <p>成本较高，需优化供应链</p>
                <ul>
                  <li v-for="config in costSensitiveConfigs" :key="config">{{ config }}</li>
                </ul>
              </div>
              <div class="quadrant market-opportunity">
                <h5>市场机会配置</h5>
                <p>成本低，可提升装配率</p>
                <ul>
                  <li v-for="config in opportunityConfigs" :key="config">{{ config }}</li>
                </ul>
              </div>
              <div class="quadrant review-needed">
                <h5>待评估配置</h5>
                <p>成本高，用户接受度低</p>
                <ul>
                  <li v-for="config in reviewConfigs" :key="config">{{ config }}</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 竞品配置对标分析 -->
    <el-card shadow="never" class="competitor-card" v-show="analysisDimension === 'competitor'">
      <template #header>
        <div class="card-header">
          <el-icon><DataAnalysis /></el-icon>
          <span>竞品配置对标分析</span>
          <div class="header-controls">
            <el-select v-model="selectedCompetitorSet" size="small" style="width: 150px">
              <el-option label="豪华品牌对比" value="luxury" />
              <el-option label="主流品牌对比" value="mainstream" />
              <el-option label="新势力对比" value="newforce" />
              <el-option label="自定义对比" value="custom" />
            </el-select>
            <el-button size="small" @click="addCompetitor" type="primary"> 添加竞品 </el-button>
          </div>
        </div>
      </template>

      <div class="competitor-content">
        <!-- 竞品配置对比表格 -->
        <div class="competitor-comparison">
          <h4>配置标准对比</h4>
          <el-table :data="competitorComparisonData" border style="width: 100%">
            <el-table-column prop="configName" label="配置项目" width="150" fixed>
              <template #default="{ row }">
                <div class="config-cell">
                  <el-icon><component :is="getConfigIcon(row.category)" /></el-icon>
                  <span>{{ row.configName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="市场标准" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getStandardType(row.marketStandard)">
                  {{ row.marketStandard }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              v-for="competitor in selectedCompetitors"
              :key="competitor.id"
              :label="competitor.name"
              width="120"
            >
              <template #header>
                <div class="competitor-header">
                  <img :src="competitor.logo" :alt="competitor.name" class="competitor-logo" />
                  <span>{{ competitor.name }}</span>
                </div>
              </template>
              <template #default="{ row }">
                <div class="competitor-config">
                  <el-icon
                    :class="getConfigStatusClass(row.competitors[competitor.id])"
                    class="config-status"
                  >
                    <component :is="getConfigStatusIcon(row.competitors[competitor.id])" />
                  </el-icon>
                  <span class="config-level">{{
                    getConfigLevelText(row.competitors[competitor.id])
                  }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="差异化机会" width="150">
              <template #default="{ row }">
                <div class="opportunity-cell">
                  <el-tag
                    v-if="row.opportunity"
                    size="small"
                    :type="getOpportunityType(row.opportunity.level)"
                  >
                    {{ row.opportunity.text }}
                  </el-tag>
                  <span v-else class="no-opportunity">无明显机会</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 配置缺口分析 -->
        <div class="gap-analysis">
          <h4>配置缺口识别</h4>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="gap-category">
                <h5>
                  <el-icon class="critical-icon"><WarningFilled /></el-icon>
                  关键缺口配置
                </h5>
                <div class="gap-list">
                  <div v-for="gap in criticalGaps" :key="gap.id" class="gap-item critical">
                    <div class="gap-info">
                      <span class="gap-name">{{ gap.name }}</span>
                      <span class="gap-impact">影响度: {{ gap.impact }}%</span>
                    </div>
                    <div class="gap-action">
                      <el-button size="small" type="danger">立即补齐</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="gap-category">
                <h5>
                  <el-icon class="warning-icon"><Warning /></el-icon>
                  一般缺口配置
                </h5>
                <div class="gap-list">
                  <div v-for="gap in moderateGaps" :key="gap.id" class="gap-item moderate">
                    <div class="gap-info">
                      <span class="gap-name">{{ gap.name }}</span>
                      <span class="gap-impact">影响度: {{ gap.impact }}%</span>
                    </div>
                    <div class="gap-action">
                      <el-button size="small" type="warning">计划补齐</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="gap-category">
                <h5>
                  <el-icon class="info-icon"><InfoFilled /></el-icon>
                  潜在机会配置
                </h5>
                <div class="gap-list">
                  <div v-for="gap in opportunityGaps" :key="gap.id" class="gap-item opportunity">
                    <div class="gap-info">
                      <span class="gap-name">{{ gap.name }}</span>
                      <span class="gap-potential">潜力值: {{ gap.potential }}%</span>
                    </div>
                    <div class="gap-action">
                      <el-button size="small" type="info">关注发展</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 配置趋势预测 -->
        <div class="trend-prediction">
          <h4>配置发展趋势预测（未来6-12个月）</h4>
          <div ref="trendChart" class="trend-chart" v-loading="analyzing"></div>
          <div class="trend-insights">
            <div class="insight-cards">
              <div
                v-for="insight in trendInsights"
                :key="insight.id"
                class="insight-card"
                :class="insight.type"
              >
                <div class="insight-header">
                  <el-icon><component :is="insight.icon" /></el-icon>
                  <h5>{{ insight.title }}</h5>
                </div>
                <div class="insight-content">
                  <p>{{ insight.description }}</p>
                  <div class="insight-metrics">
                    <span class="metric">预期增长: {{ insight.expectedGrowth }}%</span>
                    <span class="metric">影响等级: {{ insight.impactLevel }}</span>
                  </div>
                </div>
                <div class="insight-action">
                  <el-button size="small" :type="insight.actionType">{{
                    insight.actionText
                  }}</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 空状态提示 -->
    <el-empty v-if="!heatmapData.length && !loading" description="暂无配置数据">
      <el-button type="primary" @click="loadSampleData">加载示例数据</el-button>
    </el-empty>

    <!-- 添加竞品弹窗 -->
    <el-dialog v-model="showAddCompetitorDialog" title="添加竞品对比" width="50%">
      <div class="add-competitor-content">
        <el-form :model="competitorForm" label-width="100px">
          <el-form-item label="品牌名称:">
            <el-select v-model="competitorForm.brandId" placeholder="选择品牌" style="width: 100%">
              <el-option
                v-for="brand in availableBrands"
                :key="brand.id"
                :label="brand.name"
                :value="brand.id"
              >
                <div class="brand-option">
                  <img :src="brand.logo" :alt="brand.name" class="brand-logo" />
                  <span>{{ brand.name }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="对比车型:">
            <el-select
              v-model="competitorForm.modelIds"
              placeholder="选择车型"
              multiple
              style="width: 100%"
            >
              <el-option
                v-for="model in availableModels"
                :key="model.id"
                :label="`${model.name} (${model.priceRange})`"
                :value="model.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddCompetitorDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmAddCompetitor">确定添加</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 整体布局 */
.vehicle-configuration {
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
  opacity: 0.9;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.header-actions .el-button {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  font-weight: 500;
  backdrop-filter: blur(4px);
}

.header-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

/* 筛选控制区卡片 */
.filter-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.filter-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 20px 24px;
}

.filter-left {
  flex: 1;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: center;
}

.filter-form .el-form-item {
  margin-bottom: 0;
}

.filter-form .el-form-item__label {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.filter-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 卡片头部样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.card-header el-icon {
  margin-right: 8px;
  color: #4facfe;
}

.header-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 配置热度矩阵分析 */
.heatmap-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.heatmap-content {
  padding: 24px;
  display: flex;
  gap: 24px;
}

.heatmap-container {
  flex: 1;
  min-height: 400px;
}

.heatmap-chart {
  width: 100%;
  height: 400px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

/* 配置详情面板 */
.config-details-panel {
  width: 300px;
  background: #f8fafb;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e8eaed;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.detail-header h4 {
  margin: 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.detail-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-label {
  font-size: 14px;
  color: #606266;
}

.metric-value {
  font-weight: 600;
  color: #1a1a1a;
}

.metric-value.positive {
  color: #67c23a;
}

.metric-value.negative {
  color: #f56c6c;
}

.metric-value.neutral {
  color: #909399;
}

.detail-description {
  margin-bottom: 16px;
}

.detail-description p {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.brand-ranking h5 {
  margin: 0 0 12px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e8eaed;
}

.rank {
  width: 20px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #4facfe;
}

.brand {
  font-size: 12px;
  color: #1a1a1a;
  min-width: 60px;
}

.rate-bar {
  flex: 1;
  height: 6px;
  background: #f0f2f5;
  border-radius: 3px;
  overflow: hidden;
}

.rate-fill {
  height: 100%;
  background: linear-gradient(135deg, #4facfe 0%, #00d4ff 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.rate-text {
  font-size: 11px;
  color: #606266;
  min-width: 30px;
  text-align: right;
}

/* 地域特征分析 */
.regional-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.regional-content {
  padding: 24px;
}

.regional-heatmap {
  background: white;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e8eaed;
}

.regional-chart {
  width: 100%;
  height: 400px;
  border-radius: 8px;
}

.regional-ranking h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.preference-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.preference-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  cursor: pointer;
  transition: all 0.3s ease;
}

.preference-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.preference-rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe 0%, #00d4ff 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}

.preference-info {
  flex: 1;
}

.preference-info h5 {
  margin: 0 0 4px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.preference-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.preference-rate {
  color: #4facfe;
  font-weight: 500;
}

.preference-growth {
  font-weight: 500;
}

.preference-growth.positive {
  color: #67c23a;
}

.preference-growth.negative {
  color: #f56c6c;
}

.preference-growth.neutral {
  color: #909399;
}

.preference-icon {
  color: #c0c4cc;
}

/* 地域分布分析 */
.regional-distribution {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.regional-distribution h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.region-card {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  height: 100%;
}

.region-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.region-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.region-metrics {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.region-rate,
.region-demand {
  text-align: center;
}

.region-rate .label,
.region-demand .label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.region-rate .value,
.region-demand .value {
  font-size: 16px;
  font-weight: 700;
  color: #4facfe;
}

.region-insight p {
  margin: 0;
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
}

/* 盈利贡献分析 */
.profit-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.profit-content {
  padding: 24px;
}

.roi-ranking h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.roi-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.roi-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.roi-item.top-performer {
  background: linear-gradient(135deg, #fff9e6 0%, #fef0e6 100%);
  border-color: #ffd700;
}

.roi-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.roi-rank {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe 0%, #00d4ff 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
}

.crown-icon {
  position: absolute;
  top: -8px;
  right: -8px;
  color: #ffd700;
  font-size: 20px;
}

.roi-info {
  flex: 1;
}

.roi-info h5 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.roi-metrics {
  display: flex;
  gap: 16px;
  font-size: 12px;
}

.roi-value {
  color: #67c23a;
  font-weight: 600;
}

.profit-margin {
  color: #4facfe;
  font-weight: 600;
}

.roi-chart {
  width: 120px;
}

.profit-chart-container h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
}

.profit-chart {
  width: 100%;
  height: 300px;
  border-radius: 8px;
  background: white;
  border: 1px solid #e8eaed;
}

/* 成本效益矩阵 */
.cost-benefit-matrix {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.cost-benefit-matrix h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.matrix-chart {
  width: 100%;
  height: 300px;
  border-radius: 8px;
  background: white;
  border: 1px solid #e8eaed;
  margin-bottom: 20px;
}

.matrix-quadrants {
  background: #f8fafb;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e8eaed;
}

.quadrant-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.quadrant {
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.quadrant.high-value {
  border-left: 4px solid #67c23a;
}

.quadrant.cost-sensitive {
  border-left: 4px solid #e6a23c;
}

.quadrant.market-opportunity {
  border-left: 4px solid #4facfe;
}

.quadrant.review-needed {
  border-left: 4px solid #f56c6c;
}

.quadrant h5 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.quadrant p {
  margin: 0 0 12px 0;
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
}

.quadrant ul {
  margin: 0;
  padding-left: 16px;
}

.quadrant li {
  margin-bottom: 4px;
  font-size: 12px;
  color: #606266;
}

/* 竞品配置对标分析 */
.competitor-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.competitor-content {
  padding: 24px;
}

.competitor-comparison h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.config-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.competitor-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.competitor-logo {
  width: 24px;
  height: 24px;
  border-radius: 50%;
}

.competitor-config {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.config-status {
  font-size: 16px;
}

.config-status.status-standard {
  color: #67c23a;
}

.config-status.status-optional {
  color: #e6a23c;
}

.config-status.status-premium {
  color: #4facfe;
}

.config-status.status-none {
  color: #c0c4cc;
}

.config-level {
  font-size: 11px;
  color: #606266;
}

.opportunity-cell {
  text-align: center;
}

.no-opportunity {
  font-size: 12px;
  color: #c0c4cc;
}

/* 配置缺口分析 */
.gap-analysis {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.gap-analysis h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.gap-category h5 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.critical-icon {
  color: #f56c6c;
}

.warning-icon {
  color: #e6a23c;
}

.info-icon {
  color: #4facfe;
}

.gap-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gap-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.gap-item.critical {
  border-left: 4px solid #f56c6c;
  background: linear-gradient(135deg, #fff5f5 0%, #fef0f0 100%);
}

.gap-item.moderate {
  border-left: 4px solid #e6a23c;
  background: linear-gradient(135deg, #fdfcf0 0%, #fdf9e6 100%);
}

.gap-item.opportunity {
  border-left: 4px solid #4facfe;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
}

.gap-info {
  flex: 1;
}

.gap-name {
  display: block;
  font-weight: 500;
  color: #1a1a1a;
  font-size: 13px;
  margin-bottom: 4px;
}

.gap-impact,
.gap-potential {
  font-size: 11px;
  color: #606266;
}

.gap-action {
  margin-left: 12px;
}

/* 配置趋势预测 */
.trend-prediction {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.trend-prediction h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.trend-chart {
  width: 100%;
  height: 300px;
  border-radius: 8px;
  background: white;
  border: 1px solid #e8eaed;
  margin-bottom: 20px;
}

.trend-insights {
  background: #f8fafb;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e8eaed;
}

.insight-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.insight-card {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.insight-card.positive {
  border-left: 4px solid #67c23a;
}

.insight-card.warning {
  border-left: 4px solid #e6a23c;
}

.insight-card.negative {
  border-left: 4px solid #f56c6c;
}

.insight-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.insight-header el-icon {
  color: #4facfe;
}

.insight-header h5 {
  margin: 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.insight-content p {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
}

.insight-metrics {
  display: flex;
  gap: 12px;
  font-size: 11px;
  margin-bottom: 12px;
}

.metric {
  color: #909399;
}

.insight-action {
  text-align: right;
}

/* 添加竞品弹窗 */
.add-competitor-content {
  padding: 16px 0;
}

.brand-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-logo {
  width: 20px;
  height: 20px;
  border-radius: 50%;
}

.dialog-footer {
  text-align: right;
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

.el-table {
  border-radius: 8px;
  overflow: hidden;
}

.el-table th {
  background: #f8fafb;
  color: #1a1a1a;
  font-weight: 600;
}

.el-radio-group .el-radio-button__inner {
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.el-empty {
  padding: 60px 0;
}

.el-empty__description {
  margin-top: 16px;
  color: #909399;
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

.heatmap-card,
.regional-card,
.profit-card,
.competitor-card {
  animation: slideInUp 0.6s ease-out;
}

.preference-item,
.roi-item,
.gap-item,
.insight-card {
  animation: slideInUp 0.4s ease-out;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .header-left h2 {
    font-size: 28px;
  }

  .heatmap-content {
    flex-direction: column;
  }

  .config-details-panel {
    width: 100%;
  }

  .quadrant-info {
    grid-template-columns: 1fr;
  }

  .insight-cards {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1200px) {
  .vehicle-configuration {
    padding: 0 12px;
  }

  .header-content {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .header-actions {
    justify-content: center;
  }

  .filter-content {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-form {
    justify-content: center;
  }

  .filter-right {
    justify-content: center;
  }

  .regional-content .el-row {
    flex-direction: column;
  }

  .profit-content .el-row {
    flex-direction: column;
  }

  .gap-analysis .el-row {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .vehicle-configuration {
    padding: 0 8px;
  }

  .header-left h2 {
    font-size: 24px;
  }

  .header-left p {
    font-size: 14px;
  }

  .filter-form {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .filter-form .el-form-item {
    margin-bottom: 12px;
  }

  .heatmap-chart,
  .regional-chart,
  .profit-chart,
  .matrix-chart,
  .trend-chart {
    height: 250px;
  }

  .preference-item {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .roi-item {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 12px;
  }

  .roi-chart {
    width: 100%;
  }

  .gap-item {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .gap-action {
    margin-left: 0;
    text-align: center;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .vehicle-configuration {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .filter-card,
  .heatmap-card,
  .regional-card,
  .profit-card,
  .competitor-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .config-details-panel,
  .regional-heatmap,
  .preference-item,
  .region-card,
  .roi-item,
  .quadrant,
  .gap-item,
  .insight-card {
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
.preference-item:focus-visible,
.roi-item:focus-visible,
.gap-item:focus-visible {
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

/* 打印样式 */
@media print {
  .vehicle-configuration {
    background: white !important;
  }

  .header-actions,
  .filter-right,
  .header-controls,
  .gap-action,
  .insight-action {
    display: none !important;
  }

  .heatmap-card,
  .regional-card,
  .profit-card,
  .competitor-card {
    break-inside: avoid;
    margin-bottom: 12px;
    box-shadow: none !important;
    border: 1px solid #ccc !important;
  }

  .page-header {
    background: white !important;
    color: black !important;
  }
}
</style>

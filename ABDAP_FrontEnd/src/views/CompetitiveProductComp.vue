<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  DataBoard,
  TrendCharts,
  DataLine,
  Money,
  ChatLineRound,
  Check,
  Plus,
  Close,
  WarningFilled,
  Warning,
  InfoFilled,
  Trophy,
  Setting
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

// 接口定义
interface SearchForm {
  brand: string
  priceRange: string
  category: string
}

interface Competitor {
  id: number
  brand: string
  name: string
  image: string
  priceRange: string
  monthlySales: number
  rating: number
  avgMonthlySales?: number
  yoyGrowth?: number
  marketShare?: number
  salesRank?: number
  seasonalData?: SeasonalData[]
  seasonalInsight?: string
  positiveKeywords?: Keyword[]
  negativeKeywords?: Keyword[]
  overallRating?: number
  reviewCount?: number
  recommendationRate?: number
  repurchaseRate?: number
  dimensionScores?: DimensionScore[]
  sampleReviews?: Review[]
}

interface SeasonalData {
  season: string
  index: number
  percentage: number
}

interface Keyword {
  word: string
  count: number
}

interface DimensionScore {
  name: string
  score: number
  insight: string
}

interface Review {
  id: number
  username: string
  verified: string
  verifiedType: string
  rating: number
  content: string
  tags: string[]
}

interface AnalysisConfig {
  period: string
  region: string
}

interface DimensionRankingItem {
  competitorId: number
  brand: string
  name: string
  image: string
  score: number
  advantage: string
}

interface MetricItem {
  name: string
  unit: string
  values: MetricValue[]
}

interface MetricValue {
  competitorId: number
  value: string
  isBest: boolean
}

interface RegionalInsight {
  region: string
  leader: string
  growthTrend: number
  keyInsight: string
}

interface PriceCompetitivenessItem {
  id: number
  brand: string
  name: string
  image: string
  startingPrice: number
  valueIndex: number
  premiumRate: number
  discountSpace: string
}

interface ConfigComparisonRow {
  configItem: string
  category: string
  importance: number
  competitors: Record<number, ConfigStatus>
}

interface ConfigStatus {
  level: string
  available: boolean
}

interface PriceStrategy {
  competitorId: number
  brand: string
  name: string
  image: string
  strategyType: {
    type: string
    text: string
  }
  recentChange: number
  insight: string
}

interface SatisfactionRankingItem {
  competitorId: number
  brand: string
  name: string
  image: string
  score: number
  change: number
}

// 响应式数据
const loading = ref(false)
const searching = ref(false)
const analyzing = ref(false)

// 搜索表单
const searchForm = ref<SearchForm>({
  brand: '',
  priceRange: '',
  category: ''
})

// 竞品数据
const availableCompetitors = ref<Competitor[]>([])
const selectedCompetitors = ref<Competitor[]>([])

// 分析配置
const analysisConfig = ref<AnalysisConfig>({
  period: '6m',
  region: 'national'
})

// 对比结果状态
const comparisonResults = ref(false)

// 雷达图对比数据
const radarDimension = ref('performance')
const dimensionRanking = ref<DimensionRankingItem[]>([])
const currentDimensionMetrics = ref<MetricItem[]>([])

// 销量趋势数据
const trendAnalysisType = ref('monthly')
const showSeasonal = ref(false)
const regionalInsights = ref<RegionalInsight[]>([])

// 价格价值分析数据
const priceAnalysisType = ref('valueScatter')
const priceCompetitiveness = ref<PriceCompetitivenessItem[]>([])
const configComparisonData = ref<ConfigComparisonRow[]>([])
const priceStrategies = ref<PriceStrategy[]>([])

// 用户口碑分析数据
const sentimentDimension = ref('exterior')
const satisfactionRanking = ref<SatisfactionRankingItem[]>([])
const showDetailedReviews = ref(false)
const activeReviewTab = ref('1')

// 图表实例
let radarChartInstance: echarts.ECharts | null = null
let salesTrendChartInstance: echarts.ECharts | null = null
let regionalChartInstance: echarts.ECharts | null = null
let priceValueChartInstance: echarts.ECharts | null = null
let priceHistoryChartInstance: echarts.ECharts | null = null
let sentimentRadarChartInstance: echarts.ECharts | null = null
let sentimentTrendChartInstance: echarts.ECharts | null = null

// 计算属性
const getDimensionTitle = () => {
  const titleMap = {
    'performance': '性能维度',
    'economy': '经济性维度',
    'space': '空间维度'
  }
  return titleMap[radarDimension.value] || '性能维度'
}

const getSentimentDimensionTitle = () => {
  const titleMap = {
    'exterior': '外观设计',
    'interior': '内饰质感',
    'performance': '动力操控',
    'comfort': '舒适配置',
    'technology': '智能科技'
  }
  return titleMap[sentimentDimension.value] || '外观设计'
}

// 工具函数
const isSelected = (competitorId: number): boolean => {
  return selectedCompetitors.value.some(c => c.id === competitorId)
}

const getTrendClass = (value: number) => {
  if (value > 0) return 'positive'
  if (value < 0) return 'negative'
  return 'neutral'
}

const getSeasonalClass = (index: number) => {
  if (index >= 1.2) return 'high'
  if (index >= 0.8) return 'normal'
  return 'low'
}

const getSeasonalColor = (index: number) => {
  if (index >= 1.2) return '#67c23a'
  if (index >= 0.8) return '#4facfe'
  return '#f56c6c'
}

const getValueIndexColor = (value: number) => {
  if (value >= 80) return '#67c23a'
  if (value >= 60) return '#4facfe'
  if (value >= 40) return '#e6a23c'
  return '#f56c6c'
}

const getDiscountSpaceClass = (space: string) => {
  if (space === '大') return 'high'
  if (space === '中') return 'medium'
  return 'low'
}

const getDiscountSpaceText = (space: string) => {
  const textMap = {
    '大': '降价空间大',
    '中': '降价空间中等',
    '小': '降价空间有限'
  }
  return textMap[space] || space
}

const getConfigIcon = (category: string) => {
  const iconMap = {
    'safety': WarningFilled,
    'comfort': Check,
    'technology': TrendCharts,
    'performance': Trophy
  }
  return iconMap[category] || Check
}

const getConfigStatusClass = (status: ConfigStatus) => {
  if (!status || !status.available) return 'status-none'
  if (status.level === 'standard') return 'status-standard'
  if (status.level === 'optional') return 'status-optional'
  if (status.level === 'premium') return 'status-premium'
  return 'status-none'
}

const getConfigStatusIcon = (status: ConfigStatus) => {
  if (!status || !status.available) return Close
  if (status.level === 'standard') return Check
  if (status.level === 'optional') return Warning
  if (status.level === 'premium') return Trophy
  return Close
}

const getConfigStatusText = (status: ConfigStatus) => {
  if (!status || !status.available) return '无'
  const textMap = {
    'standard': '标配',
    'optional': '选配',
    'premium': '高配'
  }
  return textMap[status.level] || '无'
}

const getDimensionColor = (score: number) => {
  if (score >= 90) return '#67c23a'
  if (score >= 75) return '#4facfe'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

// 事件处理函数
const searchCompetitors = async () => {
  searching.value = true
  
  try {
    await fetchAvailableCompetitors()
    ElMessage.success('搜索完成')
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

const toggleCompetitor = (competitor: Competitor) => {
  if (isSelected(competitor.id)) {
    removeCompetitor(competitor.id)
  } else {
    if (selectedCompetitors.value.length >= 5) {
      ElMessage.warning('最多只能选择5款竞品进行对比')
      return
    }
    selectedCompetitors.value.push(competitor)
    ElMessage.success(`${competitor.brand} ${competitor.name} 已加入对比`)
  }
}

const removeCompetitor = (competitorId: number) => {
  const index = selectedCompetitors.value.findIndex(c => c.id === competitorId)
  if (index > -1) {
    const competitor = selectedCompetitors.value[index]
    selectedCompetitors.value.splice(index, 1)
    ElMessage.success(`${competitor.brand} ${competitor.name} 已移出对比`)
  }
}

const startAnalysis = async () => {
  if (selectedCompetitors.value.length < 2) {
    ElMessage.warning('至少需要选择2款竞品才能开始分析')
    return
  }
  
  analyzing.value = true
  
  try {
    // 并发获取所有分析数据
    await Promise.all([
      fetchRadarComparisonData(),
      fetchSalesTrendData(),
      fetchPriceValueData(),
      fetchSentimentData()
    ])
    
    comparisonResults.value = true
    
    // 初始化图表
    await nextTick()
    await Promise.all([
      initRadarChart(),
      initSalesTrendChart(),
      initRegionalChart(),
      initPriceValueChart(),
      initPriceHistoryChart(),
      initSentimentRadarChart(),
      initSentimentTrendChart()
    ])
    
    ElMessage.success('对比分析完成')
  } catch (error) {
    ElMessage.error('分析失败，请稍后重试')
  } finally {
    analyzing.value = false
  }
}

const updateRadarChart = async () => {
  analyzing.value = true
  
  try {
    await fetchRadarComparisonData()
    await initRadarChart()
  } catch (error) {
    ElMessage.error('更新雷达图失败')
  } finally {
    analyzing.value = false
  }
}

const updateTrendChart = async () => {
  analyzing.value = true
  
  try {
    await fetchSalesTrendData()
    await initSalesTrendChart()
  } catch (error) {
    ElMessage.error('更新趋势图失败')
  } finally {
    analyzing.value = false
  }
}

const updatePriceChart = async () => {
  analyzing.value = true
  
  try {
    await fetchPriceValueData()
    await initPriceValueChart()
  } catch (error) {
    ElMessage.error('更新价格图表失败')
  } finally {
    analyzing.value = false
  }
}

const updateSentimentChart = async () => {
  analyzing.value = true
  
  try {
    await fetchSentimentData()
    await initSentimentRadarChart()
  } catch (error) {
    ElMessage.error('更新口碑图表失败')
  } finally {
    analyzing.value = false
  }
}

const toggleSeasonalAnalysis = () => {
  showSeasonal.value = !showSeasonal.value
  ElMessage.info(showSeasonal.value ? '已显示季节性分析' : '已隐藏季节性分析')
}

const loadSampleCompetitors = async () => {
  loading.value = true
  
  try {
    selectedCompetitors.value = generateMockSelectedCompetitors()
    availableCompetitors.value = generateMockAvailableCompetitors()
    
    ElMessage.success('示例竞品已加载')
  } catch (error) {
    ElMessage.error('加载示例竞品失败')
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  loading.value = true
  
  try {
    await Promise.all([
      fetchAvailableCompetitors(),
      comparisonResults.value ? fetchAllComparisonData() : Promise.resolve()
    ])
    
    ElMessage.success('数据已刷新')
  } catch (error) {
    ElMessage.error('数据刷新失败')
  } finally {
    loading.value = false
  }
}

const exportReport = () => {
  if (selectedCompetitors.value.length === 0) {
    ElMessage.warning('暂无竞品对比数据可导出')
    return
  }
  
  const content = [
    '竞品对比分析报告',
    `生成时间: ${new Date().toLocaleString()}`,
    `分析周期: ${analysisConfig.value.period}`,
    `地域范围: ${analysisConfig.value.region}`,
    '',
    '对比竞品:',
    ...selectedCompetitors.value.map((c, index) => 
      `${index + 1}. ${c.brand} ${c.name} - ${c.priceRange}`
    ),
    '',
    '分析摘要:',
    generateAnalysisSummary(),
    '',
    '详细数据:',
    generateDetailedAnalysis()
  ].join('\n')
  
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `竞品对比分析报告_${new Date().toISOString().slice(0, 10)}.txt`
  link.click()
  
  ElMessage.success('分析报告已导出')
}

// API调用函数
const fetchAvailableCompetitors = async () => {
  try {
    const response = await axios.get('/api/competitors/search', {
      params: searchForm.value
    })
    
    if (response.data.status === 1) {
      availableCompetitors.value = response.data.data
    }
  } catch (error) {
    console.error('获取竞品列表失败:', error)
    availableCompetitors.value = generateMockAvailableCompetitors()
  }
}

const fetchRadarComparisonData = async () => {
  try {
    const response = await axios.get('/api/competitors/radar', {
      params: {
        competitors: selectedCompetitors.value.map(c => c.id).join(','),
        dimension: radarDimension.value,
        ...analysisConfig.value
      }
    })
    
    if (response.data.status === 1) {
      dimensionRanking.value = response.data.data.ranking
      currentDimensionMetrics.value = response.data.data.metrics
    }
  } catch (error) {
    console.error('获取雷达对比数据失败:', error)
    dimensionRanking.value = generateMockDimensionRanking()
    currentDimensionMetrics.value = generateMockDimensionMetrics()
  }
}

const fetchSalesTrendData = async () => {
  try {
    const response = await axios.get('/api/competitors/sales-trend', {
      params: {
        competitors: selectedCompetitors.value.map(c => c.id).join(','),
        type: trendAnalysisType.value,
        ...analysisConfig.value
      }
    })
    
    if (response.data.status === 1) {
      regionalInsights.value = response.data.data.regionalInsights
      // 更新竞品的销量数据
      selectedCompetitors.value.forEach(competitor => {
        const salesData = response.data.data.salesData.find(s => s.id === competitor.id)
        if (salesData) {
          competitor.avgMonthlySales = salesData.avgMonthlySales
          competitor.yoyGrowth = salesData.yoyGrowth
          competitor.marketShare = salesData.marketShare
          competitor.salesRank = salesData.salesRank
          competitor.seasonalData = salesData.seasonalData
          competitor.seasonalInsight = salesData.seasonalInsight
        }
      })
    }
  } catch (error) {
    console.error('获取销量趋势数据失败:', error)
    regionalInsights.value = generateMockRegionalInsights()
    generateMockSalesData()
  }
}

const fetchPriceValueData = async () => {
  try {
    const response = await axios.get('/api/competitors/price-value', {
      params: {
        competitors: selectedCompetitors.value.map(c => c.id).join(','),
        type: priceAnalysisType.value,
        ...analysisConfig.value
      }
    })
    
    if (response.data.status === 1) {
      priceCompetitiveness.value = response.data.data.competitiveness
      configComparisonData.value = response.data.data.configComparison
      priceStrategies.value = response.data.data.strategies
    }
  } catch (error) {
    console.error('获取价格价值数据失败:', error)
    priceCompetitiveness.value = generateMockPriceCompetitiveness()
    configComparisonData.value = generateMockConfigComparison()
    priceStrategies.value = generateMockPriceStrategies()
  }
}

const fetchSentimentData = async () => {
  try {
    const response = await axios.get('/api/competitors/sentiment', {
      params: {
        competitors: selectedCompetitors.value.map(c => c.id).join(','),
        dimension: sentimentDimension.value,
        ...analysisConfig.value
      }
    })
    
    if (response.data.status === 1) {
      satisfactionRanking.value = response.data.data.ranking
      // 更新竞品的口碑数据
      selectedCompetitors.value.forEach(competitor => {
        const sentimentData = response.data.data.sentimentData.find(s => s.id === competitor.id)
        if (sentimentData) {
          competitor.positiveKeywords = sentimentData.positiveKeywords
          competitor.negativeKeywords = sentimentData.negativeKeywords
          competitor.overallRating = sentimentData.overallRating
          competitor.reviewCount = sentimentData.reviewCount
          competitor.recommendationRate = sentimentData.recommendationRate
          competitor.repurchaseRate = sentimentData.repurchaseRate
          competitor.dimensionScores = sentimentData.dimensionScores
          competitor.sampleReviews = sentimentData.sampleReviews
        }
      })
    }
  } catch (error) {
    console.error('获取口碑数据失败:', error)
    satisfactionRanking.value = generateMockSatisfactionRanking()
    generateMockSentimentData()
  }
}

const fetchAllComparisonData = async () => {
  await Promise.all([
    fetchRadarComparisonData(),
    fetchSalesTrendData(),
    fetchPriceValueData(),
    fetchSentimentData()
  ])
}

// 模拟数据生成函数
const generateMockAvailableCompetitors = (): Competitor[] => {
  const brands = [
    { brand: '特斯拉', models: ['Model 3', 'Model Y', 'Model S'] },
    { brand: '比亚迪', models: ['汉EV', '唐EV', '秦PLUS'] },
    { brand: '理想', models: ['ONE', 'L9', 'L8'] },
    { brand: '小鹏', models: ['P7', 'G9', 'P5'] },
    { brand: '蔚来', models: ['ES6', 'ET7', 'ES8'] }
  ]
  
  const competitors: Competitor[] = []
  let id = 1
  
  brands.forEach(brandInfo => {
    brandInfo.models.forEach(model => {
      competitors.push({
        id: id++,
        brand: brandInfo.brand,
        name: model,
        image: `https://via.placeholder.com/150x100?text=${brandInfo.brand}+${model}`,
        priceRange: `${Math.floor(Math.random() * 30) + 20}-${Math.floor(Math.random() * 20) + 40}万`,
        monthlySales: Math.floor(Math.random() * 8000) + 2000,
        rating: Math.round((Math.random() * 2 + 3) * 10) / 10
      })
    })
  })
  
  return competitors
}

const generateMockSelectedCompetitors = (): Competitor[] => {
  const mockCompetitors = generateMockAvailableCompetitors()
  return mockCompetitors.slice(0, 3)
}

const generateMockDimensionRanking = (): DimensionRankingItem[] => {
  return selectedCompetitors.value.map((competitor, index) => ({
    competitorId: competitor.id,
    brand: competitor.brand,
    name: competitor.name,
    image: competitor.image,
    score: Math.floor(Math.random() * 30) + 70,
    advantage: index === 0 ? '动力强劲' : index === 1 ? '油耗经济' : '配置丰富'
  })).sort((a, b) => b.score - a.score)
}

const generateMockDimensionMetrics = (): MetricItem[] => {
  const metrics = radarDimension.value === 'performance' 
    ? [
        { name: '0-100km/h加速', unit: 's' },
        { name: '最大功率', unit: 'kW' },
        { name: '最大扭矩', unit: 'N·m' }
      ]
    : radarDimension.value === 'economy'
    ? [
        { name: '综合油耗', unit: 'L/100km' },
        { name: '续航里程', unit: 'km' },
        { name: '保养成本', unit: '元/年' }
      ]
    : [
        { name: '车内长度', unit: 'mm' },
        { name: '后排空间', unit: 'mm' },
        { name: '后备箱容积', unit: 'L' }
      ]
  
  return metrics.map(metric => ({
    name: metric.name,
    unit: metric.unit,
    values: selectedCompetitors.value.map(competitor => ({
      competitorId: competitor.id,
      value: (Math.random() * 50 + 50).toFixed(1),
      isBest: Math.random() > 0.7
    }))
  }))
}

const generateMockRegionalInsights = (): RegionalInsight[] => {
  return [
    {
      region: '华东地区',
      leader: '特斯拉 Model Y',
      growthTrend: 12.5,
      keyInsight: '新能源车接受度最高，高端品牌表现突出'
    },
    {
      region: '华南地区',
      leader: '比亚迪汉EV',
      growthTrend: 8.3,
      keyInsight: '本土品牌优势明显，性价比是关键因素'
    },
    {
      region: '华北地区',
      leader: '理想ONE',
      growthTrend: 15.2,
      keyInsight: '增程式技术受到认可，家庭用车需求强劲'
    }
  ]
}

const generateMockSalesData = () => {
  selectedCompetitors.value.forEach(competitor => {
    competitor.avgMonthlySales = Math.floor(Math.random() * 5000) + 2000
    competitor.yoyGrowth = (Math.random() - 0.3) * 40
    competitor.marketShare = Math.random() * 15 + 5
    competitor.salesRank = Math.floor(Math.random() * 10) + 1
    competitor.seasonalData = [
      { season: '春季', index: Math.random() * 0.4 + 0.8, percentage: Math.random() * 30 + 70 },
      { season: '夏季', index: Math.random() * 0.4 + 0.8, percentage: Math.random() * 30 + 70 },
      { season: '秋季', index: Math.random() * 0.4 + 0.8, percentage: Math.random() * 30 + 70 },
      { season: '冬季', index: Math.random() * 0.4 + 0.8, percentage: Math.random() * 30 + 70 }
    ]
    competitor.seasonalInsight = '该车型在春秋季销量表现较好，夏冬季有所下降'
  })
}

const generateMockPriceCompetitiveness = (): PriceCompetitivenessItem[] => {
  return selectedCompetitors.value.map(competitor => ({
    id: competitor.id,
    brand: competitor.brand,
    name: competitor.name,
    image: competitor.image,
    startingPrice: Math.floor(Math.random() * 30) + 20,
    valueIndex: Math.floor(Math.random() * 40) + 60,
    premiumRate: Math.floor(Math.random() * 30) + 10,
    discountSpace: Math.random() > 0.3 ? '中' : Math.random() > 0.5 ? '大' : '小'
  }))
}

const generateMockConfigComparison = (): ConfigComparisonRow[] => {
  const configs = [
    { item: '自动驾驶辅助', category: 'technology' },
    { item: '全景天窗', category: 'comfort' },
    { item: '座椅加热', category: 'comfort' },
    { item: '主动安全', category: 'safety' },
    { item: '无线充电', category: 'technology' }
  ]
  
  return configs.map(config => {
    const competitors: Record<number, ConfigStatus> = {}
    selectedCompetitors.value.forEach(competitor => {
      const levels = ['standard', 'optional', 'premium']
      const level = levels[Math.floor(Math.random() * levels.length)]
      competitors[competitor.id] = {
        level,
        available: Math.random() > 0.2
      }
    })
    
    return {
      configItem: config.item,
      category: config.category,
      importance: Math.floor(Math.random() * 2) + 4,
      competitors
    }
  })
}

const generateMockPriceStrategies = (): PriceStrategy[] => {
  return selectedCompetitors.value.map(competitor => ({
    competitorId: competitor.id,
    brand: competitor.brand,
    name: competitor.name,
    image: competitor.image,
    strategyType: {
      type: Math.random() > 0.5 ? 'success' : Math.random() > 0.5 ? 'warning' : 'info',
      text: Math.random() > 0.5 ? '稳定定价' : Math.random() > 0.5 ? '促销降价' : '试探性调价'
    },
    recentChange: (Math.random() - 0.5) * 6,
    insight: '基于市场竞争态势的定价策略调整'
  }))
}

const generateMockSatisfactionRanking = (): SatisfactionRankingItem[] => {
  return selectedCompetitors.value.map(competitor => ({
    competitorId: competitor.id,
    brand: competitor.brand,
    name: competitor.name,
    image: competitor.image,
    score: Math.round((Math.random() * 2 + 3) * 10) / 10,
    change: (Math.random() - 0.5) * 10
  })).sort((a, b) => b.score - a.score)
}

const generateMockSentimentData = () => {
  selectedCompetitors.value.forEach(competitor => {
    competitor.positiveKeywords = [
      { word: '颜值高', count: Math.floor(Math.random() * 500) + 100 },
      { word: '性能好', count: Math.floor(Math.random() * 400) + 80 },
      { word: '配置丰富', count: Math.floor(Math.random() * 300) + 60 }
    ]
    competitor.negativeKeywords = [
      { word: '价格高', count: Math.floor(Math.random() * 200) + 50 },
      { word: '做工粗糙', count: Math.floor(Math.random() * 150) + 30 },
      { word: '服务差', count: Math.floor(Math.random() * 100) + 20 }
    ]
    competitor.overallRating = Math.round((Math.random() * 2 + 3) * 10) / 10
    competitor.reviewCount = Math.floor(Math.random() * 5000) + 1000
    competitor.recommendationRate = Math.floor(Math.random() * 30) + 70
    competitor.repurchaseRate = Math.floor(Math.random() * 20) + 60
    competitor.dimensionScores = [
      { name: '外观设计', score: Math.floor(Math.random() * 30) + 70, insight: '设计时尚，符合年轻人审美' },
      { name: '内饰质感', score: Math.floor(Math.random() * 30) + 70, insight: '材质用料不错，做工有待提升' },
      { name: '动力操控', score: Math.floor(Math.random() * 30) + 70, insight: '动力充沛，操控精准' }
    ]
    competitor.sampleReviews = [
      {
        id: 1,
        username: '车主***',
        verified: '已认证车主',
        verifiedType: 'success',
        rating: Math.floor(Math.random() * 2) + 4,
        content: '整体感觉不错，外观很漂亮，内饰也很有科技感，就是价格稍微有点高。',
        tags: ['外观', '内饰', '性价比']
      },
      {
        id: 2,
        username: '用户***',
        verified: '潜在车主',
        verifiedType: 'warning',
        rating: Math.floor(Math.random() * 2) + 3,
        content: '试驾了一下，动力表现很好，就是后排空间稍微紧凑了一些。',
        tags: ['动力', '空间']
      }
    ]
  })
}

// 图表初始化函数
const initRadarChart = async () => {
  await nextTick()
  
  const chartElement = document.querySelector('.radar-chart') as HTMLElement
  if (!chartElement || selectedCompetitors.value.length === 0) return
  
  if (radarChartInstance) {
    radarChartInstance.dispose()
  }
  
  radarChartInstance = echarts.init(chartElement)
  
  const indicators = radarDimension.value === 'performance' 
    ? [
        { name: '动力性能', max: 100 },
        { name: '加速能力', max: 100 },
        { name: '操控性', max: 100 },
        { name: '制动性能', max: 100 },
        { name: 'NVH表现', max: 100 }
      ]
    : radarDimension.value === 'economy'
    ? [
        { name: '燃油经济性', max: 100 },
        { name: '续航里程', max: 100 },
        { name: '充电效率', max: 100 },
        { name: '保养成本', max: 100 },
        { name: '保值率', max: 100 }
      ]
    : [
        { name: '乘坐空间', max: 100 },
        { name: '储物空间', max: 100 },
        { name: '后排舒适性', max: 100 },
        { name: '装载能力', max: 100 },
        { name: '空间利用率', max: 100 }
      ]
  
  const colors = ['#4facfe', '#00d4ff', '#43e97b', '#38f9d7', '#ffd93d']
  
  const series = selectedCompetitors.value.map((competitor, index) => ({
    value: indicators.map(() => Math.floor(Math.random() * 40) + 60),
    name: `${competitor.brand} ${competitor.name}`,
    areaStyle: {
      color: colors[index % colors.length],
      opacity: 0.3
    },
    lineStyle: {
      color: colors[index % colors.length]
    }
  }))
  
  const option = {
    title: {
      text: getDimensionTitle() + '对比',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'horizontal',
      bottom: 10,
      data: selectedCompetitors.value.map(c => `${c.brand} ${c.name}`)
    },
    radar: {
      indicator,
      center: ['50%', '50%'],
      radius: '60%'
    },
    series: [{
      type: 'radar',
      data: series
    }]
  }
  
  radarChartInstance.setOption(option)
}

const initSalesTrendChart = async () => {
  await nextTick()
  
  const chartElement = document.querySelector('.sales-trend-chart') as HTMLElement
  if (!chartElement || selectedCompetitors.value.length === 0) return
  
  if (salesTrendChartInstance) {
    salesTrendChartInstance.dispose()
  }
  
  salesTrendChartInstance = echarts.init(chartElement)
  
  const months = ['1月', '2月', '3月', '4月', '5月', '6月']
  const colors = ['#4facfe', '#00d4ff', '#43e97b', '#38f9d7', '#ffd93d']
  
  const series = selectedCompetitors.value.map((competitor, index) => ({
    name: `${competitor.brand} ${competitor.name}`,
    type: 'line',
    data: months.map(() => Math.floor(Math.random() * 3000) + 2000),
    smooth: true,
    itemStyle: { color: colors[index % colors.length] },
    lineStyle: { color: colors[index % colors.length] }
  }))
  
  const option = {
    title: {
      text: '销量趋势对比',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: selectedCompetitors.value.map(c => `${c.brand} ${c.name}`),
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months
    },
    yAxis: {
      type: 'value',
      name: '销量（台）'
    },
    series
  }
  
  salesTrendChartInstance.setOption(option)
}

const initRegionalChart = async () => {
  await nextTick()
  
  const chartElement = document.querySelector('.regional-chart') as HTMLElement
  if (!chartElement) return
  
  if (regionalChartInstance) {
    regionalChartInstance.dispose()
  }
  
  regionalChartInstance = echarts.init(chartElement)
  
  const regions = ['华东', '华南', '华北', '华中', '西南', '西北', '东北']
  const colors = ['#4facfe', '#00d4ff', '#43e97b', '#38f9d7', '#ffd93d']
  
  const series = selectedCompetitors.value.map((competitor, index) => ({
    name: `${competitor.brand} ${competitor.name}`,
    type: 'bar',
    data: regions.map(() => Math.floor(Math.random() * 2000) + 1000),
    itemStyle: { color: colors[index % colors.length] }
  }))
  
  const option = {
    title: {
      text: '区域销量分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: selectedCompetitors.value.map(c => `${c.brand} ${c.name}`),
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: regions
    },
    yAxis: {
      type: 'value',
      name: '销量（台）'
    },
    series
  }
  
  regionalChartInstance.setOption(option)
}

const initPriceValueChart = async () => {
  await nextTick()
  
  const chartElement = document.querySelector('.price-value-chart') as HTMLElement
  if (!chartElement || selectedCompetitors.value.length === 0) return
  
  if (priceValueChartInstance) {
    priceValueChartInstance.dispose()
  }
  
  priceValueChartInstance = echarts.init(chartElement)
  
  if (priceAnalysisType.value === 'valueScatter') {
    const data = selectedCompetitors.value.map(competitor => {
      const price = Math.floor(Math.random() * 30) + 20
      const value = Math.floor(Math.random() * 40) + 60
      return [price, value, competitor.brand + ' ' + competitor.name]
    })
    
    const option = {
      title: {
        text: '性价比散点图',
        left: 'center'
      },
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          return `${params.data[2]}<br/>价格: ${params.data[0]}万<br/>配置价值: ${params.data[1]}`
        }
      },
      xAxis: {
        name: '起售价（万元）',
        nameLocation: 'middle',
        nameGap: 30
      },
      yAxis: {
        name: '配置价值评分',
        nameLocation: 'middle',
        nameGap: 50
      },
      series: [{
        type: 'scatter',
        symbolSize: 60,
        data,
        itemStyle: {
          color: '#4facfe'
        }
      }]
    }
    
    priceValueChartInstance.setOption(option)
  } else if (priceAnalysisType.value === 'priceDistribution') {
    const priceRanges = ['15-20万', '20-25万', '25-30万', '30-35万', '35-40万']
    const data = priceRanges.map(() => Math.floor(Math.random() * 5) + 1)
    
    const option = {
      title: {
        text: '价格区间分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: priceRanges
      },
      yAxis: {
        type: 'value',
        name: '车型数量'
      },
      series: [{
        type: 'bar',
        data,
        itemStyle: {
          color: '#4facfe'
        }
      }]
    }
    
    priceValueChartInstance.setOption(option)
  }
}

const initPriceHistoryChart = async () => {
  await nextTick()
  
  const chartElement = document.querySelector('.price-history-chart') as HTMLElement
  if (!chartElement || selectedCompetitors.value.length === 0) return
  
  if (priceHistoryChartInstance) {
    priceHistoryChartInstance.dispose()
  }
  
  priceHistoryChartInstance = echarts.init(chartElement)
  
  const months = ['1月', '2月', '3月', '4月', '5月', '6月']
  const colors = ['#4facfe', '#00d4ff', '#43e97b', '#38f9d7', '#ffd93d']
  
  const series = selectedCompetitors.value.map((competitor, index) => {
    const basePrice = 25 + Math.random() * 10
    return {
      name: `${competitor.brand} ${competitor.name}`,
      type: 'line',
      data: months.map((_, i) => (basePrice + (Math.random() - 0.5) * 2).toFixed(1)),
      smooth: true,
      itemStyle: { color: colors[index % colors.length] },
      lineStyle: { color: colors[index % colors.length] }
    }
  })
  
  const option = {
    title: {
      text: '价格历史趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: selectedCompetitors.value.map(c => `${c.brand} ${c.name}`),
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months
    },
    yAxis: {
      type: 'value',
      name: '价格（万元）'
    },
    series
  }
  
  priceHistoryChartInstance.setOption(option)
}

const initSentimentRadarChart = async () => {
  await nextTick()
  
  const chartElement = document.querySelector('.sentiment-radar-chart') as HTMLElement
  if (!chartElement || selectedCompetitors.value.length === 0) return
  
  if (sentimentRadarChartInstance) {
    sentimentRadarChartInstance.dispose()
  }
  
  sentimentRadarChartInstance = echarts.init(chartElement)
  
  const indicators = [
    { name: '外观设计', max: 5 },
    { name: '内饰质感', max: 5 },
    { name: '动力操控', max: 5 },
    { name: '舒适配置', max: 5 },
    { name: '智能科技', max: 5 }
  ]
  
  const colors = ['#4facfe', '#00d4ff', '#43e97b', '#38f9d7', '#ffd93d']
  
  const series = selectedCompetitors.value.map((competitor, index) => ({
    value: indicators.map(() => Math.round((Math.random() * 2 + 3) * 10) / 10),
    name: `${competitor.brand} ${competitor.name}`,
    areaStyle: {
      color: colors[index % colors.length],
      opacity: 0.3
    },
    lineStyle: {
      color: colors[index % colors.length]
    }
  }))
  
  const option = {
    title: {
      text: '用户口碑雷达图',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'horizontal',
      bottom: 10,
      data: selectedCompetitors.value.map(c => `${c.brand} ${c.name}`)
    },
    radar: {
      indicator,
      center: ['50%', '50%'],
      radius: '60%'
    },
    series: [{
      type: 'radar',
      data: series
    }]
  }
  
  sentimentRadarChartInstance.setOption(option)
}

const initSentimentTrendChart = async () => {
  await nextTick()
  
  const chartElement = document.querySelector('.sentiment-trend-chart') as HTMLElement
  if (!chartElement || selectedCompetitors.value.length === 0) return
  
  if (sentimentTrendChartInstance) {
    sentimentTrendChartInstance.dispose()
  }
  
  sentimentTrendChartInstance = echarts.init(chartElement)
  
  const months = ['1月', '2月', '3月', '4月', '5月', '6月']
  const colors = ['#4facfe', '#00d4ff', '#43e97b', '#38f9d7', '#ffd93d']
  
  const series = selectedCompetitors.value.map((competitor, index) => ({
    name: `${competitor.brand} ${competitor.name}`,
    type: 'line',
    data: months.map(() => Math.round((Math.random() * 2 + 3) * 10) / 10),
    smooth: true,
    itemStyle: { color: colors[index % colors.length] },
    lineStyle: { color: colors[index % colors.length] }
  }))
  
  const option = {
    title: {
      text: '口碑趋势变化',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: selectedCompetitors.value.map(c => `${c.brand} ${c.name}`),
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months
    },
    yAxis: {
      type: 'value',
      name: '评分',
      min: 1,
      max: 5
    },
    series
  }
  
  sentimentTrendChartInstance.setOption(option)
}

// 辅助函数
const generateAnalysisSummary = (): string => {
  if (selectedCompetitors.value.length === 0) return ''
  
  const topPerformer = dimensionRanking.value[0]
  const summary = [
    `本次对比分析了${selectedCompetitors.value.length}款竞品`,
    topPerformer ? `在${getDimensionTitle()}方面，${topPerformer.brand} ${topPerformer.name}表现最佳` : '',
    '各竞品在不同维度各有优势，建议根据目标用户群体进行差异化定位'
  ].filter(Boolean).join('，')
  
  return summary
}

const generateDetailedAnalysis = (): string => {
  const details = []
  
  if (dimensionRanking.value.length > 0) {
    details.push('性能排行:')
    dimensionRanking.value.forEach((item, index) => {
      details.push(`${index + 1}. ${item.brand} ${item.name} - 得分: ${item.score}`)
    })
  }
  
  if (regionalInsights.value.length > 0) {
    details.push('\n区域市场洞察:')
    regionalInsights.value.forEach(insight => {
      details.push(`${insight.region}: ${insight.keyInsight}`)
    })
  }
  
  return details.join('\n')
}

// 窗口大小调整
const handleResize = () => {
  const charts = [
    radarChartInstance,
    salesTrendChartInstance,
    regionalChartInstance,
    priceValueChartInstance,
    priceHistoryChartInstance,
    sentimentRadarChartInstance,
    sentimentTrendChartInstance
  ]
  
  charts.forEach(chart => {
    if (chart) {
      chart.resize()
    }
  })
}

// 监听器
watch([selectedCompetitors], () => {
  if (selectedCompetitors.value.length >= 2 && comparisonResults.value) {
    // 重新获取对比数据
    fetchAllComparisonData()
  }
}, { deep: true })

watch([analysisConfig], () => {
  if (comparisonResults.value) {
    fetchAllComparisonData()
  }
}, { deep: true })

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用竞品对比分析！')
  
  try {
    // 初始化数据
    await fetchAvailableCompetitors()
    
    // 检查URL参数
    const competitorsParam = route.query.competitors as string
    if (competitorsParam) {
      const competitorIds = competitorsParam.split(',').map(id => parseInt(id)).filter(id => !isNaN(id))
      
      if (competitorIds.length >= 2) {
        selectedCompetitors.value = availableCompetitors.value.filter(c => 
          competitorIds.includes(c.id)
        ).slice(0, 5)
        
        if (selectedCompetitors.value.length >= 2) {
          await startAnalysis()
        }
      }
    }
    
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
    radarChartInstance,
    salesTrendChartInstance,
    regionalChartInstance,
    priceValueChartInstance,
    priceHistoryChartInstance,
    sentimentRadarChartInstance,
    sentimentTrendChartInstance
  ]
  
  charts.forEach(chart => {
    if (chart) {
      chart.dispose()
    }
  })
})
</script>



<template>
  <div class="competitive-product-comp">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>竞品对比分析</h2>
          <p>多维度竞品对比分析，助力产品差异化定位与市场策略制定</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
          <el-button type="success" :icon="Download" @click="exportReport" :disabled="!selectedCompetitors.length">
            导出报告
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 竞品选择区 -->
    <el-card shadow="never" class="competitor-selector-card">
      <template #header>
        <div class="selector-header">
          <el-icon><DataBoard /></el-icon>
          <span>竞品选择与配置</span>
          <el-tag type="primary">最多对比5款车型</el-tag>
        </div>
      </template>

      <div class="selector-content">
        <el-row :gutter="20">
          <el-col :span="16">
            <div class="competitor-search">
              <el-form :inline="true" :model="searchForm" class="search-form">
                <el-form-item label="品牌筛选:">
                  <el-select v-model="searchForm.brand" placeholder="选择品牌" style="width: 150px" clearable>
                    <el-option label="全部品牌" value="" />
                    <el-option label="特斯拉" value="tesla" />
                    <el-option label="比亚迪" value="byd" />
                    <el-option label="理想" value="lixiang" />
                    <el-option label="小鹏" value="xpeng" />
                    <el-option label="蔚来" value="nio" />
                    <el-option label="奔驰" value="benz" />
                    <el-option label="宝马" value="bmw" />
                    <el-option label="奥迪" value="audi" />
                  </el-select>
                </el-form-item>
                <el-form-item label="价格区间:">
                  <el-select v-model="searchForm.priceRange" placeholder="价格区间" style="width: 150px" clearable>
                    <el-option label="全部价位" value="" />
                    <el-option label="10-20万" value="10-20" />
                    <el-option label="20-30万" value="20-30" />
                    <el-option label="30-50万" value="30-50" />
                    <el-option label="50万以上" value="50+" />
                  </el-select>
                </el-form-item>
                <el-form-item label="车型类别:">
                  <el-select v-model="searchForm.category" placeholder="车型类别" style="width: 120px" clearable>
                    <el-option label="全部类型" value="" />
                    <el-option label="轿车" value="sedan" />
                    <el-option label="SUV" value="suv" />
                    <el-option label="MPV" value="mpv" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button @click="searchCompetitors" :loading="searching">搜索车型</el-button>
                </el-form-item>
              </el-form>
            </div>

            <!-- 可选车型列表 -->
            <div class="available-competitors">
              <h4>可选车型</h4>
              <div class="competitor-grid">
                <div 
                  v-for="competitor in availableCompetitors" 
                  :key="competitor.id"
                  class="competitor-item"
                  :class="{ 'selected': isSelected(competitor.id) }"
                  @click="toggleCompetitor(competitor)"
                >
                  <img :src="competitor.image" :alt="competitor.name" class="competitor-image" />
                  <div class="competitor-info">
                    <h5>{{ competitor.brand }} {{ competitor.name }}</h5>
                    <p class="price-range">{{ competitor.priceRange }}</p>
                    <div class="quick-stats">
                      <span class="stat">销量: {{ competitor.monthlySales }}台</span>
                      <span class="stat">评分: {{ competitor.rating }}</span>
                    </div>
                  </div>
                  <div class="selection-indicator">
                    <el-icon v-if="isSelected(competitor.id)"><Check /></el-icon>
                    <el-icon v-else><Plus /></el-icon>
                  </div>
                </div>
              </div>
            </div>
          </el-col>

          <el-col :span="8">
            <!-- 已选竞品 -->
            <div class="selected-competitors">
              <h4>已选竞品 ({{ selectedCompetitors.length }}/5)</h4>
              <div class="selected-list">
                <div 
                  v-for="competitor in selectedCompetitors" 
                  :key="competitor.id"
                  class="selected-item"
                >
                  <img :src="competitor.image" :alt="competitor.name" class="selected-image" />
                  <div class="selected-info">
                    <h5>{{ competitor.brand }} {{ competitor.name }}</h5>
                    <p>{{ competitor.priceRange }}</p>
                  </div>
                  <el-button 
                    size="small" 
                    type="danger" 
                    :icon="Close" 
                    @click="removeCompetitor(competitor.id)"
                    circle
                  />
                </div>
              </div>

              <!-- 对比分析配置 -->
              <div class="analysis-config" v-if="selectedCompetitors.length >= 2">
                <h4>分析配置</h4>
                <el-form :model="analysisConfig" label-width="80px">
                  <el-form-item label="分析周期:">
                    <el-select v-model="analysisConfig.period" style="width: 100%">
                      <el-option label="最近3个月" value="3m" />
                      <el-option label="最近6个月" value="6m" />
                      <el-option label="最近12个月" value="12m" />
                      <el-option label="自定义" value="custom" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="地域范围:">
                    <el-select v-model="analysisConfig.region" style="width: 100%">
                      <el-option label="全国" value="national" />
                      <el-option label="一线城市" value="tier1" />
                      <el-option label="新一线城市" value="new-tier1" />
                      <el-option label="二三线城市" value="tier2-3" />
                    </el-select>
                  </el-form-item>
                </el-form>
                <el-button 
                  type="primary" 
                  size="large" 
                  @click="startAnalysis" 
                  :loading="analyzing"
                  style="width: 100%"
                >
                  开始对比分析
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 多维度雷达图对比 -->
    <el-card shadow="never" class="radar-comparison-card" v-if="comparisonResults">
      <template #header>
        <div class="card-header">
          <el-icon><TrendCharts /></el-icon>
          <span>多维度雷达图对比</span>
          <div class="header-controls">
            <el-radio-group v-model="radarDimension" @change="updateRadarChart">
              <el-radio-button label="performance">性能维度</el-radio-button>
              <el-radio-button label="economy">经济性维度</el-radio-button>
              <el-radio-button label="space">空间维度</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <div class="radar-content">
        <el-row :gutter="24">
          <el-col :span="16">
            <!-- 雷达图 -->
            <div class="radar-chart-container">
              <div ref="radarChart" class="radar-chart" v-loading="analyzing"></div>
            </div>
          </el-col>
          <el-col :span="8">
            <!-- 维度排名 -->
            <div class="dimension-ranking">
              <h4>{{ getDimensionTitle() }}排行榜</h4>
              <div class="ranking-list">
                <div 
                  v-for="(item, index) in dimensionRanking" 
                  :key="item.competitorId"
                  class="ranking-item"
                  :class="`rank-${index + 1}`"
                >
                  <div class="rank-number">{{ index + 1 }}</div>
                  <img :src="item.image" :alt="item.name" class="rank-image" />
                  <div class="rank-info">
                    <h5>{{ item.brand }} {{ item.name }}</h5>
                    <div class="rank-score">综合得分: {{ item.score }}</div>
                    <div class="rank-advantage">{{ item.advantage }}</div>
                  </div>
                </div>
              </div>

              <!-- 维度详细对比 -->
              <div class="dimension-details">
                <h5>详细指标对比</h5>
                <div class="detail-metrics">
                  <div 
                    v-for="metric in currentDimensionMetrics" 
                    :key="metric.name"
                    class="metric-item"
                  >
                    <div class="metric-name">{{ metric.name }}</div>
                    <div class="metric-comparison">
                      <div 
                        v-for="value in metric.values" 
                        :key="value.competitorId"
                        class="metric-value"
                        :class="{ 'best': value.isBest }"
                      >
                        <span class="value">{{ value.value }}</span>
                        <span class="unit">{{ metric.unit }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 销量趋势对比分析 -->
    <el-card shadow="never" class="sales-trend-card" v-if="comparisonResults">
      <template #header>
        <div class="card-header">
          <el-icon><DataLine /></el-icon>
          <span>销量趋势对比分析</span>
          <div class="header-controls">
            <el-select v-model="trendAnalysisType" @change="updateTrendChart" size="small">
              <el-option label="月度销量" value="monthly" />
              <el-option label="季度销量" value="quarterly" />
              <el-option label="市场份额" value="marketShare" />
              <el-option label="增长率" value="growthRate" />
            </el-select>
            <el-button size="small" @click="toggleSeasonalAnalysis">
              {{ showSeasonal ? '隐藏' : '显示' }}季节性分析
            </el-button>
          </div>
        </div>
      </template>

      <div class="sales-trend-content">
        <el-row :gutter="24">
          <el-col :span="18">
            <!-- 趋势图表 -->
            <div class="trend-chart-container">
              <div ref="salesTrendChart" class="sales-trend-chart" v-loading="analyzing"></div>
            </div>
          </el-col>
          <el-col :span="6">
            <!-- 销量关键指标 -->
            <div class="sales-metrics">
              <h4>关键销量指标</h4>
              <div class="metrics-list">
                <div 
                  v-for="competitor in selectedCompetitors" 
                  :key="competitor.id"
                  class="competitor-metrics"
                >
                  <div class="metrics-header">
                    <img :src="competitor.image" :alt="competitor.name" class="metrics-image" />
                    <span class="metrics-name">{{ competitor.brand }} {{ competitor.name }}</span>
                  </div>
                  <div class="metrics-data">
                    <div class="metric-row">
                      <span class="label">月均销量</span>
                      <span class="value">{{ competitor.avgMonthlySales || 'N/A' }}台</span>
                    </div>
                    <div class="metric-row">
                      <span class="label">同比增长</span>
                      <span class="value" :class="getTrendClass(competitor.yoyGrowth)">
                        {{ competitor.yoyGrowth >= 0 ? '+' : '' }}{{ competitor.yoyGrowth }}%
                      </span>
                    </div>
                    <div class="metric-row">
                      <span class="label">市场份额</span>
                      <span class="value">{{ competitor.marketShare }}%</span>
                    </div>
                    <div class="metric-row">
                      <span class="label">销量排名</span>
                      <span class="value rank">第{{ competitor.salesRank }}名</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 季节性分析 -->
        <div class="seasonal-analysis" v-if="showSeasonal">
          <h4>季节性销量特征分析</h4>
          <el-row :gutter="16">
            <el-col :span="6" v-for="competitor in selectedCompetitors" :key="competitor.id">
              <div class="seasonal-card">
                <div class="seasonal-header">
                  <img :src="competitor.image" :alt="competitor.name" class="seasonal-image" />
                  <h5>{{ competitor.brand }} {{ competitor.name }}</h5>
                </div>
                <div class="seasonal-data">
                  <div class="season-item" v-for="season in competitor.seasonalData" :key="season.season">
                    <div class="season-info">
                      <span class="season-name">{{ season.season }}</span>
                      <span class="season-index" :class="getSeasonalClass(season.index)">
                        {{ season.index }}
                      </span>
                    </div>
                    <el-progress 
                      :percentage="season.percentage" 
                      :show-text="false"
                      :stroke-width="6"
                      :color="getSeasonalColor(season.index)"
                    />
                  </div>
                </div>
                <div class="seasonal-insight">
                  <p>{{ competitor.seasonalInsight }}</p>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 区域表现对比 -->
        <div class="regional-performance">
          <h4>区域销量表现对比</h4>
          <div ref="regionalChart" class="regional-chart" v-loading="analyzing"></div>
          <div class="regional-insights">
            <el-row :gutter="16">
              <el-col :span="8" v-for="region in regionalInsights" :key="region.region">
                <div class="regional-insight-card">
                  <h5>{{ region.region }}</h5>
                  <div class="insight-content">
                    <div class="market-leader">
                      <span class="label">市场领导者:</span>
                      <span class="leader">{{ region.leader }}</span>
                    </div>
                    <div class="growth-trend">
                      <span class="label">增长趋势:</span>
                      <span class="trend" :class="getTrendClass(region.growthTrend)">
                        {{ region.growthTrend >= 0 ? '上升' : '下降' }}
                      </span>
                    </div>
                    <div class="key-insight">
                      <p>{{ region.keyInsight }}</p>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 价格价值分析矩阵 -->
    <el-card shadow="never" class="price-value-card" v-if="comparisonResults">
      <template #header>
        <div class="card-header">
          <el-icon><Money /></el-icon>
          <span>价格价值分析矩阵</span>
          <div class="header-controls">
            <el-radio-group v-model="priceAnalysisType" @change="updatePriceChart">
              <el-radio-button label="valueScatter">性价比散点图</el-radio-button>
              <el-radio-button label="priceDistribution">价格区间分布</el-radio-button>
              <el-radio-button label="configComparison">配置对比</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <div class="price-value-content">
        <el-row :gutter="24">
          <el-col :span="16">
            <!-- 价格分析图表 -->
            <div class="price-chart-container">
              <div ref="priceValueChart" class="price-value-chart" v-loading="analyzing"></div>
            </div>
          </el-col>
          <el-col :span="8">
            <!-- 价格竞争力分析 -->
            <div class="price-competitiveness">
              <h4>价格竞争力分析</h4>
              <div class="competitiveness-list">
                <div 
                  v-for="competitor in priceCompetitiveness" 
                  :key="competitor.id"
                  class="competitiveness-item"
                >
                  <div class="competitor-header">
                    <img :src="competitor.image" :alt="competitor.name" class="comp-image" />
                    <div class="comp-info">
                      <h5>{{ competitor.brand }} {{ competitor.name }}</h5>
                      <p class="price">起售价: {{ competitor.startingPrice }}万</p>
                    </div>
                  </div>
                  <div class="competitiveness-metrics">
                    <div class="metric">
                      <span class="metric-label">性价比指数</span>
                      <div class="metric-bar">
                        <el-progress 
                          :percentage="competitor.valueIndex" 
                          :show-text="false"
                          :stroke-width="8"
                          :color="getValueIndexColor(competitor.valueIndex)"
                        />
                        <span class="metric-value">{{ competitor.valueIndex }}</span>
                      </div>
                    </div>
                    <div class="metric">
                      <span class="metric-label">品牌溢价率</span>
                      <div class="metric-bar">
                        <el-progress 
                          :percentage="competitor.premiumRate" 
                          :show-text="false"
                          :stroke-width="8"
                          color="#e6a23c"
                        />
                        <span class="metric-value">{{ competitor.premiumRate }}%</span>
                      </div>
                    </div>
                    <div class="metric">
                      <span class="metric-label">降价空间</span>
                      <div class="metric-evaluation" :class="getDiscountSpaceClass(competitor.discountSpace)">
                        {{ getDiscountSpaceText(competitor.discountSpace) }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 配置对比表格 -->
        <div class="config-comparison-table" v-if="priceAnalysisType === 'configComparison'">
          <h4>同价位配置对比分析</h4>
          <el-table :data="configComparisonData" border style="width: 100%">
            <el-table-column prop="configItem" label="配置项目" width="150" fixed>
              <template #default="{ row }">
                <div class="config-item">
                  <el-icon><component :is="getConfigIcon(row.category)" /></el-icon>
                  <span>{{ row.configItem }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="重要程度" width="100">
              <template #default="{ row }">
                <el-rate 
                  v-model="row.importance" 
                  disabled 
                  show-score 
                  score-template="{value}"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column 
              v-for="competitor in selectedCompetitors" 
              :key="competitor.id"
              :label="competitor.brand + ' ' + competitor.name"
              width="150"
            >
              <template #header>
                <div class="table-competitor-header">
                  <img :src="competitor.image" :alt="competitor.name" class="table-image" />
                  <div class="table-info">
                    <span>{{ competitor.brand }} {{ competitor.name }}</span>
                    <span class="table-price">{{ competitor.priceRange }}</span>
                  </div>
                </div>
              </template>
              <template #default="{ row }">
                <div class="config-status">
                  <el-icon 
                    :class="getConfigStatusClass(row.competitors[competitor.id])"
                    class="status-icon"
                  >
                    <component :is="getConfigStatusIcon(row.competitors[competitor.id])" />
                  </el-icon>
                  <span class="status-text">{{ getConfigStatusText(row.competitors[competitor.id]) }}</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 降价历史分析 -->
        <div class="price-history">
          <h4>降价历史与策略分析</h4>
          <div ref="priceHistoryChart" class="price-history-chart" v-loading="analyzing"></div>
          <div class="price-strategy-insights">
            <el-row :gutter="16">
              <el-col :span="8" v-for="strategy in priceStrategies" :key="strategy.competitorId">
                <div class="strategy-card">
                  <div class="strategy-header">
                    <img :src="strategy.image" :alt="strategy.name" class="strategy-image" />
                    <h5>{{ strategy.brand }} {{ strategy.name }}</h5>
                  </div>
                  <div class="strategy-content">
                    <div class="strategy-type">
                      <span class="label">定价策略:</span>
                      <el-tag :type="strategy.strategyType.type" size="small">
                        {{ strategy.strategyType.text }}
                      </el-tag>
                    </div>
                    <div class="price-changes">
                      <span class="label">近期调价:</span>
                      <span class="changes" :class="getTrendClass(strategy.recentChange)">
                        {{ strategy.recentChange >= 0 ? '+' : '' }}{{ strategy.recentChange }}万
                      </span>
                    </div>
                    <div class="strategy-insight">
                      <p>{{ strategy.insight }}</p>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 用户口碑结构化分析 -->
    <el-card shadow="never" class="user-sentiment-card" v-if="comparisonResults">
      <template #header>
        <div class="card-header">
          <el-icon><ChatLineRound /></el-icon>
          <span>用户口碑结构化分析</span>
          <div class="header-controls">
            <el-select v-model="sentimentDimension" @change="updateSentimentChart" size="small">
              <el-option label="外观设计" value="exterior" />
              <el-option label="内饰质感" value="interior" />
              <el-option label="动力操控" value="performance" />
              <el-option label="舒适配置" value="comfort" />
              <el-option label="智能科技" value="technology" />
            </el-select>
            <el-button size="small" @click="showDetailedReviews = !showDetailedReviews">
              {{ showDetailedReviews ? '隐藏' : '查看' }}详细评价
            </el-button>
          </div>
        </div>
      </template>

      <div class="user-sentiment-content">
        <el-row :gutter="24">
          <el-col :span="16">
            <!-- 口碑对比雷达图 -->
            <div class="sentiment-radar-container">
              <div ref="sentimentRadarChart" class="sentiment-radar-chart" v-loading="analyzing"></div>
            </div>
          </el-col>
          <el-col :span="8">
            <!-- 满意度排名 -->
            <div class="satisfaction-ranking">
              <h4>{{ getSentimentDimensionTitle() }}满意度排行</h4>
              <div class="satisfaction-list">
                <div 
                  v-for="(item, index) in satisfactionRanking" 
                  :key="item.competitorId"
                  class="satisfaction-item"
                  :class="`satisfaction-rank-${index + 1}`"
                >
                  <div class="satisfaction-rank">{{ index + 1 }}</div>
                  <img :src="item.image" :alt="item.name" class="satisfaction-image" />
                  <div class="satisfaction-info">
                    <h5>{{ item.brand }} {{ item.name }}</h5>
                    <div class="satisfaction-score">
                      <el-rate 
                        v-model="item.score" 
                        disabled 
                        show-score 
                        score-template="{value}分"
                        size="small"
                      />
                    </div>
                    <div class="satisfaction-change" :class="getTrendClass(item.change)">
                      较上月{{ item.change >= 0 ? '上升' : '下降' }}{{ Math.abs(item.change) }}%
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- 口碑趋势分析 -->
        <div class="sentiment-trend-analysis">
          <h4>用户口碑趋势变化</h4>
          <div ref="sentimentTrendChart" class="sentiment-trend-chart" v-loading="analyzing"></div>
          
          <!-- 口碑关键词分析 -->
          <div class="sentiment-keywords">
            <h5>口碑关键词分析</h5>
            <el-row :gutter="16">
              <el-col :span="6" v-for="competitor in selectedCompetitors" :key="competitor.id">
                <div class="keyword-card">
                  <div class="keyword-header">
                    <img :src="competitor.image" :alt="competitor.name" class="keyword-image" />
                    <h6>{{ competitor.brand }} {{ competitor.name }}</h6>
                  </div>
                  <div class="keyword-content">
                    <div class="positive-keywords">
                      <h6>正面关键词</h6>
                      <div class="keyword-tags">
                        <el-tag 
                          v-for="keyword in competitor.positiveKeywords" 
                          :key="keyword.word"
                          size="small" 
                          type="success"
                          class="keyword-tag"
                        >
                          {{ keyword.word }} ({{ keyword.count }})
                        </el-tag>
                      </div>
                    </div>
                    <div class="negative-keywords">
                      <h6>负面关键词</h6>
                      <div class="keyword-tags">
                        <el-tag 
                          v-for="keyword in competitor.negativeKeywords" 
                          :key="keyword.word"
                          size="small" 
                          type="danger"
                          class="keyword-tag"
                        >
                          {{ keyword.word }} ({{ keyword.count }})
                        </el-tag>
                      </div>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>

        <!-- 详细用户评价 -->
        <div class="detailed-reviews" v-if="showDetailedReviews">
          <h4>用户评价详细分析</h4>
          <el-tabs v-model="activeReviewTab" type="card">
            <el-tab-pane 
              v-for="competitor in selectedCompetitors" 
              :key="competitor.id"
              :label="competitor.brand + ' ' + competitor.name"
              :name="competitor.id.toString()"
            >
              <div class="review-analysis">
                <div class="review-summary">
                  <div class="summary-metrics">
                    <div class="metric-card">
                      <div class="metric-title">总体评分</div>
                      <div class="metric-value">{{ competitor.overallRating }}</div>
                      <div class="metric-sub">基于{{ competitor.reviewCount }}条评价</div>
                    </div>
                    <div class="metric-card">
                      <div class="metric-title">推荐率</div>
                      <div class="metric-value">{{ competitor.recommendationRate }}%</div>
                      <div class="metric-sub">用户推荐意愿</div>
                    </div>
                    <div class="metric-card">
                      <div class="metric-title">复购率</div>
                      <div class="metric-value">{{ competitor.repurchaseRate }}%</div>
                      <div class="metric-sub">品牌忠诚度</div>
                    </div>
                  </div>
                </div>

                <div class="review-dimensions">
                  <h5>各维度评价分布</h5>
                  <div class="dimension-scores">
                    <div 
                      v-for="dimension in competitor.dimensionScores" 
                      :key="dimension.name"
                      class="dimension-item"
                    >
                      <div class="dimension-name">{{ dimension.name }}</div>
                      <div class="dimension-progress">
                        <el-progress 
                          :percentage="dimension.score" 
                          :show-text="false"
                          :stroke-width="10"
                          :color="getDimensionColor(dimension.score)"
                        />
                        <span class="dimension-score">{{ dimension.score }}</span>
                      </div>
                      <div class="dimension-insight">{{ dimension.insight }}</div>
                    </div>
                  </div>
                </div>

                <div class="sample-reviews">
                  <h5>典型用户评价</h5>
                  <div class="review-samples">
                    <div 
                      v-for="review in competitor.sampleReviews" 
                      :key="review.id"
                      class="review-sample"
                    >
                      <div class="review-header">
                        <div class="reviewer-info">
                          <span class="reviewer-name">{{ review.username }}</span>
                          <el-tag size="small" :type="review.verifiedType">{{ review.verified }}</el-tag>
                        </div>
                        <div class="review-rating">
                          <el-rate v-model="review.rating" disabled size="small" />
                        </div>
                      </div>
                      <div class="review-content">{{ review.content }}</div>
                      <div class="review-tags">
                        <el-tag 
                          v-for="tag in review.tags" 
                          :key="tag"
                          size="small" 
                          class="review-tag"
                        >
                          {{ tag }}
                        </el-tag>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-card>

    <!-- 空状态提示 -->
    <el-empty v-if="selectedCompetitors.length === 0 && !loading" description="请选择至少2款竞品开始对比分析">
      <el-button type="primary" @click="loadSampleCompetitors">加载示例竞品</el-button>
    </el-empty>

    <!-- 对比分析结果为空 -->
    <el-empty v-else-if="selectedCompetitors.length >= 2 && !comparisonResults && !analyzing" description="暂无对比分析结果">
      <el-button type="primary" @click="startAnalysis">开始分析</el-button>
    </el-empty>
  </div>
</template>


<style scoped>
/* 整体布局 */
.competitive-product-comp {
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

/* 竞品选择区卡片 */
.competitor-selector-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #4facfe;
  font-size: 18px;
}

.selector-header .el-tag {
  margin-left: auto;
}

.selector-content {
  padding: 24px;
}

/* 竞品搜索表单 */
.competitor-search {
  margin-bottom: 24px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.search-form .el-form-item {
  margin-bottom: 0;
}

.search-form .el-form-item__label {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

/* 可选车型列表 */
.available-competitors h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.competitor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.competitor-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  cursor: pointer;
  transition: all 0.3s ease;
}

.competitor-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  border-color: #4facfe;
}

.competitor-item.selected {
  border-color: #4facfe;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
}

.competitor-image {
  width: 60px;
  height: 40px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.competitor-info {
  flex: 1;
}

.competitor-info h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.price-range {
  margin: 0 0 8px 0;
  font-size: 12px;
  color: #606266;
}

.quick-stats {
  display: flex;
  gap: 12px;
}

.stat {
  font-size: 11px;
  color: #909399;
}

.selection-indicator {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #4facfe;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.competitor-item:not(.selected) .selection-indicator {
  background: #f0f2f5;
  color: #909399;
}

/* 已选竞品区域 */
.selected-competitors h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.selected-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.selected-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.selected-image {
  width: 50px;
  height: 30px;
  object-fit: cover;
  border-radius: 6px;
}

.selected-info {
  flex: 1;
}

.selected-info h5 {
  margin: 0 0 4px 0;
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.selected-info p {
  margin: 0;
  font-size: 11px;
  color: #606266;
}

/* 分析配置区域 */
.analysis-config {
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.analysis-config h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.analysis-config .el-form-item {
  margin-bottom: 16px;
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

.card-header .el-icon {
  margin-right: 8px;
  color: #4facfe;
}

.header-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 多维度雷达图对比 */
.radar-comparison-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.radar-content {
  padding: 24px;
}

.radar-chart-container {
  background: white;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e8eaed;
}

.radar-chart {
  width: 100%;
  height: 400px;
  border-radius: 8px;
}

/* 维度排名区域 */
.dimension-ranking h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.ranking-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.ranking-item.rank-1 {
  border-left: 4px solid #ffd700;
  background: linear-gradient(135deg, #fff9e6 0%, #fef0e6 100%);
}

.ranking-item.rank-2 {
  border-left: 4px solid #c0c0c0;
}

.ranking-item.rank-3 {
  border-left: 4px solid #cd7f32;
}

.rank-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe 0%, #00d4ff 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
}

.rank-image {
  width: 50px;
  height: 30px;
  object-fit: cover;
  border-radius: 6px;
}

.rank-info {
  flex: 1;
}

.rank-info h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.rank-score {
  font-size: 12px;
  color: #4facfe;
  font-weight: 500;
}

.rank-advantage {
  font-size: 11px;
  color: #67c23a;
  font-weight: 500;
}

/* 维度详细对比 */
.dimension-details h5 {
  margin: 0 0 12px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.detail-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-item {
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.metric-name {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.metric-comparison {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.metric-value {
  padding: 4px 8px;
  background: #f0f2f5;
  border-radius: 6px;
  font-size: 11px;
}

.metric-value.best {
  background: #67c23a;
  color: white;
  font-weight: 600;
}

.value {
  font-weight: 600;
}

.unit {
  color: #909399;
  margin-left: 2px;
}

/* 销量趋势对比分析 */
.sales-trend-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.sales-trend-content {
  padding: 24px;
}

.trend-chart-container {
  background: white;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e8eaed;
}

.sales-trend-chart {
  width: 100%;
  height: 400px;
  border-radius: 8px;
}

/* 销量关键指标 */
.sales-metrics h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.metrics-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.competitor-metrics {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.metrics-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.metrics-image {
  width: 40px;
  height: 24px;
  object-fit: cover;
  border-radius: 4px;
}

.metrics-name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.metrics-data {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-row .label {
  font-size: 12px;
  color: #606266;
}

.metric-row .value {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.metric-row .value.positive {
  color: #67c23a;
}

.metric-row .value.negative {
  color: #f56c6c;
}

.metric-row .value.neutral {
  color: #909399;
}

.metric-row .value.rank {
  color: #4facfe;
}

/* 季节性分析 */
.seasonal-analysis {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.seasonal-analysis h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.seasonal-card {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  height: 100%;
}

.seasonal-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.seasonal-image {
  width: 40px;
  height: 24px;
  object-fit: cover;
  border-radius: 4px;
}

.seasonal-header h5 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.seasonal-data {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.season-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.season-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.season-name {
  font-size: 12px;
  font-weight: 500;
  color: #1a1a1a;
}

.season-index {
  font-size: 11px;
  font-weight: 600;
}

.season-index.high {
  color: #67c23a;
}

.season-index.normal {
  color: #4facfe;
}

.season-index.low {
  color: #f56c6c;
}

.seasonal-insight p {
  margin: 0;
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
}

/* 区域表现对比 */
.regional-performance {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.regional-performance h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.regional-chart {
  width: 100%;
  height: 300px;
  border-radius: 8px;
  background: white;
  border: 1px solid #e8eaed;
  margin-bottom: 20px;
}

.regional-insights {
  margin-top: 20px;
}

.regional-insight-card {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  height: 100%;
}

.regional-insight-card h5 {
  margin: 0 0 12px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.insight-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.market-leader,
.growth-trend {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.market-leader .label,
.growth-trend .label {
  font-size: 12px;
  color: #606266;
}

.leader {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
}

.trend {
  font-size: 12px;
  font-weight: 600;
}

.trend.positive {
  color: #67c23a;
}

.trend.negative {
  color: #f56c6c;
}

.key-insight p {
  margin: 0;
  font-size: 11px;
  color: #606266;
  line-height: 1.4;
}

/* 价格价值分析矩阵 */
.price-value-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.price-value-content {
  padding: 24px;
}

.price-chart-container {
  background: white;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e8eaed;
}

.price-value-chart {
  width: 100%;
  height: 400px;
  border-radius: 8px;
}

/* 价格竞争力分析 */
.price-competitiveness h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.competitiveness-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.competitiveness-item {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.competitiveness-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.competitor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.comp-image {
  width: 50px;
  height: 30px;
  object-fit: cover;
  border-radius: 6px;
}

.comp-info h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.comp-info .price {
  margin: 0;
  font-size: 12px;
  color: #606266;
}

.competitiveness-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-label {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}

.metric-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.metric-bar .el-progress {
  flex: 1;
}

.metric-value {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 40px;
  text-align: right;
}

.metric-evaluation {
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  text-align: center;
}

.metric-evaluation.high {
  background: #67c23a;
  color: white;
}

.metric-evaluation.medium {
  background: #e6a23c;
  color: white;
}

.metric-evaluation.low {
  background: #f56c6c;
  color: white;
}

/* 配置对比表格 */
.config-comparison-table {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.config-comparison-table h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.config-item .el-icon {
  color: #4facfe;
}

.table-competitor-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.table-image {
  width: 40px;
  height: 24px;
  object-fit: cover;
  border-radius: 4px;
}

.table-info {
  text-align: center;
}

.table-info span {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
}

.table-price {
  font-size: 10px !important;
  color: #606266 !important;
  font-weight: 400 !important;
}

.config-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.status-icon {
  font-size: 16px;
}

.status-icon.status-standard {
  color: #67c23a;
}

.status-icon.status-optional {
  color: #e6a23c;
}

.status-icon.status-premium {
  color: #4facfe;
}

.status-icon.status-none {
  color: #c0c4cc;
}

.status-text {
  font-size: 12px;
  font-weight: 500;
}

/* 降价历史分析 */
.price-history {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.price-history h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.price-history-chart {
  width: 100%;
  height: 300px;
  border-radius: 8px;
  background: white;
  border: 1px solid #e8eaed;
  margin-bottom: 20px;
}

.price-strategy-insights {
  margin-top: 20px;
}

.strategy-card {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  height: 100%;
}

.strategy-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.strategy-image {
  width: 40px;
  height: 24px;
  object-fit: cover;
  border-radius: 4px;
}

.strategy-header h5 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.strategy-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.strategy-type,
.price-changes {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.strategy-type .label,
.price-changes .label {
  font-size: 12px;
  color: #606266;
}

.changes {
  font-size: 12px;
  font-weight: 600;
}

.changes.positive {
  color: #f56c6c;
}

.changes.negative {
  color: #67c23a;
}

.strategy-insight p {
  margin: 0;
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
}

/* 用户口碑结构化分析 */
.user-sentiment-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.user-sentiment-content {
  padding: 24px;
}

.sentiment-radar-container {
  background: white;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e8eaed;
}

.sentiment-radar-chart {
  width: 100%;
  height: 400px;
  border-radius: 8px;
}

/* 满意度排名 */
.satisfaction-ranking h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.satisfaction-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.satisfaction-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.satisfaction-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.satisfaction-item.satisfaction-rank-1 {
  border-left: 4px solid #ffd700;
  background: linear-gradient(135deg, #fff9e6 0%, #fef0e6 100%);
}

.satisfaction-rank {
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

.satisfaction-image {
  width: 40px;
  height: 24px;
  object-fit: cover;
  border-radius: 4px;
}

.satisfaction-info {
  flex: 1;
}

.satisfaction-info h5 {
  margin: 0 0 4px 0;
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.satisfaction-score {
  margin-bottom: 4px;
}

.satisfaction-change {
  font-size: 11px;
  font-weight: 500;
}

.satisfaction-change.positive {
  color: #67c23a;
}

.satisfaction-change.negative {
  color: #f56c6c;
}

/* 口碑趋势分析 */
.sentiment-trend-analysis {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.sentiment-trend-analysis h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.sentiment-trend-chart {
  width: 100%;
  height: 300px;
  border-radius: 8px;
  background: white;
  border: 1px solid #e8eaed;
  margin-bottom: 20px;
}

/* 口碑关键词分析 */
.sentiment-keywords {
  margin-top: 20px;
}

.sentiment-keywords h5 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.keyword-card {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  height: 100%;
}

.keyword-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.keyword-image {
  width: 30px;
  height: 18px;
  object-fit: cover;
  border-radius: 3px;
}

.keyword-header h6 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.keyword-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.positive-keywords h6,
.negative-keywords h6 {
  margin: 0 0 8px 0;
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
}

.keyword-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.keyword-tag {
  font-size: 10px !important;
}

/* 详细用户评价 */
.detailed-reviews {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.detailed-reviews h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.review-analysis {
  padding: 8px 0;
}

/* 评价摘要 */
.review-summary {
  margin-bottom: 24px;
}

.summary-metrics {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.metric-card {
  flex: 1;
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  text-align: center;
}

.metric-title {
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #4facfe;
  margin-bottom: 4px;
}

.metric-sub {
  font-size: 10px;
  color: #909399;
}

/* 评价维度 */
.review-dimensions {
  margin-bottom: 24px;
}

.review-dimensions h5 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.dimension-scores {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dimension-item {
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.dimension-name {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.dimension-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.dimension-progress .el-progress {
  flex: 1;
}

.dimension-score {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 30px;
}

.dimension-insight {
  font-size: 11px;
  color: #606266;
  line-height: 1.4;
}

/* 典型评价 */
.sample-reviews h5 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.review-samples {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-sample {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reviewer-name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}

.review-content {
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.review-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.review-tag {
  font-size: 10px !important;
}

/* 空状态样式 */
.el-empty {
  padding: 60px 0;
}

.el-empty__description {
  margin-top: 16px;
  color: #909399;
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

.el-tabs__nav-wrap::after {
  display: none;
}

.el-tabs__item {
  border-radius: 8px 8px 0 0;
  font-weight: 500;
}

/* 加载状态优化 */
.el-loading-mask {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

.el-loading-spinner .el-loading-text {
  color: #4facfe;
  font-weight: 500;
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

.radar-comparison-card,
.sales-trend-card,
.price-value-card,
.user-sentiment-card {
  animation: slideInUp 0.6s ease-out;
}

.ranking-item,
.competitor-metrics,
.competitiveness-item,
.satisfaction-item {
  animation: slideInUp 0.4s ease-out;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .header-left h2 {
    font-size: 28px;
  }

  .competitor-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  }

  .summary-metrics {
    flex-direction: column;
    gap: 12px;
  }

  .radar-chart,
  .sales-trend-chart,
  .price-value-chart,
  .sentiment-radar-chart {
    height: 350px;
  }
}

@media (max-width: 1200px) {
  .competitive-product-comp {
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

  .selector-content .el-row {
    flex-direction: column;
  }

  .radar-content .el-row,
  .sales-trend-content .el-row,
  .price-value-content .el-row,
  .user-sentiment-content .el-row {
    flex-direction: column;
  }

  .competitor-grid {
    grid-template-columns: 1fr;
  }

  .search-form {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
}

@media (max-width: 768px) {
  .competitive-product-comp {
    padding: 0 8px;
  }

  .header-left h2 {
    font-size: 24px;
  }

  .header-left p {
    font-size: 14px;
  }

  .radar-chart,
  .sales-trend-chart,
  .price-value-chart,
  .sentiment-radar-chart,
  .regional-chart,
  .price-history-chart,
  .sentiment-trend-chart {
    height: 250px;
  }

  .ranking-item {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 8px;
  }

  .competitor-metrics {
    text-align: center;
  }

  .metrics-header {
    justify-content: center;
  }

  .seasonal-card .seasonal-header {
    justify-content: center;
  }

  .competitiveness-item .competitor-header {
    justify-content: center;
    text-align: center;
  }

  .strategy-card .strategy-header {
    justify-content: center;
  }

  .keyword-card .keyword-header {
    justify-content: center;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .competitive-product-comp {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .competitor-selector-card,
  .radar-comparison-card,
  .sales-trend-card,
  .price-value-card,
  .user-sentiment-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .competitor-item,
  .selected-item,
  .ranking-item,
  .competitor-metrics,
  .seasonal-card,
  .regional-insight-card,
  .competitiveness-item,
  .strategy-card,
  .satisfaction-item,
  .keyword-card,
  .metric-card,
  .review-sample {
    background: #363636;
    border-color: #505050;
  }

  .radar-chart-container,
  .trend-chart-container,
  .price-chart-container,
  .sentiment-radar-container,
  .analysis-config {
    background: #2d2d2d;
    border-color: #404040;
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
.competitor-item:focus-visible,
.ranking-item:focus-visible,
.competitiveness-item:focus-visible,
.satisfaction-item:focus-visible {
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
  .competitive-product-comp {
    background: white !important;
  }

  .header-actions,
  .header-controls,
  .analysis-config .el-button {
    display: none !important;
  }

  .radar-comparison-card,
  .sales-trend-card,
  .price-value-card,
  .user-sentiment-card,
  .competitor-selector-card {
    break-inside: avoid;
    margin-bottom: 12px;
    box-shadow: none !important;
    border: 1px solid #ccc !important;
  }

  .page-header {
    background: white !important;
    color: black !important;
  }

  .radar-chart,
  .sales-trend-chart,
  .price-value-chart,
  .sentiment-radar-chart {
    height: 250px;
  }
}
</style>
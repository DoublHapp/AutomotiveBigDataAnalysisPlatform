<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  Search,
  TrendCharts,
  User,
  DataAnalysis,
  CircleCheckFilled,
  WarningFilled,
  Warning,
  InfoFilled,
  QuestionFilled,
  DataBoard,
  MagicStick,
  PieChart,
  Location,
  Star,
  Check,
  Close
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

// 接口定义
interface VehicleOption {
  id: number
  brand: string
  name: string
  reviewCount: number
}

interface EvaluationData {
  totalReviews: number
  verifiedCount: number
  credibilityScore: number
  overallRating: number
  ratingDistribution: Record<string, number>
  dimensionRatings: DimensionRating[]
  sentimentAnalysis: SentimentAnalysis
  sentimentTrend: number
}

interface DimensionRating {
  name: string
  score: number
  summary: string
}

interface SentimentAnalysis {
  positive: number
  neutral: number
  negative: number
}

interface ReviewItem {
  id: number
  username: string
  avatar: string
  rating: number
  content: string
  date: string
  likes: number
  tags: string[]
  highlights?: string[]
}

interface ScenarioInsight {
  id: number
  title: string
  description: string
  rating: number
  sampleSize: number
}

interface AspectResult {
  name: string
  label: string
  score: number
  keywords: KeywordItem[]
}

interface KeywordItem {
  word: string
  sentiment: 'positive' | 'negative' | 'neutral'
}

interface ProsConsItem {
  id: number
  title: string
  description: string
  examples: string[]
  mentionCount?: number
  satisfactionRate?: number
  score?: number
  complaintCount?: number
  severity?: 'critical' | 'moderate' | 'minor'
  impactLevel?: string
  suggestions?: string
}

interface IssueItem {
  id: number
  title: string
  impact: string
}

interface ComparisonTableRow {
  dimension: string
  [key: string]: any
}

interface ComparisonTableHeader {
  id: number
  name: string
}

interface CompetitiveAdvantage {
  dimension: string
  status: 'lead' | 'follow' | 'lag'
  statusText: string
  gapAnalysis: string
}

interface PurchaseRecommendation {
  type: 'recommend' | 'caution' | 'wait'
  icon: any
  title: string
  description: string
  score: number
  reasons: string[]
}

interface RiskAlert {
  id: number
  level: 'high' | 'medium' | 'low'
  levelText: string
  icon: any
  title: string
  description: string
  impact: number
  suggestion?: string
}

interface TimingAdvice {
  status: 'good' | 'fair' | 'poor'
  icon: any
  title: string
  description: string
  factors: TimingFactor[]
  score: number
}

interface TimingFactor {
  name: string
  impact: 'positive' | 'negative' | 'neutral'
  description: string
}

interface KeywordCategory {
  name: string
  words: WordCloudItem[]
}

interface WordCloudItem {
  text: string
  count: number
  sentiment: 'positive' | 'negative' | 'neutral'
}

interface TrendInsight {
  id: number
  type: 'positive' | 'negative' | 'neutral'
  icon: any
  title: string
  description: string
}

interface HotTopic {
  id: number
  title: string
  discussionCount: number
  sentiment: 'positive' | 'negative'
  summary: string
  examples: string[]
}

interface UserProfile {
  ageRange: string
  usageScenarios: string[]
  drivingExperience: string
  budgetRange: string
}

// 响应式数据
const loading = ref(false)
const searching = ref(false)
const analyzing = ref(false)

// 车型选择相关
const selectedVehicle = ref<number | null>(null)
const comparisonVehicles = ref<number[]>([])
const analysisScope = ref('comprehensive')
const vehicleOptions = ref<VehicleOption[]>([])
const comparisonOptions = ref<VehicleOption[]>([])

// 核心数据
const evaluationData = ref<EvaluationData | null>(null)

// 个性化匹配相关
const userProfileTags = ref<string[]>(['25-35岁', '家庭用户', '经济型', '实用性优先'])
const similarUserReviews = ref<ReviewItem[]>([])
const activeFocusTab = ref('scenario')
const selectedScenario = ref('commute')
const selectedAspects = ref<string[]>(['exterior', 'interior'])
const scenarioInsights = ref<ScenarioInsight[]>([])
const aspectResults = ref<AspectResult[]>([])

// 优缺点分析数据
const prosData = ref<ProsConsItem[]>([])
const consData = ref<ProsConsItem[]>([])
const criticalIssues = ref<IssueItem[]>([])
const moderateIssues = ref<IssueItem[]>([])
const minorIssues = ref<IssueItem[]>([])

// 对比分析数据
const comparisonTableData = ref<ComparisonTableRow[]>([])
const comparisonTableHeaders = ref<ComparisonTableHeader[]>([])
const competitiveAdvantages = ref<CompetitiveAdvantage[]>([])

// AI决策支持数据
const purchaseRecommendation = ref<PurchaseRecommendation>({
  type: 'recommend',
  icon: Check,
  title: '推荐购买',
  description: '基于综合评价分析，该车型表现优秀，值得购买',
  score: 4.2,
  reasons: ['整体口碑良好', '性价比突出', '质量可靠性高', '用户满意度高']
})

const riskAlerts = ref<RiskAlert[]>([])

const timingAdvice = ref<TimingAdvice>({
  status: 'good',
  icon: Check,
  title: '购买时机良好',
  description: '当前是较好的购买时机，建议尽快决策',
  factors: [],
  score: 85
})

// 深度洞察数据
const insightView = ref('keywords')
const keywordCategories = ref<KeywordCategory[]>([])
const trendInsights = ref<TrendInsight[]>([])
const hotTopics = ref<HotTopic[]>([])

// 用户画像相关
const showProfileDialog = ref(false)
const userProfile = ref<UserProfile>({
  ageRange: '26-35',
  usageScenarios: ['commute', 'family'],
  drivingExperience: 'experienced',
  budgetRange: '10-20'
})

// 图表实例
let sentimentChartInstance: echarts.ECharts | null = null
let comparisonRadarChartInstance: echarts.ECharts | null = null
let wordCloudChartInstance: echarts.ECharts | null = null
let trendChartInstance: echarts.ECharts | null = null

// 计算属性
const getVehicleName = (vehicleId: number) => {
  const vehicle = vehicleOptions.value.find(v => v.id === vehicleId)
  return vehicle ? `${vehicle.brand} ${vehicle.name}` : '未知车型'
}

// 工具函数
const getDimensionColor = (score: number) => {
  if (score >= 4.5) return '#67c23a'
  if (score >= 4.0) return '#4facfe'
  if (score >= 3.5) return '#e6a23c'
  return '#f56c6c'
}

const getScoreClass = (score: number) => {
  if (score >= 4.5) return 'excellent'
  if (score >= 4.0) return 'good'
  if (score >= 3.5) return 'fair'
  return 'poor'
}

const getSeverityText = (severity: string) => {
  const textMap = {
    'critical': '严重',
    'moderate': '一般',
    'minor': '轻微'
  }
  return textMap[severity] || '未知'
}

const getRankClass = (rank: number) => {
  if (rank === 1) return 'first'
  if (rank === 2) return 'second'
  if (rank === 3) return 'third'
  return 'normal'
}

const getRiskTagType = (level: string) => {
  const typeMap = {
    'high': 'danger',
    'medium': 'warning',
    'low': 'info'
  }
  return typeMap[level] || 'info'
}

const getRiskColor = (level: string) => {
  const colorMap = {
    'high': '#f56c6c',
    'medium': '#e6a23c',
    'low': '#67c23a'
  }
  return colorMap[level] || '#67c23a'
}

const getTimingScoreClass = (score: number) => {
  if (score >= 80) return 'excellent'
  if (score >= 60) return 'good'
  if (score >= 40) return 'fair'
  return 'poor'
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

// 事件处理函数
const handleVehicleChange = async (vehicleId: number) => {
  loading.value = true
  try {
    await fetchEvaluationData(vehicleId)
    await fetchSimilarUserReviews(vehicleId)
    await fetchProsConsData(vehicleId)
    await fetchComparisonData()
    ElMessage.success('车型数据加载成功')
  } catch (error) {
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

const handleComparisonChange = async () => {
  if (comparisonVehicles.value.length > 0) {
    await fetchComparisonData()
  }
}

const handleScopeChange = async () => {
  if (selectedVehicle.value) {
    await fetchEvaluationData(selectedVehicle.value)
  }
}

const handleFocusChange = (tabName: string) => {
  activeFocusTab.value = tabName
  if (tabName === 'scenario') {
    filterByScenario()
  } else if (tabName === 'aspect') {
    filterByAspects()
  }
}

const searchVehicles = async (query: string) => {
  if (!query) return
  
  searching.value = true
  try {
    const response = await axios.get('/api/vehicles/search', {
      params: { query, limit: 10 }
    })
    
    if (response.data.status === 1) {
      vehicleOptions.value = response.data.data
    }
  } catch (error) {
    console.error('搜索车型失败:', error)
    vehicleOptions.value = generateMockVehicleOptions(query)
  } finally {
    searching.value = false
  }
}

const filterByScenario = async () => {
  // 根据使用场景筛选评价
  try {
    const response = await axios.get('/api/reviews/scenario', {
      params: {
        vehicleId: selectedVehicle.value,
        scenario: selectedScenario.value
      }
    })
    
    if (response.data.status === 1) {
      scenarioInsights.value = response.data.data
    }
  } catch (error) {
    console.error('场景筛选失败:', error)
    scenarioInsights.value = generateMockScenarioInsights()
  }
}

const filterByAspects = async () => {
  // 根据特定方面筛选评价
  try {
    const response = await axios.get('/api/reviews/aspects', {
      params: {
        vehicleId: selectedVehicle.value,
        aspects: selectedAspects.value.join(',')
      }
    })
    
    if (response.data.status === 1) {
      aspectResults.value = response.data.data
    }
  } catch (error) {
    console.error('方面筛选失败:', error)
    aspectResults.value = generateMockAspectResults()
  }
}

const updateUserProfile = () => {
  showProfileDialog.value = true
}

const saveUserProfile = async () => {
  try {
    // 保存用户画像
    localStorage.setItem('userProfile', JSON.stringify(userProfile.value))
    
    // 更新个性化标签
    userProfileTags.value = generateUserProfileTags()
    
    // 重新获取相似用户评价
    if (selectedVehicle.value) {
      await fetchSimilarUserReviews(selectedVehicle.value)
    }
    
    showProfileDialog.value = false
    ElMessage.success('用户画像已更新')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const regenerateAnalysis = async () => {
  if (!selectedVehicle.value) return
  
  analyzing.value = true
  try {
    await fetchProsConsData(selectedVehicle.value)
    ElMessage.success('分析已重新生成')
  } catch (error) {
    ElMessage.error('重新分析失败')
  } finally {
    analyzing.value = false
  }
}

const exportProsConsReport = () => {
  if (!evaluationData.value) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  
  const content = [
    '口碑优缺点分析报告',
    `车型: ${getVehicleName(selectedVehicle.value!)}`,
    `生成时间: ${new Date().toLocaleString()}`,
    '',
    '主要优势:',
    ...prosData.value.map((item, index) => `${index + 1}. ${item.title}: ${item.description}`),
    '',
    '主要劣势:',
    ...consData.value.map((item, index) => `${index + 1}. ${item.title}: ${item.description}`),
    '',
    '购买建议:',
    purchaseRecommendation.value.description
  ].join('\n')
  
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${getVehicleName(selectedVehicle.value!)}_口碑分析报告.txt`
  link.click()
  
  ElMessage.success('报告已导出')
}

const setInsightView = (view: string) => {
  insightView.value = view
  
  nextTick(() => {
    if (view === 'keywords' && !wordCloudChartInstance) {
      initWordCloudChart()
    } else if (view === 'trends' && !trendChartInstance) {
      initTrendChart()
    }
  })
}

const likeReview = (reviewId: number) => {
  const review = similarUserReviews.value.find(r => r.id === reviewId)
  if (review) {
    review.likes++
    ElMessage.success('感谢您的反馈')
  }
}

const viewFullReview = (reviewId: number) => {
  ElMessage.info('查看详细评价功能开发中...')
}

const showVehicleSelector = () => {
  ElMessage.info('请在上方选择要分析的车型')
}

const refreshData = async () => {
  if (!selectedVehicle.value) {
    ElMessage.warning('请先选择车型')
    return
  }
  
  loading.value = true
  try {
    await Promise.all([
      fetchEvaluationData(selectedVehicle.value),
      fetchSimilarUserReviews(selectedVehicle.value),
      fetchProsConsData(selectedVehicle.value),
      fetchComparisonData()
    ])
    ElMessage.success('数据已刷新')
  } catch (error) {
    ElMessage.error('数据刷新失败')
  } finally {
    loading.value = false
  }
}

const exportReport = () => {
  if (!selectedVehicle.value) {
    ElMessage.warning('请先选择车型')
    return
  }
  
  const content = [
    '口碑聚合分析报告',
    `车型: ${getVehicleName(selectedVehicle.value)}`,
    `综合评分: ${evaluationData.value?.overallRating.toFixed(1)}`,
    `总评价数: ${evaluationData.value?.totalReviews}`,
    `可信度: ${evaluationData.value?.credibilityScore}%`,
    `生成时间: ${new Date().toLocaleString()}`,
    '',
    '分析摘要:',
    purchaseRecommendation.value.description,
    '',
    '主要优势:',
    ...prosData.value.slice(0, 3).map((item, index) => `${index + 1}. ${item.title}`),
    '',
    '主要劣势:',
    ...consData.value.slice(0, 3).map((item, index) => `${index + 1}. ${item.title}`)
  ].join('\n')
  
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${getVehicleName(selectedVehicle.value)}_口碑分析报告.txt`
  link.click()
  
  ElMessage.success('分析报告已导出')
}

// API调用函数
const fetchEvaluationData = async (vehicleId: number) => {
  try {
    const response = await axios.get(`/api/evaluation/${vehicleId}`, {
      params: { scope: analysisScope.value }
    })
    
    if (response.data.status === 1) {
      evaluationData.value = response.data.data
      await nextTick()
      initSentimentChart()
    } else {
      throw new Error('API响应失败')
    }
  } catch (error) {
    console.error('获取评价数据失败:', error)
    evaluationData.value = generateMockEvaluationData()
    await nextTick()
    initSentimentChart()
  }
}

const fetchSimilarUserReviews = async (vehicleId: number) => {
  try {
    const response = await axios.get(`/api/reviews/similar`, {
      params: {
        vehicleId,
        userProfile: JSON.stringify(userProfile.value)
      }
    })
    
    if (response.data.status === 1) {
      similarUserReviews.value = response.data.data
    }
  } catch (error) {
    console.error('获取相似用户评价失败:', error)
    similarUserReviews.value = generateMockSimilarReviews()
  }
}

const fetchProsConsData = async (vehicleId: number) => {
  try {
    const response = await axios.get(`/api/analysis/pros-cons/${vehicleId}`)
    
    if (response.data.status === 1) {
      const data = response.data.data
      prosData.value = data.pros
      consData.value = data.cons
      criticalIssues.value = data.criticalIssues
      moderateIssues.value = data.moderateIssues
      minorIssues.value = data.minorIssues
      purchaseRecommendation.value = data.recommendation
      riskAlerts.value = data.risks
      timingAdvice.value = data.timing
    }
  } catch (error) {
    console.error('获取优缺点数据失败:', error)
    prosData.value = generateMockProsData()
    consData.value = generateMockConsData()
    criticalIssues.value = generateMockIssues('critical')
    moderateIssues.value = generateMockIssues('moderate')
    minorIssues.value = generateMockIssues('minor')
    riskAlerts.value = generateMockRiskAlerts()
  }
}

const fetchComparisonData = async () => {
  if (comparisonVehicles.value.length === 0) return
  
  try {
    const response = await axios.get('/api/comparison/evaluate', {
      params: {
        mainVehicle: selectedVehicle.value,
        comparisonVehicles: comparisonVehicles.value.join(',')
      }
    })
    
    if (response.data.status === 1) {
      const data = response.data.data
      comparisonTableData.value = data.tableData
      comparisonTableHeaders.value = data.headers
      competitiveAdvantages.value = data.advantages
      
      await nextTick()
      initComparisonRadarChart()
    }
  } catch (error) {
    console.error('获取对比数据失败:', error)
    comparisonTableData.value = generateMockComparisonData()
    comparisonTableHeaders.value = generateMockComparisonHeaders()
    competitiveAdvantages.value = generateMockCompetitiveAdvantages()
    
    await nextTick()
    initComparisonRadarChart()
  }
}

// 模拟数据生成函数
const generateMockVehicleOptions = (query: string): VehicleOption[] => {
  const brands = ['特斯拉', '比亚迪', '理想', '小鹏', '蔚来', '奔驰', '宝马', '奥迪', '丰田', '本田']
  const models = ['Model 3', '汉EV', 'ONE', 'P7', 'ES6', 'C级', '3系', 'A4L', '凯美瑞', '雅阁']
  
  return Array.from({ length: 8 }, (_, index) => ({
    id: index + 1,
    brand: brands[index % brands.length],
    name: models[index % models.length],
    reviewCount: Math.floor(Math.random() * 5000) + 500
  })).filter(item => 
    item.brand.includes(query) || item.name.includes(query)
  )
}

const generateMockEvaluationData = (): EvaluationData => {
  return {
    totalReviews: 3267,
    verifiedCount: 2890,
    credibilityScore: 88,
    overallRating: 4.2,
    ratingDistribution: {
      '5': 1456,
      '4': 987,
      '3': 543,
      '2': 187,
      '1': 94
    },
    dimensionRatings: [
      { name: '外观设计', score: 4.5, summary: '外观时尚大气，获得广泛好评' },
      { name: '内饰质感', score: 4.1, summary: '内饰用料不错，工艺有提升空间' },
      { name: '动力表现', score: 4.3, summary: '动力充沛，加速响应积极' },
      { name: '操控性能', score: 4.0, summary: '操控稳定，转向精准度较好' },
      { name: '油耗表现', score: 3.8, summary: '油耗控制一般，有改善空间' },
      { name: '舒适配置', score: 4.2, summary: '配置丰富，舒适性表现优秀' },
      { name: '性价比', score: 3.9, summary: '价格略高，但配置较为丰富' }
    ],
    sentimentAnalysis: {
      positive: 68,
      neutral: 22,
      negative: 10
    },
    sentimentTrend: 0.15
  }
}

const generateMockSimilarReviews = (): ReviewItem[] => {
  const usernames = ['车友小王', '家用代步', '理性消费者', '老司机张三', '新手上路']
  const contents = [
    '总体来说这款车还是很不错的，特别是外观设计很吸引人，动力也够用。',
    '作为家庭用车很合适，空间够大，油耗也能接受，性价比不错。',
    '开了半年了，整体满意，就是内饰用料稍微差了点，但这个价位能理解。',
    '动力表现超出预期，加速很线性，高速表现也很稳定。',
    '新手买的第一台车，操控很容易上手，安全配置也比较齐全。'
  ]
  
  return Array.from({ length: 5 }, (_, index) => ({
    id: index + 1,
    username: usernames[index],
    avatar: '',
    rating: Math.random() * 2 + 3, // 3-5星
    content: contents[index],
    date: new Date(Date.now() - Math.random() * 365 * 24 * 60 * 60 * 1000).toISOString(),
    likes: Math.floor(Math.random() * 50) + 10,
    tags: ['25-35岁', '家庭用户'],
    highlights: index === 0 ? ['外观设计', '动力表现'] : undefined
  }))
}

const generateMockScenarioInsights = (): ScenarioInsight[] => {
  return [
    {
      id: 1,
      title: '日常通勤体验',
      description: '起步响应快，市区行驶平顺，油耗控制较好',
      rating: 4.2,
      sampleSize: 1234
    },
    {
      id: 2,
      title: '拥堵路况表现',
      description: '低速时发动机噪音控制得当，走走停停油耗偏高',
      rating: 3.8,
      sampleSize: 876
    }
  ]
}

const generateMockAspectResults = (): AspectResult[] => {
  return [
    {
      name: 'exterior',
      label: '外观设计',
      score: 4.5,
      keywords: [
        { word: '时尚', sentiment: 'positive' },
        { word: '大气', sentiment: 'positive' },
        { word: '运动', sentiment: 'positive' }
      ]
    },
    {
      name: 'interior',
      label: '内饰质感',
      score: 4.1,
      keywords: [
        { word: '豪华', sentiment: 'positive' },
        { word: '塑料感', sentiment: 'negative' },
        { word: '做工', sentiment: 'neutral' }
      ]
    }
  ]
}

const generateMockProsData = (): ProsConsItem[] => {
  return [
    {
      id: 1,
      title: '外观设计出色',
      description: '外观时尚动感，线条流畅，整体设计感强',
      examples: ['颜值很高', '外观很运动', '设计很时尚'],
      mentionCount: 1876,
      satisfactionRate: 92,
      score: 4.6
    },
    {
      id: 2,
      title: '动力表现优秀',
      description: '动力充沛，加速响应迅速，高速行驶稳定',
      examples: ['动力很强', '加速很快', '高速很稳'],
      mentionCount: 1543,
      satisfactionRate: 89,
      score: 4.4
    },
    {
      id: 3,
      title: '配置丰富实用',
      description: '科技配置齐全，安全配置到位，实用性高',
      examples: ['配置很全', '科技感强', '很实用'],
      mentionCount: 1234,
      satisfactionRate: 85,
      score: 4.2
    }
  ]
}

const generateMockConsData = (): ProsConsItem[] => {
  return [
    {
      id: 1,
      title: '油耗偏高',
      description: '实际油耗比官方数据高，尤其是市区行驶',
      examples: ['油耗有点高', '比预期费油', '市区很耗油'],
      complaintCount: 687,
      severity: 'moderate',
      impactLevel: '中等',
      suggestions: '优化驾驶习惯，定期保养'
    },
    {
      id: 2,
      title: '内饰用料一般',
      description: '部分区域使用硬塑料，整体质感有待提升',
      examples: ['塑料感重', '用料一般', '质感不够'],
      complaintCount: 543,
      severity: 'minor',
      impactLevel: '轻微'
    }
  ]
}

const generateMockIssues = (severity: string): IssueItem[] => {
  const issues = {
    critical: [
      { id: 1, title: '发动机异响', impact: '影响购买决策' },
      { id: 2, title: '变速箱顿挫', impact: '影响购买决策' }
    ],
    moderate: [
      { id: 3, title: '胎噪偏大', impact: '轻微影响体验' },
      { id: 4, title: '悬挂偏硬', impact: '轻微影响体验' }
    ],
    minor: [
      { id: 5, title: '储物空间不足', impact: '基本不影响' },
      { id: 6, title: '后排略挤', impact: '基本不影响' }
    ]
  }
  
  return issues[severity] || []
}

const generateMockRiskAlerts = (): RiskAlert[] => {
  return [
    {
      id: 1,
      level: 'medium',
      levelText: '中等风险',
      icon: Warning,
      title: '保值率风险',
      description: '该车型保值率略低于同级平均水平',
      impact: 60,
      suggestion: '关注二手车市场行情'
    },
    {
      id: 2,
      level: 'low',
      levelText: '低风险',
      icon: InfoFilled,
      title: '维修成本',
      description: '维修保养费用相对合理',
      impact: 30
    }
  ]
}

const generateMockComparisonData = (): ComparisonTableRow[] => {
  return [
    {
      dimension: '外观设计',
      vehicle_1: { score: 4.5, rank: 1 },
      vehicle_2: { score: 4.2, rank: 2 },
      vehicle_3: { score: 4.0, rank: 3 }
    },
    {
      dimension: '内饰质感',
      vehicle_1: { score: 4.1, rank: 2 },
      vehicle_2: { score: 4.3, rank: 1 },
      vehicle_3: { score: 3.8, rank: 3 }
    }
  ]
}

const generateMockComparisonHeaders = (): ComparisonTableHeader[] => {
  return [
    { id: 1, name: '特斯拉Model 3' },
    { id: 2, name: '比亚迪汉EV' },
    { id: 3, name: '小鹏P7' }
  ]
}

const generateMockCompetitiveAdvantages = (): CompetitiveAdvantage[] => {
  return [
    {
      dimension: '智能化程度',
      status: 'lead',
      statusText: '领先',
      gapAnalysis: '在自动驾驶和车机系统方面明显领先竞品'
    },
    {
      dimension: '续航里程',
      status: 'follow',
      statusText: '跟随',
      gapAnalysis: '续航表现良好，但略逊于部分竞品'
    }
  ]
}

const generateUserProfileTags = (): string[] => {
  const tags = []
  
  if (userProfile.value.ageRange) {
    tags.push(userProfile.value.ageRange + '岁')
  }
  
  if (userProfile.value.usageScenarios.includes('family')) {
    tags.push('家庭用户')
  }
  
  if (userProfile.value.budgetRange === 'under10') {
    tags.push('经济型')
  } else if (userProfile.value.budgetRange === 'over30') {
    tags.push('豪华型')
  } else {
    tags.push('实用型')
  }
  
  if (userProfile.value.drivingExperience === 'beginner') {
    tags.push('新手司机')
  }
  
  return tags
}

// 图表初始化函数
const initSentimentChart = async () => {
  if (!evaluationData.value || !document.querySelector('.sentiment-chart-container')) return
  
  await nextTick()
  
  const chartElement = document.querySelector('.sentiment-chart-container') as HTMLElement
  if (!chartElement) return
  
  if (sentimentChartInstance) {
    sentimentChartInstance.dispose()
  }
  
  sentimentChartInstance = echarts.init(chartElement)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}% ({d}%)'
    },
    series: [{
      name: '情感分析',
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      data: [
        { value: evaluationData.value.sentimentAnalysis.positive, name: '正面', itemStyle: { color: '#67c23a' } },
        { value: evaluationData.value.sentimentAnalysis.neutral, name: '中性', itemStyle: { color: '#909399' } },
        { value: evaluationData.value.sentimentAnalysis.negative, name: '负面', itemStyle: { color: '#f56c6c' } }
      ],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
  
  sentimentChartInstance.setOption(option)
}

const initComparisonRadarChart = async () => {
  if (comparisonVehicles.value.length === 0) return
  
  await nextTick()
  
  const chartElement = document.querySelector('.comparison-radar-container') as HTMLElement
  if (!chartElement) return
  
  if (comparisonRadarChartInstance) {
    comparisonRadarChartInstance.dispose()
  }
  
  comparisonRadarChartInstance = echarts.init(chartElement)
  
  const dimensions = ['外观设计', '内饰质感', '动力表现', '操控性能', '油耗表现', '舒适配置']
  
  const option = {
    title: {
      text: '综合对比雷达图',
      left: 'center'
    },
    tooltip: {},
    radar: {
      indicator: dimensions.map(dim => ({ name: dim, max: 5 }))
    },
    series: [{
      type: 'radar',
      data: comparisonTableHeaders.value.map((header, index) => ({
        value: dimensions.map(() => Math.random() * 2 + 3), // 3-5分
        name: header.name
      }))
    }]
  }
  
  comparisonRadarChartInstance.setOption(option)
}

const initWordCloudChart = async () => {
  await nextTick()
  
  const chartElement = document.querySelector('.wordcloud-container') as HTMLElement
  if (!chartElement) return
  
  if (wordCloudChartInstance) {
    wordCloudChartInstance.dispose()
  }
  
  // 模拟关键词数据
  keywordCategories.value = [
    {
      name: '外观相关',
      words: [
        { text: '漂亮', count: 156, sentiment: 'positive' },
        { text: '时尚', count: 134, sentiment: 'positive' },
        { text: '大气', count: 98, sentiment: 'positive' }
      ]
    },
    {
      name: '性能相关',
      words: [
        { text: '动力强', count: 123, sentiment: 'positive' },
        { text: '省油', count: 89, sentiment: 'positive' },
        { text: '噪音大', count: 45, sentiment: 'negative' }
      ]
    }
  ]
  
  ElMessage.info('词云图功能开发中...')
}

const initTrendChart = async () => {
  await nextTick()
  
  const chartElement = document.querySelector('.trend-chart-container') as HTMLElement
  if (!chartElement) return
  
  if (trendChartInstance) {
    trendChartInstance.dispose()
  }
  
  trendChartInstance = echarts.init(chartElement)
  
  // 模拟趋势数据
  const months = ['1月', '2月', '3月', '4月', '5月', '6月']
  const positiveData = [65, 68, 70, 72, 69, 71]
  const negativeData = [15, 12, 10, 8, 11, 9]
  
  const option = {
    title: {
      text: '口碑情感趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['正面评价', '负面评价'],
      top: 30
    },
    xAxis: {
      type: 'category',
      data: months
    },
    yAxis: {
      type: 'value',
      name: '占比(%)'
    },
    series: [
      {
        name: '正面评价',
        type: 'line',
        data: positiveData,
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '负面评价',
        type: 'line',
        data: negativeData,
        itemStyle: { color: '#f56c6c' }
      }
    ]
  }
  
  trendChartInstance.setOption(option)
  
  // 模拟趋势洞察
  trendInsights.value = [
    {
      id: 1,
      type: 'positive',
      icon: TrendCharts,
      title: '正面评价持续增长',
      description: '近6个月正面评价比例稳步上升，用户满意度不断提高'
    },
    {
      id: 2,
      type: 'positive',
      icon: Check,
      title: '负面评价显著下降',
      description: '主要问题得到有效解决，负面评价降至个位数'
    }
  ]
}

// 窗口大小调整
const handleResize = () => {
  if (sentimentChartInstance) {
    sentimentChartInstance.resize()
  }
  if (comparisonRadarChartInstance) {
    comparisonRadarChartInstance.resize()
  }
  if (wordCloudChartInstance) {
    wordCloudChartInstance.resize()
  }
  if (trendChartInstance) {
    trendChartInstance.resize()
  }
}

// 监听器
watch([selectedVehicle], () => {
  if (selectedVehicle.value) {
    // 获取对比选项
    const filtered = vehicleOptions.value.filter(v => v.id !== selectedVehicle.value)
    comparisonOptions.value = filtered.slice(0, 5)
  }
})

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用口碑聚合分析！')
  
  try {
    // 加载保存的用户画像
    const savedProfile = localStorage.getItem('userProfile')
    if (savedProfile) {
      userProfile.value = JSON.parse(savedProfile)
      userProfileTags.value = generateUserProfileTags()
    }
    
    // 初始化车型选项
    vehicleOptions.value = generateMockVehicleOptions('特斯拉')
    
    // 模拟热门话题
    hotTopics.value = [
      {
        id: 1,
        title: '自动驾驶体验',
        discussionCount: 456,
        sentiment: 'positive',
        summary: '用户对自动驾驶功能普遍满意，认为技术领先',
        examples: ['自动驾驶很智能', 'FSD体验不错', '辅助驾驶很实用']
      },
      {
        id: 2,
        title: '充电便利性',
        discussionCount: 234,
        sentiment: 'negative',
        summary: '部分用户反映充电桩分布不够密集',
        examples: ['充电桩太少', '排队充电时间长', '家用充电不方便']
      }
    ]
    
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
    sentimentChartInstance,
    comparisonRadarChartInstance,
    wordCloudChartInstance,
    trendChartInstance
  ]
  
  charts.forEach(chart => {
    if (chart) {
      chart.dispose()
    }
  })
})
</script>

<template>
  <div class="evaluative">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>口碑聚合分析</h2>
          <p>基于AI智能分析的真实用户评价，为您的购车决策提供信任背书</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
          <el-button type="success" :icon="Download" @click="exportReport" :disabled="!selectedVehicle">
            导出分析
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 车型选择器 -->
    <el-card shadow="never" class="vehicle-selector-card">
      <template #header>
        <div class="selector-header">
          <el-icon><Search /></el-icon>
          <span>选择分析车型</span>
          <el-tag type="info">支持多车型对比</el-tag>
        </div>
      </template>

      <div class="vehicle-selector">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="主要分析车型:">
              <el-select
                v-model="selectedVehicle"
                placeholder="请选择要分析的车型"
                @change="handleVehicleChange"
                style="width: 100%"
                filterable
                remote
                :remote-method="searchVehicles"
                :loading="searching"
              >
                <el-option
                  v-for="vehicle in vehicleOptions"
                  :key="vehicle.id"
                  :label="`${vehicle.brand} ${vehicle.name}`"
                  :value="vehicle.id"
                >
                  <div class="vehicle-option">
                    <span class="vehicle-name">{{ vehicle.brand }} {{ vehicle.name }}</span>
                    <span class="vehicle-info">{{ vehicle.reviewCount }}条评价</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="对比车型:">
              <el-select
                v-model="comparisonVehicles"
                placeholder="选择对比车型（可选）"
                @change="handleComparisonChange"
                style="width: 100%"
                multiple
                :max-collapsed-tags="2"
                :disabled="!selectedVehicle"
              >
                <el-option
                  v-for="vehicle in comparisonOptions"
                  :key="vehicle.id"
                  :label="`${vehicle.brand} ${vehicle.name}`"
                  :value="vehicle.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分析维度:">
              <el-select v-model="analysisScope" @change="handleScopeChange" style="width: 100%">
                <el-option label="全面分析" value="comprehensive" />
                <el-option label="购买决策" value="purchase" />
                <el-option label="使用体验" value="experience" />
                <el-option label="品质可靠性" value="reliability" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 智能口碑聚合核心区域 -->
    <div class="analysis-content" v-if="selectedVehicle && evaluationData">
      <!-- 综合评分概览 -->
      <el-card shadow="never" class="overview-card">
        <template #header>
          <div class="overview-header">
            <el-icon><TrendCharts /></el-icon>
            <span>综合口碑概览</span>
            <div class="data-badges">
              <el-tag type="success" size="small">{{ evaluationData.totalReviews }}条评价</el-tag>
              <el-tag type="primary" size="small">{{ evaluationData.verifiedCount }}条认证</el-tag>
              <el-tag type="warning" size="small">可信度{{ evaluationData.credibilityScore }}%</el-tag>
            </div>
          </div>
        </template>

        <div class="overview-content">
          <el-row :gutter="24">
            <!-- 综合评分区域 -->
            <el-col :xs="24" :md="8">
              <div class="overall-rating">
                <div class="rating-main">
                  <div class="rating-score">{{ evaluationData.overallRating.toFixed(1) }}</div>
                  <div class="rating-stars">
                    <el-rate
                      v-model="evaluationData.overallRating"
                      disabled
                      show-score
                      text-color="#ff9900"
                      :max="5"
                      :precision="0.1"
                    />
                  </div>
                  <div class="rating-label">综合评分</div>
                </div>
                <div class="rating-distribution">
                  <div v-for="(count, star) in evaluationData.ratingDistribution" :key="star" class="distribution-item">
                    <span class="star-level">{{ star }}星</span>
                    <div class="distribution-bar">
                      <div 
                        class="bar-fill" 
                        :style="{ width: (count / evaluationData.totalReviews * 100) + '%' }"
                      ></div>
                    </div>
                    <span class="star-count">{{ count }}</span>
                  </div>
                </div>
              </div>
            </el-col>

            <!-- 分维度评分 -->
            <el-col :xs="24" :md="10">
              <div class="dimension-ratings">
                <h4>分维度评分</h4>
                <div class="dimension-list">
                  <div v-for="dimension in evaluationData.dimensionRatings" :key="dimension.name" class="dimension-item">
                    <div class="dimension-header">
                      <span class="dimension-name">{{ dimension.name }}</span>
                      <span class="dimension-score">{{ dimension.score.toFixed(1) }}</span>
                    </div>
                    <el-progress 
                      :percentage="dimension.score * 20" 
                      :color="getDimensionColor(dimension.score)"
                      :show-text="false"
                      :stroke-width="8"
                    />
                    <div class="dimension-summary">{{ dimension.summary }}</div>
                  </div>
                </div>
              </div>
            </el-col>

            <!-- 情感分析 -->
            <el-col :xs="24" :md="6">
              <div class="sentiment-analysis">
                <h4>情感分析</h4>
                <div class="sentiment-chart">
                  <div ref="sentimentChart" class="sentiment-chart-container"></div>
                </div>
                <div class="sentiment-stats">
                  <div class="sentiment-item positive">
                    <span class="sentiment-label">正面</span>
                    <span class="sentiment-value">{{ evaluationData.sentimentAnalysis.positive }}%</span>
                  </div>
                  <div class="sentiment-item neutral">
                    <span class="sentiment-label">中性</span>
                    <span class="sentiment-value">{{ evaluationData.sentimentAnalysis.neutral }}%</span>
                  </div>
                  <div class="sentiment-item negative">
                    <span class="sentiment-label">负面</span>
                    <span class="sentiment-value">{{ evaluationData.sentimentAnalysis.negative }}%</span>
                  </div>
                </div>
                <div class="trend-indicator">
                  <span class="trend-label">趋势:</span>
                  <span class="trend-value" :class="evaluationData.sentimentTrend > 0 ? 'improving' : 'declining'">
                    {{ evaluationData.sentimentTrend > 0 ? '向好' : '下滑' }}
                  </span>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 个性化匹配推荐 -->
      <el-card shadow="never" class="personalized-card">
        <template #header>
          <div class="personalized-header">
            <el-icon><User /></el-icon>
            <span>个性化口碑匹配</span>
            <el-button size="small" @click="updateUserProfile" type="text">
              更新用户画像
            </el-button>
          </div>
        </template>

        <div class="personalized-content">
          <el-row :gutter="20">
            <!-- 相似用户评价 -->
            <el-col :xs="24" :lg="12">
              <div class="similar-users-section">
                <h4>🎯 相似用户评价</h4>
                <div class="user-profile-tags">
                  <el-tag size="small" v-for="tag in userProfileTags" :key="tag">{{ tag }}</el-tag>
                </div>
                <div class="similar-reviews">
                  <div v-for="review in similarUserReviews" :key="review.id" class="similar-review-item">
                    <div class="review-header">
                      <div class="reviewer-info">
                        <el-avatar :size="32" :src="review.avatar">{{ review.username.charAt(0) }}</el-avatar>
                        <div class="reviewer-details">
                          <span class="reviewer-name">{{ review.username }}</span>
                          <div class="reviewer-tags">
                            <el-tag size="small" type="info" v-for="tag in review.tags" :key="tag">{{ tag }}</el-tag>
                          </div>
                        </div>
                      </div>
                      <div class="review-meta">
                        <el-rate v-model="review.rating" disabled size="small" />
                        <span class="review-date">{{ formatDate(review.date) }}</span>
                      </div>
                    </div>
                    <div class="review-content">
                      <p>{{ review.content }}</p>
                      <div class="review-highlights" v-if="review.highlights">
                        <el-tag 
                          v-for="highlight in review.highlights" 
                          :key="highlight" 
                          size="small" 
                          type="success"
                        >
                          {{ highlight }}
                        </el-tag>
                      </div>
                    </div>
                    <div class="review-actions">
                      <el-button size="small" type="text" @click="likeReview(review.id)">
                        <el-icon><Star /></el-icon>
                        有用 {{ review.likes }}
                      </el-button>
                      <el-button size="small" type="text" @click="viewFullReview(review.id)">
                        查看详情
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>

            <!-- 关注点筛选 -->
            <el-col :xs="24" :lg="12">
              <div class="focus-filtering-section">
                <h4>🔍 关注点筛选</h4>
                <div class="filter-tabs">
                  <el-tabs v-model="activeFocusTab" @tab-change="handleFocusChange">
                    <el-tab-pane label="使用场景" name="scenario">
                      <div class="scenario-reviews">
                        <div class="scenario-selector">
                          <el-select v-model="selectedScenario" @change="filterByScenario">
                            <el-option label="日常通勤" value="commute" />
                            <el-option label="家庭出行" value="family" />
                            <el-option label="长途驾驶" value="longTrip" />
                            <el-option label="城市代步" value="urban" />
                          </el-select>
                        </div>
                        <div class="scenario-insights">
                          <div v-for="insight in scenarioInsights" :key="insight.id" class="insight-item">
                            <div class="insight-header">
                              <el-icon><Location /></el-icon>
                              <span>{{ insight.title }}</span>
                              <el-rate v-model="insight.rating" disabled size="small" />
                            </div>
                            <div class="insight-content">{{ insight.description }}</div>
                            <div class="insight-data">
                              <span class="data-label">样本数:</span>
                              <span class="data-value">{{ insight.sampleSize }}条</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </el-tab-pane>
                    <el-tab-pane label="特定方面" name="aspect">
                      <div class="aspect-analysis">
                        <div class="aspect-selector">
                          <el-checkbox-group v-model="selectedAspects" @change="filterByAspects">
                            <el-checkbox value="exterior">外观设计</el-checkbox>
                            <el-checkbox value="interior">内饰质感</el-checkbox>
                            <el-checkbox value="power">动力表现</el-checkbox>
                            <el-checkbox value="handling">操控性能</el-checkbox>
                            <el-checkbox value="comfort">舒适配置</el-checkbox>
                            <el-checkbox value="safety">安全性能</el-checkbox>
                          </el-checkbox-group>
                        </div>
                        <div class="aspect-results">
                          <div v-for="aspect in aspectResults" :key="aspect.name" class="aspect-result-item">
                            <div class="aspect-summary">
                              <span class="aspect-name">{{ aspect.label }}</span>
                              <span class="aspect-score" :class="getScoreClass(aspect.score)">
                                {{ aspect.score.toFixed(1) }}分
                              </span>
                            </div>
                            <div class="aspect-keywords">
                              <el-tag 
                                v-for="keyword in aspect.keywords" 
                                :key="keyword.word"
                                :type="keyword.sentiment === 'positive' ? 'success' : keyword.sentiment === 'negative' ? 'danger' : 'info'"
                                size="small"
                              >
                                {{ keyword.word }}
                              </el-tag>
                            </div>
                          </div>
                        </div>
                      </div>
                    </el-tab-pane>
                  </el-tabs>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 优缺点智能分析 -->
      <el-card shadow="never" class="pros-cons-card">
        <template #header>
          <div class="pros-cons-header">
            <el-icon><DataAnalysis /></el-icon>
            <span>智能优缺点分析</span>
            <div class="analysis-controls">
              <el-button size="small" @click="regenerateAnalysis" :loading="analyzing">
                重新分析
              </el-button>
              <el-button size="small" type="primary" @click="exportProsConsReport">
                导出报告
              </el-button>
            </div>
          </div>
        </template>

        <div class="pros-cons-content">
          <el-row :gutter="24">
            <!-- 主要优势 -->
            <el-col :xs="24" :lg="12">
              <div class="pros-section">
                <div class="section-header">
                  <el-icon class="pros-icon"><CircleCheckFilled /></el-icon>
                  <h4>主要优势</h4>
                  <el-tag type="success" size="small">{{ prosData.length }}项</el-tag>
                </div>
                <div class="pros-list">
                  <div v-for="(pro, index) in prosData" :key="pro.id" class="pros-item">
                    <div class="item-header">
                      <div class="item-rank">{{ index + 1 }}</div>
                      <div class="item-info">
                        <h5>{{ pro.title }}</h5>
                        <div class="item-meta">
                          <span class="mention-count">{{ pro.mentionCount }}次提及</span>
                          <span class="satisfaction-rate">满意度{{ pro.satisfactionRate }}%</span>
                        </div>
                      </div>
                      <div class="item-score">
                        <div class="score-value">{{ pro.score }}</div>
                        <div class="score-label">评分</div>
                      </div>
                    </div>
                    <div class="item-description">{{ pro.description }}</div>
                    <div class="item-examples">
                      <el-tag 
                        v-for="example in pro.examples" 
                        :key="example" 
                        size="small" 
                        type="success"
                      >
                        "{{ example }}"
                      </el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>

            <!-- 主要劣势 -->
            <el-col :xs="24" :lg="12">
              <div class="cons-section">
                <div class="section-header">
                  <el-icon class="cons-icon"><WarningFilled /></el-icon>
                  <h4>主要劣势</h4>
                  <el-tag type="danger" size="small">{{ consData.length }}项</el-tag>
                </div>
                <div class="cons-list">
                  <div v-for="(con, index) in consData" :key="con.id" class="cons-item">
                    <div class="item-header">
                      <div class="item-rank warning">{{ index + 1 }}</div>
                      <div class="item-info">
                        <h5>{{ con.title }}</h5>
                        <div class="item-meta">
                          <span class="complaint-count">{{ con.complaintCount }}条投诉</span>
                          <span class="severity-level" :class="con.severity">{{ getSeverityText(con.severity) }}</span>
                        </div>
                      </div>
                      <div class="item-impact" :class="con.severity">
                        <div class="impact-level">{{ con.impactLevel }}</div>
                        <div class="impact-label">影响度</div>
                      </div>
                    </div>
                    <div class="item-description">{{ con.description }}</div>
                    <div class="item-examples">
                      <el-tag 
                        v-for="example in con.examples" 
                        :key="example" 
                        size="small" 
                        type="danger"
                      >
                        "{{ example }}"
                      </el-tag>
                    </div>
                    <div class="improvement-suggestion" v-if="con.suggestions">
                      <span class="suggestion-label">改进建议:</span>
                      <span class="suggestion-text">{{ con.suggestions }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>

          <!-- 问题分级汇总 -->
          <div class="issue-classification">
            <h4>🚨 问题分级汇总</h4>
            <el-row :gutter="16">
              <el-col :span="8">
                <div class="issue-category critical">
                  <div class="category-header">
                    <el-icon><Warning /></el-icon>
                    <span>严重问题</span>
                    <el-badge :value="criticalIssues.length" type="danger" />
                  </div>
                  <div class="category-content">
                    <div v-for="issue in criticalIssues" :key="issue.id" class="issue-item">
                      <span class="issue-title">{{ issue.title }}</span>
                      <span class="issue-impact">影响购买决策</span>
                    </div>
                  </div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="issue-category moderate">
                  <div class="category-header">
                    <el-icon><InfoFilled /></el-icon>
                    <span>一般问题</span>
                    <el-badge :value="moderateIssues.length" type="warning" />
                  </div>
                  <div class="category-content">
                    <div v-for="issue in moderateIssues" :key="issue.id" class="issue-item">
                      <span class="issue-title">{{ issue.title }}</span>
                      <span class="issue-impact">轻微影响体验</span>
                    </div>
                  </div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="issue-category minor">
                  <div class="category-header">
                    <el-icon><QuestionFilled /></el-icon>
                    <span>轻微问题</span>
                    <el-badge :value="minorIssues.length" type="info" />
                  </div>
                  <div class="category-content">
                    <div v-for="issue in minorIssues" :key="issue.id" class="issue-item">
                      <span class="issue-title">{{ issue.title }}</span>
                      <span class="issue-impact">基本不影响</span>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-card>

      <!-- 口碑对比分析 -->
      <el-card shadow="never" class="comparison-card" v-if="comparisonVehicles.length > 0">
        <template #header>
          <div class="comparison-header">
            <el-icon><DataBoard /></el-icon>
            <span>口碑对比分析</span>
            <div class="comparison-models">
              <el-tag v-for="vehicleId in comparisonVehicles" :key="vehicleId" size="small">
                {{ getVehicleName(vehicleId) }}
              </el-tag>
            </div>
          </div>
        </template>

        <div class="comparison-content">
          <!-- 综合对比雷达图 -->
          <div class="comparison-radar">
            <h4>综合对比雷达图</h4>
            <div ref="comparisonRadarChart" class="comparison-radar-container"></div>
          </div>

          <!-- 详细对比表格 -->
          <div class="comparison-table">
            <h4>详细对比数据</h4>
            <el-table :data="comparisonTableData" border>
              <el-table-column prop="dimension" label="对比维度" width="120" />
              <el-table-column 
                v-for="vehicle in comparisonTableHeaders" 
                :key="vehicle.id"
                :prop="`vehicle_${vehicle.id}`"
                :label="vehicle.name"
                align="center"
              >
                <template #default="{ row }">
                  <div class="table-cell-content">
                    <div class="cell-score">{{ row[`vehicle_${vehicle.id}`].score }}</div>
                    <div class="cell-rank" :class="getRankClass(row[`vehicle_${vehicle.id}`].rank)">
                      第{{ row[`vehicle_${vehicle.id}`].rank }}名
                    </div>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 竞争优劣势分析 -->
          <div class="competitive-analysis">
            <h4>竞争优劣势分析</h4>
            <div class="advantage-comparison">
              <div v-for="advantage in competitiveAdvantages" :key="advantage.dimension" class="advantage-item">
                <div class="advantage-header">
                  <span class="dimension-name">{{ advantage.dimension }}</span>
                  <el-tag :type="advantage.status === 'lead' ? 'success' : advantage.status === 'follow' ? 'warning' : 'danger'">
                    {{ advantage.statusText }}
                  </el-tag>
                </div>
                <div class="advantage-details">
                  <span class="gap-analysis">{{ advantage.gapAnalysis }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- AI决策支持工具 -->
      <el-card shadow="never" class="decision-support-card">
        <template #header>
          <div class="decision-header">
            <el-icon><MagicStick /></el-icon>
            <span>AI决策支持</span>
            <el-tag type="primary">智能推荐</el-tag>
          </div>
        </template>

        <div class="decision-content">
          <el-row :gutter="20">
            <!-- 购买建议 -->
            <el-col :xs="24" :lg="8">
              <div class="purchase-recommendation">
                <h4>🎯 购买建议</h4>
                <div class="recommendation-card" :class="purchaseRecommendation.type">
                  <div class="recommendation-header">
                    <el-icon>
                      <component :is="purchaseRecommendation.icon" />
                    </el-icon>
                    <span class="recommendation-title">{{ purchaseRecommendation.title }}</span>
                  </div>
                  <div class="recommendation-content">
                    <p>{{ purchaseRecommendation.description }}</p>
                    <div class="recommendation-score">
                      <span class="score-label">推荐指数:</span>
                      <el-rate v-model="purchaseRecommendation.score" disabled show-score text-color="#ff9900" />
                    </div>
                  </div>
                  <div class="recommendation-reasons">
                    <h5>主要原因:</h5>
                    <ul>
                      <li v-for="reason in purchaseRecommendation.reasons" :key="reason">{{ reason }}</li>
                    </ul>
                  </div>
                </div>
              </div>
            </el-col>

            <!-- 风险提醒 -->
            <el-col :xs="24" :lg="8">
              <div class="risk-alerts">
                <h4>⚠️ 风险提醒</h4>
                <div class="risk-list">
                  <div v-for="risk in riskAlerts" :key="risk.id" class="risk-item" :class="risk.level">
                    <div class="risk-header">
                      <el-icon>
                        <component :is="risk.icon" />
                      </el-icon>
                      <span class="risk-title">{{ risk.title }}</span>
                      <el-tag :type="getRiskTagType(risk.level)" size="small">{{ risk.levelText }}</el-tag>
                    </div>
                    <div class="risk-description">{{ risk.description }}</div>
                    <div class="risk-impact">
                      <span class="impact-label">影响程度:</span>
                      <el-progress 
                        :percentage="risk.impact" 
                        :color="getRiskColor(risk.level)"
                        :show-text="false"
                        :stroke-width="6"
                      />
                    </div>
                    <div class="risk-suggestion" v-if="risk.suggestion">
                      <span class="suggestion-label">建议:</span>
                      <span class="suggestion-text">{{ risk.suggestion }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-col>

            <!-- 最佳购买时机 -->
            <el-col :xs="24" :lg="8">
              <div class="timing-advice">
                <h4>📅 购买时机建议</h4>
                <div class="timing-card">
                  <div class="timing-main">
                    <div class="timing-status" :class="timingAdvice.status">
                      <el-icon>
                        <component :is="timingAdvice.icon" />
                      </el-icon>
                      <span class="timing-text">{{ timingAdvice.title }}</span>
                    </div>
                    <div class="timing-description">{{ timingAdvice.description }}</div>
                  </div>
                  <div class="timing-factors">
                    <h5>影响因素:</h5>
                    <div class="factor-list">
                      <div v-for="factor in timingAdvice.factors" :key="factor.name" class="factor-item">
                        <span class="factor-name">{{ factor.name }}:</span>
                        <span class="factor-impact" :class="factor.impact">{{ factor.description }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="timing-score">
                    <span class="score-label">购买时机评分:</span>
                    <div class="score-bar">
                      <div 
                        class="score-fill" 
                        :style="{ width: timingAdvice.score + '%' }"
                        :class="getTimingScoreClass(timingAdvice.score)"
                      ></div>
                    </div>
                    <span class="score-text">{{ timingAdvice.score }}/100</span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 关键词云和趋势分析 -->
      <el-card shadow="never" class="insights-card">
        <template #header>
          <div class="insights-header">
            <el-icon><PieChart /></el-icon>
            <span>深度洞察分析</span>
            <div class="insights-controls">
              <el-button-group size="small">
                <el-button @click="setInsightView('keywords')" :type="insightView === 'keywords' ? 'primary' : ''">
                  关键词云
                </el-button>
                <el-button @click="setInsightView('trends')" :type="insightView === 'trends' ? 'primary' : ''">
                  趋势分析
                </el-button>
                <el-button @click="setInsightView('topics')" :type="insightView === 'topics' ? 'primary' : ''">
                  热门话题
                </el-button>
              </el-button-group>
            </div>
          </div>
        </template>

        <div class="insights-content">
          <!-- 关键词云 -->
          <div v-if="insightView === 'keywords'" class="keywords-view">
            <div class="keywords-cloud">
              <div ref="wordCloudChart" class="wordcloud-container"></div>
            </div>
            <div class="keywords-analysis">
              <h5>关键词分析</h5>
              <div class="keyword-categories">
                <div v-for="category in keywordCategories" :key="category.name" class="keyword-category">
                  <h6>{{ category.name }}</h6>
                  <div class="category-words">
                    <el-tag 
                      v-for="word in category.words" 
                      :key="word.text"
                      :type="word.sentiment === 'positive' ? 'success' : word.sentiment === 'negative' ? 'danger' : 'info'"
                      size="small"
                    >
                      {{ word.text }} ({{ word.count }})
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 趋势分析 -->
          <div v-if="insightView === 'trends'" class="trends-view">
            <div class="trend-charts">
              <div ref="trendChart" class="trend-chart-container"></div>
            </div>
            <div class="trend-analysis">
              <h5>趋势分析摘要</h5>
              <div class="trend-insights">
                <div v-for="insight in trendInsights" :key="insight.id" class="trend-insight-item">
                  <div class="insight-icon" :class="insight.type">
                    <el-icon>
                      <component :is="insight.icon" />
                    </el-icon>
                  </div>
                  <div class="insight-content">
                    <h6>{{ insight.title }}</h6>
                    <p>{{ insight.description }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 热门话题 -->
          <div v-if="insightView === 'topics'" class="topics-view">
            <div class="hot-topics">
              <div v-for="topic in hotTopics" :key="topic.id" class="topic-item">
                <div class="topic-header">
                  <span class="topic-title">{{ topic.title }}</span>
                  <div class="topic-meta">
                    <el-tag size="small">{{ topic.discussionCount }}条讨论</el-tag>
                    <el-tag size="small" :type="topic.sentiment === 'positive' ? 'success' : 'warning'">
                      {{ topic.sentiment === 'positive' ? '正面' : '负面' }}
                    </el-tag>
                  </div>
                </div>
                <div class="topic-summary">{{ topic.summary }}</div>
                <div class="topic-examples">
                  <div v-for="example in topic.examples" :key="example" class="example-item">
                    "{{ example }}"
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 空状态提示 -->
    <el-empty v-if="!selectedVehicle" description="请选择要分析的车型">
      <el-button type="primary" @click="showVehicleSelector">选择车型</el-button>
    </el-empty>

    <!-- 用户画像更新弹窗 -->
    <el-dialog v-model="showProfileDialog" title="更新用户画像" width="50%">
      <div class="profile-form">
        <el-form :model="userProfile" label-width="120px">
          <el-form-item label="年龄范围:">
            <el-radio-group v-model="userProfile.ageRange">
              <el-radio value="18-25">18-25岁</el-radio>
              <el-radio value="26-35">26-35岁</el-radio>
              <el-radio value="36-45">36-45岁</el-radio>
              <el-radio value="46+">46岁以上</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="使用场景:">
            <el-checkbox-group v-model="userProfile.usageScenarios">
              <el-checkbox value="commute">日常通勤</el-checkbox>
              <el-checkbox value="family">家庭出行</el-checkbox>
              <el-checkbox value="business">商务用车</el-checkbox>
              <el-checkbox value="leisure">休闲娱乐</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="驾驶经验:">
            <el-select v-model="userProfile.drivingExperience">
              <el-option label="新手司机" value="beginner" />
              <el-option label="经验丰富" value="experienced" />
              <el-option label="专业级别" value="professional" />
            </el-select>
          </el-form-item>
          <el-form-item label="预算范围:">
            <el-select v-model="userProfile.budgetRange">
              <el-option label="10万以下" value="under10" />
              <el-option label="10-20万" value="10-20" />
              <el-option label="20-30万" value="20-30" />
              <el-option label="30万以上" value="over30" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showProfileDialog = false">取消</el-button>
          <el-button type="primary" @click="saveUserProfile">保存画像</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 整体布局 */
.evaluative {
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

/* 车型选择器卡片 */
.vehicle-selector-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.selector-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #4facfe;
  font-size: 16px;
}

.vehicle-selector {
  padding: 24px;
}

.vehicle-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.vehicle-name {
  font-weight: 500;
  color: #1a1a1a;
}

.vehicle-info {
  font-size: 12px;
  color: #909399;
}

/* 分析内容区域 */
.analysis-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 综合评分概览卡片 */
.overview-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.data-badges {
  display: flex;
  gap: 8px;
}

.overview-content {
  padding: 24px;
}

/* 综合评分区域 */
.overall-rating {
  text-align: center;
}

.rating-main {
  margin-bottom: 24px;
}

.rating-score {
  font-size: 48px;
  font-weight: 700;
  color: #4facfe;
  margin-bottom: 8px;
}

.rating-stars {
  margin-bottom: 8px;
}

.rating-label {
  font-size: 16px;
  color: #606266;
  font-weight: 500;
}

.rating-distribution {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.distribution-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.star-level {
  width: 40px;
  text-align: right;
  color: #606266;
}

.distribution-bar {
  flex: 1;
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(135deg, #4facfe 0%, #00d4ff 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.star-count {
  width: 40px;
  text-align: left;
  color: #909399;
  font-size: 12px;
}

/* 分维度评分 */
.dimension-ratings h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.dimension-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dimension-item {
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.dimension-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.dimension-name {
  font-weight: 500;
  color: #1a1a1a;
}

.dimension-score {
  font-weight: 700;
  color: #4facfe;
  font-size: 16px;
}

.dimension-summary {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
}

/* 情感分析 */
.sentiment-analysis h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
}

.sentiment-chart {
  margin-bottom: 16px;
}

.sentiment-chart-container {
  height: 200px;
  width: 100%;
}

.sentiment-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.sentiment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
}

.sentiment-item.positive {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.sentiment-item.neutral {
  background: rgba(144, 147, 153, 0.1);
  color: #909399;
}

.sentiment-item.negative {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.sentiment-label {
  font-weight: 500;
}

.sentiment-value {
  font-weight: 700;
}

.trend-indicator {
  text-align: center;
  padding: 8px;
  background: #f0f9ff;
  border-radius: 6px;
  font-size: 12px;
}

.trend-label {
  color: #606266;
}

.trend-value.improving {
  color: #67c23a;
  font-weight: 600;
}

.trend-value.declining {
  color: #f56c6c;
  font-weight: 600;
}

/* 个性化匹配推荐卡片 */
.personalized-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.personalized-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.personalized-content {
  padding: 24px;
}

/* 相似用户评价 */
.similar-users-section h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.user-profile-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.similar-reviews {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.similar-review-item {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.similar-review-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
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
  gap: 12px;
}

.reviewer-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reviewer-name {
  font-weight: 500;
  color: #1a1a1a;
  font-size: 14px;
}

.reviewer-tags {
  display: flex;
  gap: 4px;
}

.review-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.review-date {
  font-size: 12px;
  color: #909399;
}

.review-content p {
  margin: 0 0 12px 0;
  color: #606266;
  line-height: 1.5;
  font-size: 14px;
}

.review-highlights {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.review-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 关注点筛选 */
.focus-filtering-section h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.filter-tabs {
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.scenario-selector {
  margin-bottom: 16px;
}

.scenario-insights {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.insight-item {
  padding: 12px;
  background: #f8fafb;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 500;
  color: #1a1a1a;
}

.insight-content {
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.insight-data {
  display: flex;
  gap: 8px;
  font-size: 12px;
}

.data-label {
  color: #909399;
}

.data-value {
  color: #4facfe;
  font-weight: 500;
}

.aspect-selector {
  margin-bottom: 16px;
}

.aspect-results {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.aspect-result-item {
  padding: 12px;
  background: #f8fafb;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.aspect-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.aspect-name {
  font-weight: 500;
  color: #1a1a1a;
}

.aspect-score {
  font-weight: 700;
  font-size: 14px;
}

.aspect-score.excellent {
  color: #67c23a;
}

.aspect-score.good {
  color: #4facfe;
}

.aspect-score.fair {
  color: #e6a23c;
}

.aspect-score.poor {
  color: #f56c6c;
}

.aspect-keywords {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* 优缺点智能分析卡片 */
.pros-cons-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.pros-cons-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.analysis-controls {
  display: flex;
  gap: 8px;
}

.pros-cons-content {
  padding: 24px;
}

/* 优势和劣势区域 */
.pros-section, .cons-section {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.section-header h4 {
  margin: 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.pros-icon {
  color: #67c23a;
  font-size: 18px;
}

.cons-icon {
  color: #f56c6c;
  font-size: 18px;
}

.pros-list, .cons-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
}

.pros-item, .cons-item {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.pros-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(103, 194, 58, 0.2);
  border-color: #67c23a;
}

.cons-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(245, 108, 108, 0.2);
  border-color: #f56c6c;
}

.item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.item-rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}

.item-rank.warning {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.item-info {
  flex: 1;
}

.item-info h5 {
  margin: 0 0 4px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.item-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.mention-count, .complaint-count {
  color: #606266;
}

.satisfaction-rate {
  color: #67c23a;
  font-weight: 500;
}

.severity-level {
  font-weight: 500;
}

.severity-level.critical {
  color: #f56c6c;
}

.severity-level.moderate {
  color: #e6a23c;
}

.severity-level.minor {
  color: #909399;
}

.item-score, .item-impact {
  text-align: center;
  min-width: 60px;
}

.score-value, .impact-level {
  font-size: 18px;
  font-weight: 700;
  color: #67c23a;
}

.item-impact.critical .impact-level {
  color: #f56c6c;
}

.item-impact.moderate .impact-level {
  color: #e6a23c;
}

.item-impact.minor .impact-level {
  color: #909399;
}

.score-label, .impact-label {
  font-size: 10px;
  color: #909399;
  margin-top: 2px;
}

.item-description {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

.item-examples {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.improvement-suggestion {
  padding: 8px 12px;
  background: #fff9e6;
  border-radius: 6px;
  border-left: 3px solid #ffd700;
  font-size: 12px;
}

.suggestion-label {
  color: #b8860b;
  font-weight: 500;
}

.suggestion-text {
  color: #606266;
}

/* 问题分级汇总 */
.issue-classification {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}

.issue-classification h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.issue-category {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  height: 100%;
}

.issue-category.critical {
  background: linear-gradient(135deg, #fff5f5 0%, #fef0f0 100%);
  border-color: #f56c6c;
}

.issue-category.moderate {
  background: linear-gradient(135deg, #fdfcf0 0%, #fdf9e6 100%);
  border-color: #e6a23c;
}

.issue-category.minor {
  background: linear-gradient(135deg, #f5f7fa 0%, #f0f2f5 100%);
  border-color: #909399;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #1a1a1a;
}

.category-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.issue-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  background: white;
  border-radius: 6px;
}

.issue-title {
  font-weight: 500;
  color: #1a1a1a;
  font-size: 13px;
}

.issue-impact {
  font-size: 11px;
  color: #606266;
}

/* 口碑对比分析卡片 */
.comparison-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.comparison-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.comparison-models {
  display: flex;
  gap: 8px;
}

.comparison-content {
  padding: 24px;
}

.comparison-radar {
  margin-bottom: 24px;
}

.comparison-radar h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
}

.comparison-radar-container {
  height: 300px;
  width: 100%;
}

.comparison-table {
  margin-bottom: 24px;
}

.comparison-table h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.table-cell-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.cell-score {
  font-weight: 700;
  color: #4facfe;
  font-size: 16px;
}

.cell-rank {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.cell-rank.first {
  background: #f0f9ff;
  color: #1890ff;
}

.cell-rank.second {
  background: #fff7e6;
  color: #fa8c16;
}

.cell-rank.third {
  background: #f6ffed;
  color: #52c41a;
}

.cell-rank.normal {
  background: #f5f5f5;
  color: #8c8c8c;
}

.competitive-analysis h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.advantage-comparison {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.advantage-item {
  padding: 12px;
  background: #f8fafb;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.advantage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.dimension-name {
  font-weight: 500;
  color: #1a1a1a;
}

.advantage-details {
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

.gap-analysis {
  color: #606266;
}

/* AI决策支持工具卡片 */
.decision-support-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.decision-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.decision-content {
  padding: 24px;
}

/* 购买建议 */
.purchase-recommendation h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.recommendation-card {
  padding: 20px;
  border-radius: 12px;
  border: 2px solid;
  background: white;
}

.recommendation-card.recommend {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
}

.recommendation-card.caution {
  border-color: #e6a23c;
  background: linear-gradient(135deg, #fdfcf0 0%, #fdf9e6 100%);
}

.recommendation-card.wait {
  border-color: #f56c6c;
  background: linear-gradient(135deg, #fff5f5 0%, #fef0f0 100%);
}

.recommendation-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.recommendation-title {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.recommendation-content p {
  margin: 0 0 12px 0;
  color: #606266;
  line-height: 1.5;
}

.recommendation-score {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.score-label {
  font-weight: 500;
  color: #606266;
}

.recommendation-reasons h5 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.recommendation-reasons ul {
  margin: 0;
  padding-left: 16px;
  color: #606266;
}

.recommendation-reasons li {
  margin-bottom: 4px;
  font-size: 13px;
  line-height: 1.4;
}

/* 风险提醒 */
.risk-alerts h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.risk-item {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  background: white;
}

.risk-item.high {
  border-color: #f56c6c;
  background: linear-gradient(135deg, #fff5f5 0%, #fef0f0 100%);
}

.risk-item.medium {
  border-color: #e6a23c;
  background: linear-gradient(135deg, #fdfcf0 0%, #fdf9e6 100%);
}

.risk-item.low {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f6ffed 0%, #f0f9ff 100%);
}

.risk-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.risk-title {
  flex: 1;
  font-weight: 500;
  color: #1a1a1a;
  font-size: 14px;
}

.risk-description {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

.risk-impact {
  margin-bottom: 8px;
}

.impact-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  display: block;
}

.risk-suggestion {
  font-size: 12px;
  color: #606266;
}

.suggestion-label {
  font-weight: 500;
}

.suggestion-text {
  color: #606266;
}

/* 最佳购买时机 */
.timing-advice h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.timing-card {
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  background: white;
}

.timing-main {
  margin-bottom: 16px;
}

.timing-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 600;
  font-size: 16px;
}

.timing-status.good {
  color: #67c23a;
}

.timing-status.fair {
  color: #e6a23c;
}

.timing-status.poor {
  color: #f56c6c;
}

.timing-text {
  font-weight: 600;
}

.timing-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.timing-factors h5 {
  margin: 0 0 12px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.factor-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.factor-item {
  font-size: 12px;
  line-height: 1.4;
}

.factor-name {
  font-weight: 500;
  color: #1a1a1a;
}

.factor-impact.positive {
  color: #67c23a;
}

.factor-impact.negative {
  color: #f56c6c;
}

.factor-impact.neutral {
  color: #909399;
}

.timing-score {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.score-bar {
  flex: 1;
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.score-fill.excellent {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.score-fill.good {
  background: linear-gradient(135deg, #4facfe 0%, #00d4ff 100%);
}

.score-fill.fair {
  background: linear-gradient(135deg, #e6a23c 0%, #f7ba2a 100%);
}

.score-fill.poor {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.score-text {
  font-size: 12px;
  color: #4facfe;
  font-weight: 600;
  white-space: nowrap;
}

/* 深度洞察分析卡片 */
.insights-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.insights-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.insights-controls .el-button-group {
  border-radius: 6px;
  overflow: hidden;
}

.insights-content {
  padding: 24px;
}

/* 关键词云视图 */
.keywords-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.keywords-cloud {
  text-align: center;
}

.wordcloud-container {
  height: 300px;
  width: 100%;
  background: #f8fafb;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

.keywords-analysis h5 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.keyword-categories {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.keyword-category {
  padding: 16px;
  background: #f8fafb;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.keyword-category h6 {
  margin: 0 0 12px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.category-words {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* 趋势分析视图 */
.trends-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.trend-charts {
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  padding: 16px;
}

.trend-chart-container {
  height: 300px;
  width: 100%;
}

.trend-analysis h5 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.trend-insights {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trend-insight-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8fafb;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.insight-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.insight-icon.positive {
  background: #67c23a;
}

.insight-icon.negative {
  background: #f56c6c;
}

.insight-icon.neutral {
  background: #909399;
}

.insight-content h6 {
  margin: 0 0 4px 0;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}

.insight-content p {
  margin: 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

/* 热门话题视图 */
.topics-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hot-topics {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.topic-item {
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.topic-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.topic-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.topic-title {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.topic-meta {
  display: flex;
  gap: 8px;
}

.topic-summary {
  margin-bottom: 12px;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.topic-examples {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.example-item {
  padding: 8px 12px;
  background: #f8fafb;
  border-radius: 6px;
  border-left: 3px solid #4facfe;
  font-size: 13px;
  color: #606266;
  font-style: italic;
}

/* 用户画像更新弹窗 */
.profile-form {
  padding: 16px 0;
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

.el-tabs__nav-wrap::after {
  display: none;
}

.el-tabs__item {
  border-radius: 6px 6px 0 0;
  font-weight: 500;
}

.el-tabs__item.is-active {
  color: #4facfe;
  background: white;
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

.overview-card,
.personalized-card,
.pros-cons-card,
.comparison-card,
.decision-support-card,
.insights-card {
  animation: slideInUp 0.6s ease-out;
}

.similar-review-item,
.pros-item,
.cons-item,
.risk-item,
.topic-item {
  animation: slideInUp 0.4s ease-out;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .header-left h2 {
    font-size: 28px;
  }

  .overview-content {
    padding: 20px;
  }

  .personalized-content,
  .pros-cons-content,
  .comparison-content,
  .decision-content,
  .insights-content {
    padding: 20px;
  }
}

@media (max-width: 1200px) {
  .evaluative {
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

  .overview-content .el-row {
    flex-direction: column;
  }

  .personalized-content .el-row,
  .pros-cons-content .el-row,
  .decision-content .el-row {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .evaluative {
    padding: 0 8px;
  }

  .header-left h2 {
    font-size: 24px;
  }

  .header-left p {
    font-size: 14px;
  }

  .vehicle-selector .el-row {
    flex-direction: column;
  }

  .vehicle-selector .el-col {
    margin-bottom: 16px;
  }

  .rating-score {
    font-size: 36px;
  }

  .similar-reviews {
    max-height: 300px;
  }

  .issue-classification .el-row {
    flex-direction: column;
  }

  .issue-classification .el-col {
    margin-bottom: 12px;
  }

  .comparison-radar-container,
  .trend-chart-container {
    height: 250px;
  }

  .item-header {
    flex-wrap: wrap;
    gap: 8px;
  }

  .item-score,
  .item-impact {
    min-width: 50px;
  }
}

/* 打印样式 */
@media print {
  .evaluative {
    background: white !important;
  }

  .header-actions,
  .analysis-controls,
  .insights-controls,
  .review-actions {
    display: none !important;
  }

  .page-header,
  .overview-card,
  .personalized-card,
  .pros-cons-card,
  .comparison-card,
  .decision-support-card,
  .insights-card {
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

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .evaluative {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .overview-card,
  .personalized-card,
  .pros-cons-card,
  .comparison-card,
  .decision-support-card,
  .insights-card,
  .vehicle-selector-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .similar-review-item,
  .pros-item,
  .cons-item,
  .risk-item,
  .topic-item,
  .timing-card,
  .recommendation-card {
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
.similar-review-item:focus-visible,
.pros-item:focus-visible,
.cons-item:focus-visible,
.risk-item:focus-visible,
.topic-item:focus-visible {
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
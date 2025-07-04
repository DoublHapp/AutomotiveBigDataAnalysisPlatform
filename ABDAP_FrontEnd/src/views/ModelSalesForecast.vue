<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  RefreshRight,
  Setting,
  Plus,
  TrendCharts,
  DataLine,
  ArrowDown,
  Box,
  Money,
  Clock,
  Warning,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()

// 接口定义 - 车型预测专用
interface CarModel {
  carModelId: number
  modelName: string
  brandName: string
  price: number
  category: string
  launchDate: string
}

interface ModelLifecycleStage {
  stage: 'introduction' | 'growth' | 'maturity' | 'decline' //导入期/成长期/成熟期/衰退期
  stageText: string
  description: string
  monthsInStage: number
  expectedDuration: number
}

interface VersionForecast {
  version: string
  currentSales: number
  predictedSales: number
  growthRate: number
  marketShare: number
  profitMargin: number
  suggestions: string[]
}

interface CompetitorThreat {
  modelId: number
  modelName: string
  level: 'high' | 'medium' | 'low'
  levelText: string
  marketOverlap: number
  priceCompetition: number
  expectedImpact: number
}

interface OptimizationResult {
  configuration: string
  currentRatio: number
  recommendedRatio: number
  expectedImpact: number
  reasoning: string
}

interface SimulationResult {
  salesImpact: number
  revenueImpact: number
  profitImpact: number
  marketShareImpact: number
}

interface SeasonalInventory {
  month: string
  demandIndex: number
  recommendedStock: number
  adjustment: string
}

interface CompetitorPricing {
  modelName: string
  currentPrice: number
  pricePosition: {
    type: 'success' | 'warning' | 'danger'
    text: string
  }
  marketResponse: number
}

// 响应式数据
const loading = ref(false)
const analyzing = ref(false)
const optimizing = ref(false)

// 车型选择与分析
const selectedModel = ref<number | null>(null)
const forecastPeriod = ref('12M')
const forecastGranularity = ref('monthly')
const comparisonModels = ref<number[]>([])
const comparisonDimensions = ref(['sales', 'price', 'market_share'])

// 数据源
const carModelList = ref<CarModel[]>([])
const modelResults = ref<any>(null)

// 弹窗控制
const showProductConfig = ref(false)
const showConfigOptimizer = ref(false)
const showPricingSimulator = ref(false)
const showCompetitorInsights = ref(false)

// 标签页控制
const activeConfigTab = ref('versions')
const activeOptimizerTab = ref('version')
const activeProductTab = ref('lifecycle')

// 产品生命周期数据
const currentLifecycleStage = ref<'introduction' | 'growth' | 'maturity' | 'decline'>('growth')
const marketMaturity = ref(65)
const growthPotential = ref(3)
const expectedLifecycle = ref(48)

// 配置组合数据
const versionForecast = ref<VersionForecast[]>([])

// 定价策略数据
const priceElasticity = ref(-1.8)
const optimalPrice = ref(258000)
const maxProfitMargin = ref(18.5)
const competitorPricing = ref<CompetitorPricing[]>([])

// 库存规划数据
const safetyStock = ref(2800)
const targetTurnover = ref(8.2)
const replenishmentCycle = ref(21)
const inventoryValue = ref(56000000)
const seasonalInventory = ref<SeasonalInventory[]>([])

// 竞品分析数据
const competitorThreats = ref<CompetitorThreat[]>([])

// 优化器数据
const optimizationTarget = ref('max_profit')
const optimizationConstraints = ref(['production_capacity'])
const optimizationResults = ref<OptimizationResult[] | null>(null)

// 价格模拟器数据
const priceAdjustment = ref(0)
const competitorReaction = ref('partial_follow')
const marketCondition = ref('stable')
const simulationResults = ref<SimulationResult | null>(null)

// 图表实例
const configurationChart = ref<HTMLDivElement>()
const elasticityChart = ref<HTMLDivElement>()
const inventoryChart = ref<HTMLDivElement>()
const competitorChart = ref<HTMLDivElement>()
const simulationChart = ref<HTMLDivElement>()

let configurationChartInstance: echarts.ECharts | null = null
let elasticityChartInstance: echarts.ECharts | null = null
let inventoryChartInstance: echarts.ECharts | null = null
let competitorChartInstance: echarts.ECharts | null = null
let simulationChartInstance: echarts.ECharts | null = null

// 计算属性
const filteredComparisonModels = computed(() => {
  return carModelList.value.filter((model) => model.carModelId !== selectedModel.value)
})

const lifecycleStageText = computed(() => {
  const stageMap = {
    introduction: '导入期',
    growth: '成长期',
    maturity: '成熟期',
    decline: '衰退期',
  }
  return stageMap[currentLifecycleStage.value]
})

const lifecycleDescription = computed(() => {
  const descMap = {
    introduction: '新产品市场认知度较低，销量缓慢增长',
    growth: '市场接受度提升，销量快速增长',
    maturity: '市场趋于饱和，竞争激烈',
    decline: '市场需求下降，考虑产品更新换代',
  }
  return descMap[currentLifecycleStage.value]
})

const competitionIntensity = computed(() => {
  if (marketMaturity.value >= 80) {
    return { type: 'danger', text: '激烈' }
  } else if (marketMaturity.value >= 60) {
    return { type: 'warning', text: '中等' }
  } else {
    return { type: 'success', text: '温和' }
  }
})

const lifecycleSuggestions = computed(() => {
  const suggestionMap = {
    introduction: [
      '加大品牌宣传力度，提升知名度',
      '优化渠道布局，扩大覆盖面',
      '关注用户反馈，快速迭代产品',
    ],
    growth: ['扩大产能，满足增长需求', '增加配置选择，细分市场', '强化售后服务，提升满意度'],
    maturity: ['通过技术升级保持竞争力', '优化成本结构，提升利润', '探索细分市场和差异化定位'],
    decline: ['准备下一代产品替换', '优化库存，控制风险', '考虑退出时机和策略'],
  }
  return suggestionMap[currentLifecycleStage.value]
})

// 工具函数
const getMaturityColor = (value: number) => {
  if (value >= 80) return '#f56c6c'
  if (value >= 60) return '#e6a23c'
  return '#67c23a'
}

// API调用函数
const fetchCarModels = async () => {
  try {
    const response = await axios.get('/api/carModels')
    if (response.data.status === 1) {
      carModelList.value = response.data.data
    } else {
      carModelList.value = generateMockCarModels()
    }
  } catch (error) {
    console.error('获取车型列表失败:', error)
    carModelList.value = generateMockCarModels()
  }
}

const fetchModelAnalysis = async (modelId: number) => {
  try {
    const params = {
      carModelId: modelId,
      period: forecastPeriod.value,
      granularity: forecastGranularity.value,
      comparisonModels: comparisonModels.value,
      dimensions: comparisonDimensions.value,
    }

    const response = await axios.post('/api/model/analysis', params)
    if (response.data.status === 1) {
      return response.data.data
    } else {
      return generateMockModelAnalysis()
    }
  } catch (error) {
    console.error('获取车型分析失败:', error)
    return generateMockModelAnalysis()
  }
}

// 模拟数据生成
const generateMockCarModels = (): CarModel[] => {
  return [
    {
      carModelId: 1,
      modelName: 'Model Y',
      brandName: 'Tesla',
      price: 280000,
      category: 'SUV',
      launchDate: '2021-01-01',
    },
    {
      carModelId: 2,
      modelName: 'Model 3',
      brandName: 'Tesla',
      price: 235000,
      category: 'Sedan',
      launchDate: '2019-02-01',
    },
    {
      carModelId: 3,
      modelName: '汉EV',
      brandName: 'BYD',
      price: 229800,
      category: 'Sedan',
      launchDate: '2020-07-01',
    },
    {
      carModelId: 4,
      modelName: 'ES6',
      brandName: 'NIO',
      price: 358000,
      category: 'SUV',
      launchDate: '2019-06-01',
    },
    {
      carModelId: 5,
      modelName: 'P7',
      brandName: 'XPeng',
      price: 229900,
      category: 'Sedan',
      launchDate: '2020-04-01',
    },
    {
      carModelId: 6,
      modelName: 'Model S',
      brandName: 'Tesla',
      price: 789900,
      category: 'Sedan',
      launchDate: '2016-01-01',
    },
    {
      carModelId: 7,
      modelName: '唐EV',
      brandName: 'BYD',
      price: 279800,
      category: 'SUV',
      launchDate: '2018-12-01',
    },
    {
      carModelId: 8,
      modelName: 'ES8',
      brandName: 'NIO',
      price: 468000,
      category: 'SUV',
      launchDate: '2018-06-01',
    },
    {
      carModelId: 9,
      modelName: 'P5',
      brandName: 'XPeng',
      price: 179900,
      category: 'Sedan',
      launchDate: '2021-09-01',
    },
    {
      carModelId: 10,
      modelName: 'AION Y',
      brandName: 'GAC',
      price: 139600,
      category: 'SUV',
      launchDate: '2021-04-01',
    },
  ]
}

const generateMockModelAnalysis = () => {
  // 生成版本配置预测数据
  versionForecast.value = [
    {
      version: '标准版',
      currentSales: 15600,
      predictedSales: 18200,
      growthRate: 16.7,
      marketShare: 45.2,
      profitMargin: 12.8,
      suggestions: ['主力版本', '稳定增长'],
    },
    {
      version: '豪华版',
      currentSales: 8900,
      predictedSales: 9800,
      growthRate: 10.1,
      marketShare: 24.3,
      profitMargin: 18.5,
      suggestions: ['提升配置', '扩大差异'],
    },
    {
      version: '性能版',
      currentSales: 5200,
      predictedSales: 6800,
      growthRate: 30.8,
      marketShare: 16.9,
      profitMargin: 22.3,
      suggestions: ['高增长', '重点推广'],
    },
    {
      version: '长续航版',
      currentSales: 3400,
      predictedSales: 5500,
      growthRate: 61.8,
      marketShare: 13.6,
      profitMargin: 16.2,
      suggestions: ['快速增长', '加大产能'],
    },
  ]

  // 生成竞品定价数据
  competitorPricing.value = [
    {
      modelName: '理想ONE',
      currentPrice: 338000,
      pricePosition: { type: 'warning', text: '同级' },
      marketResponse: 82,
    },
    {
      modelName: '蔚来ES6',
      currentPrice: 358000,
      pricePosition: { type: 'danger', text: '偏高' },
      marketResponse: 78,
    },
    {
      modelName: '小鹏P7',
      currentPrice: 229900,
      pricePosition: { type: 'success', text: '偏低' },
      marketResponse: 85,
    },
  ]

  // 生成季节性库存数据
  seasonalInventory.value = [
    { month: '1月', demandIndex: 0.85, recommendedStock: 2380, adjustment: '春节淡季，适当减库存' },
    { month: '2月', demandIndex: 0.92, recommendedStock: 2576, adjustment: '市场回暖，逐步增库存' },
    { month: '3月', demandIndex: 1.15, recommendedStock: 3220, adjustment: '购车旺季，增加库存' },
    { month: '4月', demandIndex: 1.08, recommendedStock: 3024, adjustment: '需求稳定，维持库存' },
    { month: '5月', demandIndex: 1.12, recommendedStock: 3136, adjustment: '五一促销，适当增库存' },
    { month: '6月', demandIndex: 1.25, recommendedStock: 3500, adjustment: '年中冲量，大幅增库存' },
  ]

  // 生成竞品威胁数据
  competitorThreats.value = [
    {
      modelId: 101,
      modelName: '理想L9',
      level: 'high',
      levelText: '高威胁',
      marketOverlap: 85,
      priceCompetition: 92,
      expectedImpact: 12.5,
    },
    {
      modelId: 102,
      modelName: '问界M7',
      level: 'medium',
      levelText: '中威胁',
      marketOverlap: 68,
      priceCompetition: 75,
      expectedImpact: 8.3,
    },
    {
      modelId: 103,
      modelName: '极氪001',
      level: 'low',
      levelText: '低威胁',
      marketOverlap: 45,
      priceCompetition: 52,
      expectedImpact: 4.2,
    },
  ]

  return {
    lifecycle: {
      stage: 'growth',
      maturity: 65,
      potential: 3,
      expectedDuration: 48,
    },
    versions: versionForecast.value,
    pricing: {
      elasticity: -1.8,
      optimal: 258000,
      maxMargin: 18.5,
      competitors: competitorPricing.value,
    },
    inventory: {
      safety: 2800,
      turnover: 8.2,
      cycle: 21,
      value: 56000000,
      seasonal: seasonalInventory.value,
    },
    competition: {
      threats: competitorThreats.value,
    },
  }
}

// 事件处理函数
const handleModelChange = () => {
  if (selectedModel.value) {
    modelResults.value = null
    comparisonModels.value = []
    ElMessage.info('车型已选择，点击开始分析按钮进行深度分析')
  }
}

const handlePeriodChange = () => {
  if (modelResults.value) {
    ElMessage.info('预测周期已更改，请重新开始分析')
    modelResults.value = null
  }
}

const handleGranularityChange = () => {
  if (modelResults.value) {
    ElMessage.info('预测粒度已更改，请重新开始分析')
    modelResults.value = null
  }
}

const resetSelection = () => {
  selectedModel.value = null
  comparisonModels.value = []
  comparisonDimensions.value = ['sales', 'price', 'market_share']
  forecastPeriod.value = '12M'
  forecastGranularity.value = 'monthly'
  modelResults.value = null

  // 清理图表
  const chartInstances = [
    configurationChartInstance,
    elasticityChartInstance,
    inventoryChartInstance,
    competitorChartInstance,
  ]

  chartInstances.forEach((instance) => {
    if (instance) {
      instance.clear()
    }
  })

  ElMessage.success('选择已重置')
}

const startModelAnalysis = async () => {
  if (!selectedModel.value) {
    ElMessage.warning('请先选择要分析的车型')
    return
  }

  analyzing.value = true

  try {
    const results = await fetchModelAnalysis(selectedModel.value)
    modelResults.value = results

    // 更新生命周期数据
    currentLifecycleStage.value = results.lifecycle.stage
    marketMaturity.value = results.lifecycle.maturity
    growthPotential.value = results.lifecycle.potential
    expectedLifecycle.value = results.lifecycle.expectedDuration

    // 更新其他数据
    versionForecast.value = results.versions
    priceElasticity.value = results.pricing.elasticity
    optimalPrice.value = results.pricing.optimal
    maxProfitMargin.value = results.pricing.maxMargin
    competitorPricing.value = results.pricing.competitors

    safetyStock.value = results.inventory.safety
    targetTurnover.value = results.inventory.turnover
    replenishmentCycle.value = results.inventory.cycle
    inventoryValue.value = results.inventory.value
    seasonalInventory.value = results.inventory.seasonal

    competitorThreats.value = results.competition.threats

    ElMessage.success('车型分析完成！')

    // 初始化图表
    await nextTick()
    await initAllCharts()
  } catch (error) {
    console.error('车型分析失败:', error)
    ElMessage.error('分析失败，请重试')
  } finally {
    analyzing.value = false
  }
}

const refreshData = async () => {
  loading.value = true
  try {
    await fetchCarModels()
    if (selectedModel.value) {
      await startModelAnalysis()
    }
    ElMessage.success('数据已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

const exportAnalysis = () => {
  if (!modelResults.value) {
    ElMessage.warning('暂无分析结果可导出')
    return
  }

  const selectedModelInfo = carModelList.value.find((m) => m.carModelId === selectedModel.value)
  const csvContent = [
    ['车型分析报告'],
    ['车型', `${selectedModelInfo?.brandName} ${selectedModelInfo?.modelName}`],
    ['分析时间', new Date().toLocaleString()],
    ['预测周期', forecastPeriod.value],
    [''],
    ['生命周期阶段', lifecycleStageText.value],
    ['市场成熟度', `${marketMaturity.value}%`],
    ['增长潜力', `${growthPotential.value}星`],
    ['预期生命周期', `${expectedLifecycle.value}个月`],
    [''],
    ['版本配置分析'],
    ['版本', '当前销量', '预测销量', '增长率', '市场份额', '利润率'],
    ...versionForecast.value.map((v) => [
      v.version,
      v.currentSales,
      v.predictedSales,
      `${v.growthRate}%`,
      `${v.marketShare}%`,
      `${v.profitMargin}%`,
    ]),
  ]
    .map((row) => row.join(','))
    .join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `model_analysis_${selectedModelInfo?.modelName}_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  ElMessage.success('分析报告已导出')
}

const exportConfigReport = () => {
  ElMessage.info('配置报告导出功能开发中...')
}

// 优化器相关函数
const runOptimization = async () => {
  optimizing.value = true

  try {
    // 模拟优化计算
    await new Promise((resolve) => setTimeout(resolve, 2000))

    optimizationResults.value = [
      {
        configuration: '标准版',
        currentRatio: 45.2,
        recommendedRatio: 42.0,
        expectedImpact: -2.1,
        reasoning: '减少标准版占比，为高端版本让出空间',
      },
      {
        configuration: '豪华版',
        currentRatio: 24.3,
        recommendedRatio: 28.0,
        expectedImpact: 5.8,
        reasoning: '增加豪华版产能，提升整体利润率',
      },
      {
        configuration: '性能版',
        currentRatio: 16.9,
        recommendedRatio: 20.0,
        expectedImpact: 12.3,
        reasoning: '性能版需求强劲，建议扩大产能',
      },
      {
        configuration: '长续航版',
        currentRatio: 13.6,
        recommendedRatio: 10.0,
        expectedImpact: -8.5,
        reasoning: '长续航版成本较高，适当减产',
      },
    ]

    ElMessage.success('优化分析完成！')
  } catch (error) {
    ElMessage.error('优化失败')
  } finally {
    optimizing.value = false
  }
}

const applyOptimization = () => {
  if (!optimizationResults.value) return

  ElMessageBox.confirm('确定要应用这些优化建议吗？这将影响生产计划和资源分配。', '确认应用优化', {
    confirmButtonText: '确定应用',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      // 应用优化建议的逻辑
      ElMessage.success('优化建议已应用，请关注后续效果')
      showConfigOptimizer.value = false
    })
    .catch(() => {
      // 用户取消
    })
}

const handleConfigOptimizerClose = () => {
  showConfigOptimizer.value = false
}

// 价格模拟器相关函数
const runPriceSimulation = async () => {
  if (priceAdjustment.value === 0) return

  try {
    // 模拟价格影响计算
    const baseElasticity = priceElasticity.value
    let elasticityMultiplier = 1.0

    // 竞品反应调整
    switch (competitorReaction.value) {
      case 'no_response':
        elasticityMultiplier = 0.8
        break
      case 'partial_follow':
        elasticityMultiplier = 1.0
        break
      case 'full_follow':
        elasticityMultiplier = 1.3
        break
      case 'price_war':
        elasticityMultiplier = 1.8
        break
    }

    // 市场环境调整
    switch (marketCondition.value) {
      case 'boom':
        elasticityMultiplier *= 0.9
        break
      case 'stable':
        elasticityMultiplier *= 1.0
        break
      case 'recession':
        elasticityMultiplier *= 1.2
        break
    }

    const effectiveElasticity = baseElasticity * elasticityMultiplier
    const salesImpact = priceAdjustment.value * effectiveElasticity
    const revenueImpact = priceAdjustment.value + salesImpact
    const profitImpact = revenueImpact * 1.2 // 利润的价格敏感性更高
    const marketShareImpact = salesImpact * 0.8

    simulationResults.value = {
      salesImpact,
      revenueImpact,
      profitImpact,
      marketShareImpact,
    }

    await nextTick()
    initSimulationChart()
  } catch (error) {
    console.error('价格模拟失败:', error)
    ElMessage.error('模拟计算失败')
  }
}

const applyPricingStrategy = () => {
  if (!simulationResults.value) return

  ElMessageBox.confirm(
    `确定要应用这个定价策略吗？价格将调整${priceAdjustment.value}%`,
    '确认定价调整',
    {
      confirmButtonText: '确定应用',
      cancelButtonText: '取消',
      type: 'warning',
    },
  )
    .then(() => {
      ElMessage.success('定价策略已应用')
      showPricingSimulator.value = false
    })
    .catch(() => {
      // 用户取消
    })
}

// 库存相关函数
const generateInventoryPlan = () => {
  ElMessage.info('正在生成智能库存计划...')

  setTimeout(() => {
    ElMessage.success('库存计划已生成，请查看季节性调整建议')
    // 这里可以刷新库存相关的图表和数据
    if (inventoryChartInstance) {
      updateInventoryChart()
    }
  }, 1500)
}

// 竞品分析相关函数
const refreshCompetitorData = async () => {
  ElMessage.info('正在刷新竞品数据...')

  try {
    // 模拟获取最新竞品数据
    await new Promise((resolve) => setTimeout(resolve, 1000))

    // 更新竞品威胁数据
    competitorThreats.value = competitorThreats.value.map((threat) => ({
      ...threat,
      marketOverlap: threat.marketOverlap + (Math.random() - 0.5) * 10,
      priceCompetition: threat.priceCompetition + (Math.random() - 0.5) * 8,
      expectedImpact: threat.expectedImpact + (Math.random() - 0.5) * 3,
    }))

    ElMessage.success('竞品数据已更新')

    if (competitorChartInstance) {
      updateCompetitorChart()
    }
  } catch (error) {
    ElMessage.error('竞品数据更新失败')
  }
}

const showCounterStrategy = (threat: CompetitorThreat) => {
  const strategies = {
    high: ['加强产品差异化', '优化价格策略', '提升服务质量', '加大营销投入'],
    medium: ['监控市场动态', '适度调整策略', '强化优势领域'],
    low: ['保持现有策略', '关注长期发展'],
  }

  const strategy = strategies[threat.level].join('、')

  ElMessageBox.alert(`针对${threat.modelName}的应对策略建议：${strategy}`, '竞品应对策略', {
    confirmButtonText: '了解',
    type: 'info',
  })
}

// 图表初始化函数
const initAllCharts = async () => {
  await Promise.all([
    initConfigurationChart(),
    initElasticityChart(),
    initInventoryChart(),
    initCompetitorChart(),
  ])
}

const initConfigurationChart = async () => {
  if (!configurationChart.value) return

  await nextTick()

  if (configurationChartInstance) {
    configurationChartInstance.dispose()
  }

  configurationChartInstance = echarts.init(configurationChart.value)

  const option = {
    title: {
      text: '版本配置销量预测',
      left: 'center',
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: ['当前销量', '预测销量'],
      top: 30,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: versionForecast.value.map((v) => v.version),
    },
    yAxis: {
      type: 'value',
      name: '销量(台)',
    },
    series: [
      {
        name: '当前销量',
        type: 'bar',
        data: versionForecast.value.map((v) => v.currentSales),
        itemStyle: { color: '#409EFF' },
      },
      {
        name: '预测销量',
        type: 'bar',
        data: versionForecast.value.map((v) => v.predictedSales),
        itemStyle: { color: '#67C23A' },
      },
    ],
  }

  configurationChartInstance.setOption(option)
}

const initElasticityChart = async () => {
  if (!elasticityChart.value) return

  await nextTick()

  if (elasticityChartInstance) {
    elasticityChartInstance.dispose()
  }

  elasticityChartInstance = echarts.init(elasticityChart.value)

  // 生成价格弹性曲线数据
  const pricePoints = []
  const salesPoints = []
  const currentPrice = optimalPrice.value

  for (let i = -30; i <= 30; i += 2) {
    const priceChange = i
    const price = currentPrice * (1 + priceChange / 100)
    const salesChange = priceChange * priceElasticity.value
    const sales = 1000 * (1 + salesChange / 100) // 基准销量1000台

    pricePoints.push(price / 1000) // 转换为千元
    salesPoints.push(Math.max(0, sales))
  }

  const option = {
    title: {
      text: '需求价格弹性曲线',
      left: 'center',
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        return `价格: ${params[0].axisValue}千元<br/>预期销量: ${params[0].value}台`
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      name: '价格(千元)',
      data: pricePoints.map((p) => p.toFixed(0)),
    },
    yAxis: {
      type: 'value',
      name: '预期销量(台)',
    },
    series: [
      {
        name: '需求曲线',
        type: 'line',
        smooth: true,
        data: salesPoints,
        itemStyle: { color: '#E6A23C' },
        lineStyle: { width: 3 },
        markPoint: {
          data: [
            {
              name: '最优点',
              coord: [
                Math.floor(pricePoints.length / 2),
                salesPoints[Math.floor(salesPoints.length / 2)],
              ],
              itemStyle: { color: '#F56C6C' },
            },
          ],
        },
      },
    ],
  }

  elasticityChartInstance.setOption(option)
}

const initInventoryChart = async () => {
  if (!inventoryChart.value) return

  await nextTick()

  if (inventoryChartInstance) {
    inventoryChartInstance.dispose()
  }

  inventoryChartInstance = echarts.init(inventoryChart.value)

  const option = {
    title: {
      text: '季节性库存需求',
      left: 'center',
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: seasonalInventory.value.map((item) => item.month),
    },
    yAxis: [
      {
        type: 'value',
        name: '库存量(台)',
        position: 'left',
      },
      {
        type: 'value',
        name: '需求指数',
        position: 'right',
        min: 0.5,
        max: 1.5,
      },
    ],
    series: [
      {
        name: '建议库存',
        type: 'bar',
        data: seasonalInventory.value.map((item) => item.recommendedStock),
        itemStyle: { color: '#409EFF' },
      },
      {
        name: '需求指数',
        type: 'line',
        yAxisIndex: 1,
        data: seasonalInventory.value.map((item) => item.demandIndex),
        itemStyle: { color: '#E6A23C' },
        lineStyle: { width: 3 },
      },
    ],
  }

  inventoryChartInstance.setOption(option)
}

const updateInventoryChart = () => {
  if (!inventoryChartInstance) return

  // 更新库存图表数据
  inventoryChartInstance.setOption({
    series: [
      {
        data: seasonalInventory.value.map((item) => item.recommendedStock),
      },
      {
        data: seasonalInventory.value.map((item) => item.demandIndex),
      },
    ],
  })
}

const initCompetitorChart = async () => {
  if (!competitorChart.value || comparisonModels.value.length === 0) return

  await nextTick()

  if (competitorChartInstance) {
    competitorChartInstance.dispose()
  }

  competitorChartInstance = echarts.init(competitorChart.value)

  // 模拟竞品对比数据
  const currentModel = carModelList.value.find((m) => m.carModelId === selectedModel.value)
  const comparisonData = [
    {
      name: currentModel?.modelName || '当前车型',
      sales: 15600,
      price: currentModel?.price || 250000,
      satisfaction: 85,
    },
    ...comparisonModels.value.slice(0, 3).map((modelId, index) => {
      const model = carModelList.value.find((m) => m.carModelId === modelId)
      return {
        name: model?.modelName || `竞品${index + 1}`,
        sales: 12000 + Math.random() * 8000,
        price: (model?.price || 200000) + (Math.random() - 0.5) * 50000,
        satisfaction: 75 + Math.random() * 20,
      }
    }),
  ]

  const option = {
    title: {
      text: '竞品对比分析',
      left: 'center',
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: ['销量', '价格', '满意度'],
      top: 30,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '20%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: comparisonData.map((item) => item.name),
    },
    yAxis: [
      {
        type: 'value',
        name: '销量(台)',
        position: 'left',
      },
      {
        type: 'value',
        name: '价格(万元)/满意度',
        position: 'right',
      },
    ],
    series: [
      {
        name: '销量',
        type: 'bar',
        data: comparisonData.map((item) => item.sales),
        itemStyle: { color: '#409EFF' },
      },
      {
        name: '价格',
        type: 'line',
        yAxisIndex: 1,
        data: comparisonData.map((item) => Math.round(item.price / 10000)),
        itemStyle: { color: '#E6A23C' },
      },
      {
        name: '满意度',
        type: 'line',
        yAxisIndex: 1,
        data: comparisonData.map((item) => item.satisfaction),
        itemStyle: { color: '#67C23A' },
      },
    ],
  }

  competitorChartInstance.setOption(option)
}

const updateCompetitorChart = () => {
  if (!competitorChartInstance) return

  // 这里可以更新竞品图表的数据
  // 暂时保持原有逻辑
}

const initSimulationChart = async () => {
  if (!simulationChart.value || !simulationResults.value) return

  await nextTick()

  if (simulationChartInstance) {
    simulationChartInstance.dispose()
  }

  simulationChartInstance = echarts.init(simulationChart.value)

  const results = simulationResults.value
  const data = [
    { name: '销量影响', value: results.salesImpact },
    { name: '收入影响', value: results.revenueImpact },
    { name: '利润影响', value: results.profitImpact },
    { name: '份额影响', value: results.marketShareImpact },
  ]

  const option = {
    title: {
      text: `价格调整${priceAdjustment.value}%的影响`,
      left: 'center',
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
      formatter: '{a} <br/>{b}: {c}%',
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.name),
    },
    yAxis: {
      type: 'value',
      name: '影响(%)',
    },
    series: [
      {
        name: '影响程度',
        type: 'bar',
        data: data.map((item) => ({
          value: item.value,
          itemStyle: {
            color: item.value >= 0 ? '#67C23A' : '#F56C6C',
          },
        })),
      },
    ],
  }

  simulationChartInstance.setOption(option)
}

// 窗口大小调整
const handleResize = () => {
  const chartInstances = [
    configurationChartInstance,
    elasticityChartInstance,
    inventoryChartInstance,
    competitorChartInstance,
    simulationChartInstance,
  ]

  chartInstances.forEach((instance) => {
    if (instance) {
      instance.resize()
    }
  })
}

// 监听对比车型变化
watch(comparisonModels, () => {
  if (modelResults.value && comparisonModels.value.length > 0) {
    nextTick(() => {
      initCompetitorChart()
    })
  }
})

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用车型销售预测系统！')

  try {
    await fetchCarModels()
    window.addEventListener('resize', handleResize)
  } catch (error) {
    console.error('页面初始化失败:', error)
    ElMessage.error('初始化失败，部分功能可能不可用')
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)

  // 销毁所有图表实例
  const chartInstances = [
    configurationChartInstance,
    elasticityChartInstance,
    inventoryChartInstance,
    competitorChartInstance,
    simulationChartInstance,
  ]

  chartInstances.forEach((instance) => {
    if (instance) {
      instance.dispose()
    }
  })
})
</script>

<template>
  <div class="model-sales-forecast">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>车型销售预测</h2>
          <p>基于产品生命周期的精细化车型销售预测与策略优化</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
          <el-button
            type="success"
            :icon="Download"
            @click="exportAnalysis"
            :disabled="!modelResults"
          >
            导出分析
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 车型选择与对比面板 -->
    <el-card shadow="never" class="model-selection-card">
      <template #header>
        <div class="card-header">
          <span>车型选择与对比分析</span>
          <el-button type="text" @click="resetSelection" :icon="RefreshRight">重置选择</el-button>
        </div>
      </template>

      <div class="model-selection-content">
        <!-- 主要分析车型 -->
        <div class="primary-model-section">
          <h4>主要分析车型</h4>
          <el-row :gutter="32">
            <el-col :span="8">
              <el-form-item label="选择车型:">
                <el-select
                  v-model="selectedModel"
                  placeholder="选择要分析的车型"
                  filterable
                  @change="handleModelChange"
                  style="width: 100%"
                >
                  <el-option
                    v-for="model in carModelList"
                    :key="model.carModelId"
                    :label="`${model.brandName} ${model.modelName}`"
                    :value="model.carModelId"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="7">
              <el-form-item label="预测周期:">
                <el-select v-model="forecastPeriod" @change="handlePeriodChange">
                  <el-option label="未来3个月" value="3M" />
                  <el-option label="未来6个月" value="6M" />
                  <el-option label="未来12个月" value="12M" />
                  <el-option label="未来18个月" value="18M" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="5">
              <el-form-item label="预测粒度:">
                <el-radio-group v-model="forecastGranularity" @change="handleGranularityChange">
                  <el-radio-button value="monthly">月度</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="4">
              <el-button
                type="primary"
                @click="startModelAnalysis"
                :loading="analyzing"
                :disabled="!selectedModel"
                style="width: 100%"
              >
                开始分析
              </el-button>
            </el-col>
          </el-row>
        </div>

        <!-- 对比车型选择 -->
        <div class="comparison-models-section" v-if="selectedModel">
          <h4>竞品对比分析</h4>
          <el-row :gutter="16">
            <el-col :span="16">
              <el-form-item label="选择对比车型:">
                <el-select
                  v-model="comparisonModels"
                  multiple
                  placeholder="最多选择3个竞品车型"
                  :max-collapse-tags="2"
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="model in filteredComparisonModels"
                    :key="model.carModelId"
                    :label="`${model.brandName} ${model.modelName}`"
                    :value="model.carModelId"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="对比维度:">
                <el-checkbox-group v-model="comparisonDimensions">
                  <el-checkbox value="sales">销量</el-checkbox>
                  <el-checkbox value="price">价格</el-checkbox>
                  <el-checkbox value="market_share">市场份额</el-checkbox>
                  <el-checkbox value="profit">利润</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>

    <!-- 产品生命周期分析 -->
    <el-row :gutter="20" v-if="modelResults">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="lifecycle-card">
          <template #header>
            <span>产品生命周期分析</span>
          </template>
          <div class="lifecycle-content">
            <!-- 生命周期阶段 -->
            <div class="lifecycle-stage">
              <div class="stage-indicator" :class="currentLifecycleStage">
                <div class="stage-icon">
                  <el-icon v-if="currentLifecycleStage === 'introduction'"><Plus /></el-icon>
                  <el-icon v-else-if="currentLifecycleStage === 'growth'"><TrendCharts /></el-icon>
                  <el-icon v-else-if="currentLifecycleStage === 'maturity'"><DataLine /></el-icon>
                  <el-icon v-else><ArrowDown /></el-icon>
                </div>
                <div class="stage-info">
                  <div class="stage-name">{{ lifecycleStageText }}</div>
                  <div class="stage-description">{{ lifecycleDescription }}</div>
                </div>
              </div>
            </div>

            <!-- 生命周期指标 -->
            <div class="lifecycle-metrics">
              <div class="metric-item">
                <span class="metric-label">市场成熟度:</span>
                <el-progress
                  :percentage="marketMaturity"
                  :color="getMaturityColor(marketMaturity)"
                />
              </div>
              <div class="metric-item">
                <span class="metric-label">增长潜力:</span>
                <el-rate v-model="growthPotential" disabled show-score />
              </div>
              <div class="metric-item">
                <span class="metric-label">竞争激烈度:</span>
                <el-tag :type="competitionIntensity.type">{{ competitionIntensity.text }}</el-tag>
              </div>
              <div class="metric-item">
                <span class="metric-label">预期生命周期:</span>
                <span class="metric-value">{{ expectedLifecycle }} 个月</span>
              </div>
            </div>

            <!-- 阶段建议 -->
            <div class="stage-recommendations">
              <h5>阶段策略建议</h5>
              <ul>
                <li v-for="suggestion in lifecycleSuggestions" :key="suggestion">
                  {{ suggestion }}
                </li>
              </ul>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 配置组合分析 -->
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="configuration-card">
          <template #header>
            <div class="card-header">
              <span>配置组合优化分析</span>
              <div class="header-actions">
                <el-button size="small" @click="showConfigOptimizer = true">配置优化器</el-button>
                <el-button size="small" @click="exportConfigReport">导出配置报告</el-button>
              </div>
            </div>
          </template>

          <div class="configuration-analysis">
            <!-- 配置销量预测图表 -->
            <div class="config-forecast-chart">
              <div ref="configurationChart" class="chart-container" v-loading="analyzing"></div>
            </div>

            <!-- 配置详细分析 -->
            <div class="config-details">
              <el-tabs v-model="activeConfigTab">
                <el-tab-pane label="版本配置" name="versions">
                  <div class="version-analysis">
                    <el-table :data="versionForecast" style="width: 100%">
                      <el-table-column prop="version" label="版本配置" width="150" />
                      <el-table-column prop="currentSales" label="当前销量" width="120">
                        <template #default="{ row }">
                          {{ row.currentSales.toLocaleString() }} 台
                        </template>
                      </el-table-column>
                      <el-table-column prop="predictedSales" label="预测销量" width="120">
                        <template #default="{ row }">
                          {{ row.predictedSales.toLocaleString() }} 台
                        </template>
                      </el-table-column>
                      <el-table-column prop="growthRate" label="增长率" width="100">
                        <template #default="{ row }">
                          <span :class="row.growthRate >= 0 ? 'text-success' : 'text-danger'">
                            {{ row.growthRate >= 0 ? '+' : '' }}{{ row.growthRate.toFixed(1) }}%
                          </span>
                        </template>
                      </el-table-column>
                      <el-table-column prop="marketShare" label="市场份额" width="100">
                        <template #default="{ row }"> {{ row.marketShare.toFixed(1) }}% </template>
                      </el-table-column>
                      <el-table-column prop="profitMargin" label="利润率" width="100">
                        <template #default="{ row }"> {{ row.profitMargin.toFixed(1) }}% </template>
                      </el-table-column>
                      <el-table-column label="优化建议" min-width="200">
                        <template #default="{ row }">
                          <el-tag
                            v-for="tag in row.suggestions"
                            :key="tag"
                            size="small"
                            style="margin-right: 4px"
                          >
                            {{ tag }}
                          </el-tag>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <br>
    
    <!-- 定价策略与库存规划 -->
    <el-row :gutter="20" v-if="modelResults">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="pricing-card">
          <template #header>
            <div class="card-header">
              <span>智能定价策略</span>
              <el-button size="small" type="primary" @click="showPricingSimulator = true">
                价格模拟器
              </el-button>
            </div>
          </template>

          <div class="pricing-analysis">
            <!-- 价格弹性分析 -->
            <div class="price-elasticity">
              <h5>需求价格弹性分析</h5>
              <div class="elasticity-chart">
                <div ref="elasticityChart" class="chart-container" style="height: 280px"></div>
              </div>

              <div class="elasticity-metrics">
                <el-row :gutter="16">
                  <el-col :span="8">
                    <div class="metric-card">
                      <div class="metric-value">{{ priceElasticity.toFixed(2) }}</div>
                      <div class="metric-label">价格弹性系数</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="metric-card">
                      <div class="metric-value">¥{{ optimalPrice.toLocaleString() }}</div>
                      <div class="metric-label">最优定价</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="metric-card">
                      <div class="metric-value">{{ maxProfitMargin.toFixed(1) }}%</div>
                      <div class="metric-label">最大利润率</div>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </div>

            <!-- 竞品定价对比 -->
            <div class="competitor-pricing">
              <h5>竞品定价对比</h5>
              <el-table :data="competitorPricing" size="small">
                <el-table-column prop="modelName" label="车型" width="120" />
                <el-table-column prop="currentPrice" label="当前价格" width="100">
                  <template #default="{ row }"> ¥{{ row.currentPrice.toLocaleString() }} </template>
                </el-table-column>
                <el-table-column prop="pricePosition" label="价格定位" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.pricePosition.type" size="small">
                      {{ row.pricePosition.text }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="marketResponse" label="市场反应" min-width="150">
                  <template #default="{ row }">
                    <el-progress
                      :percentage="row.marketResponse"
                      :stroke-width="6"
                      :show-text="false"
                    />
                    <span style="margin-left: 8px; font-size: 12px">
                      {{ row.marketResponse }}%
                    </span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="inventory-card">
          <template #header>
            <div class="card-header">
              <span>智能库存规划</span>
              <el-button size="small" type="success" @click="generateInventoryPlan">
                生成库存计划
              </el-button>
            </div>
          </template>

          <div class="inventory-planning">
            <!-- 库存预测图表 -->
            <div class="inventory-forecast">
              <div ref="inventoryChart" class="chart-container" style="height: 250px"></div>
            </div>

            <!-- 季节性调整 -->
            <div class="seasonal-adjustment">
              <h5>季节性库存调整</h5>
              <el-table :data="seasonalInventory" size="small">
                <el-table-column prop="month" label="月份" width="80" />
                <el-table-column prop="demandIndex" label="需求指数" width="100">
                  <template #default="{ row }">
                    {{ row.demandIndex.toFixed(2) }}
                  </template>
                </el-table-column>
                <el-table-column prop="recommendedStock" label="建议库存">
                  <template #default="{ row }">
                    {{ row.recommendedStock.toLocaleString() }} 台
                  </template>
                </el-table-column>
                <el-table-column prop="adjustment" label="调整说明" min-width="150" />
              </el-table>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <br>

    <!-- 竞品影响分析 -->
    <el-card
      shadow="never"
      class="competitor-analysis-card"
      v-if="modelResults && comparisonModels.length > 0"
    >
      <template #header>
        <div class="card-header">
          <span>竞品冲击分析</span>
          <div class="header-actions">
            <el-button size="small" @click="refreshCompetitorData">刷新竞品数据</el-button>
            <el-button size="small" @click="showCompetitorInsights = true">深度洞察</el-button>
          </div>
        </div>
      </template>

      <div class="competitor-analysis">
        <!-- 竞品对比图表 -->
        <div class="competitor-comparison">
          <div ref="competitorChart" class="chart-container" style="height: 400px"></div>
        </div>

        <!-- 竞品威胁评估 -->
        <div class="threat-assessment">
          <h5>竞品威胁评估</h5>
          <el-row :gutter="16">
            <el-col :span="8" v-for="threat in competitorThreats" :key="threat.modelId">
              <div class="threat-card" :class="threat.level">
                <div class="threat-header">
                  <span class="threat-title">{{ threat.modelName }}</span>
                  <el-tag :type="threat.level">{{ threat.levelText }}</el-tag>
                </div>
                <div class="threat-content">
                  <div class="threat-metrics">
                    <div class="metric">
                      <span>市场重叠度:</span>
                      <el-progress :percentage="threat.marketOverlap" :stroke-width="6" />
                    </div>
                    <div class="metric">
                      <span>价格竞争度:</span>
                      <el-progress
                        :percentage="threat.priceCompetition"
                        :stroke-width="6"
                        color="#f56c6c"
                      />
                    </div>
                    <div class="metric">
                      <span>预期冲击:</span>
                      <span class="impact-value">-{{ threat.expectedImpact.toFixed(1) }}%</span>
                    </div>
                  </div>
                  <div class="threat-actions">
                    <el-button size="small" @click="showCounterStrategy(threat)">
                      应对策略
                    </el-button>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>

    <!-- 配置优化弹窗 -->
    <el-dialog
      v-model="showConfigOptimizer"
      title="配置组合优化器"
      width="80%"
      :before-close="handleConfigOptimizerClose"
    >
      <div class="config-optimizer-content">
        <el-tabs v-model="activeOptimizerTab">
          <el-tab-pane label="版本优化" name="version">
            <div class="version-optimizer">
              <!-- 版本配置优化界面 -->
              <div class="optimizer-controls">
                <el-row :gutter="16">
                  <el-col :span="8">
                    <el-form-item label="优化目标:">
                      <el-select v-model="optimizationTarget">
                        <el-option label="最大化销量" value="max_sales" />
                        <el-option label="最大化利润" value="max_profit" />
                        <el-option label="最大化市场份额" value="max_share" />
                        <el-option label="均衡发展" value="balanced" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="约束条件:">
                      <el-checkbox-group v-model="optimizationConstraints">
                        <el-checkbox value="production_capacity">产能限制</el-checkbox>
                        <el-checkbox value="cost_control">成本控制</el-checkbox>
                        <el-checkbox value="market_positioning">市场定位</el-checkbox>
                      </el-checkbox-group>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-button type="primary" @click="runOptimization" :loading="optimizing">
                      运行优化
                    </el-button>
                  </el-col>
                </el-row>
              </div>

              <div class="optimization-results" v-if="optimizationResults">
                <h5>优化建议结果</h5>
                <el-table :data="optimizationResults" style="width: 100%">
                  <el-table-column prop="configuration" label="配置" width="150" />
                  <el-table-column prop="currentRatio" label="当前占比" width="100">
                    <template #default="{ row }"> {{ row.currentRatio.toFixed(1) }}% </template>
                  </el-table-column>
                  <el-table-column prop="recommendedRatio" label="建议占比" width="100">
                    <template #default="{ row }"> {{ row.recommendedRatio.toFixed(1) }}% </template>
                  </el-table-column>
                  <el-table-column prop="expectedImpact" label="预期影响" width="120">
                    <template #default="{ row }">
                      <span :class="row.expectedImpact >= 0 ? 'text-success' : 'text-danger'">
                        {{ row.expectedImpact >= 0 ? '+' : '' }}{{ row.expectedImpact.toFixed(1) }}%
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="reasoning" label="优化理由" min-width="200" />
                </el-table>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="价格优化" name="pricing">
            <div class="pricing-optimizer">
              <!-- 价格优化界面 -->
              <div class="price-optimization-content">
                <!-- 价格优化控件和结果展示 -->
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showConfigOptimizer = false">取消</el-button>
          <el-button type="primary" @click="applyOptimization" :disabled="!optimizationResults">
            应用优化建议
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 价格模拟器弹窗 -->
    <el-dialog v-model="showPricingSimulator" title="价格策略模拟器" width="70%">
      <div class="pricing-simulator-content">
        <!-- 价格模拟器界面 -->
        <div class="simulator-controls">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="价格调整:">
                <el-slider
                  v-model="priceAdjustment"
                  :min="-30"
                  :max="30"
                  :step="1"
                  show-input
                  @change="runPriceSimulation"
                />
                <span style="margin-left: 8px">%</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="竞品反应:">
                <el-select v-model="competitorReaction" @change="runPriceSimulation">
                  <el-option label="不跟进" value="no_response" />
                  <el-option label="部分跟进" value="partial_follow" />
                  <el-option label="完全跟进" value="full_follow" />
                  <el-option label="价格战" value="price_war" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="市场环境:">
                <el-select v-model="marketCondition" @change="runPriceSimulation">
                  <el-option label="繁荣期" value="boom" />
                  <el-option label="稳定期" value="stable" />
                  <el-option label="衰退期" value="recession" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="simulation-results" v-if="simulationResults">
          <div ref="simulationChart" class="chart-container" style="height: 350px"></div>

          <div class="simulation-metrics">
            <el-row :gutter="16">
              <el-col :span="6">
                <div class="metric-card">
                  <div class="metric-value">{{ simulationResults.salesImpact.toFixed(1) }}%</div>
                  <div class="metric-label">销量影响</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="metric-card">
                  <div class="metric-value">{{ simulationResults.revenueImpact.toFixed(1) }}%</div>
                  <div class="metric-label">收入影响</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="metric-card">
                  <div class="metric-value">{{ simulationResults.profitImpact.toFixed(1) }}%</div>
                  <div class="metric-label">利润影响</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="metric-card">
                  <div class="metric-value">
                    {{ simulationResults.marketShareImpact.toFixed(1) }}%
                  </div>
                  <div class="metric-label">份额影响</div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showPricingSimulator = false">关闭</el-button>
          <el-button type="primary" @click="applyPricingStrategy" :disabled="!simulationResults">
            应用定价策略
          </el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
.model-sales-forecast {
  padding: 0;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 页面头部 */
.page-header {
  margin-bottom: 20px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
  box-shadow: 0 8px 32px rgba(79, 172, 254, 0.3);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left h2 {
  margin: 0 0 4px 0;
  color: white;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.header-left p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  opacity: 0.9;
  font-weight: 400;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.header-actions .el-button {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  backdrop-filter: blur(15px);
  transition: all 0.3s ease;
  border-radius: 8px;
  font-weight: 500;
}

.header-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

/* 车型选择卡片 */
.model-selection-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.model-selection-content {
  padding: 8px 0;
}

.primary-model-section,
.comparison-models-section {
  padding: 20px 24px;
}

.primary-model-section {
  border-bottom: 1px solid #f0f2f5;
}

.primary-model-section h4,
.comparison-models-section h4 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.primary-model-section h4::before,
.comparison-models-section h4::before {
  content: '';
  width: 4px;
  height: 20px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  margin-right: 12px;
  border-radius: 2px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.header-actions .el-button {
  font-size: 14px;
}

/* 生命周期分析卡片 */
.lifecycle-card {
  height: 100%;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.lifecycle-content {
  padding: 8px 0;
}

.lifecycle-stage {
  margin-bottom: 24px;
}

.stage-indicator {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.stage-indicator::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  opacity: 0.1;
  border-radius: 12px;
}

.stage-indicator.introduction {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stage-indicator.introduction::before {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stage-indicator.growth {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.stage-indicator.growth::before {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stage-indicator.maturity {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.stage-indicator.maturity::before {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stage-indicator.decline {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white;
}

.stage-indicator.decline::before {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stage-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 28px;
  backdrop-filter: blur(10px);
}

.stage-info {
  flex: 1;
}

.stage-name {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stage-description {
  font-size: 14px;
  opacity: 0.9;
  line-height: 1.5;
}

.lifecycle-metrics {
  padding: 0 20px;
}

.lifecycle-metrics .metric-item {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8fafb;
  border-radius: 8px;
  border-left: 4px solid #4facfe;
}

.metric-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.stage-recommendations {
  padding: 20px;
  background: #f8fafb;
  margin: 20px;
  border-radius: 12px;
  border-left: 4px solid #4facfe;
}

.stage-recommendations h5 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.stage-recommendations ul {
  margin: 0;
  padding-left: 20px;
}

.stage-recommendations li {
  margin-bottom: 8px;
  color: #606266;
  line-height: 1.6;
  font-size: 14px;
}

/* 配置组合卡片 */
.configuration-card { 
  height: 100%;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.configuration-analysis {
  padding: 8px 0;
}

.config-forecast-chart {
  padding: 20px;
  border-bottom: 1px solid #f0f2f5;
}

.chart-container {
  width: 100%;
  height: 320px;
  background: white;
  border-radius: 8px;
}

.config-details {
  padding: 20px;
}

.version-analysis .el-table {
  border-radius: 8px;
  overflow: hidden;
}

.version-analysis .el-table th {
  background: #f8fafb;
  color: #606266;
  font-weight: 600;
  font-size: 13px;
}

.text-success {
  color: #67c23a;
  font-weight: 600;
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}

.color-analysis,
.options-analysis {
  padding: 16px 0;
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.option-item {
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.option-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.option-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.option-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.option-metrics {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-metrics .metric {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.option-metrics .metric span {
  color: #909399;
}

.option-metrics .metric strong {
  color: #1a1a1a;
  font-weight: 600;
}

/* 定价策略卡片 */
.pricing-card,
.inventory-card {
  height: 100%;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.pricing-analysis,
.inventory-planning {
  padding: 8px 0;
}

.price-elasticity,
.competitor-pricing,
.inventory-forecast,
.inventory-optimization,
.seasonal-adjustment {
  padding: 20px;
}

.price-elasticity,
.inventory-optimization {
  border-bottom: 1px solid #f0f2f5;
}

.price-elasticity h5,
.competitor-pricing h5,
.inventory-optimization h5,
.seasonal-adjustment h5 {
  margin: 0 0 20px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.price-elasticity h5::before,
.inventory-optimization h5::before {
  content: '';
  width: 4px;
  height: 16px;
  background: #4facfe;
  margin-right: 8px;
  border-radius: 2px;
}

.elasticity-chart {
  margin-bottom: 24px;
}

.elasticity-metrics {
  margin-top: 20px;
}

.metric-card {
  background: #f8fafb;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: #4facfe;
}

.metric-card .metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #1a1a1a;
  margin-bottom: 8px;
  line-height: 1;
}

.metric-card .metric-label {
  font-size: 13px;
  color: #909399;
  font-weight: 500;
}

.competitor-pricing .el-table {
  border-radius: 8px;
  overflow: hidden;
  margin-top: 16px;
}

/* 库存规划 */
.optimization-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.optimization-item {
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.optimization-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: #4facfe;
}

.item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  color: #1a1a1a;
  font-weight: 600;
  font-size: 14px;
}

.item-header .el-icon {
  width: 24px;
  height: 24px;
  color: #4facfe;
}

.item-content {
  text-align: center;
}

.item-value {
  font-size: 20px;
  font-weight: bold;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.item-description {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

/* 竞品分析卡片 */
.competitor-analysis-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
  margin-bottom: 24px;
}

.competitor-analysis {
  padding: 8px 0;
}

.competitor-comparison,
.threat-assessment,

.competitor-comparison {
  border-bottom: 1px solid #f0f2f5;
}

.threat-assessment h5,

.threat-assessment h5::before,

.threat-card {
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.threat-card.high {
  border-left: 4px solid #f56c6c;
  background: #fef0f0;
}

.threat-card.medium {
  border-left: 4px solid #e6a23c;
  background: #fdf6ec;
}

.threat-card.low {
  border-left: 4px solid #67c23a;
  background: #f0f9ff;
}

.threat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.threat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.threat-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.threat-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.threat-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.threat-metrics .metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.threat-metrics .metric span {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.impact-value {
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
}

.threat-actions {
  text-align: center;
}

.insight-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: #f8fafb;
  border-radius: 8px;
  border-left: 4px solid #e6a23c;
}

.insight-icon {
  width: 24px;
  height: 24px;
  color: #e6a23c;
  margin-top: 2px;
}

.insight-content {
  flex: 1;
}

.insight-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.insight-description {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 8px;
}

.insight-action .el-button {
  padding: 4px 12px;
  font-size: 12px;
}

/* 弹窗样式 */
.config-optimizer-content,
.pricing-simulator-content,
.product-config-content {
  max-height: 70vh;
  overflow-y: auto;
}

.optimizer-controls,
.simulator-controls {
  padding: 20px;
  background: #f8fafb;
  border-radius: 8px;
  margin-bottom: 24px;
}

.optimization-results {
  margin-top: 24px;
}

.optimization-results h5 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.optimization-results .el-table {
  border-radius: 8px;
  overflow: hidden;
}

.simulation-results {
  margin-top: 24px;
}

.simulation-metrics {
  margin-top: 20px;
}

.dialog-footer {
  text-align: right;
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;
}

/* 通用表格样式 */
.el-table {
  font-size: 14px;
}

.el-table th {
  background: #f8fafb !important;
  color: #606266 !important;
  font-weight: 600 !important;
}

.el-table td {
  border-bottom: 1px solid #f0f2f5 !important;
}

.el-table tr:hover > td {
  background: #f0f9ff !important;
}

/* 标签样式 */
.el-tag {
  border-radius: 6px;
  font-weight: 500;
  font-size: 12px;
}

.el-tag--success {
  background: #f0f9ff;
  color: #1890ff;
  border-color: #b7eb8f;
}

.el-tag--warning {
  background: #fff7e6;
  color: #fa8c16;
  border-color: #ffd591;
}

.el-tag--danger {
  background: #fff2f0;
  color: #ff4d4f;
  border-color: #ffb3b3;
}

/* 进度条样式 */
.el-progress {
  margin: 4px 0;
}

.el-progress-bar__outer {
  background: #f0f2f5;
  border-radius: 4px;
}

.el-progress-bar__inner {
  border-radius: 4px;
}

/* 按钮样式 */
.el-button {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
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
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
}

.el-button--warning {
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(250, 173, 20, 0.3);
}

/* 输入框样式 */
.el-input__wrapper,
.el-select .el-input__wrapper {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.el-input__wrapper:hover,
.el-select .el-input__wrapper:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.el-input__wrapper.is-focus,
.el-select .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px rgba(79, 172, 254, 0.2);
}

/* 滑块样式 */
.el-slider__runway {
  background: #f0f2f5;
  border-radius: 4px;
}

.el-slider__bar {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border-radius: 4px;
}

.el-slider__button {
  border: 3px solid #4facfe;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* 复选框和单选框样式 */
.el-checkbox__input.is-checked .el-checkbox__inner,
.el-radio__input.is-checked .el-radio__inner {
  background: #4facfe;
  border-color: #4facfe;
}

.el-radio-button__inner {
  border-radius: 8px !important;
  transition: all 0.3s ease;
}

.el-radio-button__original-radio:checked + .el-radio-button__inner {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border-color: #4facfe;
  box-shadow: 0 2px 8px rgba(79, 172, 254, 0.3);
}

/* 评分组件样式 */
.el-rate {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.el-rate__item {
  cursor: default;
}

/* 折叠面板样式 */
.el-collapse {
  border: none;
}

.el-collapse-item__header {
  background: #f8fafb;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 8px;
  font-weight: 600;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.el-collapse-item__header:hover {
  background: #f0f9ff;
  border-color: #4facfe;
}

.el-collapse-item__content {
  padding: 16px 0 0 0;
  border: none;
}

/* 标签页样式 */
.el-tabs__header {
  margin-bottom: 20px;
}

.el-tabs__nav-wrap::after {
  background: #f0f2f5;
}

.el-tabs__item {
  font-weight: 500;
  color: #606266;
  transition: all 0.3s ease;
}

.el-tabs__item:hover {
  color: #4facfe;
}

.el-tabs__item.is-active {
  color: #4facfe;
  font-weight: 600;
}

.el-tabs__active-bar {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  height: 3px;
  border-radius: 2px;
}

/* 动画效果 */
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.lifecycle-card,
.configuration-card,
.pricing-card,
.inventory-card,
.competitor-analysis-card {
  animation: slideInUp 0.6s ease-out;
}

.metric-card,
.option-item,
.optimization-item,
.threat-card,
.insight-item {
  animation: fadeInScale 0.4s ease-out;
}

/* 加载状态 */
.el-loading-mask {
  border-radius: 8px;
  backdrop-filter: blur(2px);
}

.el-loading-spinner .el-loading-text {
  color: #4facfe;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .chart-container {
    height: 280px;
  }

  .header-left h2 {
    font-size: 28px;
  }

  .options-grid {
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  }
}

@media (max-width: 1200px) {
  .chart-container {
    height: 260px;
  }

  .optimization-grid {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }
}

@media (max-width: 768px) {
  .model-sales-forecast {
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
    font-size: 14px;
  }

  .primary-model-section .el-row,
  .comparison-models-section .el-row {
    margin: 0;
  }

  .primary-model-section .el-col,
  .comparison-models-section .el-col {
    margin-bottom: 16px;
  }

  .stage-indicator {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }

  .stage-icon {
    margin-right: 0;
    margin-bottom: 12px;
  }

  .lifecycle-metrics .metric-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .chart-container {
    height: 240px;
  }

  .options-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .optimization-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .elasticity-metrics .el-row,
  .simulation-metrics .el-row {
    margin: 0;
  }

  .elasticity-metrics .el-col,
  .simulation-metrics .el-col {
    margin-bottom: 12px;
  }

  .threat-assessment .el-row {
    margin: 0;
  }

  .threat-assessment .el-col {
    margin-bottom: 16px;
  }

  .threat-card {
    padding: 16px;
  }

  .config-optimizer-content,
  .pricing-simulator-content {
    max-height: 60vh;
  }

  .optimizer-controls .el-row,
  .simulator-controls .el-row {
    margin: 0;
  }

  .optimizer-controls .el-col,
  .simulator-controls .el-col {
    margin-bottom: 16px;
  }
}

@media (max-width: 480px) {
  .header-left h2 {
    font-size: 20px;
  }

  .primary-model-section,
  .comparison-models-section {
    padding: 16px;
  }

  .lifecycle-content,
  .configuration-analysis,
  .pricing-analysis,
  .inventory-planning,
  .competitor-analysis {
    padding: 4px 0;
  }

  .stage-recommendations {
    margin: 16px;
    padding: 16px;
  }

  .price-elasticity,
  .competitor-pricing,
  .inventory-optimization,
  .seasonal-adjustment,
  .competitor-comparison,
  .threat-assessment,

  .chart-container {
    height: 200px;
  }

  .optimization-grid {
    grid-template-columns: 1fr;
  }

  .item-value {
    font-size: 18px;
  }

  .metric-card .metric-value {
    font-size: 20px;
  }

  .stage-name {
    font-size: 20px;
  }

  .stage-icon {
    width: 50px;
    height: 50px;
    font-size: 24px;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .model-sales-forecast {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .model-selection-card,
  .lifecycle-card,
  .configuration-card,
  .pricing-card,
  .inventory-card,
  .competitor-analysis-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .metric-card,
  .option-item,
  .optimization-item,
  .threat-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .stage-recommendations,
  .optimizer-controls,
  .simulator-controls {
    background: #262626;
  }

  .lifecycle-metrics .metric-item,
  .insight-item {
    background: #262626;
  }

  .el-table th {
    background: #262626 !important;
    color: #e4e7ed !important;
  }

  .el-table td {
    background: #2d2d2d !important;
    color: #e4e7ed !important;
    border-bottom-color: #404040 !important;
  }

  .el-table tr:hover > td {
    background: #363636 !important;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .metric-card,
  .option-item,
  .optimization-item,
  .threat-card,
  .insight-item {
    border-width: 2px;
    border-color: #000;
  }

  .metric-value,
  .option-name,
  .threat-title,
  .insight-title,
  .stage-name {
    font-weight: 700;
  }

  .chart-container {
    border: 2px solid #000;
  }
}

/* 打印样式 */
@media print {
  .model-sales-forecast {
    background: white !important;
    color: black !important;
  }

  .header-actions,
  .threat-actions,
  .insight-action,
  .dialog-footer {
    display: none !important;
  }

  .chart-container {
    height: 300px !important;
    break-inside: avoid;
    border: 1px solid #ccc;
  }

  .lifecycle-card,
  .configuration-card,
  .pricing-card,
  .inventory-card,
  .threat-card {
    break-inside: avoid;
    margin-bottom: 20px;
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
.el-input__wrapper:focus-within,
.el-select:focus-within .el-input__wrapper {
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

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
  carModelId: string
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

// 预测结果接口
interface repsonseData {
  historicalData: number[]
  fittedValues: number[]
  forecastValues: number[]
  forecastUpperBounds: number[]
  forecastLowerBounds: number[]
  residuals: number[]
  mape: number
  confidenceLevel: number
  forecastStartIndex: number
  completeTimeSeries: number[]
  completeFittedSeries: number[]
  historicalDataCount: number
  forecastDataCount: number
}

// 可选车型接口
interface carModelOption {
  label: string
  value: string
  launchDate?: string
}

// 查询车型的接口无法接通，先用fake数据
const carModelOptionsFake: carModelOption[] = [
  { label: '宝马X3', value: '1', launchDate: '2020-01-01' },
  { label: '奔驰C200L', value: '2', launchDate: '2021-02-01' },
  { label: '奥迪A4L', value: '3', launchDate: '2019-03-01' },
  { label: '特斯拉Model 3', value: '4', launchDate: '2018-04-01' },
  { label: '比亚迪汉EV', value: '5', launchDate: '2020-05-01' },
]
const carModelOptions = ref<carModelOption[]>([])

// 响应式数据
const loading = ref(false)
const analyzing = ref(false)

// 车型选择与分析
const selectedModel = ref<string>('')
const forecastPeriod = ref('12')
const forecastGranularity = ref('monthly')

// 数据源
const carModelList = ref<CarModel[]>([])
const modelResults = ref<any>(null)

// 弹窗控制
const showPricingSimulator = ref(false)

// 标签页控制
const activeProductTab = ref('lifecycle')

// 定价策略数据
const priceElasticity = ref(-1.8)
const optimalPrice = ref(258000)
const maxProfitMargin = ref(18.5)
const competitorPricing = ref<CompetitorPricing[]>([])

// 价格模拟器数据
const priceAdjustment = ref(0)
const competitorReaction = ref('partial_follow')
const marketCondition = ref('stable')
const simulationResults = ref<SimulationResult | null>(null)

// 图表实例
const forecastChart = ref<HTMLDivElement>()
const elasticityChart = ref<HTMLDivElement>()
const inventoryChart = ref<HTMLDivElement>()
const simulationChart = ref<HTMLDivElement>()

let forecastChartInstance: echarts.ECharts | null = null
let elasticityChartInstance: echarts.ECharts | null = null
let inventoryChartInstance: echarts.ECharts | null = null
let simulationChartInstance: echarts.ECharts | null = null

// 车型销量预测数据
let historySales: number[]
let historyDates: string[]
let historyPeriods: number
let forecastDates: string[]
let forecastPeriods: number
let values: number[]
let upper: number[]
let lower: number[]

// 库存规划数据
let avgSales: number
const replenishmentCycle = ref(21)
const seasonalInventory = ref<SeasonalInventory[]>([])

// 产品生命周期数据
const currentLifecycleStage = ref<'introduction' | 'growth' | 'maturity' | 'decline'>('growth')
const marketMaturity = ref(65)
const growthPotential = ref(3)
const expectedLifecycle = ref(84)

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

// 库存规划工具函数
// 计算安全库存 没有日销数据，以月销数据的标准差代替
function calcSafetyStock(
  salesHistory: number[],
  replenishmentCycleDays: number,
  serviceFactor = 1.65,
) {
  if (!salesHistory || salesHistory.length === 0) return 0
  const avg = salesHistory.reduce((a, b) => a + b, 0) / salesHistory.length
  const std = Math.sqrt(
    salesHistory.reduce((sum, v) => sum + Math.pow(v - avg, 2), 0) / salesHistory.length,
  )
  // 按月为单位，补货周期天数/30
  return Math.round(serviceFactor * std * Math.sqrt(replenishmentCycleDays / 30))
}

// 计算每月建议库存
function fillSeasonalInventoryByForecastPeriods() {
  // 获取当前年月
  const now = new Date()
  let year = now.getFullYear()
  let month = now.getMonth() + 1 // 1~12

  // 生成月份数组
  const months: string[] = []
  for (let i = 0; i < forecastPeriods; i++) {
    let m = month + i
    let y = year
    if (m > 12) {
      y += Math.floor((m - 1) / 12)
      m = ((m - 1) % 12) + 1
    }
    months.push(`${m}月`)
  }

  // 建议调整说明
  const adjustmentMap: Record<number, string> = {
    1: '春节淡季，适当减库存',
    2: '市场回暖，逐步增库存',
    3: '购车旺季，增加库存',
    4: '需求稳定，维持库存',
    5: '五一促销，适当增库存',
    6: '年中冲量，大幅增库存',
    7: '暑期淡季，适当减库存',
    8: '暑期促销，适当增库存',
    9: '金九银十，增加库存',
    10: '金九银十，增加库存',
    11: '年底冲量，适当增库存',
    12: '年终促销，适当增库存',
  }

  // 计算推荐库存和需求指数
  avgSales = Math.round(historySales.reduce((a, b) => a + b, 0) / historySales.length)
  const safety = calcSafetyStock(historySales, replenishmentCycle.value)

  seasonalInventory.value = months.map((m, idx) => {
    // 取月份数字
    const demandIndex = values[idx] / avgSales
    const mNum = parseInt(m)
    return {
      month: m,
      demandIndex: demandIndex,
      recommendedStock: Math.round(safety + values[idx]),
      adjustment: adjustmentMap[mNum] || '维持库存',
    }
  })
}

// 在补货周期或历史销量变化时调用
function onReplenishmentCycleChange() {
  fillSeasonalInventoryByForecastPeriods()
  updateInventoryChart()
}

// 监听补货周期变化是否合理
watch(replenishmentCycle, (val) => {
  if (val < 7) {
    replenishmentCycle.value = 7
    ElMessage.warning('补货周期不能小于7天')
  } else if (val > 90) {
    replenishmentCycle.value = 90
    ElMessage.warning('补货周期不能大于90天')
  }
})

// 生命周期工具函数
// 计算选中车型的 launchDate
const selectedLaunchDate = computed(() => {
  const selected = carModelOptions.value.find((item) => item.value === selectedModel.value)
  return selected?.launchDate || '未知'
})

// 计算生命周期
function calcLifecycleStage(launchDate: string, expectedLifecycle: number = 84) {
  if (!launchDate || !expectedLifecycle) return 'introduction'
  const now = new Date()
  const launch = new Date(launchDate)
  // 已上市月数
  const monthsOnMarket =
    (now.getFullYear() - launch.getFullYear()) * 12 + (now.getMonth() - launch.getMonth())
  // 阶段分割（可根据实际业务调整比例）
  const introEnd = Math.round(expectedLifecycle * 0.15) // 导入期15%
  const growthEnd = Math.round(expectedLifecycle * 0.45) // 成长期30%
  const maturityEnd = Math.round(expectedLifecycle * 0.85) // 成熟期40%
  // 判断阶段
  if (monthsOnMarket < introEnd) currentLifecycleStage.value = 'introduction'
  else if (monthsOnMarket < growthEnd) currentLifecycleStage.value = 'growth'
  else if (monthsOnMarket < maturityEnd) currentLifecycleStage.value = 'maturity'
  else currentLifecycleStage.value = 'decline'

  marketMaturity.value = Math.min(Math.round((monthsOnMarket / expectedLifecycle) * 100), 100)

  if (!historySales || historySales.length < 6) return 1
  const n = historyPeriods
  const recent = historySales.slice(n - 3, n).reduce((a, b) => a + b, 0) / 3
  const prev = historySales.slice(n - 6, n - 3).reduce((a, b) => a + b, 0) / 3
  const rate = (recent - prev) / prev
  if (rate > 0.3) growthPotential.value = 5
  if (rate > 0.15) growthPotential.value = 4
  if (rate > 0.05) growthPotential.value = 3
  if (rate > -0.05) growthPotential.value = 2
  return
}

// 防抖函数
function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number = 300,
  immediate: boolean = false,
): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout> | null = null

  return function (this: unknown, ...args: Parameters<T>) {
    if (timer) clearTimeout(timer)

    if (immediate && !timer) {
      func.apply(this, args)
    }

    timer = setTimeout(() => {
      if (!immediate) {
        func.apply(this, args)
      }
      timer = null
    }, wait)
  }
}

// 异步防抖
function debounceAsync<T extends (...args: any[]) => Promise<any>>(
  fn: T,
  delay = 300,
): (...args: Parameters<T>) => Promise<ReturnType<T>> {
  let timer: ReturnType<typeof setTimeout> | null = null
  let resolveList: ((value: any) => void)[] = []

  return function (...args: Parameters<T>): Promise<ReturnType<T>> {
    return new Promise<ReturnType<T>>((resolve) => {
      if (timer) clearTimeout(timer)
      resolveList.push(resolve)

      timer = setTimeout(async () => {
        const result = await fn(...args)
        resolveList.forEach((r) => r(result))
        resolveList = []
      }, delay)
    })
  }
}

// API调用函数
const fetchCarModels = async (query: string) => {
  try {
    const response = await axios.get('/api/car-models/search', {
      params: { keyword: query, limit: 20 },
    })
    if (response.data.status === 200) {
      carModelOptions.value = [] // 清空之前的选项
      response.data.data.forEach((model: any) => {
        carModelOptions.value.push({
          label: model.modelFullName, //要改，现在品牌和车型分开了，接口和模拟数据不匹配。
          value: model.carModelId.toString(),
          launchDate: model.launchDate,
        })
      })
      console.log('可选车型列表:', carModelOptions.value)
    } else {
      console.error('获取车型列表失败:', response.data.message)
    }
  } catch (error) {
    console.error('获取车型列表失败:', error)
  }
}

// 搜索防抖
const debounceFetchCarModels = debounceAsync(fetchCarModels, 500)

// const fetchCarModels = async (query: string) => {
//   try {
//     const response = await axios.get('/api/car-models/page?=page=1&size=20')
//     if (response.data.status === 200) {
//       response.data.data.forEach((model: any) => {
//         carModelOptions.value.push({
//           label: `${model.modelName}`, //要改，现在品牌和车型分开了，接口和模拟数据不匹配。
//           value: model.carModelId.toString(),
//           launchDate: model.launchDate,
//         })
//       })
//       console.log('获取车型列表成功:', response)
//     } else {
//       console.error('获取车型列表失败:', response.data.message)
//     }
//   } catch (error) {
//     console.error('获取车型列表失败:', error)
//   }
// }

const fetchModelAnalysis = async (modelId: string) => {
  try {
    const params = new URLSearchParams()
    // 直传车型和预测周期参数
    params.append('carModelId', selectedModel.value.toString())
    params.append('months', forecastPeriod.value.toString())

    // const params = {
    //   carModelId: modelId,
    //   period: forecastPeriod.value,
    //   granularity: forecastGranularity.value,
    // }

    const response = await axios.get(`/api/prediction/ARIMA/detail?${params.toString()}`)
    if (response.data.status === 200) {
      console.log('获取车型分析数据:', response)
      return response.data.data
    } else {
      console.error('获取车型分析失败:', response.data.message)
    }
  } catch (error) {
    console.error('获取车型分析失败:', error)
  }
}

// 事件处理函数
const handleModelChange = () => {
  if (selectedModel.value) {
    modelResults.value = null
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
  selectedModel.value = ''
  forecastPeriod.value = '12'
  forecastGranularity.value = 'monthly'
  modelResults.value = null

  // 清理图表
  const chartInstances = [elasticityChartInstance, inventoryChartInstance]

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
    const res = await fetchModelAnalysis(selectedModel.value)

    console.log('车型分析结果:', res)
    if (res) {
      const results: repsonseData = res
      modelResults.value = results

      // 车型销量预测数据
      historySales = results.historicalData
      historyPeriods = results.historicalDataCount
      forecastPeriods = results.forecastDataCount
      values = results.forecastValues
      upper = results.forecastUpperBounds
      lower = results.forecastLowerBounds
      historyDates = generateTimeSeries('2025/06', -historyPeriods)
      forecastDates = generateTimeSeries('2025/07', forecastPeriods)

      // 更新库存需求数据
      avgSales = Math.round(historySales.reduce((a, b) => a + b, 0) / historySales.length)
      fillSeasonalInventoryByForecastPeriods()

      // 更新生命周期数据
      calcLifecycleStage(selectedLaunchDate.value, expectedLifecycle.value)

      // currentLifecycleStage.value = results.lifecycle.stage
      // marketMaturity.value = results.lifecycle.maturity
      // growthPotential.value = results.lifecycle.potential
      // expectedLifecycle.value = results.lifecycle.expectedDuration

      // // 更新其他数据
      // priceElasticity.value = results.pricing.elasticity
      // optimalPrice.value = results.pricing.optimal
      // maxProfitMargin.value = results.pricing.maxMargin
      // competitorPricing.value = results.pricing.competitors

      // safetyStock.value = results.inventory.safety
      // targetTurnover.value = results.inventory.turnover
      // replenishmentCycle.value = results.inventory.cycle
      // inventoryValue.value = results.inventory.value
      // seasonalInventory.value = results.inventory.seasonal

      ElMessage.success('车型分析完成！')

      // 初始化图表
      await nextTick()
      await initAllCharts()
    }
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
    // await fetchCarModels()
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

// 计算日期
function generateTimeSeries(baseTime: string, period: number): string[] {
  // 验证输入格式
  if (!/^\d{4}\/\d{2}$/.test(baseTime)) {
    throw new Error('基准时间格式不正确，应为 "yyyy/mm"')
  }

  const [yearStr, monthStr] = baseTime.split('/')
  let year = parseInt(yearStr, 10)
  let month = parseInt(monthStr, 10)

  // 验证月份是否有效
  if (month < 1 || month > 12) {
    throw new Error('月份必须在 1-12 之间')
  }

  const result: string[] = []
  const direction = period > 0 ? 1 : -1
  const count = Math.abs(period)

  for (let i = 0; i < count; i++) {
    // 计算当前月份和年份
    let currentMonth = month
    let currentYear = year

    if (direction > 0) {
      // 正向计算
      currentMonth += i
      currentYear += Math.floor((currentMonth - 1) / 12)
      currentMonth = ((currentMonth - 1) % 12) + 1
    } else {
      // 反向计算
      currentMonth -= i
      // 处理跨年
      while (currentMonth < 1) {
        currentMonth += 12
        currentYear -= 1
      }
    }

    // 格式化月份为两位数
    const formattedMonth = currentMonth.toString().padStart(2, '0')
    result.push(`${currentYear}/${formattedMonth}`)
  }

  return result.reverse()
}

// 图表初始化函数
const initAllCharts = async () => {
  await Promise.all([initForecastChart(), initElasticityChart(), initInventoryChart()])
}

const initForecastChart = async () => {
  if (!forecastChart.value || !modelResults.value) return

  await nextTick()

  if (forecastChartInstance) {
    forecastChartInstance.dispose()
  }
  forecastChartInstance = echarts.init(forecastChart.value)

  // 假设 modelResults.value.forecast 为 [{date, value, upper, lower}]
  // 如无真实数据，可用模拟数据
  // const forecastData = modelResults.value.forecast || [
  //   { date: '2023-01', value: 1200, upper: 1300, lower: 1100 },
  //   { date: '2023-02', value: 1350, upper: 1450, lower: 1250 },
  //   { date: '2023-03', value: 1500, upper: 1600, lower: 1400 },
  //   { date: '2023-04', value: 1700, upper: 1800, lower: 1600 },
  //   { date: '2023-05', value: 1600, upper: 1700, lower: 1500 },
  //   { date: '2023-06', value: 1550, upper: 1650, lower: 1450 },
  // ]
  // const historyData =[
  //   {"date": "2022-01", "value": 932},
  //   {"date": "2022-02", "value": 1045},
  //   {"date": "2022-03", "value": 876},
  //   {"date": "2022-04", "value": 1152},
  //   {"date": "2022-05", "value": 987},
  //   {"date": "2022-06", "value": 1103},
  //   {"date": "2022-07", "value": 821},
  //   {"date": "2022-08", "value": 1176},
  //   {"date": "2022-09", "value": 899},
  //   {"date": "2022-10", "value": 1088},
  //   {"date": "2022-11", "value": 954},
  //   {"date": "2022-12", "value": 1192}
  // ]
  // const historySales = historyData.map((d: { date: string; value: number }) => d.value)
  // const historyDates = historyData.map((d: { date: string }) => d.date)
  // const forecastDates = forecastData.map((d: { date: string; value: number; upper: number; lower: number }) => d.date)
  // const values = forecastData.map((d: { date: string; value: number; upper: number; lower: number }) => d.value)
  // const upper = forecastData.map((d: { date: string; value: number; upper: number; lower: number }) => d.upper)
  // const lower = forecastData.map((d: { date: string; value: number; upper: number; lower: number }) => d.lower)

  // // 为预测数据添加前导null来正确对其
  // const forecastSales = new Array(historySales.length).fill(null).concat(values)
  // const forecastUpper = new Array(historySales.length).fill(null).concat(upper)
  // const forecastLower = new Array(historySales.length).fill(null).concat(lower)

  const forecastSales = new Array(historyPeriods).fill(null).concat(values)
  const forecastUpper = new Array(historyPeriods).fill(null).concat(upper)
  const forecastLower = new Array(historyPeriods).fill(null).concat(lower)

  const option = {
    title: {
      text: '车型销量预测',
      left: 'center',
      textStyle: { fontSize: 16, fontWeight: 'bold', color: '#333' },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        let result = `<div style="font-weight: bold; margin-bottom: 6px;">${params[0].axisValue}</div>`
        params.forEach((param: any) => {
          if (param.value !== null && param.value !== undefined) {
            const color = param.color
            const seriesName = param.seriesName
            const value = param.value.toLocaleString()
            result += `<div style="margin-bottom: 3px;">
              <span style="display: inline-block; width: 10px; height: 10px; background: ${color}; border-radius: 50%; margin-right: 6px;"></span>
              ${seriesName}: <strong>${value}</strong> 台
            </div>`
          }
        })
        return result
      },
    },
    legend: {
      data: ['预测销量', '置信区间'],
      top: 30,
      textStyle: { fontSize: 12 },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '20%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: historyDates.concat(forecastDates),
      axisLabel: { fontSize: 10, rotate: 45 },
      axisTick: { alignWithLabel: true },
    },
    yAxis: {
      type: 'value',
      name: '销量(台)',
      nameLocation: 'middle',
      nameGap: 50,
      axisLabel: {
        formatter: (value: number) =>
          value >= 1000 ? (value / 1000).toFixed(1) + 'k' : value.toString(),
      },
      splitLine: { lineStyle: { type: 'dashed', color: '#e0e6ed' } },
    },
    series: [
      {
        name: '历史销量',
        type: 'line',
        data: historySales,
        itemStyle: {
          color: '#409EFF',
        },
        lineStyle: {
          width: 2,
          type: 'dashed',
        },
        symbol: 'circle',
        symbolSize: 4,
      },
      {
        name: '预测销量',
        type: 'line',
        data: forecastSales,
        itemStyle: {
          color: '#E6A23C',
        },
        lineStyle: {
          width: 3,
        },
        symbol: 'diamond',
        symbolSize: 6,
      },
      {
        name: '置信区间',
        type: 'line',
        data: forecastUpper,
        lineStyle: { opacity: 0 },
        symbol: 'none',
        stack: 'confidence-band',
        areaStyle: { color: 'rgba(230, 162, 60, 0.3)' },
      },
      {
        name: '置信区间下限',
        type: 'line',
        data: forecastLower,
        lineStyle: { opacity: 0 },
        symbol: 'none',
        stack: 'confidence-band',
        areaStyle: { color: 'rgba(255,255,255,0.8)' },
        showInLegend: false,
      },
    ],
  }

  forecastChartInstance.setOption(option)
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
        max: 3,
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
  const chartInstances = [elasticityChartInstance, inventoryChartInstance, simulationChartInstance]

  chartInstances.forEach((instance) => {
    if (instance) {
      instance.resize()
    }
  })
}

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用车型销售预测系统！')

  try {
    // await fetchCarModels()
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
    forecastChartInstance,
    elasticityChartInstance,
    inventoryChartInstance,
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
        <div class="card-header-colored">
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
                  remote
                  :remote-method="debounceFetchCarModels"
                  @change="handleModelChange"
                  style="width: 100%"
                >
                  <!-- 使用 carModelOptionsFake 代替 carModelOptions -->
                  <el-option
                    v-for="model in carModelOptions"
                    :key="model.label"
                    :label="model.label"
                    :value="model.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="7">
              <el-form-item label="预测周期:">
                <el-select v-model="forecastPeriod" @change="handlePeriodChange">
                  <el-option label="未来3个月" value="3" />
                  <el-option label="未来6个月" value="6" />
                  <el-option label="未来12个月" value="12" />
                  <el-option label="未来18个月" value="18" />
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
      </div>
    </el-card>

    <!-- 产品生命周期分析与预测结果分析 -->
    <el-row :gutter="20" v-if="modelResults">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="lifecycle-card">
          <template #header>
            <div class="card-header-colored">
              <span>产品生命周期分析</span>
            </div>
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

      <!-- 预测结果 -->
      <el-col :xs="24" :lg="16">
        <!-- 新增生命周期预测图表 -->
        <el-card shadow="never" class="result-card" style="margin-bottom: 20px">
          <template #header>
            <div class="card-header-colored">
              <span>车型销量预测</span>
              <!-- <div class="result-actions">
                <el-button
                  type="text"
                  @click="toggleDataLabels"
                  :icon="View"
                >
                  {{ showDataLabels ? '隐藏' : '显示' }}数据标签
                </el-button>
                <el-button
                  v-if="predictionResults"
                  type="text"
                  @click="showBusinessInsights = true"
                  :icon="DataAnalysis"
                >
                  业务洞察
                </el-button>
              </div> -->
            </div>
          </template>
          <div ref="forecastChart" class="forecast-chart"></div>
        </el-card>
        <!-- ...原有配置组合分析内容... -->
      </el-col>
    </el-row>
    <br />

    <!-- 定价策略与库存规划 -->
    <el-row :gutter="20" v-if="modelResults">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="pricing-card">
          <template #header>
            <div class="card-header-colored">
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
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="inventory-card">
          <template #header>
            <div class="card-header-colored">
              <span>智能库存规划</span>
            </div>
          </template>

          <div class="inventory-planning">
            <!-- 新增补货周期输入 -->
            <el-form-item label="补货周期(天):" style="max-width: 220px">
              <el-input-number
                v-model="replenishmentCycle"
                :min="7"
                :max="90"
                :step="1"
                :controls="false"
                placeholder="补货周期"
                style="width: 120px"
                @change="onReplenishmentCycleChange"
              />
            </el-form-item>
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
    <br />

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

.el-card {
  border-radius: 16px !important;
  box-shadow: 0 8px 32px rgba(79, 172, 254, 0.1) !important;
  border: none !important;
}

.card-header-colored {
  display: flex;
  align-items: center;
  font-weight: 600;
  font-size: 18px;
  color: #fff;
  background: linear-gradient(90deg, #4facfe 0%, #00f2fe 100%);
  border-radius: 12px 12px 0 0;
  padding: 16px 28px;
  box-shadow: 0 2px 8px rgba(79, 172, 254, 0.1);
  letter-spacing: 1px;
  min-height: 48px;
  position: relative;
  overflow: hidden;
}

/* 为不同业务块定制不同色彩（如下） */
.lifecycle-card .card-header-colored {
  background: linear-gradient(90deg, #f093fb 0%, #f5576c 100%);
}

.pricing-card .card-header-colored {
  background: linear-gradient(90deg, #52c41a 0%, #73d13d 100%);
}

.inventory-card .card-header-colored {
  background: linear-gradient(90deg, #faad14 0%, #ffc53d 100%);
}

.competitor-analysis-card .card-header-colored {
  background: linear-gradient(90deg, #4facfe 0%, #00f2fe 100%);
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

.chart-container {
  width: 100%;
  height: 320px;
  background: white;
  border-radius: 8px;
}

.forecast-chart {
  width: 100%;
  height: 520px;
  min-height: 520px;
  background: white;
  border-radius: 8px;
  padding: 16px;
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
  background: #f0f9ff !important;
  color: #409eff !important;
  font-weight: 700 !important;
  font-size: 15px !important;
}
.el-table td {
  background: #fff !important;
  color: #333 !important;
  font-size: 14px !important;
}
.el-table tr:hover > td {
  background: #e6f7ff !important;
}

/* 季节性库存调整表格美化 */
.seasonal-adjustment .el-table {
  border-radius: 10px;
  overflow: hidden;
  margin-top: 12px;
  background: #f8fafb;
  box-shadow: 0 2px 12px rgba(79, 172, 254, 0.06);
  border: 1px solid #e8eaed;
}

.seasonal-adjustment .el-table th {
  background: linear-gradient(90deg, #faad14 0%, #ffc53d 100%) !important;
  color: #fff !important;
  font-weight: 700 !important;
  font-size: 15px !important;
  letter-spacing: 1px;
}

.seasonal-adjustment .el-table td {
  background: #fff !important;
  color: #333 !important;
  font-size: 14px !important;
  border-bottom: 1px solid #f0f2f5 !important;
}

.seasonal-adjustment .el-table tr:hover > td {
  background: #fffbe6 !important;
  transition: background 0.2s;
}

.seasonal-adjustment .el-table .el-table__cell {
  padding: 10px 8px !important;
}

.seasonal-adjustment .el-table-column--highlight {
  background: #fffbe6 !important;
  color: #faad14 !important;
  font-weight: bold;
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
  .forecast-chart {
    height: 450px;
    min-height: 450px;
  }

  .chart-container {
    height: 260px;
  }

  .optimization-grid {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }
}

@media (max-width: 768px) {
  .forecast-chart {
    height: 380px;
    min-height: 380px;
    padding: 8px;
  }

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
  .forecast-chart {
    height: 300px;
    min-height: 300px;
    padding: 4px;
  }

  .header-left h2 {
    font-size: 20px;
  }

  .primary-model-section,
  .comparison-models-section {
    padding: 16px;
  }

  .lifecycle-content,
  .pricing-analysis,
  .inventory-planning,
  .competitor-analysis {
    background: #f8fafb;
    border-radius: 0 0 12px 12px;
    padding: 24px 20px;
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
  .forecast-chart {
    height: 400px !important;
    break-inside: avoid;
  }

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

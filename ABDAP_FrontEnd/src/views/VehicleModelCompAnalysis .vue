<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  Delete,
  Trophy,
  Check,
  Star,
  Money,
  Lightning,
  MagicStick,
  OfficeBuilding,
  Lock,
  Monitor,
  CircleCheckFilled,
  WarningFilled,
  Coin,
  Link,
  Share,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

// 接口定义
interface CarModel {
  id: number
  name: string
  brand: string
  type: string
  engine: string
  priceRange: string
  image: string
  isRecommended?: boolean

  // 对比参数
  price: number
  power: number
  control: number
  comfort: number
  appearance: number
  config: number

  // 销量数据
  sales: number
  rating: number
  totalScore?: number
  ratingStars?: number
}

interface SearchResult {
  id: number
  name: string  // 实际存储modelFullName
  brand: string  // 实际存储modelName
  priceRange: string
}

interface WeightConfig {
  price: number
  power: number
  control: number
  comfort: number
  appearance: number
  config: number
}

interface ComparisonTableRow {
  dimension: string
  label: string
  unit?: string
  [key: string]: any
}

interface HotCarModel {
  id: number
  name: string
  brand: string
  priceRange: string
  image: string
  sales: number
  rating: number
}

// 响应式数据
const loading = ref(false)
const searching = ref(false)

// 车型选择相关
const selectedModels = ref<CarModel[]>([])
const searchResults = ref<SearchResult[]>([])
const newModelId = ref<number | null>(null)
const userScenario = ref('family')

// 权重配置
const customWeights = ref<WeightConfig>({
  price: 0.25,
  power: 0.15,
  control: 0.2,
  comfort: 0.15,
  appearance: 0.15,
  config: 0.1,
})

// 对比结果数据
const comparisonData = ref<any>(null)
const showAllDimensions = ref(false)

// 图表相关
const radarChart = ref<HTMLDivElement>()
const barChart = ref<HTMLDivElement>()
const radarViewType = ref<'score' | 'raw'>('score')
const barChartDimension = ref('price')

let radarChartInstance: echarts.ECharts | null = null
let barChartInstance: echarts.ECharts | null = null

// 弹窗控制
const showHotListDialog = ref(false)
const showShareDialog = ref(false)
const hotCarList = ref<HotCarModel[]>([])

// 计算属性
const totalWeight = computed(() => {
  const weights = customWeights.value
  return Math.round(
    (weights.price +
      weights.power +
      weights.control +
      weights.comfort +
      weights.appearance +
      weights.config) *
      100,
  )
})

const rankedModels = computed(() => {
  if (!selectedModels.value.length || !comparisonData.value) return []

  return selectedModels.value
    .map((model) => ({
      ...model,
      totalScore: calculateModelScore(model),
      ratingStars: Math.round(model.rating),
    }))
    .sort((a, b) => b.totalScore - a.totalScore)
    .map((model, index) => ({
      ...model,
      isRecommended: index === 0,
    }))
})

const comparisonTableData = computed(() => {
  if (!selectedModels.value.length) return []

  const basicDimensions = [
    { dimension: 'price', label: '价格成本', unit: '万元' },
    { dimension: 'power', label: '动力性能', unit: '分' },
    { dimension: 'control', label: '操控性', unit: '分' },
    { dimension: 'comfort', label: '舒适度', unit: '分' },
    { dimension: 'appearance', label: '外观', unit: '分' },
    { dimension: 'config', label: '科技配置', unit: '分' },
  ]

  const detailedDimensions = [
    { dimension: 'acceleration', label: '0-100km/h加速', unit: '秒' },
    { dimension: 'fuelConsumption', label: '油耗/电耗', unit: 'L/100km' },
    { dimension: 'wheelbase', label: '轴距', unit: 'mm' },
    { dimension: 'airbags', label: '安全气囊数量', unit: '个' },
    { dimension: 'screenSize', label: '中控屏尺寸', unit: '英寸' },
  ]

  return showAllDimensions.value ? [...basicDimensions, ...detailedDimensions] : basicDimensions
})

// 场景权重配置
const scenarioWeights: Record<string, WeightConfig> = {
  family: {
    appearance: 0.3,
    comfort: 0.3,
    control: 0.25,
    price: 0.15,
    power: 0.0,
    config: 0.0,
  },
  commute: { control: 0.4, price: 0.3, comfort: 0.2, power: 0.1, appearance: 0.0, config: 0.0 },
  business: {
    config: 0.25,
    power: 0.25,
    appearance: 0.25,
    comfort: 0.15,
    control: 0.05,
    price: 0.05,
  },
}

// 工具函数
const calculateModelScore = (model: CarModel): number => {
  const weights = customWeights.value
  let topPrice = 50 // 评分价格上限设为50万
  let lowPrice = 10 // 评分价格下限设为0万
  const normalizedPrice: number =Math.min(Math.max(0, 100 - (model.price - lowPrice)/(topPrice - lowPrice) * 100), 100) // 价格转换为评分 // 价格越低得分越高

  return (
    normalizedPrice * weights.price +
    model.power * weights.power +
    model.control * weights.control +
    model.comfort * weights.comfort +
    model.appearance * weights.appearance +
    model.config * weights.config
  )
}

const isModelSelected = (modelId: number): boolean => {
  return selectedModels.value.some((model) => model.id === modelId)
}

const getScenarioLabel = (scenario: string): string => {
  const labels = {
    family: '家庭用车',
    commute: '城市通勤',
    business: '商务出行',
  }
  return labels[scenario as keyof typeof labels] || scenario
}

const getRankingTagType = (scenario: string) => {
  const types = {
    family: 'success',
    commute: 'warning',
    business: 'primary',
  }
  return types[scenario as keyof typeof types] || 'info'
}

const getRecommendationTagType = () => {
  return getRankingTagType(userScenario.value)
}

const getDimensionIcon = (dimension: string) => {
  const icons: Record<
    'price' | 'power' | 'control' | 'comfort' | 'appearance' | 'config',
    any
  > = {
    price: Money,
    power: Lightning,
    control: MagicStick,
    comfort: OfficeBuilding,
    appearance: Lock,
    config: Monitor,
  }
  return icons[dimension as keyof typeof icons] || Star
}

const getBestValueClass = (row: ComparisonTableRow, modelId: number) => {
  return isBestValue(row, modelId) ? 'best-value' : ''
}

const isBestValue = (row: ComparisonTableRow, modelId: number): boolean => {
  const values = selectedModels.value.map((model) => getParameterRawValue(row, model.id))
  const currentValue = getParameterRawValue(row, modelId)

  // 价格维度：越低越好
  if (row.dimension === 'price') {
    return currentValue === Math.min(...values)
  }
  // 其他维度：越高越好
  return currentValue === Math.max(...values)
}

const getParameterValue = (row: ComparisonTableRow, modelId: number): string => {
  const rawValue = getParameterRawValue(row, modelId)
  return rawValue.toString()
}

const getParameterRawValue = (row: ComparisonTableRow, modelId: number): number => {
  const model = selectedModels.value.find((m) => m.id === modelId)
  if (!model) return 0

  const value = model[row.dimension as keyof typeof  model]
  return typeof value === 'number' ? value : 0
}

const getModelScore = (modelId: number): number => {
  const model = selectedModels.value.find((m) => m.id === modelId)
  return model ? calculateModelScore(model) : 0
}

const getModelPros = (modelId: number): string[] => {
  const model = selectedModels.value.find((m) => m.id === modelId)
  if (!model) return []

  const pros = []
  if (model.power >= 80) pros.push('动力性能出色')
  if (model.control >= 85) pros.push('可操控性强')
  if (model.comfort >= 80) pros.push('空间宽敞舒适')
  if (model.appearance >= 90) pros.push('型格兼备，外观出众')
  if (model.config >= 85) pros.push('科技配置丰富先进')
  if (model.price <= 300000) pros.push('价格具有竞争力')

  return pros.slice(0, 3)
}

const getModelCons = (modelId: number): string[] => {
  const model = selectedModels.value.find((m) => m.id === modelId)
  if (!model) return []

  const cons = []
  if (model.power < 60) cons.push('动力表现一般')
  if (model.control < 60) cons.push('操控性一般')
  if (model.comfort < 60) cons.push('空间相对局促，舒适性不足')
  if (model.appearance < 70) cons.push('外观缺乏特色')
  if (model.config < 60) cons.push('科技配置较少')
  if (model.price > 500000) cons.push('价格偏高')

  return cons.slice(0, 2)
}

const getTargetAudience = (modelId: number): string => {
  const model = selectedModels.value.find((m) => m.id === modelId)
  if (!model) return ''

  if (model.price > 500000) return '追求品质的高端用户'
  if (model.comfort >= 85 ) return '注重舒适度的家庭用户'
  if (model.appearance >= 85) return '喜欢个性化的时尚用户'
  if (model.power >= 85) return '注重强大动力的性能用户'
  if (model.control >= 85) return '追求驾驶乐趣的年轻用户'
  if (model.config >= 85) return '喜欢科技配置的时尚用户'

  return '追求均衡表现的主流用户'
}

const getBestChoice = () => {
  return rankedModels.value[0] || selectedModels.value[0]
}

const getBestChoiceReason = (): string => {
  const best = getBestChoice()
  if (!best) return ''

  const scenario = userScenario.value
  if (scenario === 'family') return '在外观性和舒适度表现方面最为出色，非常适合家庭使用'
  if (scenario === 'commute') return '在经济性和舒适度方面表现突出，是通勤出行的理想选择'
  if (scenario === 'business') return '在品质和科技配置方面领先，彰显商务品味'

  return '综合表现最为均衡，性价比突出'
}

const getBudgetChoice = () => {
  return rankedModels.value.find((model) => model.price < 300000)
}

const getBudgetChoiceReason = (): string => {
  return '价格亲民，性价比出色'
}

const getPremiumChoice = () => {
  return rankedModels.value.find((model) => model.price > 400000)
}

const getPremiumChoiceReason = (): string => {
  return '品质卓越，配置丰富'
}

// 防抖函数
function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number = 300,
  immediate: boolean = false
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
  delay = 300
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


// API 调用函数
const searchModels = async (query: string) => {
  if (!query) {
    searchResults.value = []
    return
  }

  searching.value = true
  try {
    const response = await axios.get('/api/car-models/search', { params: { keyword: query, limit: 20 } })
    if (response.data.status === 200 && response.data.data) {
      // searchResults.value = generateMockSearchResults(query)
      searchResults.value = response.data.data.map((item: any) => ({
        id: item.carModelId,
        name: item.modelFullName,
        brand: item.modelName,
        priceRange: item.officialPrice,
      }))
    } else {
      searchResults.value = generateMockSearchResults(query)
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('搜索车型失败:', error)
    ElMessage.error('搜索车型数据失败')
    searchResults.value = generateMockSearchResults(query)
  } finally {
    searching.value = false
  }
}

// 搜索防抖
const debounceSearchModels = debounceAsync(searchModels, 500)

const fetchHotCarList = async () => {
  try {
    // 现在使用生成数据，时间定为2024-12到2025-05；真实数据库应当是2025-05到2025-05
    const response = await axios.get('/api/ranking/sales', { params: { startMonth: "2024-12", endMonth: "2025-05", region: "all", top: 30 } })
    if (response.data.status === 200 && response.data.data) {
      hotCarList.value = response.data.data.map((item: any) => ({
        id: item.carModelId,
        name: item.modelName, // 接通真是数据库再决定带不带版本名
        brand: item.brandName,
        priceRange: item.officialPrice,
        image: item.imageUrl || `https://picsum.photos/300/200?random=${item.id}`,
        sales: item.saleCount,
        rating: item.opinionScore,
      }))
    } else {
      hotCarList.value = generateMockHotCarList()
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('获取热门车型失败:', error)
        ElMessage.error('搜索热门车型数据失败')
    hotCarList.value = generateMockHotCarList()
  }
}

const fetchModelDetails = async (modelId: number): Promise<CarModel | null> => {
  try {
    const response = await axios.get(`/api/car-models/detail/${modelId}`)
    if (response.data.status === 200 && response.data.data) {
      console.log('获取车型详情:', response)
      const res = response.data.data
      const modelData: CarModel = {
        id: res.carModelId,
        name: res.modelFullName,
        brand: res.modelName,
        type: res.level,
        engine: res.engineType,
        priceRange: res.officialPrice,
        image: res.imageUrl,

        price: res.officialPrice,
        power: Math.round(res.powerScore * 200) / 10, //
        control: Math.round(res.controlScore * 200) / 10,
        comfort: Math.round(res.comfortScore * 200) / 10,
        appearance: Math.round(res.appearanceScore * 200) / 10,
        config: Math.round(res.configScore * 200) / 10,

        sales: res.saleCount,
        rating: res.totalScore * 20,
      }
      return modelData
    } else {
      return generateMockCarModel(modelId)
    }
  } catch (error) {
    console.error('获取车型详情失败:', error)
    return generateMockCarModel(modelId)
  }
}
// const fetchModelDetails = async (modelId: number): Promise<CarModel | null> => {
//   try {
//     const response = await axios.get(`/api/car-models/search/${modelId}`)
//     if (response.data.status === 200 && response.data.data) {
//       return response.data.data
//     } else {
//       return generateMockCarModel(modelId)
//     }
//   } catch (error) {
//     console.error('获取车型详情失败:', error)
//     return generateMockCarModel(modelId)
//   }
// }

// 模拟数据生成
const generateMockSearchResults = (query: string): SearchResult[] => {
  const brands = ['Tesla', 'BYD', 'NIO', 'XPeng', 'BMW', 'Audi', 'Mercedes', 'Toyota']
  const models = ['Model Y', 'Model 3', '汉EV', 'ES6', 'P7', '3系', 'A4L', 'C级']

  return Array.from({ length: 8 }, (_, index) => ({
    id: index + 1,
    name: models[index],
    brand: brands[index],
    priceRange: `${(Math.random() * 30 + 20).toFixed(0)}-${(Math.random() * 20 + 50).toFixed(0)}万`,
  })).filter(
    (item) =>
      item.name.toLowerCase().includes(query.toLowerCase()) ||
      item.brand.toLowerCase().includes(query.toLowerCase()),
  )
}

const generateMockHotCarList = (): HotCarModel[] => {
  const brands = ['Tesla', 'BYD', 'NIO', 'XPeng', 'Li Auto', 'BMW', 'Audi', 'Mercedes']
  const models = ['Model Y', '汉EV', 'ES6', 'P7', '理想ONE', 'iX3', 'e-tron', 'EQC']

  return Array.from({ length: 20 }, (_, index) => ({
    id: index + 1,
    name: models[index % models.length],
    brand: brands[index % brands.length],
    priceRange: `${(Math.random() * 30 + 20).toFixed(0)}-${(Math.random() * 20 + 50).toFixed(0)}万`,
    image: `https://picsum.photos/300/200?random=${index}`,
    sales: Math.floor(Math.random() * 8000) + 2000,
    rating: Math.random() * 2 + 3,
  }))
}

const generateMockCarModel = (modelId: number): CarModel => {
  const brands = ['Tesla', 'BYD', 'NIO', 'XPeng', 'BMW', 'Audi']
  const models = ['Model Y', '汉EV', 'ES6', 'P7', 'iX3', 'e-tron']
  const types = ['SUV', 'Sedan', 'MPV']
  const engines = ['纯电动', '混合动力', '2.0T', '1.5T']

  const brand = brands[modelId % brands.length]
  const model = models[modelId % models.length]

  return {
    id: modelId,
    name: model,
    brand: brand,
    type: types[modelId % types.length],
    engine: engines[modelId % engines.length],
    priceRange: `${(Math.random() * 30 + 20).toFixed(0)}-${(Math.random() * 20 + 50).toFixed(0)}万`,
    image: `https://picsum.photos/300/200?random=${modelId}`,

    // 模拟对比参数
    price: Math.floor(Math.random() * 400000) + 200000,
    power: Math.floor(Math.random() * 30) + 70,
    control: Math.floor(Math.random() * 30) + 70,
    comfort: Math.floor(Math.random() * 30) + 70,
    appearance: Math.floor(Math.random() * 30) + 70,
    config: Math.floor(Math.random() * 30) + 70,

    sales: Math.floor(Math.random() * 8000) + 2000,
    rating: Math.random() * 2 + 3,
  }
}

// 事件处理函数
const handleAddModel = async (modelId: number) => {
  if (!modelId || selectedModels.value.length >= 4) {
    console.log(modelId, selectedModels.value.length)
    ElMessage.warning('最多只能选择4款车型进行对比')
    return
  }

  if (isModelSelected(modelId)) {
    ElMessage.warning('该车型已在对比列表中')
    return
  }

  loading.value = true
  try {
    const modelDetails = await fetchModelDetails(modelId)
    if (modelDetails) {
      selectedModels.value.push(modelDetails)
      ElMessage.success(`${modelDetails.brand} ${modelDetails.name} 已加入对比`)
      newModelId.value = null

      if (selectedModels.value.length >= 2) {
        await updateComparisonData()
      }
    }
  } catch (error) {
    ElMessage.error('添加车型失败')
  } finally {
    loading.value = false
  }
}

const removeModel = (modelId: number) => {
  const index = selectedModels.value.findIndex((model) => model.id === modelId)
  if (index > -1) {
    const removed = selectedModels.value.splice(index, 1)[0]
    ElMessage.info(`${removed.brand} ${removed.name} 已移出对比`)

    if (selectedModels.value.length >= 2) {
      updateComparisonData()
    } else {
      comparisonData.value = null
    }
  }
}

const clearAllModels = () => {
  selectedModels.value = []
  comparisonData.value = null
  ElMessage.info('已清空所有选择的车型')
}

const handleScenarioChange = () => {
  const weights = scenarioWeights[userScenario.value]
  if (weights) {
    customWeights.value = { ...weights }
    ElMessage.success(`已切换到${getScenarioLabel(userScenario.value)}场景权重`)

    if (selectedModels.value.length >= 2) {
      updateComparisonData()
    }
  }
}

const resetWeights = () => {
  customWeights.value = {
    price: 0.25,
    power: 0.15,
    control: 0.2,
    comfort: 0.15,
    appearance: 0.15,
    config: 0.1,
  }
  ElMessage.info('权重已重置为默认值')
}

const applyWeights = () => {
  if (totalWeight.value !== 100) {
    ElMessage.warning('权重总计必须为100%，请调整后再应用')
    return
  }

  updateComparisonData()
  ElMessage.success('权重配置已应用')
}

const addModelFromHotList = async () => {
  if (hotCarList.value.length === 0) {
    await fetchHotCarList()
  }
  showHotListDialog.value = true
}

const selectHotModel = async (hotModel: HotCarModel) => {
  if (isModelSelected(hotModel.id)) {
    return
  }

  if (selectedModels.value.length >= 4) {
    ElMessage.warning('最多只能选择4款车型进行对比')
    return
  }

  const modelDetails = await fetchModelDetails(hotModel.id)
  if (modelDetails) {
    selectedModels.value.push(modelDetails)
    ElMessage.success(`${modelDetails.brand} ${modelDetails.name} 已加入对比`)

    if (selectedModels.value.length >= 2) {
      await updateComparisonData()
    }
  }
}

const handleHotListClose = () => {
  showHotListDialog.value = false
}

const toggleAllDimensions = () => {
  showAllDimensions.value = !showAllDimensions.value
}

const toggleRadarView = () => {
  radarViewType.value = radarViewType.value === 'score' ? 'raw' : 'score'
  initRadarChart()
}

// 图表初始化
const updateComparisonData = async () => {
  if (selectedModels.value.length < 2) return

  comparisonData.value = {
    timestamp: Date.now(),
    models: selectedModels.value,
    weights: customWeights.value,
    scenario: userScenario.value,
  }

  await nextTick()
  await Promise.all([initRadarChart(), initBarChart()])
}

const initRadarChart = async () => {
  if (!radarChart.value || selectedModels.value.length < 2) return

  await nextTick()

  if (radarChartInstance) {
    radarChartInstance.dispose()
  }

  radarChartInstance = echarts.init(radarChart.value)

  const dimensions = ['价格成本', '动力性能', '操控性', '空间舒适', '外观', '科技配置']
  const maxValues =
    radarViewType.value === 'score'
      ? [100, 100, 100, 100, 100, 100]
      : [50, 5, 5, 5, 5, 5]
  let topPrice = 50 // 评分价格上限设为50万
  let lowPrice = 10 // 评分价格下限设为0万

  const series = selectedModels.value.map((model, index) => {
    const colors = ['#ff6b6b', '#4ecdc4', '#45b7d1', '#96ceb4', '#feca57', '#ff9ff3']
    const data =
      radarViewType.value === 'score'
        ? [
            Math.min(Math.max(0, 100 - (model.price - lowPrice)/(topPrice - lowPrice) * 100), 100).toFixed(1), // 价格转换为评分
            model.power,
            model.control,
            model.comfort,
            model.appearance,
            model.config,
          ]
        : [
            model.price,
            model.power / 20,
            model.control / 20,
            model.comfort / 20,
            model.appearance / 20,
            model.config / 20,
          ]

    return {
      name: `${model.brand} ${model.name}`,
      type: 'radar',
      data: [data],
      itemStyle: { color: colors[index % colors.length] },
      lineStyle: { width: 2 },
      symbol: 'circle',
      symbolSize: 6,
    }
  })

  const option = {
    title: {
      text: '六维雷达图对比',
      left: 'center',
      textStyle: { fontSize: 16, fontWeight: 'bold' },
    },
    tooltip: {
      trigger: 'item',
    },
    legend: {
      orient: 'horizontal',
      bottom: 10,
      data: selectedModels.value.map((m) => `${m.brand} ${m.name}`),
    },
    radar: {
      indicator: dimensions.map((name, index) => ({
        name,
        max: maxValues[index],
      })),
      radius: '65%',
      center: ['50%', '55%'],
    },
    series: series,
  }

  radarChartInstance.setOption(option)
}

const initBarChart = async () => {
  if (!barChart.value || selectedModels.value.length < 2) return

  await nextTick()

  if (barChartInstance) {
    barChartInstance.dispose()
  }

  barChartInstance = echarts.init(barChart.value)

  const dimensionMap = {
    price: {
      label: '价格对比',
      field: 'price',
      unit: '万元',
      transform: (v: number) => v,
    },
    power: {
      label: '性能对比',
      field: 'power',
      unit: '分',
      transform: (v: number) => v,
    },
    control: { label: '操控性对比', field: 'control', unit: '分', transform: (v: number) => v },
    comfort: { label: '舒适度对比', field: 'comfort', unit: '分', transform: (v: number) => v },
    appearance: { label: '外观对比', field: 'appearance', unit: '分', transform: (v: number) => v },
    config: { label: '配置对比', field: 'config', unit: '分', transform: (v: number) => v },
  }

  // 有大量的类型索引问题，直接用类型断言 as keyof typeof 解决了，有安全隐患
  const dimension = dimensionMap[barChartDimension.value as keyof typeof dimensionMap]
  const xAxisData = selectedModels.value.map((m) => `${m.brand}\n${m.name}`)
  const seriesData = selectedModels.value.map((m) => dimension.transform(m[dimension.field as keyof typeof m] as keyof typeof dimension.transform))

  const option = {
    title: {
      text: dimension.label,
      left: 'center',
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '8%',
      top: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: xAxisData,
      axisLabel: {
        fontSize: 11,
        interval: 0,
        rotate: 0,
      },
    },
    yAxis: {
      type: 'value',
      name: dimension.unit,
      axisLabel: { fontSize: 11 },
    },
    series: [
      {
        type: 'bar',
        data: seriesData,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#4facfe' },
            { offset: 1, color: '#00f2fe' },
          ]),
        },
        barWidth: '60%',
        label: {
          show: true,
          position: 'top',
          fontSize: 11,
        },
      },
    ],
  }

  barChartInstance.setOption(option)
}

// 页面操作
const refreshComparison = async () => {
  loading.value = true
  try {
    if (selectedModels.value.length >= 2) {
      await updateComparisonData()
    }
    ElMessage.success('对比数据已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

const exportReport = () => {
  if (selectedModels.value.length < 2) {
    ElMessage.warning('至少需要2款车型才能导出报告')
    return
  }

  const csvContent = [
    ['车型对比报告'],
    ['生成时间', new Date().toLocaleString()],
    ['使用场景', getScenarioLabel(userScenario.value)],
    [''],
    ['排名', '车型', '品牌', '综合评分', '价格区间'],
    ...rankedModels.value.map((model, index) => [
      index + 1,
      model.name,
      model.brand,
      model.totalScore?.toFixed(1) || '',
      model.priceRange,
    ]),
  ]
    .map((row) => row.join(','))
    .join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `车型对比报告_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  ElMessage.success('对比报告已导出')
}

const exportComparisonTable = () => {
  ElMessage.info('导出对比表功能开发中...')
}

const saveComparison = () => {
  const comparisonId = Date.now()
  localStorage.setItem(
    `comparison_${comparisonId}`,
    JSON.stringify({
      models: selectedModels.value,
      weights: customWeights.value,
      scenario: userScenario.value,
      timestamp: Date.now(),
    }),
  )

  ElMessage.success('对比结果已保存到本地')
}

const shareComparison = () => {
  showShareDialog.value = true
}

const copyShareLink = () => {
  const shareUrl = `${window.location.origin}/app/VehicleModelCompAnalysis?models=${selectedModels.value.map((m) => m.id).join(',')}`

  navigator.clipboard
    .writeText(shareUrl)
    .then(() => {
      ElMessage.success('分享链接已复制到剪贴板')
      showShareDialog.value = false
    })
    .catch(() => {
      ElMessage.error('复制失败')
    })
}

const shareToSocial = () => {
  const shareText = `我正在对比${selectedModels.value.map((m) => `${m.brand} ${m.name}`).join('、')}，推荐选择：${getBestChoice().brand} ${getBestChoice().name}`

  if (navigator.share) {
    navigator.share({
      title: '车型对比结果',
      text: shareText,
      url: window.location.href,
    })
  } else {
    ElMessage.info('您的浏览器不支持分享功能')
  }
}


const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'https://via.placeholder.com/300x200?text=Car+Image'
}

// 窗口大小调整
const handleResize = () => {
  if (radarChartInstance) {
    radarChartInstance.resize()
  }
  if (barChartInstance) {
    barChartInstance.resize()
  }
}

// 监听器
watch([barChartDimension], () => {
  if (barChartInstance && selectedModels.value.length >= 2) {
    initBarChart()
  }
})

watch(
  [customWeights],
  () => {
    if (selectedModels.value.length >= 2 && comparisonData.value) {
      // 防抖更新
      const timeoutId = setTimeout(() => {
        updateComparisonData()
      }, 500)

      return () => clearTimeout(timeoutId)
    }
  },
  { deep: true },
)

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用车型对比分析工具！')

  // 检查URL参数中是否有预选车型
  const modelsParam = route.query.models as string
  if (modelsParam) {
    const modelIds = modelsParam
      .split(',')
      .map((id) => parseInt(id))
      .filter((id) => !isNaN(id))

    for (const modelId of modelIds.slice(0, 4)) {
      const model = await fetchModelDetails(modelId)
      if (model) {
        selectedModels.value.push(model)
      }
    }

    if (selectedModels.value.length >= 2) {
      await updateComparisonData()
    }
  }

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)

  if (radarChartInstance) {
    radarChartInstance.dispose()
  }
  if (barChartInstance) {
    barChartInstance.dispose()
  }
})
</script>

<template>
  <div class="vehicle-model-comp-analysis">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>车型对比分析</h2>
          <p>科学化的多维度车型对比，帮助您做出最优购车决策</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshComparison" :loading="loading">
            刷新对比
          </el-button>
          <el-button
            type="success"
            :icon="Download"
            @click="exportReport"
            :disabled="selectedModels.length < 2"
          >
            导出报告
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 车型选择区 -->
    <el-card shadow="never" class="model-selection-card">
      <template #header>
        <div class="selection-header">
          <span>车型选择 ({{ selectedModels.length }}/4)</span>
          <div class="selection-actions">
            <el-button size="small" @click="clearAllModels" :disabled="selectedModels.length === 0">
              清空选择
            </el-button>
            <el-button size="small" type="primary" @click="addModelFromHotList">
              从热门榜单添加
            </el-button>
          </div>
        </div>
      </template>

      <div class="model-selection-content">
        <el-row :gutter="16">
          <el-col :span="18">
            <el-form-item label="搜索添加车型:">
              <el-select
                v-model="newModelId"
                placeholder="搜索车型名称或品牌"
                filterable
                remote
                :remote-method="debounceSearchModels"
                :loading="searching"
                style="width: 100%"
                @change="handleAddModel"
              >
                <el-option
                  v-for="model in searchResults"
                  :key="model.id"
                  :label="`${model.brand} ${model.name} (${model.priceRange}万元)`"
                  :value="model.id"
                  :disabled="isModelSelected(model.id)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="使用场景:">
              <el-select v-model="userScenario" @change="handleScenarioChange">
                <el-option label="家庭用车" value="family" />
                <el-option label="城市通勤" value="commute" />
                <el-option label="商务出行" value="business" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 已选择的车型 -->
        <div class="selected-models" v-if="selectedModels.length > 0">
          <div
            v-for="(model, index) in selectedModels"
            :key="model.id"
            class="selected-model-card"
            :class="{ recommended: model.isRecommended }"
          >
            <div class="model-image">
              <img :src="model.image" :alt="model.name" @error="handleImageError" />
              <div class="model-badge" v-if="model.isRecommended">
                <el-icon><Trophy /></el-icon>
                <span>推荐</span>
              </div>
            </div>
            <div class="model-info">
              <h4>{{ model.name}}</h4>
              <p class="model-price">{{ model.priceRange }}万元</p>
              <div class="model-tags">
                <el-tag size="small">{{ model.type }}</el-tag>
                <el-tag size="small" type="success">{{ model.engine }}</el-tag>
              </div>
            </div>
            <div class="model-actions">
              <el-button size="small" type="danger" :icon="Delete" @click="removeModel(model.id)">
                移除
              </el-button>
            </div>
          </div>
        </div>

        <!-- 空状态提示 -->
        <el-empty v-if="selectedModels.length === 0" description="请至少选择2款车型进行对比" />
      </div>
    </el-card>

    <!-- 权重配置区 -->
    <el-card shadow="never" class="weight-config-card" v-if="selectedModels.length >= 2">
      <template #header>
        <div class="weight-header">
          <span>对比权重配置</span>
          <div class="weight-actions">
            <el-button size="small" @click="resetWeights">重置权重</el-button>
            <el-button size="small" type="primary" @click="applyWeights">应用权重</el-button>
          </div>
        </div>
      </template>

      <div class="weight-content">
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="weight-item">
              <div class="weight-label">
                <el-icon><Money /></el-icon>
                <span>价格成本</span>
                <span class="weight-value">{{ (customWeights.price * 100).toFixed(0) }}%</span>
              </div>
              <el-slider v-model="customWeights.price" :min="0" :max="1" :step="0.05" />
            </div>
          </el-col>
          <el-col :span="8">
            <div class="weight-item">
              <div class="weight-label">
                <el-icon><Lightning /></el-icon>
                <span>动力性能</span>
                <span class="weight-value"
                  >{{ (customWeights.power * 100).toFixed(0) }}%</span
                >
              </div>
              <el-slider v-model="customWeights.power" :min="0" :max="1" :step="0.05" />
            </div>
          </el-col>
          <el-col :span="8">
            <div class="weight-item">
              <div class="weight-label">
                <el-icon><MagicStick/></el-icon>
                <span>操控性</span>
                <span class="weight-value">{{ (customWeights.control * 100).toFixed(0) }}%</span>
              </div>
              <el-slider v-model="customWeights.control" :min="0" :max="1" :step="0.05" />
            </div>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="weight-item">
              <div class="weight-label">
                <el-icon><OfficeBuilding /></el-icon>
                <span>空间舒适</span>
                <span class="weight-value">{{ (customWeights.comfort * 100).toFixed(0) }}%</span>
              </div>
              <el-slider v-model="customWeights.comfort" :min="0" :max="1" :step="0.05" />
            </div>
          </el-col>
          <el-col :span="8">
            <div class="weight-item">
              <div class="weight-label">
                <el-icon><Lock /></el-icon>
                <span>外观</span>
                <span class="weight-value">{{ (customWeights.appearance * 100).toFixed(0) }}%</span>
              </div>
              <el-slider v-model="customWeights.appearance" :min="0" :max="1" :step="0.05" />
            </div>
          </el-col>
          <el-col :span="8">
            <div class="weight-item">
              <div class="weight-label">
                <el-icon><Monitor /></el-icon>
                <span>科技配置</span>
                <span class="weight-value">{{ (customWeights.config * 100).toFixed(0) }}%</span>
              </div>
              <el-slider v-model="customWeights.config" :min="0" :max="1" :step="0.05" />
            </div>
          </el-col>
        </el-row>
        <div class="weight-total" :class="{ error: totalWeight !== 100 }">
          权重总计: {{ totalWeight }}% {{ totalWeight !== 100 ? '(请调整至100%)' : '' }}
        </div>
      </div>
    </el-card>

    <!-- 对比结果展示区 -->
    <div class="comparison-results" v-if="selectedModels.length >= 2 && comparisonData">
      <!-- 综合评分排行 -->
      <el-card shadow="never" class="ranking-card">
        <template #header>
          <div class="ranking-header">
            <span>综合评分排行</span>
            <el-tag :type="getRankingTagType(userScenario)">{{
              getScenarioLabel(userScenario)
            }}</el-tag>
          </div>
        </template>

        <div class="ranking-list">
          <div
            v-for="(model, index) in rankedModels"
            :key="model.id"
            class="ranking-item"
            :class="{ 'top-choice': index === 0, recommended: model.isRecommended }"
          >
            <div class="rank-badge">
              <span class="rank-number">{{ index + 1 }}</span>
              <el-icon v-if="index === 0" class="crown-icon"><Trophy /></el-icon>
            </div>
            <div class="rank-model">
              <img :src="model.image" :alt="model.name" class="rank-image" />
              <div class="rank-info">
                <h4>{{ model.brand }} {{ model.name }}</h4>
                <p>{{ model.priceRange }}万元</p>
              </div>
            </div>
            <div class="rank-score">
              <div class="score-value">{{ model.totalScore.toFixed(1) }}</div>
              <div class="score-label">综合评分</div>
              <el-progress :percentage="model.totalScore" :show-text="false" />
            </div>
            <div class="rank-recommendation" v-if="index === 0">
              <el-tag type="success" size="large">
                <el-icon><Check /></el-icon>
                最佳选择
              </el-tag>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 可视化对比图表 -->
      <el-row :gutter="20">
        <!-- 雷达图对比 -->
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>六维雷达图对比</span>
                <el-button size="small" @click="toggleRadarView" type="text">
                  {{ radarViewType === 'score' ? '切换到原始数据' : '切换到评分' }}
                </el-button>
              </div>
            </template>
            <div ref="radarChart" class="chart-container" v-loading="loading"></div>
          </el-card>
        </el-col>

        <!-- 柱状图对比 -->
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="chart-header">
                <span>参数柱状图对比</span>
                <el-select v-model="barChartDimension" size="small" style="width: 120px">
                  <el-option label="价格对比" value="price" />
                  <el-option label="性能对比" value="power" />
                  <el-option label="操控性对比" value="control" />
                  <el-option label="舒适度对比" value="comfort" />
                  <el-option label="外观对比" value="appearance" />
                  <el-option label="配置对比" value="config" />
                </el-select>
              </div>
            </template>
            <div ref="barChart" class="chart-container" v-loading="loading"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 详细参数对比表 -->
      <el-card shadow="never" class="table-card">
        <template #header>
          <div class="table-header">
            <span>详细参数对比</span>
            <div class="table-actions">
              <el-button size="small" type="primary" @click="exportComparisonTable">
                导出对比表
              </el-button>
            </div>
          </div>
        </template>

        <div class="comparison-table">
          <el-table :data="comparisonTableData" border style="width: 100%">
            <el-table-column prop="dimension" label="对比维度" width="150" fixed>
              <template #default="{ row }">
                <div class="dimension-cell">
                  <el-icon>
                    <component :is="getDimensionIcon(row.dimension)" />
                  </el-icon>
                  <span>{{ row.label }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-for="model in selectedModels"
              :key="model.id"
              :label="`${model.brand} ${model.name}`"
              min-width="200"
            >
              <template #header>
                <div class="model-header">
                  <img :src="model.image" :alt="model.name" class="header-image" />
                  <div class="header-info">
                    <span class="header-name">{{ model.brand }} {{ model.name }}</span>
                    <span class="header-price">{{ model.priceRange }}万元</span>
                  </div>
                </div>
              </template>
              <template #default="{ row }">
                <div class="parameter-cell" :class="getBestValueClass(row, model.id)">
                  <span class="parameter-value">{{ getParameterValue(row, model.id) }}</span>
                  <span class="parameter-unit">{{ row.unit || '' }}</span>
                  <el-icon v-if="isBestValue(row, model.id)" class="best-icon"><Star /></el-icon>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>

      <!-- 优缺点分析 -->
      <el-card shadow="never" class="analysis-card">
        <template #header>
          <span>优缺点分析</span>
        </template>

        <div class="pros-cons-analysis">
          <el-row :gutter="20">
            <el-col v-for="model in selectedModels" :key="model.id" :xs="24" :sm="12" :lg="6">
              <div class="pros-cons-card">
                <div class="model-summary">
                  <img :src="model.image" :alt="model.name" class="summary-image" />
                  <div class="summary-info">
                    <h4>{{ model.brand }} {{ model.name }}</h4>
                    <div class="summary-score">
                      <span class="score-number">{{ getModelScore(model.id).toFixed(1) }}</span>
                      <span class="score-text">综合评分</span>
                    </div>
                  </div>
                </div>

                <div class="pros-section">
                  <h5>
                    <el-icon class="pros-icon"><CircleCheckFilled /></el-icon>
                    主要优势
                  </h5>
                  <ul class="pros-list">
                    <li v-for="pro in getModelPros(model.id)" :key="pro">{{ pro }}</li>
                  </ul>
                </div>

                <div class="cons-section">
                  <h5>
                    <el-icon class="cons-icon"><WarningFilled /></el-icon>
                    主要劣势
                  </h5>
                  <ul class="cons-list">
                    <li v-for="con in getModelCons(model.id)" :key="con">{{ con }}</li>
                  </ul>
                </div>

                <div class="target-audience">
                  <h5>适合人群</h5>
                  <p>{{ getTargetAudience(model.id) }}</p>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 购买建议 -->
      <el-card shadow="never" class="recommendation-card">
        <template #header>
          <div class="recommendation-header">
            <span>购买建议</span>
            <el-tag :type="getRecommendationTagType()"
              >基于{{ getScenarioLabel(userScenario) }}场景</el-tag
            >
          </div>
        </template>

        <div class="recommendation-content">
          <div class="recommendation-summary">
            <div class="best-choice">
              <div class="choice-badge">
                <el-icon><Trophy /></el-icon>
                <span>最优选择</span>
              </div>
              <div class="choice-model">
                <img
                  :src="getBestChoice().image"
                  :alt="getBestChoice().name"
                  class="choice-image"
                />
                <div class="choice-info">
                  <h3>{{ getBestChoice().brand }} {{ getBestChoice().name }}</h3>
                  <p class="choice-reason">{{ getBestChoiceReason() }}</p>
                  <div class="choice-score">
                    <span>综合评分: {{ getBestChoice().totalScore.toFixed(1) }}</span>
                    <el-rate
                      v-model="getBestChoice().ratingStars"
                      disabled
                      show-score
                      size="small"
                    />
                  </div>
                </div>
              </div>
            </div>

            <div class="other-recommendations">
              <div class="budget-choice" v-if="getBudgetChoice()">
                <h4>
                  <el-icon><Coin /></el-icon>
                  性价比之选
                </h4>
                <div class="choice-brief">
                  <span class="choice-name"
                    >{{ getBudgetChoice()?.brand }} {{ getBudgetChoice()?.name }}</span
                  >
                  <span class="choice-highlight">{{ getBudgetChoiceReason() }}</span>
                </div>
              </div>

              <div class="premium-choice" v-if="getPremiumChoice()">
                <h4>
                  <el-icon><Star /></el-icon>
                  品质之选
                </h4>
                <div class="choice-brief">
                  <span class="choice-name"
                    >{{ getPremiumChoice()?.brand }} {{ getPremiumChoice()?.name }}</span
                  >
                  <span class="choice-highlight">{{ getPremiumChoiceReason() }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="recommendation-actions">
            <el-button type="primary" size="large" @click="saveComparison">
              保存对比结果
            </el-button>
            <el-button size="large" @click="shareComparison"> 分享对比 </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 热门车型选择弹窗 -->
    <el-dialog
      v-model="showHotListDialog"
      title="从热门榜单添加"
      width="80%"
      :before-close="handleHotListClose"
    >
      <div class="hot-list-content">
        <el-row :gutter="16">
          <el-col
            v-for="hotModel in hotCarList"
            :key="hotModel.id"
            :xs="12"
            :sm="8"
            :md="6"
            :lg="4"
          >
            <div
              class="hot-model-card"
              :class="{
                selected: isModelSelected(hotModel.id),
                disabled: isModelSelected(hotModel.id),
              }"
              @click="selectHotModel(hotModel)"
            >
              <img :src="hotModel.image" :alt="hotModel.name" class="hot-model-image" />
              <div class="hot-model-info">
                <h5>{{ hotModel.brand }} {{ hotModel.name }}</h5>
                <p class="hot-model-price">{{ hotModel.priceRange }}万元</p>
                <div class="hot-model-stats">
                  <span class="sales">销量: {{ hotModel.sales.toLocaleString() }}</span>
                  <el-rate v-model="hotModel.rating" disabled size="small" />
                </div>
              </div>
              <div class="hot-model-overlay" v-if="isModelSelected(hotModel.id)">
                <el-icon><Check /></el-icon>
                <span>已选择</span>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>

    <!-- 对比结果分享弹窗 -->
    <el-dialog v-model="showShareDialog" title="分享对比结果" width="50%">
      <div class="share-content">
        <div class="share-summary">
          <h4>对比摘要</h4>
          <p>
            共对比 {{ selectedModels.length }} 款车型，推荐选择：{{ getBestChoice().brand }}
            {{ getBestChoice().name }}
          </p>
        </div>
        <div class="share-actions">
          <el-button type="primary" @click="copyShareLink">
            <el-icon><Link /></el-icon>
            复制链接
          </el-button>
          <el-button @click="shareToSocial">
            <el-icon><Share /></el-icon>
            社交分享
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 整体布局 */
.vehicle-model-comp-analysis {
  padding: 0 16px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 页面头部样式 */
.page-header {
  margin-bottom: 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
  border: none;
  box-shadow: 0 8px 32px rgba(79, 172, 254, 0.3);
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
  font-weight: 400;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
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

/* 车型选择卡片 */
.model-selection-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.selection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.selection-actions {
  display: flex;
  gap: 8px;
}

.model-selection-content {
  padding: 8px 0;
}

.model-selection-content .el-form-item {
  margin-bottom: 16px;
}

.model-selection-content .el-form-item__label {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

/* 已选择车型卡片 */
.selected-models {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.selected-model-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 16px;
  border: 2px solid #e8eaed;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.selected-model-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  border-color: #4facfe;
}

.selected-model-card.recommended {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0fff4 0%, #f6ffed 100%);
}

.selected-model-card.recommended::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.model-image {
  position: relative;
  width: 120px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.model-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.selected-model-card:hover .model-image img {
  transform: scale(1.05);
}

.model-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: rgba(103, 194, 58, 0.9);
  color: white;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
  backdrop-filter: blur(4px);
}

.model-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.model-info h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.2;
}

.model-price {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #4facfe;
}

.model-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.model-actions {
  display: flex;
  align-items: center;
}

/* 权重配置卡片 */
.weight-config-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.weight-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.weight-actions {
  display: flex;
  gap: 8px;
}

.weight-content {
  padding: 8px 0;
}

.weight-item {
  padding: 16px 20px;
  background: #f8fafb;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.weight-item:hover {
  background: #f0f9ff;
  border-color: #4facfe;
}

.weight-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
  color: #1a1a1a;
}

.weight-label .el-icon {
  margin-right: 8px;
  color: #4facfe;
}

.weight-value {
  font-size: 14px;
  color: #4facfe;
  font-weight: 700;
}

.weight-total {
  text-align: center;
  margin-top: 20px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 8px;
  font-weight: 600;
  color: #4facfe;
}

.weight-total.error {
  background: #fff2f0;
  color: #f56c6c;
}

/* 对比结果展示区 */
.comparison-results {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 综合评分排行卡片 */
.ranking-card {
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

.ranking-list {
  padding: 8px 0;
}

.ranking-item {
  display: grid;
  grid-template-columns: auto 1fr auto auto;
  gap: 20px;
  align-items: center;
  padding: 20px 24px;
  margin-bottom: 16px;
  background: white;
  border-radius: 16px;
  border: 2px solid #e8eaed;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.ranking-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.ranking-item.top-choice {
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
  border-color: #ffd700;
}

.ranking-item.top-choice::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(135deg, #ffd700 0%, #ffb300 100%);
}

.ranking-item.recommended {
  border-color: #67c23a;
}

.rank-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
}

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
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  box-shadow: 0 4px 15px rgba(79, 172, 254, 0.3);
}

.ranking-item.top-choice .rank-number {
  background: linear-gradient(135deg, #ffd700 0%, #ffb300 100%);
  font-size: 20px;
  box-shadow: 0 4px 15px rgba(255, 215, 0, 0.4);
}

.crown-icon {
  position: absolute;
  top: -8px;
  right: -8px;
  color: #ffd700;
  font-size: 16px;
}

.rank-model {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.rank-image {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.rank-info h4 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.2;
}

.rank-info p {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.rank-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 120px;
}

.score-value {
  font-size: 24px;
  font-weight: 700;
  color: #4facfe;
}

.ranking-item.top-choice .score-value {
  color: #ffd700;
}

.score-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.rank-recommendation {
  display: flex;
  align-items: center;
}

/* 图表卡片 */
.chart-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.chart-container {
  height: 400px;
  width: 100%;
  border-radius: 8px;
  background: white;
}

/* 对比表格卡片 */
.table-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.comparison-table {
  padding: 8px 0;
}

.dimension-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1a1a1a;
}

.dimension-cell .el-icon {
  color: #4facfe;
}

.model-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
}

.header-image {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.header-price {
  font-size: 12px;
  color: #4facfe;
  font-weight: 500;
}

.parameter-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  position: relative;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.parameter-cell.best-value {
  background: linear-gradient(135deg, #f0fff4 0%, #f6ffed 100%);
  border: 1px solid #67c23a;
  font-weight: 600;
  color: #52c41a;
}

.parameter-value {
  font-size: 14px;
  font-weight: 600;
}

.parameter-unit {
  font-size: 12px;
  color: #909399;
}

.best-icon {
  color: #67c23a;
  font-size: 16px;
}

/* 优缺点分析卡片 */
.analysis-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.pros-cons-analysis {
  padding: 8px 0;
}

.pros-cons-card {
  padding: 20px;
  background: white;
  border-radius: 16px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.pros-cons-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.model-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}

.summary-image {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.summary-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-info h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.2;
}

.summary-score {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.score-number {
  font-size: 20px;
  font-weight: 700;
  color: #4facfe;
}

.score-text {
  font-size: 12px;
  color: #909399;
}

.pros-section,
.cons-section {
  margin-bottom: 16px;
  flex: 1;
}

.pros-section h5,
.cons-section h5 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
}

.pros-icon {
  color: #67c23a;
}

.cons-icon {
  color: #f56c6c;
}

.pros-list,
.cons-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.pros-list li,
.cons-list li {
  position: relative;
  padding: 4px 0 4px 16px;
  font-size: 13px;
  line-height: 1.4;
  color: #606266;
}

.pros-list li::before {
  content: '✓';
  position: absolute;
  left: 0;
  color: #67c23a;
  font-weight: 600;
}

.cons-list li::before {
  content: '✗';
  position: absolute;
  left: 0;
  color: #f56c6c;
  font-weight: 600;
}

.target-audience {
  margin-top: auto;
}

.target-audience h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.target-audience p {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
}

/* 购买建议卡片 */
.recommendation-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.recommendation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.recommendation-content {
  padding: 8px 0;
}

.recommendation-summary {
  padding: 24px;
  background: linear-gradient(135deg, #f8fafb 0%, #ffffff 100%);
  border-radius: 12px;
  margin-bottom: 24px;
}

.best-choice {
  margin-bottom: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
  border: 2px solid #ffd700;
  border-radius: 16px;
  position: relative;
}

.best-choice::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(135deg, #ffd700 0%, #ffb300 100%);
}

.choice-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #e6a23c;
}

.choice-model {
  display: flex;
  gap: 20px;
  align-items: center;
}

.choice-image {
  width: 160px;
  height: 100px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.choice-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.choice-info h3 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
}

.choice-reason {
  margin: 0;
  font-size: 16px;
  color: #606266;
  line-height: 1.5;
}

.choice-score {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #4facfe;
  font-weight: 600;
}

.other-recommendations {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.budget-choice,
.premium-choice {
  padding: 20px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.budget-choice:hover,
.premium-choice:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.budget-choice h4,
.premium-choice h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.choice-brief {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.choice-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.choice-highlight {
  font-size: 13px;
  color: #4facfe;
  font-weight: 500;
}

.recommendation-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  background: #f8fafb;
  border-radius: 12px;
}

/* 热门车型选择弹窗 */
.hot-list-content {
  max-height: 60vh;
  overflow-y: auto;
}

.hot-model-card {
  position: relative;
  padding: 16px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.hot-model-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.hot-model-card.selected {
  border-color: #67c23a;
  background: #f6ffed;
}

.hot-model-card.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.hot-model-image {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 12px;
}

.hot-model-info {
  text-align: center;
}

.hot-model-info h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.2;
}

.hot-model-price {
  margin: 0 0 8px 0;
  font-size: 12px;
  color: #4facfe;
  font-weight: 500;
}

.hot-model-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.sales {
  font-size: 11px;
  color: #909399;
}

.hot-model-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(103, 194, 58, 0.8);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 12px;
  font-weight: 600;
}

/* 分享弹窗 */
.share-content {
  text-align: center;
}

.share-summary {
  margin-bottom: 24px;
}

.share-summary h4 {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.share-summary p {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.share-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
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

.el-button--danger {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);
}

.el-button--danger:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(245, 108, 108, 0.4);
}

.el-select {
  --el-select-border-color-hover: #4facfe;
  --el-select-input-focus-border-color: #4facfe;
}

.el-select .el-input__wrapper {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.el-select .el-input__wrapper:hover {
  border-color: #4facfe;
  box-shadow: 0 0 0 2px rgba(79, 172, 254, 0.1);
}

.el-slider {
  --el-slider-main-bg-color: #4facfe;
  --el-slider-runway-bg-color: #e8eaed;
  --el-slider-button-bg-color: #4facfe;
}

.el-slider__runway {
  border-radius: 4px;
}

.el-slider__button {
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(79, 172, 254, 0.3);
}

.el-progress {
  --el-color-primary: #4facfe;
}

.el-progress-bar__outer {
  border-radius: 4px;
  background: #e8eaed;
}

.el-progress-bar__inner {
  border-radius: 4px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.el-rate {
  --el-rate-fill-color: #ffd700;
  --el-rate-void-color: #e8eaed;
}

.el-tag {
  border-radius: 6px;
  font-weight: 500;
  font-size: 12px;
  padding: 4px 8px;
}

.el-tag--primary {
  background: #e6f7ff;
  color: #1890ff;
  border-color: #91d5ff;
}

.el-tag--success {
  background: #f6ffed;
  color: #52c41a;
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

.el-empty {
  padding: 60px 0;
}

.el-empty__description {
  color: #909399;
  font-size: 14px;
}

.el-table {
  border-radius: 8px;
  overflow: hidden;
}

.el-table .el-table__header th {
  background: #f8fafb;
  color: #1a1a1a;
  font-weight: 600;
  border-bottom: 1px solid #e8eaed;
}

.el-table .el-table__body td {
  border-bottom: 1px solid #f0f2f5;
}

.el-dialog {
  border-radius: 16px;
  overflow: hidden;
}

.el-dialog__header {
  background: #f8fafb;
  border-bottom: 1px solid #e8eaed;
}

.el-dialog__body {
  padding: 24px;
}

/* 加载状态 */
.el-loading-mask {
  border-radius: 16px;
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

.selected-model-card,
.ranking-item,
.pros-cons-card {
  animation: slideInUp 0.6s ease-out;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .ranking-item {
    grid-template-columns: auto 1fr auto;
    gap: 16px;
  }

  .rank-recommendation {
    grid-column: 1 / -1;
    justify-content: center;
    margin-top: 12px;
  }

  .chart-container {
    height: 350px;
  }

  .selected-models {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  }
}

@media (max-width: 1200px) {
  .ranking-item {
    padding: 16px 20px;
  }

  .rank-model {
    gap: 12px;
  }

  .rank-image {
    width: 100px;
    height: 70px;
  }

  .chart-container {
    height: 320px;
  }

  .other-recommendations {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .vehicle-model-comp-analysis {
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

  .weight-content .el-row {
    margin: 0;
  }

  .weight-content .el-col {
    margin-bottom: 16px;
  }

  .selected-models {
    grid-template-columns: 1fr;
  }

  .selected-model-card {
    flex-direction: column;
    text-align: center;
  }

  .model-image {
    width: 100%;
    height: 160px;
    margin: 0 auto;
  }

  .ranking-item {
    grid-template-columns: 1fr;
    gap: 16px;
    text-align: center;
  }

  .rank-model {
    flex-direction: column;
  }

  .rank-image {
    width: 100%;
    height: 120px;
    margin: 0 auto;
  }

  .chart-container {
    height: 280px;
  }

  .choice-model {
    flex-direction: column;
    text-align: center;
  }

  .choice-image {
    width: 100%;
    height: 120px;
    margin: 0 auto;
  }

  .recommendation-actions {
    flex-direction: column;
  }

  .hot-model-card {
    margin-bottom: 16px;
  }

  .share-actions {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .header-left h2 {
    font-size: 20px;
  }

  .selection-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .weight-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .ranking-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .chart-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .table-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .rank-number {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }

  .choice-info h3 {
    font-size: 20px;
  }

  .model-selection-content .el-row {
    margin: 0;
  }

  .model-selection-content .el-col {
    margin-bottom: 16px;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .vehicle-model-comp-analysis {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .model-selection-card,
  .weight-config-card,
  .ranking-card,
  .chart-card,
  .table-card,
  .analysis-card,
  .recommendation-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .selected-model-card,
  .ranking-item,
  .pros-cons-card,
  .budget-choice,
  .premium-choice,
  .hot-model-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .selected-model-card.recommended {
    background: #2d3d2d;
  }

  .ranking-item.top-choice {
    background: #3d3d2d;
  }

  .best-choice {
    background: #3d3d2d;
  }

  .weight-item {
    background: #363636;
    border-color: #505050;
  }

  .weight-total {
    background: #2d3d4d;
  }

  .recommendation-summary {
    background: #363636;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .selected-model-card,
  .ranking-item,
  .pros-cons-card,
  .hot-model-card {
    border-width: 2px;
    border-color: #000;
  }

  .model-info h4,
  .rank-info h4,
  .choice-info h3 {
    font-weight: 700;
  }

  .model-image,
  .rank-image,
  .choice-image {
    border: 2px solid #000;
  }
}

/* 打印样式 */
@media print {
  .vehicle-model-comp-analysis {
    background: white !important;
  }

  .header-actions,
  .selection-actions,
  .weight-actions,
  .table-actions,
  .recommendation-actions {
    display: none !important;
  }

  .selected-model-card,
  .ranking-item,
  .pros-cons-card {
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

  .chart-container {
    height: 250px;
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
.selected-model-card:focus-visible,
.ranking-item:focus-visible,
.hot-model-card:focus-visible {
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

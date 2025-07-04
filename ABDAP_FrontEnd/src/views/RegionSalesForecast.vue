<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  Setting,
  TrendCharts,
  DataAnalysis,
  MapLocation,
  User,
  Box,
  Warning,
  Check,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()

// 接口定义
interface RegionNode {
  id: string
  name: string
  value?: string
  children?: RegionNode[]
}

interface RegionInfo {
  id: string
  name: string
  population: number
  gdpPerCapita: number
  consumptionLevel: number
  consumptionText: string
  carOwnership: number
}

interface RegionalCompetitor {
  brand: string
  marketShare: number
  growth: number
  channelStrength: string
  threat: 'success' | 'warning' | 'danger'
  threatText: string
}

interface InventoryRecommendation {
  region: string
  currentLevel: number
  recommendedLevel: number
  adjustment: number
  urgency: {
    type: 'success' | 'warning' | 'danger'
    text: string
  }
}

interface OptimizationResult {
  channel: string
  currentInvestment: number
  recommendedInvestment: number
  expectedROI: number
  reasoning: string
}

// 响应式数据
const loading = ref(false)
const analyzing = ref(false)

// 区域选择与配置
const selectedRegion = ref<string[]>([])
const forecastPeriod = ref('6M')
const analysisDimension = ref('market')
const chartView = ref('trend')

// 区域数据
const regionTree = ref<RegionNode[]>([])
const currentRegion = ref<RegionInfo>({
  id: '',
  name: '',
  population: 0,
  gdpPerCapita: 0,
  consumptionLevel: 0,
  consumptionText: '',
  carOwnership: 0,
})

// 分析结果状态
const hasResults = ref(false)

// 预测指标
const predictedSales = ref(15800)
const marketShare = ref(12.5)
const targetShare = ref(15.0)
const salesGrowth = ref(8.3)
const avgMonthlyGrowth = ref(2.1)
const industryGrowth = ref(1.8)
const forecastAccuracy = ref(89.2)
const confidenceLevel = ref(85)

// 竞争分析数据
const regionalCompetition = ref<RegionalCompetitor[]>([])

// 库存优化数据
const inventoryRecommendations = ref<InventoryRecommendation[]>([])

// 弹窗控制
const showRegionMap = ref(false)

// 地图配置
const mapDisplayMode = ref('sales')
const mapTimeRange = ref('6M')

// 图表实例
const regionTrendChart = ref<HTMLDivElement>()
const competitionChart = ref<HTMLDivElement>()
const inventoryChart = ref<HTMLDivElement>()
const regionMapChart = ref<HTMLDivElement>()

let regionTrendChartInstance: echarts.ECharts | null = null
let competitionChartInstance: echarts.ECharts | null = null
let inventoryChartInstance: echarts.ECharts | null = null
let regionMapChartInstance: echarts.ECharts | null = null

// 级联选择器配置
const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  expandTrigger: 'hover' as const,
  emitPath: false,
}

// 计算属性
const salesGrowthType = computed(() => {
  if (salesGrowth.value >= 5) return 'text-success'
  if (salesGrowth.value >= 0) return 'text-warning'
  return 'text-danger'
})

// API调用函数
const fetchRegionTree = async () => {
  try {
    const response = await axios.get('/api/regions/tree')
    if (response.data.status === 1) {
      regionTree.value = response.data.data
    } else {
      regionTree.value = generateMockRegionTree()
    }
  } catch (error) {
    console.error('获取区域树失败:', error)
    regionTree.value = generateMockRegionTree()
  }
}

const fetchRegionAnalysis = async (regionId: string) => {
  try {
    const params = {
      regionId,
      period: forecastPeriod.value,
      dimension: analysisDimension.value,
    }

    const response = await axios.get('/api/regions/analysis', { params })
    if (response.data.status === 1) {
      return response.data.data
    } else {
      return generateMockRegionAnalysis()
    }
  } catch (error) {
    console.error('获取区域分析失败:', error)
    return generateMockRegionAnalysis()
  }
}

// 模拟数据生成
const generateMockRegionTree = (): RegionNode[] => {
  return [
    {
      id: 'beijing',
      name: '北京市',
      children: [
        { id: 'beijing-dongcheng', name: '东城区' },
        { id: 'beijing-xicheng', name: '西城区' },
        { id: 'beijing-chaoyang', name: '朝阳区' },
        { id: 'beijing-haidian', name: '海淀区' },
      ],
    },
    {
      id: 'shanghai',
      name: '上海市',
      children: [
        { id: 'shanghai-huangpu', name: '黄浦区' },
        { id: 'shanghai-jingan', name: '静安区' },
        { id: 'shanghai-pudong', name: '浦东新区' },
        { id: 'shanghai-changning', name: '长宁区' },
      ],
    },
    {
      id: 'guangdong',
      name: '广东省',
      children: [
        { id: 'guangdong-guangzhou', name: '广州市' },
        { id: 'guangdong-shenzhen', name: '深圳市' },
        { id: 'guangdong-dongguan', name: '东莞市' },
        { id: 'guangdong-foshan', name: '佛山市' },
      ],
    },
    {
      id: 'zhejiang',
      name: '浙江省',
      children: [
        { id: 'zhejiang-hangzhou', name: '杭州市' },
        { id: 'zhejiang-ningbo', name: '宁波市' },
        { id: 'zhejiang-wenzhou', name: '温州市' },
        { id: 'zhejiang-jiaxing', name: '嘉兴市' },
      ],
    },
  ]
}

const generateMockRegionAnalysis = () => {
  // 生成区域基础信息
  currentRegion.value = {
    id: selectedRegion.value[selectedRegion.value.length - 1],
    name: '北京市朝阳区',
    population: 345,
    gdpPerCapita: 168000,
    consumptionLevel: 85,
    consumptionText: '高消费',
    carOwnership: 89,
  }

  // 生成竞争对手数据
  regionalCompetition.value = [
    {
      brand: '特斯拉',
      marketShare: 23.5,
      growth: 12.8,
      channelStrength: '线上优势',
      threat: 'danger',
      threatText: '高威胁',
    },
    {
      brand: '蔚来',
      marketShare: 15.2,
      growth: 8.3,
      channelStrength: '服务体验',
      threat: 'warning',
      threatText: '中威胁',
    },
    {
      brand: '理想',
      marketShare: 12.8,
      growth: 15.6,
      channelStrength: '产品力',
      threat: 'warning',
      threatText: '中威胁',
    },
    {
      brand: '小鹏',
      marketShare: 9.7,
      growth: 6.2,
      channelStrength: '智能化',
      threat: 'success',
      threatText: '低威胁',
    },
  ]

  // 生成库存推荐数据
  inventoryRecommendations.value = [
    {
      region: '朝阳区',
      currentLevel: 1200,
      recommendedLevel: 1400,
      adjustment: 16.7,
      urgency: { type: 'warning', text: '中等' },
    },
    {
      region: '海淀区',
      currentLevel: 800,
      recommendedLevel: 850,
      adjustment: 6.3,
      urgency: { type: 'success', text: '低' },
    },
    {
      region: '西城区',
      currentLevel: 650,
      recommendedLevel: 580,
      adjustment: -10.8,
      urgency: { type: 'danger', text: '高' },
    },
    {
      region: '东城区',
      currentLevel: 450,
      recommendedLevel: 520,
      adjustment: 15.6,
      urgency: { type: 'warning', text: '中等' },
    },
  ]

  hasResults.value = true
}

// 事件处理函数
const handleRegionChange = (value: string[]) => {
  hasResults.value = false
  ElMessage.info(`已选择区域: ${value[value.length - 1]}`)
}

const handlePeriodChange = () => {
  if (hasResults.value) {
    ElMessage.info(`预测周期已调整为${forecastPeriod.value}`)
  }
}

const handleDimensionChange = () => {
  if (hasResults.value) {
    ElMessage.info(`分析维度已切换为${analysisDimension.value}`)
  }
}

const startAnalysis = async () => {
  analyzing.value = true

  try {
    // 模拟分析过程
    await new Promise((resolve) => setTimeout(resolve, 2000))

    const analysisData = await fetchRegionAnalysis(selectedRegion.value[selectedRegion.value.length - 1])

    // 初始化图表
    await nextTick()
    await initAllCharts()

    ElMessage.success('区域分析完成！')
  } catch (error) {
    console.error('分析失败:', error)
    ElMessage.error('分析失败，请重试')
  } finally {
    analyzing.value = false
  }
}

const refreshData = async () => {
  loading.value = true
  try {
    await Promise.all([fetchRegionTree()])
    ElMessage.success('数据已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

const exportReport = () => {
  if (!hasResults.value) return

  const reportData = {
    region: currentRegion.value.name,
    period: forecastPeriod.value,
    predictedSales: predictedSales.value,
    marketShare: marketShare.value,
    exportTime: new Date().toISOString(),
  }

  const blob = new Blob([JSON.stringify(reportData, null, 2)], {
    type: 'application/json;charset=utf-8;',
  })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `region_analysis_${currentRegion.value.name}_${new Date().toISOString().slice(0, 10)}.json`
  link.click()

  ElMessage.success('区域分析报告已导出')
}

// 图表初始化函数
const initAllCharts = async () => {
  await Promise.all([
    initRegionTrendChart(),
    initCompetitionChart(),
    initInventoryChart(),
  ])
}

const initRegionTrendChart = async () => {
  if (!regionTrendChart.value) return

  await nextTick()

  if (regionTrendChartInstance) {
    regionTrendChartInstance.dispose()
  }

  regionTrendChartInstance = echarts.init(regionTrendChart.value)

  const months = ['1月', '2月', '3月', '4月', '5月', '6月']
  const historicalData = [1200, 1350, 1180, 1420, 1380, 1450]
  const forecastData = [1520, 1680, 1750, 1890, 1950, 2100]

  const option = {
    title: {
      text: chartView.value === 'trend' ? '区域销量趋势预测' : '区域对比分析',
      left: 'center',
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: ['历史销量', '预测销量'],
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
      data: months,
    },
    yAxis: {
      type: 'value',
      name: '销量(台)',
    },
    series: [
      {
        name: '历史销量',
        type: 'line',
        data: historicalData,
        itemStyle: { color: '#409EFF' },
        lineStyle: { width: 3 },
      },
      {
        name: '预测销量',
        type: 'line',
        data: forecastData,
        itemStyle: { color: '#E6A23C' },
        lineStyle: { width: 3, type: 'dashed' },
      },
    ],
  }

  regionTrendChartInstance.setOption(option)
}

const initCompetitionChart = async () => {
  if (!competitionChart.value) return

  await nextTick()

  if (competitionChartInstance) {
    competitionChartInstance.dispose()
  }

  competitionChartInstance = echarts.init(competitionChart.value)

  const option = {
    title: {
      text: '区域竞争格局',
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
      bottom: '3%',
      top: '20%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: regionalCompetition.value.map((item) => item.brand),
    },
    yAxis: {
      type: 'value',
      name: '市场份额(%)',
    },
    series: [
      {
        name: '市场份额',
        type: 'bar',
        data: regionalCompetition.value.map((item) => ({
          value: item.marketShare,
          itemStyle: {
            color:
              item.threat === 'danger' ? '#f56c6c' : item.threat === 'warning' ? '#e6a23c' : '#67c23a',
          },
        })),
        barWidth: '60%',
      },
    ],
  }

  competitionChartInstance.setOption(option)
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
      text: '库存分布分析',
      left: 'center',
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    legend: {
      data: ['当前库存', '建议库存'],
      top: 30,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '25%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: inventoryRecommendations.value.map((item) => item.region),
    },
    yAxis: {
      type: 'value',
      name: '库存(台)',
    },
    series: [
      {
        name: '当前库存',
        type: 'bar',
        data: inventoryRecommendations.value.map((item) => item.currentLevel),
        itemStyle: { color: '#409EFF' },
      },
      {
        name: '建议库存',
        type: 'bar',
        data: inventoryRecommendations.value.map((item) => item.recommendedLevel),
        itemStyle: { color: '#67c23a' },
      },
    ],
  }

  inventoryChartInstance.setOption(option)
}

// 弹窗相关函数
const refreshMapData = () => {
  ElMessage.info('正在刷新地图数据...')
  setTimeout(() => {
    ElMessage.success('地图数据已更新')
  }, 1000)
}

const exportMapData = () => {
  ElMessage.success('地图数据导出功能开发中...')
}

const downloadMap = () => {
  ElMessage.success('地图下载功能开发中...')
}

const generateInventoryPlan = () => {
  ElMessage.info('正在生成库存优化计划...')
  setTimeout(() => {
    ElMessage.success('库存优化计划已生成，请查看建议列表')
  }, 1500)
}

// 窗口大小调整
const handleResize = () => {
  const chartInstances = [
    regionTrendChartInstance,
    competitionChartInstance,
    inventoryChartInstance,
    regionMapChartInstance,
  ]

  chartInstances.forEach((instance) => {
    if (instance) {
      instance.resize()
    }
  })
}

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用区域销售预测系统！')

  try {
    await fetchRegionTree()
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
    regionTrendChartInstance,
    competitionChartInstance,
    inventoryChartInstance,
    regionMapChartInstance,
  ]

  chartInstances.forEach((instance) => {
    if (instance) {
      instance.dispose()
    }
  })
})
</script>

<template>
  <div class="region-sales-forecast">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>区域销售预测</h2>
          <p>区域市场深耕和渠道效率提升的执行级预测分析</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
          <el-button type="success" :icon="Download" @click="exportReport" :disabled="!hasResults">
            导出报告
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 区域选择配置 -->
    <el-card shadow="never" class="region-config-card">
      <template #header>
        <span>区域选择与预测配置</span>
      </template>

      <div class="config-content">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="选择区域:">
              <el-cascader
                v-model="selectedRegion"
                :options="regionTree"
                placeholder="选择省份/城市"
                filterable
                @change="handleRegionChange"
                style="width: 100%"
                :props="cascaderProps"
              />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="预测周期:">
              <el-select v-model="forecastPeriod" @change="handlePeriodChange">
                <el-option label="3个月" value="3M" />
                <el-option label="6个月" value="6M" />
                <el-option label="12个月" value="12M" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="分析维度:">
              <el-select v-model="analysisDimension" @change="handleDimensionChange">
                <el-option label="市场开拓" value="market" />
                <el-option label="渠道效率" value="channel" />
                <el-option label="团队管理" value="team" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-button
              type="primary"
              @click="startAnalysis"
              :loading="analyzing"
              :disabled="!selectedRegion.length"
              style="width: 100%"
            >
              开始分析
            </el-button>
          </el-col>
          <el-col :span="6">
            <el-button
              type="success"
              @click="showRegionMap = true"
              :disabled="!hasResults"
              style="width: 100%"
            >
              区域地图
            </el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 主要分析结果 -->
    <div v-if="hasResults">
      <!-- 区域预测趋势 -->
      <el-row :gutter="20">
        <el-col :xs="24" :lg="24">
          <el-card shadow="never" class="forecast-trend-card">
            <template #header>
              <div class="card-header">
                <span>区域销量预测趋势</span>
                <div class="chart-controls">
                  <el-button-group size="small">
                    <el-button
                      :type="chartView === 'trend' ? 'primary' : ''"
                      @click="chartView = 'trend'"
                    >
                      趋势预测
                    </el-button>
                    <el-button
                      :type="chartView === 'comparison' ? 'primary' : ''"
                      @click="chartView = 'comparison'"
                    >
                      区域对比
                    </el-button>
                  </el-button-group>
                </div>
              </div>
            </template>

            <div class="forecast-content">
              <!-- 预测图表 -->
              <div class="trend-chart">
                <div ref="regionTrendChart" class="chart-container" v-loading="analyzing"></div>
              </div>

              <!-- 预测关键指标 -->
              <div class="forecast-metrics">
                <el-row :gutter="16">
                  <el-col :span="6">
                    <div class="metric-card">
                      <div class="metric-value">{{ predictedSales.toLocaleString() }}</div>
                      <div class="metric-label">预测销量(台)</div>
                      <div class="metric-change" :class="salesGrowthType">
                        {{ salesGrowth >= 0 ? '+' : '' }}{{ salesGrowth.toFixed(1) }}%
                      </div>
                    </div>
                  </el-col>
                  <el-col :span="6">
                    <div class="metric-card">
                      <div class="metric-value">{{ marketShare.toFixed(1) }}%</div>
                      <div class="metric-label">市场份额</div>
                      <div class="metric-target">目标: {{ targetShare.toFixed(1) }}%</div>
                    </div>
                  </el-col>
                  <el-col :span="6">
                    <div class="metric-card">
                      <div class="metric-value">{{ avgMonthlyGrowth.toFixed(1) }}%</div>
                      <div class="metric-label">月均增长</div>
                      <div class="metric-benchmark">行业: {{ industryGrowth.toFixed(1) }}%</div>
                    </div>
                  </el-col>
                  <el-col :span="6">
                    <div class="metric-card">
                      <div class="metric-value">{{ forecastAccuracy.toFixed(1) }}%</div>
                      <div class="metric-label">预测准确度</div>
                      <div class="metric-confidence">置信度: {{ confidenceLevel }}%</div>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 区域竞争格局与库存优化 -->
      <el-row :gutter="20">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="competition-card">
            <template #header>
              <span>区域竞争格局</span>
            </template>

            <div class="competition-analysis">
              <!-- 竞争格局图表 -->
              <div class="competition-chart">
                <div ref="competitionChart" class="chart-container" style="height: 300px;"></div>
              </div>

              <!-- 主要竞争对手 -->
              <div class="competitors-list">
                <h6>主要竞争对手</h6>
                <div
                  v-for="competitor in regionalCompetition"
                  :key="competitor.brand"
                  class="competitor-item"
                >
                  <div class="competitor-header">
                    <span class="competitor-name">{{ competitor.brand }}</span>
                    <el-tag :type="competitor.threat" size="small">{{ competitor.threatText }}</el-tag>
                  </div>
                  <div class="competitor-metrics">
                    <div class="metric">
                      <span>市场份额:</span>
                      <strong>{{ competitor.marketShare.toFixed(1) }}%</strong>
                    </div>
                    <div class="metric">
                      <span>增长率:</span>
                      <strong :class="competitor.growth >= 0 ? 'text-success' : 'text-danger'">
                        {{ competitor.growth >= 0 ? '+' : '' }}{{ competitor.growth.toFixed(1) }}%
                      </strong>
                    </div>
                    <div class="metric">
                      <span>渠道优势:</span>
                      <strong>{{ competitor.channelStrength }}</strong>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="inventory-optimization-card">
            <template #header>
              <div class="card-header">
                <span>区域库存优化</span>
                <el-button size="small" @click="generateInventoryPlan">生成库存计划</el-button>
              </div>
            </template>

            <div class="inventory-optimization">
              <!-- 库存分布图表 -->
              <div class="inventory-chart">
                <div ref="inventoryChart" class="chart-container" style="height: 250px;"></div>
              </div>

              <!-- 库存优化建议 -->
              <div class="inventory-recommendations">
                <h6>库存优化建议</h6>
                <el-table :data="inventoryRecommendations" size="small" style="width: 100%">
                  <el-table-column prop="region" label="区域" width="100" />
                  <el-table-column prop="currentLevel" label="当前库存" width="100">
                    <template #default="{ row }">
                      {{ row.currentLevel.toLocaleString() }}台
                    </template>
                  </el-table-column>
                  <el-table-column prop="recommendedLevel" label="建议库存" width="100">
                    <template #default="{ row }">
                      {{ row.recommendedLevel.toLocaleString() }}台
                    </template>
                  </el-table-column>
                  <el-table-column prop="adjustment" label="调整幅度" width="100">
                    <template #default="{ row }">
                      <span :class="row.adjustment >= 0 ? 'text-success' : 'text-danger'">
                        {{ row.adjustment >= 0 ? '+' : '' }}{{ row.adjustment.toFixed(1) }}%
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="urgency" label="紧急度" width="100">
                    <template #default="{ row }">
                      <el-tag :type="row.urgency.type" size="small">
                        {{ row.urgency.text }}
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 区域地图弹窗 -->
    <el-dialog
      v-model="showRegionMap"
      title="区域销售地图"
      width="85%"
    >
      <div class="region-map-content">
        <div class="map-controls">
          <el-row :gutter="16">
            <el-col :span="6">
              <el-select v-model="mapDisplayMode" placeholder="显示模式">
                <el-option label="销量分布" value="sales" />
                <el-option label="增长率" value="growth" />
                <el-option label="渠道覆盖" value="channel" />
                <el-option label="竞争强度" value="competition" />
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-select v-model="mapTimeRange" placeholder="时间范围">
                <el-option label="最近3个月" value="3M" />
                <el-option label="最近6个月" value="6M" />
                <el-option label="最近12个月" value="12M" />
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-button @click="refreshMapData">刷新地图</el-button>
            </el-col>
            <el-col :span="6">
              <el-button @click="exportMapData">导出数据</el-button>
            </el-col>
          </el-row>
        </div>

        <div class="region-map">
          <div ref="regionMapChart" class="chart-container" style="height: 500px;"></div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showRegionMap = false">关闭</el-button>
          <el-button type="primary" @click="downloadMap">下载地图</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 整体布局 */
.region-sales-forecast {
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

/* 区域配置卡片 */
.region-config-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.config-content {
  padding: 8px 0;
}

.config-content .el-row {
  align-items: end;
}

.config-content .el-form-item {
  margin-bottom: 8px;
}

.config-content .el-form-item__label {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.config-content .el-cascader,
.config-content .el-select {
  width: 100%;
}

/* 主要分析卡片 */
.region-overview-card,
.forecast-trend-card,
.channel-efficiency-card,
.team-management-card,
.competition-card,
.inventory-optimization-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.card-header .header-actions {
  gap: 8px;
}

.chart-controls .el-button-group .el-button {
  padding: 6px 16px;
  font-size: 14px;
  border-radius: 6px;
}

/* 区域概览样式 */
.region-overview {
  padding: 8px 0;
}

.economic-indicators,
.market-potential {
  padding: 20px;
}

.economic-indicators {
  border-bottom: 1px solid #f0f2f5;
}

.economic-indicators h5,
.market-potential h5 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.economic-indicators h5::before,
.market-potential h5::before {
  content: '';
  width: 4px;
  height: 18px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  margin-right: 10px;
  border-radius: 2px;
}

.indicator-list,
.potential-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.indicator-item,
.metric-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafb;
  border-radius: 8px;
  border-left: 3px solid #4facfe;
}

.indicator-item .label,
.metric-item .metric-label {
  font-weight: 500;
  color: #606266;
  font-size: 14px;
}

.indicator-item .value {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.metric-item .el-progress {
  flex: 1;
  margin-left: 16px;
}

/* 预测图表区域 */
.forecast-content {
  padding: 8px 0;
}

.trend-chart {
  padding: 20px;
  border-bottom: 1px solid #f0f2f5;
}

.chart-container {
  width: 100%;
  height: 400px;
  min-height: 400px;
  background: white;
  border-radius: 8px;
}

.forecast-metrics {
  padding: 20px;
}

.metric-card {
  text-align: center;
  padding: 20px 16px;
  background: linear-gradient(135deg, #f8fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.metric-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  border-color: #4facfe;
}

.metric-card .metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
  line-height: 1.2;
}

.metric-card .metric-label {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
  margin-bottom: 4px;
}

.metric-card .metric-change {
  font-size: 12px;
  font-weight: 600;
}

.metric-card .metric-target,
.metric-card .metric-benchmark,
.metric-card .metric-confidence {
  font-size: 12px;
  color: #909399;
}

/* 竞争分析样式 */
.competition-analysis {
  padding: 8px 0;
}

.competition-chart {
  padding: 20px;
  border-bottom: 1px solid #f0f2f5;
}

.competitors-list {
  padding: 20px;
}

.competitors-list h6 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.competitors-list h6::before {
  content: '';
  width: 4px;
  height: 18px;
  background: linear-gradient(135deg, #f56c6c 0%, #ff7875 100%);
  margin-right: 10px;
  border-radius: 2px;
}

.competitor-item {
  background: #f8fafb;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.competitor-item:hover {
  transform: translateX(4px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  border-color: #4facfe;
}

.competitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.competitor-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 16px;
}

.competitor-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 8px;
}

.competitor-metrics .metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.competitor-metrics .metric span {
  font-size: 12px;
  color: #909399;
}

.competitor-metrics .metric strong {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

/* 库存优化样式 */
.inventory-optimization {
  padding: 8px 0;
}

.inventory-chart {
  padding: 20px;
  border-bottom: 1px solid #f0f2f5;
}

.inventory-recommendations {
  padding: 20px;
}

.inventory-recommendations h6 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.inventory-recommendations h6::before {
  content: '';
  width: 4px;
  height: 18px;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  margin-right: 10px;
  border-radius: 2px;
}

/* 弹窗样式 */
.region-map-content,
.channel-optimizer-content,
.team-planner-content {
  max-height: 70vh;
  overflow-y: auto;
}

.map-controls,
.optimizer-settings,
.planner-settings {
  padding: 20px;
  background: #f8fafb;
  border-radius: 8px;
  margin-bottom: 20px;
}

.region-map {
  margin: 20px 0;
}

.map-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
}

.legend-label {
  font-size: 14px;
  color: #606266;
}

.optimization-results,
.team-plan-results {
  margin-top: 24px;
}

.optimization-results h5,
.team-plan-results h5 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.dialog-footer {
  text-align: right;
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;
}

/* 工具类样式 */
.text-success {
  color: #67c23a !important;
}

.text-warning {
  color: #e6a23c !important;
}

.text-danger {
  color: #f56c6c !important;
}

/* 表格样式增强 */
.el-table {
  font-size: 14px;
  border-radius: 8px !important;
}

.el-table th {
  background: #f8fafb !important;
  color: #606266 !important;
  font-weight: 600 !important;
  border-bottom: 1px solid #e8eaed !important;
}

.el-table td {
  border-bottom: 1px solid #f0f2f5 !important;
  padding: 12px 0 !important;
}

.el-table tr:hover > td {
  background: #f0f9ff !important;
}

/* 标签样式 */
.el-tag {
  border-radius: 6px;
  font-weight: 500;
  font-size: 12px;
  padding: 4px 8px;
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
  height: 8px !important;
}

.el-progress-bar__inner {
  border-radius: 4px;
}

/* 按钮样式 */
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

/* 级联选择器样式 */
.el-cascader {
  width: 100%;
}

.el-cascader .el-input__wrapper {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.el-cascader .el-input__wrapper:hover {
  border-color: #4facfe;
  box-shadow: 0 0 0 2px rgba(79, 172, 254, 0.1);
}

/* 评分组件 */
.el-rate {
  display: inline-flex;
  align-items: center;
}

.el-rate__item {
  margin-right: 4px;
}

/* 加载状态 */
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

.metric-card,
.competitor-item {
  animation: slideInUp 0.6s ease-out;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .chart-container {
    height: 350px;
    min-height: 350px;
  }

  .header-left h2 {
    font-size: 28px;
  }
}

@media (max-width: 1200px) {
  .chart-container {
    height: 320px;
    min-height: 320px;
  }

  .competitor-metrics {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .region-sales-forecast {
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

  .config-content .el-row {
    margin: 0;
  }

  .config-content .el-col {
    margin-bottom: 16px;
  }

  .chart-container {
    height: 280px;
    min-height: 280px;
  }

  .metric-card .metric-value {
    font-size: 24px;
  }

  .competitor-metrics {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .map-legend {
    justify-content: flex-start;
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .header-left h2 {
    font-size: 20px;
  }

  .metric-card {
    padding: 16px 12px;
  }

  .metric-card .metric-value {
    font-size: 20px;
  }

  .competitor-item {
    padding: 12px;
  }

  .chart-container {
    height: 240px;
    min-height: 240px;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .region-sales-forecast {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .region-config-card,
  .region-overview-card,
  .forecast-trend-card,
  .channel-efficiency-card,
  .team-management-card,
  .competition-card,
  .inventory-optimization-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .metric-card,
  .competitor-item {
    background: #2d2d2d;
    border-color: #404040;
  }

  .map-controls,
  .optimizer-settings,
  .planner-settings {
    background: #262626;
  }

  .indicator-item,
  .metric-item {
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
  .competitor-item {
    border-width: 2px;
    border-color: #000;
  }

  .metric-value,
  .competitor-name {
    font-weight: 700;
  }

  .chart-container {
    border: 2px solid #000;
  }
}

/* 打印样式 */
@media print {
  .region-sales-forecast {
    background: white !important;
  }

  .header-actions,
  .dialog-footer {
    display: none !important;
  }

  .chart-container {
    height: 300px !important;
    break-inside: avoid;
    border: 1px solid #ccc;
  }

  .metric-card,
  .competitor-item {
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
.el-input__wrapper:focus-within,
.el-select:focus-within .el-input__wrapper,
.el-cascader:focus-within .el-input__wrapper {
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

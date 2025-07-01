<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Download,
  TrendCharts,
  Operation, // 替代 Car
  Money,
  Shop,
  DataAnalysis, // 替代 PieChart
} from '@element-plus/icons-vue'

// 响应式数据
const totalSales = ref(15267.8)
const totalVehicles = ref(4521)
const avgPrice = ref(33.8)
const dealerCount = ref(156)
const chartTimeRange = ref('6m')
const chartPowerType = ref('all')
const rankingType = ref('volume')

// 排行榜数据
const rankingData = ref([
  { model: 'Model Y', sales: 1245, revenue: 4235.2, growth: 15.3, marketShare: 27.5 },
  { model: 'Model 3', sales: 987, revenue: 2961.0, growth: 8.7, marketShare: 21.8 },
  { model: 'Model S', sales: 654, revenue: 5886.0, growth: -2.1, marketShare: 14.5 },
  { model: 'Model X', sales: 432, revenue: 3888.0, growth: 12.9, marketShare: 9.6 },
  { model: 'Cybertruck', sales: 321, revenue: 1926.0, growth: 45.2, marketShare: 7.1 },
])

const PieOption = {
  tooltip: {
    trigger: 'item'
  },
  legend: {
    top: '5%',
    left: 'center'
  },
  series: [
    {
      name: 'Access From',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 40,
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: false
      },
      data: [
        { value: 1048, name: '宝马' },
        { value: 735, name: '丰田' },
        { value: 580, name: '大众' },
        { value: 484, name: 'DB' },
        { value: 300, name: 'AP' }
      ]
    }
  ]
};

// 格式化数字
const formatNumber = (num: number) => {
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 1 })
}

// 刷新数据
const refreshData = () => {
  ElMessage.success('数据已刷新')
  // 这里可以调用API刷新数据
}

// 导出数据
const exportData = () => {
  ElMessage.success('报告导出功能开发中...')
  // 这里可以实现导出功能
}

onMounted(() => {
  ElMessage.success('欢迎使用销售总览页面！')

})

onBeforeUnmount(() => {

})
</script>

<template>
  <div class="sale-total-view">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>销售总览</h2>
          <p>汽车销售数据统计分析</p>
        </div>
        <div class="header-actions">
          <el-radio-group v-model="carType" @change="handleCarTypeChange">
            <el-radio-button value="all">全部车型</el-radio-button>
            <el-radio-button value="electric">新能源车</el-radio-button>
            <el-radio-button value="fuel">燃油车</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleDateRangeChange"
            style="margin-left: 16px"
          />
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-section">
      <!-- 总销量趋势图 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>月度销售趋势</span>
              <el-radio-group v-model="chartPowerType" size="small">
                <el-radio-button value="all">所有</el-radio-button>
                <el-radio-button value="fuel">油车</el-radio-button>
                <el-radio-button value="electricity">新能源车</el-radio-button>
              </el-radio-group>
              <el-radio-group v-model="chartTimeRange" size="small">
                <el-radio-button value="6m">近6个月</el-radio-button>
                <el-radio-button value="1y">近1年</el-radio-button>
                <el-radio-button value="2y">近2年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="salesTrendChart" class="chart-container" v-loading="loading"></div>
        </el-card>
      </el-col>

      <!-- 销售额变化图 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>销售额变化</span>
          </template>
          <div class="ring-pie-chart" style="width: 340px; height: 340px;">
            <VChart :option="PieOption"/> 
          </div>
        </el-card>
      </el-col>

      <!-- 车型销量排行榜 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>车型销量排行</span>
          </template>
          <div ref="topModelsChart" class="chart-container" v-loading="loading"></div>
        </el-card>
      </el-col>

      <!-- 地区销量柱状图 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>地区销量分布</span>
          </template>
          <div ref="regionSalesChart" class="chart-container" v-loading="loading"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()

// 响应式数据
const carType = ref('all') // all, electric, fuel
const timeRange = ref('month') // month, quarter, year
const dateRange = ref<[Date, Date]>([new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), new Date()])
const loading = ref(false)

// 图表实例
const salesTrendChart = ref<HTMLDivElement>()
const salesAmountChart = ref<HTMLDivElement>()
const topModelsChart = ref<HTMLDivElement>()
const regionSalesChart = ref<HTMLDivElement>()

let salesTrendChartInstance: echarts.ECharts | null = null
let salesAmountChartInstance: echarts.ECharts | null = null
let topModelsChartInstance: echarts.ECharts | null = null
let regionSalesChartInstance: echarts.ECharts | null = null

// API接口函数
const fetchSalesTrend = async () => {
  try {
    const response = await axios.get('/api/SaleTotal/TotalSalesTrend', {
      params: {
        date_range_type: timeRange.value,
        car_type: carType.value,
      },
    })

    if (response.data.status === 1) {
      return response.data.data
    } else {
      console.log('API响应失败，使用模拟数据')
      return generateMockSalesTrendData()
    }
  } catch (error) {
    console.log('API调用失败，使用模拟数据:', error)
    return generateMockSalesTrendData()
  }
}

const fetchSalesAmount = async () => {
  try {
    const response = await axios.get('/api/SaleTotal/SalesAmount', {
      params: {
        date_range_type: timeRange.value,
        car_type: carType.value,
      },
    })

    if (response.data.status === 1) {
      return response.data.data
    } else {
      console.log('API响应失败，使用模拟数据')
      return generateMockSalesAmountData()
    }
  } catch (error) {
    console.log('API调用失败，使用模拟数据:', error)
    return generateMockSalesAmountData()
  }
}

const fetchTopModels = async () => {
  try {
    const response = await axios.get('/api/SaleTotal/TopModels', {
      params: {
        date_range_type: timeRange.value,
        car_type: carType.value,
      },
    })

    if (response.data.status === 1) {
      return response.data.data
    } else {
      console.log('API响应失败，使用模拟数据')
      return generateMockTopModelsData()
    }
  } catch (error) {
    console.log('API调用失败，使用模拟数据:', error)
    return generateMockTopModelsData()
  }
}

const fetchRegionSales = async () => {
  try {
    const response = await axios.get('/api/SaleTotal/RegionSales', {
      params: {
        date_range_type: timeRange.value,
        car_type: carType.value,
      },
    })

    if (response.data.status === 1) {
      return response.data.data
    } else {
      console.log('API响应失败，使用模拟数据')
      return generateMockRegionSalesData()
    }
  } catch (error) {
    console.log('API调用失败，使用模拟数据:', error)
    return generateMockRegionSalesData()
  }
}

// 生成模拟数据函数
const generateMockSalesTrendData = () => {
  const data = []
  const baseDate = new Date()
  for (let i = 11; i >= 0; i--) {
    const date = new Date(baseDate.getFullYear(), baseDate.getMonth() - i, 1)
    data.push({
      date: date.toISOString().slice(0, 7), // YYYY-MM 格式
      salesVolume: Math.floor(Math.random() * 2000) + 3000,
    })
  }
  console.log('生成销量趋势模拟数据:', data)
  return data
}

const generateMockSalesAmountData = () => {
  const data = []
  const baseDate = new Date()
  for (let i = 11; i >= 0; i--) {
    const date = new Date(baseDate.getFullYear(), baseDate.getMonth() - i, 1)
    data.push({
      date: date.toISOString().slice(0, 7), // YYYY-MM 格式
      salesAmount: Math.floor(Math.random() * 50000) + 80000,
    })
  }
  console.log('生成销售额模拟数据:', data)
  return data
}

const generateMockTopModelsData = () => {
  const data = [
    { carModel: '特斯拉 Model Y', salesVolume: 1245 },
    { carModel: '比亚迪海豚', salesVolume: 987 },
    { carModel: '理想ONE', salesVolume: 854 },
    { carModel: '蔚来ES6', salesVolume: 732 },
    { carModel: '小鹏P7', salesVolume: 665 },
    { carModel: '奔驰C级', salesVolume: 587 },
    { carModel: '宝马3系', salesVolume: 523 },
    { carModel: '奥迪A4L', salesVolume: 478 },
  ]
  console.log('生成车型排行模拟数据:', data)
  return data
}

const generateMockRegionSalesData = () => {
  const data = [
    { region: '北京', salesVolume: 2345 },
    { region: '上海', salesVolume: 2156 },
    { region: '广东', salesVolume: 1987 },
    { region: '浙江', salesVolume: 1654 },
    { region: '江苏', salesVolume: 1543 },
    { region: '山东', salesVolume: 1432 },
    { region: '四川', salesVolume: 1234 },
    { region: '湖北', salesVolume: 1123 },
  ]
  console.log('生成地区销量模拟数据:', data)
  return data
}

// 初始化销量趋势图
const initSalesTrendChart = async () => {
  console.log('开始初始化销量趋势图')

  if (!salesTrendChart.value) {
    console.error('销量趋势图DOM元素不存在')
    return
  }

  const data = await fetchSalesTrend()
  console.log('获取到销量趋势数据:', data)

  if (salesTrendChartInstance) {
    salesTrendChartInstance.dispose()
  }

  salesTrendChartInstance = echarts.init(salesTrendChart.value)
  console.log('ECharts实例已创建')

  const option = {
    title: {
      text: '销量趋势',
      textStyle: {
        fontSize: 14,
        color: '#333',
      },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        return `${data.axisValue}<br/>销量: ${data.value.toLocaleString()} 辆`
      },
    },
    xAxis: {
      type: 'category',
      data: data.map((item: any) => item.date),
    },
    yAxis: {
      type: 'value',
      name: '销量(辆)',
      axisLabel: {
        formatter: (value: number) => value.toLocaleString(),
      },
    },
    series: [
      {
        name: '销量',
        type: 'line',
        data: data.map((item: any) => item.salesVolume),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          width: 3,
          color: '#409EFF',
        },
        itemStyle: {
          color: '#409EFF',
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.1)' },
            ],
          },
        },
      },
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
  }

  salesTrendChartInstance.setOption(option)
  console.log('销量趋势图配置已设置')

  // 添加点击事件
  salesTrendChartInstance.on('click', (params: any) => {
    navigateToTopModels(params.name)
  })
}

// 初始化销售额变化图
const initSalesAmountChart = async () => {
  console.log('开始初始化销售额变化图')

  if (!salesAmountChart.value) {
    console.error('销售额变化图DOM元素不存在')
    return
  }

  const data = await fetchSalesAmount()
  console.log('获取到销售额数据:', data)

  if (salesAmountChartInstance) {
    salesAmountChartInstance.dispose()
  }

  salesAmountChartInstance = echarts.init(salesAmountChart.value)

  const option = {
    title: {
      text: '销售额变化',
      textStyle: {
        fontSize: 14,
        color: '#333',
      },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        return `${data.axisValue}<br/>销售额: ${data.value.toLocaleString()} 万元`
      },
    },
    xAxis: {
      type: 'category',
      data: data.map((item: any) => item.date),
    },
    yAxis: {
      type: 'value',
      name: '销售额(万元)',
      axisLabel: {
        formatter: (value: number) => value.toLocaleString(),
      },
    },
    series: [
      {
        name: '销售额',
        type: 'line',
        data: data.map((item: any) => item.salesAmount),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          width: 3,
          color: '#67C23A',
        },
        itemStyle: {
          color: '#67C23A',
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.1)' },
            ],
          },
        },
      },
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
  }

  salesAmountChartInstance.setOption(option)

  // 添加点击事件
  salesAmountChartInstance.on('click', (params: any) => {
    navigateToTopModels(params.name)
  })
}

// 初始化车型排行榜
const initTopModelsChart = async () => {
  console.log('开始初始化车型排行榜')

  if (!topModelsChart.value) {
    console.error('车型排行榜DOM元素不存在')
    return
  }

  const data = await fetchTopModels()
  console.log('获取到车型排行数据:', data)

  if (topModelsChartInstance) {
    topModelsChartInstance.dispose()
  }

  topModelsChartInstance = echarts.init(topModelsChart.value)

  const option = {
    title: {
      text: '车型销量排行',
      textStyle: {
        fontSize: 14,
        color: '#333',
      },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter: (params: any) => {
        const data = params[0]
        return `${data.axisValue}<br/>销量: ${data.value.toLocaleString()} 辆`
      },
    },
    xAxis: {
      type: 'value',
      name: '销量(辆)',
      axisLabel: {
        formatter: (value: number) => value.toLocaleString(),
      },
    },
    yAxis: {
      type: 'category',
      data: data.map((item: any) => item.carModel),
      axisLabel: {
        interval: 0,
      },
    },
    series: [
      {
        name: '销量',
        type: 'bar',
        data: data.map((item: any) => item.salesVolume),
        itemStyle: {
          color: '#E6A23C',
        },
        barWidth: '60%',
      },
    ],
    grid: {
      left: '25%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true,
    },
  }

  topModelsChartInstance.setOption(option)
}

// 初始化地区销量图
const initRegionSalesChart = async () => {
  console.log('开始初始化地区销量图')

  if (!regionSalesChart.value) {
    console.error('地区销量图DOM元素不存在')
    return
  }

  const data = await fetchRegionSales()
  console.log('获取到地区销量数据:', data)

  if (regionSalesChartInstance) {
    regionSalesChartInstance.dispose()
  }

  regionSalesChartInstance = echarts.init(regionSalesChart.value)

  const option = {
    title: {
      text: '地区销量分布',
      textStyle: {
        fontSize: 14,
        color: '#333',
      },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter: (params: any) => {
        const data = params[0]
        return `${data.axisValue}<br/>销量: ${data.value.toLocaleString()} 辆`
      },
    },
    xAxis: {
      type: 'category',
      data: data.map((item: any) => item.region),
      axisLabel: {
        interval: 0,
        rotate: 45,
      },
    },
    yAxis: {
      type: 'value',
      name: '销量(辆)',
      axisLabel: {
        formatter: (value: number) => value.toLocaleString(),
      },
    },
    series: [
      {
        name: '销量',
        type: 'bar',
        data: data.map((item: any) => item.salesVolume),
        itemStyle: {
          color: '#F56C6C',
        },
        barWidth: '60%',
      },
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '20%',
      top: '15%',
      containLabel: true,
    },
  }

  regionSalesChartInstance.setOption(option)

  // 添加点击事件
  regionSalesChartInstance.on('click', (params: any) => {
    navigateToHeatMap(params.name)
  })
}

// 处理车型类型变化
const handleCarTypeChange = () => {
  console.log('车型类型变化:', carType.value)
  refreshAllCharts()
}

// 处理时间范围变化
const handleTimeRangeChange = () => {
  console.log('时间范围变化:', timeRange.value)
  refreshAllCharts()
}

// 处理日期范围变化
const handleDateRangeChange = () => {
  console.log('日期范围变化:', dateRange.value)
  refreshAllCharts()
}

// 刷新所有图表
const refreshAllCharts = async () => {
  console.log('开始刷新所有图表')
  loading.value = true

  try {
    // 添加延迟确保DOM已渲染
    await nextTick()
    await new Promise((resolve) => setTimeout(resolve, 100))

    await Promise.all([
      initSalesTrendChart(),
      initSalesAmountChart(),
      initTopModelsChart(),
      initRegionSalesChart(),
    ])

    ElMessage.success('数据已刷新')
    console.log('所有图表刷新完成')
  } catch (error) {
    console.error('刷新图表失败:', error)
    ElMessage.error('刷新数据失败')
  } finally {
    loading.value = false
  }
}

// 刷新数据
const refreshData = () => {
  refreshAllCharts()
}

// 导航到热门车型页面
const navigateToTopModels = (date?: string) => {
  const query: any = {}
  if (date) {
    query.date = date
  }
  router.push({
    name: 'TopCarModelList',
    query,
  })
}

// 导航到购车热力图页面
const navigateToHeatMap = (region?: string) => {
  const query: any = {}
  if (region) {
    query.region = region
  }
  router.push({
    name: 'CarPurchasesHeatMap',
    query,
  })
}

// 窗口大小调整处理
const handleResize = () => {
  nextTick(() => {
    salesTrendChartInstance?.resize()
    salesAmountChartInstance?.resize()
    topModelsChartInstance?.resize()
    regionSalesChartInstance?.resize()
  })
}

// 监听DOM元素变化
watch(
  [salesTrendChart, salesAmountChart, topModelsChart, regionSalesChart],
  () => {
    nextTick(() => {
      if (
        salesTrendChart.value &&
        salesAmountChart.value &&
        topModelsChart.value &&
        regionSalesChart.value
      ) {
        console.log('所有DOM元素已准备就绪，开始初始化图表')
        refreshAllCharts()
      }
    })
  },
  { immediate: false },
)

onMounted(async () => {
  console.log('SaleTotal组件已挂载')
  ElMessage.success('欢迎使用销售总览页面！')

  // 等待DOM元素渲染完成
  await nextTick()
  await new Promise((resolve) => setTimeout(resolve, 200))

  console.log('DOM元素检查:')
  console.log('salesTrendChart:', salesTrendChart.value)
  console.log('salesAmountChart:', salesAmountChart.value)
  console.log('topModelsChart:', topModelsChart.value)
  console.log('regionSalesChart:', regionSalesChart.value)

  // 初始化所有图表
  await refreshAllCharts()

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  console.log('SaleTotal组件将要卸载')

  // 销毁图表实例
  salesTrendChartInstance?.dispose()
  salesAmountChartInstance?.dispose()
  topModelsChartInstance?.dispose()
  regionSalesChartInstance?.dispose()

  // 移除事件监听
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.el-card__body {
  height: 100%;
}

.sale-total-view {
  padding: 0;
  background: #f5f5f5;
}

.page-header {
  margin-bottom: 20px;
  border-radius: 8px;
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
  color: #303133;
  font-size: 24px;
}

.header-left p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.charts-section {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 8px;
  margin-bottom: 20px;
  min-height: 450px;
  background: white;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  color: #303133;
}

.chart-container {
  height: 380px;
  width: 100%;
  min-height: 380px;
  background: white;
  border-radius: 4px;
}

/* 确保图表容器有明确的尺寸 */
.chart-card .el-card__body {
  padding: 20px;
  height: 400px;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: center;
  }

  .chart-container {
    height: 300px;
    min-height: 300px;
  }

  .chart-card {
    min-height: 350px;
  }

  .chart-card .el-card__body {
    height: 320px;
  }
}
</style>

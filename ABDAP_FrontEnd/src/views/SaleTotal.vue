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
              <span style="margin-right: 20px;">总销量趋势</span>
              <span :title="salesTrendAnalysis.salesTrendAnalysisData">{{ salesTrendAnalysis.sales }}</span>
              <el-radio-group v-model="timeRange" size="small" @change="handleTimeRangeChange">
                <el-radio-button value="month">月</el-radio-button>
                <el-radio-button value="quarter">季度</el-radio-button>
                <el-radio-button value="year">年</el-radio-button>
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
            <span style="margin-right: 20px;">销售额变化</span>
            <span :title="salesAmountAnalysis.salesAmountAnalysisData">{{ salesAmountAnalysis.sales }}</span>
          </template>
          <div ref="salesAmountChart" class="chart-container" v-loading="loading"></div>
        </el-card>
      </el-col>

      <!-- 车型销量排行榜 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span style="margin-right: 20px;">车型销量排行</span>
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
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
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

let salesVolStd =ref(0)
let salesVolSlope =ref(0)
let salesAmtStd =ref(0)
let salesAmtSlope =ref(0)

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
  salesVolStd.value = calcStd(data.map(item => item.salesVolume))
  console.log('销量标准差:', salesVolStd.value)
  salesVolSlope.value = calcSlope(data.map(item => item.salesVolume))
  console.log('销量斜率:', salesVolSlope.value)
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
  salesAmtStd.value = calcStd(data.map(item => item.salesAmount))
  console.log('销售额标准差:', salesAmtStd.value)
  salesAmtSlope.value = calcSlope(data.map(item => item.salesAmount))
  console.log('销售额斜率:', salesAmtSlope.value)
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

//数据分析算法，帮助用户解读数据
//标准差，计算数据波动
function calcStd(arr: number[]) {
  const avg = arr.reduce((a, b) => a + b, 0) / arr.length
  const variance = arr.reduce((a, b) => a + (b - avg) ** 2, 0) / arr.length
  return Math.sqrt(variance)
}

//斜率，计算线性趋势
function calcSlope(arr: number[]) {
  const n = arr.length
  const xSum = (n - 1) * n / 2
  const ySum = arr.reduce((a, b) => a + b, 0)
  const xySum = arr.map((y, x) => x * y).reduce((a, b) => a + b, 0)
  const xxSum = arr.map((_, x) => x * x).reduce((a, b) => a + b, 0)
  return (n * xySum - xSum * ySum) / (n * xxSum - xSum * xSum)
}

// 智能解读文本
const salesTrendAnalysis = computed(() => {
  if (salesVolStd.value > 800) {
    if (salesVolSlope.value > 100) {
      return {
        sales: '销量波动较大，但整体呈明显上升趋势，市场活跃且增长迅速。',
        salesTrendAnalysisData: "数据标准差：" + salesVolStd.value.toLocaleString() + "，数据线性趋势：" + salesVolSlope.value.toLocaleString()
      }
    } else if (salesVolSlope.value < -100) {
      return {
        sales: '销量波动较大，且整体呈下降趋势，需关注市场风险。',
        salesTrendAnalysisData: "数据标准差：" + salesVolStd.value.toLocaleString() + "，数据线性趋势：" + salesVolSlope.value.toLocaleString()
      }
    } else {
      return {
        sales: '销量波动较大，整体趋势较为平稳，市场存在不确定性。',
        salesTrendAnalysisData: "数据标准差：" + salesVolStd.value.toLocaleString() + "，数据线性趋势：" + salesVolSlope.value.toLocaleString()
      }
    }
  } else {
    if (salesVolSlope.value > 100) {
      return {
        sales: '销量波动较小，整体呈上升趋势，市场表现稳定且向好。',
        salesTrendAnalysisData: "数据标准差：" + salesVolStd.value.toLocaleString() + "，数据线性趋势：" + salesVolSlope.value.toLocaleString()
      }
    } else if (salesVolSlope.value < -100) {
      return {
        sales: '销量波动较小，但整体呈下降趋势，需警惕市场下滑。',
        salesTrendAnalysisData: "数据标准差：" + salesVolStd.value.toLocaleString() + "，数据线性趋势：" + salesVolSlope.value.toLocaleString()
      }
    } else {
      return {
        sales: '销量波动较小，整体趋势平稳，市场较为稳定。',
        salesTrendAnalysisData: "数据标准差：" + salesVolStd.value.toLocaleString() + "，数据线性趋势：" + salesVolSlope.value.toLocaleString()
      }
    }
  }
})

const salesAmountAnalysis = computed(() => {
  if (salesAmtStd.value > 15000) {
    if (salesAmtSlope.value > 1000) {
      return {
        sales: '销售额波动较大，但整体呈明显上升趋势，市场需求旺盛。',
        salesAmountAnalysisData: "数据标准差："+salesAmtStd.value.toLocaleString()+"，数据线性趋势："+salesAmtSlope.value.toLocaleString()
      }
    } else if (salesAmtSlope.value < -1000) {
      return {
        sales: '销售额波动较大，且整体呈下降趋势，需关注收入风险。',
        salesAmountAnalysisData: "数据标准差："+salesAmtStd.value.toLocaleString()+"，数据线性趋势："+salesAmtSlope.value.toLocaleString()
      }
    } else {
      return {
        sales: '销售额波动较大，整体趋势较为平稳，市场存在不确定性。',
        salesAmountAnalysisData: "数据标准差："+salesAmtStd.value.toLocaleString()+"，数据线性趋势："+salesAmtSlope.value.toLocaleString()
      }
    }
  } else {
    if (salesAmtSlope.value > 1000) {
      return {
        sales: '销售额波动较小，整体呈上升趋势，市场表现良好。',
        salesAmountAnalysisData: "数据标准差："+salesAmtStd.value.toLocaleString()+"，数据线性趋势："+salesAmtSlope.value.toLocaleString()
      }
    } else if (salesAmtSlope.value < -1000) {
      return {
        sales: '销售额波动较小，但整体呈下降趋势，需警惕市场下滑。',
        salesAmountAnalysisData: "数据标准差："+salesAmtStd.value.toLocaleString()+"，数据线性趋势："+salesAmtSlope.value.toLocaleString()
      }
    } else {
      return {
        sales: '销售额波动较小，整体趋势平稳，市场较为稳定。',
        salesAmountAnalysisData: "数据标准差："+salesAmtStd.value.toLocaleString()+"，数据线性趋势："+salesAmtSlope.value.toLocaleString()
      }
    }
  }
})

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

  let data = await fetchTopModels()
  console.log('获取到车型排行数据:', data)

  // 按销量降序排列，销量大的在上面
  data = data.slice().sort((a: any, b: any) => b.salesVolume - a.salesVolume)

  // 定义颜色数组
  const colorList = [
    '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#1abc9c', '#9b59b6', '#e67e22'
  ]

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
    grid: {
      left: '5%',
      right: '15%',
      bottom: '8%',
      top: 50,
      containLabel: true,
    },
    xAxis: {
      type: 'value',
      name: '销量(辆)',
      position: 'top', // x轴放顶部，交点在左上角
      axisLabel: {
        formatter: (value: number) => value.toLocaleString(),
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#eee'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: data.map((item: any) => item.carModel),
      inverse: true, // 让销量大的在上面
      axisLabel: {
        interval: 0,
        fontSize: 14,
        color: '#333'
      },
      axisTick: { show: false }
    },
    series: [
      {
        name: '销量',
        type: 'bar',
        data: data.map((item: any) => item.salesVolume),
        itemStyle: {
          color: (params: any) => colorList[params.dataIndex % colorList.length],
        },
        barWidth: 28,
        label: {
          show: true,
          position: 'right',
          formatter: '{c}',
          fontSize: 13,
          color: '#333'
        },
      },
    ],
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
  overflow-x: auto;
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

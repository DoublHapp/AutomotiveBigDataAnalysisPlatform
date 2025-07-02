<template>
  <div class="car-purchases-heat-map">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>购车热区地图</h2>
          <p>
            {{
              currentLevel === 'country'
                ? '全国汽车销售量热力分布图'
                : `${currentProvince}汽车销售量热力分布图`
            }}
          </p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 筛选控制区 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-content">
        <div class="filter-left">
          <!-- 面包屑导航 -->
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item @click="navigateToLevel('country')">
              <span class="breadcrumb-link" :class="{ active: currentLevel === 'country' }"
                >全国</span
              >
            </el-breadcrumb-item>
            <el-breadcrumb-item
              v-if="currentLevel === 'province'"
              @click="navigateToLevel('province')"
            >
              <span class="breadcrumb-link active">{{ currentProvince }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="filter-right">
          <!-- 时间粒度选择 -->
          <el-radio-group v-model="timeGranularity" @change="handleTimeGranularityChange">
            <el-radio-button value="month">月</el-radio-button>
            <el-radio-button value="quarter">季</el-radio-button>
            <el-radio-button value="year">年</el-radio-button>
          </el-radio-group>

          <!-- 具体时间选择 -->
          <el-date-picker
            v-model="selectedTime"
            :type="datePickerType"
            :placeholder="datePickerPlaceholder"
            :format="datePickerFormat"
            :value-format="datePickerValueFormat"
            @change="handleTimeChange"
            style="margin-left: 16px; width: 200px"
          />

          <!-- 季度选择 (当选择季度时显示) -->
          <el-select
            v-if="timeGranularity === 'quarter'"
            v-model="selectedQuarter"
            placeholder="选择季度"
            @change="handleQuarterChange"
            style="margin-left: 16px; width: 200px"
          >
            <el-option
              v-for="quarter in quarterOptions"
              :key="quarter.value"
              :label="quarter.label"
              :value="quarter.value"
            />
          </el-select>

          <!-- 车型筛选 -->
          <el-select
            v-model="selectedCarModel"
            placeholder="选择车型"
            clearable
            @change="handleCarModelChange"
            style="margin-left: 16px; width: 200px"
          >
            <el-option label="全部车型" value="" />
            <el-option
              v-for="model in carModelList"
              :key="model.carModelId"
              :label="model.modelName"
              :value="model.carModelId"
            />
          </el-select>
        </div>
      </div>
    </el-card>

    <!-- 地图和数据展示区 -->
    <el-row :gutter="20">
      <!-- 中国地图热力图 -->
      <el-col :xs="24" :sm="24" :md="24" :lg="13" :xl="14">
        <el-card shadow="never" class="map-card">
          <template #header>
            <div class="card-header">
              <span>{{ mapTitle }}</span>
              <div class="legend">
                <span class="legend-label">销量热度:</span>
                <div class="legend-gradient">
                  <span class="legend-min">低</span>
                  <div class="gradient-bar"></div>
                  <span class="legend-max">高</span>
                </div>
              </div>
            </div>
          </template>
          <div
            ref="chinaMapChart"
            class="china-map-container"
            v-loading="loading"
            :element-loading-text="
              currentLevel === 'country'
                ? '加载全国地图数据中...'
                : `加载${currentProvince}地图数据中...`
            "
          ></div>
        </el-card>
      </el-col>

      <!-- 散点图分析 -->
      <el-col :xs="24" :sm="24" :md="24" :lg="11" :xl="10">
        <el-card shadow="never" class="scatter-card">
          <template #header>
            <span>{{
              currentLevel === 'country'
                ? '全国汽车销售热力分布'
                : `${currentProvince}汽车销售热力分布`
            }}</span>
          </template>
          <div ref="scatterChart" class="scatter-chart-container" v-loading="loading"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 排行榜和统计 -->
    <el-row :gutter="20">
      <!-- 排行榜 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="ranking-card">
          <template #header>
            <span>{{ currentLevel === 'country' ? '省份' : '城市' }}销量排行</span>
          </template>
          <div class="ranking-content" v-loading="loading">
            <div
              v-for="(item, index) in rankingData"
              :key="item.regionId"
              class="ranking-item"
              :class="{ 'top-three': index < 3 }"
              @click="handleRegionClick(item)"
            >
              <div class="rank-number" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
              <div class="region-info">
                <div class="region-name">{{ item.regionName }}</div>
                <div class="sales-info">
                  <span class="sales-count">{{ item.salesCount.toLocaleString() }}台</span>
                  <span class="growth-rate" :class="item.growthRate >= 0 ? 'positive' : 'negative'">
                    {{ item.growthRate >= 0 ? '+' : '' }}{{ item.growthRate.toFixed(1) }}%
                  </span>
                </div>
              </div>
              <div class="market-share">{{ item.marketShare.toFixed(1) }}%</div>
            </div>

            <!-- 空状态 -->
            <el-empty v-if="rankingData.length === 0 && !loading" description="暂无数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 统计数据 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="stats-card">
          <template #header>
            <span>销量统计概览</span>
          </template>
          <div class="stats-content">
            <div class="stat-item">
              <div class="stat-icon total-sales">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="stat-details">
                <div class="stat-value">{{ totalSales.toLocaleString() }}</div>
                <div class="stat-label">总销量（台）</div>
              </div>
            </div>

            <div class="stat-item">
              <div class="stat-icon total-regions">
                <el-icon><Location /></el-icon>
              </div>
              <div class="stat-details">
                <div class="stat-value">{{ totalRegions }}</div>
                <div class="stat-label">覆盖{{ currentLevel === 'country' ? '省份' : '城市' }}</div>
              </div>
            </div>

            <div class="stat-item">
              <div class="stat-icon avg-growth">
                <el-icon><DataAnalysis /></el-icon>
              </div>
              <div class="stat-details">
                <div class="stat-value">
                  {{ averageGrowth >= 0 ? '+' : '' }}{{ averageGrowth.toFixed(1) }}%
                </div>
                <div class="stat-label">平均增长率</div>
              </div>
            </div>

            <div class="stat-item">
              <div class="stat-icon top-region">
                <el-icon><Trophy /></el-icon>
              </div>
              <div class="stat-details">
                <div class="stat-value">{{ topRegionName }}</div>
                <div class="stat-label">销量冠军</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, TrendCharts, Location, DataAnalysis, Trophy } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

// 数据接口定义
interface RegionSalesData {
  regionId: number
  regionName: string
  salesCount: number
  salesAmount: number
  growthRate: number
  marketShare: number
  longitude?: number
  latitude?: number
  parentRegionId?: number | null
  saleMonth?: string
}

interface CarModel {
  carModelId: number
  modelName: string
  brandName: string
}

// 响应式数据
const loading = ref(false)
const timeGranularity = ref('month') // month, quarter, year
const selectedTime = ref(new Date())
const selectedQuarter = ref('') // 新增：季度选择
const selectedCarModel = ref('')
const currentLevel = ref<'country' | 'province' | 'city'>('country')
const currentProvince = ref('')
const currentProvinceId = ref<number | null>(null)

// 图表相关
const chinaMapChart = ref<HTMLDivElement>()
const scatterChart = ref<HTMLDivElement>()
let chinaMapChartInstance: echarts.ECharts | null = null
let scatterChartInstance: echarts.ECharts | null = null

// 数据
const salesData = ref<RegionSalesData[]>([])
const carModelList = ref<CarModel[]>([])

// 计算属性
const datePickerType = computed(() => {
  switch (timeGranularity.value) {
    case 'month':
      return 'month'
    case 'quarter':
      return 'year' // 季度选择时，年份选择器 + 单独的季度选择框
    case 'year':
      return 'year'
    default:
      return 'month'
  }
})

const datePickerPlaceholder = computed(() => {
  switch (timeGranularity.value) {
    case 'month':
      return '选择月份'
    case 'quarter':
      return '选择年份'
    case 'year':
      return '选择年份'
    default:
      return '选择时间'
  }
})

const datePickerFormat = computed(() => {
  switch (timeGranularity.value) {
    case 'month':
      return 'YYYY-MM'
    case 'quarter':
      return 'YYYY'
    case 'year':
      return 'YYYY'
    default:
      return 'YYYY-MM'
  }
})

const datePickerValueFormat = computed(() => {
  switch (timeGranularity.value) {
    case 'month':
      return 'YYYY-MM'
    case 'quarter':
      return 'YYYY'
    case 'year':
      return 'YYYY'
    default:
      return 'YYYY-MM'
  }
})

// 季度选项
const quarterOptions = computed(() => {
  if (!selectedTime.value) return []

  const year = new Date(selectedTime.value).getFullYear()
  return [
    { label: `${year}年第1季度 (1-3月)`, value: `${year}-Q1` },
    { label: `${year}年第2季度 (4-6月)`, value: `${year}-Q2` },
    { label: `${year}年第3季度 (7-9月)`, value: `${year}-Q3` },
    { label: `${year}年第4季度 (10-12月)`, value: `${year}-Q4` },
  ]
})

const mapTitle = computed(() => {
  if (currentLevel.value === 'country') {
    return '全国汽车销量热力图'
  } else if (currentLevel.value === 'province') {
    return `${currentProvince.value} 汽车销量热力图`
  }
  return '汽车销量热力图'
})

const rankingData = computed(() => {
  return salesData.value.sort((a, b) => b.salesCount - a.salesCount).slice(0, 10)
})

const totalSales = computed(() => {
  return salesData.value.reduce((sum, item) => sum + item.salesCount, 0)
})

const totalRegions = computed(() => {
  return salesData.value.length
})

const averageGrowth = computed(() => {
  if (salesData.value.length === 0) return 0
  const sum = salesData.value.reduce((sum, item) => sum + item.growthRate, 0)
  return sum / salesData.value.length
})

const topRegionName = computed(() => {
  if (salesData.value.length === 0) return '暂无'
  const topRegion = salesData.value.reduce((max, item) =>
    item.salesCount > max.salesCount ? item : max,
  )
  return topRegion.regionName
})

// API 调用函数
const fetchCarModels = async () => {
  try {
    console.log('正在获取车型列表...')
    const response = await axios.get('/api/carModels')
    if (response.data.status === 1) {
      carModelList.value = response.data.data
      console.log('车型列表获取成功:', carModelList.value)
    }
  } catch (error) {
    console.error('获取车型列表失败:', error)
    // 使用模拟数据
    carModelList.value = generateMockCarModels()
    console.log('使用模拟车型数据:', carModelList.value)
  }
}

// 生成模拟车型数据
const generateMockCarModels = (): CarModel[] => {
  return [
    { carModelId: 1, modelName: 'Model Y', brandName: 'Tesla' },
    { carModelId: 2, modelName: 'Model 3', brandName: 'Tesla' },
    { carModelId: 3, modelName: '汉EV', brandName: 'BYD' },
    { carModelId: 4, modelName: 'ES6', brandName: 'NIO' },
    { carModelId: 5, modelName: 'P7', brandName: 'XPeng' },
    { carModelId: 6, modelName: 'Model S', brandName: 'Tesla' },
    { carModelId: 7, modelName: '唐EV', brandName: 'BYD' },
    { carModelId: 8, modelName: 'ES8', brandName: 'NIO' },
    { carModelId: 9, modelName: 'P5', brandName: 'XPeng' },
    { carModelId: 10, modelName: 'Model X', brandName: 'Tesla' },
  ]
}

const fetchSalesData = async () => {
  loading.value = true
  try {
    console.log('正在获取销售数据...')
    const params = {
      timeGranularity: timeGranularity.value,
      selectedTime: formatSelectedTime(),
      carModelId: selectedCarModel.value || null,
      level: currentLevel.value,
      parentRegionId: currentProvinceId.value,
    }

    console.log('API请求参数:', params)
    const response = await axios.get('/api/sales/heatmap', { params })

    if (response.data.status === 1) {
      salesData.value = response.data.data
      console.log('销售数据获取成功:', salesData.value)
    } else {
      throw new Error('API响应失败')
    }
  } catch (error) {
    console.error('获取销售数据失败:', error)
    ElMessage.warning('API调用失败，使用模拟数据')

    // 使用模拟数据
    try {
      salesData.value = await generateMockSalesData()
      console.log('使用模拟销售数据:', salesData.value)
    } catch (mockError) {
      console.error('生成模拟数据失败:', mockError)
      ElMessage.error('数据加载失败')
      salesData.value = []
    }
  } finally {
    loading.value = false
  }
}

// 格式化选择的时间
const formatSelectedTime = () => {
  if (!selectedTime.value) return ''

  const date = new Date(selectedTime.value)
  switch (timeGranularity.value) {
    case 'month':
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
    case 'quarter':
      if (selectedQuarter.value) {
        return selectedQuarter.value // 直接返回 "YYYY-Q1" 格式
      } else {
        // 如果没有选择季度，默认返回当前季度
        const quarter = Math.floor(date.getMonth() / 3) + 1
        return `${date.getFullYear()}-Q${quarter}`
      }
    case 'year':
      return date.getFullYear().toString()
    default:
      return ''
  }
}

// 生成模拟销售数据（基于数据库结构）
const generateMockSalesData = async (): Promise<RegionSalesData[]> => {
  console.log(`生成${currentLevel.value}级别模拟数据`)

  if (currentLevel.value === 'country') {
    // 生成省份级别销售数据
    return generateProvinceData()
  } else if (currentLevel.value === 'province') {
    // 生成城市级别销售数据
    return generateCityData(currentProvince.value)
  } else {
    return []
  }
}

// 生成省份数据（模拟从 region 和 sale_record 表查询）
const generateProvinceData = (): RegionSalesData[] => {
  const provinces = [
    { id: 1, name: '北京市', multiplier: 1.8, lng: 116.4074, lat: 39.9042 },
    { id: 2, name: '上海市', multiplier: 1.7, lng: 121.4737, lat: 31.2304 },
    { id: 3, name: '广东省', multiplier: 2.2, lng: 113.2644, lat: 23.1291 },
    { id: 4, name: '浙江省', multiplier: 1.6, lng: 120.1551, lat: 30.2741 },
    { id: 5, name: '江苏省', multiplier: 1.9, lng: 118.7778, lat: 32.0617 },
    { id: 6, name: '山东省', multiplier: 1.5, lng: 117.0009, lat: 36.6758 },
    { id: 7, name: '四川省', multiplier: 1.3, lng: 104.0665, lat: 30.5728 },
    { id: 8, name: '湖北省', multiplier: 1.2, lng: 114.3896, lat: 30.5156 },
    { id: 9, name: '河南省', multiplier: 1.4, lng: 113.7539, lat: 34.7655 },
    { id: 10, name: '湖南省', multiplier: 1.1, lng: 112.9836, lat: 28.1128 },
    { id: 11, name: '福建省', multiplier: 1.0, lng: 119.2965, lat: 26.0745 },
    { id: 12, name: '安徽省', multiplier: 0.9, lng: 117.2272, lat: 31.8206 },
    { id: 13, name: '河北省', multiplier: 1.0, lng: 114.4995, lat: 38.1006 },
    { id: 14, name: '辽宁省', multiplier: 0.8, lng: 123.4315, lat: 41.8057 },
    { id: 15, name: '陕西省', multiplier: 0.7, lng: 108.9286, lat: 34.2778 },
    { id: 16, name: '重庆市', multiplier: 1.1, lng: 106.5516, lat: 29.563 },
    { id: 17, name: '天津市', multiplier: 1.2, lng: 117.2008, lat: 39.0842 },
    { id: 18, name: '江西省', multiplier: 0.6, lng: 115.8921, lat: 28.6765 },
    { id: 19, name: '广西壮族自治区', multiplier: 0.5, lng: 108.3669, lat: 22.8176 },
    { id: 20, name: '云南省', multiplier: 0.4, lng: 102.7123, lat: 25.0406 },
    { id: 21, name: '吉林省', multiplier: 0.5, lng: 125.3245, lat: 43.8868 },
    { id: 22, name: '山西省', multiplier: 0.6, lng: 112.5489, lat: 37.857 },
    { id: 23, name: '贵州省', multiplier: 0.3, lng: 106.7135, lat: 26.5783 },
    { id: 24, name: '新疆维吾尔自治区', multiplier: 0.25, lng: 87.6177, lat: 43.7928 },
    { id: 25, name: '甘肃省', multiplier: 0.25, lng: 103.8236, lat: 36.0581 },
    { id: 26, name: '海南省', multiplier: 0.3, lng: 110.3312, lat: 20.0311 },
    { id: 27, name: '内蒙古自治区', multiplier: 0.4, lng: 111.6635, lat: 40.8183 },
    { id: 28, name: '宁夏回族自治区', multiplier: 0.15, lng: 106.2581, lat: 38.4681 },
    { id: 29, name: '青海省', multiplier: 0.12, lng: 101.7781, lat: 36.6171 },
    { id: 30, name: '西藏自治区', multiplier: 0.08, lng: 91.132, lat: 29.6604 },
    { id: 31, name: '黑龙江省', multiplier: 0.6, lng: 126.642, lat: 45.756 },
  ]

  return provinces.map((province) => {

    // 根据时间粒度调整基础销量
    let baseMultiplier = 1
    switch (timeGranularity.value) {
      case 'month':
        baseMultiplier = 1
        break
      case 'quarter':
        baseMultiplier = 3 // 季度是月度的3倍
        break
      case 'year':
        baseMultiplier = 12 // 年度是月度的12倍
        break
    }

    // 模拟查询 sale_record 表的聚合数据
    const baseSales = 3000 + Math.floor(Math.random() * 7000)
    const finalSales = Math.floor(baseSales * province.multiplier)

    // 确保最小销量
    let minSales = 300
    if (province.multiplier >= 1.5) minSales = 2000
    else if (province.multiplier >= 1.0) minSales = 1200
    else if (province.multiplier >= 0.5) minSales = 800

    const salesCount = Math.max(finalSales, minSales)
    const averagePrice = 180 + Math.random() * 120 // 18-30万

    // 模拟同比增长率计算
    const growthRate = (Math.random() - 0.3) * 40 // -12% 到 +28%

    // 模拟市场份额计算
    const marketShare = (salesCount / 150000) * 100

    return {
      regionId: province.id,
      regionName: province.name,
      salesCount: salesCount,
      salesAmount: Math.floor(salesCount * averagePrice * 1000), // 转换为元
      growthRate: growthRate,
      marketShare: marketShare,
      longitude: province.lng,
      latitude: province.lat,
      parentRegionId: null, // 省份级别没有父级
      saleMonth: formatSelectedTime(),
    }
  })
}

// 生成城市数据（模拟从 region 和 sale_record 表查询特定省份的城市）
const generateCityData = (provinceName: string): RegionSalesData[] => {
  console.log(`为${provinceName}生成城市销售数据`)

  // 模拟从数据库查询城市数据
  const cityConfigs: Record<string, Array<{ name: string; tier: number; population: number }>> = {
    广东: [
      { name: '广州市', tier: 1, population: 1500 },
      { name: '深圳市', tier: 1, population: 1300 },
      { name: '东莞市', tier: 2, population: 1000 },
      { name: '佛山市', tier: 2, population: 800 },
      { name: '中山市', tier: 3, population: 400 },
      { name: '珠海市', tier: 3, population: 250 },
      { name: '惠州市', tier: 3, population: 600 },
      { name: '江门市', tier: 3, population: 450 },
      { name: '肇庆市', tier: 4, population: 430 },
      { name: '茂名市', tier: 4, population: 610 },
      { name: '湛江市', tier: 4, population: 700 },
      { name: '韶关市', tier: 4, population: 290 },
      { name: '汕头市', tier: 3, population: 560 },
      { name: '汕尾市', tier: 4, population: 300 },
    ],
    江苏: [
      { name: '南京市', tier: 1, population: 930 },
      { name: '苏州市', tier: 2, population: 1270 },
      { name: '无锡市', tier: 2, population: 750 },
      { name: '常州市', tier: 2, population: 530 },
      { name: '南通市', tier: 3, population: 770 },
      { name: '徐州市', tier: 3, population: 900 },
      { name: '盐城市', tier: 3, population: 720 },
      { name: '扬州市', tier: 3, population: 460 },
      { name: '镇江市', tier: 3, population: 320 },
      { name: '泰州市', tier: 3, population: 460 },
    ],
    山东: [
      { name: '济南市', tier: 2, population: 920 },
      { name: '青岛市', tier: 2, population: 1000 },
      { name: '烟台市', tier: 3, population: 710 },
      { name: '潍坊市', tier: 3, population: 940 },
      { name: '临沂市', tier: 3, population: 1100 },
      { name: '淄博市', tier: 3, population: 470 },
      { name: '济宁市', tier: 3, population: 835 },
      { name: '威海市', tier: 3, population: 290 },
    ],
    浙江: [
      { name: '杭州市', tier: 1, population: 1200 },
      { name: '宁波市', tier: 2, population: 850 },
      { name: '温州市', tier: 2, population: 960 },
      { name: '嘉兴市', tier: 3, population: 540 },
      { name: '湖州市', tier: 3, population: 340 },
      { name: '绍兴市', tier: 3, population: 530 },
      { name: '金华市', tier: 3, population: 560 },
      { name: '台州市', tier: 3, population: 610 },
    ],
    北京: [
      { name: '东城区', tier: 1, population: 80 },
      { name: '西城区', tier: 1, population: 110 },
      { name: '朝阳区', tier: 1, population: 380 },
      { name: '丰台区', tier: 2, population: 240 },
      { name: '石景山区', tier: 2, population: 60 },
      { name: '海淀区', tier: 1, population: 360 },
      { name: '门头沟区', tier: 3, population: 30 },
      { name: '房山区', tier: 3, population: 130 },
      { name: '通州区', tier: 2, population: 160 },
      { name: '顺义区', tier: 3, population: 130 },
    ],
    上海: [
      { name: '黄浦区', tier: 1, population: 60 },
      { name: '徐汇区', tier: 1, population: 110 },
      { name: '长宁区', tier: 1, population: 70 },
      { name: '静安区', tier: 1, population: 100 },
      { name: '普陀区', tier: 2, population: 130 },
      { name: '虹口区', tier: 2, population: 75 },
      { name: '杨浦区', tier: 2, population: 130 },
      { name: '浦东新区', tier: 1, population: 560 },
      { name: '闵行区', tier: 2, population: 250 },
      { name: '宝山区', tier: 3, population: 200 },
    ],
  }

  const cities = cityConfigs[provinceName] || [
    { name: `${provinceName}市`, tier: 1, population: 800 },
    { name: `${provinceName}县1`, tier: 3, population: 300 },
    { name: `${provinceName}县2`, tier: 3, population: 250 },
    { name: `${provinceName}县3`, tier: 4, population: 200 },
  ]

  return cities.map((city, index) => {
    // 基于城市等级和人口计算销量
    const tierMultipliers = { 1: 2.5, 2: 1.8, 3: 1.2, 4: 0.8 }
    const tierMultiplier = tierMultipliers[city.tier as keyof typeof tierMultipliers] || 1.0

    const baseSales = Math.floor((city.population * 3 + Math.random() * 500) * tierMultiplier)

    const minSales = city.tier === 1 ? 1000 : city.tier === 2 ? 500 : city.tier === 3 ? 200 : 100
    const finalSales = Math.max(baseSales, minSales)

    const averagePrice = 150 + Math.random() * 100 // 15-25万

    return {
      regionId: index + 1000,
      regionName: city.name,
      salesCount: finalSales,
      salesAmount: Math.floor(finalSales * averagePrice * 1000),
      growthRate: (Math.random() - 0.3) * 40,
      marketShare: (finalSales / 50000) * 100,
      longitude: 116.4074 + (Math.random() - 0.5) * 20,
      latitude: 39.9042 + (Math.random() - 0.5) * 10,
      parentRegionId: currentProvinceId.value,
      saleMonth: formatSelectedTime(),
    }
  })
}

// 省份地图数据URL映射
const provinceMapUrls: Record<string, string> = {
  北京: 'https://geo.datav.aliyun.com/areas_v3/bound/110000_full.json',
  天津: 'https://geo.datav.aliyun.com/areas_v3/bound/120000_full.json',
  上海: 'https://geo.datav.aliyun.com/areas_v3/bound/310000_full.json',
  重庆: 'https://geo.datav.aliyun.com/areas_v3/bound/500000_full.json',
  河北: 'https://geo.datav.aliyun.com/areas_v3/bound/130000_full.json',
  山西: 'https://geo.datav.aliyun.com/areas_v3/bound/140000_full.json',
  辽宁: 'https://geo.datav.aliyun.com/areas_v3/bound/210000_full.json',
  吉林: 'https://geo.datav.aliyun.com/areas_v3/bound/220000_full.json',
  黑龙江: 'https://geo.datav.aliyun.com/areas_v3/bound/230000_full.json',
  江苏: 'https://geo.datav.aliyun.com/areas_v3/bound/320000_full.json',
  浙江: 'https://geo.datav.aliyun.com/areas_v3/bound/330000_full.json',
  安徽: 'https://geo.datav.aliyun.com/areas_v3/bound/340000_full.json',
  福建: 'https://geo.datav.aliyun.com/areas_v3/bound/350000_full.json',
  江西: 'https://geo.datav.aliyun.com/areas_v3/bound/360000_full.json',
  山东: 'https://geo.datav.aliyun.com/areas_v3/bound/370000_full.json',
  河南: 'https://geo.datav.aliyun.com/areas_v3/bound/410000_full.json',
  湖北: 'https://geo.datav.aliyun.com/areas_v3/bound/420000_full.json',
  湖南: 'https://geo.datav.aliyun.com/areas_v3/bound/430000_full.json',
  广东: 'https://geo.datav.aliyun.com/areas_v3/bound/440000_full.json',
  广西: 'https://geo.datav.aliyun.com/areas_v3/bound/450000_full.json',
  海南: 'https://geo.datav.aliyun.com/areas_v3/bound/460000_full.json',
  四川: 'https://geo.datav.aliyun.com/areas_v3/bound/510000_full.json',
  贵州: 'https://geo.datav.aliyun.com/areas_v3/bound/520000_full.json',
  云南: 'https://geo.datav.aliyun.com/areas_v3/bound/530000_full.json',
  陕西: 'https://geo.datav.aliyun.com/areas_v3/bound/610000_full.json',
  甘肃: 'https://geo.datav.aliyun.com/areas_v3/bound/620000_full.json',
  青海: 'https://geo.datav.aliyun.com/areas_v3/bound/630000_full.json',
  宁夏: 'https://geo.datav.aliyun.com/areas_v3/bound/640000_full.json',
  新疆: 'https://geo.datav.aliyun.com/areas_v3/bound/650000_full.json',
  西藏: 'https://geo.datav.aliyun.com/areas_v3/bound/540000_full.json',
  内蒙古: 'https://geo.datav.aliyun.com/areas_v3/bound/150000_full.json',
}

// 加载省份地图数据
const loadProvinceMapData = async (provinceName: string): Promise<string | null> => {
  try {
    console.log(`开始加载 ${provinceName} 省份地图数据...`)

    // 标准化省份名称
    const standardName = getStandardProvinceName(provinceName)
    console.log(`标准化省份名称: ${provinceName} -> ${standardName}`)

    const mapUrl = provinceMapUrls[standardName]
    if (!mapUrl) {
      console.warn(`未找到省份 ${standardName} 的地图数据URL`)
      return null
    }

    console.log(`加载地图数据: ${mapUrl}`)
    const response = await fetch(mapUrl)
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const geoJson = await response.json()
    if (!geoJson || !geoJson.features) {
      throw new Error('Invalid GeoJSON data')
    }

    // 注册省份地图
    const mapId = `${standardName}_province`
    echarts.registerMap(mapId, geoJson)
    console.log(`省份地图注册成功: ${mapId}`)

    return mapId
  } catch (error) {
    console.error(`加载 ${provinceName} 省份地图数据失败:`, error)
    return null
  }
}

// 加载中国地图数据
const loadChinaMapData = async () => {
  try {
    const response = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json')
    const geoJson = await response.json()
    echarts.registerMap('china', geoJson)
    return true
  } catch (error) {
    console.error('加载中国地图数据失败:', error)
    return false
  }
}

// 省份名称映射表
const provinceNameMapping: Record<string, string> = {
  北京市: '北京',
  天津市: '天津',
  上海市: '上海',
  重庆市: '重庆',
  河北省: '河北',
  山西省: '山西',
  辽宁省: '辽宁',
  吉林省: '吉林',
  黑龙江省: '黑龙江',
  江苏省: '江苏',
  浙江省: '浙江',
  安徽省: '安徽',
  福建省: '福建',
  江西省: '江西',
  山东省: '山东',
  河南省: '河南',
  湖北省: '湖北',
  湖南省: '湖南',
  广东省: '广东',
  海南省: '海南',
  四川省: '四川',
  贵州省: '贵州',
  云南省: '云南',
  陕西省: '陕西',
  甘肃省: '甘肃',
  青海省: '青海',
  台湾省: '台湾',
  内蒙古自治区: '内蒙古',
  广西壮族自治区: '广西',
  西藏自治区: '西藏',
  宁夏回族自治区: '宁夏',
  新疆维吾尔自治区: '新疆',
}

const getStandardProvinceName = (mapName: string): string => {
  return provinceNameMapping[mapName] || mapName
}

// 初始化中国地图热力图
const initChinaMap = async () => {
  if (!chinaMapChart.value) return

  await nextTick()

  if (chinaMapChartInstance) {
    chinaMapChartInstance.dispose()
  }

  chinaMapChartInstance = echarts.init(chinaMapChart.value)

  const mapData = salesData.value.map((item) => ({
    name: item.regionName,
    value: item.salesCount,
    salesAmount: item.salesAmount,
    growthRate: item.growthRate,
    marketShare: item.marketShare,
  }))

  if (mapData.length === 0) {
    console.warn('地图数据为空')
    return
  }

  const salesCounts = salesData.value.map((item) => item.salesCount)
  const maxValue = Math.max(...salesCounts)
  const minValue = Math.min(...salesCounts)

  let mapId = 'china'
  if (currentLevel.value === 'province' && currentProvince.value) {
    mapId = `${currentProvince.value}_province`
  }

  const option = {
    title: {
      text:
        currentLevel.value === 'country'
          ? '全国汽车销量热力图'
          : `${currentProvince.value}汽车销量热力图`,
      left: 'center',
      top: 20,
      textStyle: {
        color: '#333',
        fontSize: 16,
        fontWeight: 'bold',
      },
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0,0,0,0.8)',
      borderColor: '#333',
      textStyle: {
        color: '#fff',
        fontSize: 12,
      },
      formatter: (params: any) => {
        if (params.data && params.data.value !== undefined) {
          const data = params.data
          return `
            <div style="padding: 8px;">
              <div style="font-weight: bold; margin-bottom: 6px; color: #4fc3f7;">${params.name}</div>
              <div style="margin-bottom: 3px;">销量：<span style="color: #fff; font-weight: bold;">${data.value?.toLocaleString() || 0}</span> 台</div>
              <div style="margin-bottom: 3px;">销售额：<span style="color: #fff; font-weight: bold;">¥${((data.salesAmount || 0) / 10000).toFixed(0)}</span> 万</div>
              <div style="margin-bottom: 3px;">增长率：<span style="color: ${(data.growthRate || 0) >= 0 ? '#67c23a' : '#f56c6c'}; font-weight: bold;">${(data.growthRate || 0) >= 0 ? '+' : ''}${(data.growthRate || 0).toFixed(1)}%</span></div>
              <div>市场份额：<span style="color: #fff; font-weight: bold;">${(data.marketShare || 0).toFixed(1)}%</span></div>
            </div>
          `
        }
        return `<div style="padding: 8px;"><strong>${params.name}</strong><br/>暂无数据</div>`
      },
    },
    visualMap: {
      min: Math.max(minValue, 1),
      max: maxValue,
      left: 'left',
      top: 'bottom',
      text: ['高', '低'],
      textStyle: {
        color: '#333',
      },
      inRange: {
        color: [
          '#ffffff',
          '#e3f2fd',
          '#bbdefb',
          '#90caf9',
          '#64b5f6',
          '#42a5f5',
          '#2196f3',
          '#1976d2',
          '#ffcdd2',
          '#f48fb1',
          '#f44336',
          '#d32f2f',
          '#b71c1c',
        ],
      },
      calculable: true,
      orient: 'horizontal',
      formatter: (value: number) => value.toLocaleString() + '台',
    },
    geo: {
      map: mapId,
      roam: true,
      scaleLimit: { min: 0.8, max: 3 },
      zoom: currentLevel.value === 'province' ? 1.0 : 1.2,
      itemStyle: {
        borderColor: '#4fc3f7',
        borderWidth: 1,
        areaColor: '#f8f9fa',
      },
      emphasis: {
        itemStyle: {
          areaColor: '#e3f2fd',
          borderWidth: 2,
          borderColor: '#1976d2',
        },
        label: {
          show: true,
          fontSize: 12,
          fontWeight: 'bold',
          color: '#333',
        },
      },
    },
    series: [
      {
        name: currentLevel.value === 'country' ? '销量' : '城市销量',
        type: 'map',
        map: mapId,
        data: mapData,
        geoIndex: 0,
        roam: true,
        emphasis: {
          label: {
            show: true,
            fontSize: 12,
            fontWeight: 'bold',
            color: '#fff',
          },
          itemStyle: {
            areaColor: '#1976d2',
          },
        },
      },
    ],
  }

  chinaMapChartInstance.setOption(option, true)

  // 添加点击事件
  chinaMapChartInstance.on('click', (params: any) => {
    console.log('点击了地图区域:', params)
    if (params.data && currentLevel.value === 'country') {
      drillDownToProvince(params.name)
    } else if (params.data && currentLevel.value === 'province') {
      ElMessage.info(`查看 ${params.name} 详细信息`)
    }
  })

  console.log('地图初始化完成')
}

// 优化散点图配置 - 解决显示不全问题
const initScatterChart = async () => {
  if (!scatterChart.value) return

  await nextTick()

  if (scatterChartInstance) {
    scatterChartInstance.dispose()
  }

  scatterChartInstance = echarts.init(scatterChart.value)

  const data = salesData.value.map((item, index) => [
    index,
    item.salesCount,
    item.salesCount,
    item.regionName,
  ])

  const option = {
    title: {
      text: currentLevel.value === 'country' ? '销售热力分布' : `${currentProvince.value}销售分布`,
      left: 'center',
      top: 5,
      textStyle: {
        color: '#333',
        fontSize: 12,
        fontWeight: 'bold',
      },
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0,0,0,0.8)',
      borderColor: '#333',
      textStyle: {
        color: '#fff',
        fontSize: 12,
      },
      formatter: (params: any) => {
        const [index, sales, value, name] = params.data
        return `
          <div style="padding: 6px;">
            <div style="font-weight: bold; margin-bottom: 4px; color: #4fc3f7;">${name}</div>
            <div>销量: <span style="color: #fff; font-weight: bold;">${sales.toLocaleString()}</span> 台</div>
          </div>
        `
      },
    },
    // 优化grid布局，为visualMap留出空间
    grid: {
      left: '8%',
      right: '15%',
      top: '15%',
      bottom: '12%',
      containLabel: false,
    },
    xAxis: {
      type: 'value',
      name: '序号',
      nameLocation: 'middle',
      nameGap: 20,
      nameTextStyle: {
        fontSize: 10,
        color: '#666',
      },
      axisLabel: {
        fontSize: 9,
        color: '#666',
      },
      splitLine: {
        show: true,
        lineStyle: { color: '#e0e6ed', type: 'dashed', width: 1 },
      },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#ddd' } },
    },
    yAxis: {
      type: 'value',
      name: '销量',
      nameLocation: 'middle',
      nameGap: 30,
      nameTextStyle: {
        fontSize: 10,
        color: '#666',
      },
      axisLabel: {
        formatter: (value: number) => {
          if (value >= 10000) {
            return (value / 10000).toFixed(1) + 'w'
          }
          return value >= 1000 ? (value / 1000).toFixed(0) + 'k' : value.toString()
        },
        fontSize: 9,
        color: '#666',
      },
      splitLine: {
        show: true,
        lineStyle: { color: '#e0e6ed', type: 'dashed', width: 1 },
      },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#ddd' } },
    },
    // 优化visualMap位置和大小
    visualMap: {
      min: Math.min(...salesData.value.map((item) => item.salesCount)),
      max: Math.max(...salesData.value.map((item) => item.salesCount)),
      dimension: 2,
      orient: 'vertical',
      right: 5,
      top: 'center',
      text: ['高', '低'],
      textStyle: {
        fontSize: 9,
        color: '#666',
      },
      calculable: true,
      inRange: {
        color: ['#e3f2fd', '#42a5f5', '#1976d2', '#0d47a1'],
        symbolSize: [12, 35], // 增大气泡大小范围：从 [6, 20] 改为 [12, 35]
      },
      itemWidth: 10,
      itemHeight: 60,
      formatter: (value: number) => {
        if (value >= 10000) {
          return (value / 10000).toFixed(1) + 'w'
        }
        return value >= 1000 ? (value / 1000).toFixed(0) + 'k' : value.toString()
      },
    },
    series: [
      {
        name: '销量分布',
        type: 'scatter',
        data: data,
        symbolSize: (val: number[]) => {
          const maxSales = Math.max(...salesData.value.map((item) => item.salesCount))
          const minSales = Math.min(...salesData.value.map((item) => item.salesCount))
          const ratio = (val[2] - minSales) / (maxSales - minSales)
          return 12 + ratio * 25 // 增大气泡大小范围：从 8-24 改为 12-37
        },
        itemStyle: {
          opacity: 0.8,
          borderWidth: 1,
          borderColor: '#fff',
        },
        emphasis: {
          label: {
            show: true,
            formatter: (param: any) => param.data[3],
            position: 'top',
            fontSize: 10, // 稍微增大标签字体
            fontWeight: 'bold',
            color: '#333',
            backgroundColor: 'rgba(255,255,255,0.9)',
            padding: [3, 8], // 增大标签内边距
            borderRadius: 4,
            borderWidth: 1,
            borderColor: '#ddd',
          },
          itemStyle: {
            borderWidth: 2,
            borderColor: '#1976d2',
            shadowBlur: 10, // 增大阴影效果
            shadowColor: 'rgba(25, 118, 210, 0.6)',
          },
        },
      },
    ],
  }

  scatterChartInstance.setOption(option)
}

// 事件处理函数
const handleTimeGranularityChange = () => {
  console.log('时间粒度变更为:', timeGranularity.value)

  // 重置选择的时间
  selectedTime.value = new Date()

  // 如果切换到季度，初始化季度选择
  if (timeGranularity.value === 'quarter') {
    const currentYear = new Date().getFullYear()
    const currentQuarter = Math.floor(new Date().getMonth() / 3) + 1
    selectedQuarter.value = `${currentYear}-Q${currentQuarter}`
  } else {
    selectedQuarter.value = ''
  }

  // 重新获取数据
  fetchSalesData()
}

const handleTimeChange = () => {
  console.log('时间变更:', selectedTime.value)

  // 如果是季度模式，需要更新季度选择
  if (timeGranularity.value === 'quarter' && selectedTime.value) {
    const year = new Date(selectedTime.value).getFullYear()
    const currentQuarter = selectedQuarter.value.split('-Q')[1] || '1'
    selectedQuarter.value = `${year}-Q${currentQuarter}`
  }

  fetchSalesData()
}

const handleQuarterChange = () => {
  console.log('季度变更:', selectedQuarter.value)
  fetchSalesData()
}

const handleCarModelChange = () => {
  fetchSalesData()
}

const handleRegionClick = (region: RegionSalesData) => {
  if (currentLevel.value === 'country') {
    drillDownToProvince(region.regionName)
  }
}

// 下钻到省份
const drillDownToProvince = async (provinceName: string) => {
  console.log(`开始下钻到省份: ${provinceName}`)

  loading.value = true

  try {
    // 标准化省份名称
    const standardName = getStandardProvinceName(provinceName)

    // 加载省份地图数据
    const mapId = await loadProvinceMapData(standardName)

    if (!mapId) {
      ElMessage.warning(`暂不支持查看 ${standardName} 的详细城市数据`)
      loading.value = false
      return
    }

    // 更新状态
    currentLevel.value = 'province'
    currentProvince.value = standardName
    currentProvinceId.value =
      salesData.value.find((item) => item.regionName === standardName)?.regionId || null

    // 获取省份城市数据
    await fetchSalesData()

    // 重新初始化地图
    await initChinaMap()

    ElMessage.success(`正在查看 ${standardName} 的城市销量分布`)
  } catch (error) {
    console.error('下钻到省份失败:', error)
    ElMessage.error('加载省份数据失败')
  } finally {
    loading.value = false
  }
}

// 导航到指定级别
const navigateToLevel = async (level: 'country' | 'province') => {
  if (level === 'country') {
    currentLevel.value = 'country'
    currentProvince.value = ''
    currentProvinceId.value = null
    await fetchSalesData()
    await initChinaMap()
  } else if (level === 'province' && currentProvince.value) {
    currentLevel.value = 'province'
    await fetchSalesData()
    await initChinaMap()
  }
}

// 刷新数据
const refreshData = async () => {
  ElMessage.success('正在刷新数据...')
  await fetchSalesData()
}

// 监听数据变化，重新渲染图表
watch(
  salesData,
  async () => {
    await nextTick()
    await initChinaMap()
    await initScatterChart()
  },
  { deep: true },
)

// 窗口大小变化时重新调整图表
const handleResize = () => {
  if (chinaMapChartInstance) {
    chinaMapChartInstance.resize()
  }
  if (scatterChartInstance) {
    scatterChartInstance.resize()
  }
}

// 组件生命周期
onMounted(async () => {
  console.log('CarPurchasesHeatMap组件已挂载')

  try {
    // 加载地图数据
    await loadChinaMapData()

    // 并行加载车型列表和销售数据
    await Promise.all([fetchCarModels(), fetchSalesData()])

    // 等待DOM更新
    await nextTick()

    // 初始化图表
    await Promise.all([initChinaMap(), initScatterChart()])

    // 监听窗口大小变化
    window.addEventListener('resize', handleResize)

    console.log('页面初始化完成')
  } catch (error) {
    console.error('页面初始化失败:', error)
    ElMessage.error('页面初始化失败')
  }
})

// 清理函数
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)

  if (chinaMapChartInstance) {
    chinaMapChartInstance.dispose()
    chinaMapChartInstance = null
  }
  if (scatterChartInstance) {
    scatterChartInstance.dispose()
    scatterChartInstance = null
  }
})
</script>

<style scoped>
.car-purchases-heat-map {
  padding: 0;
}

/* 页面头部 */
.page-header {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

/* 筛选区 */
.filter-card {
  margin-bottom: 20px;
}

.filter-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.breadcrumb {
  font-size: 16px;
}

.breadcrumb-link {
  color: #409eff;
  cursor: pointer;
  transition: color 0.3s;
}

.breadcrumb-link:hover {
  color: #66b1ff;
}

.breadcrumb-link.active {
  color: #303133;
  font-weight: 500;
  cursor: default;
}

.breadcrumb-link:not(.active):hover {
  color: #66b1ff;
}

.filter-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

/* 中国地图卡片 */
.map-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.legend {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-label {
  font-size: 12px;
  color: #666;
}

.legend-gradient {
  display: flex;
  align-items: center;
  gap: 4px;
}

.gradient-bar {
  width: 60px;
  height: 12px;
  background: linear-gradient(to right, #ffffff, #2196f3, #f44336, #b71c1c);
  border: 1px solid #ddd;
  border-radius: 2px;
}

.legend-min,
.legend-max {
  font-size: 10px;
  color: #999;
}

.china-map-container {
  width: 100%;
  height: 500px;
}

/* 散点图 */
.scatter-card {
  margin-bottom: 20px;
  min-width: 500px;
}

.scatter-chart-container {
  width: 100%;
  height: 500px;
}

/* 排行榜 */
.ranking-card {
  margin-bottom: 20px;
}

.ranking-content {
  max-height: 400px;
  overflow-y: auto;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.3s ease;
}

.ranking-item:hover {
  background: #f0f9ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.ranking-item.top-three {
  background: linear-gradient(135deg, #fff4e6, #ffe7cc);
}

.rank-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: white;
  margin-right: 12px;
}

.rank-number.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffb300);
}

.rank-number.rank-2 {
  background: linear-gradient(135deg, #c0c0c0, #a9a9a9);
}

.rank-number.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #b8860b);
}

.rank-number:not(.rank-1):not(.rank-2):not(.rank-3) {
  background: linear-gradient(135deg, #74b9ff, #0984e3);
}

.region-info {
  flex: 1;
}

.region-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.sales-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sales-count {
  font-size: 14px;
  color: #606266;
}

.growth-rate {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.growth-rate.positive {
  background: #f0f9ff;
  color: #409eff;
}

.growth-rate.negative {
  background: #fef0f0;
  color: #f56c6c;
}

.market-share {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

/* 统计卡片 */
.stats-card {
  margin-bottom: 20px;
}

.stats-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 24px;
  background: rgba(255, 255, 255, 0.2);
}

.stat-details {
  flex: 1;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  opacity: 0.9;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-content {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-right {
    justify-content: center;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .china-map-container,
  .scatter-chart-container {
    height: 300px;
  }

  .ranking-item {
    padding: 8px;
  }

  .rank-number {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }

  .stats-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .stat-item {
    padding: 12px;
  }

  .stat-value {
    font-size: 18px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
}
</style>

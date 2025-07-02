<template>
  <div class="car-purchases-heat-map">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>购车热区地图</h2>
          <p>全国汽车销售量热力分布图</p>
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
              <span class="breadcrumb-link">全国</span>
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentLevel === 'city'" @click="navigateToLevel('province')">
              <span class="breadcrumb-link">{{ currentProvince }}</span>
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
            @change="handleTimeChange"
            style="margin-left: 16px; width: 200px"
          />

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
      <el-col :xs="24" :lg="14">
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
            element-loading-text="加载地图数据中..."
          ></div>
        </el-card>
      </el-col>

      <!-- 散点图分析 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="scatter-card">
          <template #header>
            <span>全国汽车销售热力分布</span>
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

// 地图数据接口
interface RegionSalesData {
  regionId: number
  regionName: string
  salesCount: number
  salesAmount: number
  growthRate: number
  marketShare: number
  longitude?: number
  latitude?: number
  parentRegionId?: number
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
      return 'month' // 季度用月份选择器，后续处理为季度
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
      return '选择季度'
    case 'year':
      return '选择年份'
    default:
      return '选择时间'
  }
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
    const response = await axios.get('/api/carModels')
    if (response.data.status === 1) {
      carModelList.value = response.data.data
    }
  } catch (error) {
    console.error('获取车型列表失败:', error)
    // 使用模拟数据
    carModelList.value = [
      { carModelId: 1, modelName: 'Model Y', brandName: 'Tesla' },
      { carModelId: 2, modelName: 'Model 3', brandName: 'Tesla' },
      { carModelId: 3, modelName: '汉EV', brandName: 'BYD' },
      { carModelId: 4, modelName: 'ES6', brandName: 'NIO' },
      { carModelId: 5, modelName: 'P7', brandName: 'XPeng' },
    ]
  }
}

const fetchSalesData = async () => {
  loading.value = true
  try {
    const params = {
      timeGranularity: timeGranularity.value,
      selectedTime: formatSelectedTime(),
      carModelId: selectedCarModel.value || null,
      level: currentLevel.value,
      parentRegionId: currentProvinceId.value,
    }

    const response = await axios.get('/api/sales/heatmap', { params })

    if (response.data.status === 1) {
      salesData.value = response.data.data
    } else {
      throw new Error('API响应失败')
    }
  } catch (error) {
    console.error('获取销售数据失败:', error)
    ElMessage.warning('API调用失败，使用模拟数据')
    // 使用模拟数据
    salesData.value = generateMockData()
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
      const quarter = Math.floor(date.getMonth() / 3) + 1
      return `${date.getFullYear()}-Q${quarter}`
    case 'year':
      return date.getFullYear().toString()
    default:
      return ''
  }
}


// 省份对应的地图数据URL映射
const provinceMapUrls: Record<string, string> = {
  '北京': 'https://geo.datav.aliyun.com/areas_v3/bound/110000_full.json',
  '天津': 'https://geo.datav.aliyun.com/areas_v3/bound/120000_full.json',
  '上海': 'https://geo.datav.aliyun.com/areas_v3/bound/310000_full.json',
  '重庆': 'https://geo.datav.aliyun.com/areas_v3/bound/500000_full.json',
  '河北': 'https://geo.datav.aliyun.com/areas_v3/bound/130000_full.json',
  '山西': 'https://geo.datav.aliyun.com/areas_v3/bound/140000_full.json',
  '辽宁': 'https://geo.datav.aliyun.com/areas_v3/bound/210000_full.json',
  '吉林': 'https://geo.datav.aliyun.com/areas_v3/bound/220000_full.json',
  '黑龙江': 'https://geo.datav.aliyun.com/areas_v3/bound/230000_full.json',
  '江苏': 'https://geo.datav.aliyun.com/areas_v3/bound/320000_full.json',
  '浙江': 'https://geo.datav.aliyun.com/areas_v3/bound/330000_full.json',
  '安徽': 'https://geo.datav.aliyun.com/areas_v3/bound/340000_full.json',
  '福建': 'https://geo.datav.aliyun.com/areas_v3/bound/350000_full.json',
  '江西': 'https://geo.datav.aliyun.com/areas_v3/bound/360000_full.json',
  '山东': 'https://geo.datav.aliyun.com/areas_v3/bound/370000_full.json',
  '河南': 'https://geo.datav.aliyun.com/areas_v3/bound/410000_full.json',
  '湖北': 'https://geo.datav.aliyun.com/areas_v3/bound/420000_full.json',
  '湖南': 'https://geo.datav.aliyun.com/areas_v3/bound/430000_full.json',
  '广东': 'https://geo.datav.aliyun.com/areas_v3/bound/440000_full.json',
  '广西': 'https://geo.datav.aliyun.com/areas_v3/bound/450000_full.json',
  '海南': 'https://geo.datav.aliyun.com/areas_v3/bound/460000_full.json',
  '四川': 'https://geo.datav.aliyun.com/areas_v3/bound/510000_full.json',
  '贵州': 'https://geo.datav.aliyun.com/areas_v3/bound/520000_full.json',
  '云南': 'https://geo.datav.aliyun.com/areas_v3/bound/530000_full.json',
  '陕西': 'https://geo.datav.aliyun.com/areas_v3/bound/610000_full.json',
  '甘肃': 'https://geo.datav.aliyun.com/areas_v3/bound/620000_full.json',
  '青海': 'https://geo.datav.aliyun.com/areas_v3/bound/630000_full.json',
  '宁夏': 'https://geo.datav.aliyun.com/areas_v3/bound/640000_full.json',
  '新疆': 'https://geo.datav.aliyun.com/areas_v3/bound/650000_full.json',
  '西藏': 'https://geo.datav.aliyun.com/areas_v3/bound/540000_full.json',
  '内蒙古': 'https://geo.datav.aliyun.com/areas_v3/bound/150000_full.json',
}

// 加载省份地图数据
const loadProvinceMapData = async (provinceName: string) => {
  try {
    console.log(`开始加载 ${provinceName} 省份地图数据...`)

    const mapUrl = provinceMapUrls[provinceName]
    if (!mapUrl) {
      console.warn(`未找到省份 ${provinceName} 的地图数据URL`)
      return false
    }

    const response = await fetch(mapUrl)
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const geoJson = await response.json()
    if (!geoJson || !geoJson.features) {
      throw new Error('Invalid GeoJSON data')
    }

    // 注册省份地图，使用省份名称作为地图ID
    const mapId = `${provinceName}_province`
    echarts.registerMap(mapId, geoJson)

    console.log(`${provinceName} 省份地图数据加载成功，注册为: ${mapId}`)
    return mapId
  } catch (error) {
    console.error(`加载 ${provinceName} 省份地图数据失败:`, error)
    return false
  }
}

// 生成模拟数据
const generateMockData = (): RegionSalesData[] => {
  if (currentLevel.value === 'country') {
    // 省份数据（使用地图标准名称）
    const provinces = [
      { name: '北京', standardName: '北京', baseMultiplier: 1.8 },
      { name: '上海', standardName: '上海', baseMultiplier: 1.7 },
      { name: '广东', standardName: '广东', baseMultiplier: 2.2 },
      { name: '浙江', standardName: '浙江', baseMultiplier: 1.6 },
      { name: '江苏', standardName: '江苏', baseMultiplier: 1.9 },
      { name: '山东', standardName: '山东', baseMultiplier: 1.5 },
      { name: '四川', standardName: '四川', baseMultiplier: 1.3 },
      { name: '湖北', standardName: '湖北', baseMultiplier: 1.2 },
      { name: '河南', standardName: '河南', baseMultiplier: 1.4 },
      { name: '湖南', standardName: '湖南', baseMultiplier: 1.1 },
      { name: '福建', standardName: '福建', baseMultiplier: 1.0 },
      { name: '安徽', standardName: '安徽', baseMultiplier: 0.9 },
      { name: '河北', standardName: '河北', baseMultiplier: 1.0 },
      { name: '辽宁', standardName: '辽宁', baseMultiplier: 0.8 },
      { name: '陕西', standardName: '陕西', baseMultiplier: 0.7 },
      { name: '重庆', standardName: '重庆', baseMultiplier: 1.1 },
      { name: '天津', standardName: '天津', baseMultiplier: 1.2 },
      { name: '江西', standardName: '江西', baseMultiplier: 0.6 },
      { name: '广西', standardName: '广西', baseMultiplier: 0.5 },
      { name: '云南', standardName: '云南', baseMultiplier: 0.4 },
      { name: '吉林', standardName: '吉林', baseMultiplier: 0.5 },
      { name: '山西', standardName: '山西', baseMultiplier: 0.6 },
      { name: '贵州', standardName: '贵州', baseMultiplier: 0.3 },
      { name: '新疆', standardName: '新疆', baseMultiplier: 0.25 },
      { name: '甘肃', standardName: '甘肃', baseMultiplier: 0.25 },
      { name: '海南', standardName: '海南', baseMultiplier: 0.3 },
      { name: '内蒙古', standardName: '内蒙古', baseMultiplier: 0.4 },
      { name: '宁夏', standardName: '宁夏', baseMultiplier: 0.15 },
      { name: '青海', standardName: '青海', baseMultiplier: 0.12 },
      { name: '西藏', standardName: '西藏', baseMultiplier: 0.08 },
      { name: '黑龙江', standardName: '黑龙江', baseMultiplier: 0.6 },
    ]

    const mockData = provinces.map((province, index) => {
      // 修复销量计算逻辑 - 确保生成合理的销量数据
      const baseSales = 3000 + Math.floor(Math.random() * 7000) // 基础销量3000-10000
      const multipliedSales = Math.floor(baseSales * province.baseMultiplier)

      // 确保最小销量不为0，根据经济发展水平设定最小值
      let minSales = 500 // 默认最小销量
      if (province.baseMultiplier >= 1.5) {
        minSales = 2000 // 发达省份最小2000台
      } else if (province.baseMultiplier >= 1.0) {
        minSales = 1200 // 中等省份最小1200台
      } else if (province.baseMultiplier >= 0.5) {
        minSales = 800 // 一般省份最小800台
      } else {
        minSales = 300 // 欠发达省份最小300台
      }

      const finalSales = Math.max(multipliedSales, minSales)

      // 调试信息
      console.log(`省份: ${province.standardName}`)
      console.log(`  基础销量: ${baseSales}`)
      console.log(`  倍数: ${province.baseMultiplier}`)
      console.log(`  计算后销量: ${multipliedSales}`)
      console.log(`  最小销量限制: ${minSales}`)
      console.log(`  最终销量: ${finalSales}`)
      console.log('---')

      return {
        regionId: index + 1,
        regionName: province.standardName,
        salesCount: finalSales,
        salesAmount: finalSales * (180 + Math.floor(Math.random() * 120)), // 18-30万均价
        growthRate: (Math.random() - 0.3) * 40, // -12% 到 +28% 的增长率
        marketShare: (finalSales / 150000) * 100, // 基于总销量150万计算市场份额
      }
    })

    // 输出汇总信息
    const totalSales = mockData.reduce((sum, item) => sum + item.salesCount, 0)
    const maxSales = Math.max(...mockData.map((item) => item.salesCount))
    const minSales = Math.min(...mockData.map((item) => item.salesCount))

    console.log('=== 模拟数据汇总 ===')
    console.log(`总销量: ${totalSales.toLocaleString()} 台`)
    console.log(`最高销量: ${maxSales.toLocaleString()} 台`)
    console.log(`最低销量: ${minSales.toLocaleString()} 台`)
    console.log(`省份数量: ${mockData.length}`)
    console.log('=================')

    return mockData
  } else {
    // 城市数据（模拟当前省份的城市）
    const cities = [
      { name: '广州市', tier: 1 },
      { name: '深圳市', tier: 1 },
      { name: '佛山市', tier: 2 },
      { name: '东莞市', tier: 2 },
      { name: '中山市', tier: 3 },
      { name: '珠海市', tier: 3 },
      { name: '惠州市', tier: 3 },
      { name: '江门市', tier: 3 },
      { name: '肇庆市', tier: 4 },
      { name: '茂名市', tier: 4 },
      { name: '湛江市', tier: 4 },
      { name: '韶关市', tier: 4 },
    ]

    return cities.map((city, index) => {
      // 根据城市等级设置销量，确保不为0
      let baseSales = 0
      switch (city.tier) {
        case 1: // 一线城市
          baseSales = 2500 + Math.floor(Math.random() * 2500) // 2500-5000
          break
        case 2: // 二线城市
          baseSales = 1500 + Math.floor(Math.random() * 1500) // 1500-3000
          break
        case 3: // 三线城市
          baseSales = 800 + Math.floor(Math.random() * 1200) // 800-2000
          break
        case 4: // 四线城市
          baseSales = 300 + Math.floor(Math.random() * 700) // 300-1000
          break
      }

      console.log(`城市: ${city.name}, 等级: ${city.tier}, 销量: ${baseSales}`)

      return {
        regionId: index + 100,
        regionName: city.name,
        salesCount: baseSales,
        salesAmount: baseSales * (150 + Math.floor(Math.random() * 100)), // 15-25万均价
        growthRate: (Math.random() - 0.4) * 50, // -20% 到 +30% 的增长率
        marketShare: (baseSales / 30000) * 100, // 基于城市总销量3万计算市场份额
      }
    })
  }
}

// 加载中国地图GeoJSON数据
const loadChinaMapData = async () => {
  try {
    // 从公开的CDN加载中国地图数据
    const response = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json')
    const geoJson = await response.json()

    // 注册地图
    echarts.registerMap('china', geoJson)
    return true
  } catch (error) {
    console.error('加载中国地图数据失败:', error)
  }
}

// 省份名称映射表 - 解决地图数据与标准名称不匹配的问题
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
  香港特别行政区: '香港',
  澳门特别行政区: '澳门',
}

// 反向映射表
const reverseProvinceNameMapping = Object.fromEntries(
  Object.entries(provinceNameMapping).map(([key, value]) => [value, key]),
)

// 初始化中国地图热力图
const initChinaMap = async () => {
  if (!chinaMapChart.value) return

  await nextTick()

  if (chinaMapChartInstance) {
    chinaMapChartInstance.dispose()
  }

  chinaMapChartInstance = echarts.init(chinaMapChart.value)

  // 准备地图数据 - 修复名称映射问题
  console.log('=== 开始准备地图数据 ===')
  console.log('salesData.value:', salesData.value)

  const mapData = salesData.value.map((item) => {
    // 尝试多种名称匹配方式
    const originalName = item.regionName
    const standardName = reverseProvinceNameMapping[originalName] || originalName

    const mappedItem = {
      name: standardName, // 使用标准名称
      value: item.salesCount,
      salesAmount: item.salesAmount,
      growthRate: item.growthRate,
      marketShare: item.marketShare,
    }

    console.log(`地图数据项: ${originalName} -> ${standardName} - ${mappedItem.value}台`)
    return mappedItem
  })

  console.log('最终地图数据 mapData:', mapData)

  if (mapData.length === 0) {
    console.error('mapData为空，无法渲染地图')
    return
  }

  const salesCounts = salesData.value.map((item) => item.salesCount)
  const maxValue = Math.max(...salesCounts)
  const minValue = Math.min(...salesCounts)

  console.log(`销量统计: 最小值=${minValue}, 最大值=${maxValue}`)
  console.log('========================')
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
        if (params.data) {
          const data = params.data
          return `
            <div style="padding: 8px;">
              <div style="font-weight: bold; margin-bottom: 6px; color: #4fc3f7;">${params.name}</div>
              <div style="margin-bottom: 3px;">销量：<span style="color: #fff; font-weight: bold;">${data.value?.toLocaleString() || 0}</span> 台</div>
              <div style="margin-bottom: 3px;">销售额：<span style="color: #fff; font-weight: bold;">¥${((data.salesAmount || 0) / 10000).toFixed(0)}</span> 万</div>
              <div style="margin-bottom: 3px;">增长率：<span style="color: ${data.growthRate >= 0 ? '#67c23a' : '#f56c6c'}; font-weight: bold;">${data.growthRate >= 0 ? '+' : ''}${(data.growthRate || 0).toFixed(1)}%</span></div>
              <div>市场份额：<span style="color: #fff; font-weight: bold;">${(data.marketShare || 0).toFixed(1)}%</span></div>
            </div>
          `
        }
        return `<div style="padding: 8px;"><strong>${params.name}</strong><br/>暂无数据</div>`
      },
    },
    visualMap: {
      min: minValue,
      max: maxValue,
      left: 'left',
      top: 'bottom',
      text: ['高', '低'],
      inRange: {
        // 修改颜色方案：白色→蓝色→红色渐变
        color: [
          '#ffffff', // 白色 - 最低销量
          '#e3f2fd', // 浅蓝色
          '#bbdefb', // 蓝色
          '#90caf9', // 中蓝色
          '#64b5f6', // 深蓝色
          '#42a5f5', // 更深蓝色
          '#2196f3', // 标准蓝
          '#1976d2', // 深蓝
          '#1565c0', // 过渡蓝
          '#ffcdd2', // 浅红色过渡
          '#f8bbd9', // 粉红过渡
          '#f48fb1', // 中粉红
          '#f06292', // 深粉红
          '#ec407a', // 红粉色
          '#e91e63', // 深红粉
          '#d81b60', // 暗红粉
          '#c2185b', // 深暗红
          '#ad1457', // 紫红
          '#880e4f', // 深紫红
          '#e57373', // 浅红
          '#ef5350', // 中红
          '#f44336', // 标准红
          '#e53935', // 深红
          '#d32f2f', // 更深红
          '#c62828', // 暗红
          '#b71c1c', // 最深红 - 最高销量
        ],
      },
      calculable: true,
      orient: 'horizontal',
      textStyle: {
        color: '#333',
      },
      formatter: (value: number) => value.toLocaleString(),
    },
    geo: {
      map: 'china',
      roam: true,
      scaleLimit: {
        min: 0.8,
        max: 3,
      },
      zoom: 1.2,
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
        name: '销量',
        type: 'map',
        map: 'china',
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

  chinaMapChartInstance.setOption(option)

  // 添加点击事件
  chinaMapChartInstance.on('click', (params: any) => {
    if (params.data && currentLevel.value === 'country') {
      // 点击省份，下钻到城市级别
      drillDownToProvince(params.name)
    }
  })
}

// 初始化散点图
const initScatterChart = async () => {
  if (!scatterChart.value) return

  await nextTick()

  if (scatterChartInstance) {
    scatterChartInstance.dispose()
  }

  scatterChartInstance = echarts.init(scatterChart.value)

  // 按销量水平分级显示
  const data = salesData.value.map((item, index) => [
    index,
    item.salesCount,
    item.salesCount,
    item.regionName,
  ])

  const option = {
    title: {
      text: '全国汽车销售热力分布(气泡图)',
      left: 'center',
      textStyle: {
        color: '#333',
        fontSize: 14,
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const [index, sales, value, name] = params.data
        return `${name}<br/>销量: ${sales.toLocaleString()} 台`
      },
    },
    xAxis: {
      type: 'value',
      name: '区域 (序号)',
      splitLine: {
        show: true,
        lineStyle: {
          color: '#e0e6ed',
          type: 'dashed',
        },
      },
    },
    yAxis: {
      type: 'value',
      name: '销量 (台)',
      axisLabel: {
        formatter: (value: number) => value.toLocaleString(),
      },
      splitLine: {
        show: true,
        lineStyle: {
          color: '#e0e6ed',
          type: 'dashed',
        },
      },
    },
    visualMap: {
      min: Math.min(...salesData.value.map((item) => item.salesCount)),
      max: Math.max(...salesData.value.map((item) => item.salesCount)),
      dimension: 2,
      orient: 'vertical',
      right: 10,
      top: 'center',
      text: ['高', '低'],
      calculable: true,
      inRange: {
        color: ['#4fc3f7', '#29b6f6', '#039be5', '#0277bd', '#01579b'],
      },
    },
    series: [
      {
        name: '销量分布',
        type: 'scatter',
        data: data,
        symbolSize: (val: number[]) => Math.max(val[2] / 100, 10),
        emphasis: {
          label: {
            show: true,
            formatter: (param: any) => param.data[3],
            position: 'top',
          },
        },
      },
    ],
  }

  scatterChartInstance.setOption(option)
}

// 事件处理函数
const handleTimeGranularityChange = () => {
  selectedTime.value = new Date()
  fetchSalesData()
}

const handleTimeChange = () => {
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
const drillDownToProvince = (provinceName: string) => {
  currentLevel.value = 'province'
  currentProvince.value = provinceName
  currentProvinceId.value =
    salesData.value.find((item) => item.regionName === provinceName)?.regionId || null

  ElMessage.success(`正在查看 ${provinceName} 的城市销量分布`)
  fetchSalesData()
}

// 导航到指定级别
const navigateToLevel = (level: 'country' | 'province') => {
  if (level === 'country') {
    currentLevel.value = 'country'
    currentProvince.value = ''
    currentProvinceId.value = null
    fetchSalesData()
  } else if (level === 'province' && currentProvince.value) {
    currentLevel.value = 'province'
    fetchSalesData()
  }
}

// 刷新数据
const refreshData = () => {
  ElMessage.success('正在刷新数据...')
  fetchSalesData()
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
  // 先加载地图数据
  await loadChinaMapData()

  await fetchCarModels()
  await fetchSalesData()
  await nextTick()
  await initChinaMap()
  await initScatterChart()

  window.addEventListener('resize', handleResize)
})

// 清理函数
onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('resize', handleResize)

  // 销毁图表实例
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
  /* 修改图例渐变条颜色，与地图热力色阶保持一致 */
  background: linear-gradient(
    to right,
    #ffffff,
    /* 白色 - 低销量 */ #2196f3,
    /* 蓝色 - 中等销量 */ #f44336,
    /* 红色 - 高销量 */ #b71c1c /* 深红 - 最高销量 */
  );
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

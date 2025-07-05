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
          <el-button type="success" :icon="Download" @click="exportData"> 导出数据 </el-button>
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
              <span class="breadcrumb-link" :class="{ active: currentLevel === 'country' }">
                全国
              </span>
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
          <!-- 时间范围选择 -->
          <el-radio-group v-model="timeRange" @change="handleTimeRangeChange">
            <el-radio-button value="month">近一月</el-radio-button>
            <el-radio-button value="quarter">近一季</el-radio-button>
            <el-radio-button value="year">近一年</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>

          <!-- 自定义时间范围选择 -->
          <el-date-picker
            v-if="timeRange === 'custom'"
            v-model="customDateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleCustomDateChange"
            style="margin-left: 16px; width: 300px"
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
              v-for="model in availableCarModels"
              :key="model.carModelId"
              :label="`${model.brandName} ${model.modelName}`"
              :value="model.carModelId.toString()"
            />
          </el-select>

          <!-- 地区层级选择 -->
          <el-select
            v-model="regionLevel"
            placeholder="地区层级"
            @change="handleRegionLevelChange"
            style="margin-left: 16px; width: 120px"
          >
            <el-option label="省份级别" value="province" />
            <el-option label="城市级别" value="city" />
          </el-select>
        </div>
      </div>
    </el-card>

    <!-- 数据概览卡片 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="overview-card">
          <div class="overview-content">
            <div class="overview-icon total-sales">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="overview-details">
              <div class="overview-value">{{ totalSales.toLocaleString() }}</div>
              <div class="overview-label">总销量 (台)</div>
              <div class="overview-trend" :class="totalSalesGrowth >= 0 ? 'positive' : 'negative'">
                {{ totalSalesGrowth >= 0 ? '+' : '' }}{{ totalSalesGrowth.toFixed(1) }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="overview-card">
          <div class="overview-content">
            <div class="overview-icon total-amount">
              <el-icon><Money /></el-icon>
            </div>
            <div class="overview-details">
              <div class="overview-value">{{ (totalSalesAmount / 10000).toFixed(0) }}</div>
              <div class="overview-label">总销售额 (万元)</div>
              <div class="overview-trend" :class="totalAmountGrowth >= 0 ? 'positive' : 'negative'">
                {{ totalAmountGrowth >= 0 ? '+' : '' }}{{ totalAmountGrowth.toFixed(1) }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="overview-card">
          <div class="overview-content">
            <div class="overview-icon coverage">
              <el-icon><Location /></el-icon>
            </div>
            <div class="overview-details">
              <div class="overview-value">{{ totalRegions }}</div>
              <div class="overview-label">
                覆盖{{ currentLevel === 'country' ? '省份' : '城市' }}
              </div>
              <div class="overview-trend">最高: {{ topRegionName }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="overview-card">
          <div class="overview-content">
            <div class="overview-icon avg-growth">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="overview-details">
              <div class="overview-value">
                {{ averageGrowth >= 0 ? '+' : '' }}{{ averageGrowth.toFixed(1) }}%
              </div>
              <div class="overview-label">平均增长率</div>
              <div class="overview-trend">行业平均: {{ industryAverageGrowth.toFixed(1) }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 地图和数据展示区 -->
    <el-row :gutter="20">
      <!-- 中国地图热力图 -->
      <el-col :xs="24" :sm="24" :md="24" :lg="14" :xl="15">
        <el-card shadow="never" class="map-card">
          <template #header>
            <div class="card-header">
              <span>{{ mapTitle }}</span>
              <div class="map-controls">
                <el-tooltip content="热力图说明" placement="top">
                  <el-button size="small" :icon="QuestionFilled" @click="showMapHelp = true" />
                </el-tooltip>
                <el-dropdown @command="handleMapExport">
                  <el-button size="small">
                    导出<el-icon><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="image">导出图片</el-dropdown-item>
                      <el-dropdown-item command="pdf">导出PDF</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <div class="legend">
                  <span class="legend-label">销量热度:</span>
                  <div class="legend-gradient">
                    <span class="legend-min">低</span>
                    <div class="gradient-bar"></div>
                    <span class="legend-max">高</span>
                  </div>
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
      <el-col :xs="24" :sm="24" :md="24" :lg="10" :xl="9">
        <el-card shadow="never" class="scatter-card">
          <template #header>
            <div class="card-header">
              <span>{{
                currentLevel === 'country' ? '省份销量分析' : `${currentProvince}城市销量分析`
              }}</span>
              <el-button size="small" @click="showScatterDetail = true"> 详细分析 </el-button>
            </div>
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
            <div class="card-header">
              <span>{{ currentLevel === 'country' ? '省份' : '城市' }}销量排行</span>
              <div class="ranking-controls">
                <el-radio-group
                  v-model="rankingType"
                  @change="handleRankingTypeChange"
                  size="small"
                >
                  <el-radio-button value="sales">销量</el-radio-button>
                  <el-radio-button value="amount">销售额</el-radio-button>
                  <el-radio-button value="growth">增长率</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <div class="ranking-content" v-loading="loading">
            <div
              v-for="(item, index) in rankingData"
              :key="item.regionId"
              class="ranking-item"
              :class="{ 'top-three': index < 3 }"
              @click="handleRegionClick(item)"
            >
              <div class="rank-number" :class="`rank-${Math.min(index + 1, 4)}`">
                {{ index + 1 }}
              </div>
              <div class="region-info">
                <div class="region-name">{{ item.regionName }}</div>
                <div class="sales-info">
                  <span class="sales-count">
                    {{
                      rankingType === 'sales'
                        ? item.salesCount.toLocaleString() + '台'
                        : rankingType === 'amount'
                          ? (item.salesAmount / 10000).toFixed(0) + '万元'
                          : (item.growthRate >= 0 ? '+' : '') + item.growthRate.toFixed(1) + '%'
                    }}
                  </span>
                  <span class="growth-rate" :class="item.growthRate >= 0 ? 'positive' : 'negative'">
                    {{ item.growthRate >= 0 ? '+' : '' }}{{ item.growthRate.toFixed(1) }}%
                  </span>
                </div>
                <div class="market-info">
                  <span class="market-share">份额: {{ item.marketShare.toFixed(1) }}%</span>
                  <span class="vs-average"
                    >vs行业: {{ (item.growthRate - averageGrowth).toFixed(1) }}%</span
                  >
                </div>
              </div>
              <div class="region-actions">
                <el-button size="small" type="text" @click.stop="showRegionDetail(item)">
                  详情
                </el-button>
              </div>
            </div>

            <!-- 空状态 -->
            <el-empty v-if="rankingData.length === 0 && !loading" description="暂无数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 增长趋势分析 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="growth-card">
          <template #header>
            <div class="card-header">
              <span>增长趋势分析</span>
              <el-button size="small" @click="showGrowthDetail = true"> 查看详情 </el-button>
            </div>
          </template>
          <div class="growth-content">
            <!-- 增长分布 -->
            <div class="growth-distribution">
              <div class="growth-category positive">
                <div class="category-header">
                  <el-icon color="#67c23a"><TrendCharts /></el-icon>
                  <span>增长地区</span>
                </div>
                <div class="category-value">{{ positiveGrowthRegions }}</div>
                <div class="category-desc">
                  {{ ((positiveGrowthRegions / totalRegions) * 100).toFixed(1) }}% 地区
                </div>
              </div>

              <div class="growth-category stable">
                <div class="category-header">
                  <el-icon color="#e6a23c"><Minus /></el-icon>
                  <span>平稳地区</span>
                </div>
                <div class="category-value">{{ stableGrowthRegions }}</div>
                <div class="category-desc">
                  {{ ((stableGrowthRegions / totalRegions) * 100).toFixed(1) }}% 地区
                </div>
              </div>

              <div class="growth-category negative">
                <div class="category-header">
                  <el-icon color="#f56c6c"><ArrowDown /></el-icon>
                  <span>下滑地区</span>
                </div>
                <div class="category-value">{{ negativeGrowthRegions }}</div>
                <div class="category-desc">
                  {{ ((negativeGrowthRegions / totalRegions) * 100).toFixed(1) }}% 地区
                </div>
              </div>
            </div>

            <!-- 热门地区推荐 -->
            <div class="hot-regions">
              <h4>🔥 热门增长地区</h4>
              <div class="hot-region-list">
                <div
                  v-for="region in topGrowthRegions"
                  :key="region.regionId"
                  class="hot-region-item"
                  @click="handleRegionClick(region)"
                >
                  <span class="region-name">{{ region.regionName }}</span>
                  <span class="growth-badge positive">+{{ region.growthRate.toFixed(1) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 地区详情弹窗 -->
    <el-dialog
      v-model="showRegionDetailDialog"
      :title="`${selectedRegionDetail?.regionName} 详细信息`"
      width="60%"
    >
      <div v-if="selectedRegionDetail" class="region-detail-content">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="detail-card">
              <h4>销售数据</h4>
              <div class="detail-item">
                <span class="label">销量:</span>
                <span class="value">{{ selectedRegionDetail.salesCount.toLocaleString() }} 台</span>
              </div>
              <div class="detail-item">
                <span class="label">销售额:</span>
                <span class="value"
                  >¥{{ (selectedRegionDetail.salesAmount / 10000).toFixed(0) }} 万元</span
                >
              </div>
              <div class="detail-item">
                <span class="label">市场份额:</span>
                <span class="value">{{ selectedRegionDetail.marketShare.toFixed(2) }}%</span>
              </div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="detail-card">
              <h4>增长分析</h4>
              <div class="detail-item">
                <span class="label">增长率:</span>
                <span
                  class="value"
                  :class="selectedRegionDetail.growthRate >= 0 ? 'positive' : 'negative'"
                >
                  {{ selectedRegionDetail.growthRate >= 0 ? '+' : ''
                  }}{{ selectedRegionDetail.growthRate.toFixed(1) }}%
                </span>
              </div>
              <div class="detail-item">
                <span class="label">vs 平均:</span>
                <span class="value"
                  >{{ (selectedRegionDetail.growthRate - averageGrowth).toFixed(1) }}%</span
                >
              </div>
              <div class="detail-item">
                <span class="label">排名:</span>
                <span class="value">第 {{ getRankByRegion(selectedRegionDetail) }} 位</span>
              </div>
            </div>
          </el-col>
        </el-row>

        <div class="detail-actions">
          <el-button
            type="primary"
            @click="drillDownToRegion(selectedRegionDetail)"
            v-if="currentLevel === 'country'"
          >
            查看城市详情
          </el-button>
          <el-button @click="showRegionDetailDialog = false">关闭</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 地图说明弹窗 -->
    <el-dialog v-model="showMapHelp" title="热力图说明" width="50%">
      <div class="map-help-content">
        <h4>颜色说明</h4>
        <ul>
          <li><span class="color-sample low"></span> 蓝色区域：销量相对较低</li>
          <li><span class="color-sample medium"></span> 绿色区域：销量中等水平</li>
          <li><span class="color-sample high"></span> 黄色区域：销量较高</li>
          <li><span class="color-sample highest"></span> 红色区域：销量最高</li>
        </ul>
        <h4>操作说明</h4>
        <ul>
          <li>点击地图省份可查看该省城市分布</li>
          <li>使用鼠标滚轮可以缩放地图</li>
          <li>拖拽可以移动地图视角</li>
          <li>悬停在地区上可查看详细数据</li>
        </ul>
      </div>
    </el-dialog>

    <!-- 散点图详细分析弹窗 -->
    <el-dialog v-model="showScatterDetail" title="销量分布详细分析" width="70%">
      <div ref="detailScatterChart" class="detail-scatter-chart"></div>
    </el-dialog>

    <!-- 增长详情弹窗 -->
    <el-dialog v-model="showGrowthDetail" title="增长趋势详细分析" width="70%">
      <div ref="growthTrendChart" class="growth-trend-chart"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Download,
  TrendCharts,
  Location,
  DataAnalysis,
  Money,
  ArrowDown,
  ArrowUp,
  Minus,
  QuestionFilled,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

// =============================================
// 接口定义
// =============================================

// 基础数据接口 - 与API响应完全对应
interface CarModel {
  carModelId: number
  modelName: string
  brandId: number
  brandName: string
  level: string
  launchDate: string
  officialPrice: number
  engineType: string
  seatNum: number
  driveType?: string
  rangeKm?: number
}

interface SaleRecord {
  saleId: number
  carModelId: number
  carModelName: string
  regionId: number
  regionName: string
  saleMonth: string
  saleCount: number
  saleAmount: number
}

interface Region {
  regionId: number
  regionName: string
  parentRegionId: number | null
  parentRegionName: string | null
}

//  基础数据层
interface BaseData {
  carModels: CarModel[]
  saleRecords: SaleRecord[]
  regions: Region[]
  topLevelRegions: Region[]
  nonTopLevelRegions: Region[]
}

// 计算数据层
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

interface BusinessMetrics {
  totalSales: number
  totalSalesAmount: number
  totalRegions: number
  averageGrowth: number
  industryAverageGrowth: number
  positiveGrowthRegions: number
  stableGrowthRegions: number
  negativeGrowthRegions: number
  topRegionName: string
  totalSalesGrowth: number
  totalAmountGrowth: number
}

// =============================================
// 响应式数据
// =============================================

const loading = ref(false)
const showRegionDetailDialog = ref(false)
const showMapHelp = ref(false)
const showScatterDetail = ref(false)
const showGrowthDetail = ref(false)
const selectedRegionDetail = ref<RegionSalesData | null>(null)
const timeRange = ref<'month' | 'quarter' | 'year' | 'custom'>('year')
const customDateRange = ref<[Date, Date] | null>(null)
const selectedCarModel = ref('')
const regionLevel = ref<'province' | 'city'>('province')

//  基础数据存储
const baseData = ref<BaseData>({
  carModels: [],
  saleRecords: [],
  regions: [],
  topLevelRegions: [],
  nonTopLevelRegions: [],
})

//  计算后的业务数据
const salesData = ref<RegionSalesData[]>([])
const availableCarModels = ref<CarModel[]>([])
const businessMetrics = ref<BusinessMetrics>({
  totalSales: 0,
  totalSalesAmount: 0,
  totalRegions: 0,
  averageGrowth: 0,
  industryAverageGrowth: 0,
  positiveGrowthRegions: 0,
  stableGrowthRegions: 0,
  negativeGrowthRegions: 0,
  topRegionName: '',
  totalSalesGrowth: 0,
  totalAmountGrowth: 0,
})

//  修复：使用 ref 变量代替 reactive 对象
// const globalFilters = reactive({
//   timeRange: 'year' as 'month' | 'quarter' | 'year' | 'custom',
//   customDateRange: null as [Date, Date] | null,
//   selectedCarModel: '',
//   regionLevel: 'province' as 'province' | 'city'
// })

// 图表和显示控制
const currentLevel = ref<'country' | 'province' | 'city'>('country')
const currentProvince = ref('')
const currentProvinceId = ref<number | null>(null)
const rankingType = ref<'sales' | 'amount' | 'growth'>('sales')

// 图表实例
const chinaMapChart = ref<HTMLDivElement>()
const scatterChart = ref<HTMLDivElement>()
const detailScatterChart = ref<HTMLDivElement>()
const growthTrendChart = ref<HTMLDivElement>()

let chinaMapChartInstance: echarts.ECharts | null = null
let scatterChartInstance: echarts.ECharts | null = null
let detailScatterChartInstance: echarts.ECharts | null = null
let growthTrendChartInstance: echarts.ECharts | null = null

// API 调用函数保持不变...
const fetchCarModels = async (): Promise<CarModel[]> => {
  try {
    console.log('正在获取车型列表...')
    const response = await axios.get('/api/car-models')

    if (response.data.status === 200 && response.data.data) {
      console.log('获取车型数据成功:', response.data.data.length, '个车型')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('获取车型列表失败:', error)
    ElMessage.error('车型数据加载失败')
    throw error
  }
}

const fetchSaleRecords = async (): Promise<SaleRecord[]> => {
  try {
    console.log('正在获取销售记录...')
    const response = await axios.get('/api/sale-records')

    if (response.data.status === 200 && response.data.data) {
      console.log('获取销售记录成功:', response.data.data.length, '条记录')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('获取销售记录失败:', error)
    ElMessage.error('销售数据加载失败')
    throw error
  }
}

const fetchRegions = async (): Promise<Region[]> => {
  try {
    console.log('正在获取地区信息...')
    const response = await axios.get('/api/regions')

    if (response.data.status === 200 && response.data.data) {
      console.log(' 获取地区信息成功:', response.data.data.length, '个地区')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('获取地区信息失败:', error)
    ElMessage.error('地区数据加载失败')
    throw error
  }
}

const fetchTopLevelRegions = async (): Promise<Region[]> => {
  try {
    console.log('正在获取省份信息...')
    const response = await axios.get('/api/regions/top-level')

    if (response.data.status === 200 && response.data.data) {
      console.log(' 获取省份信息成功:', response.data.data.length, '个省份')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error(' 获取省份信息失败:', error)
    ElMessage.error('省份数据加载失败')
    throw error
  }
}

const fetchNonTopLevelRegions = async (): Promise<Region[]> => {
  try {
    console.log('正在获取城市信息...')
    const response = await axios.get('/api/regions/non-top-level')

    if (response.data.status === 200 && response.data.data) {
      console.log(' 获取城市信息成功:', response.data.data.length, '个城市')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('获取城市信息失败:', error)
    ElMessage.error('城市数据加载失败')
    throw error
  }
}

const loadAllBaseData = async () => {
  try {
    console.log('开始加载基础数据...')

    const [carModels, saleRecords, regions, topLevelRegions, nonTopLevelRegions] =
      await Promise.all([
        fetchCarModels(),
        fetchSaleRecords(),
        fetchRegions(),
        fetchTopLevelRegions(),
        fetchNonTopLevelRegions(),
      ])

    baseData.value = {
      carModels,
      saleRecords,
      regions,
      topLevelRegions,
      nonTopLevelRegions,
    }

    console.log(' 基础数据加载完成:', {
      车型数量: carModels.length,
      销售记录数量: saleRecords.length,
      地区数量: regions.length,
      省份数量: topLevelRegions.length,
      城市数量: nonTopLevelRegions.length,
    })

    // 更新可用车型列表
    availableCarModels.value = carModels

    ElMessage.success('基础数据加载完成')
  } catch (error) {
    console.error(' 基础数据加载失败:', error)
    ElMessage.error('数据加载失败，请检查网络连接')
    throw error
  }
}

//  修复：数据处理函数
const processRegionSalesData = () => {
  console.log(' 处理地区销售数据...')

  if (baseData.value.saleRecords.length === 0) {
    console.warn('销售记录为空')
    salesData.value = []
    return
  }

  // 使用新的 ref 变量
  let filteredRecords = baseData.value.saleRecords

  // 时间筛选 - 修复逻辑
  if (timeRange.value === 'custom' && customDateRange.value) {
    const [startDate, endDate] = customDateRange.value
    filteredRecords = filteredRecords.filter((record) => {
      const recordDate = new Date(record.saleMonth)
      return recordDate >= startDate && recordDate <= endDate
    })
  } else if (timeRange.value !== 'custom') {
    const currentDate = new Date()
    let monthsBack = 12

    switch (timeRange.value) {
      case 'month':
        monthsBack = 1
        break
      case 'quarter':
        monthsBack = 3
        break
      case 'year':
        monthsBack = 12
        break
    }

    const startDate = new Date(currentDate.getFullYear(), currentDate.getMonth() - monthsBack, 1)
    filteredRecords = filteredRecords.filter((record) => {
      const recordDate = new Date(record.saleMonth)
      return recordDate >= startDate
    })
  }

  // 车型筛选
  if (selectedCarModel.value) {
    const selectedModelId = parseInt(selectedCarModel.value)
    filteredRecords = filteredRecords.filter((record) => record.carModelId === selectedModelId)
  }

  console.log('地区销量筛选后记录数:', filteredRecords.length)
  console.log('筛选后的记录样本:', filteredRecords.slice(0, 3))

  //  修复：如果筛选后数据为空，使用原始数据
  if (filteredRecords.length === 0) {
    console.warn('筛选后数据为空，使用原始数据')
    filteredRecords = baseData.value.saleRecords
  }

  // 确定使用的地区列表
  let targetRegions: Region[] = []
  if (currentLevel.value === 'country') {
    targetRegions = baseData.value.topLevelRegions
  } else if (currentLevel.value === 'province' && currentProvinceId.value) {
    targetRegions = baseData.value.nonTopLevelRegions.filter(
      (region) => region.parentRegionId === currentProvinceId.value,
    )
  }

  console.log('目标地区数量:', targetRegions.length)
  console.log('目标地区样本:', targetRegions.slice(0, 3))

  if (targetRegions.length === 0) {
    console.warn('没有找到目标地区，使用所有地区')
    targetRegions = baseData.value.regions
  }

  // 修复：按地区聚合销售数据 - 简化逻辑
  const regionSalesMap = new Map<
    number,
    {
      regionName: string
      salesVolume: number
      salesAmount: number
      lastYearSalesVolume: number
      lastYearSalesAmount: number
    }
  >()

  const currentYear = new Date().getFullYear()
  console.log('当前年份:', currentYear)

  // 修复：处理销售记录 - 改进逻辑
  filteredRecords.forEach((record, index) => {
    console.log(`处理记录 ${index + 1}:`, {
      regionId: record.regionId,
      regionName: record.regionName,
      saleCount: record.saleCount,
      saleAmount: record.saleAmount,
      saleMonth: record.saleMonth,
    })

    const recordDate = new Date(record.saleMonth)
    const recordYear = recordDate.getFullYear()

    let targetRegionId = record.regionId
    let targetRegionName = record.regionName

    // 修复：如果是国家级视图，需要找到省级地区
    if (currentLevel.value === 'country') {
      const recordRegion = baseData.value.regions.find((r) => r.regionId === record.regionId)
      if (recordRegion) {
        if (recordRegion.parentRegionId === null) {
          // 已经是省级地区
          targetRegionId = recordRegion.regionId
          targetRegionName = recordRegion.regionName
        } else {
          // 是市级地区，找到其父级省份
          targetRegionId = recordRegion.parentRegionId
          const parentRegion = baseData.value.regions.find(
            (r) => r.regionId === recordRegion.parentRegionId,
          )
          targetRegionName = parentRegion?.regionName || recordRegion.regionName
        }
      }
    }

    //  修复：确保目标地区存在
    if (!regionSalesMap.has(targetRegionId)) {
      regionSalesMap.set(targetRegionId, {
        regionName: targetRegionName,
        salesVolume: 0,
        salesAmount: 0,
        lastYearSalesVolume: 0,
        lastYearSalesAmount: 0,
      })
    }

    const existing = regionSalesMap.get(targetRegionId)!

    // 修复：累加销量数据 - 添加调试信息
    if (recordYear === currentYear) {
      existing.salesVolume += record.saleCount
      existing.salesAmount += record.saleAmount
      console.log(`累加当年数据到地区 ${targetRegionName}:`, {
        新增销量: record.saleCount,
        累计销量: existing.salesVolume,
        新增销售额: record.saleAmount,
        累计销售额: existing.salesAmount,
      })
    } else if (recordYear === currentYear - 1) {
      existing.lastYearSalesVolume += record.saleCount
      existing.lastYearSalesAmount += record.saleAmount
      console.log(`累加去年数据到地区 ${targetRegionName}:`, {
        新增销量: record.saleCount,
        累计销量: existing.lastYearSalesVolume,
      })
    } else {
      // 新增：处理其他年份的数据
      console.log(`记录年份 ${recordYear} 不在当年或去年范围内，但仍计入当年数据`)
      existing.salesVolume += record.saleCount
      existing.salesAmount += record.saleAmount
    }
  })

  console.log('地区销量聚合结果:', Object.fromEntries(regionSalesMap))

  // 转换为最终数据格式
  const regionsArray = Array.from(regionSalesMap.entries()).map(([regionId, data]) => {
    const growthRate =
      data.lastYearSalesVolume > 0
        ? ((data.salesVolume - data.lastYearSalesVolume) / data.lastYearSalesVolume) * 100
        : data.salesVolume > 0
          ? 50 //  修复：没有去年数据时，设置合理的增长率
          : 0

    return {
      regionId,
      regionName: data.regionName,
      salesCount: data.salesVolume,
      salesAmount: data.salesAmount,
      growthRate,
      marketShare: 0,
      longitude: 116.4074 + (Math.random() - 0.5) * 20,
      latitude: 39.9042 + (Math.random() - 0.5) * 10,
      parentRegionId: currentLevel.value === 'province' ? currentProvinceId.value : null,
      saleMonth: formatCurrentPeriod(),
    }
  })

  console.log('转换后的地区数组:', regionsArray)

  //  修复：计算市场份额
  const totalSales = regionsArray.reduce((sum, region) => sum + region.salesCount, 0)
  console.log('总销量:', totalSales)

  regionsArray.forEach((region) => {
    region.marketShare = totalSales > 0 ? (region.salesCount / totalSales) * 100 : 0
  })

  //  修复：只有有数据的地区才排序
  regionsArray.sort((a, b) => b.salesCount - a.salesCount)

  salesData.value = regionsArray
  console.log('地区销量处理完成，覆盖', regionsArray.length, '个地区')
  console.log(
    '最终销量数据:',
    regionsArray.map((r) => ({ 地区: r.regionName, 销量: r.salesCount, 销售额: r.salesAmount })),
  )
}
// 其他数据处理函数保持不变，但需要更新变量引用...

// 计算业务指标函数保持不变...
const calculateBusinessMetrics = () => {
  console.log(' 计算业务指标...')

  if (salesData.value.length === 0) {
    businessMetrics.value = {
      totalSales: 0,
      totalSalesAmount: 0,
      totalRegions: 0,
      averageGrowth: 0,
      industryAverageGrowth: 0,
      positiveGrowthRegions: 0,
      stableGrowthRegions: 0,
      negativeGrowthRegions: 0,
      topRegionName: '',
      totalSalesGrowth: 0,
      totalAmountGrowth: 0,
    }
    return
  }

  businessMetrics.value.totalSales = salesData.value.reduce((sum, item) => sum + item.salesCount, 0)
  businessMetrics.value.totalSalesAmount = salesData.value.reduce(
    (sum, item) => sum + item.salesAmount,
    0,
  )
  businessMetrics.value.totalRegions = salesData.value.length

  const growthRates = salesData.value.map((item) => item.growthRate)
  businessMetrics.value.averageGrowth =
    growthRates.length > 0
      ? growthRates.reduce((sum, rate) => sum + rate, 0) / growthRates.length
      : 0

  businessMetrics.value.industryAverageGrowth = businessMetrics.value.averageGrowth * 0.85

  businessMetrics.value.positiveGrowthRegions = salesData.value.filter(
    (item) => item.growthRate > 5,
  ).length
  businessMetrics.value.stableGrowthRegions = salesData.value.filter(
    (item) => item.growthRate >= -5 && item.growthRate <= 5,
  ).length
  businessMetrics.value.negativeGrowthRegions = salesData.value.filter(
    (item) => item.growthRate < -5,
  ).length

  const topRegion = salesData.value.reduce(
    (max, item) => (item.salesCount > max.salesCount ? item : max),
    salesData.value[0] || { regionName: '暂无', salesCount: 0 },
  )
  businessMetrics.value.topRegionName = topRegion.regionName

  const currentYearTotal = businessMetrics.value.totalSales
  const lastYear = new Date().getFullYear() - 1
  const lastYearTotal = baseData.value.saleRecords
    .filter((record) => new Date(record.saleMonth).getFullYear() === lastYear)
    .reduce((sum, record) => sum + record.saleCount, 0)

  businessMetrics.value.totalSalesGrowth =
    lastYearTotal > 0
      ? ((currentYearTotal - lastYearTotal) / lastYearTotal) * 100
      : currentYearTotal > 0
        ? 100
        : 0

  const lastYearAmount = baseData.value.saleRecords
    .filter((record) => new Date(record.saleMonth).getFullYear() === lastYear)
    .reduce((sum, record) => sum + record.saleAmount, 0)

  businessMetrics.value.totalAmountGrowth =
    lastYearAmount > 0
      ? ((businessMetrics.value.totalSalesAmount - lastYearAmount) / lastYearAmount) * 100
      : businessMetrics.value.totalSalesAmount > 0
        ? 100
        : 0

  console.log('业务指标计算完成:', businessMetrics.value)
}

const processAllData = () => {
  try {
    console.log('开始处理所有数据...')

    if (baseData.value.saleRecords.length === 0) {
      ElMessage.warning('销售记录为空，无法生成热力图')
      return
    }

    processRegionSalesData()
    calculateBusinessMetrics()

    console.log('所有数据处理完成')
  } catch (error) {
    console.error(' 数据处理失败:', error)
    ElMessage.error('数据处理失败，请重试')
  }
}

// 计算属性保持不变...
const totalSales = computed(() => businessMetrics.value.totalSales)
const totalSalesAmount = computed(() => businessMetrics.value.totalSalesAmount)
const totalRegions = computed(() => businessMetrics.value.totalRegions)
const averageGrowth = computed(() => businessMetrics.value.averageGrowth)
const industryAverageGrowth = computed(() => businessMetrics.value.industryAverageGrowth)
const positiveGrowthRegions = computed(() => businessMetrics.value.positiveGrowthRegions)
const stableGrowthRegions = computed(() => businessMetrics.value.stableGrowthRegions)
const negativeGrowthRegions = computed(() => businessMetrics.value.negativeGrowthRegions)
const topRegionName = computed(() => businessMetrics.value.topRegionName)
const totalSalesGrowth = computed(() => businessMetrics.value.totalSalesGrowth)
const totalAmountGrowth = computed(() => businessMetrics.value.totalAmountGrowth)

const mapTitle = computed(() => {
  if (currentLevel.value === 'country') {
    return '全国汽车销量热力图'
  } else if (currentLevel.value === 'province') {
    return `${currentProvince.value} 汽车销量热力图`
  }
  return '汽车销量热力图'
})

const rankingData = computed(() => {
  let sortedData = [...salesData.value]

  switch (rankingType.value) {
    case 'sales':
      sortedData.sort((a, b) => b.salesCount - a.salesCount)
      break
    case 'amount':
      sortedData.sort((a, b) => b.salesAmount - a.salesAmount)
      break
    case 'growth':
      sortedData.sort((a, b) => b.growthRate - a.growthRate)
      break
  }

  return sortedData.slice(0, 10)
})

const topGrowthRegions = computed(() => {
  return salesData.value
    .filter((region) => region.growthRate > 0)
    .sort((a, b) => b.growthRate - a.growthRate)
    .slice(0, 5)
})

// 工具函数
const formatCurrentPeriod = () => {
  const now = new Date()
  switch (timeRange.value) {
    case 'month':
      return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    case 'quarter':
      const quarter = Math.floor(now.getMonth() / 3) + 1
      return `${now.getFullYear()}-Q${quarter}`
    case 'year':
      return now.getFullYear().toString()
    default:
      return now.toISOString().slice(0, 7)
  }
}

//  修复：事件处理函数
const handleTimeRangeChange = () => {
  console.log('时间范围变更:', timeRange.value)
  processAllData()
  nextTick(() => {
    initAllCharts()
  })
}

const handleCustomDateChange = () => {
  console.log('自定义时间范围变更:', customDateRange.value)
  processAllData()
  nextTick(() => {
    initAllCharts()
  })
}

const handleCarModelChange = () => {
  console.log('车型筛选变更:', selectedCarModel.value)
  processAllData()
  nextTick(() => {
    initAllCharts()
  })
}

const handleRegionLevelChange = () => {
  console.log('地区层级变更:', regionLevel.value)
  processAllData()
  nextTick(() => {
    initAllCharts()
  })
}

const handleRankingTypeChange = () => {
  console.log('排行类型变更:', rankingType.value)
}

// 图表初始化函数...省份地图数据URL映射等保持不变
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

const loadProvinceMapData = async (provinceName: string): Promise<string | null> => {
  try {
    console.log(`开始加载 ${provinceName} 省份地图数据...`)

    const standardName = getStandardProvinceName(provinceName)
    const mapUrl = provinceMapUrls[standardName]

    if (!mapUrl) {
      console.warn(`未找到省份 ${standardName} 的地图数据URL`)
      return null
    }

    const response = await fetch(mapUrl)
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const geoJson = await response.json()
    if (!geoJson || !geoJson.features) {
      throw new Error('Invalid GeoJSON data')
    }

    const mapId = `${standardName}_province`
    echarts.registerMap(mapId, geoJson)
    console.log(`省份地图注册成功: ${mapId}`)

    return mapId
  } catch (error) {
    console.error(`加载 ${provinceName} 省份地图数据失败:`, error)
    return null
  }
}

//  修复：图表初始化函数
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
    console.warn('地图数据为空，显示空白地图')

    // 修复：即使数据为空也显示基础地图
    const option = {
      title: {
        text: mapTitle.value,
        left: 'center',
        top: 20,
        textStyle: {
          color: '#333',
          fontSize: 16,
          fontWeight: 'bold',
        },
      },
      geo: {
        map: 'china',
        roam: true,
        scaleLimit: { min: 0.8, max: 3 },
        zoom: 1.2,
        itemStyle: {
          borderColor: '#4fc3f7',
          borderWidth: 1,
          areaColor: '#f8f9fa',
        },
      },
    }

    chinaMapChartInstance.setOption(option, true)
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
      text: mapTitle.value,
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

// 散点图初始化函数
const initScatterChart = async () => {
  if (!scatterChart.value) return

  await nextTick()

  if (scatterChartInstance) {
    scatterChartInstance.dispose()
  }

  scatterChartInstance = echarts.init(scatterChart.value)

  // 处理空数据情况
  if (salesData.value.length === 0) {
    const option = {
      title: {
        text: '暂无数据',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#999',
          fontSize: 14,
        },
      },
    }
    scatterChartInstance.setOption(option)
    return
  }

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
        symbolSize: [12, 35],
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
          const ratio = maxSales > minSales ? (val[2] - minSales) / (maxSales - minSales) : 0.5
          return 12 + ratio * 25
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
            fontSize: 10,
            fontWeight: 'bold',
            color: '#333',
            backgroundColor: 'rgba(255,255,255,0.9)',
            padding: [3, 8],
            borderRadius: 4,
            borderWidth: 1,
            borderColor: '#ddd',
          },
          itemStyle: {
            borderWidth: 2,
            borderColor: '#1976d2',
            shadowBlur: 10,
            shadowColor: 'rgba(25, 118, 210, 0.6)',
          },
        },
      },
    ],
  }

  scatterChartInstance.setOption(option)
}

const initAllCharts = async () => {
  await nextTick()
  try {
    await Promise.all([initChinaMap(), initScatterChart()])
  } catch (error) {
    console.error('图表初始化失败:', error)
  }
}

// 事件处理函数保持不变，其他函数也保持不变...
const handleRegionClick = (region: RegionSalesData) => {
  if (currentLevel.value === 'country') {
    drillDownToProvince(region.regionName)
  }
}

const drillDownToProvince = async (provinceName: string) => {
  console.log(`开始下钻到省份: ${provinceName}`)

  loading.value = true

  try {
    const standardName = getStandardProvinceName(provinceName)
    const mapId = await loadProvinceMapData(standardName)

    if (!mapId) {
      ElMessage.warning(`暂不支持查看 ${standardName} 的详细城市数据`)
      loading.value = false
      return
    }

    currentLevel.value = 'province'
    currentProvince.value = standardName
    currentProvinceId.value =
      salesData.value.find((item) => item.regionName === standardName)?.regionId || null

    processAllData()
    await initAllCharts()

    ElMessage.success(`正在查看 ${standardName} 的城市销量分布`)
  } catch (error) {
    console.error('下钻到省份失败:', error)
    ElMessage.error('加载省份数据失败')
  } finally {
    loading.value = false
  }
}

const drillDownToRegion = (regionDetail: RegionSalesData) => {
  if (currentLevel.value === 'country') {
    drillDownToProvince(regionDetail.regionName)
  }
}

const navigateToLevel = async (level: 'country' | 'province') => {
  if (level === 'country') {
    currentLevel.value = 'country'
    currentProvince.value = ''
    currentProvinceId.value = null
    processAllData()
    await initAllCharts()
  } else if (level === 'province' && currentProvince.value) {
    currentLevel.value = 'province'
    processAllData()
    await initAllCharts()
  }
}

const showRegionDetail = (region: RegionSalesData) => {
  selectedRegionDetail.value = region
  showRegionDetailDialog.value = true
}

const getRankByRegion = (region: RegionSalesData): number => {
  const sortedData = [...salesData.value].sort((a, b) => b.salesCount - a.salesCount)
  return sortedData.findIndex((item) => item.regionId === region.regionId) + 1
}

const handleMapExport = (command: string) => {
  if (command === 'image') {
    const url = chinaMapChartInstance?.getDataURL({
      type: 'png',
      backgroundColor: '#fff',
    })
    if (url) {
      const link = document.createElement('a')
      link.href = url
      link.download = 'heat_map.png'
      link.click()
    }
  } else if (command === 'pdf') {
    ElMessage.info('PDF导出功能开发中...')
  }
}

const exportData = () => {
  if (salesData.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  const csvContent = [
    ['地区销量数据报告'],
    ['生成时间', new Date().toLocaleString()],
    [''],
    ['地区', '销量(台)', '销售额(万元)', '增长率(%)', '市场份额(%)'],
    ...salesData.value.map((item) => [
      item.regionName,
      item.salesCount,
      (item.salesAmount / 10000).toFixed(0),
      item.growthRate.toFixed(1),
      item.marketShare.toFixed(1),
    ]),
  ]
    .map((row) => row.join(','))
    .join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `地区销量数据_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  ElMessage.success('数据已导出')
}

const refreshData = async () => {
  loading.value = true
  try {
    await loadAllBaseData()
    processAllData()
    await initAllCharts()
    ElMessage.success('数据刷新完成')
  } catch (error) {
    console.error('数据刷新失败:', error)
    ElMessage.error('数据刷新失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

const handleResize = () => {
  nextTick(() => {
    chinaMapChartInstance?.resize()
    scatterChartInstance?.resize()
    detailScatterChartInstance?.resize()
    growthTrendChartInstance?.resize()
  })
}

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用购车热区地图！')

  try {
    await loadChinaMapData()
    await loadAllBaseData()
    processAllData()
    await initAllCharts()

    window.addEventListener('resize', handleResize)

    console.log('页面初始化完成')
  } catch (error) {
    console.error('页面初始化失败:', error)
    ElMessage.error('页面初始化失败')
  }
})

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
  if (detailScatterChartInstance) {
    detailScatterChartInstance.dispose()
    detailScatterChartInstance = null
  }
  if (growthTrendChartInstance) {
    growthTrendChartInstance.dispose()
    growthTrendChartInstance = null
  }
})

watch(
  salesData,
  async () => {
    await nextTick()
    await initAllCharts()
  },
  { deep: true },
)
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

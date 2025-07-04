<template>
  <div class="top-car-model-list">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>热门车型排行榜</h2>
          <p>基于销量数据的智能化购车决策起点</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
          <el-button
            type="success"
            :icon="Download"
            @click="exportRanking"
            :disabled="!hotCarList.length"
          >
            导出排行
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 筛选配置区 -->
    <el-card shadow="never" class="filter-card">
      <template #header>
        <span>筛选条件</span>
      </template>

      <div class="filter-content">
        <el-row :gutter="16">
          <el-col :span="4">
            <el-form-item label="时间范围:">
              <el-select v-model="timeRange" @change="handleFilterChange">
                <el-option label="本月" value="month" />
                <el-option label="本季度" value="quarter" />
                <el-option label="本年度" value="year" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="价格区间:">
              <el-select v-model="priceRange" @change="handleFilterChange">
                <el-option label="全部价格" value="all" />
                <el-option label="10万以下" value="0-10" />
                <el-option label="10-20万" value="10-20" />
                <el-option label="20-30万" value="20-30" />
                <el-option label="30万以上" value="30+" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="动力类型:">
              <el-select v-model="powerType" @change="handleFilterChange">
                <el-option label="全部类型" value="all" />
                <el-option label="纯电动" value="electric" />
                <el-option label="混合动力" value="hybrid" />
                <el-option label="燃油车" value="fuel" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="车型类别:">
              <el-select v-model="carType" @change="handleFilterChange">
                <el-option label="全部车型" value="all" />
                <el-option label="轿车" value="sedan" />
                <el-option label="SUV" value="suv" />
                <el-option label="MPV" value="mpv" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="地区筛选:">
              <el-select v-model="region" @change="handleFilterChange" filterable>
                <el-option label="全国" value="all" />
                <el-option label="北京" value="beijing" />
                <el-option label="上海" value="shanghai" />
                <el-option label="广东" value="guangdong" />
                <el-option label="浙江" value="zhejiang" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-button type="primary" @click="resetFilters" :icon="RefreshRight">
              重置筛选
            </el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 快速对比工具 -->
    <el-card shadow="never" class="quick-compare-card" v-if="selectedModels.length > 0">
      <template #header>
        <div class="compare-header">
          <span>快速对比 ({{ selectedModels.length }}/3)</span>
          <div class="compare-actions">
            <el-button size="small" @click="clearComparison">清空对比</el-button>
            <el-button
              size="small"
              type="primary"
              @click="startComparison"
              :disabled="selectedModels.length < 2"
            >
              开始对比
            </el-button>
          </div>
        </div>
      </template>

      <div class="selected-models">
        <div v-for="model in selectedModels" :key="model.id" class="selected-model-item">
          <img :src="model.image" :alt="model.name" class="model-thumb" />
          <div class="model-info">
            <span class="model-name">{{ model.brand }} {{ model.name }}</span>
            <span class="model-price">{{ model.priceRange }}</span>
          </div>
          <el-button
            size="small"
            type="danger"
            :icon="Close"
            @click="removeFromComparison(model.id)"
          />
        </div>
      </div>
    </el-card>

    <!-- 热门排行榜主体 -->
    <el-card shadow="never" class="ranking-card">
      <template #header>
        <div class="ranking-header">
          <span>热门车型 TOP {{ displayCount }}</span>
          <div class="ranking-controls">
            <el-radio-group v-model="rankingType" @change="handleRankingTypeChange" size="small">
              <el-radio-button value="sales">销量排行</el-radio-button>
              <el-radio-button value="hot">热度排行</el-radio-button>
              <el-radio-button value="value">性价比排行</el-radio-button>
            </el-radio-group>
            <el-select
              v-model="displayCount"
              @change="handleDisplayCountChange"
              style="width: 100px; margin-left: 12px"
            >
              <el-option label="TOP 10" :value="10" />
              <el-option label="TOP 20" :value="20" />
              <el-option label="TOP 50" :value="50" />
            </el-select>
          </div>
        </div>
      </template>

      <div class="car-list" v-loading="loading">
        <!-- 热门车型列表 -->
        <div
          v-for="(car, index) in paginatedCarList"
          :key="car.id"
          class="car-item"
          :class="{
            'top-three': index < 3,
            selected: isSelected(car.id),
            'trending-up': car.trendDirection === 'up',
            'trending-down': car.trendDirection === 'down',
          }"
          @click="handleCarItemClick(car)"
        >
          <!-- 排名徽章 -->
          <div class="ranking">
            <span class="rank-number" :class="`rank-${index + 1}`">
              {{ index + 1 }}
            </span>
            <div class="rank-trend" v-if="car.rankChange !== 0">
              <el-icon v-if="car.rankChange > 0" class="trend-up"><CaretTop /></el-icon>
              <el-icon v-else class="trend-down"><CaretBottom /></el-icon>
              <span class="trend-value">{{ Math.abs(car.rankChange) }}</span>
            </div>
          </div>

          <!-- 车型图片 -->
          <div class="car-image">
            <img :src="car.image" :alt="car.name" @error="handleImageError" />
            <div class="image-overlay" v-if="car.isHot || car.isNew">
              <el-tag v-if="car.isHot" type="danger" size="small">HOT</el-tag>
              <el-tag v-if="car.isNew" type="success" size="small">NEW</el-tag>
            </div>
          </div>

          <!-- 车型基本信息 -->
          <div class="car-info">
            <h3>{{ car.name }}</h3>
            <p class="brand">{{ car.brand }}</p>
            <div class="specs">
              <span class="spec-item">{{ car.type }}</span>
              <span class="spec-item">{{ car.engine }}</span>
              <span class="spec-item">{{ car.transmission }}</span>
            </div>
            <div class="key-features" v-if="car.keyFeatures && car.keyFeatures.length > 0">
              <el-tag
                v-for="feature in car.keyFeatures.slice(0, 3)"
                :key="feature"
                size="small"
                class="feature-tag"
              >
                {{ feature }}
              </el-tag>
            </div>
          </div>

          <!-- 核心数据统计 -->
          <div class="car-stats">
            <div class="stat-item main-stat">
              <span class="label">{{
                rankingType === 'sales' ? '销量' : rankingType === 'hot' ? '热度指数' : '性价比评分'
              }}</span>
              <span class="value primary">
                {{
                  rankingType === 'sales'
                    ? car.sales.toLocaleString() + '台'
                    : rankingType === 'hot'
                      ? car.hotIndex
                      : car.valueScore.toFixed(1)
                }}
              </span>
              <span class="change" :class="getChangeType(car.salesGrowth)">
                {{ car.salesGrowth >= 0 ? '+' : '' }}{{ car.salesGrowth.toFixed(1) }}%
              </span>
            </div>
            <div class="stat-item">
              <span class="label">价格区间</span>
              <span class="value">{{ car.priceRange }}</span>
            </div>
            <div class="stat-item">
              <span class="label">用户评分</span>
              <div class="rating-wrapper">
                <el-rate v-model="car.rating" disabled show-score size="small" />
                <span class="rating-count">({{ car.reviewCount }}评价)</span>
              </div>
            </div>
          </div>

          <!-- 操作按钮区 -->
          <div class="car-actions">
            <el-button
              size="small"
              @click.stop="toggleComparison(car)"
              :type="isSelected(car.id) ? 'primary' : ''"
              :disabled="!isSelected(car.id) && selectedModels.length >= 3"
            >
              {{ isSelected(car.id) ? '已选' : '对比' }}
            </el-button>
            <el-button size="small" type="primary" @click.stop="viewDetails(car)"> 详情 </el-button>
            <el-dropdown @click.stop trigger="click" size="small">
              <el-button size="small" :icon="MoreFilled" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="viewTrendChart(car)">
                    <el-icon><TrendCharts /></el-icon>
                    查看趋势
                  </el-dropdown-item>
                  <el-dropdown-item @click="shareModel(car)">
                    <el-icon><Share /></el-icon>
                    分享车型
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

        </div>

        <!-- 空状态 -->
        <el-empty v-if="!loading && hotCarList.length === 0" description="暂无符合条件的车型数据" />
      </div>

      <!-- 分页控件 -->
      <div class="pagination-wrapper" v-if="hotCarList.length > displayCount">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="displayCount"
          :total="hotCarList.length"
          layout="prev, pager, next, jumper, total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 车型详情抽屉 -->
    <el-drawer v-model="showDetailDrawer" title="车型详情" direction="rtl" size="50%">
      <div class="detail-content" v-if="selectedCarDetail">
        <!-- 车型详情内容 -->
        <div class="detail-header">
          <img :src="selectedCarDetail.image" :alt="selectedCarDetail.name" class="detail-image" />
          <div class="detail-basic">
            <h2>{{ selectedCarDetail.brand }} {{ selectedCarDetail.name }}</h2>
            <p class="detail-price">{{ selectedCarDetail.priceRange }}</p>
            <div class="detail-tags">
              <el-tag type="primary">{{ selectedCarDetail.type }}</el-tag>
              <el-tag type="success">{{ selectedCarDetail.engine }}</el-tag>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="detail-sections">
          <div class="detail-section">
            <h4>市场表现</h4>
            <el-row :gutter="16">
              <el-col :span="12">
                <div class="detail-metric">
                  <span class="metric-label">当月销量</span>
                  <span class="metric-value">{{ selectedCarDetail.sales.toLocaleString() }}台</span>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="detail-metric">
                  <span class="metric-label">同比增长</span>
                  <span class="metric-value" :class="getChangeType(selectedCarDetail.salesGrowth)">
                    {{ selectedCarDetail.salesGrowth >= 0 ? '+' : ''
                    }}{{ selectedCarDetail.salesGrowth.toFixed(1) }}%
                  </span>
                </div>
              </el-col>
            </el-row>
          </div>

          <div class="detail-section">
            <h4>用户反馈</h4>
            <div class="user-feedback">
              <el-rate v-model="selectedCarDetail.rating" disabled show-score />
              <span class="feedback-count">基于{{ selectedCarDetail.reviewCount }}条真实评价</span>
            </div>
          </div>
        </div>

        <div class="detail-actions">
          <el-button type="primary" size="large" @click="addToComparison(selectedCarDetail)">
            加入对比
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 趋势图表弹窗 -->
    <el-dialog v-model="showTrendDialog" title="销量趋势分析" width="70%">
      <div class="trend-chart-container">
        <div ref="trendChart" class="trend-chart" style="height: 400px"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  RefreshRight,
  Close,
  CaretTop,
  CaretBottom,
  MoreFilled,
  Star,
  TrendCharts,
  Share,
  Bell,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()

// 接口定义
interface CarModel {
  id: number
  name: string
  brand: string
  type: string
  engine: string
  transmission: string
  priceRange: string
  avgPrice?: number      // 新增：平均价格字段
  sales: number
  hotIndex: number
  valueScore: number
  rating: number
  reviewCount: number
  salesGrowth: number
  rankChange: number
  trendDirection: 'up' | 'down' | 'stable'
  image: string
  isHot: boolean
  isNew: boolean
  keyFeatures: string[]
}

interface SelectedModel {
  id: number
  name: string
  brand: string
  priceRange: string
  image: string
}


// 响应式数据
const loading = ref(false)
const analyzing = ref(false)

// 筛选条件
const timeRange = ref('month')
const priceRange = ref('all')
const powerType = ref('all')
const carType = ref('all')
const region = ref('all')

// 排行榜配置
const rankingType = ref('sales')
const displayCount = ref(10)
const currentPage = ref(1)

// 车型数据
const hotCarList = ref<CarModel[]>([])
const selectedModels = ref<SelectedModel[]>([])

// 详情抽屉
const showDetailDrawer = ref(false)
const selectedCarDetail = ref<CarModel | null>(null)

// 趋势图表
const showTrendDialog = ref(false)
const trendChart = ref<HTMLDivElement>()
let trendChartInstance: echarts.ECharts | null = null

// 计算属性
const paginatedCarList = computed(() => {
  const start = (currentPage.value - 1) * displayCount.value
  const end = start + displayCount.value
  return hotCarList.value.slice(start, end)
})

// 工具函数
const getChangeType = (value: number) => {
  if (value > 5) return 'positive'
  if (value < -5) return 'negative'
  return 'neutral'
}


const isSelected = (carId: number) => {
  return selectedModels.value.some((model) => model.id === carId)
}

// 热度指数计算函数
const calculateHotIndex = (car: any) => {
  const baseHotIndex = 500 // 统一基础分

  // 销量影响 (40%权重)
  const salesImpact = (car.sales / 1000) * 20

  // 增长趋势影响 (25%权重)
  let trendImpact = 0
  const growth = car.salesGrowth || 0
  if (growth > 20) {
    trendImpact = 150
  } else if (growth > 10) {
    trendImpact = 100
  } else if (growth > 0) {
    trendImpact = 50
  } else if (growth > -10) {
    trendImpact = -30
  } else {
    trendImpact = -100
  }

  // 用户评分影响 (10%权重)
  const rating = car.rating || 3.0
  const ratingImpact = (rating - 3.0) * 50

  return Math.floor(baseHotIndex + salesImpact + trendImpact + ratingImpact)
}


// 性价比评分计算函数
const calculateValueScore = (avgPrice: number, engineType: string) => {
  // 1. 配置价值分 (满分100分)
  const configValueScore = calculateConfigValueScore(engineType)
  
  // 2. 使用成本分 (满分100分) 
  const usageCostScore = calculateUsageCostScore(engineType, avgPrice)
  
  // 3. 价格调整系数
  const priceAdjustmentFactor = calculatePriceAdjustmentFactor(avgPrice)
  
  // 4. 最终性价比评分 = (配置价值分 + 使用成本分) / 价格调整系数
  const finalScore = (configValueScore + usageCostScore) / priceAdjustmentFactor
  
  // 限制在0-10分范围内
  return Math.min(10, Math.max(0, finalScore))
}

// 配置价值分计算
const calculateConfigValueScore = (engineType: string): number => {
  const baseConfigScore = Math.random() * 30 + 60 // 基础配置分60-90
  
  // 根据动力类型调整配置价值
  const engineBonus = {
    '纯电动': 15,      // 新能源科技配置加分
    '混合动力': 10,    // 混动技术加分
    '2.0T': 5,         // 涡轮增压技术加分
    '1.5T': 3,         // 小排量涡轮加分
    '3.0L': 0          // 传统大排量无加分
  }
  
  return Math.min(100, baseConfigScore + (engineBonus[engineType] || 0))
}

// 使用成本分计算 (成本越低分数越高)
const calculateUsageCostScore = (engineType: string, avgPrice: number): number => {
  const baseCostScore = 70 // 基础使用成本分
  
  // 根据动力类型调整使用成本分
  const engineCostScore = {
    '纯电动': 30,      // 电费便宜，使用成本低
    '混合动力': 20,    // 油电混合，成本中等
    '1.5T': 10,        // 小排量省油
    '2.0T': 0,         // 中等排量正常成本
    '3.0L': -15        // 大排量油耗高，成本高
  }
  
  // 价格越高的车，保养成本通常越高
  const priceCostPenalty = avgPrice > 40 ? -10 : avgPrice > 25 ? -5 : 0
  
  const finalCostScore = baseCostScore + (engineCostScore[engineType] || 0) + priceCostPenalty
  
  return Math.min(100, Math.max(0, finalCostScore))
}

// 价格调整系数计算
const calculatePriceAdjustmentFactor = (avgPrice: number): number => {
  // 根据价格区间设定不同的调整系数
  if (avgPrice <= 15) {
    return 1.8        // 经济型车价格期望较低，系数较大
  } else if (avgPrice <= 25) {
    return 2.0        // 主流车型标准系数
  } else if (avgPrice <= 40) {
    return 2.3        // 中高端车型期望较高
  } else {
    return 2.6        // 豪华车型期望很高
  }
}

// API 调用函数
const fetchHotCarList = async () => {
  try {
    const params = {
      timeRange: timeRange.value,
      priceRange: priceRange.value,
      powerType: powerType.value,
      carType: carType.value,
      region: region.value,
      rankingType: rankingType.value,
      limit: 50,
    }

    const response = await axios.get('/api/rankings/cars', { params })
    if (response.data.status === 1) {
      // 对后端返回的数据计算热度指数和性价比评分
      hotCarList.value = response.data.data.map((car) => ({
        ...car,
        hotIndex: calculateHotIndex(car),
        valueScore: calculateValueScore(car.avgPrice || parseAvgPrice(car.priceRange), car.engine)
      }))
    } else {
      hotCarList.value = generateMockCarList()
    }
  } catch (error) {
    console.error('获取热门车型失败:', error)
    hotCarList.value = generateMockCarList()
  }
}

// 从价格区间字符串解析平均价格
const parseAvgPrice = (priceRange: string): number => {
  const match = priceRange.match(/(\d+\.?\d*)-(\d+\.?\d*)万/)
  if (match) {
    const min = parseFloat(match[1])
    const max = parseFloat(match[2])
    return (min + max) / 2
  }
  return 25 // 默认值
}


// 模拟数据生成
const generateMockCarList = (): CarModel[] => {
 const brands = [
    'Tesla',
    'BYD',
    'NIO',
    'XPeng',
    'Li Auto',
    'BMW',
    'Audi',
    'Mercedes',
    'Toyota',
    'Honda',
  ]
  const models = [
    'Model Y',
    'Model 3',
    '汉EV',
    'ES6',
    'P7',
    '理想ONE',
    'iX3',
    'e-tron',
    'GLE',
    'Camry',
  ]
  const types = ['SUV', 'Sedan', 'MPV']
  const engines = ['纯电动', '混合动力', '2.0T', '1.5T', '3.0L']
  const transmissions = ['单速变速器', 'CVT', '8AT', '7DCT']

  return Array.from({ length: 50 }, (_, index) => {
    const brand = brands[index % brands.length]
    const model = models[index % models.length]
    const sales = Math.floor(Math.random() * 10000) + 1000
    const salesGrowth = (Math.random() - 0.3) * 50 // -15% 到 +35%
    const userRating = Math.random() * 2 + 3 // 3-5星
    
    // 模拟车型价格（万元）
    const priceMin = Math.random() * 40 + 10 // 10-50万
    const priceMax = priceMin + Math.random() * 20 + 10 // 比最低价高10-30万
    const avgPrice = (priceMin + priceMax) / 2

    // 计算热度指数（保持原有逻辑）
    const baseHotIndex = 500
    const salesImpact = (sales / 1000) * 20
    
    let trendImpact = 0
    if (salesGrowth > 20) {
      trendImpact = 150
    } else if (salesGrowth > 10) {
      trendImpact = 100
    } else if (salesGrowth > 0) {
      trendImpact = 50
    } else if (salesGrowth > -10) {
      trendImpact = -30
    } else {
      trendImpact = -100
    }
    
    const ratingImpact = (userRating - 3.0) * 50
    const hotIndex = Math.floor(baseHotIndex + salesImpact + trendImpact + ratingImpact)

    // 计算性价比评分：(配置价值分 + 使用成本分) / 价格调整系数
    const valueScore = calculateValueScore(avgPrice, engines[index % engines.length])

    return {
      id: index + 1,
      name: model,
      brand: brand,
      type: types[index % types.length],
      engine: engines[index % engines.length],
      transmission: transmissions[index % transmissions.length],
      priceRange: `${priceMin.toFixed(0)}-${priceMax.toFixed(0)}万`,
      avgPrice: avgPrice, // 添加平均价格字段用于计算
      sales: sales,
      hotIndex: hotIndex,
      valueScore: valueScore,
      rating: userRating,
      reviewCount: Math.floor(Math.random() * 5000) + 100,
      salesGrowth: salesGrowth,
      rankChange: Math.floor((Math.random() - 0.5) * 10),
      trendDirection: salesGrowth > 5 ? 'up' : salesGrowth < -5 ? 'down' : 'stable',
      image: `https://picsum.photos/300/200?random=${index}`,
      isHot: hotIndex > 800,
      isNew: Math.random() > 0.8,
      keyFeatures: [
        '智能驾驶',
        '续航500km',
        '快充技术',
        '全景天窗',
        '真皮座椅',
        '无线充电',
        '语音控制',
        '自动泊车',
      ]
        .sort(() => Math.random() - 0.5)
        .slice(0, 3),
    }
  })
}


// 事件处理函数
const handleFilterChange = async () => {
  loading.value = true
  currentPage.value = 1 // 重置分页
  try {
    await fetchHotCarList()
    ElMessage.success('筛选已更新')
  } catch (error) {
    ElMessage.error('筛选更新失败')
  } finally {
    loading.value = false
  }
}

const handleRankingTypeChange = async () => {
  loading.value = true
  try {
    await fetchHotCarList()

    // 根据排行榜类型进行排序
    if (rankingType.value === 'sales') {
      hotCarList.value.sort((a, b) => b.sales - a.sales)
    } else if (rankingType.value === 'hot') {
      hotCarList.value.sort((a, b) => b.hotIndex - a.hotIndex)
    } else if (rankingType.value === 'value') {
      hotCarList.value.sort((a, b) => b.valueScore - a.valueScore)
    }

    ElMessage.info(
      `已切换到${rankingType.value === 'sales' ? '销量' : rankingType.value === 'hot' ? '热度' : '性价比'}排行`,
    )
  } catch (error) {
    ElMessage.error('排行榜切换失败')
  } finally {
    loading.value = false
  }
}

const handleDisplayCountChange = () => {
  currentPage.value = 1 // 重置分页
  ElMessage.info(`显示数量已调整为TOP ${displayCount.value}`)
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  // 滚动到顶部
  document.querySelector('.ranking-card')?.scrollIntoView({ behavior: 'smooth' })
}

const resetFilters = async () => {
  timeRange.value = 'month'
  priceRange.value = 'all'
  powerType.value = 'all'
  carType.value = 'all'
  region.value = 'all'
  currentPage.value = 1

  await handleFilterChange()
  ElMessage.success('筛选条件已重置')
}

// 对比功能
const toggleComparison = (car: CarModel) => {
  const isAlreadySelected = isSelected(car.id)

  if (isAlreadySelected) {
    removeFromComparison(car.id)
  } else {
    if (selectedModels.value.length >= 3) {
      ElMessage.warning('最多只能选择3款车型进行对比')
      return
    }

    const selectedModel: SelectedModel = {
      id: car.id,
      name: car.name,
      brand: car.brand,
      priceRange: car.priceRange,
      image: car.image,
    }

    selectedModels.value.push(selectedModel)
    ElMessage.success(`${car.brand} ${car.name} 已加入对比`)
  }
}

const removeFromComparison = (carId: number) => {
  const index = selectedModels.value.findIndex((model) => model.id === carId)
  if (index > -1) {
    const removedModel = selectedModels.value.splice(index, 1)[0]
    ElMessage.info(`${removedModel.brand} ${removedModel.name} 已移出对比`)
  }
}

const clearComparison = () => {
  selectedModels.value = []
  ElMessage.info('对比列表已清空')
}

const startComparison = () => {
  if (selectedModels.value.length < 2) {
    ElMessage.warning('至少选择2款车型才能进行对比')
    return
  }

  // 跳转到车型对比页面
  const modelIds = selectedModels.value.map((model) => model.id).join(',')
  router.push({
    name: 'VehicleModelCompAnalysis',
    query: { models: modelIds },
  })
}

// 车型操作
const handleCarItemClick = (car: CarModel) => {
  // 点击车型项时的处理逻辑
  ElMessage.info(`查看 ${car.brand} ${car.name} 的详细信息`)
}

// 修改为只处理CarModel
const viewDetails = (car: CarModel) => {
  selectedCarDetail.value = car
  showDetailDrawer.value = true
}

const addToComparison = (car: CarModel) => {
  toggleComparison(car)
  showDetailDrawer.value = false
}


const viewTrendChart = async (car: CarModel) => {
  showTrendDialog.value = true
  await nextTick()
  await initTrendChart(car)
}

const shareModel = (car: CarModel) => {
  // 模拟分享功能
  const shareUrl = `${window.location.origin}/car/${car.id}`

  if (navigator.share) {
    navigator.share({
      title: `${car.brand} ${car.name}`,
      text: `查看这款车的详细信息和用户评价`,
      url: shareUrl,
    })
  } else {
    // 复制到剪贴板
    navigator.clipboard
      .writeText(shareUrl)
      .then(() => {
        ElMessage.success('分享链接已复制到剪贴板')
      })
      .catch(() => {
        ElMessage.error('分享失败')
      })
  }
}

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'https://via.placeholder.com/300x200?text=Car+Image'
}

// 数据刷新
const refreshData = async () => {
  loading.value = true
  try {
    await fetchHotCarList()
    ElMessage.success('数据已刷新')
  } catch (error) {
    ElMessage.error('数据刷新失败')
  } finally {
    loading.value = false
  }
}


const exportRanking = () => {
  if (hotCarList.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  const csvContent = [
    ['排名', '车型', '品牌', '销量', '价格区间', '用户评分'],
    ...hotCarList.value
      .slice(0, displayCount.value)
      .map((car, index) => [
        index + 1,
        car.name,
        car.brand,
        car.sales,
        car.hotIndex,
        car.priceRange,
        car.rating.toFixed(1),
      ]),
  ]
    .map((row) => row.join(','))
    .join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `热门车型排行榜_${timeRange.value}_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  ElMessage.success('排行榜数据已导出')
}

// 图表初始化
const initTrendChart = async (car: CarModel) => {
  if (!trendChart.value) return

  if (trendChartInstance) {
    trendChartInstance.dispose()
  }

  trendChartInstance = echarts.init(trendChart.value)

  // 模拟趋势数据
  const months = ['1月', '2月', '3月', '4月', '5月', '6月']
  const salesData = months.map(() => Math.floor(Math.random() * 2000) + car.sales - 1000)

  const option = {
    title: {
      text: `${car.brand} ${car.name} 销量趋势`,
      left: 'center',
      textStyle: { fontSize: 16 },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: ['月销量'],
      top: 40,
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
      axisLabel: { fontSize: 12 },
    },
    yAxis: {
      type: 'value',
      name: '销量(台)',
      axisLabel: { fontSize: 12 },
    },
    series: [
      {
        name: '月销量',
        type: 'line',
        data: salesData,
        smooth: true,
        itemStyle: { color: '#409EFF' },
        lineStyle: { width: 3 },
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
  }

  trendChartInstance.setOption(option)
}

// 窗口大小调整
const handleResize = () => {
  if (trendChartInstance) {
    trendChartInstance.resize()
  }
}

// 监听筛选条件变化
watch([timeRange, rankingType], () => {
  if (hotCarList.value.length > 0) {
    handleFilterChange()
  }
})

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用热门车型排行榜！')

  try {
    await fetchHotCarList()
    window.addEventListener('resize', handleResize)
  } catch (error) {
    console.error('页面初始化失败:', error)
    ElMessage.error('初始化失败，部分功能可能不可用')
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)

  if (trendChartInstance) {
    trendChartInstance.dispose()
  }
})
</script>

<style scoped>
/* 整体布局 */
.top-car-model-list {
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

/* 筛选配置卡片 */
.filter-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.filter-content {
  padding: 8px 0;
}

.filter-content .el-row {
  align-items: end;
}

.filter-content .el-form-item {
  margin-bottom: 8px;
}

.filter-content .el-form-item__label {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.filter-content .el-select {
  width: 100%;
}

/* 快速对比工具卡片 */
.quick-compare-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #4facfe;
  overflow: hidden;
}

.compare-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #4facfe;
  font-size: 16px;
}

.compare-actions {
  display: flex;
  gap: 8px;
}

.selected-models {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  padding: 8px 0;
}

.selected-model-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: 12px;
  border: 1px solid #4facfe;
  transition: all 0.3s ease;
}

.selected-model-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(79, 172, 254, 0.2);
}

.model-thumb {
  width: 48px;
  height: 32px;
  object-fit: cover;
  border-radius: 6px;
}

.model-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.model-name {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.model-price {
  font-size: 12px;
  color: #606266;
}

/* 排行榜主体卡片 */
.ranking-card {
  margin-bottom: 24px;
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

.ranking-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ranking-controls .el-radio-group .el-radio-button__inner {
  padding: 6px 16px;
  font-size: 14px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

/* 车型列表样式 */
.car-list {
  padding: 8px 0;
}

.car-item {
  display: grid;
  grid-template-columns: auto 200px 1fr 280px 160px;
  gap: 20px;
  align-items: center;
  padding: 20px 24px;
  margin-bottom: 16px;
  background: white;
  border-radius: 16px;
  border: 2px solid transparent;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.car-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.car-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  border-color: #4facfe;
}

.car-item:hover::before {
  opacity: 1;
}

.car-item.top-three {
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
  border-color: #ffd700;
}

.car-item.top-three::before {
  background: linear-gradient(135deg, #ffd700 0%, #ffb300 100%);
  opacity: 1;
}

.car-item.selected {
  border-color: #4facfe;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
}

.car-item.trending-up {
  border-left: 4px solid #67c23a;
}

.car-item.trending-down {
  border-left: 4px solid #f56c6c;
}

/* 排名徽章 */
.ranking {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 60px;
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
  position: relative;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.rank-number.rank-1 {
  background: linear-gradient(135deg, #ffd700 0%, #ffb300 100%);
  font-size: 20px;
}

.rank-number.rank-2 {
  background: linear-gradient(135deg, #c0c0c0 0%, #a9a9a9 100%);
  font-size: 18px;
}

.rank-number.rank-3 {
  background: linear-gradient(135deg, #cd7f32 0%, #b8860b 100%);
  font-size: 18px;
}

.rank-number:not(.rank-1):not(.rank-2):not(.rank-3) {
  background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
  font-size: 16px;
}

.rank-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 600;
}

.trend-up {
  color: #67c23a;
}

.trend-down {
  color: #f56c6c;
}

.trend-value {
  font-size: 10px;
}

/* 车型图片 */
.car-image {
  position: relative;
  width: 200px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.car-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.car-item:hover .car-image img {
  transform: scale(1.05);
}

.image-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
}

.image-overlay .el-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}

/* 车型基本信息 */
.car-info {
  flex: 1;
  min-width: 0;
}

.car-info h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
}

.car-info .brand {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.specs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.spec-item {
  padding: 4px 8px;
  background: #f0f2f5;
  border-radius: 6px;
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}

.key-features {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.feature-tag {
  font-size: 11px;
  padding: 3px 6px;
  border-radius: 4px;
}

/* 核心数据统计 */
.car-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 280px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-item.main-stat {
  padding: 12px 16px;
  background: linear-gradient(135deg, #f8fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.stat-item .label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.stat-item .value {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.stat-item .value.primary {
  font-size: 18px;
  color: #4facfe;
}

.stat-item .change {
  font-size: 12px;
  font-weight: 600;
  margin-left: 8px;
}

.stat-item .change.positive {
  color: #67c23a;
}

.stat-item .change.negative {
  color: #f56c6c;
}

.stat-item .change.neutral {
  color: #909399;
}

.rating-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-count {
  font-size: 11px;
  color: #909399;
}

/* 操作按钮区 */
.car-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 160px;
}

.car-actions .el-button {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.car-actions .el-dropdown {
  width: 100%;
}

.car-actions .el-dropdown .el-button {
  width: 100%;
}


/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px 0 8px 0;
}

.pagination-wrapper .el-pagination {
  --el-pagination-button-color: #606266;
  --el-pagination-button-bg-color: white;
  --el-pagination-button-border-radius: 8px;
}


/* 详情抽屉样式 */
.detail-content {
  padding: 16px;
}

.detail-header {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.detail-image {
  width: 200px;
  height: 120px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.detail-basic {
  flex: 1;
}

.detail-basic h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.detail-price {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #4facfe;
}

.detail-tags {
  display: flex;
  gap: 8px;
}

.detail-sections {
  margin-bottom: 24px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.detail-metric {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: #f8fafb;
  border-radius: 8px;
}

.metric-label {
  font-size: 12px;
  color: #909399;
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.user-feedback {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feedback-count {
  font-size: 13px;
  color: #909399;
}

.detail-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 趋势图表弹窗 */
.trend-chart-container {
  padding: 16px 0;
}

.trend-chart {
  width: 100%;
  border-radius: 8px;
  background: white;
}

/* 工具类样式 */
.positive {
  color: #67c23a !important;
}

.negative {
  color: #f56c6c !important;
}

.neutral {
  color: #909399 !important;
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

.car-item {
  animation: slideInUp 0.6s ease-out;
}

.recommendation-item {
  animation: slideInUp 0.4s ease-out;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .car-item {
    grid-template-columns: auto 180px 1fr 240px 140px;
    gap: 16px;
  }

  .header-left h2 {
    font-size: 28px;
  }

  .ranking-controls {
    flex-direction: column;
    gap: 8px;
  }
}

@media (max-width: 1200px) {
  .car-item {
    grid-template-columns: auto 160px 1fr 200px;
    gap: 12px;
  }

  .car-actions {
    min-width: 120px;
  }

  .car-stats {
    min-width: 200px;
  }

}

@media (max-width: 768px) {
  .top-car-model-list {
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

  .filter-content .el-row {
    margin: 0;
  }

  .filter-content .el-col {
    margin-bottom: 16px;
  }

  .car-item {
    grid-template-columns: 1fr;
    gap: 16px;
    text-align: center;
    padding: 16px;
  }

  .ranking {
    flex-direction: row;
    justify-content: center;
  }

  .car-image {
    width: 100%;
    height: 180px;
    margin: 0 auto;
  }

  .car-stats {
    min-width: auto;
  }

  .car-actions {
    min-width: auto;
    flex-direction: row;
    justify-content: center;
  }

  .selected-models {
    flex-direction: column;
  }


  .detail-header {
    flex-direction: column;
    text-align: center;
  }

  .detail-image {
    width: 100%;
    margin: 0 auto;
  }

  .detail-actions {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .header-left h2 {
    font-size: 20px;
  }

  .car-item {
    padding: 12px;
  }

  .rank-number {
    width: 40px;
    height: 40px;
    font-size: 14px;
  }

  .car-info h3 {
    font-size: 18px;
  }

  .car-image {
    height: 160px;
  }

  .compare-actions {
    flex-direction: column;
    gap: 4px;
  }

  .ranking-controls .el-radio-group {
    flex-direction: column;
  }

  .ranking-controls .el-select {
    width: 100%;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .top-car-model-list {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .filter-card,
  .quick-compare-card,
  .ranking-card,

  .car-item,

  .car-item.top-three {
    background: #3d3d2d;
  }

  .car-item.selected {
    background: #2d3d4d;
  }

  .selected-model-item {
    background: #2d3d4d;
    border-color: #4facfe;
  }

  .spec-item {
    background: #404040;
    color: #e4e7ed;
  }

  .stat-item.main-stat {
    background: #363636;
    border-color: #505050;
  }

  .detail-metric {
    background: #363636;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .car-item,
  .recommendation-item,
  .selected-model-item {
    border-width: 2px;
    border-color: #000;
  }

  .car-info h3,
  .rank-number {
    font-weight: 700;
  }

  .car-image {
    border: 2px solid #000;
  }
}

/* 打印样式 */
@media print {
  .top-car-model-list {
    background: white !important;
  }

  .header-actions,
  .compare-actions,
  .car-actions {
    display: none !important;
  }

  .car-item {
    break-inside: avoid;
    margin-bottom: 12px;
    box-shadow: none !important;
    border: 1px solid #ccc !important;
    grid-template-columns: auto 1fr 200px;
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
.car-item:focus-visible {
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

<template>
  <div class="fuel-cons-list">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>油耗/电耗榜单</h2>
          <p>基于真实数据的燃油经济性排行，助您做出最经济的购车选择</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
          <el-button
            type="success"
            :icon="Download"
            @click="exportRanking"
            :disabled="!fuelConsRankingList.length"
          >
            导出榜单
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 个性化用车成本计算器 -->
    <el-card shadow="never" class="calculator-card">
      <template #header>
        <div class="calculator-header">
          <el-icon><MagicStick /></el-icon>
          <span>个性化用车成本计算器</span>
          <el-tag type="primary">智能计算</el-tag>
        </div>
      </template>

      <div class="calculator-content">
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <div class="calculator-inputs">
              <el-form :model="calculatorForm" label-width="120px" size="default">
                <el-form-item label="日均里程">
                  <el-input-number
                    v-model="calculatorForm.dailyMileage"
                    :min="1"
                    :max="1000"
                    placeholder="公里"
                    style="width: 100%"
                    @change="calculateCost"
                  />
                  <div class="input-tip">单位：公里，建议根据实际出行情况填写</div>
                </el-form-item>

                <el-form-item label="当前油价">
                  <el-input-number
                    v-model="calculatorForm.fuelPrice"
                    :min="1"
                    :max="50"
                    :precision="2"
                    placeholder="元/升"
                    style="width: 100%"
                    @change="calculateCost"
                  />
                  <div class="input-tip">单位：元/L，请参考当地实际油价输入</div>
                </el-form-item>

                <el-form-item label="基础油耗/电耗">
                  <el-input-number
                    v-model="calculatorForm.baseFuelConsumption"
                    :min="1"
                    :max="100"
                    :precision="1"
                    placeholder="L/100km 或 kWh/100km"
                    style="width: 100%"
                    @change="calculateCost"
                  />
                  <div class="input-tip">
                    单位：L/100km 或 kWh/100km，请输入您的车辆实际油耗或电耗
                  </div>
                </el-form-item>

                <el-form-item label="驾驶风格">
                  <el-radio-group v-model="calculatorForm.drivingStyle" @change="calculateCost">
                    <el-radio value="eco">节能</el-radio>
                    <el-radio value="normal">温和</el-radio>
                    <el-radio value="aggressive">激进</el-radio>
                  </el-radio-group>
                  <div class="input-tip">节能驾驶降低15%油耗，激进驾驶增加20%油耗</div>
                </el-form-item>

                <el-form-item label="使用场景">
                  <el-radio-group v-model="calculatorForm.usageScenario" @change="calculateCost">
                    <el-radio value="city">城市通勤</el-radio>
                    <el-radio value="highway">高速长途</el-radio>
                    <el-radio value="mixed">混合路况</el-radio>
                  </el-radio-group>
                  <div class="input-tip">城市驾驶油耗增加15%，高速驾驶油耗减少12%</div>
                </el-form-item>
              </el-form>
            </div>
          </el-col>

          <el-col :xs="24" :md="12">
            <div class="calculator-results">
              <h4>🎯 个性化成本预估</h4>
              <div class="cost-cards">
                <div class="cost-card daily">
                  <div class="cost-label">日均成本</div>
                  <div class="cost-value">¥{{ costResults.dailyCost }}</div>
                </div>
                <div class="cost-card monthly">
                  <div class="cost-label">月度成本</div>
                  <div class="cost-value">¥{{ costResults.monthlyCost }}</div>
                </div>
                <div class="cost-card yearly">
                  <div class="cost-label">年度成本</div>
                  <div class="cost-value">¥{{ costResults.yearlyCost }}</div>
                </div>
              </div>

              <!-- 节油建议 -->
              <div class="fuel-saving-tips" v-if="fuelSavingTips.length > 0">
                <h5>💡 个性化节油建议</h5>
                <ul class="tips-list">
                  <li v-for="tip in fuelSavingTips" :key="tip">{{ tip }}</li>
                </ul>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 筛选配置区 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-content">
        <el-row :gutter="16" align="bottom">
          <el-col :xs="24" :sm="6" :md="4">
            <el-form-item label="车型类别">
              <el-select v-model="vehicleType" @change="handleFilterChange" style="width: 100%">
                <el-option label="全部车型" value="all" />
                <el-option label="轿车" value="sedan" />
                <el-option label="SUV" value="suv" />
                <el-option label="MPV" value="mpv" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="6" :md="4">
            <el-form-item label="价格区间">
              <el-select v-model="priceRange" @change="handleFilterChange" style="width: 100%">
                <el-option label="全部价格" value="all" />
                <el-option label="10万以下" value="under10" />
                <el-option label="10-20万" value="10-20" />
                <el-option label="20-30万" value="20-30" />
                <el-option label="30万以上" value="over30" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="6" :md="4">
            <el-form-item label="燃料类型">
              <el-select v-model="fuelType" @change="handleFilterChange" style="width: 100%">
                <el-option label="全部类型" value="all" />
                <el-option label="燃油车" value="gasoline" />
                <el-option label="纯电动" value="electric" />
                <el-option label="混合动力" value="hybrid" />
                <el-option label="插电混动" value="phev" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="6" :md="4">
            <el-form-item label="显示数量">
              <el-select
                v-model="displayCount"
                @change="handleDisplayCountChange"
                style="width: 100%"
              >
                <el-option label="TOP 20" :value="20" />
                <el-option label="TOP 50" :value="50" />
                <el-option label="TOP 100" :value="100" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="4">
            <el-button
              type="primary"
              @click="resetFilters"
              :icon="RefreshRight"
              style="width: 100%"
            >
              重置筛选
            </el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 油耗排行榜主体 -->
    <el-card shadow="never" class="ranking-card">
      <template #header>
        <div class="ranking-header">
          <span>综合油耗排行 TOP {{ displayCount }}</span>
          <div class="ranking-controls">
            <el-button
              size="small"
              @click="showComparisonModal = true"
              type="primary"
              :disabled="comparisonList.length < 2"
            >
              对比分析 ({{ comparisonList.length }})
            </el-button>
          </div>
        </div>
      </template>

      <div class="ranking-list" v-loading="loading">
        <!-- 油耗排行项 -->
        <div
          v-for="(item, index) in paginatedRankingList"
          :key="item.id"
          class="ranking-item"
          :class="{
            'top-three': index < 3,
            selected: isSelected(item.id),
            'eco-champion': item.fuelConsumption <= getEcoThreshold(item.fuelType),
          }"
          @click="viewDetails(item)"
        >
          <!-- 排名标识 -->
          <div class="rank-number" :class="`rank-${index + 1}`">
            {{ index + 1 }}
          </div>

          <!-- 车型图片 -->
          <div class="vehicle-image">
            <img :src="item.image" :alt="item.name" @error="handleImageError" />
            <div class="image-overlay" v-if="item.isEcoChampion || item.isNew">
              <el-tag v-if="item.isEcoChampion" type="success" size="small">ECO</el-tag>
              <el-tag v-if="item.isNew" type="primary" size="small">新款</el-tag>
            </div>
          </div>

          <!-- 车型信息 -->
          <div class="vehicle-info">
            <h3>{{ item.brand }} {{ item.name }}</h3>
            <p class="vehicle-specs">
              {{ item.type }} · {{ item.engine }} · {{ item.transmission }}
            </p>
            <div class="price-range">{{ item.priceRange }}</div>
            <div class="fuel-type-badge">
              <el-tag :type="getFuelTypeColor(item.fuelType)" size="small">
                {{ item.fuelType }}
              </el-tag>
            </div>
          </div>

          <!-- 油耗核心数据 -->
          <div class="fuel-data">
            <div class="main-fuel-consumption">
              <span class="fuel-label">{{ getFuelLabel(item.fuelType) }}</span>
              <span
                class="fuel-value"
                :class="getFuelEfficiencyClass(item.fuelConsumption, item.fuelType)"
              >
                {{ formatFuelConsumption(item.fuelConsumption, item.fuelType) }}
              </span>
              <span class="fuel-unit">{{ getFuelUnit(item.fuelType) }}</span>
            </div>

            <!-- 数据可靠性 -->
            <div class="data-reliability" v-if="item.sampleSize">
              <el-tooltip :content="`基于 ${item.sampleSize} 个样本数据`" placement="top">
                <el-rate
                  v-model="item.dataReliability"
                  disabled
                  show-score
                  text-color="#ff9900"
                  score-template="{value}"
                  max="5"
                  size="small"
                />
              </el-tooltip>
            </div>
          </div>

          <!-- 经济性评估 -->
          <div class="economy-assessment">
            <div class="cost-preview">
              <div class="cost-item">
                <span class="cost-label">年油费预估</span>
                <span class="cost-value">¥{{ calculateYearlyCost(item) }}</span>
              </div>
              <div class="cost-comparison" v-if="index > 0">
                <span class="comparison-label">vs 第1名</span>
                <span class="comparison-value" :class="getCostComparisonClass(item, index)">
                  {{ getCostComparison(item, index) }}
                </span>
              </div>
            </div>

            <div class="economy-score">
              <span class="score-label">经济性评分</span>
              <div class="score-bar">
                <div
                  class="score-fill"
                  :style="{ width: item.economyScore + '%' }"
                  :class="getScoreClass(item.economyScore)"
                ></div>
              </div>
              <span class="score-text">{{ item.economyScore }}/100</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons">
            <el-button
              size="small"
              @click.stop="toggleComparison(item)"
              :type="isSelected(item.id) ? 'primary' : ''"
              :disabled="!isSelected(item.id) && comparisonList.length >= 3"
            >
              {{ isSelected(item.id) ? '已选' : '对比' }}
            </el-button>
            <el-dropdown @click.stop trigger="click" size="small">
              <el-button size="small" :icon="MoreFilled" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="viewDetails(item)">
                    <el-icon><View /></el-icon>
                    查看详情
                  </el-dropdown-item>
                  <el-dropdown-item @click="shareModel(item)">
                    <el-icon><Share /></el-icon>
                    分享车型
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty
          v-if="!loading && fuelConsRankingList.length === 0"
          description="暂无符合条件的车型数据"
        />
      </div>

      <!-- 分页控件 -->
      <div class="pagination-wrapper" v-if="fuelConsRankingList.length > displayCount">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="displayCount"
          :total="fuelConsRankingList.length"
          layout="prev, pager, next, jumper, total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 快速对比工具 -->
    <el-card shadow="never" class="quick-compare-card" v-if="comparisonList.length > 0">
      <template #header>
        <div class="compare-header">
          <span>快速对比 ({{ comparisonList.length }}/3)</span>
          <div class="compare-actions">
            <el-button size="small" @click="clearComparison">清空对比</el-button>
            <el-button
              size="small"
              type="primary"
              @click="startComparison"
              :disabled="comparisonList.length < 2"
            >
              开始对比
            </el-button>
          </div>
        </div>
      </template>

      <div class="selected-models">
        <div v-for="model in comparisonList" :key="model.id" class="selected-model-item">
          <img :src="model.image" :alt="model.name" class="model-thumb" />
          <div class="model-info">
            <span class="model-name">{{ model.brand }} {{ model.name }}</span>
            <span class="model-fuel"
              >{{ formatFuelConsumption(model.fuelConsumption, model.fuelType) }}
              {{ getFuelUnit(model.fuelType) }}</span
            >
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

    <!-- 对比分析弹窗 -->
    <el-dialog
      v-model="showComparisonModal"
      title="多车型成本对比分析"
      width="80%"
      :before-close="handleComparisonClose"
    >
      <div class="comparison-content" v-if="comparisonList.length >= 2">
        <div class="comparison-summary">
          <h4>成本对比总结</h4>
          <div class="summary-stats">
            <div class="stat-item">
              <span class="stat-label">最省油车型：</span>
              <span class="stat-value"
                >{{ getMostEfficientModel().brand }} {{ getMostEfficientModel().name }}</span
              >
            </div>
            <div class="stat-item">
              <span class="stat-label">年最大节省：</span>
              <span class="stat-value">¥{{ calculateMaxSavings() }}</span>
            </div>
          </div>
        </div>

        <div class="comparison-table">
          <el-table :data="comparisonList" style="width: 100%">
            <el-table-column prop="brand" label="品牌" width="100" />
            <el-table-column prop="name" label="车型" width="120" />
            <el-table-column label="油耗" width="120">
              <template #default="scope">
                {{ formatFuelConsumption(scope.row.fuelConsumption, scope.row.fuelType) }}
                {{ getFuelUnit(scope.row.fuelType) }}
              </template>
            </el-table-column>
            <el-table-column label="年成本" width="120">
              <template #default="scope"> ¥{{ calculateYearlyCost(scope.row) }} </template>
            </el-table-column>
            <el-table-column prop="economyScore" label="经济性评分" width="120" />
            <el-table-column prop="priceRange" label="价格区间" />
          </el-table>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showComparisonModal = false">关闭</el-button>
          <el-button
            type="primary"
            @click="exportComparisonReport"
            :disabled="comparisonList.length < 2"
          >
            导出对比报告
          </el-button>
        </div>
      </template>
    </el-dialog>

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
              <el-tag :type="getFuelTypeColor(selectedCarDetail.fuelType)">{{
                selectedCarDetail.fuelType
              }}</el-tag>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="detail-sections">
          <div class="detail-section">
            <h4>油耗表现</h4>
            <el-row :gutter="16">
              <el-col :span="12">
                <div class="detail-metric">
                  <span class="metric-label"
                    >综合{{ getFuelLabel(selectedCarDetail.fuelType) }}</span
                  >
                  <span class="metric-value">
                    {{
                      formatFuelConsumption(
                        selectedCarDetail.fuelConsumption,
                        selectedCarDetail.fuelType,
                      )
                    }}
                    {{ getFuelUnit(selectedCarDetail.fuelType) }}
                  </span>
                </div>
              </el-col>
              <el-col :span="12" v-if="selectedCarDetail.sampleSize">
                <div class="detail-metric">
                  <span class="metric-label">数据样本</span>
                  <span class="metric-value">{{ selectedCarDetail.sampleSize }} 个</span>
                </div>
              </el-col>
            </el-row>
          </div>

          <div class="detail-section">
            <h4>成本分析</h4>
            <div class="cost-analysis">
              <div class="cost-breakdown">
                <div class="breakdown-item">
                  <span class="breakdown-label">年油费（按1.5万公里）</span>
                  <span class="breakdown-value">¥{{ calculateYearlyCost(selectedCarDetail) }}</span>
                </div>
                <div class="breakdown-item">
                  <span class="breakdown-label">每公里燃料成本</span>
                  <span class="breakdown-value">¥{{ calculatePerKmCost(selectedCarDetail) }}</span>
                </div>
              </div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  MagicStick,
  RefreshRight,
  MoreFilled,
  View,
  Share,
  Close,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

// =============================================
// 基础数据层 - 直接从API获取
// =============================================

// 基础数据接口定义
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

interface FuelEconomy {
  fuelId: number
  carModelId: number
  fuelType: string
  avgFuel: number
  sampleCount: number
  collectTime: string
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

interface Opinion {
  opinionId: number
  carModelId: number
  modelName: string
  brandName: string
  score: number
}

// 基础数据存储
interface BaseData {
  carModels: CarModel[]
  fuelEconomies: FuelEconomy[]
  saleRecords: SaleRecord[]
  opinions: Opinion[]
}

// =============================================
// 计算数据层 - 基于基础数据计算
// =============================================

// 处理后的车型油耗数据
interface ProcessedFuelModel {
  id: number
  brand: string
  name: string
  type: string
  engine: string
  transmission: string
  priceRange: string
  fuelType: string
  fuelConsumption: number
  powerConsumption?: number
  image: string
  sampleSize: number
  dataReliability: number
  economyScore: number
  isEcoChampion: boolean
  isNew: boolean
}

// 计算器表单
interface CalculatorForm {
  dailyMileage: number
  fuelPrice: number
  drivingStyle: string
  usageScenario: string
  baseFuelConsumption: number //百公里油耗,L/100km
}

interface CostResults {
  dailyCost: string
  monthlyCost: string
  yearlyCost: string
}

// =============================================
// 业务指标层 - 最终展示的KPI
// =============================================

// 响应式数据
const loading = ref(false)
const currentPage = ref(1)

// 筛选条件
const vehicleType = ref('all')
const priceRange = ref('all')
const fuelType = ref('all')
const displayCount = ref(20)

// 基础数据存储
const baseData = ref<BaseData>({
  carModels: [],
  fuelEconomies: [],
  saleRecords: [],
  opinions: [],
})

// 处理后的业务数据
const fuelConsRankingList = ref<ProcessedFuelModel[]>([])

// 个性化计算器
const calculatorForm = ref<CalculatorForm>({
  dailyMileage: 50,
  fuelPrice: 7.5,
  drivingStyle: 'normal',
  usageScenario: 'mixed',
  baseFuelConsumption: 7.0, // 默认值
})

const costResults = ref<CostResults>({
  dailyCost: '0.00',
  monthlyCost: '0.00',
  yearlyCost: '0.00',
})

const fuelSavingTips = ref<string[]>([])

// 对比工具
const comparisonList = ref<ProcessedFuelModel[]>([])
const showComparisonModal = ref(false)

// 车型详情
const showDetailDrawer = ref(false)
const selectedCarDetail = ref<ProcessedFuelModel | null>(null)

// =============================================
// API 调用函数
// =============================================

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

const fetchAllFuelEconomy = async (): Promise<FuelEconomy[]> => {
  try {
    console.log('正在获取油耗数据...')
    const response = await axios.get('/api/fuel-economy')

    if (response.data.status === 200 && response.data.data) {
      console.log('获取油耗数据成功:', response.data.data.length, '条记录')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.code}`)
    }
  } catch (error) {
    console.error('获取油耗数据失败:', error)
    ElMessage.error('油耗数据加载失败')
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

const fetchOpinions = async (): Promise<Opinion[]> => {
  try {
    console.log('正在获取口碑数据...')
    const response = await axios.get('/api/opinions')

    if (response.data.status === 200 && response.data.data) {
      console.log('获取口碑数据成功:', response.data.data.length, '条评价')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('获取口碑数据失败:', error)
    ElMessage.error('口碑数据加载失败')
    return []
  }
}

// =============================================
// 数据处理函数
// =============================================

const loadAllBaseData = async () => {
  try {
    console.log('开始加载基础数据...')
    loading.value = true

    const [carModels, fuelEconomies, saleRecords, opinions] = await Promise.all([
      fetchCarModels(),
      fetchAllFuelEconomy(),
      fetchSaleRecords(),
      fetchOpinions(),
    ])

    baseData.value = {
      carModels,
      fuelEconomies,
      saleRecords,
      opinions,
    }

    console.log('基础数据加载完成:', {
      车型数量: carModels.length,
      油耗记录数量: fuelEconomies.length,
      销售记录数量: saleRecords.length,
      口碑数量: opinions.length,
    })

    // 处理业务数据
    processFuelConsRankingData()

    ElMessage.success('数据加载完成')
  } catch (error) {
    console.error('基础数据加载失败:', error)
    ElMessage.error('数据加载失败，请检查网络连接')
    // 生成空数据以保证页面正常运行
    fuelConsRankingList.value = []
  } finally {
    loading.value = false
  }
}

const processFuelConsRankingData = () => {
  try {
    console.log('开始处理油耗排行数据...')

    if (baseData.value.fuelEconomies.length === 0) {
      console.warn('油耗数据为空')
      fuelConsRankingList.value = []
      return
    }

    // 组合车型和油耗数据
    const processedData: ProcessedFuelModel[] = baseData.value.fuelEconomies
      .map((fuelEconomy) => {
        // 查找对应的车型信息
        const carModel = baseData.value.carModels.find(
          (model) => model.carModelId === fuelEconomy.carModelId,
        )

        if (!carModel) {
          console.warn(`未找到车型ID ${fuelEconomy.carModelId} 的车型信息`)
          return null
        }

        // 查找口碑评分
        const opinion = baseData.value.opinions.find((op) => op.carModelId === carModel.carModelId)

        // 计算价格区间
        const priceRange = calculatePriceRange(carModel.officialPrice)

        // 计算经济性评分
        const economyScore = calculateEconomyScore({
          fuelConsumption: fuelEconomy.avgFuel,
          price: carModel.officialPrice,
          fuelType: fuelEconomy.fuelType,
          sampleSize: fuelEconomy.sampleCount,
          rating: opinion?.score || 3.5,
        })

        // 映射车型类型
        const vehicleTypeMapping = {
          A级: '轿车',
          B级: '轿车',
          C级: '轿车',
          D级: '轿车',
        }

        const mappedType =
          vehicleTypeMapping[carModel.level] ||
          (carModel.level.includes('SUV') ? 'SUV' : carModel.level.includes('MPV') ? 'MPV' : '轿车')

        // 标准化燃料类型
        const normalizedFuelType = normalizeFuelType(carModel.engineType)

        return {
          id: carModel.carModelId,
          brand: carModel.brandName,
          name: carModel.modelName,
          type: mappedType,
          engine: carModel.engineType,
          transmission: generateTransmission(carModel.engineType),
          priceRange,
          fuelType: normalizedFuelType,
          fuelConsumption: fuelEconomy.avgFuel,
          powerConsumption: carModel.engineType === '纯电动' ? fuelEconomy.avgFuel : undefined,
          image: generateCarImage(carModel.carModelId),
          sampleSize: fuelEconomy.sampleCount,
          dataReliability: calculateReliability(fuelEconomy.sampleCount),
          economyScore,
          isEcoChampion: fuelEconomy.avgFuel <= getEcoThreshold(normalizedFuelType),
          isNew: isNewModel(carModel.launchDate),
        }
      })
      .filter(Boolean) as ProcessedFuelModel[] // 移除 null 值

    // 应用筛选条件
    let filteredData = processedData.filter((model) => applyFilters(model))

    // 按油耗排序
    filteredData.sort((a, b) => a.fuelConsumption - b.fuelConsumption)

    fuelConsRankingList.value = filteredData

    console.log('油耗排行数据处理完成:', filteredData.length, '个车型')
  } catch (error) {
    console.error('处理油耗排行数据失败:', error)
    fuelConsRankingList.value = []
  }
}

// =============================================
// 辅助计算函数
// =============================================

const calculatePriceRange = (officialPrice: number): string => {
  const priceInWan = officialPrice / 10000
  const minPrice = Math.max(priceInWan * 0.85, priceInWan - 3)
  const maxPrice = priceInWan * 1.15 + 2
  return `${minPrice.toFixed(0)}-${maxPrice.toFixed(0)}万`
}

const calculateEconomyScore = (params: {
  fuelConsumption: number
  price: number
  fuelType: string
  sampleSize: number
  rating: number
}): number => {
  const { fuelConsumption, price, fuelType, sampleSize, rating } = params

  // 基础油耗评分（40%权重）
  const fuelScore = calculateFuelEfficiencyScore(fuelConsumption, fuelType)

  // 价格经济性评分（30%权重）
  const priceScore = calculatePriceEconomyScore(price, fuelConsumption)

  // 数据可靠性评分（20%权重）
  const reliabilityScore = Math.min(100, (sampleSize / 1000) * 100)

  // 用户评价评分（10%权重）
  const ratingScore = (rating / 5) * 100

  // 综合评分
  const totalScore = fuelScore * 0.4 + priceScore * 0.3 + reliabilityScore * 0.2 + ratingScore * 0.1

  return Math.floor(Math.min(100, Math.max(0, totalScore)))
}

const calculateFuelEfficiencyScore = (consumption: number, fuelType: string): number => {
  const standards = {
    燃油: { excellent: 5, good: 7, poor: 12 },
    纯电动: { excellent: 15, good: 20, poor: 25 },
    混合动力: { excellent: 4, good: 6, poor: 10 },
    插电混动: { excellent: 2, good: 4, poor: 8 },
  }

  const standard = standards[fuelType] || standards['燃油']

  if (consumption <= standard.excellent) return 100
  if (consumption <= standard.good) return 80
  if (consumption <= standard.poor) return 60
  return 40
}

const calculatePriceEconomyScore = (price: number, consumption: number): number => {
  const pricePerKm = price / 100000 / consumption // 每公里价格影响
  if (pricePerKm <= 0.5) return 100
  if (pricePerKm <= 1.0) return 80
  if (pricePerKm <= 2.0) return 60
  return 40
}

const normalizeFuelType = (engineType: string): string => {
  const typeMap: Record<string, string> = {
    纯电动: '纯电动',
    混合动力: '混合动力',
    插电式混合动力: '插电混动',
    增程式: '插电混动',
  }
  return typeMap[engineType] || '燃油'
}

const generateTransmission = (engineType: string): string => {
  if (engineType === '纯电动') return '单速变速箱'
  const transmissions = ['CVT', '6AT', '7DCT', '8AT', '手动']
  return transmissions[Math.floor(Math.random() * transmissions.length)]
}

const generateCarImage = (carModelId: number): string => {
  return `https://picsum.photos/300/200?random=${carModelId}`
}

const calculateReliability = (sampleSize: number): number => {
  if (sampleSize >= 1000) return 5
  if (sampleSize >= 500) return 4
  if (sampleSize >= 200) return 3
  if (sampleSize >= 50) return 2
  return 1
}

const getEcoThreshold = (fuelType: string): number => {
  const thresholds = {
    燃油: 5.0,
    纯电动: 15.0,
    混合动力: 4.5,
    插电混动: 3.0,
  }
  return thresholds[fuelType] || 5.0
}

const isNewModel = (launchDate: string): boolean => {
  const launch = new Date(launchDate)
  const now = new Date()
  const monthsDiff =
    (now.getFullYear() - launch.getFullYear()) * 12 + (now.getMonth() - launch.getMonth())
  return monthsDiff <= 12
}

const applyFilters = (model: ProcessedFuelModel): boolean => {
  // 车型类别筛选
  if (vehicleType.value !== 'all') {
    const typeMap = {
      sedan: '轿车',
      suv: 'SUV',
      mpv: 'MPV',
    }
    if (model.type !== typeMap[vehicleType.value]) return false
  }

  // 价格区间筛选
  if (priceRange.value !== 'all') {
    const price = extractPriceFromRange(model.priceRange)
    const rangeMap = {
      under10: price < 10,
      '10-20': price >= 10 && price < 20,
      '20-30': price >= 20 && price < 30,
      over30: price >= 30,
    }
    if (!rangeMap[priceRange.value]) return false
  }

  // 燃料类型筛选
  if (fuelType.value !== 'all') {
    const fuelMap = {
      gasoline: '燃油',
      electric: '纯电动',
      hybrid: '混合动力',
      phev: '插电混动',
    }
    if (model.fuelType !== fuelMap[fuelType.value]) return false
  }

  return true
}

const extractPriceFromRange = (priceRange: string): number => {
  const match = priceRange.match(/(\d+)-(\d+)万/)
  if (match) {
    return (parseInt(match[1]) + parseInt(match[2])) / 2
  }
  return 0
}

// =============================================
// 计算属性
// =============================================

const paginatedRankingList = computed(() => {
  const start = (currentPage.value - 1) * displayCount.value
  const end = start + displayCount.value
  return fuelConsRankingList.value.slice(start, end)
})

// =============================================
// 工具函数
// =============================================

const getFuelLabel = (fuelType: string) => {
  if (fuelType === '纯电动') return '电耗'
  return '油耗'
}

const getFuelTypeColor = (type: string) => {
  const colors = {
    燃油: 'warning',
    纯电动: 'success',
    混合动力: 'primary',
    插电混动: 'info',
  }
  return colors[type] || 'info'
}

const getFuelUnit = (fuelType: string) => {
  if (fuelType === '纯电动') return 'kWh/100km'
  return 'L/100km'
}

const formatFuelConsumption = (consumption: number, fuelType: string) => {
  if (!consumption) return '--'
  return consumption.toFixed(1)
}

const getFuelEfficiencyClass = (consumption: number, fuelType: string) => {
  if (fuelType === '纯电动') {
    if (consumption <= 15) return 'excellent'
    if (consumption <= 20) return 'good'
    return 'normal'
  } else {
    if (consumption <= 5) return 'excellent'
    if (consumption <= 7) return 'good'
    return 'normal'
  }
}

const getScoreClass = (score: number) => {
  if (score >= 90) return 'excellent'
  if (score >= 75) return 'good'
  if (score >= 60) return 'fair'
  return 'poor'
}

const isSelected = (modelId: number) => {
  return comparisonList.value.some((model) => model.id === modelId)
}

// =============================================
// 成本计算相关
// =============================================

const calculateCost = () => {
  const { dailyMileage, fuelPrice, drivingStyle, usageScenario, baseFuelConsumption } =
    calculatorForm.value

  const styleMultiplier = {
    eco: 0.85,
    normal: 1.0,
    aggressive: 1.2,
  }

  const scenarioMultiplier = {
    city: 1.15,
    highway: 0.88,
    mixed: 1.0,
  }

  const adjustedConsumption =
    baseFuelConsumption * styleMultiplier[drivingStyle] * scenarioMultiplier[usageScenario]

  const dailyCost = (dailyMileage / 100) * adjustedConsumption * fuelPrice
  const monthlyCost = dailyCost * 30
  const yearlyCost = dailyCost * 365

  costResults.value = {
    dailyCost: dailyCost.toFixed(2),
    monthlyCost: monthlyCost.toFixed(0),
    yearlyCost: yearlyCost.toFixed(0),
  }

  updateFuelSavingTips()
}

const updateFuelSavingTips = () => {
  const tips = []
  const { dailyMileage, fuelPrice, drivingStyle, usageScenario } = calculatorForm.value

  if (drivingStyle === 'aggressive') {
    tips.push('建议采用温和驾驶方式，避免急加速急刹车，可节省15-20%燃油')
  }

  if (usageScenario === 'city') {
    tips.push('城市驾驶建议：使用ECO模式，合理规划路线避开拥堵')
  } else if (usageScenario === 'highway') {
    tips.push('高速驾驶建议：保持经济时速80-90km/h，定速巡航更省油')
  }

  if (dailyMileage > 100) {
    tips.push('长距离驾驶建议考虑混动或纯电动车型，长期更经济')
  }

  tips.push('定期保养车辆，保持胎压正常，可提升3-5%燃油效率')

  fuelSavingTips.value = tips.slice(0, 4)
}

const calculateYearlyCost = (model: ProcessedFuelModel) => {
  const yearlyMileage = 15000
  const defaultFuelPrice = 7.5

  if (model.fuelType === '纯电动') {
    const electricPrice = 0.6
    const consumption = model.powerConsumption || model.fuelConsumption
    return Math.floor((yearlyMileage / 100) * consumption * electricPrice)
  } else {
    const consumption = model.fuelConsumption
    return Math.floor((yearlyMileage / 100) * consumption * defaultFuelPrice)
  }
}

const calculatePerKmCost = (model: ProcessedFuelModel) => {
  const yearlyCost = calculateYearlyCost(model)
  return (yearlyCost / 15000).toFixed(3)
}

const getCostComparison = (model: ProcessedFuelModel, index: number) => {
  if (index === 0) return ''

  const firstModelCost = calculateYearlyCost(fuelConsRankingList.value[0])
  const currentModelCost = calculateYearlyCost(model)
  const difference = currentModelCost - firstModelCost

  return difference > 0 ? `+¥${difference}` : `¥${Math.abs(difference)}`
}

const getCostComparisonClass = (model: ProcessedFuelModel, index: number) => {
  if (index === 0) return ''

  const firstModelCost = calculateYearlyCost(fuelConsRankingList.value[0])
  const currentModelCost = calculateYearlyCost(model)

  return currentModelCost > firstModelCost ? 'higher' : 'lower'
}

// =============================================
// 事件处理函数
// =============================================

const handleFilterChange = async () => {
  console.log('筛选条件变更，重新处理数据...')
  processFuelConsRankingData()
  currentPage.value = 1
  ElMessage.success('筛选条件已更新')
}

const handleDisplayCountChange = () => {
  currentPage.value = 1
  ElMessage.info(`显示数量已调整为TOP ${displayCount.value}`)
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  document.querySelector('.ranking-card')?.scrollIntoView({ behavior: 'smooth' })
}

const resetFilters = async () => {
  vehicleType.value = 'all'
  priceRange.value = 'all'
  fuelType.value = 'all'
  displayCount.value = 20
  currentPage.value = 1

  processFuelConsRankingData()
  ElMessage.success('筛选条件已重置')
}

// =============================================
// 对比功能
// =============================================

const toggleComparison = (model: ProcessedFuelModel) => {
  const index = comparisonList.value.findIndex((item) => item.id === model.id)

  if (index > -1) {
    comparisonList.value.splice(index, 1)
    ElMessage.success(`${model.brand} ${model.name} 已移出对比`)
  } else {
    if (comparisonList.value.length >= 3) {
      ElMessage.warning('最多只能对比3款车型')
      return
    }
    comparisonList.value.push(model)
    ElMessage.success(`${model.brand} ${model.name} 已加入对比`)
  }
}

const removeFromComparison = (modelId: number) => {
  const index = comparisonList.value.findIndex((item) => item.id === modelId)
  if (index > -1) {
    const model = comparisonList.value[index]
    comparisonList.value.splice(index, 1)
    ElMessage.success(`${model.brand} ${model.name} 已移出对比`)
  }
}

const clearComparison = () => {
  comparisonList.value = []
  ElMessage.info('对比列表已清空')
}

const startComparison = () => {
  if (comparisonList.value.length < 2) {
    ElMessage.warning('至少需要2款车型才能开始对比')
    return
  }

  showComparisonModal.value = true
}

const getMostEfficientModel = () => {
  if (comparisonList.value.length === 0) {
    return { brand: '', name: '' }
  }

  return comparisonList.value.reduce((min, current) => {
    return current.fuelConsumption < min.fuelConsumption ? current : min
  })
}

const calculateMaxSavings = () => {
  if (comparisonList.value.length < 2) return 0

  const costs = comparisonList.value.map((model) => calculateYearlyCost(model))
  const minCost = Math.min(...costs)
  const maxCost = Math.max(...costs)

  return maxCost - minCost
}

// =============================================
// 车型详情
// =============================================

const viewDetails = (model: ProcessedFuelModel) => {
  selectedCarDetail.value = model
  showDetailDrawer.value = true
}

const addToComparison = (model: ProcessedFuelModel) => {
  toggleComparison(model)
  showDetailDrawer.value = false
}

const shareModel = (model: ProcessedFuelModel) => {
  const shareUrl = `${window.location.origin}/fuel-cons/${model.id}`

  if (navigator.share) {
    navigator.share({
      title: `${model.brand} ${model.name} 油耗信息`,
      text: `查看这款车的详细油耗数据和经济性分析`,
      url: shareUrl,
    })
  } else {
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

const handleComparisonClose = () => {
  showComparisonModal.value = false
}

// =============================================
// 导出功能
// =============================================

const exportComparisonReport = () => {
  if (comparisonList.value.length < 2) {
    ElMessage.warning('至少需要2款车型才能导出报告')
    return
  }

  const csvContent = [
    ['车型对比报告 - 油耗经济性分析'],
    ['生成时间', new Date().toLocaleString()],
    [''],
    ['车型', '品牌', '油耗', '年度成本(元)', '经济性评分'],
    ...comparisonList.value.map((model) => [
      model.name,
      model.brand,
      `${formatFuelConsumption(model.fuelConsumption, model.fuelType)} ${getFuelUnit(model.fuelType)}`,
      calculateYearlyCost(model),
      model.economyScore,
    ]),
  ]
    .map((row) => row.join(','))
    .join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `油耗对比报告_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  ElMessage.success('对比报告已导出')
}

const refreshData = async () => {
  await loadAllBaseData()
}

const exportRanking = () => {
  if (fuelConsRankingList.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  const csvContent = [
    ['综合油耗排行榜'],
    ['生成时间', new Date().toLocaleString()],
    [''],
    ['排名', '车型', '品牌', '综合油耗', '价格区间', '经济性评分'],
    ...fuelConsRankingList.value
      .slice(0, displayCount.value)
      .map((model, index) => [
        index + 1,
        model.name,
        model.brand,
        `${formatFuelConsumption(model.fuelConsumption, model.fuelType)} ${getFuelUnit(model.fuelType)}`,
        model.priceRange,
        model.economyScore,
      ]),
  ]
    .map((row) => row.join(','))
    .join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `综合油耗排行榜_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  ElMessage.success('排行榜数据已导出')
}

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'https://via.placeholder.com/300x200?text=Car+Image'
}

// =============================================
// 监听器
// =============================================

watch(
  [calculatorForm],
  () => {
    calculateCost()
  },
  { deep: true },
)

// =============================================
// 生命周期
// =============================================

onMounted(async () => {
  ElMessage.success('欢迎使用油耗榜单！')

  try {
    // 初始化计算器
    calculateCost()

    // 加载基础数据
    await loadAllBaseData()
  } catch (error) {
    console.error('页面初始化失败:', error)
    ElMessage.error('初始化失败，部分功能可能不可用')
  }
})

onUnmounted(() => {
  // 清理资源
})
</script>

<style scoped>
/* 整体布局 */
.fuel-cons-list {
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
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  gap: 12px;
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

/* 个性化计算器卡片 */
.calculator-card {
  margin-bottom: 24px;
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.calculator-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.calculator-content {
  padding: 24px;
}

.calculator-inputs {
  padding-right: 20px;
}

.live-price-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 8px;
  font-size: 12px;
}

.tip-text {
  color: #4facfe;
  font-weight: 500;
}

.calculator-results {
  padding-left: 20px;
  border-left: 1px solid #e8eaed;
}

.calculator-results h4 {
  margin: 0 0 24px 0;
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 600;
}

.cost-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.cost-card {
  text-align: center;
  padding: 20px 16px;
  border-radius: 12px;
  transition: all 0.3s ease;
  border: 1px solid #e8eaed;
}

.cost-card.daily {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-color: #4facfe;
}

.cost-card.monthly {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-color: #67c23a;
}

.cost-card.yearly {
  background: linear-gradient(135deg, #fff7ed 0%, #fed7aa 100%);
  border-color: #e6a23c;
}

.cost-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.cost-label {
  display: block;
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.cost-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.fuel-saving-tips {
  padding: 16px;
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
  border-radius: 12px;
  border-left: 4px solid #ffd700;
}

.fuel-saving-tips h5 {
  margin: 0 0 12px 0;
  color: #b8860b;
  font-size: 16px;
  font-weight: 600;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  list-style: none;
}

.tips-list li {
  margin-bottom: 8px;
  color: #8b6914;
  font-size: 14px;
  position: relative;
  line-height: 1.4;
}

.tips-list li::before {
  content: '💡';
  margin-right: 8px;
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
  padding: 16px 0;
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

/* 油耗排行榜主体 */
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
  gap: 16px;
}

.ranking-list {
  padding: 8px 0;
}

.ranking-item {
  display: grid;
  grid-template-columns: auto 160px 1fr 200px 180px 120px;
  gap: 20px;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f2f5;
  transition: all 0.3s ease;
  cursor: pointer;
}

.ranking-item:hover {
  background: #f8fafb;
  transform: translateX(4px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.ranking-item.selected {
  background: #f0f9ff;
  border-color: #4facfe;
}

.ranking-item.top-three {
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
  border-left: 4px solid #ffd700;
}

.ranking-item.eco-champion {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-left: 4px solid #67c23a;
}

/* 排名标识 */
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
  background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%);
  box-shadow: 0 4px 15px rgba(144, 147, 153, 0.3);
  flex-shrink: 0;
}

.rank-number.rank-1 {
  background: linear-gradient(135deg, #ffd700 0%, #ffb300 100%);
  box-shadow: 0 4px 15px rgba(255, 215, 0, 0.3);
}

.rank-number.rank-2 {
  background: linear-gradient(135deg, #c0c4cc 0%, #909399 100%);
  box-shadow: 0 4px 15px rgba(192, 196, 204, 0.3);
}

.rank-number.rank-3 {
  background: linear-gradient(135deg, #cd7f32 0%, #b8721e 100%);
  box-shadow: 0 4px 15px rgba(205, 127, 50, 0.3);
}

/* 车型图片 */
.vehicle-image {
  position: relative;
  flex-shrink: 0;
}

.vehicle-image img {
  width: 160px;
  height: 100px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.image-overlay {
  position: absolute;
  top: 8px;
  left: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 车型信息 */
.vehicle-info {
  flex: 1;
  min-width: 0;
}

.vehicle-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.3;
}

.vehicle-specs {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
}

.price-range {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
}

.fuel-type-badge {
  display: flex;
  gap: 8px;
}

/* 油耗核心数据 */
.fuel-data {
  text-align: center;
  min-width: 200px;
}

.main-fuel-consumption {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin-bottom: 12px;
}

.fuel-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.fuel-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.fuel-value.excellent {
  color: #67c23a;
}

.fuel-value.good {
  color: #4facfe;
}

.fuel-value.normal {
  color: #e6a23c;
}

.fuel-unit {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.data-reliability {
  display: flex;
  justify-content: center;
}

/* 经济性评估 */
.economy-assessment {
  min-width: 180px;
}

.cost-preview {
  margin-bottom: 12px;
}

.cost-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.cost-label {
  font-size: 12px;
  color: #909399;
}

.cost-value {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.cost-comparison {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.comparison-label {
  color: #909399;
}

.comparison-value.higher {
  color: #f56c6c;
}

.comparison-value.lower {
  color: #67c23a;
}

.economy-score {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.score-label {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.score-bar {
  height: 6px;
  background: #e8eaed;
  border-radius: 3px;
  overflow: hidden;
  position: relative;
}

.score-fill {
  height: 100%;
  border-radius: 3px;
  transition: all 0.3s ease;
}

.score-fill.excellent {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.score-fill.good {
  background: linear-gradient(135deg, #4facfe 0%, #66b1ff 100%);
}

.score-fill.fair {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.score-fill.poor {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.score-text {
  font-size: 11px;
  color: #4facfe;
  text-align: center;
  font-weight: 600;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 120px;
}

.action-buttons .el-button {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.action-buttons .el-dropdown {
  width: 100%;
}

.action-buttons .el-dropdown .el-button {
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
}

.model-thumb {
  width: 60px;
  height: 40px;
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
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.model-fuel {
  font-size: 12px;
  color: #4facfe;
  font-weight: 500;
}

.price-display {
  margin-bottom: 16px;
}

.main-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.price-label {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.price-value {
  font-size: 28px;
  font-weight: 700;
  color: #f56c6c;
}

.price-unit {
  font-size: 14px;
  color: #909399;
}

.price-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.price-item {
  padding: 8px 12px;
  background: #f8fafb;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
}

.price-trend {
  margin-bottom: 8px;
  font-size: 14px;
}

.trend-label {
  color: #909399;
}

.trend-value.increase {
  color: #f56c6c;
  font-weight: 600;
}

.trend-value.decrease {
  color: #67c23a;
  font-weight: 600;
}

.update-time {
  font-size: 12px;
  color: #c0c4cc;
}

/* 对比计算器弹窗 */
.modal-calculator-content {
  max-height: 70vh;
  overflow-y: auto;
}

.calculator-models {
  margin-bottom: 24px;
}

.calculator-models h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.model-selection {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.calculator-model-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  flex: 1;
  min-width: 240px;
}

.calc-model-image {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
}

.calc-model-info {
  flex: 1;
}

.calc-model-info h5 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.calc-model-fuel {
  font-size: 14px;
  color: #4facfe;
  margin-bottom: 8px;
}

.calc-cost-result {
  font-size: 12px;
  color: #909399;
}

.calculator-summary h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.cost-chart-container {
  height: 250px;
  width: 100%;
  margin-bottom: 16px;
}

.savings-analysis {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.savings-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: 8px;
}

.savings-label {
  font-size: 14px;
  color: #606266;
}

.savings-value {
  font-size: 16px;
  font-weight: 600;
  color: #4facfe;
}

/* 车型详情抽屉 */
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
  flex-shrink: 0;
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
  color: #f56c6c;
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

.cost-analysis {
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
}

.cost-breakdown {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.breakdown-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #e8eaed;
}

.breakdown-item:last-child {
  border-bottom: none;
}

.breakdown-label {
  font-size: 14px;
  color: #606266;
}

.breakdown-value {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.detail-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* Element Plus 组件样式优化 */
.el-card {
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.el-button {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.el-button:hover {
  transform: translateY(-1px);
}

.el-form-item__label {
  font-weight: 500;
}

.el-select .el-input__wrapper {
  border-radius: 8px;
}

.el-input-number .el-input__wrapper {
  border-radius: 8px;
}

.el-radio-group .el-radio {
  margin-right: 16px;
}

.el-radio__label {
  font-weight: 500;
}

.el-switch__label {
  font-weight: 500;
}

.el-rate {
  --el-rate-fill-color: #ff9900;
}

.el-dialog__header {
  border-radius: 8px 8px 0 0;
}

.el-drawer__header {
  border-bottom: 1px solid #e8eaed;
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

.calculator-card,
.filter-card,
.ranking-card,
.quick-compare-card,
.utility-card {
  animation: slideInUp 0.6s ease-out;
}

.ranking-item {
  animation: slideInUp 0.4s ease-out;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .ranking-item {
    grid-template-columns: auto 140px 1fr 180px 160px 100px;
    gap: 16px;
  }

  .header-left h2 {
    font-size: 28px;
  }

  .cost-cards {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

@media (max-width: 1200px) {
  .ranking-item {
    grid-template-columns: auto 120px 1fr 160px 140px;
    gap: 12px;
  }

  .calculator-content {
    padding: 16px;
  }

  .calculator-inputs,
  .calculator-results {
    padding: 0;
  }

  .calculator-results {
    border-left: none;
    border-top: 1px solid #e8eaed;
    padding-top: 20px;
    margin-top: 20px;
  }

  .utility-tools .el-col {
    margin-bottom: 16px;
  }
}

@media (max-width: 768px) {
  .fuel-cons-list {
    padding: 0 8px;
  }

  .header-content {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .header-actions {
    justify-content: center;
  }

  .header-left h2 {
    font-size: 24px;
    text-align: center;
  }

  .header-left p {
    text-align: center;
  }

  .ranking-item {
    grid-template-columns: auto 1fr;
    gap: 16px;
    padding: 16px;
  }

  .rank-number {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }

  .vehicle-image img {
    width: 120px;
    height: 80px;
  }

  .fuel-value {
    font-size: 20px;
  }

  .filter-content .el-row {
    flex-direction: column;
    gap: 12px;
  }

  .ranking-controls {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .selected-models {
    flex-direction: column;
    gap: 12px;
  }

  .model-selection {
    flex-direction: column;
  }

  .detail-header {
    flex-direction: column;
    gap: 16px;
  }

  .detail-image {
    width: 100%;
    max-width: 300px;
    margin: 0 auto;
  }

  .detail-actions {
    flex-direction: column;
  }

  .savings-analysis {
    gap: 8px;
  }

  .savings-item {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
}

@media (max-width: 480px) {
  .calculator-models h4,
  .calculator-summary h4 {
    font-size: 14px;
  }

  .calculator-model-item {
    min-width: 100%;
  }

  .detail-basic h2 {
    font-size: 20px;
  }

  .price-display {
    text-align: center;
  }

  .main-price {
    justify-content: center;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .fuel-cons-list {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .calculator-card,
  .filter-card,
  .ranking-card,
  .quick-compare-card,
  .utility-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .ranking-item,
  .selected-model-item,
  .calculator-model-item {
    background: #2d2d2d;
    border-color: #404040;
  }

  .fuel-saving-tips,
  .cost-card {
    background: #363636;
    border-color: #505050;
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
.ranking-item:focus-visible {
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

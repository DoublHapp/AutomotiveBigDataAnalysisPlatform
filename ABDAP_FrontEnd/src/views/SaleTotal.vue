<template>
  <div class="sale-total-view">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>销售总览</h2>
          <p>汽车销售数据统计分析与业务洞察</p>
        </div>
        <div class="header-actions">
          <el-button type="warning" :icon="Setting" @click="showTargetDialog = true">
            目标设置
          </el-button>
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
          <el-button type="success" :icon="Download" @click="exportData"> 导出报告 </el-button>
        </div>
      </div>
    </el-card>

    <!-- 目标和投入设置弹窗 -->
    <el-dialog
      v-model="showTargetDialog"
      title="销售目标与营销投入设置"
      width="60%"
      :before-close="handleTargetDialogClose"
    >
      <div class="target-setting-content">
        <el-form :model="targetForm" :rules="targetRules" ref="targetFormRef" label-width="140px">
          <!-- 月度目标 -->
          <el-form-item label="月度销量目标:" prop="monthTarget">
            <el-input-number
              v-model="targetForm.monthTarget"
              :min="0"
              :max="1000000"
              :step="1000"
              :precision="0"
              style="width: 200px"
            />
            <span style="margin-left: 8px; color: #909399">台</span>
          </el-form-item>
          <!-- 季度目标 -->
          <el-form-item label="季度销量目标:" prop="quarterTarget">
            <el-input-number
              v-model="targetForm.quarterTarget"
              :min="0"
              :max="3000000"
              :step="1000"
              :precision="0"
              style="width: 200px"
            />
            <span style="margin-left: 8px; color: #909399">台</span>
          </el-form-item>
          <!-- 年度目标 -->
          <el-form-item label="年度销量目标:" prop="yearTarget">
            <el-input-number
              v-model="targetForm.yearTarget"
              :min="0"
              :max="12000000"
              :step="10000"
              :precision="0"
              style="width: 200px"
            />
            <span style="margin-left: 8px; color: #909399">台</span>
          </el-form-item>

          <!-- 营销投入设置 -->
          <el-form-item label="营销投入:" prop="marketingInvestment">
            <el-input-number
              v-model="targetForm.marketingInvestment"
              :min="100000"
              :max="100000000"
              :step="100000"
              :precision="0"
              style="width: 200px"
            />
            <span style="margin-left: 8px; color: #909399">元</span>
            <el-tag size="small" type="info" style="margin-left: 12px">
              约 {{ (targetForm.marketingInvestment / 10000).toFixed(0) }} 万元
            </el-tag>
          </el-form-item>

          <!--行业平均ROI输入 -->
          <el-form-item label="行业平均ROI:" prop="industryAverageROI">
            <el-input-number
              v-model="targetForm.industryAverageROI"
              :min="-100"
              :max="100"
              :step="1"
              :precision="1"
              style="width: 200px"
            />
            <el-tooltip content="请输入当前行业的平均ROI水平作为对比基准" placement="top">
              <el-icon style="margin-left: 8px; color: #909399"><QuestionFilled /></el-icon>
            </el-tooltip>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showTargetDialog = false">取消</el-button>
          <el-button @click="resetTargetForm">重置</el-button>
          <el-button type="primary" @click="saveTargets">保存设置</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 业务预警区 -->
    <el-card v-if="businessAlerts.length > 0" class="alert-card" shadow="never">
      <template #header>
        <div class="alert-header">
          <el-icon color="#f56c6c"><Warning /></el-icon>
          <span>业务预警</span>
        </div>
      </template>
      <div class="alerts-container">
        <el-alert
          v-for="alert in businessAlerts"
          :key="alert.id"
          :title="alert.title"
          :description="alert.message"
          :type="alert.type"
          :closable="false"
          show-icon
          class="business-alert"
        >
          <template #default>
            <div class="alert-content">
              <p>{{ alert.message }}</p>
            </div>
          </template>
        </el-alert>
      </div>
    </el-card>

    <!-- 全局筛选器 -->
    <el-card class="global-filter" shadow="never">
      <div class="filter-row">
        <div class="filter-group">
          <label>时间范围:</label>
          <el-radio-group v-model="globalFilters.timeRange" @change="handleGlobalFilterChange">
            <el-radio-button value="month">近一月</el-radio-button>
            <el-radio-button value="quarter">近一季</el-radio-button>
            <el-radio-button value="year">近一年</el-radio-button>
            <el-radio-button value="all">全部时间</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-if="globalFilters.timeRange === 'custom'"
            v-model="globalFilters.customDateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleGlobalFilterChange"
            style="margin-left: 12px"
          />
        </div>

        <div class="filter-group">
          <label>车型:</label>
          <el-select
            v-model="globalFilters.carModel"
            placeholder="搜索车型"
            filterable
            remote
            :remote-method="searchCarModels"
            :loading="carModelSearchLoading"
            clearable
            @change="handleGlobalFilterChange"
            style="width: 220px"
          >
            <el-option label="全部车型" value="all" />
            <el-option
              v-for="model in carModelSearchResults"
              :key="model.carModelId"
              :label="`${model.brandName} ${model.modelName}`"
              :value="model.carModelId.toString()"
            />
          </el-select>
        </div>

        <el-select
          v-model="globalFilters.region"
          filterable
          remote
          reserve-keyword
          placeholder="输入地区名称搜索"
          :remote-method="searchRegionByName"
          :loading="regionSearchLoading"
          @change="handleGlobalFilterChange"
          clearable
          style="width: 200px"
        >
          <el-option
            v-for="region in regionSearchResults"
            :key="region.regionId"
            :label="region.regionName"
            :value="region.regionId || region.regionName"
          />
        </el-select>
      </div>
    </el-card>

    <!-- 关键业务指标卡片 -->
    <el-row :gutter="20" class="kpi-section">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="kpi-card achievement" @click="showKPIDetail('achievement')">
          <div class="kpi-content">
            <div class="kpi-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="kpi-details">
              <div
                class="kpi-value"
                :class="
                  achievementRate >= 100 ? 'success' : achievementRate >= 85 ? 'warning' : 'danger'
                "
              >
                {{ achievementRate.toFixed(1) }}%
              </div>
              <div class="kpi-label">目标完成率</div>
              <div class="kpi-trend">
                实际: {{ actualSales.toLocaleString() }} / 目标:
                {{ targetSales.toLocaleString() }} 台
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="kpi-card yoy-growth" @click="showKPIDetail('growth')">
          <div class="kpi-content">
            <div class="kpi-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="kpi-details">
              <div class="kpi-value" :class="yoyGrowth >= 0 ? 'success' : 'danger'">
                {{ yoyGrowth >= 0 ? '+' : '' }}{{ yoyGrowth.toFixed(1) }}%
              </div>
              <div class="kpi-label">同比增长</div>
              <div class="kpi-trend">
                当年: {{ actualSales.toLocaleString() }} / 去年:
                {{ lastYearSales.toLocaleString() }} 台
              </div>
              <div class="kpi-benchmark">行业平均: {{ industryGrowth.toFixed(1) }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="kpi-card market-share" @click="showKPIDetail('share')">
          <div class="kpi-content">
            <div class="kpi-icon">
              <el-icon><PieChart /></el-icon>
            </div>
            <div class="kpi-details">
              <div class="kpi-value">{{ marketShare.toFixed(1) }}%</div>
              <div class="kpi-label">市场份额</div>
              <div class="kpi-trend">行业总量: {{ totalMarketSales.toLocaleString() }} 台</div>
              <div class="kpi-rank">行业排名: 第{{ marketShareRank }}位</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="kpi-card channel-efficiency" @click="showKPIDetail('roi')">
          <div class="kpi-content">
            <div class="kpi-icon">
              <el-icon><Money /></el-icon>
            </div>
            <div class="kpi-details">
              <div
                class="kpi-value"
                :class="channelROI >= 4 ? 'success' : channelROI >= 2 ? 'warning' : 'danger'"
              >
                {{ channelROI.toFixed(1) }}
              </div>
              <div class="kpi-label">实际ROI</div>
              <div class="kpi-trend">投入: {{ (channelInvestment / 10000).toFixed(0) }} 万</div>
              <div class="kpi-target">目标ROI: {{ targetForm.industryAverageROI }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据摘要卡片 -->
    <el-row :gutter="20" class="summary-section">
      <el-col :xs="24" :md="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-content">
            <div class="summary-icon sales">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="summary-details">
              <div class="summary-value">{{ actualSales.toLocaleString() }}</div>
              <div class="summary-label">总销量 (台)</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-content">
            <div class="summary-icon amount">
              <el-icon><Money /></el-icon>
            </div>
            <div class="summary-details">
              <div class="summary-value">
                {{ ((businessMetrics.averagePrice * actualSales) / 10000).toFixed(0) }}
              </div>
              <div class="summary-label">总销售额 (万元)</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-content">
            <div class="summary-icon models">
              <el-icon><Box /></el-icon>
            </div>
            <div class="summary-details">
              <div class="summary-value">{{ availableCarModels.length }}</div>
              <div class="summary-label">车型总数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="6">
        <el-card shadow="never" class="summary-card">
          <div class="summary-content">
            <div class="summary-icon regions">
              <el-icon><Location /></el-icon>
            </div>
            <div class="summary-details">
              <div class="summary-value">{{ availableRegions.length }}</div>
              <div class="summary-label">覆盖地区</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表联动区域 -->
    <el-row :gutter="20" class="charts-section">
      <!-- 销量趋势图 - 增强版 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>销量趋势分析</span>
              <div class="chart-actions">
                <el-button
                  size="small"
                  @click="toggleTrendComparison"
                  :type="showComparison ? 'primary' : ''"
                >
                  {{ showComparison ? '隐藏' : '显示' }}同比
                </el-button>
                <el-button size="small" @click="showForecast" type="success"> 预测分析 </el-button>
                <el-dropdown @command="handleTrendExport">
                  <el-button size="small">
                    导出<el-icon><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="image">导出图片</el-dropdown-item>
                      <el-dropdown-item command="excel">导出Excel</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
            <div class="trend-analysis-summary">
              <el-tag :type="trendAnalysis.type">{{ trendAnalysis.summary }}</el-tag>
              <span class="analysis-detail">{{ trendAnalysis.detail }}</span>
            </div>
          </template>
          <div
            ref="salesTrendChart"
            class="chart-container"
            v-loading="loading"
            @click="handleTrendChartClick"
          ></div>
        </el-card>
      </el-col>

      <!-- 销售额变化图 - 增强版 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>销售额分析</span>
              <div class="selected-time-info" v-if="selectedTimePoint">
                <el-tag size="small" type="success">{{ selectedTimePoint }}</el-tag>
                <el-button size="small" type="text" @click="clearTimeSelection">清除选择</el-button>
              </div>
            </div>
            <div class="amount-analysis-summary">
              <el-tag :type="amountAnalysis.type">{{ amountAnalysis.summary }}</el-tag>
              <span class="analysis-detail">{{ amountAnalysis.detail }}</span>
            </div>
          </template>
          <div
            ref="salesAmountChart"
            class="chart-container"
            v-loading="loading"
            @click="handleAmountChartClick"
          ></div>
        </el-card>
      </el-col>

      <!-- 车型排行榜 - 增强版 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>热门车型排行</span>
              <div class="ranking-filters">
                <el-select
                  v-model="modelRankingType"
                  size="small"
                  @change="handleRankingTypeChange"
                >
                  <el-option label="销量排行" value="sales" />
                  <el-option label="增长率排行" value="growth" />
                  <el-option label="市场份额排行" value="share" />
                </el-select>
                <el-input-number
                  v-model="topN"
                  :min="5"
                  :max="20"
                  size="small"
                  @change="handleTopNChange"
                />
              </div>
            </div>
          </template>
          <div
            ref="topModelsChart"
            class="chart-container"
            v-loading="loading"
            @click="handleModelChartClick"
          ></div>
        </el-card>
      </el-col>

      <!-- 地区销量分布 - 增强版 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>地区销量分布</span>
              <div class="region-controls">
                <div class="region-highlight" v-if="highlightedRegion">
                  <el-tag type="success">{{ highlightedRegion }}</el-tag>
                  <el-button size="small" type="text" @click="clearRegionHighlight">清除</el-button>
                </div>
                <el-button size="small" @click="showHeatMap" type="primary">热力图</el-button>
              </div>
            </div>
          </template>
          <div
            ref="regionSalesChart"
            class="chart-container"
            v-loading="loading"
            @click="handleRegionChartClick"
          ></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细分析弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="detailDialogTitle"
      width="80%"
      :before-close="handleDetailDialogClose"
    >
      <div class="detail-analysis-content">
        <el-tabs v-model="activeDetailTab">
          <el-tab-pane label="数据详情" name="data">
            <el-table :data="detailTableData" stripe>
              <el-table-column prop="name" label="项目" />
              <el-table-column prop="value" label="数值" />
              <el-table-column prop="change" label="变化" />
              <el-table-column prop="trend" label="趋势">
                <template #default="{ row }">
                  <el-icon v-if="row.trend === 'up'" color="#67c23a"><ArrowUp /></el-icon>
                  <el-icon v-else-if="row.trend === 'down'" color="#f56c6c"><ArrowDown /></el-icon>
                  <el-icon v-else color="#909399"><Minus /></el-icon>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="对比分析" name="comparison">
            <div ref="comparisonChart" class="comparison-chart"></div>
          </el-tab-pane>
          <el-tab-pane label="预测分析" name="forecast">
            <div class="forecast-content">
              <p>基于当前趋势的预测分析...</p>
              <div ref="forecastChart" class="forecast-chart"></div>
              <div class="forecast-summary">
                <h4>预测摘要</h4>
                <p>{{ forecastSummary }}</p>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="建议措施" name="recommendations">
            <div class="recommendations-content">
              <div v-for="rec in currentRecommendations" :key="rec.id" class="recommendation-item">
                <h4>{{ rec.title }}</h4>
                <p>{{ rec.description }}</p>
                <div class="recommendation-actions">
                  <el-button size="small" type="primary">采纳建议</el-button>
                  <el-button size="small">稍后处理</el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <!-- KPI详情弹窗 -->
    <el-dialog v-model="showKPIDialog" :title="kpiDialogTitle" width="60%">
      <div class="kpi-detail-content">
        <div class="kpi-overview">
          <div class="kpi-main-value">
            <span class="kpi-number">{{ currentKPIValue }}</span>
            <span class="kpi-unit">{{ currentKPIUnit }}</span>
          </div>
          <div class="kpi-comparison">
            <div class="comparison-item">
              <span class="comparison-label">与目标对比:</span>
              <span class="comparison-value" :class="kpiVsTarget.type">
                {{ kpiVsTarget.text }}
              </span>
            </div>
            <div class="comparison-item">
              <span class="comparison-label">与行业对比:</span>
              <span class="comparison-value" :class="kpiVsIndustry.type">
                {{ kpiVsIndustry.text }}
              </span>
            </div>
          </div>
        </div>
        <div ref="kpiTrendChart" class="kpi-trend-chart"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  Warning,
  TrendCharts,
  DataAnalysis,
  PieChart,
  Money,
  ArrowDown,
  ArrowUp,
  Minus,
  Trophy,
  Setting,
  QuestionFilled,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()

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
  parentRegionName: string | null
}

//  基础数据层
interface BaseData {
  carModels?: CarModel[]
  saleRecords?: SaleRecord[]
  regions?: Region[]
  topLevelRegions?: Region[]
  nonTopLevelRegions?: Region[]
}

// 计算数据层
interface CalculatedData {
  salesTrend: SalesTrendItem[]
  salesAmount: SalesAmountItem[]
  topModels: TopModelItem[]
  regionSales: RegionSalesItem[]
  businessMetrics: BusinessMetrics
}

// 业务指标层
interface BusinessMetrics {
  actualSales: number
  lastYearSales: number
  averagePrice: number
  totalMarketSales: number
  marketShare: number
  achievementRate: number
  yoyGrowth: number
  totalROI: number
  industryGrowth: number
}

// 业务数据接口
interface SalesTrendItem {
  date: string
  salesVolume: number
  lastYearSalesVolume?: number
  month: number
  year: number
}

interface SalesAmountItem {
  date: string
  salesAmount: number
  month: number
  year: number
}

interface TopModelItem {
  carModel: string
  brandName: string
  salesVolume: number
  salesAmount: number
  growthRate: number
  marketShare: number
  profit: number
}

interface RegionSalesItem {
  region: string
  salesVolume: number
  salesAmount: number
  marketShare: number
}

interface RegionOption {
  id: number | string
  name: string
}

// =============================================
// 响应式数据
// =============================================

const loading = ref(false)
const showComparison = ref(false)
const showDetailDialog = ref(false)
const showKPIDialog = ref(false)
const activeDetailTab = ref('data')
const detailDialogTitle = ref('')
const kpiDialogTitle = ref('')
const selectedTimePoint = ref('')
const highlightedRegion = ref('')
const modelRankingType = ref('sales')
const topN = ref(10)
const showTargetDialog = ref(false)
const targetFormRef = ref()

const regionSearchResults = ref<Region[]>([])
const regionSearchLoading = ref(false)

const carModelSearchResults = ref<CarModel[]>([])
const carModelSearchLoading = ref(false)
let carModelSearchTimer: ReturnType<typeof setTimeout> | null = null

const searchCarModels = (query: string) => {
  if (carModelSearchTimer) clearTimeout(carModelSearchTimer)
  carModelSearchTimer = setTimeout(async () => {
    if (!query) {
      carModelSearchResults.value = []
      return
    }
    carModelSearchLoading.value = true
    try {
      const response = await axios.get('/api/car-models/search', {
        params: { keyword: query, limit: 20 },
      })
      if (response.data.status === 200 && response.data.data) {
        carModelSearchResults.value = response.data.data
      } else {
        carModelSearchResults.value = []
      }
    } catch (error) {
      carModelSearchResults.value = []
    } finally {
      carModelSearchLoading.value = false
    }
  }, 300) // 300ms防抖
}

//  基础数据存储
const baseData = ref<BaseData>({
  carModels: [],
  saleRecords: [],
  regions: [],
  topLevelRegions: [],
  nonTopLevelRegions: [],
})

//  计算后的业务数据
const salesTrendData = ref<SalesTrendItem[]>([])
const salesAmountData = ref<SalesAmountItem[]>([])
const topModelsData = ref<TopModelItem[]>([])
const regionSalesData = ref<RegionSalesItem[]>([])
const availableRegions = ref<RegionOption[]>([])
const availableCarModels = ref<CarModel[]>([])
const detailTableData = ref<any[]>([])
const currentRecommendations = ref<any[]>([])
const forecastSummary = ref('')

const monthlySummaryData = ref<any[]>([])

// 业务指标
const businessMetrics = ref<BusinessMetrics>({
  actualSales: 0,
  lastYearSales: 0,
  averagePrice: 0,
  totalMarketSales: 0,
  marketShare: 0,
  achievementRate: 0,
  yoyGrowth: 0,
  totalROI: 0,
  industryGrowth: 0,
})

// 目标设置表单
const targetForm = reactive({
  monthTarget: 0,
  quarterTarget: 0,
  yearTarget: 0,
  marketingInvestment: 0,
  industryAverageROI: 0,
})

// 表单验证规则
const targetRules = {
  monthTarget: [
    { required: true, message: '请输入月度销量目标', trigger: 'blur' },
    { type: 'number', min: 0, message: '销量目标不能少于0台', trigger: 'blur' },
  ],
  quarterTarget: [
    { required: true, message: '请输入季度销量目标', trigger: 'blur' },
    { type: 'number', min: 0, message: '季度目标不能少于0台', trigger: 'blur' },
  ],
  yearTarget: [
    { required: true, message: '请输入年度销量目标', trigger: 'blur' },
    { type: 'number', min: 0, message: '年度目标不能少于0台', trigger: 'blur' },
  ],
  marketingInvestment: [
    { required: true, message: '请输入营销投入', trigger: 'blur' },
    { type: 'number', min: 0, message: '营销投入不能少于0万元', trigger: 'blur' },
  ],
  industryAverageROI: [
    { type: 'number', min: -100, max: 100, message: 'ROI应在-100%到100%之间', trigger: 'blur' },
  ],
}

// 全局筛选器
const globalFilters = reactive({
  timeRange: 'year',
  customDateRange: null as [Date, Date] | null,
  carModel: 'all',
  region: 'all', // 这里 region 可能是 regionId（市）或 regionName（省）
})

// 图表实例
const salesTrendChart = ref<HTMLDivElement>()
const salesAmountChart = ref<HTMLDivElement>()
const topModelsChart = ref<HTMLDivElement>()
const regionSalesChart = ref<HTMLDivElement>()
const comparisonChart = ref<HTMLDivElement>()
const forecastChart = ref<HTMLDivElement>()
const kpiTrendChart = ref<HTMLDivElement>()

let salesTrendChartInstance: echarts.ECharts | null = null
let salesAmountChartInstance: echarts.ECharts | null = null
let topModelsChartInstance: echarts.ECharts | null = null
let regionSalesChartInstance: echarts.ECharts | null = null
let comparisonChartInstance: echarts.ECharts | null = null
let forecastChartInstance: echarts.ECharts | null = null
let kpiTrendChartInstance: echarts.ECharts | null = null

//辅助函数

//计算时间范围参数
const getMonthRange = () => {
  let startMonth = ''
  let endMonth = ''
  if (globalFilters.timeRange === 'custom' && globalFilters.customDateRange) {
    const [start, end] = globalFilters.customDateRange
    startMonth = `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}`
    endMonth = `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}`
  } else {
    const now = new Date()
    let monthsBack = 12
    switch (globalFilters.timeRange) {
      case 'month':
        monthsBack = 1
        break
      case 'quarter':
        monthsBack = 3
        break
      case 'year':
        monthsBack = 12
        break
      case 'all':
        monthsBack = 60
        break // 假设最多5年
    }
    const start = new Date(now.getFullYear(), now.getMonth() - monthsBack + 1, 1)
    startMonth = `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}`
    endMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  }
  return { startMonth, endMonth }
}

// =============================================
// API 调用函数
// =============================================


const fetchRegions = async (): Promise<Region[]> => {
  try {
    console.log(' 正在获取地区信息...')
    const response = await axios.get('/api/regions')

    if (response.data.status === 200 && response.data.data) {
      console.log('获取地区信息成功:', response.data.data.length, '个地区')
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
    console.log(' 正在获取省份信息...')
    const response = await axios.get('/api/regions/top-level/old')

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
    console.log(' 正在获取城市信息...')
    const response = await axios.get('/api/regions/non-top-level')

    if (response.data.status === 200 && response.data.data) {
      console.log(' 获取城市信息成功:', response.data.data.length, '个城市')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error(' 获取城市信息失败:', error)
    ElMessage.error('城市数据加载失败')
    throw error
  }
}

const fetchMonthlySummary = async (params: {
  startMonth: string
  endMonth: string
  region?: string
  carModel?: string
}) => {
  try {
    const response = await axios.get('/api/complex/monthly-summary', { params })
    if (response.data.status === 200&& response.data.data) {
      return response.data.data
    } else {
      throw new Error(response.data.message || 'API返回错误')
    }
  } catch (error) {
    ElMessage.error('获取月度汇总数据失败')
    return []
  }
}


const fetchSalesRanking = async (startMonth: string, endMonth: string, region: string = 'all', top: number = 10) => {
  try {
    const response = await axios.get('/api/ranking/sales', {
      params: { startMonth, endMonth, region, top }
    })
    if (response.data.status === 200 && response.data.data) {
      return response.data.data
    } else {
      throw new Error(response.data.message || 'API返回错误')
    }
  } catch (error) {
    ElMessage.error('获取销量排行榜数据失败')
    return []
  }
}

// 获取地区销量排行榜数据
const fetchRegionSalesRanking = async (
  startMonth: string,
  endMonth: string,
  region: string = 'all',
  top: number = 10
) => {
  try {
    const response = await axios.get('/api/ranking/region-sales', {
      params: { startMonth, endMonth, region, top }
    })
    if (response.data.status === 200 && response.data.data) {
      return response.data.data
    } else {
      throw new Error(response.data.message || 'API返回错误')
    }
  } catch (error) {
    ElMessage.error('获取地区销量排行榜数据失败')
    return []
  }
}



// =============================================
// 数据处理函数
// =============================================

const loadMonthlySummary = async () => {
  loading.value = true
  try {
    // 当前时间范围
    const { startMonth, endMonth } = getMonthRange()
    const params: any = { startMonth, endMonth }
    params.region =
      globalFilters.region && globalFilters.region !== 'all' ? globalFilters.region : 'all'
     // 车型筛选：将 carModelId 转为 modelName
    if (globalFilters.carModel && globalFilters.carModel !== 'all') {
      const selectedModel = carModelSearchResults.value.find(
        m => m.carModelId.toString() === globalFilters.carModel
      )
      params.carModel = selectedModel ? selectedModel.modelName : globalFilters.carModel
    } else {
      params.carModel = 'all'
    }

    // 请求当前数据
    const currentData = await fetchMonthlySummary(params)

    // 计算去年同期的时间范围
    const startYear = parseInt(startMonth.split('-')[0])
    const startMon = parseInt(startMonth.split('-')[1])
    const endYear = parseInt(endMonth.split('-')[0])
    const endMon = parseInt(endMonth.split('-')[1])
    const lastStartMonth = `${startYear - 1}-${String(startMon).padStart(2, '0')}`
    const lastEndMonth = `${endYear - 1}-${String(endMon).padStart(2, '0')}`

    const lastParams = { ...params, startMonth: lastStartMonth, endMonth: lastEndMonth }
    const lastYearData = await fetchMonthlySummary(lastParams)

    // 合并数据，标记年份
    monthlySummaryData.value = [
      ...currentData.map((item:any) => ({ ...item, _period: 'current' })),
      ...lastYearData.map((item:any) => ({ ...item, _period: 'last' })),
    ]
  } finally {
    loading.value = false
  }
}

const loadAllBaseData = async () => {
  try {
    console.log('开始加载基础数据...')

    const [regions, topLevelRegions, nonTopLevelRegions] =
      await Promise.all([
        fetchRegions(),
        fetchTopLevelRegions(),
        fetchNonTopLevelRegions(),
      ])

    baseData.value = {
      regions,
      topLevelRegions,
      nonTopLevelRegions,
    }

    console.log(' 基础数据加载完成:', {
      地区数量: regions.length,
      省份数量: topLevelRegions.length,
      城市数量: nonTopLevelRegions.length,
    })

    ElMessage.success('基础数据加载完成')
  } catch (error) {
    console.error(' 基础数据加载失败:', error)
    ElMessage.error('数据加载失败，请检查网络连接')
    throw error
  }
}

const searchRegionByName = async (query: string) => {
  if (!query) {
    regionSearchResults.value = []
    return
  }
  regionSearchLoading.value = true
  try {
    // 合并省级和市级地区进行搜索，并去重
    const allRegions = [...baseData.value.topLevelRegions, ...baseData.value.nonTopLevelRegions]
    const filtered = allRegions.filter((region) => region.regionName.includes(query))
    // 用 Map 以 regionName 去重
    const unique = Array.from(
      new Map(filtered.map((region) => [region.regionName, region])).values(),
    )
    regionSearchResults.value = unique
  } finally {
    regionSearchLoading.value = false
  }
}

const getRegionRequestParams = () => {
  if (globalFilters.region === 'all') return {}
  // 先在省列表中查找
  const province = baseData.value.topLevelRegions.find(
    (r) => r.regionName === globalFilters.region || r.regionId === globalFilters.region,
  )
  if (province) {
    return { regionName: province.regionName }
  }
  // 再在市列表中查找
  const city = baseData.value.nonTopLevelRegions.find(
    (r) => r.regionId === globalFilters.region || r.regionName === globalFilters.region,
  )
  if (city) {
    return { regionId: city.regionId }
  }
  return {}
}

// 处理销量趋势数据
const processSalesTrendData = () => {
  console.log('处理销量趋势数据...')

  // 拆分本期和去年同期
  const current = monthlySummaryData.value.filter(item => item._period === 'current')
  const last = monthlySummaryData.value.filter(item => item._period === 'last')

  // 以当前月份为基准，查找去年同期
  const trendData: SalesTrendItem[] = current.map((item) => {
    const lastYearItem = last.find(
      l => l.month.slice(5, 7) === item.month.slice(5, 7)
    )
    return {
      date: item.month,
      salesVolume: item.saleCount,
      lastYearSalesVolume: lastYearItem ? lastYearItem.saleCount : 0,
      month: parseInt(item.month.split('-')[1]),
      year: parseInt(item.month.split('-')[0]),
    }
  })

  salesTrendData.value = trendData

  // 计算实际销量总和和去年销量总和
  businessMetrics.value.actualSales = trendData.reduce((sum, item) => sum + item.salesVolume, 0)
  businessMetrics.value.lastYearSales = trendData.reduce((sum, item) => sum + (item.lastYearSalesVolume || 0), 0)

  console.log(
    ' 销量趋势处理完成，实际销量:',
    businessMetrics.value.actualSales,
    '去年销量:',
    businessMetrics.value.lastYearSales,
  )

}

// 处理销售额数据
const processSalesAmountData = () => {
  console.log(' 处理销售额数据...')

  const amountData: SalesAmountItem[] = monthlySummaryData.value.map((item) => ({
    date: item.month,
    salesAmount: Math.round((item.saleAmount / 10000) * 100) / 100, // 万元
    month: parseInt(item.month.split('-')[1]),
    year: parseInt(item.month.split('-')[0]),
  }))

  salesAmountData.value = amountData
  console.log(' 销售额数据处理完成')

}

// 处理车型排行数据
const processTopModelsData = async () => {
  console.log('处理车型排行数据...')
  if (modelRankingType.value === 'sales') {
    // 只在销量排行时调用后端接口
    const { startMonth, endMonth } = getMonthRange()
    const region = globalFilters.region && globalFilters.region !== 'all' ? globalFilters.region : 'all'
    const data = await fetchSalesRanking(startMonth, endMonth, region, topN.value)
    // 适配接口返回结构
    topModelsData.value = data.map((item:any) => ({
      carModel: item.modelName,
      brandName: item.brandName,
      salesVolume: item.saleCount,
      growthRate: item.saleGrowthRate != null ? item.saleGrowthRate * 100 : 0,
      marketShare: item.marketShare != null ? item.marketShare * 100 : 0,
      imageUrl: item.imageUrl,
      opinionScore: item.opinionScore,
    }))
    console.log('销量排行数据已更新:', topModelsData.value.length)
    return
  }


}

// 处理地区销量数据
const processRegionSalesData = async() => {
   console.log('处理地区销量数据...')
  const { startMonth, endMonth } = getMonthRange()
  const region = globalFilters.region && globalFilters.region !== 'all' ? globalFilters.region : 'all'
  const data = await fetchRegionSalesRanking(startMonth, endMonth, region, 20) // 20为最大地区数，可调整

  // 适配接口返回结构
  regionSalesData.value = data.map((item: any) => ({
    region: item.regionName,
    salesVolume: item.saleCount,
    salesAmount: 0, // 若接口无销售额字段可设为0或后端补充
    marketShare: item.marketShare != null ? item.marketShare * 100 : 0,
    growthRate: item.saleGrowthRate != null ? item.saleGrowthRate * 100 : 0,
    regionId: item.regionId,
    parentRegion: item.parentRegion,
    regionType: item.regionType,
  }))
  // 更新可用地区列表
  availableRegions.value = data.map((item: any) => ({
    id: item.regionId,
    name: item.regionName,
  }))

  console.log('地区销量排行数据已更新:', regionSalesData.value.length)
}

// 计算业务指标
const calculateBusinessMetrics = () => {
  console.log('计算业务指标...')
  // 总销量
  businessMetrics.value.actualSales = salesTrendData.value.reduce(
    (sum, item) => sum + item.salesVolume,
    0,
  )
  // 去年销量
  const currentYear = new Date().getFullYear()
  businessMetrics.value.lastYearSales = salesTrendData.value
    .filter((item) => item.year === currentYear - 1)
    .reduce((sum, item) => sum + item.salesVolume, 0)
  // 平均价格
  const totalAmount = salesAmountData.value.reduce((sum, item) => sum + item.salesAmount, 0)
  const totalSales = businessMetrics.value.actualSales
  businessMetrics.value.averagePrice = totalSales > 0 ? (totalAmount * 10000) / totalSales : 0
  // 目标完成率
  businessMetrics.value.achievementRate =
    targetSales.value > 0 ? (businessMetrics.value.actualSales / targetSales.value) * 100 : 0
  // 同比增长
  businessMetrics.value.yoyGrowth =
    businessMetrics.value.lastYearSales > 0
      ? ((businessMetrics.value.actualSales - businessMetrics.value.lastYearSales) /
          businessMetrics.value.lastYearSales) *
        100
      : businessMetrics.value.actualSales > 0
        ? 100
        : 0
  // ROI
  businessMetrics.value.totalROI =
    targetForm.marketingInvestment > 0
      ? ((businessMetrics.value.actualSales * businessMetrics.value.averagePrice -
          targetForm.marketingInvestment) /
          targetForm.marketingInvestment) *
        100
      : 0
  // 其他指标可继续补充
  console.log(' 业务指标计算完成:', businessMetrics.value)
}

// 处理所有数据
const processAllData = async() => {
  try {
    console.log('开始处理所有数据...')

     processSalesTrendData()
     processSalesAmountData()
    await processTopModelsData()
    await processRegionSalesData()
    calculateBusinessMetrics()

    console.log(' 所有数据处理完成')
  } catch (error) {
    console.error(' 数据处理失败:', error)
    ElMessage.error('数据处理失败，请重试')
  }
}

// =============================================
// 计算属性 - 从业务指标中获取
// =============================================

const targetSales = computed(() => {
  switch (globalFilters.timeRange) {
    case 'month':
      return targetForm.monthTarget
    case 'quarter':
      return targetForm.quarterTarget
    case 'year':
      return targetForm.yearTarget
    default:
      return targetForm.yearTarget
  }
})
const actualSales = computed(() => businessMetrics.value.actualSales)
const lastYearSales = computed(() => businessMetrics.value.lastYearSales)
const totalMarketSales = computed(() => businessMetrics.value.totalMarketSales)
const achievementRate = computed(() => businessMetrics.value.achievementRate)
const yoyGrowth = computed(() => businessMetrics.value.yoyGrowth)
const marketShare = computed(() => businessMetrics.value.marketShare)
const channelROI = computed(() => businessMetrics.value.totalROI)

const industryGrowth = computed(() => {
  // 只统计所有车型（region=all, carModel=all）下的月度数据
  const current = monthlySummaryData.value.filter(item => item._period === 'current')
  const last = monthlySummaryData.value.filter(item => item._period === 'last')
  const currentTotal = current.reduce((sum, item) => sum + (item.saleCount || 0), 0)
  const lastTotal = last.reduce((sum, item) => sum + (item.saleCount || 0), 0)
  return lastTotal > 0 ? ((currentTotal - lastTotal) / lastTotal) * 100 : (currentTotal > 0 ? 100 : 0)
})

// 其他计算属性

const marketShareRank = computed(() => {
  return 555;
})

const channelInvestment = computed(() => targetForm.marketingInvestment)
const currentKPIValue = ref('')
const currentKPIUnit = ref('')

const kpiVsTarget = computed(() => {
  return { type: 'success', text: '超出目标5.2%' }
})

const kpiVsIndustry = computed(() => {
  return { type: 'warning', text: '略低于行业平均' }
})

// 趋势分析
const trendAnalysis = computed(() => {
  if (salesTrendData.value.length < 3) {
    return { type: 'info', summary: '数据不足', detail: '需要更多数据进行分析' }
  }

  const recent = salesTrendData.value.slice(-3)
  const trend = recent[recent.length - 1].salesVolume - recent[0].salesVolume

  if (trend > 500) {
    return {
      type: 'success',
      summary: '强劲增长',
      detail: `近期销量增长${trend}台，增长趋势明显，建议加大市场投入`,
    }
  } else if (trend < -500) {
    return {
      type: 'danger',
      summary: '下滑风险',
      detail: `近期销量下降${Math.abs(trend)}台，需要关注市场变化并制定应对策略`,
    }
  } else {
    return {
      type: 'warning',
      summary: '平稳运行',
      detail: `销量波动在正常范围内`,
    }
  }
})

// 销售额分析
const amountAnalysis = computed(() => {
  if (salesAmountData.value.length < 3) {
    return { type: 'info', summary: '数据不足', detail: '需要更多数据进行分析' }
  }

  const recent = salesAmountData.value.slice(-3)
  const trend = recent[recent.length - 1].salesAmount - recent[0].salesAmount

  if (trend > 1000) {
    return {
      type: 'success',
      summary: '营收增长',
      detail: `销售额呈上升趋势，较两月前增长${trend.toFixed(1)}万元`,
    }
  } else if (trend < -1000) {
    return {
      type: 'danger',
      summary: '营收下滑',
      detail: `销售额下降${Math.abs(trend).toFixed(1)}万元，需要关注产品结构和定价策略`,
    }
  } else {
    return {
      type: 'warning',
      summary: '平稳发展',
      detail: '销售额变化平缓，建议关注市场动态',
    }
  }
})

// 业务预警
const businessAlerts = computed(() => {
  const alerts = []

  if (achievementRate.value < 70) {
    alerts.push({
      id: 1,
      type: 'error',
      title: '目标完成率预警',
      message: `当前完成率${achievementRate.value.toFixed(1)}%，距离目标差距较大`,
    })
  }

  if (yoyGrowth.value < -10) {
    alerts.push({
      id: 2,
      type: 'warning',
      title: '同比增长预警',
      message: `同比下降${Math.abs(yoyGrowth.value).toFixed(1)}%，需要制定增长策略`,
    })
  }

  if (channelROI.value < 2) {
    alerts.push({
      id: 3,
      type: 'warning',
      title: 'ROI偏低提醒',
      message: `实际ROI仅${channelROI.value.toFixed(1)}，低于行业标准`,
    })
  }

  return alerts
})

// =============================================
// 工具函数
// =============================================

const handleGlobalFilterChange = () => {
  console.log('全局筛选条件变更:', globalFilters)
  refreshData()
}
// =============================================
// 图表初始化函数
// =============================================

const initSalesTrendChart = async () => {
  console.log("初始化销量趋势图表...")
  if (!salesTrendChart.value || salesTrendData.value.length === 0) return

  const data = salesTrendData.value

  if (salesTrendChartInstance) {
    salesTrendChartInstance.dispose()
  }

  salesTrendChartInstance = echarts.init(salesTrendChart.value)

  // 计算目标线数据
  let targetValue = 0
  let targetLabel = ''
  if (globalFilters.timeRange === 'month') {
    targetValue = targetForm.monthTarget
    targetLabel = '月度目标'
  } else if (globalFilters.timeRange === 'quarter') {
    targetValue = targetForm.quarterTarget
    targetLabel = '季度目标'
  } else if (globalFilters.timeRange === 'year') {
    targetValue = targetForm.yearTarget
    targetLabel = '年度目标'
  }

  const option = {
    title: {
      text: '销量趋势分析',
      textStyle: { fontSize: 14, color: '#333' },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        let result = `<div style="padding: 8px;"><strong>${params[0].axisValue}</strong><br/>`
        params.forEach((item: any) => {
          result += `${item.seriesName}: ${item.value.toLocaleString()} 台<br/>`
        })
        result += '</div>'
        return result
      },
    },
    legend: {
      data: showComparison.value ? ['当期销量', '去年同期'] : ['当期销量'],
      top: 30,
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
      data: data.map((item) => item.date),
      axisLabel: { fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      name: '销量(台)',
      axisLabel: {
        formatter: (value: number) => value.toLocaleString(),
      },
    },
    series: [
      {
        name: '当期销量',
        type: 'line',
        data: data.map((item) => item.salesVolume),
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 3, color: '#409EFF' },
        areaStyle: { color: 'rgba(64, 158, 255, 0.1)' },
        // 新增目标线
        markLine:
          targetValue > 0
            ? {
                symbol: 'none',
                label: {
                  show: true,
                  formatter: targetLabel,
                  color: '#f56c6c',
                  fontWeight: 'bold',
                },
                lineStyle: {
                  color: '#f56c6c',
                  type: 'solid',
                  width: 2,
                },
                data: [
                  {
                    yAxis: targetValue,
                  },
                ],
              }
            : undefined,
      },
      ...(showComparison.value
        ? [
            {
              name: '去年同期',
              type: 'line',
              data: data.map((item) => item.lastYearSalesVolume || 0),
              smooth: true,
              symbol: 'circle',
              symbolSize: 6,
              lineStyle: { width: 2, color: '#909399', type: 'dashed' },
            },
          ]
        : []),
    ],
  }

  salesTrendChartInstance.setOption(option)
}

const initSalesAmountChart = async () => {
  if (!salesAmountChart.value || salesAmountData.value.length === 0) return

  const data = salesAmountData.value

  if (salesAmountChartInstance) {
    salesAmountChartInstance.dispose()
  }

  salesAmountChartInstance = echarts.init(salesAmountChart.value)

  const option = {
    title: {
      text: '销售额变化',
      textStyle: { fontSize: 14, color: '#333' },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        return `${data.axisValue}<br/>销售额: ${data.value.toFixed(1)} 万元`
      },
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
      data: data.map((item) => item.date),
      axisLabel: { fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      name: '销售额(万元)',
      axisLabel: {
        formatter: (value: number) => value.toFixed(0),
      },
    },
    series: [
      {
        name: '销售额',
        type: 'bar',
        data: data.map((item) => item.salesAmount),
        itemStyle: {
          color: '#67C23A',
          borderRadius: [4, 4, 0, 0],
        },
      },
    ],
  }

  salesAmountChartInstance.setOption(option)
}

const initTopModelsChart = async () => {
  if (!topModelsChart.value || topModelsData.value.length === 0) return

  const data = topModelsData.value

  if (topModelsChartInstance) {
    topModelsChartInstance.dispose()
  }

  topModelsChartInstance = echarts.init(topModelsChart.value)

  const colorList = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']

  const option = {
    title: {
      text: `车型${modelRankingType.value === 'sales' ? '销量' : modelRankingType.value === 'growth' ? '增长率' : '市场份额'}排行`,
      textStyle: { fontSize: 14, color: '#333' },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const data = params[0]
        const item = topModelsData.value[data.dataIndex]
        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 6px;">${data.axisValue}</div>
            <div>销量: ${item.salesVolume.toLocaleString()} 台</div>
            <div>增长率: ${item.growthRate >= 0 ? '+' : ''}${item.growthRate.toFixed(1)}%</div>
            <div>市场份额: ${item.marketShare.toFixed(1)}%</div>
          </div>
        `
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
      name:
        modelRankingType.value === 'sales'
          ? '销量(台)'
          : modelRankingType.value === 'growth'
            ? '增长率(%)'
            : '市场份额(%)',
      position: 'top',
      axisLabel: {
        formatter: (value: number) => {
          if (modelRankingType.value === 'sales') {
            return value.toLocaleString()
          } else {
            return value.toFixed(1) + '%'
          }
        },
      },
    },
    yAxis: {
      type: 'category',
      data: data.map((item) => `${item.brandName} ${item.carModel}`),
      axisLabel: { fontSize: 10 },
    },
    series: [
      {
        name: '排行',
        type: 'bar',
        data: data.map((item, index) => ({
          value:
            modelRankingType.value === 'sales'
              ? item.salesVolume
              : modelRankingType.value === 'growth'
                ? item.growthRate
                : item.marketShare,
          itemStyle: {
            color: colorList[index % colorList.length],
          },
        })),
        label: {
          show: true,
          position: 'right',
          fontSize: 10,
          formatter: (params: any) => {
            if (modelRankingType.value === 'sales') {
              return params.value.toLocaleString()
            } else {
              return params.value.toFixed(1) + '%'
            }
          },
        },
      },
    ],
  }

  topModelsChartInstance.setOption(option)
}

const initRegionSalesChart = async () => {
  if (!regionSalesChart.value || regionSalesData.value.length === 0) return

  const data = regionSalesData.value

  if (regionSalesChartInstance) {
    regionSalesChartInstance.dispose()
  }

  regionSalesChartInstance = echarts.init(regionSalesChart.value)

  const option = {
    title: {
      text: '地区销量分布',
      textStyle: { fontSize: 14, color: '#333' },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const data = params[0]
        return `${data.axisValue}<br/>销量: ${data.value.toLocaleString()} 台`
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '20%',
      top: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.region),
      axisLabel: { interval: 0, rotate: 45, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      name: '销量(台)',
      axisLabel: {
        formatter: (value: number) => value.toLocaleString(),
      },
    },
    series: [
      {
        name: '销量',
        type: 'bar',
        data: data.map((item) => item.salesVolume),
        itemStyle: {
          color: '#F56C6C',
          borderRadius: [4, 4, 0, 0],
        },
      },
    ],
  }

  regionSalesChartInstance.setOption(option)
}

const initAllCharts = async () => {
  await nextTick()
  await Promise.all([
    initSalesTrendChart(),
    initSalesAmountChart(),
    initTopModelsChart(),
    initRegionSalesChart(),
  ])
}

// =============================================
// 事件处理函数
// =============================================

const handleTrendChartClick = (params: any) => {
  const timeData = salesTrendData.value.find((item) => item.date === params.name)
  if (timeData) {
    selectedTimePoint.value = params.name
    showDetailAnalysis('销量趋势详情', timeData)
  }
}

const handleAmountChartClick = (params: any) => {
  showDetailAnalysis('销售额详情', params)
}

const handleModelChartClick = (params: any) => {
  const modelData = topModelsData.value.find(
    (item) => `${item.brandName} ${item.carModel}` === params.name,
  )
  if (modelData) {
    showDetailAnalysis('车型详情', modelData)
  }
}

const handleRegionChartClick = (params: any) => {
  router.push({
    name: 'CarPurchasesHeatMap',
    query: { region: params.name },
  })
}

const showKPIDetail = (type: string) => {
  switch (type) {
    case 'achievement':
      kpiDialogTitle.value = '目标完成率详情'
      currentKPIValue.value = achievementRate.value.toFixed(1)
      currentKPIUnit.value = '%'
      break
    case 'growth':
      kpiDialogTitle.value = '同比增长详情'
      currentKPIValue.value = yoyGrowth.value.toFixed(1)
      currentKPIUnit.value = '%'
      break
    case 'share':
      kpiDialogTitle.value = '市场份额详情'
      currentKPIValue.value = marketShare.value.toFixed(1)
      currentKPIUnit.value = '%'
      break
    case 'roi':
      kpiDialogTitle.value = '实际ROI详情'
      currentKPIValue.value = channelROI.value.toFixed(1)
      currentKPIUnit.value = ''
      break
  }
  showKPIDialog.value = true
}

const showDetailAnalysis = (title: string, data: any) => {
  detailDialogTitle.value = title

  if (title.includes('销量趋势')) {
    const timeData = data as SalesTrendItem
    detailTableData.value = [
      {
        name: '当期销量',
        value: `${timeData?.salesVolume.toLocaleString()} 台`,
        change: '',
        trend: '',
      },
      {
        name: '去年同期',
        value: `${timeData?.lastYearSalesVolume?.toLocaleString()} 台`,
        change: '',
        trend: '',
      },
      {
        name: '同比增长',
        value: `${timeData && timeData.lastYearSalesVolume ? (((timeData.salesVolume - timeData.lastYearSalesVolume) / timeData.lastYearSalesVolume) * 100).toFixed(1) : '0'}%`,
        change: '',
        trend: '',
      },
    ]
  }

  showDetailDialog.value = true
}

const handleDetailDialogClose = () => {
  showDetailDialog.value = false
  activeDetailTab.value = 'data'
}

const handleTargetDialogClose = (done: () => void) => {
  done()
}

const resetTargetForm = () => {
  targetForm.monthTarget = 0
  targetForm.quarterTarget = 0
  targetForm.yearTarget = 0
  targetForm.marketingInvestment = 0
  targetForm.industryAverageROI = 0
}

const toggleTrendComparison = () => {
  showComparison.value = !showComparison.value
  initSalesTrendChart()
}

const clearTimeSelection = () => {
  selectedTimePoint.value = ''
}

const clearRegionHighlight = () => {
  highlightedRegion.value = ''
}

const handleRankingTypeChange = () => {
  processTopModelsData()
  initTopModelsChart()
}

const handleTopNChange = () => {
  processTopModelsData()
  initTopModelsChart()
}

const handleTrendExport = (command: string) => {
  if (command === 'image') {
    const url = salesTrendChartInstance?.getDataURL({
      type: 'png',
      backgroundColor: '#fff',
    })
    if (url) {
      const link = document.createElement('a')
      link.href = url
      link.download = 'sales_trend.png'
      link.click()
    }
  } else if (command === 'excel') {
    ElMessage.info('Excel导出功能开发中...')
  }
}

const exportData = () => {
  ElMessage.info('数据导出功能开发中...')
}

const showForecast = () => {
  router.push({ name: 'SalesForecast' })
}

const showHeatMap = () => {
  router.push({ name: 'CarPurchasesHeatMap' })
}

// 窗口大小调整
const handleResize = () => {
  nextTick(() => {
    salesTrendChartInstance?.resize()
    salesAmountChartInstance?.resize()
    topModelsChartInstance?.resize()
    regionSalesChartInstance?.resize()
    comparisonChartInstance?.resize()
    forecastChartInstance?.resize()
    kpiTrendChartInstance?.resize()
  })
}

// =============================================
// 主数据刷新函数
// =============================================

const refreshData = async () => {
  loading.value = true
  try {
    // 1. 加载基础数据（如首次进入或点击刷新按钮时）

      await loadAllBaseData()

    // 2. 加载月度聚合数据
    await loadMonthlySummary()
    // 3. 处理所有业务数据
    await processAllData()
    // 4. 初始化所有图表
    await initAllCharts()
    ElMessage.success('数据刷新完成')
  } catch (error) {
    ElMessage.error('数据刷新失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 保存目标设置
const loadSavedTargets = async () => {
  const saved = localStorage.getItem('sales_targets')
  if (saved) {
    try {
      const parsed = JSON.parse(saved)
      Object.assign(targetForm, parsed)
    } catch (error) {
      console.warn('加载保存的目标设置失败:', error)
    }
  }
}

const saveTargets = () => {
  localStorage.setItem('sales_targets', JSON.stringify(targetForm))
  ElMessage.success('目标设置已保存')
}

// =============================================
// 生命周期
// =============================================

onMounted(async () => {
  ElMessage.success('欢迎使用销售总览页面！')
  await loadSavedTargets()
  await refreshData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)

  // 销毁图表实例
  if (salesTrendChartInstance) {
    salesTrendChartInstance.dispose()
    salesTrendChartInstance = null
  }
  if (salesAmountChartInstance) {
    salesAmountChartInstance.dispose()
    salesAmountChartInstance = null
  }
  if (topModelsChartInstance) {
    topModelsChartInstance.dispose()
    topModelsChartInstance = null
  }
  if (regionSalesChartInstance) {
    regionSalesChartInstance.dispose()
    regionSalesChartInstance = null
  }
  if (comparisonChartInstance) {
    comparisonChartInstance.dispose()
    comparisonChartInstance = null
  }
  if (forecastChartInstance) {
    forecastChartInstance.dispose()
    forecastChartInstance = null
  }
  if (kpiTrendChartInstance) {
    kpiTrendChartInstance.dispose()
    kpiTrendChartInstance = null
  }
})
</script>

<style scoped>
.sale-total-view {
  padding: 0;
  background: #f5f5f5;
}

/* 页面头部 */
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
  gap: 12px;
}

/* 业务预警 */
.alert-card {
  margin-bottom: 20px;
  border-left: 4px solid #f56c6c;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f56c6c;
  font-weight: 600;
}

.alerts-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.business-alert {
  border-radius: 6px;
}

.alert-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alert-content p {
  margin: 0;
  flex: 1;
}

/* 全局筛选器 */
.global-filter {
  margin-bottom: 20px;
  border-radius: 8px;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group label {
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
}

/* KPI卡片 */
.kpi-section {
  margin-bottom: 20px;
}

.kpi-card {
  border-radius: 12px;
  overflow: hidden;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.kpi-card.achievement {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.kpi-card.yoy-growth {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.kpi-card.market-share {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.kpi-card.channel-efficiency {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
}

.kpi-content {
  display: flex;
  align-items: center;
  padding: 20px;
}

.kpi-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
}

.kpi-details {
  flex: 1;
}

.kpi-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 4px;
}

.kpi-value.success {
  color: #67c23a;
}

.kpi-value.warning {
  color: #e6a23c;
}

.kpi-value.danger {
  color: #f56c6c;
}

.kpi-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 2px;
}

.kpi-trend {
  font-size: 12px;
  opacity: 0.8;
}

/* 图表区域 */
.charts-section {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 8px;
  margin-bottom: 20px;
  min-height: 500px;
  background: white;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.chart-actions {
  display: flex;
  gap: 8px;
}

.trend-analysis-summary,
.amount-analysis-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.analysis-detail {
  font-size: 12px;
  color: #909399;
}

.selected-time-info,
.region-highlight {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ranking-filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-container {
  height: 400px;
  width: 100%;
  min-height: 400px;
  background: white;
  border-radius: 4px;
}

/* 业务洞察面板 */
.insights-panel {
  margin-bottom: 20px;
  border-radius: 8px;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  font-weight: 600;
}

.insight-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: white;
  transition: all 0.3s ease;
}

.insight-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.insight-card.priority-high {
  border-left: 4px solid #f56c6c;
}

.insight-card.priority-medium {
  border-left: 4px solid #e6a23c;
}

.insight-card.priority-low {
  border-left: 4px solid #909399;
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.insight-title {
  font-weight: 600;
  flex: 1;
}

.icon-opportunity {
  color: #67c23a;
}

.icon-success {
  color: #409eff;
}

.icon-risk {
  color: #f56c6c;
}

.insight-content p {
  margin: 0 0 12px 0;
  color: #606266;
  line-height: 1.5;
}

.insight-actions {
  display: flex;
  justify-content: flex-end;
}

/* 详细分析弹窗 */
.detail-analysis-content {
  min-height: 400px;
}

.comparison-chart,
.forecast-chart {
  height: 300px;
  width: 100%;
}

.forecast-content p {
  margin-bottom: 16px;
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: center;
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .filter-group {
    flex-direction: column;
    align-items: stretch;
    gap: 4px;
  }

  .chart-container {
    height: 300px;
    min-height: 300px;
  }

  .chart-header {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .chart-actions {
    justify-content: center;
  }

  .kpi-content {
    padding: 16px;
  }

  .kpi-icon {
    width: 50px;
    height: 50px;
    font-size: 20px;
  }

  .kpi-value {
    font-size: 24px;
  }

  .trend-analysis-summary,
  .amount-analysis-summary {
    flex-direction: column;
    align-items: stretch;
    gap: 4px;
  }
}

@media (max-width: 480px) {
  .chart-container {
    height: 250px;
    min-height: 250px;
  }

  .kpi-content {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }

  .kpi-icon {
    margin-right: 0;
  }

  .insight-header {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
}

/* 新增样式 */
.target-setting-content {
  max-height: 70vh;
  overflow-y: auto;
}

.historical-reference {
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}

.ref-item {
  text-align: center;
  padding: 12px 8px;
  border-radius: 6px;
  background: white;
}

.ref-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.ref-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.dialog-footer {
  text-align: right;
}
</style>

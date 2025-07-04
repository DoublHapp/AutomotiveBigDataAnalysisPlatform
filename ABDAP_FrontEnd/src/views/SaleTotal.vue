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
          <el-button type="success" :icon="Download" @click="exportData">
            导出报告
          </el-button>
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
          <!-- 时间周期选择 -->
          <el-form-item label="目标周期:" prop="period">
            <el-radio-group v-model="targetForm.period" @change="handlePeriodChange">
              <el-radio-button value="month">月度目标</el-radio-button>
              <el-radio-button value="quarter">季度目标</el-radio-button>
              <el-radio-button value="year">年度目标</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <!-- 目标时间选择 -->
          <el-form-item label="目标时间:" prop="targetTime">
            <el-date-picker
              v-model="targetForm.targetTime"
              :type="targetForm.period === 'month' ? 'month' : targetForm.period === 'quarter' ? 'year' : 'year'"
              :placeholder="getTimePlaceholder()"
              :format="targetForm.period === 'month' ? 'YYYY-MM' : 'YYYY'"
              style="width: 200px;"
            />
            <el-select
              v-if="targetForm.period === 'quarter'"
              v-model="targetForm.quarter"
              placeholder="选择季度"
              style="width: 120px; margin-left: 12px;"
            >
              <el-option label="第1季度" value="Q1" />
              <el-option label="第2季度" value="Q2" />
              <el-option label="第3季度" value="Q3" />
              <el-option label="第4季度" value="Q4" />
            </el-select>
          </el-form-item>

          <!-- 销量目标设置 -->
          <el-form-item label="销量目标:" prop="salesTarget">
            <el-input-number
              v-model="targetForm.salesTarget"
              :min="1000"
              :max="1000000"
              :step="1000"
              :precision="0"
              style="width: 200px;"
            />
            <span style="margin-left: 8px; color: #909399;">台</span>
          </el-form-item>

          <!-- 营销投入设置 -->
          <el-form-item label="营销投入:" prop="marketingInvestment">
            <el-input-number
              v-model="targetForm.marketingInvestment"
              :min="100000"
              :max="100000000"
              :step="100000"
              :precision="0"
              style="width: 200px;"
            />
            <span style="margin-left: 8px; color: #909399;">元</span>
            <el-tag size="small" type="info" style="margin-left: 12px;">
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
              style="width: 200px;"
            />
            <el-tooltip content="请输入当前行业的平均ROI水平作为对比基准" placement="top">
              <el-icon style="margin-left: 8px; color: #909399;"><QuestionFilled /></el-icon>
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
            placeholder="选择车型"
            @change="handleGlobalFilterChange"
            clearable
          >
            <el-option label="全部车型" value="all" />
            <el-option
              v-for="model in availableCarModels"
              :key="model.carModelId"
              :label="`${model.brandName} ${model.modelName}`"
              :value="model.carModelId.toString()"
            />
          </el-select>
        </div>

        <div class="filter-group">
          <label>地区筛选:</label>
          <el-select v-model="globalFilters.region" @change="handleGlobalFilterChange" clearable>
            <el-option label="全国" value="all" />
            <el-option
              v-for="region in availableRegions"
              :key="region.id"
              :label="region.name"
              :value="region.id"
            />
          </el-select>
        </div>
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
              <div class="kpi-value" :class="achievementRate >= 100 ? 'success' : achievementRate >= 85 ? 'warning' : 'danger'">
                {{ achievementRate.toFixed(1) }}%
              </div>
              <div class="kpi-label">目标完成率</div>
              <div class="kpi-trend">
                实际: {{ actualSales.toLocaleString() }} / 目标: {{ targetSales.toLocaleString() }} 台
              </div>
              <div class="kpi-change" :class="achievementChangeType">
                {{ achievementChange >= 0 ? '+' : '' }}{{ achievementChange.toFixed(1) }}% vs 上月
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
                当年: {{ actualSales.toLocaleString() }} / 去年: {{ lastYearSales.toLocaleString() }} 台
              </div>
              <div class="kpi-benchmark">
                行业平均: {{ industryGrowth.toFixed(1) }}%
              </div>
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
              <div class="kpi-trend">
                行业总量: {{ totalMarketSales.toLocaleString() }} 台
              </div>
              <div class="kpi-rank">
                行业排名: 第{{ marketShareRank }}位
              </div>
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
              <div class="kpi-value" :class="channelROI >= 4 ? 'success' : channelROI >= 2 ? 'warning' : 'danger'">
                {{ channelROI.toFixed(1) }}
              </div>
              <div class="kpi-label">渠道ROI</div>
              <div class="kpi-trend">
                投入: {{ (channelInvestment / 10000).toFixed(0) }} 万
              </div>
              <div class="kpi-target">
                目标ROI: {{ targetForm.industryAverageROI }}%
              </div>
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
              <div class="summary-value">{{ (businessMetrics.averagePrice * actualSales / 10000).toFixed(0) }}</div>
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
                <el-button size="small" @click="toggleTrendComparison" :type="showComparison ? 'primary' : ''">
                  {{ showComparison ? '隐藏' : '显示' }}同比
                </el-button>
                <el-button size="small" @click="showForecast" type="success">
                  预测分析
                </el-button>
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
                <el-select v-model="modelRankingType" size="small" @change="handleRankingTypeChange">
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
    <el-dialog
      v-model="showKPIDialog"
      :title="kpiDialogTitle"
      width="60%"
    >
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
  QuestionFilled
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

//  基础数据存储
const baseData = ref<BaseData>({
  carModels: [],
  saleRecords: [],
  regions: [],
  topLevelRegions: [],
  nonTopLevelRegions: []
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
  industryGrowth: 0
})

// 目标设置表单
const targetForm = reactive({
  period: 'month' as 'month' | 'quarter' | 'year',
  targetTime: new Date(),
  quarter: 'Q1',
  salesTarget: 50000,
  marketingInvestment: 5000000,
  industryAverageROI: 3.5
})

// 表单验证规则
const targetRules = {
  period: [
    { required: true, message: '请选择目标周期', trigger: 'change' }
  ],
  targetTime: [
    { required: true, message: '请选择目标时间', trigger: 'change' }
  ],
  salesTarget: [
    { required: true, message: '请输入销量目标', trigger: 'blur' },
    { type: 'number', min: 1000, message: '销量目标不能少于1000台', trigger: 'blur' }
  ],
  marketingInvestment: [
    { required: true, message: '请输入营销投入', trigger: 'blur' },
    { type: 'number', min: 100000, message: '营销投入不能少于10万元', trigger: 'blur' }
  ],
  industryAverageROI: [
    { type: 'number', min: -100, max: 100, message: 'ROI应在-100%到100%之间', trigger: 'blur' }
  ]
}

// 全局筛选器
const globalFilters = reactive({
  timeRange: 'year',
  customDateRange: null as [Date, Date] | null,
  carModel: 'all',
  region: 'all'
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

// =============================================
// API 调用函数
// =============================================

const fetchCarModels = async (): Promise<CarModel[]> => {
  try {
    console.log(' 正在获取车型列表...')
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
      console.log(' 获取销售记录成功:', response.data.data.length, '条记录')
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

// =============================================
// 数据处理函数
// =============================================

const loadAllBaseData = async () => {
  try {
    console.log('开始加载基础数据...')
    
    const [carModels, saleRecords, regions, topLevelRegions, nonTopLevelRegions] = await Promise.all([
      fetchCarModels(),
      fetchSaleRecords(),
      fetchRegions(),
      fetchTopLevelRegions(),
      fetchNonTopLevelRegions()
    ])
    
    baseData.value = {
      carModels,
      saleRecords,
      regions,
      topLevelRegions,
      nonTopLevelRegions
    }
    
    console.log(' 基础数据加载完成:', {
      车型数量: carModels.length,
      销售记录数量: saleRecords.length,
      地区数量: regions.length,
      省份数量: topLevelRegions.length,
      城市数量: nonTopLevelRegions.length
    })
    
    ElMessage.success('基础数据加载完成')
    
  } catch (error) {
    console.error(' 基础数据加载失败:', error)
    ElMessage.error('数据加载失败，请检查网络连接')
    throw error
  }
}

// 处理销量趋势数据
const processSalesTrendData = () => {
  console.log('处理销量趋势数据...')
  
  const trendMap = new Map<string, number>()
  const lastYearTrendMap = new Map<string, number>()
  
  const currentYear = new Date().getFullYear()
  
  //  添加调试日志，查看实际的销售记录
  console.log('当前年份:', currentYear)
  console.log('全局筛选条件:', globalFilters)
  console.log('销售记录样本:', baseData.value.saleRecords.slice(0, 3))
  
  baseData.value.saleRecords.forEach(record => {
    const saleDate = new Date(record.saleMonth)
    const year = saleDate.getFullYear()
    const monthKey = saleDate.toISOString().slice(0, 7) // "2024-01"
    
    console.log(`处理记录: ${record.saleMonth}, 解析年份: ${year}, 月份键: ${monthKey}`)
    
    //  修改时间范围，包含更多年份的数据
    if (year >= currentYear - 2) { // 包含近2年的数据
      const currentCount = trendMap.get(monthKey) || 0
      trendMap.set(monthKey, currentCount + record.saleCount)
      
      // 如果是去年的数据，也加入去年地图
      if (year === currentYear - 1) {
        const lastYearMonthKey = `${currentYear}-${String(saleDate.getMonth() + 1).padStart(2, '0')}`
        const lastYearCount = lastYearTrendMap.get(lastYearMonthKey) || 0
        lastYearTrendMap.set(lastYearMonthKey, lastYearCount + record.saleCount)
      }
    }
  })
  
  //  如果当年数据为空，使用所有可用数据
  if (trendMap.size === 0) {
    console.log(' 当年数据为空，使用所有可用数据...')
    baseData.value.saleRecords.forEach(record => {
      const monthKey = record.saleMonth.slice(0, 7)
      const currentCount = trendMap.get(monthKey) || 0
      trendMap.set(monthKey, currentCount + record.saleCount)
    })
  }
  
  console.log('处理后的月度数据:', Object.fromEntries(trendMap))
  
  //  根据全局筛选器生成时间范围
  let dateList: string[] = []
  
  if (globalFilters.timeRange === 'custom' && globalFilters.customDateRange) {
    // 使用自定义时间范围
    const [startDate, endDate] = globalFilters.customDateRange
    const start = new Date(startDate)
    const end = new Date(endDate)
    
    console.log('使用自定义时间范围:', start, end)
    
    let current = new Date(start.getFullYear(), start.getMonth(), 1)
    const endMonth = new Date(end.getFullYear(), end.getMonth(), 1)
    
    while (current <= endMonth) {
      const monthKey = current.toISOString().slice(0, 7)
      dateList.push(monthKey)
      current.setMonth(current.getMonth() + 1)
    }
  } else if (globalFilters.timeRange === 'all') {
    // 使用所有可用数据的时间范围
    const allMonths = Array.from(trendMap.keys()).sort()
    if (allMonths.length > 0) {
      dateList = allMonths
    }
  } else {
    // 使用固定时间范围（近一月、近一季、近一年）
    const currentDate = new Date()
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
    }
    
    for (let i = monthsBack - 1; i >= 0; i--) {
      const date = new Date(currentDate.getFullYear(), currentDate.getMonth() - i, 1)
      const monthKey = date.toISOString().slice(0, 7)
      dateList.push(monthKey)
    }
  }
  
  console.log('生成的时间列表:', dateList)
  
  // 生成趋势数据
  const trendData: SalesTrendItem[] = []
  
  dateList.forEach(monthKey => {
    const salesVolume = trendMap.get(monthKey) || 0
    const lastYearSalesVolume = lastYearTrendMap.get(monthKey) || 0
    const date = new Date(monthKey + '-01')
    
    trendData.push({
      date: monthKey,
      salesVolume: salesVolume,
      lastYearSalesVolume: lastYearSalesVolume,
      month: date.getMonth() + 1,
      year: date.getFullYear()
    })
  })
  
  salesTrendData.value = trendData
  
  // 计算实际销量总和和去年销量总和
  businessMetrics.value.actualSales = trendData.reduce((sum, item) => sum + item.salesVolume, 0)
  businessMetrics.value.lastYearSales = trendData.reduce((sum, item) => sum + (item.lastYearSalesVolume || 0), 0)
  
  console.log(' 销量趋势处理完成，实际销量:', businessMetrics.value.actualSales, '去年销量:', businessMetrics.value.lastYearSales)
}

// 处理销售额数据
const processSalesAmountData = () => {
  console.log(' 处理销售额数据...')
  
  const amountMap = new Map<string, number>()
  
  baseData.value.saleRecords.forEach(record => {
    const monthKey = record.saleMonth.slice(0, 7)
    const currentAmount = amountMap.get(monthKey) || 0
    // 销售额直接从API获取，转换为万元
    const monthlyAmount = record.saleAmount / 10000
    amountMap.set(monthKey, currentAmount + monthlyAmount)
  })
  
  // 使用与销量趋势相同的时间范围
  let dateList: string[] = []
  
  if (globalFilters.timeRange === 'custom' && globalFilters.customDateRange) {
    // 使用自定义时间范围
    const [startDate, endDate] = globalFilters.customDateRange
    const start = new Date(startDate)
    const end = new Date(endDate)
    
    let current = new Date(start.getFullYear(), start.getMonth(), 1)
    const endMonth = new Date(end.getFullYear(), end.getMonth(), 1)
    
    while (current <= endMonth) {
      const monthKey = current.toISOString().slice(0, 7)
      dateList.push(monthKey)
      current.setMonth(current.getMonth() + 1)
    }
  } else if (globalFilters.timeRange === 'all') {
    // 使用所有可用数据的时间范围
    const allMonths = Array.from(amountMap.keys()).sort()
    if (allMonths.length > 0) {
      dateList = allMonths
    }
  } else {
    // 使用固定时间范围
    const currentDate = new Date()
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
    }
    
    for (let i = monthsBack - 1; i >= 0; i--) {
      const date = new Date(currentDate.getFullYear(), currentDate.getMonth() - i, 1)
      const monthKey = date.toISOString().slice(0, 7)
      dateList.push(monthKey)
    }
  }
  
  const amountData: SalesAmountItem[] = []
  
  dateList.forEach(monthKey => {
    const salesAmount = amountMap.get(monthKey) || 0
    const date = new Date(monthKey + '-01')
    
    amountData.push({
      date: monthKey,
      salesAmount: Math.round(salesAmount * 100) / 100, // 保留2位小数
      month: date.getMonth() + 1,
      year: date.getFullYear()
    })
  })
  
  salesAmountData.value = amountData
  console.log(' 销售额数据处理完成')
}
// 处理车型排行数据
const processTopModelsData = () => {
  console.log(' 处理车型排行数据...')
  
  const modelSalesMap = new Map<number, {
    carModel: string
    brandName: string
    salesVolume: number
    salesAmount: number
    lastYearSalesVolume: number
  }>()
  
  const currentYear = new Date().getFullYear()
  
  //  根据全局筛选器过滤销售记录
  let filteredRecords = baseData.value.saleRecords
  
  // 时间筛选
  if (globalFilters.timeRange === 'custom' && globalFilters.customDateRange) {
    const [startDate, endDate] = globalFilters.customDateRange
    filteredRecords = filteredRecords.filter(record => {
      const recordDate = new Date(record.saleMonth)
      return recordDate >= startDate && recordDate <= endDate
    })
  } else if (globalFilters.timeRange !== 'all') {
    const currentDate = new Date()
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
    }
    
    const startDate = new Date(currentDate.getFullYear(), currentDate.getMonth() - monthsBack, 1)
    filteredRecords = filteredRecords.filter(record => {
      const recordDate = new Date(record.saleMonth)
      return recordDate >= startDate
    })
  }
  
  // 车型筛选
  if (globalFilters.carModel !== 'all') {
    const selectedModelId = parseInt(globalFilters.carModel)
    filteredRecords = filteredRecords.filter(record => record.carModelId === selectedModelId)
  }
  
  // 地区筛选
  if (globalFilters.region !== 'all') {
    const selectedRegionId = globalFilters.region
    filteredRecords = filteredRecords.filter(record => record.regionId.toString() === selectedRegionId.toString())
  }
  
  console.log('车型排行筛选后记录数:', filteredRecords.length)
  
  filteredRecords.forEach(record => {
    const carModel = baseData.value.carModels.find(model => model.carModelId === record.carModelId)
    if (!carModel) return
    
    const saleDate = new Date(record.saleMonth)
    const year = saleDate.getFullYear()
    
    if (!modelSalesMap.has(record.carModelId)) {
      modelSalesMap.set(record.carModelId, {
        carModel: carModel.modelName,
        brandName: carModel.brandName,
        salesVolume: 0,
        salesAmount: 0,
        lastYearSalesVolume: 0
      })
    }
    
    const existing = modelSalesMap.get(record.carModelId)!
    
    // 修改逻辑：如果是自定义时间范围，则不区分当年/去年
    if (globalFilters.timeRange === 'custom' || globalFilters.timeRange === 'all') {
      // 自定义时间范围或全部时间，统一计算到当期
      existing.salesVolume += record.saleCount
      existing.salesAmount += record.saleAmount
    } else {
      // 固定时间范围，区分当年和去年
      if (year === currentYear) {
        existing.salesVolume += record.saleCount
        existing.salesAmount += record.saleAmount
      } else if (year === currentYear - 1) {
        existing.lastYearSalesVolume += record.saleCount
      }
    }
  })
  
  // 转换为数组并计算增长率
  const modelsArray = Array.from(modelSalesMap.values()).map(model => ({
    ...model,
    growthRate: model.lastYearSalesVolume > 0 
      ? ((model.salesVolume - model.lastYearSalesVolume) / model.lastYearSalesVolume) * 100 
      : (model.salesVolume > 0 ? 100 : 0),
    marketShare: businessMetrics.value.actualSales > 0 
      ? (model.salesVolume / businessMetrics.value.actualSales * 100) 
      : 0,
    profit: Math.floor(model.salesAmount * 0.15) // 假设15%利润率
  }))
  
  // 根据排序类型排序
  if (modelRankingType.value === 'sales') {
    modelsArray.sort((a, b) => b.salesVolume - a.salesVolume)
  } else if (modelRankingType.value === 'growth') {
    modelsArray.sort((a, b) => b.growthRate - a.growthRate)
  } else if (modelRankingType.value === 'share') {
    modelsArray.sort((a, b) => b.marketShare - a.marketShare)
  }
  
  topModelsData.value = modelsArray.slice(0, topN.value)
  console.log('车型排行处理完成，前', topN.value, '名车型, 筛选后数据量:', modelsArray.length)
}

// 处理地区销量数据
const processRegionSalesData = () => {
  console.log(' 处理地区销量数据...')
  
  const regionSalesMap = new Map<number, {
    regionName: string
    salesVolume: number
    salesAmount: number
  }>()
  
  //  根据全局筛选器过滤销售记录
  let filteredRecords = baseData.value.saleRecords
  
  // 时间筛选
  if (globalFilters.timeRange === 'custom' && globalFilters.customDateRange) {
    const [startDate, endDate] = globalFilters.customDateRange
    filteredRecords = filteredRecords.filter(record => {
      const recordDate = new Date(record.saleMonth)
      return recordDate >= startDate && recordDate <= endDate
    })
  } else if (globalFilters.timeRange !== 'all') {
    const currentDate = new Date()
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
    }
    
    const startDate = new Date(currentDate.getFullYear(), currentDate.getMonth() - monthsBack, 1)
    filteredRecords = filteredRecords.filter(record => {
      const recordDate = new Date(record.saleMonth)
      return recordDate >= startDate
    })
  }
  
  // 车型筛选
  if (globalFilters.carModel !== 'all') {
    const selectedModelId = parseInt(globalFilters.carModel)
    filteredRecords = filteredRecords.filter(record => record.carModelId === selectedModelId)
  }
  
  // 注意：地区销量分布通常不需要根据地区筛选器再次过滤
  // 因为我们就是要看各个地区的分布情况
  // 除非用户选择了特定地区，那我们只显示该地区的数据
  if (globalFilters.region !== 'all') {
    const selectedRegionId = globalFilters.region
    filteredRecords = filteredRecords.filter(record => record.regionId.toString() === selectedRegionId.toString())
  }
  
  console.log('地区销量筛选后记录数:', filteredRecords.length)
  
  filteredRecords.forEach(record => {
    if (regionSalesMap.has(record.regionId)) {
      const existing = regionSalesMap.get(record.regionId)!
      existing.salesVolume += record.saleCount
      existing.salesAmount += record.saleAmount
    } else {
      regionSalesMap.set(record.regionId, {
        regionName: record.regionName,
        salesVolume: record.saleCount,
        salesAmount: record.saleAmount
      })
    }
  })
  
  // 转换为数组并排序
  const regionsArray = Array.from(regionSalesMap.values())
  regionsArray.sort((a, b) => b.salesVolume - a.salesVolume)
  
  regionSalesData.value = regionsArray.map(region => ({
    region: region.regionName,
    salesVolume: region.salesVolume,
    salesAmount: Math.round(region.salesAmount / 10000 * 100) / 100, // 转换为万元
    marketShare: businessMetrics.value.actualSales > 0 
      ? (region.salesVolume / businessMetrics.value.actualSales * 100) 
      : 0
  }))
  
  // 更新可用地区列表（基于筛选后的数据）
  availableRegions.value = regionsArray.map((region, index) => ({
    id: index + 1,
    name: region.regionName
  }))
  
  console.log(' 地区销量处理完成，覆盖', regionsArray.length, '个地区, 筛选后数据')}

// 计算业务指标
const calculateBusinessMetrics = () => {
 console.log('计算业务指标...')
  
  // 使用与趋势数据相同的筛选逻辑
  let filteredRecords = baseData.value.saleRecords
  
  // 应用时间筛选
  if (globalFilters.timeRange === 'custom' && globalFilters.customDateRange) {
    const [startDate, endDate] = globalFilters.customDateRange
    filteredRecords = filteredRecords.filter(record => {
      const recordDate = new Date(record.saleMonth)
      return recordDate >= startDate && recordDate <= endDate
    })
  } else if (globalFilters.timeRange !== 'all') {
    const currentDate = new Date()
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
    }
    
    const startDate = new Date(currentDate.getFullYear(), currentDate.getMonth() - monthsBack, 1)
    filteredRecords = filteredRecords.filter(record => {
      const recordDate = new Date(record.saleMonth)
      return recordDate >= startDate
    })
  }
  
  // 应用车型筛选
  if (globalFilters.carModel !== 'all') {
    const selectedModelId = parseInt(globalFilters.carModel)
    filteredRecords = filteredRecords.filter(record => record.carModelId === selectedModelId)
  }
  
  // 应用地区筛选
  if (globalFilters.region !== 'all') {
    const selectedRegionId = globalFilters.region
    filteredRecords = filteredRecords.filter(record => record.regionId.toString() === selectedRegionId.toString())
  }
  
  console.log('业务指标计算筛选后记录数:', filteredRecords.length)
  
  // 1. 基于筛选后的数据计算平均价格
  const totalAmount = filteredRecords.reduce((sum, record) => sum + record.saleAmount, 0)
  const totalSalesCount = filteredRecords.reduce((sum, record) => sum + record.saleCount, 0)
  businessMetrics.value.averagePrice = totalSalesCount > 0 ? totalAmount / totalSalesCount : 0
  
  // 2. 计算行业总销量（基于筛选后数据估算整个市场）
  businessMetrics.value.totalMarketSales = businessMetrics.value.actualSales * 3 // 假设占市场33%
  
  // 3. 计算市场份额
  businessMetrics.value.marketShare = businessMetrics.value.totalMarketSales > 0 
    ? (businessMetrics.value.actualSales / businessMetrics.value.totalMarketSales * 100) 
    : 0
  
  // 4. 计算目标完成率
  businessMetrics.value.achievementRate = targetForm.salesTarget > 0 
    ? (businessMetrics.value.actualSales / targetForm.salesTarget * 100) 
    : 0
  
  // 5. 计算同比增长
  businessMetrics.value.yoyGrowth = businessMetrics.value.lastYearSales > 0 
    ? ((businessMetrics.value.actualSales - businessMetrics.value.lastYearSales) / businessMetrics.value.lastYearSales * 100) 
    : (businessMetrics.value.actualSales > 0 ? 100 : 0)
  
  // 6. 计算总ROI
  businessMetrics.value.totalROI = targetForm.marketingInvestment > 0 
    ? ((businessMetrics.value.actualSales * businessMetrics.value.averagePrice - targetForm.marketingInvestment) / targetForm.marketingInvestment * 100) 
    : 0
  
  // 7. 计算行业增长率（基于数据估算）
  businessMetrics.value.industryGrowth = businessMetrics.value.yoyGrowth * 0.8 // 假设我们略优于行业平均
  
  // 更新可用车型列表（基于原始数据，不筛选）
  availableCarModels.value = baseData.value.carModels
  
  console.log(' 业务指标计算完成:', businessMetrics.value)
}

// 处理所有数据
const processAllData = () => {
  try {
    console.log('开始处理所有数据...')
    
    if (baseData.value.saleRecords.length === 0) {
      ElMessage.warning('销售记录为空，无法生成报表')
      return
    }
    
    processSalesTrendData()
    processSalesAmountData()
    processTopModelsData()
    processRegionSalesData()
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

const targetSales = computed(() => targetForm.salesTarget)
const actualSales = computed(() => businessMetrics.value.actualSales)
const lastYearSales = computed(() => businessMetrics.value.lastYearSales)
const totalMarketSales = computed(() => businessMetrics.value.totalMarketSales)
const achievementRate = computed(() => businessMetrics.value.achievementRate)
const yoyGrowth = computed(() => businessMetrics.value.yoyGrowth)
const marketShare = computed(() => businessMetrics.value.marketShare)
const channelROI = computed(() => businessMetrics.value.totalROI)
const industryGrowth = computed(() => businessMetrics.value.industryGrowth)

// 其他计算属性
const achievementChange = ref(2.5)
const marketShareRank = ref(3)
const channelInvestment = computed(() => targetForm.marketingInvestment)
const currentKPIValue = ref('')
const currentKPIUnit = ref('')

const achievementChangeType = computed(() => {
  if (achievementChange.value >= 5) return 'success'
  if (achievementChange.value >= 0) return 'warning'
  return 'danger'
})

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
      detail: `近期销量增长${trend}台，增长趋势明显，建议加大市场投入`
    }
  } else if (trend < -500) {
    return {
      type: 'danger',
      summary: '下滑风险',
      detail: `近期销量下降${Math.abs(trend)}台，需要关注市场变化并制定应对策略`
    }
  } else {
    return {
      type: 'warning',
      summary: '平稳运行',
      detail: `销量波动在正常范围内`
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
      detail: `销售额呈上升趋势，较两月前增长${trend.toFixed(1)}万元`
    }
  } else if (trend < -1000) {
    return {
      type: 'danger',
      summary: '营收下滑',
      detail: `销售额下降${Math.abs(trend).toFixed(1)}万元，需要关注产品结构和定价策略`
    }
  } else {
    return {
      type: 'warning',
      summary: '平稳发展',
      detail: '销售额变化平缓，建议关注市场动态'
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
      message: `当前完成率${achievementRate.value.toFixed(1)}%，距离目标差距较大`
    })
  }
  
  if (yoyGrowth.value < -10) {
    alerts.push({
      id: 2,
      type: 'warning',
      title: '同比增长预警',
      message: `同比下降${Math.abs(yoyGrowth.value).toFixed(1)}%，需要制定增长策略`
    })
  }
  
  if (channelROI.value < 2) {
    alerts.push({
      id: 3,
      type: 'warning',
      title: 'ROI偏低提醒',
      message: `渠道ROI仅${channelROI.value.toFixed(1)}，低于行业标准`
    })
  }
  
  return alerts
})

// =============================================
// 工具函数
// =============================================

const getTimePlaceholder = () => {
  switch (targetForm.period) {
    case 'month': return '选择目标月份'
    case 'quarter': return '选择目标年份'
    case 'year': return '选择目标年份'
    default: return '请选择时间'
  }
}

const handlePeriodChange = () => {
  targetForm.targetTime = new Date()
  if (targetForm.period === 'quarter') {
    targetForm.quarter = 'Q1'
  }
}

const handleGlobalFilterChange = () => {
  console.log('全局筛选条件变更:', globalFilters)
  // 可以在这里添加筛选逻辑重新处理数据
  processAllData()
  initAllCharts()
}

// =============================================
// 图表初始化函数
// =============================================

const initSalesTrendChart = async () => {
  if (!salesTrendChart.value || salesTrendData.value.length === 0) return
  
  const data = salesTrendData.value
  
  if (salesTrendChartInstance) {
    salesTrendChartInstance.dispose()
  }
  
  salesTrendChartInstance = echarts.init(salesTrendChart.value)
  
  const option = {
    title: {
      text: '销量趋势分析',
      textStyle: { fontSize: 14, color: '#333' }
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
      }
    },
    legend: {
      data: showComparison.value ? ['当期销量', '去年同期'] : ['当期销量'],
      top: 30
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '8%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.date),
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '销量(台)',
      axisLabel: {
        formatter: (value: number) => value.toLocaleString()
      }
    },
    series: [
      {
        name: '当期销量',
        type: 'line',
        data: data.map(item => item.salesVolume),
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 3, color: '#409EFF' },
        areaStyle: { color: 'rgba(64, 158, 255, 0.1)' }
      },
      ...(showComparison.value ? [{
        name: '去年同期',
        type: 'line',
        data: data.map(item => item.lastYearSalesVolume || 0),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#909399', type: 'dashed' }
      }] : [])
    ]
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
      textStyle: { fontSize: 14, color: '#333' }
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        return `${data.axisValue}<br/>销售额: ${data.value.toFixed(1)} 万元`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '8%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.date),
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '销售额(万元)',
      axisLabel: {
        formatter: (value: number) => value.toFixed(0)
      }
    },
    series: [{
      name: '销售额',
      type: 'bar',
      data: data.map(item => item.salesAmount),
      itemStyle: {
        color: '#67C23A',
        borderRadius: [4, 4, 0, 0]
      }
    }]
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
      textStyle: { fontSize: 14, color: '#333' }
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
      }
    },
    grid: {
      left: '5%',
      right: '15%',
      bottom: '8%',
      top: 50,
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: modelRankingType.value === 'sales' ? '销量(台)' :
            modelRankingType.value === 'growth' ? '增长率(%)' : '市场份额(%)',
      position: 'top',
      axisLabel: {
        formatter: (value: number) => {
          if (modelRankingType.value === 'sales') {
            return value.toLocaleString()
          } else {
            return value.toFixed(1) + '%'
          }
        }
      }
    },
    yAxis: {
      type: 'category',
      data: data.map(item => `${item.brandName} ${item.carModel}`),
      axisLabel: { fontSize: 10 }
    },
    series: [{
      name: '排行',
      type: 'bar',
      data: data.map((item, index) => ({
        value: modelRankingType.value === 'sales' ? item.salesVolume :
               modelRankingType.value === 'growth' ? item.growthRate : item.marketShare,
        itemStyle: {
          color: colorList[index % colorList.length]
        }
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
        }
      }
    }]
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
      textStyle: { fontSize: 14, color: '#333' }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const data = params[0]
        return `${data.axisValue}<br/>销量: ${data.value.toLocaleString()} 台`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '20%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.region),
      axisLabel: { interval: 0, rotate: 45, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '销量(台)',
      axisLabel: {
        formatter: (value: number) => value.toLocaleString()
      }
    },
    series: [{
      name: '销量',
      type: 'bar',
      data: data.map(item => item.salesVolume),
      itemStyle: {
        color: '#F56C6C',
        borderRadius: [4, 4, 0, 0]
      }
    }]
  }
  
  regionSalesChartInstance.setOption(option)
}

const initAllCharts = async () => {
  await nextTick()
  await Promise.all([
    initSalesTrendChart(),
    initSalesAmountChart(),
    initTopModelsChart(),
    initRegionSalesChart()
  ])
}

// =============================================
// 🎯 事件处理函数
// =============================================

const handleTrendChartClick = (params: any) => {
  const timeData = salesTrendData.value.find(item => item.date === params.name)
  if (timeData) {
    selectedTimePoint.value = params.name
    showDetailAnalysis('销量趋势详情', timeData)
  }
}

const handleAmountChartClick = (params: any) => {
  showDetailAnalysis('销售额详情', params)
}

const handleModelChartClick = (params: any) => {
  const modelData = topModelsData.value.find(item =>
    `${item.brandName} ${item.carModel}` === params.name)
  if (modelData) {
    showDetailAnalysis('车型详情', modelData)
  }
}

const handleRegionChartClick = (params: any) => {
  router.push({
    name: 'CarPurchasesHeatMap',
    query: { region: params.name }
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
      kpiDialogTitle.value = '渠道ROI详情'
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
      { name: '当期销量', value: `${timeData?.salesVolume.toLocaleString()} 台`, change: '', trend: '' },
      { name: '去年同期', value: `${timeData?.lastYearSalesVolume?.toLocaleString()} 台`, change: '', trend: '' },
      { name: '同比增长', value: `${timeData && timeData.lastYearSalesVolume ? ((timeData.salesVolume - timeData.lastYearSalesVolume) / timeData.lastYearSalesVolume * 100).toFixed(1) : '0'}%`, change: '', trend: '' }
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
  targetForm.period = 'month'
  targetForm.targetTime = new Date()
  targetForm.quarter = 'Q1'
  targetForm.salesTarget = 50000
  targetForm.marketingInvestment = 5000000
  targetForm.industryAverageROI = 3.5
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
      backgroundColor: '#fff'
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
// 🔄 主数据刷新函数
// =============================================

const refreshData = async () => {
  loading.value = true
  try {
    await loadAllBaseData()
    processAllData()
    await initAllCharts()
    ElMessage.success('数据刷新完成')
  } catch (error) {
    console.error('❌ 数据刷新失败:', error)
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
// 🔄 生命周期
// =============================================

onMounted(async () => {
  ElMessage.success('欢迎使用销售总览页面！')
  
  // 加载已保存的目标设置
  await loadSavedTargets()
  
  // 加载数据并初始化图表
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

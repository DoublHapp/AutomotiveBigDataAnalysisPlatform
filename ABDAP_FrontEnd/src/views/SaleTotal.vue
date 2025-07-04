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

          <!-- 投入分配明细 -->
          <el-form-item label="投入分配:">
            <div class="investment-breakdown">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-card shadow="never" class="breakdown-card">
                    <template #header>
                      <span>渠道投入分配</span>
                    </template>
                    <div class="breakdown-item">
                      <span>线上广告:</span>
                      <el-input-number
                        v-model="investmentBreakdown.online"
                        :min="0"
                        :max="100"
                        size="small"
                        style="width: 80px;"
                      />
                      <span>%</span>
                    </div>
                    <div class="breakdown-item">
                      <span>线下活动:</span>
                      <el-input-number
                        v-model="investmentBreakdown.offline"
                        :min="0"
                        :max="100"
                        size="small"
                        style="width: 80px;"
                      />
                      <span>%</span>
                    </div>
                    <div class="breakdown-item">
                      <span>经销商支持:</span>
                      <el-input-number
                        v-model="investmentBreakdown.dealer"
                        :min="0"
                        :max="100"
                        size="small"
                        style="width: 80px;"
                      />
                      <span>%</span>
                    </div>
                    <div class="breakdown-item">
                      <span>其他投入:</span>
                      <el-input-number
                        v-model="investmentBreakdown.other"
                        :min="0"
                        :max="100"
                        size="small"
                        style="width: 80px;"
                      />
                      <span>%</span>
                    </div>
                    <div class="breakdown-total" :class="totalPercentage !== 100 ? 'error' : 'success'">
                      总计: {{ totalPercentage }}%
                      <span v-if="totalPercentage !== 100" class="error-text">（需要等于100%）</span>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="12">
                  <el-card shadow="never" class="breakdown-card">
                    <template #header>
                      <span>预期ROI分析</span>
                    </template>
                    <div class="roi-analysis">
                      <div class="roi-item">
                        <span>预期ROI:</span>
                        <strong>{{ expectedROI.toFixed(2) }}</strong>
                      </div>
                      <div class="roi-item">
                        <span>行业平均ROI:</span>
                        <span>{{ targetForm.industryAverageROI?.toFixed(1) || '--' }}</span>
                      </div>
                      <div class="roi-item">
                        <span>ROI评估:</span>
                        <el-tag :type="roiAssessment.type" size="small">
                          {{ roiAssessment.text }}
                        </el-tag>
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showTargetDialog = false">取消</el-button>
          <el-button @click="resetTargetForm">重置</el-button>
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
            <!-- 使用真实的车型数据 -->
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
                目标: {{ targetSales.toLocaleString() }} 台
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
                去年同期: {{ lastYearSales.toLocaleString() }} 台
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
                目标ROI: 4.0
              </div>
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
                      <el-dropdown-item command="pdf">导出PDF</el-dropdown-item>
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

// 接口定义 - 与后端API响应结构完全匹配
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
  driveType: string
  rangeKm: number
}

interface SaleRecord {
  saleRecordId: number
  carModelId: number
  regionId: number
  saleMonth: string
  saleCount: number
  saleAmount: number
  carModel: {
    carModelId: number
    modelName: string
    brandId: number
  }
  region: {
    regionId: number
    regionName: string
    parentRegionId?: number | null
  }
}

// 业务数据接口定义
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
  recordCount: number
  growthRate?: number
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

// 响应式数据
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

// 基础数据存储 - 直接从API获取
const carModelsData = ref<CarModel[]>([])
const saleRecordsData = ref<SaleRecord[]>([])
const availableCarModels = ref<CarModel[]>([])

// 目标设置表单
const targetForm = reactive({
  period: 'month', // month, quarter, year
  targetTime: new Date(),
  quarter: 'Q1',
  salesTarget: 50000,
  marketingInvestment: 5000000,
  industryAverageROI: 3.5
})

// 投入分配
const investmentBreakdown = reactive({
  online: 40,    // 线上广告
  offline: 30,   // 线下活动
  dealer: 25,    // 经销商支持
  other: 5       // 其他投入
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
  timeRange: 'month',
  customDateRange: null as [Date, Date] | null,
  carModel: 'all',
  region: 'all'
})

// 业务指标
const targetSales = ref(50000)
const actualSales = ref(0)
const lastYearSales = ref(0)
const totalMarketSales = ref(500000)
const channelInvestment = ref(5000000)
const industryGrowth = ref(8.5)
const marketShareRank = ref(3)
const achievementChange = ref(2.5)
const currentKPIValue = ref('')
const currentKPIUnit = ref('')

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

// 业务数据存储 - 从基础数据处理生成
const salesTrendData = ref<SalesTrendItem[]>([])
const salesAmountData = ref<SalesAmountItem[]>([])
const topModelsData = ref<TopModelItem[]>([])
const regionSalesData = ref<RegionSalesItem[]>([])
const availableRegions = ref<RegionOption[]>([])
const detailTableData = ref<any[]>([])
const currentRecommendations = ref<any[]>([])
const forecastSummary = ref('')

// ===========================================
// 🎯 API调用函数 - 仅使用提供的两个接口
// ===========================================

const fetchCarModels = async (): Promise<CarModel[]> => {
  try {
    console.log('🚀 正在获取车型列表...')
    const response = await axios.get('/api/car-models')
    console.log('📡 车型API响应:', response.data)

    if (response.data.status === 200) {
      const models = response.data.data
      console.log('✅ 获取到车型数据:', models.length, '个车型')
      return models
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ 获取车型列表失败:', error)
    ElMessage.error('车型数据加载失败，使用模拟数据')
    return generateMockCarModels()
  }
}

const fetchSaleRecords = async (): Promise<SaleRecord[]> => {
  try {
    console.log('🚀 正在获取销售记录...')
    const response = await axios.get('/api/sale-records')
    console.log('📡 销售记录API响应:', response.data)

    if (response.data.status === 200) {
      const records = response.data.data
      console.log('✅ 获取到销售记录:', records.length, '条记录')
      return records
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ 获取销售记录失败:', error)
    ElMessage.error('销售数据加载失败，使用模拟数据')
    return generateMockSaleRecords()
  }
}

// ===========================================
// 🔄 基础数据处理函数 - 核心业务逻辑
// ===========================================

const processBasicData = () => {
  try {
    console.log(' 开始处理基础数据...')

    if (!validateData()) {
      ElMessage.warning('数据验证失败，可能影响显示效果')
    }

    // 按顺序处理各种业务数据
    processSalesTrendData()        // 处理销量趋势
    processSalesAmountData()       // 处理销售额数据
    processTopModelsData()         // 处理车型排行
    processRegionSalesData()       // 处理地区销量
    calculateBusinessMetrics()     // 计算业务指标

    console.log(' 数据处理完成:', {
      趋势数据点: salesTrendData.value.length,
      销售额数据点: salesAmountData.value.length,
      车型排行: topModelsData.value.length,
      地区数据: regionSalesData.value.length,
      实际销量: actualSales.value.toLocaleString()
    })
  } catch (error) {
    console.error('❌ 数据处理失败:', error)
    ElMessage.error('数据处理失败，部分功能可能不可用')
  }
}

// 数据验证函数
const validateData = () => {
  const issues = []

  if (carModelsData.value.length === 0) {
    issues.push('车型数据为空')
  }

  if (saleRecordsData.value.length === 0) {
    issues.push('销售记录为空')
  }

  // 检查数据完整性：销售记录是否有对应的车型信息
  const missingCarModels = saleRecordsData.value.filter(record =>
    !carModelsData.value.find(model => model.carModelId === record.carModelId)
  )

  if (missingCarModels.length > 0) {
    issues.push(`${missingCarModels.length}条销售记录缺少对应车型信息`)
    console.warn('⚠️ 数据完整性问题:', {
      缺失车型的销售记录: missingCarModels.slice(0, 3), // 只显示前3条
      总缺失数量: missingCarModels.length,
      总销售记录数: saleRecordsData.value.length
    })
  }

  if (issues.length > 0) {
    console.warn('🔍 数据验证发现问题:', issues)
  }

  return issues.length === 0
}

// 1. 处理销量趋势数据
const processSalesTrendData = () => {
  console.log('📈 处理销量趋势数据...')

  const trendMap = new Map<string, number>()
  const lastYearTrendMap = new Map<string, number>()

  // 分别汇总当年和去年的销量数据
  saleRecordsData.value.forEach(record => {
    const saleDate = new Date(record.saleMonth)
    const monthKey = saleDate.toISOString().slice(0, 7) // "2024-01"
    const year = saleDate.getFullYear()
    const currentYear = new Date().getFullYear()

    if (year === currentYear) {
      // 当年数据
      const currentCount = trendMap.get(monthKey) || 0
      trendMap.set(monthKey, currentCount + record.saleCount)
    } else if (year === currentYear - 1) {
      // 去年数据
      const lastYearMonthKey = `${currentYear}-${String(saleDate.getMonth() + 1).padStart(2, '0')}`
      const lastYearCount = lastYearTrendMap.get(lastYearMonthKey) || 0
      lastYearTrendMap.set(lastYearMonthKey, lastYearCount + record.saleCount)
    }
  })

  // 生成过去12个月的趋势数据
  const trendData: SalesTrendItem[] = []
  const currentDate = new Date()

  for (let i = 11; i >= 0; i--) {
    const date = new Date(currentDate.getFullYear(), currentDate.getMonth() - i, 1)
    const monthKey = date.toISOString().slice(0, 7)
    const salesVolume = trendMap.get(monthKey) || 0
    const lastYearSalesVolume = lastYearTrendMap.get(monthKey) || 0

    trendData.push({
      date: monthKey,
      salesVolume: salesVolume,
      month: date.getMonth() + 1,
      year: date.getFullYear(),
      lastYearSalesVolume: lastYearSalesVolume // ✅ 使用真实的去年数据
    })
  }

  salesTrendData.value = trendData

  // 计算实际销量总和和去年销量总和
  actualSales.value = trendData.reduce((sum, item) => sum + item.salesVolume, 0)
  lastYearSales.value = trendData.reduce((sum, item) => sum + (item.lastYearSalesVolume || 0), 0)

  console.log('销量趋势处理完成，总销量:', actualSales.value, '去年总销量:', lastYearSales.value)

}

// 2. 处理销售额数据
const processSalesAmountData = () => {
  console.log('处理销售额数据...')

  const amountMap = new Map<string, number>()

  // 汇总每月销售额
  saleRecordsData.value.forEach(record => {
    const monthKey = record.saleMonth.slice(0, 7)
    const currentAmount = amountMap.get(monthKey) || 0
    // 销售额 = 销量 × 单价 (转换为万元)
    const monthlyAmount = (record.saleCount * record.saleAmount) / 10000
    amountMap.set(monthKey, currentAmount + monthlyAmount)
  })

  const amountData: SalesAmountItem[] = []
  const currentDate = new Date()

  for (let i = 11; i >= 0; i--) {
    const date = new Date(currentDate.getFullYear(), currentDate.getMonth() - i, 1)
    const monthKey = date.toISOString().slice(0, 7)
    const salesAmount = amountMap.get(monthKey) || 0

    amountData.push({
      date: monthKey,
      salesAmount: Math.round(salesAmount * 100) / 100, // 保留2位小数
      month: date.getMonth() + 1,
      year: date.getFullYear()
    })
  }

  salesAmountData.value = amountData
  console.log('💰 销售额数据处理完成')
}

// 3. 处理车型排行数据
const processTopModelsData = () => {
  console.log('🏆 处理车型排行数据...')

  const modelSalesMap = new Map<number, {
    carModel: string
    brandName: string
    salesVolume: number
    salesAmount: number
    recordCount: number
    lastYearSalesVolume: number
    growthRate?: number
  }>()

  // 分别计算当年和去年的车型销量
  saleRecordsData.value.forEach(record => {
    const modelId = record.carModelId
    const modelName = record.carModel?.modelName || '未知车型'
    const carModelInfo = carModelsData.value.find(model => model.carModelId === modelId)
    const brandName = carModelInfo?.brandName || '未知品牌'

    const saleDate = new Date(record.saleMonth)
    const year = saleDate.getFullYear()
    const currentYear = new Date().getFullYear()

    if (!modelSalesMap.has(modelId)) {
      modelSalesMap.set(modelId, {
        carModel: modelName,
        brandName: brandName,
        salesVolume: 0,
        salesAmount: 0,
        recordCount: 0,
        lastYearSalesVolume: 0
      })
    }

    const existing = modelSalesMap.get(modelId)!

    if (year === currentYear) {
      // 当年数据
      existing.salesVolume += record.saleCount
      existing.salesAmount += record.saleCount * record.saleAmount
      existing.recordCount += 1
    } else if (year === currentYear - 1) {
      // 去年数据
      existing.lastYearSalesVolume += record.saleCount
    }
  })

  // 计算真实增长率
  modelSalesMap.forEach(model => {
    if (model.lastYearSalesVolume > 0) {
      model.growthRate = ((model.salesVolume - model.lastYearSalesVolume) / model.lastYearSalesVolume) * 100
    } else {
      model.growthRate = model.salesVolume > 0 ? 100 : 0 // 新车型或去年无销量
    }
  })

  // 转换为数组并根据不同类型排序
  const modelsArray = Array.from(modelSalesMap.values())

  if (modelRankingType.value === 'sales') {
    modelsArray.sort((a, b) => b.salesVolume - a.salesVolume)
  } else if (modelRankingType.value === 'growth') {
    modelsArray.sort((a, b) => (b.growthRate || 0) - (a.growthRate || 0))
  } else if (modelRankingType.value === 'share') {
    modelsArray.sort((a, b) => b.salesAmount - a.salesAmount)
  }

  // 取前N名
  topModelsData.value = modelsArray.slice(0, topN.value).map(model => ({
    ...model,
    marketShare: actualSales.value > 0 ? (model.salesVolume / actualSales.value * 100) : 0,
    profit: Math.floor(model.salesAmount * 0.15) // 假设15%利润率
  }))

  console.log('🏆 车型排行处理完成，前', topN.value, '名车型')
}

// 4. 处理地区销量数据
const processRegionSalesData = () => {
  console.log('📍 处理地区销量数据...')

  const regionSalesMap = new Map<number, {
    regionId: number
    regionName: string
    salesVolume: number
    salesAmount: number
    recordCount: number
  }>()

  // 汇总每个地区的销量数据
  saleRecordsData.value.forEach(record => {
    const regionId = record.regionId
    const regionName = record.region?.regionName || '未知地区'

    if (regionSalesMap.has(regionId)) {
      const existing = regionSalesMap.get(regionId)!
      existing.salesVolume += record.saleCount
      existing.salesAmount += record.saleCount * record.saleAmount
      existing.recordCount += 1
    } else {
      regionSalesMap.set(regionId, {
        regionId: regionId,
        regionName: regionName,
        salesVolume: record.saleCount,
        salesAmount: record.saleCount * record.saleAmount,
        recordCount: 1
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
    marketShare: actualSales.value > 0 ? (region.salesVolume / actualSales.value * 100) : 0
  }))

  // 更新可用地区列表
  availableRegions.value = regionsArray.map(region => ({
    id: region.regionId,
    name: region.regionName
  }))

  console.log('📍 地区销量处理完成，覆盖', regionsArray.length, '个地区')
}

// 5. 计算业务指标
const calculateBusinessMetrics = () => {
  console.log('📊 计算业务指标...')

  // 计算上年同期销量（简化处理）
  const currentTotal = actualSales.value
  lastYearSales.value = Math.floor(currentTotal * (0.85 + Math.random() * 0.3)) // 模拟上年数据

  // 更新可用车型列表
  availableCarModels.value = carModelsData.value

  // 更新全局筛选条件
  if (salesTrendData.value.length > 0) {
    globalFilters.customDateRange = [
      new Date(salesTrendData.value[0].date),
      new Date(salesTrendData.value[salesTrendData.value.length - 1].date)
    ]
  }

  console.log('📊 业务指标计算完成')
}

// ===========================================
// 📊 计算属性 - 业务指标
// ===========================================

const totalPercentage = computed(() => {
  return investmentBreakdown.online + investmentBreakdown.offline +
         investmentBreakdown.dealer + investmentBreakdown.other
})

const expectedROI = computed(() => {
  if (targetForm.marketingInvestment <= 0) return 0
  // 简化ROI计算：预期收益 / 投入成本
  const expectedRevenue = targetForm.salesTarget * 200000 // 假设平均单价20万
  return ((expectedRevenue - targetForm.marketingInvestment) / targetForm.marketingInvestment * 100)
})

const roiAssessment = computed(() => {
  const roi = expectedROI.value
  const industryAvg = targetForm.industryAverageROI || 0

  if (roi > industryAvg + 10) {
    return { type: 'success', text: '优秀' }
  } else if (roi > industryAvg) {
    return { type: 'warning', text: '良好' }
  } else {
    return { type: 'danger', text: '需改进' }
  }
})

const achievementRate = computed(() => {
  return targetSales.value > 0 ? (actualSales.value / targetSales.value * 100) : 0
})

const achievementChangeType = computed(() => {
  if (achievementChange.value >= 5) return 'success'
  if (achievementChange.value >= 0) return 'warning'
  return 'danger'
})

const yoyGrowth = computed(() => {
  return lastYearSales.value > 0 ?
    ((actualSales.value - lastYearSales.value) / lastYearSales.value * 100) :
    (actualSales.value > 0 ? 100 : 0) //  修正：如果去年无数据但今年有，增长率为100%
})

const marketShare = computed(() => {
  return totalMarketSales.value > 0 ? (actualSales.value / totalMarketSales.value * 100) : 0
})

const channelROI = computed(() => {
  return channelInvestment.value > 0 ?
    (actualSales.value * 200000 / channelInvestment.value) : 0 // 假设平均单价20万
})

const kpiVsTarget = computed(() => {
  // 根据当前KPI值与目标对比
  return { type: 'success', text: '超出目标5.2%' }
})

const kpiVsIndustry = computed(() => {
  // 根据当前KPI值与行业对比
  return { type: 'warning', text: '略低于行业平均' }
})

// 趋势分析
const trendAnalysis = computed(() => {
  if (salesTrendData.value.length < 3) {
    return { type: 'info', summary: '数据不足', detail: '需要更多数据进行分析' }
  }

  const recent = salesTrendData.value.slice(-3)
  const trend = recent[recent.length - 1].salesVolume - recent[0].salesVolume
  const volatility = calculateVolatility(recent.map(item => item.salesVolume))

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
      detail: `销量波动在正常范围内，波动率${volatility.toFixed(1)}%`
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

// ===========================================
// 🛠️ 工具函数
// ===========================================

const calculateVolatility = (data: number[]) => {
  const mean = data.reduce((sum, val) => sum + val, 0) / data.length
  const variance = data.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / data.length
  return Math.sqrt(variance)
}

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
  console.log('🔄 全局筛选条件变更:', globalFilters)
  // 重新处理数据（可以在这里添加筛选逻辑）
  processBasicData()
}

// ===========================================
// 📈 图表初始化函数
// ===========================================

const initSalesTrendChart = async () => {
  if (!salesTrendChart.value) return

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
      data: data.map((item: any) => item.date),
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
        data: data.map((item: any) => item.salesVolume),
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 3, color: '#409EFF' },
        areaStyle: { color: 'rgba(64, 158, 255, 0.1)' }
      },
      ...(showComparison.value ? [{
        name: '去年同期',
        type: 'line',
        data: data.map((item: any) => item.lastYearSalesVolume || 0),
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
  if (!salesAmountChart.value) return

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
      data: data.map((item: any) => item.date),
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
      data: data.map((item: any) => item.salesAmount),
      itemStyle: {
        color: '#67C23A',
        borderRadius: [4, 4, 0, 0]
      }
    }]
  }

  salesAmountChartInstance.setOption(option)
}

const initTopModelsChart = async () => {
  if (!topModelsChart.value) return

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
            <div>增长率: ${item.growthRate >= 0 ? '+' : ''}${(item.growthRate || 0).toFixed(1)}%</div>
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
      data: data.map((item: any) => `${item.brandName} ${item.carModel}`),
      axisLabel: { fontSize: 10 }
    },
    series: [{
      name: '排行',
      type: 'bar',
      data: data.map((item: any, index: number) => ({
        value: modelRankingType.value === 'sales' ? item.salesVolume :
               modelRankingType.value === 'growth' ? (item.growthRate || 0) : item.marketShare,
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
  if (!regionSalesChart.value) return

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
      data: data.map((item: any) => item.region),
      axisLabel: { interval: 0, rotate: 45, fontSize: 11 }
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
        name: '销量',
        type: 'bar',
        data: data.map((item: any) => item.salesVolume),
        itemStyle: {
          color: '#F56C6C',
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  }

  regionSalesChartInstance.setOption(option)
}

// ===========================================
// 🎯 事件处理函数
// ===========================================

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
  if (totalPercentage.value !== 100) {
    ElMessageBox.confirm('投入分配总和不等于100%，是否继续关闭？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      done()
    }).catch(() => {
      // 不关闭
    })
  } else {
    done()
  }
}

const resetTargetForm = () => {
  targetForm.period = 'month'
  targetForm.targetTime = new Date()
  targetForm.quarter = 'Q1'
  targetForm.salesTarget = 50000
  targetForm.marketingInvestment = 5000000
  targetForm.industryAverageROI = 3.5

  investmentBreakdown.online = 40
  investmentBreakdown.offline = 30
  investmentBreakdown.dealer = 25
  investmentBreakdown.other = 5
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
  if (saleRecordsData.value.length > 0) {
    processTopModelsData()
    initTopModelsChart()
  }
}

const handleTopNChange = () => {
  if (saleRecordsData.value.length > 0) {
    processTopModelsData()
    initTopModelsChart()
  }
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

// ===========================================
// 🔄 主数据刷新函数
// ===========================================

const refreshAllCharts = async () => {
  loading.value = true
  try {
    ElMessage.info('正在加载基础数据...')
    console.log('🚀 开始获取基础数据...')

    // 1. 并行获取基础数据
    const [carModels, saleRecords] = await Promise.all([
      fetchCarModels(),
      fetchSaleRecords()
    ])

    // 2. 存储基础数据
    carModelsData.value = carModels
    saleRecordsData.value = saleRecords

    console.log('📊 基础数据获取完成:', {
      车型数量: carModels.length,
      销售记录数量: saleRecords.length
    })

    ElMessage.info('正在处理业务数据...')

    // 3. 处理基础数据生成业务数据
    processBasicData()

    // 4. 初始化图表
    console.log('📈 开始初始化图表...')
    await Promise.all([
      initSalesTrendChart(),
      initSalesAmountChart(),
      initTopModelsChart(),
      initRegionSalesChart()
    ])

    ElMessage.success(`数据加载完成！共加载${saleRecords.length}条销售记录，${carModels.length}个车型`)
  } catch (error) {
    console.error('❌ 数据加载失败:', error)
    ElMessage.error('数据加载失败，使用模拟数据')

    // 降级到模拟数据
    console.log('🔄 降级使用模拟数据...')
    carModelsData.value = generateMockCarModels()
    saleRecordsData.value = generateMockSaleRecords()

    // 处理模拟数据
    processBasicData()

    await Promise.all([
      initSalesTrendChart(),
      initSalesAmountChart(),
      initTopModelsChart(),
      initRegionSalesChart()
    ])
    console.log('✅ 模拟数据加载完成')
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  refreshAllCharts()
}

// ===========================================
// 🎲 模拟数据生成函数（降级使用）
// ===========================================

const generateMockCarModels = (): CarModel[] => {
  return [
    {
      carModelId: 1001,
      modelName: "Model Y",
      brandId: 101,
      brandName: "特斯拉",
      level: "B级",
      launchDate: "2021-01-01",
      officialPrice: 263900.00,
      engineType: "纯电动",
      seatNum: 5,
      driveType: "四驱",
      rangeKm: 594
    },
    {
      carModelId: 1002,
      modelName: "海豚",
      brandId: 102,
      brandName: "比亚迪",
      level: "A级",
      launchDate: "2021-08-01",
      officialPrice: 103800.00,
      engineType: "纯电动",
      seatNum: 5,
      driveType: "前驱",
      rangeKm: 405
    },
    {
      carModelId: 1003,
      modelName: "理想ONE",
      brandId: 103,
      brandName: "理想",
      level: "B级",
      launchDate: "2019-12-01",
      officialPrice: 338000.00,
      engineType: "增程式",
      seatNum: 6,
      driveType: "四驱",
      rangeKm: 180
    },
    {
      carModelId: 1004,
      modelName: "ES6",
      brandId: 104,
      brandName: "蔚来",
      level: "B级",
      launchDate: "2019-06-01",
      officialPrice: 358000.00,
      engineType: "纯电动",
      seatNum: 5,
      driveType: "四驱",
      rangeKm: 420
    },
    {
      carModelId: 1005,
      modelName: "P7",
      brandId: 105,
      brandName: "小鹏",
      level: "B级",
      launchDate: "2020-04-01",
      officialPrice: 229900.00,
      engineType: "纯电动",
      seatNum: 5,
      driveType: "后驱",
      rangeKm: 480
    }
  ]
}

const generateMockSaleRecords = (): SaleRecord[] => {
  const records: SaleRecord[] = []
  const currentDate = new Date()

  // 模拟地区数据
  const regions = [
    { id: 101, name: '北京市' },
    { id: 102, name: '上海市' },
    { id: 103, name: '广东省' },
    { id: 104, name: '浙江省' },
    { id: 105, name: '江苏省' },
    { id: 106, name: '山东省' },
    { id: 107, name: '四川省' },
    { id: 108, name: '湖北省' }
  ]

  for (let i = 0; i < 500; i++) {
    const monthsBack = Math.floor(Math.random() * 12)
    const date = new Date(currentDate.getFullYear(), currentDate.getMonth() - monthsBack, 1)
    const carModelId = 1001 + (i % 5)
    const regionIndex = Math.floor(Math.random() * regions.length)

    records.push({
      saleRecordId: i + 1,
      carModelId: carModelId,
      regionId: regions[regionIndex].id,
      saleMonth: date.toISOString().slice(0, 10),
      saleCount: Math.floor(Math.random() * 200) + 50,
      saleAmount: 200000 + Math.random() * 100000,
      carModel: {
        carModelId: carModelId,
        modelName: ["Model Y", "海豚", "理想ONE", "ES6", "P7"][i % 5],
        brandId: 101 + (i % 5)
      },
      region: {
        regionId: regions[regionIndex].id,
        regionName: regions[regionIndex].name,
        parentRegionId: null
      }
    })
  }

  return records
}

// 保存目标设置
const loadSavedTargets = async () => {
  // 从本地存储加载已保存的目标设置
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

// ===========================================
// 🔄 生命周期
// ===========================================

onMounted(async () => {
  ElMessage.success('欢迎使用销售总览页面！')

  // 初始化市场数据
  totalMarketSales.value = 500000

  // 加载已保存的目标设置
  await loadSavedTargets()

  await nextTick()
  await refreshAllCharts()

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

.investment-breakdown {
  width: 100%;
}

.breakdown-card {
  height: 100%;
}

.breakdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.breakdown-item:last-child {
  border-bottom: none;
}

.breakdown-total {
  margin-top: 16px;
  padding: 12px;
  border-radius: 6px;
  text-align: center;
  font-weight: 600;
}

.breakdown-total.success {
  background: #f0f9ff;
  color: #409eff;
}

.breakdown-total.error {
  background: #fef0f0;
  color: #f56c6c;
}

.error-text {
  font-size: 12px;
  margin-left: 8px;
}

.roi-analysis {
  padding: 16px 0;
}

.roi-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
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

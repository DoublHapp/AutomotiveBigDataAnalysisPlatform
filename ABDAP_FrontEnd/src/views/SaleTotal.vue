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

          <!-- 新增：行业平均ROI输入 -->
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
          <label>车型类别:</label>
          <el-select v-model="globalFilters.carType" @change="handleGlobalFilterChange">
            <el-option label="特斯拉 Model Y" value="model_y" />
            <el-option label="特斯拉 Model 3" value="model_3" />
            <el-option label="比亚迪汉EV" value="han_ev" />
            <el-option label="比亚迪海豚" value="dolphin" />
            <el-option label="理想ONE" value="ideal_one" />
            <el-option label="蔚来ES6" value="es6" />
            <el-option label="小鹏P7" value="p7" />
            <el-option label="奔驰C级" value="c_class" />
            <el-option label="宝马3系" value="bmw_3" />
            <el-option label="奥迪A4L" value="a4l" />
            <el-option label="丰田凯美瑞" value="camry" />
            <el-option label="本田雅阁" value="accord" />
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
                <el-select v-model="modelRankingType" size="small" @change="updateModelRanking">
                  <el-option label="销量排行" value="sales" />
                  <el-option label="增长率排行" value="growth" />
                  <el-option label="市场份额排行" value="share" />
          
                </el-select>
                <el-input-number
                  v-model="topN"
                  :min="5"
                  :max="20"
                  size="small"
                  @change="updateModelRanking"
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
  Setting
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()

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


// 目标设置表单
const targetForm = reactive({
  period: 'month', // month, quarter, year
  targetTime: new Date(),
  quarter: 'Q1',
  salesTarget: 50000,
  marketingInvestment: 5000000
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
const totalMarketSales = ref(0)
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

// 数据存储
const salesTrendData = ref<any[]>([])
const salesAmountData = ref<any[]>([])
const topModelsData = ref<any[]>([])
const regionSalesData = ref<any[]>([])
const availableRegions = ref<any[]>([])
const detailTableData = ref<any[]>([])
const currentRecommendations = ref<any[]>([])
const forecastSummary = ref('')

// 计算属性 - 业务指标

const totalPercentage = computed(() => {
  return investmentBreakdown.online + investmentBreakdown.offline +
         investmentBreakdown.dealer + investmentBreakdown.other
})

const expectedROI = computed(() => {
  if (targetForm.marketingInvestment === 0) return 0
  // 假设平均售价20万，计算预期ROI
  const expectedRevenue = targetForm.salesTarget * 200000
  return expectedRevenue / targetForm.marketingInvestment
})

const roiAssessment = computed(() => {
  const roi = expectedROI.value
  if (roi >= 4.5) return { type: 'success', text: '优秀' }
  if (roi >= 3.5) return { type: 'warning', text: '良好' }
  if (roi >= 2.5) return { type: 'info', text: '一般' }
  return { type: 'danger', text: '偏低' }
})
const achievementRate = computed(() => {
  return targetSales.value > 0 ? (actualSales.value / targetSales.value) * 100 : 0
})

const yoyGrowth = computed(() => {
  return lastYearSales.value > 0 ? ((actualSales.value - lastYearSales.value) / lastYearSales.value) * 100 : 0
})

const marketShare = computed(() => {
  return totalMarketSales.value > 0 ? (actualSales.value / totalMarketSales.value) * 100 : 0
})

const channelROI = computed(() => {
  return channelInvestment.value > 0 ? (actualSales.value * 200000) / channelInvestment.value : 0
})

const achievementChangeType = computed(() => {
  if (achievementChange.value > 0) return 'success'
  if (achievementChange.value < 0) return 'danger'
  return 'info'
})

const kpiVsTarget = computed(() => {
  const current = achievementRate.value
  if (current >= 100) return { type: 'success', text: `超额完成 ${(current - 100).toFixed(1)}%` }
  if (current >= 90) return { type: 'warning', text: `接近目标 ${(100 - current).toFixed(1)}%` }
  return { type: 'danger', text: `低于目标 ${(100 - current).toFixed(1)}%` }
})

const kpiVsIndustry = computed(() => {
  const diff = yoyGrowth.value - industryGrowth.value
  if (diff > 2) return { type: 'success', text: `超越行业 ${diff.toFixed(1)}%` }
  if (diff > -2) return { type: 'warning', text: `持平行业 ${diff.toFixed(1)}%` }
  return { type: 'danger', text: `落后行业 ${Math.abs(diff).toFixed(1)}%` }
})

// 智能分析
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
      summary: '平稳发展',
      detail: `销量相对稳定，波动性${volatility > 300 ? '较大' : '适中'}，建议优化产品结构`
    }
  }
})

const amountAnalysis = computed(() => {
  if (salesAmountData.value.length < 3) {
    return { type: 'info', summary: '数据不足', detail: '需要更多数据进行分析' }
  }

  const recent = salesAmountData.value.slice(-3)
  const trend = recent[recent.length - 1].salesAmount - recent[0].salesAmount
  const avgPrice = actualSales.value > 0 ? (recent[recent.length - 1].salesAmount * 10000) / actualSales.value : 0

  if (trend > 10000) {
    return {
      type: 'success',
      summary: '收入增长',
      detail: `销售额增长${(trend/10000).toFixed(1)}万，平均单价${(avgPrice/10000).toFixed(1)}万`
    }
  } else if (trend < -10000) {
    return {
      type: 'danger',
      summary: '收入下滑',
      detail: `销售额下降${Math.abs(trend/10000).toFixed(1)}万，需要提升单价或销量`
    }
  } else {
    return {
      type: 'warning',
      summary: '收入稳定',
      detail: `销售额基本持平，平均单价${(avgPrice/10000).toFixed(1)}万`
    }
  }
})

// 业务预警
const businessAlerts = computed(() => {
  const alerts = []

  if (achievementRate.value < 85) {
    alerts.push({
      id: 'achievement_low',
      type: 'error',
      title: '目标完成率预警',
      message: `当前完成率${achievementRate.value.toFixed(1)}%，距离目标还差${(targetSales.value - actualSales.value).toLocaleString()}台`,

    })
  }

  if (yoyGrowth.value < -10) {
    alerts.push({
      id: 'growth_negative',
      type: 'warning',
      title: '同比增长预警',
      message: `同比下降${Math.abs(yoyGrowth.value).toFixed(1)}%，市场表现不佳`,

    })
  }

  if (marketShare.value < 5) {
    alerts.push({
      id: 'share_low',
      type: 'warning',
      title: '市场份额提醒',
      message: `市场份额仅${marketShare.value.toFixed(1)}%，存在提升空间`,
    })
  }

  if (channelROI.value < 2) {
    alerts.push({
      id: 'roi_low',
      type: 'warning',
      title: 'ROI偏低提醒',
      message: `渠道ROI仅${channelROI.value.toFixed(1)}，低于行业标准`,
      
    })
  }

  return alerts
})


// 工具函数
const calculateVolatility = (data: number[]) => {
  const mean = data.reduce((sum, val) => sum + val, 0) / data.length
  const variance = data.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / data.length
  return Math.sqrt(variance)
}

// API调用函数
const fetchSalesTrend = async () => {
  try {
    const response = await axios.get('/api/SaleTotal/TotalSalesTrend', {
      params: globalFilters
    })
    if (response.data.status === 1) {
      return response.data.data
    } else {
      return generateMockSalesTrendData()
    }
  } catch (error) {
    return generateMockSalesTrendData()
  }
}

const fetchSalesAmount = async () => {
  try {
    const response = await axios.get('/api/SaleTotal/SalesAmount', {
      params: globalFilters
    })
    if (response.data.status === 1) {
      return response.data.data
    } else {
      return generateMockSalesAmountData()
    }
  } catch (error) {
    return generateMockSalesAmountData()
  }
}

const fetchTopModels = async () => {
  try {
    const response = await axios.get('/api/SaleTotal/TopModels', {
      params: { ...globalFilters, type: modelRankingType.value, limit: topN.value }
    })
    if (response.data.status === 1) {
      return response.data.data
    } else {
      return generateMockTopModelsData()
    }
  } catch (error) {
    return generateMockTopModelsData()
  }
}

const fetchRegionSales = async () => {
  try {
    const response = await axios.get('/api/SaleTotal/RegionSales', {
      params: globalFilters
    })
    if (response.data.status === 1) {
      return response.data.data
    } else {
      return generateMockRegionSalesData()
    }
  } catch (error) {
    return generateMockRegionSalesData()
  }
}

// 模拟数据生成
const generateMockSalesTrendData = () => {
  const data = []
  const baseDate = new Date()
  let baseSales = 3000

  for (let i = 11; i >= 0; i--) {
    const date = new Date(baseDate.getFullYear(), baseDate.getMonth() - i, 1)
    const trend = Math.sin(i * 0.5) * 500
    const random = (Math.random() - 0.5) * 1000
    const salesVolume = Math.floor(baseSales + trend + random)

    data.push({
      date: date.toISOString().slice(0, 7),
      salesVolume: Math.max(1000, salesVolume),
      lastYearSalesVolume: Math.floor(salesVolume * 0.9 + (Math.random() - 0.5) * 500)
    })

    baseSales += 100
  }

  actualSales.value = data.reduce((sum, item) => sum + item.salesVolume, 0)
  lastYearSales.value = data.reduce((sum, item) => sum + item.lastYearSalesVolume, 0)

  return data
}

const generateMockSalesAmountData = () => {
  const data = []
  const baseDate = new Date()

  for (let i = 11; i >= 0; i--) {
    const date = new Date(baseDate.getFullYear(), baseDate.getMonth() - i, 1)
    data.push({
      date: date.toISOString().slice(0, 7),
      salesAmount: Math.floor(Math.random() * 50000) + 80000,
    })
  }
  return data
}

const generateMockTopModelsData = () => {
  const models = [
    '特斯拉 Model Y', '比亚迪海豚', '理想ONE', '蔚来ES6', '小鹏P7',
    '奔驰C级', '宝马3系', '奥迪A4L', '丰田凯美瑞', '本田雅阁'
  ]

  return models.slice(0, topN.value).map((model, index) => ({
    carModel: model,
    salesVolume: Math.floor(1500 - index * 120 + Math.random() * 200),
    growthRate: (Math.random() - 0.3) * 50,
    marketShare: Math.random() * 10,
    profit: Math.floor(Math.random() * 5000) + 2000
  }))
}

const generateMockRegionSalesData = () => {
  const regions = [
    '北京', '上海', '广东', '浙江', '江苏', '山东', '四川', '湖北'
  ]

  availableRegions.value = regions.map((name, index) => ({ id: index + 1, name }))

  return regions.map(region => ({
    region,
    salesVolume: Math.floor(Math.random() * 2000) + 1000,
  }))
}

// 图表初始化 - 增强版
const initSalesTrendChart = async () => {
  if (!salesTrendChart.value) return

  const data = await fetchSalesTrend()
  salesTrendData.value = data

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
      axisPointer: { type: 'cross' },
      formatter: (params: any) => {
        let result = `<div style="font-weight: bold; margin-bottom: 6px;">${params[0].axisValue}</div>`
        params.forEach((param: any) => {
          const color = param.color
          const name = param.seriesName
          const value = param.value.toLocaleString()
          result += `<div style="margin-bottom: 3px;">
            <span style="display: inline-block; width: 10px; height: 10px; background: ${color}; margin-right: 6px;"></span>
            ${name}: <strong>${value}</strong> 台
          </div>`
        })
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
        itemStyle: { color: '#409EFF' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
            ]
          }
        },
        markLine: targetSales.value > 0 ? {
          data: [
            {
              name: '销量目标',
              yAxis: targetSales.value / data.length,
              lineStyle: { color: '#f56c6c', type: 'dashed' },
              label: { position: 'end', formatter: '目标线' }
            }
          ]
        } : undefined
      }
    ]
  }

  // 添加同比数据
  if (showComparison.value) {
    option.series.push({
      name: '去年同期',
      type: 'line',
      data: data.map((item: any) => item.lastYearSalesVolume),
      smooth: true,
      symbol: 'diamond',
      symbolSize: 6,
      lineStyle: { width: 2, color: '#E6A23C', type: 'dashed' },
      itemStyle: { color: '#E6A23C' }
    } as any)
  }

  salesTrendChartInstance.setOption(option)

  // 添加点击事件
  salesTrendChartInstance.on('click', (params: any) => {
    selectedTimePoint.value = params.name
    handleTrendChartClick(params)
  })
}

const initSalesAmountChart = async () => {
  if (!salesAmountChart.value) return

  const data = await fetchSalesAmount()
  salesAmountData.value = data

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
        return `${data.axisValue}<br/>销售额: ${data.value.toLocaleString()} 万元`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '8%',
      top: '12%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map((item: any) => item.date)
    },
    yAxis: {
      type: 'value',
      name: '销售额(万元)',
      axisLabel: {
        formatter: (value: number) => value.toLocaleString()
      }
    },
    series: [
      {
        name: '销售额',
        type: 'line',
        data: data.map((item: any) => item.salesAmount),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#67C23A' },
        itemStyle: { color: '#67C23A' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
              { offset: 1, color: 'rgba(103, 194, 58, 0.1)' }
            ]
          }
        }
      }
    ]
  }

  salesAmountChartInstance.setOption(option)

  // 添加点击事件
  salesAmountChartInstance.on('click', (params: any) => {
    handleAmountChartClick(params)
  })
}

const initTopModelsChart = async () => {
  if (!topModelsChart.value) return

  const data = await fetchTopModels()
  topModelsData.value = data

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
          }
          return value.toFixed(1) + '%'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: data.map((item: any) => item.carModel).reverse(),
      inverse: true,
      axisLabel: { interval: 0, fontSize: 12 }
    },
    series: [
      {
        name: modelRankingType.value === 'sales' ? '销量' :
              modelRankingType.value === 'growth' ? '增长率' : '市场份额',
        type: 'bar',
        data: data.map((item: any, index: number) => ({
          value: modelRankingType.value === 'sales' ? item.salesVolume :
                 modelRankingType.value === 'growth' ? item.growthRate : item.marketShare,
          itemStyle: {
            color: colorList[index % colorList.length]
          }
        })).reverse(),
        barWidth: 20,
        label: {
          show: true,
          position: 'right',
          formatter: (params: any) => {
            if (modelRankingType.value === 'sales') {
              return params.value.toLocaleString()
            }
            return params.value.toFixed(1) + '%'
          },
          fontSize: 11
        }
      }
    ]
  }

  topModelsChartInstance.setOption(option)

  // 添加点击事件
  topModelsChartInstance.on('click', (params: any) => {
    handleModelChartClick(params)
  })
}

const initRegionSalesChart = async () => {
  if (!regionSalesChart.value) return

  const data = await fetchRegionSales()
  regionSalesData.value = data

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
        },
        barWidth: '60%',
        label: {
          show: true,
          position: 'top',
          formatter: (params: any) => params.value.toLocaleString(),
          fontSize: 10
        }
      }
    ]
  }

  regionSalesChartInstance.setOption(option)

  // 添加点击事件
  regionSalesChartInstance.on('click', (params: any) => {
    highlightedRegion.value = params.name
    handleRegionChartClick(params)
  })
}

const initKPITrendChart = (type: string) => {
  if (!kpiTrendChart.value) return

  if (kpiTrendChartInstance) {
    kpiTrendChartInstance.dispose()
  }

  kpiTrendChartInstance = echarts.init(kpiTrendChart.value)

  // 模拟KPI趋势数据
  const dates = []
  const values = []
  const baseDate = new Date()

  for (let i = 11; i >= 0; i--) {
    const date = new Date(baseDate.getFullYear(), baseDate.getMonth() - i, 1)
    dates.push(date.toISOString().slice(0, 7))

    switch (type) {
      case 'achievement':
        values.push(Math.random() * 30 + 70)
        break
      case 'growth':
        values.push((Math.random() - 0.5) * 40)
        break
      case 'share':
        values.push(Math.random() * 8 + 2)
        break
      case 'roi':
        values.push(Math.random() * 3 + 1)
        break
    }
  }

  const option = {
    title: {
      text: `${kpiDialogTitle.value}趋势`,
      textStyle: { fontSize: 14, color: '#333' }
    },
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '10%',
      right: '10%',
      bottom: '10%',
      top: '20%'
    },
    xAxis: {
      type: 'category',
      data: dates
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: kpiDialogTitle.value,
        type: 'line',
        data: values,
        smooth: true,
        lineStyle: { width: 3 },
        areaStyle: { opacity: 0.3 }
      }
    ]
  }

  kpiTrendChartInstance.setOption(option)
}

// 事件处理函数
const getTimePlaceholder = () => {
  switch (targetForm.period) {
    case 'month': return '选择目标月份'
    case 'quarter': return '选择目标年份'
    case 'year': return '选择目标年份'
    default: return '选择时间'
  }
}

const handlePeriodChange = () => {
  // 重置时间选择
  targetForm.targetTime = new Date()
  if (targetForm.period === 'quarter') {
    targetForm.quarter = 'Q1'
  }
}


const resetTargetForm = () => {
  targetFormRef.value?.resetFields()
  // 重置投入分配为默认值
  Object.assign(investmentBreakdown, {
    online: 40,
    offline: 30,
    dealer: 25,
    other: 5
  })
}


const formatTargetTime = () => {
  const date = new Date(targetForm.targetTime)
  switch (targetForm.period) {
    case 'month':
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
    case 'quarter':
      return `${date.getFullYear()}-${targetForm.quarter}`
    case 'year':
      return date.getFullYear().toString()
    default:
      return ''
  }
}

const handleTargetDialogClose = () => {
  showTargetDialog.value = false
}

// 页面加载时获取已保存的目标设置
const loadSavedTargets = async () => {
  try {
    // const response = await axios.get('/api/sales/targets/current')
    // if (response.data.status === 1) {
    //   const data = response.data.data
    //   targetSales.value = data.salesTarget
    //   channelInvestment.value = data.marketingInvestment
    // }
  } catch (error) {
    console.error('加载目标设置失败:', error)
  }
}

const handleGlobalFilterChange = () => {
  updateAllChartsWithFilter()
}


const handleTrendChartClick = (params: any) => {
  selectedTimePoint.value = params.name
  updateChartsForTimePoint(params.name)
  showDetailAnalysis('趋势详情', params)
}

const handleAmountChartClick = (params: any) => {
  showDetailAnalysis('销售额详情', params)
}

const handleModelChartClick = (params: any) => {
  const modelData = topModelsData.value.find(item => item.carModel === params.name)
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
  nextTick(() => {
    initKPITrendChart(type)
  })
}

const updateAllChartsWithFilter = async () => {
  loading.value = true
  try {
    await Promise.all([
      initSalesTrendChart(),
      initSalesAmountChart(),
      initTopModelsChart(),
      initRegionSalesChart()
    ])
  } finally {
    loading.value = false
  }
}

const updateChartsForTimePoint = (timePoint: string) => {
  const charts = [salesAmountChartInstance, topModelsChartInstance, regionSalesChartInstance]
  charts.forEach(chart => {
    if (chart) {
      chart.dispatchAction({
        type: 'highlight',
        dataIndex: salesTrendData.value.findIndex(item => item.date === timePoint)
      })
    }
  })
}

const clearTimeSelection = () => {
  selectedTimePoint.value = ''
  const charts = [salesTrendChartInstance, salesAmountChartInstance, topModelsChartInstance, regionSalesChartInstance]
  charts.forEach(chart => {
    if (chart) {
      chart.dispatchAction({ type: 'downplay' })
    }
  })
}

const clearRegionHighlight = () => {
  highlightedRegion.value = ''
  if (regionSalesChartInstance) {
    regionSalesChartInstance.dispatchAction({ type: 'downplay' })
  }
}

const showDetailAnalysis = (title: string, data: any) => {
  detailDialogTitle.value = title

  if (title.includes('趋势')) {
    const timeData = salesTrendData.value.find(item => item.date === data.name)
    detailTableData.value = [
      { name: '当期销量', value: `${timeData?.salesVolume.toLocaleString()} 台`, change: '', trend: '' },
      { name: '去年同期', value: `${timeData?.lastYearSalesVolume.toLocaleString()} 台`, change: '', trend: '' },
      { name: '同比增长', value: `${((timeData?.salesVolume - timeData?.lastYearSalesVolume) / timeData?.lastYearSalesVolume * 100).toFixed(1)}%`, change: '', trend: '' }
    ]
  }

  showDetailDialog.value = true
}

const handleDetailDialogClose = () => {
  showDetailDialog.value = false
  activeDetailTab.value = 'data'
}

const toggleTrendComparison = () => {
  showComparison.value = !showComparison.value
  initSalesTrendChart()
}

const updateModelRanking = () => {
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





const showModelComparison = () => {
  ElMessage.info('正在对比车型表现...')
}



const showHeatMap = () => {
  router.push({ name: 'CarPurchasesHeatMap' })
}



const refreshAllCharts = async () => {
  loading.value = true
  try {
    await Promise.all([
      initSalesTrendChart(),
      initSalesAmountChart(),
      initTopModelsChart(),
      initRegionSalesChart()
    ])
    ElMessage.success('数据已刷新')
  } catch (error) {
    ElMessage.error('刷新数据失败')
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  refreshAllCharts()
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

// 生命周期
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
  // 销毁图表实例
  const charts = [
    salesTrendChartInstance,
    salesAmountChartInstance,
    topModelsChartInstance,
    regionSalesChartInstance,
    comparisonChartInstance,
    forecastChartInstance,
    kpiTrendChartInstance
  ]

  charts.forEach(chart => chart?.dispose())

  window.removeEventListener('resize', handleResize)
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

<template>
  <div class="sales-forecast">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>智能销售预测</h2>
          <p>基于历史数据与市场环境的多维度销售预测分析</p>
        </div>
        <div class="header-actions">
          <el-button type="warning" :icon="Setting" @click="showAdvancedConfig = true">
            高级配置
          </el-button>
          <el-button type="primary" :icon="Refresh" @click="refreshData" :loading="loading">
            刷新数据
          </el-button>
          <el-button
            type="success"
            :icon="Download"
            @click="exportResults"
            :disabled="!predictionResults"
          >
            导出报告
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 预测场景选择 -->
    <el-card shadow="never" class="scenario-card">
      <template #header>
        <span>预测场景配置</span>
      </template>
      <div class="scenario-tabs">
        <el-radio-group
          v-model="forecastScenario"
          @change="handleScenarioChange"
          class="scenario-group"
        >
          <el-radio-button value="normal">
            <el-icon><TrendCharts /></el-icon>
            <span>常规预测</span>
          </el-radio-button>
          <el-radio-button value="newProduct">
            <el-icon><Plus /></el-icon>
            <span>新品上市</span>
          </el-radio-button>
          <el-radio-button value="promotion">
            <el-icon><Promotion /></el-icon>
            <span>促销活动</span>
          </el-radio-button>
          <el-radio-button value="competitor">
            <el-icon><Warning /></el-icon>
            <span>竞品冲击</span>
          </el-radio-button>
          <el-radio-button value="seasonal">
            <el-icon><Calendar /></el-icon>
            <span>季节调整</span>
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 场景特定配置 -->
      <div class="scenario-config" v-if="forecastScenario !== 'normal'">
        <!-- 新品上市配置 -->
        <div v-if="forecastScenario === 'newProduct'" class="config-section">
          <h4>新品上市参数</h4>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="上市时间:">
                <el-date-picker v-model="scenarioConfig.newProduct.launchDate" type="month" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="预期市场接受度:">
                <el-select v-model="scenarioConfig.newProduct.marketAcceptance">
                  <el-option label="保守(30%)" value="conservative" />
                  <el-option label="乐观(60%)" value="optimistic" />
                  <el-option label="激进(90%)" value="aggressive" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="同类产品替换率:">
                <el-slider
                  v-model="scenarioConfig.newProduct.replacementRate"
                  :min="0"
                  :max="100"
                  show-input
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 促销活动配置 -->
        <div v-if="forecastScenario === 'promotion'" class="config-section">
          <h4>促销活动参数</h4>
          <el-row :gutter="16">
            <el-col :span="6">
              <el-form-item label="活动类型:">
                <el-select v-model="scenarioConfig.promotion.type">
                  <el-option label="价格折扣" value="discount" />
                  <el-option label="置换补贴" value="trade_in" />
                  <el-option label="金融方案" value="financing" />
                  <el-option label="礼品赠送" value="gift" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="折扣幅度:">
                <el-input-number
                  v-model="scenarioConfig.promotion.discountRate"
                  :min="0"
                  :max="50"
                />
                <span>%</span>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="活动时长:">
                <el-input-number v-model="scenarioConfig.promotion.duration" :min="1" :max="12" />
                <span>月</span>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="预期提升:">
                <el-input-number
                  v-model="scenarioConfig.promotion.expectedLift"
                  :min="0"
                  :max="200"
                />
                <span>%</span>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 竞品冲击配置 -->
        <div v-if="forecastScenario === 'competitor'" class="config-section">
          <h4>竞品冲击分析</h4>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="竞品类型:">
                <el-select v-model="scenarioConfig.competitor.type">
                  <el-option label="同级别新品" value="same_level" />
                  <el-option label="降维打击" value="downgrade" />
                  <el-option label="价格战" value="price_war" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="冲击强度:">
                <el-select v-model="scenarioConfig.competitor.intensity">
                  <el-option label="轻微影响" value="mild" />
                  <el-option label="中等影响" value="moderate" />
                  <el-option label="强烈冲击" value="severe" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="预期市场流失:">
                <el-slider
                  v-model="scenarioConfig.competitor.marketLoss"
                  :min="0"
                  :max="50"
                  show-input
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 季节调整配置 -->
        <div v-if="forecastScenario === 'seasonal'" class="config-section">
          <h4>季节性调整参数</h4>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="季节性强度:">
                <el-radio-group v-model="scenarioConfig.seasonal.intensity">
                  <el-radio value="weak">弱季节性</el-radio>
                  <el-radio value="normal">正常季节性</el-radio>
                  <el-radio value="strong">强季节性</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="特殊事件:">
                <el-checkbox-group v-model="scenarioConfig.seasonal.events">
                  <el-checkbox value="spring_festival">春节</el-checkbox>
                  <el-checkbox value="golden_week">国庆黄金周</el-checkbox>
                  <el-checkbox value="auto_show">车展</el-checkbox>
                  <el-checkbox value="policy_change">政策变化</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>

    <!-- 预测配置区 -->
    <el-row :gutter="20">
      <!-- 基础配置 -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="config-card">
          <template #header>
            <div class="card-header">
              <span>基础配置</span>
              <el-button type="text" @click="resetConfig" :icon="RefreshRight">重置</el-button>
            </div>
          </template>

          <el-form :model="forecastConfig" label-width="100px" class="config-form">
            <!-- 预测对象选择 -->
            <el-form-item label="预测车型">
              <el-select
                v-model="forecastConfig.carModelId"
                placeholder="选择车型"
                filterable
                @change="handleCarModelChange"
              >
                <el-option label="全部车型" :value="null" />
                <el-option
                  v-for="model in availableCarModels"
                  :key="model.carModelId"
                  :label="`${model.brandName} ${model.modelName}`"
                  :value="model.carModelId"
                />
              </el-select>
            </el-form-item>

            <!-- 地区选择 -->
            <el-form-item label="预测地区">
              <el-select
                v-model="forecastConfig.regionId"
                placeholder="选择地区"
                filterable
                @change="handleRegionChange"
              >
                <el-option label="全国" :value="null" />
                <el-option
                  v-for="region in availableRegions"
                  :key="region.regionId"
                  :label="region.regionName"
                  :value="region.regionId"
                />
              </el-select>
            </el-form-item>

            <!-- 预测周期 -->
            <el-form-item label="预测周期">
              <el-select v-model="forecastConfig.period" placeholder="选择预测周期">
                <el-option label="未来3个月" value="3M" />
                <el-option label="未来6个月" value="6M" />
                <el-option label="未来12个月" value="12M" />
                <el-option label="未来24个月" value="24M" />
              </el-select>
            </el-form-item>

            <!-- 预测模型选择 -->
            <el-form-item label="预测模型">
              <el-radio-group v-model="forecastConfig.modelType" @change="handleModelChange">
                <el-radio value="ARIMA">ARIMA</el-radio>
                <el-radio value="Prophet">Prophet</el-radio>
              </el-radio-group>
              <div class="model-description" style="margin-top: 8px; font-size: 12px; color: #666">
                <div v-if="forecastConfig.modelType === 'ARIMA'">
                  ARIMA模型适用于平稳时间序列的短期预测，精度较高
                </div>
                <div v-else-if="forecastConfig.modelType === 'Prophet'">
                  Prophet模型擅长处理季节性和节假日效应，适合长期预测
                </div>
              </div>
            </el-form-item>

            <!-- 操作按钮 -->
            <el-form-item class="action-buttons">
              <el-button
                type="primary"
                @click="startPrediction"
                :loading="predicting"
                :disabled="!canPredict"
                size="large"
              >
                开始预测
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 预测结果展示 -->
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="result-card">
          <template #header>
            <div class="card-header">
              <span>预测结果</span>
              <div class="result-actions">
                <el-tag v-if="predictionResults" type="success">
                  拟合优度: {{ fitScore.toFixed(4) }}
                </el-tag>
                <el-button
                  v-if="predictionResults"
                  type="text"
                  @click="toggleDataLabels"
                  :icon="View"
                >
                  {{ showDataLabels ? '隐藏' : '显示' }}数据标签
                </el-button>
                <el-button
                  v-if="predictionResults"
                  type="text"
                  @click="showBusinessInsights = true"
                  :icon="DataAnalysis"
                >
                  业务洞察
                </el-button>
              </div>
            </div>
          </template>

          <!-- 预测图表 -->
          <div
            v-if="predictionResults"
            ref="forecastChart"
            class="forecast-chart"
            v-loading="predicting"
          ></div>

          <!-- 空状态 -->
          <el-empty
            v-else
            description="请配置预测参数并开始预测"
            :image-size="200"
            class="empty-state"
          >
            <template #image>
              <el-icon size="100" color="#dcdfe6"><TrendCharts /></el-icon>
            </template>
          </el-empty>
        </el-card>
      </el-col>
    </el-row>

    <!-- 业务分析面板 -->
    <div v-if="predictionResults">
      <!-- 关键预测指标 -->
      <el-card shadow="never" class="metrics-card">
        <template #header>
          <span>关键预测指标</span>
        </template>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="6">
            <div class="metric-item">
              <div class="metric-icon predicted-sales">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="metric-details">
                <div class="metric-value">{{ predictedTotalSales.toLocaleString() }}</div>
                <div class="metric-label">预测总销量</div>
                <div class="metric-change" :class="salesChangeType">
                  {{ salesGrowth >= 0 ? '+' : '' }}{{ salesGrowth.toFixed(1) }}% vs 历史
                </div>
              </div>
            </div>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <div class="metric-item">
              <div class="metric-icon inventory-suggestion">
                <el-icon><Box /></el-icon>
              </div>
              <div class="metric-details">
                <div class="metric-value">{{ recommendedInventory.toLocaleString() }}</div>
                <div class="metric-label">库存数量建议</div>
                <div class="metric-benchmark">安全库存: {{ safetyStock.toLocaleString() }} 台</div>
              </div>
            </div>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <div class="metric-item">
              <div class="metric-icon revenue-forecast">
                <el-icon><Money /></el-icon>
              </div>
              <div class="metric-details">
                <div class="metric-value">{{ (predictedRevenue / 10000).toFixed(0) }}</div>
                <div class="metric-label">预测销售额(万)</div>
                <div class="metric-trend">均价: {{ avgPrice.toFixed(1) }}万</div>
              </div>
            </div>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <div class="metric-item">
              <div class="metric-icon risk-level">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="metric-details">
                <div class="metric-value" :class="riskLevel.type">{{ riskLevel.text }}</div>
                <div class="metric-label">预测风险等级</div>
                <div class="metric-confidence">置信度: {{ (fitScore * 100).toFixed(1) }}%</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 预测数据详细分析 -->
      <el-card shadow="never" class="analysis-card">
        <template #header>
          <div class="card-header">
            <span>预测数据分析</span>
            <el-radio-group v-model="analysisView" size="small">
              <el-radio-button value="trend">趋势分析</el-radio-button>
              <el-radio-button value="monthly">月度分解</el-radio-button>
              <el-radio-button value="confidence">置信区间</el-radio-button>
            </el-radio-group>
          </div>
        </template>

        <div class="analysis-content">
          <!-- 趋势分析视图 -->
          <div v-if="analysisView === 'trend'" class="trend-analysis">
            <el-row :gutter="16">
              <el-col :span="8">
                <div class="trend-metric">
                  <div class="trend-label">上升趋势</div>
                  <div
                    class="trend-value"
                    :class="trendDirection === 'up' ? 'positive' : 'neutral'"
                  >
                    {{ trendDirection === 'up' ? '强劲' : '平稳' }}
                  </div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="trend-metric">
                  <div class="trend-label">波动性</div>
                  <div class="trend-value">
                    {{
                      calculateVolatility() < 100 ? '低' : calculateVolatility() < 200 ? '中' : '高'
                    }}
                  </div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="trend-metric">
                  <div class="trend-label">预测可靠性</div>
                  <div class="trend-value" :class="fitScore >= 0.8 ? 'positive' : 'warning'">
                    {{ fitScore >= 0.9 ? '很高' : fitScore >= 0.8 ? '较高' : '一般' }}
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- 月度分解视图 -->
          <div v-if="analysisView === 'monthly'" class="monthly-breakdown">
            <el-table :data="monthlyBreakdownData" style="width: 100%">
              <el-table-column prop="month" label="月份" width="120" />
              <el-table-column prop="predictedSales" label="预测销量" width="120">
                <template #default="{ row }">
                  {{ row.predictedSales?.toLocaleString() }} 台
                </template>
              </el-table-column>
              <el-table-column prop="growthRate" label="环比增长" width="120">
                <template #default="{ row }">
                  <span :class="getGrowthClass(row.growthRate)">
                    {{
                      row.growthRate !== undefined
                        ? `${row.growthRate >= 0 ? '+' : ''}${row.growthRate.toFixed(1)}%`
                        : '-'
                    }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="confidence" label="置信度" width="100">
                <template #default="{ row }">
                  <el-progress
                    :percentage="row.confidence"
                    :color="getConfidenceColor(row.confidence)"
                    :show-text="false"
                  />
                  <span class="confidence-text">{{ row.confidence }}%</span>
                </template>
              </el-table-column>
              <el-table-column prop="riskLevel" label="风险评估">
                <template #default="{ row }">
                  <el-tag :type="getRiskTagType(row.riskLevel)">
                    {{ row.riskLevel }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 置信区间视图 -->
          <div v-if="analysisView === 'confidence'" class="confidence-analysis">
            <div class="confidence-summary">
              <el-alert
                :title="`预测结果置信度为 ${(fitScore * 100).toFixed(1)}%`"
                :type="fitScore >= 0.8 ? 'success' : fitScore >= 0.6 ? 'warning' : 'error'"
                :description="getConfidenceDescription()"
                show-icon
                :closable="false"
              />
            </div>

            <div
              class="confidence-chart"
              ref="confidenceChart"
              style="height: 300px; margin-top: 20px"
            ></div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 高级配置弹窗 -->
    <el-dialog
      v-model="showAdvancedConfig"
      title="高级预测配置"
      width="70%"
      :before-close="handleAdvancedConfigClose"
    >
      <div class="advanced-config-content">
        <el-tabs v-model="activeAdvancedTab">
          <el-tab-pane label="外部因素" name="external">
            <div class="external-factors">
              <h4>宏观经济因素</h4>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="GDP增长率:">
                    <el-input-number
                      v-model="externalFactors.gdpGrowth"
                      :min="-5"
                      :max="15"
                      :step="0.1"
                    />
                    <span>%</span>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="消费信心指数:">
                    <el-input-number
                      v-model="externalFactors.consumerConfidence"
                      :min="0"
                      :max="200"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="汽车购置税率:">
                    <el-input-number
                      v-model="externalFactors.purchaseTax"
                      :min="0"
                      :max="20"
                      :step="0.5"
                    />
                    <span>%</span>
                  </el-form-item>
                </el-col>
              </el-row>

              <h4>行业特定因素</h4>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="新能源政策影响:">
                    <el-select v-model="externalFactors.evPolicy">
                      <el-option label="强力推进" value="strong" />
                      <el-option label="稳步推进" value="moderate" />
                      <el-option label="政策收紧" value="restrictive" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="供应链稳定性:">
                    <el-rate v-model="externalFactors.supplyChain" show-score />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="原材料价格趋势:">
                    <el-select v-model="externalFactors.materialPrice">
                      <el-option label="上涨" value="rising" />
                      <el-option label="稳定" value="stable" />
                      <el-option label="下降" value="falling" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </div>
          </el-tab-pane>

          <el-tab-pane label="模型参数" name="model">
            <el-collapse v-model="activeCollapseAdvanced">
              <el-collapse-item title="ARIMA模型参数" name="arima">
                <div class="arima-params">
                  <el-row :gutter="16">
                    <el-col :span="8">
                      <el-form-item label="p值(自回归项):">
                        <el-input-number v-model="forecastConfig.arimaParams.p" :min="0" :max="5" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="d值(差分次数):">
                        <el-input-number v-model="forecastConfig.arimaParams.d" :min="0" :max="2" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="q值(移动平均项):">
                        <el-input-number v-model="forecastConfig.arimaParams.q" :min="0" :max="5" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                </div>
              </el-collapse-item>

              <el-collapse-item title="Prophet模型参数" name="prophet">
                <div class="prophet-params">
                  <el-row :gutter="16">
                    <el-col :span="8">
                      <el-form-item label="季节性开关:">
                        <el-switch v-model="forecastConfig.prophetParams.seasonality" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="趋势变化点数量:">
                        <el-input-number
                          v-model="forecastConfig.prophetParams.changepoints"
                          :min="1"
                          :max="20"
                        />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="置信区间:">
                        <el-slider
                          v-model="forecastConfig.prophetParams.confidence"
                          :min="80"
                          :max="99"
                          show-input
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>
                </div>
              </el-collapse-item>
            </el-collapse>
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAdvancedConfig = false">取消</el-button>
          <el-button @click="resetAdvancedConfig">重置</el-button>
          <el-button type="primary" @click="applyAdvancedConfig">应用配置</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 业务洞察弹窗 -->
    <el-dialog v-model="showBusinessInsights" title="业务洞察与建议" width="80%">
      <div class="business-insights-content">
        <el-row :gutter="20">
          <el-col :xs="24" :md="8" v-for="insight in businessInsights" :key="insight.id">
            <div class="insight-card" :class="insight.type">
              <div class="insight-header">
                <el-icon><component :is="insight.icon" /></el-icon>
                <span class="insight-title">{{ insight.title }}</span>
              </div>
              <div class="insight-content">
                <p>{{ insight.content }}</p>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  RefreshRight,
  View,
  TrendCharts,
  DataLine,
  Aim,
  Sort,
  Setting,
  Plus,
  Promotion,
  Warning,
  Calendar,
  DataAnalysis,
  Box,
  Money,
  QuestionFilled,
  InfoFilled,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()

// =============================================
// 📊 基础数据层 - 直接从API获取
// =============================================

// 接口定义
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

interface Region {
  regionId: number
  regionName: string
  parentRegionId?: number | null
  parentRegionName?: string | null
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

interface Config {
  configId: number
  carModelId: number
  configName: string
  configValue: string
}

interface FuelEconomy {
  carModelId: number
  modelName: string
  brandName: string
  fuelConsumption: number
  fuelType: string
}

// 基础数据存储
interface BaseData {
  carModels: CarModel[]
  regions: Region[]
  topLevelRegions: Region[]
  nonTopLevelRegions: Region[]
  saleRecords: SaleRecord[]
  opinions: Opinion[]
  configs: Config[]
  fuelEconomies: FuelEconomy[]
}

// 预测API返回的详细数据
interface ARIMADetailResult {
  historicalData: number[]
  fittedValues: number[]
  forecastValues: number[]
  forecastUpperBounds: number[]
  forecastLowerBounds: number[]
  residuals: number[]
  mape: number
  confidenceLevel: number
  forecastStartIndex: number
  completeTimeSeries: number[]
  completeFittedSeries: number[]
  historicalDataCount: number
  forecastDataCount: number
}

interface ProphetDetailResult {
  historicalData: number[]
  fittedValues: number[]
  forecastValues: number[]
  trendComponent: number[]
  seasonalComponent: number[]
  residuals: number[]
  mape: number
  forecastStartIndex: number
  completeTimeSeries: number[]
  completeFittedSeries: number[]
  historicalDataCount: number
  forecastDataCount: number
}

// =============================================
// 🧮 计算数据层 - 基于基础数据计算
// =============================================

interface ForecastConfig {
  carModelId: number | null
  regionId: number | null
  modelType: 'ARIMA' | 'Prophet'
  period: string
  arimaParams: {
    p: number
    d: number
    q: number
  }
  prophetParams: {
    seasonality: boolean
    changepoints: number
    confidence: number
  }
}

interface PredictionData {
  date: string
  value: number
  isHistorical: boolean
  fitted?: number
  upper?: number
  lower?: number
  trend?: number
  seasonal?: number
  residual?: number
}

interface ProcessedPredictionResult {
  historicalData: PredictionData[]
  forecastData: PredictionData[]
  allData: PredictionData[]
  modelMetrics: {
    mape: number
    confidenceLevel: number
    fitScore: number
    modelType: string
  }
  components?: {
    trend: number[]
    seasonal: number[]
    residuals: number[]
  }
}

interface PredictionRecord {
  predId: number
  carModelId: number
  carModelName: string
  regionId: number
  regionName: string
  scenario: string
  modelType: string
  period: string
  fitScore: number
  createTime: string
  predResult: string
}

interface BusinessInsight {
  id: string
  type: 'opportunity' | 'risk' | 'recommendation'
  icon: string
  title: string
  content: string
}

// =============================================
// 📈 业务指标层 - 最终展示的KPI
// =============================================

interface BusinessMetrics {
  predictedTotalSales: number
  salesGrowth: number
  recommendedInventory: number
  safetyStock: number
  predictedRevenue: number
  avgPrice: number
  riskLevel: {
    type: 'success' | 'warning' | 'danger'
    text: string
  }
  marketVolatility: number
  confidenceLevel: number
  modelAccuracy: number
  trendStrength: number
  seasonalityIndex: number
}

// =============================================
// 🎛️ 响应式数据
// =============================================

const loading = ref(false)
const predicting = ref(false)
const saving = ref(false)
const showDataLabels = ref(false)
const showAdvancedConfig = ref(false)
const showBusinessInsights = ref(false)
const activeAdvancedTab = ref('external')
const activeCollapseAdvanced = ref<string[]>(['arima'])
const analysisView = ref('trend')

// 基础数据存储
const baseData = ref<BaseData>({
  carModels: [],
  regions: [],
  topLevelRegions: [],
  nonTopLevelRegions: [],
  saleRecords: [],
  opinions: [],
  configs: [],
  fuelEconomies: [],
})

// 计算后的业务数据
const availableCarModels = ref<CarModel[]>([])
const availableRegions = ref<Region[]>([])
const predictionResult = ref<ProcessedPredictionResult | null>(null)
const predictionHistory = ref<PredictionRecord[]>([])
const monthlyBreakdownData = ref<any[]>([])

// 预测场景配置
const forecastScenario = ref<'normal' | 'newProduct' | 'promotion' | 'competitor' | 'seasonal'>(
  'normal',
)

const scenarioConfig = reactive({
  newProduct: {
    launchDate: new Date(),
    marketAcceptance: 'optimistic',
    replacementRate: 30,
  },
  promotion: {
    type: 'discount',
    discountRate: 10,
    duration: 3,
    expectedLift: 25,
  },
  competitor: {
    type: 'same_level',
    intensity: 'moderate',
    marketLoss: 15,
  },
  seasonal: {
    intensity: 'normal',
    events: [] as string[],
  },
})

// 预测配置
const forecastConfig = ref<ForecastConfig>({
  carModelId: null,
  regionId: null,
  modelType: 'ARIMA',
  period: '6M',
  arimaParams: {
    p: 1,
    d: 1,
    q: 1,
  },
  prophetParams: {
    seasonality: true,
    changepoints: 5,
    confidence: 95,
  },
})

// 高级配置
const externalFactors = reactive({
  gdpGrowth: 5.2,
  consumerConfidence: 115,
  purchaseTax: 10,
  evPolicy: 'moderate',
  supplyChain: 4,
  materialPrice: 'stable',
})

const pricingSensitivity = reactive({
  priceChange: 0,
  elasticity: -1.2,
})

// 图表实例
const forecastChart = ref<HTMLDivElement>()
const confidenceChart = ref<HTMLDivElement>()
let forecastChartInstance: echarts.ECharts | null = null
let confidenceChartInstance: echarts.ECharts | null = null

// =============================================
// 🌐 API调用函数 - 基础数据获取
// =============================================

const fetchCarModels = async (): Promise<CarModel[]> => {
  try {
    console.log('🚀 正在获取车型列表...')
    const response = await axios.get('/api/car-models')

    if (response.data.status === 200 && response.data.data) {
      console.log('✅ 获取车型数据成功:', response.data.data.length, '个车型')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ 获取车型列表失败:', error)
    ElMessage.error('车型数据加载失败')
    throw error
  }
}

const fetchRegions = async (): Promise<Region[]> => {
  try {
    console.log('🚀 正在获取地区信息...')
    const response = await axios.get('/api/regions')

    if (response.data.status === 200 && response.data.data) {
      console.log('✅ 获取地区数据成功:', response.data.data.length, '个地区')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ 获取地区信息失败:', error)
    ElMessage.error('地区数据加载失败')
    throw error
  }
}

const fetchTopLevelRegions = async (): Promise<Region[]> => {
  try {
    console.log('🚀 正在获取省份信息...')
    const response = await axios.get('/api/regions/top-level')

    if (response.data.status === 200 && response.data.data) {
      console.log('✅ 获取省份数据成功:', response.data.data.length, '个省份')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ 获取省份信息失败:', error)
    ElMessage.error('省份数据加载失败')
    throw error
  }
}

const fetchNonTopLevelRegions = async (): Promise<Region[]> => {
  try {
    console.log('🚀 正在获取城市信息...')
    const response = await axios.get('/api/regions/non-top-level')

    if (response.data.status === 200 && response.data.data) {
      console.log('✅ 获取城市信息成功:', response.data.data.length, '个城市')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ 获取城市信息失败:', error)
    ElMessage.error('城市数据加载失败')
    throw error
  }
}

const fetchSaleRecords = async (params?: {
  carModelId?: number
  regionId?: number
}): Promise<SaleRecord[]> => {
  try {
    console.log('🚀 正在获取销售记录...')
    let url = '/api/sale-records'

    if (params?.carModelId && params?.regionId) {
      url = `/api/sale-records?carModelId=${params.carModelId}&regionId=${params.regionId}`
    } else if (params?.carModelId) {
      url = `/api/sale-records?carModelId=${params.carModelId}`
    } else if (params?.regionId) {
      url = `/api/sale-records?regionId=${params.regionId}`
    }

    const response = await axios.get(url)

    if (response.data.status === 200 && response.data.data) {
      console.log('✅ 获取销售记录成功:', response.data.data.length, '条记录')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ 获取销售记录失败:', error)
    ElMessage.error('销售数据加载失败')
    throw error
  }
}

const fetchOpinions = async (): Promise<Opinion[]> => {
  try {
    console.log('🚀 正在获取口碑数据...')
    const response = await axios.get('/api/opinions')

    if (response.data.status === 200 && response.data.data) {
      console.log('✅ 获取口碑数据成功:', response.data.data.length, '条评价')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ 获取口碑数据失败:', error)
    ElMessage.error('口碑数据加载失败')
    throw error
  }
}

const fetchConfigs = async (carModelId: number): Promise<Config[]> => {
  try {
    console.log('🚀 正在获取车型配置...')
    const response = await axios.get(`/api/configs?carModelId=${carModelId}`)

    if (response.data.status === 200 && response.data.data) {
      console.log('✅ 获取配置数据成功:', response.data.data.length, '个配置')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ 获取配置数据失败:', error)
    return []
  }
}

const fetchFuelEconomy = async (carModelId: number): Promise<FuelEconomy | null> => {
  try {
    console.log('🚀 正在获取油耗信息...')
    const response = await axios.get(`/api/fuel-economy/car-model/${carModelId}`)

    if (response.data.status === 200 && response.data.data) {
      console.log('✅ 获取油耗数据成功')
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ 获取油耗数据失败:', error)
    return null
  }
}

// =============================================
// 🔮 预测API调用函数 - 使用新的详细API
// =============================================

const fetchARIMADetailPrediction = async (config: {
  carModelId: number
  regionId: number
  months: number
  p?: number
  d?: number
  q?: number
}): Promise<ARIMADetailResult> => {
  try {
    console.log('🔮 开始ARIMA详细预测...')
    const { p, d, q } = config
    let url = `/api/prediction/ARIMA/detail?carModelId=${config.carModelId}&regionId=${config.regionId}&months=${config.months}`

    if (p !== undefined) url += `&p=${p}`
    if (d !== undefined) url += `&d=${d}`
    if (q !== undefined) url += `&q=${q}`

    const response = await axios.get(url)

    if (response.data.status === 200 && response.data.data) {
      console.log('✅ ARIMA预测完成，数据详情:', {
        历史数据点数: response.data.data.historicalDataCount,
        预测数据点数: response.data.data.forecastDataCount,
        MAPE: response.data.data.mape,
        置信水平: response.data.data.confidenceLevel,
      })
      return response.data.data
    } else {
      throw new Error(`ARIMA预测API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ ARIMA预测失败:', error)
    throw error
  }
}

const fetchProphetDetailPrediction = async (config: {
  carModelId: number
  regionId: number
  months: number
}): Promise<ProphetDetailResult> => {
  try {
    console.log('🔮 开始Prophet详细预测...')
    const url = `/api/prediction/Prophet/detail?carModelId=${config.carModelId}&regionId=${config.regionId}&months=${config.months}`

    const response = await axios.get(url)

    if (response.data.status === 200 && response.data.data) {
      console.log('✅ Prophet预测完成，数据详情:', {
        历史数据点数: response.data.data.historicalDataCount,
        预测数据点数: response.data.data.forecastDataCount,
        MAPE: response.data.data.mape,
      })
      return response.data.data
    } else {
      throw new Error(`Prophet预测API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('❌ Prophet预测失败:', error)
    throw error
  }
}

// =============================================
// 📊 数据加载函数
// =============================================

const loadAllBaseData = async () => {
  try {
    console.log('📊 开始加载基础数据...')

    const [carModels, regions, topLevelRegions, nonTopLevelRegions, saleRecords, opinions] =
      await Promise.all([
        fetchCarModels(),
        fetchRegions(),
        fetchTopLevelRegions(),
        fetchNonTopLevelRegions(),
        fetchSaleRecords(),
        fetchOpinions(),
      ])

    baseData.value = {
      carModels,
      regions,
      topLevelRegions,
      nonTopLevelRegions,
      saleRecords,
      opinions,
      configs: [],
      fuelEconomies: [],
    }

    console.log('📊 基础数据加载完成:', {
      车型数量: carModels.length,
      地区数量: regions.length,
      省份数量: topLevelRegions.length,
      城市数量: nonTopLevelRegions.length,
      销售记录数量: saleRecords.length,
      口碑数量: opinions.length,
    })

    // 更新可用选项
    availableCarModels.value = carModels
    availableRegions.value = [...topLevelRegions, ...nonTopLevelRegions]

    ElMessage.success('基础数据加载完成')
  } catch (error) {
    console.error('❌ 基础数据加载失败:', error)
    ElMessage.error('数据加载失败，请检查网络连接')
    throw error
  }
}

// =============================================
// 🧮 数据处理函数
// =============================================

const processDetailPredictionResult = (
  rawResult: ARIMADetailResult | ProphetDetailResult,
  modelType: 'ARIMA' | 'Prophet',
): ProcessedPredictionResult => {
  console.log('🧮 处理详细预测结果...')

  // 生成时间序列日期
  const startDate = new Date()
  startDate.setMonth(startDate.getMonth() - rawResult.historicalDataCount + 1)

  const dates: string[] = []
  for (let i = 0; i < rawResult.completeTimeSeries.length; i++) {
    const date = new Date(startDate)
    date.setMonth(date.getMonth() + i)
    dates.push(date.toISOString().slice(0, 7)) // YYYY-MM格式
  }

  // 处理历史数据
  const historicalData: PredictionData[] = []
  for (let i = 0; i < rawResult.historicalDataCount; i++) {
    historicalData.push({
      date: dates[i],
      value: rawResult.historicalData[i],
      fitted: rawResult.fittedValues[i],
      isHistorical: true,
      residual: rawResult.residuals[i],
      trend: 'trendComponent' in rawResult ? rawResult.trendComponent[i] : undefined,
      seasonal: 'seasonalComponent' in rawResult ? rawResult.seasonalComponent[i] : undefined,
    })
  }

  // 处理预测数据
  const forecastData: PredictionData[] = []
  for (let i = 0; i < rawResult.forecastDataCount; i++) {
    const dataIndex = rawResult.historicalDataCount + i
    forecastData.push({
      date: dates[dataIndex],
      value: rawResult.forecastValues[i],
      fitted: rawResult.forecastValues[i],
      isHistorical: false,
      upper: 'forecastUpperBounds' in rawResult ? rawResult.forecastUpperBounds[i] : undefined,
      lower: 'forecastLowerBounds' in rawResult ? rawResult.forecastLowerBounds[i] : undefined,
    })
  }

  // 合并所有数据
  const allData = [...historicalData, ...forecastData]

  // 计算模型指标
  const modelMetrics = {
    mape: rawResult.mape,
    confidenceLevel: 'confidenceLevel' in rawResult ? rawResult.confidenceLevel : 0.95,
    fitScore: Math.max(0, (100 - rawResult.mape) / 100), // 基于MAPE计算拟合分数
    modelType,
  }

  // 组件分析（仅Prophet模型有）
  const components =
    'trendComponent' in rawResult
      ? {
          trend: rawResult.trendComponent,
          seasonal: rawResult.seasonalComponent,
          residuals: rawResult.residuals,
        }
      : undefined

  const result: ProcessedPredictionResult = {
    historicalData,
    forecastData,
    allData,
    modelMetrics,
    components,
  }

  console.log('🧮 预测结果处理完成:', {
    历史数据点: historicalData.length,
    预测数据点: forecastData.length,
    模型精度: modelMetrics.mape,
    拟合分数: modelMetrics.fitScore,
  })

  return result
}

const calculateBusinessMetrics = (): BusinessMetrics => {
  console.log('📈 计算业务指标...')

  if (!predictionResult.value) {
    return {
      predictedTotalSales: 0,
      salesGrowth: 0,
      recommendedInventory: 0,
      safetyStock: 0,
      predictedRevenue: 0,
      avgPrice: 0,
      riskLevel: { type: 'warning', text: '暂无数据' },
      marketVolatility: 0,
      confidenceLevel: 0,
      modelAccuracy: 0,
      trendStrength: 0,
      seasonalityIndex: 0,
    }
  }

  const { historicalData, forecastData, modelMetrics, components } = predictionResult.value

  // 计算预测总销量
  const predictedTotalSales = forecastData.reduce((sum, item) => sum + item.value, 0)

  // 计算销量增长率
  const avgHistorical =
    historicalData.reduce((sum, item) => sum + item.value, 0) / historicalData.length
  const avgForecast = predictedTotalSales / forecastData.length
  const salesGrowth = avgHistorical > 0 ? ((avgForecast - avgHistorical) / avgHistorical) * 100 : 0

  // 计算平均价格（基于选中车型）
  const selectedCarModel = baseData.value.carModels.find(
    (model) => model.carModelId === forecastConfig.value.carModelId,
  )
  const avgPrice = selectedCarModel ? selectedCarModel.officialPrice : 220000

  // 计算库存建议
  const avgMonthlySales = predictedTotalSales / forecastData.length
  const safetyFactor = salesGrowth > 10 ? 1.8 : salesGrowth < -10 ? 2.2 : 2.0
  const recommendedInventory = Math.floor(avgMonthlySales * safetyFactor)
  const safetyStock = Math.floor(avgMonthlySales * 1.5)

  // 计算预测收入
  const predictedRevenue = predictedTotalSales * avgPrice

  // 计算市场波动性
  const values = forecastData.map((item) => item.value)
  const mean = values.reduce((sum, val) => sum + val, 0) / values.length
  const variance = values.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / values.length
  const marketVolatility = Math.sqrt(variance)

  // 计算趋势强度
  const trendStrength = components?.trend
    ? calculateTrendStrength(components.trend)
    : calculateTrendStrength(values)

  // 计算季节性指数
  const seasonalityIndex = components?.seasonal ? calculateSeasonalityIndex(components.seasonal) : 0

  // 计算风险等级
  const fitScore = modelMetrics.fitScore
  let riskLevel: BusinessMetrics['riskLevel']
  if (fitScore >= 0.9 && marketVolatility < 100) {
    riskLevel = { type: 'success', text: '低风险' }
  } else if (fitScore >= 0.8 && marketVolatility < 200) {
    riskLevel = { type: 'warning', text: '中风险' }
  } else {
    riskLevel = { type: 'danger', text: '高风险' }
  }

  const result: BusinessMetrics = {
    predictedTotalSales,
    salesGrowth,
    recommendedInventory,
    safetyStock,
    predictedRevenue,
    avgPrice,
    riskLevel,
    marketVolatility,
    confidenceLevel: modelMetrics.confidenceLevel * 100,
    modelAccuracy: 100 - modelMetrics.mape,
    trendStrength,
    seasonalityIndex,
  }

  console.log('📈 业务指标计算完成:', result)
  return result
}

const calculateTrendStrength = (data: number[]): number => {
  if (data.length < 2) return 0

  const firstHalf = data.slice(0, Math.floor(data.length / 2))
  const secondHalf = data.slice(Math.floor(data.length / 2))

  const firstAvg = firstHalf.reduce((sum, val) => sum + val, 0) / firstHalf.length
  const secondAvg = secondHalf.reduce((sum, val) => sum + val, 0) / secondHalf.length

  return firstAvg > 0 ? ((secondAvg - firstAvg) / firstAvg) * 100 : 0
}

const calculateSeasonalityIndex = (seasonalData: number[]): number => {
  if (seasonalData.length === 0) return 0

  const variance =
    seasonalData.reduce((sum, val) => sum + Math.pow(val, 2), 0) / seasonalData.length
  return Math.sqrt(variance)
}

const generateMonthlyBreakdown = () => {
  if (!predictionResult.value) return []

  const { forecastData, modelMetrics } = predictionResult.value

  const breakdown = forecastData.map((item, index) => {
    const previousValue = index > 0 ? forecastData[index - 1].value : item.value
    const growthRate = previousValue > 0 ? ((item.value - previousValue) / previousValue) * 100 : 0

    // 基于模型精度计算置信度
    const baseConfidence = Math.floor(100 - modelMetrics.mape)
    const volatilityPenalty = Math.min(10, Math.abs(growthRate) / 5)
    const confidence = Math.max(60, baseConfidence - volatilityPenalty)

    // 风险评估
    let riskLevel = '低风险'
    if (Math.abs(growthRate) > 20) {
      riskLevel = '高风险'
    } else if (Math.abs(growthRate) > 10) {
      riskLevel = '中风险'
    }

    return {
      month: item.date,
      predictedSales: item.value,
      growthRate,
      confidence,
      riskLevel,
      upperBound: item.upper,
      lowerBound: item.lower,
    }
  })

  monthlyBreakdownData.value = breakdown
  return breakdown
}

const calculateVolatility = (): number => {
  if (!predictionResult.value || predictionResult.value.forecastData.length < 3) return 0

  const values = predictionResult.value.forecastData.map((item) => item.value)
  const mean = values.reduce((sum, val) => sum + val, 0) / values.length
  const variance = values.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / values.length

  return Math.sqrt(variance)
}

// =============================================
// 🧮 计算属性 - 业务指标
// =============================================

const businessMetrics = computed<BusinessMetrics>(() => calculateBusinessMetrics())

const canPredict = computed(() => {
  return (
    forecastConfig.value.carModelId && forecastConfig.value.regionId && forecastConfig.value.period
  )
})

const trendDirection = computed(() => {
  if (!predictionResult.value || predictionResult.value.forecastData.length < 2) return 'stable'

  const recent = predictionResult.value.forecastData.slice(-3)
  const first = recent[0]?.value || 0
  const last = recent[recent.length - 1]?.value || 0

  if (last > first * 1.05) return 'up'
  if (last < first * 0.95) return 'down'
  return 'stable'
})

const fitScore = computed(() => predictionResult.value?.modelMetrics.fitScore || 0)
const predictedTotalSales = computed(() => businessMetrics.value.predictedTotalSales)
const salesGrowth = computed(() => businessMetrics.value.salesGrowth)
const recommendedInventory = computed(() => businessMetrics.value.recommendedInventory)
const safetyStock = computed(() => businessMetrics.value.safetyStock)
const predictedRevenue = computed(() => businessMetrics.value.predictedRevenue)
const avgPrice = computed(() => businessMetrics.value.avgPrice / 10000) // 转换为万元
const riskLevel = computed(() => businessMetrics.value.riskLevel)
const predictionResults = computed(() => predictionResult.value?.allData || null)

const salesChangeType = computed(() => {
  if (salesGrowth.value > 5) return 'success'
  if (salesGrowth.value < -5) return 'danger'
  return 'warning'
})

const businessInsights = computed((): BusinessInsight[] => {
  const insights: BusinessInsight[] = []

  if (!predictionResult.value) return insights

  const { modelMetrics } = predictionResult.value

  // 高精度模型洞察
  if (modelMetrics.mape < 5) {
    insights.push({
      id: 'high_accuracy',
      type: 'opportunity',
      icon: 'TrendCharts',
      title: '高精度预测机会',
      content: `模型精度达到${(100 - modelMetrics.mape).toFixed(1)}%，可作为重要决策依据进行资源配置和战略规划`,
    })
  }

  if (salesGrowth.value > 10) {
    insights.push({
      id: 'strong_growth',
      type: 'opportunity',
      icon: 'TrendCharts',
      title: '强劲增长机会',
      content: `预测显示销量将增长${salesGrowth.value.toFixed(1)}%，建议加大产能投入和渠道扩张`,
    })
  }

  if (trendDirection.value === 'down' && fitScore.value > 0.85) {
    insights.push({
      id: 'market_adjustment',
      type: 'risk',
      icon: 'Warning',
      title: '市场调整风险',
      content: '高可信度预测显示市场下行趋势，建议提前调整产品策略和定价策略',
    })
  }

  if (businessMetrics.value.seasonalityIndex > 50) {
    insights.push({
      id: 'seasonal_pattern',
      type: 'recommendation',
      icon: 'Calendar',
      title: '季节性模式建议',
      content: `检测到明显的季节性波动（指数：${businessMetrics.value.seasonalityIndex.toFixed(1)}），建议制定差异化的季节性营销策略`,
    })
  }

  if (recommendedInventory.value > 0) {
    const inventoryStatus = recommendedInventory.value > safetyStock.value * 1.3 ? '充足' : '紧张'
    insights.push({
      id: 'inventory_optimization',
      type: 'recommendation',
      icon: 'Box',
      title: '库存优化建议',
      content: `建议库存${recommendedInventory.value.toLocaleString()}台，当前库存状态${inventoryStatus}。建议根据预测波动性及时调整库存策略。`,
    })
  }

  return insights
})

// =============================================
// 🎯 事件处理函数
// =============================================

const handleScenarioChange = () => {
  predictionResult.value = null
  if (forecastChartInstance) {
    forecastChartInstance.clear()
  }
  ElMessage.info(`已切换到${getScenarioText()}预测场景`)
}

const getScenarioText = () => {
  const scenarioMap = {
    normal: '常规',
    newProduct: '新品上市',
    promotion: '促销活动',
    competitor: '竞品冲击',
    seasonal: '季节调整',
  }
  return scenarioMap[forecastScenario.value]
}

const handleCarModelChange = () => {
  predictionResult.value = null
  if (forecastChartInstance) {
    forecastChartInstance.clear()
  }
}

const handleRegionChange = () => {
  predictionResult.value = null
  if (forecastChartInstance) {
    forecastChartInstance.clear()
  }
}

const handleModelChange = () => {
  ElMessage.info(`已切换到${forecastConfig.value.modelType}预测模型`)
}

const startPrediction = async () => {
  if (!canPredict.value) {
    ElMessage.warning('请完善预测配置')
    return
  }

  predicting.value = true

  try {
    const periodMonths = parseInt(forecastConfig.value.period.replace('M', ''))
    let rawResult: ARIMADetailResult | ProphetDetailResult

    // 根据模型类型调用不同的API
    if (forecastConfig.value.modelType === 'ARIMA') {
      rawResult = await fetchARIMADetailPrediction({
        carModelId: forecastConfig.value.carModelId!,
        regionId: forecastConfig.value.regionId!,
        months: periodMonths,
        p: forecastConfig.value.arimaParams.p,
        d: forecastConfig.value.arimaParams.d,
        q: forecastConfig.value.arimaParams.q,
      })
    } else {
      rawResult = await fetchProphetDetailPrediction({
        carModelId: forecastConfig.value.carModelId!,
        regionId: forecastConfig.value.regionId!,
        months: periodMonths,
      })
    }

    // 处理预测结果
    predictionResult.value = processDetailPredictionResult(
      rawResult,
      forecastConfig.value.modelType,
    )

    // 应用场景调整
    applyScenarioAdjustments()

    // 生成月度分解数据
    generateMonthlyBreakdown()

    ElMessage.success(`预测完成！模型精度: ${(100 - rawResult.mape).toFixed(1)}%`)
    await nextTick()
    await initForecastChart()
  } catch (error) {
    console.error('❌ 预测失败:', error)
    ElMessage.error('预测失败，请重试')
  } finally {
    predicting.value = false
  }
}

const applyScenarioAdjustments = () => {
  if (!predictionResult.value || forecastScenario.value === 'normal') return

  predictionResult.value.forecastData = predictionResult.value.forecastData.map(
    (prediction, index) => {
      let adjustment = 0

      switch (forecastScenario.value) {
        case 'newProduct':
          adjustment = index > 3 ? (index - 3) * 15 : 0
          break
        case 'promotion':
          adjustment =
            index <= scenarioConfig.promotion.duration
              ? (scenarioConfig.promotion.expectedLift * prediction.value) / 100
              : 0
          break
        case 'competitor':
          adjustment =
            index > 2 ? (-scenarioConfig.competitor.marketLoss * prediction.value) / 100 : 0
          break
        case 'seasonal':
          const intensity = scenarioConfig.seasonal.intensity
          const multiplier = intensity === 'strong' ? 1.5 : intensity === 'weak' ? 0.5 : 1.0
          adjustment = Math.sin((index + new Date().getMonth()) * 0.5) * 45 * multiplier
          break
      }

      return {
        ...prediction,
        value: Math.max(80, prediction.value + adjustment),
        upper: prediction.upper ? Math.max(80, prediction.upper + adjustment) : undefined,
        lower: prediction.lower ? Math.max(80, prediction.lower + adjustment) : undefined,
      }
    },
  )

  // 重新计算合并数据
  predictionResult.value.allData = [
    ...predictionResult.value.historicalData,
    ...predictionResult.value.forecastData,
  ]
}

// =============================================
// 📊 图表初始化函数
// =============================================

const initForecastChart = async () => {
  if (!forecastChart.value || !predictionResult.value) return

  await nextTick()

  if (forecastChartInstance) {
    forecastChartInstance.dispose()
  }

  forecastChartInstance = echarts.init(forecastChart.value)

  const { historicalData, forecastData, allData } = predictionResult.value
  const dates = allData.map((item) => item.date)
  const historicalValues = historicalData.map((item) => item.value)
  const fittedValues = historicalData.map((item) => item.fitted || item.value)
  const forecastValues = forecastData.map((item) => item.value)

  // 对齐数据以便在图表中正确显示
  const historicalAligned = historicalValues.concat(new Array(forecastData.length).fill(null))
  const fittedAligned = fittedValues.concat(new Array(forecastData.length).fill(null))
  const forecastAligned = new Array(historicalData.length).fill(null).concat(forecastValues)

  const series: any[] = [
    {
      name: '历史销量',
      type: 'line',
      data: historicalAligned,
      itemStyle: { color: '#409EFF' },
      lineStyle: { width: 3 },
      symbol: 'circle',
      symbolSize: 6,
      label: {
        show: showDataLabels.value,
        position: 'top',
        fontSize: 10,
      },
    },
    {
      name: '拟合值',
      type: 'line',
      data: fittedAligned,
      itemStyle: { color: '#67C23A' },
      lineStyle: { width: 2, type: 'solid' },
      symbol: 'diamond',
      symbolSize: 4,
      label: {
        show: showDataLabels.value,
        position: 'bottom',
        fontSize: 9,
      },
    },
    {
      name: '预测销量',
      type: 'line',
      data: forecastAligned,
      itemStyle: { color: '#E6A23C' },
      lineStyle: { width: 3, type: 'dashed' },
      symbol: 'diamond',
      symbolSize: 6,
      label: {
        show: showDataLabels.value,
        position: 'top',
        fontSize: 10,
      },
    },
  ]

  // 添加置信区间（仅ARIMA模型）
  if (
    forecastConfig.value.modelType === 'ARIMA' &&
    forecastData.some((item) => item.upper && item.lower)
  ) {
    const upperBound = new Array(historicalData.length)
      .fill(null)
      .concat(forecastData.map((item) => item.upper || item.value))
    const lowerBound = new Array(historicalData.length)
      .fill(null)
      .concat(forecastData.map((item) => item.lower || item.value))

    series.push({
      name: '置信区间上限',
      type: 'line',
      data: upperBound,
      lineStyle: { opacity: 0 },
      symbol: 'none',
      stack: 'confidence-band',
      areaStyle: { color: 'rgba(230, 162, 60, 0.2)' },
    })

    series.push({
      name: '置信区间下限',
      type: 'line',
      data: lowerBound,
      lineStyle: { opacity: 0 },
      symbol: 'none',
      stack: 'confidence-band',
      areaStyle: { color: 'rgba(255, 255, 255, 0.8)' },
      showInLegend: false,
    })
  }

  const option = {
    title: {
      text: `${getScenarioText()}销售预测 (${forecastConfig.value.modelType})`,
      subtext: `模型精度: ${(100 - predictionResult.value.modelMetrics.mape).toFixed(1)}%`,
      left: 'center',
      textStyle: { fontSize: 16, fontWeight: 'bold', color: '#333' },
      subtextStyle: { fontSize: 12, color: '#666' },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params: any) => {
        let result = `<div style="font-weight: bold; margin-bottom: 6px;">${params[0].axisValue}</div>`

        params.forEach((param: any) => {
          if (param.value !== null && param.value !== undefined) {
            const color = param.color
            const seriesName = param.seriesName
            const value =
              typeof param.value === 'number' ? param.value.toLocaleString() : param.value
            result += `<div style="margin-bottom: 3px;">
              <span style="display: inline-block; width: 10px; height: 10px; background: ${color}; border-radius: 50%; margin-right: 6px;"></span>
              ${seriesName}: <strong>${value}</strong> 台
            </div>`
          }
        })

        const dataIndex = params[0].dataIndex
        if (dataIndex >= historicalData.length) {
          result += `<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #eee;">
            <div style="font-size: 12px; color: #666;">预测场景: ${getScenarioText()}</div>
            <div style="font-size: 12px; color: #666;">模型: ${forecastConfig.value.modelType}</div>
            <div style="font-size: 12px; color: #666;">置信度: ${(predictionResult.value!.modelMetrics.confidenceLevel * 100).toFixed(1)}%</div>
          </div>`
        }

        return result
      },
    },
    legend: {
      data: series.filter((s) => s.showInLegend !== false).map((s) => s.name),
      top: 30,
      textStyle: { fontSize: 12 },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '20%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { fontSize: 10, rotate: 45 },
      axisTick: { alignWithLabel: true },
    },
    yAxis: {
      type: 'value',
      name: '销量(台)',
      nameLocation: 'middle',
      nameGap: 50,
      axisLabel: {
        formatter: (value: number) => {
          if (value >= 1000) {
            return (value / 1000).toFixed(1) + 'k'
          }
          return value.toString()
        },
      },
      splitLine: {
        lineStyle: { type: 'dashed', color: '#e0e6ed' },
      },
    },
    series,
    dataZoom: [
      {
        type: 'slider',
        show: true,
        start: 0,
        end: 100,
        height: 20,
      },
    ],
  }

  forecastChartInstance.setOption(option)
}

// =============================================
// 🛠️ 工具函数
// =============================================

const resetConfig = () => {
  forecastConfig.value = {
    carModelId: null,
    regionId: null,
    modelType: 'ARIMA',
    period: '6M',
    arimaParams: { p: 1, d: 1, q: 1 },
    prophetParams: { seasonality: true, changepoints: 5, confidence: 95 },
  }

  forecastScenario.value = 'normal'
  predictionResult.value = null

  if (forecastChartInstance) {
    forecastChartInstance.clear()
  }

  ElMessage.success('配置已重置')
}

const refreshData = async () => {
  loading.value = true
  try {
    await loadAllBaseData()
    ElMessage.success('数据已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

const exportResults = () => {
  if (!predictionResult.value) return

  const { allData, modelMetrics } = predictionResult.value

  const csvContent = [
    [
      '日期',
      '类型',
      '实际值/预测值',
      '拟合值',
      '置信区间上限',
      '置信区间下限',
      '预测场景',
      '模型类型',
      'MAPE',
      '置信水平',
    ],
    ...allData.map((item) => [
      item.date,
      item.isHistorical ? '历史' : '预测',
      item.value,
      item.fitted || '',
      item.upper || '',
      item.lower || '',
      getScenarioText(),
      forecastConfig.value.modelType,
      modelMetrics.mape.toFixed(4),
      (modelMetrics.confidenceLevel * 100).toFixed(1) + '%',
    ]),
  ]
    .map((row) => row.join(','))
    .join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `sales_forecast_${forecastConfig.value.modelType}_${forecastScenario.value}_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  ElMessage.success('预测结果已导出')
}

const toggleDataLabels = () => {
  showDataLabels.value = !showDataLabels.value
  if (forecastChartInstance) {
    updateChartLabels()
  }
}

const updateChartLabels = () => {
  if (!forecastChartInstance) return

  forecastChartInstance.setOption({
    series: [
      { label: { show: showDataLabels.value } },
      { label: { show: showDataLabels.value } },
      { label: { show: showDataLabels.value } },
    ],
  })
}

// 高级配置相关
const handleAdvancedConfigClose = () => {
  showAdvancedConfig.value = false
}

const resetAdvancedConfig = () => {
  Object.assign(externalFactors, {
    gdpGrowth: 5.2,
    consumerConfidence: 115,
    purchaseTax: 10,
    evPolicy: 'moderate',
    supplyChain: 4,
    materialPrice: 'stable',
  })

  Object.assign(pricingSensitivity, {
    priceChange: 0,
    elasticity: -1.2,
  })

  ElMessage.success('高级配置已重置')
}

const applyAdvancedConfig = () => {
  showAdvancedConfig.value = false
  ElMessage.success('高级配置已应用，重新预测以查看效果')
}

// 分析视图相关函数
const getGrowthClass = (growthRate: number) => {
  if (growthRate > 0) return 'success'
  if (growthRate < 0) return 'danger'
  return 'warning'
}

const getConfidenceColor = (confidence: number) => {
  if (confidence >= 80) return '#67c23a'
  if (confidence >= 60) return '#e6a23c'
  return '#f56c6c'
}

const getRiskTagType = (riskLevel: string) => {
  if (riskLevel === '低风险') return 'success'
  if (riskLevel === '中风险') return 'warning'
  return 'danger'
}

const getConfidenceDescription = () => {
  const accuracy = businessMetrics.value.modelAccuracy
  if (accuracy >= 95) {
    return '预测结果具有很高的可信度，可以作为重要的决策依据。'
  } else if (accuracy >= 90) {
    return '预测结果具有较高的可信度，建议结合其他因素综合判断。'
  } else {
    return '预测结果可信度一般，建议谨慎使用，需要更多数据验证。'
  }
}

// 窗口大小调整
const handleResize = () => {
  if (forecastChartInstance) {
    forecastChartInstance.resize()
  }
  if (confidenceChartInstance) {
    confidenceChartInstance.resize()
  }
}

// =============================================
// 🔄 生命周期
// =============================================

onMounted(async () => {
  ElMessage.success('欢迎使用智能销售预测系统！')

  try {
    await loadAllBaseData()
    window.addEventListener('resize', handleResize)
  } catch (error) {
    console.error('❌ 页面初始化失败:', error)
    ElMessage.error('初始化失败，部分功能可能不可用')
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)

  if (forecastChartInstance) {
    forecastChartInstance.dispose()
    forecastChartInstance = null
  }

  if (confidenceChartInstance) {
    confidenceChartInstance.dispose()
    confidenceChartInstance = null
  }
})
</script>

<style scoped>
.sales-forecast {
  padding: 0;
  background: #f5f5f5;
  min-height: 100vh;
}

/* 页面头部 */
.page-header {
  margin-bottom: 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
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
  color: white;
  font-size: 28px;
  font-weight: 600;
}

.header-left p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  opacity: 0.9;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.header-actions .el-button {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.header-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

/* 预测场景卡片 */
.scenario-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.scenario-tabs {
  padding: 8px 0;
}

.scenario-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.scenario-group .el-radio-button {
  margin: 0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.scenario-group .el-radio-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.scenario-group .el-radio-button__inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  min-width: 120px;
  justify-content: center;
  background: white;
  border: 2px solid #e4e7ed;
  transition: all 0.3s ease;
}

.scenario-group .el-radio-button__inner:hover {
  border-color: #409eff;
}

.scenario-group .el-radio-button.is-active .el-radio-button__inner {
  background: linear-gradient(135deg, #409eff 0%, #36cfc9 100%);
  color: white;
  border-color: #409eff;
}

.scenario-config {
  margin-top: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.config-section {
  margin-bottom: 24px;
}

.config-section h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.config-section h4::before {
  content: '';
  width: 4px;
  height: 16px;
  background: #409eff;
  margin-right: 8px;
  border-radius: 2px;
}

/* 配置卡片 */
.config-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #303133;
}

.config-form {
  padding: 8px 0;
}

.config-form .el-form-item {
  margin-bottom: 20px;
}

.config-form .el-form-item__label {
  color: #606266;
  font-weight: 500;
}

.config-form .el-select,
.config-form .el-input-number {
  width: 100%;
}

.action-buttons {
  margin-top: 24px;
  text-align: center;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
}

.action-buttons .el-button {
  margin: 0 8px;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.action-buttons .el-button--primary {
  background: linear-gradient(135deg, #409eff 0%, #36cfc9 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.action-buttons .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}

.action-buttons .el-button--success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

/* 结果卡片 */
.result-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.result-actions .el-tag {
  font-weight: 500;
}

.forecast-chart {
  width: 100%;
  height: 520px;
  min-height: 520px;
  background: white;
  border-radius: 8px;
  padding: 16px;
}

.empty-state {
  padding: 80px 20px;
  text-align: center;
}

.empty-state .el-icon {
  margin-bottom: 16px;
  opacity: 0.6;
}

/* 关键指标卡片 */
.metrics-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.metric-item {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
  cursor: pointer;
}

.metric-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  font-size: 28px;
  color: white;
  position: relative;
}

.metric-icon::after {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  background: linear-gradient(45deg, transparent, rgba(255, 255, 255, 0.3));
  z-index: -1;
}

.metric-icon.predicted-sales {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.metric-icon.inventory-turnover {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.metric-icon.revenue-forecast {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.metric-icon.risk-level {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.metric-details {
  text-align: center;
}

.metric-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 8px;
  color: #303133;
  line-height: 1;
}

.metric-value.success {
  color: #67c23a;
}

.metric-value.warning {
  color: #e6a23c;
}

.metric-value.danger {
  color: #f56c6c;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: 500;
}

.metric-change,
.metric-trend,
.metric-benchmark,
.metric-confidence {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
}

.metric-change.success {
  color: #67c23a;
}

.metric-change.danger {
  color: #f56c6c;
}

/* 添加新的库存建议样式 */
.metric-icon.inventory-suggestion {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
}

/* 高级配置弹窗 */
.advanced-config-content {
  max-height: 70vh;
  overflow-y: auto;
}

.external-factors,
.pricing-sensitivity {
  padding: 16px 0;
}

.external-factors h4,
.pricing-sensitivity h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  padding-bottom: 8px;
  border-bottom: 2px solid #f0f0f0;
}

.arima-params,
.prophet-params {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin: 8px 0;
}

.elasticity-preview {
  margin-top: 16px;
  padding: 12px;
  background: #ecf5ff;
  border-radius: 6px;
  border-left: 4px solid #409eff;
}

.elasticity-preview p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.dialog-footer {
  text-align: right;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 业务洞察弹窗 */
.business-insights-content {
  padding: 16px 0;
}

.insight-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  border: 2px solid #f0f0f0;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.insight-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #409eff, #36cfc9);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.insight-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.insight-card:hover::before {
  transform: scaleX(1);
}

.insight-card.opportunity {
  border-left-color: #67c23a;
}

.insight-card.risk {
  border-left-color: #f56c6c;
}

.insight-card.recommendation {
  border-left-color: #409eff;
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.insight-header .el-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f9ff;
  color: #409eff;
}

.insight-title {
  font-weight: 600;
  color: #303133;
  font-size: 16px;
}

.insight-content p {
  margin: 0 0 16px 0;
  color: #606266;
  line-height: 1.6;
  font-size: 14px;
}

/* 历史记录卡片 */
.history-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.history-card .el-table {
  border-radius: 8px;
  overflow: hidden;
}

/* 状态标签 */
.el-tag {
  border-radius: 6px;
  font-weight: 500;
}

/* 评分组件 */
.el-rate {
  display: inline-flex;
  align-items: center;
}

/* 滑块组件 */
.el-slider {
  margin: 8px 0;
}

/* 折叠面板 */
.el-collapse {
  border: none;
}

.el-collapse-item__header {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 8px;
  font-weight: 500;
  border: none;
}

.el-collapse-item__content {
  padding: 16px 0 0 0;
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

.metric-item,
.channel-item,
.risk-item,
.insight-card {
  animation: slideInUp 0.6s ease-out;
}

/* 加载状态优化 */
.el-loading-mask {
  border-radius: 8px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .forecast-chart {
    height: 450px;
    min-height: 450px;
  }

  .metric-icon {
    width: 50px;
    height: 50px;
    font-size: 24px;
  }

  .metric-value {
    font-size: 28px;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .header-actions {
    justify-content: center;
    flex-wrap: wrap;
  }

  .scenario-group {
    flex-direction: column;
    align-items: stretch;
  }

  .scenario-group .el-radio-button__inner {
    min-width: auto;
    justify-content: flex-start;
  }

  .forecast-chart {
    height: 380px;
    min-height: 380px;
    padding: 8px;
  }

  .metric-item {
    padding: 16px;
    text-align: center;
  }

  .metric-icon {
    width: 48px;
    height: 48px;
    font-size: 22px;
    margin: 0 auto 12px;
  }

  .metric-value {
    font-size: 24px;
  }

  .channel-metrics {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .risk-item {
    flex-direction: column;
    gap: 8px;
  }

  .insight-card {
    padding: 16px;
  }

  .config-section .el-row {
    margin: 0;
  }

  .config-section .el-col {
    margin-bottom: 12px;
  }
}

@media (max-width: 480px) {
  .sales-forecast {
    padding: 0 8px;
  }

  .header-left h2 {
    font-size: 24px;
  }

  .header-left p {
    font-size: 14px;
  }

  .action-buttons .el-button {
    display: block;
    width: 100%;
    margin: 8px 0;
  }

  .result-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .forecast-chart {
    height: 300px;
    min-height: 300px;
    padding: 4px;
  }

  .metric-value {
    font-size: 20px;
  }

  .metric-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .channel-item,
  .risk-item {
    padding: 12px;
  }

  .insight-card {
    padding: 12px;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .sales-forecast {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .config-card,
  .result-card,
  .metrics-card,
  .channel-card,
  .risk-card,
  .history-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .metric-item {
    background: #2d2d2d;
    border-color: #404040;
  }

  .channel-item {
    background: #262626;
  }

  .scenario-config {
    background: #262626;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .metric-item,
  .channel-item,
  .risk-item,
  .insight-card {
    border-width: 2px;
    border-color: #000;
  }

  .metric-value,
  .channel-name,
  .risk-title,
  .insight-title {
    font-weight: 700;
  }
}

/* 打印样式 */
@media print {
  .sales-forecast {
    background: white !important;
  }

  .header-actions,
  .action-buttons,
  .result-actions,
  .risk-actions {
    display: none !important;
  }

  .forecast-chart {
    height: 400px !important;
    break-inside: avoid;
  }

  .metric-item,
  .channel-item,
  .risk-item {
    break-inside: avoid;
    margin-bottom: 12px;
  }
}
</style>

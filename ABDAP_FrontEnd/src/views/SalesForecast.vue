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
        <span>预测场景</span>
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
                  v-for="model in carModelList"
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
                  v-for="region in regionList"
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
  <div class="model-description" style="margin-top: 8px; font-size: 12px; color: #666;">
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
              <el-button
                v-if="predictionResults"
                type="success"
                @click="savePrediction"
                :loading="saving"
                size="large"
              >
                保存结果
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
                <div class="metric-trend">单价: {{ avgPrice.toFixed(1) }}万</div>
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
            <!-- 现有的ARIMA和Prophet参数配置 -->
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

          <el-tab-pane label="价格敏感性" name="pricing">
            <div class="pricing-sensitivity">
              <h4>价格弹性分析</h4>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="价格变动幅度:">
                    <el-slider
                      v-model="pricingSensitivity.priceChange"
                      :min="-30"
                      :max="30"
                      show-input
                    />
                    <span>%</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="需求弹性系数:">
                    <el-input-number
                      v-model="pricingSensitivity.elasticity"
                      :min="-5"
                      :max="0"
                      :step="0.1"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <div class="elasticity-preview">
                <p>
                  预计价格调整 {{ pricingSensitivity.priceChange }}% 将导致销量变化
                  {{ (pricingSensitivity.priceChange * pricingSensitivity.elasticity).toFixed(1) }}%
                </p>
              </div>
            </div>
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

    <!-- 历史预测记录 -->
    <el-card v-if="predictionHistory.length > 0" shadow="never" class="history-card">
      <template #header>
        <span>历史预测记录</span>
      </template>

      <el-table :data="predictionHistory" style="width: 100%">
        <el-table-column prop="createTime" label="预测时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="carModelName" label="预测车型" />
        <el-table-column prop="regionName" label="预测地区" />
        <el-table-column prop="scenario" label="预测场景" width="100" />
        <el-table-column prop="modelType" label="模型类型" width="100" />
        <el-table-column prop="period" label="预测周期" width="100" />
        <el-table-column prop="fitScore" label="拟合优度" width="100">
          <template #default="{ row }">
            <el-tag :type="getFitScoreType(row.fitScore)">
              {{ (row.fitScore * 100).toFixed(1) }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="text" @click="loadPrediction(row)">加载</el-button>
            <el-button type="text" @click="deletePrediction(row.predId)" style="color: #f56c6c"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </el-card>
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

// 接口定义
interface CarModel {
  carModelId: number
  modelName: string
  brandName: string
}

interface Region {
  regionId: number
  regionName: string
  parentRegionId?: number
}

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
  upper?: number
  lower?: number
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

// 响应式数据
const loading = ref(false)
const predicting = ref(false)
const saving = ref(false)
const showDataLabels = ref(false)
const showAdvancedConfig = ref(false)
const showBusinessInsights = ref(false)
const activeAdvancedTab = ref('external')
const activeCollapseAdvanced = ref<string[]>(['arima'])

// 数据源
const carModelList = ref<CarModel[]>([])
const regionList = ref<Region[]>([])
const historicalData = ref<PredictionData[]>([])
const predictionResults = ref<PredictionData[] | null>(null)
const predictionHistory = ref<PredictionRecord[]>([])
const fitScore = ref(0)

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
let forecastChartInstance: echarts.ECharts | null = null

// 计算属性 - 业务指标增强

// 基础预测能力判断
const canPredict = computed(() => {
  return (
    forecastConfig.value.carModelId && forecastConfig.value.regionId && forecastConfig.value.period
  )
})

// 预测趋势方向
const trendDirection = computed(() => {
  if (!predictionResults.value || predictionResults.value.length < 2) return 'stable'

  const recent = predictionResults.value.slice(-3)
  const first = recent[0]?.value || 0
  const last = recent[recent.length - 1]?.value || 0

  if (last > first * 1.05) return 'up'
  if (last < first * 0.95) return 'down'
  return 'stable'
})

// 预测总销量
const predictedTotalSales = computed(() => {
  if (!predictionResults.value) return 0
  return predictionResults.value.reduce((sum, item) => sum + item.value, 0)
})

// 销量增长率
const salesGrowth = computed(() => {
  if (!historicalData.value.length || !predictionResults.value.length) return 0

  const avgHistorical =
    historicalData.value.reduce((sum, item) => sum + item.value, 0) / historicalData.value.length
  const avgForecast =
    predictionResults.value.reduce((sum, item) => sum + item.value, 0) /
    predictionResults.value.length

  return ((avgForecast - avgHistorical) / avgHistorical) * 100
})

const salesChangeType = computed(() => {
  if (salesGrowth.value > 5) return 'success'
  if (salesGrowth.value < -5) return 'danger'
  return 'warning'
})

// 替换为库存数量建议计算属性
const recommendedInventory = computed(() => {
  if (!predictionResults.value) return 0
  const avgMonthlySales = predictedTotalSales.value / predictionResults.value.length
  // 基于预测销量和安全系数计算建议库存
  const safetyFactor = salesGrowth.value > 10 ? 1.8 : salesGrowth.value < -10 ? 2.2 : 2.0
  return Math.floor(avgMonthlySales * safetyFactor)
})

const safetyStock = computed(() => {
  if (!predictionResults.value) return 0
  const avgMonthlySales = predictedTotalSales.value / predictionResults.value.length
  return Math.floor(avgMonthlySales * 1.5) // 安全库存为1.5倍月均销量
})

// 预测收入
const predictedRevenue = computed(() => {
  return predictedTotalSales.value * avgPrice.value
})

// 平均售价
const avgPrice = computed(() => {
  return 220000 // 假设平均售价22万
})

// 风险等级评估
const riskLevel = computed(() => {
  const score = fitScore.value
  const volatility = calculateVolatility()

  if (score >= 0.9 && volatility < 100) {
    return { type: 'success', text: '低风险' }
  } else if (score >= 0.8 && volatility < 200) {
    return { type: 'warning', text: '中风险' }
  } else {
    return { type: 'danger', text: '高风险' }
  }
})


// 业务洞察
const businessInsights = computed((): BusinessInsight[] => {
  const insights: BusinessInsight[] = []

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

  
      // 库存优化建议
  if (recommendedInventory.value > 0) {
    const inventoryStatus = recommendedInventory.value > safetyStock.value * 1.3 ? '充足' : '紧张'
    insights.push({
      id: 'inventory_optimization',
      type: 'recommendation',
      icon: 'Box',
      title: '库存优化建议',
      content: `建议库存${recommendedInventory.value.toLocaleString()}台，当前库存状态${inventoryStatus}。建议根据季节性需求和市场变化及时调整库存策略。`,
      
    })
  }

  return insights
})

// 工具函数
const calculateVolatility = () => {
  if (!predictionResults.value || predictionResults.value.length < 3) return 0

  const values = predictionResults.value.map((item) => item.value)
  const mean = values.reduce((sum, val) => sum + val, 0) / values.length
  const variance = values.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / values.length

  return Math.sqrt(variance)
}

// API调用函数 - 增强版
const fetchCarModels = async () => {
  try {
    const response = await axios.get('/api/carModels')
    if (response.data.status === 1) {
      carModelList.value = response.data.data
    } else {
      carModelList.value = generateMockCarModels()
    }
  } catch (error) {
    console.error('获取车型列表失败:', error)
    carModelList.value = generateMockCarModels()
  }
}

const fetchRegions = async () => {
  try {
    const response = await axios.get('/api/regions')
    if (response.data.status === 1) {
      regionList.value = response.data.data
    } else {
      regionList.value = generateMockRegions()
    }
  } catch (error) {
    console.error('获取地区列表失败:', error)
    regionList.value = generateMockRegions()
  }
}

const fetchHistoricalData = async () => {
  try {
    const params = {
      carModelId: forecastConfig.value.carModelId,
      regionId: forecastConfig.value.regionId,
      scenario: forecastScenario.value,
    }

    const response = await axios.get('/api/sales/historical', { params })
    if (response.data.status === 1) {
      return response.data.data
    } else {
      return generateMockHistoricalData()
    }
  } catch (error) {
    console.error('获取历史数据失败:', error)
    return generateMockHistoricalData()
  }
}

const fetchPredictionHistory = async () => {
  try {
    const response = await axios.get('/api/predictions/history')
    if (response.data.status === 1) {
      predictionHistory.value = response.data.data
    } else {
      predictionHistory.value = []
    }
  } catch (error) {
    console.error('获取预测历史失败:', error)
    predictionHistory.value = []
  }
}

// 模拟数据生成 - 增强版
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
    { carModelId: 10, modelName: 'AION Y', brandName: 'GAC' },
  ]
}

const generateMockRegions = (): Region[] => {
  return [
    { regionId: 1, regionName: '北京市' },
    { regionId: 2, regionName: '上海市' },
    { regionId: 3, regionName: '广东省' },
    { regionId: 4, regionName: '浙江省' },
    { regionId: 5, regionName: '江苏省' },
    { regionId: 6, regionName: '山东省' },
    { regionId: 7, regionName: '四川省' },
    { regionId: 8, regionName: '湖北省' },
    { regionId: 9, regionName: '河南省' },
    { regionId: 10, regionName: '湖南省' },
  ]
}

const generateMockHistoricalData = (): PredictionData[] => {
  const data: PredictionData[] = []
  const startDate = new Date('2023-01-01')
  let baseValue = 400

  for (let i = 0; i < 18; i++) {
    const date = new Date(startDate)
    date.setMonth(startDate.getMonth() + i)

    // 基础趋势
    const trend = i * 8

    // 季节性因素
    const seasonal = Math.sin(i * 0.5) * 60

    // 场景调整
    let scenarioAdjustment = 0
    if (forecastScenario.value === 'promotion' && i > 12) {
      scenarioAdjustment = 50 // 促销提升
    } else if (forecastScenario.value === 'competitor' && i > 15) {
      scenarioAdjustment = -30 // 竞品冲击
    }

    // 随机波动
    const random = (Math.random() - 0.5) * 80

    const value = baseValue + trend + seasonal + scenarioAdjustment + random

    data.push({
      date: date.toISOString().slice(0, 7),
      value: Math.floor(Math.max(100, value)),
      isHistorical: true,
    })

    baseValue += 5 // 缓慢增长
  }

  return data
}

// 事件处理函数 - 增强版
const handleScenarioChange = () => {
  predictionResults.value = null
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
  predictionResults.value = null
  if (forecastChartInstance) {
    forecastChartInstance.clear()
  }
}

const handleRegionChange = () => {
  predictionResults.value = null
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
    // 获取历史数据
    historicalData.value = await fetchHistoricalData()

    // 构建预测参数
    const params = {
        carModelId: forecastConfig.value.carModelId,
  regionId: forecastConfig.value.regionId,
  modelType: forecastConfig.value.modelType,
  period: forecastConfig.value.period,
  scenario: forecastScenario.value,
  scenarioParams: getScenarioParams(),
  externalFactors: externalFactors,
  // 根据选择的模型类型传递对应参数
  modelParams:
    forecastConfig.value.modelType === 'ARIMA'
      ? forecastConfig.value.arimaParams
      : forecastConfig.value.prophetParams,
    }

    const response = await axios.post('/api/predictions/forecast', params)

    if (response.data.status === 1) {
      predictionResults.value = response.data.data.predictions
      fitScore.value = response.data.data.fitScore

      ElMessage.success('预测完成！')
      await nextTick()
      await initForecastChart()
    } else {
      // 使用模拟预测结果
      const mockResults = generateEnhancedMockPrediction()
      predictionResults.value = mockResults
      fitScore.value = 0.82 + Math.random() * 0.15

      ElMessage.success('预测完成（使用智能模拟数据）')
      await nextTick()
      await initForecastChart()
    }
  } catch (error) {
    console.error('预测失败:', error)

    // 使用增强的模拟预测结果
    const mockResults = generateEnhancedMockPrediction()
    predictionResults.value = mockResults
    fitScore.value = 0.8 + Math.random() * 0.15

    ElMessage.warning('API调用失败，使用智能模拟预测结果')
    await nextTick()
    await initForecastChart()
  } finally {
    predicting.value = false
  }
}

const getScenarioParams = () => {
  switch (forecastScenario.value) {
    case 'newProduct':
      return scenarioConfig.newProduct
    case 'promotion':
      return scenarioConfig.promotion
    case 'competitor':
      return scenarioConfig.competitor
    case 'seasonal':
      return scenarioConfig.seasonal
    default:
      return {}
  }
}

const generateEnhancedMockPrediction = (): PredictionData[] => {
  const results: PredictionData[] = []
  const periodMonths = parseInt(forecastConfig.value.period.replace('M', ''))
  const lastHistorical = historicalData.value[historicalData.value.length - 1]
  let baseValue = lastHistorical?.value || 450

  for (let i = 1; i <= periodMonths; i++) {
    const date = new Date()
    date.setMonth(date.getMonth() + i)

    // 基础趋势
    let trend = i * 6

    // 季节性因素
    const seasonal = Math.sin((i + new Date().getMonth()) * 0.5) * 45

    // 场景特定调整
    let scenarioAdjustment = 0
    switch (forecastScenario.value) {
      case 'newProduct':
        scenarioAdjustment = i > 3 ? (i - 3) * 15 : 0 // 新品逐步上量
        break
      case 'promotion':
        scenarioAdjustment =
          i <= scenarioConfig.promotion.duration
            ? (scenarioConfig.promotion.expectedLift * baseValue) / 100
            : 0
        break
      case 'competitor':
        scenarioAdjustment = i > 2 ? (-scenarioConfig.competitor.marketLoss * baseValue) / 100 : 0
        break
      case 'seasonal':
        const intensity = scenarioConfig.seasonal.intensity
        const multiplier = intensity === 'strong' ? 1.5 : intensity === 'weak' ? 0.5 : 1.0
        scenarioAdjustment = seasonal * multiplier
        break
    }

    // 外部因素影响
    const externalImpact =
      (externalFactors.gdpGrowth - 5) * 3 + (externalFactors.consumerConfidence - 100) * 0.5

    // 随机波动
    const random = (Math.random() - 0.5) * 60

    const value = baseValue + trend + seasonal + scenarioAdjustment + externalImpact + random

    results.push({
      date: date.toISOString().slice(0, 7),
      value: Math.floor(Math.max(80, value)),
      isHistorical: false,
      upper: Math.floor(value * 1.18),
      lower: Math.floor(value * 0.82),
    })

    baseValue = value // 更新基准值
  }

  return results
}

const savePrediction = async () => {
  if (!predictionResults.value) return

  saving.value = true

  try {
    const params = {
    carModelId: forecastConfig.value.carModelId,
  regionId: forecastConfig.value.regionId,
  scenario: forecastScenario.value,
  modelType: forecastConfig.value.modelType,
  period: forecastConfig.value.period,
  // 根据模型类型保存对应参数
  modelParams: JSON.stringify(
    forecastConfig.value.modelType === 'ARIMA'
      ? forecastConfig.value.arimaParams
      : forecastConfig.value.prophetParams,
  ),
  scenarioParams: JSON.stringify(getScenarioParams()),
  externalFactors: JSON.stringify(externalFactors),
  predResult: JSON.stringify(predictionResults.value),
  fitScore: fitScore.value,
  predictedTotalSales: predictedTotalSales.value,
  salesGrowth: salesGrowth.value,
  riskLevel: riskLevel.value.text,
    }

    const response = await axios.post('/api/predictions/save', params)

    if (response.data.status === 1) {
      ElMessage.success('预测结果已保存')
      await fetchPredictionHistory()
    } else {
      throw new Error('保存失败')
    }
  } catch (error) {
    console.error('保存预测结果失败:', error)
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

const loadPrediction = (record: PredictionRecord) => {
  try {
    forecastConfig.value.carModelId = record.carModelId
    forecastConfig.value.regionId = record.regionId
    forecastConfig.value.modelType = record.modelType as any
    forecastConfig.value.period = record.period
    forecastScenario.value = record.scenario as any

    predictionResults.value = JSON.parse(record.predResult)
    fitScore.value = record.fitScore

    nextTick(() => {
      initForecastChart()
    })

    ElMessage.success('预测结果已加载')
  } catch (error) {
    console.error('加载预测结果失败:', error)
    ElMessage.error('加载失败')
  }
}

const deletePrediction = async (predId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条预测记录吗？', '确认删除', {
      type: 'warning',
    })

    const response = await axios.delete(`/api/predictions/${predId}`)

    if (response.data.status === 1) {
      ElMessage.success('删除成功')
      await fetchPredictionHistory()
    } else {
      throw new Error('删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除预测记录失败:', error)
      ElMessage.error('删除失败')
    }
  }
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


// 图表和界面控制
const resetConfig = () => {
  forecastConfig.value = {
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
  }

  forecastScenario.value = 'normal'
  predictionResults.value = null

  if (forecastChartInstance) {
    forecastChartInstance.clear()
  }

  ElMessage.success('配置已重置')
}

const refreshData = async () => {
  loading.value = true
  try {
    await Promise.all([fetchCarModels(), fetchRegions(), fetchPredictionHistory()])
    ElMessage.success('数据已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

const exportResults = () => {
  if (!predictionResults.value) return

  const csvContent = [
    ['日期', '预测值', '置信区间上限', '置信区间下限', '预测场景', '拟合优度'],
    ...predictionResults.value.map((item) => [
      item.date,
      item.value,
      item.upper || '',
      item.lower || '',
      getScenarioText(),
      fitScore.value.toFixed(4),
    ]),
  ]
    .map((row) => row.join(','))
    .join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `sales_forecast_${forecastScenario.value}_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()

  ElMessage.success('预测结果已导出')
}

const toggleDataLabels = () => {
  showDataLabels.value = !showDataLabels.value
  if (forecastChartInstance) {
    updateChartLabels()
  }
}

// 图表初始化 - 增强版
const initForecastChart = async () => {
  if (!forecastChart.value || !predictionResults.value) return

  await nextTick()

  if (forecastChartInstance) {
    forecastChartInstance.dispose()
  }

  forecastChartInstance = echarts.init(forecastChart.value)

  const allData = [...historicalData.value, ...predictionResults.value]
  const dates = allData.map((item) => item.date)
  const historicalValues = historicalData.value.map((item) => item.value)
  const forecastValues = predictionResults.value.map((item) => item.value)

  // 为预测数据添加前导null值以正确对齐
  const forecastAligned = new Array(historicalData.value.length).fill(null).concat(forecastValues)

  const option = {
    title: {
      text: `${getScenarioText()}销售预测`,
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#333',
      },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
      },
      formatter: (params: any) => {
        let result = `<div style="font-weight: bold; margin-bottom: 6px;">${params[0].axisValue}</div>`

        params.forEach((param: any) => {
          if (param.value !== null && param.value !== undefined) {
            const color = param.color
            const seriesName = param.seriesName
            const value = param.value.toLocaleString()
            result += `<div style="margin-bottom: 3px;">
              <span style="display: inline-block; width: 10px; height: 10px; background: ${color}; border-radius: 50%; margin-right: 6px;"></span>
              ${seriesName}: <strong>${value}</strong> 台
            </div>`
          }
        })

        // 添加业务指标
        if (params[0].dataIndex >= historicalData.value.length) {
          result += `<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #eee;">
            <div style="font-size: 12px; color: #666;">预测场景: ${getScenarioText()}</div>
            <div style="font-size: 12px; color: #666;">置信度: ${(fitScore.value * 100).toFixed(1)}%</div>
          </div>`
        }

        return result
      },
    },
    legend: {
      data: ['历史销量', '预测销量', '置信区间'],
      top: 30,
      textStyle: {
        fontSize: 12,
      },
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
      axisLabel: {
        fontSize: 10,
        rotate: 45,
      },
      axisTick: {
        alignWithLabel: true,
      },
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
        lineStyle: {
          type: 'dashed',
          color: '#e0e6ed',
        },
      },
    },
    series: [
      {
        name: '历史销量',
        type: 'line',
        data: historicalValues.concat(new Array(predictionResults.value.length).fill(null)),
        itemStyle: {
          color: '#409EFF',
        },
        lineStyle: {
          width: 3,
        },
        symbol: 'circle',
        symbolSize: 6,
        label: {
          show: showDataLabels.value,
          position: 'top',
          fontSize: 10,
        },
      },
      {
        name: '预测销量',
        type: 'line',
        data: forecastAligned,
        itemStyle: {
          color: '#E6A23C',
        },
        lineStyle: {
          width: 3,
          type: 'dashed',
        },
        symbol: 'diamond',
        symbolSize: 6,
        label: {
          show: showDataLabels.value,
          position: 'top',
          fontSize: 10,
        },
      },
    ],
  }

  // 添加置信区间
  if (predictionResults.value.some((item) => item.upper && item.lower)) {
    const upperBound = new Array(historicalData.value.length)
      .fill(null)
      .concat(predictionResults.value.map((item) => item.upper || item.value))
    const lowerBound = new Array(historicalData.value.length)
      .fill(null)
      .concat(predictionResults.value.map((item) => item.lower || item.value))

    option.series.push({
      name: '置信区间',
      type: 'line',
      data: upperBound,
      lineStyle: {
        opacity: 0,
      },
      symbol: 'none',
      stack: 'confidence-band',
      areaStyle: {
        color: 'rgba(230, 162, 60, 0.2)',
      },
    } as any)

    option.series.push({
      name: '置信区间下限',
      type: 'line',
      data: lowerBound,
      lineStyle: {
        opacity: 0,
      },
      symbol: 'none',
      stack: 'confidence-band',
      areaStyle: {
        color: 'rgba(255, 255, 255, 0.8)',
      },
      showInLegend: false,
    } as any)
  }

  forecastChartInstance.setOption(option)
}

const updateChartLabels = () => {
  if (!forecastChartInstance) return

  forecastChartInstance.setOption({
    series: [
      {
        label: {
          show: showDataLabels.value,
        },
      },
      {
        label: {
          show: showDataLabels.value,
        },
      },
    ],
  })
}

// 工具函数
const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getFitScoreType = (score: number) => {
  if (score >= 0.9) return 'success'
  if (score >= 0.8) return 'warning'
  return 'danger'
}

// 窗口大小调整
const handleResize = () => {
  if (forecastChartInstance) {
    forecastChartInstance.resize()
  }
}

// 生命周期
onMounted(async () => {
  ElMessage.success('欢迎使用智能销售预测系统！')

  try {
    await Promise.all([fetchCarModels(), fetchRegions(), fetchPredictionHistory()])

    window.addEventListener('resize', handleResize)
  } catch (error) {
    console.error('页面初始化失败:', error)
    ElMessage.error('初始化失败，部分功能可能不可用')
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)

  if (forecastChartInstance) {
    forecastChartInstance.dispose()
    forecastChartInstance = null
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
  .risk-actions, {
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

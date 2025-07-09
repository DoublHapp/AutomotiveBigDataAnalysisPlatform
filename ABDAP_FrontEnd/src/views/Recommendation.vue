<template>
  <div class="recommendation-analysis">
    <!-- 页面头部 -->
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>智能购车推荐</h2>
          <p>基于真实销量数据和用户口碑的AI智能推荐系统</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshAllData" :loading="loading">
            刷新数据
          </el-button>
          <el-button
            type="success"
            :icon="Download"
            @click="exportRecommendation"
            :disabled="!recommendationResult"
          >
            导出推荐
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 筛选条件卡片 -->
    <el-card v-if="baseDataLoaded" shadow="never" class="filter-card">
      <template #header>
        <div class="filter-header">
          <span>购车需求筛选</span>
          <div class="filter-stats">
            <el-button
              size="small"
              type="primary"
              @click="generateRecommendation"
              :disabled="!isStep1Valid()"
              :loading="analyzing"
            >
              <el-icon><MagicStick /></el-icon>
              生成推荐
            </el-button>
          </div>
        </div>
      </template>

      <div class="filter-content">
        <!-- 预算区间 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><Money /></el-icon>
            <span>预算区间</span>
            <span class="required">*</span>
          </div>
          <div class="filter-options budget-options">
            <div
              v-for="option in budgetOptions"
              :key="option.value"
              class="filter-option"
              :class="{ active: questionnaireData.budget === option.value }"
              @click="selectBudget(option.value)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc" v-if="option.desc">{{ option.desc }}</span>

          </div>
        </div>
      </div>
    </div>

        <!-- 车型类别 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><OfficeBuilding /></el-icon>
            <span>车型类别</span>
            <span class="required">*</span>
          </div>
          <div class="filter-options level-options">
            <div
              v-for="option in levelOptions"
              :key="option.value"
              class="filter-option"
              :class="{ active: questionnaireData.level.includes(option.value) }"
              @click="selectLevelType(option.value)"
            >
              <el-icon><component :is="option.icon" /></el-icon>
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc">{{ option.desc }}</span>

            </div>
          </div>
        </div>

        <!-- 能源类型 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><Lightning /></el-icon>
            <span>能源类型</span>
            <span class="required">*</span>
          </div>
          <div class="filter-options engine-type-options">
            <div
              v-for="option in engineTypeOptions"
              :key="option.value"
              class="filter-option"
              :class="{ active: questionnaireData.engineType === option.value }"
              @click="selectEngineType(option.value)"
            >
              <el-icon><component :is="option.icon" /></el-icon>
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc">{{ option.desc }}</span>

            </div>
          </div>
        </div>

        <!-- 乘坐人数 -->
        <div class="filter-section">
          <div class="section-label">
            <el-icon><Star /></el-icon>
            <span>乘坐人数</span>
            <span class="required">*</span>
          </div>
          <div class="filter-options seat-num-options">
            <div
              v-for="option in passengerOptions"
              :key="option.value"
              class="filter-option"
              :class="{ active: questionnaireData.seatNum === option.value }"
              @click="selectSeatNum(option.value)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-desc">{{ option.desc }}</span>
            </div>
          </div>
        </div>

        <!-- 主机厂偏好 -->
<div class="filter-section">
  <div class="section-label">
    <el-icon><OfficeBuilding /></el-icon>
    <span>主机厂偏好</span>
  </div>
  <el-select
    v-model="questionnaireData.factory"
    filterable
    remote
    reserve-keyword
    placeholder="请输入主机厂名称"
    :remote-method="fetchFactoryOptions"
    :loading="factoryLoading"
    style="width: 220px"
    clearable
  >
    <el-option
      v-for="opt in factoryOptions"
      :key="opt.value"
      :value="opt.value"
    />
  </el-select>
</div>


    </el-card>

    <!-- 推荐结果展示区 -->
    <div class="recommendation-results" v-if="recommendationResult">
      <!-- 推荐摘要卡片 -->
      <el-card shadow="never" class="summary-card">
        <template #header>
          <div class="summary-header">
            <el-icon><Trophy /></el-icon>
            <span>推荐结果</span>
            <div class="summary-tags">
              <!-- <el-tag :type="getScenarioTagType()">{{ getUserScenarioLabel() }}</el-tag> -->
              <!-- <el-tag type="success">基于{{ businessMetrics.totalSalesRecords }}条真实销量</el-tag>
              <el-tag type="info">{{ businessMetrics.totalOpinions }}条用户口碑</el-tag> -->
            </div>
          </div>
        </template>

        <div class="summary-content">
          <div class="summary-stats">
            <div class="stat-card">
              <div class="stat-number">{{ recommendationResult.recommendations.length }}</div>
              <div class="stat-label">推荐车型</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ recommendationResult.matchScore }}%</div>
              <div class="stat-label">综合匹配度</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ recommendationResult.totalCandidates }}</div>
              <div class="stat-label">候选车型</div>
            </div>
            <div class="stat-card">
              <div class="stat-number">{{ getAnalysisTime() }}s</div>
              <div class="stat-label">分析耗时</div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 主推荐车型 -->
      <el-card shadow="never" class="primary-recommendation-card">
        <template #header>
          <div class="primary-header">
            <el-icon><Medal /></el-icon>
            <span>最佳推荐</span>
            <div class="confidence-info">
              <el-tag v-if="recommendationResult.primaryRecommendation.isHot" type="danger">
                🔥 HOT
              </el-tag>
            </div>
          </div>
        </template>

        <div class="primary-content">
          <div class="primary-model">
            <div class="model-showcase">
              <img
                :src="recommendationResult.primaryRecommendation.image"
                :alt="recommendationResult.primaryRecommendation.name"
                class="showcase-image"
                @error="handleImageError"
              />
              <div class="model-badges">
                <el-tag type="success" size="large">
                  <el-icon><Trophy /></el-icon>
                  最佳匹配
                </el-tag>
              </div>
            </div>
            <div class="model-details">
              <h3>
                {{ recommendationResult.primaryRecommendation.brand }}
                {{ recommendationResult.primaryRecommendation.name }}
              </h3>
              <p class="model-price">{{ recommendationResult.primaryRecommendation.priceRange }}</p>

              <!-- 车型基本信息 -->
              <div class="model-specs">
                <el-tag size="small">{{ recommendationResult.primaryRecommendation.type }}</el-tag>
                <el-tag size="small" type="success">{{
                  recommendationResult.primaryRecommendation.engine
                }}</el-tag>
                <el-tag size="small" type="warning">{{
                  recommendationResult.primaryRecommendation.level
                }}</el-tag>
                <el-tag size="small" type="info"
                  >{{ recommendationResult.primaryRecommendation.seatNum }}座</el-tag
                >
              </div>


              <!-- 匹配度雷达图 -->
              <div class="match-radar">
                <h4>匹配度分析</h4>
                <div ref="primaryRadarChart" class="radar-chart"></div>
              </div>
            </div>
          </div>

          <!-- 推荐理由 -->
          <div class="recommendation-reasons">
            <h4>🎯 推荐理由</h4>
            <ul class="reasons-list">
              <li
                v-for="reason in recommendationResult.primaryRecommendation.reasons"
                :key="reason.id"
              >
                <el-icon><Check /></el-icon>
                <span class="reason-text">{{ reason.text }}</span>
                <el-tag size="small" :type="reason.type">{{ reason.category }}</el-tag>
              </li>
            </ul>
          </div>

          <!-- 核心优势 -->
          <div class="core-advantages">
            <h4>⭐ 核心优势</h4>
            <div class="advantages-grid">
              <div
                v-for="advantage in recommendationResult.primaryRecommendation.advantages"
                :key="advantage.label"
                class="advantage-item"
              >
                <el-icon>
                  <component :is="advantage.icon" />
                </el-icon>
                <div class="advantage-info">
                  <h5>{{ advantage.label }}</h5>
                  <p>{{ advantage.description }}</p>
                  <div class="advantage-data" v-if="advantage.data">
                    <span class="data-label">{{ advantage.data.label }}</span>
                    <span class="data-value">{{ advantage.data.value }}</span>
                  </div>
                </div>
                <div class="advantage-score">
                  <el-progress
                    type="circle"
                    :percentage="advantage.score"
                    :width="60"
                    :show-text="true"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="primary-actions">
          <el-button
            size="large"
            @click="addToComparison(recommendationResult.primaryRecommendation)"
          >
            <el-icon><DataBoard /></el-icon>
            加入对比
          </el-button>
        </div>
      </el-card>

      <!-- 备选推荐 -->
      <el-card shadow="never" class="alternatives-card">
        <template #header>
          <div class="alternatives-header">
            <el-icon><List /></el-icon>
            <span>备选推荐</span>
            <div class="alternatives-controls">
              <el-select
                v-model="alternativeSortBy"
                placeholder="排序方式"
                size="small"
                style="width: 120px"
              >
                <el-option label="匹配度" value="matchScore" />
              </el-select>
              <!-- <el-button
                size="small"
                type="text"
                @click="showAllAlternatives = !showAllAlternatives"
              >
                {{ showAllAlternatives ? '收起' : '查看全部' }}
              </el-button> -->
            </div>
          </div>
        </template>

        <div class="alternatives-content">
          <div class="alternatives-tabs">
            <!-- <el-tabs v-model="activeAlternativeTab" type="card">
              <el-tab-pane label="性价比推荐" name="budget">
                <div class="alternative-group">
                  <div
                    v-for="model in getSortedAlternatives(recommendationResult.alternatives.budget)"
                    :key="model.id"
                    class="alternative-item"
                  >
                    <img
                      :src="model.image"
                      :alt="model.name"
                      class="alternative-image"
                      @error="handleImageError"
                    />
                    <div class="alternative-info">
                      <h4>{{ model.brand }} {{ model.name }}</h4>
                      <p class="alternative-price">{{ model.priceRange }}</p>
                      <div class="alternative-specs">
                        <el-tag size="mini">{{ model.engine }}</el-tag>
                        <el-tag size="mini" type="info">{{ model.seatNum }}座</el-tag>
                      </div>
                      <div class="alternative-highlight">
                        <el-tag size="small" type="success">{{ model.highlight }}</el-tag>
                      </div>
                      <div class="alternative-metrics">
                        <div class="metric">
                          <span class="metric-label">匹配度</span>
                          <el-progress :percentage="model.matchScore" :show-text="false" />
                          <span class="metric-value">{{ model.matchScore }}%</span>
                        </div>
                        
                        <div class="metric">
                          <span class="metric-label">月销量</span>
                          <span class="metric-value"
                            >{{ getModelSalesData(model.id).avgMonthlySales }}台</span
                          >
                        </div>
                        <div class="metric">
                          <span class="metric-label">用户评分</span>
                          <el-rate
                            :model-value="getModelOpinionScore(model.id)"
                            disabled
                            size="small"
                            :show-text="false"
                          />
                        </div>
                      </div>
                    </div>
                    <div class="alternative-actions">
                      <el-button size="small" @click="viewModelDetails(model)">
                        <el-icon><Monitor /></el-icon>
                        详情
                      </el-button>
                      <el-button size="small" type="primary" @click="addToComparison(model)">
                        <el-icon><DataBoard /></el-icon>
                        对比
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="豪华配置" name="luxury">
                <div class="alternative-group">
                  <div
                    v-for="model in getSortedAlternatives(recommendationResult.alternatives.luxury)"
                    :key="model.id"
                    class="alternative-item"
                  >
                    <img
                      :src="model.image"
                      :alt="model.name"
                      class="alternative-image"
                      @error="handleImageError"
                    />
                    <div class="alternative-info">
                      <h4>{{ model.brand }} {{ model.name }}</h4>
                      <p class="alternative-price">{{ model.priceRange }}</p>
                      <div class="alternative-specs">
                        <el-tag size="mini">{{ model.engine }}</el-tag>
                        <el-tag size="mini" type="info">{{ model.seatNum }}座</el-tag>
                      </div>
                      <div class="alternative-highlight">
                        <el-tag size="small" type="warning">{{ model.highlight }}</el-tag>
                      </div>
                      <div class="alternative-metrics">
                        <div class="metric">
                          <span class="metric-label">匹配度</span>
                          <el-progress :percentage="model.matchScore" :show-text="false" />
                          <span class="metric-value">{{ model.matchScore }}%</span>
                        </div>

                      </div>
                    </div>
                    <div class="alternative-actions">
                      <el-button size="small" @click="viewModelDetails(model)">
                        <el-icon><Monitor /></el-icon>
                        详情
                      </el-button>
                      <el-button size="small" type="primary" @click="addToComparison(model)">
                        <el-icon><DataBoard /></el-icon>
                        对比
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="实用首选" name="practical">
                <div class="alternative-group">
                  <div
                    v-for="model in getSortedAlternatives(
                      recommendationResult.alternatives.practical,
                    )"
                    :key="model.id"
                    class="alternative-item"
                  >
                    <img
                      :src="model.image"
                      :alt="model.name"
                      class="alternative-image"
                      @error="handleImageError"
                    />
                    <div class="alternative-info">
                      <h4>{{ model.brand }} {{ model.name }}</h4>
                      <p class="alternative-price">{{ model.priceRange }}</p>
                      <div class="alternative-specs">
                        <el-tag size="mini">{{ model.engine }}</el-tag>
                        <el-tag size="mini" type="info">{{ model.seatNum }}座</el-tag>
                      </div>
                      <div class="alternative-highlight">
                        <el-tag size="small" type="info">{{ model.highlight }}</el-tag>
                      </div>
                      <div class="alternative-metrics">
                        <div class="metric">
                          <span class="metric-label">匹配度</span>
                          <el-progress :percentage="model.matchScore" :show-text="false" />
                          <span class="metric-value">{{ model.matchScore }}%</span>
                        </div>
                      </div>
                    </div>
                    <div class="alternative-actions">
                      <el-button size="small" @click="viewModelDetails(model)">
                        <el-icon><Monitor /></el-icon>
                        详情
                      </el-button>
                      <el-button size="small" type="primary" @click="addToComparison(model)">
                        <el-icon><DataBoard /></el-icon>
                        对比
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs> -->

             <div class="alternative-group">
    <div
      v-for="model in getSortedAlternatives(recommendationResult.alternatives.budget.concat(recommendationResult.alternatives.luxury, recommendationResult.alternatives.practical))"
      :key="model.id"
      class="alternative-item"
    >
      <img
        :src="model.image"
        :alt="model.name"
        class="alternative-image"
        @error="handleImageError"
      />
      <div class="alternative-info">
        <h4>{{ model.brand }} {{ model.name }}</h4>
        <p class="alternative-price">{{ model.priceRange }}</p>
        <div class="alternative-specs">
          <el-tag size="mini">{{ model.engine }}</el-tag>
          <el-tag size="mini" type="info">{{ model.seatNum }}座</el-tag>
        </div>
        <div class="alternative-highlight">
          <el-tag size="small" type="success">{{ model.highlight }}</el-tag>
        </div>
        <div class="alternative-metrics">
          <div class="metric">
            <span class="metric-label">匹配度</span>
            <el-progress :percentage="model.matchScore" :show-text="false" />
            <span class="metric-value">{{ model.matchScore }}%</span>
          </div>
        </div>
      </div>
      <div class="alternative-actions">
        <!-- <el-button size="small" @click="viewModelDetails(model)">
          <el-icon><Monitor /></el-icon>
          详情
        </el-button> -->
        <el-button size="small" type="primary" @click="addToComparison(model)">
          <el-icon><DataBoard /></el-icon>
          对比
        </el-button>
      </div>
    </div>
  </div>

          </div>
        </div>
      </el-card>

      <!-- 对比分析工具 -->
      <el-card shadow="never" class="comparison-tool-card" v-if="comparisonList.length > 0">
        <template #header>
          <div class="comparison-header">
            <el-icon><DataBoard /></el-icon>
            <span>快速对比 ({{ comparisonList.length }}/4)</span>
            <div class="comparison-actions">
              <el-button size="small" @click="clearComparison">
                <el-icon><Close /></el-icon>
                清空
              </el-button>
              <el-button
                size="small"
                type="primary"
                @click="startDetailedComparison"
                :disabled="comparisonList.length < 2"
              >
                <el-icon><DataBoard /></el-icon>
                开始对比
              </el-button>
            </div>
          </div>
        </template>

        <div class="comparison-content">
          <div class="comparison-models">
            <div v-for="model in comparisonList" :key="model.id" class="comparison-model">
              <img
                :src="model.image"
                :alt="model.name"
                class="comparison-image"
                @error="handleImageError"
              />
              <div class="comparison-info">
                <h5>{{ model.brand }} {{ model.name }}</h5>
                <p>{{ model.priceRange }}</p>
                <div class="comparison-metrics">
                  <div class="metric-item">
                    <span>匹配度</span>
                    <span>{{ model.matchScore }}%</span>
                  </div>
                </div>
              </div>
              <el-button
                size="small"
                type="danger"
                :icon="Close"
                @click="removeFromComparison(model.id)"
              />
            </div>
          </div>
        </div>
      </el-card>

      <!-- 购买建议 -->
      <el-card shadow="never" class="purchase-advice-card">
        <template #header>
          <div class="advice-header">
            <el-icon><Guide /></el-icon>
            <span>智能购买建议</span>
            <el-tag type="success">基于市场数据分析</el-tag>
          </div>
        </template>

        <div class="advice-content">
          <div class="advice-sections">
            <!-- 购车时机建议 -->
            <!-- <div class="advice-section">
              <h4>💰 购车时机建议</h4>
              <div class="timing-advice">
                <div class="advice-item">
                  <el-icon><TrendCharts /></el-icon>
                  <div class="advice-text">
                    <h5>市场趋势分析</h5>
                    <p>{{ getPurchaseTimingAdvice() }}</p>
                    <div class="market-data">
                      <span class="data-item">
                        <span class="label">当月销量趋势</span>
                        <span class="value" :class="getMarketTrendClass()">
                          {{ getMarketTrendText() }}
                        </span>
                      </span>
                      <span class="data-item">
                        <span class="label">平均优惠幅度</span>
                        <span class="value">{{ getAverageDiscount() }}</span>
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div> -->

            <!-- 购车渠道建议 -->
            <div class="advice-section">
              <h4>🏪 购车渠道建议</h4>
              <div class="channel-advice">
                <div class="channel-options">
                  <div
                    class="channel-item"
                    v-for="channel in getRecommendedChannels()"
                    :key="channel.type"
                  >
                    <el-icon>
                      <component :is="channel.icon" />
                    </el-icon>
                    <div class="channel-info">
                      <h5>{{ channel.name }}</h5>
                      <p>{{ channel.advantage }}</p>
                      <div class="channel-details" v-if="channel.details">
                        <span class="detail-item" v-for="detail in channel.details" :key="detail">
                          {{ detail }}
                        </span>
                      </div>
                    </div>
                    <el-tag :type="channel.recommended ? 'success' : 'info'">
                      {{ channel.recommended ? '推荐' : '可选' }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>



            <!-- 购车清单 -->
            <div class="advice-section">
              <h4>📋 购车准备清单</h4>
              <div class="checklist">
                <el-checkbox-group v-model="checkedItems" class="checklist-items">
                  <el-checkbox
                    v-for="item in enhancedPurchaseChecklist"
                    :key="item.id"
                    :value="item.id"
                  >
                    <div class="checklist-item-content">
                      <span class="item-text">{{ item.text }}</span>
                      <span class="item-tip" v-if="item.tip">{{ item.tip }}</span>
                    </div>
                  </el-checkbox>
                </el-checkbox-group>
              </div>
            </div>
          </div>
        </div>

        <div class="advice-actions">
          <el-button type="primary" size="large" @click="saveRecommendation">
            <el-icon><Download /></el-icon>
            保存推荐结果
          </el-button>
          <el-button size="large" @click="shareRecommendation">
            <el-icon><Share /></el-icon>
            分享给朋友
          </el-button>
          <el-button size="large" @click="restartQuestionnaire">
            <el-icon><Refresh /></el-icon>
            重新推荐
          </el-button>
          <!-- <el-button
            size="large"
            @click="contactDealer"
            v-if="recommendationResult.primaryRecommendation"
          >
            <el-icon><Phone /></el-icon>
            联系经销商
          </el-button> -->
        </div>
      </el-card>
    </div>

    <!-- 数据分析状态 -->
    <div class="loading-overlay" v-if="analyzing">
      <el-card shadow="never" class="loading-card">
        <div class="loading-content">
          <el-icon class="loading-icon"><MagicStick /></el-icon>
          <h3>正在分析您的需求...</h3>
          <p>{{ currentAnalysisStep }}</p>
          <el-progress :percentage="analysisProgress" :show-text="false" />
          <div class="analysis-details">
            <div class="detail-item">
              <span class="label">分析车型数量</span>
            </div>
            <div class="detail-item">
              <span class="label">销量数据样本</span>

            </div>
            <div class="detail-item">
              <span class="label">用户评价样本</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 无结果状态 -->
    <el-card
      v-if="baseDataLoaded && !loading && !recommendationResult && questionnaireData.budget"
      shadow="never"
      class="empty-state-card"
    >
      <div class="empty-state">
        <el-icon class="empty-icon"><Monitor /></el-icon>
        <h3>暂无推荐结果</h3>
        <p>请调整筛选条件或点击"生成推荐"按钮</p>
        <el-button type="primary" @click="generateRecommendation" :disabled="!isStep1Valid()">
          <el-icon><MagicStick /></el-icon>
          开始智能推荐
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Download,
  Loading,
  MagicStick,
  Money,
  OfficeBuilding,
  Lightning,
  Star,
  Guide,
  TrendCharts,
  Trophy,
  Medal,
  Check,
  List,
  Monitor,
  DataBoard,
  Close,
  Share,
  Phone,
  Van,
  Timer,
  Location,
  Service,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'

const router = useRouter()

// =============================================
//  基础数据层 - 直接从API获取
// =============================================

// 定义 CarModel 类型
interface CarModel {
  carModelId: number
  modelName: string
  modelFullName: string
  brandId: number
  brandName: string
  level: string
  launchDate: string
  officialPrice: number
  engineType: string
  seatNum: number
  driveType: string
  rangeKm: number
  imageUrl: string
  totalScore: number
  budgetScore: number
  levelScore: number
  engineTypeScore: number
  seatNumScore: number
  brandScore: number
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

interface Region {
  regionId: number
  regionName: string
  parentRegionName?: string | null
}

interface BaseData {
  carModels?: CarModel[]
  saleRecords?: SaleRecord[]
  regions?: Region[]
  topLevelRegions?: Region[]
  nonTopLevelRegions?: Region[]
  opinions?: Opinion[]
}

//  计算数据层 - 基于基础数据计算
interface ProcessedCarModel {
  id: number
  brand: string
  name: string
  priceRange: string
  type: string
  engine: string
  transmission: string
  image: string
  matchScore: number
  level: string
  seatNum: number
  totalScore?: number
  budgetScore?: number
  levelScore?: number
  engineTypeScore?: number
  seatNumScore?: number
  brandScore?: number
  isHot?: boolean
  isNew?: boolean
  highlight?: string
}

interface PrimaryRecommendation extends ProcessedCarModel {
  confidence: number
  reasons: Array<{
    id: string
    text: string
    category: string
    type: 'success' | 'warning' | 'info'
  }>
  advantages: Array<{
    label: string
    description: string
    icon: any
    score: number
    data?: {
      label: string
      value: string
    }
  }>
}

interface RecommendationResult {
  recommendations: ProcessedCarModel[]
  primaryRecommendation: PrimaryRecommendation
  alternatives: {
    budget: ProcessedCarModel[]
    luxury: ProcessedCarModel[]
    practical: ProcessedCarModel[]
  }
  matchScore: number
  totalCandidates: number
  analysisTime: number
}

//  业务指标层 - 最终展示的KPI
interface BusinessMetrics {
  totalSalesRecords: number
  totalOpinions: number
  totalCarModels: number
  avgMarketPrice: number
  topBrands: string[]
  marketTrend: 'up' | 'down' | 'stable'
}

// =============================================
//  响应式数据
// =============================================

const loading = ref(false)
const baseDataLoaded = ref(false)
const analyzing = ref(false)
const currentLoadingStep = ref('')
const loadingProgress = ref(0)
const currentAnalysisStep = ref('')
const analysisProgress = ref(0)

// 基础数据存储
const baseData = ref<BaseData>({
  carModels: [],
  saleRecords: [],
  regions: [],
  topLevelRegions: [],
  nonTopLevelRegions: [],
  opinions: [],
})

// 业务指标
const businessMetrics = ref<BusinessMetrics>({
  totalSalesRecords: 0,
  totalOpinions: 0,
  totalCarModels: 0,
  avgMarketPrice: 0,
  topBrands: [],
  marketTrend: 'stable',
})

// =============================================
//  响应式问卷数据
// =============================================

const questionnaireData = ref({
  budget: '',
  level: '',           // 车型类别
  engineType: '',      // 动力类型
  seatNum: '',         // 乘坐人数
  factory: '',         // 主机厂
})

const levelOptions = ref<string[]>([])
const engineTypeOptions = ref<string[]>([])
const factoryOptions = ref<{ value: string; label: string }[]>([])
const factoryLoading = ref(false)



// 推荐结果
const recommendationResult = ref<RecommendationResult | null>(null)
const comparisonList = ref<ProcessedCarModel[]>([])
const checkedItems = ref<string[]>([])

// UI控制
const alternativeSortBy = ref('matchScore')
const activeAlternativeTab = ref('budget')
const showAllAlternatives = ref(false)

// 图表实例
const primaryRadarChart = ref<HTMLDivElement>()
let primaryRadarChartInstance: echarts.ECharts | null = null

// =============================================
// 筛选选项配置
// =============================================

const budgetOptions = [
  { value: 'under10', label: '10万以下', desc: '经济实用' },
  { value: '10-20', label: '10-20万', desc: '主流选择' },
  { value: '20-30', label: '20-30万', desc: '品质之选' },
  { value: '30-50', label: '30-50万', desc: '豪华配置' },
  { value: 'over50', label: '50万以上', desc: '顶级奢华' },
  { value: 'unlimited', label: '预算不限', desc: '最优选择' },
]

// const bodyTypeOptions = [
//   { value: 'sedan', label: '轿车', desc: '经典优雅', icon: Van },
//   { value: 'suv', label: 'SUV', desc: '空间宽敞', icon: Van },
//   { value: 'mpv', label: 'MPV', desc: '商务实用', icon: Van },
//   { value: 'hatchback', label: '两厢车', desc: '时尚灵活', icon: Van },
//   { value: 'coupe', label: '跑车', desc: '运动激情', icon: Van },
// ]

const energyTypeOptions = [
  { value: 'gasoline', label: '汽油', desc: '成熟可靠', icon: Service },
  { value: 'electric', label: '纯电动', desc: '环保节能', icon: Lightning },
  { value: 'hybrid', label: '混合动力', desc: '节能环保', icon: Lightning },
  { value: 'any', label: '不限', desc: '综合考虑', icon: Star },
]

const passengerOptions = [
  { value: '2', label: '2座', desc: '双人世界' },
  { value: '5', label: '5座', desc: '家庭首选' },
  { value: '7', label: '7座', desc: '大家庭' },
  { value: 'any', label: '不限', desc: '灵活选择' },
]

// const usageOptions = [
//   { value: 'daily', label: '日常通勤', desc: '上下班代步', icon: Timer },
//   { value: 'family', label: '家用出行', desc: '全家出游', icon: Van },
//   { value: 'business', label: '商务用车', desc: '工作需要', icon: Service },
//   { value: 'travel', label: '长途旅行', desc: '自驾游', icon: Location },
// ]

// const mileageOptions = [
//   { value: 'low', label: '30km以下', desc: '市内短途' },
//   { value: 'medium', label: '30-80km', desc: '中等距离' },
//   { value: 'high', label: '80km以上', desc: '长距离通勤' },
//   { value: 'varies', label: '里程不定', desc: '使用灵活' },
// ]

// =============================================
//  API调用函数
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

const fetchLevelOptions = async () => {
  const res = await axios.get('/api/car-models/levels')
  if (res.data.status === 200 && res.data.data) {
    levelOptions.value = res.data.data.map((level: string) => ({
      value: level,
      label: level,
    }))
  }
}

const fetchEngineTypeOptions = async () => {
  const res = await axios.get('/api/car-models/engine-types')
  if (res.data.status === 200 && res.data.data) {
    engineTypeOptions.value = res.data.data.map((type: string) => ({
      value: type,
      label: type,
    }))
  }
}

const fetchFactoryOptions = async (keyword: string) => {
  factoryLoading.value = true
  try {
    const res = await axios.get('/api/car-models/factorys', { params: { keyword } })
    if (res.data.status === 200 && res.data.data) {
      factoryOptions.value = res.data.data.map((factory: string) => ({
        value: factory,
        label: factory,
      }))
    }
  } finally {
    factoryLoading.value = false
  }
}


// 新增推荐榜单API请求函数
const fetchCarModelMatchScoreRanking = async (
  level: string = 'all',
  minPrice?: number,
  maxPrice?: number,
  engineType: string = 'all',
  seatNum?: number,
  factory: string = 'all',
  top: number = 4
) => {
  try {
    const params: any = { level, engineType, factory, top }
    if (typeof minPrice === 'number' && !isNaN(minPrice)) params.minPrice = minPrice
    if (typeof maxPrice === 'number' && !isNaN(maxPrice)) params.maxPrice = maxPrice
    if (typeof seatNum === 'number' && !isNaN(seatNum)) params.seatNum = seatNum
    const response = await axios.get('/api/ranking/match-score', { params })
    if (response.data.status === 200 && response.data.data) {
      return response.data.data
    } else {
      throw new Error(response.data.message || 'API返回错误')
    }
  } catch (error) {
    ElMessage.error('获取推荐榜单数据失败')
    return []
  }
}


const fetchRegions = async (): Promise<Region[]> => {
  try {
    console.log('正在获取地区信息...')
    const response = await axios.get('/api/regions')
    if (response.data.status === 200 && response.data.data) {
      console.log('获取地区数据成功:', response.data.data.length, '个地区')
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
    const response = await axios.get('/api/regions/top-level/old')
    if (response.data.status === 200 && response.data.data) {
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('获取省份信息失败:', error)
    throw error
  }
}

const fetchNonTopLevelRegions = async (): Promise<Region[]> => {
  try {
    const response = await axios.get('/api/regions/non-top-level')
    if (response.data.status === 200 && response.data.data) {
      return response.data.data
    } else {
      throw new Error(`API返回错误状态: ${response.data.status}`)
    }
  } catch (error) {
    console.error('获取城市信息失败:', error)
    throw error
  }
}

// =============================================
// 数据加载与处理函数
// =============================================

const loadAllBaseData = async () => {
  try {
    console.log('开始加载基础数据...')
    loading.value = true
    loadingProgress.value = 0

    currentLoadingStep.value = '正在获取车型信息...'
    loadingProgress.value = 10
    const carModels = await fetchCarModels()


    currentLoadingStep.value = '正在获取地区信息...'
    loadingProgress.value = 50
    const [regions, topLevelRegions, nonTopLevelRegions] = await Promise.all([
      fetchRegions(),
      fetchTopLevelRegions(),
      fetchNonTopLevelRegions(),
    ])


    currentLoadingStep.value = '正在处理数据...'
    loadingProgress.value = 90

    baseData.value = {
      carModels,
      regions,
      topLevelRegions,
      nonTopLevelRegions,
    }

    calculateBusinessMetrics()

    currentLoadingStep.value = '数据加载完成'
    loadingProgress.value = 100
    baseDataLoaded.value = true

    ElMessage.success('基础数据加载完成')
  } catch (error) {
    console.error('基础数据加载失败:', error)
    ElMessage.error('数据加载失败，请检查网络连接')
    throw error
  } finally {
    loading.value = false
  }
}

const calculateBusinessMetrics = () => {
  console.log('计算业务指标...')

  businessMetrics.value.totalCarModels = baseData.value.carModels.length

  // // 计算平均市场价格
  // if (baseData.value.carModels.length > 0) {
  //   const totalPrice = baseData.value.carModels.reduce((sum, car) => sum + car.officialPrice, 0)
  //   businessMetrics.value.avgMarketPrice = totalPrice / baseData.value.carModels.length / 10000
  // }

  // // 统计热门品牌
  // const brandSalesMap = new Map<string, number>()
  // baseData.value.saleRecords.forEach((record) => {
  //   const carModel = baseData.value.carModels.find((car) => car.carModelId === record.carModelId)
  //   if (carModel) {
  //     const currentSales = brandSalesMap.get(carModel.brandName) || 0
  //     brandSalesMap.set(carModel.brandName, currentSales + record.saleCount)
  //   }
  // })

  // businessMetrics.value.topBrands = Array.from(brandSalesMap.entries())
  //   .sort((a, b) => b[1] - a[1])
  //   .slice(0, 5)
  //   .map(([brand]) => brand)

  // // 计算市场趋势
  // const recentRecords = baseData.value.saleRecords.filter((record) => {
  //   const recordDate = new Date(record.saleMonth)
  //   const threeMonthsAgo = new Date()
  //   threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3)
  //   return recordDate >= threeMonthsAgo
  // })

  // if (recentRecords.length > 0) {
  //   const currentMonthSales = recentRecords
  //     .filter((record) => new Date(record.saleMonth).getMonth() === new Date().getMonth())
  //     .reduce((sum, record) => sum + record.saleCount, 0)

  //   const lastMonthSales = recentRecords
  //     .filter((record) => new Date(record.saleMonth).getMonth() === new Date().getMonth() - 1)
  //     .reduce((sum, record) => sum + record.saleCount, 0)

  //   if (lastMonthSales > 0) {
  //     const growthRate = ((currentMonthSales - lastMonthSales) / lastMonthSales) * 100
  //     if (growthRate > 5) {
  //       businessMetrics.value.marketTrend = 'up'
  //     } else if (growthRate < -5) {
  //       businessMetrics.value.marketTrend = 'down'
  //     } else {
  //       businessMetrics.value.marketTrend = 'stable'
  //     }
  //   }
  // }

  console.log('业务指标计算完成:', businessMetrics.value)
}




// 推荐生成函数，直接用后端返回的数据
const generateRecommendation = async () => {
  if (!isStep1Valid()) {
    ElMessage.warning('请先完成必填项目')
    return
  }

  try {
    analyzing.value = true
    analysisProgress.value = 0
    currentAnalysisStep.value = '正在请求推荐榜单...'

    // 组装参数
    let level = questionnaireData.value.level || 'all'
  let minPrice: number | undefined, maxPrice: number | undefined
  if (questionnaireData.value.budget && questionnaireData.value.budget !== 'unlimited') {
    const budgetRanges = {
      under10: [0, 10],
      '10-20': [10, 20],
      '20-30': [20, 30],
      '30-50': [30, 50],
      over50: [50, 200],
      unlimited: [0, 200],
    }
    const [min, max] = budgetRanges[questionnaireData.value.budget]
    minPrice = min
    maxPrice = max
  }
  let engineType = questionnaireData.value.engineType || 'all'
  let seatNum = questionnaireData.value.seatNum ? parseInt(questionnaireData.value.seatNum) : undefined
  let factory = questionnaireData.value.factory || 'all'

    // 直接请求后端推荐榜单
    const data = await fetchCarModelMatchScoreRanking(
      level,
      minPrice,
      maxPrice,
      engineType,
      seatNum,
      factory,
      10
    )

    // 适配后端返回结构
    const processedList = data.map((item: any, idx: number) => ({
      id: item.carModelId,
      brand: item.brandName,
      name: item.modelName,
      priceRange: item.officialPrice
        ? `${(item.officialPrice ).toFixed(1)}万`
        : '--',
      type: item.level || '',
      engine: item.engineType,
      transmission: item.driveType,
      image: item.imageUrl || `https://picsum.photos/400/300?random=${item.carModelId}`,
      highlight: idx === 0 ? '最佳推荐' : idx < 3 ? '优选' : '',
      matchScore: item.totalScore ? Math.round(item.totalScore) : 0,
      level: item.level,
      seatNum: item.seatNum,
      avgMonthlySales: 0,
      totalSales: 0,
      avgPrice: item.officialPrice ? item.officialPrice / 10000 : 0,
      userRating: 4.0,
      marketHeat: 60,
      salesRank: idx + 1,
      isHot: idx < 3,
      isNew: false,
      // 可扩展更多字段
      totalScore: item.totalScore,
      budgetScore: item.budgetScore,
      levelScore: item.levelScore,
      engineTypeScore: item.engineTypeScore,
      seatNumScore: item.seatNumScore,
      brandScore: item.brandScore,
    }))

    // 主推荐
    const topCandidate = processedList[0]
    const primaryRecommendation: PrimaryRecommendation = {
      ...topCandidate,
      confidence: topCandidate.matchScore,
      reasons: [
        { id: '1', text: `综合得分${topCandidate.matchScore}分，匹配度高`, category: '综合评分', type: 'success' },
        { id: '2', text: `官方指导价${topCandidate.priceRange}，价格合理`, category: '预算', type: 'success' },
        { id: '3', text: `座位数${topCandidate.seatNum}，空间充足`, category: '空间', type: 'success' },
      ],
      advantages: [
        {
          label: '综合得分',
          description: '多维度综合评分',
          icon: Star,
          score: topCandidate.matchScore,
          data: { label: '总分', value: `${topCandidate.matchScore}分` },
        },
        {
          label: '预算匹配',
          description: '价格区间合理',
          icon: Money,
          score: topCandidate.budgetScore || 0,
          data: { label: '预算分', value: `${topCandidate.budgetScore || 0}分` },
        },
        {
          label: '空间表现',
          description: '座位数情况',
          icon: OfficeBuilding,
          score: topCandidate.seatNumScore || 0,
          data: { label: '座位分', value: `${topCandidate.seatNumScore || 0}分` },
        },
      ],
    }

    // 备选推荐分组
    const budgetCars = processedList
      .filter((car: { budgetScore: number }) => car.budgetScore >= 20)
      .slice(1, 4)
    const luxuryCars = processedList
      .filter((car: { levelScore: number }) => car.levelScore >= 20)
      .slice(1, 4)
    const practicalCars = processedList
      .filter((car: { engineTypeScore: number }) => car.engineTypeScore >= 15)
      .slice(1, 4)

    const result: RecommendationResult = {
      recommendations: processedList,
      primaryRecommendation,
      alternatives: {
        budget: budgetCars,
        luxury: luxuryCars,
        practical: practicalCars,
      },
      matchScore: Math.round(
        processedList.reduce((sum, car) => sum + car.matchScore, 0) /
          Math.max(processedList.length, 1)
      ),
      totalCandidates: processedList.length,
      analysisTime: Math.random() * 2 + 1,
    }

    recommendationResult.value = result

    currentAnalysisStep.value = '推荐生成完成！'
    analysisProgress.value = 100
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success(`推荐生成成功！为您找到了${processedList.length}款适合的车型`)

    // 初始化雷达图
    await nextTick()
    initPrimaryRadarChart()
  } catch (error) {
    console.error('推荐生成失败:', error)
    ElMessage.error('推荐生成失败，请重试')
  } finally {
    analyzing.value = false
  }
}

// =============================================
// 筛选操作函数
// =============================================

const selectBudget = (budget: string) => {
  questionnaireData.value.budget = budget
}

const selectLevelType = (level: string) => {
  questionnaireData.value.level = level
}

const selectEngineType = (engineType: string) => {
  questionnaireData.value.engineType = engineType
}

const selectSeatNum = (seatNum: string) => {
  questionnaireData.value.seatNum = seatNum
}



// =============================================
// 验证函数
// =============================================

const isStep1Valid = (): boolean => {
  return !!(
    questionnaireData.value.budget &&
    questionnaireData.value.level.length > 0 &&
    questionnaireData.value.engineType &&
    questionnaireData.value.seatNum
  )
}

// =============================================
// 智能提示函数
// =============================================


// =============================================
// 数据获取函数
// =============================================

// const getModelSalesData = (carModelId: number) => {
//   const salesRecords = baseData.value.saleRecords.filter(
//     (record) => record.carModelId === carModelId,
//   )
//   const totalSales = salesRecords.reduce((sum, record) => sum + record.saleCount, 0)
//   const avgMonthlySales = salesRecords.length > 0 ? Math.floor(totalSales / salesRecords.length) : 0

//   return { totalSales, avgMonthlySales }
// }

// const getModelOpinionScore = (carModelId: number): number => {
//   const opinion = baseData.value.opinions.find((op) => op.carModelId === carModelId)
//   return opinion ? opinion.score : 3.5
// }

// const getPrimarySalesData = () => {
//   if (!recommendationResult.value) return { totalSales: 0, avgMonthlySales: 0 }
//   return getModelSalesData(recommendationResult.value.primaryRecommendation.id)
// }

// const getPrimaryOpinionScore = (): number => {
//   if (!recommendationResult.value) return 3.5
//   return getModelOpinionScore(recommendationResult.value.primaryRecommendation.id)
// }

// const getPrimaryMarketHeat = (): number => {
//   if (!recommendationResult.value) return 50
//   return recommendationResult.value.primaryRecommendation.marketHeat
// }

// const getPrimaryRecommendationSalesRank = (): number => {
//   if (!recommendationResult.value) return 999
//   return recommendationResult.value.primaryRecommendation.salesRank
// }

// =============================================
//  UI辅助函数
// =============================================


const getAnalysisTime = () => {
  return recommendationResult.value?.analysisTime.toFixed(1) || '0.0'
}

const getHeatColor = (heat: number) => {
  if (heat >= 80) return '#f56c6c'
  if (heat >= 60) return '#e6a23c'
  if (heat >= 40) return '#67c23a'
  return '#909399'
}

const getSortedAlternatives = (alternatives: ProcessedCarModel[]) => {
  if (!alternatives) return []
  // 只按匹配度排序
  return [...alternatives].sort((a, b) => b.matchScore - a.matchScore)
}

// =============================================
// 购买建议函数
// =============================================

// const getPurchaseTimingAdvice = (): string => {
//   const trend = businessMetrics.value.marketTrend
//   if (trend === 'up') {
//     return '当前市场销量呈上升趋势，购车需求旺盛，建议提前预订以避免等车周期延长。'
//   } else if (trend === 'down') {
//     return '当前市场销量有所下降，经销商可能有更多优惠政策，是购车的好时机。'
//   }
//   return '当前市场相对稳定，价格波动较小，任何时候购车都是不错的选择。'
// }

// const getMarketTrendClass = () => {
//   const trend = businessMetrics.value.marketTrend
//   if (trend === 'up') return 'trend-up'
//   if (trend === 'down') return 'trend-down'
//   return 'trend-stable'
// }

// const getMarketTrendText = () => {
//   const trend = businessMetrics.value.marketTrend
//   if (trend === 'up') return '↗ 上升趋势'
//   if (trend === 'down') return '↘ 下降趋势'
//   return '→ 平稳发展'
// }

// const getAverageDiscount = () => {
//   return '3-8%'
// }

const getRecommendedChannels = () => {
  return [
    {
      type: 'dealer',
      name: '品牌4S店',
      advantage: '原厂品质保证，服务专业',
      icon: Service,
      recommended: true,
      details: ['原厂质保', '专业服务', '配件齐全'],
    },
    {
      type: 'online',
      name: '官方在线商城',
      advantage: '价格透明，购车便捷',
      icon: Monitor,
      recommended: true,
      details: ['价格透明', '在线预订', '送车上门'],
    },
    {
      type: 'platform',
      name: '汽车电商平台',
      advantage: '多品牌对比，优惠丰富',
      icon: Star,
      recommended: false,
      details: ['多品牌', '价格对比', '金融服务'],
    },
  ]
}




const enhancedPurchaseChecklist = [
  { id: 'budget', text: '确认购车预算和贷款方案', tip: '包含购置税、保险、上牌费用' },
  { id: 'test_drive', text: '预约试驾体验', tip: '亲身感受车辆性能和舒适度' },
  { id: 'insurance', text: '对比保险方案', tip: '选择性价比最高的保险组合' },
  { id: 'finance', text: '准备购车材料', tip: '身份证、驾驶证、收入证明等' },
  { id: 'delivery', text: '确认交车时间', tip: '合理安排提车和上牌时间' },
]

// =============================================
// 雷达图初始化
// =============================================

const initPrimaryRadarChart = async () => {
  if (!primaryRadarChart.value || !recommendationResult.value) return

  try {
    if (primaryRadarChartInstance) {
      primaryRadarChartInstance.dispose()
    }

    primaryRadarChartInstance = echarts.init(primaryRadarChart.value)

    const primaryCar = recommendationResult.value.primaryRecommendation

    // 计算每个维度的最大值（可用主推荐分数的1.2倍，或所有推荐车型的最大分数）
    const getMax = (field: keyof ProcessedCarModel) => {
      const all = recommendationResult.value?.recommendations ?? []
      const max = Math.max(...all.map(car => car[field] ?? 0), primaryCar[field] ?? 0)
      return Math.max(10, Math.ceil(max * 1.2))
    }

    const radarData = [
      { name: '价格匹配', max: getMax('budgetScore'), value: primaryCar.budgetScore },
      { name: '车辆类型', max: getMax('levelScore'), value: primaryCar.levelScore },
      { name: '动力类型', max: getMax('engineTypeScore'), value: primaryCar.engineTypeScore },
      { name: '空间大小', max: getMax('seatNumScore'), value: primaryCar.seatNumScore },
      { name: '主机厂偏好', max: getMax('brandScore'), value: primaryCar.brandScore },
    ]

   const option = {
  // title: {
  //   text: '综合评分雷达图',
  //   left: 'center',
  //   top: 8, // 上移标题，避免遮挡
  //   textStyle: {
  //     fontSize: 13,
  //     fontWeight: 700,
  //     color: '#222', // 更深的字体色
  //   },
  // },
  radar: {
    indicator: radarData.map((item) => ({ name: item.name, max: item.max })),
    radius: '70%',
    axisLine: { lineStyle: { color: '#b2b2b2', width: 1.5 } },
    splitLine: { lineStyle: { color: '#b2b2b2', width: 1 } },
    splitArea: {
      areaStyle: {
        color: [
          'rgba(79,172,254,0.08)',
          'rgba(79,172,254,0.04)',
          'rgba(79,172,254,0.01)',
          'rgba(255,255,255,0)',
        ],
      },
    },
    axisLabel: { color: '#333', fontSize: 11, fontWeight: 600 },
    name: {
      color: '#222',
      fontSize: 13,
      fontWeight: 600,
      backgroundColor: 'rgba(255,255,255,0.7)',
      padding: [2, 6],
      borderRadius: 4,
    },
  },
  series: [
    {
      type: 'radar',
      data: [
        {
          value: radarData.map((item) => item.value),
          name: '综合评分',
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { color: '#409EFF', width: 3 },
          areaStyle: { color: 'rgba(64,158,255,0.25)' },
          itemStyle: { color: '#409EFF', borderColor: '#fff', borderWidth: 2 },
        },
      ],
      label: {
        show: true,
        color: '#222',
        fontWeight: 600,
        fontSize: 12,
        backgroundColor: 'rgba(255,255,255,0.8)',
        borderRadius: 3,
        padding: [2, 4],
      },
    },
  ],
  grid: { left: 0, right: 0, top: 0, bottom: 0 }, 
}

    primaryRadarChartInstance.setOption(option)

    window.addEventListener('resize', () => {
      primaryRadarChartInstance?.resize()
    })
  } catch (error) {
    console.error('雷达图初始化失败:', error)
  }
}

// =============================================
//  操作函数
// =============================================

const addToComparison = (model: ProcessedCarModel | PrimaryRecommendation) => {
  if (comparisonList.value.length >= 4) {
    ElMessage.warning('最多只能对比4款车型')
    return
  }

  const modelData =
    'confidence' in model
      ? {
          id: model.id,
          brand: model.brand,
          name: model.name,
          priceRange: model.priceRange,
          type: model.type,
          engine: model.engine,
          transmission: model.transmission,
          image: model.image,
          highlight: '主推荐',
          matchScore: 95,
          level: model.level,
          seatNum: model.seatNum,
          avgPrice: parseFloat(model.priceRange.split('-')[0]),
          isHot: model.isHot,
          isNew: false,
        }
      : model

  const exists = comparisonList.value.find((item) => item.id === modelData.id)
  if (exists) {
    ElMessage.info('该车型已在对比列表中')
    return
  }

  comparisonList.value.push(modelData)
  ElMessage.success(`已将 ${modelData.brand} ${modelData.name} 加入对比`)
}

const removeFromComparison = (modelId: number) => {
  const index = comparisonList.value.findIndex((item) => item.id === modelId)
  if (index > -1) {
    const removedModel = comparisonList.value[index]
    comparisonList.value.splice(index, 1)
    ElMessage.success(`已移除 ${removedModel.brand} ${removedModel.name}`)
  }
}

const clearComparison = () => {
  comparisonList.value = []
  ElMessage.success('已清空对比列表')
}

const startDetailedComparison = () => {
  if (comparisonList.value.length < 2) {
    ElMessage.warning('至少需要2款车型才能进行对比')
    return
  }

  const modelIds = comparisonList.value.map((car) => car.id).join(',')
  router.push({
    name: 'VehicleModelCompAnalysis',
    query: { models: modelIds },
  })
}

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.src = 'https://via.placeholder.com/400x300/f0f2f5/909399?text=暂无图片'
}

const refreshAllData = async () => {
  loading.value = true
  try {
    await loadAllBaseData()
    if (recommendationResult.value) {
      await generateRecommendation()
    }
    ElMessage.success('数据刷新完成')
  } catch (error) {
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

const exportRecommendation = () => {
  if (!recommendationResult.value) return

  const exportData = {
    timestamp: new Date().toISOString(),
    userRequirements: questionnaireData.value,
    primaryRecommendation: recommendationResult.value.primaryRecommendation,
    alternatives: recommendationResult.value.alternatives,
    analysisMetrics: {
      totalCandidates: recommendationResult.value.totalCandidates,
      matchScore: recommendationResult.value.matchScore,
      analysisTime: recommendationResult.value.analysisTime,
    },
  }

  const dataStr = JSON.stringify(exportData, null, 2)
  const dataBlob = new Blob([dataStr], { type: 'application/json' })

  const link = document.createElement('a')
  link.href = URL.createObjectURL(dataBlob)
  link.download = `购车推荐报告_${new Date().toISOString().slice(0, 10)}.json`
  link.click()

  ElMessage.success('推荐报告已导出')
}

const saveRecommendation = () => {
  if (!recommendationResult.value) return

  const savedData = {
    timestamp: new Date().toISOString(),
    userRequirements: questionnaireData.value,
    recommendation: recommendationResult.value,
  }

  localStorage.setItem('car_recommendation', JSON.stringify(savedData))
  ElMessage.success('推荐结果已保存到本地')
}

const shareRecommendation = async () => {
  if (!recommendationResult.value) return

  const shareText = `我在智能购车推荐系统中找到了心仪的车型：${recommendationResult.value.primaryRecommendation.brand} ${recommendationResult.value.primaryRecommendation.name}，匹配度高达${recommendationResult.value.primaryRecommendation.confidence}%！`

  if (navigator.share) {
    try {
      await navigator.share({
        title: '智能购车推荐',
        text: shareText,
        url: window.location.href,
      })
    } catch (error) {
      console.log('分享取消或失败')
    }
  } else {
    await navigator.clipboard.writeText(shareText)
    ElMessage.success('推荐内容已复制到剪贴板')
  }
}

const restartQuestionnaire = () => {
  ElMessageBox.confirm('确定要重新开始推荐吗？这将清除当前的筛选条件和推荐结果。', '确认重置', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      questionnaireData.value = {
        budget: '',
        level: '',
        engineType: '',
        seatNum: '',
        factory: '',
      }
      recommendationResult.value = null
      comparisonList.value = []
      checkedItems.value = []

      ElMessage.success('已重置，请重新填写需求')
    })
    .catch(() => {
      // 用户取消
    })
}

const contactDealer = () => {
  if (!recommendationResult.value) return

  ElMessage.info('联系经销商功能开发中...')
}

// =============================================
// 生命周期
// =============================================

onMounted(async () => {
  ElMessage.success('欢迎使用智能购车推荐系统！')

  try {
      await fetchLevelOptions()
      await fetchEngineTypeOptions()
  // 不主动加载factoryOptions，等用户输入再查
    await loadAllBaseData()
  } catch (error) {
    console.error('页面初始化失败:', error)
    ElMessage.error('初始化失败，部分功能可能不可用')
  }
})

onUnmounted(() => {
  if (primaryRadarChartInstance) {
    primaryRadarChartInstance.dispose()
    primaryRadarChartInstance = null
  }
})
</script>

<style scoped>
/* 整体布局 */
.recommendation-analysis {
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

/* 筛选卡片整体 */
.filter-card {
  margin-bottom: 24px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.08);
  border: 1px solid #e8eaed;
  padding: 0 0 12px 0;
}

/* 卡片头部 */
.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px 0 24px;
  font-weight: 600;
  color: #303133;
  font-size: 18px;
  border-bottom: 1px solid #f0f2f5;
  background: linear-gradient(90deg, #f5f7fa 0%, #eaf6ff 100%);
  border-radius: 12px 12px 0 0;
}

/* 筛选内容区 */
.filter-content {
  padding: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 32px 24px;
  background: #f8fafb;
  border-radius: 0 0 12px 12px;
}

.filter-radio-group {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  width: 100%;
}

/* 每个筛选区块 */
.filter-section {
  min-width: 220px;
  flex: 1 1 220px;
  background: #fff;
  border-radius: 8px;
  padding: 18px 16px 12px 16px;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.04);
  border: 1px solid #f0f2f5;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #409eff;
  font-size: 15px;
  margin-bottom: 8px;
}

.required {
  color: #f56c6c;
  font-size: 12px;
  margin-left: 4px;
}

/* 筛选选项组 */
.filter-options {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-option {
  min-width: 80px;
  padding: 10px 18px;
  background: #f5f7fa;
  border: 1.5px solid #e8eaed;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #303133;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  box-shadow: 0 1px 4px rgba(64, 158, 255, 0.03);
}

.filter-option:hover {
  border-color: #409eff;
  background: #eaf6ff;
  color: #409eff;
}

.filter-option.active {
  background: linear-gradient(90deg, #409eff 0%, #67c23a 100%);
  color: #fff;
  border-color: #409eff;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.filter-option .option-label {
  font-size: 14px;
  font-weight: 600;
}

.filter-option .option-desc {
  font-size: 11px;
  color: #909399;
}

.filter-option.active .option-desc {
  color: #fff;
}

/* 响应式 */
@media (max-width: 1200px) {
  .filter-content {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }
  .filter-section {
    min-width: 0;
    padding: 12px 8px;
  }
}

@media (max-width: 768px) {
  .filter-header {
    flex-direction: column;
    gap: 8px;
    padding: 12px 8px 0 8px;
    font-size: 16px;
  }
  .filter-content {
    padding: 8px;
  }
}

/* 品牌选项特殊样式 */
.brand-option {
  min-height: 90px;
}

.brand-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  margin-bottom: 8px;
  border-radius: 4px;
}

/* 筛选预览样式 */
.filter-preview {
  margin-top: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.filter-preview h4 {
  margin: 0 0 16px 0;
  color: #1a1a1a;
  font-size: 16px;
  font-weight: 600;
}

.preview-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e8eaed;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #4facfe;
}

/* 智能提示样式 */
.smart-hints {
  margin-top: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #fff9e6 0%, #fffbf0 100%);
  border-radius: 12px;
  border: 1px solid #ffd700;
}

.smart-hints h4 {
  margin: 0 0 12px 0;
  color: #e6a23c;
  font-size: 16px;
  font-weight: 600;
}

.hints-list {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.hints-list li {
  margin-bottom: 8px;
  color: #b8860b;
  font-size: 14px;
  line-height: 1.5;
}

/* 推荐结果区域 */
.recommendation-results {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 摘要卡片样式 */
.summary-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.summary-content {
  padding: 8px 0;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #4facfe;
  margin-bottom: 8px;
  display: block;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

/* 主推荐卡片样式 */
.primary-recommendation-card {
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
  border: 2px solid #4facfe;
  overflow: hidden;
  position: relative;
}

.primary-recommendation-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.primary-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.confidence-badge {
  margin-left: auto;
  padding: 4px 12px;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.primary-content {
  padding: 8px 0;
}

.primary-model {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.model-showcase {
  position: relative;
}

.showcase-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 12px;
}

.model-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.model-details h3 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.model-price {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #4facfe;
}

.model-specs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.match-radar {
  margin-top: 20px;
}

.match-radar h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.radar-chart {
  height: 200px;
  width: 100%;
}

/* 推荐理由样式 */
.recommendation-reasons {
  margin-bottom: 24px;
}

.recommendation-reasons h4 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.reasons-list {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.reasons-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.reasons-list li .el-icon {
  color: #67c23a;
  font-size: 16px;
  margin-top: 2px;
  flex-shrink: 0;
}

/* 核心优势样式 */
.core-advantages h4 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.advantages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.advantage-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.advantage-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.advantage-item .el-icon {
  font-size: 24px;
  color: #4facfe;
  flex-shrink: 0;
}

.advantage-info {
  flex: 1;
}

.advantage-info h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.advantage-info p {
  margin: 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.3;
}

.advantage-score {
  font-size: 16px;
  font-weight: 700;
  color: #4facfe;
}

.primary-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  border-top: 1px solid #f0f2f5;
}

/* 备选推荐样式 */
.alternatives-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.alternatives-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.alternatives-content {
  padding: 8px 0;
}

.alternatives-tabs .el-tabs__header {
  margin: 0;
}

.alternative-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
  padding: 16px 0;
}

.alternative-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.alternative-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #4facfe;
}

.alternative-image {
  width: 100px;
  height: 70px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.alternative-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alternative-info h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.alternative-price {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #4facfe;
}

.alternative-highlight {
  margin: 4px 0;
}

.match-score {
  margin-top: auto;
}

.match-score span {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
  display: block;
}

.alternative-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-self: center;
}

/* 对比工具样式 */
.comparison-tool-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #4facfe;
  overflow: hidden;
}

.comparison-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.comparison-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.comparison-content {
  padding: 8px 0;
}

.comparison-models {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.comparison-model {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  min-width: 200px;
}

.comparison-image {
  width: 60px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
}

.comparison-info {
  flex: 1;
}

.comparison-info h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.comparison-info p {
  margin: 0;
  font-size: 12px;
  color: #606266;
}

/* 购买建议样式 */
.purchase-advice-card {
  border-radius: 16px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  overflow: hidden;
}

.advice-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 18px;
}

.advice-content {
  padding: 8px 0;
}

.advice-sections {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.advice-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.timing-advice,
.channel-advice {
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
}

.advice-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.advice-item .el-icon {
  font-size: 20px;
  color: #4facfe;
  margin-top: 2px;
}

.advice-text h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.advice-text p {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.channel-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.channel-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border: 1px solid #e8eaed;
  border-radius: 8px;
}

.channel-item .el-icon {
  font-size: 20px;
  color: #4facfe;
}

.channel-info {
  flex: 1;
}

.channel-info h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.channel-info p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.checklist {
  padding: 16px;
  background: #f8fafb;
  border-radius: 12px;
}

.checklist-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.advice-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  border-top: 1px solid #f0f2f5;
}

/* 加载状态样式 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-card {
  min-width: 400px;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}

.loading-content {
  text-align: center;
  padding: 40px 20px;
}

.loading-icon {
  font-size: 48px;
  color: #4facfe;
  margin-bottom: 16px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loading-content h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.loading-content p {
  margin: 0 0 20px 0;
  font-size: 14px;
  color: #606266;
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

.el-tag {
  border-radius: 6px;
  font-weight: 500;
}

.el-progress__text {
  font-size: 12px !important;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .primary-model {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .advantages-grid {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }

  .alternative-group {
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .recommendation-analysis {
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
  }

  .filter-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .budget-options,
  .body-type-options,
  .energy-options,
  .passenger-options,
  .brand-options,
  .usage-options,
  .mileage-options {
    grid-template-columns: repeat(2, 1fr);
    justify-content: center;
  }

  .filter-option {
    min-height: 70px;
    padding: 12px 8px;
  }

  .preview-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .summary-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .primary-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .confidence-badge {
    margin-left: 0;
    text-align: center;
  }

  .primary-actions {
    flex-direction: column;
  }

  .alternatives-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .alternative-group {
    grid-template-columns: 1fr;
  }

  .alternative-item {
    flex-direction: column;
    text-align: center;
  }

  .alternative-image {
    width: 100%;
    height: 120px;
  }

  .alternative-actions {
    flex-direction: row;
    align-self: stretch;
  }

  .comparison-header {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .comparison-models {
    flex-direction: column;
  }

  .comparison-model {
    min-width: auto;
  }

  .advice-actions {
    flex-direction: column;
  }

  .channel-options {
    gap: 8px;
  }

  .channel-item {
    flex-direction: column;
    text-align: center;
  }

  .loading-card {
    min-width: 320px;
    margin: 0 16px;
  }
}

@media (max-width: 480px) {
  .budget-options,
  .body-type-options,
  .energy-options,
  .passenger-options,
  .brand-options,
  .usage-options,
  .mileage-options {
    grid-template-columns: 1fr;
  }

  .preview-stats,
  .summary-stats {
    grid-template-columns: 1fr;
  }

  .advantages-grid {
    grid-template-columns: 1fr;
  }

  .stat-number {
    font-size: 24px;
  }

  .header-left h2 {
    font-size: 20px;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .recommendation-analysis {
    background: #1a1a1a;
    color: #e4e7ed;
  }

  .filter-card,
  .summary-card,
  .primary-recommendation-card,
  .alternatives-card,
  .comparison-tool-card,
  .purchase-advice-card {
    background: #2d2d2d;
    border-color: #404040;
  }

  .filter-option,
  .stat-card,
  .advantage-item,
  .alternative-item,
  .comparison-model,
  .channel-item {
    background: #2d2d2d;
    border-color: #404040;
  }

  .filter-preview,
  .smart-hints,
  .timing-advice,
  .channel-advice,
  .checklist {
    background: #363636;
  }

  .loading-card {
    background: #2d2d2d;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .filter-option,
  .alternative-item,
  .comparison-model,
  .channel-item {
    border-width: 2px;
    border-color: #000;
  }

  .showcase-image,
  .alternative-image,
  .comparison-image {
    border: 2px solid #000;
  }
}

/* 打印样式 */
@media print {
  .recommendation-analysis {
    background: white !important;
  }

  .header-actions,
  .filter-stats,
  .primary-actions,
  .alternative-actions,
  .comparison-actions,
  .advice-actions {
    display: none !important;
  }

  .filter-card,
  .summary-card,
  .primary-recommendation-card,
  .alternatives-card,
  .purchase-advice-card {
    break-inside: avoid;
    margin-bottom: 12px;
    box-shadow: none !important;
    border: 1px solid #ccc !important;
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
.filter-option:focus-visible,
.alternative-item:focus-visible,
.comparison-model:focus-visible {
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

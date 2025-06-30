<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Download,
  TrendCharts,
  Operation, // 替代 Car
  Money,
  Shop,
  DataAnalysis, // 替代 PieChart
} from '@element-plus/icons-vue'

// 响应式数据
const totalSales = ref(15267.8)
const totalVehicles = ref(4521)
const avgPrice = ref(33.8)
const dealerCount = ref(156)
const chartTimeRange = ref('6m')
const rankingType = ref('volume')

// 排行榜数据
const rankingData = ref([
  { model: 'Model Y', sales: 1245, revenue: 4235.2, growth: 15.3, marketShare: 27.5 },
  { model: 'Model 3', sales: 987, revenue: 2961.0, growth: 8.7, marketShare: 21.8 },
  { model: 'Model S', sales: 654, revenue: 5886.0, growth: -2.1, marketShare: 14.5 },
  { model: 'Model X', sales: 432, revenue: 3888.0, growth: 12.9, marketShare: 9.6 },
  { model: 'Cybertruck', sales: 321, revenue: 1926.0, growth: 45.2, marketShare: 7.1 },
])

// 格式化数字
const formatNumber = (num: number) => {
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 1 })
}

// 刷新数据
const refreshData = () => {
  ElMessage.success('数据已刷新')
  // 这里可以调用API刷新数据
}

// 导出数据
const exportData = () => {
  ElMessage.success('报告导出功能开发中...')
  // 这里可以实现导出功能
}

onMounted(() => {
  ElMessage.success('欢迎使用销售总览页面！')
})
</script>

<template>
  <div class="sale-total-view">
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>销售总览</h2>
          <p>汽车销售数据统计分析</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData">刷新数据</el-button>
          <el-button type="success" :icon="Download" @click="exportData">导出报告</el-button>
        </div>
      </div>
    </el-card>

    <!-- 数据统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total-sales">
              <el-icon :size="32"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <h3>{{ formatNumber(totalSales) }}</h3>
              <p>总销售额 (万元)</p>
              <span class="stat-trend positive">+12.5%</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total-vehicles">
              <el-icon :size="32"><Operation /></el-icon>
            </div>
            <div class="stat-info">
              <h3>{{ formatNumber(totalVehicles) }}</h3>
              <p>销售车辆 (台)</p>
              <span class="stat-trend positive">+8.3%</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon avg-price">
              <el-icon :size="32"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <h3>{{ formatNumber(avgPrice) }}</h3>
              <p>平均售价 (万元)</p>
              <span class="stat-trend positive">+3.7%</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon dealers">
              <el-icon :size="32"><Shop /></el-icon>
            </div>
            <div class="stat-info">
              <h3>{{ dealerCount }}</h3>
              <p>经销商数量</p>
              <span class="stat-trend neutral">+2</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-section">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>月度销售趋势</span>
              <el-radio-group v-model="chartTimeRange" size="small">
                <el-radio-button value="6m">近6个月</el-radio-button>
                <el-radio-button value="1y">近1年</el-radio-button>
                <el-radio-button value="2y">近2年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-placeholder">
            <el-icon :size="48" color="#dcdfe6"><TrendCharts /></el-icon>
            <p>销售趋势图表 (需要集成图表库如 ECharts)</p>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>车型销售占比</span>
          </template>
          <div class="chart-placeholder">
            <el-icon :size="48" color="#dcdfe6"><DataAnalysis /></el-icon>
            <p>车型占比饼图</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 销售排行榜 -->
    <el-card shadow="never" class="ranking-card">
      <template #header>
        <div class="card-header">
          <span>销售排行榜</span>
          <el-select v-model="rankingType" placeholder="选择排行类型" style="width: 150px">
            <el-option label="按销量" value="volume" />
            <el-option label="按销售额" value="revenue" />
            <el-option label="按增长率" value="growth" />
          </el-select>
        </div>
      </template>

      <el-table :data="rankingData" style="width: 100%">
        <el-table-column type="index" label="排名" width="80" />
        <el-table-column prop="model" label="车型" />
        <el-table-column prop="sales" label="销量 (台)" />
        <el-table-column prop="revenue" label="销售额 (万元)" />
        <el-table-column prop="growth" label="增长率">
          <template #default="scope">
            <span :class="scope.row.growth >= 0 ? 'text-success' : 'text-danger'">
              {{ scope.row.growth >= 0 ? '+' : '' }}{{ scope.row.growth }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="marketShare" label="市场份额">
          <template #default="scope">
            <el-progress :percentage="scope.row.marketShare" :stroke-width="8" :show-text="false" />
            <span class="ml-2">{{ scope.row.marketShare }}%</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.sale-total-view {
  padding: 0;
}

.page-header {
  margin-bottom: 20px;
  border-radius: 8px;
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

.header-actions {
  display: flex;
  gap: 12px;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.total-sales {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.total-vehicles {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.avg-price {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.dealers {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info h3 {
  margin: 0 0 4px 0;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.stat-info p {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
}

.stat-trend {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 4px;
}

.stat-trend.positive {
  background: #f0f9ff;
  color: #0ea5e9;
}

.stat-trend.neutral {
  background: #f8fafc;
  color: #64748b;
}

.charts-section {
  margin-bottom: 20px;
}

.chart-card,
.ranking-card {
  border-radius: 8px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #909399;
  background: #fafafa;
  border-radius: 4px;
  margin: 20px 0;
}

.chart-placeholder p {
  margin: 12px 0 0 0;
  font-size: 14px;
}

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}

.ml-2 {
  margin-left: 8px;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .stat-content {
    flex-direction: column;
    text-align: center;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>

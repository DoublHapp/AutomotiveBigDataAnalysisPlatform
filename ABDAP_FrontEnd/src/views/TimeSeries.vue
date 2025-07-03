<script setup lang="ts">
import { ref, watch, nextTick, onMounted, computed } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

// 主筛选类型
const compareMode = ref<'region' | 'carModel' | 'none'>('carModel')

// 选项
const carModelOptions = ['宝马X3', '奥迪A6', '特斯拉Model Y', '比亚迪汉']
const regionOptionsAll = ['全国', '北京省', '上海省', '广东省', '浙江省', '山东省']
const regionOptionsNoAll = regionOptionsAll.slice(1) // 不含全国

// 筛选条件
const dateRangeType = ref<'month' | 'quarter' | 'year'>('month')
const dateRange = ref<[string, string]>(['2023-01', '2023-12'])
const carModelTargets = ref<string[]>(['宝马X3'])
const regionTargets = ref<string[]>(['全国'])
const powerType = ref<string>('')

// 计算当前可选项和选择方式
const carModelMultiple = computed(() => compareMode.value == 'carModel')
const regionMultiple = computed(() => compareMode.value == 'region')
const regionOptions = computed(() =>
  compareMode.value === 'region'
    ? regionOptionsNoAll
    : regionOptionsAll
)

// 图表实例
const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

// 统计数据
const statsList = ref<{ name: string, min: number, max: number, avg: number }[]>([])

// 监听筛选条件变化，刷新图表
const handleFilterChange = () => {
  fetchDataAndRender()
}

// 选择时自动修正选项
watch(compareMode, (mode) => {
  if (mode === 'region') {
    // 地区多选，车型单选，地区包含全国
    carModelTargets.value = [carModelOptions[0]]
    regionTargets.value = [regionOptionsNoAll[0]]
  } else if (mode === 'carModel') {
    // 车型多选，地区单选，不含全国
    regionTargets.value = [regionOptionsAll[0]]
    carModelTargets.value = ['宝马X3']
  } else {
    // 不限，地区多选不含全国，车型多选
    regionTargets.value = [regionOptionsNoAll[0]]
    carModelTargets.value = [carModelOptions[0]]
  }
})

// 生成模拟数据
function generateMockData() {
  const [start, end] = dateRange.value
  const dateList: string[] = []
  let cur = new Date(start + '-01')
  const endDate = new Date(end + '-01')
  if (dateRangeType.value === 'month') {
    while (cur <= endDate) {
      dateList.push(cur.toISOString().slice(0, 7))
      cur.setMonth(cur.getMonth() + 1)
    }
  } else if (dateRangeType.value === 'quarter') {
    while (cur <= endDate) {
      const year = cur.getFullYear()
      const month = cur.getMonth()
      const quarter = Math.floor(month / 3) + 1
      dateList.push(`${year}-Q${quarter}`)
      cur.setMonth(month + 3)
    }
  } else if (dateRangeType.value === 'year') {
    while (cur <= endDate) {
      dateList.push(cur.getFullYear().toString())
      cur.setFullYear(cur.getFullYear() + 1)
    }
  }

  let targets: { region: string, carModel: string }[] = []
  if (compareMode.value === 'region') {
    // 地区多选，车型单选
    regionTargets.value.forEach(region =>
      targets.push({ region, carModel: carModelTargets.value[0] })
    )
  } else if (compareMode.value === 'carModel') {
    // 车型多选，地区单选
    carModelTargets.value.forEach(carModel =>
      targets.push({ region: regionTargets.value[0], carModel })
    )
  } else {
    // 不限，地区和车型多选，耦合为独立项
    regionTargets.value.forEach(region =>
      carModelTargets.value.forEach(carModel =>
        targets.push({ region, carModel })
      )
    )
  }

  return targets.map((target, idx) => {
    const base = 2000 + Math.floor(Math.random() * 2000)
    const fluct = 500 + Math.floor(Math.random() * 500)
    const data = dateList.map((_d, i) => base + Math.floor(Math.sin(i / 2 + idx) * fluct + Math.random() * 200))
    return {
      name: `${target.region}-${target.carModel}`,
      data,
      dateList
    }
  })
}

// 预留接口函数
async function fetchTrendData() {
  // 这里实际应调用后端接口
  // const res = await axios.get('/api/xxx', { params: { ... } })
  // return res.data
  return generateMockData()
}

// 渲染图表
async function fetchDataAndRender() {
  const seriesData = await fetchTrendData()
  if (!chartRef.value) return

  // 统计
  statsList.value = seriesData.map((item: any) => {
    const min = Math.min(...item.data)
    const max = Math.max(...item.data)
    const avg = Math.round(item.data.reduce((a: number, b: number) => a + b, 0) / item.data.length)
    return { name: item.name, min, max, avg }
  })

  // 定义颜色数组
  const colorList = [
    '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#1abc9c', '#9b59b6', '#e67e22'
  ]

  // 多条折线
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'line' // 可选：'line'、'shadow'、'cross'
      },
      formatter: function (params: any) {
        let tooltipText = `${params[0].axisValue}<br/>`;
        params.forEach((item: any) => {
          tooltipText += `${item.marker} ${item.seriesName}：${item.data}<br/>`;
        });
        return tooltipText;
      }
    },
    legend: {
      data: seriesData.map((item: any) => item.name)
    },
    xAxis: {
      type: 'category',
      data: seriesData[0]?.dateList || [],
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      name: '销量(台)'
    },
    series: seriesData.map((item: any, idx: number) => ({
      name: item.name,
      type: 'line',
      data: item.data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2 },
      itemStyle: { 
        color: (params: any) => colorList[params.dataIndex % colorList.length],
      },
      markPoint: {
        data: [
          { type: 'max', name: '最大值' },
          { type: 'min', name: '最小值' },
          { type: 'average', name: '平均值' }
        ]
      }
    }))
  }

  await nextTick()
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value!)
  }
  chartInstance.setOption(option, true)
  chartInstance.resize()
}

onMounted(() => {
  fetchDataAndRender()
})

watch([dateRangeType, dateRange, carModelTargets, regionTargets, powerType], fetchDataAndRender)
</script>

<template>
  <div class="time-series-view">
    <el-card class="filter-card" shadow="never">
      <div class="filter-content">
        <div class="filter-time">
          <!-- 时间粒度选择 -->
          <el-radio-group v-model="dateRangeType" @change="handleFilterChange">
            <el-radio-button label="month">月</el-radio-button>
            <el-radio-button label="quarter">季度</el-radio-button>
            <el-radio-button label="year">年</el-radio-button>
          </el-radio-group>
          <!-- 时间范围选择 -->
          <el-date-picker
            v-model="dateRange"
            type="monthrange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM"
            value-format="YYYY-MM"
            style="margin-left: 16px; max-width: 260px"
            @change="handleFilterChange"
          />
        </div>
        <br>
        <div class="filter-options">
          <!-- 主筛选类型选择 -->
          <el-radio-group v-model="compareMode" @change="handleFilterChange">
            <el-radio-button label="carModel">以车型为主</el-radio-button>
            <el-radio-button label="region">以地区为主</el-radio-button> 
            <el-radio-button label="none">不限</el-radio-button>
          </el-radio-group>
          <!-- 车型选择 -->
          <el-select
            v-model="carModelTargets"
            :multiple="carModelMultiple"
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择车型"
            style="margin-left: 16px; width: 220px; max-height: 32px; overflow: hidden;vertical-align: middle;"
            @change="handleFilterChange"
          >
            <el-option
              v-for="item in carModelOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
          <!-- 地区选择 -->
          <el-select
            v-model="regionTargets"
            :multiple="regionMultiple"
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择地区"
            style="margin-left: 16px; width: 220px; vertical-align: middle;"
            @change="handleFilterChange"
          >
            <el-option
              v-for="item in regionOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
          <!-- 动力类型选择 -->
          <el-select v-model="powerType" placeholder="动力类型" style="margin-left: 16px; width: 120px" @change="handleFilterChange">
            <el-option label="全部" value="" />
            <el-option label="燃油" value="fuel" />
            <el-option label="新能源" value="electric" />
          </el-select>
        </div>
      </div>
    </el-card>

    <el-card class="chart-card" shadow="never" style="margin-top: 20px;">
      <template #header>
        <span>销量趋势对比</span>
      </template>
      <div ref="chartRef" class="chart-container" style="height: 420px;"></div>
      <div class="stats-summary" v-if="statsList.length">
        <div v-for="(stats, idx) in statsList" :key="idx" class="stats-item">
          <span class="stats-label">{{ stats.name }}</span>
          <span class="stats-value">最小值: {{ stats.min }}</span>
          <span class="stats-value">最大值: {{ stats.max }}</span>
          <span class="stats-value">平均值: {{ stats.avg }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.filter-content {
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  height: 100px;
  padding-left: 30px;
}

.filter-time{
  display: flex;
  justify-content: flex-start;
  gap: 48px;
}

.filter-options {
  display: flex;
  justify-content: flex-start;
  gap: 32px;
  margin-top: 10px;
}

</style>
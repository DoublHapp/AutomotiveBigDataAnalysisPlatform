<script setup lang="ts">
import { ref, watch, nextTick, onMounted, computed } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import axios from 'axios'


// 筛选模式类型
const compareMode = ref<'region' | 'carModel' | 'none'>('carModel')

// 选项
// 地区
interface RegionNode {
  id: string
  name: string
  value?: string
  children?: RegionNode[]
}

interface Region{
  id: string
  name: string
  parentRegion: string
}

const regionTree = ref<RegionNode[]>([])

interface regionOption {
  label: string
  value: string
}

const regionOptionsAllFake: regionOption[] = [
  { label: '全国', value: '0' },
  { label: '北京市', value: '北京' },
  { label: '上海市', value: '上海' },
  { label: '广东省', value: '广东' },
  { label: '浙江省', value: '浙江' },
  { label: '山东省', value: '山东' }
]
const regionOptionsNoAllFake = regionOptionsAllFake.slice(1) // 不含全国
const regionOptions = ref<regionOption[]>([])
// 车型
interface carModelOption {
  label: string
  value: string
}

const carModelOptionsFake: carModelOption[] = [
  { label: '宝马X3', value: '1' },
  { label: '奔驰C200L', value: '2' },
  { label: '奥迪A4L', value: '3' },
  { label: '特斯拉Model 3', value: '4' },
  { label: '比亚迪汉EV', value: '5' }
]

const carModelOptions = ref<carModelOption[]>([])
// 动力类型

interface powerOption {
  label: string
  value: string
}

const powerOptions: powerOption[] = [
  { label: '全部', value: 'all' },
  { label: '燃油', value: 'fuel' },
  { label: '新能源', value: 'electric' }
]

const powerType = ref<string>('all')

// 筛选条件
const dateRangeType = ref<'month' | 'quarter' | 'year'>('month')
const dateRange = ref<[string, string]>(['2023-01', '2023-12'])
const regionSingle = ref<string>('北京'); 
const regionArray = ref<string[]>([]);
const carModelSingle = ref<string>(''); 
const carModelArray = ref<string[]>([]);

// 计算当前可选项和选择方式
const carModelMultiple = computed(() => compareMode.value == 'carModel' || compareMode.value == 'none')
const regionMultiple = computed(() => compareMode.value == 'region' || compareMode.value == 'none')

// 计算当前选择数组还是对象
const carModelTargets = computed<any>({
  get() {
    return carModelMultiple.value ? carModelArray.value : carModelSingle.value;
  },
  set(newVal) {
    if (carModelMultiple.value) {
      carModelArray.value = newVal;
    } else {
      carModelSingle.value = newVal;
    }
  },
});

// 在<script setup>中添加
// 构建el-cascader所需的options
const regionCascaderOptions = computed(() => {
  // regionTree.value 需为 [{ label, value, children: [...] }]
  // 若regionTree为空可用regionOptionsAllFake等模拟
  if (regionTree.value.length > 0) {
    // 转换RegionNode为el-cascader格式
    return regionTree.value.map(province => ({
      label: province.name,
      value: province.id,
      children: province.children?.map(city => ({
        label: city.name,
        value: city.id
      })) || []
    }))
  } else {
    // 用模拟数据
    return regionOptionsNoAllFake.map(opt => ({
      label: opt.label,
      value: opt.value
    }))
  }
})

// el-cascader props
const regionCascaderProp = {
  checkStrictly: false, // 只可选叶子节点
  emitPath: false // 只返回选中节点的value
}
const regionCascaderProps = {
  multiple: true,
  checkStrictly: false, // 只可选叶子节点
  emitPath: false // 只返回选中节点的value
}

const regionTargets = computed<any>({
  get() {
    return regionMultiple.value ? regionArray.value : regionSingle.value;
  },
  set(newVal) {
    if (carModelMultiple) {
      regionArray.value = newVal;
    } else {
      regionSingle.value = newVal;
    }
  },
});

const getSafeRegionArray = () => {
  return regionMultiple.value 
    ? regionArray.value 
    : regionSingle.value 
      ? [regionSingle.value] 
      : [];
};

const getSafeCarModelArray = () => {
  return carModelMultiple.value 
    ? carModelArray.value 
    : carModelSingle.value 
      ? [carModelSingle.value] 
      : [];
};

// 图表实例
const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

const barChartRef = ref<HTMLDivElement | null>(null)
let barChartInstance: echarts.ECharts | null = null


// 统计数据
const statsList = ref<{ name: string, min: number, max: number, avg: number, data: number[], dateList: string[] }[]>([])
const statsDetailVisible = ref(false)
const currentStats = ref<{ name: string, min: number, max: number, avg: number, data: number[], dateList: string[] } | null>(null)

function showStatsDetail(stats: { name: string, min: number, max: number, avg: number, data: number[], dateList: string[] }) {
  currentStats.value = stats
  statsDetailVisible.value = true
}

// 选择筛选模式时自动修正选项
watch(compareMode, (mode) => {
  console.log('当前筛选模式:', regionMultiple.value, carModelMultiple.value)  
  console.log('当前地区选项:', regionTargets.value, typeof regionArray.value, regionSingle.value)
  if (mode === 'region') {
    // 地区多选，车型单选，不包含全国
    regionArray.value = [];
    carModelSingle.value = ''; 
  } else if (mode === 'carModel') {
    // 车型多选，地区单选，包含全国
    carModelArray.value = [];
    regionSingle.value = '';
  } else {
    // 不限，地区多选不含全国，车型多选
    regionArray.value = [];
    carModelArray.value = [];
  }
})

// 监听弹窗打开时渲染柱状图
watch(statsDetailVisible, async (visible) => {
  if (visible && currentStats.value) {
    await nextTick()
    if (barChartRef.value) {
      if (!barChartInstance) {
        barChartInstance = echarts.init(barChartRef.value)
      }
      barChartInstance.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: currentStats.value.dateList },
        yAxis: { type: 'value', name: '销量(台)' },
        series: [{
          type: 'bar',
          data: currentStats.value.data,
          animation: true, 
          animationDuration: 1000, 
          animationEasing: 'elasticOut', 
          animationDelay: function (idx: number) { 
            return idx * 100; 
          },
          label: {
            show: true,
            position: 'top'
          },
          itemStyle: { color: '#409EFF' }
        }]
      })
      barChartInstance.resize()
    }
  }
})

// 弹窗关闭时销毁实例释放内存
watch(statsDetailVisible, (visible) => {
  if (!visible && barChartInstance) {
    barChartInstance.dispose()
    barChartInstance = null
  }
})

interface repsonseData {
  saleId: number;
  carModelId: number;
  carModelName: string;
  regionId: number;
  regionName: string;
  saleMonth: string;
  saleCount: number;
  saleAmount: number;
}

interface chartData {
  name: string;
  data: number[];
  dateList: string[];
}

function processResponseData(salesData: repsonseData[]): chartData[] {
  // 创建一个Map来按名称分组数据
  const groupedData = new Map<string, { counts: number[]; dates: string[] }>();

  // 遍历所有销售记录
  salesData.forEach(record => {
    const key = record.regionName ===  "全国"?`${record.carModelName}`:`${record.carModelName}--${record.regionName}`;
    
    // 如果Map中还没有这个key，就初始化
    if (!groupedData.has(key)) {
      groupedData.set(key, { counts: [], dates: [] });
    }
    
    // 获取当前分组
    const group = groupedData.get(key)!;
    
    // 添加销售数量和日期
    group.counts.push(record.saleCount);
    group.dates.push(record.saleMonth);
  });

  // 将Map转换为目标数组
  const result: chartData[] = [];
  groupedData.forEach((value, key) => {
    // 按日期排序（如果需要）
    const sortedIndices = value.dates
      .map((date, index) => ({ date, index }))
      .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
      .map(item => item.index);
    
    // 按日期顺序重新排列数据
    const sortedData = sortedIndices.map(i => value.counts[i]);
    const sortedDates = sortedIndices.map(i => value.dates[i]);

    result.push({
      name: key,
      data: sortedData,
      dateList: sortedDates
    });
  });

  return result;
}

// API接口调用函数
const fetchCarModels = async () => {
  try {
    const response = await axios.get('/api/car-models/page?page=1&size=20')
    if (response.data.status === 200) {
      response.data.data.forEach((model: any) => {
        carModelOptions.value.push({
          label: `${model.modelName}`, //要改，现在品牌和车型分开了，接口和模拟数据不匹配。
          value: model.carModelId.toString(),
        })
      })
      console.log('获取车型列表成功:', response)
    } else {
      console.error('获取车型列表失败:', response.data.message)
    }
  } catch (error) {
    console.error('获取车型列表失败:', error)
  }
}

// 创建树结构
function addChildNode(
  tree: RegionNode[],
  newRegion: Region
): boolean {
  for (const node of tree) {
    if (node.name === newRegion.parentRegion) {
      if (!node.children) {
        node.children = []
      }
      // 添加市级节点
      const newChild: RegionNode = {
        id: newRegion.id,
        name: newRegion.name,
      }
      node.children.push(newChild)
      return true
    }
  }

  // 找不到目标节点，创建一个新的省级节点
  const newNode: RegionNode = {
    id: newRegion.parentRegion,
    name: newRegion.parentRegion,
    children: []
  }
  tree.push(newNode)

  // 添加市级节点
  const newChild: RegionNode = {
    id: newRegion.id,
    name: newRegion.name,
  }
  const lastNode = tree[tree.length - 1]
  lastNode.children!.push(newChild)
  return true // 成功添加新节点
}


const fetchRegions = async () => {
  try {
    const response = await axios.get('/api/regions')
    if (response.data.status === 200) {
      const res: any[] = response.data.data
      res.forEach((region: any) => {
        addChildNode(regionTree.value, {
          id: region.regionId,
          name: region.regionName,
          parentRegion: region.parentRegion,
        })
        regionOptions.value.push({
          label: region.parentRegion.toString(),
          value: region.regionId.toString(),
        })
      })
      console.log('获取地区列表成功:', response)
    } else {
      console.error('获取地区列表失败:', response.data.message)
    }
  } catch (error) {
    console.error('获取地区列表失败:', error)
  }
}

async function fetchTrendData() {
  const params = new URLSearchParams();
  if(regionMultiple.value){
    regionArray.value.forEach(item => {
      params.append('regionIds', item.toString());
    });
  }else{
    console.log(regionSingle.value)
    params.append('regionIds', regionSingle.value.toString());
  }
  console.log('params:',params.toString())
  if(carModelMultiple.value){
    carModelArray.value.forEach(item => {
      params.append('carModelIds', item.toString());
    });
  }else{
    params.append('carModelIds', carModelSingle.value.toString());
  }
  const res = await axios.get(`/api/sale-records/multiple?${params.toString()}`)
  return processResponseData(res.data.data)
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
    return { name: item.name, min, max, avg, data: item.data, dateList: item.dateList }
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
      animation: true,
      animationDuration: 1000,
      animationEasing: 'elasticOut',
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
  fetchCarModels()
  fetchRegions()
})
</script>

<template>
  <div class="time-series-view">
    <el-card class="filter-card" shadow="never">
      <div class="filter-content">
        <div class="filter-time">
          <!-- 时间粒度选择 -->
          <el-radio-group v-model="dateRangeType">
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
          />
        </div>
        <br>
        <div class="filter-options">
          <!-- 主筛选类型选择 -->
          <el-radio-group v-model="compareMode">
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
          >
            <el-option
              v-for="item in carModelOptions"
              :key="item.label"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <!-- 地区选择 -->
          <el-cascader
            v-if="regionMultiple==false"
            v-model="regionSingle"
            :options="regionCascaderOptions"
            :props="regionCascaderProp"
            :show-all-levels="false"
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择地区"
            style="margin-left: 16px; width: 220px; vertical-align: middle;"
            @change="(...args: any[]) => { console.log('el-cascader multiple:', regionMultiple, args) }"
          />
          <el-cascader
            v-else
            class="multiple-cascader"
            v-model="regionTargets"
            :options="regionCascaderOptions"
            :props="regionCascaderProps"
            :show-all-levels="false"
            multiple=true
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择地区"
            style="margin-left: 16px; width: 220px; vertical-align: middle;"
            @change="(...args: any[]) => { console.log('el-cascader multiple:', regionMultiple, args) }"
          />
          
          <!-- <el-select
            v-model="regionTargets"
            :multiple="regionMultiple"
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择地区"
            style="margin-left: 16px; width: 220px; vertical-align: middle;"
          >
            <el-option
              v-for="item in regionOptions"
              :key="item.label"
              :label="item.label"
              :value="item.value"
            />
          </el-select> -->
          <!-- 动力类型选择 -->
          <el-select v-model="powerType" placeholder="动力类型" style="margin-left: 16px; width: 120px">
            <el-option
              v-for="option in powerOptions"
              :key="option.label"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-button @click="fetchDataAndRender" style="margin-left: 30px;">更新数据</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="chart-summary-row" style="margin-top: 20px;">
      <el-col :lg="19" :md="16" :sm="24" class="chart-col">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <span>销量趋势对比</span>
          </template>
          <div ref="chartRef" class="chart-container" style="height: 420px;"></div>
        </el-card>
      </el-col>
      <el-col :lg="5" :md="8" :sm="24" class="summary-col">
        <el-card class="stats-card" shadow="never">
          <template #header>
            <span>关键数据</span>
          </template>
          <div class="stats-summary">
            <div
              v-for="(stats, idx) in statsList"
              :key="idx"
              class="stats-item clickable"
              @click="showStatsDetail(stats)"
            >
              <span class="stats-label">{{ stats.name }}</span>
              <span class="stats-value">最小值: {{ stats.min }}</span>
              <span class="stats-value">最大值: {{ stats.max }}</span>
              <span class="stats-value">平均值: {{ stats.avg }}</span>
            </div>
            <el-dialog v-model="statsDetailVisible" title="详细信息" width="700px" append-to-body>
              <div v-if="currentStats">
                <p><b>名称：</b>{{ currentStats.name }}</p>
                <p><b>最小值：</b>{{ currentStats.min }}</p>
                <p><b>最大值：</b>{{ currentStats.max }}</p>
                <p><b>平均值：</b>{{ currentStats.avg }}</p>
                <div ref="barChartRef" class="chart-container" style="width:100%;height:360px;margin-top:16px;"></div>
              </div>
            </el-dialog>
          </div>
        </el-card>
      </el-col>
    </el-row>
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
  gap: 20px;
  margin-top: 10px;
}

.stats-item.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s, background 0.2s;
}

.stats-value {
  margin-left: 8px;
  white-space: nowrap;
}

.chart-summary-row {
  width: 100%;
}

.chart-col {
  min-width: 0;
}

.summary-col {
  min-width: 0;
}

.stats-card {
  height: 100%;
  min-height: 420px;
  display: flex;
  flex-direction: column;
}

.stats-summary {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 12px;
  max-height: 380px;
  overflow-y: auto;
  padding: 4px 0;
  margin-top: 18px;
}

.stats-item {
  background: #d6f2ff;
  border-radius: 6px;
  padding: 8px 14px;
  font-size: 14px;
  color: #333;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  width: 100%;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: box-shadow 0.2s, background 0.2s;
}

.stats-item.clickable:hover {
  background: #98deff;
  box-shadow: 0 2px 8px rgba(64,158,255,0.12);
}

.stats-label {
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 2px;
}

.stats-value {
  white-space: nowrap;
  font-size: 13px;
  color: #666;
}

.el-dialog {
  width: 700px !important;
  max-width: 90vw !important;
}
</style>
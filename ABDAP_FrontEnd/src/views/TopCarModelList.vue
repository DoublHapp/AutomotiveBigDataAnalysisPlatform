<template>
  <div class="top-car-model-list">
    <el-card class="page-header" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <h2>热门车型排行</h2>
          <p>基于销量数据的车型排行榜</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshData">刷新</el-button>
        </div>
      </div>
    </el-card>

    <!-- 车型排行列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>热门车型 TOP 10</span>
          <el-select v-model="timeRange" placeholder="选择时间范围" style="width: 150px">
            <el-option label="本月" value="month" />
            <el-option label="本季度" value="quarter" />
            <el-option label="本年度" value="year" />
          </el-select>
        </div>
      </template>

      <div class="car-list">
        <div
          v-for="(car, index) in carModelList"
          :key="car.id"
          class="car-item"
          :class="{ 'top-three': index < 3 }"
        >
          <div class="ranking">
            <span class="rank-number" :class="`rank-${index + 1}`">{{ index + 1 }}</span>
          </div>

          <div class="car-image">
            <img :src="car.image" :alt="car.name" />
          </div>

          <div class="car-info">
            <h3>{{ car.name }}</h3>
            <p class="brand">{{ car.brand }}</p>
            <div class="specs">
              <span class="spec-item">{{ car.type }}</span>
              <span class="spec-item">{{ car.engine }}</span>
              <span class="spec-item">{{ car.transmission }}</span>
            </div>
          </div>

          <div class="car-stats">
            <div class="stat-item">
              <span class="label">销量</span>
              <span class="value">{{ car.sales.toLocaleString() }} 台</span>
            </div>
            <div class="stat-item">
              <span class="label">价格区间</span>
              <span class="value">{{ car.priceRange }}</span>
            </div>
            <div class="stat-item">
              <span class="label">满意度</span>
              <el-rate v-model="car.rating" disabled show-score />
            </div>
          </div>

          <div class="car-actions">
            <el-button type="primary" size="small" @click="viewDetails(car)"> 查看详情 </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

interface CarModel {
  id: number
  name: string
  brand: string
  type: string
  engine: string
  transmission: string
  sales: number
  priceRange: string
  rating: number
  image: string
}

const timeRange = ref('month')
const carModelList = ref<CarModel[]>([
  {
    id: 1,
    name: 'Model Y',
    brand: 'Tesla',
    type: 'SUV',
    engine: '纯电动',
    transmission: '单速变速箱',
    sales: 8456,
    priceRange: '26.39-41.79万',
    rating: 4.8,
    image: 'https://img.car-house.cn/Upload/activity/20220304/20220304154428_8493.jpg',
  },
  {
    id: 2,
    name: 'Model 3',
    brand: 'Tesla',
    type: '轿车',
    engine: '纯电动',
    transmission: '单速变速箱',
    sales: 7892,
    priceRange: '22.99-33.99万',
    rating: 4.7,
    image: 'https://img.car-house.cn/Upload/activity/20220304/20220304154428_8493.jpg',
  },
  {
    id: 3,
    name: 'BYD 汉EV',
    brand: 'BYD',
    type: '轿车',
    engine: '纯电动',
    transmission: '单速变速箱',
    sales: 6543,
    priceRange: '21.98-27.98万',
    rating: 4.6,
    image: 'https://img.car-house.cn/Upload/activity/20220304/20220304154428_8493.jpg',
  },
  {
    id: 4,
    name: 'NIO ES6',
    brand: 'NIO',
    type: 'SUV',
    engine: '纯电动',
    transmission: '单速变速箱',
    sales: 5234,
    priceRange: '35.80-52.60万',
    rating: 4.5,
    image: 'https://img.car-house.cn/Upload/activity/20220304/20220304154428_8493.jpg',
  },
  {
    id: 5,
    name: 'XPeng P7',
    brand: 'XPeng',
    type: '轿车',
    engine: '纯电动',
    transmission: '单速变速箱',
    sales: 4567,
    priceRange: '20.99-40.99万',
    rating: 4.4,
    image: 'https://img.car-house.cn/Upload/activity/20220304/20220304154428_8493.jpg',
  },
])

const refreshData = () => {
  ElMessage.success('数据已刷新')
}

const viewDetails = (car: CarModel) => {
  ElMessage.info(`查看 ${car.name} 详情功能开发中...`)
}

onMounted(() => {
  ElMessage.success('欢迎查看热门车型排行榜！')
})
</script>

<style scoped>
.top-car-model-list {
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.car-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.car-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.3s ease;
  background: white;
}

.car-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
}

.car-item.top-three {
  background: linear-gradient(135deg, #ffeaa7 0%, #fab1a0 100%);
  border-color: #e17055;
}

.ranking {
  margin-right: 20px;
  min-width: 50px;
  text-align: center;
}

.rank-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  font-size: 18px;
  font-weight: bold;
  color: white;
}

.rank-number.rank-1 {
  background: linear-gradient(135deg, #ffd700 0%, #ffb347 100%);
}

.rank-number.rank-2 {
  background: linear-gradient(135deg, #c0c0c0 0%, #a9a9a9 100%);
}

.rank-number.rank-3 {
  background: linear-gradient(135deg, #cd7f32 0%, #b8860b 100%);
}

.rank-number:not(.rank-1):not(.rank-2):not(.rank-3) {
  background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
}

.car-image {
  margin-right: 20px;
  width: 120px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
}

.car-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.car-info {
  flex: 1;
  margin-right: 20px;
}

.car-info h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.car-info .brand {
  margin: 0 0 8px 0;
  color: #909399;
  font-size: 14px;
}

.specs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.spec-item {
  padding: 2px 8px;
  background: #f0f2f5;
  color: #606266;
  font-size: 12px;
  border-radius: 4px;
}

.car-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-right: 20px;
  min-width: 150px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.stat-item .label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.stat-item .value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .car-item {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }

  .ranking,
  .car-image,
  .car-info,
  .car-stats {
    margin-right: 0;
    width: 100%;
  }

  .car-stats {
    flex-direction: row;
    justify-content: space-around;
  }

  .stat-item {
    align-items: center;
  }
}
</style>

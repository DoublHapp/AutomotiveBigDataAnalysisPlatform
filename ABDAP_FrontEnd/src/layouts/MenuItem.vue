<template>
  <div v-if="!item.meta?.hidden">
    <!-- 如果有子菜单 -->
    <el-sub-menu v-if="hasChildren" :index="item.path">
      <template #title>
        <el-icon v-if="item.meta?.icon">
          <component :is="getIconComponent(item.meta.icon)" />
        </el-icon>
        <span>{{ item.meta?.title }}</span>
      </template>

      <menu-item
        v-for="child in item.children"
        :key="child.path"
        :item="child"
        :base-path="resolvePath(child.path)"
      />
    </el-sub-menu>

    <!-- 单个菜单项 -->
    <el-menu-item v-else :index="resolvePath(item.path)">
      <el-icon v-if="item.meta?.icon">
        <component :is="getIconComponent(item.meta.icon)" />
      </el-icon>
      <template #title>
        <span>{{ item.meta?.title }}</span>
      </template>
    </el-menu-item>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MenuRoute } from '@/stores/RouterPermissionManagement'
import {
  TrendCharts,
  Star,
  Setting,
  Location,
  DataAnalysis,
  Timer,
  DataLine,
  MapLocation,
  ChatLineRound,
  ChatDotRound,
  Operation,
  Monitor,
  Tools,
  Connection,
} from '@element-plus/icons-vue'

interface Props {
  item: MenuRoute
  basePath: string
}

const props = defineProps<Props>()

const hasChildren = computed(() => {
  return props.item.children && props.item.children.length > 0
})

const resolvePath = (routePath: string) => {
  if (routePath.startsWith('/')) {
    return routePath
  }
  return `${props.basePath}/${routePath}`.replace(/\/+/g, '/')
}

/**
 * 根据图标名称获取对应的图标组件
 * 支持 Element Plus 图标库中的图标
 */
const getIconComponent = (iconName: string) => {
  const iconMap: Record<string, any> = {
    // 销售经理相关图标
    TrendCharts: TrendCharts, // 销售统计总览
    Location: Location, // 购车热区地图
    Timer: Timer, // 时间序列可视化
    DataAnalysis: DataAnalysis, // 多维度销售预测
    DataLine: DataLine, // 车型销售预测
    MapLocation: MapLocation, // 区域销售预测

    // 消费者相关图标
    Star: Star, // 热门车型排行榜
    Scale: Tools, // 车型对比分析 (使用 Tools 替代 Scale)
    ChatLineRound: ChatLineRound, // 购车推荐分析
    GasPump: Operation, // 油耗榜单 (使用 Operation 替代 GasPump)
    ChatDotRound: ChatDotRound, // 口碑聚合分析

    // 产品经理相关图标
    Setting: Setting, // 车辆配置热度分析


    // 通用图标
    Operation: Operation,
    Monitor: Monitor,
    Tools: Tools,
    Connection: Connection,
  }

  return iconMap[iconName] || Setting // 默认返回 Setting 图标
}
</script>

<style scoped>
/* 菜单项样式 */
.el-menu-item,
.el-sub-menu {
  border-radius: 6px;
  margin: 4px 8px;
  overflow: hidden;
}

.el-menu-item:hover,
.el-sub-menu:hover {
  background-color: rgba(24, 144, 255, 0.1) !important;
  color: #1890ff !important;
}

.el-menu-item.is-active {
  background-color: #1890ff !important;
  color: #fff !important;
  border-radius: 6px;
}

.el-menu-item.is-active .el-icon {
  color: #fff !important;
}

/* 子菜单标题样式 */
.el-sub-menu__title:hover {
  background-color: rgba(24, 144, 255, 0.1) !important;
  color: #1890ff !important;
}

/* 图标样式 */
.el-icon {
  margin-right: 8px;
  font-size: 16px;
  transition: all 0.3s ease;
}

/* 菜单文字样式 */
span {
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .el-menu-item,
  .el-sub-menu {
    margin: 2px 4px;
  }

  .el-icon {
    font-size: 14px;
  }

  span {
    font-size: 13px;
  }
}
</style>

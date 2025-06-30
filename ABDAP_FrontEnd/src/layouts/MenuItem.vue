<template>
  <div v-if="!item.meta?.hidden">
    <!-- 如果有子菜单 -->
    <el-sub-menu v-if="hasChildren" :index="item.path">
      <template #title>
        <el-icon v-if="item.meta?.icon">
          <component :is="item.meta.icon" />
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
        <component :is="item.meta.icon" />
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
</script>

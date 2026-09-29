<template>
  <ContentWrap title="业务模块">
    <div v-loading="loading">
      <el-result
        v-if="failed"
        icon="warning"
        title="模块信息加载失败"
        sub-title="请确认后端已重启且当前角色已分配业务模块查询权限。"
      >
        <template #extra><el-button @click="load">重新加载</el-button></template>
      </el-result>
      <template v-else-if="home">
        <h2 class="mb-12px text-18px font-600">{{ home.name }}</h2>
        <p class="text-[var(--el-text-color-secondary)]">{{ home.description }}</p>
      </template>
    </div>
  </ContentWrap>
</template>
<script setup lang="ts">
import { getBusinessHome, type BusinessHome } from '@/api/business/home'
defineOptions({ name: 'BusinessHome' })
const home = ref<BusinessHome>()
const loading = ref(false)
const failed = ref(false)
const load = async () => {
  loading.value = true
  failed.value = false
  try {
    home.value = await getBusinessHome()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

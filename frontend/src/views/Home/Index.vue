<template>
  <div class="workspace-home">
    <header class="home-heading"
      ><div
        ><p>工作台 / OVERVIEW</p><h1>{{ userStore.getUser.nickname || '你好' }}，欢迎回来</h1
        ><span>从常用入口开始今天的工作。</span></div
      ><time>{{ dateLabel }}</time></header
    >
    <section class="home-panel">
      <div class="section-title"><h2>常用入口</h2><span>按当前账号权限展示</span></div>
      <div v-if="entries.length" class="home-entries">
        <router-link
          v-for="entry in entries"
          :key="entry.name"
          :to="{ name: entry.name }"
          class="home-entry"
          ><span class="entry-icon"><Icon :icon="entry.icon" :size="22" /></span
          ><div
            ><h3>{{ entry.title }}</h3
            ><p>{{ entry.description }}</p></div
          ><Icon icon="ep:arrow-right" :size="14"
        /></router-link>
      </div>
      <el-empty v-else description="暂无可用入口，请联系管理员分配菜单权限" :image-size="80" />
    </section>
    <div class="home-lower">
      <section class="home-panel"
        ><div class="section-title"><h2>工作指引</h2></div
        ><div v-for="(item, i) in guidance" :key="item.title" class="home-guide"
          ><span>0{{ i + 1 }}</span
          ><div
            ><h3>{{ item.title }}</h3
            ><p>{{ item.description }}</p></div
          ></div
        ></section
      >
      <section class="home-panel home-note"
        ><span>个人工作空间</span><h2>清晰的分工，<br />有序的协作。</h2
        ><p>左侧菜单提供全部可用功能。使用顶部搜索快速定位页面，通过消息入口查看站内通知。</p
        ><router-link to="/user/profile"
          >查看个人资料 <Icon icon="ep:arrow-right" :size="14" /></router-link
      ></section>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useUserStore } from '@/store/modules/user'
defineOptions({ name: 'Index' })
const userStore = useUserStore()
const router = useRouter()
const dateLabel = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())
const entries = computed(() =>
  [
    {
      name: 'SystemUser',
      title: '用户管理',
      description: '维护成员账号与所属部门',
      icon: 'ep:user'
    },
    {
      name: 'SystemRole',
      title: '角色权限',
      description: '管理角色与功能访问范围',
      icon: 'ep:key'
    },
    {
      name: 'SystemDept',
      title: '组织架构',
      description: '维护部门与组织层级',
      icon: 'ep:office-building'
    },
    {
      name: 'ExampleTask',
      title: '示例任务',
      description: '创建、查询与管理个人任务',
      icon: 'ep:document'
    },
    {
      name: 'InfraFile',
      title: '文件管理',
      description: '查看与维护上传的文件',
      icon: 'ep:folder-opened'
    },
    { name: 'SystemLoginLog', title: '登录日志', description: '查询账号登录记录', icon: 'ep:clock' }
  ].filter((entry) => router.hasRoute(entry.name))
)
const guidance = [
  { title: '完善个人资料', description: '确认个人联系方式，方便接收工作通知。' },
  { title: '按职责使用功能', description: '菜单与操作权限由管理员按角色分配。' },
  { title: '及时查看消息', description: '关注站内通知，及时跟进需要处理的事项。' }
]
</script>
<style scoped lang="scss">
.workspace-home {
  max-width: 1440px;
  margin: 0 auto;
  h3 {
    margin: 0 0 7px;
    font-size: 14px;
    font-weight: 600;
  }
}
.home-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 12px 0 30px;
  p {
    margin-bottom: 12px;
    font-size: 12px;
    letter-spacing: 1px;
    color: var(--el-text-color-secondary);
  }
  h1 {
    margin-bottom: 12px;
    font-size: 26px;
    font-weight: 600;
  }
  span,
  time {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}
.home-panel {
  padding: 24px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
  }
  > span {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
.home-entries {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.home-entry {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  text-decoration: none;
  color: var(--el-text-color-primary);
  transition:
    border-color 0.15s,
    background 0.15s;
  > div {
    flex: 1;
  }
  &:hover {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-fill-color-light);
  }
  &:focus-visible {
    outline: 2px solid var(--el-color-primary);
    outline-offset: 3px;
  }
}
.home-entry p,
.home-guide p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.8;
}
.entry-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 6px;
  color: var(--el-color-primary);
  background: var(--el-fill-color-light);
}
.home-lower {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 20px;
  margin-top: 20px;
}
.home-guide {
  display: flex;
  gap: 20px;
  padding: 18px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  > span {
    color: var(--el-text-color-secondary);
    font-size: 13px;
    font-variant-numeric: tabular-nums;
  }
}
.home-note {
  padding: 32px;
  > span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
  h2 {
    margin: 24px 0 16px;
    font-size: 25px;
    line-height: 1.6;
    font-weight: 500;
  }
  p {
    max-width: 360px;
    color: var(--el-text-color-secondary);
    line-height: 1.9;
    font-size: 13px;
  }
  a {
    display: inline-flex;
    align-items: center;
    gap: 12px;
    margin-top: 24px;
    font-size: 13px;
    color: var(--el-color-primary);
    text-decoration: none;
  }
}
@media (max-width: 1100px) {
  .home-entries {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .home-heading {
    align-items: flex-start;
    flex-direction: column;
  }
  .home-entries,
  .home-lower {
    grid-template-columns: 1fr;
  }
  .home-panel {
    padding: 18px;
  }
}
</style>

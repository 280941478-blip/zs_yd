<template>
  <main class="workspace-login">
    <aside class="workspace-login__intro">
      <div class="brand"
        ><img src="@/assets/svgs/workspace-mark.svg" alt="" />{{ appStore.getTitle }}</div
      >
      <div class="intro-copy"
        ><p class="eyebrow">工作，从这里开始</p><h1>专注业务，<br />让协作更有序。</h1
        ><p>统一的工作入口，清晰的组织与权限。<br />连接日常工作中的每一个环节。</p
        ><div class="intro-details"
          ><span>组织管理</span><span>业务协作</span><span>消息通知</span></div
        ></div
      >
      <p class="intro-footer">企业管理工作台</p>
    </aside>
    <section class="login-main">
      <header
        ><span>{{ appStore.getTitle }}</span
        ><div><ThemeSwitch /><LocaleDropdown /></div
      ></header>
      <div class="form-area"
        ><p class="eyebrow">欢迎回来</p
        ><LoginForm /><MobileForm /><QrCodeForm /><RegisterForm /><SSOLoginVue /><ForgetPasswordForm /><p
          class="hint"
          >如需开通账号或调整权限，请联系管理员。</p
        ></div
      >
      <footer>© {{ new Date().getFullYear() }} {{ appStore.getTitle }}</footer>
    </section>
  </main>
</template>
<script lang="ts" setup>
import { useAppStore } from '@/store/modules/app'
import { ThemeSwitch } from '@/layout/components/ThemeSwitch'
import { LocaleDropdown } from '@/layout/components/LocaleDropdown'
import {
  LoginForm,
  MobileForm,
  QrCodeForm,
  RegisterForm,
  SSOLoginVue,
  ForgetPasswordForm
} from './components'
defineOptions({ name: 'Login' })
const appStore = useAppStore()
</script>
<style scoped lang="scss">
.workspace-login {
  display: grid;
  grid-template-columns: minmax(360px, 44%) 1fr;
  min-height: 100dvh;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
}
.workspace-login__intro {
  display: flex;
  flex-direction: column;
  padding: 40px 48px;
  color: #253b48;
  background: #e9eeef;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 19px;
  font-weight: 600;
  img {
    width: 36px;
    height: 36px;
  }
}
.intro-copy {
  margin: auto 0;
  padding: 72px 0;
  h1 {
    margin: 24px 0;
    font-size: clamp(32px, 3.3vw, 52px);
    font-weight: 600;
    line-height: 1.45;
    letter-spacing: -1px;
  }
  > p:last-of-type {
    color: #5d707b;
    font-size: 15px;
    line-height: 2;
  }
}
.eyebrow {
  margin-bottom: 16px;
  font-size: 13px;
  letter-spacing: 2px;
  color: #71828d;
}
.intro-details {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid #cdd7dc;
  font-size: 13px;
}
.intro-footer {
  color: #71828d;
  font-size: 12px;
}
.login-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 28px 40px;
  header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    > div {
      display: flex;
      align-items: center;
      gap: 14px;
    }
  }
  footer {
    text-align: center;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
.form-area {
  width: 100%;
  max-width: 380px;
  margin: auto;
  padding: 64px 0;
  :deep(.login-form) {
    width: 100%;
  }
  :deep(h2) {
    text-align: left;
    margin: 0 0 16px;
    font-size: 28px;
    font-weight: 600;
  }
  :deep(.el-input__wrapper),
  :deep(.el-button) {
    min-height: 44px;
  }
}
.hint {
  margin-top: 16px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.8;
}
.dark .workspace-login__intro {
  background: #222e35;
  color: #e4e9ed;
}
@media (max-width: 800px) {
  .workspace-login {
    grid-template-columns: 1fr;
  }
  .workspace-login__intro {
    display: none;
  }
  .login-main {
    padding: 24px;
  }
}
</style>

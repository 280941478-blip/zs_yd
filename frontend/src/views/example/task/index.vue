<template>
  <ContentWrap>
    <el-alert
      title="开发示例：仅查看和管理自己创建的任务；导入失败时整批回滚。"
      type="info"
      :closable="false"
      class="mb-16px"
    />
    <el-form :inline="true" :model="query" @submit.prevent="search">
      <el-form-item label="任务名称"
        ><el-input v-model="query.name" clearable @keyup.enter="search"
      /></el-form-item>
      <el-form-item label="任务编码"
        ><el-input v-model="query.code" clearable @keyup.enter="search"
      /></el-form-item>
      <el-form-item label="状态"
        ><el-select v-model="query.status" clearable class="!w-140px"
          ><el-option
            v-for="(label, value) in statuses"
            :key="value"
            :label="label"
            :value="value" /></el-select
      ></el-form-item>
      <el-form-item
        ><el-button @click="search">搜索</el-button
        ><el-button @click="reset">重置</el-button></el-form-item
      >
    </el-form>
    <el-space wrap>
      <el-button v-hasPermi="['example:task:create']" type="primary" @click="open()"
        >新增任务</el-button
      >
      <el-button v-hasPermi="['example:task:export']" :loading="exporting" @click="exportRows"
        >导出</el-button
      >
      <el-button v-hasPermi="['example:task:import']" @click="downloadTemplate"
        >下载导入模板</el-button
      >
      <el-button
        v-hasPermi="['example:task:import']"
        :loading="importing"
        @click="fileInput?.click()"
        >导入 Excel</el-button
      >
      <input ref="fileInput" type="file" accept=".xlsx,.xls" hidden @change="importRows" />
    </el-space>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="rows">
      <el-table-column label="编码" prop="code" min-width="140" />
      <el-table-column label="名称" prop="name" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="110"
        ><template #default="{ row }"
          ><el-tag>{{ statuses[row.status] }}</el-tag></template
        ></el-table-column
      >
      <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
      <el-table-column label="创建时间" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" width="150"
        ><template #default="{ row }">
          <el-button v-hasPermi="['example:task:update']" link type="primary" @click="open(row.id)"
            >编辑</el-button
          >
          <el-button v-hasPermi="['example:task:delete']" link type="danger" @click="remove(row.id)"
            >删除</el-button
          >
        </template></el-table-column
      >
    </el-table>
    <Pagination
      v-model:page="query.pageNo"
      v-model:limit="query.pageSize"
      :total="total"
      @pagination="load"
    />
  </ContentWrap>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑任务' : '新增任务'"
    width="520px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" v-loading="saving">
      <el-form-item label="任务编码" prop="code"
        ><el-input v-model="form.code" maxlength="32" placeholder="字母、数字、下划线或短横线"
      /></el-form-item>
      <el-form-item label="任务名称" prop="name"
        ><el-input v-model="form.name" maxlength="100"
      /></el-form-item>
      <el-form-item label="状态" prop="status"
        ><el-select v-model="form.status"
          ><el-option
            v-for="(label, value) in statuses"
            :key="value"
            :label="label"
            :value="value" /></el-select
      ></el-form-item>
      <el-form-item label="备注"
        ><el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save">保存</el-button></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import * as api from '@/api/example/task'
import type { FormInstance, FormRules } from 'element-plus'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
defineOptions({ name: 'ExampleTask' })
const message = useMessage()
const statuses = ['待办', '进行中', '完成']
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  name: '',
  code: '',
  status: undefined as number | undefined
})
const rows = ref<api.Task[]>([])
const total = ref(0)
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const importing = ref(false)
const exporting = ref(false)
const formRef = ref<FormInstance>()
const fileInput = ref<HTMLInputElement>()
const empty = (): api.Task => ({ code: '', name: '', status: 0, remark: '' })
const form = ref<api.Task>(empty())
const rules: FormRules = {
  code: [
    { required: true, message: '请输入编码' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '编码格式不正确' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入任务名称' }],
  status: [{ required: true, message: '请选择状态' }]
}
const load = async () => {
  loading.value = true
  try {
    const data = await api.page(query)
    rows.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const search = () => {
  query.pageNo = 1
  return load()
}
const reset = () => {
  Object.assign(query, { name: '', code: '', status: undefined })
  return search()
}
const open = async (id?: number) => {
  form.value = id ? await api.get(id) : empty()
  visible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}
const save = async () => {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    if (form.value.id) await api.update(form.value)
    else await api.create(form.value)
    visible.value = false
    message.success('保存成功')
    await load()
  } finally {
    saving.value = false
  }
}
const remove = async (id: number) => {
  try {
    await message.delConfirm()
  } catch {
    return
  }
  await api.remove(id)
  message.success('删除成功')
  await search()
}
const downloadTemplate = async () => download.excel(await api.template(), '任务导入模板.xlsx')
const exportRows = async () => {
  exporting.value = true
  try {
    download.excel(await api.exportExcel(query), '示例任务.xlsx')
  } finally {
    exporting.value = false
  }
}
const importRows = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  importing.value = true
  try {
    const count = await api.importExcel(file)
    message.success('已导入 ' + count + ' 条')
    await search()
  } finally {
    importing.value = false
    input.value = ''
  }
}
onMounted(load)
</script>

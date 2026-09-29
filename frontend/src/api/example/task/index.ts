import request from '@/config/axios'

export interface Task {
  id?: number
  code: string
  name: string
  status: number
  remark?: string
  createTime?: string
}
const base = '/example/task'
export const page = (params: any) =>
  request.get<{ list: Task[]; total: number }>({ url: base + '/page', params })
export const get = (id: number) => request.get<Task>({ url: base + '/get', params: { id } })
export const create = (data: Task) => request.post({ url: base + '/create', data })
export const update = (data: Task) => request.put({ url: base + '/update', data })
export const remove = (id: number) => request.delete({ url: base + '/delete', params: { id } })
export const exportExcel = (params: any) =>
  request.download({ url: base + '/export-excel', params })
export const template = () => request.download({ url: base + '/import-template' })
export const importExcel = (file: File) => {
  const data = new FormData()
  data.append('file', file)
  return request.post<number>({
    url: base + '/import-excel',
    data,
    headersType: 'multipart/form-data'
  })
}

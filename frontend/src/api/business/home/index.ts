import request from '@/config/axios'

export interface BusinessHome {
  name: string
  description: string
}

export const getBusinessHome = () => request.get<BusinessHome>({ url: '/business/home/get' })

import request from '@/utils/request'

export interface Domain {
  id: number
  name: string
  createdAt?: string
  updatedAt?: string
}

export const getDomains = (params?: { q?: string }) => {
  return request.get<any, { data: Domain[] }>('/domains', { params })
}

export const createDomain = (data: { name: string }) => {
  return request.post<any, { data: Domain }>('/domains', data)
}

export const updateDomain = (id: number, data: { name: string }) => {
  return request.put<any, { data: Domain }>(`/domains/${id}`, data)
}

export const deleteDomain = (id: number) => {
  return request.delete(`/domains/${id}`)
}

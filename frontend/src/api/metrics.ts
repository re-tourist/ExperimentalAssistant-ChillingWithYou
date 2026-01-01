import request from '@/utils/request'
import type { ApiResponse } from './types'

export interface MetricDef {
  id?: number
  name: string
  direction: 'MAX' | 'MIN' | 'NONE'
  description?: string
  createdAt?: string
  updatedAt?: string
}

export function getMetricDefs() {
  return request.get<any, ApiResponse<MetricDef[]>>('/metrics/defs')
}

export function createMetricDef(data: MetricDef) {
  return request.post<any, ApiResponse<MetricDef>>('/metrics/defs', data)
}

export function updateMetricDef(id: number, data: MetricDef) {
  return request.put<any, ApiResponse<MetricDef>>(`/metrics/defs/${id}`, data)
}

export function deleteMetricDef(id: number) {
  return request.delete<any, ApiResponse<void>>(`/metrics/defs/${id}`)
}

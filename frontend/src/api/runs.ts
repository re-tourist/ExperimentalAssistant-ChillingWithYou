import request from '@/utils/request'
import type { ApiResponse, PageResult } from './types'

export interface Run {
  id: number
  projectId: number
  name: string
  status: string
  modelName?: string
  datasetName?: string
  startTime?: string
  endTime?: string
  updatedAt?: string
}

export interface RunDetail extends Run {
  // Legacy fixed fields support (optional)
  optimizer?: string
  lr?: number
  batchSize?: number
  epochs?: number
  seed?: number
  
  note?: string
  metrics: { metricDefId: number; name?: string; displayName?: string; direction?: string; value: number }[]
  tags: { id: number; name: string }[]
  
  // Dynamic fields
  fieldValues?: Record<string, any>
}

export interface RunCreateUpdate {
  projectId: number
  name: string
  status?: string
  
  // Dynamic fields map (key: fieldKey, value: string/number)
  fieldValues?: Record<string, any>
  
  note?: string
  metrics?: { metricDefId: number; value: number }[]
  tagIds?: number[]
  startTime?: string
  endTime?: string
}

export function getRuns(params: {
  page?: number
  size?: number
  projectId?: number
  status?: string
  q?: string
  dateFrom?: string
  dateTo?: string
  tagIds?: number[]
}) {
  return request.get<any, ApiResponse<PageResult<Run>>>('/runs', { params })
}

export function getRun(id: number) {
  return request.get<any, ApiResponse<RunDetail>>(`/runs/${id}`)
}

export function createRun(data: RunCreateUpdate) {
  return request.post<any, ApiResponse<RunDetail>>('/runs', data)
}

export function updateRun(id: number, data: RunCreateUpdate) {
  return request.put<any, ApiResponse<RunDetail>>(`/runs/${id}`, data)
}

export function deleteRun(id: number) {
  return request.delete<any, ApiResponse<void>>(`/runs/${id}`)
}

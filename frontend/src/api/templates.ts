import request from '@/utils/request'
import type { ApiResponse, PageResult } from './types'

export interface Template {
  id: number
  name: string
  domain: string
  description?: string
  configJson?: string
  createdAt: string
  updatedAt: string
}

export interface TemplateDetail extends Template {
  metricDefs: TemplateMetricDetail[]
  tags: TemplateTagDetail[]
}

export interface TemplateMetricDetail {
  metricDefId: number
  name: string
  displayName: string
  direction: string
  isDefault: boolean
  sortOrder: number
}

export interface TemplateTagDetail {
  tagId: number
  name: string
  isDefault: boolean
  sortOrder: number
}

export interface TemplateUpsertRequest {
  name: string
  domain: string
  description?: string
  configJson?: string
  metricDefs?: { metricDefId: number; isDefault: boolean; sortOrder: number }[]
  tags?: { tagId: number; isDefault: boolean; sortOrder: number }[]
}

export function getTemplates(params: {
  page?: number
  size?: number
  q?: string
  domain?: string
}) {
  return request.get<any, ApiResponse<PageResult<Template>>>('/templates', { params })
}

export function getTemplate(id: number) {
  return request.get<any, ApiResponse<TemplateDetail>>(`/templates/${id}`)
}

export function createTemplate(data: TemplateUpsertRequest) {
  return request.post<any, ApiResponse<TemplateDetail>>('/templates', data)
}

export function updateTemplate(id: number, data: TemplateUpsertRequest) {
  return request.put<any, ApiResponse<TemplateDetail>>(`/templates/${id}`, data)
}

export function deleteTemplate(id: number) {
  return request.delete<any, ApiResponse<void>>(`/templates/${id}`)
}

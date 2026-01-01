import request from '@/utils/request'
import type { ApiResponse, PageResult } from './types'

export interface Project {
  id?: number
  name: string
  description?: string
  createdAt?: string
  updatedAt?: string
  runCount?: number | string // For UI display
  templateId?: number
  projectConfigSnapshot?: string
}

export interface ProjectCreateUpdate {
  name: string
  description?: string
  templateId?: number
}

export function getProjects(params: { page?: number; size?: number; q?: string }) {
  return request.get<any, ApiResponse<PageResult<Project>>>('/projects', { params })
}

export function createProject(data: ProjectCreateUpdate) {
  return request.post<any, ApiResponse<Project>>('/projects', data)
}

export function updateProject(id: number, data: ProjectCreateUpdate) {
  return request.put<any, ApiResponse<Project>>(`/projects/${id}`, data)
}

export function deleteProject(id: number) {
  return request.delete<any, ApiResponse<void>>(`/projects/${id}`)
}

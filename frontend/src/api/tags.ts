import request from '@/utils/request'
import type { ApiResponse } from './types'

export interface Tag {
  id?: number
  name: string
  createdAt?: string
}

export function getTags(params?: { q?: string }) {
  return request.get<any, ApiResponse<Tag[]>>('/tags', { params })
}

export function createTag(data: Tag) {
  return request.post<any, ApiResponse<Tag>>('/tags', data)
}

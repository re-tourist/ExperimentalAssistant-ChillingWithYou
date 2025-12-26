import request from '@/utils/request'
import type { ApiResponse } from './types'

export interface RunNote {
  id: number
  runId: number
  type: 'NOTE' | 'CONCLUSION' | 'AI_DRAFT'
  title?: string
  contentMd: string
  createdAt: string
  updatedAt: string
}

export interface RunNoteCreate {
  type: 'NOTE' | 'AI_DRAFT'
  title?: string
  contentMd: string
}

export interface RunNoteUpdate {
  title?: string
  contentMd: string
}

export interface RunConclusionResponse {
  runId: number
  conclusion: RunNote | null
}

export const getNotes = (runId: number, type?: string) => {
  return request.get<any, ApiResponse<RunNote[]>>(`/runs/${runId}/notes`, {
    params: { type }
  })
}

export const createNote = (runId: number, data: RunNoteCreate) => {
  return request.post<any, ApiResponse<RunNote>>(`/runs/${runId}/notes`, data)
}

export const updateNote = (runId: number, noteId: number, data: RunNoteUpdate) => {
  return request.put<any, ApiResponse<RunNote>>(`/runs/${runId}/notes/${noteId}`, data)
}

export const deleteNote = (runId: number, noteId: number) => {
  return request.delete<any, ApiResponse<void>>(`/runs/${runId}/notes/${noteId}`)
}

export const getConclusion = (runId: number) => {
  return request.get<any, ApiResponse<RunConclusionResponse>>(`/runs/${runId}/conclusion`)
}

export const upsertConclusion = (runId: number, data: RunNoteUpdate) => {
  return request.put<any, ApiResponse<RunNote>>(`/runs/${runId}/conclusion`, data)
}

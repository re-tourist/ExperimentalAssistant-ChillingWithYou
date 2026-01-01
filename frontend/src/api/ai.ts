import request from '@/utils/request'
import type { ApiResponse } from './types'

export interface AiDraftRequest {
  runId: number
  userHint?: string
  includeMetrics?: boolean
  includeTags?: boolean
  includeTemplate?: boolean
  includeNotes?: boolean
  saveDraft?: boolean
}

export interface AiDraftResponse {
  draftMd: string
  extracted: {
    keyFindings: string[]
    nextSteps: string[]
  }
}

export const generateRunDraft = (data: AiDraftRequest) => {
  return request.post<any, ApiResponse<AiDraftResponse>>('/ai/run-draft', data)
}

export const parseMarkdownRefs = (textMd: string) => {
  return request.post<any, ApiResponse<number[]>>('/markdown/refs', { textMd })
}

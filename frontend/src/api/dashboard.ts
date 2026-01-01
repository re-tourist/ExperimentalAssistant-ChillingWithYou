import request from '@/utils/request'
import type { ApiResponse } from './types'

export interface DashboardFilter {
  projectId?: number
  dateFrom?: string
  dateTo?: string
  status?: string
  tagIds?: number[]
  metricDefId?: number
  limit?: number
  by?: string
  granularity?: string
}

export interface SummaryResponse {
  totalRuns: number
  runsLast7Days: number
  successRate: number
  bestMetric?: {
    metricDefId: number
    metricName: string
    value: number
    runId: number
    runName: string
  }
}

export interface TrendPoint {
  x: string
  y: number
}

export interface DistributionItem {
  name: string
  value: number
}

export interface TopRun {
  runId: number
  runName: string
  modelName: string
  datasetName: string
  value: number
  endTime: string
  status: string
}

export function getSummary(params: DashboardFilter) {
  return request.get<any, ApiResponse<SummaryResponse>>('/dashboard/summary', { params })
}

export function getTrend(params: DashboardFilter) {
  return request.get<any, ApiResponse<TrendPoint[]>>('/dashboard/trend', { params })
}

export function getDistribution(params: DashboardFilter) {
  return request.get<any, ApiResponse<DistributionItem[]>>('/dashboard/distribution', { params })
}

export function getTopRuns(params: DashboardFilter) {
  return request.get<any, ApiResponse<TopRun[]>>('/dashboard/top', { params })
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface SystemConfig {
  aiEnabled: boolean
  aiProvider: string
  aiModel?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

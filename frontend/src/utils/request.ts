import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/api/types'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse
    if (res.code !== 0) {
      ElMessage.error(res.message || 'Error')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res as any
  },
  (error) => {
    ElMessage.error(error.message || 'Request Error')
    return Promise.reject(error)
  }
)

export default service

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
      // Business Error
      ElMessage.error(res.message || 'Error')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res as any
  },
  (error) => {
    // System/Network Error
    console.error('Request Error:', error) // Log full error to console for devs
    
    let message = 'An unexpected error occurred.'
    let description = 'Please check your network or try again later.'
    
    if (error.response) {
        const status = error.response.status
        const data = error.response.data
        
        if (status === 400) {
            message = 'Invalid Request'
            description = data.message || 'Please check your inputs.'
        } else if (status === 401) {
            message = 'Unauthorized'
            description = 'Please log in again.'
        } else if (status === 403) {
            message = 'Forbidden'
            description = 'You do not have permission to perform this action.'
        } else if (status === 404) {
            message = 'Not Found'
            description = 'The requested resource does not exist.'
        } else if (status >= 500) {
            message = 'Server Error'
            description = 'Our servers are experiencing issues. Please try again later.'
        } else {
            message = `Error ${status}`
            description = data.message || error.message
        }
    } else if (error.request) {
        message = 'Network Error'
        description = 'No response received from server.'
    } else {
        description = error.message
    }

    // Use Notification for detailed system errors to be user-friendly but noticeable
    // Or simple Message if it's common. 
    // Requirement: "简短说明 + 可行动建议"
    
    ElMessage.error({
        message: `${message}: ${description}`,
        duration: 5000,
        showClose: true
    })
    
    return Promise.reject(error)
  }
)

export default service

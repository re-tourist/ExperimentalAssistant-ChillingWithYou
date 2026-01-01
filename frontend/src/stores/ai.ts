import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export interface Message {
  role: 'system' | 'user' | 'assistant'
  content: string
}

export interface Attachment {
  type: 'run' | 'note'
  id?: number
  projectId?: number
  name?: string // Display name
  content?: string // For note
  mode: 'compact' | 'full'
}

export interface AiChatResponse {
  reply: string
  usage?: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
}

export interface AiConfigResponse {
  enabled: boolean
  provider: string
  model?: string
  baseUrl?: string
}

export const useAiStore = defineStore('ai', () => {
  // Config
  const aiEnabled = ref(true)
  const aiProvider = ref<string | null>(null)
  const aiModel = ref<string | null>(null)
  
  // State
  const messages = ref<Message[]>([])
  const attachments = ref<Attachment[]>([])
  const isSending = ref(false)
  const error = ref<string | null>(null)

  const fetchConfig = async () => {
    try {
      const res = await request.get<any, { data: AiConfigResponse }>('/ai/config')
      aiEnabled.value = !!res.data.enabled
      aiProvider.value = res.data.provider || null
      aiModel.value = res.data.model || null
    } catch {
      aiEnabled.value = false
      aiProvider.value = null
      aiModel.value = null
    }
  }

  // Actions
  const addAttachment = (att: Attachment) => {
    // Avoid duplicates
    const exists = attachments.value.some(a => {
      if (a.type !== att.type) return false
      if (a.type === 'run') return a.id === att.id
      if (a.type === 'note') return a.projectId === att.projectId
      return false
    })
    if (!exists) {
      attachments.value.push(att)
    }
  }

  const removeAttachment = (index: number) => {
    attachments.value.splice(index, 1)
  }

  const clearAttachments = () => {
    attachments.value = []
  }

  const sendMessage = async (content: string) => {
    if (!content.trim() && attachments.value.length === 0) return

    isSending.value = true
    error.value = null

    // User Message
    const userMsg: Message = { role: 'user', content }
    messages.value.push(userMsg)

    try {
      const payload = {
        messages: messages.value.map(m => ({ role: m.role, content: m.content })), // Send history
        attachments: attachments.value,
        options: {
          temperature: 0.7,
          stream: false
        }
      }

      const res = await request.post<any, { data: AiChatResponse }>('/ai/chat', payload, {
        timeout: 120000 // 2 minutes timeout for AI requests
      })
      
      const replyMsg: Message = { role: 'assistant', content: res.data.reply }
      messages.value.push(replyMsg)
      
      // Clear attachments after send
      clearAttachments()
      
    } catch (err: any) {
      console.error(err)
      error.value = err.message || 'Failed to send message'
      // Add error system message
      messages.value.push({ role: 'system', content: `Error: ${error.value}` })
      
      // Check if AI Disabled
      if (err.message && err.message.includes('AI 未启用')) {
          aiEnabled.value = false
      }
    } finally {
      isSending.value = false
    }
  }
  
  const clearHistory = () => {
      messages.value = []
      error.value = null
  }

  return {
    aiEnabled,
    aiProvider,
    aiModel,
    messages,
    attachments,
    isSending,
    error,
    fetchConfig,
    addAttachment,
    removeAttachment,
    clearAttachments,
    sendMessage,
    clearHistory
  }
})

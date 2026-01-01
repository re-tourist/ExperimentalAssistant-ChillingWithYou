<template>
  <div class="ai-chat" @dragover.prevent @drop="onDrop">
    <!-- Messages Area -->
    <div class="messages-container" ref="messagesRef">
      <div v-if="aiStore.messages.length === 0" class="welcome-screen">
        <el-icon :size="60" color="#E4E7ED"><ChatDotSquare /></el-icon>
        <h2>AI Assistant</h2>
        <p>Drag items from the left sidebar to start analyzing.</p>
        <p v-if="!aiStore.aiEnabled" class="disabled-warning">AI is currently disabled.</p>
      </div>
      
      <div v-for="(msg, index) in aiStore.messages" :key="index" :class="['message-row', msg.role]">
        <div class="message-bubble">
          <div class="message-role">{{ msg.role === 'user' ? 'You' : 'AI' }}</div>
          <div class="message-content markdown-body" v-html="renderMarkdown(msg.content)"></div>
        </div>
      </div>
      
      <div v-if="aiStore.isSending" class="message-row assistant">
        <div class="message-bubble">
           <div class="typing-indicator">
             <span></span><span></span><span></span>
           </div>
        </div>
      </div>
      
      <div v-if="aiStore.error" class="error-banner">
        {{ aiStore.error }}
      </div>
    </div>

    <!-- Input Area -->
    <div class="input-area">
      <!-- Chips -->
      <div v-if="aiStore.attachments.length > 0" class="chips-container">
        <el-tag
          v-for="(att, index) in aiStore.attachments"
          :key="index"
          closable
          effect="plain"
          @close="aiStore.removeAttachment(index)"
        >
          {{ att.name }}
        </el-tag>
        <el-button link size="small" @click="aiStore.clearAttachments">Clear All</el-button>
      </div>

      <!-- Text Input -->
      <div class="input-wrapper">
        <el-input
          v-model="inputContent"
          type="textarea"
          :rows="2"
          placeholder="Ask something... (Drag items here to reference)"
          @keydown.enter.prevent="handleSend"
          :disabled="!aiStore.aiEnabled"
        />
        <el-button type="primary" @click="handleSend" :loading="aiStore.isSending" :disabled="!aiStore.aiEnabled">
          Send
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { ChatDotSquare } from '@element-plus/icons-vue'
import { useAiStore, type Attachment } from '@/stores/ai'
import MarkdownIt from 'markdown-it'

const props = defineProps<{
  draggedItem: Attachment | null
}>()

const emit = defineEmits(['drop-item'])

const aiStore = useAiStore()
const inputContent = ref('')
const messagesRef = ref<HTMLElement | null>(null)
const md = new MarkdownIt()

const renderMarkdown = (text: string) => {
  return md.render(text)
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

watch(() => aiStore.messages.length, scrollToBottom)

const handleSend = () => {
  if (!inputContent.value.trim() && aiStore.attachments.length === 0) return
  if (aiStore.isSending) return
  
  aiStore.sendMessage(inputContent.value)
  inputContent.value = ''
}

const onDrop = (e: DragEvent) => {
  e.preventDefault()
  if (props.draggedItem) {
    aiStore.addAttachment(props.draggedItem)
    emit('drop-item')
  } else if (e.dataTransfer) {
    // Fallback if prop not set
    try {
      const data = JSON.parse(e.dataTransfer.getData('application/json'))
      if (data && data.type) {
        aiStore.addAttachment(data)
      }
    } catch {
      // ignore
    }
  }
}
</script>

<style scoped>
.ai-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f9f9f9;
}

.welcome-screen {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.welcome-screen h2 {
  margin: 10px 0;
  color: #303133;
}

.disabled-warning {
  color: #f56c6c;
  font-weight: bold;
}

.message-row {
  display: flex;
  margin-bottom: 20px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant {
  justify-content: flex-start;
}

.message-row.system {
  justify-content: center;
}

.message-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.user .message-bubble {
  background-color: #409eff;
  color: #fff;
  border-bottom-right-radius: 0;
}

.assistant .message-bubble {
  background-color: #fff;
  color: #303133;
  border-bottom-left-radius: 0;
}

.system .message-bubble {
  background-color: #f4f4f5;
  color: #909399;
  font-size: 12px;
  padding: 8px 12px;
}

.message-role {
  font-size: 12px;
  margin-bottom: 4px;
  opacity: 0.8;
}

.error-banner {
  padding: 10px;
  background-color: #fef0f0;
  color: #f56c6c;
  text-align: center;
  border-radius: 4px;
  margin-top: 10px;
}

.input-area {
  padding: 20px;
  border-top: 1px solid #ebeef5;
  background-color: #fff;
}

.chips-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.input-wrapper {
  display: flex;
  gap: 10px;
}

.typing-indicator span {
  display: inline-block;
  width: 6px;
  height: 6px;
  background-color: #909399;
  border-radius: 50%;
  margin: 0 2px;
  animation: typing 1s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}
</style>

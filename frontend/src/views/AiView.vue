<template>
  <div class="ai-container">
    <!-- Left Sidebar -->
    <div class="sidebar" :style="{ width: sidebarWidth + 'px' }">
      <AiSidebar @drag-start="handleDragStart" />
    </div>

    <!-- Resizer -->
    <div class="resizer" @mousedown="startResize"></div>

    <!-- Main Chat Area -->
    <div class="main-chat">
      <AiChat :dragged-item="draggedItem" @drop-item="handleDrop" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AiSidebar from './ai/AiSidebar.vue'
import AiChat from './ai/AiChat.vue'
import type { Attachment } from '@/stores/ai'
import { useAiStore } from '@/stores/ai'

const sidebarWidth = ref(300)
const draggedItem = ref<Attachment | null>(null)
const aiStore = useAiStore()

onMounted(() => {
  aiStore.fetchConfig()
})

const handleDragStart = (item: Attachment) => {
  draggedItem.value = item
}

const handleDrop = () => {
  draggedItem.value = null
}

// Resizing logic
const startResize = (e: MouseEvent) => {
  const startX = e.clientX
  const startWidth = sidebarWidth.value

  const onMouseMove = (e: MouseEvent) => {
    const newWidth = startWidth + (e.clientX - startX)
    if (newWidth > 200 && newWidth < 600) {
      sidebarWidth.value = newWidth
    }
  }

  const onMouseUp = () => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}
</script>

<style scoped>
.ai-container {
  display: flex;
  height: calc(100vh - 80px); /* Adjust based on header/padding */
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.sidebar {
  border-right: 1px solid #ebeef5;
  background-color: #f9fafc;
  display: flex;
  flex-direction: column;
}

.resizer {
  width: 5px;
  cursor: col-resize;
  background-color: #f0f2f5;
  border-left: 1px solid #dcdfe6;
  border-right: 1px solid #dcdfe6;
}

.resizer:hover {
  background-color: #409eff;
}

.main-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  min-width: 0;
}
</style>

<template>
  <div class="markdown-editor" :class="{ 'fullscreen': fullscreen }">
    <div class="editor-toolbar">
      <div class="left">
        <slot name="toolbar-left"></slot>
      </div>
      <div class="right">
        <el-button link @click="togglePreview">
          {{ showPreview ? 'Hide Preview' : 'Show Preview' }}
        </el-button>
        <el-button link @click="fullscreen = !fullscreen">
          {{ fullscreen ? 'Exit Fullscreen' : 'Fullscreen' }}
        </el-button>
      </div>
    </div>
    
    <div class="editor-container">
      <div class="input-area" :style="{ width: showPreview ? '50%' : '100%' }">
        <el-input
          v-model="internalValue"
          type="textarea"
          class="editor-textarea"
          :placeholder="placeholder"
          resize="none"
          @input="handleInput"
        />
      </div>
      <div v-if="showPreview" class="preview-area" :style="{ width: '50%' }">
        <div class="markdown-body" v-html="renderedHtml" @click="handlePreviewClick"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import MarkdownIt from 'markdown-it'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: 'Type markdown content here...'
  }
})

const emit = defineEmits(['update:modelValue', 'run-click'])

const internalValue = ref(props.modelValue)
const showPreview = ref(true)
const fullscreen = ref(false)

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true
})

// Custom rendering for [[run:ID]]
const defaultText = md.renderer.rules.text || function(tokens, idx, options, _env, self) {
  return self.renderToken(tokens, idx, options)
}

md.renderer.rules.text = function(tokens, idx, options, env, self) {
  const content = tokens[idx].content
  // Replace [[run:123]] with <a href="#" data-run-id="123" class="run-link">Run #123</a>
  const regex = /\[\[run:(\d+)\]\]/g
  if (regex.test(content)) {
    return content.replace(regex, (_match, id) => {
      return `<span class="run-ref-link" data-run-id="${id}">Run #${id}</span>`
    })
  }
  return defaultText(tokens, idx, options, env, self)
}

const renderedHtml = computed(() => {
  return md.render(internalValue.value || '')
})

watch(() => props.modelValue, (val) => {
  if (val !== internalValue.value) {
    internalValue.value = val
  }
})

const handleInput = (val: string) => {
  emit('update:modelValue', val)
}

const togglePreview = () => {
  showPreview.value = !showPreview.value
}

const handlePreviewClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target && target.classList.contains('run-ref-link')) {
    const runId = target.getAttribute('data-run-id')
    if (runId) {
      emit('run-click', Number(runId))
    }
  }
}
</script>

<style scoped>
.markdown-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  height: 400px;
  background: #fff;
}

.markdown-editor.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 2000;
}

.editor-toolbar {
  padding: 5px 10px;
  border-bottom: 1px solid #dcdfe6;
  background: #f5f7fa;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.input-area, .preview-area {
  height: 100%;
  overflow: auto;
}

.input-area {
  border-right: 1px solid #dcdfe6;
}

.preview-area {
  padding: 20px;
  background: #fff;
}

.editor-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  height: 100%;
  border-radius: 0;
  resize: none;
  padding: 15px;
  font-family: monospace;
}

/* Minimal Markdown Styles */
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) {
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
  line-height: 1.25;
}
.markdown-body :deep(p) {
  margin-bottom: 1em;
}
.markdown-body :deep(ul), .markdown-body :deep(ol) {
  padding-left: 2em;
  margin-bottom: 1em;
}
.markdown-body :deep(code) {
  background-color: #f6f8fa;
  border-radius: 3px;
  padding: 0.2em 0.4em;
  font-family: monospace;
}
.markdown-body :deep(pre) {
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
}
.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
}
.markdown-body :deep(.run-ref-link) {
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
  font-weight: 500;
}
.markdown-body :deep(.run-ref-link:hover) {
  color: #66b1ff;
}
</style>

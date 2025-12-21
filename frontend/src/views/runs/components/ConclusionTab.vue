<template>
  <div class="conclusion-tab" v-loading="loading">
    <div v-if="!conclusion && !isEditing" class="empty-state">
      <el-empty description="No conclusion yet">
        <el-button type="primary" @click="startEdit">Write Conclusion</el-button>
      </el-empty>
    </div>

    <div v-else-if="!isEditing" class="conclusion-view">
      <div class="header">
        <h3>{{ conclusion?.title || 'Conclusion' }}</h3>
        <el-button type="primary" link @click="startEdit">Edit</el-button>
      </div>
      <div class="markdown-body" v-html="renderedHtml" @click="handlePreviewClick"></div>
      <div class="meta">
        Last updated: {{ formatDateTime(conclusion?.updatedAt || '') }}
      </div>
    </div>

    <div v-else class="conclusion-edit">
      <div class="edit-actions">
        <el-button @click="cancelEdit">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="saveConclusion">Save</el-button>
      </div>
      <div class="edit-form">
        <el-input v-model="form.title" placeholder="Title" class="mb-2" />
        <MarkdownEditor v-model="form.contentMd" @run-click="handleRunClick" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { getConclusion, upsertConclusion } from '@/api/runNotes'
import type { RunNote } from '@/api/runNotes'

const props = defineProps<{
  runId: number
}>()

const emit = defineEmits(['run-click'])

const loading = ref(false)
const conclusion = ref<RunNote | null>(null)
const isEditing = ref(false)
const submitting = ref(false)

const form = reactive({
  title: '',
  contentMd: ''
})

const md = new MarkdownIt({
  html: true,
  linkify: true
})

// Custom renderer
const defaultText = md.renderer.rules.text || function(tokens, idx, options, _env, self) {
  return self.renderToken(tokens, idx, options)
}
md.renderer.rules.text = function(tokens, idx, options, env, self) {
  const content = tokens[idx].content
  const regex = /\[\[run:(\d+)\]\]/g
  if (regex.test(content)) {
    return content.replace(regex, (_match, id) => {
      return `<span class="run-ref-link" data-run-id="${id}">Run #${id}</span>`
    })
  }
  return defaultText(tokens, idx, options, env, self)
}

const renderedHtml = computed(() => {
  return md.render(conclusion.value?.contentMd || '')
})

const fetchData = async () => {
  if (!props.runId) return
  loading.value = true
  try {
    const res = await getConclusion(props.runId)
    conclusion.value = res.data.conclusion
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

watch(() => props.runId, () => {
  isEditing.value = false
  fetchData()
})

onMounted(() => {
  fetchData()
})

const startEdit = () => {
  form.title = conclusion.value?.title || 'Conclusion'
  form.contentMd = conclusion.value?.contentMd || ''
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
}

const saveConclusion = async () => {
  if (!form.contentMd) {
    ElMessage.warning('Content is required')
    return
  }
  submitting.value = true
  try {
    const res = await upsertConclusion(props.runId, {
      title: form.title,
      contentMd: form.contentMd
    })
    conclusion.value = res.data
    isEditing.value = false
    ElMessage.success('Saved')
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const handleRunClick = (id: number) => {
  emit('run-click', id)
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

const formatDateTime = (val: string) => {
  return val ? new Date(val).toLocaleString() : ''
}
</script>

<style scoped>
.conclusion-tab {
  padding: 10px 0;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
  margin-bottom: 15px;
}
.header h3 {
  margin: 0;
}
.meta {
  margin-top: 20px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}
.edit-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}
.mb-2 {
  margin-bottom: 10px;
}
.markdown-body :deep(.run-ref-link) {
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}
</style>

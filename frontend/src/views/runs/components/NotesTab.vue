<template>
  <div class="notes-tab">
    <div class="header">
      <el-button type="primary" @click="openCreateDialog">Add Note</el-button>
      <el-radio-group v-model="filterType" size="small" @change="fetchData">
        <el-radio-button label="">All</el-radio-button>
        <el-radio-button label="NOTE">Notes</el-radio-button>
        <el-radio-button label="AI_DRAFT">AI Drafts</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="notes-list">
      <div v-if="notes.length === 0" class="empty-state">No notes found.</div>
      <el-card v-for="note in notes" :key="note.id" class="note-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="title-row">
              <el-tag :type="note.type === 'AI_DRAFT' ? 'warning' : 'info'" size="small" class="mr-2">
                {{ note.type }}
              </el-tag>
              <span class="note-title">{{ note.title || 'Untitled' }}</span>
            </div>
            <div class="actions">
              <el-button v-if="note.type === 'AI_DRAFT'" type="success" link size="small" @click="setAsConclusion(note)">
                Set as Conclusion
              </el-button>
              <el-button type="primary" link size="small" @click="openEditDialog(note)">Edit</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(note)">Delete</el-button>
            </div>
          </div>
          <div class="meta-row">
             {{ formatDateTime(note.createdAt) }}
          </div>
        </template>
        <div class="markdown-preview" v-html="renderMarkdown(note.contentMd)" @click="handlePreviewClick"></div>
      </el-card>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'Edit Note' : 'Create Note'"
      width="800px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="Title">
          <el-input v-model="form.title" placeholder="Optional title" />
        </el-form-item>
        <el-form-item label="Content">
          <MarkdownEditor v-model="form.contentMd" @run-click="handleRunClick" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">Cancel</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">Save</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownIt from 'markdown-it'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { getNotes, createNote, updateNote, deleteNote, upsertConclusion } from '@/api/runNotes'
import type { RunNote } from '@/api/runNotes'

const props = defineProps<{
  runId: number
}>()

const emit = defineEmits(['run-click'])

const loading = ref(false)
const notes = ref<RunNote[]>([])
const filterType = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const currentNoteId = ref<number | null>(null)

const form = reactive({
  title: '',
  contentMd: ''
})

const md = new MarkdownIt({
  html: true,
  linkify: true
})

// Custom renderer for list view
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

const renderMarkdown = (content: string) => {
  return md.render(content || '')
}

const fetchData = async () => {
  if (!props.runId) return
  loading.value = true
  try {
    // We filter NOTE and AI_DRAFT here, or backend does it.
    // If backend returns all types including CONCLUSION, we might want to filter CONCLUSION out if this tab is strictly for Notes/Drafts.
    // But let's assume getNotes returns what we asked for.
    // If filterType is empty, we get all.
    const res = await getNotes(props.runId, filterType.value || undefined)
    // Filter out CONCLUSION manually if backend returns it and we don't want it here (since it has its own tab)
    notes.value = res.data.filter((n: any) => n.type !== 'CONCLUSION')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

watch(() => props.runId, () => {
  fetchData()
})

onMounted(() => {
  fetchData()
})

const openCreateDialog = () => {
  isEdit.value = false
  currentNoteId.value = null
  form.title = ''
  form.contentMd = ''
  dialogVisible.value = true
}

const openEditDialog = (note: RunNote) => {
  isEdit.value = true
  currentNoteId.value = note.id
  form.title = note.title || ''
  form.contentMd = note.contentMd
  dialogVisible.value = true
}

const handleDelete = async (note: RunNote) => {
  try {
    await ElMessageBox.confirm('Delete this note?', 'Warning', { type: 'warning' })
    await deleteNote(props.runId, note.id)
    ElMessage.success('Deleted')
    fetchData()
  } catch {
    // cancel
  }
}

const handleSubmit = async () => {
  if (!form.contentMd) {
    ElMessage.warning('Content is required')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && currentNoteId.value) {
      await updateNote(props.runId, currentNoteId.value, {
        title: form.title,
        contentMd: form.contentMd
      })
      ElMessage.success('Updated')
    } else {
      await createNote(props.runId, {
        type: 'NOTE',
        title: form.title,
        contentMd: form.contentMd
      })
      ElMessage.success('Created')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const setAsConclusion = async (note: RunNote) => {
  try {
    await ElMessageBox.confirm('Use this draft content as the Run Conclusion? This will overwrite existing conclusion.', 'Confirm', { type: 'warning' })
    await upsertConclusion(props.runId, {
      title: note.title || 'Conclusion from Draft',
      contentMd: note.contentMd
    })
    ElMessage.success('Conclusion updated')
  } catch {
    // cancel
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
  return new Date(val).toLocaleString()
}
</script>

<style scoped>
.notes-tab {
  padding: 10px 0;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.note-card {
  margin-bottom: 15px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title-row {
  display: flex;
  align-items: center;
}
.note-title {
  font-weight: bold;
}
.meta-row {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
.mr-2 {
  margin-right: 8px;
}
.markdown-preview :deep(.run-ref-link) {
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}
</style>

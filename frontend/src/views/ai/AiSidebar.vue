<template>
  <div class="ai-sidebar">
    <div class="sidebar-header">
      <el-select
        v-model="selectedProjectId"
        class="project-select"
        placeholder="Project"
        clearable
        filterable
        :loading="loadingProjects"
        @change="handleProjectChange"
      >
        <el-option
          v-for="p in projects"
          :key="p.id"
          :label="p.name"
          :value="p.id"
        />
      </el-select>
      <el-input
        v-model="searchQuery"
        placeholder="Search runs..."
        :prefix-icon="Search"
        clearable
        @input="handleSearch"
      />
    </div>

    <div class="tree-container">
      <el-collapse v-model="activeNames">
        <!-- Runs Section -->
        <el-collapse-item title="Runs" name="runs">
          <div v-loading="loadingRuns" class="tree-list">
            <div
              v-for="run in filteredRuns"
              :key="run.id"
              class="tree-item"
              draggable="true"
              @dragstart="onDragStart($event, run, 'run')"
            >
              <el-icon class="item-icon"><DataLine /></el-icon>
              <div class="item-content">
                <span class="item-title">{{ run.name }}</span>
                <span class="item-sub">#{{ run.id }} | {{ run.status }}</span>
              </div>
            </div>
            <div v-if="filteredRuns.length === 0" class="empty-text">No runs found</div>
          </div>
        </el-collapse-item>

        <!-- Notes Section -->
        <el-collapse-item title="Notes" name="notes">
          <div class="tree-list">
            <div
              v-for="note in notes"
              :key="note.projectId"
              class="tree-item"
              draggable="true"
              @dragstart="onDragStart($event, note, 'note')"
              @click="editNote(note)"
            >
              <el-icon class="item-icon"><Document /></el-icon>
              <div class="item-content">
                <span class="item-title">Project {{ note.projectId }} Note</span>
                <span class="item-sub">{{ note.updatedAt }}</span>
              </div>
            </div>
            <div class="add-note">
               <el-button size="small" @click="createNote">+ New Note</el-button>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- Note Editor Dialog -->
    <el-dialog v-model="noteDialogVisible" title="Edit Note" width="600px">
      <el-form label-width="80px">
        <el-form-item label="Project ID">
           <el-input-number v-model="currentNote.projectId" :disabled="isEditingNote" />
        </el-form-item>
        <el-form-item label="Content">
           <el-input 
             v-model="currentNote.content" 
             type="textarea" 
             :rows="10" 
             placeholder="Markdown content..."
           />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noteDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="saveNote">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Search, DataLine, Document } from '@element-plus/icons-vue'
import { getRuns, type Run } from '@/api/runs'
import { getProjects, type Project } from '@/api/projects'
import type { Attachment } from '@/stores/ai'

const emit = defineEmits(['drag-start'])

// State
const searchQuery = ref('')
const activeNames = ref(['runs', 'notes'])
const runs = ref<Run[]>([])
const loadingRuns = ref(false)
const projects = ref<Project[]>([])
const loadingProjects = ref(false)
const selectedProjectId = ref<number | undefined>(undefined)

// Notes (Local Storage for MVP)
interface LocalNote {
  projectId: number
  content: string
  updatedAt: string
}
const notes = ref<LocalNote[]>([])
const noteDialogVisible = ref(false)
const currentNote = ref<LocalNote>({ projectId: 0, content: '', updatedAt: '' })
const isEditingNote = ref(false)

// Runs Logic
const fetchRuns = async () => {
  loadingRuns.value = true
  try {
    const res = await getRuns({
      page: 1,
      size: 50,
      q: searchQuery.value,
      projectId: selectedProjectId.value
    })
    runs.value = res.data.records
  } finally {
    loadingRuns.value = false
  }
}

const handleSearch = () => {
  fetchRuns()
}

const handleProjectChange = () => {
  fetchRuns()
}

const filteredRuns = computed(() => runs.value) // Already filtered by API 'q'

// Notes Logic
const loadNotes = () => {
  const stored = localStorage.getItem('ea_project_notes')
  if (stored) {
    notes.value = JSON.parse(stored)
  }
}

const createNote = () => {
  currentNote.value = { projectId: 1, content: '', updatedAt: new Date().toLocaleString() }
  isEditingNote.value = false
  noteDialogVisible.value = true
}

const editNote = (note: LocalNote) => {
  currentNote.value = { ...note }
  isEditingNote.value = true
  noteDialogVisible.value = true
}

const saveNote = () => {
  if (!currentNote.value.projectId) return
  
  currentNote.value.updatedAt = new Date().toLocaleString()
  
  const index = notes.value.findIndex(n => n.projectId === currentNote.value.projectId)
  if (index > -1) {
    notes.value[index] = { ...currentNote.value }
  } else {
    notes.value.push({ ...currentNote.value })
  }
  
  localStorage.setItem('ea_project_notes', JSON.stringify(notes.value))
  noteDialogVisible.value = false
}

// Drag Logic
const onDragStart = (e: DragEvent, item: any, type: 'run' | 'note') => {
  const attachment: Attachment = {
    type,
    mode: 'compact'
  }
  
  if (type === 'run') {
    attachment.id = item.id
    attachment.name = `Run #${item.id} ${item.name}`
  } else {
    attachment.projectId = item.projectId
    attachment.name = `Note (Project ${item.projectId})`
    attachment.content = item.content // Pass content for MVP backend
  }
  
  if (e.dataTransfer) {
    e.dataTransfer.setData('application/json', JSON.stringify(attachment))
    e.dataTransfer.effectAllowed = 'copy'
  }
  
  emit('drag-start', attachment)
}

onMounted(() => {
  ;(async () => {
    loadingProjects.value = true
    try {
      const res = await getProjects({ page: 1, size: 200 })
      projects.value = res.data.records || []
    } finally {
      loadingProjects.value = false
    }
  })()
  fetchRuns()
  loadNotes()
})
</script>

<style scoped>
.ai-sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.project-select {
  width: 100%;
}

.tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px;
}

.tree-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tree-item {
  display: flex;
  align-items: center;
  padding: 8px;
  cursor: grab;
  border-radius: 4px;
  transition: background-color 0.2s;
  border: 1px solid transparent;
}

.tree-item:hover {
  background-color: #ecf5ff;
  border-color: #d9ecff;
}

.item-icon {
  margin-right: 8px;
  color: #909399;
}

.item-content {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.item-title {
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-sub {
  font-size: 11px;
  color: #909399;
}

.empty-text {
  font-size: 12px;
  color: #909399;
  text-align: center;
  padding: 10px;
}

.add-note {
  margin-top: 10px;
  text-align: center;
}
</style>

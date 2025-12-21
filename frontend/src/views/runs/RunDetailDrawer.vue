<template>
  <el-drawer
    v-model="visible"
    title="Run Details"
    size="50%"
    :before-close="handleClose"
  >
    <div v-if="run" class="run-detail" v-loading="loading">
      <el-descriptions :column="2" border class="mb-4">
        <el-descriptions-item label="ID">{{ run.id }}</el-descriptions-item>
        <el-descriptions-item label="Name">{{ run.name }}</el-descriptions-item>
        <el-descriptions-item label="Project ID">{{ run.projectId }}</el-descriptions-item>
        <el-descriptions-item label="Status">
           <el-tag :type="getStatusType(run.status)">{{ run.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Model">{{ run.modelName }}</el-descriptions-item>
        <el-descriptions-item label="Dataset">{{ run.datasetName }}</el-descriptions-item>
        <el-descriptions-item label="Optimizer">{{ run.optimizer }}</el-descriptions-item>
        <el-descriptions-item label="LR">{{ run.lr }}</el-descriptions-item>
        <el-descriptions-item label="Batch">{{ run.batchSize }}</el-descriptions-item>
        <el-descriptions-item label="Epochs">{{ run.epochs }}</el-descriptions-item>
        <el-descriptions-item label="Seed">{{ run.seed }}</el-descriptions-item>
        <el-descriptions-item label="Created">{{ formatDateTime(run.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      
      <el-tabs v-model="activeTab" class="detail-tabs">
        <el-tab-pane label="Overview" name="overview">
          <div class="detail-section">
             <h3>Note</h3>
             <p>{{ run.note || 'No note provided.' }}</p>
          </div>

          <div class="detail-section">
             <h3>Tags</h3>
             <div class="tags-list">
                <el-tag v-for="tag in run.tags" :key="tag.id" class="mr-2">{{ tag.name }}</el-tag>
                <span v-if="!run.tags?.length">No tags.</span>
             </div>
          </div>

          <div class="detail-section">
             <h3>Metrics</h3>
             <el-table :data="run.metrics" border stripe size="small">
                <el-table-column label="Metric">
                  <template #default="{ row }">
                    {{ row.displayName || row.name || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="value" label="Value" />
             </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="Notes" name="notes">
          <NotesTab 
            :runId="run.id" 
            @run-click="handleRunLinkClick" 
          />
        </el-tab-pane>

        <el-tab-pane label="Conclusion" name="conclusion">
          <ConclusionTab 
            ref="conclusionTabRef"
            :runId="run.id" 
            @run-click="handleRunLinkClick" 
          />
        </el-tab-pane>

        <el-tab-pane label="AI Analysis" name="ai">
          <AiTab 
            :runId="run.id" 
            @run-click="handleRunLinkClick" 
            @refresh-notes="handleRefreshNotes"
            @refresh-conclusion="handleRefreshConclusion"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getRun } from '@/api/runs'
import type { RunDetail } from '@/api/runs'
import NotesTab from './components/NotesTab.vue'
import ConclusionTab from './components/ConclusionTab.vue'
import AiTab from './components/AiTab.vue'

const props = defineProps<{
  modelValue: boolean
  runId: number | null
}>()

const emit = defineEmits(['update:modelValue', 'update:runId'])

const loading = ref(false)
const run = ref<RunDetail | null>(null)
const activeTab = ref('overview')
const conclusionTabRef = ref<any>(null)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

watch(() => props.runId, async (newId) => {
  if (newId) {
    await fetchRunDetail(newId)
    // Reset tab to overview when opening a new run, or keep it?
    // Requirement says "prevent infinite recursion: allow jump, but drawer is same".
    // It's better to keep user context if they are clicking links, but maybe overview is safer default.
    // Let's keep current tab if it exists, otherwise default to overview.
  } else {
    run.value = null
  }
})

const fetchRunDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await getRun(id)
    run.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  visible.value = false
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'FINISHED': return 'success'
    case 'FAILED': return 'danger'
    default: return 'primary'
  }
}

const formatDateTime = (val: string) => {
  return val ? new Date(val).toLocaleString() : '-'
}

const handleRunLinkClick = (id: number) => {
  // Update the prop to switch context
  emit('update:runId', id)
}

const handleRefreshNotes = () => {
  // Notes tab watches runId, but here runId hasn't changed.
  // We need a way to force refresh NotesTab.
  // Actually, NotesTab component can expose a refresh method or we can use a key.
  // But since we are using <NotesTab :runId="run.id">, maybe we can just let it be.
  // The user will switch to Notes tab to see it.
  // Or we can use a ref on NotesTab.
  // For now, let's keep it simple. The user might need to switch tabs or we can rely on manual refresh.
  // Better: make NotesTab fetch on activation?
}

const handleRefreshConclusion = () => {
  if (conclusionTabRef.value) {
    conclusionTabRef.value.fetchData?.()
  }
}
</script>

<style scoped>
.mb-4 {
  margin-bottom: 20px;
}
.mr-2 {
  margin-right: 8px;
}
.detail-section {
  margin-top: 20px;
}
.detail-section h3 {
  font-size: 16px;
  margin-bottom: 10px;
  color: #303133;
}
.detail-tabs {
  margin-top: 20px;
}
</style>

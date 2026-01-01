<template>
  <el-drawer
    v-model="visible"
    title="Run Details"
    size="50%"
    :before-close="handleClose"
  >
    <div class="run-detail" v-loading="loading">
      <el-empty v-if="!run && !loading" description="No run selected." />
      <div v-else-if="run">
      <el-descriptions :column="2" border class="mb-4">
        <el-descriptions-item label="ID">{{ run.id }}</el-descriptions-item>
        <el-descriptions-item label="Name">{{ run.name }}</el-descriptions-item>
        <el-descriptions-item label="Project ID">{{ run.projectId }}</el-descriptions-item>
        <el-descriptions-item label="Start Time">{{ formatDateTime(run.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="End Time">{{ formatDateTime(run.endTime || run.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="Status">
           <el-tag :type="getStatusType(run.status)">{{ run.status }}</el-tag>
        </el-descriptions-item>
        
        <!-- Legacy fields support -->
        <el-descriptions-item v-if="run.modelName" label="Model">{{ run.modelName }}</el-descriptions-item>
        <el-descriptions-item v-if="run.datasetName" label="Dataset">{{ run.datasetName }}</el-descriptions-item>
        <el-descriptions-item v-if="run.optimizer" label="Optimizer">{{ run.optimizer }}</el-descriptions-item>
        <el-descriptions-item v-if="run.lr !== undefined" label="LR">{{ run.lr }}</el-descriptions-item>
        <el-descriptions-item v-if="run.batchSize !== undefined" label="Batch">{{ run.batchSize }}</el-descriptions-item>
        <el-descriptions-item v-if="run.epochs !== undefined" label="Epochs">{{ run.epochs }}</el-descriptions-item>
        <el-descriptions-item v-if="run.seed !== undefined" label="Seed">{{ run.seed }}</el-descriptions-item>

        <!-- Dynamic fields support -->
        <template v-if="run.fieldValues">
           <el-descriptions-item 
              v-for="(val, key) in run.fieldValues" 
              :key="key" 
              :label="key"
           >
              {{ val }}
           </el-descriptions-item>
        </template>
        
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
      </el-tabs>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getRun } from '@/api/runs'
import type { RunDetail } from '@/api/runs'
import NotesTab from './components/NotesTab.vue'
import ConclusionTab from './components/ConclusionTab.vue'

const props = defineProps<{
  modelValue: boolean
  runId: number | null
}>()

const emit = defineEmits(['update:modelValue', 'update:runId'])

const loading = ref(false)
const run = ref<RunDetail | null>(null)
const activeTab = ref('overview')

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

watch(
  () => [props.modelValue, props.runId] as const,
  async ([isOpen, runId]) => {
    if (!isOpen) return
    if (runId) {
      await fetchRunDetail(runId)
    } else {
      run.value = null
    }
  },
  { immediate: true }
)

const fetchRunDetail = async (id: number) => {
  loading.value = true
  try {
    run.value = null
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

const formatDateTime = (val?: string) => {
  return val ? new Date(val).toLocaleString() : '-'
}

const handleRunLinkClick = (id: number) => {
  // Update the prop to switch context
  emit('update:runId', id)
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

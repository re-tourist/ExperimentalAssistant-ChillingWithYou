<template>
  <div class="runs-view">
    <!-- Filter Bar -->
    <div class="filter-bar">
      <el-form :inline="true" :model="filters" class="demo-form-inline">
        <el-form-item label="Project">
          <el-select v-model="filters.projectId" placeholder="Select Project" clearable>
            <el-option
              v-for="project in projects"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="filters.status" placeholder="Select Status" clearable>
            <el-option label="RUNNING" value="RUNNING" />
            <el-option label="FINISHED" value="FINISHED" />
            <el-option label="FAILED" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="Search">
          <el-input v-model="filters.q" placeholder="Search by Name" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">Filter</el-button>
          <el-button @click="resetFilter">Reset</el-button>
          <el-button type="success" @click="openCreateDialog">Create Run</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Runs List Table -->
    <el-table
      v-loading="loading"
      :data="runs"
      style="width: 100%"
      @row-click="handleRowClick"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="Name" min-width="150" />
      <el-table-column prop="modelName" label="Model" width="120" />
      <el-table-column prop="datasetName" label="Dataset" width="120" />
      <el-table-column prop="status" label="Status" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="Created At" width="180">
        <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click.stop="openEditDialog(row)">Edit</el-button>
          <el-button type="danger" link @click.stop="handleDelete(row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'Edit Run' : 'Create Run'"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Project" prop="projectId">
              <el-select v-model="form.projectId" placeholder="Select Project" style="width: 100%">
                <el-option
                  v-for="project in projects"
                  :key="project.id"
                  :label="project.name"
                  :value="project.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Run Name" prop="name">
              <el-input v-model="form.name" placeholder="Run Name" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
           <el-col :span="12">
             <el-form-item label="Status" prop="status">
               <el-select v-model="form.status" placeholder="Select Status" style="width: 100%">
                 <el-option label="RUNNING" value="RUNNING" />
                 <el-option label="FINISHED" value="FINISHED" />
                 <el-option label="FAILED" value="FAILED" />
               </el-select>
             </el-form-item>
           </el-col>
           <el-col :span="12">
             <el-form-item label="Tags" prop="tagIds">
                <el-select v-model="form.tagIds" multiple placeholder="Select Tags" style="width: 100%">
                  <el-option
                    v-for="tag in tags"
                    :key="tag.id"
                    :label="tag.name"
                    :value="tag.id"
                  />
                </el-select>
             </el-form-item>
           </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Model" prop="modelName">
              <el-input v-model="form.modelName" placeholder="Model Name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Dataset" prop="datasetName">
              <el-input v-model="form.datasetName" placeholder="Dataset Name" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
             <el-form-item label="Optimizer" prop="optimizer">
               <el-input v-model="form.optimizer" />
             </el-form-item>
          </el-col>
          <el-col :span="8">
             <el-form-item label="Learning Rate" prop="lr">
               <el-input-number v-model="form.lr" :precision="6" :step="0.0001" style="width: 100%" />
             </el-form-item>
          </el-col>
           <el-col :span="8">
             <el-form-item label="Batch Size" prop="batchSize">
               <el-input-number v-model="form.batchSize" :min="1" style="width: 100%" />
             </el-form-item>
           </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="8">
             <el-form-item label="Epochs" prop="epochs">
               <el-input-number v-model="form.epochs" :min="1" style="width: 100%" />
             </el-form-item>
          </el-col>
          <el-col :span="8">
             <el-form-item label="Seed" prop="seed">
               <el-input-number v-model="form.seed" style="width: 100%" />
             </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Note" prop="note">
          <el-input v-model="form.note" type="textarea" :rows="2" />
        </el-form-item>

        <!-- Dynamic Metrics Form -->
        <el-divider content-position="left">Metrics</el-divider>
        <el-button type="primary" size="small" @click="addMetricRow" style="margin-bottom: 10px">
           Add Metric
        </el-button>
        <el-table :data="form.metrics" border style="width: 100%">
           <el-table-column label="Metric Definition" width="250">
             <template #default="{ row, $index }">
                <el-select 
                  v-model="row.metricDefId" 
                  placeholder="Select Metric" 
                  style="width: 100%"
                  @change="validateMetrics"
                >
                  <el-option
                    v-for="def in metricDefs"
                    :key="def.id"
                    :label="`${def.name} (${def.direction})`"
                    :value="def.id"
                    :disabled="isMetricDisabled(def.id, $index)"
                  />
                </el-select>
             </template>
           </el-table-column>
           <el-table-column label="Value">
              <template #default="{ row }">
                 <el-input-number v-model="row.value" :precision="4" :step="0.01" style="width: 100%" />
              </template>
           </el-table-column>
           <el-table-column width="80" align="center">
              <template #default="{ $index }">
                 <el-button type="danger" circle size="small" @click="removeMetricRow($index)">
                    <el-icon><Delete /></el-icon>
                 </el-button>
              </template>
           </el-table-column>
        </el-table>
        <div v-if="metricError" class="error-text">{{ metricError }}</div>

      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">Cancel</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            Confirm
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer
      v-model="drawerVisible"
      title="Run Details"
      size="50%"
    >
      <div v-if="currentRun" class="run-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentRun.id }}</el-descriptions-item>
          <el-descriptions-item label="Name">{{ currentRun.name }}</el-descriptions-item>
          <el-descriptions-item label="Project ID">{{ currentRun.projectId }}</el-descriptions-item>
          <el-descriptions-item label="Status">
             <el-tag :type="getStatusType(currentRun.status)">{{ currentRun.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Model">{{ currentRun.modelName }}</el-descriptions-item>
          <el-descriptions-item label="Dataset">{{ currentRun.datasetName }}</el-descriptions-item>
          <el-descriptions-item label="Optimizer">{{ currentRun.optimizer }}</el-descriptions-item>
          <el-descriptions-item label="Learning Rate">{{ currentRun.lr }}</el-descriptions-item>
          <el-descriptions-item label="Batch Size">{{ currentRun.batchSize }}</el-descriptions-item>
          <el-descriptions-item label="Epochs">{{ currentRun.epochs }}</el-descriptions-item>
          <el-descriptions-item label="Seed">{{ currentRun.seed }}</el-descriptions-item>
          <el-descriptions-item label="Created At">{{ formatDateTime(currentRun.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="detail-section">
           <h3>Note</h3>
           <p>{{ currentRun.note || 'No note provided.' }}</p>
        </div>

        <div class="detail-section">
           <h3>Tags</h3>
           <div class="tags-list">
              <el-tag v-for="tag in currentRun.tags" :key="tag.id" class="mr-2">{{ tag.name }}</el-tag>
              <span v-if="!currentRun.tags?.length">No tags.</span>
           </div>
        </div>

        <div class="detail-section">
           <h3>Metrics</h3>
           <el-table :data="currentRun.metrics" border stripe>
              <el-table-column prop="metricName" label="Metric Name" />
              <el-table-column prop="value" label="Value" />
           </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getRuns, getRun, createRun, updateRun, deleteRun } from '@/api/runs'
import { getProjects } from '@/api/projects'
import { getTags } from '@/api/tags'
import { getMetricDefs } from '@/api/metrics'
import type { Run, RunDetail, RunCreateUpdate } from '@/api/runs'
import type { Project } from '@/api/projects'
import type { Tag } from '@/api/tags'
import type { MetricDef } from '@/api/metrics'

// --- State ---
const loading = ref(false)
const runs = ref<Run[]>([])
const projects = ref<Project[]>([])
const tags = ref<Tag[]>([])
const metricDefs = ref<MetricDef[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const filters = reactive({
  projectId: undefined as number | undefined,
  status: undefined as string | undefined,
  q: undefined as string | undefined
})

const dialogVisible = ref(false)
const drawerVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const currentRunId = ref<number | null>(null)
const currentRun = ref<RunDetail | null>(null)

// Form
const formRef = ref()
const form = reactive<RunCreateUpdate & { metrics: any[] }>({
  projectId: undefined as any,
  name: '',
  status: 'RUNNING', // Add status to form model to fix type error if API expects it, though interface might need update
  modelName: '',
  datasetName: '',
  optimizer: '',
  lr: 0.001,
  batchSize: 32,
  epochs: 10,
  seed: 42,
  note: '',
  tagIds: [],
  metrics: []
})

const rules = {
  projectId: [{ required: true, message: 'Please select a project', trigger: 'change' }],
  name: [{ required: true, message: 'Please enter run name', trigger: 'blur' }],
  status: [{ required: true, message: 'Please select status', trigger: 'change' }]
}

const metricError = ref('')

// --- Lifecycle ---
onMounted(async () => {
  await fetchProjects()
  await fetchTags()
  await fetchMetricDefs()
  await fetchRuns()
})

// --- API Calls ---
const fetchProjects = async () => {
  try {
    const res = await getProjects({ page: 1, size: 100 })
    projects.value = res.data.records
  } catch (error) {
    console.error(error)
  }
}

const fetchTags = async () => {
  try {
    const res = await getTags()
    tags.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchMetricDefs = async () => {
  try {
    const res = await getMetricDefs()
    metricDefs.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchRuns = async () => {
  loading.value = true
  try {
    const res = await getRuns({
      page: pagination.page,
      size: pagination.size,
      projectId: filters.projectId,
      status: filters.status,
      q: filters.q
    })
    runs.value = res.data.records
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

// --- Actions ---
const handleFilter = () => {
  pagination.page = 1
  fetchRuns()
}

const resetFilter = () => {
  filters.projectId = undefined
  filters.status = undefined
  filters.q = undefined
  handleFilter()
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  fetchRuns()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchRuns()
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

// --- Detail Drawer ---
const handleRowClick = async (row: Run) => {
  try {
    const res = await getRun(row.id)
    currentRun.value = res.data
    drawerVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

// --- Create/Edit Dialog ---
const openCreateDialog = () => {
  isEdit.value = false
  currentRunId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = async (row: Run) => {
  isEdit.value = true
  currentRunId.value = row.id
  try {
    const res = await getRun(row.id)
    const data = res.data
    Object.assign(form, {
      projectId: data.projectId,
      name: data.name,
      status: data.status,
      modelName: data.modelName,
      datasetName: data.datasetName,
      optimizer: data.optimizer,
      lr: data.lr,
      batchSize: data.batchSize,
      epochs: data.epochs,
      seed: data.seed,
      note: data.note,
      tagIds: data.tags.map(t => t.id),
      metrics: data.metrics.map(m => ({
        metricDefId: m.metricDefId,
        value: m.value
      }))
    })
    dialogVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = (row: Run) => {
  ElMessageBox.confirm('Are you sure you want to delete this run?', 'Warning', {
    confirmButtonText: 'Yes',
    cancelButtonText: 'No',
    type: 'warning'
  }).then(async () => {
    await deleteRun(row.id)
    ElMessage.success('Deleted successfully')
    fetchRuns()
  })
}

const resetForm = () => {
  Object.assign(form, {
    projectId: undefined,
    name: '',
    status: 'RUNNING',
    modelName: '',
    datasetName: '',
    optimizer: '',
    lr: 0.001,
    batchSize: 32,
    epochs: 10,
    seed: 42,
    note: '',
    tagIds: [],
    metrics: []
  })
  if (formRef.value) formRef.value.resetFields()
  metricError.value = ''
}

// --- Dynamic Metrics ---
const addMetricRow = () => {
  form.metrics.push({ metricDefId: undefined, value: 0 })
}

const removeMetricRow = (index: number) => {
  form.metrics.splice(index, 1)
  validateMetrics()
}

const isMetricDisabled = (id: number | undefined, currentIndex: number) => {
  if (!id) return false
  return form.metrics.some((m, idx) => idx !== currentIndex && m.metricDefId === id)
}

const validateMetrics = () => {
  const ids = form.metrics.map(m => m.metricDefId).filter(id => id !== undefined)
  const uniqueIds = new Set(ids)
  if (ids.length !== uniqueIds.size) {
    metricError.value = 'Duplicate metrics selected'
    return false
  }
  metricError.value = ''
  return true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      if (!validateMetrics()) return
      
      submitting.value = true
      try {
        const payload = { ...form }
        // Ensure status is passed if needed, currently RunCreateUpdate interface might miss it if backend requires it
        // Based on previous step RunCreateUpdate interface:
        // export interface RunCreateUpdate { ... status?: string ... } - Wait, let's check interface
        
        if (isEdit.value && currentRunId.value) {
          await updateRun(currentRunId.value, payload)
          ElMessage.success('Updated successfully')
        } else {
          await createRun(payload)
          ElMessage.success('Created successfully')
        }
        dialogVisible.value = false
        fetchRuns()
      } catch (error) {
        console.error(error)
      } finally {
        submitting.value = false
      }
    }
  })
}
</script>

<style scoped>
.runs-view {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}
.filter-bar {
  margin-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 20px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.error-text {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 5px;
}
.detail-section {
  margin-top: 20px;
}
.detail-section h3 {
  font-size: 16px;
  margin-bottom: 10px;
  color: #303133;
}
.mr-2 {
  margin-right: 8px;
}
</style>

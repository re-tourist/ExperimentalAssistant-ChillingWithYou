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

    <ActiveFilters
      v-model:filters="filters"
      :config="filterConfig"
      @change="handleFiltersChange"
    />

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
                <div style="width: 100%">
                  <div style="display: flex; flex-wrap: wrap; gap: 8px; min-height: 32px">
                    <el-tag
                      v-for="tag in selectedTags"
                      :key="tag.id"
                      closable
                      @close="removeSelectedTag(tag.id)"
                    >
                      {{ tag.name }}
                    </el-tag>
                  </div>
                  <el-input
                    ref="tagInputRef"
                    v-model="tagInput"
                    placeholder="输入标签，回车添加"
                    style="margin-top: 8px"
                    :disabled="creatingTag || optionsLoading.tags"
                    @keyup.enter="handleTagEnter"
                  />
                </div>
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
        <div style="display: flex; gap: 8px; margin-bottom: 10px">
          <el-button type="primary" size="small" @click="addMetricRow">Add Metric</el-button>
          <el-button type="success" size="small" @click="openNewMetricDefDialog()">+ New Metric</el-button>
        </div>
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
                    :label="`${getMetricDefDisplayName(def)} (${def.direction})`"
                    :value="def.id"
                    :disabled="isMetricDisabled(def.id, $index)"
                  />
                  <template #empty>
                    <div style="padding: 8px 12px; display: flex; align-items: center; justify-content: space-between">
                      <span style="color: var(--el-text-color-secondary)">暂无指标定义，请先创建</span>
                      <el-button type="primary" link @click.stop="openNewMetricDefDialog($index)">+ New Metric</el-button>
                    </div>
                  </template>
                </el-select>
             </template>
           </el-table-column>
           <el-table-column label="Value">
              <template #default="{ row, $index }">
                 <el-input-number
                   :ref="(el: any) => setMetricValueRef(el, $index)"
                   v-model="row.value"
                   :precision="4"
                   :step="0.01"
                   style="width: 100%"
                 />
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

    <el-dialog v-model="metricDefDialogVisible" title="New Metric" width="520px" :close-on-click-modal="false">
      <el-form ref="metricDefFormRef" :model="metricDefForm" :rules="metricDefRules" label-width="120px">
        <el-form-item label="name" prop="name">
          <el-input v-model="metricDefForm.name" placeholder="e.g. acc" />
        </el-form-item>
        <el-form-item label="displayName" prop="displayName">
          <el-input v-model="metricDefForm.displayName" placeholder="e.g. Accuracy" />
        </el-form-item>
        <el-form-item label="direction" prop="direction">
          <el-select v-model="metricDefForm.direction" style="width: 100%">
            <el-option label="MAX" value="MAX" />
            <el-option label="MIN" value="MIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="metricDefDialogVisible = false">Cancel</el-button>
          <el-button type="primary" :loading="metricDefSubmitting" @click="submitNewMetricDef">Create</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <RunDetailDrawer
      v-model="drawerVisible"
      v-model:runId="currentRunId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getRuns, getRun, createRun, updateRun, deleteRun } from '@/api/runs'
import { getProjects } from '@/api/projects'
import { getTags, createTag } from '@/api/tags'
import { getMetricDefs, createMetricDef } from '@/api/metrics'
import { getTemplates, getTemplate } from '@/api/templates'
import RunDetailDrawer from '@/views/runs/RunDetailDrawer.vue'
import ActiveFilters from '@/components/ActiveFilters.vue'
import type { Run, RunCreateUpdate } from '@/api/runs'
import type { Project } from '@/api/projects'
import type { Tag } from '@/api/tags'
import type { MetricDef } from '@/api/metrics'
import type { Template } from '@/api/templates'

const route = useRoute()

// --- State ---
const loading = ref(false)
const runs = ref<Run[]>([])
const projects = ref<Project[]>([])
const tags = ref<Tag[]>([])
const metricDefs = ref<MetricDef[]>([])
const templates = ref<Template[]>([])
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

const filterConfig = {
  projectId: { 
    label: 'Project',
    formatter: (val: number) => projects.value.find(p => p.id === val)?.name || String(val)
  },
  status: { label: 'Status' },
  q: { label: 'Search' }
}

const handleFiltersChange = () => {
  handleFilter()
}

const dialogVisible = ref(false)
const drawerVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const currentRunId = ref<number | null>(null)

const optionsLoading = reactive({
  tags: false,
  metricDefs: false
})

// Form
const formRef = ref()
const form = reactive<RunCreateUpdate & { metrics: any[] }>({
  projectId: undefined as any,
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
  // templateId: undefined, // Removed
  tagIds: [],
  metrics: []
})

const rules = {
  projectId: [{ required: true, message: 'Please select a project', trigger: 'change' }],
  name: [{ required: true, message: 'Please enter run name', trigger: 'blur' }],
  status: [{ required: true, message: 'Please select status', trigger: 'change' }]
}

const metricError = ref('')
const metricValueRefs = ref<any[]>([])

const tagInputRef = ref<any>(null)
const tagInput = ref('')
const creatingTag = ref(false)

const selectedTags = computed(() => {
  const idSet = new Set(form.tagIds || [])
  return tags.value.filter(t => t.id != null && idSet.has(t.id))
})

const removeSelectedTag = (tagId: number | undefined) => {
  if (tagId == null) return
  form.tagIds = (form.tagIds || []).filter(id => id !== tagId)
}

const focusTagInput = async () => {
  await nextTick()
  tagInputRef.value?.focus?.()
}

const addTagId = (tagId: number | undefined) => {
  if (tagId == null) return
  const current = form.tagIds || []
  if (!current.includes(tagId)) {
    form.tagIds = [...current, tagId]
  }
}

const findTagByName = (name: string) => {
  const normalized = name.trim().toLowerCase()
  return tags.value.find(t => (t.name || '').trim().toLowerCase() === normalized)
}

const handleTagEnter = async () => {
  const raw = tagInput.value
  const name = raw.trim()
  if (!name) return
  if (name.length > 32) {
    ElMessage.error('标签长度需 <= 32')
    return
  }

  const existing = findTagByName(name)
  if (existing?.id != null) {
    addTagId(existing.id)
    tagInput.value = ''
    await focusTagInput()
    return
  }

  creatingTag.value = true
  try {
    const res = await createTag({ name })
    const created = res.data
    const alreadyInList = tags.value.some(t => t.id === created.id)
    if (!alreadyInList) {
      tags.value = [...tags.value, created]
    }
    addTagId(created.id)
    ElMessage.success(alreadyInList ? '标签已存在，已为你选择该标签' : '已创建并添加标签')
  } catch {
    await fetchTags()
    const after = findTagByName(name)
    if (after?.id != null) {
      addTagId(after.id)
      ElMessage.success('标签已存在，已为你选择该标签')
    }
  } finally {
    creatingTag.value = false
    tagInput.value = ''
    await focusTagInput()
  }
}

const metricDefDialogVisible = ref(false)
const metricDefSubmitting = ref(false)
const metricDefTargetIndex = ref<number | null>(null)
const metricDefFormRef = ref<any>(null)
const metricDefForm = reactive({
  name: '',
  displayName: '',
  direction: 'MAX' as 'MAX' | 'MIN'
})

const metricDefRules = {
  name: [{ required: true, message: 'name is required', trigger: 'blur' }],
  displayName: [{ required: true, message: 'displayName is required', trigger: 'blur' }],
  direction: [{ required: true, message: 'direction is required', trigger: 'change' }]
}

const getMetricDefDisplayName = (def: MetricDef) => {
  return def.description || def.name
}

const openNewMetricDefDialog = (targetIndex?: number) => {
  if (typeof targetIndex === 'number') {
    metricDefTargetIndex.value = targetIndex
  } else {
    if (form.metrics.length === 0) {
      addMetricRow()
    }
    metricDefTargetIndex.value = form.metrics.length - 1
  }
  Object.assign(metricDefForm, { name: '', displayName: '', direction: 'MAX' })
  metricDefDialogVisible.value = true
}

const setMetricValueRef = (el: any, index: number) => {
  if (!el) return
  metricValueRefs.value[index] = el
}

const focusMetricValue = async (index: number) => {
  await nextTick()
  metricValueRefs.value[index]?.focus?.()
}

const submitNewMetricDef = async () => {
  if (!metricDefFormRef.value) return
  await metricDefFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    const name = metricDefForm.name.trim()
    const displayName = metricDefForm.displayName.trim()
    if (name.length > 32) {
      ElMessage.error('name 长度需 <= 32')
      return
    }
    if (!/^[a-z0-9_]+$/.test(name)) {
      ElMessage.warning('建议使用小写字母/数字/下划线')
    }
    if (displayName.length > 64) {
      ElMessage.error('displayName 长度需 <= 64')
      return
    }

    metricDefSubmitting.value = true
    try {
      const res = await createMetricDef({
        name,
        direction: metricDefForm.direction,
        description: displayName
      } as any)
      const created = res.data
      const alreadyInList = metricDefs.value.some(m => m.id === created.id)
      if (!alreadyInList) {
        metricDefs.value = [...metricDefs.value, created]
      }

      const targetIndex = metricDefTargetIndex.value
      if (targetIndex != null && form.metrics[targetIndex]) {
        const alreadySelectedElsewhere = form.metrics.some((m, idx) => idx !== targetIndex && m.metricDefId === created.id)
        if (alreadySelectedElsewhere) {
          ElMessage.error('同一个 Run 内不能重复选择同一个指标定义')
        } else {
          form.metrics[targetIndex].metricDefId = created.id
          validateMetrics()
          metricDefDialogVisible.value = false
          await focusMetricValue(targetIndex)
          ElMessage.success(alreadyInList ? '指标已存在，已为你选择该指标' : '已创建指标定义')
        }
      } else {
        metricDefDialogVisible.value = false
        ElMessage.success(alreadyInList ? '指标已存在' : '已创建指标定义')
      }
    } finally {
      metricDefSubmitting.value = false
    }
  })
}

// --- Lifecycle ---
onMounted(async () => {
  // Check for query params from ProjectsView
  if (route.query.projectId) {
    const pid = Number(route.query.projectId)
    if (!isNaN(pid)) {
      filters.projectId = pid
    }
  }

  await fetchProjects()
  await fetchTags()
  await fetchMetricDefs()
  await fetchTemplates()
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
  optionsLoading.tags = true
  try {
    const res = await getTags()
    tags.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('加载标签失败')
  } finally {
    optionsLoading.tags = false
  }
}

const fetchMetricDefs = async () => {
  optionsLoading.metricDefs = true
  try {
    const res = await getMetricDefs()
    metricDefs.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('加载指标定义失败')
  } finally {
    optionsLoading.metricDefs = false
  }
}

const fetchTemplates = async () => {
  try {
    const res = await getTemplates({ page: 1, size: 100 })
    templates.value = res.data.records
  } catch (error) {
    console.error(error)
  }
}

import { useAiStore } from '@/stores/aiAssistant'

const aiStore = useAiStore()

// ...

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
    
    // Sync to AI Store (Runs Context)
    updateAiContext()
    
  } finally {
    loading.value = false
  }
}

// ...

const updateAiContext = () => {
  // Sync available runs
  // Note: we might want to map Run -> AiContextRun structure
  // Current Run interface: { id, name, status, tags, metrics ... }
  // We need to fetch full details? Usually list endpoint returns simplified data.
  // Requirement says: "Runs node supports: Multi-select, Search, Only display current page or last N"
  // So we can just use the current 'runs' list.
  // We might miss metrics/hyperparams if list doesn't have them.
  // Let's assume list has basic info, and if user selects, we might need more details.
  // But for Context Tree display, list info is enough. 
  // Backend AI context might need more.
  // If backend needs more, we might need to fetch details for selected runs before analyzing.
  // Phase 1 simplification: Use what we have in list. 
  // Actually, getRuns list usually doesn't return full metrics map.
  // Let's map what we can.
  
  const mappedRuns = runs.value.map(r => ({
      run_id: r.id,
      name: r.name,
      status: r.status,
      tags: [], // List might not have tags? Check interface. Interface has 'tags' in RunDetail but not Run?
                // Interface Run: id, projectId, name, status, modelName, datasetName, createdAt...
                // Interface RunDetail extends Run: + metrics, tags...
                // So Run list item might NOT have tags/metrics.
                // We might need to fetch details if we want rich context.
                // Or just pass basic info.
      metrics: {},
      hyperparameters: {
          model: r.modelName,
          dataset: r.datasetName
      }
  }))
  
  aiStore.setAvailableRuns(mappedRuns)
  
  // Also sync project context if filtered
  if (filters.projectId) {
      const currentProj = projects.value.find(p => p.id === filters.projectId)
      if (currentProj) {
          aiStore.setProjectContext({
              id: currentProj.id,
              name: currentProj.name,
              description: currentProj.description
          })
      }
  }
}

// Watch changes
watch(() => runs.value, () => {
    updateAiContext()
})


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
  currentRunId.value = row.id
  drawerVisible.value = true
}

// --- Create/Edit Dialog ---
const openCreateDialog = () => {
  isEdit.value = false
  currentRunId.value = null
  resetForm()
  fetchTags()
  fetchMetricDefs()
  dialogVisible.value = true
  focusTagInput()
}

const openEditDialog = async (row: Run) => {
  isEdit.value = true
  currentRunId.value = row.id
  try {
    await Promise.all([fetchTags(), fetchMetricDefs()])
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
      // templateId: data.templateId,
      tagIds: data.tags.map(t => t.id),
      metrics: data.metrics.map(m => ({
        metricDefId: m.metricDefId,
        value: m.value
      }))
    })
    dialogVisible.value = true
    focusTagInput()
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
    // templateId: undefined,
    tagIds: [],
    metrics: []
  })
  if (formRef.value) formRef.value.resetFields()
  metricError.value = ''
  metricValueRefs.value = []
  tagInput.value = ''
}

const handleTemplateChange = async (val: number | undefined) => {
  if (!val) return
  
  // If editing, user might be just changing template but keeping data.
  // But requirement says: "If user switches template: popup confirm overwrite"
  // Let's check if form has data.
  const hasData = form.metrics.length > 0 || form.tagIds!.length > 0
  
  if (hasData) {
    try {
      await ElMessageBox.confirm(
        'Do you want to overwrite current metrics and tags with template defaults?', 
        'Apply Template', 
        { confirmButtonText: 'Yes, Overwrite', cancelButtonText: 'No, Keep Current', type: 'warning' }
      )
      // Confirmed overwrite
      await applyTemplate(val)
    } catch {
      // Cancelled, do nothing (keep current data, but templateId is updated)
    }
  } else {
    // No data, apply directly
    await applyTemplate(val)
  }
}

const applyTemplate = async (id: number) => {
  try {
    const res = await getTemplate(id)
    const tmpl = res.data
    
    // Reset and apply
    form.metrics = []
    form.tagIds = []
    
    // Apply Metrics
    if (tmpl.metricDefs) {
      tmpl.metricDefs.forEach(m => {
        if (m.isDefault) {
          form.metrics.push({
            metricDefId: m.metricDefId,
            value: 0 // Default value placeholder
          })
        }
      })
    }
    
    // Apply Tags
    if (tmpl.tags) {
      const newTagIds: number[] = []
      tmpl.tags.forEach(t => {
        if (t.isDefault) {
          newTagIds.push(t.tagId)
        }
      })
      form.tagIds = newTagIds
      
      // Update selectedTags for chips display
      // We need to fetch tags if they are not in `tags` list?
      // `tags` list is already fetched in onMounted.
      // But we need to ensure selectedTags computed property works.
      // Wait, `selectedTags` logic in previous A2 implementation might rely on `form.tagIds` and `tags` list.
      // Let's check A2 implementation of `selectedTags`.
      // I don't see `selectedTags` defined in the visible code above, it must be there from A2 patch.
      // Assuming A2 patch added `selectedTags` computed property.
    }
    
    ElMessage.success(`Template "${tmpl.name}" applied`)
  } catch (error) {
    console.error(error)
  }
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

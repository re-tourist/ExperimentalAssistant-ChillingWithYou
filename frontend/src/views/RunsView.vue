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
      <el-table-column prop="startTime" label="Start Time" width="180">
        <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="endTime" label="End Time" width="180">
        <template #default="{ row }">
            {{ formatDateTime(row.endTime || row.updatedAt) }}
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
                <div class="tag-input-wrapper">
                  <div class="tag-selected-list">
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
                    placeholder="输入标签，↑↓选择，Enter/Tab 补全"
                    :disabled="creatingTag || optionsLoading.tags"
                    @focus="onTagFocus"
                    @blur="onTagBlur"
                    @keydown.down.prevent="onTagKeyDown('down')"
                    @keydown.up.prevent="onTagKeyDown('up')"
                    @keydown.enter.prevent="onTagConfirm('enter')"
                    @keydown.tab.prevent="onTagConfirm('tab')"
                    @input="onTagInputChange"
                  />
                  <div
                    v-if="tagSuggestions.length && tagInputFocused"
                    class="tag-suggestion-panel"
                  >
                    <div
                      v-for="(tag, idx) in tagSuggestions"
                      :key="tag.id"
                      :class="['tag-suggestion-item', { active: idx === activeTagIndex }]"
                      @mousedown.prevent="handleTagSuggestionClick(tag)"
                    >
                      {{ tag.name }}
                    </div>
                  </div>
                </div>
             </el-form-item>
           </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Start Time" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="Select start time"
                style="width: 100%"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="End Time" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="Select end time"
                style="width: 100%"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Dynamic Template Fields -->
        <div v-loading="templateLoading">
          <el-row :gutter="20">
            <el-col 
              v-for="field in currentTemplateFields" 
              :key="field.fieldKey"
              :span="field.fieldType === 'TEXTAREA' ? 24 : 12"
            >
              <el-form-item 
                :label="field.label" 
                :prop="'fieldValues.' + field.fieldKey"
                :rules="[{ required: field.isRequired, message: field.label + ' is required' }]"
              >
                <!-- TEXT -->
                <el-input 
                  v-if="field.fieldType === 'TEXT'" 
                  v-model="form.fieldValues[field.fieldKey]" 
                  :placeholder="field.placeholder || field.label" 
                />
                
                <!-- TEXTAREA -->
                <el-input 
                  v-else-if="field.fieldType === 'TEXTAREA'" 
                  type="textarea"
                  v-model="form.fieldValues[field.fieldKey]" 
                  :placeholder="field.placeholder || field.label" 
                />

                <!-- NUMBER -->
                <el-input-number 
                  v-else-if="field.fieldType === 'NUMBER'" 
                  v-model="form.fieldValues[field.fieldKey]" 
                  style="width: 100%"
                />
                
                <!-- SELECT -->
                <el-select 
                  v-else-if="field.fieldType === 'SELECT'" 
                  v-model="form.fieldValues[field.fieldKey]" 
                  style="width: 100%"
                >
                  <el-option 
                    v-for="opt in (field.optionsJson ? JSON.parse(field.optionsJson) : [])" 
                    :key="opt" 
                    :label="opt" 
                    :value="opt" 
                  />
                </el-select>

                <!-- BOOLEAN -->
                <el-switch 
                  v-else-if="field.fieldType === 'BOOLEAN'" 
                  v-model="form.fieldValues[field.fieldKey]" 
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

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
import RunDetailDrawer from '@/views/runs/RunDetailDrawer.vue'
import ActiveFilters from '@/components/ActiveFilters.vue'
import type { Run, RunCreateUpdate } from '@/api/runs'
import type { Project } from '@/api/projects'
import type { Tag } from '@/api/tags'
import type { MetricDef } from '@/api/metrics'

import { getTemplate } from '@/api/templates'
import type { TemplateField } from '@/api/templates'

const route = useRoute()

// --- State ---
const loading = ref(false)
const runs = ref<Run[]>([])
const projects = ref<Project[]>([])
const tags = ref<Tag[]>([])
const metricDefs = ref<MetricDef[]>([])
const currentTemplateFields = ref<TemplateField[]>([])
const templateLoading = ref(false)

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
const form = reactive<RunCreateUpdate & { metrics: any[], fieldValues: Record<string, any> }>({
  projectId: undefined as any,
  name: '',
  status: 'RUNNING',
  fieldValues: {},
  note: '',
  metrics: [] as any[],
  tagIds: [] as number[],
  startTime: '',
  endTime: ''
})

const castTemplateFieldValue = (field: TemplateField, value: any) => {
  if (value === undefined || value === null) return value
  if (typeof value === 'string' && value.trim() === '') return value
  if (field.fieldType === 'NUMBER') {
    const n = Number(value)
    return Number.isFinite(n) ? n : value
  }
  if (field.fieldType === 'BOOLEAN') {
    if (typeof value === 'boolean') return value
    const s = String(value).toLowerCase()
    if (s === 'true') return true
    if (s === 'false') return false
    return value
  }
  return value
}

const buildTemplateDefaults = (fields: TemplateField[]) => {
  const defaults: Record<string, any> = {}
  fields.forEach(f => {
    if (f.defaultValue !== undefined && f.defaultValue !== null && f.defaultValue !== '') {
      defaults[f.fieldKey] = castTemplateFieldValue(f, f.defaultValue)
    }
  })
  return defaults
}

const normalizeFieldValuesByTemplate = (raw: Record<string, any> | undefined, fields: TemplateField[]) => {
  const base: Record<string, any> = { ...raw }
  const fieldMap = new Map(fields.map(f => [f.fieldKey, f] as const))
  Object.keys(base).forEach(key => {
    const field = fieldMap.get(key)
    if (!field) return
    base[key] = castTemplateFieldValue(field, base[key])
  })
  return base
}

watch(() => form.projectId, async (newVal) => {
  if (!newVal) {
    currentTemplateFields.value = []
    form.fieldValues = {}
    return
  }
  
  // Find project to get templateId
  const project = projects.value.find(p => p.id === newVal)
  if (project && project.templateId) {
    templateLoading.value = true
    try {
      const res = await getTemplate(project.templateId)
      currentTemplateFields.value = res.data.fields || []
      
      const defaults = buildTemplateDefaults(currentTemplateFields.value)
      if (isEdit.value) {
        const existing = normalizeFieldValuesByTemplate(form.fieldValues, currentTemplateFields.value)
        form.fieldValues = { ...defaults, ...existing }
      } else {
        form.fieldValues = { ...defaults }
      }
    } catch (e) {
      console.error(e)
    } finally {
      templateLoading.value = false
    }
  } else {
    currentTemplateFields.value = []
    form.fieldValues = {}
  }
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
const tagInputFocused = ref(false)
const creatingTag = ref(false)
const activeTagIndex = ref(-1)

const selectedTags = computed(() => {
  const idSet = new Set(form.tagIds || [])
  return tags.value.filter(t => t.id != null && idSet.has(t.id))
})

const tagSuggestions = computed(() => {
  const query = tagInput.value.trim().toLowerCase()
  const sorted = [...tags.value].sort((a, b) =>
    (a.name || '').localeCompare(b.name || '', 'en', { sensitivity: 'base' })
  )

  if (!query) {
    return sorted.slice(0, 20)
  }
  return sorted
    .filter(t => (t.name || '').toLowerCase().includes(query))
    .slice(0, 20)
})

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

const removeSelectedTag = (tagId: number | undefined) => {
  if (tagId == null) return
  form.tagIds = (form.tagIds || []).filter(id => id !== tagId)
}

const findTagByName = (name: string) => {
  const normalized = name.trim().toLowerCase()
  return tags.value.find(t => (t.name || '').trim().toLowerCase() === normalized)
}

const onTagFocus = () => {
  tagInputFocused.value = true
}

const onTagBlur = () => {
  tagInputFocused.value = false
  activeTagIndex.value = -1
}

const onTagInputChange = () => {
  activeTagIndex.value = tagSuggestions.value.length > 0 ? 0 : -1
}

const handleTagSuggestionClick = (tag: Tag) => {
  addTagId(tag.id)
  tagInput.value = ''
  activeTagIndex.value = -1
  focusTagInput()
}

const confirmTagByText = async () => {
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

const onTagKeyDown = (dir: 'up' | 'down') => {
  const list = tagSuggestions.value
  if (!list.length) return

  if (activeTagIndex.value === -1) {
    activeTagIndex.value = dir === 'down' ? 0 : list.length - 1
    return
  }

  if (dir === 'down') {
    activeTagIndex.value = (activeTagIndex.value + 1) % list.length
  } else {
    activeTagIndex.value = (activeTagIndex.value - 1 + list.length) % list.length
  }
}

const onTagConfirm = async (_from: 'enter' | 'tab') => {
  const list = tagSuggestions.value
  if (list.length && activeTagIndex.value >= 0 && activeTagIndex.value < list.length) {
    const tag = list[activeTagIndex.value]
    addTagId(tag.id)
    tagInput.value = ''
    activeTagIndex.value = -1
    await focusTagInput()
    return
  }

  await confirmTagByText()
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

const handleProjectChange = (projectId: number | undefined) => {
  if (!projectId) return
  const project = projects.value.find(p => p.id === projectId)
  if (!project || !project.projectConfigSnapshot) return

  // Apply snapshot defaults
  try {
    const snapshot = JSON.parse(project.projectConfigSnapshot)
    // Snapshot structure from backend: { sourceTemplateId, configJson, ... }
    // Wait, the backend snapshot logic (ProjectServiceImpl.java) put:
    // sourceTemplateId, sourceTemplateName, domain, configJson
    // It didn't put metricDefs or tags explicitly into the map!
    // It relied on "inherit flexible config".
    // BUT, TemplateUpsertRequest has metricDefs and tags.
    // Template entity has configJson string.
    // If we want to apply defaults, we need the metricDefs and tags in the snapshot.
    // Backend `handleTemplateSnapshot` only put `configJson` string.
    // It did NOT put the relations (template_metric_def, template_tag).
    // THIS IS A BUG/MISSING FEATURE IN PHASE 1 BACKEND.
    
    // I need to fix Backend ProjectServiceImpl first to include metricDefs and tags in snapshot.
    // Otherwise Frontend cannot apply them.

    // Apply defaults
    form.metrics = []
    form.tagIds = []

    if (snapshot.metricDefs && Array.isArray(snapshot.metricDefs)) {
      snapshot.metricDefs.forEach((m: any) => {
        if (m.isDefault) {
          form.metrics.push({
            metricDefId: m.metricDefId,
            value: 0
          })
        }
      })
    }

    if (snapshot.tags && Array.isArray(snapshot.tags)) {
      const newTags: number[] = []
      snapshot.tags.forEach((t: any) => {
        if (t.isDefault) {
          newTags.push(t.tagId)
        }
      })
      form.tagIds = newTags
    }

    ElMessage.success('Project defaults applied')
  } catch (e) {
    console.error('Failed to parse project config snapshot', e)
  }
}

// Watch changes
watch(() => form.projectId, (newVal) => {
  if (!isEdit.value && newVal) {
    handleProjectChange(newVal)
  }
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

const formatDateTime = (val?: string) => {
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
    resetForm()
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
      fieldValues: data.fieldValues || {},
      tagIds: data.tags.map(t => t.id),
      metrics: data.metrics.map(m => ({
        metricDefId: m.metricDefId,
        value: m.value
      })),
      startTime: data.startTime,
      endTime: data.endTime
    })
    dialogVisible.value = true
    await nextTick()
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
    fieldValues: {},
    note: '',
    // templateId: undefined,
    tagIds: [],
    metrics: [],
    startTime: '',
    endTime: ''
  })
  if (formRef.value) formRef.value.resetFields()
  metricError.value = ''
  metricValueRefs.value = []
  tagInput.value = ''
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
        const payload: any = { ...form }
        if (!payload.startTime) delete payload.startTime
        if (!payload.endTime) delete payload.endTime
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
.tag-input-wrapper {
  position: relative;
}
.tag-selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 4px;
}
.tag-suggestion-panel {
  position: absolute;
  left: 0;
  right: 0;
  top: 100%;
  margin-top: 4px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  max-height: 180px;
  overflow-y: auto;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  z-index: 2000;
}
.tag-suggestion-item {
  padding: 4px 8px;
  font-size: 13px;
  line-height: 1.4;
  cursor: pointer;
}
.tag-suggestion-item.active {
  background-color: var(--el-color-primary-light-9, #ecf5ff);
  color: var(--el-color-primary, #409eff);
}
</style>

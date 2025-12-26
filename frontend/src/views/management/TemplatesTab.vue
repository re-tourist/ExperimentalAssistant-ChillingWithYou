<template>
  <div class="templates-tab">
    <div class="filter-bar">
      <el-input v-model="filters.q" placeholder="Search Templates" style="width: 200px; margin-right: 10px" @keyup.enter="fetchData" />
      <el-select v-model="filters.domain" placeholder="Domain" clearable style="width: 150px; margin-right: 10px" @change="fetchData">
        <el-option label="Classification" value="classification" />
        <el-option label="Regression" value="regression" />
        <el-option label="Clustering" value="clustering" />
        <el-option label="NLP" value="nlp" />
        <el-option label="CV" value="cv" />
        <el-option label="Custom" value="custom" />
      </el-select>
      <el-button type="primary" @click="fetchData">Search</el-button>
      <el-button type="success" @click="openCreateDialog">New Template</el-button>
    </div>

    <el-table v-loading="loading" :data="templates" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="Name" min-width="150" />
      <el-table-column prop="domain" label="Domain" width="120">
        <template #default="{ row   domain: [{ required: true, message: 'Please select domain', trigger: 'change' }]
}">
          <el-tag>{{ row.domain }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="Description" show-overflow-tooltip />
      <el-table-column prop="updatedAt" label="Updated At" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">View</el-button>
          <el-button link type="primary" @click="handleEdit(row)">Edit</el-button>
          <el-button link type="danger" @click="handleDelete(row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'Edit Template' : 'New Template'"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Name" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Domain" prop="domain">
              <el-select v-model="form.domain" style="width: 100%">
                <el-option label="Classification" value="classification" />
                <el-option label="Regression" value="regression" />
                <el-option label="Clustering" value="clustering" />
                <el-option label="NLP" value="nlp" />
                <el-option label="CV" value="cv" />
                <el-option label="Custom" value="custom" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Description" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="Config JSON" prop="configJson">
          <el-input v-model="form.configJson" type="textarea" :rows="3" placeholder="{}" />
        </el-form-item>

        <!-- Metric Config -->
        <el-divider content-position="left">Metric Definitions</el-divider>
        <el-button size="small" @click="addMetricRow" style="margin-bottom: 10px">Add Metric</el-button>
        <el-table :data="form.metricDefs" border size="small" style="width: 100%">
          <el-table-column label="Metric Definition">
            <template #default="{ row }">
              <el-select v-model="row.metricDefId" placeholder="Select Metric" style="width: 100%" filterable>
                <el-option
                  v-for="def in allMetricDefs"
                  :key="def.id"
                  :label="`${def.name} (${def.direction})`"
                  :value="def.id"
                  :disabled="isMetricDisabled(def.id!)"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="Default?" width="100" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.isDefault" />
            </template>
          </el-table-column>
          <el-table-column label="Sort Order" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.sortOrder" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column width="60" align="center">
            <template #default="{ $index }">
              <el-button type="danger" circle size="small" @click="removeMetricRow($index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Tag Config -->
        <el-divider content-position="left">Tags</el-divider>
        <el-button size="small" @click="addTagRow" style="margin-bottom: 10px">Add Tag</el-button>
        <el-table :data="form.tags" border size="small" style="width: 100%">
          <el-table-column label="Tag">
            <template #default="{ row }">
              <el-select v-model="row.tagId" placeholder="Select Tag" style="width: 100%" filterable>
                <el-option
                  v-for="tag in allTags"
                  :key="tag.id"
                  :label="tag.name"
                  :value="tag.id"
                  :disabled="isTagDisabled(tag.id!)"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="Default?" width="100" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.isDefault" />
            </template>
          </el-table-column>
          <el-table-column label="Sort Order" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.sortOrder" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column width="60" align="center">
            <template #default="{ $index }">
              <el-button type="danger" circle size="small" @click="removeTagRow($index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>

      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">Cancel</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">Confirm</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- View Drawer -->
    <el-drawer v-model="drawerVisible" title="Template Details" size="50%">
      <div v-if="currentTemplate">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ currentTemplate.id }}</el-descriptions-item>
          <el-descriptions-item label="Name">{{ currentTemplate.name }}</el-descriptions-item>
          <el-descriptions-item label="Domain">{{ currentTemplate.domain }}</el-descriptions-item>
          <el-descriptions-item label="Description">{{ currentTemplate.description }}</el-descriptions-item>
          <el-descriptions-item label="Updated At">{{ formatDateTime(currentTemplate.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
        
        <div style="margin-top: 20px">
          <h4>Metric Definitions</h4>
          <el-table :data="currentTemplate.metricDefs" border stripe size="small">
             <el-table-column prop="name" label="Name" />
             <el-table-column prop="isDefault" label="Default">
               <template #default="{ row }">
                 <el-icon v-if="row.isDefault" color="green"><Check /></el-icon>
               </template>
             </el-table-column>
             <el-table-column prop="sortOrder" label="Order" width="80" />
          </el-table>
        </div>

        <div style="margin-top: 20px">
          <h4>Tags</h4>
          <el-table :data="currentTemplate.tags" border stripe size="small">
             <el-table-column prop="name" label="Name" />
             <el-table-column prop="isDefault" label="Default">
               <template #default="{ row }">
                 <el-icon v-if="row.isDefault" color="green"><Check /></el-icon>
               </template>
             </el-table-column>
             <el-table-column prop="sortOrder" label="Order" width="80" />
          </el-table>
        </div>
        
        <div style="margin-top: 20px">
           <h4>Config JSON</h4>
           <pre style="background: #f5f7fa; padding: 10px; border-radius: 4px">{{ currentTemplate.configJson }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Delete, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTemplates, getTemplate, createTemplate, updateTemplate, deleteTemplate } from '@/api/templates'
import { getMetricDefs } from '@/api/metrics'
import { getTags } from '@/api/tags'
import type { Template, TemplateDetail, TemplateUpsertRequest } from '@/api/templates'
import type { MetricDef } from '@/api/metrics'
import type { Tag } from '@/api/tags'

const loading = ref(false)
const templates = ref<Template[]>([])
const filters = reactive({ q: '', domain: '' })

// Create/Edit
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const currentId = ref<number | null>(null)

const form = reactive<TemplateUpsertRequest>({
  name: '',
  domain: 'general',
  description: '',
  configJson: '',
  metricDefs: [],
  tags: []
})

const rules = {
  name: [{ required: true, message: 'Please enter name', trigger: 'blur' }],
  domain: [{ required: true, message: 'Please select domain', trigger: 'change' }]
}

// Options
const allMetricDefs = ref<MetricDef[]>([])
const allTags = ref<Tag[]>([])

// View
const drawerVisible = ref(false)
const currentTemplate = ref<TemplateDetail | null>(null)

onMounted(async () => {
  fetchData()
  // Pre-load options
  try {
    const [mRes, tRes] = await Promise.all([getMetricDefs(), getTags()])
    allMetricDefs.value = mRes.data
    allTags.value = tRes.data
  } catch (e) {
    console.error(e)
  }
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTemplates({ 
      q: filters.q, 
      domain: filters.domain,
      page: 1, 
      size: 100 
    })
    templates.value = res.data.records
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleView = async (row: Template) => {
  try {
    const res = await getTemplate(row.id)
    currentTemplate.value = res.data
    drawerVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

const handleEdit = async (row: Template) => {
  isEdit.value = true
  currentId.value = row.id
  try {
    const res = await getTemplate(row.id)
    const data = res.data
    form.name = data.name
    form.domain = data.domain
    form.description = data.description
    form.configJson = data.configJson
    form.metricDefs = data.metricDefs.map(m => ({
      metricDefId: m.metricDefId,
      isDefault: m.isDefault,
      sortOrder: m.sortOrder
    }))
    form.tags = data.tags.map(t => ({
      tagId: t.tagId,
      isDefault: t.isDefault,
      sortOrder: t.sortOrder
    }))
    dialogVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

const handleDelete = (row: Template) => {
  ElMessageBox.confirm('Are you sure to delete this template?', 'Warning', {
    type: 'warning'
  }).then(async () => {
    await deleteTemplate(row.id)
    ElMessage.success('Deleted')
    fetchData()
  })
}

const resetForm = () => {
  form.name = ''
  form.domain = 'general'
  form.description = ''
  form.configJson = ''
  form.metricDefs = []
  form.tags = []
  if (formRef.value) formRef.value.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitting.value = true
      try {
        if (isEdit.value && currentId.value) {
          await updateTemplate(currentId.value, form)
          ElMessage.success('Updated')
        } else {
          await createTemplate(form)
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
  })
}

// Helper for dynamic rows
const addMetricRow = () => {
  form.metricDefs?.push({ metricDefId: undefined as any, isDefault: true, sortOrder: 0 })
}
const removeMetricRow = (idx: number) => {
  form.metricDefs?.splice(idx, 1)
}
const isMetricDisabled = (id: number) => {
  return form.metricDefs?.some(m => m.metricDefId === id)
}

const addTagRow = () => {
  form.tags?.push({ tagId: undefined as any, isDefault: true, sortOrder: 0 })
}
const removeTagRow = (idx: number) => {
  form.tags?.splice(idx, 1)
}
const isTagDisabled = (id: number) => {
  return form.tags?.some(t => t.tagId === id)
}

const formatDateTime = (val?: string) => {
  return val ? new Date(val).toLocaleString() : '-'
}
</script>

<style scoped>
.filter-bar {
  margin-bottom: 20px;
}
</style>

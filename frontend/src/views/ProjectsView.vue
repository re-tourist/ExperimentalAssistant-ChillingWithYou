<template>
  <div class="projects-container">
    <div class="header">
      <div class="title-group">
        <h1>Projects</h1>
        <p class="subtitle">Manage your experiment projects.</p>
      </div>
    </div>

    <el-card shadow="hover" class="main-card">
      <!-- Toolbar -->
      <div class="toolbar">
        <div class="search-area">
          <el-input
            v-model="searchQuery"
            placeholder="Search by project name"
            clearable
            style="width: 300px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button @click="handleSearch">Filter</el-button>
          <el-button @click="handleReset">Reset</el-button>
        </div>
        <div class="actions">
          <el-button :icon="Refresh" circle @click="refreshData" />
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">Create Project</el-button>
        </div>
      </div>

      <ActiveFilters
        v-model:filters="filters"
        :config="filterConfig"
        @change="handleFiltersChange"
      />

      <!-- Table -->
      <el-table
        v-loading="loading"
        :data="projects"
        style="width: 100%"
        stripe
      >
        <template #empty>
          <el-empty description="No projects found">
            <el-button type="primary" @click="openCreateDialog">Create Project</el-button>
          </el-empty>
        </template>

        <el-table-column prop="name" label="Name" min-width="200">
          <template #default="{ row }">
            <span class="project-name-link" @click="goToRuns(row)">{{ row.name }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="Description" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="description-text">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Runs" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.runCount !== undefined && row.runCount !== '-'" type="info" effect="plain" round>
              {{ row.runCount }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="Created At" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="Actions" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">Edit</el-button>
            <el-button link type="primary" @click="goToRuns(row)">View Runs</el-button>
            <el-button link type="danger" @click="handleDelete(row)">Delete</el-button>
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
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'Edit Project' : 'Create Project'"
      width="500px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        @submit.prevent
      >
        <el-form-item label="Name" prop="name">
          <el-input v-model="form.name" placeholder="Project Name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="Description" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="Description (optional)"
            maxlength="200"
            show-word-limit
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="Template" prop="templateId">
          <el-select v-model="form.templateId" placeholder="Select a template" style="width: 100%">
            <el-option
              v-for="item in templates"
              :key="item.id"
              :label="item.name + (item.domain ? ` (${item.domain})` : '')"
              :value="item.id"
            />
          </el-select>
          <div v-if="isEdit && form.templateId" class="form-tip">
             Warning: Changing template will update the snapshot but won't affect existing runs.
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">Cancel</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? 'Update' : 'Create' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
import ActiveFilters from '@/components/ActiveFilters.vue'
import { getProjects, createProject, updateProject, deleteProject, type Project } from '@/api/projects'
import { getRuns } from '@/api/runs'
import { getTemplates, type Template } from '@/api/templates'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()

// --- State ---
const loading = ref(false)
const projects = ref<Project[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const searchQuery = ref('')
// Store actual query applied to list (for pagination consistency)
const filters = reactive({ q: '' })

const filterConfig = {
  q: { label: 'Search' }
}

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const currentId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  description: '',
  templateId: undefined as number | undefined
})

const templates = ref<Template[]>([])

const rules = reactive<FormRules>({
  name: [
    { required: true, message: 'Please enter project name', trigger: 'blur' },
    { min: 2, max: 50, message: 'Length should be 2 to 50', trigger: 'blur' }
  ],
  description: [
    { max: 200, message: 'Length should be less than 200', trigger: 'blur' }
  ],
  templateId: [
    { required: true, message: 'Please select a template', trigger: 'change' }
  ]
})

// --- Lifecycle ---
onMounted(() => {
  fetchProjects()
})

// --- Methods ---
const fetchProjects = async () => {
  loading.value = true
  try {
    const res = await getProjects({
      page: pagination.page,
      size: pagination.size,
      q: filters.q
    })
    projects.value = res.data.records || []
    pagination.total = res.data.total
    
    // Best-effort: fetch run counts
    fetchRunCounts(projects.value)
  } catch (error) {
    console.error('Failed to fetch projects', error)
  } finally {
    loading.value = false
  }
}

// Strategy A: Best-effort run count fetching
const fetchRunCounts = async (projectList: Project[]) => {
  if (!projectList.length) return
  
  // Create a pool of promises
  const promises = projectList.map(async (p) => {
    if (!p.id) return
    try {
      // Fetch 1 item just to get total
      const res = await getRuns({ page: 1, size: 1, projectId: p.id })
      p.runCount = res.data.total
    } catch {
      // Silent fail, show '-'
      p.runCount = '-'
    }
  })

  // Execute all (could limit concurrency if list is huge, but page size is usually small)
  await Promise.allSettled(promises)
}

const handleSearch = () => {
  filters.q = searchQuery.value
  pagination.page = 1
  fetchProjects()
}

const handleReset = () => {
  searchQuery.value = ''
  filters.q = ''
  pagination.page = 1
  fetchProjects()
}

const handleFiltersChange = () => {
  searchQuery.value = filters.q || ''
  pagination.page = 1
  fetchProjects()
}

const refreshData = () => {
  fetchProjects()
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  fetchProjects()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchProjects()
}

const formatDateTime = (val?: string) => {
  return val ? new Date(val).toLocaleString() : '-'
}

const goToRuns = (row: Project) => {
  if (!row.id) return
  router.push({ 
    name: 'runs', 
    query: { projectId: String(row.id) } 
  })
}

// --- Dialog & CRUD ---
const openCreateDialog = async () => {
  isEdit.value = false
  currentId.value = null
  resetForm()
  dialogVisible.value = true
  // Fetch templates for dropdown
  try {
    const res = await getTemplates({ page: 1, size: 100 })
    templates.value = res.data.records
    const defaultTemplate = templates.value.find(t => t.isDefault)
    if (defaultTemplate) {
      form.templateId = defaultTemplate.id
    } else if (templates.value.length > 0) {
      form.templateId = templates.value[0].id
    }
  } catch (e) { console.error(e) }
}

const openEditDialog = async (row: Project) => {
  isEdit.value = true
  currentId.value = row.id || null
  form.name = row.name
  form.description = row.description || ''
  // @ts-ignore
  form.templateId = row.templateId
  
  dialogVisible.value = true
  try {
    const res = await getTemplates({ page: 1, size: 100 })
    templates.value = res.data.records
  } catch (e) { console.error(e) }
}

const resetForm = () => {
  form.name = ''
  form.description = ''
  form.templateId = undefined
  if (formRef.value) formRef.value.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (isEdit.value && currentId.value) {
          await updateProject(currentId.value, { ...form })
          ElMessage.success('Project updated successfully')
        } else {
          await createProject({ ...form })
          ElMessage.success('Project created successfully')
        }
        dialogVisible.value = false
        // Refresh list
        fetchProjects()
      } catch (error) {
        console.error(error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleDelete = (row: Project) => {
  if (!row.id) return
  ElMessageBox.confirm(
    `Are you sure you want to delete project "${row.name}"?`,
    'Warning',
    {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteProject(row.id!)
      ElMessage.success('Project deleted successfully')
      
      // Check if current page becomes empty
      if (projects.value.length === 1 && pagination.page > 1) {
        pagination.page--
      }
      fetchProjects()
    } catch (error) {
      console.error(error)
    }
  }).catch(() => {})
}
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.5;
  margin-top: 5px;
}
.projects-container {
  padding: 20px;
}
.header {
  margin-bottom: 20px;
}
.title-group h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}
.subtitle {
  margin: 5px 0 0;
  color: #909399;
  font-size: 14px;
}
.main-card {
  min-height: 500px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}
.search-area {
  display: flex;
  gap: 10px;
  align-items: center;
}
.project-name-link {
  font-weight: 600;
  color: #409EFF;
  cursor: pointer;
}
.project-name-link:hover {
  text-decoration: underline;
}
.description-text {
  color: #606266;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

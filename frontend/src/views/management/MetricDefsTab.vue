<template>
  <div class="metrics-tab">
    <div class="filter-bar">
      <el-button type="success" @click="openCreateDialog">Create Metric Definition</el-button>
    </div>

    <el-table v-loading="loading" :data="metricDefs" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="Name" />
      <el-table-column prop="direction" label="Direction" width="100">
        <template #default="{ row }">
          <el-tag :type="row.direction === 'MAX' ? 'success' : 'warning'">{{ row.direction }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="Description" />
      <el-table-column prop="createdAt" label="Created At">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="140">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEditDialog(row)">Edit</el-button>
          <el-button type="danger" link @click="handleDelete(row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? 'Edit Metric Definition' : 'Create Metric Definition'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" @submit.prevent>
        <el-form-item label="Name" prop="name">
          <el-input 
            v-model="form.name" 
            placeholder="e.g. acc" 
            @keydown.enter.prevent="handleSubmit"
          />
        </el-form-item>
        <el-form-item label="Direction" prop="direction">
          <el-select v-model="form.direction" style="width: 100%">
            <el-option label="MAX" value="MAX" />
            <el-option label="MIN" value="MIN" />
            <el-option label="NONE" value="NONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="Description" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">Cancel</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">Confirm</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getMetricDefs, createMetricDef, updateMetricDef, deleteMetricDef } from '@/api/metrics'
import type { MetricDef } from '@/api/metrics'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const metricDefs = ref<MetricDef[]>([])

const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const formRef = ref()
const form = reactive<{ name: string; direction: 'MAX' | 'MIN' | 'NONE'; description: string }>({ name: '', direction: 'MAX', description: '' })
const rules = {
  name: [{ required: true, message: 'Please enter metric name', trigger: 'blur' }],
  direction: [{ required: true, message: 'Please select direction', trigger: 'change' }]
}

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMetricDefs()
    metricDefs.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  currentId.value = null
  Object.assign(form, { name: '', direction: 'MAX', description: '' })
  dialogVisible.value = true
}

const openEditDialog = (row: MetricDef) => {
  if (!row.id) return
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, {
    name: row.name,
    direction: row.direction,
    description: row.description || ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitting.value = true
      try {
        if (isEdit.value && currentId.value != null) {
          await updateMetricDef(currentId.value, { ...form })
          ElMessage.success('Updated successfully')
        } else {
          await createMetricDef({ ...form })
          ElMessage.success('Created successfully')
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

const handleDelete = async (row: MetricDef) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`Delete metric definition "${row.name}"?`, 'Confirm', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await deleteMetricDef(row.id)
    ElMessage.success('Deleted successfully')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
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

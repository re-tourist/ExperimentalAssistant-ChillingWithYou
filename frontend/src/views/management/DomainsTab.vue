<template>
  <div class="domains-tab">
    <div class="filter-bar">
      <el-input 
        v-model="filters.q" 
        placeholder="Search Domains" 
        style="width: 200px; margin-right: 10px" 
        @keyup.enter="fetchData" 
      />
      <el-button type="primary" @click="fetchData">Search</el-button>
      <el-button type="success" @click="openCreateDialog">Create Domain</el-button>
    </div>

    <el-table v-loading="loading" :data="domains" style="width: 100%">
      <el-table-column prop="name" label="Name" />
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? 'Edit Domain' : 'Create Domain'" width="400px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" @submit.prevent>
        <el-form-item label="Name" prop="name">
          <el-input 
            v-model="form.name" 
            autofocus
            @keydown.enter.prevent="handleSubmit" 
            placeholder="Press Enter to submit"
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
import { getDomains, createDomain, updateDomain, deleteDomain } from '@/api/domains'
import type { Domain } from '@/api/domains'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const domains = ref<Domain[]>([])
const filters = reactive({ q: '' })

const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const formRef = ref()
const form = reactive({ name: '' })
const rules = {
  name: [{ required: true, message: 'Please enter domain name', trigger: 'blur' }]
}

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDomains({ q: filters.q })
    domains.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  currentId.value = null
  form.name = ''
  dialogVisible.value = true
}

const openEditDialog = (row: Domain) => {
  if (!row.id) return
  isEdit.value = true
  currentId.value = row.id
  form.name = row.name
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  // Frontend Pre-check
  const normalizedName = form.name.trim().toLowerCase()
  if (!normalizedName) return

  const existing = domains.value.find(d => 
    d.name.toLowerCase() === normalizedName && d.id !== currentId.value
  )
  
  if (existing) {
    ElMessageBox.alert(`Domain "${existing.name}" already exists.`, 'Already Exists', {
      confirmButtonText: 'OK',
      type: 'warning'
    })
    return
  }

  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitting.value = true
      try {
        if (isEdit.value && currentId.value != null) {
          await updateDomain(currentId.value, { name: form.name })
          ElMessage.success('Updated successfully')
        } else {
          await createDomain({ name: form.name })
          ElMessage.success('Created successfully')
        }
        dialogVisible.value = false
        fetchData()
      } catch (error: any) {
        console.error(error)
        // If backend returns error (which it should now), display it
        // Assuming request interceptor handles it or we catch it here.
        // If not handled by interceptor globally:
        if (error.response?.data?.message) {
           ElMessage.error(error.response.data.message)
        }
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleDelete = async (row: Domain) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`Delete domain "${row.name}"?`, 'Confirm', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
    await deleteDomain(row.id)
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

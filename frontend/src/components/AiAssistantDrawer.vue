<template>
  <el-drawer
    v-model="aiStore.drawerVisible"
    title="AI Assistant"
    size="500px"
    :before-close="handleClose"
    direction="rtl"
    class="ai-assistant-drawer"
  >
    <div class="drawer-content">
      <!-- 1. Context Tree Section -->
      <div class="section context-section">
        <h3 class="section-title">Context Tree</h3>
        <el-card shadow="never" class="tree-card">
          <el-tree
            ref="treeRef"
            :data="treeData"
            show-checkbox
            node-key="id"
            default-expand-all
            :default-checked-keys="defaultCheckedKeys"
            @check-change="handleCheckChange"
          >
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <span>{{ node.label }}</span>
                <span v-if="data.count" class="badge">{{ data.count }}</span>
              </span>
            </template>
          </el-tree>
        </el-card>
        
        <!-- Context Preview -->
        <div class="context-preview">
          <el-collapse>
            <el-collapse-item title="Context Preview (JSON)" name="1">
               <pre class="json-preview">{{ JSON.stringify(aiStore.finalContextJson, null, 2) }}</pre>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>

      <!-- 2. User Intent Section -->
      <div class="section intent-section">
        <h3 class="section-title">User Intent</h3>
        <el-input
          v-model="aiStore.userIntent"
          type="textarea"
          :rows="3"
          placeholder="e.g., Analyze the trend of accuracy over time..."
        />
        <div class="actions">
           <el-button 
             type="primary" 
             :loading="aiStore.loading" 
             @click="aiStore.analyze"
             :disabled="!aiStore.userIntent.trim()"
           >
             Analyze
           </el-button>
        </div>
      </div>

      <!-- 3. Output Section -->
      <div class="section output-section" v-if="aiStore.lastAnalysis">
        <h3 class="section-title">Analysis Result</h3>
        <div class="markdown-body">
           <pre class="markdown-raw">{{ aiStore.lastAnalysis }}</pre>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useAiStore } from '@/stores/aiAssistant'

const aiStore = useAiStore()
const treeRef = ref()

// Construct Tree Data Structure
const treeData = computed(() => {
  const data = []
  
  // Project Node
  if (aiStore.currentProject) {
    data.push({
      id: 'project',
      label: `Project: ${aiStore.currentProject.name}`,
      children: [
        { id: 'project_meta', label: 'Meta Info (Name, Desc)' }
      ]
    })
  } else {
    data.push({
        id: 'project',
        label: 'Project (No Project Selected)',
        disabled: true
    })
  }
  
  // Dashboard Node
  if (aiStore.dashboardSnapshot) {
    data.push({
      id: 'dashboard',
      label: 'Dashboard Snapshot',
      children: [
        { id: 'dashboard_summary', label: 'Summary' },
        { id: 'dashboard_trend', label: 'Trend' },
        { id: 'dashboard_dist', label: 'Distribution' },
        { id: 'dashboard_top', label: 'Top Runs' }
      ]
    })
  }
  
  // Runs Node
  const runs = aiStore.availableRuns || []
  if (runs.length > 0) {
    data.push({
      id: 'runs',
      label: 'Runs',
      count: runs.length,
      children: runs.map(r => ({
        id: `run_${r.run_id}`,
        label: `Run #${r.run_id} ${r.name}`,
        isRun: true,
        runId: r.run_id
      }))
    })
  } else {
    data.push({
        id: 'runs',
        label: 'Runs (None available)',
        disabled: true
    })
  }
  
  return data
})

const defaultCheckedKeys = computed(() => {
  const keys = []
  if (aiStore.selection.project) keys.push('project', 'project_meta')
  if (aiStore.selection.dashboard) keys.push('dashboard', 'dashboard_summary', 'dashboard_trend', 'dashboard_dist', 'dashboard_top')
  if (aiStore.selection.runs.length > 0) {
    keys.push('runs')
    aiStore.selection.runs.forEach(rid => keys.push(`run_${rid}`))
  }
  return keys
})

// Sync Checkbox changes back to store
const handleCheckChange = () => {
  if (!treeRef.value) return
  const keys = treeRef.value.getCheckedKeys()
  
  // Update Store Selection
  aiStore.selection.project = keys.includes('project')
  aiStore.selection.dashboard = keys.includes('dashboard')
  
  const selectedRunIds = keys
    .filter((k: string) => k.startsWith('run_'))
    .map((k: string) => parseInt(k.replace('run_', '')))
    
  aiStore.selection.runs = selectedRunIds
}

// Watch store state to update tree check status if modified externally
watch(() => aiStore.selection, () => {
    if (treeRef.value) {
        treeRef.value.setCheckedKeys(defaultCheckedKeys.value)
    }
}, { deep: true })

const handleClose = () => {
  aiStore.closeDrawer()
}
</script>

<style scoped>
.ai-assistant-drawer .el-drawer__body {
  padding: 0;
}
.drawer-content {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
  overflow-y: auto;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
}
.tree-card {
  max-height: 300px;
  overflow-y: auto;
}
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}
.badge {
  background-color: #f0f2f5;
  color: #909399;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
}
.json-preview {
  font-size: 12px;
  color: #606266;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 200px;
}
.actions {
  margin-top: 10px;
  text-align: right;
}
.output-section {
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
}
.markdown-body {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  min-height: 200px;
  background-color: #fff;
}
.markdown-raw {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
}
</style>

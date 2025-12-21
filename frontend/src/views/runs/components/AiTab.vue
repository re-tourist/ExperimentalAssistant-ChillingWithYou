<template>
  <div class="ai-tab">
    <div class="controls">
      <el-input 
        v-model="form.userHint" 
        placeholder="What should the AI focus on? (e.g., 'Check for overfitting')" 
        class="mb-3"
      >
        <template #prepend>Hint</template>
      </el-input>
      
      <div class="switches mb-3">
        <el-checkbox v-model="form.includeMetrics" label="Metrics" />
        <el-checkbox v-model="form.includeTags" label="Tags" />
        <el-checkbox v-model="form.includeTemplate" label="Template" />
        <el-checkbox v-model="form.includeNotes" label="Notes" />
      </div>

      <el-button type="primary" :loading="generating" @click="handleGenerate" class="w-full">
        Generate Draft
      </el-button>
    </div>

    <div v-if="result" class="result-area mt-4">
      <el-divider>Analysis Result</el-divider>
      
      <div class="extracted-info mb-4">
        <el-row :gutter="20">
          <el-col :span="12">
            <h4>Key Findings</h4>
            <ul>
              <li v-for="(item, idx) in result.extracted.keyFindings" :key="idx">{{ item }}</li>
            </ul>
          </el-col>
          <el-col :span="12">
            <h4>Next Steps</h4>
            <ul>
              <li v-for="(item, idx) in result.extracted.nextSteps" :key="idx">{{ item }}</li>
            </ul>
          </el-col>
        </el-row>
      </div>

      <div class="draft-editor mb-4">
        <MarkdownEditor v-model="draftContent" @run-click="handleRunClick" />
      </div>

      <div class="actions">
        <el-button type="success" @click="saveAsDraft" :loading="saving">Save as Draft Note</el-button>
        <el-button type="warning" @click="writeToConclusion" :loading="saving">Write to Conclusion</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { generateRunDraft } from '@/api/ai'
import { createNote, upsertConclusion } from '@/api/runNotes'
import type { AiDraftResponse } from '@/api/ai'

const props = defineProps<{
  runId: number
}>()

const emit = defineEmits(['run-click', 'refresh-notes', 'refresh-conclusion'])

const generating = ref(false)
const saving = ref(false)
const result = ref<AiDraftResponse | null>(null)
const draftContent = ref('')

const form = reactive({
  userHint: '',
  includeMetrics: true,
  includeTags: true,
  includeTemplate: true,
  includeNotes: true
})

const handleGenerate = async () => {
  generating.value = true
  try {
    const res = await generateRunDraft({
      runId: props.runId,
      ...form
    })
    result.value = res.data
    draftContent.value = res.data.draftMd
  } catch (error) {
    console.error(error)
  } finally {
    generating.value = false
  }
}

const saveAsDraft = async () => {
  if (!draftContent.value) return
  saving.value = true
  try {
    await createNote(props.runId, {
      type: 'AI_DRAFT',
      title: 'AI Analysis Draft',
      contentMd: draftContent.value
    })
    ElMessage.success('Saved to Notes')
    emit('refresh-notes')
  } catch (error) {
    console.error(error)
  } finally {
    saving.value = false
  }
}

const writeToConclusion = async () => {
  if (!draftContent.value) return
  try {
    await ElMessageBox.confirm('Overwrite existing conclusion?', 'Warning', { type: 'warning' })
    saving.value = true
    await upsertConclusion(props.runId, {
      title: 'AI Analysis Conclusion',
      contentMd: draftContent.value
    })
    ElMessage.success('Written to Conclusion')
    emit('refresh-conclusion')
  } catch {
    // cancel
  } finally {
    saving.value = false
  }
}

const handleRunClick = (id: number) => {
  emit('run-click', id)
}
</script>

<style scoped>
.ai-tab {
  padding: 10px 0;
}
.mb-3 {
  margin-bottom: 15px;
}
.mb-4 {
  margin-bottom: 20px;
}
.mt-4 {
  margin-top: 20px;
}
.w-full {
  width: 100%;
}
.switches {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}
.extracted-info {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
}
.extracted-info h4 {
  margin-top: 0;
  margin-bottom: 10px;
}
.extracted-info ul {
  padding-left: 20px;
  margin: 0;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

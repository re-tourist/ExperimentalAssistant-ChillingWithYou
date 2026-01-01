import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import type { ApiResponse } from '@/api/types'

export interface AiContextProject {
  id: number
  name: string
  description?: string
  ai_profile?: any
}

export interface AiContextDashboard {
  summary: any
  trend: any
  distribution: any
  top_runs: any
}

export interface AiContextRun {
  run_id: number
  name: string
  status: string
  tags: string[]
  metrics: Record<string, number>
  hyperparameters: Record<string, any>
  note?: string
}

export interface AiAnalysisRequest {
  userIntent: string
  contextJson: {
    project?: AiContextProject
    dashboard_snapshot?: AiContextDashboard
    runs?: AiContextRun[]
  }
}

export interface AiAnalysisResponse {
  markdownReport: string
}

export const useAiStore = defineStore('ai', () => {
  // State
  const drawerVisible = ref(false)
  const loading = ref(false)
  
  // Context Data (to be populated by Views)
  const currentProject = ref<AiContextProject | null>(null)
  const dashboardSnapshot = ref<AiContextDashboard | null>(null)
  const availableRuns = ref<AiContextRun[]>([])
  
  // User Selection State (Context Tree)
  const selection = ref({
    project: true,
    dashboard: false,
    runs: [] as number[] // selected run IDs
  })

  // Analysis Result
  const lastAnalysis = ref<string>('')
  const userIntent = ref('')

  // Actions
  const openDrawer = (initialMode: 'dashboard' | 'runs' | 'detail' = 'dashboard') => {
    drawerVisible.value = true
    
    // Set default selections based on mode
    if (initialMode === 'dashboard') {
      selection.value.project = true
      selection.value.dashboard = true
      selection.value.runs = []
    } else if (initialMode === 'runs') {
      selection.value.project = true
      selection.value.dashboard = false
      // Runs selection depends on user manual action, usually empty initially or pre-selected
    } else if (initialMode === 'detail') {
      selection.value.project = true
      selection.value.dashboard = false
      // Single run should be pre-selected (caller should push it to availableRuns and select it)
    }
  }

  const closeDrawer = () => {
    drawerVisible.value = false
  }

  const setProjectContext = (project: AiContextProject) => {
    currentProject.value = project
  }

  const setDashboardContext = (snapshot: AiContextDashboard) => {
    dashboardSnapshot.value = snapshot
  }

  const setAvailableRuns = (runs: AiContextRun[]) => {
    availableRuns.value = runs
  }
  
  const addAvailableRun = (run: AiContextRun) => {
    // Check if exists
    if (!availableRuns.value.some(r => r.run_id === run.run_id)) {
      availableRuns.value.push(run)
    }
  }
  
  const selectRun = (runId: number) => {
    if (!selection.value.runs.includes(runId)) {
      selection.value.runs.push(runId)
    }
  }

  // Computed Context JSON for Preview/Sending
  const finalContextJson = computed(() => {
    const json: any = {}
    
    if (selection.value.project && currentProject.value) {
      json.project = currentProject.value
    }
    
    if (selection.value.dashboard && dashboardSnapshot.value) {
      json.dashboard_snapshot = dashboardSnapshot.value
    }
    
    if (selection.value.runs.length > 0) {
      json.runs = availableRuns.value.filter(r => selection.value.runs.includes(r.run_id))
    }
    
    return json
  })

  // API Call
  const analyze = async () => {
    if (!userIntent.value.trim()) return
    
    loading.value = true
    try {
      const payload: AiAnalysisRequest = {
        userIntent: userIntent.value,
        contextJson: finalContextJson.value
      }
      const res = await request.post<any, ApiResponse<AiAnalysisResponse>>('/ai/analyze', payload)
      lastAnalysis.value = res.data.markdownReport
    } catch (error) {
      console.error(error)
      // Ideally show error in drawer
      lastAnalysis.value = `**Error**: Failed to generate analysis. \n\n${error}`
    } finally {
      loading.value = false
    }
  }

  return {
    drawerVisible,
    loading,
    currentProject,
    dashboardSnapshot,
    availableRuns,
    selection,
    lastAnalysis,
    userIntent,
    finalContextJson,
    openDrawer,
    closeDrawer,
    setProjectContext,
    setDashboardContext,
    setAvailableRuns,
    addAvailableRun,
    selectRun,
    analyze
  }
})

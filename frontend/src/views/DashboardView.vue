<template>
  <div class="dashboard-container">
    <div class="header">
      <h2>Dashboard</h2>
      <div class="filters">
        <el-select
          v-model="filters.projectId"
          placeholder="Project"
          clearable
          style="width: 180px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="p in projects"
            :key="p.id"
            :label="p.name"
            :value="p.id"
          />
        </el-select>

        <el-select
          v-model="filters.metricDefId"
          placeholder="Metric"
          clearable
          style="width: 150px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="m in metricDefs"
            :key="m.id"
            :label="`${m.name} (${m.direction})`"
            :value="m.id"
          />
        </el-select>

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="to"
          start-placeholder="Start Date"
          end-placeholder="End Date"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="handleDateChange"
        />

        <el-select
          v-model="filters.status"
          placeholder="Status"
          clearable
          style="width: 120px"
          @change="handleFilterChange"
        >
          <el-option label="Finished" value="FINISHED" />
          <el-option label="Running" value="RUNNING" />
          <el-option label="Failed" value="FAILED" />
          <el-option label="Created" value="CREATED" />
        </el-select>

        <el-button type="primary" :icon="Refresh" @click="refreshData">Refresh</el-button>
      </div>
    </div>

    <!-- KPI Cards -->
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6">
        <el-card shadow="hover" v-loading="loading.summary">
          <template #header>
            <div class="card-header">
              <span>Total Runs</span>
            </div>
          </template>
          <div class="statistic-value">{{ summary.totalRuns }}</div>
          <div class="statistic-sub">All time</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" v-loading="loading.summary">
          <template #header>
            <div class="card-header">
              <span>Recent Activity</span>
            </div>
          </template>
          <div class="statistic-value">{{ summary.runsLast7Days }}</div>
          <div class="statistic-sub">Runs in last 7 days</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" v-loading="loading.summary">
          <template #header>
            <div class="card-header">
              <span>Success Rate</span>
            </div>
          </template>
          <div class="statistic-value">{{ (summary.successRate * 100).toFixed(1) }}%</div>
          <div class="statistic-sub">Finished / Total</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" v-loading="loading.summary">
          <template #header>
            <div class="card-header">
              <span>Best Metric</span>
            </div>
          </template>
          <div v-if="summary.bestMetric">
            <div class="statistic-value">{{ summary.bestMetric.value }}</div>
            <div class="statistic-sub">
              {{ summary.bestMetric.metricName }} - {{ summary.bestMetric.runName }}
            </div>
          </div>
          <div v-else class="statistic-value">-</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="16">
        <el-card shadow="hover" v-loading="loading.trend">
          <template #header>
            <div class="card-header">
              <span>Trend (Best-of-Day)</span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" v-loading="loading.distribution">
          <template #header>
            <div class="card-header">
              <span>Distribution</span>
              <el-select 
                v-model="filters.by" 
                size="small" 
                style="width: 100px" 
                @change="handleFilterChange"
              >
                <el-option label="Model" value="model" />
                <el-option label="Dataset" value="dataset" />
                <el-option label="Tag" value="tag" />
              </el-select>
            </div>
          </template>
          <div ref="distributionChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Top N Table -->
    <el-card shadow="hover" class="top-n-card" v-loading="loading.top">
      <template #header>
        <div class="card-header">
          <span>Top Runs</span>
        </div>
      </template>
      <el-table :data="topRuns" stripe style="width: 100%" height="300">
        <el-table-column prop="runName" label="Run Name" width="180" />
        <el-table-column prop="value" label="Metric Value" width="120">
          <template #default="{ row }">
            <strong>{{ row.value }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="Model" width="150" />
        <el-table-column prop="datasetName" label="Dataset" width="150" />
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created At" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { 
  getSummary, 
  getTrend, 
  getDistribution, 
  getTopRuns,
  type DashboardFilter,
  type SummaryResponse,
  type TopRun
} from '@/api/dashboard'
import { getProjects, type Project } from '@/api/projects'
import { getMetricDefs, type MetricDef } from '@/api/metrics'
import { dashboardCache } from '@/utils/cache'

// State
const projects = ref<Project[]>([])
const metricDefs = ref<MetricDef[]>([])
const dateRange = ref<[string, string] | null>(null)

const filters = reactive<DashboardFilter>({
  projectId: undefined,
  dateFrom: undefined,
  dateTo: undefined,
  status: undefined,
  metricDefId: undefined,
  by: 'model',
  limit: 10
})

const loading = reactive({
  summary: false,
  trend: false,
  distribution: false,
  top: false
})

const summary = ref<SummaryResponse>({
  totalRuns: 0,
  runsLast7Days: 0,
  successRate: 0
})

const topRuns = ref<TopRun[]>([])

// Chart Refs
const trendChartRef = ref<HTMLElement | null>(null)
const distributionChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let distributionChart: echarts.ECharts | null = null

// Methods
const getStatusType = (status: string) => {
  switch (status) {
    case 'FINISHED': return 'success'
    case 'FAILED': return 'danger'
    case 'RUNNING': return 'primary'
    default: return 'info'
  }
}

const resetDashboardData = () => {
  summary.value = {
    totalRuns: 0,
    runsLast7Days: 0,
    successRate: 0
  }
  topRuns.value = []
  trendChart?.clear()
  distributionChart?.clear()
}

const handleDateChange = (val: [string, string] | null) => {
  if (val) {
    filters.dateFrom = val[0]
    filters.dateTo = val[1]
  } else {
    filters.dateFrom = undefined
    filters.dateTo = undefined
  }
  handleFilterChange()
}

const handleFilterChange = () => {
  fetchDashboardData()
}

const refreshData = () => {
  dashboardCache.clear()
  fetchDashboardData()
}

const fetchOptions = async () => {
  try {
    const [pRes, mRes] = await Promise.all([
      getProjects({ size: 100 }),
      getMetricDefs()
    ])
    projects.value = pRes.data.records
    metricDefs.value = mRes.data

    if (projects.value.length > 0 && filters.projectId == null) {
      filters.projectId = projects.value[0].id
    }
    
    // Set default metric if available
    if (metricDefs.value.length > 0) {
      filters.metricDefId = metricDefs.value[0].id
    }
  } catch (error) {
    console.error('Failed to load options', error)
  }
}

const fetchDashboardData = () => {
  if (!filters.projectId) {
    resetDashboardData()
    return
  }

  fetchSummary()
  fetchDistribution()

  if (filters.metricDefId) {
    fetchTrend()
    fetchTopRuns()
  } else {
    topRuns.value = []
    trendChart?.clear()
  }
}

const fetchSummary = async () => {
  loading.summary = true
  try {
    const cacheKey = `summary:${JSON.stringify(filters)}`
    const cached = dashboardCache.get<SummaryResponse>(cacheKey)
    
    if (cached) {
      summary.value = cached
    } else {
      const res = await getSummary(filters)
      summary.value = res.data
      dashboardCache.set(cacheKey, res.data)
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.summary = false
  }
}

const fetchTrend = async () => {
  loading.trend = true
  try {
    const cacheKey = `trend:${JSON.stringify(filters)}`
    let data = dashboardCache.get<any[]>(cacheKey)
    
    if (!data) {
      const res = await getTrend(filters)
      data = res.data
      dashboardCache.set(cacheKey, data)
    }
    
    updateTrendChart(data)
  } catch (error) {
    console.error(error)
  } finally {
    loading.trend = false
  }
}

const fetchDistribution = async () => {
  loading.distribution = true
  try {
    const cacheKey = `distribution:${JSON.stringify(filters)}`
    let data = dashboardCache.get<any[]>(cacheKey)
    
    if (!data) {
      const res = await getDistribution(filters)
      data = res.data
      dashboardCache.set(cacheKey, data)
    }
    
    updateDistributionChart(data)
  } catch (error) {
    console.error(error)
  } finally {
    loading.distribution = false
  }
}

const fetchTopRuns = async () => {
  loading.top = true
  try {
    const cacheKey = `top:${JSON.stringify(filters)}`
    let data = dashboardCache.get<TopRun[]>(cacheKey)
    
    if (!data) {
      const res = await getTopRuns(filters)
      data = res.data
      dashboardCache.set(cacheKey, data)
    }
    
    topRuns.value = data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.top = false
  }
}

const updateTrendChart = (data: any[]) => {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  
  const dates = data.map(item => item.x)
  const values = data.map(item => item.y)
  
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [{
      data: values,
      type: 'line',
      smooth: true,
      areaStyle: {}
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
  })
}

const updateDistributionChart = (data: any[]) => {
  if (!distributionChartRef.value) return
  if (!distributionChart) {
    distributionChart = echarts.init(distributionChartRef.value)
  }
  
  distributionChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: false, position: 'center' },
      emphasis: {
        label: { show: true, fontSize: 20, fontWeight: 'bold' }
      },
      labelLine: { show: false },
      data: data
    }]
  })
}

const handleResize = () => {
  trendChart?.resize()
  distributionChart?.resize()
}

onMounted(async () => {
  await fetchOptions()
  fetchDashboardData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  distributionChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.kpi-row {
  margin-bottom: 20px;
}
.statistic-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}
.statistic-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
.charts-row {
  margin-bottom: 20px;
}
.chart-container {
  height: 300px;
  width: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

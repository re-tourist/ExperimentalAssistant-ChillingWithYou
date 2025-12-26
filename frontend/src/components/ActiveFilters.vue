<template>
  <div v-if="hasActiveFilters" class="active-filters">
    <span class="label">Filters:</span>
    <transition-group name="filter-list">
      <el-tag
        v-for="filter in activeFilters"
        :key="filter.key"
        closable
        type="info"
        effect="plain"
        class="filter-tag"
        @close="handleRemove(filter.key)"
      >
        {{ filter.label }}: <strong>{{ filter.value }}</strong>
      </el-tag>
    </transition-group>
    <el-button
      v-if="hasActiveFilters"
      type="primary"
      link
      size="small"
      @click="handleClearAll"
    >
      Clear All
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface FilterConfig {
  [key: string]: {
    label: string
    formatter?: (val: any) => string
  }
}

const props = defineProps<{
  filters: Record<string, any>
  config: FilterConfig
}>()

const emit = defineEmits(['update:filters', 'change'])

const activeFilters = computed(() => {
  const list = []
  for (const key in props.filters) {
    const val = props.filters[key]
    const conf = props.config[key]
    
    // Check if value is "active" (not undefined, null, empty string)
    if (val !== undefined && val !== null && val !== '' && conf) {
      list.push({
        key,
        label: conf.label,
        value: conf.formatter ? conf.formatter(val) : String(val)
      })
    }
  }
  return list
})

const hasActiveFilters = computed(() => activeFilters.value.length > 0)

const handleRemove = (key: string) => {
  // Emit update to clear specific filter
  const newFilters = { ...props.filters }
  newFilters[key] = undefined // or default value, but undefined usually works for optional filters
  emit('update:filters', newFilters)
  emit('change')
}

const handleClearAll = () => {
  const newFilters = { ...props.filters }
  for (const key in props.config) {
    newFilters[key] = undefined
  }
  emit('update:filters', newFilters)
  emit('change')
}
</script>

<style scoped>
.active-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
}
.label {
  color: #909399;
  font-weight: 500;
  margin-right: 4px;
}
.filter-tag {
  border-color: #dcdfe6;
  background-color: #fff;
}
.filter-list-enter-active,
.filter-list-leave-active {
  transition: all 0.3s ease;
}
.filter-list-enter-from,
.filter-list-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>

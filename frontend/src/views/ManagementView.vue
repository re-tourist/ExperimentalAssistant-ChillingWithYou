<template>
  <div class="management-view">
    <el-tabs v-model="activeTab" class="demo-tabs">
      <el-tab-pane label="Templates" name="templates">
        <TemplatesTab />
      </el-tab-pane>
      <el-tab-pane label="Tags" name="tags">
        <TagsTab />
      </el-tab-pane>
      <el-tab-pane label="Metric Definitions" name="metrics">
        <MetricDefsTab />
      </el-tab-pane>
      <el-tab-pane label="Domains" name="domains">
        <DomainsTab />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TemplatesTab from './management/TemplatesTab.vue'
import TagsTab from './management/TagsTab.vue'
import MetricDefsTab from './management/MetricDefsTab.vue'
import DomainsTab from './management/DomainsTab.vue'

const activeTab = ref('templates')
</script>

<style scoped>
.management-view {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}
</style>

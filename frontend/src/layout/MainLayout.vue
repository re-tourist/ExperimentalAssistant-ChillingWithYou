<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <!-- ... (existing menu) -->
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        router
        :collapse="false"
      >
        <div class="logo">ExpAssistant</div>
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>Dashboard</span>
        </el-menu-item>
        <el-menu-item index="/projects">
          <el-icon><Folder /></el-icon>
          <span>Projects</span>
        </el-menu-item>
        <el-menu-item index="/runs">
          <el-icon><DataLine /></el-icon>
          <span>Runs</span>
        </el-menu-item>
        <el-menu-item index="/management">
          <el-icon><Setting /></el-icon>
          <span>Management</span>
        </el-menu-item>
        <el-menu-item index="/ai">
          <el-icon><ChatDotSquare /></el-icon>
          <span>AI Chat</span>
        </el-menu-item>
        <el-menu-item index="/about">
          <el-icon><InfoFilled /></el-icon>
          <span>About</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header>
        <div class="header-content">
          <span>Experiment Tracking System</span>
        </div>
      </el-header>
      
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { 
  Odometer, 
  Folder, 
  DataLine, 
  Setting, 
  InfoFilled, 
  ChatDotSquare 
} from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
/* ... (existing styles) */
.layout-container {
  height: 100vh;
}

.el-aside {
  background-color: #304156;
  color: #fff;
}

.el-menu {
  border-right: none;
  background-color: #304156;
}

.el-menu-item {
  color: #bfcbd9;
}

.el-menu-item.is-active {
  color: #409eff;
  background-color: #263445;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  background-color: #2b2f3a;
}

.el-header {
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between; /* Changed to space-between */
  padding: 0 20px;
}

.header-content {
  font-size: 16px;
  color: #333;
}

.el-main {
  padding: 20px;
  background-color: #f0f2f5;
}
</style>

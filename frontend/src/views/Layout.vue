<script setup lang="ts">
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { 
  Menu as MenuIcon, 
  House, 
  Goods, 
  List, 
  DataLine, 
  Setting, 
  Expand, 
  Fold, 
  UserFilled,
  ArrowDown
} from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();
const user = JSON.parse(localStorage.getItem('user') || '{}');

const activeMenu = computed(() => route.path);

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title);
  return matched;
});

const logout = () => {
  localStorage.removeItem('user');
  router.push('/login');
};
</script>

<template>
  <div class="app-wrapper">
    <el-container class="main-container">
      <el-aside width="220px" class="sidebar-container">
        <div class="logo-container">
          <div class="logo-icon">W</div>
          <span class="logo-text">Warehouse OS</span>
        </div>
        
        <el-scrollbar>
          <el-menu
            :default-active="activeMenu"
            class="sidebar-menu"
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409EFF"
            router
            unique-opened
          >
            <el-menu-item index="/dashboard">
              <el-icon><MenuIcon /></el-icon>
              <span>仪表盘</span>
            </el-menu-item>
            
            <el-sub-menu index="/warehouses">
              <template #title>
                <el-icon><House /></el-icon>
                <span>仓库管理</span>
              </template>
              <el-menu-item index="/warehouses">仓库列表</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="/materials">
              <template #title>
                <el-icon><Goods /></el-icon>
                <span>物资管理</span>
              </template>
              <el-menu-item index="/materials">物资列表</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="/inventory">
              <template #title>
                <el-icon><List /></el-icon>
                <span>库存管理</span>
              </template>
              <el-menu-item index="/inventory/inbound">物资入库</el-menu-item>
              <el-menu-item index="/inventory/outbound">物资出库</el-menu-item>
              <el-menu-item index="/inventory/records">出入库记录</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="/reports">
              <template #title>
                <el-icon><DataLine /></el-icon>
                <span>报表统计</span>
              </template>
              <el-menu-item index="/reports/stock">库存报表</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="/settings">
              <template #title>
                <el-icon><Setting /></el-icon>
                <span>系统设置</span>
              </template>
              <el-menu-item index="/settings">基础数据</el-menu-item>
            </el-sub-menu>
          </el-menu>
        </el-scrollbar>
      </el-aside>
      
      <el-container class="content-container">
        <el-header class="navbar">
          <div class="navbar-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <div class="navbar-right">
            <el-dropdown trigger="click">
              <div class="avatar-wrapper">
                <el-avatar :size="30" :icon="UserFilled" class="user-avatar" />
                <span class="username">{{ user.username }}</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item disabled>角色: {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        
        <el-main class="app-main">
          <router-view v-slot="{ Component }">
            <transition name="fade-transform" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.app-wrapper {
  position: relative;
  height: 100vh;
  width: 100%;
}

.main-container {
  height: 100%;
}

.sidebar-container {
  background-color: #304156;
  height: 100%;
  overflow: hidden;
  transition: width 0.28s;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
  z-index: 1001;
}

.logo-container {
  height: 50px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background-color: #2b2f3a;
  color: #fff;
  font-weight: 600;
  overflow: hidden;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: #409EFF;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 20px;
  font-weight: bold;
}

.logo-text {
  font-size: 16px;
  white-space: nowrap;
}

.sidebar-menu {
  border: none;
}

.content-container {
  background-color: #f0f2f5;
  display: flex;
  flex-direction: column;
}

.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.navbar-right {
  display: flex;
  align-items: center;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 8px;
  transition: background 0.3s;
}

.avatar-wrapper:hover {
  background: rgba(0, 0, 0, 0.025);
}

.user-avatar {
  background-color: #409EFF;
}

.username {
  margin-left: 8px;
  font-size: 14px;
  color: #606266;
}

.app-main {
  padding: 20px;
  position: relative;
  overflow-y: auto;
}

/* Custom Scrollbar */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #909399;
  border-radius: 3px;
}
</style>

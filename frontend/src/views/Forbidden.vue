<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = () => {
  router.push('/');
};

const goBack = () => {
  router.back();
};
</script>

<template>
  <div class="forbidden-container">
    <div class="forbidden-content">
      <div class="error-code">403</div>
      <h2 class="error-title">访问被拒绝</h2>
      <p class="error-desc">抱歉，您没有权限访问该页面或执行此操作。</p>
      <p class="error-hint">如需权限，请联系系统管理员。</p>
      <div class="error-actions">
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="goBack">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forbidden-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100%;
  background: #f0f2f5;
}

.forbidden-content {
  text-align: center;
  padding: 60px 40px;
}

.error-code {
  font-size: 120px;
  font-weight: 800;
  color: #e6a23c;
  line-height: 1;
  margin-bottom: 20px;
  text-shadow: 0 4px 12px rgba(230, 162, 60, 0.3);
}

.error-title {
  font-size: 24px;
  color: #303133;
  margin-bottom: 12px;
  font-weight: 600;
}

.error-desc {
  font-size: 16px;
  color: #909399;
  margin-bottom: 8px;
}

.error-hint {
  font-size: 14px;
  color: #c0c4cc;
  margin-bottom: 32px;
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>

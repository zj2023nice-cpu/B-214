<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import axios from '../api/axios';
import { ElMessage } from 'element-plus';
import { User, Lock, ArrowRight } from '@element-plus/icons-vue';

const router = useRouter();
const form = reactive({
  username: '',
  password: ''
});
const loading = ref(false);

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  
  loading.value = true;
  try {
    const res: any = await axios.post('/users/login', form);
    if (res.code === 200) {
      localStorage.setItem('user', JSON.stringify(res.data));
      ElMessage.success('登录成功');
      router.push('/');
    }
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="login-container">
    <div class="login-content">
      <div class="login-left">
        <div class="brand">
          <h1>Warehouse OS</h1>
          <p>智能、高效、可视化的现代仓库管理系统</p>
        </div>
      </div>
      <div class="login-right">
        <el-card class="login-card">
          <div class="login-header">
            <h2>欢迎登录</h2>
            <p>请输入您的账号信息以继续</p>
          </div>
          
          <el-form :model="form" size="large" @keyup.enter="handleLogin">
            <el-form-item>
              <el-input 
                v-model="form.username" 
                placeholder="用户名" 
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item>
              <el-input 
                v-model="form.password" 
                type="password" 
                placeholder="密码" 
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                @click="handleLogin" 
                :loading="loading" 
                style="width: 100%"
                class="login-btn"
              >
                登 录
                <el-icon class="el-icon--right"><ArrowRight /></el-icon>
              </el-button>
            </el-form-item>
          </el-form>
          
          <div class="login-footer">
            <span>测试账号: admin / 123456</span>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100%;
  background: linear-gradient(135deg, #1c2434 0%, #304156 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  top: -10%;
  left: -10%;
  width: 50%;
  height: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
}

.login-container::after {
  content: '';
  position: absolute;
  bottom: -10%;
  right: -10%;
  width: 50%;
  height: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
}

.login-content {
  display: flex;
  width: 900px;
  height: 550px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  z-index: 1;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
  padding: 40px;
  position: relative;
}

.brand {
  text-align: center;
}

.brand h1 {
  font-size: 2.5rem;
  margin-bottom: 1rem;
  font-weight: 800;
  letter-spacing: 2px;
}

.brand p {
  font-size: 1.1rem;
  opacity: 0.9;
  line-height: 1.6;
}

.login-right {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
  background-color: #fff;
}

.login-card {
  width: 100%;
  max-width: 360px;
  box-shadow: none !important;
  border: none !important;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 1.8rem;
  color: #303133;
  margin-bottom: 10px;
}

.login-header p {
  color: #909399;
  font-size: 0.9rem;
}

.login-btn {
  font-size: 16px;
  padding: 20px 0;
  border-radius: 8px;
  letter-spacing: 4px;
  font-weight: bold;
  transition: all 0.3s;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(64, 158, 255, 0.4);
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  color: #c0c4cc;
  font-size: 12px;
}

/* Responsive */
@media (max-width: 960px) {
  .login-content {
    width: 90%;
    height: auto;
    flex-direction: column;
  }
  
  .login-left {
    padding: 30px;
  }
  
  .login-right {
    padding: 30px;
  }
}
</style>

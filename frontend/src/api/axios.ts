import axios from 'axios';
import { ElMessage } from 'element-plus';

const instance = axios.create({
  baseURL: '/api',
  timeout: 5000,
});

instance.interceptors.request.use(
  (config) => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        if (user.id) {
          config.headers['X-User-Id'] = String(user.id);
        }
      } catch {
        // ignore parse errors
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

instance.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    if (error.response?.status === 403) {
      window.location.href = '/403';
      return Promise.reject(error);
    }
    const msg = error.response?.data?.message || 'Network Error';
    ElMessage.error(msg);
    return Promise.reject(error);
  }
);

export default instance;

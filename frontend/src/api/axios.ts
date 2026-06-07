import axios from 'axios';
import { ElMessage } from 'element-plus';

const instance = axios.create({
  baseURL: '/api',
  timeout: 5000,
});

instance.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    const msg = error.response?.data?.message || 'Network Error';
    ElMessage.error(msg);
    return Promise.reject(error);
  }
);

export default instance;

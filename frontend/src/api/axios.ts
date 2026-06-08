import axios from 'axios';
import { ElMessage } from 'element-plus';
import { getAccessToken, getRefreshToken, setTokens, clearAuth } from './auth';

const instance = axios.create({
  baseURL: '/api',
  timeout: 5000,
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: any) => void;
}> = [];

function processQueue(error: any, token: string | null = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token!);
    }
  });
  failedQueue = [];
}

instance.interceptors.request.use(
  (config) => {
    const token = getAccessToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
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
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = getRefreshToken();

      if (!refreshToken) {
        clearAuth();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers['Authorization'] = `Bearer ${token}`;
              resolve(instance(originalRequest));
            },
            reject,
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const res: any = await axios.post('/api/users/refresh', {
          refreshToken,
        });

        if (res.data?.code === 200 && res.data?.data) {
          const { accessToken, refreshToken: newRefreshToken, user } = res.data.data;
          setTokens(accessToken, newRefreshToken, user);
          processQueue(null, accessToken);
          originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
          return instance(originalRequest);
        } else {
          clearAuth();
          processQueue(error, null);
          window.location.href = '/login';
          return Promise.reject(error);
        }
      } catch (refreshError) {
        clearAuth();
        processQueue(refreshError, null);
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

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

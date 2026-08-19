import Axios, { InternalAxiosRequestConfig } from 'axios';

import { useNotifications } from '@/components/ui/notifications';
import { env } from '@/config/env';
import { paths } from '@/config/paths';
import { useTokenStore } from './token-store';
import { refreshToken } from './refresh-token';

// Mở rộng kiểu dữ liệu AxiosRequestConfig để hỗ trợ cờ _retry
interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

function authRequestInterceptor(config: InternalAxiosRequestConfig) {
  if (config.headers) {
    config.headers.Accept = 'application/json';
    const token = useTokenStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }

  config.withCredentials = true;
  return config;
}

export const api = Axios.create({
  baseURL: env.API_URL,
});

api.interceptors.request.use(authRequestInterceptor);
api.interceptors.response.use(
  (response) => {
    // Backend bọc mọi response trong CommonResponse{ successOrNot, statusCode, data }
    return response.data.data;
  },
  async (error) => {
    const originalRequest = error.config as CustomAxiosRequestConfig;

    // Các endpoint auth không thực hiện retry refresh để tránh vòng lặp
    const isAuthEndpoint =
      originalRequest?.url?.includes('/auth/login') ||
      originalRequest?.url?.includes('/auth/refresh') ||
      originalRequest?.url?.includes('/auth/logout');

    // 2. Xử lý khi gặp lỗi 401 và request chưa được retry
    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isAuthEndpoint
    ) {
      originalRequest._retry = true;

      try {
        // Gọi hàm refresh token (đã có cơ chế deduplication in-flight)
        const result = await refreshToken();

        if (result) {
          // Cập nhật lại Header Authorization cho request gốc và gọi lại
          originalRequest.headers.Authorization = `Bearer ${result.accessToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        // Refresh thất bại -> Clear token store và chuyển hướng đăng nhập
        useTokenStore.getState().clearToken();

        const searchParams = new URLSearchParams(window.location.search);
        const redirectTo =
          searchParams.get('redirectTo') || window.location.pathname;
        window.location.href = paths.auth.login.getHref(redirectTo);

        return Promise.reject(refreshError);
      }
    }

    const message = error.response?.data?.message || error.message;
    useNotifications.getState().addNotification({
      type: 'error',
      title: 'Error',
      message,
    });

    if (error.response?.status === 401 && !originalRequest?._retry) {
      useTokenStore.getState().clearToken();
      const searchParams = new URLSearchParams(window.location.search);
      const redirectTo =
        searchParams.get('redirectTo') || window.location.pathname;
      window.location.href = paths.auth.login.getHref(redirectTo);
    }

    return Promise.reject(error);
  },
);

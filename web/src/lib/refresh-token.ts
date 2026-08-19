import axios from 'axios';

import { env } from '@/config/env';
import { User } from '@/types/api';

import { useTokenStore } from './token-store';

// 1. Axios instance riêng biệt — KHÔNG dính interceptor của api-client
const refreshApi = axios.create({
  baseURL: env.API_URL,
  withCredentials: true, // Bắt buộc để trình duyệt tự gửi HttpOnly Refresh Cookie
});

type RefreshResult = { accessToken: string; user: User };

// Biến giữ Promise của request refresh đang chạy (In-flight Promise)
let refreshPromise: Promise<RefreshResult | null> | null = null;

export const refreshToken = async (): Promise<RefreshResult | null> => {
  // 2. Coalescing / Deduplication: Nếu đang có request refresh in-flight, dùng lại Promise đó
  if (refreshPromise) {
    return refreshPromise;
  }

  // 3. Khởi tạo Promise refresh mới
  refreshPromise = (async () => {
    try {
      // refreshApi không đi qua interceptor của api-client nên phải tự bóc CommonResponse
      const response = await refreshApi.post<{
        data: { accessToken: string; user: User };
      }>('/auth/refresh');
      const { accessToken, user } = response.data.data;

      // Cập nhật Access Token mới vào Zustand store
      useTokenStore.getState().setAccessToken(accessToken);

      return { accessToken, user };
    } catch (error) {
      // Refresh thất bại (401/Cookie hết hạn) -> Xóa token trong Zustand store
      useTokenStore.getState().clearToken();
      throw error;
    } finally {
      // 4. Giải phóng biến sau khi request kết thúc (kể cả thành công hay thất bại)
      refreshPromise = null;
    }
  })();

  return refreshPromise;
};
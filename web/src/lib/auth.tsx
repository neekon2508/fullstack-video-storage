import { configureAuth } from 'react-query-auth';
import { Navigate, useLocation } from 'react-router';
import { z } from 'zod';

import { paths } from '@/config/paths';
import { AuthResponse, User } from '@/types/api';

import { api } from './api-client';
import { refreshToken } from './refresh-token';
import { useTokenStore } from './token-store';


const getUser = async (): Promise<User | null> => {
  try {
    // /auth/refresh đã trả về user kèm access token mới, không cần gọi thêm /auth/me
    const result = await refreshToken();
    return result?.user ?? null;
  } catch {
    // Trả về null thay vì throw error để AuthLoader hiểu là người dùng chưa đăng nhập
    return null;
  }
};


const logout = async  (): Promise<void> => {
  try {
    await api.post('/auth/logout');
  } finally {
    // Luôn đảm bảo clear token ở client ngay cả khi request gặp lỗi
    useTokenStore.getState().clearToken();
  }
};

export const loginInputSchema = z.object({
  username: z.string().min(1, 'Required'),
  password: z.string().min(5, 'Required'),
});

export type LoginInput = z.infer<typeof loginInputSchema>;

const loginWithUsernameAndPassword = (
  data: LoginInput,
): Promise<AuthResponse> => {
  return api.post('/auth/login', data);
};

export const registerInputSchema = z
  .object({
    email: z.string().min(1, 'Required'),
    firstName: z.string().min(1, 'Required'),
    lastName: z.string().min(1, 'Required'),
    password: z.string().min(5, 'Required'),
  })
  .and(
    z
      .object({
        teamId: z.string().min(1, 'Required'),
        teamName: z.null().default(null),
      })
      .or(
        z.object({
          teamName: z.string().min(1, 'Required'),
          teamId: z.null().default(null),
        }),
      ),
  );

export type RegisterInput = z.infer<typeof registerInputSchema>;

const registerWithEmailAndPassword = (
  data: RegisterInput,
): Promise<AuthResponse> => {
  return api.post('/auth/register', data);
};

const authConfig = {
  userFn: getUser,
  loginFn: async (data: LoginInput) => {
    const response = await loginWithUsernameAndPassword(data);
    // Lưu accessToken vào Zustand store trước khi trả về user object
    useTokenStore.getState().setAccessToken(response.accessToken);
    return response.user;
  },
  registerFn: async (data: RegisterInput) => {
    const response = await registerWithEmailAndPassword(data);
    return response.user;
  },
  logoutFn: logout,
};

export const { useUser, useLogin, useLogout, useRegister, AuthLoader } =
  configureAuth(authConfig);

export const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const user = useUser();
  const location = useLocation();

  if (!user.data) {
    return (
      <Navigate to={paths.auth.login.getHref(location.pathname)} replace />
    );
  }

  return children;
};

import { create } from 'zustand';

type TokenStore = {
  accessToken: string | null;
  setAccessToken: (token: string | null) => void;
  clearToken: () => void;
};

export const useTokenStore = create<TokenStore>((set) => ({
  accessToken: null,
  setAccessToken: (accessToken) => set({ accessToken }),
  clearToken: () => set({ accessToken: null }),
}));

import axios from "axios";

const BASE = import.meta.env.VITE_API_BASE || "/api";

export const api = axios.create({
  baseURL: BASE,
  timeout: 10000
});

// 清除本地登录态并跳转登录页（hash 路由，带防循环）
export function clearAuthAndRedirect() {
  localStorage.removeItem("tf_token");
  localStorage.removeItem("tf_userId");
  localStorage.removeItem("tf_username");
  const current = window.location.hash.replace(/^#/, "") || "/";
  if (!current.startsWith("/login") && !current.startsWith("/register") && current !== "/") {
    window.location.hash = "#/login" + "?redirect=" + encodeURIComponent(current);
  } else if (current !== "/login") {
    window.location.hash = "#/login";
  }
}

// 每次请求自动携带 JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("tf_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 401（未认证）/ 403（令牌失效或无权）统一跳转登录
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status;
    if (status === 401 || status === 403) {
      clearAuthAndRedirect();
    }
    return Promise.reject(err);
  }
);

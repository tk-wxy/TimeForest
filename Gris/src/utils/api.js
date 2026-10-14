import axios from "axios";

const BASE = import.meta.env.VITE_API_BASE || "/api";

export const api = axios.create({
  baseURL: BASE,
  timeout: 10000
});

// 每次请求自动携带 JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("tf_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 401 时跳转登录
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem("tf_token");
      localStorage.removeItem("tf_userId");
      localStorage.removeItem("tf_username");
      window.location.href = "/#/login";
    }
    return Promise.reject(err);
  }
);

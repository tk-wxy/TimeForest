import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { api } from "../utils/api.js";

export const useAuthStore = defineStore("auth", () => {
  const token = ref(localStorage.getItem("tf_token") || "");
  const userId = ref(Number(localStorage.getItem("tf_userId")) || null);
  const username = ref(localStorage.getItem("tf_username") || "");

  const isLoggedIn = computed(() => !!token.value);

  function setAuth(data) {
    token.value = data.token;
    userId.value = data.userId;
    username.value = data.username;
    localStorage.setItem("tf_token", data.token);
    localStorage.setItem("tf_userId", String(data.userId));
    localStorage.setItem("tf_username", data.username);
  }

  function clearAuth() {
    token.value = "";
    userId.value = null;
    username.value = "";
    localStorage.removeItem("tf_token");
    localStorage.removeItem("tf_userId");
    localStorage.removeItem("tf_username");
  }

  async function login(username, password) {
    const res = await api.post("/auth/login", { username, password });
    setAuth(res.data);
    return res.data;
  }

  async function register(username, email, password) {
    const res = await api.post("/auth/register", { username, email, password });
    setAuth(res.data);
    return res.data;
  }

  function logout() {
    clearAuth();
  }

  return { token, userId, username, isLoggedIn, login, register, logout };
});

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth.js";

const router = useRouter();
const auth = useAuthStore();

const username = ref("");
const email = ref("");
const password = ref("");
const confirm = ref("");
const error = ref("");
const loading = ref(false);

async function handleRegister() {
  error.value = "";
  if (!username.value.trim() || !password.value) {
    error.value = "请填写用户名和密码";
    return;
  }
  if (password.value !== confirm.value) {
    error.value = "两次输入的密码不一致";
    return;
  }
  if (password.value.length < 6) {
    error.value = "密码长度不能少于 6 位";
    return;
  }
  loading.value = true;
  try {
    await auth.register(username.value.trim(), email.value.trim(), password.value);
    router.push("/chat");
  } catch (e) {
    error.value = e?.response?.data?.error || "注册失败，请稍后重试";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="forest-bg" aria-hidden="true"></div>
    <div class="forest-veil" aria-hidden="true"></div>
    <div class="grain" aria-hidden="true"></div>

    <div class="auth-card">
      <div class="auth-brand">
        <span class="brand-leaf"><i class="fa-solid fa-leaf"></i></span>
        <span class="brand-name">TimeForest</span>
      </div>
      <h2 class="auth-title">种下你的树</h2>
      <p class="auth-sub">注册，开始你的时光之旅</p>

      <form class="auth-form" @submit.prevent="handleRegister">
        <div class="field">
          <label class="field-label">用户名 <span class="required">*</span></label>
          <input
            v-model="username"
            type="text"
            class="field-input"
            placeholder="设置你的用户名"
            autocomplete="username"
            autofocus
          />
        </div>
        <div class="field">
          <label class="field-label">邮箱（可选）</label>
          <input
            v-model="email"
            type="email"
            class="field-input"
            placeholder="your@email.com"
            autocomplete="email"
          />
        </div>
        <div class="field">
          <label class="field-label">密码 <span class="required">*</span></label>
          <input
            v-model="password"
            type="password"
            class="field-input"
            placeholder="至少 6 位"
            autocomplete="new-password"
          />
        </div>
        <div class="field">
          <label class="field-label">确认密码 <span class="required">*</span></label>
          <input
            v-model="confirm"
            type="password"
            class="field-input"
            placeholder="再次输入密码"
            autocomplete="new-password"
          />
        </div>

        <p v-if="error" class="auth-error"><i class="fa-solid fa-circle-exclamation"></i> {{ error }}</p>

        <button type="submit" class="auth-btn" :disabled="loading">
          <span v-if="loading"><i class="fa-solid fa-spinner fa-spin"></i> 注册中…</span>
          <span v-else>进入林间</span>
        </button>
      </form>

      <p class="auth-switch">
        已有账号？
        <RouterLink to="/login" class="auth-link">立即登录</RouterLink>
      </p>
      <p class="auth-switch">
        <RouterLink to="/" class="auth-link">← 返回首页</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.forest-bg {
  position: absolute;
  inset: 0;
  background-image: url("/Forest.png");
  background-size: cover;
  background-position: center 30%;
  transform: scale(1.03);
}

.forest-veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(8, 6, 3, 0.55) 0%,
    rgba(8, 6, 3, 0.45) 100%
  );
}

.grain {
  position: fixed;
  inset: 0;
  z-index: 200;
  pointer-events: none;
  opacity: 0.045;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='320' height='320'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.88' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='320' height='320' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 320px 320px;
}

.auth-card {
  position: relative;
  z-index: 2;
  width: min(420px, 90vw);
  padding: 2.4rem 2.2rem 2rem;
  border-radius: 16px;
  background: rgba(16, 12, 7, 0.72);
  border: 1px solid rgba(220, 195, 150, 0.18);
  backdrop-filter: blur(24px) saturate(1.3);
  -webkit-backdrop-filter: blur(24px) saturate(1.3);
  box-shadow: 0 8px 48px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.07);
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.2rem;
}

.brand-leaf { font-size: 0.9rem; color: rgba(180, 160, 110, 0.8); }

.brand-name {
  font-family: "Cinzel", "Cormorant Garamond", Georgia, serif;
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: rgba(230, 215, 180, 0.9);
}

.auth-title {
  margin: 0;
  font-family: "Cormorant Garamond", Georgia, serif;
  font-size: 1.6rem;
  font-weight: 700;
  color: rgba(245, 235, 210, 0.96);
}

.auth-sub {
  margin: 0 0 0.4rem;
  font-size: 0.82rem;
  color: rgba(200, 185, 155, 0.65);
  letter-spacing: 0.04em;
}

.auth-form { display: flex; flex-direction: column; gap: 0.85rem; }

.field { display: flex; flex-direction: column; gap: 0.36rem; }

.field-label {
  font-size: 0.78rem;
  letter-spacing: 0.06em;
  color: rgba(210, 195, 160, 0.75);
}

.required { color: rgba(240, 140, 100, 0.8); font-size: 0.7rem; }

.field-input {
  padding: 0.62rem 0.9rem;
  border-radius: 8px;
  border: 1px solid rgba(220, 195, 150, 0.2);
  background: rgba(255, 250, 235, 0.07);
  color: rgba(245, 235, 210, 0.95);
  font-size: 0.9rem;
  outline: none;
  transition: border-color 180ms ease, background 180ms ease;
}

.field-input::placeholder { color: rgba(200, 185, 155, 0.4); }

.field-input:focus {
  border-color: rgba(200, 175, 120, 0.55);
  background: rgba(255, 250, 235, 0.11);
}

.auth-error {
  margin: 0;
  font-size: 0.8rem;
  color: rgba(240, 160, 120, 0.9);
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.auth-btn {
  margin-top: 0.3rem;
  padding: 0.72rem;
  border-radius: 8px;
  border: 1px solid rgba(200, 175, 120, 0.35);
  background: rgba(180, 150, 90, 0.22);
  color: rgba(245, 235, 210, 0.95);
  font-size: 0.92rem;
  font-family: "Cormorant Garamond", Georgia, serif;
  font-weight: 600;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: background 180ms ease, border-color 180ms ease, transform 140ms ease;
}

.auth-btn:hover:not(:disabled) {
  background: rgba(200, 165, 95, 0.35);
  border-color: rgba(210, 185, 130, 0.55);
  transform: translateY(-1px);
}

.auth-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.auth-switch {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  color: rgba(200, 185, 155, 0.6);
  text-align: center;
}

.auth-link {
  color: rgba(210, 185, 130, 0.85);
  text-decoration: none;
  transition: color 180ms ease;
}

.auth-link:hover { color: rgba(230, 210, 160, 1); }
</style>

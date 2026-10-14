<script setup>
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth.js";
const router = useRouter();
const auth = useAuthStore();

function goToChat() {
  if (auth.isLoggedIn) {
    router.push("/chat");
  } else {
    router.push("/login");
  }
}
</script>

<template>
  <div class="home-page">
    <!-- Forest background -->
    <div class="forest-bg" aria-hidden="true"></div>
    <!-- Warm vignette overlay for depth -->
    <div class="forest-veil" aria-hidden="true"></div>
    <!-- Film grain texture -->
    <div class="grain" aria-hidden="true"></div>

    <!-- Centered logo block -->
    <div class="home-center" role="banner">
      <div class="logo-wrap">
        <h1 class="home-logo">TimeForest</h1>
        <div class="logo-deco" aria-hidden="true">
          <span class="deco-line"></span>
          <span class="deco-leaf"><i class="fa-solid fa-leaf"></i></span>
          <span class="deco-line"></span>
        </div>
        <p class="home-tagline">时光之林 · 与 AI 共生</p>
      </div>
    </div>

    <!-- Bottom bar -->
    <div class="home-bottom">
      <!-- Left: three nav buttons -->
      <nav class="home-nav" aria-label="主导航">
        <button class="nav-btn" @click="goToChat">
          <i class="fa-regular fa-comment-dots nav-icon"></i>
          <span class="nav-label">对话</span>
        </button>
        <button class="nav-btn">
          <i class="fa-solid fa-seedling nav-icon"></i>
          <span class="nav-label">林间笔记</span>
        </button>
        <button class="nav-btn">
          <i class="fa-solid fa-feather nav-icon"></i>
          <span class="nav-label">Blog</span>
        </button>
      </nav>

      <!-- Right: profile entry -->
      <button class="user-btn" aria-label="个人主页 / 登录"
        @click="auth.isLoggedIn ? router.push('/profile') : router.push('/login')">
        <span class="user-avatar" aria-hidden="true">
          <i class="fa-regular fa-user"></i>
        </span>
        <span class="user-label">{{ auth.isLoggedIn ? auth.username : '登录' }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
/* ── Page shell ── */
.home-page {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* ── Forest background ── */
.forest-bg {
  position: absolute;
  inset: 0;
  background-image: url("/Forest.png");
  background-size: cover;
  background-position: center 30%;
  background-repeat: no-repeat;
  transform: scale(1.03);
  transition: transform 0.6s ease;
}

/* Multi-layer veil: dark bottom for readability + warm center glow */
.forest-veil {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 70% 55% at 50% 44%, rgba(20, 14, 8, 0.08) 0%, transparent 100%),
    linear-gradient(to bottom,
      rgba(10, 8, 5, 0.28) 0%,
      rgba(10, 8, 5, 0.04) 38%,
      rgba(10, 8, 5, 0.04) 62%,
      rgba(8, 6, 3, 0.62) 100%
    );
}

/* Film grain */
.grain {
  position: fixed;
  inset: 0;
  z-index: 200;
  pointer-events: none;
  opacity: 0.045;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='320' height='320'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.88' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='320' height='320' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 320px 320px;
}

/* ── Center logo ── */
.home-center {
  position: relative;
  z-index: 2;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  pointer-events: none;
}

.logo-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.6rem;
  text-align: center;
}

.home-logo {
  margin: 0;
  font-family: "Cinzel", "Cormorant Garamond", Georgia, serif;
  font-size: clamp(1.7rem, 2.6vw, 2.3rem);
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(255, 248, 234, 0.96);
  text-shadow:
    0 2px 32px rgba(0, 0, 0, 0.45),
    0 0 80px rgba(200, 160, 100, 0.18);
  line-height: 1;
}

.logo-deco {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  width: min(280px, 60vw);
  margin-top: 0.2rem;
}

.deco-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(220, 195, 150, 0.5) 40%,
    rgba(220, 195, 150, 0.5) 60%,
    transparent
  );
}

.deco-leaf {
  font-size: 0.7rem;
  color: rgba(200, 175, 120, 0.72);
  flex-shrink: 0;
}

.home-tagline {
  margin: 0;
  font-family: "Cormorant Garamond", Georgia, "Noto Serif SC", serif;
  font-size: clamp(0.78rem, 1.6vw, 1rem);
  font-weight: 500;
  letter-spacing: 0.28em;
  color: rgba(220, 200, 165, 0.72);
  text-shadow: 0 1px 12px rgba(0, 0, 0, 0.4);
}

/* ── Bottom bar ── */
.home-bottom {
  position: relative;
  z-index: 2;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 2rem 2.2rem;
  box-sizing: border-box;
  flex-shrink: 0;
}

/* ── Navigation buttons (left) ── */
.home-nav {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.55rem 1rem 0.55rem 0.8rem;
  border-radius: 40px;
  border: 1px solid rgba(255, 245, 225, 0.2);
  background: rgba(20, 14, 8, 0.38);
  backdrop-filter: blur(18px) saturate(1.2);
  -webkit-backdrop-filter: blur(18px) saturate(1.2);
  box-shadow:
    0 2px 16px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background 180ms ease,
    transform 140ms ease,
    box-shadow 180ms ease;
}

.nav-btn:hover {
  border-color: rgba(220, 195, 150, 0.42);
  background: rgba(30, 20, 10, 0.55);
  box-shadow:
    0 6px 28px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.14);
  transform: translateY(-2px);
}

.nav-btn:active {
  transform: translateY(0);
}

.nav-icon {
  font-size: 0.82rem;
  color: rgba(220, 195, 145, 0.78);
  flex-shrink: 0;
}

.nav-label {
  font-family: "Cormorant Garamond", "Cinzel", Georgia, serif;
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: rgba(240, 225, 195, 0.9);
  white-space: nowrap;
}

/* ── User / Login button (right) ── */
.user-btn {
  display: flex;
  align-items: center;
  gap: 0.48rem;
  padding: 0.5rem 1rem 0.5rem 0.72rem;
  border-radius: 40px;
  border: 1px solid rgba(255, 245, 225, 0.2);
  background: rgba(20, 14, 8, 0.38);
  backdrop-filter: blur(18px) saturate(1.2);
  -webkit-backdrop-filter: blur(18px) saturate(1.2);
  box-shadow:
    0 2px 16px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition:
    border-color 180ms ease,
    background 180ms ease,
    transform 140ms ease;
}

.user-btn:hover {
  border-color: rgba(220, 195, 150, 0.42);
  background: rgba(30, 20, 10, 0.55);
  transform: translateY(-2px);
}

.user-btn:active {
  transform: translateY(0);
}

.user-avatar {
  width: 1.6rem;
  height: 1.6rem;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 0.78rem;
  color: rgba(220, 195, 145, 0.78);
  background: rgba(255, 245, 210, 0.1);
  border: 1px solid rgba(220, 195, 150, 0.25);
  flex-shrink: 0;
}

.user-label {
  font-family: "Cormorant Garamond", Georgia, serif;
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: rgba(240, 225, 195, 0.85);
  white-space: nowrap;
}

/* ── Responsive ── */
@media (max-width: 600px) {
  .home-bottom {
    padding: 0 1.2rem 1.8rem;
  }

  .home-nav {
    gap: 0.45rem;
  }

  .nav-btn {
    padding: 0.5rem 0.75rem 0.5rem 0.65rem;
    gap: 0.38rem;
  }

  .nav-label {
    font-size: 0.76rem;
  }

  .home-logo {
    letter-spacing: 0.15em;
  }
}
</style>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, computed } from "vue";
import { useRouter } from "vue-router";
import SessionSidebar from "../components/SessionSidebar.vue";
import ChatMessageList from "../components/ChatMessageList.vue";
import ChatComposer from "../components/ChatComposer.vue";
import { useAuthStore } from "../stores/auth.js";
import { api, clearAuthAndRedirect } from "../utils/api.js";
import { currentTheme, setTheme } from "../utils/theme.js";

const router = useRouter();
const auth = useAuthStore();
const BASE = import.meta.env.VITE_API_BASE || "/api";

// ── State ─────────────────────────────────────────────────
const chatContainer = ref(null);
const composerRef = ref(null);
const sidebarRef = ref(null);
const inputText = ref("");
const sending = ref(false);
const loading = ref(false);
const streamingText = ref("");
const activeSessionId = ref("");
const activeRole = ref(null);
const sidebarOpen = ref(false);
const menuOpen = ref(false);
const windowWidth = ref(window.innerWidth);
const messages = ref([]);
const isAutoScrolling = ref(true);
const showScrollDownBtn = ref(false);

// Toast
const toast = ref({ show: false, text: "", timeoutId: 0 });

// ── Responsive ────────────────────────────────────────────
const isDesktop = computed(() => windowWidth.value >= 768);

function onResize() {
  windowWidth.value = window.innerWidth;
  if (isDesktop.value && !sidebarOpen.value) sidebarOpen.value = true;
}

onMounted(() => {
  window.addEventListener("resize", onResize);
  sidebarOpen.value = isDesktop.value;
  composerRef.value?.focus();
});
onUnmounted(() => {
  window.removeEventListener("resize", onResize);
  document.removeEventListener("click", closeMenu);
  clearTimeout(toast.value.timeoutId);
});

// ── Menu ──────────────────────────────────────────────────
function toggleMenu(e) { e.stopPropagation(); menuOpen.value = !menuOpen.value; }
function closeMenu() { menuOpen.value = false; }
onMounted(() => document.addEventListener("click", closeMenu));

async function deleteCurrentSession() {
  menuOpen.value = false;
  if (!activeSessionId.value) return;
  if (!confirm("删除此对话？聊天记录将一并清除。")) return;
  await api.delete(`/sessions/${activeSessionId.value}`);
  activeSessionId.value = "";
  activeRole.value = null;
  messages.value = [];
  await sidebarRef.value?.fetchSessions();
}

// ── Toast ─────────────────────────────────────────────────
function showToast(text) {
  clearTimeout(toast.value.timeoutId);
  toast.value = { show: true, text, timeoutId: setTimeout(() => { toast.value.show = false; }, 4500) };
}

// ── Helpers ───────────────────────────────────────────────
function scrollToBottom(force = false) {
  if (!force && !isAutoScrolling.value) return;
  nextTick(() => {
    if (!chatContainer.value) return;
    /* force=true 用于历史加载、新建会话等一次性跳底 → instant 避免视觉抖动
     * force=false 用于流式输出中跟随 → smooth 保持流畅 */
    chatContainer.value.scrollTo({ top: chatContainer.value.scrollHeight, behavior: force ? 'instant' : 'smooth' });
  });
}
function isAtBottom() {
  if (!chatContainer.value) return true;
  const el = chatContainer.value;
  return el.scrollTop + el.clientHeight >= el.scrollHeight - 16;
}
function onChatScroll() {
  if (!chatContainer.value) return;
  if (isAtBottom()) {
    isAutoScrolling.value = true;
    showScrollDownBtn.value = false;
  } else {
    isAutoScrolling.value = false;
    showScrollDownBtn.value = true;
  }
}
function scrollToBottomNow() {
  isAutoScrolling.value = true;
  showScrollDownBtn.value = false;
  nextTick(() => {
    if (!chatContainer.value) return;
    chatContainer.value.scrollTo({ top: chatContainer.value.scrollHeight, behavior: 'smooth' });
  });
}
function parseSseEvent(block) {
  const lines = block.split("\n").filter(l => l.startsWith("data:")).map(l => l.slice(5).replace(/^ /, ""));
  if (lines.length) return lines.join("\n");
  const raw = block.trim();
  return raw === "[DONE]" ? "[DONE]" : raw;
}
function renderMarkdown(text) {
  return globalThis.marked ? globalThis.marked.parse(text, { gfm: true, breaks: true }) : text;
}

// ── Sessions ──────────────────────────────────────────────
async function loadHistory(session) {
  messages.value = []; streamingText.value = "";
  activeRole.value = session.role || null;
  try {
    const res = await api.get(`/sessions/${session.id}/messages`);
    for (const m of res.data) {
      messages.value.push({ id: crypto.randomUUID(), role: m.role === "user" ? "user" : "ai",
        text: m.role === "assistant" ? renderMarkdown(m.content) : m.content, isHtml: m.role === "assistant" });
    }
    if (!messages.value.length) pushWelcome();
  } catch { pushWelcome(); }
  scrollToBottom(true);
}

function pushWelcome() {
  const name = activeRole.value?.name || "TimeForest";
  messages.value.push({ id: crypto.randomUUID(), role: "ai", isHtml: true,
    text: renderMarkdown(`你好，我是 **${name}**。${activeRole.value?.description ? '\n\n> ' + activeRole.value.description : ''}\n\n有什么我可以帮你的？`) });
}

async function onSelectSession(session) {
  activeSessionId.value = session.id;
  if (!isDesktop.value) sidebarOpen.value = false;
  await loadHistory(session);
  nextTick(() => composerRef.value?.focus());
}

async function onCreateSession(session) {
  activeSessionId.value = session.id;
  activeRole.value = session.role || null;
  messages.value = [];
  if (!isDesktop.value) sidebarOpen.value = false;
  pushWelcome(); scrollToBottom(true);
  nextTick(() => composerRef.value?.focus());
}

// ── Send ──────────────────────────────────────────────────
async function sendMessage() {
  const content = inputText.value.trim();
  if (!content || sending.value || !activeSessionId.value) return;
  sending.value = true;
  isAutoScrolling.value = true;
  showScrollDownBtn.value = false;
  messages.value.push({ id: crypto.randomUUID(), role: "user", text: content, isHtml: false });
  inputText.value = ""; scrollToBottom(true);
  loading.value = true; streamingText.value = "";
  try {
    const response = await fetch(`${BASE}/ask`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Bearer ${auth.token}` },
      body: JSON.stringify({ question: content, sessionId: activeSessionId.value })
    });
    if (!response.ok) throw new Error(`HTTP_${response.status}`);
    if (!response.body) throw new Error("ReadableStream 不可用");
    loading.value = false;
    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let full = "", pending = "", closed = false;
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      pending += decoder.decode(value, { stream: true });
      const events = pending.replace(/\r\n/g, "\n").split("\n\n");
      pending = events.pop() || "";
      for (const b of events) {
        const p = parseSseEvent(b);
        if (!p) continue;
        if (p === "[DONE]") { closed = true; break; }
        full += p;
      }
      streamingText.value = full; scrollToBottom();
      if (closed) break;
    }
    const fp = parseSseEvent((pending + decoder.decode()).replace(/\r\n/g, "\n"));
    if (fp && fp !== "[DONE]") full += fp;
    messages.value.push({ id: crypto.randomUUID(), role: "ai", isHtml: true,
      text: full.trim() ? renderMarkdown(full) : renderMarkdown("（AI 未返回内容）") });
    streamingText.value = "";
    if (closed) { try { reader.releaseLock(); } catch {} }
    await sidebarRef.value?.silentRefresh();
    await nextTick();
    if (globalThis.hljs) globalThis.hljs.highlightAll();
    isAutoScrolling.value = false;
    showScrollDownBtn.value = true;
  } catch (err) {
    loading.value = false; streamingText.value = "";
    const m = err?.message ?? "";
    if (m.includes("401") || m.includes("403")) {
      clearAuthAndRedirect();
      return;
    }
    showToast(m.includes("500") ? "后端服务出错，请查看日志" : "网络异常，请确认后端已启动");
  } finally {
    sending.value = false;
    nextTick(() => composerRef.value?.focus());
  }
}
</script>

<template>
  <div class="chat-root">
    <!-- Mobile backdrop -->
    <Transition name="fade">
      <div v-if="!isDesktop && sidebarOpen" class="backdrop" @click="sidebarOpen = false"></div>
    </Transition>

    <!-- Sidebar wrapper -->
    <div class="sidebar-wrap" :class="{ open: sidebarOpen }">
      <SessionSidebar
        ref="sidebarRef"
        :active-session-id="activeSessionId"
        @select="onSelectSession"
        @create="onCreateSession"
      />
    </div>

    <!-- Main -->
    <div class="chat-stage">
      <div class="grain-overlay" aria-hidden="true"></div>

      <!-- Header -->
      <header class="chat-header">
        <button class="icon-btn" :title="sidebarOpen ? '收起侧边栏' : '展开侧边栏'" @click="sidebarOpen = !sidebarOpen">
          <i class="fa-solid fa-table-columns"></i>
        </button>

        <div class="header-identity">
          <Transition name="badge" mode="out-in">
            <div v-if="activeRole" class="role-badge" :key="activeRole.id">
              <span class="role-emoji">{{ activeRole.avatarEmoji || '🌿' }}</span>
              <span class="role-name">{{ activeRole.name }}</span>
            </div>
            <span v-else class="header-hint" key="hint">选择角色开始对话</span>
          </Transition>
        </div>

        <div class="menu-wrap" @click.stop>
          <button class="icon-btn" :class="{ active: menuOpen }" title="菜单" @click="toggleMenu">
            <i class="fa-solid fa-bars"></i>
          </button>
          <Transition name="drop">
            <div v-if="menuOpen" class="dropdown" role="menu">
              <div class="dd-section-label">外观</div>
              <div class="theme-row">
                <button class="theme-btn" :class="{ on: currentTheme === 'system' }" @click="setTheme('system')">
                  <i class="fa-solid fa-circle-half-stroke"></i> 系统
                </button>
                <button class="theme-btn" :class="{ on: currentTheme === 'dark' }" @click="setTheme('dark')">
                  <i class="fa-solid fa-moon"></i> 深色
                </button>
                <button class="theme-btn" :class="{ on: currentTheme === 'light' }" @click="setTheme('light')">
                  <i class="fa-solid fa-sun"></i> 浅色
                </button>
              </div>
              <div class="dd-divider"></div>
              <button class="dd-item" @click="router.push('/'); menuOpen = false">
                <i class="fa-solid fa-house"></i> 首页
              </button>
              <button class="dd-item" @click="router.push('/sites'); menuOpen = false">
                <i class="fa-solid fa-globe"></i> 网站
              </button>
              <button class="dd-item" @click="router.push('/blogs'); menuOpen = false">
                <i class="fa-solid fa-feather"></i> 博客
              </button>
              <button class="dd-item" @click="router.push('/profile'); menuOpen = false">
                <i class="fa-regular fa-circle-user"></i> 个人主页
              </button>
              <button class="dd-item dd-danger" :disabled="!activeSessionId" @click="deleteCurrentSession">
                <i class="fa-solid fa-trash-can"></i> 删除此对话
              </button>
            </div>
          </Transition>
        </div>
      </header>

      <div class="chat-layout">
        <!-- Messages -->
        <main class="messages-area" ref="chatContainer" aria-live="polite" @scroll.passive="onChatScroll">
          <ChatMessageList
            :messages="messages"
            :loading="loading"
            :streaming-text="streamingText"
            :active-role="activeRole"
            :active-session-id="activeSessionId"
            :show-scroll-down-btn="showScrollDownBtn"
            @scroll-to-bottom="scrollToBottomNow"
          />
        </main>

        <!-- Composer -->
        <ChatComposer
          ref="composerRef"
          v-model="inputText"
          :active-session-id="activeSessionId"
          :sending="sending"
          @send="sendMessage"
        />
      </div>

      <!-- Toast -->
      <Transition name="toast">
        <div v-if="toast.show" class="toast-bar">{{ toast.text }}</div>
      </Transition>
    </div>
  </div>
</template>

<style scoped>
/* ════════════════════ Root ════════════════════ */
.chat-root {
  display: flex; width: 100%; height: 100%; overflow: hidden;
  font-family: "Inter","PingFang SC","Noto Sans SC",sans-serif;
  background: var(--tf-bg);
  position: relative;
}

/* ── Backdrop ── */
.backdrop {
  position: fixed; inset: 0; z-index: 400;
  background: rgba(0,0,0,0.45);
  backdrop-filter: blur(2px);
}

/* ════════════════════ Sidebar wrapper ════════════════════ */
.sidebar-wrap {
  flex-shrink: 0; overflow: hidden;
  transition: width 0.3s cubic-bezier(0.4,0,0.2,1);
}
@media (min-width: 768px) {
  .sidebar-wrap       { width: 0; }
  .sidebar-wrap.open  { width: 260px; }
}
@media (max-width: 767px) {
  .sidebar-wrap {
    position: fixed; left: 0; top: 0; bottom: 0;
    width: 260px; z-index: 500;
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4,0,0.2,1);
  }
  .sidebar-wrap.open {
    transform: translateX(0);
    box-shadow: 4px 0 32px rgba(0,0,0,0.4);
  }
}

/* ════════════════════ Stage ════════════════════ */
.chat-stage {
  flex: 1; min-width: 0; display: flex; flex-direction: column;
  overflow: hidden; position: relative;
  background: var(--tf-bg);
}

.grain-overlay {
  position: fixed; inset: 0; z-index: 100; pointer-events: none;
  opacity: 0.025;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='256' height='256'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='256' height='256' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 256px;
}

/* ── Header ── */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-shrink: 0;
  width: 100%;
  padding: 0.65rem 0.75rem;
  box-sizing: border-box;
  border-bottom: 1px solid var(--tf-border);
  background: var(--tf-bg);
}

/* ── Layout ── */
.chat-layout {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  width: 100%;
  max-width: 860px;
  margin: 0 auto;
  padding: 0 1.25rem 1.2rem 0.75rem;
  box-sizing: border-box;
}

@media (max-width: 480px) {
  .chat-header { padding: 0.65rem 0.75rem; }
  .chat-layout { padding: 0 0.75rem 0.65rem 0.65rem; }
}

/* ── Icon button ── */
.icon-btn {
  width: 36px; height: 36px; border-radius: 10px;
  border: 1px solid var(--tf-border);
  background: var(--tf-surface);
  color: var(--tf-text2);
  cursor: pointer; display: grid; place-items: center; font-size: 0.82rem;
  flex-shrink: 0;
  transition: background 150ms, color 150ms, border-color 150ms;
}
.icon-btn:hover, .icon-btn.active {
  background: var(--tf-accent-dim);
  color: var(--tf-accent-text, var(--tf-accent));
  border-color: var(--tf-border-a);
}

/* ── Menu ── */
.menu-wrap { position: relative; flex-shrink: 0; }

.dropdown {
  position: absolute; top: calc(100% + 8px); right: 0;
  min-width: 196px; background: var(--tf-dropdown);
  border: 1px solid var(--tf-border);
  border-radius: 14px; padding: 0.45rem;
  box-shadow: 0 16px 44px var(--tf-shadow);
  z-index: 600;
}

.dd-section-label {
  font-size: 0.67rem; font-weight: 700; letter-spacing: 0.08em;
  text-transform: uppercase; color: var(--tf-text3);
  padding: 0.25rem 0.75rem 0.4rem;
}

.theme-row { display: flex; gap: 0.35rem; padding: 0 0.35rem 0.35rem; }

.theme-btn {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 0.22rem;
  padding: 0.45rem 0.3rem;
  border: 1.5px solid var(--tf-border);
  border-radius: 8px; background: var(--tf-surface); cursor: pointer;
  font-size: 0.68rem; color: var(--tf-text2);
  transition: all 150ms;
}
.theme-btn i { font-size: 0.85rem; }
.theme-btn.on {
  border-color: var(--tf-accent);
  background: var(--tf-accent-dim);
  color: var(--tf-accent);
}
.theme-btn:hover:not(.on) { background: var(--tf-surface2); }

.dd-item {
  display: flex; align-items: center; gap: 0.7rem; width: 100%;
  padding: 0.52rem 0.75rem; border: none; background: none;
  color: var(--tf-text); font-size: 0.82rem; border-radius: 7px;
  cursor: pointer; text-align: left;
  transition: background 140ms, color 140ms;
}
.dd-item i { width: 14px; text-align: center; color: var(--tf-text3); }
.dd-item:hover { background: var(--tf-accent-dim); }
.dd-item:disabled { opacity: 0.35; cursor: not-allowed; }
.dd-item:disabled:hover { background: none; }
.dd-danger { color: var(--tf-text-danger) !important; }
.dd-danger:hover { background: rgba(200,60,40,0.1) !important; }
.dd-divider { height: 1px; background: var(--tf-border); margin: 0.3rem 0; }

/* ── Role badge ── */
.header-identity {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  padding: 0 0.5rem;
}

.role-badge {
  display: flex; align-items: center; gap: 0.45rem;
  padding: 0.3rem 0.8rem 0.3rem 0.55rem;
  background: var(--tf-accent-dim);
  border: 1px solid var(--tf-border-a);
  border-radius: 22px;
  max-width: 100%;
}
.role-emoji { font-size: 0.95rem; line-height: 1; flex-shrink: 0; }
.role-name {
  font-size: 0.8rem; font-weight: 600; letter-spacing: 0.02em;
  color: var(--tf-accent-text, var(--tf-accent));
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.header-hint { font-size: 0.78rem; color: var(--tf-text3); white-space: nowrap; }

/* ── Messages area (container only) ── */
.messages-area {
  flex: 1; overflow-y: auto;
  padding: 1.4rem 0 0.8rem;
  display: flex; flex-direction: column; gap: 1.1rem;
}

/* ── Toast ── */
.toast-bar {
  position: fixed;
  bottom: 5.5rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 900;
  max-width: 420px;
  padding: 0.55rem 1.1rem;
  border-radius: 10px;
  font-size: 0.84rem;
  background: var(--tf-dropdown, #2d2d2d);
  border: 1px solid var(--tf-border);
  color: var(--tf-text);
  box-shadow: 0 8px 28px var(--tf-shadow);
  text-align: center;
}

.toast-enter-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.toast-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(10px); }

/* ── Transitions ── */
.drop-enter-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.drop-leave-active { transition: opacity 0.1s ease, transform 0.1s ease; }
.drop-enter-from, .drop-leave-to { opacity: 0; transform: translateY(-6px) scale(0.97); }
.badge-enter-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.badge-leave-active { transition: opacity 0.15s ease; }
.badge-enter-from { opacity: 0; transform: translateX(-6px); }
.badge-leave-to { opacity: 0; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, computed } from "vue";
import { useRouter } from "vue-router";
import SessionSidebar from "../components/SessionSidebar.vue";
import { useAuthStore } from "../stores/auth.js";
import { api } from "../utils/api.js";
import { currentTheme, setTheme } from "../utils/theme.js";

const router = useRouter();
const auth = useAuthStore();
const BASE = import.meta.env.VITE_API_BASE || "/api";

// ── State ─────────────────────────────────────────────────
const chatContainer = ref(null);
const messageInput = ref(null);
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

// ── Responsive ────────────────────────────────────────────
const isDesktop = computed(() => windowWidth.value >= 768);

function onResize() {
  windowWidth.value = window.innerWidth;
  if (isDesktop.value && !sidebarOpen.value) sidebarOpen.value = true;
}

onMounted(() => {
  window.addEventListener("resize", onResize);
  sidebarOpen.value = isDesktop.value;
  resizeInput();
  messageInput.value?.focus();
});
onUnmounted(() => {
  window.removeEventListener("resize", onResize);
  document.removeEventListener("click", closeMenu);
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

// ── Helpers ───────────────────────────────────────────────
function scrollToBottom() {
  nextTick(() => { if (chatContainer.value) chatContainer.value.scrollTop = chatContainer.value.scrollHeight; });
}
function resizeInput() {
  if (!messageInput.value) return;
  messageInput.value.style.height = "auto";
  messageInput.value.style.height = `${Math.min(messageInput.value.scrollHeight, 200)}px`;
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
  scrollToBottom();
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
  nextTick(() => messageInput.value?.focus());
}

async function onCreateSession(session) {
  activeSessionId.value = session.id;
  activeRole.value = session.role || null;
  messages.value = [];
  if (!isDesktop.value) sidebarOpen.value = false;
  pushWelcome(); scrollToBottom();
  nextTick(() => messageInput.value?.focus());
}

// ── Send ──────────────────────────────────────────────────
async function sendMessage() {
  const content = inputText.value.trim();
  if (!content || sending.value || !activeSessionId.value) return;
  sending.value = true;
  messages.value.push({ id: crypto.randomUUID(), role: "user", text: content, isHtml: false });
  inputText.value = ""; resizeInput(); scrollToBottom();
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
    scrollToBottom();
  } catch (err) {
    loading.value = false; streamingText.value = "";
    const m = err?.message ?? "";
    const tip = m.includes("401") ? "登录已过期，请重新登录"
      : m.includes("403") ? "无权访问此会话"
      : m.includes("500") ? "后端服务出错，请查看日志"
      : "网络异常，请确认后端已启动";
    messages.value.push({ id: crypto.randomUUID(), role: "ai", text: tip, isHtml: false });
  } finally {
    sending.value = false;
    nextTick(() => { messageInput.value?.focus(); scrollToBottom(); });
  }
}
</script>

<template>
  <div class="chat-root">
    <!-- Mobile backdrop -->
    <Transition name="fade">
      <div v-if="!isDesktop && sidebarOpen" class="backdrop" @click="sidebarOpen = false"></div>
    </Transition>

    <!-- Sidebar wrapper (controls width/position) -->
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

      <div class="chat-layout">
        <!-- ── Header ── -->
        <header class="chat-header">
          <div class="menu-wrap" @click.stop>
            <button class="icon-btn" :class="{ active: menuOpen }" @click="toggleMenu">
              <i class="fa-solid fa-bars"></i>
            </button>
            <Transition name="drop">
              <div v-if="menuOpen" class="dropdown" role="menu">
                <!-- Theme picker -->
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
                <!-- Sidebar -->
                <button class="dd-item" @click="sidebarOpen = !sidebarOpen; menuOpen = false">
                  <i class="fa-solid fa-table-columns"></i>
                  {{ sidebarOpen ? '隐藏侧边栏' : '显示侧边栏' }}
                </button>
                <div class="dd-divider"></div>
                <!-- Navigation -->
                <button class="dd-item" @click="router.push('/'); menuOpen = false">
                  <i class="fa-solid fa-house"></i> 返回首页
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

          <div class="header-identity">
            <span class="site-logo">
              <i class="fa-solid fa-leaf logo-leaf"></i>
              <span class="logo-text">TimeForest</span>
            </span>
            <Transition name="badge">
              <div v-if="activeRole" class="role-badge">
                <span>{{ activeRole.avatarEmoji || '🌿' }}</span>
                <span class="role-name">{{ activeRole.name }}</span>
              </div>
            </Transition>
          </div>

          <div class="header-right">
            <span v-if="!activeSessionId" class="header-hint">选择角色开始对话</span>
          </div>
        </header>

        <!-- ── Messages ── -->
        <main class="messages-area" ref="chatContainer" aria-live="polite">
          <div v-if="!messages.length && !loading && !activeSessionId" class="empty-state">
            <div class="empty-icon"><i class="fa-solid fa-leaf"></i></div>
            <p class="empty-title">时光之林</p>
            <p class="empty-sub">点击左上角 <kbd>☰</kbd> 菜单，打开侧边栏选择角色</p>
          </div>

          <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'">
            <div v-if="msg.role === 'ai'" class="ava ava-ai">{{ activeRole?.avatarEmoji || '🌿' }}</div>
            <div class="bubble" :class="msg.role === 'user' ? 'b-user' : 'b-ai'">
              <div v-if="msg.isHtml" class="md-body" v-html="msg.text"></div>
              <span v-else>{{ msg.text }}</span>
            </div>
            <div v-if="msg.role === 'user'" class="ava ava-user">{{ auth.username?.[0]?.toUpperCase() || 'U' }}</div>
          </div>

          <div v-if="loading" class="msg-row msg-ai">
            <div class="ava ava-ai">{{ activeRole?.avatarEmoji || '🌿' }}</div>
            <div class="bubble b-ai thinking"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
          </div>

          <div v-if="streamingText" class="msg-row msg-ai">
            <div class="ava ava-ai">{{ activeRole?.avatarEmoji || '🌿' }}</div>
            <div class="bubble b-ai">
              <div class="md-body" v-html="renderMarkdown(streamingText)"></div>
              <span class="cursor">▍</span>
            </div>
          </div>
        </main>

        <!-- ── Composer ── -->
        <div class="composer-wrap">
          <div class="composer" :class="{ disabled: !activeSessionId }">
            <textarea ref="messageInput" v-model="inputText" rows="1" class="c-input"
              :placeholder="activeSessionId ? '写下你的问题… (Enter 发送，Shift+Enter 换行)' : '请先从侧边栏选择角色开始对话'"
              :disabled="sending || !activeSessionId"
              @input="resizeInput" @keydown.enter.exact.prevent="sendMessage"></textarea>
            <button class="c-send" :class="{ ready: inputText.trim() && !sending && activeSessionId }"
              :disabled="sending || !activeSessionId || !inputText.trim()" @click="sendMessage">
              <i class="fa-solid fa-paper-plane"></i>
            </button>
          </div>
          <p class="c-tip">AI 可能犯错，请独立判断重要信息</p>
        </div>
      </div>
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

/* Desktop: collapse/expand via width */
@media (min-width: 768px) {
  .sidebar-wrap       { width: 0; }
  .sidebar-wrap.open  { width: 260px; }
}

/* Mobile: fixed overlay via transform */
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

.chat-layout {
  position: relative; z-index: 1; display: flex; flex-direction: column;
  height: 100%; width: 100%; max-width: 860px; margin: 0 auto;
  padding: 0 1.2rem; box-sizing: border-box;
}
@media (max-width: 480px) { .chat-layout { padding: 0 0.8rem; } }

/* ════════════════════ Header ════════════════════ */
.chat-header {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.85rem 0 0.65rem;
  border-bottom: 1px solid var(--tf-border);
  flex-shrink: 0;
}

/* ── Menu ── */
.menu-wrap { position: relative; flex-shrink: 0; }

.icon-btn {
  width: 34px; height: 34px; border-radius: 9px;
  border: 1px solid var(--tf-border);
  background: var(--tf-surface);
  color: var(--tf-text2);
  cursor: pointer; display: grid; place-items: center; font-size: 0.85rem;
  transition: background 150ms, color 150ms, border-color 150ms;
}
.icon-btn:hover, .icon-btn.active {
  background: var(--tf-accent-dim);
  color: var(--tf-accent-text, var(--tf-accent));
  border-color: var(--tf-border-a);
}

.dropdown {
  position: absolute; top: calc(100% + 8px); left: 0;
  min-width: 192px; background: var(--tf-dropdown);
  border: 1px solid var(--tf-border);
  border-radius: 12px; padding: 0.45rem;
  box-shadow: 0 12px 36px var(--tf-shadow);
  z-index: 600;
}

.dd-section-label {
  font-size: 0.67rem; font-weight: 700; letter-spacing: 0.08em;
  text-transform: uppercase; color: var(--tf-text3);
  padding: 0.25rem 0.75rem 0.4rem;
}

.theme-row {
  display: flex; gap: 0.35rem; padding: 0 0.35rem 0.35rem;
}

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

/* ── Logo ── */
.header-identity { display: flex; align-items: center; gap: 0.7rem; flex: 1; min-width: 0; }

.site-logo {
  display: flex; align-items: center; gap: 0.4rem;
  font-family: "Cinzel","Cormorant Garamond",serif;
  font-size: 0.92rem; font-weight: 600; letter-spacing: 0.1em;
  color: var(--tf-text); white-space: nowrap;
}
.logo-leaf { color: var(--tf-accent); font-size: 0.78rem; }

@media (max-width: 380px) { .logo-text { display: none; } }

.role-badge {
  display: flex; align-items: center; gap: 0.38rem;
  padding: 0.22rem 0.6rem 0.22rem 0.42rem;
  background: var(--tf-accent-dim);
  border: 1px solid var(--tf-border-a);
  border-radius: 20px;
}
.role-name { font-size: 0.75rem; font-weight: 600; color: var(--tf-accent-text, var(--tf-accent)); }

.header-right { margin-left: auto; }
.header-hint { font-size: 0.72rem; color: var(--tf-text3); white-space: nowrap; }
@media (max-width: 480px) { .header-hint { display: none; } }

/* ════════════════════ Messages ════════════════════ */
.messages-area {
  flex: 1; overflow-y: auto;
  padding: 1.4rem 0 0.8rem;
  display: flex; flex-direction: column; gap: 1.1rem;
}

.empty-state {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 0.9rem; padding: 2rem; text-align: center;
}
.empty-icon {
  width: 60px; height: 60px; border-radius: 50%;
  background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a);
  display: grid; place-items: center; font-size: 1.5rem; color: var(--tf-accent);
  opacity: 0.55;
}
.empty-title { margin: 0; font-family: "Cinzel",serif; font-size: 1.2rem; font-weight: 600; letter-spacing: 0.12em; color: var(--tf-text2); }
.empty-sub { margin: 0; font-size: 0.82rem; color: var(--tf-text3); max-width: 300px; }
kbd { font-family: inherit; padding: 1px 5px; border: 1px solid var(--tf-border); border-radius: 4px; background: var(--tf-surface); font-size: 0.85em; }

/* ── Message rows ── */
.msg-row { display: flex; align-items: flex-start; gap: 0.6rem; padding: 0 0.1rem; animation: appear 0.2s ease-out; }
@keyframes appear { from { opacity:0; transform:translateY(5px); } to { opacity:1; transform:translateY(0); } }
.msg-user { flex-direction: row-reverse; }

/* ── Avatars ── */
.ava {
  width: 32px; height: 32px; border-radius: 50%;
  display: grid; place-items: center;
  font-size: 0.9rem; flex-shrink: 0; margin-top: 4px; line-height: 1;
}
.ava-ai { background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a); }
.ava-user {
  background: var(--tf-bubble-user, #1c4e12);
  border: 1px solid var(--tf-border-a);
  font-family: "Cinzel",serif; font-size: 0.78rem; font-weight: 700;
  color: var(--tf-user-text, #fff);
}

/* ── Bubbles ── */
.bubble {
  max-width: min(76%, 720px); border-radius: 16px;
  word-break: break-word; line-height: 1.72;
}
@media (max-width: 480px) { .bubble { max-width: calc(100% - 44px); } }

.b-ai {
  background: var(--tf-bubble-ai);
  border: 1px solid var(--tf-border);
  border-top-left-radius: 4px;
  padding: 0.9rem 1.1rem;
  color: var(--tf-text);
  backdrop-filter: blur(6px);
  box-shadow: 0 1px 12px var(--tf-shadow);
}

.b-user {
  background: var(--tf-bubble-user);
  border: 1px solid var(--tf-border-a);
  border-top-right-radius: 4px;
  padding: 0.75rem 1rem;
  color: var(--tf-user-text);
  box-shadow: 0 1px 10px var(--tf-shadow);
}

/* Thinking */
.thinking { padding: 0.8rem 1rem; display: flex; align-items: center; gap: 6px; }
.dot { width: 7px; height: 7px; border-radius: 50%; background: var(--tf-accent); opacity: 0.5; animation: bounce 1.3s ease-in-out infinite; }
.dot:nth-child(2) { animation-delay: 0.18s; }
.dot:nth-child(3) { animation-delay: 0.36s; }
@keyframes bounce { 0%,60%,100% { transform:translateY(0); opacity:0.5; } 30% { transform:translateY(-5px); opacity:1; } }

.cursor { display: inline-block; color: var(--tf-accent); animation: blink 0.9s step-end infinite; font-size: 0.88em; margin-left: 1px; }
@keyframes blink { 0%,100% { opacity:1; } 50% { opacity:0; } }

/* ════════════════════ Markdown ════════════════════ */
:deep(.md-body) { font-size: 0.9rem; line-height: 1.8; color: inherit; }
:deep(.md-body p) { margin: 0 0 0.65em; }
:deep(.md-body p:last-child) { margin-bottom: 0; }
:deep(.md-body h1),:deep(.md-body h2),:deep(.md-body h3) {
  font-family: "Cormorant Garamond",Georgia,serif; font-weight: 700;
  color: var(--tf-text); margin: 0.9em 0 0.4em; line-height: 1.3;
}
:deep(.md-body h1) { font-size: 1.25em; }
:deep(.md-body h2) { font-size: 1.1em; }
:deep(.md-body h3) { font-size: 1em; }
:deep(.md-body code) {
  background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a);
  padding: 1px 5px; border-radius: 4px;
  font-family: "Fira Code",monospace; font-size: 0.82em; color: var(--tf-accent);
}
:deep(.md-body pre) {
  background: var(--tf-bg3, #1a2314); border: 1px solid var(--tf-border);
  border-radius: 10px; padding: 0.9rem 1rem; overflow-x: auto; margin: 0.6em 0;
}
:deep(.md-body pre code) { background: none; border: none; padding: 0; color: var(--tf-text); font-size: 0.85em; }
:deep(.md-body ul),:deep(.md-body ol) { padding-left: 1.4em; margin: 0.4em 0; }
:deep(.md-body li) { margin: 0.28em 0; }
:deep(.md-body blockquote) {
  border-left: 3px solid var(--tf-border-a);
  margin: 0.6em 0; padding: 0.35em 0.85em;
  color: var(--tf-text2); background: var(--tf-accent-dim); border-radius: 0 6px 6px 0;
}
:deep(.md-body table) { width: 100%; border-collapse: collapse; margin: 0.6em 0; font-size: 0.86em; }
:deep(.md-body th) { background: var(--tf-accent-dim); padding: 0.45em 0.8em; text-align: left; font-weight: 600; border-bottom: 2px solid var(--tf-border-a); }
:deep(.md-body td) { padding: 0.4em 0.8em; border-bottom: 1px solid var(--tf-border); }
:deep(.md-body a) { color: var(--tf-accent); text-decoration: underline; text-underline-offset: 2px; }
:deep(.md-body strong) { color: var(--tf-text); font-weight: 700; }

/* ════════════════════ Composer ════════════════════ */
.composer-wrap { flex-shrink: 0; padding: 0.6rem 0 0.85rem; }

.composer {
  display: flex; align-items: flex-end; gap: 0.6rem;
  background: var(--tf-surface);
  border: 1.5px solid var(--tf-border);
  border-radius: 14px; padding: 0.65rem 0.6rem 0.65rem 1rem;
  transition: border-color 200ms, box-shadow 200ms;
}
.composer:focus-within {
  border-color: var(--tf-border-a);
  box-shadow: 0 0 0 3px var(--tf-accent-dim), 0 4px 20px var(--tf-shadow);
}
.composer.disabled { opacity: 0.42; }

.c-input {
  flex: 1; background: none; border: none; outline: none; resize: none;
  font-size: 0.92rem; line-height: 1.65;
  color: var(--tf-text); min-height: 1.65rem; max-height: 200px;
  overflow-y: auto; scrollbar-width: thin; font-family: inherit;
}
.c-input::placeholder { color: var(--tf-placeholder); }

.c-send {
  width: 38px; height: 38px; border-radius: 10px; flex-shrink: 0;
  border: 1.5px solid var(--tf-border); background: var(--tf-surface);
  color: var(--tf-text3); font-size: 0.82rem; cursor: pointer;
  display: grid; place-items: center; transition: all 150ms ease;
}
.c-send.ready {
  border-color: var(--tf-accent); background: var(--tf-accent-dim); color: var(--tf-accent);
  box-shadow: 0 2px 10px var(--tf-accent-dim);
}
.c-send.ready:hover { background: var(--tf-accent); color: #fff; transform: scale(1.05); }
.c-send:disabled { cursor: not-allowed; }

.c-tip { margin: 0.4rem 0 0; text-align: center; font-size: 0.7rem; color: var(--tf-text3); }

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

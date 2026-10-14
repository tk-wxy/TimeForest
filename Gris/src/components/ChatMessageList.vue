<script setup>
import { computed } from "vue";

const props = defineProps({
  messages: { type: Array, required: true },
  loading: { type: Boolean, default: false },
  streamingText: { type: String, default: "" },
  activeRole: { type: Object, default: null },
  activeSessionId: { type: String, default: "" },
  showScrollDownBtn: { type: Boolean, default: false }
});

const emit = defineEmits(["scroll-to-bottom"]);

function renderMarkdown(text) {
  return globalThis.marked ? globalThis.marked.parse(text, { gfm: true, breaks: true }) : text;
}
</script>

<template>
  <div v-if="!messages.length && !loading && !activeSessionId" class="empty-state">
    <div class="empty-icon"><i class="fa-solid fa-leaf"></i></div>
    <p class="empty-title">时光之林</p>
    <p class="empty-sub">点击左上角 <i class="fa-solid fa-table-columns"></i> 展开侧边栏，选择一个角色开始对话</p>
  </div>

  <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'">
    <div v-if="msg.role === 'ai'" class="ava ava-ai">{{ activeRole?.avatarEmoji || '🌿' }}</div>
    <div class="bubble" :class="msg.role === 'user' ? 'b-user' : 'b-ai'">
      <div v-if="msg.isHtml" class="md-body" v-html="msg.text"></div>
      <span v-else>{{ msg.text }}</span>
    </div>
    <div v-if="msg.role === 'user'" class="ava ava-user">U</div>
  </div>

  <div v-if="loading" class="msg-row msg-ai">
    <div class="ava ava-ai">{{ activeRole?.avatarEmoji || '🌿' }}</div>
    <div class="bubble b-ai thinking">
      <span class="dot"></span><span class="dot"></span><span class="dot"></span>
    </div>
  </div>

  <div v-if="streamingText" class="msg-row msg-ai">
    <div class="ava ava-ai">{{ activeRole?.avatarEmoji || '🌿' }}</div>
    <div class="bubble b-ai">
      <div class="md-body" v-html="renderMarkdown(streamingText)"></div>
      <span class="cursor">▍</span>
    </div>
  </div>

  <Transition name="scroll-btn">
    <button v-if="showScrollDownBtn" class="scroll-bottom-btn" @click="emit('scroll-to-bottom')">
      <i class="fa-solid fa-chevron-down"></i>
    </button>
  </Transition>
</template>

<style scoped>
/* ════════════════════ Messages ════════════════════ */
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

.msg-row { display: flex; align-items: flex-start; gap: 0.7rem; padding: 0 0.1rem; animation: appear 0.22s ease-out; }
@keyframes appear { from { opacity:0; transform:translateY(6px); } to { opacity:1; transform:translateY(0); } }
.msg-user { flex-direction: row-reverse; }

.ava {
  width: 34px; height: 34px; border-radius: 11px;
  display: grid; place-items: center;
  font-size: 0.95rem; flex-shrink: 0; margin-top: 2px; line-height: 1;
}
.ava-ai { background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a); }
.ava-user {
  background: var(--tf-bubble-user, #294e40);
  border: 1px solid var(--tf-border-a);
  font-family: "Inter",sans-serif; font-size: 0.8rem; font-weight: 700;
  color: var(--tf-user-text, #fff);
}

.bubble {
  max-width: min(78%, 680px); border-radius: 16px;
  word-break: break-word; line-height: 1.72;
  font-size: 0.925rem;
}
@media (max-width: 480px) { .bubble { max-width: calc(100% - 46px); } }

.b-ai {
  background: var(--tf-bubble-ai);
  border: 1px solid var(--tf-border);
  border-top-left-radius: 5px;
  padding: 0.85rem 1.05rem;
  color: var(--tf-text);
}

.b-user {
  background: var(--tf-bubble-user);
  border-top-right-radius: 5px;
  padding: 0.78rem 1.05rem;
  color: var(--tf-user-text);
  box-shadow: 0 2px 14px var(--tf-shadow);
}

.thinking { padding: 0.85rem 1.05rem; display: flex; align-items: center; gap: 6px; }
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

/* ── 返回底部浮动按钮 ── */
.scroll-btn-enter-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.scroll-btn-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.scroll-btn-enter-from, .scroll-btn-leave-to { opacity: 0; transform: translateY(8px) scale(0.95); }

.scroll-bottom-btn {
  position: sticky; bottom: 0.6rem; left: 50%; transform: translateX(-50%);
  width: 36px; height: 36px; border-radius: 50%;
  border: 1px solid var(--tf-border-a);
  background: var(--tf-surface);
  color: var(--tf-accent-text, var(--tf-accent));
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.82rem;
  box-shadow: 0 4px 18px var(--tf-shadow);
  z-index: 100;
  transition: background 150ms, box-shadow 150ms;
  flex-shrink: 0;
  margin-top: -0.4rem;
}
.scroll-bottom-btn:hover {
  background: var(--tf-accent-dim);
  box-shadow: 0 6px 22px var(--tf-shadow);
}
</style>

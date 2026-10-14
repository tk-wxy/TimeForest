<script setup>
import { ref } from "vue";
import { useRolesStore } from "../stores/roles.js";

const rolesStore = useRolesStore();
const emit = defineEmits(["created", "close"]);

const EMOJIS = [
  "🌿","🌲","🌸","🦋","🦁","🐉","💎","⚡","🎭","📚",
  "🎨","🔮","🌊","🔥","❄️","🌙","☀️","🎵","💡","🤖",
  "🦊","🐺","🦅","🌺","🍀","🎯","🧭","🏔️","🌌","🎪",
];

const selectedEmoji = ref("🤖");
const name = ref("");
const introText = ref("");
const error = ref("");
const loading = ref(false);

async function submit() {
  error.value = "";
  if (!name.value.trim()) { error.value = "请输入角色名"; return; }
  if (!introText.value.trim()) { error.value = "请输入角色介绍文本"; return; }
  loading.value = true;
  try {
    const role = await rolesStore.createRole(name.value.trim(), introText.value.trim(), selectedEmoji.value);
    emit("created", role);
  } catch (e) {
    error.value = e?.response?.data?.error || "创建失败，请稍后重试";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="mask" @click.self="emit('close')">
      <div class="panel" role="dialog" aria-modal="true">
        <!-- Header -->
        <div class="panel-header">
          <span class="panel-title">
            <i class="fa-solid fa-wand-magic-sparkles"></i>
            新建角色
          </span>
          <button class="close-btn" @click="emit('close')">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </div>

        <form class="form" @submit.prevent="submit">
          <!-- Emoji picker -->
          <div class="field">
            <label class="label">选择头像</label>
            <div class="emoji-grid">
              <button
                v-for="e in EMOJIS"
                :key="e"
                type="button"
                class="emoji-btn"
                :class="{ selected: selectedEmoji === e }"
                @click="selectedEmoji = e"
              >{{ e }}</button>
            </div>
          </div>

          <!-- Preview + Name -->
          <div class="name-row">
            <div class="name-preview">{{ selectedEmoji }}</div>
            <div class="field name-field">
              <label class="label">角色名 <span class="req">*</span></label>
              <input
                v-model="name"
                type="text"
                class="input"
                placeholder="例：我的技术顾问"
                maxlength="30"
                autofocus
              />
            </div>
          </div>

          <!-- Intro text -->
          <div class="field">
            <label class="label">
              角色介绍 / 知识文本
              <span class="req">*</span>
            </label>
            <p class="field-hint">
              描述这个角色的身份、性格、专业领域或具体知识内容。<br>
              这段文字会同时成为 AI 的人格设定和专属知识库。
            </p>
            <textarea
              v-model="introText"
              class="textarea"
              rows="7"
              placeholder="例：你是一位专注于 Java 后端开发的技术顾问，精通 Spring Boot、数据库优化和微服务架构。你善于用类比解释复杂概念，回答问题时总是先给出结论再解释原因……"
            ></textarea>
            <div class="char-count">{{ introText.length }} 字</div>
          </div>

          <p v-if="error" class="error-msg">
            <i class="fa-solid fa-circle-exclamation"></i> {{ error }}
          </p>

          <div class="actions">
            <button type="button" class="btn-cancel" @click="emit('close')">取消</button>
            <button type="submit" class="btn-create" :disabled="loading">
              <span v-if="loading"><i class="fa-solid fa-spinner fa-spin"></i> 创建中…</span>
              <span v-else><i class="fa-solid fa-check"></i> 创建角色</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.mask {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
  padding: 1.5rem;
  animation: fade 0.18s ease;
}
@keyframes fade { from { opacity: 0; } to { opacity: 1; } }

.panel {
  background: var(--tf-dropdown, var(--tf-bg));
  border: 1px solid var(--tf-border);
  border-radius: 18px;
  padding: 1.8rem 2rem 2rem;
  width: min(560px, 94vw);
  max-height: 90vh;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--tf-border) transparent;
  box-shadow: 0 24px 64px var(--tf-shadow);
  animation: slide 0.22s ease;
}
@keyframes slide { from { opacity:0; transform:translateY(14px); } to { opacity:1; transform:translateY(0); } }

.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 1.4rem;
}

.panel-title {
  display: flex; align-items: center; gap: 0.55rem;
  font-family: "Cinzel", "Cormorant Garamond", serif;
  font-size: 1.05rem; font-weight: 700; letter-spacing: 0.06em;
  color: var(--tf-text);
}
.panel-title i { color: var(--tf-accent); font-size: 0.9rem; }

.close-btn {
  width: 28px; height: 28px;
  border: 1px solid var(--tf-border); background: var(--tf-surface);
  color: var(--tf-text3); border-radius: 8px; cursor: pointer;
  display: grid; place-items: center; font-size: 0.8rem;
  transition: background 150ms, color 150ms;
}
.close-btn:hover { background: var(--tf-surface2); color: var(--tf-text); }

.form { display: flex; flex-direction: column; gap: 1.2rem; }

.field { display: flex; flex-direction: column; gap: 0.4rem; }

.label {
  font-size: 0.8rem; font-weight: 600; letter-spacing: 0.05em;
  color: var(--tf-text2);
}

.req { color: var(--tf-text-danger); font-size: 0.72rem; }

.field-hint {
  margin: 0; font-size: 0.75rem;
  color: var(--tf-text3); line-height: 1.5;
}

/* Emoji grid */
.emoji-grid { display: flex; flex-wrap: wrap; gap: 6px; }

.emoji-btn {
  width: 38px; height: 38px; border-radius: 9px;
  border: 1.5px solid var(--tf-border);
  background: var(--tf-surface);
  font-size: 1.25rem; cursor: pointer;
  display: grid; place-items: center;
  transition: border-color 150ms, background 150ms, transform 120ms;
  line-height: 1;
}
.emoji-btn:hover { background: var(--tf-accent-dim); border-color: var(--tf-border-a); transform: scale(1.1); }
.emoji-btn.selected {
  border-color: var(--tf-accent);
  background: var(--tf-accent-dim);
  box-shadow: 0 0 0 2px var(--tf-accent-dim);
}

/* Name row */
.name-row { display: flex; align-items: flex-end; gap: 1rem; }

.name-preview {
  font-size: 2.8rem; line-height: 1; flex-shrink: 0;
  width: 60px; height: 60px;
  background: var(--tf-accent-dim);
  border: 1.5px solid var(--tf-border-a);
  border-radius: 14px; display: grid; place-items: center;
}

.name-field { flex: 1; }

.input {
  padding: 0.65rem 0.9rem; border-radius: 9px;
  border: 1.5px solid var(--tf-border);
  background: var(--tf-input-bg, var(--tf-surface));
  color: var(--tf-text);
  font-size: 0.92rem; outline: none;
  transition: border-color 180ms, box-shadow 180ms;
  width: 100%; box-sizing: border-box;
}
.input:focus { border-color: var(--tf-border-a); box-shadow: 0 0 0 3px var(--tf-accent-dim); }
.input::placeholder { color: var(--tf-placeholder); }

.textarea {
  padding: 0.75rem 0.9rem; border-radius: 9px;
  border: 1.5px solid var(--tf-border);
  background: var(--tf-input-bg, var(--tf-surface));
  color: var(--tf-text);
  font-size: 0.88rem; line-height: 1.7; outline: none;
  resize: vertical; min-height: 130px; font-family: inherit;
  transition: border-color 180ms, box-shadow 180ms;
  width: 100%; box-sizing: border-box;
}
.textarea:focus { border-color: var(--tf-border-a); box-shadow: 0 0 0 3px var(--tf-accent-dim); }
.textarea::placeholder { color: var(--tf-placeholder); }

.char-count {
  text-align: right; font-size: 0.72rem;
  color: var(--tf-text3); margin-top: 2px;
}

.error-msg {
  margin: 0; font-size: 0.8rem;
  color: var(--tf-text-danger);
  display: flex; align-items: center; gap: 0.45rem;
}

.actions { display: flex; gap: 0.75rem; justify-content: flex-end; padding-top: 0.4rem; }

.btn-cancel {
  padding: 0.62rem 1.2rem; border-radius: 9px;
  border: 1px solid var(--tf-border);
  background: var(--tf-surface);
  color: var(--tf-text2);
  font-size: 0.88rem; cursor: pointer;
  transition: border-color 150ms, color 150ms;
}
.btn-cancel:hover { background: var(--tf-surface2); color: var(--tf-text); }

.btn-create {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.62rem 1.5rem; border-radius: 9px;
  border: 1.5px solid var(--tf-border-a);
  background: var(--tf-accent-dim);
  color: var(--tf-accent);
  font-size: 0.88rem; font-weight: 600; cursor: pointer;
  transition: background 160ms, border-color 160ms, transform 130ms, color 130ms;
}
.btn-create:hover:not(:disabled) { background: var(--tf-accent); border-color: var(--tf-accent); color: #fff; transform: translateY(-1px); }
.btn-create:disabled { opacity: 0.5; cursor: not-allowed; }
</style>

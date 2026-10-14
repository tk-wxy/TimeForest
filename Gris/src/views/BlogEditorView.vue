<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from "vue";
import { useRoute, useRouter, onBeforeRouteLeave } from "vue-router";
import { api } from "../utils/api.js";

const route = useRoute();
const router = useRouter();
const blogId = route.params.id;
const BASE = import.meta.env.VITE_API_BASE || "/api";

const title = ref("");
const content = ref("");
const loading = ref(true);
const saving = ref(false);
const status = ref("DRAFT");
const publishModal = ref(false);
const publishSubtitle = ref("");
const publishCoverUrl = ref("");
const publishPosterText = ref("");
const publishing = ref(false);
const dirty = ref(false);
const coverError = ref(false);
let autoSaveTimer = null;
let saveToken = 0;

function formatTime(d) {
  if (!d) return "";
  return new Date(d).toLocaleString("zh-CN");
}

async function fetchDraft() {
  try {
    const { data } = await api.get(`/blogs/${blogId}`);
    title.value = data.title || "";
    content.value = data.content || "";
    status.value = data.status;
    if (data.status === "PUBLISHED") {
      router.replace(`/blog/${blogId}`);
      return;
    }
  } catch {
    alert("无法加载草稿");
    router.replace("/blogs");
  } finally {
    loading.value = false;
  }
}

async function saveDraft(silent = false) {
  const token = ++saveToken;
  saving.value = !silent;
  try {
    await api.put(`/blogs/${blogId}`, {
      title: title.value.trim(),
      content: content.value
    });
    if (token === saveToken) dirty.value = false;
  } catch {
    if (!silent) alert("保存失败，请重试");
  } finally {
    if (token === saveToken) saving.value = false;
  }
}

function triggerAutoSave() {
  dirty.value = true;
  clearTimeout(autoSaveTimer);
  autoSaveTimer = setTimeout(() => { if (dirty.value) saveDraft(true); }, 3000);
}

watch([title, content], triggerAutoSave, { deep: false });

function openPublishModal() {
  publishSubtitle.value = "";
  publishCoverUrl.value = "";
  publishPosterText.value = title.value || "";
  coverError.value = false;
  publishModal.value = true;
}

function onCoverError() {
  coverError.value = true;
}

async function doPublish() {
  if (!publishSubtitle.value.trim()) {
    alert("请填写副标题");
    return;
  }
  publishing.value = true;
  try {
    await api.put(`/blogs/${blogId}`, {
      title: title.value.trim(),
      content: content.value
    });
    await api.put(`/blogs/${blogId}/publish`, {
      subtitle: publishSubtitle.value.trim(),
      coverImageUrl: publishCoverUrl.value.trim() || null,
      coverPosterText: publishPosterText.value.trim() || null
    });
    publishModal.value = false;
    router.replace(`/blog/${blogId}`);
  } catch {
    alert("发布失败，请检查填写的链接");
  } finally {
    publishing.value = false;
  }
}

function renderMarkdown(text) {
  return globalThis.marked ? globalThis.marked.parse(text, { gfm: true, breaks: true }) : text;
}
function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === "s") {
    e.preventDefault();
    saveDraft(false);
  }
}

onMounted(() => {
  fetchDraft();
  window.addEventListener("keydown", onKeydown);
});
onUnmounted(() => {
  clearTimeout(autoSaveTimer);
  window.removeEventListener("keydown", onKeydown);
});

onBeforeRouteLeave((to, from, next) => {
  if (dirty.value) {
    saveDraft(true).finally(() => next());
  } else {
    next();
  }
});
</script>

<template>
  <div class="editor-root">
    <div class="grain-overlay" aria-hidden="true"></div>

    <div v-if="loading" class="loading-hint">
      <i class="fa-solid fa-spinner fa-spin"></i> 加载草稿…
    </div>

    <template v-else>
      <header class="editor-header">
        <div class="header-left">
          <button class="back-btn" @click="router.push('/blogs')">
            <i class="fa-solid fa-arrow-left"></i>
          </button>
          <span class="draft-badge">草稿</span>
          <span v-if="saving" class="save-indicator">保存中…</span>
          <span v-else-if="dirty" class="save-indicator unsaved">未保存</span>
          <span v-else class="save-indicator saved">已保存</span>
        </div>
        <div class="header-right">
          <button class="save-btn" :disabled="saving" @click="saveDraft(false)">
            <i class="fa-solid fa-floppy-disk"></i> 保存
          </button>
          <button class="publish-btn" @click="openPublishModal">
            <i class="fa-solid fa-rocket"></i> 发布
          </button>
        </div>
      </header>

      <div class="editor-body">
        <input
          v-model="title"
          class="title-input"
          type="text"
          placeholder="输入文章标题…"
          maxlength="200"
        />

        <div class="editor-panes">
          <div class="pane pane-edit">
            <div class="pane-label">编辑</div>
            <textarea
              v-model="content"
              class="content-textarea"
              placeholder="用 Markdown 书写正文…"
            ></textarea>
          </div>
          <div class="pane pane-preview">
            <div class="pane-label">预览</div>
            <div
              v-if="content.trim()"
              class="md-body"
              v-html="renderMarkdown(content)"
            ></div>
            <div v-else class="preview-empty">
              <i class="fa-solid fa-eye"></i>
              <p>预览区域</p>
            </div>
          </div>
        </div>
      </div>

      <Teleport to="body">
        <Transition name="modal">
          <div v-if="publishModal" class="modal-backdrop" @mousedown.self="publishModal = false">
            <div class="modal-card" @click.stop>
              <h3 class="modal-title">发布设置</h3>

              <div class="modal-field">
                <label>副标题 <span class="required">*</span></label>
                <input
                  v-model="publishSubtitle"
                  class="modal-input"
                  type="text"
                  placeholder="用一句话吸引读者…"
                  maxlength="300"
                />
              </div>

              <div class="modal-field">
                <label>封面图片 (图床链接)</label>
                <input
                  v-model="publishCoverUrl"
                  class="modal-input"
                  type="url"
                  placeholder="https://…"
                />
                <div v-if="publishCoverUrl && !coverError" class="cover-preview">
                  <img :src="publishCoverUrl" alt="封面预览" referrerpolicy="no-referrer" @error="onCoverError" />
                </div>
              </div>

              <div class="modal-divider">
                <span>或</span>
              </div>

              <div class="modal-field">
                <label>海报文字 (无图床时用文字渲染封面)</label>
                <input
                  v-model="publishPosterText"
                  class="modal-input"
                  type="text"
                  placeholder="默认使用文章标题"
                  maxlength="120"
                />
              </div>

              <div class="modal-actions">
                <button class="modal-cancel" @click="publishModal = false">取消</button>
                <button
                  class="modal-confirm"
                  :disabled="!publishSubtitle.trim() || publishing"
                  @click="doPublish"
                >
                  {{ publishing ? '发布中…' : '确认发布' }}
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>
    </template>
  </div>
</template>

<style scoped>
.editor-root {
  min-height: 100vh; height: 100vh;
  display: flex; flex-direction: column;
  background: var(--tf-bg);
  font-family: "Inter","PingFang SC","Noto Sans SC",sans-serif;
  position: relative; overflow: hidden;
}
.grain-overlay {
  position: fixed; inset: 0; z-index: 0; pointer-events: none; opacity: 0.025;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='256' height='256'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='256' height='256' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 256px;
}
.loading-hint {
  flex: 1; display: flex; align-items: center; justify-content: center;
  color: var(--tf-text3); gap: 0.5rem;
}

.editor-header {
  position: relative; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0.8rem 1.5rem; border-bottom: 1px solid var(--tf-border);
  background: var(--tf-bg); flex-shrink: 0; flex-wrap: wrap; gap: 0.5rem;
}
.header-left { display: flex; align-items: center; gap: 0.6rem; }
.back-btn {
  width: 34px; height: 34px; border-radius: 8px;
  border: 1px solid var(--tf-border); background: var(--tf-surface);
  color: var(--tf-text2); cursor: pointer; display: grid; place-items: center; font-size: 0.82rem;
}
.back-btn:hover { background: var(--tf-accent-dim); color: var(--tf-accent); }

.draft-badge {
  font-size: 0.72rem; padding: 0.15rem 0.5rem;
  border-radius: 6px; background: var(--tf-accent-dim);
  color: var(--tf-accent-text, var(--tf-accent));
  border: 1px solid var(--tf-border-a);
}
.save-indicator { font-size: 0.72rem; }
.save-indicator.unsaved { color: var(--tf-text3); }
.save-indicator.saved { color: var(--tf-text3); opacity: 0.6; }

.header-right { display: flex; align-items: center; gap: 0.5rem; }
.save-btn, .publish-btn {
  padding: 0.35rem 0.9rem; border-radius: 16px;
  font-size: 0.82rem; cursor: pointer; display: flex; align-items: center; gap: 0.3rem;
  transition: all 150ms;
}
.save-btn {
  border: 1px solid var(--tf-border); background: var(--tf-surface); color: var(--tf-text2);
}
.save-btn:hover { background: var(--tf-accent-dim); }
.save-btn:disabled { opacity: 0.5; }
.publish-btn {
  border: 1px solid var(--tf-accent); background: var(--tf-accent);
  color: #fff; font-weight: 600;
}
.publish-btn:hover { opacity: 0.9; }

.editor-body {
  position: relative; z-index: 1;
  flex: 1; display: flex; flex-direction: column;
  min-height: 0; padding: 1rem 1.2rem;
}

.title-input {
  width: 100%; border: none; outline: none; background: none;
  font-family: "Cormorant Garamond",Georgia,serif;
  font-size: 1.6rem; font-weight: 700; letter-spacing: 0.03em;
  color: var(--tf-text); padding: 0.3rem 0; border-bottom: 2px solid var(--tf-border);
}
.title-input::placeholder { color: var(--tf-text3); }

.editor-panes {
  flex: 1; display: flex; gap: 1rem; min-height: 0; margin-top: 0.8rem;
}
.pane {
  flex: 1; display: flex; flex-direction: column;
  border: 1px solid var(--tf-border); border-radius: 10px;
  background: var(--tf-surface); overflow: hidden;
}
.pane-label {
  font-size: 0.72rem; font-weight: 700; letter-spacing: 0.08em;
  text-transform: uppercase; color: var(--tf-text3);
  padding: 0.35rem 0.8rem; border-bottom: 1px solid var(--tf-border);
  flex-shrink: 0;
}

.content-textarea {
  flex: 1; border: none; outline: none; resize: none;
  background: none; padding: 0.7rem 0.8rem;
  font-size: 0.9rem; line-height: 1.75;
  color: var(--tf-text); font-family: "Fira Code","Consolas",monospace;
  min-height: 0;
}
.content-textarea::placeholder { color: var(--tf-placeholder); }

.pane-preview { overflow-y: auto; padding: 0.7rem 0.9rem; }
.preview-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: center; color: var(--tf-text3); gap: 0.5rem; font-size: 0.88rem;
  opacity: 0.5;
}

:deep(.md-body) { font-size: 0.95rem; line-height: 1.9; color: var(--tf-text); }
:deep(.md-body p) { margin: 0 0 0.7em; }
:deep(.md-body h1),:deep(.md-body h2),:deep(.md-body h3) {
  font-family: "Cormorant Garamond",Georgia,serif; font-weight: 700;
  color: var(--tf-text); margin: 1em 0 0.4em;
}
:deep(.md-body code) {
  background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a);
  padding: 1px 5px; border-radius: 4px; font-size: 0.85em; color: var(--tf-accent);
}
:deep(.md-body pre) {
  background: var(--tf-bg3, #1a2314); border: 1px solid var(--tf-border);
  border-radius: 8px; padding: 0.7rem 0.9rem; overflow-x: auto; margin: 0.6em 0;
}
:deep(.md-body pre code) { background: none; border: none; padding: 0; }
:deep(.md-body blockquote) {
  border-left: 3px solid var(--tf-border-a); margin: 0.5em 0; padding: 0.3em 0.8em;
  color: var(--tf-text2); background: var(--tf-accent-dim); border-radius: 0 6px 6px 0;
}
:deep(.md-body img) { max-width: 100%; border-radius: 6px; }
:deep(.md-body a) { color: var(--tf-accent); }

@media (max-width: 768px) {
  .editor-panes { flex-direction: column; }
  .pane { flex: initial; }
  .pane-edit { flex: 2; min-height: 40vh; }
  .pane-preview { flex: 1; min-height: 20vh; }
  .title-input { font-size: 1.3rem; }
}

.modal-backdrop {
  position: fixed; inset: 0; z-index: 999;
  background: rgba(0,0,0,0.55); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  padding: 1rem;
}
.modal-card {
  background: var(--tf-dropdown, #2d2d2d);
  border: 1px solid var(--tf-border);
  border-radius: 16px; padding: 1.5rem;
  width: 100%; max-width: 440px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.3);
}
.modal-title {
  margin: 0 0 1.2rem; font-family: "Cinzel",serif;
  font-size: 1.1rem; font-weight: 700; color: var(--tf-text);
}

.modal-field { margin-bottom: 1rem; }
.modal-field label {
  display: block; font-size: 0.82rem; font-weight: 600;
  color: var(--tf-text2); margin-bottom: 0.3rem;
}
.required { color: #e05550; }
.modal-input {
  width: 100%; padding: 0.5rem 0.7rem;
  border: 1px solid var(--tf-border); border-radius: 8px;
  background: var(--tf-surface); color: var(--tf-text);
  font-size: 0.86rem; outline: none; box-sizing: border-box;
}
.modal-input:focus { border-color: var(--tf-border-a); }

.cover-preview {
  margin-top: 0.5rem; border-radius: 8px; overflow: hidden;
  max-height: 160px; border: 1px solid var(--tf-border);
}
.cover-preview img { width: 100%; object-fit: cover; max-height: 160px; }

.modal-divider {
  display: flex; align-items: center; gap: 0.6rem;
  margin: 1rem 0; color: var(--tf-text3); font-size: 0.78rem;
}
.modal-divider::before, .modal-divider::after {
  content: ''; flex: 1; height: 1px; background: var(--tf-border);
}

.modal-actions {
  display: flex; justify-content: flex-end; gap: 0.6rem;
  margin-top: 1.5rem;
}
.modal-cancel {
  padding: 0.45rem 1rem; border-radius: 10px;
  border: 1px solid var(--tf-border); background: var(--tf-surface);
  color: var(--tf-text2); cursor: pointer; font-size: 0.84rem;
}
.modal-confirm {
  padding: 0.45rem 1.3rem; border-radius: 10px;
  border: none; background: var(--tf-accent);
  color: #fff; cursor: pointer; font-size: 0.84rem; font-weight: 600;
}
.modal-confirm:disabled { opacity: 0.5; cursor: not-allowed; }

.modal-enter-active { transition: opacity 0.2s ease; }
.modal-enter-active .modal-card { transition: transform 0.2s ease; }
.modal-leave-active { transition: opacity 0.15s ease; }
.modal-leave-active .modal-card { transition: transform 0.15s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal-card { transform: translateY(12px) scale(0.97); }
.modal-leave-to .modal-card { transform: translateY(-6px) scale(0.98); }
</style>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "../utils/api.js";

const route = useRoute();
const router = useRouter();
const blogId = route.params.id;

const blog = ref(null);
const loading = ref(true);
const comments = ref([]);
const commentText = ref("");
const submitting = ref(false);
const coverError = ref(false);
const authUserId = Number(localStorage.getItem("tf_userId") || "0");

function formatDate(d) {
  if (!d) return "";
  return new Date(d).toLocaleDateString("zh-CN", { year: "numeric", month: "long", day: "numeric" });
}

function formatTime(d) {
  if (!d) return "";
  return new Date(d).toLocaleString("zh-CN");
}

async function fetchDetail() {
  try {
    const { data } = await api.get(`/blogs/${blogId}`);
    blog.value = data;
  } catch { blog.value = null; } finally { loading.value = false; }
}

async function fetchComments() {
  try {
    const { data } = await api.get(`/blogs/${blogId}/comments`);
    comments.value = data;
  } catch {}
}

async function toggleLike() {
  if (!blog.value) return;
  try {
    const { data } = await api.post(`/blogs/${blogId}/like`);
    blog.value.liked = data.active;
    blog.value.likeCount = data.count;
  } catch {}
}

async function toggleFavorite() {
  if (!blog.value) return;
  try {
    const { data } = await api.post(`/blogs/${blogId}/favorite`);
    blog.value.favorited = data.active;
    blog.value.favoriteCount = data.count;
  } catch {}
}

async function postComment() {
  const t = commentText.value.trim();
  if (!t || submitting.value) return;
  submitting.value = true;
  try {
    const { data } = await api.post(`/blogs/${blogId}/comments`, { content: t });
    comments.value.push(data);
    commentText.value = "";
    if (blog.value) blog.value.commentCount++;
  } catch {} finally { submitting.value = false; }
}

async function deleteComment(id) {
  if (!confirm("删除此评论？")) return;
  try {
    await api.delete(`/blogs/${blogId}/comments/${id}`);
    comments.value = comments.value.filter(c => c.id !== id);
    if (blog.value) blog.value.commentCount = Math.max(0, blog.value.commentCount - 1);
  } catch {}
}

function onCoverError() {
  coverError.value = true;
}

function renderMarkdown(text) {
  return globalThis.marked ? globalThis.marked.parse(text, { gfm: true, breaks: true }) : text;
}

onMounted(() => {
  fetchDetail().then(() => fetchComments());
});
</script>

<template>
  <div class="detail-root">
    <div class="grain-overlay" aria-hidden="true"></div>

    <div v-if="loading" class="msg">
      <i class="fa-solid fa-spinner fa-spin"></i> 加载中…
    </div>
    <div v-else-if="!blog" class="msg">
      <p>文章不存在或无权访问</p>
      <button class="lnk" @click="router.push('/blogs')">返回博客列表</button>
    </div>

    <template v-else>
      <!-- Top bar -->
      <header class="topbar">
        <button class="tb-btn" @click="router.push('/blogs')">
          <i class="fa-solid fa-arrow-left"></i>
        </button>
        <span class="tb-title">{{ blog.title }}</span>
        <div class="tb-spacer"></div>
        <button class="tb-btn" @click="router.push('/profile')">
          <i class="fa-solid fa-circle-user"></i>
        </button>
        <template v-if="blog.owner && blog.status === 'DRAFT'">
          <span class="badge">草稿</span>
          <button class="tb-btn accent" @click="router.push(`/blog/editor/${blog.id}`)">
            <i class="fa-solid fa-pen-to-square"></i> 编辑
          </button>
        </template>
      </header>

      <!-- Cover -->
      <div v-if="blog.coverImageUrl && !coverError" class="cover">
        <img :src="blog.coverImageUrl" :alt="blog.title" referrerpolicy="no-referrer" @error="onCoverError" />
      </div>

      <!-- 2:1 layout -->
      <div class="layout">
        <!-- Left: main content -->
        <main class="main-col">
          <article>
            <h1 class="art-title">{{ blog.title || '无标题' }}</h1>
            <p v-if="blog.subtitle" class="art-sub">{{ blog.subtitle }}</p>
            <div class="art-meta">
              <span class="art-author">{{ blog.authorName }}</span>
              <span class="sep">·</span>
              <span>{{ formatDate(blog.publishedAt || blog.updatedAt) }}</span>
            </div>

            <div class="art-body md-body" v-html="renderMarkdown(blog.content || '（正文为空）')"></div>
          </article>
        </main>

        <!-- Right: comments + stats -->
        <aside class="side-col">
          <!-- Stats row -->
          <div class="stat-row">
            <span class="stat" title="阅读">
              <i class="fa-solid fa-eye"></i> {{ blog.viewCount }}
            </span>
            <span
              class="stat clickable"
              :class="{ on: blog.liked }"
              title="点赞"
              @click="toggleLike"
            >
              <i :class="blog.liked ? 'fa-solid' : 'fa-regular'" class="fa-heart"></i>
              {{ blog.likeCount }}
            </span>
            <span
              class="stat clickable"
              :class="{ on: blog.favorited }"
              title="收藏"
              @click="toggleFavorite"
            >
              <i :class="blog.favorited ? 'fa-solid' : 'fa-regular'" class="fa-star"></i>
              {{ blog.favoriteCount }}
            </span>
            <span class="stat" title="评论">
              <i class="fa-regular fa-comment"></i> {{ blog.commentCount }}
            </span>
          </div>

          <!-- Comments -->
          <div class="comm-box">
            <div class="comm-input-wrap">
              <textarea
                v-model="commentText"
                class="comm-textarea"
                rows="2"
                placeholder="写下评论…"
                maxlength="500"
              ></textarea>
              <div class="comm-input-foot">
                <span class="comm-count">{{ commentText.length }}/500</span>
                <button
                  class="comm-btn"
                  :disabled="!commentText.trim() || submitting"
                  @click="postComment"
                >
                  {{ submitting ? '…' : '发表' }}
                </button>
              </div>
            </div>

            <div v-if="!comments.length" class="comm-empty">暂无评论</div>

            <div v-for="c in comments" :key="c.id" class="comm-item">
              <div class="comm-avatar">{{ c.authorName[0]?.toUpperCase() }}</div>
              <div class="comm-content">
                <div class="comm-head-row">
                  <span class="comm-name">{{ c.authorName }}</span>
                  <span class="comm-time">{{ formatTime(c.createdAt) }}</span>
                </div>
                <p class="comm-text">{{ c.content }}</p>
              </div>
              <button v-if="c.owner" class="comm-del" title="删除" @click="deleteComment(c.id)">
                <i class="fa-solid fa-trash-can"></i>
              </button>
            </div>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* ── Root ── */
.detail-root {
  min-height: 100vh;
  background: var(--tf-bg);
  font-family: "Inter","PingFang SC","Noto Sans SC",sans-serif;
  position: relative;
}
.grain-overlay {
  position: fixed; inset: 0; z-index: 0; pointer-events: none; opacity: 0.025;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='256' height='256'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='256' height='256' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 256px;
}
.msg { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 6rem 1rem; color: var(--tf-text3); gap: 1rem; position: relative; z-index: 1; }
.lnk { background: none; border: none; color: var(--tf-accent); cursor: pointer; font-size: 0.9rem; }

/* ── Top bar ── */
.topbar {
  position: sticky; top: 0; z-index: 100;
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.8rem 1.5rem;
  background: var(--tf-bg);
  border-bottom: 1px solid var(--tf-border);
  backdrop-filter: blur(12px);
}
.tb-btn {
  display: flex; align-items: center; gap: 0.35rem;
  padding: 0.35rem 0.65rem; border-radius: 7px;
  border: 1px solid var(--tf-border);
  background: var(--tf-surface); color: var(--tf-text2);
  font-size: 0.8rem; cursor: pointer; transition: all 0.15s;
}
.tb-btn:hover { background: var(--tf-surface2); color: var(--tf-text); }
.tb-btn.accent { border-color: var(--tf-border-a); background: var(--tf-accent-dim); color: var(--tf-accent); }
.tb-btn.accent:hover { background: var(--tf-accent); color: #fff; }
.tb-spacer { flex: 1; }
.tb-title {
  font-size: 0.82rem; color: var(--tf-text2); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 280px;
}
.badge {
  font-size: 0.7rem; padding: 0.18rem 0.5rem; border-radius: 6px;
  background: var(--tf-accent-dim); color: var(--tf-accent-text, var(--tf-accent));
  border: 1px solid var(--tf-border-a);
}

/* ── Cover ── */
.cover { width: 100%; max-height: 340px; overflow: hidden; border-bottom: 1px solid var(--tf-border); }
.cover img { width: 100%; height: 100%; max-height: 340px; object-fit: cover; display: block; }

/* ── Layout ── */
.layout {
  position: relative; z-index: 1;
  max-width: 1200px; margin: 0 auto;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
  padding: 2rem 1.5rem 4rem;
  align-items: start;
}
.main-col { min-width: 0; }
.side-col { position: sticky; top: 56px; }

@media (max-width: 800px) {
  .layout {
    grid-template-columns: 1fr;
    gap: 1.5rem;
    padding: 1.5rem 1rem 4rem;
  }
  .side-col { position: static; }
  .tb-title { display: none; }
}

/* ── Article ── */
.art-title {
  font-family: "Cormorant Garamond","Cinzel",Georgia,serif;
  font-size: 1.9rem; font-weight: 700; letter-spacing: 0.04em;
  color: var(--tf-text); margin: 0 0 0.4rem; line-height: 1.3;
}
.art-sub { margin: 0 0 0.6rem; font-size: 0.95rem; color: var(--tf-text2); line-height: 1.5; }
.art-meta {
  display: flex; align-items: center; gap: 0.5rem;
  font-size: 0.82rem; color: var(--tf-text3); margin-bottom: 2rem; flex-wrap: wrap;
}
.art-author { color: var(--tf-accent); font-weight: 600; }
.sep { opacity: 0.4; }

.art-body { font-size: 1rem; line-height: 2; color: var(--tf-text); }

/* ── Stat row ── */
.stat-row {
  display: flex; align-items: center; gap: 0.25rem; margin-bottom: 1rem;
  padding: 0.45rem 0.65rem; border-radius: 10px;
  background: var(--tf-surface); border: 1px solid var(--tf-border);
}
.stat {
  display: flex; align-items: center; gap: 0.28rem;
  font-size: 0.78rem; color: var(--tf-text3);
  padding: 0.25rem 0.45rem; border-radius: 6px;
}
.stat i { font-size: 0.75rem; }
.stat.clickable { cursor: pointer; transition: all 0.15s; user-select: none; }
.stat.clickable:hover { background: var(--tf-accent-dim); color: var(--tf-accent); }
.stat.on { color: var(--tf-accent); }

/* ── Comments ── */
.comm-box {
  background: var(--tf-surface); border: 1px solid var(--tf-border);
  border-radius: 12px; padding: 0.85rem;
  max-height: calc(100vh - 200px); overflow-y: auto;
}
.comm-input-wrap { margin-bottom: 0.8rem; }
.comm-textarea {
  width: 100%; border: none; outline: none; resize: none;
  background: var(--tf-bg); border-radius: 8px; padding: 0.55rem 0.7rem;
  font-size: 0.84rem; line-height: 1.5; color: var(--tf-text);
  font-family: inherit; min-height: 2.6rem;
}
.comm-textarea::placeholder { color: var(--tf-placeholder); }
.comm-input-foot {
  display: flex; align-items: center; justify-content: flex-end;
  gap: 0.5rem; margin-top: 0.35rem;
}
.comm-count { font-size: 0.68rem; color: var(--tf-text3); }
.comm-btn {
  padding: 0.25rem 0.7rem; border-radius: 8px;
  border: 1px solid var(--tf-border-a); background: var(--tf-accent-dim);
  color: var(--tf-accent); cursor: pointer; font-size: 0.76rem; font-weight: 500;
  transition: all 0.15s;
}
.comm-btn:hover:not(:disabled) { background: var(--tf-accent); color: #fff; }
.comm-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.comm-empty { padding: 2rem 0; text-align: center; color: var(--tf-text3); font-size: 0.82rem; }

.comm-item {
  display: flex; gap: 0.55rem; padding: 0.6rem 0;
  border-bottom: 1px solid var(--tf-border);
}
.comm-item:last-child { border-bottom: none; }
.comm-avatar {
  width: 30px; height: 30px; border-radius: 8px;
  background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a);
  display: grid; place-items: center; font-weight: 700; font-size: 0.75rem;
  color: var(--tf-accent-text, var(--tf-accent)); flex-shrink: 0;
}
.comm-content { flex: 1; min-width: 0; }
.comm-head-row { display: flex; align-items: center; gap: 0.4rem; margin-bottom: 0.2rem; }
.comm-name { font-size: 0.8rem; font-weight: 600; color: var(--tf-text); }
.comm-time { font-size: 0.68rem; color: var(--tf-text3); }
.comm-text { margin: 0; font-size: 0.84rem; line-height: 1.55; color: var(--tf-text2); word-break: break-word; }
.comm-del {
  flex-shrink: 0; background: none; border: none; color: var(--tf-text3);
  cursor: pointer; font-size: 0.66rem; padding: 0.15rem;
  opacity: 0; transition: opacity 0.15s;
}
.comm-item:hover .comm-del { opacity: 1; }
.comm-del:hover { color: var(--tf-text-danger); }

/* ── Markdown ── */
:deep(.md-body) { font-size: 1rem; line-height: 1.9; color: var(--tf-text); }
:deep(.md-body p) { margin: 0 0 1em; }
:deep(.md-body h1), :deep(.md-body h2), :deep(.md-body h3) {
  font-family: "Cormorant Garamond","Cinzel",Georgia,serif; font-weight: 700;
  color: var(--tf-text); margin: 1em 0 0.35em; line-height: 1.3;
}
:deep(.md-body h1) { font-size: 1.4em; }
:deep(.md-body h2) { font-size: 1.2em; }
:deep(.md-body h3) { font-size: 1.05em; }
:deep(.md-body code) {
  background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a);
  padding: 1px 6px; border-radius: 4px; font-family: "Fira Code",monospace;
  font-size: 0.85em; color: var(--tf-accent);
}
:deep(.md-body pre) {
  background: var(--tf-bg3); border: 1px solid var(--tf-border);
  border-radius: 8px; padding: 0.85rem 1rem; overflow-x: auto; margin: 1em 0;
}
:deep(.md-body pre code) { background: none; border: none; padding: 0; color: var(--tf-text); font-size: 0.85em; }
:deep(.md-body ul), :deep(.md-body ol) { padding-left: 1.4em; margin: 0.35em 0; }
:deep(.md-body li) { margin: 0.25em 0; }
:deep(.md-body blockquote) {
  border-left: 3px solid var(--tf-border-a); margin: 0.8em 0; padding: 0.4em 0.9em;
  color: var(--tf-text2); background: var(--tf-accent-dim); border-radius: 0 7px 7px 0;
}
:deep(.md-body table) { width: 100%; border-collapse: collapse; margin: 0.8em 0; font-size: 0.88em; }
:deep(.md-body th) {
  background: var(--tf-accent-dim); padding: 0.4em 0.7em; text-align: left;
  font-weight: 600; border-bottom: 2px solid var(--tf-border-a);
}
:deep(.md-body td) { padding: 0.35em 0.7em; border-bottom: 1px solid var(--tf-border); }
:deep(.md-body a) { color: var(--tf-accent); text-decoration: underline; text-underline-offset: 2px; }
:deep(.md-body strong) { color: var(--tf-text); font-weight: 700; }
:deep(.md-body img) { max-width: 100%; border-radius: 8px; }
:deep(.md-body hr) { border: none; border-top: 1px solid var(--tf-border); margin: 1.2em 0; }
</style>
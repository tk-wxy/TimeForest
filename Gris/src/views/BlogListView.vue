<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { api } from "../utils/api.js";

const router = useRouter();

const blogs = ref([]);
const loading = ref(false);
const fetchError = ref(false);
const searchQuery = ref("");
const totalPages = ref(0);
const currentPage = ref(0);
const totalElements = ref(0);
const size = 12;

const menuOpen = ref(false);
function toggleMenu(e) { e.stopPropagation(); menuOpen.value = !menuOpen.value; }
function closeMenu() { menuOpen.value = false; }
onMounted(() => { fetchBlogs(); document.addEventListener("click", closeMenu); });
onUnmounted(() => document.removeEventListener("click", closeMenu));

function formatDate(d) {
  if (!d) return "";
  return new Date(d).toLocaleDateString("zh-CN", { year: "numeric", month: "short", day: "numeric" });
}

function truncate(s, len = 60) {
  if (!s) return "";
  return s.length > len ? s.slice(0, len) + "…" : s;
}

async function fetchBlogs(page = 0) {
  loading.value = true;
  fetchError.value = false;
  try {
    const { data } = await api.get("/blogs", { params: { q: searchQuery.value, page, size } });
    if (page === 0) blogs.value = data.items;
    else blogs.value.push(...data.items);
    currentPage.value = data.page;
    totalPages.value = data.totalPages;
    totalElements.value = data.totalElements;
  } catch (e) {
    console.error("加载博客列表失败", e);
    fetchError.value = true;
  } finally { loading.value = false; }
}

function onSearch() { fetchBlogs(0); }
function loadMore() { if (currentPage.value < totalPages.value - 1) fetchBlogs(currentPage.value + 1); }

async function createDraft() {
  try { const { data } = await api.post("/blogs"); router.push(`/blog/editor/${data.id}`); }
  catch (e) { alert("创建失败，请先登录"); }
}

function goDetail(id) { router.push(`/blog/${id}`); }
</script>

<template>
  <div class="blog-list-root">
    <div class="grain-overlay" aria-hidden="true"></div>

    <header class="bl-header">
      <button class="back-btn" @click="router.push('/')">
        <i class="fa-solid fa-arrow-left"></i> 返回
      </button>
      <div class="bl-title">博客</div>
      <button class="create-btn" @click="createDraft">
        <i class="fa-solid fa-feather"></i> 写博客
      </button>
      <div class="search-box">
        <i class="fa-solid fa-search search-icon"></i>
        <input v-model="searchQuery" class="search-input" placeholder="搜索博客…" @keydown.enter="onSearch" />
        <button v-if="searchQuery" class="clear-btn" @click="searchQuery = ''; onSearch()">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </div>
      <div class="menu-wrap" @click.stop>
        <button class="icon-btn" :class="{ active: menuOpen }" title="菜单" @click="toggleMenu">
          <i class="fa-solid fa-bars"></i>
        </button>
        <transition name="drop">
          <div v-if="menuOpen" class="dropdown" role="menu" @click.stop>
            <button class="dd-item" @click="router.push('/'); menuOpen = false">
              <i class="fa-solid fa-house"></i> 首页
            </button>
            <button class="dd-item" @click="router.push('/chat'); menuOpen = false">
              <i class="fa-regular fa-comment-dots"></i> 对话
            </button>
            <button class="dd-item" @click="router.push('/sites'); menuOpen = false">
              <i class="fa-solid fa-globe"></i> 网站
            </button>
            <div class="dd-divider"></div>
            <button class="dd-item" @click="router.push('/profile'); menuOpen = false">
              <i class="fa-solid fa-circle-user"></i> 个人主页
            </button>
          </div>
        </transition>
      </div>
    </header>

    <div class="bl-content">
      <div v-if="loading && !blogs.length && !fetchError" class="loading-hint">
        <i class="fa-solid fa-spinner fa-spin"></i> 加载中…
      </div>
      <div v-if="fetchError && !blogs.length" class="empty-hint">
        <div class="empty-icon" style="color:#e05550"><i class="fa-solid fa-triangle-exclamation"></i></div>
        <p>加载失败，请确认后端服务已启动</p>
        <button class="retry-btn" @click="fetchBlogs(0)">重试</button>
      </div>
      <div v-if="!loading && !fetchError && !blogs.length && !searchQuery" class="empty-hint">
        <div class="empty-icon"><i class="fa-solid fa-feather"></i></div>
        <p>还没有博客，快来写下你的第一篇吧</p>
      </div>
      <div v-if="!loading && !fetchError && !blogs.length && searchQuery" class="empty-hint">
        <div class="empty-icon"><i class="fa-solid fa-magnifying-glass"></i></div>
        <p>未找到 "{{ searchQuery }}" 的结果</p>
      </div>

      <div class="blog-grid">
        <article v-for="blog in blogs" :key="blog.id" class="blog-card" @click="goDetail(blog.id)">
          <div class="card-cover">
            <img v-if="blog.coverImageUrl" :src="blog.coverImageUrl" :alt="blog.title" class="cover-img" />
            <div v-else-if="blog.coverPosterText" class="cover-poster">{{ blog.coverPosterText }}</div>
            <div v-else class="cover-placeholder"><i class="fa-solid fa-feather"></i></div>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ blog.title || '无标题' }}</h3>
            <p class="card-subtitle" v-if="blog.subtitle">{{ truncate(blog.subtitle, 80) }}</p>
            <div class="card-meta">
              <span class="meta-author">{{ blog.authorName }}</span>
              <span class="meta-date">{{ formatDate(blog.publishedAt) }}</span>
            </div>
            <div class="card-stats">
              <span title="浏览"><i class="fa-solid fa-eye"></i> {{ blog.viewCount }}</span>
              <span :title="blog.liked ? '已点赞' : '点赞'" :class="{ active: blog.liked }">
                <i class="fa-solid fa-heart"></i> {{ blog.likeCount }}
              </span>
              <span :title="blog.favorited ? '已收藏' : '收藏'" :class="{ active: blog.favorited }">
                <i class="fa-solid fa-star"></i> {{ blog.favoriteCount }}
              </span>
              <span title="评论"><i class="fa-solid fa-comment"></i> {{ blog.commentCount }}</span>
            </div>
          </div>
        </article>
      </div>

      <div v-if="currentPage < totalPages - 1" class="load-more-wrap">
        <button class="load-more-btn" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中…' : `加载更多 (${totalElements - blogs.length} 篇)` }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.blog-list-root { min-height: 100vh; background: var(--tf-bg); font-family: "Inter","PingFang SC","Noto Sans SC",sans-serif; position: relative; }
.grain-overlay { position: fixed; inset: 0; z-index: 0; pointer-events: none; opacity: 0.025; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='256' height='256'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='256' height='256' filter='url(%23n)'/%3E%3C/svg%3E"); background-size: 256px; }

.bl-header { position: sticky; top: 0; z-index: 50; display: flex; align-items: center; gap: 0.8rem; padding: 0.8rem 1.5rem; background: var(--tf-bg); border-bottom: 1px solid var(--tf-border); flex-wrap: wrap; }
.back-btn { background: none; border: none; color: var(--tf-text2); cursor: pointer; font-size: 0.88rem; white-space: nowrap; display: flex; align-items: center; gap: 0.4rem; }
.back-btn:hover { color: var(--tf-accent); }
.bl-title { font-family: "Cinzel",serif; font-size: 1.1rem; font-weight: 700; letter-spacing: 0.08em; color: var(--tf-text); margin-right: auto; }

/* create button (before search, after title) */
.create-btn { padding: 0.42rem 1rem; border: 1px solid var(--tf-border-a); border-radius: 18px; background: var(--tf-accent-dim); color: var(--tf-accent-text, var(--tf-accent)); font-size: 0.84rem; cursor: pointer; display: flex; align-items: center; gap: 0.35rem; transition: background 150ms; flex-shrink: 0; }
.create-btn:hover { background: var(--tf-accent); color: #fff; }

.search-box { position: relative; display: flex; align-items: center; }
.search-icon { position: absolute; left: 0.75rem; color: var(--tf-text3); font-size: 0.78rem; }
.search-input { width: 200px; padding: 0.45rem 0.75rem 0.45rem 2.1rem; border: 1px solid var(--tf-border); border-radius: 18px; background: var(--tf-surface); color: var(--tf-text); font-size: 0.85rem; outline: none; transition: border-color 150ms; }
.search-input:focus { border-color: var(--tf-border-a); }
.clear-btn { position: absolute; right: 0.55rem; background: none; border: none; color: var(--tf-text3); cursor: pointer; font-size: 0.72rem; }

.icon-btn { width: 36px; height: 36px; border-radius: 10px; border: 1px solid var(--tf-border); background: var(--tf-surface); color: var(--tf-text2); cursor: pointer; display: grid; place-items: center; font-size: 0.82rem; flex-shrink: 0; transition: background 150ms, color 150ms, border-color 150ms; }
.icon-btn:hover, .icon-btn.active { background: var(--tf-accent-dim); color: var(--tf-accent-text, var(--tf-accent)); border-color: var(--tf-border-a); }

.menu-wrap { position: relative; flex-shrink: 0; }
.dropdown { position: absolute; top: calc(100% + 8px); right: 0; min-width: 160px; background: var(--tf-dropdown); border: 1px solid var(--tf-border); border-radius: 14px; padding: 0.45rem; box-shadow: 0 16px 44px var(--tf-shadow); z-index: 600; }
.dd-item { display: flex; align-items: center; gap: 0.7rem; width: 100%; padding: 0.52rem 0.75rem; border: none; background: none; color: var(--tf-text); font-size: 0.82rem; border-radius: 7px; cursor: pointer; text-align: left; transition: background 140ms, color 140ms; }
.dd-item i { width: 14px; text-align: center; color: var(--tf-text3); }
.dd-item:hover { background: var(--tf-accent-dim); }
.dd-divider { height: 1px; background: var(--tf-border); margin: 0.3rem 0; }

.bl-content { position: relative; z-index: 1; max-width: 1200px; margin: 0 auto; padding: 1.5rem 1.5rem 3rem; }
.loading-hint, .empty-hint { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 4rem 1rem; color: var(--tf-text3); gap: 0.6rem; }
.empty-icon { width: 56px; height: 56px; border-radius: 50%; background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a); display: grid; place-items: center; font-size: 1.3rem; color: var(--tf-accent); opacity: 0.55; }

.blog-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.3rem; }
.blog-card { border-radius: 14px; overflow: hidden; background: var(--tf-surface); border: 1px solid var(--tf-border); cursor: pointer; transition: transform 0.18s ease, box-shadow 0.18s ease; }
.blog-card:hover { transform: translateY(-3px); box-shadow: 0 8px 28px var(--tf-shadow); }
.card-cover { width: 100%; height: 160px; overflow: hidden; background: var(--tf-accent-dim); display: flex; align-items: center; justify-content: center; }
.cover-img { width: 100%; height: 100%; object-fit: cover; }
.cover-poster { font-family: "Cormorant Garamond",serif; font-size: 1.4rem; font-weight: 700; letter-spacing: 0.05em; color: var(--tf-accent-text, var(--tf-accent)); padding: 1rem; text-align: center; }
.cover-placeholder { font-size: 2rem; color: var(--tf-accent-text, var(--tf-accent)); opacity: 0.4; }
.card-body { padding: 0.9rem 1rem 1rem; }
.card-title { margin: 0 0 0.3rem; font-size: 0.98rem; font-weight: 600; color: var(--tf-text); line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-subtitle { margin: 0 0 0.6rem; font-size: 0.8rem; color: var(--tf-text3); line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-meta { display: flex; align-items: center; gap: 0.6rem; font-size: 0.74rem; color: var(--tf-text3); margin-bottom: 0.5rem; }
.meta-author { color: var(--tf-accent); }
.card-stats { display: flex; gap: 1rem; font-size: 0.74rem; color: var(--tf-text3); align-items: center; }
.card-stats span { display: flex; align-items: center; gap: 0.22rem; }
.card-stats .active { color: var(--tf-accent); }

.load-more-wrap { display: flex; justify-content: center; margin-top: 2rem; }
.load-more-btn { padding: 0.55rem 1.5rem; border: 1px solid var(--tf-border); border-radius: 20px; background: var(--tf-surface); color: var(--tf-text2); cursor: pointer; font-size: 0.84rem; transition: background 150ms; }
.load-more-btn:hover { background: var(--tf-accent-dim); }
.load-more-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.retry-btn { padding: 0.45rem 1.2rem; border-radius: 18px; border: 1px solid var(--tf-border-a); background: var(--tf-accent-dim); color: var(--tf-accent); cursor: pointer; font-size: 0.84rem; margin-top: 0.5rem; }
.retry-btn:hover { background: var(--tf-accent); color: #fff; }

.drop-enter-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.drop-leave-active { transition: opacity 0.1s ease, transform 0.1s ease; }
.drop-enter-from, .drop-leave-to { opacity: 0; transform: translateY(-6px) scale(0.97); }

@media (max-width: 600px) { .bl-content { padding: 1rem 1rem 2rem; } .search-input { width: 140px; } }
</style>
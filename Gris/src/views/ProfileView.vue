<script setup>
import { ref, onMounted, onUnmounted, computed } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth.js";
import { api } from "../utils/api.js";
import { currentTheme, setTheme } from "../utils/theme.js";

const router = useRouter();
const auth = useAuthStore();

const menuOpen = ref(false);
function toggleMenu(e) { e.stopPropagation(); menuOpen.value = !menuOpen.value; }
function closeMenu() { menuOpen.value = false; }
onMounted(() => document.addEventListener("click", closeMenu));
onUnmounted(() => document.removeEventListener("click", closeMenu));

const profile = ref(null);
const sessions = ref([]);
const drafts = ref([]);
const publishedBlogs = ref([]);
const favorites = ref([]);
const loading = ref(true);
const activeTab = ref("overview");  // "overview" | "sessions" | "blog"
const blogTab = ref("draft");       // "draft" | "published" | "favorite"

onMounted(async () => {
  try {
    const [me, sess, d, p] = await Promise.all([
      api.get("/users/me"),
      api.get("/sessions"),
      api.get("/blogs/mine", { params: { status: "draft" } }),
      api.get("/blogs/mine", { params: { status: "published" } })
    ]);
    profile.value = me.data;
    sessions.value = sess.data;
    drafts.value = d.data || [];
    publishedBlogs.value = p.data || [];
    // 预加载收藏
    await fetchFavorites();
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
});

async function fetchFavorites() {
  try {
    const { data } = await api.get("/blogs/favorites");
    favorites.value = data.items || [];
  } catch (e) {
    console.error("加载收藏失败", e);
    favorites.value = [];
  }
}

const joinedText = computed(() => {
  if (!profile.value?.createdAt) return "—";
  const d = new Date(profile.value.createdAt);
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日`;
});

const initial = computed(() => (profile.value?.username || auth.username || "U")[0].toUpperCase());

const blogCount = computed(() => drafts.value.length + publishedBlogs.value.length);

function formatBlogDate(d) {
  if (!d) return "";
  return new Date(d).toLocaleDateString("zh-CN", { year: "numeric", month: "short", day: "numeric" });
}

function truncate(s, len = 60) {
  if (!s) return "";
  return s.length > len ? s.slice(0, len) + "…" : s;
}

async function createDraft() {
  try {
    const { data } = await api.post("/blogs");
    router.push(`/blog/editor/${data.id}`);
  } catch {
    alert("创建草稿失败");
  }
}

async function deleteBlog(id) {
  if (!confirm("确定删除这篇文章吗？此操作不可撤销。")) return;
  try {
    await api.delete(`/blogs/${id}`);
    drafts.value = drafts.value.filter(b => b.id !== id);
    publishedBlogs.value = publishedBlogs.value.filter(b => b.id !== id);
    favorites.value = favorites.value.filter(b => b.id !== id);
  } catch {
    alert("删除失败，请重试");
  }
}

async function deleteSession(id) {
  if (!confirm("删除此对话？")) return;
  await api.delete(`/sessions/${id}`);
  sessions.value = sessions.value.filter(s => s.id !== id);
  if (profile.value) profile.value.sessionCount = Math.max(0, (profile.value.sessionCount || 1) - 1);
}

function goChat(sessionId) {
  router.push({ path: "/chat", query: { session: sessionId } });
}

function logout() { auth.logout(); router.push("/login"); }
</script>

<template>
  <div class="profile-page">
    <div class="grain" aria-hidden="true"></div>

    <!-- ── Top bar ── -->
    <header class="topbar">
      <button class="nav-btn logo-btn" @click="router.push('/')">
        <i class="fa-solid fa-leaf"></i>
      </button>
      <span class="topbar-brand">
        <i class="fa-solid fa-leaf"></i> TimeForest
      </span>
      <div class="topbar-right">
        <!-- Theme picker -->
        <div class="theme-pills">
          <button :class="['pill', currentTheme==='system'&&'on']" @click="setTheme('system')" title="跟随系统">
            <i class="fa-solid fa-circle-half-stroke"></i>
          </button>
          <button :class="['pill', currentTheme==='dark'&&'on']" @click="setTheme('dark')" title="深色">
            <i class="fa-solid fa-moon"></i>
          </button>
          <button :class="['pill', currentTheme==='light'&&'on']" @click="setTheme('light')" title="浅色">
            <i class="fa-solid fa-sun"></i>
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
              <button class="dd-item" @click="router.push('/blogs'); menuOpen = false">
                <i class="fa-solid fa-feather"></i> 博客
              </button>
            </div>
          </transition>
        </div>
      </div>
    </header>

    <div v-if="loading" class="loader">
      <i class="fa-solid fa-spinner fa-spin"></i>
    </div>

    <div v-else class="main-content">
      <!-- ── User card ── -->
      <section class="user-card">
        <div class="avatar-wrap">
          <div class="avatar-border">
            <div class="avatar-inner">{{ initial }}</div>
          </div>
        </div>
        <div class="user-meta">
          <h1 class="username">{{ profile?.username }}</h1>
          <p class="email">{{ profile?.email || '未绑定邮箱' }}</p>
          <p class="joined"><i class="fa-regular fa-calendar-days"></i> 加入于 {{ joinedText }}</p>
        </div>
        <!-- Stats inline -->
        <div class="stats">
          <div class="stat">
            <span class="sn">{{ profile?.sessionCount ?? sessions.length }}</span>
            <span class="sl">对话</span>
          </div>
          <div class="stat-div"></div>
          <div class="stat">
            <span class="sn">{{ profile?.roleCount ?? 0 }}</span>
            <span class="sl">角色</span>
          </div>
          <div class="stat-div"></div>
          <div class="stat">
            <span class="sn">{{ blogCount }}</span>
            <span class="sl">Blog</span>
          </div>
        </div>
      </section>

      <!-- ── Tab nav ── -->
      <nav class="tabs">
        <button :class="['tab', activeTab==='overview'&&'on']" @click="activeTab='overview'">
          <i class="fa-solid fa-house-chimney"></i> 概览
        </button>
        <button :class="['tab', activeTab==='sessions'&&'on']" @click="activeTab='sessions'">
          <i class="fa-regular fa-comments"></i> 对话管理
        </button>
        <button :class="['tab', activeTab==='blog'&&'on']" @click="activeTab='blog'">
          <i class="fa-solid fa-feather-pointed"></i> 林间记录
        </button>
      </nav>

      <!-- ════════════ OVERVIEW ════════════ -->
      <section v-show="activeTab === 'overview'" class="tab-panel">
        <div class="overview-grid">
          <!-- Quick actions -->
          <div class="card">
            <div class="card-title"><i class="fa-solid fa-bolt"></i> 快速操作</div>
            <div class="action-list">
              <button class="action-row" @click="router.push('/chat')">
                <span class="action-icon"><i class="fa-regular fa-comments"></i></span>
                <span class="action-text">开始新对话</span>
                <i class="fa-solid fa-chevron-right action-arrow"></i>
              </button>
              <button class="action-row" @click="router.push('/chat')">
                <span class="action-icon"><i class="fa-solid fa-masks-theater"></i></span>
                <span class="action-text">管理角色</span>
                <i class="fa-solid fa-chevron-right action-arrow"></i>
              </button>
              <button class="action-row" @click="createDraft">
                <span class="action-icon"><i class="fa-solid fa-feather-pointed"></i></span>
                <span class="action-text">写篇文章</span>
                <i class="fa-solid fa-chevron-right action-arrow"></i>
              </button>
            </div>
          </div>
          <!-- Recent sessions -->
          <div class="card">
            <div class="card-title"><i class="fa-regular fa-clock"></i> 最近对话</div>
            <div v-if="sessions.length === 0" class="empty-mini">暂无对话记录</div>
            <div v-for="s in sessions.slice(0,4)" :key="s.id" class="session-row" @click="router.push('/chat')">
              <span class="s-emoji">{{ s.role?.avatarEmoji || '💬' }}</span>
              <span class="s-title">{{ s.title }}</span>
              <span class="s-role">{{ s.role?.name || '通用' }}</span>
            </div>
            <button v-if="sessions.length > 4" class="more-link" @click="activeTab='sessions'">
              查看全部 {{ sessions.length }} 条对话 →
            </button>
          </div>
        </div>

        <!-- Account section -->
        <div class="card account-card">
          <div class="card-title"><i class="fa-solid fa-gear"></i> 账户</div>
          <div class="account-row">
            <div>
              <p class="ar-label">用户 ID</p>
              <p class="ar-val mono">#{{ profile?.id }}</p>
            </div>
            <div>
              <p class="ar-label">邮箱</p>
              <p class="ar-val">{{ profile?.email || '未绑定' }}</p>
            </div>
          </div>
          <button class="btn-danger-sm" @click="logout">
            <i class="fa-solid fa-right-from-bracket"></i> 退出登录
          </button>
        </div>
      </section>

      <!-- ════════════ SESSIONS ════════════ -->
      <section v-show="activeTab === 'sessions'" class="tab-panel">
        <div class="card">
          <div class="card-title">
            <span><i class="fa-regular fa-comments"></i> 全部对话</span>
            <span class="badge-count">{{ sessions.length }}</span>
          </div>
          <div v-if="sessions.length === 0" class="empty-mini">
            <i class="fa-regular fa-comment-dots"></i>
            <p>还没有对话，去 <button class="link-btn" @click="router.push('/chat')">对话页</button> 开始吧</p>
          </div>
          <div v-for="s in sessions" :key="s.id" class="session-item-full">
            <span class="sif-emoji">{{ s.role?.avatarEmoji || '💬' }}</span>
            <div class="sif-info">
              <span class="sif-title">{{ s.title }}</span>
              <span class="sif-meta">{{ s.role?.name || '通用助手' }} · {{ new Date(s.updatedAt).toLocaleDateString('zh-CN') }}</span>
            </div>
            <div class="sif-actions">
              <button class="sif-btn" title="进入对话" @click="router.push('/chat')">
                <i class="fa-solid fa-arrow-up-right-from-square"></i>
              </button>
              <button class="sif-btn danger" title="删除" @click="deleteSession(s.id)">
                <i class="fa-solid fa-trash-can"></i>
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- ════════════ BLOG ════════════ -->
      <section v-show="activeTab === 'blog'" class="tab-panel">
        <div class="card">
          <div class="card-title-row">
            <span class="card-title"><i class="fa-solid fa-feather-pointed"></i> 林间记录</span>
            <button class="btn-accent-sm" @click="createDraft">
              <i class="fa-solid fa-plus"></i> 写文章
            </button>
          </div>

          <!-- Blog sub-tabs -->
          <div class="blog-tabs">
            <button :class="['btab', blogTab==='draft'&&'on']" @click="blogTab='draft'">
              草稿 <span class="cnt">{{ drafts.length }}</span>
            </button>
            <button :class="['btab', blogTab==='published'&&'on']" @click="blogTab='published'">
              已发布 <span class="cnt">{{ publishedBlogs.length }}</span>
            </button>
            <button :class="['btab', blogTab==='favorite'&&'on']" @click="blogTab='favorite'">
              <i class="fa-solid fa-star"></i> 收藏 <span class="cnt">{{ favorites.length }}</span>
            </button>
          </div>

          <!-- Drafts -->
          <template v-if="blogTab === 'draft'">
            <div v-if="!drafts.length" class="blog-empty">
              <div class="blog-empty-icon"><i class="fa-regular fa-file-lines"></i></div>
              <p class="blog-empty-title">还没有草稿</p>
              <p class="blog-empty-sub">在森林里记录你的想法与灵感</p>
            </div>
            <div v-for="b in drafts" :key="b.id" class="blog-item" @click="router.push(`/blog/editor/${b.id}`)">
              <div class="blog-item-icon"><i class="fa-regular fa-file-lines"></i></div>
              <div class="blog-item-info">
                <span class="blog-item-title">{{ b.title || '无标题' }}</span>
                <span class="blog-item-meta">{{ formatBlogDate(b.updatedAt || b.publishedAt) }}</span>
              </div>
              <button class="blog-item-del" title="删除" @click.stop="deleteBlog(b.id)">
                <i class="fa-solid fa-trash-can"></i>
              </button>
            </div>
          </template>

          <!-- Published -->
          <template v-if="blogTab === 'published'">
            <div v-if="!publishedBlogs.length" class="blog-empty">
              <div class="blog-empty-icon"><i class="fa-solid fa-book-open"></i></div>
              <p class="blog-empty-title">还没有发布的文章</p>
              <p class="blog-empty-sub">将草稿发布，与世界分享你的故事</p>
            </div>
            <div v-for="b in publishedBlogs" :key="b.id" class="blog-item" @click="router.push(`/blog/${b.id}`)">
              <div class="blog-item-icon"><i class="fa-solid fa-book-open"></i></div>
              <div class="blog-item-info">
                <span class="blog-item-title">{{ b.title || '无标题' }}</span>
                <span class="blog-item-meta">{{ truncate(b.subtitle, 30) }} · {{ formatBlogDate(b.publishedAt) }}</span>
              </div>
              <div class="blog-item-stats">
                <span><i class="fa-solid fa-eye"></i> {{ b.viewCount }}</span>
                <span><i class="fa-solid fa-heart"></i> {{ b.likeCount }}</span>
              </div>
              <button class="blog-item-del" title="删除" @click.stop="deleteBlog(b.id)">
                <i class="fa-solid fa-trash-can"></i>
              </button>
            </div>
          </template>

          <!-- Favorites -->
          <template v-if="blogTab === 'favorite'">
            <div v-if="!favorites.length" class="blog-empty">
              <div class="blog-empty-icon"><i class="fa-solid fa-star"></i></div>
              <p class="blog-empty-title">还没有收藏的文章</p>
              <p class="blog-empty-sub">去发现页收藏你喜欢的文章吧</p>
            </div>
            <div v-for="b in favorites" :key="b.id" class="blog-item" @click="router.push(`/blog/${b.id}`)">
              <div class="blog-item-icon"><i class="fa-solid fa-star"></i></div>
              <div class="blog-item-info">
                <span class="blog-item-title">{{ b.title || '无标题' }}</span>
                <span class="blog-item-meta">{{ truncate(b.subtitle, 30) }} · {{ b.authorName }} · {{ formatBlogDate(b.publishedAt) }}</span>
              </div>
              <div class="blog-item-stats">
                <span><i class="fa-solid fa-eye"></i> {{ b.viewCount }}</span>
                <span><i class="fa-solid fa-heart"></i> {{ b.likeCount }}</span>
              </div>
              <button class="blog-item-del" title="删除" @click.stop="deleteBlog(b.id)">
                <i class="fa-solid fa-trash-can"></i>
              </button>
            </div>
          </template>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* ════════════ Base ════════════ */
.profile-page {
  min-height: 100%; width: 100%;
  background: var(--tf-bg);
  color: var(--tf-text);
  font-family: "Inter","PingFang SC","Noto Sans SC",sans-serif;
  overflow-y: auto; position: relative;
  transition: background 0.25s ease;
}

.grain {
  position: fixed; inset: 0; z-index: 0; pointer-events: none; opacity: 0.025;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='256' height='256'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='256' height='256' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 256px;
}

/* ════════════ Top bar ════════════ */
.topbar {
  position: sticky; top: 0; z-index: 100;
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.85rem 1.5rem;
  background: var(--tf-bg);
  border-bottom: 1px solid var(--tf-border);
  backdrop-filter: blur(12px);
}

.nav-btn {
  display: flex; align-items: center; gap: 0.45rem;
  padding: 0.42rem 0.9rem; border-radius: 8px;
  border: 1px solid var(--tf-border);
  background: var(--tf-surface);
  color: var(--tf-text2); font-size: 0.8rem; cursor: pointer;
  transition: all 150ms;
}
.nav-btn:hover { background: var(--tf-surface2); color: var(--tf-text); }
.nav-btn.accent {
  border-color: var(--tf-border-a); background: var(--tf-accent-dim);
  color: var(--tf-accent);
}
.nav-btn.accent:hover { background: var(--tf-accent); color: #fff; }

.topbar-brand {
  flex: 1; text-align: center;
  font-family: "Cinzel","Cormorant Garamond",serif;
  font-size: 0.9rem; font-weight: 600; letter-spacing: 0.1em;
  color: var(--tf-text2);
  display: flex; align-items: center; justify-content: center; gap: 0.4rem;
}

.topbar-right { display: flex; align-items: center; gap: 0.6rem; }

.theme-pills { display: flex; gap: 0.25rem; }
.pill {
  width: 30px; height: 30px; border-radius: 7px; font-size: 0.75rem;
  border: 1px solid var(--tf-border); background: var(--tf-surface);
  color: var(--tf-text3); cursor: pointer;
  display: grid; place-items: center; transition: all 150ms;
}
.pill.on { border-color: var(--tf-accent); background: var(--tf-accent-dim); color: var(--tf-accent); }
.pill:hover:not(.on) { background: var(--tf-surface2); color: var(--tf-text); }

/* ════════════ Loading ════════════ */
.loader { display: flex; justify-content: center; padding: 4rem; color: var(--tf-text3); font-size: 1.5rem; }

/* ── Icon button ── */
.icon-btn { width: 36px; height: 36px; border-radius: 10px; border: 1px solid var(--tf-border); background: var(--tf-surface); color: var(--tf-text2); cursor: pointer; display: grid; place-items: center; font-size: 0.82rem; flex-shrink: 0; transition: background 150ms, color 150ms, border-color 150ms; }
.icon-btn:hover, .icon-btn.active { background: var(--tf-accent-dim); color: var(--tf-accent-text, var(--tf-accent)); border-color: var(--tf-border-a); }

/* ── Menu ── */
.menu-wrap { position: relative; flex-shrink: 0; }
.dropdown { position: absolute; top: calc(100% + 8px); right: 0; min-width: 160px; background: var(--tf-dropdown); border: 1px solid var(--tf-border); border-radius: 14px; padding: 0.45rem; box-shadow: 0 16px 44px var(--tf-shadow); z-index: 600; }
.dd-item { display: flex; align-items: center; gap: 0.7rem; width: 100%; padding: 0.52rem 0.75rem; border: none; background: none; color: var(--tf-text); font-size: 0.82rem; border-radius: 7px; cursor: pointer; text-align: left; transition: background 140ms, color 140ms; }
.dd-item i { width: 14px; text-align: center; color: var(--tf-text3); }
.dd-item:hover { background: var(--tf-accent-dim); }

.drop-enter-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.drop-leave-active { transition: opacity 0.1s ease, transform 0.1s ease; }
.drop-enter-from, .drop-leave-to { opacity: 0; transform: translateY(-6px) scale(0.97); }

/* ════════════ Main content ════════════ */
.main-content {
  max-width: 780px; margin: 0 auto;
  padding: 2rem 1.5rem 3rem;
  display: flex; flex-direction: column; gap: 1.5rem;
  position: relative; z-index: 1;
}
@media (max-width: 600px) { .main-content { padding: 1.4rem 1rem 2rem; gap: 1.2rem; } }

/* ════════════ User card ════════════ */
.user-card {
  display: flex; align-items: center; gap: 1.5rem;
  padding: 1.6rem 1.8rem;
  background: var(--tf-surface);
  border: 1px solid var(--tf-border);
  border-radius: 18px;
  box-shadow: 0 2px 20px var(--tf-shadow);
}

@media (max-width: 600px) {
  .user-card { flex-direction: column; text-align: center; gap: 1rem; padding: 1.4rem; }
  .stats { justify-content: center; }
}

.avatar-wrap { flex-shrink: 0; }

.avatar-border {
  width: 80px; height: 80px; border-radius: 50%; padding: 3px;
  background: conic-gradient(var(--tf-accent) 0deg, var(--tf-accent-dim) 120deg, var(--tf-accent) 240deg, var(--tf-accent-dim) 360deg);
  animation: spin 8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.avatar-inner {
  width: 100%; height: 100%; border-radius: 50%;
  background: var(--tf-bg2, var(--tf-bg));
  display: grid; place-items: center;
  font-family: "Cinzel",serif; font-size: 1.9rem; font-weight: 700;
  color: var(--tf-text);
}

.user-meta { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 0.25rem; }
.username { margin: 0; font-family: "Cormorant Garamond",Georgia,serif; font-size: 1.55rem; font-weight: 700; color: var(--tf-text); }
.email { margin: 0; font-size: 0.82rem; color: var(--tf-text2); }
.joined { margin: 0; font-size: 0.78rem; color: var(--tf-text3); display: flex; align-items: center; gap: 0.4rem; }

.stats { display: flex; align-items: center; gap: 1rem; margin-top: 0.5rem; }
.stat { display: flex; flex-direction: column; align-items: center; gap: 0.12rem; }
.sn { font-family: "Cinzel",serif; font-size: 1.3rem; font-weight: 700; color: var(--tf-accent); line-height: 1; }
.sl { font-size: 0.68rem; color: var(--tf-text3); letter-spacing: 0.05em; }
.stat-div { width: 1px; height: 28px; background: var(--tf-border); }

/* ════════════ Tabs ════════════ */
.tabs {
  display: flex; gap: 0.3rem;
  background: var(--tf-surface); border: 1px solid var(--tf-border);
  border-radius: 12px; padding: 0.35rem;
}

.tab {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 0.45rem;
  padding: 0.55rem 0.5rem; border-radius: 9px; border: none;
  background: none; color: var(--tf-text2); font-size: 0.82rem; font-weight: 600;
  letter-spacing: 0.03em; cursor: pointer; transition: all 150ms;
}
.tab.on { background: var(--tf-surface2); color: var(--tf-text); box-shadow: 0 1px 6px var(--tf-shadow); }
.tab:not(.on):hover { background: var(--tf-surface2); color: var(--tf-text); }
@media (max-width: 480px) { .tab i { display: none; } }

/* ════════════ Tab panels ════════════ */
.tab-panel { display: flex; flex-direction: column; gap: 1rem; }

.card {
  background: var(--tf-surface);
  border: 1px solid var(--tf-border);
  border-radius: 14px;
  padding: 1.2rem 1.4rem;
  box-shadow: 0 1px 12px var(--tf-shadow);
}
@media (max-width: 480px) { .card { padding: 1rem; } }

.card-title {
  font-size: 0.82rem; font-weight: 700; color: var(--tf-text2);
  letter-spacing: 0.05em; text-transform: uppercase;
  margin-bottom: 1rem;
  display: flex; align-items: center; gap: 0.5rem;
}
.card-title i { color: var(--tf-accent); }
.badge-count {
  margin-left: auto; background: var(--tf-accent-dim);
  color: var(--tf-accent); border-radius: 20px;
  padding: 1px 8px; font-size: 0.72rem; font-weight: 700;
}

/* Overview grid */
.overview-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
@media (max-width: 580px) { .overview-grid { grid-template-columns: 1fr; } }

/* Action list */
.action-list { display: flex; flex-direction: column; gap: 0.3rem; }
.action-row {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.62rem 0.7rem; border-radius: 9px;
  border: none; background: none; cursor: pointer; width: 100%; text-align: left;
  color: var(--tf-text); transition: background 140ms;
}
.action-row:hover:not(.disabled) { background: var(--tf-accent-dim); }
.action-row.disabled { opacity: 0.5; cursor: not-allowed; }
.action-icon { width: 30px; height: 30px; border-radius: 8px; background: var(--tf-accent-dim); display: grid; place-items: center; font-size: 0.8rem; color: var(--tf-accent); flex-shrink: 0; }
.action-text { flex: 1; font-size: 0.85rem; }
.action-arrow { color: var(--tf-text3); font-size: 0.65rem; }
.tag { font-size: 0.68rem; background: var(--tf-accent-dim); color: var(--tf-accent); padding: 1px 6px; border-radius: 4px; margin-left: 0.4rem; font-style: normal; }

/* Recent sessions mini */
.empty-mini { color: var(--tf-text3); font-size: 0.82rem; text-align: center; padding: 1.2rem; display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
.empty-mini i { font-size: 1.5rem; opacity: 0.4; }

.session-row {
  display: flex; align-items: center; gap: 0.6rem;
  padding: 0.5rem 0.5rem; border-radius: 8px;
  cursor: pointer; transition: background 140ms;
}
.session-row:hover { background: var(--tf-accent-dim); }
.s-emoji { font-size: 1rem; flex-shrink: 0; }
.s-title { flex: 1; font-size: 0.82rem; color: var(--tf-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.s-role { font-size: 0.68rem; color: var(--tf-text3); white-space: nowrap; }
.more-link { width: 100%; text-align: center; padding: 0.5rem; font-size: 0.78rem; color: var(--tf-accent); background: none; border: none; cursor: pointer; border-radius: 7px; transition: background 140ms; }
.more-link:hover { background: var(--tf-accent-dim); }

/* Account card */
.account-card { display: flex; flex-direction: column; gap: 1rem; }
.account-row { display: flex; gap: 2rem; flex-wrap: wrap; }
.ar-label { margin: 0 0 0.2rem; font-size: 0.72rem; color: var(--tf-text3); text-transform: uppercase; letter-spacing: 0.07em; }
.ar-val { margin: 0; font-size: 0.88rem; color: var(--tf-text); }
.mono { font-family: "Fira Code", monospace; }
.btn-danger-sm {
  align-self: flex-start; display: flex; align-items: center; gap: 0.45rem;
  padding: 0.5rem 1rem; border-radius: 8px; font-size: 0.82rem;
  border: 1px solid rgba(200,70,50,0.3); background: rgba(180,50,30,0.08);
  color: var(--tf-text-danger); cursor: pointer; transition: all 150ms;
}
.btn-danger-sm:hover { background: rgba(200,60,40,0.18); border-color: rgba(220,80,55,0.5); }

/* ════════════ Sessions tab ════════════ */
.session-item-full {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.7rem 0.5rem; border-radius: 10px;
  border-bottom: 1px solid var(--tf-border);
  transition: background 140ms;
}
.session-item-full:last-child { border-bottom: none; }
.session-item-full:hover { background: var(--tf-accent-dim); }
.sif-emoji { font-size: 1.2rem; flex-shrink: 0; }
.sif-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.sif-title { font-size: 0.85rem; color: var(--tf-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sif-meta { font-size: 0.7rem; color: var(--tf-text3); }
.sif-actions { display: flex; gap: 0.4rem; flex-shrink: 0; }
.sif-btn {
  width: 30px; height: 30px; border-radius: 7px; border: 1px solid var(--tf-border);
  background: none; color: var(--tf-text3); font-size: 0.72rem; cursor: pointer;
  display: grid; place-items: center; transition: all 140ms;
}
.sif-btn:hover { background: var(--tf-surface2); color: var(--tf-text); }
.sif-btn.danger:hover { background: rgba(200,60,40,0.15); color: var(--tf-text-danger); border-color: rgba(200,60,40,0.3); }
.link-btn { background: none; border: none; color: var(--tf-accent); cursor: pointer; font-size: 0.82rem; text-decoration: underline; }

/* ════════════ Blog tab ════════════ */
.card-title-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
.btn-accent-sm {
  display: flex; align-items: center; gap: 0.4rem;
  padding: 0.42rem 0.9rem; border-radius: 8px; font-size: 0.78rem;
  border: 1px solid var(--tf-border-a); background: var(--tf-accent-dim);
  color: var(--tf-accent); cursor: pointer; transition: all 150ms;
}
.btn-accent-sm:disabled { opacity: 0.45; cursor: not-allowed; }
.btn-accent-sm:hover:not(:disabled) { background: var(--tf-accent); color: #fff; }

.blog-tabs { display: flex; gap: 0.3rem; margin-bottom: 1.5rem; background: var(--tf-bg); border-radius: 9px; padding: 0.3rem; }
.btab { flex: 1; padding: 0.48rem; border-radius: 7px; border: none; background: none; color: var(--tf-text2); font-size: 0.82rem; font-weight: 600; cursor: pointer; transition: all 150ms; display: flex; align-items: center; justify-content: center; gap: 0.4rem; }
.btab.on { background: var(--tf-surface2); color: var(--tf-text); }
.btab:not(.on):hover { background: var(--tf-surface2); }
.cnt { background: var(--tf-accent-dim); color: var(--tf-accent); border-radius: 10px; padding: 0 6px; font-size: 0.7rem; }

.blog-empty { display: flex; flex-direction: column; align-items: center; gap: 0.8rem; padding: 2.5rem 1rem; text-align: center; }
.blog-empty-icon { width: 64px; height: 64px; border-radius: 50%; background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a); display: grid; place-items: center; font-size: 1.6rem; color: var(--tf-accent); opacity: 0.7; }
.blog-empty-title { margin: 0; font-size: 1rem; font-weight: 600; color: var(--tf-text2); }
.blog-empty-sub { margin: 0; font-size: 0.82rem; color: var(--tf-text3); max-width: 280px; }
.coming-tag { display: flex; align-items: center; gap: 0.45rem; font-size: 0.75rem; color: var(--tf-accent); background: var(--tf-accent-dim); padding: 0.38rem 1rem; border-radius: 20px; border: 1px solid var(--tf-border-a); }

.blog-item {
  display: flex; align-items: center; gap: 0.7rem;
  padding: 0.65rem 0.5rem; border-radius: 10px;
  border-bottom: 1px solid var(--tf-border);
  cursor: pointer; transition: background 140ms;
}
.blog-item:last-child { border-bottom: none; }
.blog-item:hover { background: var(--tf-accent-dim); }

.blog-item-icon {
  width: 34px; height: 34px; border-radius: 9px;
  background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a);
  display: grid; place-items: center; font-size: 0.82rem;
  color: var(--tf-accent-text, var(--tf-accent)); flex-shrink: 0;
}

.blog-item-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 0.15rem; }
.blog-item-title {
  font-size: 0.85rem; font-weight: 600; color: var(--tf-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.blog-item-meta { font-size: 0.72rem; color: var(--tf-text3); }

.blog-item-stats {
  display: flex; gap: 0.8rem; font-size: 0.72rem; color: var(--tf-text3); flex-shrink: 0;
}
.blog-item-stats span { display: flex; align-items: center; gap: 0.18rem; }

.blog-item-arrow { color: var(--tf-text3); font-size: 0.65rem; flex-shrink: 0; }

.blog-item-del {
  width: 28px; height: 28px; border-radius: 6px;
  border: 1px solid transparent; background: none;
  color: var(--tf-text3); cursor: pointer;
  display: grid; place-items: center; font-size: 0.7rem;
  flex-shrink: 0; transition: all 0.15s;
}
.blog-item-del:hover { background: rgba(200,60,40,0.15); color: var(--tf-text-danger); border-color: rgba(200,60,40,0.3); }
</style>

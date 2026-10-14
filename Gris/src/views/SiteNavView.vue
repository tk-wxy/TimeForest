<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { api } from "../utils/api.js";

const router = useRouter();

const sites = ref([]);
const loading = ref(false);
const fetchError = ref(false);

const showAddModal = ref(false);
const formUrl = ref("");
const formName = ref("");
const formDescription = ref("");
const formIconUrl = ref("");
const adding = ref(false);
const addError = ref("");
const aiFilling = ref(false);
const aiFilled = ref(false);

const deletingId = ref(null);
const menuOpen = ref(false);

function toggleMenu(e) { e.stopPropagation(); menuOpen.value = !menuOpen.value; }
function closeMenu() { menuOpen.value = false; }
onMounted(() => { fetchSites(); document.addEventListener("click", closeMenu); });
onUnmounted(() => document.removeEventListener("click", closeMenu));

async function fetchSites() {
  loading.value = true;
  fetchError.value = false;
  try {
    const { data } = await api.get("/sites");
    sites.value = data || [];
  } catch (e) {
    console.error("加载网站列表失败", e);
    fetchError.value = true;
  } finally {
    loading.value = false;
  }
}

function openAddModal() {
  formUrl.value = "";
  formName.value = "";
  formDescription.value = "";
  formIconUrl.value = "";
  addError.value = "";
  aiFilling.value = false;
  aiFilled.value = false;
  showAddModal.value = true;
}

function closeAddModal() {
  showAddModal.value = false;
}

function applyLocalDefaults() {
  let url = formUrl.value.trim();
  if (!url) return;
  if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
  try {
    let host = new URL(url).hostname;
    if (host.startsWith("www.")) host = host.substring(4);
    if (!formName.value) formName.value = host;
  } catch (e) {}
}

async function onUrlBlur() {
  const url = formUrl.value.trim();
  if (!url) return;
  applyLocalDefaults();
  aiFilling.value = true;
  aiFilled.value = false;
  aiFilling.value = false;
  aiFilled.value = true;
}

async function handleAdd() {
  const url = formUrl.value.trim();
  if (!url) { addError.value = "请输入网址"; return; }
  adding.value = true;
  addError.value = "";
  try {
    const payload = { url };
    if (formName.value.trim()) payload.name = formName.value.trim();
    if (formDescription.value.trim()) payload.description = formDescription.value.trim();
    if (formIconUrl.value.trim()) payload.iconUrl = formIconUrl.value.trim();
    await api.post("/sites", payload);
    showAddModal.value = false;
    await fetchSites();
  } catch (e) {
    const msg = e.response?.data?.message || e.message || "添加失败";
    addError.value = msg;
  } finally {
    adding.value = false;
  }
}

async function handleDelete(id) {
  if (deletingId.value === id) {
    try { await api.delete(`/sites/${id}`); sites.value = sites.value.filter(s => s.id !== id); } catch (e) { alert("删除失败"); }
    deletingId.value = null;
  } else {
    deletingId.value = id;
  }
}

function cancelDelete() { deletingId.value = null; }
function openSite(url) { window.open(url, "_blank"); }

function onIconError(e) {
  e.target.style.display = "none";
  const fallback = e.target.parentElement.querySelector(".fallback-icon");
  if (fallback) fallback.style.display = "";
}
</script>

<template>
  <div class="sitenav-root">
    <div class="grain-overlay" aria-hidden="true"></div>

    <header class="sn-header">
      <button class="back-btn" @click="router.push('/')">
        <i class="fa-solid fa-arrow-left"></i> 返回
      </button>
      <div class="sn-title">米奇妙妙屋</div>
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
            <button class="dd-item" @click="router.push('/blogs'); menuOpen = false">
              <i class="fa-solid fa-feather"></i> 博客
            </button>
            <div class="dd-divider"></div>
            <button class="dd-item" @click="router.push('/profile'); menuOpen = false">
              <i class="fa-solid fa-circle-user"></i> 个人主页
            </button>
          </div>
        </transition>
      </div>
    </header>

    <div class="sn-content">
      <div v-if="loading && !sites.length && !fetchError" class="loading-hint">
        <i class="fa-solid fa-spinner fa-spin"></i> 加载中…
      </div>

      <div v-if="fetchError && !sites.length" class="empty-hint">
        <div class="empty-icon" style="color:#e05550"><i class="fa-solid fa-triangle-exclamation"></i></div>
        <p>加载失败，请确认后端服务已启动</p>
        <button class="retry-btn" @click="fetchSites">重试</button>
      </div>

      <div class="site-grid">
        <div v-for="site in sites" :key="site.id" class="site-card" @click="openSite(site.url)">
          <button
            class="card-delete-btn" :class="{ confirming: deletingId === site.id }"
            @click.stop="handleDelete(site.id)"
            :title="deletingId === site.id ? '确认删除' : '删除网站'"
          >
            <i v-if="deletingId === site.id" class="fa-solid fa-check"></i>
            <i v-else class="fa-solid fa-xmark"></i>
          </button>
          <button v-if="deletingId === site.id" class="card-cancel-delete-btn" @click.stop="cancelDelete" title="取消">
            <i class="fa-solid fa-rotate-left"></i>
          </button>

          <div class="card-icon">
            <img v-if="site.iconUrl" :src="site.iconUrl" :alt="site.name" class="icon-img" @error="onIconError" referrerpolicy="no-referrer" />
            <i class="fa-solid fa-globe fallback-icon" :style="{ display: site.iconUrl ? 'none' : '' }"></i>
          </div>
          <div class="card-info">
            <h3 class="card-name">{{ site.name || '未知网站' }}</h3>
            <p class="card-desc" v-if="site.description">{{ site.description }}</p>
          </div>
        </div>

        <div class="site-card add-card" @click="openAddModal">
          <div class="card-icon add-icon"><i class="fa-solid fa-plus"></i></div>
          <div class="card-info"><h3 class="card-name">添加网站</h3></div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="fade">
<div v-if="showAddModal" class="modal-overlay" @mousedown.self="closeAddModal">
          <div class="modal-panel">
            <div class="modal-header">
              <h2>添加网站</h2>
              <button class="modal-close-btn" @click="closeAddModal"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <div class="modal-body">
              <div class="form-group">
                <label class="form-label">网址 <span class="required">*</span></label>
                <input v-model="formUrl" class="form-input" type="text" placeholder="例如：github.com" @blur="onUrlBlur" @keydown.enter="handleAdd" />
              </div>
              <div v-if="aiFilling" class="ai-hint"><i class="fa-solid fa-spinner fa-spin"></i> AI 检索中…</div>
              <div v-else-if="aiFilled" class="ai-hint done"><i class="fa-solid fa-circle-check"></i> AI 已自动填充信息</div>
              <div v-else-if="formUrl.trim()" class="ai-hint pending"><i class="fa-solid fa-wand-magic-sparkles"></i> 添加时将自动检索该网站信息</div>

              <div class="form-group">
                <label class="form-label">网站名称 <span class="optional">（选填）</span></label>
                <input v-model="formName" class="form-input" type="text" placeholder="留空则自动获取" @keydown.enter="handleAdd" />
              </div>
              <div class="form-group">
                <label class="form-label">图标 URL <span class="optional">（选填）</span></label>
                <input v-model="formIconUrl" class="form-input" type="text" placeholder="留空则自动获取" @keydown.enter="handleAdd" />
              </div>
              <div class="form-group">
                <label class="form-label">简介 <span class="optional">（选填）</span></label>
                <textarea v-model="formDescription" class="form-textarea" rows="3" placeholder="留空则自动获取"></textarea>
              </div>
              <div v-if="addError" class="form-error"><i class="fa-solid fa-circle-exclamation"></i> {{ addError }}</div>
            </div>
            <div class="modal-footer">
              <button class="modal-cancel-btn" @click="closeAddModal" :disabled="adding">取消</button>
              <button class="modal-submit-btn" @click="handleAdd" :disabled="adding || !formUrl.trim()">
                <i v-if="adding" class="fa-solid fa-spinner fa-spin"></i> {{ adding ? '添加中…' : '添加' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.sitenav-root { min-height: 100vh; background: var(--tf-bg); font-family: "Inter","PingFang SC","Noto Sans SC",sans-serif; position: relative; }
.grain-overlay { position: fixed; inset: 0; z-index: 0; pointer-events: none; opacity: 0.025; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='256' height='256'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='256' height='256' filter='url(%23n)'/%3E%3C/svg%3E"); background-size: 256px; }

.sn-header { position: sticky; top: 0; z-index: 50; display: flex; align-items: center; gap: 1rem; padding: 0.8rem 1.5rem; background: var(--tf-bg); border-bottom: 1px solid var(--tf-border); }
.back-btn { background: none; border: none; color: var(--tf-text2); cursor: pointer; font-size: 0.88rem; white-space: nowrap; display: flex; align-items: center; gap: 0.4rem; }
.back-btn:hover { color: var(--tf-accent); }
.sn-title { font-family: "Cinzel",serif; font-size: 1.1rem; font-weight: 700; letter-spacing: 0.08em; color: var(--tf-text); flex: 1; }

.icon-btn { width: 36px; height: 36px; border-radius: 10px; border: 1px solid var(--tf-border); background: var(--tf-surface); color: var(--tf-text2); cursor: pointer; display: grid; place-items: center; font-size: 0.82rem; flex-shrink: 0; transition: background 150ms, color 150ms, border-color 150ms; }
.icon-btn:hover, .icon-btn.active { background: var(--tf-accent-dim); color: var(--tf-accent-text, var(--tf-accent)); border-color: var(--tf-border-a); }

.menu-wrap { position: relative; flex-shrink: 0; }
.dropdown { position: absolute; top: calc(100% + 8px); right: 0; min-width: 160px; background: var(--tf-dropdown); border: 1px solid var(--tf-border); border-radius: 14px; padding: 0.45rem; box-shadow: 0 16px 44px var(--tf-shadow); z-index: 600; }
.dd-item { display: flex; align-items: center; gap: 0.7rem; width: 100%; padding: 0.52rem 0.75rem; border: none; background: none; color: var(--tf-text); font-size: 0.82rem; border-radius: 7px; cursor: pointer; text-align: left; transition: background 140ms, color 140ms; }
.dd-item i { width: 14px; text-align: center; color: var(--tf-text3); }
.dd-item:hover { background: var(--tf-accent-dim); }
.dd-divider { height: 1px; background: var(--tf-border); margin: 0.3rem 0; }

.sn-content { position: relative; z-index: 1; max-width: 1200px; margin: 0 auto; padding: 1.5rem 1.5rem 3rem; }
.loading-hint, .empty-hint { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 4rem 1rem; color: var(--tf-text3); gap: 0.6rem; }
.empty-icon { width: 56px; height: 56px; border-radius: 50%; background: var(--tf-accent-dim); border: 1px solid var(--tf-border-a); display: grid; place-items: center; font-size: 1.3rem; color: var(--tf-accent); opacity: 0.55; }
.retry-btn { padding: 0.45rem 1.2rem; border-radius: 18px; border: 1px solid var(--tf-border-a); background: var(--tf-accent-dim); color: var(--tf-accent); cursor: pointer; font-size: 0.84rem; margin-top: 0.5rem; }
.retry-btn:hover { background: var(--tf-accent); color: #fff; }

.site-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 1.2rem; }
.site-card { position: relative; border-radius: 14px; overflow: hidden; background: var(--tf-surface); border: 1px solid var(--tf-border); cursor: pointer; transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease; display: flex; flex-direction: column; align-items: center; padding: 1.5rem 1.2rem 1.2rem; gap: 0.8rem; min-height: 180px; }
.site-card:hover { transform: translateY(-3px); box-shadow: 0 8px 28px var(--tf-shadow); border-color: var(--tf-border-a); }
.card-delete-btn { position: absolute; top: 0.5rem; right: 0.5rem; width: 28px; height: 28px; border-radius: 50%; border: 1px solid transparent; background: rgba(255,255,255,0.06); color: var(--tf-text3); cursor: pointer; display: grid; place-items: center; font-size: 0.72rem; transition: all 0.15s ease; z-index: 2; }
.card-delete-btn:hover { background: rgba(220,80,60,0.2); border-color: rgba(220,80,60,0.4); color: #e0917a; }
.card-delete-btn.confirming { background: rgba(220,80,60,0.35); border-color: rgba(220,80,60,0.6); color: #fff; }
.card-cancel-delete-btn { position: absolute; top: 0.5rem; right: 2.4rem; width: 28px; height: 28px; border-radius: 50%; border: 1px solid var(--tf-border); background: rgba(255,255,255,0.06); color: var(--tf-text2); cursor: pointer; display: grid; place-items: center; font-size: 0.7rem; transition: all 0.15s ease; z-index: 2; }
.card-cancel-delete-btn:hover { background: var(--tf-accent-dim); color: var(--tf-accent); }
.card-icon { width: 56px; height: 56px; border-radius: 14px; background: var(--tf-accent-dim); display: grid; place-items: center; flex-shrink: 0; overflow: hidden; }
.icon-img { width: 100%; height: 100%; object-fit: contain; }
.fallback-icon { font-size: 1.5rem; color: var(--tf-accent); opacity: 0.7; }
.card-info { text-align: center; width: 100%; display: flex; flex-direction: column; gap: 0.3rem; }
.card-name { margin: 0; font-size: 0.95rem; font-weight: 600; color: var(--tf-text); line-height: 1.3; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-desc { margin: 0; font-size: 0.78rem; color: var(--tf-text3); line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.add-card { border-style: dashed; border-color: var(--tf-border-a); opacity: 0.7; justify-content: center; }
.add-card:hover { opacity: 1; border-style: solid; }
.add-icon { background: rgba(124,176,150,0.12); }
.add-icon .fa-plus { font-size: 1.4rem; color: var(--tf-accent); opacity: 0.8; }

.modal-overlay { position: fixed; inset: 0; z-index: 999; background: rgba(0,0,0,0.55); backdrop-filter: blur(6px); -webkit-backdrop-filter: blur(6px); display: flex; align-items: center; justify-content: center; padding: 1rem; }
.modal-panel { background: var(--tf-bg2); border: 1px solid var(--tf-border); border-radius: 16px; width: 100%; max-width: 460px; box-shadow: 0 20px 60px rgba(0,0,0,0.4); overflow: hidden; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 1.2rem 1.3rem 0.8rem; border-bottom: 1px solid var(--tf-border); }
.modal-header h2 { margin: 0; font-size: 1.1rem; font-weight: 600; color: var(--tf-text); font-family: "Cinzel",serif; letter-spacing: 0.05em; }
.modal-close-btn { width: 32px; height: 32px; border-radius: 50%; border: 1px solid var(--tf-border); background: none; color: var(--tf-text3); cursor: pointer; display: grid; place-items: center; font-size: 0.85rem; transition: all 0.15s; }
.modal-close-btn:hover { background: var(--tf-accent-dim); color: var(--tf-accent); }
.modal-body { padding: 1.2rem 1.3rem; display: flex; flex-direction: column; gap: 1rem; }
.modal-footer { display: flex; justify-content: flex-end; gap: 0.7rem; padding: 0.9rem 1.3rem 1.2rem; }
.form-group { display: flex; flex-direction: column; gap: 0.35rem; }
.form-label { font-size: 0.84rem; font-weight: 500; color: var(--tf-text2); }
.required { color: #e0917a; } .optional { color: var(--tf-text3); font-weight: 400; font-size: 0.76rem; }
.form-input, .form-textarea { padding: 0.55rem 0.8rem; border: 1px solid var(--tf-border); border-radius: 10px; background: var(--tf-input-bg); color: var(--tf-text); font-size: 0.88rem; outline: none; transition: border-color 0.15s; font-family: inherit; }
.form-input:focus, .form-textarea:focus { border-color: var(--tf-border-a); }
.form-textarea { resize: vertical; min-height: 60px; }
.ai-hint { display: flex; align-items: center; gap: 0.5rem; font-size: 0.78rem; padding: 0.4rem 0.8rem; border-radius: 8px; background: var(--tf-accent-dim); color: var(--tf-accent-text); }
.ai-hint.done { background: rgba(116,173,146,0.15); color: var(--tf-accent); }
.ai-hint.pending { opacity: 0.7; }
.form-error { display: flex; align-items: center; gap: 0.4rem; font-size: 0.82rem; color: #e0917a; padding: 0.4rem 0.8rem; border-radius: 8px; background: rgba(220,80,60,0.08); }
.modal-cancel-btn { padding: 0.5rem 1.2rem; border: 1px solid var(--tf-border); border-radius: 10px; background: none; color: var(--tf-text2); cursor: pointer; font-size: 0.85rem; transition: background 0.15s; }
.modal-cancel-btn:hover { background: var(--tf-surface); }
.modal-submit-btn { padding: 0.5rem 1.5rem; border: 1px solid var(--tf-border-a); border-radius: 10px; background: var(--tf-accent-dim); color: var(--tf-accent); cursor: pointer; font-size: 0.85rem; font-weight: 500; display: flex; align-items: center; gap: 0.4rem; transition: all 0.15s; }
.modal-submit-btn:hover:not(:disabled) { background: var(--tf-accent); color: #fff; }
.modal-submit-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.drop-enter-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.drop-leave-active { transition: opacity 0.1s ease, transform 0.1s ease; }
.drop-enter-from, .drop-leave-to { opacity: 0; transform: translateY(-6px) scale(0.97); }

@media (max-width: 600px) {
  .sn-content { padding: 1rem 1rem 2rem; }
  .site-grid { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 0.8rem; }
  .site-card { padding: 1rem 0.8rem 0.9rem; min-height: 140px; }
  .card-icon { width: 44px; height: 44px; border-radius: 10px; }
  .card-name { font-size: 0.85rem; }
  .modal-panel { max-width: 92vw; }
}
</style>
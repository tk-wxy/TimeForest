<script setup>
import { ref, onMounted } from "vue";
import { useAuthStore } from "../stores/auth.js";
import { useRolesStore } from "../stores/roles.js";
import { api } from "../utils/api.js";
import { useRouter } from "vue-router";
import RoleCreateModal from "./RoleCreateModal.vue";

const router = useRouter();
const auth = useAuthStore();
const rolesStore = useRolesStore();

const props = defineProps({
  activeSessionId: { type: String, default: "" }
});

const emit = defineEmits(["select", "create"]);

// ── State ──────────────────────────────────────────────────
const activeTab = ref("sessions");   // "sessions" | "roles"
const sessions = ref([]);
const sessionsLoading = ref(false);
const showCreateModal = ref(false);

// ── Sessions ───────────────────────────────────────────────
async function fetchSessions() {
  sessionsLoading.value = true;
  try {
    const res = await api.get("/sessions");
    sessions.value = res.data;
  } finally {
    sessionsLoading.value = false;
  }
}

// 静默刷新：只更新数据，不显示加载动画（用于 AI 回复后刷新标题）
async function silentRefresh() {
  try {
    const res = await api.get("/sessions");
    sessions.value = res.data;
  } catch {}
}

async function startSession(role) {
  const res = await api.post("/sessions", { roleId: role.id });
  sessions.value.unshift(res.data);
  activeTab.value = "sessions";
  emit("create", res.data);
}

async function deleteSession(id, e) {
  e.stopPropagation();
  if (!confirm("删除此对话？聊天记录将一并清除。")) return;
  await api.delete(`/sessions/${id}`);
  sessions.value = sessions.value.filter((s) => s.id !== id);
  if (props.activeSessionId === id && sessions.value.length > 0) {
    emit("select", sessions.value[0]);
  }
}

// ── Roles ──────────────────────────────────────────────────
async function onRoleCreated(role) {
  showCreateModal.value = false;
  // 直接用新角色开始一个对话
  await startSession(role);
}

async function deleteRole(id, e) {
  e.stopPropagation();
  if (!confirm("删除此角色？该角色的专属知识库也将停用。")) return;
  await rolesStore.deleteRole(id);
}

// ── Auth ───────────────────────────────────────────────────
function logout() {
  auth.logout();
  router.push("/login");
}

// ── 快速新建默认角色对话 ────────────────────────────────────
async function quickNewSession() {
  if (!rolesStore.loaded) await rolesStore.fetchRoles();
  const defaultRole = rolesStore.roles[0];
  if (defaultRole) {
    await startSession(defaultRole);
  } else {
    showCreateModal.value = true;
  }
}

onMounted(async () => {
  await fetchSessions();
  await rolesStore.fetchRoles();

  if (sessions.value.length > 0) {
    // 已有会话 → 自动激活最近一条
    emit("select", sessions.value[0]);
  } else {
    // 无会话 → 用默认角色自动创建一条，让对话框立即可用
    const defaultRole = rolesStore.roles[0];
    if (defaultRole) await startSession(defaultRole);
  }
});

defineExpose({ fetchSessions, silentRefresh });
</script>

<template>
  <aside class="sidebar">
    <!-- ── Brand ── -->
    <div class="sidebar-brand">
      <span class="brand-text">
        <i class="fa-solid fa-leaf brand-leaf"></i>
        时光之林
      </span>
      <button class="quick-new-btn" title="快速新建默认对话" @click="quickNewSession">
        <i class="fa-solid fa-plus"></i>
      </button>
    </div>

    <!-- ── Tabs ── -->
    <div class="tabs">
      <button
        class="tab"
        :class="{ active: activeTab === 'sessions' }"
        @click="activeTab = 'sessions'"
      >
        <i class="fa-regular fa-comments"></i>
        对话
      </button>
      <button
        class="tab"
        :class="{ active: activeTab === 'roles' }"
        @click="activeTab = 'roles'"
      >
        <i class="fa-solid fa-masks-theater"></i>
        角色
      </button>
    </div>

    <!-- ══════════════ SESSIONS TAB ══════════════ -->
    <div v-show="activeTab === 'sessions'" class="tab-content">
      <div v-if="sessionsLoading" class="hint">
        <i class="fa-solid fa-spinner fa-spin"></i>
      </div>
      <div v-else-if="sessions.length === 0" class="hint empty">
        <i class="fa-regular fa-comment-dots"></i>
        <p>点击右上角 <strong>+</strong> 快速开始，或到「角色」页自定义角色</p>
      </div>
      <div
        v-for="s in sessions"
        :key="s.id"
        class="session-item"
        :class="{ active: s.id === activeSessionId }"
        @click="emit('select', s)"
      >
        <span class="s-emoji">{{ s.role?.avatarEmoji || '💬' }}</span>
        <div class="s-info">
          <span class="s-title">{{ s.title }}</span>
          <span class="s-role">{{ s.role?.name || '通用' }}</span>
        </div>
        <button class="icon-action del" title="删除" @click="deleteSession(s.id, $event)">
          <i class="fa-solid fa-trash-can"></i>
        </button>
      </div>
    </div>

    <!-- ══════════════ ROLES TAB ══════════════ -->
    <div v-show="activeTab === 'roles'" class="tab-content">
      <!-- New role button -->
      <button class="new-role-btn" @click="showCreateModal = true">
        <i class="fa-solid fa-plus"></i>
        新建角色
      </button>

      <!-- System roles -->
      <div class="role-section-label">系统角色</div>
      <template v-for="r in rolesStore.roles.filter(r => !r.isOwner)" :key="r.id">
        <div class="role-card">
          <span class="r-emoji">{{ r.avatarEmoji }}</span>
          <div class="r-info">
            <span class="r-name">{{ r.name }}</span>
            <span class="r-desc">{{ r.description }}</span>
          </div>
          <button class="start-btn" @click="startSession(r)" title="开始对话">
            <i class="fa-solid fa-play"></i>
          </button>
        </div>
      </template>
      <div v-if="rolesStore.roles.filter(r => !r.isOwner).length === 0" class="hint">暂无系统角色</div>

      <!-- User roles -->
      <div class="role-section-label" style="margin-top:1rem;">
        我的角色
      </div>
      <template v-for="r in rolesStore.roles.filter(r => r.isOwner)" :key="r.id">
        <div class="role-card">
          <span class="r-emoji">{{ r.avatarEmoji }}</span>
          <div class="r-info">
            <span class="r-name">{{ r.name }}</span>
            <span class="r-desc">{{ r.description }}</span>
          </div>
          <button class="start-btn" @click="startSession(r)" title="开始对话">
            <i class="fa-solid fa-play"></i>
          </button>
          <button class="icon-action del" title="删除角色" @click="deleteRole(r.id, $event)">
            <i class="fa-solid fa-trash-can"></i>
          </button>
        </div>
      </template>
      <div v-if="rolesStore.roles.filter(r => r.isOwner).length === 0" class="hint empty small">
        <p>还没有自定义角色，点击「新建角色」创建</p>
      </div>
    </div>

    <!-- ── Footer ── -->
    <div class="sidebar-footer">
      <button class="user-info" title="个人主页" @click="router.push('/profile')">
        <div class="user-ava">{{ auth.username?.[0]?.toUpperCase() || 'U' }}</div>
        <span class="user-name">{{ auth.username }}</span>
      </button>
      <button class="logout-btn" title="退出登录" @click="logout">
        <i class="fa-solid fa-right-from-bracket"></i>
      </button>
    </div>
  </aside>

  <!-- Create role modal -->
  <RoleCreateModal
    v-if="showCreateModal"
    @created="onRoleCreated"
    @close="showCreateModal = false"
  />
</template>

<style scoped>
/* ── Base ── */
.sidebar {
  width: 100%;        /* 由父 wrapper 控制宽度 */
  height: 100%;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--tf-sidebar, #080d06);
  border-right: 1px solid var(--tf-border-a, rgba(80,150,50,0.12));
  overflow: hidden;
  transition: background 0.25s ease;
}

/* ── Brand ── */
.sidebar-brand {
  padding: 0.85rem 0.9rem 0.7rem;
  border-bottom: 1px solid var(--tf-border);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand-text {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-family: "Cinzel", "Cormorant Garamond", serif;
  font-size: 0.85rem;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--tf-text);
}

.brand-leaf { color: var(--tf-accent); font-size: 0.78rem; }

.quick-new-btn {
  width: 26px; height: 26px;
  border-radius: 7px;
  border: 1px solid var(--tf-border-a);
  background: var(--tf-accent-dim);
  color: var(--tf-accent);
  font-size: 0.72rem;
  cursor: pointer;
  display: grid; place-items: center;
  flex-shrink: 0;
  transition: background 150ms, border-color 150ms, transform 120ms;
}
.quick-new-btn:hover {
  background: var(--tf-accent);
  color: #fff;
  transform: scale(1.08);
}

/* ── Tabs ── */
.tabs {
  display: flex;
  padding: 0.5rem 0.6rem 0;
  gap: 0.3rem;
  flex-shrink: 0;
}

.tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.45rem 0.5rem;
  border-radius: 8px;
  border: none;
  background: none;
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--tf-text2);
  cursor: pointer;
  transition: background 150ms, color 150ms;
}

.tab.active {
  background: var(--tf-accent-dim);
  color: var(--tf-accent);
}

.tab:not(.active):hover {
  background: var(--tf-surface2);
  color: var(--tf-text);
}

/* ── Tab content area ── */
.tab-content {
  flex: 1;
  overflow-y: auto;
  padding: 0.6rem 0.55rem;
  scrollbar-width: thin;
  scrollbar-color: var(--tf-border) transparent;
  min-height: 0;
}

/* ── Hints ── */
.hint {
  padding: 0.75rem 0.5rem;
  font-size: 0.76rem;
  color: var(--tf-text3);
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.hint.empty {
  flex-direction: column;
  padding-top: 1.5rem;
  gap: 0.6rem;
}

.hint.empty i { font-size: 1.5rem; opacity: 0.4; }
.hint.empty p { margin: 0; }
.hint.small { padding-top: 0.6rem; }

/* ════════════ Sessions ════════════ */
.session-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.55rem 0.65rem;
  border-radius: 9px;
  cursor: pointer;
  transition: background 140ms;
  margin-bottom: 2px;
}

.session-item:hover { background: var(--tf-accent-dim); }

.session-item.active {
  background: var(--tf-accent-dim);
  outline: 1px solid var(--tf-border-a);
}

.s-emoji { font-size: 1.05rem; flex-shrink: 0; width: 24px; text-align: center; }

.s-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }

.s-title {
  font-size: 0.79rem;
  color: var(--tf-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.35;
}

.s-role { font-size: 0.67rem; color: var(--tf-text3); }

/* ════════════ Roles ════════════ */
.new-role-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.6rem;
  border-radius: 10px;
  border: 1.5px dashed var(--tf-border-a);
  background: var(--tf-accent-dim);
  color: var(--tf-accent);
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  cursor: pointer;
  margin-bottom: 0.9rem;
  transition: border-color 160ms, background 160ms, color 160ms;
}

.new-role-btn:hover {
  background: var(--tf-accent);
  color: #fff;
  border-color: var(--tf-accent);
}

.role-section-label {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--tf-text3);
  padding: 0.1rem 0.4rem 0.4rem;
}

.role-card {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.65rem 0.65rem;
  border-radius: 10px;
  border: 1px solid var(--tf-border);
  background: var(--tf-surface);
  margin-bottom: 0.5rem;
  transition: border-color 150ms, background 150ms;
}

.role-card:hover {
  border-color: var(--tf-border-a);
  background: var(--tf-accent-dim);
}

.r-emoji { font-size: 1.4rem; flex-shrink: 0; line-height: 1; }

.r-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 0.12rem; }

.r-name { font-size: 0.82rem; font-weight: 700; color: var(--tf-text); }

.r-desc {
  font-size: 0.7rem;
  color: var(--tf-text2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Action buttons */
.icon-action {
  width: 26px; height: 26px;
  border: none; background: none;
  border-radius: 6px; cursor: pointer;
  display: grid; place-items: center;
  font-size: 0.68rem;
  transition: color 150ms, background 150ms;
  flex-shrink: 0;
  opacity: 0;
}

.session-item:hover .icon-action,
.session-item.active .icon-action,
.role-card:hover .icon-action { opacity: 1; }

.icon-action.del { color: var(--tf-text-danger); }
.icon-action.del:hover { color: var(--tf-text-danger); background: rgba(200,70,50,0.12); }

.start-btn {
  width: 28px; height: 28px;
  border: 1px solid var(--tf-border-a);
  background: var(--tf-accent-dim);
  color: var(--tf-accent);
  border-radius: 7px; cursor: pointer;
  display: grid; place-items: center;
  font-size: 0.7rem;
  flex-shrink: 0;
  transition: background 150ms, border-color 150ms, color 150ms;
}

.start-btn:hover {
  background: var(--tf-accent);
  border-color: var(--tf-accent);
  color: #fff;
}

/* ── Footer ── */
.sidebar-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.8rem 1rem;
  border-top: 1px solid var(--tf-border);
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  min-width: 0;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 8px;
  transition: background 150ms;
}
.user-info:hover { background: var(--tf-accent-dim); }

.user-ava {
  width: 28px; height: 28px;
  border-radius: 50%;
  background: var(--tf-accent-dim);
  border: 1px solid var(--tf-border-a);
  display: grid; place-items: center;
  font-family: "Cinzel", serif;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--tf-accent);
  flex-shrink: 0;
}

.user-name {
  font-size: 0.78rem;
  color: var(--tf-text2);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.logout-btn {
  border: none; background: none;
  color: var(--tf-text3);
  font-size: 0.85rem; cursor: pointer;
  padding: 5px 6px; border-radius: 7px;
  transition: color 150ms, background 150ms;
  flex-shrink: 0;
}

.logout-btn:hover {
  color: var(--tf-text-danger);
  background: rgba(200,60,40,0.08);
}
</style>

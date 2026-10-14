import { createRouter, createWebHashHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import ChatView from "../views/ChatView.vue";
import LoginView from "../views/LoginView.vue";
import RegisterView from "../views/RegisterView.vue";
import ProfileView from "../views/ProfileView.vue";
import BlogListView from "../views/BlogListView.vue";
import BlogDetailView from "../views/BlogDetailView.vue";
import BlogEditorView from "../views/BlogEditorView.vue";
import SiteNavView from "../views/SiteNavView.vue";
import { api } from "../utils/api.js";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", component: HomeView },
    { path: "/login", component: LoginView },
    { path: "/register", component: RegisterView },
    { path: "/chat", component: ChatView, meta: { requiresAuth: true } },
    { path: "/profile", component: ProfileView, meta: { requiresAuth: true } },
    { path: "/blogs", component: BlogListView },
    { path: "/blog/:id", component: BlogDetailView },
{ path: "/blog/editor/:id", component: BlogEditorView, meta: { requiresAuth: true } },
    { path: "/sites", component: SiteNavView }
  ]
});

// 缓存"已验证通过的 token"，仅当 token 变化时才重新请求后端验证，避免每次跳转都打接口
let lastValidatedToken = null;

router.beforeEach(async (to) => {
  const token = localStorage.getItem("tf_token");

  if (to.meta.requiresAuth) {
    if (!token) return { path: "/login", query: { redirect: to.fullPath } };

    // 关键：进入受保护页面前，主动验证 token 是否仍然有效（防止旧 token 残留导致"进得去用不了"）
    if (token !== lastValidatedToken) {
      try {
        await api.get("/users/me");
        lastValidatedToken = token;
      } catch {
        localStorage.removeItem("tf_token");
        localStorage.removeItem("tf_userId");
        localStorage.removeItem("tf_username");
        lastValidatedToken = null;
        return { path: "/login", query: { redirect: to.fullPath } };
      }
    }
  }

  // 已登录（且 token 有效）用户访问登录/注册页 → 回到指定页面或对话页
  if ((to.path === "/login" || to.path === "/register") && token && token === lastValidatedToken) {
    const redirect = to.query.redirect;
    return redirect && typeof redirect === "string" && redirect !== "/login" && redirect !== "/register"
      ? redirect
      : "/chat";
  }
});

export default router;

import { createRouter, createWebHashHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import ChatView from "../views/ChatView.vue";
import LoginView from "../views/LoginView.vue";
import RegisterView from "../views/RegisterView.vue";
import ProfileView from "../views/ProfileView.vue";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", component: HomeView },
    { path: "/login", component: LoginView },
    { path: "/register", component: RegisterView },
    { path: "/chat", component: ChatView, meta: { requiresAuth: true } },
    { path: "/profile", component: ProfileView, meta: { requiresAuth: true } }
  ]
});

router.beforeEach((to) => {
  const token = localStorage.getItem("tf_token");
  if (to.meta.requiresAuth && !token) {
    return "/login";
  }
  if ((to.path === "/login" || to.path === "/register") && token) {
    return "/chat";
  }
});

export default router;

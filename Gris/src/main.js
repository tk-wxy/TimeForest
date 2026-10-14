import { createApp } from "vue";
import { createPinia } from "pinia";
import "./style.css";
import App from "./App.vue";
import router from "./router/index.js";
import { initTheme } from "./utils/theme.js";

initTheme();
createApp(App).use(createPinia()).use(router).mount("#app");

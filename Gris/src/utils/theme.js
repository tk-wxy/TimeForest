import { ref } from "vue";

const STORAGE_KEY = "tf-theme";
export const currentTheme = ref("system"); // "dark" | "light" | "system"

function getSystemPref() {
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function applyTheme(mode) {
  const effective = mode === "system" ? getSystemPref() : mode;
  document.documentElement.setAttribute("data-theme", effective);
}

export function setTheme(mode) {
  currentTheme.value = mode;
  localStorage.setItem(STORAGE_KEY, mode);
  applyTheme(mode);
}

export function initTheme() {
  const saved = localStorage.getItem(STORAGE_KEY) || "system";
  currentTheme.value = saved;
  applyTheme(saved);

  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
    if (currentTheme.value === "system") applyTheme("system");
  });
}

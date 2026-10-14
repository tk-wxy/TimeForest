<script setup>
import { ref, watch, nextTick } from "vue";

const props = defineProps({
  activeSessionId: { type: String, default: "" },
  sending: { type: Boolean, default: false },
  modelValue: { type: String, default: "" }
});

const emit = defineEmits(["update:modelValue", "send"]);

const textareaRef = ref(null);

function resize() {
  const el = textareaRef.value;
  if (!el) return;
  el.style.height = "auto";
  el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
}

watch(() => props.modelValue, () => nextTick(resize));

function onKeydown(e) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    emit("send");
  }
}

function onInput(e) {
  emit("update:modelValue", e.target.value);
  resize();
}

function focus() {
  textareaRef.value?.focus();
}

defineExpose({ focus });
</script>

<template>
  <div class="composer-wrap">
    <div class="composer" :class="{ disabled: !activeSessionId }">
      <textarea
        ref="textareaRef"
        rows="1"
        class="c-input"
        :value="modelValue"
        :placeholder="activeSessionId ? '写下你的问题…' : '请先选择角色开始对话'"
        :disabled="sending || !activeSessionId"
        @input="onInput"
        @keydown="onKeydown"
      ></textarea>
      <button
        class="c-send"
        :class="{ ready: modelValue.trim() && !sending && activeSessionId }"
        :disabled="sending || !activeSessionId || !modelValue.trim()"
        @click="emit('send')"
      >
        <i class="fa-solid fa-paper-plane"></i>
      </button>
    </div>
    <p class="c-tip">AI 可能犯错，请独立判断重要信息</p>
  </div>
</template>

<style scoped>
.composer-wrap { flex-shrink: 0; padding: 0.5rem 0 0.8rem; }

.composer {
  display: flex; align-items: flex-end; gap: 0.5rem;
  background: var(--tf-surface);
  border: 1.5px solid var(--tf-border);
  border-radius: 20px;
  padding: 0.4rem 0.4rem 0.4rem 1.05rem;
  transition: border-color 200ms, box-shadow 200ms;
}
.composer:focus-within {
  border-color: var(--tf-border-a);
  box-shadow: 0 0 0 3px var(--tf-accent-dim);
}
.composer.disabled { opacity: 0.5; }

.c-input {
  flex: 1; background: none; border: none; outline: none; resize: none;
  font-size: 0.92rem; line-height: 1.6;
  color: var(--tf-text);
  padding: 0.5rem 0;
  min-height: 1.45rem; max-height: 180px;
  overflow-y: auto; scrollbar-width: thin; font-family: inherit;
}
.c-input::placeholder { color: var(--tf-placeholder); }

.c-send {
  width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
  border: none; background: var(--tf-surface2);
  color: var(--tf-text3); font-size: 0.8rem; cursor: pointer;
  display: grid; place-items: center;
  transition: background 150ms, color 150ms, transform 150ms;
}
.c-send.ready {
  background: var(--tf-accent); color: #fff;
}
.c-send.ready:hover { transform: scale(1.08); }
.c-send:disabled { cursor: not-allowed; }

.c-tip { margin: 0.45rem 0 0; text-align: center; font-size: 0.7rem; color: var(--tf-text3); }
</style>

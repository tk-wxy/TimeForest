<script setup>
defineProps({
  title: {
    type: String,
    default: ""
  }
});
</script>

<template>
  <article class="frost-card">
    <header v-if="title" class="frost-head">
      <h2>{{ title }}</h2>
    </header>
    <div class="frost-body">
      <slot />
    </div>
  </article>
</template>

<style scoped>
.frost-card {
  position: relative;
  border-radius: 22px;
  background: rgba(246, 243, 255, 0.15);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.32);
  box-shadow:
    0 20px 40px rgba(12, 8, 30, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.22);
  overflow: hidden;
}

.frost-card::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.2),
    rgba(255, 255, 255, 0.02) 45%,
    rgba(255, 255, 255, 0.14)
  );
  pointer-events: none;
}

.frost-head {
  position: relative;
  z-index: 1;
  padding: 1rem 1.2rem 0.7rem;
}

.frost-head h2 {
  margin: 0;
  font-family: "Cormorant Garamond", "Cinzel", "Times New Roman", Georgia, "Noto Serif SC", serif;
  font-size: 1.12rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: rgba(246, 241, 255, 0.95);
  text-transform: uppercase;
}

.frost-body {
  position: relative;
  z-index: 1;
  padding: 0 1.2rem 1.2rem;
}
</style>

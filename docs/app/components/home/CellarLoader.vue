<template>
  <svg viewBox="0 0 48 48" role="img" aria-label="Loading">
    <circle v-for="slot in slots" :key="`empty-${slot.x}-${slot.y}`" :cx="slot.x" :cy="slot.y" r="4.4" class="empty" />
    <g v-for="(slot, index) in slots" :key="`bottle-${slot.x}-${slot.y}`" class="bottle" :style="{ animationDelay: `${index * 140}ms` }">
      <circle :cx="slot.x" :cy="slot.y" r="4.6" :class="colors[slot.color]" />
      <circle :cx="slot.x" :cy="slot.y" r="1.4" class="punt" />
    </g>
  </svg>
</template>

<script setup lang="ts">
  // The loader of the A+ design: the cellar fills up bottle by bottle, stays full, then empties
  const slots = [
    { x: 9, y: 36.98, color: 4 }, { x: 19, y: 36.98, color: 5 }, { x: 29, y: 36.98, color: 1 }, { x: 39, y: 36.98, color: 3 },
    { x: 14, y: 28.32, color: 3 }, { x: 24, y: 28.32, color: 2 }, { x: 34, y: 28.32, color: 5 },
    { x: 19, y: 19.66, color: 1 }, { x: 29, y: 19.66, color: 4 },
    { x: 24, y: 11, color: 0 },
  ]

  const colors = [
    'fill-cognac-500 dark:fill-cognac-400',
    'fill-rose-700 dark:fill-rose-400',
    'fill-pink-600 dark:fill-pink-400',
    'fill-blue-700 dark:fill-blue-400',
    'fill-lime-700 dark:fill-lime-400',
    'fill-purple-700 dark:fill-purple-300',
  ]
</script>

<style scoped>
  .empty { fill: none; stroke: var(--ui-border); stroke-width: 1.2; }
  .punt { fill: var(--ui-bg); }

  .bottle {
    transform-box: fill-box;
    transform-origin: center;
    opacity: 0;
    animation: pour 3.2s ease-out infinite;
  }

  @keyframes pour {
    0% { opacity: 0; transform: translateY(-30%) scale(.5); }
    12% { opacity: 1; transform: translateY(0) scale(1.1); }
    18%, 70% { opacity: 1; transform: scale(1); }
    85%, 100% { opacity: 0; transform: scale(.6); }
  }

  @media (prefers-reduced-motion: reduce) {
    .bottle { animation: none; opacity: 1; }
  }
</style>

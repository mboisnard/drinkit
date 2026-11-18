<template>
  <main>
    <h1>{{ problem.status }} {{ problem.title }}</h1>
    <p v-if="problem.detail">
      {{ problem.detail }}
    </p>
    <button
      type="button"
      @click="goHome"
    >
      Back to the home page
    </button>
  </main>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { clearError } from '#app';
  import type { NuxtError } from '#app';
  import { toProblemDetail } from './problem-detail';

  const props = defineProps<{ error: NuxtError }>();

  const problem = computed(() => toProblemDetail(props.error));

  async function goHome(): Promise<void> {
    await clearError({ redirect: '/' });
  }
</script>

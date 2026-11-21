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
      {{ t('nuxt.error.backHome') }}
    </button>
  </main>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { clearError } from '#app';
  import type { NuxtError } from '#app';
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import { toProblemDetail } from './problem-detail';
  import { useLocalePath } from '#i18n';

  const props = defineProps<{ error: NuxtError }>();

  const { t } = useTranslate();
  const localePath = useLocalePath();
  const problem = computed(() => toProblemDetail(props.error));

  async function goHome(): Promise<void> {
    await clearError({ redirect: localePath('/') });
  }
</script>

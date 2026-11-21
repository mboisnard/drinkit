<template>
  <UApp>
    <UError
      :error="{ status: problem.status, statusText: title, message: problem.detail }"
      :redirect="localePath('/')"
    />
  </UApp>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { useSeoMeta } from '#app';
  import type { NuxtError } from '#app';
  import { UApp, UError } from '#components';
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import { toProblemDetail } from '@drinkit/nuxt-starter/problem-detail';
  import { useLocalePath } from '#i18n';

  const props = defineProps<{ error: NuxtError }>();

  const { t, exists } = useTranslate();
  const localePath = useLocalePath();
  const problem = computed(() => toProblemDetail(props.error));

  // The page Nuxt does not find has an English title of its own, which the catalog replaces
  const title = computed(() => {
    const key = `error.status.${String(problem.value.status)}`;

    return exists(key) ? t(key) : (problem.value.title ?? t('error.status.default'));
  });

  useSeoMeta({ title: () => [problem.value.status, title.value].filter(Boolean).join(' ') });
</script>

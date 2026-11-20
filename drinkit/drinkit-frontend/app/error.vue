<template>
  <UApp>
    <UError :error="{ status: problem.status, statusText: problem.title, message: problem.detail }" />
  </UApp>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { useSeoMeta } from '#app';
  import type { NuxtError } from '#app';
  import { UApp, UError } from '#components';
  import { toProblemDetail } from '@drinkit/nuxt-starter/problem-detail';

  const props = defineProps<{ error: NuxtError }>();

  const problem = computed(() => toProblemDetail(props.error));

  useSeoMeta({ title: () => [problem.value.status, problem.value.title].filter(Boolean).join(' ') });
</script>

<template>
  <UApp>
    <div
      v-if="isLoading"
      role="progressbar"
      aria-label="Loading the page"
      class="fixed top-0 left-0 z-50 h-0.5 bg-primary transition-[width]"
      :style="{ width: `${progress}%` }"
    />
    <NuxtRouteAnnouncer />
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>

<script setup lang="ts">
  import { useLoadingIndicator } from '#app';
  import { NuxtLayout, NuxtPage, NuxtRouteAnnouncer, UApp } from '#components';

  // NuxtLoadingIndicator stays in the page while hidden, and has no name a screen reader could announce
  const { isLoading, progress } = useLoadingIndicator();
</script>

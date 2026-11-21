<template>
  <UApp>
    <div
      v-if="isLoading"
      role="progressbar"
      :aria-label="t('layout.loading')"
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
  import { useHead, useLoadingIndicator } from '#app';
  import { NuxtLayout, NuxtPage, NuxtRouteAnnouncer, UApp } from '#components';
  import { useTranslate } from '@drinkit/i18n-starter/translate';

  const { t } = useTranslate();

  // NuxtLoadingIndicator stays in the page while hidden, and has no name a screen reader could announce
  const { isLoading, progress } = useLoadingIndicator();

  useHead({ titleTemplate: (page) => (page ? t('layout.pageTitle', { page }) : t('home.title')) });
</script>

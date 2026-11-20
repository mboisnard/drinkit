<template>
  <UApp>
    <NuxtRouteAnnouncer />
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>

<script setup lang="ts">
  import { NuxtLayout, NuxtPage, NuxtRouteAnnouncer, UApp } from '#components';
</script>

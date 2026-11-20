<template>
  <div>
    <NuxtRouteAnnouncer />
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>

<script setup lang="ts">
  import { NuxtLayout, NuxtPage, NuxtRouteAnnouncer } from '#components';
</script>

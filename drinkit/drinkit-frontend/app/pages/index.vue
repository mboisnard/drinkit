<template>
  <main>
    <h1>DrinkIt</h1>
    <p>Your wine and spirit cellars, in one place.</p>
  </main>
</template>

<script setup lang="ts">
  import { useSeoMeta } from '#app';

  useSeoMeta({
    title: 'DrinkIt',
    description: 'Manage your wine and spirit cellars: what you hold, where it lies and when to open it.',
  });
</script>

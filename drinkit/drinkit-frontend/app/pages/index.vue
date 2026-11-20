<template>
  <main>
    <h1>DrinkIt</h1>
    <p v-if="greeting">
      {{ greeting }}
    </p>
    <p>Your wine and spirit cellars, in one place.</p>
  </main>
</template>

<script setup lang="ts">
  import { useSeoMeta } from '#app';
  import { definePageMeta } from '#imports';
  import { useGreeting } from '../composables/useGreeting';

  definePageMeta({ auth: 'public' });

  useSeoMeta({
    title: 'DrinkIt',
    description: 'Manage your wine and spirit cellars: what you hold, where it lies and when to open it.',
  });

  const { data: greeting, error } = await useGreeting();
  if (error.value) {
    throw error.value;
  }
</script>

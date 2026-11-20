<template>
  <main>
    <h1>My cellars</h1>
    <CellarList :cellars="cellars ?? []" />
  </main>
</template>

<script setup lang="ts">
  import { useSeoMeta } from '#app';
  import { definePageMeta } from '#imports';
  import CellarList from '../components/CellarList.vue';
  import { useCellars } from '../composables/useCellars';

  definePageMeta({ auth: 'ROLE_USER', layout: 'signed-in' });
  useSeoMeta({ title: 'My cellars · DrinkIt' });

  const { data: cellars, error } = await useCellars();
  if (error.value) {
    throw error.value;
  }
</script>

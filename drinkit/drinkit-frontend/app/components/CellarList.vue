<template>
  <ul v-if="cellars.length > 0">
    <li
      v-for="cellar in cellars"
      :key="cellar.id"
    >
      {{ cellar.name }}, {{ cellar.location.city }}
    </li>
  </ul>
  <p v-else>
    No cellar yet.
  </p>
</template>

<script setup lang="ts">
  import type { CellarResponse } from '#api';

  defineProps<{ cellars: CellarResponse[] }>();
</script>

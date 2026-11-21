<template>
  <h1>{{ t('cellar.welcome', { cellarName: 'La Cave' }) }}</h1>
</template>

<script setup lang="ts">
  import { useFixtureTranslate } from '../fixture-translate.ts';

  const t = useFixtureTranslate();
</script>

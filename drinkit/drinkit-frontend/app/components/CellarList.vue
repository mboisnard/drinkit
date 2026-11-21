<template>
  <ul v-if="cellars.length > 0">
    <li
      v-for="cellar in cellars"
      :key="cellar.id"
    >
      {{ t('cellars.item', { cellarName: cellar.name, city: cellar.location.city }) }}
    </li>
  </ul>
  <p v-else>
    {{ t('cellars.empty') }}
  </p>
</template>

<script setup lang="ts">
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import type { CellarResponse } from '#api';

  defineProps<{ cellars: CellarResponse[] }>();

  const { t } = useTranslate();
</script>

<template>
  <ul v-if="cellars.length > 0">
    <li
      v-for="cellar in cellars"
      :key="cellar.id"
    >
      {{ t('cellars.item', { cellarName: cellar.name, city: cellar.location.city }) }}
      <button
        type="button"
        :aria-label="t('cellars.deleteLabel', { cellarName: cellar.name })"
        :disabled="deleting"
        @click="emit('delete', cellar.id)"
      >
        {{ t('cellars.delete') }}
      </button>
    </li>
  </ul>
  <p v-else>
    {{ t('cellars.empty') }}
  </p>
</template>

<script setup lang="ts">
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import type { CellarResponse } from '#api';

  defineProps<{ cellars: CellarResponse[]; deleting: boolean }>();
  const emit = defineEmits<{ delete: [cellarId: string] }>();

  const { t } = useTranslate();
</script>

<template>
  <main>
    <h1>{{ t('cellars.title') }}</h1>
    <CellarList
      :cellars="cellars ?? []"
      :deleting="pending"
      @delete="remove"
    />
    <p
      v-if="problem"
      role="alert"
    >
      {{ problem }}
    </p>
  </main>
</template>

<script setup lang="ts">
  import { useSeoMeta } from '#app';
  import { definePageMeta } from '#imports';
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import CellarList from '../components/CellarList.vue';
  import { useCellarDeletion } from '../composables/useCellarDeletion';
  import { useCellars } from '../composables/useCellars';

  definePageMeta({ auth: 'ROLE_USER', layout: 'signed-in' });

  const { t } = useTranslate();
  useSeoMeta({ title: () => t('cellars.title') });

  const { data: cellars, error, refresh } = await useCellars();
  if (error.value) {
    throw error.value;
  }

  const { pending, problem, remove } = useCellarDeletion(refresh);
</script>

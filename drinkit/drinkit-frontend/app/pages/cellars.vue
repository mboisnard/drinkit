<template>
  <main>
    <h1>{{ title }}</h1>
    <p v-if="showsCount">
      {{ t('cellars.count', { count: cellars?.length ?? 0 }) }}
    </p>
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
  import { computed } from 'vue';
  import { useSeoMeta } from '#app';
  import { definePageMeta } from '#imports';
  import { useFlag } from '@drinkit/feature-management-starter/use-flag';
  import { useVariant } from '@drinkit/feature-management-starter/use-variant';
  import { useTranslate } from '@drinkit/i18n-starter/translate';
  import CellarList from '../components/CellarList.vue';
  import { useCellarDeletion } from '../composables/useCellarDeletion';
  import { useCellars } from '../composables/useCellars';

  definePageMeta({ auth: 'ROLE_USER', layout: 'signed-in' });

  const { t } = useTranslate();
  const showsCount = useFlag('cellar-count');
  const heading = useVariant('cellars-heading');
  const title = computed(() => t(heading.value === 'welcome' ? 'cellars.welcomeTitle' : 'cellars.title'));
  useSeoMeta({ title });

  const { data: cellars, error, refresh } = await useCellars();
  if (error.value) {
    throw error.value;
  }

  const { pending, problem, remove } = useCellarDeletion(refresh);
</script>

<template>
  <!-- Docus's aside, with only the current folder open and the records under a Decisions heading. The menu opens its
       groups when it mounts, so it mounts again when the tab changes -->
  <UContentNavigation
    :key="records.length ? 'decisions' : 'folders'"
    :default-open="records.length ? undefined : true"
    highlight
    variant="link"
    :navigation="navigation"
  />
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { UContentNavigation } from '#components';
  import { useAppConfig, useSubNavigation } from '#imports';
  import type { ContentNavigationItem } from '@nuxt/content';

  const { sidebarNavigation } = useSubNavigation();

  const appConfig = useAppConfig();

  const items = computed<ContentNavigationItem[]>(() => sidebarNavigation.value.map((item) => typeof item.tile === 'string'
    ? { ...item, ui: { linkLeadingIcon: `size-6 rounded-md p-1 ${appConfig.tiles[item.tile] ?? ''}` } }
    : item));
  const records = computed(() => items.value.filter(isRecord));

  // The records sit beside the page about them, which keeps their routes short: the menu gathers them under a heading
  const navigation = computed(() => {
    const [firstRecord] = records.value;
    if (firstRecord === undefined) {
      return items.value;
    }

    const heading = {
      title: 'Decisions',
      path: firstRecord.path.replace(/[^/]+$/, 'records'),
      ui: { linkTitle: 'font-mono text-xs uppercase tracking-wider text-muted' },
      children: records.value,
    };
    return [...items.value.filter((item) => !isRecord(item)), heading];
  });

  // A record's menu title starts with its number, as the template of new-adr writes it
  function isRecord(item: ContentNavigationItem): boolean {
    return /^\d{3} · /.test(item.title);
  }
</script>

<template>
  <div
    v-if="folder?.icon"
    class="flex items-center gap-3"
  >
    <span :class="['inline-flex size-10 shrink-0 items-center justify-center rounded-lg', tile]">
      <UIcon
        :name="folder.icon"
        class="size-5"
      />
    </span>

    <span class="flex flex-col gap-0.5">
      <span class="font-mono text-xs uppercase tracking-wider text-muted">{{ label }}</span>
      <span :class="['text-sm font-semibold', ink]">{{ record ?? folder.title }}</span>
    </span>
  </div>
</template>

<script setup lang="ts">
  import { computed, inject } from 'vue';
  import type { Ref } from 'vue';
  import { UIcon } from '#components';
  import { useAppConfig } from '#imports';
  import type { ContentNavigationItem } from '@nuxt/content';

  type Folder = ContentNavigationItem & { icon?: string; step?: number; tile?: string };

  const props = defineProps<{ path: string; record?: string }>();

  const navigation = inject<Ref<ContentNavigationItem[]>>('navigation');
  const appConfig = useAppConfig();

  const trail = computed(() => findTrail(navigation?.value ?? [], props.path) ?? []);
  const folder = computed(() => trail.value.findLast((item): item is Folder => Boolean(item.icon)));

  const tile = computed(() => appConfig.tiles[folder.value?.tile ?? ''] ?? '');
  const ink = computed(() => tile.value.split(' ').filter((c) => c.includes('text-')).join(' '));

  const label = computed(() => {
    if (props.record) return 'Decision record';
    if (folder.value?.step) return `Step ${twoDigits(folder.value.step)} of ${twoDigits(lastStep(navigation?.value ?? []))}`;
    return trail.value[0]?.title;
  });

  function findTrail(items: ContentNavigationItem[], path: string): ContentNavigationItem[] | undefined {
    for (const item of items) {
      if (!item.children?.length) {
        if (item.path === path) return [];
        continue;
      }

      const below = findTrail(item.children, path);
      if (below) return [item, ...below];
    }
  }

  function lastStep(items: ContentNavigationItem[]): number {
    return Math.max(0, ...items.map((item) => Math.max(Number(item.step ?? 0), lastStep(item.children ?? []))));
  }

  function twoDigits(value: number) {
    return String(value).padStart(2, '0');
  }
</script>

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
  import type { ContentNavigationItem } from '@nuxt/content'

  const { sidebarNavigation } = useSubNavigation()

  const appConfig = useAppConfig()

  // A record's menu title starts with its number, as the template of new-adr writes it
  const isRecord = (item: ContentNavigationItem) => /^\d{3} · /.test(item.title)

  const items = computed<ContentNavigationItem[]>(() => (sidebarNavigation.value ?? []).map(item => item.tile
    ? { ...item, ui: { linkLeadingIcon: `size-6 rounded-md p-1 ${appConfig.tiles[item.tile as string]}` } }
    : item))
  const records = computed(() => items.value.filter(isRecord))

  // The records sit beside the page about them, which keeps their routes short: the menu gathers them under a heading
  const navigation = computed(() => {
    if (!records.value.length) return items.value

    const heading = {
      title: 'Decisions',
      path: records.value[0]!.path.replace(/[^/]+$/, 'records'),
      ui: { linkTitle: 'font-mono text-xs uppercase tracking-wider text-muted' },
      children: records.value,
    }
    return [...items.value.filter(item => !isRecord(item)), heading]
  })
</script>

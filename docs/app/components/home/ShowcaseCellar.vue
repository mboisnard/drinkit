<template>
  <div class="flex flex-col gap-3.5">
    <div class="flex items-center justify-between gap-3">
      <div>
        <p class="font-display text-lg font-bold">
          Living room cellar
        </p>
        <p class="text-sm text-muted">
          48 bottles · shared with Tom
        </p>
      </div>
      <UAvatarGroup size="sm">
        <UAvatar text="M" />
        <UAvatar text="T" />
      </UAvatarGroup>
    </div>

    <div class="flex flex-wrap items-center gap-2">
      <UButton
        v-for="item in filters"
        :key="item.label"
        size="xs"
        color="neutral"
        :variant="filter === item.kind ? 'solid' : 'soft'"
        @click="pick(item.kind)"
      >
        {{ item.label }} · {{ item.count }}
      </UButton>
      <UBadge v-if="!tried" variant="soft" icon="i-lucide-pointer">
        Try a filter
      </UBadge>
    </div>

    <ul class="divide-y divide-default border-y border-default">
      <li v-for="bottle in shown" :key="bottle.name" class="flex items-center gap-3 py-1.5">
        <HomeDrinkTile :icon="bottle.icon" :tile="bottle.tile" />
        <span class="flex-1">
          <span class="block text-sm font-semibold">{{ bottle.name }}</span>
          <span class="block text-xs text-muted">{{ bottle.detail }}</span>
        </span>
        <span class="font-mono text-xs text-muted">×{{ bottle.count }}</span>
      </li>
    </ul>

    <p class="text-xs text-muted">
      Open a bottle: photos, tasting notes, the moments it was shared.
    </p>
  </div>
</template>

<script setup lang="ts">
  const bottles = [
    { name: 'Château Musar', detail: 'Red · 2016 · Bekaa Valley', kind: 'wine', icon: 'i-lucide-wine', tile: 'rose', count: 3 },
    { name: 'Huet Le Mont', detail: 'White · 2019 · Vouvray', kind: 'wine', icon: 'i-lucide-wine', tile: 'lime', count: 2 },
    { name: 'Yamazaki 12', detail: 'Whisky · Japan', kind: 'bar', icon: 'i-lucide-glass-water', tile: 'orange', count: 1 },
    { name: 'Monkey 47', detail: 'Gin · Black Forest', kind: 'bar', icon: 'i-lucide-martini', tile: 'cyan', count: 1 },
    { name: 'Orval', detail: 'Beer · Trappist · Belgium', kind: 'beer', icon: 'i-lucide-beer', tile: 'yellow', count: 6 },
  ]

  const filters = [
    { label: 'All', count: 48, kind: undefined },
    { label: 'My Wines', count: 31, kind: 'wine' },
    { label: 'My Bar', count: 12, kind: 'bar' },
    { label: 'Beers', count: 5, kind: 'beer' },
  ]

  const filter = ref<string | undefined>()
  const tried = ref(false)
  const shown = computed(() => bottles.filter(bottle => !filter.value || bottle.kind === filter.value))

  function pick(kind: string | undefined) {
    filter.value = kind
    tried.value = true
  }
</script>

<template>
  <div class="flex flex-col gap-4">
    <div class="rounded-xl border border-primary/30 bg-primary/5 p-4">
      <p class="text-xs font-semibold uppercase tracking-wider text-primary">
        Planned · Sat, Dec 6
      </p>
      <div class="mt-2 flex items-center gap-3">
        <HomeDrinkTile
          icon="i-lucide-wine"
          tile="rose"
        />
        <span>
          <span class="block font-display text-lg font-bold">Tom's 40th</span>
          <span class="block text-sm text-muted">Château Musar 2016 · Living room cellar</span>
        </span>
      </div>

      <div class="mt-4 flex flex-col gap-3">
        <USwitch
          v-model="reminder"
          label="Email me a reminder on Dec 5"
        />
        <USwitch
          v-model="offer"
          label="Offer the bottle to Tom"
        />
        <p
          v-if="offer"
          class="text-sm text-muted"
        >
          Tom will find it in his cellar after the evening.
        </p>
      </div>
    </div>

    <UBadge
      v-if="!tried"
      variant="soft"
      icon="i-lucide-pointer"
      class="self-start"
    >
      Flip a switch
    </UBadge>

    <ol class="flex flex-col gap-3 border-s border-default ps-4">
      <li
        v-for="moment in past"
        :key="moment.title"
      >
        <p class="font-mono text-xs text-muted">
          {{ moment.date }}
        </p>
        <p class="text-sm font-medium">
          {{ moment.title }}
        </p>
        <p class="text-xs text-muted">
          {{ moment.bottle }}
        </p>
      </li>
    </ol>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { HomeDrinkTile, UBadge, USwitch } from '#components';

  const reminder = ref(true);
  const offer = ref(false);
  const tried = ref(false);

  watch([reminder, offer], () => tried.value = true);

  const past = [
    { date: 'Oct 12', title: 'Harvest dinner', bottle: 'Domaine Tempier Bandol 2019' },
    { date: 'Sep 3', title: 'Back from Islay', bottle: 'Lagavulin 16' },
  ];
</script>

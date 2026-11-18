<template>
  <div class="flex flex-col gap-4">
    <div class="scan-photo relative flex h-40 items-center justify-center overflow-hidden rounded-2xl">
      <div class="scan-label flex w-36 -rotate-2 flex-col gap-0.5 rounded px-2.5 py-3 text-center">
        <span class="text-[9px] uppercase tracking-[.18em]">Grand vin du Liban</span>
        <span class="font-accent text-base font-medium italic">Château Musar</span>
        <span class="text-[9px] uppercase tracking-[.18em]">2016 · Bekaa Valley</span>
      </div>

      <div class="absolute inset-y-4 left-1/2 w-52 -translate-x-1/2">
        <span class="absolute left-0 top-0 size-5 rounded-tl-lg border-l-3 border-t-3 border-primary-500" />
        <span class="absolute right-0 top-0 size-5 rounded-tr-lg border-r-3 border-t-3 border-primary-500" />
        <span class="absolute bottom-0 left-0 size-5 rounded-bl-lg border-b-3 border-l-3 border-primary-500" />
        <span class="absolute bottom-0 right-0 size-5 rounded-br-lg border-b-3 border-r-3 border-primary-500" />
        <span class="scan-beam absolute inset-x-1 h-0.5 rounded-full bg-primary-500" />
      </div>
    </div>

    <div class="flex items-center gap-3">
      <HomeDrinkTile
        icon="i-lucide-wine"
        tile="rose"
      />
      <span class="flex-1">
        <span class="block text-sm font-semibold">Château Musar 2016</span>
        <span class="block text-xs text-muted">Red · Bekaa Valley, Lebanon</span>
      </span>
      <UBadge
        variant="soft"
        size="sm"
      >
        Read from the label
      </UBadge>
    </div>

    <p class="flex items-center gap-2 text-sm text-muted">
      <UIcon
        name="i-lucide-trending-up"
        class="size-4 text-success"
      />
      <span><span class="font-semibold text-highlighted">€42</span> today · bought €38 in 2021</span>
    </p>

    <div class="flex flex-wrap items-center gap-2">
      <UButton
        :color="added ? 'success' : 'primary'"
        :icon="added ? 'i-lucide-check' : 'i-lucide-plus'"
        @click="added = true"
      >
        {{ added ? 'Added · ×4 now' : 'Add to Living room cellar' }}
      </UButton>
      <UButton
        color="neutral"
        variant="outline"
        icon="i-lucide-download"
      >
        Import from Vivino
      </UButton>
      <UBadge
        v-if="!added"
        variant="soft"
        icon="i-lucide-pointer"
      >
        Click Add
      </UBadge>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { HomeDrinkTile, UBadge, UButton, UIcon } from '#components';

  const added = ref(false);
</script>

<style scoped>
  .scan-photo {
    background: linear-gradient(160deg, #4a1f2a, #171717 70%);
  }

  .scan-label {
    background: #f6efe3;
    color: #4a1f2a;
  }

  .scan-beam {
    box-shadow: 0 0 12px 2px rgb(224 129 44 / 70%);
    animation: beam 2.4s ease-in-out infinite alternate;
  }

  @keyframes beam {
    from { top: 0.5rem; }
    to { top: calc(100% - 0.75rem); }
  }

  @media (prefers-reduced-motion: reduce) {
    .scan-beam {
      animation: none;
      top: 50%;
    }
  }
</style>

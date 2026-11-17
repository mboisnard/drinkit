<template>
  <div
    class="flex w-full max-w-130 flex-col gap-3"
    @pointerenter="paused = true"
    @pointerleave="paused = false"
    @focusin="paused = true"
    @focusout="paused = false"
    @pointerdown="stop"
    @keydown="stop"
  >
    <UTabs
      v-model="active"
      :items="features"
      :content="false"
      size="sm"
      class="self-start"
      :ui="{ list: 'rounded-full', indicator: 'rounded-full', trigger: 'rounded-full data-[state=inactive]:[&>span:last-child]:sr-only' }"
    />

    <div class="relative overflow-hidden rounded-3xl border border-default bg-default shadow-[0_24px_48px_-16px_rgba(23,23,23,.18)]">
      <span
        v-if="playing"
        :key="active"
        class="showcase-progress absolute inset-x-0 top-0 h-0.5 origin-left bg-primary"
        :class="{ paused }"
        :style="{ animationDuration: `${seconds}s` }"
        @animationend="next"
      />

      <div class="flex items-center gap-3 border-b border-default px-4 py-3">
        <img :src="favicon" alt="" class="size-6">
        <span class="font-display text-lg font-extrabold tracking-tight">DrinkIt</span>
        <span class="ms-auto hidden flex-1 items-center gap-2 rounded-full bg-elevated px-3 py-1.5 text-xs text-muted sm:flex">
          <UIcon name="i-lucide-search" class="size-3.5" />
          Search bottles, cocktails, moments
        </span>
        <UAvatar text="M" size="sm" class="ms-auto sm:ms-0" />
      </div>

      <div class="h-110 overflow-y-auto p-5">
        <UTheme :ui="{ badge: { base: 'rounded-full' }, button: { base: 'rounded-full' } }">
          <Transition name="panel" mode="out-in">
            <HomeShowcaseCellar v-if="active === 'cellar'" />
            <HomeShowcaseScan v-else-if="active === 'scan'" />
            <HomeShowcaseMoments v-else-if="active === 'moments'" />
            <HomeShowcaseCocktails v-else-if="active === 'cocktails'" />
            <HomeShowcaseAssistant v-else :autoplay="playing" />
          </Transition>
        </UTheme>
      </div>
    </div>

    <p class="text-sm text-muted">
      Sample data. Pick a feature, or let it play.
    </p>
  </div>
</template>

<script setup lang="ts">
  const features = [
    { label: 'Cellar', icon: 'i-lucide-wine', value: 'cellar' },
    { label: 'Scan', icon: 'i-lucide-scan-line', value: 'scan' },
    { label: 'Moments', icon: 'i-lucide-calendar-heart', value: 'moments' },
    { label: 'Cocktails', icon: 'i-lucide-martini', value: 'cocktails' },
    { label: 'Assistant', icon: 'i-lucide-sparkles', value: 'assistant', seconds: 9 },
  ]

  const favicon = `${useRuntimeConfig().app.baseURL}favicon.svg`

  const active = ref('cellar')
  const paused = ref(false)
  const playing = ref(true)

  // The assistant answers its first question a few seconds in, and needs the time to be read
  const seconds = computed(() => features.find(feature => feature.value === active.value)?.seconds ?? 6)

  // The tour moves on when the progress bar fills, which the style keeps still under reduced motion and on touch
  function next() {
    const index = features.findIndex(feature => feature.value === active.value)
    active.value = features[(index + 1) % features.length]!.value
  }

  function stop() {
    playing.value = false
  }
</script>

<style scoped>
  .showcase-progress {
    animation: fill linear forwards;
  }

  .showcase-progress.paused {
    animation-play-state: paused;
  }

  .panel-enter-active,
  .panel-leave-active {
    transition: opacity 0.2s ease, transform 0.2s ease;
  }

  .panel-enter-from {
    opacity: 0;
    transform: translateX(12px);
  }

  .panel-leave-to {
    opacity: 0;
    transform: translateX(-12px);
  }

  @media (prefers-reduced-motion: reduce), (hover: none) {
    .showcase-progress {
      display: none;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .panel-enter-active,
    .panel-leave-active {
      transition: none;
    }
  }

  @keyframes fill {
    from { transform: scaleX(0); }
    to { transform: scaleX(1); }
  }
</style>

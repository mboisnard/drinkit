<template>
  <UTheme :ui="{ badge: { base: 'rounded-full' }, button: { base: 'rounded-full' } }">
    <div class="mt-4 flex flex-wrap items-center gap-2">
      <template v-if="facts.status">
        <UBadge :color="statusColor" variant="subtle" :icon="facts.status === 'accepted' ? 'i-lucide-circle-check' : undefined" class="capitalize" :label="facts.status" />
        <UBadge v-if="facts.date" color="neutral" variant="outline" :label="day(facts.date)" />
        <UButton
          v-if="facts.explainedIn"
          :to="facts.explainedIn.to"
          :label="`Explained in ${facts.explainedIn.title}`"
          size="xs"
          color="neutral"
          variant="outline"
        />
        <UButton v-if="facts.supersedes" :to="facts.supersedes" label="Supersedes an earlier record" size="xs" color="neutral" variant="outline" />
        <UButton v-if="facts.supersededBy" :to="facts.supersededBy" label="Superseded by a later record" size="xs" color="warning" variant="outline" />
      </template>

      <template v-else>
        <UBadge v-if="facts.readingTime" color="neutral" variant="outline" :label="`${facts.readingTime} min read`" />
        <UBadge v-if="facts.updatedAt" color="neutral" variant="outline" :label="`Updated ${day(facts.updatedAt)}`" />
      </template>
    </div>
  </UTheme>
</template>

<script setup lang="ts">
  export type Facts = {
    status?: string
    date?: string
    explainedIn?: { title: string, to: string }
    supersedes?: string
    supersededBy?: string
    readingTime?: number
    updatedAt?: string
  }

  const props = defineProps<{ facts: Facts }>()

  const statusColors: Record<string, 'success' | 'warning'> = { accepted: 'success', proposed: 'warning' }
  const statusColor = computed(() => statusColors[props.facts.status ?? ''] ?? 'neutral')

  function day(value: string) {
    return new Date(value).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric', timeZone: 'UTC' })
  }
</script>

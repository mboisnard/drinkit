<template>
  <ProseH2 v-bind="$attrs">
    <span
      v-if="icon"
      :class="['inline-flex size-8 shrink-0 items-center justify-center rounded-md align-middle me-3', appConfig.tiles[icon.tile]]"
    >
      <UIcon :name="icon.icon" class="size-4.5" />
    </span>
    <template v-else-if="emoji">{{ emoji }} </template>
    <slot />
  </ProseH2>
</template>

<script setup lang="ts">
  import ProseH2 from '@nuxt/ui/components/prose/H2.vue'

  const props = defineProps<{ emoji?: string }>()

  const appConfig = useAppConfig()
  const icon = computed(() => props.emoji ? appConfig.headingIcons[props.emoji] : undefined)
</script>

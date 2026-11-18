<template>
  <UiProseH2 v-bind="$attrs">
    <span
      v-if="icon"
      :class="['inline-flex size-8 shrink-0 items-center justify-center rounded-md align-middle me-3', tile]"
    >
      <UIcon
        :name="icon.icon"
        class="size-4.5"
      />
    </span>
    <template v-else-if="emoji">
      {{ emoji }}
    </template>
    <slot />
  </UiProseH2>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { UIcon } from '#components';
  import { useAppConfig } from '#imports';
  import UiProseH2 from '#ui/components/prose/H2.vue';

  const props = defineProps<{ emoji?: string }>();

  const appConfig = useAppConfig();
  const icon = computed(() => props.emoji === undefined ? undefined : appConfig.headingIcons[props.emoji]);
  const tile = computed(() => icon.value === undefined ? undefined : appConfig.tiles[icon.value.tile]);
</script>

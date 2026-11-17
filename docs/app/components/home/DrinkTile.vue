<template>
  <span :class="['inline-flex shrink-0 items-center justify-center rounded-lg', small ? 'size-8' : 'size-9', appConfig.tiles[tile]]">
    <UIcon :name="icon" :class="small ? 'size-4' : 'size-5'" />
  </span>
</template>

<script setup lang="ts">
  defineProps<{ icon: string, tile: string, small?: boolean }>()

  const appConfig = useAppConfig()
</script>

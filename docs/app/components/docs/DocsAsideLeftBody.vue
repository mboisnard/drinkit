<template>
  <!-- Docus's aside, but with the folders closed except the one of the current page, and each one able to open -->
  <UContentNavigation
    default-open
    :highlight="contentNavVariants.highlight ?? true"
    :highlight-color="contentNavVariants.highlightColor"
    :variant="contentNavVariants.variant ?? 'link'"
    :color="contentNavVariants.color"
    :navigation="sidebarNavigation"
  />
</template>

<script setup lang="ts">
  const { sidebarNavigation } = useSubNavigation()

  const contentNavVariants = useUIConfig('contentNavigation')
</script>

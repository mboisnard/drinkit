<template>
  <!-- Docus's bottom of the table of contents, which holds the copy menu of the page instead of the links and the
       assistant this site leaves out: in the header, the menu pushed a long title onto two lines -->
  <div class="space-y-6">
    <USeparator type="dashed" />

    <DocsPageHeaderLinks />
  </div>
</template>

<script setup lang="ts">
  import { DocsPageHeaderLinks, USeparator } from '#components';
</script>

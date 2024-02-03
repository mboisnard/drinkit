<template>
  <div>
    <svg id="radar"></svg>
  </div>
</template>
<script setup lang="ts">
  import { onMounted } from 'vue';

  interface TechRadarEntry {
    quadrant: Number,
    ring: Number,
    label: String,
    active: Boolean,
    moved: Number,
  }

  const width = 1000;
  const height = 1000;

  const props = defineProps({
    title: String,
    //entries: TechRadarEntry,
  })

  onMounted(() => {
    const techRadarScript = document.createElement('script');
    techRadarScript.setAttribute('src', 'https://opensource.zalando.com/tech-radar/radar.js');
    document.head.appendChild(techRadarScript)
  })

</script>
<style>
  body {
    font-family: 'Source Sans Pro', arial, helvetica, sans-serif;
    padding-bottom: 50px;
  }
  h3 {
    margin-top: 50px;
  }
  li {
    margin: 25px 50px 0 0;
  }
  table {
    width: 1400px;
    margin: 0 50px 0 50px;
  }
  td {
    width: 50%;
    vertical-align: top;
    padding-right: 60px;
  }
</style>
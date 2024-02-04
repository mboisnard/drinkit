---
sidebar: false
aside: false
---

<script setup>
    import TechRadar from '../../../components/TechRadar.vue'
</script>

[Back to index][index]

<TechRadar 
    title="Platform Tech Radar" 
    :quadrants="['Languages & Frameworks', 'Tools', 'Databases', 'Techniques']"
    :entries='[
        { "quadrant": "Techniques", "ring": "ADOPT", "label": "Contract first Apis" },
        { "quadrant": "Techniques", "ring": "ADOPT", "label": "Contract first approach" },
    ]'
/>

<style>
    .VPContent div .container {
        margin: 0 !important;
        display: block !important;
    }
</style>

[index]: what-is-it
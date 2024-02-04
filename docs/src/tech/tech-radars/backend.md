---
sidebar: false
---

<script setup>
    import TechRadar from '../../../components/TechRadar.vue'
</script>

[Back to index][index]

<TechRadar
    title="Backend Tech Radar"
    :quadrants="['Languages & Frameworks', 'Tools', 'Databases', 'Techniques']"
    :entries='[
    { "quadrant": "Techniques", "ring": "ADOPT", "label": "Contract first Apis" },
    { "quadrant": "Techniques", "ring": "ADOPT", "label": "Contract first approach" },
    ]'
/>


<style>
    div .container {
        margin: 0 !important;
    }
</style>

[index]: what-is-it
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

[index]: what-is-it
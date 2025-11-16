# Frontend Tech Radar

<script setup>
    import TechRadar from '../../../components/TechRadar.vue'
</script>

<TechRadar 
    title="Frontend Tech Radar"
    :quadrants="['Languages & Frameworks', 'Tools', 'Databases', 'Techniques']"
    :entries='[
    { "quadrant": "Techniques", "ring": "ADOPT", "label": "Contract first Apis" },
    { "quadrant": "Techniques", "ring": "ADOPT", "label": "Contract first approach" },
    ]'
/>
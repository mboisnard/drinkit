# Frontend Tech Radar

<script setup>
    import TechRadar from '../../../components/TechRadar.vue'
</script>

<TechRadar
    title="Frontend Tech Radar"
    :quadrants="['Languages & Frameworks', 'Tools', 'Libraries', 'Techniques']"
    :entries='[
    {
      "quadrant": "Languages & Frameworks",
      "ring": "ADOPT",
      "label": "Nuxt",
      "description": "The framework of the web application",
      "link": "https://nuxt.com/"
    },
    {
      "quadrant": "Languages & Frameworks",
      "ring": "ADOPT",
      "label": "Vue",
      "description": "The component framework under Nuxt",
      "link": "https://vuejs.org/"
    },
    {
      "quadrant": "Languages & Frameworks",
      "ring": "ADOPT",
      "label": "TypeScript",
      "description": "The language of the web application",
      "link": "https://www.typescriptlang.org/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "OpenAPI Generator",
      "description": "Generates the TypeScript client of the API from its contract",
      "link": "https://openapi-generator.tech/docs/generators/typescript-fetch/"
    },
    {
      "quadrant": "Libraries",
      "ring": "TRIAL",
      "label": "PrimeVue",
      "description": "UI components of the first screens",
      "link": "https://primevue.org/"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Contract First APIs",
      "description": "The client is generated from the same OpenAPI contract as the backend",
      "link": "/drinkit/engineering/architecture/contract-first.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ASSESS",
      "label": "Nuxt Layers as Tech Starters",
      "description": "One technical concern per layer that the app extends, such as the API client or monitoring",
      "link": "/drinkit/engineering/architecture/tech-starters.html#frontend-starters"
    },
    ]'
/>

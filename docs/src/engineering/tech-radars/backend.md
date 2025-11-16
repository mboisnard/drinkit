# Backend Tech Radar

<script setup>
    import TechRadar from '../../../components/TechRadar.vue'
</script>

<TechRadar
    :quadrants="['Languages & Frameworks', 'Tools', 'Databases & Middlewares', 'Techniques']"
    :entries='[
    {
      "quadrant": "Languages & Frameworks",
      "ring": "ADOPT",
      "label": "Kotlin",
      "description": "Our primary backend language for type-safe, concise code",
      "link": "https://kotlinlang.org/"
    },
    {
      "quadrant": "Languages & Frameworks",
      "ring": "ADOPT",
      "label": "Spring Boot",
      "description": "Framework for building production-ready applications",
      "link": "https://spring.io/projects/spring-boot"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Contract First APIs",
      "description": "Define OpenAPI contracts before implementation for better API design",
      "link": "https://swagger.io/specification/"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Hexagonal Architecture",
      "description": "Clean architecture pattern separating business logic from infrastructure",
      "link": "https://alistair.cockburn.us/hexagonal-architecture/"
    },
    {
      "quadrant": "Techniques",
      "ring": "TRIAL",
      "label": "Event Sourcing",
      "description": "Store state changes as events for audit and temporal queries",
      "link": "https://martinfowler.com/eaaDev/EventSourcing.html",
      "moved": 1
    },
    {
      "quadrant": "Databases & Middlewares",
      "ring": "ADOPT",
      "label": "PostgreSQL",
      "description": "Primary relational database for transactional data",
      "link": "https://www.postgresql.org/"
    },
    {
      "quadrant": "Databases & Middlewares",
      "ring": "TRIAL",
      "label": "Redis",
      "description": "In-memory cache and session store",
      "link": "https://redis.io/"
    },
    {
        "quadrant": "Databases & Middlewares",
        "ring": "ADOPT",
        "label": "Meilisearch",
        "description": "Lightning-fast, typo-tolerant search engine for full-text search",
        "link": "https://www.meilisearch.com/"
    },
    {
        "quadrant": "Databases & Middlewares",
        "ring": "ADOPT",
        "label": "RabbitMQ",
        "description": "Reliable message broker for asynchronous communication and event-driven architecture",
        "link": "https://www.rabbitmq.com/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Gradle",
      "description": "Build tool with Kotlin DSL for better IDE support",
      "link": "https://gradle.org/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "jOOQ",
      "description": "Type-safe SQL builder for database access",
      "link": "https://www.jooq.org/"
    },
    {
        "quadrant": "Tools",
        "ring": "ADOPT",
        "label": "Feign",
        "description": "Declarative HTTP client for Spring with minimal boilerplate",
        "link": "https://spring.io/projects/spring-cloud-openfeign"
    },
    {
        "quadrant": "Tools",
        "ring": "ADOPT",
        "label": "Kotest",
        "description": "Powerful Kotlin testing framework with expressive assertions and property-based testing",
        "link": "https://kotest.io/"
    },
    {
        "quadrant": "Tools",
        "ring": "ADOPT",
        "label": "Testcontainers",
        "description": "Docker containers for integration tests with real dependencies",
        "link": "https://testcontainers.com/"
    },
    {
      "quadrant": "Tools",
      "ring": "ASSESS",
      "label": "Spring AI",
      "description": "Integration layer for AI/ML services",
      "link": "https://spring.io/projects/spring-ai",
      "moved": 1
    }
    ]'
/>
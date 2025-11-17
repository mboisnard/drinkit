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
      "quadrant": "Languages & Frameworks",
      "ring": "ASSESS",
      "label": "Spring AI",
      "description": "Integration layer for AI/ML services",
      "link": "https://spring.io/projects/spring-ai",
      "moved": 1
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
      "ring": "ADOPT",
      "label": "SOLID Principles",
      "description": "Five fundamental object-oriented design principles for maintainable and scalable code",
      "link": "/drinkit/engineering/guidelines/best-practices/solid.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Living Documentation",
      "description": "Documentation that evolves with the code and stays up-to-date automatically",
      "link": "/drinkit/engineering/guidelines/best-practices/living-documentation.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Strong Typing",
      "description": "Leverage type systems to catch errors at compile time and improve code safety",
      "link": "/drinkit/engineering/guidelines/best-practices/coding-rules.html#strong-typing-for-clarity-and-safety"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Scout Rule",
      "description": "Always leave the code better than you found it - continuous incremental improvement",
      "link": "/drinkit/engineering/guidelines/best-practices/scout-rule.html"
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
      "quadrant": "Techniques",
      "ring": "ASSESS",
      "label": "Embeddings",
      "description": "Vector representations of data for semantic search and AI applications",
      "link": "https://docs.spring.io/spring-ai/reference/api/embeddings.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ASSESS",
      "label": "OCR",
      "description": "Optical Character Recognition for extracting text from images and documents",
      "link": "https://github.com/tesseract-ocr/tesseract"
    },
    {
      "quadrant": "Techniques",
      "ring": "ASSESS",
      "label": "LLM Integration",
      "description": "Large Language Model integration for AI-powered features - testing in this project",
      "link": "https://docs.spring.io/spring-ai/reference/api/chatclient.html"
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
      "label": "Gradle Conventions",
      "description": "Modular convention plugins to maintain consistency across Gradle modules",
      "link": "/drinkit/engineering/guidelines/build-tool.html#gradle-conventions"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Gradle Platform",
      "description": "Internal BOM for consistent dependency version management across modules",
      "link": "/drinkit/engineering/guidelines/build-tool.html#internal-dependency-management-bom"
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
    }
    ]'
/>
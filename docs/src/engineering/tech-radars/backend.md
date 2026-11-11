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
      "description": "The language of the backend, its build scripts and its tests",
      "link": "https://kotlinlang.org/"
    },
    {
      "quadrant": "Languages & Frameworks",
      "ring": "ADOPT",
      "label": "Spring Boot",
      "description": "The application framework, extended by our own tech starters",
      "link": "https://spring.io/projects/spring-boot"
    },
    {
      "quadrant": "Languages & Frameworks",
      "ring": "ADOPT",
      "label": "Spring Security",
      "description": "Sessions, login and access rules of the API",
      "link": "https://spring.io/projects/spring-security"
    },
    {
      "quadrant": "Languages & Frameworks",
      "ring": "ADOPT",
      "label": "HTTP Interface Clients",
      "description": "Spring interfaces annotated with the calls they make, used for every outgoing HTTP client",
      "link": "https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface"
    },
    {
      "quadrant": "Languages & Frameworks",
      "ring": "ASSESS",
      "label": "Spring AI",
      "description": "Integration layer for LLM and embedding services, in the platform but not used by the code yet",
      "link": "https://spring.io/projects/spring-ai"
    },
    {
      "quadrant": "Languages & Frameworks",
      "ring": "TRIAL",
      "label": "OpenFeature",
      "description": "Feature flags and A/B tests behind a vendor-neutral API, served to clients over OFREP, not used by a feature yet",
      "link": "https://openfeature.dev/",
      "moved": 1
    },
    {
      "quadrant": "Languages & Frameworks",
      "ring": "HOLD",
      "label": "Feign",
      "description": "Replaced by Spring HTTP interface clients when Spring Cloud left the platform",
      "link": "https://spring.io/projects/spring-cloud-openfeign",
      "moved": -1
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Gradle Conventions",
      "description": "Convention plugins that configure every module of a kind the same way",
      "link": "/drinkit/engineering/build-and-data/build-tool.html#convention-plugins"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Gradle Platform",
      "description": "One internal BOM and two version catalogs for every version of the build",
      "link": "/drinkit/engineering/build-and-data/build-tool.html#versions-in-one-place"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "jOOQ",
      "description": "Type-safe SQL in Kotlin, from classes generated from the schema",
      "link": "https://www.jooq.org/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Liquibase",
      "description": "Applies the schema changelogs and records which ones each database received",
      "link": "/drinkit/engineering/build-and-data/database.html"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Squawk",
      "description": "Lints the SQL of every changelog for locks and statements that could stall users",
      "link": "https://squawkhq.com/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "detekt",
      "description": "Static analysis and formatting of Kotlin, failing the build on any finding",
      "link": "https://detekt.dev/"
    },
    {
      "quadrant": "Tools",
      "ring": "TRIAL",
      "label": "Konture",
      "description": "Architecture rules written as tests against the Kotlin sources, a young library",
      "link": "/drinkit/engineering/adr/0004-check-the-architecture-with-tests.html"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "OpenAPI Generator",
      "description": "Generates the Spring interfaces and models of the API from its contract",
      "link": "https://openapi-generator.tech/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "KSP",
      "description": "Kotlin Symbol Processing, which writes the generated pages of this site",
      "link": "/drinkit/engineering/writing-code/best-practices/living-documentation.html"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Kotest",
      "description": "Assertion library of every test",
      "link": "https://kotest.io/"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Testcontainers",
      "description": "Real PostgreSQL and services in Docker for the integration tests",
      "link": "/drinkit/engineering/writing-code/testing/testcontainers.html"
    },
    {
      "quadrant": "Tools",
      "ring": "ADOPT",
      "label": "Mailpit",
      "description": "SMTP server that catches the emails of a local start and of the tests, and shows them",
      "link": "/drinkit/engineering/adr/0010-send-emails-over-smtp-to-a-server-chosen-per-environment.html"
    },
    {
      "quadrant": "Tools",
      "ring": "ASSESS",
      "label": "GraalVM Native Image",
      "description": "Native build of the application, configured but not built in CI",
      "link": "https://www.graalvm.org/latest/reference-manual/native-image/"
    },
    {
      "quadrant": "Databases & Middlewares",
      "ring": "ADOPT",
      "label": "PostgreSQL",
      "description": "The database of the application",
      "link": "https://www.postgresql.org/"
    },
    {
      "quadrant": "Databases & Middlewares",
      "ring": "ADOPT",
      "label": "Spring Application Events",
      "description": "Carry the platform events of the application, asynchronously and inside the process",
      "link": "/drinkit/engineering/architecture/platform-events.html"
    },
    {
      "quadrant": "Databases & Middlewares",
      "ring": "TRIAL",
      "label": "Meilisearch",
      "description": "Search engine wired by a tech starter, not used by a feature yet",
      "link": "https://www.meilisearch.com/",
      "moved": -1
    },
    {
      "quadrant": "Databases & Middlewares",
      "ring": "TRIAL",
      "label": "Flipt",
      "description": "Feature flag provider of OpenFeature, its state in git per environment, not used by a feature yet",
      "link": "https://www.flipt.io/",
      "moved": 1
    },
    {
      "quadrant": "Tools",
      "ring": "ASSESS",
      "label": "PostHog",
      "description": "Planned tool that picks the variants of A/B tests and measures them, not wired yet",
      "link": "https://posthog.com/"
    },
    {
      "quadrant": "Databases & Middlewares",
      "ring": "ASSESS",
      "label": "RabbitMQ",
      "description": "Message broker, not used today: platform events stay inside the process",
      "link": "https://www.rabbitmq.com/",
      "moved": -1
    },
    {
      "quadrant": "Databases & Middlewares",
      "ring": "ASSESS",
      "label": "Redis",
      "description": "In-memory cache and session store, not used by the application",
      "link": "https://redis.io/",
      "moved": -1
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Hexagonal Architecture",
      "description": "Business code at the center, adapters around it, dependencies pointing inward",
      "link": "/drinkit/engineering/architecture/hexagonal-architecture.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Modular Monolith",
      "description": "One deployable split into bounded contexts with no cycle between them",
      "link": "/drinkit/engineering/architecture/modular-monolith.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Tech Starters",
      "description": "One technical concern per library, ready to use, with no business code",
      "link": "/drinkit/engineering/architecture/tech-starters.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Contract First APIs",
      "description": "The OpenAPI contract is written first, and both sides are generated from it",
      "link": "/drinkit/engineering/architecture/contract-first.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Architecture Tests",
      "description": "Every architecture rule is a test that fails the build",
      "link": "/drinkit/engineering/writing-code/testing/architecture-tests.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Test-Driven Development",
      "description": "Tests first, in red, green and refactor cycles",
      "link": "https://martinfowler.com/bliki/TestDrivenDevelopment.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Hand-Written Test Doubles",
      "description": "In-memory doubles checked by the same test contract as the real adapter, never mocks",
      "link": "/drinkit/engineering/writing-code/testing/testdouble.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Safe Database Migrations",
      "description": "Idempotent changelogs, bounded locks and expand and contract steps",
      "link": "/drinkit/engineering/build-and-data/database.html#writing-safe-migrations"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "SOLID Principles",
      "description": "Five design principles for code that stays easy to change",
      "link": "/drinkit/engineering/writing-code/best-practices/solid.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Strong Typing",
      "description": "Business values are types, never primitives, and check themselves",
      "link": "/drinkit/engineering/writing-code/best-practices/coding-rules.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Scout Rule",
      "description": "Leave the code a little better than you found it",
      "link": "/drinkit/engineering/writing-code/best-practices/scout-rule.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ADOPT",
      "label": "Living Documentation",
      "description": "Pages generated from the code, so they change with it",
      "link": "/drinkit/engineering/writing-code/best-practices/living-documentation.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "TRIAL",
      "label": "Event Sourcing",
      "description": "The user aggregate is stored as its events, other aggregates still as their state",
      "link": "/drinkit/engineering/architecture/event-sourcing.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "TRIAL",
      "label": "Functional Core, Imperative Shell",
      "description": "Decisions in pure functions, input and output in the use case around them",
      "link": "https://www.destroyallsoftware.com/screencasts/catalog/functional-core-imperative-shell"
    },
    {
      "quadrant": "Techniques",
      "ring": "HOLD",
      "label": "Tesseract OCR",
      "description": "Replaced by vision language models served outside the application",
      "link": "/drinkit/engineering/adr/0021-serve-ai-models-outside-the-application-and-reach-them-through-ai-starter.html",
      "moved": -1
    },
    {
      "quadrant": "Techniques",
      "ring": "ASSESS",
      "label": "Embeddings",
      "description": "Vector representations of data for semantic search",
      "link": "https://docs.spring.io/spring-ai/reference/api/embeddings.html"
    },
    {
      "quadrant": "Techniques",
      "ring": "ASSESS",
      "label": "LLM Integration",
      "description": "Language and vision models through Spring AI, served by a frontier API or LocalAI, not used by a feature yet",
      "link": "/drinkit/engineering/adr/0021-serve-ai-models-outside-the-application-and-reach-them-through-ai-starter.html"
    },
    ]'
/>

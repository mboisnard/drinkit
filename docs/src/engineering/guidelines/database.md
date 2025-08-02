# Database


Doc ideas:
- Liquibase to easily manage the sql migration
- Liquibase outside application to restrict application roles/grant on database
- Idempotency required for all changes (IF NOT EXISTS, ...)
- Dedicated schema (avoid "public" schema usage)
- Jooq
- Why commiting the jooq generated files?
- Integration Testing with TestContainers
- Use Jooq Schemas from generated code instead of @Sql annotation with sql file instructions
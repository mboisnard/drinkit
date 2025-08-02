# Database

This page explains how we use **PostgreSQL** database with **jOOQ** for code generation and **Liquibase** for managing migrations in this project.
This approach ensures reliable deployments, safe migrations, and a smooth integration between the database and the application code.

## 🐘 Liquibase

Liquibase is an essential tool for tracking, managing, and applying database schema changes in a platform-independent way. It enables versioned, reproducible, and team-friendly migrations.

### 📜 Structure of Liquibase Scripts

Each SQL migration script must follow a specific format to be correctly interpreted by Liquibase.

1.  **Liquibase Header**: The script must begin with the following line:
    ```sql
    --liquibase formatted sql
    ```

2.  **Changeset Description**: Immediately after, you must define the changeset with a unique identifier and a description. The `logicalFilePath` must be fixed to prevent errors if the file is ever moved.
    ```sql
    --changeset dev-team:00012-my-migration logicalFilePath:fixed
    ```

### 🧰 Useful Changeset Options

Several options can be added to the `--changeset` line to control its behavior:

-   `logicalFilePath:fixed`: **Mandatory**. Prevents Liquibase from complaining if you move the changeset file.
-   `runInTransaction:false`: Necessary for operations that cannot run inside a transaction, such as creating an index with the `CONCURRENTLY` option.
-   `splitStatements:false`: Essential when using `DO` blocks with custom delimiters (e.g., `$do$`).
-   `runOnChange:true`: Used for scripts that should be re-executed if their content changes (e.g., views, stored functions).
-   `--validCheckSum: ANY`: Added on a separate line, it prevents Liquibase from raising an error if the file's checksum has changed. Useful in specific cases, like updating views via custom mechanisms.
-   `labels:local`: Allows you to run a script only in a specific environment (e.g., `local` for development).

### 🔒Security: Liquibase Outside the Application

To enhance security, it is recommended to run Liquibase via an external process (CI/CD, dedicated script) with elevated privileges, while the application connects to the database using a role with restricted permissions (read/write on its tables, but no administrative rights like `CREATE`, `ALTER`, `DROP`).

## ✅ Schema Best Practices

To avoid conflicts and keep your database well-organized, it is crucial not to use the default `public` schema.

-   **Use a Dedicated Schema**: Create a specific schema for your application, such as `drinkit_application`. This isolates your database objects and simplifies permissions management.

## 🛡️Writing Safe Migrations

### The Golden Rule: Idempotency 🔄

The fundamental rule is that **all creation and addition operations must be idempotent**.
An idempotent operation can be executed multiple times without changing the result beyond its initial application.

To achieve this, use SQL constructs that check for existence before creating or modifying:
-   `CREATE TABLE IF NOT EXISTS ...`
-   `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...`
-   `CREATE INDEX IF NOT EXISTS ...`

Idempotency is required for all changes to ensure that migrations can be re-applied without causing errors.

### Handling Complex Schema Changes (Without Locking) ⚠️

Certain database operations can lock tables for a long time (`ACCESS EXCLUSIVE lock`), which is unacceptable in production. Here is how to handle the most common cases.

#### Creating/Dropping Indexes 🔍

A standard index creation or deletion rewrites the table and locks it.

-   **Creation**: Use `CREATE INDEX CONCURRENTLY`. This operation is slower but does not block writes to the table. It requires being run outside a transaction (`runInTransaction:false`).
-   **Deletion**: Use `DROP INDEX CONCURRENTLY`.
-   **Tip**: Before dropping a column (`DROP COLUMN`), it is better to first drop any associated indexes using `DROP INDEX CONCURRENTLY` to minimize the duration of the `ACCESS EXCLUSIVE` lock on the table.

#### NOT NULL Constraint 🚫

Adding a `NOT NULL` constraint on a large table locks it while it scans all rows. To avoid this, proceed in three steps (in separate transactions/changesets):

1.  **Add a non-validated `CHECK` constraint**: It is added instantly because the database does not check existing data.
    ```sql
    ALTER TABLE my_table ADD CONSTRAINT my_column_not_null CHECK (my_column IS NOT NULL) NOT VALID;
    ```
2.  **Manually verify** that there are no longer any `NULL` values in the column.
3.  **Validate the constraint**: This operation only requires a light lock to update metadata.
    ```sql
    ALTER TABLE my_table VALIDATE CONSTRAINT my_column_not_null;
    ```

#### Changing a Column Type ↔️

In most cases, changing a column's type rewrites the entire table. A safer approach is as follows:
1.  Create a **new column** with the desired type.
2.  Use a **trigger** to copy and synchronize data from the old column to the new one during `INSERT` and `UPDATE` operations.
3.  Update the **application code** to use the new column.
4.  Once the code is deployed and stable, drop the old column and the trigger in a subsequent migration.

::: tip
More best practices [here](https://medium.com/paypal-tech/postgresql-at-scale-database-schema-changes-without-downtime-20d3749ed680)
:::

## ✨ jOOQ Code Generation

jOOQ generates Java/Kotlin classes from your database schema. This provides a type-safe DSL (Domain Specific Language) for writing elegant and safe SQL queries directly in your code.

### Why Commit Generated Code? 🚀

We commit the jOOQ-generated files to our code repository. The main reason is to **simplify the Continuous Integration (CI) pipeline**. Without this, the CI would have to:
1.  Start a PostgreSQL container using TestContainer.
2.  Run all Liquibase migrations.
3.  Generate the jOOQ code from this newly created database.
4.  Compile and test the application.

Committing the code avoids this overhead and complexity in the CI process.

## 🧪 Integration Testing

To ensure our queries work as expected, we use integration tests that connect to a real database.

-   **TestContainers** 🐳: This tool starts a PostgreSQL Docker container for each test suite. The database connection is then configured to point to this container.
-   **Using jOOQ Schemas**: Instead of using annotations like `@Sql` to load test data from SQL files, we can directly use the jOOQ-generated schemas to insert the necessary data for the test.

Here is a configuration example with a custom annotation in Kotlin:
```kotlin
// Usage in a test class
@JooqIntegrationTest(schemas = [DrinkitApplication::class])
class MyJooqRepositoryTest {
    // ... tests ...
}
```
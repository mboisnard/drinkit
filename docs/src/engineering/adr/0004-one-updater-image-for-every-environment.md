# 0004. One updater image for every environment

- Status: accepted
- Date: 2026-10-29
- Issue: [#403](https://github.com/mboisnard/drinkit/issues/403)

The updater, the Spring Boot application that applies DrinkIt's Liquibase changelogs to PostgreSQL, ships as one
image on GHCR that holds no changelog, and each environment supplies its own. A local start mounts them from the
repository, and a deployment will copy them into an image built from this one: every environment runs the same
updater, and no developer builds it.

## Context

The backend's `dev` profile starts the services of `deployment/local/compose.yml`, but a new database stayed
empty: someone had to run `UpdaterApplication` by hand, on the first start and after each pull that brought a
changelog. Liquibase stays out of the backend, whose production role cannot change the schema, as the
[database page](../build-and-data/database.md#liquibase) explains. The maintainer wants the updater to become the
one program that maintains the infrastructure: the schema today, message queues and feature flags later.

These facts, from Spring Boot 4.1.1, Liquibase 5.0.3 and Docker Compose 5.6 in October 2026, shaped the decision:

- Spring Boot starts compose with `docker compose up --wait`, which fails when a container exits, even with code
  0, unless another service waits for it to complete
  ([docker/compose#10596](https://github.com/docker/compose/issues/10596)).
- Through Spring, Liquibase's `includeAll` fails on a changelog given as a `file:` path
  ([liquibase/liquibase#6978](https://github.com/liquibase/liquibase/issues/6978)), and works on the classpath.
- The image runs in about 2 seconds when there is nothing to apply, measured on Docker Desktop.

## Decision

The image `ghcr.io/mboisnard/drinkit-updater` holds the updater and no changelog, so a new changelog never needs a
new image. It runs on distroless Java 25 as a non-root user, with no default credentials, and reads the
changelogs from the classpath under `/workspace`. `SPRING_LIQUIBASE_CHANGE_LOG` picks the changelog to run.

- **Locally**, compose mounts the changelogs read-only and runs the image as a one-shot service on every start of
  the backend, so a pulled changelog applies at the next start. A second service waits for it to complete, as
  `--wait` requires. The second pull request of #403 adds both services.
- **A deployment** will build an image `FROM` this one with the changelogs copied in, one per commit, so that its
  digest names the exact SQL. It runs before the backend of the same commit, and the same image moves from
  integration to production. Each environment provides a role that owns the schema, since a changelog cannot
  create the role that runs it.
- **Master builds the image** when a push that passed the gate ran the Updater lane, reproducibly, and publishes it
  only when its digest changed and CI ran it, as [container images](../delivery/container-images.md) describes.

The backend never runs Liquibase.

## How it is checked

- The Updater [lane](../delivery/ci-cd.md#lanes) builds the image and runs it twice against a new database: the
  second run must apply nothing.
- `Deploy updater` runs the same test and a Trivy scan on the built image, on amd64 and arm64, before it pushes it.
- The [changelog rules and Squawk](../build-and-data/database.md#checked-rules) keep checking the changelogs the
  image reads.

## Consequences

- Every environment runs an updater that CI ran, built once.
- Each start of the backend will run the updater, about 2 seconds more.
- A change to the updater's code reaches compose after its publication and a digest bump. Until then, its
  developer runs `UpdaterApplication` from the IDE.
- The package is made public once, so that compose pulls it without a login, and GitHub cannot make it private
  again.
- One job on master can write packages. It builds and checks nothing: Gradle, BuildKit, Trivy and the image under
  test run in jobs that hold no package token.
- The minor and patch releases of the workflow's actions and the new digests of its images merge themselves, as
  everywhere in the CI, so a compromised one would reach the image, or the package token of the publish job.
- A new digest of the base merges itself, as every image digest does, so a CVE fix ships fast but unreviewed. The
  test and Trivy run on it before it is pushed.
- A folder on the classpath rules out the JVM's ahead-of-time cache, which could shorten each start. A Liquibase
  runner that reads the folder directly would lift that limit.
- A CVE that Trivy finds keeps the new image out of the registry, and `latest` on the previous one, until a bump of
  the base or of a library fixes it.
- The image has no shell, so debugging it takes the `:debug` variant of distroless.

## Alternatives

- Liquibase in the backend, as a development-only dependency: no image to publish, but the local backend would get
  rights it never has in production, and the updater could not grow to queues and feature flags.
- Compose building the image from the Dockerfile: no registry, but the first start of every clone would build the
  updater with Gradle, minutes before the backend starts.
- The official Liquibase image: it no longer ships the PostgreSQL driver, and runs none of DrinkIt's code.
- The changelogs inside the image: one image per changelog, and every local change would wait for CI.
- The changelogs mounted from a checkout in deployments: a Kubernetes job has no checkout, a ConfigMap stops at
  1 MiB, and running the updater from a GitHub runner means exposing the database.
- A script or an IntelliJ "Before launch" task: the schema would follow only that way of starting the backend.
- A Temurin JRE base: a shell, and 111 MiB against 69. A `jlink` runtime: about 25 MiB less, for one build per
  platform.
- `bootBuildImage`: one platform per build, and a memory calculator that runs before each start.

## Sources

- [Distroless container images](https://github.com/GoogleContainerTools/distroless), Google
- [Efficient container images](https://docs.spring.io/spring-boot/reference/packaging/container-images/efficient-images.html), Spring Boot

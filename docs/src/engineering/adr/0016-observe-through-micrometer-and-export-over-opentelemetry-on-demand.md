# 0016. Observe through Micrometer, and export over OpenTelemetry on demand

- Status: accepted
- Date: 2026-11-07

DrinkIt observes itself with what Spring Boot already ships: Actuator for health, Micrometer for metrics and traces,
its structured logging for JSON lines. An environment exports over OpenTelemetry when it switches it on, and
nothing leaves the application before. The code depends on no vendor, and a developer's machine needs no collector.

## Context

The `observability-starter`, called `monitoring-starter` before, only exposed the health and the info of Actuator.
A few starters reported the health of their third party, such as Meilisearch, but no rule said which. Nothing linked
a log to the request that wrote it, and a stack trace became one event per line on a log platform such as Datadog. DrinkIt has no production yet,
and its future platform is not chosen.

Spring Boot 4.1.2 brings each piece: health indicators that each starter contributes, the Observation API of
Micrometer that gives a timer and a span in one call, an OpenTelemetry starter that pushes them over OTLP, and JSON
logs behind one property. Its OTLP export of metrics is on by default, towards `localhost:4318`.

## Decision

- **Health.** Each starter that talks to a third party brings its `HealthIndicator`, as the search engine starter
  does. Readiness waits for the database alone. Every Actuator endpoint is read-only and, except the health status,
  reserved to admins.
- **Metrics and traces.** The code measures through Micrometer, an `ObservationRegistry` or a `MeterRegistry`, never
  through the OpenTelemetry API or a wrapper of our own. The starter exports over OTLP once
  `starters.observability.opentelemetry.enabled` is true, through the JDK HTTP client, and never while
  `starters.observability.enabled` is false. Until then, tracing still gives
  each request its `traceId`, which reaches no collector. A trace starts in the application: the `traceparent` that a
  caller sends is ignored, so that no one on the internet chooses a `traceId` or forces a trace into the samples.
- **Logs.** Plain text on a developer's machine, one JSON line per event in the Logstash format elsewhere. Each line
  carries `traceId`, `spanId` and, once signed in, `user.id`. Logs go to the console, never over OTLP.
- **Errors.** Every error of the API, 4xx or 5xx, answers a problem detail of RFC 9457 that carries the `traceId` of
  the request, so that a client reads every error the same way and support finds its logs. One exception handler of the starter
  answers them all: a filter hands its refusal to Spring MVC rather than writing a body.

The [logging page](../build-and-data/logging.md) and the README of the starter give the settings.

## Consequences

- A metric, a span or a health check of a new starter is a few lines, with no module of our own to learn.
- Changing platform, Datadog, Grafana or another, means setting an OTLP endpoint, not changing code.
- A user who reports an error gives a `traceId`, and one search finds the logs of that request.
- The OpenTelemetry SDK, about twenty jars, is in every application, even where nothing is exported.
- Spring Boot creates a span for every request, sampled or not: a cost in memory that a measure may one day question.
- The `traceId` and `user.id` live in the MDC of the request's thread: a platform event handled on another thread
  logs without them.
- A log platform that cannot read JSON from the console, or a team that wants logs over OTLP, needs a Logback appender
  of OpenTelemetry, which Spring Boot does not install.

## Alternatives

- **A metrics service of our own** over Micrometer. Rejected because Micrometer already hides the vendor, and the
  wrapper would be one more API to keep up. It offered one place to name and tag every metric.
- **The OpenTelemetry Java agent.** Rejected because it instruments the bytecode at start-up, which a native image
  cannot do, and duplicates what Spring Boot observes. It offered spans for libraries that Micrometer does not cover.
- **The Datadog agent and its tracer**, as many companies run. Rejected because it ties the code and the deployment
  to one vendor before one is chosen. It offered a complete platform at once.
- **Health and info only, as before.** Rejected because a failing request could not be traced back to its logs. It
  added no dependency.

## Sources

- [OpenTelemetry with Spring Boot](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/), Spring
- [Observability](https://docs.spring.io/spring-boot/reference/actuator/observability.html), Spring Boot reference
- [Kubernetes probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes), Spring Boot reference, on keeping external checks out of liveness
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html), IETF
- [Error Handling Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Error_Handling_Cheat_Sheet.html), OWASP, on a generic message for an unexpected error

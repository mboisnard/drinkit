# Logging

The code logs through [kotlin-logging](https://github.com/oshai/kotlin-logging) on Logback, the logger of Spring
Boot, with `private val logger = KotlinLogging.logger { }` in each class. The `observability-starter` sets the
format: plain text on a developer's machine, one JSON object per line everywhere else. Read this page before you add
a log or deploy an environment.

## 🧾 Plain text locally, JSON elsewhere

A log platform such as Datadog reads a line of plain text as one event: a stack trace of forty lines becomes forty
events. A JSON line keeps the whole event, its level and its fields together. A person reads plain text faster, so
the developer's machine keeps it.

An environment turns JSON on with one variable, read by the
[structured logging](https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.structured)
of Spring Boot:

```
LOGGING_STRUCTURED_FORMAT_CONSOLE=logstash
```

We use the Logstash format: Datadog maps its `level`, `message` and `logger_name` fields without a remapper, which the
`log.level` of the ECS format needs. Each line also carries `application`, the `spring.application.name`.

## 🔗 Fields of every line

Each key of the MDC (the map of values that Logback attaches to every log of a thread) becomes a field of the JSON
line. Three keys are set for each request:

- `traceId` and `spanId`, by Micrometer Tracing, which the plain text also shows after the level. Every line logged
  during one request shares its `traceId`, even when the export to OpenTelemetry is off.
- `user.id`, by the `security-starter`, once the caller is signed in.

A problem detail, the body of a 400, 401 or 403 that Spring or the `security-starter` writes, carries the same
`traceId`. A user who reports an error gives it, and the logs of that request come up in one search. The MDC lives
on its thread: work handed to another thread, such as a platform event, starts without these keys.

## 🙈 What never reaches a log

No email, sign-in code, token, cookie or session id: the
[security principles](../security/principles.md#security-events-in-the-logs) list the rule. A user appears by their
id only. A SQL query is logged with a `?` for each bind value, never the value.

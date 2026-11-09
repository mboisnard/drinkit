# Mail Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/tech-starters.md)
that sends the application's emails, such as a sign-in code. An adapter injects
`EmailSender`, and the starter queues each email for an SMTP server or, when none is set, drops it with a warning.

```mermaid
flowchart LR
    Adapter[Adapter of the application] --> EmailSender
    EmailSender -->|spring.mail.host set| Queue["Queued as a platform event"]
    Queue --> Smtp[(SMTP server)]
    EmailSender -->|no host| Dropped["Dropped, with a warning"]
```

## How it works

`send` returns once the email is queued, without waiting for the mail server. Inside a transaction, the email is
queued once the transaction commits, and never when it rolls back. The starter delivers through the
[messaging starter](https://github.com/mboisnard/drinkit/blob/master/tech-starters/backend/messaging-starter/README.md),
which comes with it: with `starters.messaging.enabled=false` and a mail server set, the application refuses to start.
The delivery also needs an `Idempotency` bean: the
[idempotency starter](https://github.com/mboisnard/drinkit/blob/master/tech-starters/backend/idempotency-starter/README.md)
with PostgreSQL, or `inMemoryIdempotency()` in tests.

```mermaid
sequenceDiagram
    participant Caller
    participant EmailSender
    participant Delivery
    participant Smtp as SMTP server
    Caller->>EmailSender: send(email)
    EmailSender-->>Caller: returns, the email queued after the commit
    EmailSender->>Delivery: the email, on a thread of its own
    Delivery->>Smtp: the email
    alt the server takes it
        Smtp-->>Delivery: 250
    else the server refuses it, a 4xx or 5xx code or no answer
        Smtp-->>Delivery: refused
        Note over Delivery: WARN with the subject and the failure, then a new attempt after the retry waits
        Note over Delivery: after the last attempt, ERROR naming the delivery, never the content
    end
```

Each email is retried on its own, with the `starters.messaging.retry.*` waits, three attempts in all.
No log line holds the content of an email. The failure of an attempt is logged whole, as the mail server words it:
its message can name an address the server refused.

## Usage

A module declares the starter in its build file:

```kotlin
dependencies {
    implementation(project(":mail-starter"))
}
```

```kotlin
emailSender.send(
    Email(
        sender = Sender("contact@drinkit.com"),
        recipients = Recipients(setOf(Recipient("jane.doe@example.com"))),
        subject = Subject("Your DrinkIt sign-in code"),
        content = Content(TXT, "Your code is 731904"),
    ),
)
```

- **With `spring.mail.host` set**, the email goes to that SMTP server, later and on another thread.
- **Without a host**, the starter warns at startup, then drops each email with a warning that names its subject,
  never its recipients or its content. The application starts without a mail server, and the logs say what it did
  not send.

The `dev` profile of the backend sends to the [Mailpit](https://mailpit.axllent.org/) of the local compose file, whose
web interface on `http://localhost:8025` shows each email.
[ADR 0010](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/adr/0010-send-emails-over-smtp-to-a-server-chosen-per-environment.md)
records the SMTP server of each environment.

## Configuration

| Property | Default | Environment variable |
|---|---|---|
| `starters.mail.enabled` | `true`, `false` registers no `EmailSender` | `STARTERS_MAIL_ENABLED` |
| `spring.mail.host` | None, required in production: without it, emails are dropped | `SPRING_MAIL_HOST` |
| `spring.mail.username` | None, required in production | `SPRING_MAIL_USERNAME` |
| `spring.mail.password` | None, required in production | `SPRING_MAIL_PASSWORD` |
| `spring.mail.port` | `587`, with STARTTLS | `SPRING_MAIL_PORT` |
| `spring.mail.properties.mail.smtp.timeout`, with `connectiontimeout` and `writetimeout` | `5000` milliseconds each | `SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT` |

## Tests

From `testFixtures(project(":mail-starter"))`, `SpyEmailSender` records the emails an adapter sends, and
`@MailpitIntegrationTest` starts [Mailpit](https://mailpit.axllent.org/), an SMTP server in Docker, for a real send.
`Mailpit.refuseRecipients(451)` makes it refuse every recipient with that code, until `acceptAll()` or the next test.

## Limits

- A refusal for good, such as `550` for an unknown recipient, is retried like a temporary one, to the last attempt.
- An email waiting for its delivery or for its next attempt lives in memory: a restart loses it.
- Each email is delivered once per event. An email held past the 5 minute lease of the idempotency starter, by a
  slow mail server, may leave twice.

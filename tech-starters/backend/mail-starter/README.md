# Mail Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/tech-starters.md)
that sends the application's emails. An adapter injects the `EmailSender` interface, and the starter delivers each
email to an SMTP server or, when none is set, writes it to the logs.

## What is it for?

The application sends transactional emails, such as the verification code at registration. The domain never calls
this starter: an adapter of `drinkit-infra` does, as the
[hexagonal architecture](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/hexagonal-architecture.md)
asks.

## Configuration

| Property | Default | Environment variable | Required in production |
|---|---|---|---|
| `spring.mail.host` | None | `SPRING_MAIL_HOST` | Yes |
| `spring.mail.username`, `spring.mail.password` | None | `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Yes |
| `spring.mail.port` | `587` | `SPRING_MAIL_PORT` | No |
| `starters.mail.enabled` | `true` | `STARTERS_MAIL_ENABLED` | No |

The starter also sets `spring.mail.properties.mail.smtp.*` to turn on authentication, STARTTLS and 5 second
timeouts. A property of the application or an environment variable overrides any of these defaults.

Without `spring.mail.host`, the starter sends nothing: it warns at startup, then writes each email to the logs at
`DEBUG`, a level production does not show, so no address or code leaks there. `starters.mail.enabled=false`
switches the starter off: it registers no `EmailSender`.

A failed send is dropped, and logged at `ERROR` with the email's subject and the exception, whose message can hold
an address the server refused.

## Usage

The module of the adapter declares the starter, which then wires itself:

```kotlin
dependencies {
    implementation(project(":mail-starter"))
    testImplementation(testFixtures(project(":mail-starter")))
}
```

```kotlin
emailSender.send(
    Email(
        sender = Sender("contact@drinkit.com"),
        recipients = Recipients(setOf(Recipient("jane.doe@example.com"))),
        subject = Subject("Your verification code"),
        content = Content(TXT, "Your code is 731904"),
    ),
)
```

The test of an adapter checks what it sends with `SpyEmailSender` of the test fixtures, which records the emails.
`@MailpitIntegrationTest` starts [Mailpit](https://mailpit.axllent.org/), an SMTP server in Docker, for the test of a
real send.
[ADR 0006](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/adr/0006-mailpit-as-the-smtp-server-of-the-tests.md)
says why Mailpit.

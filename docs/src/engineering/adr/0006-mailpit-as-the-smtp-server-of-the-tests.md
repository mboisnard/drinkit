# 0006. Mailpit as the SMTP server of the tests

- Status: accepted
- Date: 2026-11-02
- Issue: [#392](https://github.com/mboisnard/drinkit/issues/392)

The tests of `mail-starter`, the [tech starter](../architecture/tech-starters.md) that sends the application's
emails, deliver a real email to Mailpit, an SMTP server started in Docker, then read it back through Mailpit's HTTP
API. They check what a server received, not the `MimeMessage` Spring built before sending it.

## Context

No test checked that an SMTP server accepts the emails of `mail-starter`. The
[Testcontainers](../writing-code/testing/testcontainers.md) page asks for each adapter to be tested against its real
technology, through an annotation that the starter's test fixtures ship. The local environment needs an SMTP server
too, and [#552](https://github.com/mboisnard/drinkit/issues/552) adds one.

## Decision

`mail-starter` ships `@MailpitIntegrationTest` in its test fixtures. Its extension starts Mailpit in a container that
later runs reuse, as [the other extensions do](../writing-code/testing/testcontainers.md#one-container-for-every-run).
The test gets from it a `JavaMailSender` connected to Mailpit, and the last email Mailpit received. The extension
deletes the emails after each test class, so no class reads another's. A use case is still tested in memory with
`SpyEmailSender`, the double of `EmailSender`, the interface the application injects.

## How it is checked

`SpringEmailSenderTest`, the test of the sender `mail-starter` uses when a server is set, delivers an email to
Mailpit in every `./gradlew build`.

## Consequences

- The local environment of #552 can run the same image, with a web page that lists the emails.
- The tests of `mail-starter` need Docker, and pay a container start once per machine.
- One more image tag, which Renovate keeps up to date.

## Alternatives

- A capturing double of `JavaMailSender`: no Docker, but it checks the message Spring builds, never what a server
  accepts.
- GreenMail, an SMTP server inside the test's JVM: no Docker either, but a library to add, and no page that lists the
  emails for the local environment.
- The `testcontainers-mailpit` community module: a ready-made container and client, but it brings Jackson 2 into
  test fixtures that need no JSON. Its auto-configuration also adds a `JavaMailSender` to every Spring test that has
  the module on its classpath, and `mail-starter` sends by SMTP, instead of to the logs, as soon as one exists.

## Sources

- [Mailpit](https://mailpit.axllent.org/docs/), Axllent
- [GreenMail](https://greenmail-mail-test.github.io/greenmail/), GreenMail
- [testcontainers-mailpit](https://github.com/martinellich/testcontainers-mailpit), Simon Martinelli

# 0010. Send emails over SMTP to a server chosen per environment

- Status: accepted
- Date: 2026-11-06

The application sends its emails over plain SMTP, to a server that each environment sets: Mailpit on a local start
and in the tests, Brevo's free plan in production to begin with. Changing provider then means changing settings, not
code, and a local start never spends the production quota or sends a real email.

## Context

DrinkIt sends few emails, all transactional, such as the code that signs a user in, and has no budget for them. A
local start used to write each email to the logs, the sign-in code included, and never ran the path that talks to a
mail server. The emails are plain text today, and will become HTML built from templates of our own.

## Decision

The [mail starter](https://github.com/mboisnard/drinkit/blob/master/tech-starters/backend/mail-starter/README.md)
speaks SMTP through `spring.mail.*`, with no provider's SDK or API. The templates stay in the code: plain text now,
then HTML compiled from MJML. A provider's own templates are not used.

- **Local start and tests.** [Mailpit](https://mailpit.axllent.org/), an SMTP server that keeps every email and
  shows it in a web interface, runs in the local compose file and through Testcontainers, from the same image. It
  delivers nothing and needs no account or key.
- **Production.** Brevo's free plan, which allows 300 emails a day as of November 2026, from an address on DrinkIt's
  domain, with SPF, DKIM and DMARC set on that domain.

## Consequences

- A provider is replaced through `spring.mail.*` alone, and the defaults of the starter, port 587 with STARTTLS and
  authentication, suit any of them.
- Production sends 300 emails a day at most. Past that, a paid plan or another provider is needed.
- Without a provider's API, DrinkIt gets no open tracking and no bounce webhook.
- Rendering the HTML of an email is DrinkIt's work, not the provider's.
- Mailpit adds a container to the local start.
- Without a mail server, the starter drops each email with a warning rather than failing: the application starts,
  and the logs name each email it did not send, never its content.

## Alternatives

- **A personal Gmail account.** Rejected because it sends from a `gmail.com` address only: an email from DrinkIt's
  domain fails DMARC and lands in spam or is refused. It offered 500 emails a day for free.
- **Google Workspace.** Rejected because it is paid per user, for a volume that Brevo's free plan covers. It offered
  Gmail's SMTP on DrinkIt's domain.
- **A provider's API and templates**, such as Mailjet, Resend or Postmark. Rejected because changing provider would
  mean rewriting the sending and the templates. It offered templates edited outside the code, tracking and webhooks.
- **Emails in the logs on a local start.** Rejected because the codes and the personal data of each email reach the
  logs, and the SMTP path never runs before production. It offered a local start with no other container.

## Sources

- [Brevo pricing](https://www.brevo.com/pricing/), Brevo
- [Email sender guidelines](https://support.google.com/a/answer/81126), Google
- [Mailpit](https://mailpit.axllent.org/docs/), Axllent

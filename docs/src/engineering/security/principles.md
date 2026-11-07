# Security principles

The API of DrinkIt has one client: the browser that runs the Nuxt frontend, on the same origin. The session lives
on the server, in PostgreSQL, and the browser holds only its id, in a cookie that no script can read and no other
site can use. The backend refuses by default: an endpoint answers only once a rule says who may call it, so that a
forgotten rule closes an endpoint instead of opening it. Read this page before you add an endpoint, or change a
cookie, a header or the way users sign in.

The [tech starter](../architecture/tech-starters.md) `security-starter` builds all of this from Spring Security and
Spring Session. The backend application that uses it gives it its users and its rules, and
[calling the API](calling-the-api.md) shows what a client sends in return.

## 🗺️ The whole picture

```mermaid
---
title: What crosses each boundary, and which side enforces what
---
flowchart LR
  subgraph browser["Browser"]
    direction TB
    nuxt["Nuxt app<br/>copies the CSRF cookie<br/>into X-XSRF-TOKEN"]
    jar["Cookie jar<br/>__Host-SESSION, __Host-remember-me: HttpOnly<br/>__Host-XSRF-TOKEN: readable by the app"]
    enforced["Enforced by the browser<br/>Secure, SameSite=Lax, the __Host- prefix,<br/>Sec-Fetch-Site on each request,<br/>the security headers of each response"]
    nuxt -. "reads the CSRF cookie only" .-> jar
  end
  subgraph backend["Backend, same origin as the app"]
    direction TB
    starter["security-starter<br/>Fetch Metadata, CSRF token, session,<br/>sign-in by code, remember-me, URL rules"]
    application["Application<br/>EndpointRules, @PreAuthorize,<br/>UserAccounts, SignInCodeSender, controllers"]
    starter --> application
  end
  subgraph postgres["PostgreSQL"]
    direction TB
    session[("spring_session<br/>one row per session")]
    remember[("remember_me_token<br/>token hashes")]
    code[("sign_in_code<br/>code hashes")]
    user[("user<br/>accounts and roles")]
  end
  mail["Mail server"]
  browser -- "HTTPS: cookies, X-XSRF-TOKEN,<br/>Sec-Fetch-Site, JSON" --> starter
  starter -- "Set-Cookie, security headers,<br/>JSON or a problem detail" --> browser
  starter --> session
  starter --> remember
  starter --> code
  application -- "UserAccounts adapter" --> user
  application -- "the code, after the answer" --> mail
```

The browser holds random ids and the CSRF token, and what they grant lives in PostgreSQL. The browser's defenses keep
other sites away from a user, but hold only in a browser: curl sends any cookie and any header. The backend's checks
hold for every client, so none of them relies on the browser alone.

## 🎯 Threats and their defenses

```mermaid
---
title: Each threat and the defenses that stop it
---
flowchart LR
  forged["Forged request from another site"]
  xss["Injected script reads the session"]
  fixation["Session id planted before the sign-in"]
  stolenSession["Session cookie stolen"]
  stolenRemember["Remember-me cookie stolen"]
  guessed["Sign-in code guessed"]
  dumped["Sign-in code read in the database"]
  enumerated["Accounts found by their email"]
  flooded["Code emails sent in bulk"]
  framed["Another site frames or reads a response"]
  sniffed["JSON run as a script"]
  unruled["Endpoint added without a rule"]
  broad["URL rule too broad for one method"]
  logs["Secrets read in the logs"]
  subgraph browserSide["Enforced by the browser"]
    samesite["SameSite=Lax"]
    httponly["HttpOnly"]
    transport["Secure, HSTS, the __Host- prefix"]
    csp["CSP default-src 'none'"]
    nosniff["X-Content-Type-Options: nosniff"]
    origin["frame-ancestors 'none', X-Frame-Options,<br/>CORP and COOP same-origin, no CORS"]
  end
  subgraph backendSide["Checked by the backend"]
    token["CSRF token in X-XSRF-TOKEN"]
    fetch["Fetch Metadata"]
    newId["New session id at sign-in"]
    lifetime["256-bit ids, 30 min idle, 8 h at most,<br/>Sessions.revokeAll"]
    rotation["Token rotated at each use,<br/>replay detected, hashes only"]
    codeLimits["Code used once, 10 min,<br/>3 wrong tries per email and hour,<br/>SHA-256 hash only"]
    sameAnswer["Same 204 for any email,<br/>one 401 for every failure"]
    rateLimit["Rate limiting per email and IP,<br/>not built yet"]
    deny["Refused by default,<br/>coverage test in the build"]
    preAuthorize["@PreAuthorize on the method"]
    vocabulary["OWASP event names, user ids only"]
  end
  forged --> samesite & token & fetch
  xss --> httponly & csp
  fixation --> newId & transport
  stolenSession --> transport & lifetime
  stolenRemember --> httponly & rotation
  guessed --> codeLimits
  guessed -.-> rateLimit
  dumped --> codeLimits
  enumerated --> sameAnswer
  flooded -.-> rateLimit
  framed --> origin
  sniffed --> nosniff & csp
  unruled --> deny
  broad --> preAuthorize
  logs --> vocabulary
```

Most threats meet more than one defense, so that one gap leaves another standing. A dashed line is a defense that
does not exist yet.

## 🛣️ The path of a request

```mermaid
---
title: What a request meets, in order
---
flowchart LR
  request([Request]) --> forgery["Fetch Metadata,<br/>then the CSRF token"]
  forgery --> starterEndpoints["sign-in, logout:<br/>answer at once"]
  forgery --> identify["session, or remember-me,<br/>or anonymous"]
  identify --> url{"URL rules,<br/>first match decides"}
  url -- "no rule matches" --> refused["Refused: 401 or 403"]
  url -- allowed --> method{"@PreAuthorize"}
  method -- refused --> refused
  method -- allowed --> controller([Controller])
```

Forged requests are refused first, before they reach the sign-in or the logout. The URL rules then run from top to
bottom: the starter's code, sign-in, logout and CSRF endpoints, `/actuator/health`, the `EndpointRules` beans by
`@Order`, and a last rule that refuses anything else. The starter's endpoints come first so that no rule of the
application can lock users out, and health so that no rule hides it from the platform's probes. A broad pattern such
as `/api/**` goes in the last `EndpointRules` bean, since the first match decides. The beans are the application's,
and those of the other tech starters that serve an endpoint, such as the stream of `sse-starter`:
[ADR 0015](../adr/0015-push-server-events-over-sse-to-a-user-or-a-session.md) records why that starter brings the rule
of its own stream.

```mermaid
---
title: Each layer catches what the other misses
---
flowchart LR
  endpoint[New endpoint] --> url{"URL rule in<br/>EndpointRules?"}
  url -- no --> refused["Refused at runtime,<br/>and the coverage test<br/>of the application fails"]
  url -- yes --> annotation{"@PreAuthorize<br/>on the controller?"}
  annotation -- no --> urlOnly["The URL rule applies"]
  annotation -- yes --> both["Both apply,<br/>the stricter wins"]
```

The application adds `@PreAuthorize` to a controller method that needs more than its URL rule, such as a role, and
the stricter of the two wins. A forgotten URL rule refuses the endpoint, and a test of the backend fails the build
when an operation of the API contract has none. A forgotten annotation leaves the URL rule in force.

Every refusal answers a JSON problem detail: 403 for a forged request, and for a rule 401 to an anonymous caller,
403 to a signed-in one. The starter rethrows a refusal from a controller ahead of the application's exception
handlers, so that a catch-all handler cannot turn it into a 500.

## 🔑 Signing in

A user signs in with a code of six digits sent to their email, in two calls: one asks for the code, the other
sends it back. [ADR 0011](../adr/0011-sign-in-without-a-password.md) records why DrinkIt signs users in this way.

```mermaid
---
title: A sign-in by code
---
sequenceDiagram
  participant Nuxt
  participant Starter as security-starter
  participant Sender as SignInCodeSender
  participant Accounts as UserAccounts
  participant DB as PostgreSQL
  Nuxt->>Starter: GET /api/auth/csrf
  Starter-->>Nuxt: 204, Set-Cookie __Host-XSRF-TOKEN
  Nuxt->>Starter: POST /api/auth/code, X-XSRF-TOKEN<br/>{email}
  Starter->>DB: replace the code hash of the email in sign_in_code
  Starter->>Sender: send(email, code, locale)
  Starter-->>Nuxt: 204, whatever the email
  Note over Nuxt: the user reads the code in their mailbox
  Nuxt->>Starter: POST /api/auth/code/sign-in, X-XSRF-TOKEN<br/>{email, code, rememberMe}
  Starter->>DB: try the code, deleted once accepted
  Starter->>Accounts: findByLogin(email), or register(email, locale) when none
  Accounts-->>Starter: UserAccount: found, reactivated or created
  Starter->>Starter: new session id, new CSRF token
  Starter->>DB: insert into spring_session, and remember_me_token if asked
  Starter-->>Nuxt: 204, Set-Cookie __Host-SESSION, __Host-remember-me, __Host-XSRF-TOKEN
```

The starter draws the code at random and keeps only its SHA-256 hash, for 10 minutes. An email gets 3 wrong tries per
hour, whatever code it holds: a new request replaces the earlier code of that email, never its tries. The request never
looks the email up, so it answers the same 204, as fast, whether an account exists or not. The email is trimmed and
lower-cased before anything else.

The code is accepted once. The starter then asks the application for the account, and DrinkIt's backend brings back
a user deleted less than 30 days ago or creates a new one. A user therefore exists only once a code has proven their
email, and a new user completes their profile to get `ROLE_USER`. A wrong, used or expired code, the last try spent,
or an account the application refuses give the same 401.

The sign-in changes the session id, so that an id planted before it is worth nothing after it, and replaces the CSRF
token in its own response. It also grants the factor of a one-time token, with its time, for a later check of a
recent sign-in. The application changes the session id the same way through `CurrentUser`, when it signs in a user
proven another way or reloads their roles.

A code of six digits can be found back from its hash, which is why it lives minutes. Nothing limits the requests per
email or per address yet: a caller can ask for code after code, and so send emails to any address.

### Where each piece lives

| Piece | Kind | Where |
|---|---|---|
| The code: its draw, hash, expiry and tries | Authentication | `security-starter`, in `drinkit_session.sign_in_code` |
| The sending of the code | A channel the application picks | `SignInCodeSender`: `drinkit-backend` publishes a [platform event](../architecture/platform-events.md), whose handler emails the code after the answer |
| The account: its creation, reactivation and roles | Business | The `user` [event stream](../architecture/event-sourcing.md), which the starter reaches through `UserAccounts` |

`UserAccounts` and `SignInCodeSender` are the ports the application implements, so that the starter reads no table
of the application and sends no email itself.

## 🍪 Sessions and cookies

```mermaid
---
title: Where each piece of state lives, and for how long
---
flowchart LR
  subgraph browser["Browser cookies"]
    direction TB
    sessionCookie["__Host-SESSION, HttpOnly<br/>the session id, 256 random bits<br/>until the browser closes"]
    rememberCookie["__Host-remember-me, HttpOnly<br/>a series per device, and a token<br/>30 days from the sign-in"]
    xsrfCookie["__Host-XSRF-TOKEN, read by the app<br/>the CSRF token, no copy on the server<br/>replaced at each sign-in, expired at logout"]
  end
  subgraph postgres["PostgreSQL"]
    direction TB
    session[("spring_session<br/>user id, creation, last access<br/>30 min idle, 8 h at most")]
    attributes[("spring_session_attributes, JSONB<br/>SecurityContext: user id, roles<br/>SessionData: a value per SessionKey")]
    remember[("remember_me_token<br/>series, user id, SHA-256<br/>of the current and previous token")]
    user[("user, the application's<br/>accounts and roles")]
  end
  sessionCookie -- "id" --> session
  session --- attributes
  rememberCookie -- "series" --> remember
  remember -- "opens a new session" --> session
  remember -. "user id, through UserAccounts" .-> user
```

The cookies hold random values only, and what they grant lives in rows that a revocation deletes. A session ends
after 30 idle minutes or 8 hours, and `Sessions.revokeAll` signs a user out everywhere at once.
[ADR 0012](../adr/0012-keep-sessions-in-postgresql.md) records why the session lives in PostgreSQL and how long it
lasts. An anonymous caller gets no row: the CSRF token lives in its cookie, and the starter keeps no refused request.

Every cookie is `Secure`, `SameSite=Lax` and `Path=/`, and the session and remember-me cookies are `HttpOnly`:

- **`Secure`** keeps the cookie off plain HTTP, where anyone on the network reads it.
- **`HttpOnly`** keeps the session out of reach of a script injected into the page.
- **`SameSite=Lax`** leaves the cookies out of a request another site starts, except a top-level navigation, so a
  link from an email still opens DrinkIt signed in.
- **The `__Host-` prefix** makes the browser refuse a cookie set by a subdomain or a plain HTTP page.
- **`Path=/`** is set by hand: the backend runs under the context path `/drinkit`, and the browser drops a
  `__Host-` cookie with any other path.

The `dev` profile runs over plain HTTP on `localhost`, and drops both the prefix and `Secure`. The starter refuses to
start with a prefix and no `Secure`.

A remember-me cookie opens a new session once the current one has ended. Its token changes at each use and is stored
hashed, so that a stolen copy shows itself the first time both copies are used, and it lasts 30 days from the
sign-in. [ADR 0013](../adr/0013-remember-me-with-a-rotating-hashed-token.md) explains the rotation, its grace for
parallel requests and the theft detection.

```mermaid
---
title: A remembered visit
---
sequenceDiagram
  participant Nuxt
  participant Security as Spring Security
  participant DB as PostgreSQL
  Nuxt->>Security: GET /api/user/me, __Host-remember-me, no session
  Security->>DB: find the series in remember_me_token
  Security->>Security: compare the token hash
  Security->>DB: store the new token hash, keep the previous one for 10 s
  Security->>DB: insert into spring_session
  Security->>Security: new CSRF token, if the browser held one
  Security-->>Nuxt: 200, Set-Cookie __Host-SESSION, __Host-remember-me, __Host-XSRF-TOKEN
```

## 🛡️ Forged requests

A forged request comes from another site, with the victim's cookies. `SameSite=Lax` already keeps them off a
cross-site `POST`, and two checks of the backend stand behind it, on every method but `GET`, `HEAD`, `OPTIONS` and
`TRACE`:

- **CSRF token.** The client copies the `__Host-XSRF-TOKEN` cookie into the `X-XSRF-TOKEN` header, which another
  site cannot read to fill.
- **Fetch Metadata.** The starter refuses a request whose `Sec-Fetch-Site` is neither `same-origin` nor `none`, a URL
  the user typed. A client that sends no such header, such as curl, still needs the token.

The sign-in needs the token too: without it, another site could sign the victim in to the attacker's account.
[ADR 0014](../adr/0014-protect-against-csrf-with-a-cookie-token-and-fetch-metadata.md) records why the token lives in
a cookie rather than in the session.

## 📋 Security headers

DrinkIt sends no CORS header: the app and the API share one origin in production, and the Nuxt dev server forwards
the API calls in development. The browser then keeps any other origin from reading a response. Each response also
carries these headers, which cost nothing to a JSON API:

| Header | Value | Protects against |
|---|---|---|
| `Content-Security-Policy` | `default-src 'none'; frame-ancestors 'none'` | A response rendered as a page that loads scripts, or framed by another site |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | A first request in plain HTTP, sent even when TLS ends at a proxy |
| `X-Content-Type-Options` | `nosniff` | A JSON response that the browser guesses to be a script |
| `X-Frame-Options` | `DENY` | Framing, for browsers that ignore `frame-ancestors` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | A URL and its parameters sent to another site |
| `Permissions-Policy` | Camera, microphone and location turned off | A page of DrinkIt that uses them |
| `Cross-Origin-Opener-Policy` | `same-origin` | A window of another site that keeps a handle on DrinkIt's |
| `Cross-Origin-Resource-Policy` | `same-origin` | Another site that embeds a response of the API |
| `Cache-Control` | `no-cache, no-store, max-age=0, must-revalidate` | Personal data left in a shared or browser cache |
| `Clear-Site-Data` | `"cache", "storage"`, at logout, over HTTPS | What the app stored in the browser, kept after logout |

`Clear-Site-Data` leaves out `"cookies"`, which Chrome applies to every subdomain. The logout expires DrinkIt's
cookies one by one instead.

## 📝 Security events in the logs

The starter logs each security event under a name of OWASP's
[logging vocabulary](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Vocabulary_Cheat_Sheet.html), such as
`authn_login_success`, `authn_login_fail`, `authn_token_reuse`, `authz_fail` or `session_revoked`, so that an alert
can match on them. A success names its method, such as `authn_login_success:<id>,method=email_code` or
`method=remember_me`. A user appears by their id only, and no email, sign-in code, token, cookie or session id
reaches the [logs](../build-and-data/logging.md): a log is read by more people than the database. An anonymous
caller refused is the normal start of every visit, and is not logged.

## 🧩 What the business code uses

```mermaid
---
title: From the starter to the domain
---
flowchart LR
  controller[Controller] -- "currentUser()" --> current[CurrentUser]
  current --> connected["ConnectedUser<br/>id, roles"]
  connected -- ".author" --> author[Author]
  author --> domain[Domain use case]
  controller -- "get, set, getOrPut" --> data["SessionData<br/>a value per SessionKey"]
  controller -- "revokeAll(userId)" --> sessions[Sessions]
```

A controller reads the connected user from `CurrentUser`, and turns it into the `Author` that the use cases of the
domain take, so that the domain never depends on the starter. `SessionData` keeps values in the user's session, and
`Sessions.revokeAll` signs a user out everywhere, after a role change or a deletion. The
[README of `security-starter`](https://github.com/mboisnard/drinkit/blob/master/tech-starters/backend/security-starter/README.md)
shows how an application plugs it in.

## 📚 Sources

- [Application Security Verification Standard 5.0](https://github.com/OWASP/ASVS/tree/v5.0.0), OWASP
- [Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html), OWASP
- [HTTP Security Response Headers Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html), OWASP
- [Spring Security reference](https://docs.spring.io/spring-security/reference/), Spring

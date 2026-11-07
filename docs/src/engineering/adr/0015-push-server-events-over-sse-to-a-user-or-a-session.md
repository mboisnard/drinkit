# 0015. Push server events over SSE, to a user or a session

- Status: accepted
- Date: 2026-11-07

DrinkIt pushes events from the server to the browser over Server-Sent Events, through `sse-starter`. An event goes
to a user, or to the session of a visitor who is not signed in, and reaches every tab of that recipient open on the
instance. It is a signal that something changed, which the page reads again through the API, since delivery is at
most once. The starter brings the access rule of its stream, so the application never has to know its path.

## Context

The browser keeps one stream open per tab, and the server writes to it whenever the application has something to
say. The code that has something to say runs in a use case or in the handler of a platform event, where the signed-in
user is not the request's: a cellar shared by its owner changes for every user it is shared with. It knows user ids.
A page shown before the sign-in has something to say too, to a visitor the application only knows by the HTTP
session, whose id is a secret that must never travel in an event, nor be stored with the events that outlive a
restart.

A browser opens at most 6 connections per origin over HTTP/1.1, every stream counting as one. A stream closes after
a few minutes, as the proxies in front of the application close idle ones, and the browser opens it again: an event
sent in between reaches no one.

`security-starter` refuses every request that no `EndpointRules` bean matches, as the
[security principles](../security/principles.md) explain. The stream had no rule, so it was refused: the application
would have had to know a path that the starter owns. DrinkIt is one deployable application, so the code that sends
an event and the code that writes it to the stream run in the same process.

## Decision

The starter serves one stream per tab, at `/sse/event-stream`, and gives the application a `ServerEvents` interface
that sends an event, by name, with a payload, to a user id or to a session id.

- **Recipients.** The stream of a signed-in user belongs to that user. The stream of a visitor belongs to the session
  the application opened for the visitor, and a visitor without one gets 204, which tells the browser to stop asking:
  the stream never opens a session, which would let anyone fill the session store. The starter keeps a session under
  the SHA-256 hash of its id, in its events as in its streams.
- **Delivery.** The starter queues the event as a platform event of its own, and the instance writes it to the
  streams of that recipient it holds. The browser filters the events by name, so a tab never opens one stream per
  kind of event. A comment every 30 seconds keeps a stream open through the proxies, and the starter closes every
  stream when the application stops, for the browser to open it again on another instance.
- **Access.** With `security-starter` on the classpath, the starter registers a rule that opens its stream to anyone,
  in an auto-configuration of its own guarded by `@ConditionalOnClass(EndpointRules::class)`, with `security-starter`
  as a `compileOnly` dependency. An application bean of the same name replaces it. The path stays out of `/api/**`,
  so that a broad rule of the application never decides for it first.
- **Contract.** The stream goes in the API contract, as the endpoints of `security-starter` do, so that the API
  documentation shows it. The application's coverage test of its rules leaves it out, since the starter's tests prove
  its rule.
- **Domain.** The domain never sees the starter: a handler of the application turns a business event into a call to
  `ServerEvents`, as `NotificationSender` turns one into an email.

The starter's README gives the usage and the settings.

## Consequences

- A use case or a handler sends to a user without knowing whether, or where, that user is connected.
- A tab opens one connection whatever the number of events the page listens to.
- An event sent while a tab reconnects is lost, so a page reads its data again when its stream opens, not only when
  an event comes.
- A visitor gets events only once the application has opened their session, and the stream of a visitor stays on that
  session: after the sign-in, which renews the session id, the page opens its stream again.
- A user or a session keeps a few streams at most, the oldest closing when one more opens, so that no caller fills the
  memory of an instance.
- A starter that serves an endpoint depends on `security-starter` at compile time, and its tests start the security
  chain.
- Until the platform events cross instances through a broker, an event reaches only the tabs connected to the
  instance that sent it.

## Alternatives

- **The HTTP session as the only key.** Rejected because the code that sends knows users, not sessions, and a session
  id changes at each sign-in. It offered one key for every tab.
- **Visitors get only events sent to everyone.** Rejected because a page shown before the sign-in may have something
  to say to one visitor. It offered no session for a visitor.
- **The application declares the rule of the stream.** Rejected because the application would have to know the
  starter's path, and forgot it. It offered no dependency between starters.
- **A module of the event's class alone, apart from the starter, as an application of many services shares its
  messages.** Rejected because one deployable has no second service to share it with, and the class would be a
  public API the starter cannot change. It offered a domain that sends without depending on the starter.
- **WebSocket.** Rejected because the browser sends nothing back on this channel, and a WebSocket needs its own
  security and proxy settings. It offered a channel both ways.

## Sources

- [Using server-sent events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events), MDN
- [Server-sent events](https://html.spec.whatwg.org/multipage/server-sent-events.html), WHATWG HTML Living Standard

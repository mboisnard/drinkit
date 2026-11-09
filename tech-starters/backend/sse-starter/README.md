# SSE Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/tech-starters.md)
that pushes events from the server to the browser with
[Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events). Each tab opens one stream,
and the application sends an event to a user, or to the session of a visitor who is not signed in, who gets it in
every tab open on the instance.

```mermaid
flowchart LR
    Code[Code of the application] -->|"send(userId or sessionId, name, payload)"| Api[ServerEvents]
    Api --> Events[Platform events]
    Events --> Starter[SSE starter]
    Browser -->|"GET /sse/event-stream"| Starter
    Starter -->|event of that name| Browser
```

## How it works

An event is queued as a platform event, after the commit of the transaction that sends it, and written to the
streams of its recipient that the instance holds. A comment every 30 seconds keeps a stream open through the
proxies:

```mermaid
sequenceDiagram
    participant Tab as Browser tab
    participant Starter as SSE starter
    participant Events as Platform events
    participant App as Handler of the application
    Tab->>Starter: GET /sse/event-stream
    alt signed in, or a session the application opened
        Starter-->>Tab: 200, a comment, the stream is open
    else a visitor without a session
        Starter-->>Tab: 204, EventSource stops asking
    end
    App->>Starter: send(ada, cellar-updated, payload)
    Starter->>Events: the event, after the commit
    Events->>Starter: the event
    Starter-->>Tab: event cellar-updated, to every tab of ada
    Note over Starter,Tab: after 3 minutes, the starter closes the stream and the browser opens it again
```

Each opening of the stream is a request that keeps the session alive. A page therefore holds its stream only while
its tab is visible, so that the idle timeout of a session runs once nobody looks at the tab:

```mermaid
sequenceDiagram
    participant Page as Page of a tab
    participant Starter as SSE starter
    participant Session as Session of the user
    Page->>Starter: GET /sse/event-stream, the tab is visible
    Starter->>Session: a request, the idle timeout starts again
    Starter-->>Page: 200, the stream
    Page->>Page: reads its data through the API
    Note over Page,Starter: every 3 minutes, the stream closes and EventSource opens it again
    Page->>Starter: closes the stream, the tab is hidden
    Note over Session: no request, the session ends after 30 idle minutes
    Page->>Starter: GET /sse/event-stream, the tab is visible again
    alt the session is alive, or a remember-me cookie signs the user in again
        Starter-->>Page: 200, the page reads its data again
    else the session has ended
        Starter-->>Page: 204, the next call of the API asks to sign in
    end
```

[ADR 0015](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/adr/0015-push-server-events-over-sse-to-a-user-or-a-session.md)
records why a stream belongs to a user or a session, carries signals, follows the visibility of its tab, and comes with
its access rule.

## Usage

A module declares the starter in its build file:

```kotlin
dependencies {
    implementation(project(":sse-starter"))
}
```

The browser opens one stream per tab, under the context path of the application, while the tab is visible. It
listens to the events it needs by name, and reads its data again at each opening, since an event sent while the
stream was closed is lost:

```javascript
let events = null;

function openStream() {
  events = new EventSource('/drinkit/sse/event-stream');
  events.addEventListener('open', () => refreshCellars());
  events.addEventListener('cellar-updated', (event) => refreshCellar(JSON.parse(event.data).cellarId));
}

document.addEventListener('visibilitychange', () => {
  events?.close();
  if (!document.hidden) openStream();
});
if (!document.hidden) openStream();
```

The application sends an event to a user from a handler or an adapter, never from the domain, which calls a port of
its own:

```kotlin
serverEvents.send(userId = owner.id, eventName = "cellar-updated", payload = mapOf("cellarId" to cellarId))
serverEvents.sendToSession(sessionId = session.id, eventName = "price-changed", payload = mapOf("bottleId" to id))
```

- The stream of a signed-in user gets the events sent to that user. The stream of a visitor gets those sent to the
  session the application opened for the visitor, and a visitor without one gets 204, which tells `EventSource` to stop
  asking: the page opens the stream again once a call of the application has opened the session, and again after the
  sign-in, which renews the session id.
- The payload is written as JSON. It says what changed, never personal data, and the page reads the data again
  through the API: an event sent while a tab reconnects is lost.
- With `security-starter`, the starter opens the stream to anyone, and each caller gets their own events only. An
  application bean named `serverEventsAccessRules` replaces that rule:

```kotlin
@Bean
fun serverEventsAccessRules() = endpointRules {
    authorize(HttpMethod.GET, "/sse/event-stream", authenticated)
}
```

- `EndpointRulesCoverage` of `security-starter` applies this rule beside the application's, so the stream needs no
  line in the application's coverage test.
- When the application stops, the starter closes every stream, and the browser opens it again on another instance.

## Configuration

| Property | Default | Environment variable |
|---|---|---|
| `starters.sse.timeout` | `3m`, kept under the idle timeout of the proxies in front of the application | `STARTERS_SSE_TIMEOUT` |
| `starters.sse.heartbeat` | `30s` between two comments on each stream, shorter than the timeout | `STARTERS_SSE_HEARTBEAT` |
| `starters.sse.max-streams` | `5` per user or session, one more closes the oldest | `STARTERS_SSE_MAXSTREAMS` |
| `starters.sse.enabled` | `true` | `STARTERS_SSE_ENABLED` |

## Tests

`SpyServerEvents`, from `testFixtures(project(":sse-starter"))`, records what the code under test sends, and refuses
the event names the real sender refuses:

```kotlin
val serverEvents = SpyServerEvents()
// the code under test sends
serverEvents.sentTo("ada-id").single().eventName shouldBe "cellar-updated"
```

## Limits

- An event reaches only the streams of the instance that published it, since the
  [platform events](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/platform-events.md)
  travel inside the process. A broker would deliver it to every instance.
- A visible tab left alone keeps its session alive until its absolute timeout, as each opening of the stream counts
  as activity.
- A signed-in tab keeps its stream until the stream times out, even after a sign out or a revocation: the events
  it gets meanwhile are signals, whose data the API no longer serves to that tab.

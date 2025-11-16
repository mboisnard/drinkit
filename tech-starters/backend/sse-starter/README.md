# SSE Starter

A Spring Boot starter that provides a scalable, production-ready implementation of Server-Sent Events (SSE) with multi-instance support.

## What is Server-Sent Events (SSE)?

Server-Sent Events (SSE) is a server push technology that enables servers to push real-time updates to clients over a single HTTP connection. Unlike WebSockets, SSE is:

- **Unidirectional**: Server-to-client communication only
- **HTTP-based**: Works over standard HTTP/HTTPS
- **Automatic reconnection**: Built-in browser support for reconnection
- **Text-based**: Simple protocol for sending text data

**Use Cases:**
- Real-time notifications
- Live updates (stock prices)
- Activity feeds
- Progress tracking

## Architecture Overview

This starter implements a scalable SSE solution that separates business logic from technical concerns and supports horizontal scaling across multiple application instances.

### Client Connection Flow

```mermaid
sequenceDiagram
    participant Client
    participant SseApi
    participant Repository as InMemoryEmittersRepository

    Client->>SseApi: GET /sse/event-stream?eventName=notifications
    SseApi->>SseApi: Create SseEmitter
    SseApi->>SseApi: Setup lifecycle callbacks<br/>(onCompletion, onError, onTimeout)
    SseApi->>Repository: register(sessionId, eventName, emitter)
    Repository->>Repository: Store in ConcurrentHashMap
    SseApi->>Client: Return SseEmitter (keep connection open)

    Note over Client,Repository: Connection established and maintained
```

### Sending Message Flow

```mermaid
sequenceDiagram
    participant Business as Business Logic
    participant Messaging as Messaging Layer
    participant SseApi
    participant Repository as InMemoryEmittersRepository
    participant Client

    Business->>Messaging: Publish SendSseEvent
    Note over Business,Messaging: Async decoupling:<br/>Business logic doesn't know about SSE
    Messaging->>SseApi: @PlatformEventHandler<br/>sendMessageToEventStream()
    SseApi->>Repository: findAllBy(sessionId)
    Repository-->>SseApi: List<Emitter> or null

    alt Emitters found
        SseApi->>SseApi: Filter by eventName
        loop For each matching emitter
            SseApi->>Client: emitter.send(event)
        end
    else No emitters on this instance

        Note over SseApi: Skip silently - another<br/>instance may have the emitter
    end
```

### Multi-Instance Architecture

```mermaid
graph TB
    subgraph "Client Tier"
        C1[Client 1<br/>Session A]
        C2[Client 2<br/>Session B]
        C3[Client 3<br/>Session C]
    end

    subgraph "Load Balancer"
        LB[Load Balancer<br/>Round Robin]
    end

    subgraph "Application Instance 1"
        API1[SseApi]
        REPO1[InMemoryRepository<br/>Sessions: A, C]
        HANDLER1[Event Handler<br/>oneQueuePerInstance=true]
        Q1[Auto-created Queue]
    end

    subgraph "Application Instance 2"
        API2[SseApi]
        REPO2[InMemoryRepository<br/>Sessions: B]
        HANDLER2[Event Handler<br/>oneQueuePerInstance=true]
        Q2[Auto-created Queue]
    end

    subgraph "Business Layer"
        BS[Business Service]
    end

    subgraph "Message Broker"
        EXCHANGE[RabbitMQ Exchange<br/>send.sse.event]
    end

    C1 -->|SSE Connection| LB
    C2 -->|SSE Connection| LB
    C3 -->|SSE Connection| LB

    LB -->|Any instance| API1
    LB -->|Any instance| API2

    API1 <--> REPO1
    API2 <--> REPO2

    BS -->|Publish SendSseEvent| EXCHANGE

    HANDLER1 -.->|Creates at startup| Q1
    HANDLER2 -.->|Creates at startup| Q2

    EXCHANGE -->|Broadcast to ALL| Q1
    EXCHANGE -->|Broadcast to ALL| Q2

    Q1 --> HANDLER1
    Q2 --> HANDLER2

    HANDLER1 -.->|Only if session exists| API1
    HANDLER2 -.->|Only if session exists| API2

    style BS fill:#e1f5e1
    style EXCHANGE fill:#ffe1e1
    style REPO1 fill:#e1e5ff
    style REPO2 fill:#e1e5ff
    style Q1 fill:#fff3e1
    style Q2 fill:#fff3e1
```

**How it works:**

1. **Dynamic Routing**: Clients connect through a load balancer to any available instance
2. **Instance-Local Storage**: Each instance maintains its own in-memory repository of connected emitters for sessions it handles
3. **Auto-Queue Creation**: Each instance creates its own RabbitMQ queue at startup (via `oneQueuePerInstance=true`)
4. **Business Event**: A business service publishes a `SendSseEvent` to the exchange, unaware of which instance holds the connection
5. **Broadcast to All**: The exchange broadcasts the event to ALL instance queues simultaneously
6. **Smart Filtering**: Every instance receives the event, but only the one with the matching session's emitter processes it (others skip silently)
7. **No Coordination Needed**: Instances operate independently; the right instance automatically handles its sessions

## Key Features

### ✅ Separation of Concerns

Business services **don't need to know about SSE**. They simply publish a `SendSseEvent`:

```kotlin
// In your business service
eventPublisher.publish(
    SendSseEvent(
        eventName = "order-update",
        sessionId = userSessionId,
        payload = orderStatus
    )
)
```

The SSE infrastructure handles the technical details of delivering the message.

### ✅ Multi-Instance Scalability

- **Async Messaging**: Uses `SendSseEvent` platform event
- **RabbitMQ Broadcasting**: Messages are sent to ALL instances via exchange
- **Instance-Aware**: Only the instance holding the emitter sends the message
- **No Coordination**: Instances operate independently without shared state

### ✅ Memory Leak Prevention

Automatic cleanup of dead connections through lifecycle callbacks:

- **onCompletion**: Client gracefully disconnects
- **onError**: Network error or send failure
- **onTimeout**: Emitter timeout (default: 3 minutes)

Thread-safe collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`) ensure safe concurrent access.

### ✅ Multiple Event Streams per Session

A single session can subscribe to multiple event types:

```javascript
// Client can have multiple streams
const notifications = new EventSource('/sse/event-stream?eventName=notifications');
const updates = new EventSource('/sse/event-stream?eventName=order-updates');
```

Each is managed independently and filtered by `eventName`.

## Usage

### 1. Add Dependency

```kotlin
dependencies {
    implementation(project(":sse-starter"))
}
```

### 2. Client Connection (JavaScript)

```javascript
const eventSource = new EventSource('/sse/event-stream?eventName=notifications');

eventSource.addEventListener('notifications', (event) => {
    const data = JSON.parse(event.data);
    console.log('Received:', data);
});

eventSource.onerror = (error) => {
    console.error('SSE Error:', error);
};
```

### 3. Send Events from Business Logic

```kotlin
@Service
class NotificationService(
    private val eventPublisher: EventPublisher
) {
    fun notifyUser(sessionId: String, message: String) {
        eventPublisher.publish(
            SendSseEvent(
                eventName = "notifications",
                sessionId = sessionId,
                payload = mapOf("message" to message, "timestamp" to Instant.now())
            )
        )
    }
}
```
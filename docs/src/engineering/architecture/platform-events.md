# Platform events

A platform event is a message that one part of the application publishes and that other parts handle later, on another
thread. The creation of a user, for one, triggers the verification email. Code publishes through
`PlatformEventPublisher` and handles it in a method annotated `@PlatformEventHandler`, whose parameter is the event.
Both come from [`messaging-starter`][starter], and neither shows the transport behind them: Spring application events
inside the process today, with the limits listed below, and RabbitMQ as the target. Read this page before you publish
or handle a platform event.

The name keeps it apart from the events of [event sourcing](event-sourcing.md), which are the stored history of one
aggregate. A platform event tells other parts of the application that something happened, and is gone once handled.

## 🧭 Why asynchronous messages

A use case publishes what happened and returns. The work that follows, such as an email or an update in another
[bounded context](modular-monolith.md), runs in a handler. The handler neither slows the use case down nor makes it fail.
The publisher does not know its handlers, so a new reaction is a new handler, with no change to the use case. A
business handler lives in `drinkit-backend` and calls a use case, as a primary adapter of the
[hexagonal architecture](hexagonal-architecture.md). Once a broker carries the events, a message also survives a
restart and reaches several queues.

## 🏷️ One annotation, whatever the broker

`@PlatformEventHandler` declares what a handler needs from the transport, and the starter turns it into what that
transport understands. Moving from Spring events to RabbitMQ, or later to Kafka, changes the starter and none of the
handlers. The publisher's interface also lets tests run without any transport: `SpyPlatformEventPublisher` records
what a use case published. The attributes ask a broker for:

- **`name`**: the name of the handler's durable queue. Each handler gets its own copy of an event, and the instances
  of the application share its queue.
- **`exchange`**: the exchange that the event is published to, and that the handler's queue is bound to.
- **`oneQueuePerInstance`**: a queue per instance, deleted with it, so that every instance gets every event. It
  serves what lives in the memory of one instance, such as the browser connection that a server-sent event must
  reach. An event published before the instance started never reaches it.
- **`delay`**, not built yet: handle the event after a delay, through a waiting queue whose messages expire into the
  handler's queue. The broker sets the delay when it creates the waiting queue, so changing a handler's delay takes a
  new queue. It does not combine with `oneQueuePerInstance`: the waiting queue would outlive the instance it feeds.

Spring events ignore them today, but every handler already sets its `name`, so that the move to a broker changes no
handler.

## ⚙️ Today: Spring events in the process

The publisher hands the event to Spring, which calls each handler on a new thread. An exception in a handler is
logged, and the event is dropped. That keeps the starter simple while one instance runs, but:

- an event not handled yet is lost when the application stops
- a failed handler is never retried
- a handler can run before the transaction that published the event commits, and miss what it wrote
  ([#528](https://github.com/mboisnard/drinkit/issues/528))

## 🐇 Toward RabbitMQ

With RabbitMQ, the starter publishes an event once its transaction commits, and never when it rolls back. A failed
message comes back after a growing delay, then waits in a dead-letter queue for someone to look at it. Delivery is
at least once, and in no set order. Write events and handlers by these rules now, so that the move costs nothing:

- **An event is named in the past tense,** such as `UserCreated`: it states a fact, so the publisher depends on none
  of the handlers that react to it.
- **An event carries ids and facts that serialize to JSON, never an entity.** An entity ties the handler to the
  publisher's model, and is out of date by the time the handler runs.
- **A handler is idempotent,** since a message can arrive twice: handling it twice has the effect of handling it
  once.
- **No handler relies on the order of events.**
- **An event changes by [expand and contract][expand-and-contract].** A message can wait in a queue, or come back from
  a retry, long after it was published. A feature flag therefore cannot switch a publisher and its handlers at the
  same moment.

## 📚 Sources

- [Guaranteed Delivery](https://www.enterpriseintegrationpatterns.com/patterns/messaging/GuaranteedMessaging.html), [Publish-Subscribe Channel](https://www.enterpriseintegrationpatterns.com/patterns/messaging/PublishSubscribeChannel.html), [Competing Consumers](https://www.enterpriseintegrationpatterns.com/patterns/messaging/CompetingConsumers.html) and [Idempotent Receiver](https://www.enterpriseintegrationpatterns.com/patterns/messaging/IdempotentReceiver.html), Gregor Hohpe and Bobby Woolf, Enterprise Integration Patterns
- [Publisher-Subscriber pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/publisher-subscriber), Microsoft Azure Architecture Center
- [Pattern: Transactional outbox](https://microservices.io/patterns/data/transactional-outbox.html), Chris Richardson, microservices.io
- [Events](https://docs.spring.io/spring-modulith/reference/events.html), Spring Modulith reference documentation
- [Binder abstraction](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/binders.html), Spring Cloud Stream reference documentation
- [AMQP 0-9-1 Model Explained](https://www.rabbitmq.com/tutorials/amqp-concepts), [Reliability Guide](https://www.rabbitmq.com/docs/reliability), [Dead Letter Exchanges](https://www.rabbitmq.com/docs/dlx) and [Time-To-Live and Expiration](https://www.rabbitmq.com/docs/ttl), RabbitMQ

[starter]: https://github.com/mboisnard/drinkit/tree/master/tech-starters/backend/messaging-starter
[expand-and-contract]: ../writing-code/best-practices/coding-rules.md#evolve-a-contract-in-steps

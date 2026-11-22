// The values of readyState that EventSource names
const CONNECTING = 0;
const OPEN = 1;
const CLOSED = 2;

/**
 * The browser's `EventSource` with the server in the hands of the test, every stream opened kept in `instances`.
 * A test stubs it with `vi.stubGlobal('EventSource', FakeEventSource)` and empties `instances` before each test.
 */
export class FakeEventSource extends EventTarget {
  static readonly instances: FakeEventSource[] = [];

  readonly url: string;
  readyState: number = CONNECTING;
  // Kept from one event to the next, as the browser does
  private lastEventId = '';

  constructor(url: string | URL) {
    super();
    this.url = String(url);
    FakeEventSource.instances.push(this);
  }

  /** The streams not closed yet, by the page or by the server. */
  static active(): FakeEventSource[] {
    return FakeEventSource.instances.filter((source) => source.readyState !== CLOSED);
  }

  close(): void {
    this.readyState = CLOSED;
  }

  /**
   * The server answers 200: the stream opens, again after a drop. With `lastEventId`, the backend's `sse-starter`
   * then sends the `init` event, which carries the id of the last event sent to the user.
   */
  accept(lastEventId?: string): void {
    this.readyState = OPEN;
    this.dispatchEvent(new Event('open'));

    if (lastEventId !== undefined) {
      this.emit('init', {}, lastEventId);
    }
  }

  /** The server writes an event of that name, its data serialized as the backend does, with its id if any. */
  emit(name: string, data: unknown, id?: string): void {
    if (this.readyState === OPEN) {
      this.lastEventId = id ?? this.lastEventId;
      this.dispatchEvent(new MessageEvent(name, { data: JSON.stringify(data), lastEventId: this.lastEventId }));
    }
  }

  /** The connection breaks: the browser reconnects, and the stream opens again once the server accepts it. */
  drop(): void {
    this.readyState = CONNECTING;
    this.dispatchEvent(new Event('error'));
  }
}

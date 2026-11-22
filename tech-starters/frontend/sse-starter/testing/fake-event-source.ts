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

  /** The server answers 200: the stream opens, again after a drop. */
  accept(): void {
    this.readyState = OPEN;
    this.dispatchEvent(new Event('open'));
  }

  /** The server writes an event of that name, its data serialized as the backend does. */
  emit(name: string, data: unknown): void {
    if (this.readyState === OPEN) {
      this.dispatchEvent(new MessageEvent(name, { data: JSON.stringify(data) }));
    }
  }

  /** The connection breaks: the browser reconnects, and the stream opens again once the server accepts it. */
  drop(): void {
    this.readyState = CONNECTING;
    this.dispatchEvent(new Event('error'));
  }
}

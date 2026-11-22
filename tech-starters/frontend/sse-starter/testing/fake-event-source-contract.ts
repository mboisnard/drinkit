import { expect, test } from 'vitest';

/** A stream as a page holds it, and the server behind it, driven by the test. */
export interface StreamUnderTest {
  source: EventTarget & { readonly readyState: number };
  accept: () => Promise<void> | void;
  emit: (name: string, data: unknown, id?: string) => Promise<void> | void;
  drop: () => Promise<void> | void;
}

/**
 * What `sse-starter` relies on in the browser's `EventSource`, run on `FakeEventSource` and on the `EventSource` of
 * Node, which follows the same specification, to check that the fake still tells the truth.
 */
export function fakeEventSourceContract(connect: () => Promise<StreamUnderTest>): void {
  test('an accepted stream is open', async () => {
    // Given
    const { source, accept } = await connect();
    const opened = next(source, 'open');

    // When
    await accept();

    // Then
    await opened;
    expect(source.readyState).toBe(1);
  });

  test('an event reaches the listeners of its name with the data the server wrote', async () => {
    // Given
    const { source, accept, emit } = await connect();
    await accept();
    const received = next(source, 'cellar-updated');

    // When
    await emit('cellar-updated', { id: 'c1' });

    // Then
    expect(((await received) as MessageEvent<string>).data).toBe('{"id":"c1"}');
  });

  test('an event carries the id the server wrote, kept by the next event that has none', async () => {
    // Given
    const { source, accept, emit } = await connect();
    await accept();
    await emit('cellar-updated', { id: 'c1' }, 'e1');
    const received = next(source, 'bottle-added');

    // When
    await emit('bottle-added', { id: 'b1' });

    // Then
    expect(((await received) as MessageEvent<string>).lastEventId).toBe('e1');
  });

  test('a dropped stream opens again once the server accepts it', async () => {
    // Given
    const { source, accept, drop } = await connect();
    await accept();
    const failed = next(source, 'error');
    await drop();
    await failed;
    const reopened = next(source, 'open');

    // When
    await accept();

    // Then
    await reopened;
    expect(source.readyState).toBe(1);
  });
}

async function next(source: EventTarget, type: string): Promise<Event> {
  return new Promise((resolve) => {
    source.addEventListener(type, resolve, { once: true });
  });
}

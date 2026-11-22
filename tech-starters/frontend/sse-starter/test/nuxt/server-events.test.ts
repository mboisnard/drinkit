import { effectScope } from 'vue';
import type { App, EffectScope } from 'vue';
import { useNuxtApp } from '#app';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { FakeEventSource } from '@drinkit/sse-starter/testing';
import { useServerData } from '@drinkit/sse-starter/use-server-data';
import { onStreamOpen, useServerEvent } from '@drinkit/sse-starter/use-server-event';

// EventSource.CLOSED
const CLOSED = 2;

const components: EffectScope[] = [];
let errorHandler: App['config']['errorHandler'];
let pages = 0;

function mounted<T>(setup: () => T): T {
  const component = effectScope();
  components.push(component);

  return component.run(setup) as T;
}

function unmountAll(): void {
  components.splice(0).forEach((component) => {
    component.stop();
  });
}

// A page that shows the answer of an agent, which comes in chunks
function answerPage(read: () => Promise<string[]>): ReturnType<typeof useServerData<string[]>> {
  return mounted(() => useServerData(`answer-${String(++pages)}`, read, {
    'agent-chunk': (answer: string[], chunk: string) => [...answer, chunk],
  }));
}

function showTab(state: DocumentVisibilityState): void {
  Object.defineProperty(document, 'visibilityState', { value: state, configurable: true });
  document.dispatchEvent(new Event('visibilitychange'));
}

beforeEach(() => {
  ({ errorHandler } = useNuxtApp().vueApp.config);
  FakeEventSource.instances.length = 0;
  vi.stubGlobal('EventSource', FakeEventSource);
});

afterEach(() => {
  unmountAll();
  showTab('visible');
  useNuxtApp().vueApp.config.errorHandler = errorHandler;
  vi.unstubAllGlobals();
});

test('two components listening to two events share one connection', () => {
  // When
  mounted(() => {
    useServerEvent('cellar-updated', vi.fn());
  });
  mounted(() => {
    useServerEvent('bottle-added', vi.fn());
  });

  // Then
  expect(FakeEventSource.active().map((source) => source.url)).toStrictEqual(['/drinkit/sse/event-stream']);
});

test('an event reaches the handlers of its name only, with the payload the backend sent', () => {
  // Given
  const cellarUpdates: unknown[] = [];
  const bottlesAdded: unknown[] = [];
  mounted(() => {
    useServerEvent('cellar-updated', (payload) => cellarUpdates.push(payload));
  });
  mounted(() => {
    useServerEvent('bottle-added', (payload) => bottlesAdded.push(payload));
  });
  FakeEventSource.instances[0]?.accept();

  // When
  FakeEventSource.instances[0]?.emit('cellar-updated', { id: 'c1' });

  // Then
  expect([cellarUpdates, bottlesAdded]).toStrictEqual([[{ id: 'c1' }], []]);
});

test('an event the page cannot read reaches the error handler of the app, under its name', () => {
  // Given
  const errors: string[] = [];
  useNuxtApp().vueApp.config.errorHandler = (_error, _instance, info) => {
    errors.push(info);
  };
  mounted(() => {
    useServerEvent('cellar-updated', vi.fn());
  });
  FakeEventSource.instances[0]?.accept();

  // When
  FakeEventSource.instances[0]?.dispatchEvent(new MessageEvent('cellar-updated', { data: 'not json' }));

  // Then
  expect(errors).toStrictEqual(['server event cellar-updated']);
});

test('the connection closes once no component listens', () => {
  // Given
  mounted(() => {
    useServerEvent('cellar-updated', vi.fn());
  });

  // When
  unmountAll();

  // Then
  expect(FakeEventSource.instances.map((source) => source.readyState)).toStrictEqual([CLOSED]);
});

test('no connection stays open after the sign-out', async () => {
  // Given
  mounted(() => {
    useServerEvent('cellar-updated', vi.fn());
  });
  FakeEventSource.instances[0]?.accept();

  // When
  await useNuxtApp().callHook('drinkit:signed-out');

  // Then
  expect(FakeEventSource.instances.map((source) => source.readyState)).toStrictEqual([CLOSED]);
});

test('the sign-in opens a new connection in place of the old one', async () => {
  // Given
  mounted(() => {
    useServerEvent('cellar-updated', vi.fn());
  });

  // When
  await useNuxtApp().callHook('drinkit:signed-in');

  // Then
  expect(FakeEventSource.active()).toStrictEqual([FakeEventSource.instances[1]]);
});

test('a hidden tab holds no connection', () => {
  // Given
  mounted(() => {
    useServerEvent('cellar-updated', vi.fn());
  });

  // When
  showTab('hidden');

  // Then
  expect(FakeEventSource.instances.map((source) => source.readyState)).toStrictEqual([CLOSED]);
});

test('a tab visible again opens a connection', () => {
  // Given
  mounted(() => {
    useServerEvent('cellar-updated', vi.fn());
  });
  showTab('hidden');

  // When
  showTab('visible');

  // Then
  expect(FakeEventSource.active()).toHaveLength(1);
});

test('the opening that the mount starts calls no onStreamOpen handler, the page has just read its data', () => {
  // Given
  let opens = 0;
  mounted(() => {
    onStreamOpen(() => opens++);
    useServerEvent('cellar-updated', vi.fn());
  });

  // When
  FakeEventSource.instances[0]?.accept();

  // Then
  expect(opens).toBe(0);
});

test('a reconnection calls the handlers of onStreamOpen', () => {
  // Given
  let opens = 0;
  mounted(() => {
    useServerEvent('cellar-updated', vi.fn());
    onStreamOpen(() => opens++);
  });
  const source = FakeEventSource.instances[0];
  source?.accept();
  source?.drop();

  // When
  source?.accept();

  // Then
  expect(opens).toBe(1);
});

test('a page mounted in a hidden tab calls its onStreamOpen handlers once the tab is shown', () => {
  // Given
  let opens = 0;
  showTab('hidden');
  mounted(() => {
    useServerEvent('cellar-updated', vi.fn());
    onStreamOpen(() => opens++);
  });
  showTab('visible');

  // When
  FakeEventSource.instances[0]?.accept();

  // Then
  expect(opens).toBe(1);
});

test('a page reads its data once as it loads', async () => {
  // Given
  let reads = 0;
  await answerPage(async () => {
    reads++;

    return Promise.resolve(['Hello']);
  });

  // When
  FakeEventSource.instances[0]?.accept();

  // Then
  expect(reads).toBe(1);
});

test('an event changes the data through its reducer, without reading it again', async () => {
  // Given
  let reads = 0;
  const { data } = await answerPage(async () => {
    reads++;

    return Promise.resolve(['Hello']);
  });
  FakeEventSource.instances[0]?.accept();

  // When
  FakeEventSource.instances[0]?.emit('agent-chunk', 'world');

  // Then
  expect({ answer: data.value, reads }).toStrictEqual({ answer: ['Hello', 'world'], reads: 1 });
});

test('a reconnection reads the data again', async () => {
  // Given
  let reads = 0;
  await answerPage(async () => {
    reads++;

    return Promise.resolve(['Hello']);
  });
  const source = FakeEventSource.instances[0];
  source?.accept();
  source?.drop();

  // When
  source?.accept();

  // Then
  await vi.waitFor(() => {
    expect(reads).toBe(2);
  });
});

test('an event that comes before the data is read is left to the read, with no error', async () => {
  // Given
  const errors: unknown[] = [];
  useNuxtApp().vueApp.config.errorHandler = (error) => {
    errors.push(error);
  };
  const read = Promise.withResolvers<string[]>();
  const page = answerPage(async () => read.promise);
  FakeEventSource.instances[0]?.accept();
  FakeEventSource.instances[0]?.emit('agent-chunk', 'world');

  // When
  read.resolve(['Hello']);

  // Then
  expect({ answer: (await page).data.value, errors }).toStrictEqual({ answer: ['Hello'], errors: [] });
});

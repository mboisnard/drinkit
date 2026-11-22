import { createServer } from 'node:http';
import type { ServerResponse } from 'node:http';
import type { AddressInfo } from 'node:net';
import { afterEach, describe } from 'vitest';
import { FakeEventSource, fakeEventSourceContract } from '@drinkit/sse-starter/testing';
import type { StreamUnderTest } from '@drinkit/sse-starter/testing';

describe('FakeEventSource', () => {
  fakeEventSourceContract(async () => {
    const source = new FakeEventSource('/drinkit/sse/event-stream');

    return Promise.resolve({
      source,
      accept: () => {
        source.accept();
      },
      emit: (name, data) => {
        source.emit(name, data);
      },
      drop: () => {
        source.drop();
      },
    });
  });
});

describe('Node\'s EventSource', () => {
  const closings: Array<() => void> = [];

  afterEach(() => {
    closings.splice(0).forEach((close) => {
      close();
    });
  });

  fakeEventSourceContract(async () => {
    const requests: ServerResponse[] = [];
    const waiting: Array<(response: ServerResponse) => void> = [];
    const server = createServer((_, response) => {
      const resolve = waiting.shift();

      if (resolve) {
        resolve(response);
      } else {
        requests.push(response);
      }
    });
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    const { port } = server.address() as AddressInfo;
    const source = new EventSource(`http://127.0.0.1:${String(port)}`);
    let response: ServerResponse | undefined;
    closings.push(() => {
      source.close();
      server.closeAllConnections();
      server.close();
    });

    return {
      source,
      accept: async () => {
        response = requests.shift() ?? (await new Promise<ServerResponse>((resolve) => waiting.push(resolve)));
        // A short retry, so that a drop reconnects within the test
        response.writeHead(200, { 'content-type': 'text/event-stream' }).write('retry: 10\n\n');
      },
      emit: async (name, data) => new Promise<void>((resolve) => {
        response?.write(`event: ${name}\ndata: ${JSON.stringify(data)}\n\n`, () => {
          resolve();
        });
      }),
      drop: () => {
        response?.end();
      },
    } satisfies StreamUnderTest;
  });
});

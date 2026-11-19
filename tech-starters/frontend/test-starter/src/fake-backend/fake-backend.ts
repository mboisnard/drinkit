import { createServer } from 'node:http';
import type { IncomingMessage, ServerResponse } from 'node:http';
import type { AddressInfo } from 'node:net';
import { buffer } from 'node:stream/consumers';
import { getResponse } from 'msw';
import type { RequestHandler } from 'msw';

export interface FakeBackend {
  /** The origin the app under test calls instead of the backend, such as `http://127.0.0.1:41234`. */
  url: string;
  close: () => Promise<void>;
}

/** Serves MSW handlers over real HTTP, so the requests of the server render reach them too. */
export async function startFakeBackend(...handlers: RequestHandler[]): Promise<FakeBackend> {
  const server = createServer((incoming, outgoing) => {
    void answer(handlers, incoming, outgoing);
  });

  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const { port } = server.address() as AddressInfo;

  return {
    url: `http://127.0.0.1:${String(port)}`,
    close: () => new Promise((resolve, reject) => {
      server.close((error) => {
        if (error) {
          reject(error);
        } else {
          resolve();
        }
      });
      // A request a handler never answers would otherwise hold the server open
      server.closeAllConnections();
    }),
  };
}

async function answer(handlers: RequestHandler[], incoming: IncomingMessage, outgoing: ServerResponse): Promise<void> {
  const request = await toRequest(incoming);
  const baseUrl = new URL(request.url).origin;
  const response = await getResponse(handlers, request, { baseUrl }) ?? unhandled(request);

  outgoing.writeHead(response.status, Object.fromEntries(response.headers));
  outgoing.end(Buffer.from(await response.arrayBuffer()));
}

async function toRequest(incoming: IncomingMessage): Promise<Request> {
  const url = new URL(incoming.url ?? '/', `http://${incoming.headers.host ?? 'localhost'}`);
  const headers = new Headers();
  for (const [name, values] of Object.entries(incoming.headers)) {
    for (const value of [values ?? []].flat()) {
      headers.append(name, value);
    }
  }

  const hasBody = incoming.method !== 'GET' && incoming.method !== 'HEAD';

  return new Request(url, {
    method: incoming.method,
    headers,
    body: hasBody ? await buffer(incoming) : undefined,
  });
}

function unhandled(request: Request): Response {
  const { pathname, search } = new URL(request.url);

  return new Response(`No handler of the fake backend for ${request.method} ${pathname}${search}`, { status: 501 });
}

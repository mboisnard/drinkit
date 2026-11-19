import { expect } from '@playwright/test';
import type { APIRequestContext } from '@playwright/test';

const NONCE = /nonce="[^"]*"/g;

/** Renders the page twice on the server: a difference means a request saw state the previous one left behind. */
export async function expectSameServerRender(request: APIRequestContext, url: string): Promise<void> {
  const first = await renderOnServer(request, url);
  const second = await renderOnServer(request, url);

  expect(second, 'The second render differs from the first').toBe(first);
}

async function renderOnServer(request: APIRequestContext, url: string): Promise<string> {
  const response = await request.get(url);

  return (await response.text()).replaceAll(NONCE, 'nonce=""');
}

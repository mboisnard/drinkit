import { randomUUID } from 'node:crypto';
import { expect, test } from 'vitest';
import { AUTH_ENDPOINTS } from '../shared/auth-endpoints.ts';

export interface AuthBackendContractOptions {
  /** The URL of the API, such as `http://localhost:8080/drinkit`, read once the backend runs. */
  apiUrl: () => string;
  /** The path under the API that answers the current user. */
  currentUserPath: string;
  /** The code the backend sent to the email, read where the backend sends it. */
  codeOf: (email: string) => Promise<string>;
}

type Call = (method: 'GET' | 'POST', path: string, body?: object) => Promise<Response>;

/**
 * What the frontend relies on in the sign-in protocol of the backend, run on `authBackendDouble` by the tests of this
 * starter, and on the real backend to check that the double still tells the truth.
 */
export function authBackendContract({ apiUrl, currentUserPath, codeOf }: AuthBackendContractOptions): void {
  async function signIn(call: Call, email: string): Promise<Response> {
    await call('GET', '/api/auth/csrf');
    await call('POST', AUTH_ENDPOINTS.code, { email });

    return call('POST', AUTH_ENDPOINTS.codeSignIn, { email, code: await codeOf(email), rememberMe: false });
  }

  test('a visitor gets a 401 on the current user', async () => {
    // When
    const response = await browser(apiUrl())('GET', currentUserPath);

    // Then
    expect(response.status).toBe(401);
  });

  test('the code sent to an email signs that email in', async () => {
    // Given
    const call = browser(apiUrl());

    // When
    const signedIn = await signIn(call, newEmail());

    // Then
    const user = await call('GET', currentUserPath);
    expect({ signedIn: signedIn.status, user: user.status }).toStrictEqual({ signedIn: 204, user: 200 });
    expect(await user.json()).toHaveProperty('roles');
  });

  test('a wrong code is refused with a 401', async () => {
    // Given
    const call = browser(apiUrl());
    const email = newEmail();
    await call('GET', '/api/auth/csrf');
    await call('POST', AUTH_ENDPOINTS.code, { email });
    const wrongCode = (await codeOf(email)) === '000000' ? '111111' : '000000';

    // When
    const response = await call('POST', AUTH_ENDPOINTS.codeSignIn, { email, code: wrongCode, rememberMe: false });

    // Then
    expect(response.status).toBe(401);
  });

  test('signing out ends the session', async () => {
    // Given
    const call = browser(apiUrl());
    await signIn(call, newEmail());

    // When
    await call('POST', AUTH_ENDPOINTS.signOut);

    // Then
    expect((await call('GET', currentUserPath)).status).toBe(401);
  });

  test('a write without the CSRF token is refused', async () => {
    // When
    const response = await browser(apiUrl())('POST', AUTH_ENDPOINTS.code, { email: newEmail() });

    // Then
    expect(response.status).toBe(403);
  });
}

function newEmail(): string {
  return `contract-${randomUUID()}@drinkit.dev`;
}

// What a browser on the app's origin does: its cookies, the CSRF token copied from its cookie, same-origin metadata
function browser(apiUrl: string): Call {
  const cookies = new Map<string, string>();

  return async (method, path, body) => {
    const headers = new Headers({ 'sec-fetch-site': 'same-origin', 'content-type': 'application/json' });
    headers.set('cookie', [...cookies].map(([name, value]) => `${name}=${value}`).join('; '));
    const token = cookies.get('XSRF-TOKEN');
    if (token && method === 'POST') {
      headers.set('x-xsrf-token', token);
    }

    const response = await fetch(`${apiUrl}${path}`, { method, headers, body: body && JSON.stringify(body) });
    for (const setCookie of response.headers.getSetCookie()) {
      const [pair = ''] = setCookie.split(';');
      const [name = '', value = ''] = pair.split('=');
      if (/max-age=0/i.test(setCookie)) {
        cookies.delete(name);
      } else {
        cookies.set(name, value);
      }
    }

    return response;
  };
}

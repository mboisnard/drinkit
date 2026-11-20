import { registerEndpoint } from '@nuxt/test-utils/runtime';
import { readBody, setResponseHeader, setResponseStatus } from 'h3';
import type { H3Event } from 'h3';

interface User {
  firstname?: string;
  roles: string[];
}

/** The auth endpoints of the backend in the Nuxt test environment, with what they received. */
export interface AuthBackend {
  user: User | null;
  readsOfCurrentUser: number;
  bodies: Record<string, unknown[]>;
  /** The code that signs in: any other is refused. */
  code: string;
  /** The user the right code signs in. */
  signsIn: User;
}

export function authBackend(): AuthBackend {
  const backend: AuthBackend = { user: null, readsOfCurrentUser: 0, bodies: {}, code: '123456', signsIn: { roles: [] } };

  registerEndpoint('/drinkit/api/user/me', {
    method: 'GET',
    handler: (event) => {
      backend.readsOfCurrentUser++;

      return backend.user ?? unauthorized(event);
    },
  });
  registerEndpoint('/drinkit/api/auth/code', { method: 'POST', handler: noContent(backend, 'code') });
  registerEndpoint('/drinkit/api/auth/code/sign-in', {
    method: 'POST',
    handler: async (event) => {
      const body = await readBody<{ code: string }>(event);
      (backend.bodies['sign-in'] ??= []).push(body);
      if (body.code !== backend.code) {
        return unauthorized(event);
      }
      backend.user = backend.signsIn;
      setResponseStatus(event, 204);

      return null;
    },
  });
  registerEndpoint('/drinkit/api/auth/logout', {
    method: 'POST',
    handler: (event) => {
      backend.user = null;

      return noContent(backend, 'logout')(event);
    },
  });

  return backend;
}

function noContent(backend: AuthBackend, name: string): (event: H3Event) => Promise<null> {
  return async (event) => {
    (backend.bodies[name] ??= []).push(await readBody(event).catch(() => undefined));
    setResponseStatus(event, 204);

    return null;
  };
}

function unauthorized(event: H3Event): { title: string; status: number } {
  setResponseStatus(event, 401, 'Unauthorized');
  setResponseHeader(event, 'content-type', 'application/problem+json');

  return { title: 'Unauthorized', status: 401 };
}

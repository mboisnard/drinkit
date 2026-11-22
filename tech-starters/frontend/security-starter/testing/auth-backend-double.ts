import { randomInt, randomUUID } from 'node:crypto';
import { http, HttpResponse } from 'msw';
import type { RequestHandler } from 'msw';
import { AUTH_ENDPOINTS } from '../shared/auth-endpoints.ts';

/** The path under the API that writes the CSRF cookie, `apiClient.csrfPath` of the app. */
export const CSRF_PATH = '/api/auth/csrf';

export interface AuthBackendDoubleOptions<User extends { roles: readonly string[] }> {
  /** The path of the API, `apiClient.path` of the app, such as `/drinkit`. */
  apiPath: string;
  /** The path under the API that answers the current user, `auth.currentUserPath` of the app. */
  currentUserPath: string;
  /** The account an email the double has never seen signs in to, as the backend creates it. */
  newUser: (email: string) => User;
}

interface SignInRequest {
  email: string;
  code: string;
  rememberMe: boolean;
}

export interface AuthBackendDouble<User> {
  /** The handlers to give the fake backend of `@drinkit/test-starter`. */
  handlers: RequestHandler[];
  /** The last code the double "sent" to an email, which a test types as the user reads it in their mailbox. */
  lastCodeOf: (email: string) => string | undefined;
  /** The cookies of a session already open for the user, to add to a browser before its first page. */
  signedInAs: (user: User) => Array<{ name: string; value: string }>;
  /** The user of a request's session, for the handlers of the app's own endpoints. */
  userOf: (request: Request) => User | undefined;
}

/**
 * The sign-in protocol of the backend `security-starter`, kept in memory: a code by email, the session and
 * remember-me cookies, the CSRF token each write must echo, and the same 401 for every refused code. Its contract,
 * `authBackendContract`, runs on it and on the real backend.
 */
export function authBackendDouble<User extends { roles: readonly string[] }>(
  options: AuthBackendDoubleOptions<User>,
): AuthBackendDouble<User> {
  const accounts = new Map<string, User>();
  const codes = new Map<string, string>();
  const sessions = new Map<string, User>();
  const rememberedUsers = new Map<string, User>();

  function api(path: string): string {
    return `${options.apiPath}${path}`;
  }

  function openSession(user: User): string {
    const session = randomUUID();
    sessions.set(session, user);

    return session;
  }

  function userOf(request: Request): User | undefined {
    return sessions.get(cookieOf(request, 'SESSION') ?? '');
  }

  return {
    lastCodeOf: (email) => codes.get(email),
    signedInAs: (user) => [{ name: 'SESSION', value: openSession(user) }],
    userOf,
    handlers: [
      http.get(api(CSRF_PATH), () => new HttpResponse(null, {
        status: 204,
        headers: { 'set-cookie': cookie('XSRF-TOKEN', randomUUID(), { httpOnly: false }) },
      })),
      http.post(api(AUTH_ENDPOINTS.code), async ({ request }) => {
        if (!echoesCsrfToken(request)) {
          return problem(403, 'Forbidden');
        }
        const { email } = await request.json() as { email: string };
        codes.set(email, String(randomInt(1_000_000)).padStart(6, '0'));

        return new HttpResponse(null, { status: 204 });
      }),
      http.post(api(AUTH_ENDPOINTS.codeSignIn), async ({ request }) => {
        if (!echoesCsrfToken(request)) {
          return problem(403, 'Forbidden');
        }
        const { email, code, rememberMe } = await request.json() as SignInRequest;
        if (!codes.has(email) || codes.get(email) !== code) {
          return problem(401, 'Unauthorized');
        }
        codes.delete(email);

        const user = accounts.get(email) ?? options.newUser(email);
        accounts.set(email, user);
        const headers = new Headers();
        headers.append('set-cookie', cookie('SESSION', openSession(user)));
        headers.append('set-cookie', cookie('XSRF-TOKEN', randomUUID(), { httpOnly: false }));
        if (rememberMe) {
          const token = randomUUID();
          rememberedUsers.set(token, user);
          headers.append('set-cookie', cookie('remember-me', token));
        }

        return new HttpResponse(null, { status: 204, headers });
      }),
      http.post(api(AUTH_ENDPOINTS.signOut), ({ request }) => {
        if (!echoesCsrfToken(request)) {
          return problem(403, 'Forbidden');
        }
        sessions.delete(cookieOf(request, 'SESSION') ?? '');
        rememberedUsers.delete(cookieOf(request, 'remember-me') ?? '');
        const headers = new Headers();
        for (const name of ['SESSION', 'remember-me', 'XSRF-TOKEN']) {
          headers.append('set-cookie', `${name}=; Max-Age=0; Path=/`);
        }

        return new HttpResponse(null, { status: 204, headers });
      }),
      http.get(api(options.currentUserPath), ({ request }) => {
        const user = userOf(request);
        if (user) {
          return HttpResponse.json(user);
        }

        // As Spring's remember-me services: a new session for the remembered user
        const remembered = rememberedUsers.get(cookieOf(request, 'remember-me') ?? '');
        if (!remembered) {
          return problem(401, 'Unauthorized');
        }

        return HttpResponse.json(remembered, { headers: { 'set-cookie': cookie('SESSION', openSession(remembered)) } });
      }),
    ],
  };
}

// The names of the dev profile: the app under test runs on http, where a browser refuses a __Host- cookie
function cookie(name: string, value: string, { httpOnly } = { httpOnly: true }): string {
  return `${name}=${value}; Path=/; SameSite=Lax${httpOnly ? '; HttpOnly' : ''}`;
}

function cookieOf(request: Request, name: string): string | undefined {
  const pairs = (request.headers.get('cookie') ?? '').split(';').map((pair) => pair.trim().split('='));

  return pairs.find(([cookieName]) => cookieName === name)?.[1];
}

function echoesCsrfToken(request: Request): boolean {
  const token = cookieOf(request, 'XSRF-TOKEN');

  return Boolean(token) && request.headers.get('x-xsrf-token') === token;
}

function problem(status: number, title: string): Response {
  return HttpResponse.json({ title, status }, { status, headers: { 'content-type': 'application/problem+json' } });
}

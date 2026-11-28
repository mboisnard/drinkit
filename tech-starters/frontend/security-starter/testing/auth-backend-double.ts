import { randomBytes, randomInt, randomUUID } from 'node:crypto';
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

/** A passkey of a user, as the app's own endpoint lists it. */
export interface Passkey {
  id: string;
  label: string;
  created: string;
  lastUsed: string;
}

interface PasskeyRegistration {
  publicKey?: { credential?: { id?: string }; label?: string };
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
  /** The passkeys of a request's user, oldest first, for the app's endpoint that lists them. */
  passkeysOf: (request: Request) => Passkey[];
}

/**
 * The sign-in protocol of the backend `security-starter`, kept in memory: a code by email, passkeys, the session and
 * remember-me cookies, the CSRF token each write must echo, and the same 401 for every refused code. Its contract,
 * `authBackendContract`, runs on it and on the real backend.
 *
 * shortcut: a passkey is known by its id alone, neither its signature nor its challenge is checked; the UI tests
 * prove the ceremony with Chromium's virtual authenticator, the real backend proves the cryptography.
 */
export function authBackendDouble<User extends { roles: readonly string[] }>(
  options: AuthBackendDoubleOptions<User>,
): AuthBackendDouble<User> {
  const accounts = new Map<string, User>();
  const codes = new Map<string, string>();
  const sessions = new Map<string, User>();
  const rememberedUsers = new Map<string, User>();
  const passkeys = new Map<string, Passkey & { owner: User }>();
  const userHandles = new Map<User, string>();

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

  // As the backend after any sign-in: a new session id and a new XSRF token
  function signedIn(user: User): Headers {
    const headers = new Headers();
    headers.append('set-cookie', cookie('SESSION', openSession(user)));
    headers.append('set-cookie', cookie('XSRF-TOKEN', randomUUID(), { httpOnly: false }));

    return headers;
  }

  return {
    lastCodeOf: (email) => codes.get(email),
    signedInAs: (user) => [{ name: 'SESSION', value: openSession(user) }],
    userOf,
    passkeysOf: (request) => [...passkeys.values()]
      .filter(({ owner }) => owner === userOf(request))
      .map(({ id, label, created, lastUsed }) => ({ id, label, created, lastUsed })),
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
        const headers = signedIn(user);
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
      // As Spring's filters: a bare status, and 400 rather than 401 to a visitor who asks to create a passkey
      http.post(api(AUTH_ENDPOINTS.passkeyCreationOptions), ({ request }) => {
        if (!echoesCsrfToken(request)) {
          return problem(403, 'Forbidden');
        }
        const user = userOf(request);
        if (!user) {
          return new HttpResponse(null, { status: 400 });
        }
        const userHandle = userHandles.get(user) ?? base64url(16);
        userHandles.set(user, userHandle);

        return HttpResponse.json({
          rp: { id: RP_ID, name: RP_ID },
          user: { id: userHandle, name: userHandle, displayName: userHandle },
          challenge: base64url(32),
          pubKeyCredParams: [{ type: 'public-key', alg: -7 }, { type: 'public-key', alg: -257 }],
          authenticatorSelection: { residentKey: 'required', userVerification: 'required' },
          attestation: 'none',
        });
      }),
      http.post(api(AUTH_ENDPOINTS.passkeyCreation), async ({ request }) => {
        if (!echoesCsrfToken(request)) {
          return problem(403, 'Forbidden');
        }
        const owner = userOf(request);
        if (!owner) {
          return problem(401, 'Unauthorized');
        }
        const { publicKey } = await request.json() as PasskeyRegistration;
        const id = publicKey?.credential?.id;
        if (!id || !publicKey.label) {
          return new HttpResponse(null, { status: 400 });
        }
        const now = new Date().toISOString();
        passkeys.set(id, { id, label: publicKey.label, created: now, lastUsed: now, owner });

        return HttpResponse.json({ success: true });
      }),
      http.delete(api(`${AUTH_ENDPOINTS.passkeyCreation}/:id`), ({ request, params }) => {
        if (!echoesCsrfToken(request)) {
          return problem(403, 'Forbidden');
        }
        const user = userOf(request);
        if (!user) {
          return problem(401, 'Unauthorized');
        }
        const passkey = passkeys.get(String(params.id));
        if (passkey && passkey.owner !== user) {
          return new HttpResponse(null, { status: 403 });
        }
        passkeys.delete(String(params.id));

        return new HttpResponse(null, { status: 204 });
      }),
      http.post(api(AUTH_ENDPOINTS.passkeySignInOptions), ({ request }) => {
        if (!echoesCsrfToken(request)) {
          return problem(403, 'Forbidden');
        }

        return HttpResponse.json({ challenge: base64url(32), rpId: RP_ID, userVerification: 'required' });
      }),
      http.post(api(AUTH_ENDPOINTS.passkeySignIn), async ({ request }) => {
        if (!echoesCsrfToken(request)) {
          return problem(403, 'Forbidden');
        }
        const { id } = await request.json() as { id?: string };
        const passkey = passkeys.get(id ?? '');
        if (!passkey) {
          return new HttpResponse(null, { status: 401 });
        }
        passkey.lastUsed = new Date().toISOString();

        return HttpResponse.json({ authenticated: true, redirectUrl: '/' }, { headers: signedIn(passkey.owner) });
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

// The host of the app under test: a browser refuses an IP address as relying party id, so the tests open localhost
const RP_ID = 'localhost';

function base64url(size: number): string {
  return randomBytes(size).toString('base64url');
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

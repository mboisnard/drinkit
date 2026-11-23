import type { BrowserContext, Page } from '@playwright/test';
import { http, HttpResponse } from 'msw';
import { idempotentEndpoint } from '@drinkit/api-client-starter/testing';
import type { IdempotentEndpoint } from '@drinkit/api-client-starter/testing';
import { ofrepBackendDouble } from '@drinkit/feature-management-starter/testing';
import type { EvaluatedFlag, OfrepBackendDouble } from '@drinkit/feature-management-starter/testing';
import { authBackendDouble } from '@drinkit/security-starter/testing';
import type { AuthBackendDouble } from '@drinkit/security-starter/testing';
import type { FakeBackend } from '@drinkit/test-starter/fake-backend';
import { expect } from '@drinkit/test-starter/playwright';

/** The user of the current user path, as the contract describes it. */
export interface User {
  firstname?: string;
  roles: string[];
  locale?: string;
}

const API_PATH = '/drinkit';
const ME_PATH = '/api/user/me';

export const CURRENT_USER_PATH = `${API_PATH}${ME_PATH}`;

export const EMAIL = 'camille@drinkit.dev';

export function member(): User {
  return { firstname: 'Camille', roles: ['ROLE_USER'] };
}

export const BORDEAUX = {
  id: '6f1c2b8e-0d1a-4c55-9a40-3c1f3b1b7a01',
  name: 'Cave de Bordeaux',
  location: { city: 'Bordeaux', country: { name: 'France', code: 'FR' }, point: { longitude: -0.58, latitude: 44.84 } },
};

export interface CellarBackend extends AuthBackendDouble<User> {
  /** Opens a session for the user in the browser, before its first page. */
  openSession: (context: BrowserContext, baseURL: string | undefined, user: User) => Promise<void>;
  cellarDeletion: IdempotentEndpoint;
  /** The flags and variants the backend evaluates for a signed-in user and for a visitor, none by default. */
  flags: { user: EvaluatedFlag[]; visitor: EvaluatedFlag[] };
  exposures: OfrepBackendDouble['exposures'];
}

/**
 * The backend of DrinkIt for the UI tests: its sign-in, the cellars of a user and their deletion, the completion
 * of an account, its language, and the feature flags.
 */
export function cellarBackend(
  fakeBackend: FakeBackend,
  newUser: () => User = () => ({ roles: ['ROLE_REGISTRATION_IN_PROGRESS'] }),
): CellarBackend {
  const auth = authBackendDouble<User>({
    apiPath: API_PATH,
    currentUserPath: ME_PATH,
    newUser,
  });
  const cellars = [BORDEAUX];
  const flags: CellarBackend['flags'] = { user: [], visitor: [] };
  const ofrep = ofrepBackendDouble({
    apiPath: API_PATH,
    evaluate: (request) => (auth.userOf(request) ? flags.user : flags.visitor),
  });

  function isUser(request: Request): boolean {
    return auth.userOf(request)?.roles.includes('ROLE_USER') === true;
  }

  const cellarDeletion = idempotentEndpoint(({ request, params }) => {
    if (!isUser(request)) {
      return problem(401, 'Unauthorized');
    }
    const index = cellars.findIndex((cellar) => cellar.id === params.cellarId);
    if (index < 0) {
      return problem(404, 'Not Found');
    }
    cellars.splice(index, 1);

    return new HttpResponse(null, { status: 204 });
  });

  fakeBackend.use(
    ...auth.handlers,
    ...ofrep.handlers,
    http.get('/drinkit/api/cellars', ({ request }) => {
      if (!isUser(request)) {
        return problem(401, 'Unauthorized');
      }

      return HttpResponse.json({ cellars });
    }),
    http.delete('/drinkit/api/cellars/:cellarId', cellarDeletion.resolver),
    http.put('/drinkit/api/user/me/locale', async ({ request }) => {
      const user = auth.userOf(request);
      if (!user) {
        return problem(401, 'Unauthorized');
      }
      Object.assign(user, await request.json());

      return new HttpResponse(null, { status: 204 });
    }),
    http.post('/drinkit/api/registration/user/information', async ({ request }) => {
      const user = auth.userOf(request);
      if (!user) {
        return problem(401, 'Unauthorized');
      }
      Object.assign(user, await request.json(), { roles: ['ROLE_USER'] });

      return new HttpResponse(null, { status: 200 });
    }),
  );

  async function openSession(context: BrowserContext, baseURL: string | undefined, user: User): Promise<void> {
    await context.addCookies(auth.signedInAs(user).map((cookie) => ({ ...cookie, url: baseURL })));
  }

  return { ...auth, openSession, cellarDeletion, flags, exposures: ofrep.exposures };
}

/** Signs in on the sign-in page with the code the backend sent to EMAIL. */
export async function signInByCode(page: Page, backend: AuthBackendDouble<User>, rememberMe = false): Promise<void> {
  await page.getByLabel('Email').fill(EMAIL);
  await page.getByRole('button', { name: 'Send me a code' }).click();
  await expect(page.getByLabel('Code')).toBeVisible();
  await page.getByLabel('Code').fill(backend.lastCodeOf(EMAIL) ?? '');
  await page.getByLabel('Keep me signed in on this device').setChecked(rememberMe);
  await page.getByRole('button', { name: 'Sign in' }).click();
}

function problem(status: number, title: string): Response {
  return HttpResponse.json({ title, status }, { status, headers: { 'content-type': 'application/problem+json' } });
}

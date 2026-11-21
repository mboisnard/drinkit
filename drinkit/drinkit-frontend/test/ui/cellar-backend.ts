import { http, HttpResponse } from 'msw';
import { idempotentEndpoint } from '@drinkit/api-client-starter/testing';
import type { IdempotentEndpoint } from '@drinkit/api-client-starter/testing';
import { authBackendDouble } from '@drinkit/security-starter/testing';
import type { AuthBackendDouble } from '@drinkit/security-starter/testing';
import type { FakeBackend } from '@drinkit/test-starter/fake-backend';

/** The user of the current user path, as the contract describes it. */
export interface User {
  firstname?: string;
  roles: string[];
  locale?: string;
}

export const BORDEAUX = {
  id: '6f1c2b8e-0d1a-4c55-9a40-3c1f3b1b7a01',
  name: 'Cave de Bordeaux',
  location: { city: 'Bordeaux', country: { name: 'France', code: 'FR' }, point: { longitude: -0.58, latitude: 44.84 } },
};

export interface CellarBackend extends AuthBackendDouble<User> {
  cellarDeletion: IdempotentEndpoint;
}

/**
 * The backend of DrinkIt for the UI tests: its sign-in, the cellars of a user and their deletion, and the completion
 * of an account.
 */
export function cellarBackend(
  fakeBackend: FakeBackend,
  newUser: () => User = () => ({ roles: ['ROLE_REGISTRATION_IN_PROGRESS'] }),
): CellarBackend {
  const auth = authBackendDouble<User>({
    apiPath: '/drinkit',
    currentUserPath: '/api/user/me',
    newUser,
  });
  const cellars = [BORDEAUX];

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
    http.get('/drinkit/api/cellars', ({ request }) => {
      if (!isUser(request)) {
        return problem(401, 'Unauthorized');
      }

      return HttpResponse.json({ cellars });
    }),
    http.delete('/drinkit/api/cellars/:cellarId', cellarDeletion.resolver),
    http.post('/drinkit/api/registration/user/information', async ({ request }) => {
      const user = auth.userOf(request);
      if (!user) {
        return problem(401, 'Unauthorized');
      }
      Object.assign(user, await request.json(), { roles: ['ROLE_USER'] });

      return new HttpResponse(null, { status: 200 });
    }),
  );

  return { ...auth, cellarDeletion };
}

function problem(status: number, title: string): Response {
  return HttpResponse.json({ title, status }, { status, headers: { 'content-type': 'application/problem+json' } });
}

import { http, HttpResponse } from 'msw';
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

/** The backend of DrinkIt for the UI tests: its sign-in, the cellars of a user, and the completion of an account. */
export function cellarBackend(
  fakeBackend: FakeBackend,
  newUser: () => User = () => ({ roles: ['ROLE_REGISTRATION_IN_PROGRESS'] }),
): AuthBackendDouble<User> {
  const auth = authBackendDouble<User>({
    apiPath: '/drinkit',
    currentUserPath: '/api/user/me',
    newUser,
  });

  fakeBackend.use(
    ...auth.handlers,
    http.get('/drinkit/api/cellars', ({ request }) => {
      const user = auth.userOf(request);
      if (!user?.roles.includes('ROLE_USER')) {
        return HttpResponse.json({ title: 'Unauthorized', status: 401 }, { status: 401 });
      }

      return HttpResponse.json({ cellars: [BORDEAUX] });
    }),
    http.post('/drinkit/api/registration/user/information', async ({ request }) => {
      const user = auth.userOf(request);
      if (!user) {
        return HttpResponse.json({ title: 'Unauthorized', status: 401 }, { status: 401 });
      }
      Object.assign(user, await request.json(), { roles: ['ROLE_USER'] });

      return new HttpResponse(null, { status: 200 });
    }),
  );

  return auth;
}

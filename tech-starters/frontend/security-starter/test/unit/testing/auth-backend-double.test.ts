import { afterAll, beforeAll } from 'vitest';
import { authBackendContract, authBackendDouble } from '@drinkit/security-starter/testing';
import { startFakeBackend } from '@drinkit/test-starter/fake-backend';
import type { FakeBackend } from '@drinkit/test-starter/fake-backend';

const double = authBackendDouble({
  apiPath: '/drinkit',
  currentUserPath: '/api/user/me',
  newUser: () => ({ roles: ['ROLE_USER'] }),
});
let backend: FakeBackend;

beforeAll(async () => {
  backend = await startFakeBackend(...double.handlers);
});

afterAll(async () => {
  await backend.close();
});

authBackendContract({
  apiUrl: () => `${backend.url}/drinkit`,
  currentUserPath: '/api/user/me',
  codeOf: async (email) => Promise.resolve(double.lastCodeOf(email) ?? ''),
});

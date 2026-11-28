import { registerEndpoint } from '@nuxt/test-utils/runtime';
import { setResponseStatus } from 'h3';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { usePasskeys } from '@drinkit/security-starter/use-passkeys';

const RP_ID = 'drinkit.test';

let signalled: UnknownCredentialOptions[];

// The browser's WebAuthn, whose authenticator holds a passkey the backend removed
class RemovedPasskey {
  static readonly parseRequestOptionsFromJSON = (options: object): object => options;
  static readonly parseCreationOptionsFromJSON = (options: object): object => options;
  static readonly signalUnknownCredential = (options: UnknownCredentialOptions): Promise<void> => {
    signalled.push(options);

    return Promise.resolve();
  };

  readonly id = 'removed-passkey';

  toJSON(): object {
    return { id: this.id };
  }
}

beforeEach(() => {
  signalled = [];
  vi.stubGlobal('PublicKeyCredential', RemovedPasskey);
  Object.defineProperty(navigator, 'credentials', {
    configurable: true,
    value: { get: () => Promise.resolve(new RemovedPasskey()) },
  });
  registerEndpoint('/drinkit/webauthn/authenticate/options', { method: 'POST', handler: () => ({ challenge: 'c', rpId: RP_ID }) });
  registerEndpoint('/drinkit/login/webauthn', {
    method: 'POST',
    handler: (event) => {
      setResponseStatus(event, 401, 'Unauthorized');

      return null;
    },
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
  Reflect.deleteProperty(navigator, 'credentials');
});

test('a passkey the backend refuses is told unknown to the authenticator, which can then forget it', async () => {
  // When
  const signIn = usePasskeys().signIn();

  // Then
  await expect(signIn).rejects.toBeInstanceOf(ProblemError);
  expect(signalled).toStrictEqual([{ rpId: RP_ID, credentialId: 'removed-passkey' }]);
});

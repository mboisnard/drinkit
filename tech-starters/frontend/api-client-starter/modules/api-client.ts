import { join, resolve } from 'node:path';
import { createClient } from '@hey-api/openapi-ts';
import { addServerHandler, createResolver, defineNuxtModule } from 'nuxt/kit';

export interface ApiClientOptions {
  /** The OpenAPI contract of the app's backend, relative to the app. */
  contract?: string;
  /** The path of the API on the backend, which the browser calls on the app's origin, such as `/drinkit`. */
  path?: string;
  /** The origin of the backend on a developer's machine, which `NUXT_API_CLIENT_BACKEND_URL` replaces elsewhere. */
  backendUrl?: string;
  /** The path under the API that writes the CSRF cookie, such as `/api/auth/csrf`, called before a write without it. */
  csrfPath?: string;
}

/**
 * Generates the client of the app's contract under `.nuxt/openapi`, imported as `#api`, at each prepare and build,
 * and relays the calls of the browser to the path of the API.
 */
export default defineNuxtModule<ApiClientOptions>({
  meta: { name: 'api-client', configKey: 'apiClient' },
  setup({ contract, path, backendUrl, csrfPath = '' }, nuxt) {
    if (!contract) {
      return;
    }
    if (!path?.startsWith('/') || !backendUrl) {
      throw new Error('apiClient needs the path of the API, such as /drinkit, and the backendUrl of the backend');
    }

    nuxt.options.runtimeConfig.apiClient = { backendUrl };
    nuxt.options.runtimeConfig.public.apiClient = { path, csrfPath };
    addServerHandler({ route: `${path}/**`, handler: createResolver(import.meta.url).resolve('../server/relay.ts') });

    const input = resolve(nuxt.options.rootDir, contract);
    const output = join(nuxt.options.buildDir, 'openapi');
    nuxt.options.alias['#api'] = output;

    async function generate(): Promise<void> {
      await createClient({
        input,
        output,
        logs: { level: 'silent' },
        plugins: [
          {
            name: '@hey-api/client-ofetch',
            runtimeConfigPath: '@drinkit/api-client-starter/client-config',
            throwOnError: true,
          },
          '@hey-api/typescript',
          '@hey-api/sdk',
        ],
      });
    }

    // Once Nuxt has cleared its build folder: prepare and dev write the types, a build may not
    nuxt.hook('prepare:types', generate);
    nuxt.hook('build:before', generate);
  },
});

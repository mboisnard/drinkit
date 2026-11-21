import { defineUiTestConfig } from '@drinkit/test-starter/playwright';

// A backend that hangs fails a server render after a second, rather than the minute of a deployed app
process.env.NUXT_API_CLIENT_SERVER_RENDER_TIMEOUT = '1000';

// A built app refuses to start on a local origin, which its links to the other languages would name
process.env.NUXT_PUBLIC_I18N_BASE_URL = 'https://drinkit.test';

export default defineUiTestConfig();

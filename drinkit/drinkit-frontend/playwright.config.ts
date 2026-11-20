import { defineUiTestConfig } from '@drinkit/test-starter/playwright';

// A backend that hangs fails a server render after a second, rather than the minute of a deployed app
process.env.NUXT_API_CLIENT_SERVER_RENDER_TIMEOUT = '1000';

export default defineUiTestConfig();

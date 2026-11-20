import { useRuntimeConfig } from '#imports';
import { createError, defineEventHandler, getRequestWebStream, sendProxy } from 'h3';
import { backendUrlOf } from '../shared/api-path.ts';
import { clientOf, relayHeaders } from '../shared/relay-headers.ts';

/** The calls of the browser, on the app's origin, relayed to the backend with only the headers the API reads. */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event);
  const target = backendUrlOf(config.apiClient.backendUrl, config.public.apiClient.path, event.path);
  if (!target) {
    throw createError({ statusCode: 404 });
  }

  const hasBody = event.method !== 'GET' && event.method !== 'HEAD';
  await sendProxy(event, target.href, {
    headers: relayHeaders(event.headers, clientOf(event), 'browser'),
    fetchOptions: {
      method: event.method,
      body: hasBody ? getRequestWebStream(event) : undefined,
      // A body streamed rather than read whole
      duplex: 'half',
      redirect: 'manual',
    },
  });
});

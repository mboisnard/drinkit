import { useRuntimeConfig } from '#imports';
import { defineEventHandler, getRequestWebStream, isError, send, sendProxy, setResponseStatus } from 'h3';
import type { H3Event } from 'h3';
import type { ProblemDetail } from '@drinkit/nuxt-starter/problem-detail';
import { backendUrlOf } from '../shared/api-path.ts';
import { clientOf, relayHeaders } from '../shared/relay-headers.ts';

const BAD_GATEWAY = 502;
const NOT_FOUND = 404;

/**
 * The calls of the browser, on the app's origin, relayed to the backend with only the headers the API reads. The
 * relay's own refusals are problem details too, so that the browser reads every error of the API the same way.
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event);
  const target = backendUrlOf(config.apiClient.backendUrl, config.public.apiClient.path, event.path);
  if (!target) {
    return refuse(event, NOT_FOUND, 'Not Found');
  }

  const hasBody = event.method !== 'GET' && event.method !== 'HEAD';
  try {
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
  } catch (error) {
    // The error sendProxy throws when the backend gives no answer
    if (isError(error) && error.statusCode === BAD_GATEWAY) {
      return refuse(event, BAD_GATEWAY, 'Bad Gateway');
    }
    throw error;
  }
});

function refuse(event: H3Event, status: number, title: string): Promise<void> {
  const problem: ProblemDetail = { status, title };
  setResponseStatus(event, status);

  return send(event, JSON.stringify(problem), 'application/problem+json');
}

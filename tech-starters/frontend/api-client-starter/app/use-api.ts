import { useNuxtApp, useRequestEvent, useRuntimeConfig } from '#app';
import type { NuxtApp } from '#app';
import { getResponseHeader, setResponseHeader } from 'h3';
import type { H3Event } from 'h3';
import type { FetchContext, FetchHook } from 'ofetch';
import { toProblemDetail } from '@drinkit/nuxt-starter/problem-detail';
import { CookieJar } from '../shared/cookie-jar.ts';
import { clientOf, relayHeaders } from '../shared/relay-headers.ts';
import { ProblemError } from './problem-error.ts';

/** The options every function of the generated client takes, from the page or component that calls it. */
export interface ApiOptions {
  baseUrl: string;
  onRequest: FetchHook;
  onResponse: FetchHook;
}

const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS', 'TRACE'];

/**
 * Calls of the browser go through the relay of the same origin. Calls of the server render go to the backend with
 * the cookies of the page, and give the page the cookies the backend sets.
 */
export function useApi(): ApiOptions {
  const nuxtApp = useNuxtApp();
  const event = useRequestEvent();
  const config = useRuntimeConfig();
  const apiPath = config.public.apiClient.path;

  if (import.meta.server && event) {
    return serverRenderApi(nuxtApp, event, `${config.apiClient.backendUrl}${apiPath}`);
  }

  return {
    baseUrl: apiPath,
    onRequest: ({ options }) => {
      withCsrfToken(options, new CookieJar(document.cookie));
    },
    onResponse: async ({ response }) => {
      await throwProblem(nuxtApp, response);
    },
  };
}

async function throwProblem(nuxtApp: NuxtApp, response: FetchContext['response']): Promise<void> {
  if (response && !response.ok) {
    const problem = toProblemDetail({ status: response.status, statusText: response.statusText, data: response._data });
    await nuxtApp.callHook('drinkit:api:problem', problem);

    throw new ProblemError(response, problem);
  }
}

function serverRenderApi(nuxtApp: NuxtApp, event: H3Event, baseUrl: string): ApiOptions {
  const jar = cookieJarOf(event);
  const client = clientOf(event);

  function onRequest({ options }: FetchContext): void {
    for (const [name, value] of relayHeaders(event.headers, client, 'server-render')) {
      options.headers.set(name, value);
    }
    options.headers.set('cookie', jar.cookieHeader());
    withCsrfToken(options, jar);
  }

  function keepCookies({ response }: FetchContext): void {
    if (response) {
      jar.receive(response.headers.getSetCookie());
      const pageSetCookies = [getResponseHeader(event, 'set-cookie') ?? []].flat().map(String);
      setResponseHeader(event, 'set-cookie', jar.setCookies(pageSetCookies));
    }
  }

  return {
    baseUrl,
    onRequest,
    onResponse: async (context) => {
      keepCookies(context);
      await throwProblem(nuxtApp, context.response);
    },
  };
}

function cookieJarOf(event: H3Event): CookieJar {
  const context = event.context as { apiCookieJar?: CookieJar };
  context.apiCookieJar ??= new CookieJar(event.headers.get('cookie') ?? '');

  return context.apiCookieJar;
}

function isWrite(options: FetchContext['options']): boolean {
  return !SAFE_METHODS.includes(options.method?.toUpperCase() ?? 'GET');
}

// The __Host- cookie first: another site of the domain could plant the one without the prefix
function csrfTokenOf(jar: CookieJar): string | undefined {
  return jar.value('__Host-XSRF-TOKEN') ?? jar.value('XSRF-TOKEN');
}

function withCsrfToken(options: FetchContext['options'], jar: CookieJar): void {
  const token = csrfTokenOf(jar);
  if (token && isWrite(options)) {
    options.headers.set('x-xsrf-token', token);
  }
}

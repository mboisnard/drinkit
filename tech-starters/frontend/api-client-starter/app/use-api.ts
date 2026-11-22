import { useNuxtApp, useRequestEvent, useRuntimeConfig } from '#app';
import type { NuxtApp } from '#app';
import { getResponseHeader, setResponseHeader } from 'h3';
import type { H3Event } from 'h3';
import type { FetchContext, FetchHook } from 'ofetch';
import { useTranslate } from '@drinkit/i18n-starter/translate';
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
  const { path: apiPath, csrfPath } = config.public.apiClient;
  const { language } = useTranslate();

  if (import.meta.server && event) {
    const { backendUrl, serverRenderTimeout } = config.apiClient;

    return serverRenderApi(nuxtApp, event, { baseUrl: `${backendUrl}${apiPath}`, csrfPath, serverRenderTimeout, language });
  }

  return {
    baseUrl: apiPath,
    onRequest: async ({ options }) => {
      // The language of the page, not that of the browser: the URL decides it
      const acceptLanguage = language();
      if (needsCsrfToken(options, new CookieJar(document.cookie), csrfPath)) {
        await $fetch(csrfPath, { baseURL: apiPath, headers: { 'accept-language': acceptLanguage } });
      }
      withCsrfToken(options, new CookieJar(document.cookie));
      options.headers.set('accept-language', acceptLanguage);
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

interface ServerRenderOptions {
  baseUrl: string;
  csrfPath: string;
  serverRenderTimeout: number;
  language: () => string;
}

function serverRenderApi(
  nuxtApp: NuxtApp,
  event: H3Event,
  { baseUrl, csrfPath, serverRenderTimeout, language }: ServerRenderOptions,
): ApiOptions {
  const jar = cookieJarOf(event);
  const client = clientOf(event);

  function onRequest({ options }: FetchContext): void {
    for (const [name, value] of relayHeaders(event.headers, client, 'server-render')) {
      options.headers.set(name, value);
    }
    options.headers.set('cookie', jar.cookieHeader());
    options.headers.set('accept-language', language());
    withCsrfToken(options, jar);
    // A backend that accepts the connection but never answers would leave the page blank for Node's 5 minutes. The
    // browser's calls have none: an action, such as a call to an AI model, may take longer than a page should
    options.timeout = serverRenderTimeout;
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
    onRequest: async (context) => {
      if (needsCsrfToken(context.options, jar, csrfPath)) {
        await $fetch(csrfPath, { baseURL: baseUrl, onRequest, onResponse: keepCookies });
      }
      onRequest(context);
    },
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

export function isWrite(method = 'GET'): boolean {
  return !SAFE_METHODS.includes(method.toUpperCase());
}

// The __Host- cookie first: another site of the domain could plant the one without the prefix
function csrfTokenOf(jar: CookieJar): string | undefined {
  return [jar.value('__Host-XSRF-TOKEN'), jar.value('XSRF-TOKEN')].find(Boolean);
}

// A visitor's first write, or the first after a sign-out, has no token yet: the backend writes it on demand.
// OWASP: ASVS 5.0 V3.5.1, CSRF Prevention Cheat Sheet (cookie-to-header token)
function needsCsrfToken(options: FetchContext['options'], jar: CookieJar, csrfPath: string): boolean {
  return Boolean(csrfPath) && isWrite(options.method) && !csrfTokenOf(jar);
}

function withCsrfToken(options: FetchContext['options'], jar: CookieJar): void {
  const token = csrfTokenOf(jar);
  if (token && isWrite(options.method)) {
    options.headers.set('x-xsrf-token', token);
  }
}

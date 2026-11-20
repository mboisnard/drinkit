import { defineComponent, h } from 'vue';
import { clearError, clearNuxtState, createError, updateAppConfig, useNuxtApp } from '#app';
import { renderSuspended } from '@nuxt/test-utils/runtime';
import UApp from '@nuxt/ui/components/App.vue';
import { fireEvent, screen } from '@testing-library/vue';
import { flushPromises } from '@vue/test-utils';
import { consola } from 'consola';
import type { ConsolaReporter, LogObject } from 'consola';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { withErrorMessage } from '../../app/error-message.ts';
import { errorsOfAnotherHandler } from './plugins/another-error-handler.ts';

const logs: LogObject[] = [];
const reporter: ConsolaReporter = { log: (log) => logs.push(log) };
const consoleReporters = consola.options.reporters;
const reports: Array<{ error: unknown; fatal: boolean }> = [];
let stopListening: () => void;

beforeEach(() => {
  // The toasts of the previous test, which Nuxt UI keeps in a useState
  clearNuxtState();
  logs.length = 0;
  errorsOfAnotherHandler.length = 0;
  reports.length = 0;
  consola.setReporters([reporter]);
  stopListening = useNuxtApp().hook('drinkit:error', ({ error, fatal }) => {
    reports.push({ error, fatal });
  });
});

afterEach(async () => {
  consola.setReporters(consoleReporters);
  stopListening();
  await clearError();
});

// A button of the app whose handler fails, as an action the user clicks. renderSuspended mounts it in a Vue app of
// its own, which gets the error handler of the app's
async function clickFailing(action: () => Promise<void>): Promise<void> {
  const { errorHandler } = useNuxtApp().vueApp.config;
  await renderSuspended(
    defineComponent({ render: () => h(UApp, () => h('button', { onClick: action }, 'Save')) }),
    { global: { config: { errorHandler } } },
  );
  await fireEvent.click(screen.getByRole('button', { name: 'Save' }));
}

function named(name: string, message: string): Error {
  return Object.assign(new Error(message), { name });
}

function refusal(status: number, traceId?: string): ProblemError {
  return new ProblemError({ status, statusText: 'Refused' }, { status, title: 'Refused', traceId });
}

test('an unhandled refusal is logged and reported once, and shown with the message of its caller and its trace id', async () => {
  // Given
  const failure = refusal(500, '4bf92f3577b34da6');
  const save = withErrorMessage(() => Promise.reject(failure), 'The cellar was not saved.');

  // When
  await clickFailing(save);

  // Then
  await screen.findByText('The cellar was not saved.');
  expect({
    reference: screen.getByText('Reference: 4bf92f3577b34da6').textContent,
    logs: logs.map(({ type, tag, args }) => ({ type, tag, fields: args[1] as unknown })),
    reports,
  }).toStrictEqual({
    reference: 'Reference: 4bf92f3577b34da6',
    logs: [{ type: 'error', tag: 'drinkit', fields: { status: 500, title: 'Refused', traceId: '4bf92f3577b34da6' } }],
    reports: [{ error: failure, fatal: false }],
  });
});

test('a fatal error is logged once and left to the error page', async () => {
  // Given
  // Nuxt keeps the thrown error as the cause of the one its error page shows, without its problem
  const failure = createError({ status: 500, statusText: 'Broken', fatal: true, cause: refusal(500, 'b7ad6b7169203331') });

  // When
  await clickFailing(() => Promise.reject(failure));

  // Then
  await vi.waitFor(() => {
    expect(logs).not.toHaveLength(0);
  });
  expect({
    logs: logs.map(({ args }) => args[1] as unknown),
    reports: reports.map(({ fatal }) => fatal),
    toast: screen.queryByText('Something went wrong. Try again.'),
  }).toStrictEqual({
    logs: [{ status: 500, title: 'Refused', traceId: 'b7ad6b7169203331' }],
    reports: [true],
    toast: null,
  });
});

test('an error no component handles, such as a promise of a timer, is reported too', async () => {
  // Given
  const failure = refusal(500, '00f067aa0ba902b7');
  await renderSuspended(defineComponent({ render: () => h(UApp) }));

  // When: what the browser dispatches, which happy-dom lacks
  window.dispatchEvent(Object.assign(new Event('unhandledrejection', { cancelable: true }), { reason: failure }));

  // Then
  expect((await screen.findByText('Reference: 00f067aa0ba902b7')).textContent).toBe('Reference: 00f067aa0ba902b7');
});

test('a value thrown that is no error, such as a string, is still logged and shown', async () => {
  // Given
  await renderSuspended(defineComponent({ render: () => h(UApp) }));

  // When
  window.dispatchEvent(Object.assign(new Event('unhandledrejection', { cancelable: true }), { reason: 'Not saved' }));

  // Then
  await screen.findByText('Something went wrong. Try again.');
  expect(logs.map(({ args }) => (args[2] as Error).message)).toStrictEqual(['Not saved']);
});

test('the error handler another layer installed still receives each error of a component', async () => {
  // Given
  const failure = refusal(500);

  // When
  await clickFailing(() => Promise.reject(failure));

  // Then
  await screen.findByText('Something went wrong. Try again.');
  expect(errorsOfAnotherHandler).toStrictEqual([failure]);
});

test('an action wrapped twice shows the message of the outermost wrapper', async () => {
  // Given
  const save = withErrorMessage(() => Promise.reject(refusal(500)), 'The bottle was not saved.');
  const saveFromThePage = withErrorMessage(save, 'The cellar was not saved.');

  // When
  await clickFailing(saveFromThePage);

  // Then
  expect((await screen.findByText('The cellar was not saved.')).textContent).toBe('The cellar was not saved.');
});

test.each([
  { failure: refusal(401), message: 'Your session has ended. Sign in again.' },
  { failure: refusal(500), message: 'Something went wrong. Try again.' },
  { failure: refusal(502), message: 'The server is unavailable. Try again in a moment.' },
  { failure: named('FetchError', '[POST] "/drinkit/api/cellars": <no response> Failed to fetch'), message: 'The server cannot be reached. Check your connection and try again.' },
  { failure: refusal(409), message: 'A cellar already has this name.' },
])('without a message of its caller, an unhandled error shows the message the app gives its status: $message', async ({ failure, message }) => {
  // Given
  updateAppConfig({ observability: { errorMessages: { 409: 'A cellar already has this name.' } } });

  // When
  await clickFailing(() => Promise.reject(failure));

  // Then
  expect((await screen.findByText(message)).textContent).toBe(message);
});

test('a cancelled call is neither logged nor shown', async () => {
  // When
  await clickFailing(() => Promise.reject(named('AbortError', 'The operation was aborted.')));

  // Then
  await flushPromises();
  expect({ logs, reports }).toStrictEqual({ logs: [], reports: [] });
});

test('a listener of drinkit:error that fails is logged once, without reporting its own error again', async () => {
  // Given
  const stopFailing = useNuxtApp().hook('drinkit:error', () => {
    throw new Error('The alerting tool is down');
  });

  // When
  await clickFailing(() => Promise.reject(refusal(500)));

  // Then
  await vi.waitFor(() => {
    expect(logs).toHaveLength(2);
  });
  stopFailing();
  await flushPromises();
  expect({ logs: logs.map(({ args }) => args[0] as unknown), reports: reports.length })
    .toStrictEqual({ logs: ['Unhandled error', 'A listener of drinkit:error failed'], reports: 1 });
});

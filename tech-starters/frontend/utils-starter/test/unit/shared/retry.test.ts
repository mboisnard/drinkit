import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { retry } from '@drinkit/utils-starter/retry';

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
});

/** A run that fails with each given error in turn, then answers `done`, and counts its calls. */
function runFailing(...errors: Error[]): { run: () => Promise<string>; calls: () => number } {
  let calls = 0;

  return {
    run: () => {
      calls++;
      const error = errors.shift();

      return error ? Promise.reject(error) : Promise.resolve('done');
    },
    calls: () => calls,
  };
}

test('a run that succeeds is not repeated', async () => {
  // Given
  const { run, calls } = runFailing();

  // When
  const result = await retry(run, { retries: 3, delayMs: 1000 });

  // Then
  expect(result).toBe('done');
  expect(calls()).toBe(1);
});

test('a failed run runs again once the delay is over', async () => {
  // Given
  const { run, calls } = runFailing(new Error('busy'));

  // When
  const result = retry(run, { retries: 3, delayMs: 1000 });
  await vi.advanceTimersByTimeAsync(999);

  // Then
  expect(calls()).toBe(1);
  await vi.advanceTimersByTimeAsync(1);
  await expect(result).resolves.toBe('done');
  expect(calls()).toBe(2);
});

test('the last error is thrown once the retries are spent', async () => {
  // Given
  const { run, calls } = runFailing(new Error('first'), new Error('second'), new Error('third'));

  // When
  const outcome = retry(run, { retries: 2, delayMs: 10 }).catch((error: unknown) => error);
  await vi.runAllTimersAsync();

  // Then
  expect(await outcome).toStrictEqual(new Error('third'));
  expect(calls()).toBe(3);
});

test('an error retryIf refuses is thrown at once', async () => {
  // Given
  const { run, calls } = runFailing(new Error('refused'));

  // When & Then
  await expect(
    retry(run, { retries: 3, delayMs: 10, retryIf: (error) => (error as Error).message !== 'refused' }),
  ).rejects.toThrow('refused');
  expect(calls()).toBe(1);
});

test('a delay given as a function is read before each wait', async () => {
  // Given
  const { run, calls } = runFailing(new Error('first'), new Error('second'));
  const delays = [100, 500];

  // When
  const result = retry(run, { retries: 3, delayMs: () => delays.shift() ?? 0 });
  await vi.advanceTimersByTimeAsync(100);
  await vi.advanceTimersByTimeAsync(499);

  // Then
  expect(calls()).toBe(2);
  await vi.advanceTimersByTimeAsync(1);
  await expect(result).resolves.toBe('done');
});

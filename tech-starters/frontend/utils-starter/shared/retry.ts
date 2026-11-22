export interface RetryOptions {
  /** How many times `run` runs again after its first failure. */
  retries: number;
  /** The wait before each new run, read again before each one when it is a function, such as a `Retry-After`. */
  delayMs: number | (() => number);
  /** Whether an error is worth another run. Every error is, by default. */
  retryIf?: (error: unknown) => boolean;
}

/** Runs `run` until it succeeds, waiting between runs, and throws its last error once the retries are spent. */
export async function retry<T>(
  run: () => Promise<T>,
  { retries, delayMs, retryIf = () => true }: RetryOptions,
): Promise<T> {
  for (let retried = 0; ; retried++) {
    try {
      return await run();
    } catch (error) {
      if (retried >= retries || !retryIf(error)) {
        throw error;
      }

      const delay = typeof delayMs === 'number' ? delayMs : delayMs();
      await new Promise((resolve) => setTimeout(resolve, delay));
    }
  }
}

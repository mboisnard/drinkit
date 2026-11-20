const messages = new WeakMap<object, string>();

/**
 * The same action, whose failure shows the user this message rather than the default of its status. The error
 * still reaches its caller as it was thrown, so that a caller may catch it.
 */
export function withErrorMessage<A extends unknown[], R>(
  action: (...args: A) => Promise<R>,
  message: string,
): (...args: A) => Promise<R> {
  return async (...args) => {
    try {
      return await action(...args);
    } catch (error) {
      if (error instanceof Object) {
        messages.set(error, message);
      }
      throw error;
    }
  };
}

/** The message a caller gave the failure, if any. */
export function errorMessageOf(error: unknown): string | undefined {
  return error instanceof Object ? messages.get(error) : undefined;
}

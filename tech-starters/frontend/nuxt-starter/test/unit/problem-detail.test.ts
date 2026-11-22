import { expect, test } from 'vitest';
import { toProblemDetail } from '@drinkit/nuxt-starter/problem-detail';

test('an error carrying a problem detail shows its title and detail', () => {
  // Given
  const error = { status: 409, statusText: 'Conflict', data: { title: 'Cellar full', detail: 'No free slot left' } };

  // When
  const problem = toProblemDetail(error);

  // Then
  expect(problem).toStrictEqual({ status: 409, title: 'Cellar full', detail: 'No free slot left' });
});

test('an error without a problem detail shows its status text as the title', () => {
  // Given
  const error = { status: 404, statusText: 'Page not found: /cellars/42', data: undefined };

  // When
  const problem = toProblemDetail(error);

  // Then
  expect(problem).toStrictEqual({ status: 404, title: 'Page not found: /cellars/42' });
});

test('data that is not a problem detail is never shown', () => {
  // Given
  const error = { status: 500, statusText: 'Server Error', data: { stack: 'at render (server.mjs:1)' } };

  // When
  const problem = toProblemDetail(error);

  // Then
  expect(problem).toStrictEqual({ status: 500, title: 'Server Error' });
});

test('the status of the response wins over the status of the body', () => {
  // Given
  const error = { status: 502, statusText: 'Bad Gateway', data: { title: 'Backend down', status: undefined } };

  // When
  const problem = toProblemDetail(error);

  // Then
  expect(problem).toStrictEqual({ status: 502, title: 'Backend down' });
});

test('a body whose title is not text is never shown', () => {
  // Given
  const error = { status: 500, statusText: 'Server Error', data: { title: 42, detail: 'Boom' } };

  // When
  const problem = toProblemDetail(error);

  // Then
  expect(problem).toStrictEqual({ status: 500, title: 'Server Error' });
});

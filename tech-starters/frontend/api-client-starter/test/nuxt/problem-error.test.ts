import { createError } from '#app';
import { expect, test } from 'vitest';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { toProblemDetail } from '@drinkit/nuxt-starter/problem-detail';

test('a refused call nobody catches shows the problem of the backend on the error page', () => {
  // Given
  const refused = new ProblemError({ status: 409, statusText: 'Conflict' }, { title: 'Cellar full', status: 409 });

  // When
  const problem = toProblemDetail(createError(refused));

  // Then
  expect(problem).toStrictEqual({ title: 'Cellar full', status: 409 });
});

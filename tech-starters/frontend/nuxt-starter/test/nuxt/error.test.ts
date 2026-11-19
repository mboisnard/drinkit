import { createError, useRouter } from '#app';
import { renderSuspended } from '@nuxt/test-utils/runtime';
import { fireEvent, screen } from '@testing-library/vue';
import { expect, test } from 'vitest';
import ErrorPage from '../../app/error.vue';

test('the error page heads with the status and the title of the problem', async () => {
  // Given
  const error = createError({ status: 409, statusText: 'Conflict', data: { title: 'Cellar full' } });

  // When
  await renderSuspended(ErrorPage, { props: { error } });

  // Then
  expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('409 Cellar full');
});

test('the error page takes the user back to the home page', async () => {
  // Given
  await renderSuspended(ErrorPage, { props: { error: createError({ status: 404 }) }, route: '/cellars/42' });

  // When
  await fireEvent.click(screen.getByRole('button', { name: 'Back to the home page' }));

  // Then
  await expect.poll(() => useRouter().currentRoute.value.path).toBe('/');
});

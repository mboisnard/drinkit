import { expect, test } from '@drinkit/test-starter/playwright';

function directivesOf(policy: string | undefined): Map<string, string> {
  return new Map((policy ?? '').split(';').map((directive) => {
    const [name = '', ...sources] = directive.trim().split(/\s+/);

    return [name, sources.join(' ')];
  }));
}

test('no page, not even one of the app, may show a page in a frame', async ({ request }) => {
  // When
  const page = await request.get('/');

  // Then
  expect(directivesOf(page.headers()['content-security-policy']).get('frame-ancestors')).toBe('\'none\'');
  expect(page.headers()['x-frame-options']).toBe('DENY');
});

test('a page reaches by default only its own origin', async ({ request }) => {
  // When
  const page = await request.get('/');

  // Then
  const policy = directivesOf(page.headers()['content-security-policy']);
  expect(policy.get('default-src')).toBe('\'self\'');
  expect(policy.get('connect-src')).toBe('\'self\'');
});

test('a page runs only the scripts its response signs', async ({ request }) => {
  // When
  const page = await request.get('/');

  // Then
  expect(directivesOf(page.headers()['content-security-policy']).get('script-src'))
    .toMatch(/^'self' 'strict-dynamic' 'nonce-[^']+'$/);
});

test('a page applies only its own styles and those its response signs', async ({ request }) => {
  // When
  const page = await request.get('/');

  // Then
  expect(directivesOf(page.headers()['content-security-policy']).get('style-src')).toMatch(/^'self' 'nonce-[^']+'$/);
});

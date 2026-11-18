import { posix } from 'node:path';
import { defineNuxtModule } from 'nuxt/kit';

// A page is served without a trailing slash, as /en/engineering, so a browser resolves its relative links from
// /en/ and loses a segment. Each relative link to a page becomes the absolute route Nuxt Content gives that page.
export default defineNuxtModule({
  meta: { name: 'absolute-links' },
  setup(_options, nuxt) {
    nuxt.hook('content:file:beforeParse', ({ file }) => {
      if (!file.id.endsWith('.md')) return;

      const folder = posix.dirname(file.id.split('/').slice(1).join('/'));
      file.body = file.body.replace(
        /(\]\(|\]:[ \t]*)((?![a-z]+:|\/)[^)\s#]+\.md)(#[^)\s]*)?/g,
        (_match, lead: string, target: string, anchor?: string) => lead + routeOf(posix.join(folder, target)) + (anchor ?? ''),
      );
    });
  },
});

function routeOf(path: string) {
  const parts = path.replace(/\.md$/, '').split('/').map((part) => part.replace(/^\d+\./, ''));
  return '/' + parts.filter((part) => part !== 'index').join('/');
}

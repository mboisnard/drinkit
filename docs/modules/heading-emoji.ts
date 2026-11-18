import { defineNuxtModule } from 'nuxt/kit';

type Node = [string, Record<string, unknown>, ...unknown[]];

const leadingEmoji = /^(\p{Extended_Pictographic}(?:️|‍\p{Extended_Pictographic}|\p{Emoji_Modifier})*)\s+/u;

// The markdown keeps the emoji of a ## heading, which GitHub shows. The site moves it into an emoji prop, which
// app/components/content/ProseH2.vue draws as the icon of its family, and leaves it out of the outline.
export default defineNuxtModule({
  meta: { name: 'heading-emoji' },
  setup(_options, nuxt) {
    nuxt.hook('content:file:afterParse', ({ content }) => {
      const body = content.body as { value?: Node[]; toc?: { links?: Array<{ text: string }> } } | undefined;

      for (const node of body?.value ?? []) {
        const match = node[0] === 'h2' && typeof node[2] === 'string' ? (leadingEmoji.exec(node[2])) : null;
        if (match) {
          node[1].emoji = match[1];
          node[2] = (node[2] as string).slice(match[0].length);
        }
      }

      for (const link of body?.toc?.links ?? []) {
        link.text = link.text.replace(leadingEmoji, '');
      }
    });
  },
});

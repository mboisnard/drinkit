import { defineNuxtModule } from 'nuxt/kit';

// A heading that opens with an emoji such as 🛤️ keeps its invisible variation selector at the start of its id, where
// the links between pages expect the words alone: #lanes, not #️-lanes. The ids of headings and of the outline drop it.
export default defineNuxtModule({
  meta: { name: 'heading-anchors' },
  setup(_options, nuxt) {
    nuxt.hook('content:file:afterParse', ({ content }) => {
      dropVariationSelector(content);
    });
  },
});

function dropVariationSelector(value: unknown) {
  if (Array.isArray(value)) {
    value.forEach(dropVariationSelector);
    return;
  }

  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>;
    for (const [key, child] of Object.entries(record)) {
      if (key === 'id' && typeof child === 'string') {
        record[key] = child.replace(/^️+-?/, '');
      } else {
        dropVariationSelector(child);
      }
    }
  }
}

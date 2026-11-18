interface MarkdownNode {
  type: string;
  name?: string;
  value?: string;
  children?: MarkdownNode[];
  data?: Record<string, unknown>;
}

// The pages write callouts as GitHub alerts, which GitHub renders too, and the site renders each one as the Nuxt UI
// callout that an MDC ::note would give. Nuxt UI has no Important callout: the generic one stands in for it.
const callouts: Record<string, { hName: string; hProperties?: Record<string, string> }> = {
  NOTE: { hName: 'note' },
  TIP: { hName: 'tip' },
  IMPORTANT: { hName: 'callout', hProperties: { icon: 'i-lucide-message-square-warning', color: 'primary' } },
  WARNING: { hName: 'warning' },
  CAUTION: { hName: 'caution' },
};

const marker = /^\[?!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]?$/;

export function githubAlerts(): (tree: MarkdownNode) => void {
  return (tree) => {
    visit(tree);
  };
}

function visit(parent: MarkdownNode): void {
  for (const child of parent.children ?? []) {
    if (child.type === 'blockquote') {
      toCallout(child);
    }

    visit(child);
  }
}

function toCallout(quote: MarkdownNode): void {
  const paragraph = quote.children?.[0];
  if (paragraph?.type !== 'paragraph' || paragraph.children === undefined) {
    return;
  }

  const kind = takeMarker(paragraph.children);
  if (kind === undefined) {
    return;
  }

  if (paragraph.children.length === 0) {
    quote.children?.shift();
  }

  quote.data = { ...quote.data, ...callouts[kind] };
}

// MDC reads [!NOTE] as a span holding !NOTE, plain markdown as the start of a text
function takeMarker(children: MarkdownNode[]): string | undefined {
  const first = children[0];
  if (first === undefined) {
    return undefined;
  }

  const text = markerText(first);
  const kind = text === undefined ? undefined : marker.exec(text)?.[1];
  if (text === undefined || kind === undefined) {
    return undefined;
  }

  if (first.type === 'text') {
    first.value = first.value?.slice(text.length);
  } else {
    children.shift();
  }

  const next = children[0];
  if (next?.type === 'text') {
    next.value = next.value?.replace(/^\s+/, '');
    if (!next.value) {
      children.shift();
    }
  }

  if (children[0]?.type === 'break') {
    children.shift();
  }

  return kind;
}

function markerText(first: MarkdownNode): string | undefined {
  if (first.type === 'text') {
    return first.value?.split(/\s/)[0];
  }

  const span = first.name === 'span' ? first.children : undefined;
  return span?.length === 1 && span[0]?.type === 'text' ? span[0].value : undefined;
}

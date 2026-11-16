import type { Blockquote, Parent, PhrasingContent, Root } from 'mdast'

// The pages write callouts as GitHub alerts, which GitHub renders too, and the site renders each one as the Nuxt UI
// callout that an MDC ::note would give. Nuxt UI has no Important callout: the generic one stands in for it.
const callouts: Record<string, { hName: string, hProperties?: Record<string, string> }> = {
  NOTE: { hName: 'note' },
  TIP: { hName: 'tip' },
  IMPORTANT: { hName: 'callout', hProperties: { icon: 'i-lucide-message-square-warning', color: 'primary' } },
  WARNING: { hName: 'warning' },
  CAUTION: { hName: 'caution' },
}

const marker = /^\[?!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]?$/

export default function githubAlerts() {
  return (tree: Root) => visit(tree)
}

function visit(parent: Parent) {
  for (const child of parent.children) {
    if (child.type === 'blockquote') {
      toCallout(child)
    }

    if ('children' in child) {
      visit(child)
    }
  }
}

function toCallout(quote: Blockquote) {
  const paragraph = quote.children[0]
  if (paragraph?.type !== 'paragraph') {
    return
  }

  const kind = takeMarker(paragraph.children)
  if (!kind) {
    return
  }

  if (!paragraph.children.length) {
    quote.children.shift()
  }

  quote.data = { ...quote.data, ...callouts[kind] }
}

// MDC reads [!NOTE] as a span holding !NOTE, plain markdown as the start of a text
function takeMarker(children: PhrasingContent[]) {
  const first = children[0] as PhrasingContent & { name?: string }
  const span = first?.type !== 'text' && first?.name === 'span' && 'children' in first ? first.children : undefined
  const text = span?.length === 1 && span[0]?.type === 'text' ? span[0].value : first?.type === 'text' ? first.value.split(/\s/)[0] : undefined
  const kind = text?.match(marker)?.[1]
  if (!kind || !first) {
    return undefined
  }

  if (first.type === 'text') {
    first.value = first.value.slice(text!.length)
  }
  else {
    children.shift()
  }

  const next = children[0]
  if (next?.type === 'text') {
    next.value = next.value.replace(/^\s+/, '')
    if (!next.value) {
      children.shift()
    }
  }

  if (children[0]?.type === 'break') {
    children.shift()
  }

  return kind
}

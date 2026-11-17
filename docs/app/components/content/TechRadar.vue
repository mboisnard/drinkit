<template>
  <div class="not-prose my-8 flex flex-col gap-6">
    <UTabs v-model="view" :items="views" :content="false" class="self-end" />

    <div v-if="view === 'radar'" class="grid items-start gap-8 lg:grid-cols-[minmax(0,1fr)_17rem]">
      <div class="flex min-w-0 flex-col gap-2">
        <div class="flex justify-between gap-4">
          <QuadrantTitle :name="quadrants[1]" :count="countIn(quadrants[1])" />
          <QuadrantTitle :name="quadrants[0]" :count="countIn(quadrants[0])" class="text-right" />
        </div>

        <div class="relative">
          <svg :viewBox="`0 0 ${SIZE} ${SIZE}`" role="group" :aria-label="`${quadrants.length} quadrants, ${entries.length} entries`" class="block h-auto w-full">
            <circle v-for="(ring, r) in [...RINGS].reverse()" :key="ring.name" :cx="CENTER" :cy="CENTER" :r="ring.radius" :class="r % 2 ? 'band-muted' : 'band'" />
            <path :d="`M${CENTER - OUTER} ${CENTER}H${CENTER + OUTER}M${CENTER} ${CENTER - OUTER}V${CENTER + OUTER}`" class="axis" />

            <template v-for="(ring, r) in RINGS" :key="ring.name">
              <text v-for="side in [-1, 1]" :key="side" :x="CENTER + side * ringMiddle(r)" :y="CENTER + 3.5" :class="ring.css" class="ring-name">{{ ring.name }}</text>
            </template>

            <a
              v-for="blip in blips"
              :key="blip.number"
              :href="href(blip.link)"
              :target="external(blip.link) ? '_blank' : undefined"
              :aria-label="`${blip.number}. ${blip.label}, ${blip.ring.label}`"
              :class="blip.ring.css"
              class="blip"
              @mouseenter="active = blip"
              @mouseleave="active = undefined"
              @focus="active = blip"
              @blur="active = undefined"
            >
              <circle v-if="blip.moved" :cx="blip.x" :cy="blip.y" r="13.5" :stroke-dasharray="blip.moved < 0 ? '3 3' : undefined" class="halo" />
              <circle :cx="blip.x" :cy="blip.y" r="9.5" class="dot" />
              <text :x="blip.x" :y="blip.y + 3.3" class="number">{{ blip.number }}</text>
            </a>
          </svg>

          <div
            v-if="active"
            class="card"
            :style="{ left: `${active.x / SIZE * 100}%`, top: `${active.y / SIZE * 100}%`, translate: active.x < CENTER ? '20px -50%' : 'calc(-100% - 20px) -50%' }"
          >
            <span class="flex items-center gap-2">
              <b class="text-highlighted">{{ active.number }} · {{ active.label }}</b>
              <RingBadge :ring="active.ring" />
            </span>
            <span v-if="active.description">{{ active.description }}</span>
            <span v-if="active.moved" :class="active.ring.css" class="flex items-center gap-1 font-semibold text-(--ring)">
              <UIcon :name="active.moved > 0 ? 'i-lucide-arrow-up' : 'i-lucide-arrow-down'" />
              Moved {{ active.moved > 0 ? 'in' : 'out' }} from {{ movedFrom(active) }}
            </span>
          </div>
        </div>

        <div class="flex justify-between gap-4">
          <QuadrantTitle :name="quadrants[2]" :count="countIn(quadrants[2])" />
          <QuadrantTitle :name="quadrants[3]" :count="countIn(quadrants[3])" class="text-right" />
        </div>
      </div>

      <div class="flex flex-col gap-5 text-sm lg:pt-12">
        <div v-for="ring in RINGS" :key="ring.name" :class="ring.css" class="flex gap-3">
          <span class="mt-1 size-3.5 shrink-0 rounded-full bg-(--ring)" />
          <span>
            <b class="text-highlighted">{{ ring.label }}</b> <span class="font-mono text-xs text-muted">{{ countOf(ring) }}</span><br>
            <span class="text-toned">{{ ring.meaning }}</span>
          </span>
        </div>

        <div class="flex flex-col gap-3 rounded-xl border border-default p-4 text-toned">
          <b class="text-highlighted">Movement</b>
          <span v-for="move in [1, -1]" :key="move" class="flex items-center gap-2.5">
            <svg width="26" height="26" viewBox="0 0 26 26" aria-hidden="true" :class="RINGS[move > 0 ? 1 : 3].css">
              <circle cx="13" cy="13" r="11.5" :stroke-dasharray="move < 0 ? '3 3' : undefined" class="halo" />
              <circle cx="13" cy="13" r="7.5" class="dot" />
            </svg>
            Moved {{ move > 0 ? 'in, closer to Adopt' : 'out, away from Adopt' }}
          </span>
          <span>Hover or tab to a blip for its card, Enter opens its page.</span>
        </div>
      </div>
    </div>

    <div v-else class="flex flex-col gap-4">
      <div class="flex flex-wrap items-center gap-2">
        <UButton
          v-for="filter in ringFilters"
          :key="filter.label"
          :variant="ringFilter === filter.ring ? 'solid' : 'soft'"
          color="neutral"
          size="sm"
          class="rounded-full"
          @click="ringFilter = filter.ring"
        >
          {{ filter.label }} <span class="font-mono text-xs opacity-70">{{ filter.count }}</span>
        </UButton>
        <UInput v-model="nameFilter" icon="i-lucide-search" placeholder="Filter by name" class="ms-auto min-w-56" />
      </div>

      <section v-for="group in groups" :key="group.quadrant">
        <h3 class="flex items-baseline gap-2 border-b border-default pt-4 pb-2 font-display text-lg font-bold">
          {{ group.quadrant }} <span class="font-mono text-xs font-normal text-muted">{{ group.blips.length }}</span>
        </h3>
        <ULink
          v-for="blip in group.blips"
          :key="blip.number"
          :to="blip.link"
          :target="external(blip.link) ? '_blank' : undefined"
          :class="blip.ring.css"
          class="grid grid-cols-[28px_minmax(0,14rem)_1fr_auto_16px] items-center gap-3.5 rounded-lg border-b border-muted px-2 py-2.5 text-default hover:bg-elevated max-sm:grid-cols-[28px_1fr_auto]"
        >
          <span class="flex size-6 items-center justify-center rounded-full bg-(--ring) font-mono text-[11px] text-inverted">{{ blip.number }}</span>
          <span class="flex flex-col">
            <b class="font-semibold text-highlighted">{{ blip.label }}</b>
            <span v-if="blip.moved" class="flex items-center gap-1 text-xs font-semibold text-(--ring)">
              <UIcon :name="blip.moved > 0 ? 'i-lucide-arrow-up' : 'i-lucide-arrow-down'" />
              moved {{ blip.moved > 0 ? 'in' : 'out' }}
            </span>
          </span>
          <span class="text-sm text-toned max-sm:hidden">{{ blip.description }}</span>
          <RingBadge :ring="blip.ring" />
          <UIcon name="i-lucide-arrow-right" class="text-dimmed max-sm:hidden" />
        </ULink>
      </section>

      <p v-if="!groups.length" class="text-sm text-muted">No entry matches.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'

interface TechRadarEntry {
  quadrant: string
  ring: string
  label: string
  description?: string
  link: string
  moved?: number
}

const props = defineProps<{
  quadrants: string[]
  entries: TechRadarEntry[]
}>()

const SIZE = 600
const CENTER = SIZE / 2
const OUTER = 290
const BLIP_SPACE = 14

const RINGS = [
  { name: 'ADOPT', label: 'Adopt', css: 'ring-adopt', radius: 135, meaning: 'Our default: use it without asking.' },
  { name: 'TRIAL', label: 'Trial', css: 'ring-trial', radius: 195, meaning: 'In use on one part, judged on what it gives.' },
  { name: 'ASSESS', label: 'Assess', css: 'ring-assess', radius: 245, meaning: 'Worth a spike before any decision.' },
  { name: 'HOLD', label: 'Hold', css: 'ring-hold', radius: OUTER, meaning: 'Start nothing new with it.' },
]
type Ring = typeof RINGS[number]

// Quadrants turn from the top right to the bottom right, as their titles stand around the radar
const QUADRANT_START = [-Math.PI / 2, -Math.PI, Math.PI / 2, 0]

const view = ref<'radar' | 'list'>('radar')
const views = [
  { label: 'Radar', icon: 'i-lucide-radar', value: 'radar' },
  { label: 'List', icon: 'i-lucide-list', value: 'list' },
]

const blips = computed(() => {
  let number = 0

  return props.quadrants.flatMap((quadrant, q) => RINGS.flatMap((ring, r) => {
    const inSegment = props.entries.filter(entry => entry.quadrant === quadrant && entry.ring === ring.name)

    return inSegment.map((entry, i) => ({ ...entry, ring, number: ++number, ...place(q, r, i, inSegment.length) }))
  }))
})
type Blip = typeof blips.value[number]

const active = ref<Blip>()

// A Fibonacci lattice spreads the blips of one segment evenly, and puts each one at the same place at every build
function place(quadrant: number, ring: number, index: number, count: number) {
  const inner = ring === 0 ? 2 * BLIP_SPACE : RINGS[ring - 1]!.radius + BLIP_SPACE
  const outer = RINGS[ring]!.radius - BLIP_SPACE
  const radius = Math.sqrt(inner ** 2 + (index + 0.5) / count * (outer ** 2 - inner ** 2))
  const margin = BLIP_SPACE / radius
  const angle = QUADRANT_START[quadrant]! + margin + ((index + 0.5) * 0.618034 % 1) * (Math.PI / 2 - 2 * margin)

  // Node and the browser differ on the last digit of a cosine: rounded, the server's page hydrates unchanged
  return { x: round(CENTER + radius * Math.cos(angle)), y: round(CENTER + radius * Math.sin(angle)) }
}

function round(coordinate: number) {
  return Math.round(coordinate * 10) / 10
}

function ringMiddle(ring: number) {
  return ((RINGS[ring - 1]?.radius ?? 0) + RINGS[ring]!.radius) / 2
}

function movedFrom(blip: Blip) {
  return RINGS[RINGS.findIndex(ring => ring.name === blip.ring.name) + (blip.moved ?? 0)]?.label
}

const countIn = (quadrant?: string) => props.entries.filter(entry => entry.quadrant === quadrant).length
const countOf = (ring: Ring) => props.entries.filter(entry => entry.ring === ring.name).length

const ringFilter = ref<string>()
const nameFilter = ref('')
const ringFilters = computed(() => [
  { label: 'All', ring: undefined, count: props.entries.length },
  ...RINGS.map(ring => ({ label: ring.label, ring: ring.name, count: countOf(ring) })),
])

const groups = computed(() => props.quadrants
  .map(quadrant => ({
    quadrant,
    blips: blips.value.filter(blip => blip.quadrant === quadrant
      && (!ringFilter.value || blip.ring.name === ringFilter.value)
      && blip.label.toLowerCase().includes(nameFilter.value.trim().toLowerCase())),
  }))
  .filter(group => group.blips.length))

const router = useRouter()

const external = (link: string) => !link.startsWith('/')
const href = (link: string) => external(link) ? link : router.resolve(link).href

const QuadrantTitle = (attrs: { name?: string, count: number }) => h('span', { class: 'flex flex-col' }, [
  h('b', { class: 'font-display font-bold text-highlighted' }, attrs.name),
  h('span', { class: 'text-xs text-muted' }, `${attrs.count} ${attrs.count === 1 ? 'entry' : 'entries'}`),
])

const RingBadge = (attrs: { ring: Ring }) => h('span', { class: `ring-badge ${attrs.ring.css}` }, attrs.ring.label)
</script>

<style scoped>
/* The ring colors of the A+ design, the ones of the decision statuses, lighter on the dark background */
.ring-adopt { --ring: #4d7c0f; }
.ring-trial { --ring: #b4500f; }
.ring-assess { --ring: #1d4ed8; }
.ring-hold { --ring: #b91c47; }

.dark .ring-adopt { --ring: #a3e635; }
.dark .ring-trial { --ring: #f0a060; }
.dark .ring-assess { --ring: #60a5fa; }
.dark .ring-hold { --ring: #fb7185; }

.band { fill: var(--ui-bg); stroke: var(--ui-border); }
.band-muted { fill: var(--ui-bg-muted); stroke: var(--ui-border); }
.axis { stroke: var(--ui-border-accented); }

.ring-name {
  fill: var(--ring);
  stroke: var(--ui-bg);
  stroke-width: 4px;
  paint-order: stroke;
  text-anchor: middle;
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 1.2px;
}

.blip { cursor: pointer; outline: none; }
.dot { fill: var(--ring); stroke: var(--ui-bg); stroke-width: 1.5px; }
.halo { fill: none; stroke: var(--ring); stroke-width: 2px; }
.blip .number { fill: var(--ui-bg); text-anchor: middle; font-family: var(--font-mono); font-size: 9px; pointer-events: none; }
.blip:hover .dot, .blip:focus-visible .dot { stroke: var(--ui-text-highlighted); stroke-width: 2px; }

.card {
  position: absolute;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 250px;
  padding: 12px 14px;
  border: 1px solid var(--ui-border);
  border-radius: 14px;
  background: var(--ui-bg);
  box-shadow: 0 16px 32px -12px rgb(23 23 23 / 25%);
  color: var(--ui-text-toned);
  font-size: 13px;
  line-height: 1.5;
  pointer-events: none;
}

.ring-badge {
  padding: 3px 9px;
  border-radius: 999px;
  background: color-mix(in oklab, var(--ring) 12%, transparent);
  color: var(--ring);
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  white-space: nowrap;
}
</style>

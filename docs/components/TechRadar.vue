<template>
  <div class="tech-radar-container">
    <!-- View Toggle -->
    <div class="view-toggle">
      <button
        :class="{ active: currentView === 'radar' }"
        @click="currentView = 'radar'"
      >
        Radar View
      </button>
      <button
        :class="{ active: currentView === 'list' }"
        @click="currentView = 'list'"
      >
        List View
      </button>
    </div>

    <!-- Filters -->
    <div class="filters" v-if="currentView === 'list'">
      <div class="filter-group">
        <label>Ring:</label>
        <select v-model="selectedRing">
          <option value="">All</option>
          <option v-for="ring in Object.keys(rings)" :key="ring" :value="ring">
            {{ ring }}
          </option>
        </select>
      </div>
      <div class="filter-group">
        <label>Quadrant:</label>
        <select v-model="selectedQuadrant">
          <option value="">All</option>
          <option v-for="quadrant in quadrants" :key="quadrant" :value="quadrant">
            {{ quadrant }}
          </option>
        </select>
      </div>
    </div>

    <!-- Radar View -->
    <div v-if="currentView === 'radar'" class="radar-view">
      <svg
        :viewBox="`-450 -450 900 900`"
        class="radar-svg"
        preserveAspectRatio="xMidYMid meet"
      >
        <!-- Grid lines -->
        <line x1="0" y1="-400" x2="0" y2="400" stroke="#e0e0e0" stroke-width="2"/>
        <line x1="-400" y1="0" x2="400" y2="0" stroke="#e0e0e0" stroke-width="2"/>

        <!-- Rings -->
        <g v-for="(ring, index) in ringRadii" :key="index">
          <circle
            cx="0"
            cy="0"
            :r="ring.radius"
            fill="none"
            stroke="#e0e0e0"
            stroke-width="1.5"
          />
          <text
            :y="-ring.radius + 40"
            text-anchor="middle"
            :fill="ring.color"
            opacity="0.3"
            font-size="36"
            font-weight="bold"
          >
            {{ ring.name }}
          </text>
        </g>

          <!-- Quadrant labels at edges within their quadrant -->
          <!-- Top-right quadrant -->
          <text x="200" y="-420" text-anchor="middle" font-size="16" font-weight="bold" fill="#333">
              {{ quadrants[0] }}
          </text>
          <!-- Top-left quadrant -->
          <text x="-200" y="-420" text-anchor="middle" font-size="16" font-weight="bold" fill="#333">
              {{ quadrants[1] }}
          </text>
          <!-- Bottom-left quadrant -->
          <text x="-200" y="435" text-anchor="middle" font-size="16" font-weight="bold" fill="#333">
              {{ quadrants[2] }}
          </text>
          <!-- Bottom-right quadrant -->
          <text x="200" y="435" text-anchor="middle" font-size="16" font-weight="bold" fill="#333">
              {{ quadrants[3] }}
          </text>

        <!-- Blips -->
        <g
          v-for="(entry, index) in positionedEntries"
          :key="index"
          class="blip-group"
          :transform="`translate(${entry.x}, ${entry.y})`"
        >
          <!-- Invisible larger circle for better hover detection -->
          <circle
            r="15"
            fill="transparent"
            class="blip-hover-area"
            @mouseenter="hoveredEntry = entry"
            @mouseleave="hoveredEntry = null"
            @click="handleBlipClick(entry)"
          />

          <!-- Blip shape -->
          <circle
            v-if="entry.moved === 0"
            r="10"
            :fill="entry.color"
            class="blip-circle"
            pointer-events="none"
          />
          <path
            v-else-if="entry.moved > 0"
            d="M -10,5 10,5 0,-13 z"
            :fill="entry.color"
            class="blip-triangle"
            pointer-events="none"
          />
          <path
            v-else
            d="M -10,-5 10,-5 0,13 z"
            :fill="entry.color"
            class="blip-triangle"
            pointer-events="none"
          />

          <!-- Blip number -->
          <text
            y="4"
            text-anchor="middle"
            fill="white"
            font-size="10"
            font-weight="bold"
            pointer-events="none"
          >
            {{ entry.id }}
          </text>

          <!-- Tooltip attached to this blip -->
          <g
            v-if="hoveredEntry && hoveredEntry.label === entry.label"
            class="blip-tooltip"
            pointer-events="none"
          >
            <rect
              :x="-hoveredEntry.label.length * 3.5"
              y="-30"
              :width="hoveredEntry.label.length * 7"
              height="18"
              fill="#333"
              rx="3"
              opacity="0.9"
            />
            <text
              y="-18"
              text-anchor="middle"
              fill="white"
              font-size="12"
            >
              {{ hoveredEntry.label }}
            </text>
          </g>
        </g>
      </svg>

      <!-- Legend -->
      <div class="legend">
        <div v-for="qIndex in [3, 2, 1, 0]" :key="qIndex" class="legend-quadrant">
          <h3>{{ quadrants[qIndex] }}</h3>
          <div v-for="ringName in Object.keys(rings)" :key="ringName" class="legend-ring">
            <h4 :style="{ color: rings[ringName].color }">{{ ringName }}</h4>
            <ul>
              <li
                v-for="(entry, eIndex) in getEntriesForQuadrantRing(qIndex, ringName)"
                :key="eIndex"
                @mouseenter="hoveredEntry = entry"
                @mouseleave="hoveredEntry = null"
                @click="handleBlipClick(entry)"
                class="legend-item"
              >
                <span class="entry-number">{{ entry.id }}.</span>
                <span class="entry-label">{{ entry.label }}</span>
                <span v-if="entry.moved > 0" class="move-indicator">▲</span>
                <span v-if="entry.moved < 0" class="move-indicator">▼</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- List View -->
    <div v-else class="list-view">
      <div class="entries-list">
        <div
          v-for="(entry, index) in filteredEntries"
          :key="index"
          class="entry-card"
          :style="{ borderLeftColor: rings[entry.ring].color }"
        >
          <div class="entry-header">
            <h3>{{ entry.label }}</h3>
            <span class="entry-ring" :style="{ backgroundColor: rings[entry.ring].color }">
              {{ entry.ring }}
            </span>
          </div>
          <div class="entry-meta">
            <span class="entry-quadrant">{{ entry.quadrant }}</span>
            <span v-if="entry.moved > 0" class="entry-moved up">Moved up</span>
            <span v-if="entry.moved < 0" class="entry-moved down">Moved down</span>
          </div>
          <p v-if="entry.description" class="entry-description">{{ entry.description }}</p>
          <a v-if="entry.link" :href="entry.link" target="_blank" class="entry-link">
            Learn more →
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const rings = {
  ADOPT: { name: 'ADOPT', color: '#5ba300', index: 0, radius: 150 },
  TRIAL: { name: 'TRIAL', color: '#009eb0', index: 1, radius: 250 },
  ASSESS: { name: 'ASSESS', color: '#c7ba00', index: 2, radius: 350 },
  HOLD: { name: 'HOLD', color: '#e09b96', index: 3, radius: 450 },
};
type Ring = keyof typeof rings;

interface TechRadarEntry {
  quadrant: string;
  ring: Ring;
  label: string;
  description?: string;
  link?: string;
  moved?: number;
}

interface TechRadarProps {
  quadrants: string[];
  entries: TechRadarEntry[];
}

const props = defineProps<TechRadarProps>();

const currentView = ref<'radar' | 'list'>('radar');
const selectedRing = ref<string>('');
const selectedQuadrant = ref<string>('');
const hoveredEntry = ref<any>(null);

const ringRadii = computed(() =>
  Object.keys(rings).map(key => ({
    name: key,
    radius: rings[key].radius,
    color: rings[key].color
  }))
);

// Position entries in the radar with proper spacing
const positionedEntries = computed(() => {
  // Visual quadrant positions (matching labels in template):
  // quadrants[3] → Top-right (0° to 90°)
  // quadrants[2] → Top-left (90° to 180°)
  // quadrants[1] → Bottom-left (180° to 270°)
  // quadrants[0] → Bottom-right (270° to 360°)

  const quadrantConfig = [
    { visualIndex: 3, min: 0, max: Math.PI / 2 },           // Top-right
    { visualIndex: 2, min: Math.PI / 2, max: Math.PI },     // Top-left
    { visualIndex: 1, min: Math.PI, max: 3 * Math.PI / 2 }, // Bottom-left
    { visualIndex: 0, min: 3 * Math.PI / 2, max: 2 * Math.PI } // Bottom-right
  ];

  const positioned: any[] = [];
  let globalId = 1;

  // Process in visual order: top-right, top-left, bottom-left, bottom-right
  for (const config of quadrantConfig) {
    const quadrantName = props.quadrants[config.visualIndex];

    for (const ringKey of Object.keys(rings) as Ring[]) {
      const ringInfo = rings[ringKey];

      // Get all entries for this quadrant and ring
      const entriesInSegment = props.entries.filter(
        e => e.quadrant === quadrantName && e.ring === ringKey
      );

      if (entriesInSegment.length === 0) continue;

      // Sort alphabetically
      entriesInSegment.sort((a, b) => a.label.localeCompare(b.label));

      // Position each entry
      const minRadius = ringInfo.index === 0 ? 40 : rings[Object.keys(rings)[ringInfo.index - 1] as Ring].radius + 15;
      const maxRadius = ringInfo.radius - 15;
      const angleRange = config.max - config.min;
      const angleStep = angleRange / (entriesInSegment.length + 1);

      entriesInSegment.forEach((entry, idx) => {
        // Deterministic but varied positioning based on label
        const seed = entry.label.split('').reduce((acc: number, char: string) =>
          acc + char.charCodeAt(0), 0);
        const random = (Math.sin(seed) + 1) / 2;

        // Angle: evenly distributed with slight randomness
        const angle = config.min + angleStep * (idx + 1) + (random - 0.5) * angleStep * 0.3;

        // Radius: varied to avoid overlap
        const radiusRange = maxRadius - minRadius;
        const radius = minRadius + radiusRange * (0.3 + random * 0.6);

        positioned.push({
          ...entry,
          x: radius * Math.cos(angle),
          y: radius * Math.sin(angle),
          color: ringInfo.color,
          moved: entry.moved || 0,
          id: globalId++,
          quadrantIndex: config.visualIndex,
          ringIndex: ringInfo.index
        });
      });
    }
  }

  return positioned;
});

const filteredEntries = computed(() => {
  return props.entries.filter(entry => {
    const ringMatch = !selectedRing.value || entry.ring === selectedRing.value;
    const quadrantMatch = !selectedQuadrant.value || entry.quadrant === selectedQuadrant.value;
    return ringMatch && quadrantMatch;
  });
});

function getEntriesForQuadrantRing(quadrantIndex: number, ringName: string) {
  const quadrantName = props.quadrants[quadrantIndex];
  return positionedEntries.value.filter(
    entry => entry.quadrant === quadrantName && entry.ring === ringName
  );
}

function handleBlipClick(entry: any) {
  if (entry.link) {
    window.open(entry.link, '_blank');
  }
}
</script>

<style scoped>
.tech-radar-container {
  width: 100%;
  margin: 2rem 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.view-toggle {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 2px solid #e0e0e0;
}

.view-toggle button {
  padding: 0.75rem 1.5rem;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  color: #666;
  transition: all 0.2s;
}

.view-toggle button:hover {
  color: #333;
}

.view-toggle button.active {
  color: #5ba300;
  border-bottom-color: #5ba300;
}

.filters {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f5f5f5;
  border-radius: 8px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.filter-group label {
  font-weight: 600;
  font-size: 0.9rem;
}

.filter-group select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
}

.radar-view {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.radar-svg {
  width: 100%;
  max-width: 900px;
  height: auto;
  margin-bottom: 2rem;
}

.blip-group {
  /* No transform to prevent movement on hover */
}

.blip-hover-area {
  cursor: pointer;
}

.blip-circle,
.blip-triangle {
  transition: filter 0.2s, opacity 0.2s;
}

.blip-group:hover .blip-circle,
.blip-group:hover .blip-triangle {
  filter: brightness(1.15) drop-shadow(0 0 3px rgba(0, 0, 0, 0.3));
}

.blip-tooltip {
  animation: fadeIn 0.15s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.legend {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 2rem;
  width: 100%;
  max-width: 1200px;
}

.legend-quadrant h3 {
  font-size: 1.2rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

.legend-ring h4 {
  font-size: 1rem;
  font-weight: 600;
  margin: 1rem 0 0.5rem;
}

.legend-ring ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.legend-item {
  padding: 0.4rem 0;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.legend-item:hover {
  background: #f5f5f5;
}

.entry-number {
  font-weight: 600;
  color: #666;
  min-width: 2rem;
}

.entry-label {
  flex: 1;
}

.move-indicator {
  font-size: 0.8rem;
  color: #666;
}

.list-view {
  width: 100%;
}

.entries-list {
  display: grid;
  gap: 1rem;
}

.entry-card {
  padding: 1.5rem;
  background: white;
  border-left: 4px solid;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.entry-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.entry-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  margin-bottom: 0.5rem;
}

.entry-header h3 {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0;
}

.entry-ring {
  padding: 0.25rem 0.75rem;
  color: white;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.entry-meta {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.75rem;
  font-size: 0.9rem;
  color: #666;
}

.entry-quadrant {
  font-weight: 500;
}

.entry-moved {
  font-weight: 600;
}

.entry-moved.up {
  color: #5ba300;
}

.entry-moved.down {
  color: #e09b96;
}

.entry-description {
  margin: 0.75rem 0;
  color: #555;
  line-height: 1.5;
}

.entry-link {
  display: inline-block;
  color: #5ba300;
  text-decoration: none;
  font-weight: 500;
  margin-top: 0.5rem;
}

.entry-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .legend {
    grid-template-columns: 1fr;
  }

  .filters {
    flex-direction: column;
  }
}
</style>

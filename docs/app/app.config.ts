import { defineAppConfig } from '#imports';

// The tiles of the A+ design, for the folders, the ## headings and the drinks of the home page
const tiles: Record<string, string> = {
  blue: 'bg-blue-50 text-blue-700 dark:bg-blue-400/15 dark:text-blue-300',
  cognac: 'bg-cognac-50 text-cognac-700 dark:bg-cognac-400/15 dark:text-cognac-300',
  purple: 'bg-purple-50 text-purple-700 dark:bg-purple-400/15 dark:text-purple-300',
  pink: 'bg-pink-50 text-pink-600 dark:bg-pink-400/15 dark:text-pink-300',
  lime: 'bg-lime-50 text-lime-700 dark:bg-lime-400/15 dark:text-lime-300',
  rose: 'bg-rose-50 text-rose-700 dark:bg-rose-400/15 dark:text-rose-300',
  yellow: 'bg-yellow-50 text-yellow-700 dark:bg-yellow-400/15 dark:text-yellow-300',
  orange: 'bg-orange-50 text-orange-700 dark:bg-orange-400/15 dark:text-orange-300',
  cyan: 'bg-cyan-50 text-cyan-700 dark:bg-cyan-400/15 dark:text-cyan-300',
  slate: 'bg-slate-100 text-slate-600 dark:bg-slate-400/15 dark:text-slate-300',
  neutral: 'bg-neutral-100 text-neutral-700 dark:bg-neutral-400/15 dark:text-neutral-300',
};

// A ## heading's emoji gives its icon and tile: blue explains, lime checks, rose protects
const headingIcons: Record<string, { icon: string; tile: string }> = {
  '📚': { icon: 'i-lucide-book-open', tile: 'blue' },
  '🎓': { icon: 'i-lucide-graduation-cap', tile: 'blue' },
  '🧭': { icon: 'i-lucide-compass', tile: 'blue' },
  '🎯': { icon: 'i-lucide-target', tile: 'blue' },
  '🗺️': { icon: 'i-lucide-map', tile: 'blue' },
  '🛣️': { icon: 'i-lucide-route', tile: 'blue' },
  '🛤️': { icon: 'i-lucide-route', tile: 'blue' },
  '🌐': { icon: 'i-lucide-globe', tile: 'blue' },
  '⚙️': { icon: 'i-lucide-settings-2', tile: 'cognac' },
  '🛠️': { icon: 'i-lucide-wrench', tile: 'cognac' },
  '🔧': { icon: 'i-lucide-wrench', tile: 'cognac' },
  '🧰': { icon: 'i-lucide-toolbox', tile: 'cognac' },
  '🩹': { icon: 'i-lucide-bandage', tile: 'cognac' },
  '🍷': { icon: 'i-lucide-wine', tile: 'cognac' },
  '🔌': { icon: 'i-lucide-plug', tile: 'cognac' },
  '🖥️': { icon: 'i-lucide-monitor', tile: 'cognac' },
  '🧑‍💻': { icon: 'i-lucide-monitor', tile: 'cognac' },
  '➕': { icon: 'i-lucide-circle-plus', tile: 'cognac' },
  '🧩': { icon: 'i-lucide-puzzle', tile: 'purple' },
  '🗂️': { icon: 'i-lucide-folder', tile: 'purple' },
  '📁': { icon: 'i-lucide-folder', tile: 'purple' },
  '🏷️': { icon: 'i-lucide-tag', tile: 'purple' },
  '🧱': { icon: 'i-lucide-layers', tile: 'purple' },
  '🔗': { icon: 'i-lucide-link', tile: 'purple' },
  '📌': { icon: 'i-lucide-pin', tile: 'purple' },
  '📦': { icon: 'i-lucide-package', tile: 'purple' },
  '🗄️': { icon: 'i-lucide-database', tile: 'purple' },
  '🔁': { icon: 'i-lucide-repeat', tile: 'pink' },
  '🔄': { icon: 'i-lucide-repeat', tile: 'pink' },
  '♻️': { icon: 'i-lucide-repeat', tile: 'pink' },
  '🚀': { icon: 'i-lucide-rocket', tile: 'pink' },
  '🔀': { icon: 'i-lucide-git-merge', tile: 'pink' },
  '⚡': { icon: 'i-lucide-zap', tile: 'pink' },
  '🚦': { icon: 'i-lucide-traffic-cone', tile: 'pink' },
  '🔍': { icon: 'i-lucide-search', tile: 'lime' },
  '🔎': { icon: 'i-lucide-search', tile: 'lime' },
  '🧐': { icon: 'i-lucide-search', tile: 'lime' },
  '🩺': { icon: 'i-lucide-stethoscope', tile: 'lime' },
  '🧪': { icon: 'i-lucide-flask-conical', tile: 'lime' },
  '✅': { icon: 'i-lucide-circle-check', tile: 'lime' },
  '🔢': { icon: 'i-lucide-hash', tile: 'lime' },
  '🔒': { icon: 'i-lucide-lock', tile: 'rose' },
  '🔓': { icon: 'i-lucide-lock-open', tile: 'rose' },
  '🛡️': { icon: 'i-lucide-shield', tile: 'rose' },
  '🔑': { icon: 'i-lucide-key-round', tile: 'rose' },
  '🍪': { icon: 'i-lucide-cookie', tile: 'rose' },
  '🙈': { icon: 'i-lucide-eye-off', tile: 'rose' },
  '⚖️': { icon: 'i-lucide-scale', tile: 'yellow' },
  '🪙': { icon: 'i-lucide-coins', tile: 'yellow' },
  '🧮': { icon: 'i-lucide-calculator', tile: 'yellow' },
  '📈': { icon: 'i-lucide-chart-line', tile: 'yellow' },
  '📏': { icon: 'i-lucide-ruler', tile: 'yellow' },
  '✂️': { icon: 'i-lucide-scissors', tile: 'yellow' },
  '🚧': { icon: 'i-lucide-triangle-alert', tile: 'yellow' },
  '🗣️': { icon: 'i-lucide-message-square', tile: 'orange' },
  '🪨': { icon: 'i-lucide-message-square', tile: 'orange' },
  '🤖': { icon: 'i-lucide-bot', tile: 'orange' },
  '🪝': { icon: 'i-lucide-webhook', tile: 'orange' },
  '🧑‍⚖️': { icon: 'i-lucide-gavel', tile: 'orange' },
  '📜': { icon: 'i-lucide-scroll-text', tile: 'slate' },
  '📝': { icon: 'i-lucide-notebook-pen', tile: 'slate' },
  '🧾': { icon: 'i-lucide-receipt', tile: 'slate' },
  '📋': { icon: 'i-lucide-clipboard-list', tile: 'slate' },
  '🔤': { icon: 'i-lucide-type', tile: 'slate' },
  '🐘': { icon: 'i-lucide-box', tile: 'neutral' },
  '🐇': { icon: 'i-lucide-rabbit', tile: 'neutral' },
  '📬': { icon: 'i-lucide-mail', tile: 'neutral' },
};

export default defineAppConfig({
  header: {
    title: 'DrinkIt',
    logo: {
      light: '/drinkit/logo-light.svg',
      dark: '/drinkit/logo-dark.svg',
      alt: 'DrinkIt',
      favicon: '/drinkit/favicon.svg',
    },
  },
  github: {
    url: 'https://github.com/mboisnard/drinkit',
    branch: 'master',
    rootDir: 'docs',
  },
  navigation: {
    sub: 'header',
  },
  tiles,
  headingIcons,
  ui: {
    colors: {
      primary: 'cognac',
      secondary: 'purple',
      neutral: 'neutral',
    },
    button: {
      compoundVariants: [
        { color: 'primary', variant: 'solid', class: 'bg-cognac-500 text-neutral-900 hover:bg-cognac-400' },
      ],
    },
    pageHeader: {
      slots: {
        title: 'font-display font-extrabold tracking-tight',
      },
    },
    pageHero: {
      slots: {
        title: 'font-display font-extrabold tracking-tight',
      },
    },
    pageSection: {
      slots: {
        headline: 'text-xs font-mono uppercase tracking-wider text-primary',
        title: 'font-display font-extrabold tracking-tight',
      },
      variants: {
        orientation: {
          vertical: { headline: 'justify-start', title: 'text-left', description: 'text-left' },
        },
      },
    },
    pageCard: {
      slots: {
        leadingIcon: 'size-9 shrink-0 rounded-lg p-2',
      },
    },
    prose: {
      h1: { slots: { base: 'font-display font-extrabold tracking-tight' } },
      h2: { slots: { base: 'font-display font-extrabold tracking-tight' } },
      h3: { slots: { base: 'font-display font-bold tracking-tight' } },
    },
  },
});

import type { Linter } from 'eslint';

// gitignore patterns, where an unescaped leading # starts a comment
const VUE_APP = ['\\#app', '\\#app/**', '\\#components', '\\#build', '\\#build/**'];
const SERVER = ['\\#server', '\\#server/**'];
const IMPORTS = '\\#imports';

// Nuxt guards these imports in the folders of the app only, not in those of a layer
export function folderImportRules(): Linter.Config[] {
  return [
    restrict('**/shared/**', 'Shared code runs in the Vue app and in Nitro: it imports neither of them',
      [...VUE_APP, ...SERVER, IMPORTS, 'vue', '**/app/**', '**/server/**']),
    restrict('**/app/**', 'The Vue app reaches the server through HTTP: move the code both need to shared/',
      [...SERVER, '**/server/**']),
    restrict('**/server/**', 'The server never runs the Vue app: move the code both need to shared/',
      [...VUE_APP, '**/app/**']),
    restrict('**/modules/**', 'A module runs at build time, where no runtime alias exists',
      [...VUE_APP, ...SERVER, IMPORTS]),
  ];
}

function restrict(folder: string, message: string, group: string[]): Linter.Config {
  return {
    files: [`${folder}/*.{ts,vue}`],
    rules: { 'no-restricted-imports': ['error', { patterns: [{ group, message }] }] },
  };
}

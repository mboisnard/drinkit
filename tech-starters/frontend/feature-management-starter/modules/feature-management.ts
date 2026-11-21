import { addTypeTemplate, defineNuxtModule } from 'nuxt/kit';

export interface FeatureManagementOptions {
  /** The keys of the flags the app reads, evaluated together once per server render. */
  flags?: string[];
  /** The keys of the A/B tests the app runs, each behind the flag of the same key on the backend. */
  experiments?: string[];
}

/** Hands the keys of the app to its runtime, and types `useFlag` and `useVariant` with them. */
export default defineNuxtModule<FeatureManagementOptions>({
  meta: { name: 'feature-management', configKey: 'featureManagement' },
  defaults: { flags: [], experiments: [] },
  setup({ flags = [], experiments = [] }, nuxt) {
    Object.assign(nuxt.options.runtimeConfig.public, { featureManagement: { flags, experiments } });

    addTypeTemplate({
      filename: 'types/feature-management.d.ts',
      getContents: () => [
        keysTypeOf('@drinkit/feature-management-starter/use-flag', 'FlagKeys', flags),
        keysTypeOf('@drinkit/feature-management-starter/use-variant', 'ExperimentKeys', experiments),
        'export {};\n',
      ].join('\n'),
    });
  },
});

function keysTypeOf(module: string, name: string, keys: string[]): string {
  const members = keys.map((key) => `    ${JSON.stringify(key)}: true;`).join('\n');

  return `declare module '${module}' {\n  interface ${name} {\n${members}\n  }\n}\n`;
}

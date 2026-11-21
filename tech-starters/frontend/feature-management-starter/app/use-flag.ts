import { computed } from 'vue';
import type { ComputedRef } from 'vue';
import { useFeatureFlags } from './feature-flags.ts';

/** The flags the app declares in `featureManagement.flags`, which the module adds by declaration merging. */
// eslint-disable-next-line @typescript-eslint/no-empty-object-type -- filled by declaration merging
export interface FlagKeys {}

export type FlagKey = [keyof FlagKeys] extends [never] ? string : Extract<keyof FlagKeys, string>;

/** Whether the flag is on for the user or the visitor, or `defaultValue` when the backend gave no boolean for it. */
export function useFlag(key: FlagKey, defaultValue = false): ComputedRef<boolean> {
  const flags = useFeatureFlags();

  return computed(() => {
    const value = flags.value?.[key]?.value;

    return typeof value === 'boolean' ? value : defaultValue;
  });
}

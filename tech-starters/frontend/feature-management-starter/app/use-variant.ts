import { computed, onMounted, watch } from 'vue';
import type { ComputedRef } from 'vue';
import { useApi } from '@drinkit/api-client-starter/use-api';
import { exposurePathOf } from '../shared/ofrep.ts';
import { useFeatureFlags } from './feature-flags.ts';

/** The A/B tests the app declares in `featureManagement.experiments`, which the module adds by declaration merging. */
// eslint-disable-next-line @typescript-eslint/no-empty-object-type -- filled by declaration merging
export interface ExperimentKeys {}

export type ExperimentKey = [keyof ExperimentKeys] extends [never] ? string : Extract<keyof ExperimentKeys, string>;

const CONTROL = 'control';

/**
 * The variant of the A/B test to render, `control` outside it. Call it in the component that renders in every
 * variant, where the variant takes effect: once mounted, it reports the exposure of a user the backend drew, and again
 * when a sign-in or a sign-out draws another variant, never during a server render, whose visitor may be a bot or a
 * prefetch.
 */
export function useVariant(key: ExperimentKey): ComputedRef<string> {
  const flags = useFeatureFlags();
  const { baseUrl, onRequest, onResponse } = useApi();
  const evaluation = computed(() => flags.value?.[key]);

  function reportExposure(variant: string | undefined): void {
    if (evaluation.value?.reason !== 'SPLIT' || !variant) {
      return;
    }

    // Lost when it fails: the user still sees the variant, and the results count one exposure less
    $fetch(exposurePathOf(key), { method: 'POST', body: { variant }, baseURL: baseUrl, onRequest, onResponse })
      .catch(() => undefined);
  }

  onMounted(() => {
    watch(() => evaluation.value?.variant, reportExposure, { immediate: true });
  });

  return computed(() => evaluation.value?.variant ?? CONTROL);
}

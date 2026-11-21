import type { Ref } from 'vue';
import { useRuntimeConfig, useState } from '#app';
import { useApi } from '@drinkit/api-client-starter/use-api';
import { OFREP_EVALUATION_PATH, ofrepRequestOf } from '../shared/ofrep.ts';
import type { BulkEvaluation, EvaluatedFlag } from '../shared/ofrep.ts';

/** The evaluations by key, `null` until read: the payload of the server render hands them to the browser. */
export function useFeatureFlags(): Ref<Record<string, EvaluatedFlag> | null> {
  return useState<Record<string, EvaluatedFlag> | null>('drinkit:feature-flags', () => null);
}

/**
 * Reads every declared key in one request for the subject of the session, the user or the visitor. A backend that
 * cannot evaluate them leaves every key to its default, never to the value of the previous subject.
 */
export function useFeatureFlagsLoader(): () => Promise<void> {
  const flags = useFeatureFlags();
  const { baseUrl, onRequest, onResponse } = useApi();
  const { flags: flagKeys, experiments } = useRuntimeConfig().public.featureManagement;
  let latest = 0;

  return async () => {
    const call = ++latest;
    const evaluation = await $fetch<BulkEvaluation>(OFREP_EVALUATION_PATH, {
      method: 'POST',
      body: ofrepRequestOf(flagKeys, experiments),
      baseURL: baseUrl,
      onRequest,
      onResponse,
    }).catch(() => ({ flags: [] }));
    // A sign-in then a sign-out in a row: the answer of the first may come back last
    if (call !== latest) {
      return;
    }

    flags.value = Object.fromEntries(evaluation.flags.map((flag) => [flag.key, flag]));
  };
}

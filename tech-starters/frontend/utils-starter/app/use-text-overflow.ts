import { readonly, shallowRef } from 'vue';
import type { Ref } from 'vue';
import { unrefElement, useMutationObserver, useResizeObserver } from '@vueuse/core';
import type { MaybeComputedElementRef } from '@vueuse/core';

/**
 * Whether the text of an element no longer fits it, cut by an ellipsis or a line clamp, measured again when the
 * element resizes or its text changes: a label shows its full text in a tooltip only then.
 */
export function useTextOverflow(target: MaybeComputedElementRef): Readonly<Ref<boolean>> {
  const overflows = shallowRef(false);

  function measure(): void {
    const element = unrefElement(target);
    overflows.value = element !== null && element !== undefined
      && (element.scrollWidth > element.clientWidth || element.scrollHeight > element.clientHeight);
  }

  useResizeObserver(target, measure);
  useMutationObserver(target, measure, { characterData: true, childList: true, subtree: true });

  return readonly(overflows);
}

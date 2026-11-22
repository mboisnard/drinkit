import { defineComponent, h, nextTick, shallowRef } from 'vue';
import { mountSuspended } from '@nuxt/test-utils/runtime';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { useTextOverflow } from '@drinkit/utils-starter/use-text-overflow';

// happy-dom lays nothing out: the test gives the label its widths and plays the browser's resize observer
let resize: () => void;

class FakeResizeObserver {
  constructor(callback: ResizeObserverCallback) {
    resize = () => {
      callback([], this);
    };
  }

  observe = (): undefined => undefined;

  unobserve = (): undefined => undefined;

  disconnect = (): undefined => undefined;
}

beforeEach(() => {
  vi.stubGlobal('ResizeObserver', FakeResizeObserver);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

const label = defineComponent({
  props: { text: { type: String, required: true } },
  setup(props) {
    const element = shallowRef<HTMLElement>();
    const overflows = useTextOverflow(element);

    return () => h('p', { ref: element, title: overflows.value ? props.text : undefined }, props.text);
  },
});

interface Label {
  element: HTMLElement;
  setText: (text: string) => Promise<void>;
}

async function mountLabel(widths: { text: number; element: number }): Promise<Label> {
  const wrapper = await mountSuspended(label, { props: { text: 'Château Margaux' } });
  const element = wrapper.element as HTMLElement;
  Object.defineProperty(element, 'scrollWidth', { get: () => widths.text });
  Object.defineProperty(element, 'clientWidth', { get: () => widths.element });

  return { element, setText: async (text) => wrapper.setProps({ text }) };
}

test('a text wider than its element overflows', async () => {
  // Given
  const { element } = await mountLabel({ text: 300, element: 200 });

  // When
  resize();
  await nextTick();

  // Then
  expect(element.title).toBe('Château Margaux');
});

test('a text that fits its element once resized no longer overflows', async () => {
  // Given
  const widths = { text: 300, element: 200 };
  const { element } = await mountLabel(widths);
  resize();
  await nextTick();

  // When
  widths.element = 400;
  resize();
  await nextTick();

  // Then
  expect(element.title).toBe('');
});

test('a text that grows past its element overflows without any resize', async () => {
  // Given
  const widths = { text: 150, element: 200 };
  const { element, setText } = await mountLabel(widths);
  resize();
  await nextTick();

  // When
  widths.text = 300;
  await setText('Château Margaux, Premier Grand Cru Classé');
  await vi.waitFor(() => {
    // Then
    expect(element.title).toBe('Château Margaux, Premier Grand Cru Classé');
  });
});

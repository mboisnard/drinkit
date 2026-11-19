import { cleanup } from '@testing-library/vue';
import { afterEach } from 'vitest';

// Testing Library unmounts what a test rendered on its own only when Vitest's globals are on, and DrinkIt imports them
afterEach(() => {
  cleanup();
});

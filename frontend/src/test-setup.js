// TICKET-ADV125 — Jest-DOM matchers for React Testing Library
import '@testing-library/jest-dom/vitest';

// jsdom doesn't implement matchMedia; ThemeContext's initialTheme() calls it
// on every mount, so any test wrapping a component in <ThemeProvider> crashes
// without this.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
});

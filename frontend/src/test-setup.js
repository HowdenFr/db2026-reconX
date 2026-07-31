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

// jsdom doesn't implement EventSource either; useTradeStream() opens one
// unconditionally in a useEffect, so any test rendering a component that
// calls it crashes unless a test explicitly stubs a fuller mock itself
// (e.g. via vi.stubGlobal), which still overrides this default fine.
if (typeof window.EventSource === 'undefined') {
  window.EventSource = class EventSource {
    constructor(url) {
      this.url = url;
      this.readyState = 0;
      this.onopen = null;
      this.onmessage = null;
      this.onerror = null;
    }
    addEventListener() {}
    removeEventListener() {}
    close() {}
  };
}

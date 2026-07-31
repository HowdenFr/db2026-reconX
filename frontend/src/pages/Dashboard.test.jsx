import { act, render, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';

import { AuthContext, AuthProvider } from '@context/AuthContext.jsx';
import { ThemeProvider } from '@context/ThemeContext.jsx';
import Dashboard from '@pages/Dashboard.jsx';

class MockEventSource {
  static instances = [];

  constructor(url) {
    this.url = url;
    this.onopen = null;
    this.onmessage = null;
    this.onerror = null;
    this.close = vi.fn();
    this.listeners = new Map();
    MockEventSource.instances.push(this);
  }

  addEventListener(type, handler) {
    this.listeners.set(type, handler);
  }

  removeEventListener(type, handler) {
    if (this.listeners.get(type) === handler) {
      this.listeners.delete(type);
    }
  }

  triggerOpen() {
    this.onopen?.();
  }

  triggerMessage(payload) {
    this.onmessage?.({ data: JSON.stringify(payload) });
  }
}

const trades = [
  { id: 1, tradeRef: 'TRD-2026-0001', instrument: 'SAP.DE', quantity: 100, price: 250, status: 'MATCHED' },
  { id: 2, tradeRef: 'TRD-2026-0002', instrument: 'SAP.DE', quantity: 50, price: 251, status: 'UNMATCHED' },
];

function renderWithProviders(ui) {
  const user = { email: 'trader@db.com', role: 'TRADER' };

  return render(
    <AuthContext.Provider value={{ user, isLoading: false, login: () => {}, logout: () => {} }}>
      <ThemeProvider>
        <MemoryRouter>{ui}</MemoryRouter>
      </ThemeProvider>
    </AuthContext.Provider>,
  );
}

function renderDashboard() {
  return render(
    <MemoryRouter>
      <ThemeProvider>
        <AuthProvider>
          <Dashboard />
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>,
  );
}

describe('<Dashboard />', () => {
  beforeEach(() => {
    MockEventSource.instances = [];
    vi.stubGlobal('EventSource', MockEventSource);
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it('shows summary cards', () => {
    renderWithProviders(<Dashboard trades={trades} />);

    expect(screen.getByRole('heading', { name: /^portfolio value$/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /^matched trades$/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /^unmatched trades$/i })).toBeInTheDocument();
    expect(screen.getByText(/37,550/)).toBeInTheDocument();
  });

  it('mounts one EventSource, shows newest trades first, and closes on unmount', () => {
    sessionStorage.setItem('reconx-token', 'token');
    sessionStorage.setItem('reconx-role', 'OPS');

    const { unmount } = renderDashboard();

    expect(MockEventSource.instances).toHaveLength(1);
    expect(MockEventSource.instances[0].url).toBe('/api/v1/trades/stream');

    act(() => {
      MockEventSource.instances[0].triggerOpen();
      MockEventSource.instances[0].triggerMessage({
        id: 1,
        tradeRef: 'TR-1',
        instrumentSymbol: 'EUR/USD',
        qty: 1000000,
        price: 1.0852,
        status: 'PENDING',
      });
      MockEventSource.instances[0].triggerMessage({
        id: 2,
        tradeRef: 'TR-2',
        instrumentSymbol: 'AAPL',
        qty: 500,
        price: 178.2,
        status: 'MATCHED',
      });
    });

    expect(screen.getByRole('status')).toHaveTextContent('SSE: connected');
    expect(screen.getByText('Trades streamed')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();

    const feed = screen.getByRole('list', { name: 'Live trade feed' });
    const items = within(feed).getAllByRole('listitem');
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('TR-2');
    expect(items[1]).toHaveTextContent('TR-1');

    unmount();

    expect(MockEventSource.instances[0].close).toHaveBeenCalledTimes(1);
  });
});

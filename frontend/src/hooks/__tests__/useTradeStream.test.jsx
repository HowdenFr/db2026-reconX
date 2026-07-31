import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useTradeStream } from '@hooks/useTradeStream.js';

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

  triggerMalformedMessage(data = '{bad-json') {
    this.onmessage?.({ data });
  }

  triggerError() {
    this.onerror?.(new Event('error'));
  }

  triggerNamedEvent(type, payload) {
    this.listeners.get(type)?.({ data: JSON.stringify(payload) });
  }
}

describe('useTradeStream', () => {
  beforeEach(() => {
    MockEventSource.instances = [];
    vi.stubGlobal('EventSource', MockEventSource);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('opens one EventSource on mount and closes it on unmount', () => {
    const { result, unmount } = renderHook(() => useTradeStream());

    expect(MockEventSource.instances).toHaveLength(1);
    expect(MockEventSource.instances[0].url).toBe('/api/v1/trades/stream');
    expect(result.current.isConnected).toBe(false);

    act(() => {
      MockEventSource.instances[0].triggerOpen();
    });

    expect(result.current.isConnected).toBe(true);

    unmount();

    expect(MockEventSource.instances[0].close).toHaveBeenCalledTimes(1);
  });

  it('prepends new trades, caps the buffer, and ignores malformed payloads', () => {
    const { result } = renderHook(() => useTradeStream('/stream/test'));
    const source = MockEventSource.instances[0];

    act(() => {
      for (let index = 0; index < 205; index += 1) {
        source.triggerMessage({
          id: index,
          tradeRef: `TR-${index}`,
          symbol: `SYM${index}`,
          quantity: index,
          price: index + 0.5,
          status: 'PENDING',
        });
      }
      source.triggerMalformedMessage();
    });

    expect(result.current.trades).toHaveLength(200);
    expect(result.current.trades[0].tradeRef).toBe('TR-204');
    expect(result.current.trades.at(-1).tradeRef).toBe('TR-5');
  });

  it('updates trade status from named trade-matched events and reflects disconnects', () => {
    const { result } = renderHook(() => useTradeStream());
    const source = MockEventSource.instances[0];

    act(() => {
      source.triggerMessage({
        id: 7,
        tradeRef: 'TR-7',
        symbol: 'AAPL',
        quantity: 100,
        price: 178.2,
        status: 'PENDING',
      });
      source.triggerNamedEvent('trade-matched', {
        id: 7,
        tradeRef: 'TR-7',
        status: 'MATCHED',
      });
      source.triggerError();
    });

    expect(result.current.trades[0].status).toBe('MATCHED');
    expect(result.current.isConnected).toBe(false);
  });
});

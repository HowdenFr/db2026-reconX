import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useWebSocket } from '@hooks/useWebSocket.js';

class MockWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;
  static instances = [];

  constructor(url) {
    this.url = url;
    this.readyState = MockWebSocket.CONNECTING;
    this.sent = [];
    this.onopen = null;
    this.onmessage = null;
    this.onerror = null;
    this.onclose = null;
    this.close = vi.fn(() => {
      this.readyState = MockWebSocket.CLOSED;
      this.onclose?.();
    });
    this.send = vi.fn((payload) => {
      this.sent.push(payload);
    });
    MockWebSocket.instances.push(this);
  }

  triggerOpen() {
    this.readyState = MockWebSocket.OPEN;
    this.onopen?.();
  }

  triggerMessage(data) {
    this.onmessage?.({ data });
  }

  triggerError() {
    this.onerror?.(new Event('error'));
  }

  triggerClose() {
    this.readyState = MockWebSocket.CLOSED;
    this.onclose?.(new CloseEvent('close'));
  }
}

describe('useWebSocket', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    MockWebSocket.instances = [];
    vi.stubGlobal('WebSocket', MockWebSocket);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('opens exactly one socket, parses messages, and closes on unmount', async () => {
    const { result, unmount } = renderHook(() => useWebSocket('ws://localhost:8080/feed'));

    expect(MockWebSocket.instances).toHaveLength(1);
    expect(result.current.status).toBe('connecting');

    act(() => {
      MockWebSocket.instances[0].triggerOpen();
      MockWebSocket.instances[0].triggerMessage('{"price":245.5}');
    });

    expect(result.current.status).toBe('open');
    expect(result.current.data).toEqual({ price: 245.5 });

    unmount();

    expect(MockWebSocket.instances[0].close).toHaveBeenCalledTimes(1);
  });

  it('send is a no-op unless the socket is OPEN and stringifies object payloads', () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:8080/feed'));
    const socket = MockWebSocket.instances[0];

    act(() => {
      result.current.send({ type: 'PING' });
    });

    expect(socket.send).not.toHaveBeenCalled();

    act(() => {
      socket.triggerOpen();
      result.current.send({ type: 'PING' });
      result.current.send('raw');
    });

    expect(socket.send).toHaveBeenCalledTimes(2);
    expect(socket.send).toHaveBeenNthCalledWith(1, '{"type":"PING"}');
    expect(socket.send).toHaveBeenNthCalledWith(2, 'raw');
  });

  it('reconnects with exponential backoff on unexpected close until maxRetries', () => {
    renderHook(() =>
      useWebSocket('ws://localhost:8080/feed', {
        reconnect: true,
        maxRetries: 3,
        baseDelay: 500,
        maxDelay: 4000,
      }),
    );

    expect(MockWebSocket.instances).toHaveLength(1);

    act(() => {
      MockWebSocket.instances[0].triggerOpen();
      MockWebSocket.instances[0].triggerClose();
    });

    act(() => {
      vi.advanceTimersByTime(499);
    });
    expect(MockWebSocket.instances).toHaveLength(1);

    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(MockWebSocket.instances).toHaveLength(2);

    act(() => {
      MockWebSocket.instances[1].triggerClose();
      vi.advanceTimersByTime(999);
    });
    expect(MockWebSocket.instances).toHaveLength(2);

    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(MockWebSocket.instances).toHaveLength(3);

    act(() => {
      MockWebSocket.instances[2].triggerClose();
      vi.advanceTimersByTime(1999);
    });
    expect(MockWebSocket.instances).toHaveLength(3);

    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(MockWebSocket.instances).toHaveLength(4);

    act(() => {
      MockWebSocket.instances[3].triggerClose();
      vi.advanceTimersByTime(4000);
    });
    expect(MockWebSocket.instances).toHaveLength(4);
  });
});

// TICKET-ADV115 — useWebSocket(url) with auto-reconnect (exp backoff up to 5 tries).
import { useCallback, useEffect, useRef, useState } from 'react';

const DEFAULTS = {
  reconnect: true,
  maxRetries: 5,
  baseDelay: 500,
  maxDelay: 30000,
};

export function useWebSocket(url, options = {}) {
  const { reconnect, maxRetries, baseDelay, maxDelay } = { ...DEFAULTS, ...options };
  const [data, setData] = useState(null);
  const [status, setStatus] = useState(url ? 'connecting' : 'closed');
  const wsRef = useRef(null);
  const retriesRef = useRef(0);
  const timerRef = useRef(null);
  const shouldStopRef = useRef(false);

  const connect = useCallback(() => {
    if (!url) {
      setStatus('closed');
      return;
    }

    const ws = new WebSocket(url);
    wsRef.current = ws;
    setStatus('connecting');

    ws.onopen = () => {
      if (shouldStopRef.current) {
        return;
      }
      retriesRef.current = 0;
      setStatus('open');
    };

    ws.onmessage = (event) => {
      if (shouldStopRef.current) {
        return;
      }
      try {
        setData(JSON.parse(event.data));
      } catch {
        setData(event.data);
      }
    };

    ws.onerror = () => {
      if (!shouldStopRef.current) {
        setStatus('error');
      }
    };

    ws.onclose = () => {
      if (wsRef.current === ws) {
        wsRef.current = null;
      }
      if (shouldStopRef.current) {
        setStatus('closed');
        return;
      }

      setStatus('closed');
      if (!reconnect || retriesRef.current >= maxRetries) {
        return;
      }

      const delay = Math.min(maxDelay, baseDelay * 2 ** retriesRef.current);
      retriesRef.current += 1;
      timerRef.current = setTimeout(() => {
        timerRef.current = null;
        if (!shouldStopRef.current) {
          connect();
        }
      }, delay);
    };
  }, [baseDelay, maxDelay, maxRetries, reconnect, url]);

  useEffect(() => {
    shouldStopRef.current = false;
    retriesRef.current = 0;
    connect();

    return () => {
      shouldStopRef.current = true;
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        timerRef.current = null;
      }
      if (wsRef.current && wsRef.current.readyState <= WebSocket.OPEN) {
        wsRef.current.close();
      }
      wsRef.current = null;
    };
  }, [connect]);

  const send = useCallback((payload) => {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) {
      return;
    }

    wsRef.current.send(typeof payload === 'string' ? payload : JSON.stringify(payload));
  }, []);

  return { data, status, send };
}

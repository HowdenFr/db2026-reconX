// TICKET-ADV116 - useTradeStream() - SSE subscription returning live trades.
import { useEffect, useState } from 'react';

const MAX_BUFFER = 200;

function parseTradeEvent(data) {
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
}

export function useTradeStream(url = '/api/v1/trades/stream') {
  const [trades, setTrades] = useState([]);
  const [isConnected, setConnected] = useState(false);

  useEffect(() => {
    const sse = new EventSource(url);

    const handleTradeMatched = (event) => {
      const payload = parseTradeEvent(event.data);
      if (!payload) {
        return;
      }

      setTrades((prev) => prev.map((trade) => {
        const sameId = payload.id != null && trade.id === payload.id;
        const sameRef = payload.tradeRef && trade.tradeRef === payload.tradeRef;

        if (!sameId && !sameRef) {
          return trade;
        }

        return {
          ...trade,
          ...payload,
          status: payload.status ?? 'MATCHED',
        };
      }));
    };

    sse.onopen = () => setConnected(true);
    sse.onmessage = (event) => {
      const nextTrade = parseTradeEvent(event.data);
      if (!nextTrade) {
        return;
      }

      setTrades((prev) => [nextTrade, ...prev].slice(0, MAX_BUFFER));
    };
    sse.onerror = () => setConnected(false);
    sse.addEventListener('trade-matched', handleTradeMatched);

    return () => {
      sse.removeEventListener('trade-matched', handleTradeMatched);
      sse.close();
    };
  }, [url]);

  return { trades, isConnected };
}

// TICKET-ADV120 - useMemo for portfolio-value calc.
// TICKET-ADV116 - useTradeStream live feed.
import React, { useMemo } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import { withErrorBoundary } from '@components/withErrorBoundary.jsx';
import { useTradeStream } from '@hooks/useTradeStream.js';

function StatCard({ label, value }) {
  return (
    <article className="stat-card">
      <h3>{label}</h3>
      <p>{value}</p>
    </article>
  );
}

function Dashboard({ trades: seededTrades }) {
  const { trades: streamedTrades, isConnected } = useTradeStream();
  const trades = seededTrades ?? streamedTrades;

  const portfolioValue = useMemo(
    () => trades.reduce(
      (sum, trade) => {
        const quantity = Number(trade.quantity ?? trade.qty ?? 0);
        const price = Number(trade.price ?? 0);
        return sum + quantity * price;
      },
      0,
    ),
    [trades],
  );

  const statusSummary = useMemo(() => {
    const matched = trades.filter((trade) => trade.status === 'MATCHED');
    const unmatched = trades.filter((trade) => trade.status === 'UNMATCHED');
    const disputed = trades.filter((trade) => trade.status === 'DISPUTED');

    return {
      matchedCount: matched.length,
      unmatchedCount: unmatched.length,
      openBreaksCount: unmatched.length + disputed.length,
    };
  }, [trades]);

  return (
    <section>
      <h2>Dashboard</h2>
      <div className="stat-grid">
        <StatCard label="Portfolio value" value={portfolioValue.toLocaleString('en-US')} />
        <StatCard label="Trades streamed" value={trades.length} />
        <StatCard label="Matched trades" value={statusSummary.matchedCount} />
        <StatCard label="Unmatched trades" value={statusSummary.unmatchedCount} />
      </div>
      <div role="status" aria-live="polite">
        SSE: {isConnected ? 'connected' : 'disconnected'}
      </div>
      <section aria-labelledby="live-trades-heading">
        <h3 id="live-trades-heading">Live trades</h3>
        <ol aria-label="Live trade feed">
          {trades.map((trade, index) => {
            const symbol = trade.instrumentSymbol || trade.instrument || trade.symbol || '-';
            const quantity = trade.qty ?? trade.quantity ?? '-';
            const key = trade.id ?? trade.tradeRef ?? `${symbol}-${index}`;

            return (
              <li key={key}>
                <strong>{trade.tradeRef || 'Unknown ref'}</strong>{' '}
                <span>{symbol}</span>{' '}
                <span>qty={quantity}</span>{' '}
                <span>price={trade.price ?? '-'}</span>{' '}
                <span>[{trade.status || 'PENDING'}]</span>
              </li>
            );
          })}
        </ol>
      </section>
    </section>
  );
}

export default withErrorBoundary(withAuth(Dashboard));

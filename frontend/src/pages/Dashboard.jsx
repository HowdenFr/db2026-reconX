import React from 'react';
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
  const portfolioValue = trades.reduce((sum, trade) => {
    const quantity = trade.quantity ?? trade.qty ?? 0;
    return sum + quantity * (trade.price ?? 0);
  }, 0);
  const matchedTrades = trades.filter((trade) => trade.status === 'MATCHED').length;
  const unmatchedTrades = trades.filter((trade) => trade.status === 'UNMATCHED').length;

  return (
    <section>
      <h2>Dashboard</h2>
      <div className="stat-grid">
        <StatCard label="Portfolio value" value={portfolioValue.toLocaleString('en-US')} />
        <StatCard label="Trades streamed" value={trades.length} />
        <StatCard label="Matched trades" value={matchedTrades} />
        <StatCard label="Unmatched trades" value={unmatchedTrades} />
      </div>
      <div role="status" aria-live="polite">
        SSE: {isConnected ? 'connected' : 'disconnected'}
      </div>
    </section>
  );
}

export default withErrorBoundary(withAuth(Dashboard));

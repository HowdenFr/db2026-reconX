// TICKET-ADV120 - useMemo for portfolio-value calc.
// TICKET-ADV116 - useTradeStream live feed.
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

function Dashboard() {
  const { trades, isConnected } = useTradeStream();

  // TODO(TICKET-ADV120): use useMemo to compute `portfolioValue` =
  //                     sum(trades[i].quantity * trades[i].price).
  //                     Memoise on `trades` so it doesn't recompute every render.

  // TODO(TICKET-ADV120): derive `matched` (status === 'MATCHED') and
  //                     `breaks` (status in ['UNMATCHED','DISPUTED']) counts.

  return (
    <section>
      <h2>Dashboard</h2>
      <div className="stat-grid">
        {/* TODO(TICKET-ADV120): render four <StatCard>s - Portfolio value,
            Trades streamed, Matched, Open breaks. */}
        <StatCard label="Trades streamed" value={trades.length} />
      </div>
      <div role="status" aria-live="polite">
        SSE: {isConnected ? 'connected' : 'disconnected'}
      </div>
      <section aria-labelledby="live-trades-heading">
        <h3 id="live-trades-heading">Live trades</h3>
        <ol aria-label="Live trade feed">
          {trades.map((trade, index) => {
            const symbol = trade.instrumentSymbol || trade.symbol || '-';
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

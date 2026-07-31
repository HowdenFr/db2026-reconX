// TICKET-ADV119 — React.memo on <TradeRow /> with a field-scoped equality check.
import React from 'react';

// display: contents drops this wrapper from the box tree so its children
// land directly in <DataTable.Body>'s CSS grid row, matching the plain
// <span> markup DataTable.Body's `render` prop expects for each column.
function TradeRowImpl({ trade, onClick }) {
  return (
    <span style={{ display: 'contents' }} onClick={() => onClick(trade.id)}>
      <span>{trade.tradeRef}</span>
      <span>{trade.instrumentSymbol}</span>
      <span>{trade.quantity}</span>
      <span>{trade.price}</span>
      <span className={`status-pill ${trade.status.toLowerCase()}`}>{trade.status}</span>
    </span>
  );
}

// Only the fields this row actually renders that can change post-creation.
function areEqual(prev, next) {
  return prev.trade.id     === next.trade.id
      && prev.trade.status === next.trade.status
      && prev.trade.price  === next.trade.price
      && prev.onClick      === next.onClick;
}

export const TradeRow = React.memo(TradeRowImpl, areEqual);

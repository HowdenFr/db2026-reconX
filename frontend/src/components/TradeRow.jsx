// TICKET-ADV119 — React.memo on <TradeRow /> with a field-scoped equality check.
import React from 'react';

function TradeRowImpl({ trade, onClick }) {
  return (
    <tr onClick={() => onClick(trade.id)}>
      <td>{trade.tradeRef}</td>
      <td>{trade.instrumentSymbol}</td>
      <td>{trade.quantity}</td>
      <td>{trade.price}</td>
      <td><span className={`status-pill ${trade.status.toLowerCase()}`}>{trade.status}</span></td>
    </tr>
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

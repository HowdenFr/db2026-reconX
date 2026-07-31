// TICKET-ADV114 — Compound DataTable.
// TICKET-ADV117 — useDebouncedSearch.
// TICKET-ADV121 — useCallback on the handler passed to memoised <TradeRow />.
import React, { useCallback, useEffect, useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import { TradeRow } from '@components/TradeRow.jsx';
import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';
import { api } from '@services/apiService.js';

function Trades() {
  const [search, setSearch] = useState('');
  const debounced = useDebouncedSearch(search, 300);
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ items: [], totalPages: 0 });
  const [selectedId, setSelectedId] = useState(null);

  // Reference-stable across renders so ADV119's TradeRow memo actually holds.
  const handleSelect = useCallback((id) => setSelectedId(id), []);

  useEffect(() => {
    let cancelled = false;
    const params = new URLSearchParams({ page: String(page) });
    if (debounced) params.set('status', debounced);

    api.listTrades(`?${params.toString()}`)
      .then((res) => {
        if (cancelled) return;
        setData({ items: res.content, totalPages: res.totalPages });
      })
      .catch(() => {
        if (cancelled) return;
        setData({ items: [], totalPages: 0 });
      });

    return () => { cancelled = true; };
  }, [page, debounced]);

  return (
    <section>
      <h2>Trades</h2>
      <input
        aria-label="Filter by status"
        placeholder="status filter (PENDING/MATCHED/…)"
        value={search}
        onChange={(e) => setSearch(e.target.value.toUpperCase())}
      />
      <DataTable>
        <DataTable.Header columns={[
          { key: 'tradeRef', label: 'Ref' },
          { key: 'symbol',   label: 'Symbol' },
          { key: 'qty',      label: 'Qty' },
          { key: 'price',    label: 'Price' },
          { key: 'status',   label: 'Status' },
        ]} />
        <DataTable.Body
          rows={data.items}
          render={(row) => <TradeRow trade={row} onClick={handleSelect} />}
        />
        <DataTable.Pagination
          page={page}
          totalPages={Math.max(1, data.totalPages)}
          onChange={setPage}
        />
      </DataTable>
    </section>
  );
}

export default withAuth(Trades);

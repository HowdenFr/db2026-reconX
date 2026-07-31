// TICKET-ADV117 — useDebouncedSearch(query, delay).
import { useEffect, useState } from 'react';

export function useDebouncedSearch(query, delay = 300) {
  const [debounced, setDebounced] = useState(query);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      setDebounced(query);
    }, delay);

    return () => {
      clearTimeout(timeoutId);
    };
  }, [query, delay]);

  return debounced;
}
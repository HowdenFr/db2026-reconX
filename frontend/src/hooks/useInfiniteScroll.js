// TICKET-ADV118 — useInfiniteScroll: invokes loadMore() when sentinel is visible.
import { useEffect, useRef } from 'react';

export function useInfiniteScroll(loadMore, { rootMargin = '200px' } = {}) {
  const sentinelRef = useRef(null);
  const loadMoreRef = useRef(loadMore);

  // Keep the latest loadMore available to the observer without recreating it —
  // dodges stale closures over old state on every render.
  useEffect(() => {
    loadMoreRef.current = loadMore;
  }, [loadMore]);

  // Observer is created once (only churns if rootMargin itself changes).
  useEffect(() => {
    const node = sentinelRef.current;
    if (!node) return undefined;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) loadMoreRef.current();
      },
      { rootMargin }
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [rootMargin]);

  return sentinelRef;
}

import { useEffect, useRef } from 'react';

/**
 * Keeps the currently visible screen synchronized with backend changes.
 * Polling covers other browsers/devices, while focus and data-update events
 * make the current browser refresh as soon as it becomes active.
 */
export default function useLiveRefresh(callback, {
  enabled = true,
  interval = 8000,
  refreshKey = '',
} = {}) {
  const callbackRef = useRef(callback);

  useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);

  useEffect(() => {
    if (!enabled) return undefined;

    let running = false;
    const refresh = async () => {
      if (running || document.visibilityState !== 'visible') return;
      running = true;
      try {
        await callbackRef.current?.();
      } finally {
        running = false;
      }
    };
    const refreshWhenVisible = () => {
      if (document.visibilityState === 'visible') refresh();
    };

    const timer = window.setInterval(refresh, interval);
    window.addEventListener('focus', refresh);
    window.addEventListener('ssc:data-updated', refresh);
    document.addEventListener('visibilitychange', refreshWhenVisible);

    return () => {
      window.clearInterval(timer);
      window.removeEventListener('focus', refresh);
      window.removeEventListener('ssc:data-updated', refresh);
      document.removeEventListener('visibilitychange', refreshWhenVisible);
    };
  }, [enabled, interval, refreshKey]);
}

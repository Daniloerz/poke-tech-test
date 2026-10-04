import { useEffect, useState } from 'react';

/**
 * Runs one API request and keeps its state. The request runs again when a value in `deps` changes
 * or when `reload()` is called.
 */
export function useApi(request, deps) {
  const [state, setState] = useState({ data: null, error: null, loading: true });
  const [reloadCount, setReloadCount] = useState(0);

  useEffect(() => {
    let active = true;
    setState((previous) => ({ ...previous, error: null, loading: true }));

    request()
      .then((data) => active && setState({ data, error: null, loading: false }))
      .catch((error) => active && setState({ data: null, error, loading: false }));

    // Ignores the answer of a request that is no longer needed (the page changed meanwhile).
    return () => {
      active = false;
    };
  }, [...deps, reloadCount]);

  return { ...state, reload: () => setReloadCount((count) => count + 1) };
}

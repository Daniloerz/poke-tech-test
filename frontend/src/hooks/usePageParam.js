import { useSearchParams } from 'react-router';

/**
 * The current page lives in the URL (?page=1, one-based for people), so refresh and back keep it.
 * Returns the zero-based page used by the API and a function to change it.
 */
export function usePageParam() {
  const [searchParams, setSearchParams] = useSearchParams();
  const pageNumber = Number.parseInt(searchParams.get('page') ?? '1', 10);
  const page = Number.isNaN(pageNumber) || pageNumber < 1 ? 0 : pageNumber - 1;

  const setPage = (newPage) => {
    setSearchParams({ page: String(newPage + 1) });
    window.scrollTo({ top: 0 });
  };

  return [page, setPage];
}

import { fetchPokemonPage } from '../api/pokemonApi.js';
import Pagination from '../components/Pagination.jsx';
import PokemonCard from '../components/PokemonCard.jsx';
import StatusMessage from '../components/StatusMessage.jsx';
import { useApi } from '../hooks/useApi.js';
import { usePageParam } from '../hooks/usePageParam.js';

const PAGE_SIZE = 20;

export default function CatalogPage() {
  const [page, setPage] = usePageParam();
  const { data, error, loading } = useApi(() => fetchPokemonPage(page, PAGE_SIZE), [page]);

  return (
    <>
      <h1>Pokemon catalog</h1>
      {loading && <StatusMessage>Loading Pokemon...</StatusMessage>}
      {error && <StatusMessage type="error">{error.message}</StatusMessage>}
      {data && data.content.length === 0 && <StatusMessage>There are no Pokemon on this page.</StatusMessage>}
      {data && !loading && (
        <>
          <ul className="card-grid">
            {data.content.map((pokemon) => (
              <PokemonCard key={pokemon.id} pokemon={pokemon} />
            ))}
          </ul>
          <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </>
  );
}

import { useState } from 'react';
import { Link } from 'react-router';
import { deleteLocalPokemon, fetchLocalPokemonPage } from '../api/localPokemonApi.js';
import Pagination from '../components/Pagination.jsx';
import StatusMessage from '../components/StatusMessage.jsx';
import TypeList from '../components/TypeList.jsx';
import { useApi } from '../hooks/useApi.js';
import { usePageParam } from '../hooks/usePageParam.js';
import styles from './LocalPokemonListPage.module.css';

const PAGE_SIZE = 10;

export default function LocalPokemonListPage() {
  const [page, setPage] = usePageParam();
  const { data, error, loading, reload } = useApi(() => fetchLocalPokemonPage(page, PAGE_SIZE), [page]);
  const [deleteError, setDeleteError] = useState(null);

  const handleDelete = async (pokemon) => {
    if (!window.confirm(`Delete ${pokemon.name} from My Pokemon?`)) {
      return;
    }
    setDeleteError(null);
    try {
      await deleteLocalPokemon(pokemon.id);
      reload();
    } catch (error) {
      setDeleteError(error);
    }
  };

  return (
    <>
      <h1>My Pokemon</h1>
      <p>Your local copies of PokeAPI Pokemon, with your own name, region and tags.</p>
      {deleteError && <StatusMessage type="error">{deleteError.message}</StatusMessage>}
      {loading && <StatusMessage>Loading your Pokemon...</StatusMessage>}
      {error && <StatusMessage type="error">{error.message}</StatusMessage>}
      {data && !loading && data.totalElements === 0 && (
        <StatusMessage>
          You have no Pokemon yet. Open one in the <Link to="/pokemon">catalog</Link> and click &quot;Save to My
          Pokemon&quot;.
        </StatusMessage>
      )}
      {data && !loading && data.totalElements > 0 && data.content.length === 0 && (
        <StatusMessage>There are no Pokemon on this page.</StatusMessage>
      )}
      {data && !loading && data.content.length > 0 && (
        <>
          <ul className={styles.list}>
            {data.content.map((pokemon) => (
              <li key={pokemon.id} className={styles.item}>
                {pokemon.spriteUrl && <img src={pokemon.spriteUrl} alt={pokemon.name} width="72" height="72" />}
                <div className={styles.info}>
                  <h2 className={styles.name}>
                    <Link to={`/pokemon/${pokemon.name}`}>{pokemon.name}</Link>
                    {pokemon.localizedName && <span className={styles.localized}> · {pokemon.localizedName}</span>}
                  </h2>
                  <TypeList types={pokemon.types} />
                  <p className={styles.meta}>Region: {pokemon.region ?? '—'}</p>
                  {pokemon.tags.length > 0 && (
                    <ul className="tag-list" aria-label="Tags">
                      {pokemon.tags.map((tag) => (
                        <li key={tag} className="tag">
                          {tag}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
                <div className={styles.actions}>
                  <Link to={`/my-pokemon/${pokemon.id}/edit`} className="button secondary">
                    Edit
                  </Link>
                  <button type="button" className="button secondary" onClick={() => handleDelete(pokemon)}>
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
          <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </>
  );
}

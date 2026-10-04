import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { fetchPokemonDetail } from '../api/pokemonApi.js';
import { syncPokemon } from '../api/localPokemonApi.js';
import { useAuth } from '../auth/AuthContext.jsx';
import EvolutionTree from '../components/EvolutionTree.jsx';
import StatusMessage from '../components/StatusMessage.jsx';
import TypeList from '../components/TypeList.jsx';
import { useApi } from '../hooks/useApi.js';
import styles from './PokemonDetailPage.module.css';

// The highest base stat a Pokemon can have; used as 100% of the bar.
const MAX_BASE_STAT = 255;

export default function PokemonDetailPage() {
  const { idOrName } = useParams();
  const { data: pokemon, error, loading } = useApi(() => fetchPokemonDetail(idOrName), [idOrName]);

  if (loading) {
    return <StatusMessage>Loading Pokemon...</StatusMessage>;
  }
  if (error) {
    return (
      <>
        <StatusMessage type="error">{error.message}</StatusMessage>
        <p>
          <Link to="/pokemon">Back to the catalog</Link>
        </p>
      </>
    );
  }

  return (
    <article>
      <p>
        <Link to="/pokemon">← Back to the catalog</Link>
      </p>
      <div className={styles.header}>
        {pokemon.imageUrl && (
          <img src={pokemon.imageUrl} alt={pokemon.name} width="240" height="240" className={styles.image} />
        )}
        <div className={styles.summary}>
          <p className={styles.number}>#{pokemon.id}</p>
          <h1 className={styles.name}>{pokemon.name}</h1>
          <TypeList types={pokemon.types} />
          <p>
            Height: <strong>{pokemon.heightM} m</strong> · Weight: <strong>{pokemon.weightKg} kg</strong>
          </p>
          {pokemon.description && <p>{pokemon.description}</p>}
          <SaveToMyPokemon idOrName={pokemon.name} />
        </div>
      </div>

      <section className="panel">
        <h2>Base stats</h2>
        <dl className={styles.stats}>
          {pokemon.stats.map((stat) => (
            <div key={stat.name} className={styles.stat}>
              <dt>{stat.name}</dt>
              <dd>
                <span className={styles.statValue}>{stat.baseStat}</span>
                <span className={styles.bar}>
                  <span style={{ width: `${(stat.baseStat / MAX_BASE_STAT) * 100}%` }} />
                </span>
              </dd>
            </div>
          ))}
        </dl>
      </section>

      {pokemon.evolutionChain && (
        <section className={`panel ${styles.evolution}`}>
          <h2>Evolution chain</h2>
          <EvolutionTree node={pokemon.evolutionChain} />
        </section>
      )}
    </article>
  );
}

function SaveToMyPokemon({ idOrName }) {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  if (!isAuthenticated) {
    return (
      <p>
        <Link to="/login" state={{ from: `/pokemon/${idOrName}` }}>
          Log in
        </Link>{' '}
        to save this Pokemon and add your own data.
      </p>
    );
  }

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const saved = await syncPokemon(idOrName);
      navigate(`/my-pokemon/${saved.id}/edit`);
    } catch (saveError) {
      setError(saveError);
      setSaving(false);
    }
  };

  const existingId = error?.status === 409 ? error.problem.localId : null;

  return (
    <div className={styles.save}>
      <button type="button" className="button" onClick={handleSave} disabled={saving}>
        {saving ? 'Saving...' : 'Save to My Pokemon'}
      </button>
      {existingId && (
        <StatusMessage>
          This Pokemon is already in My Pokemon. <Link to={`/my-pokemon/${existingId}/edit`}>Edit it</Link>
        </StatusMessage>
      )}
      {error && !existingId && <StatusMessage type="error">{error.message}</StatusMessage>}
    </div>
  );
}

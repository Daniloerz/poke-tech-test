import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { fieldErrors } from '../api/client.js';
import { fetchLocalPokemon, updateLocalPokemon } from '../api/localPokemonApi.js';
import StatusMessage from '../components/StatusMessage.jsx';
import TypeList from '../components/TypeList.jsx';
import { useApi } from '../hooks/useApi.js';

export default function LocalPokemonEditPage() {
  const { id } = useParams();
  const { data: pokemon, error, loading } = useApi(() => fetchLocalPokemon(id), [id]);

  if (loading) {
    return <StatusMessage>Loading...</StatusMessage>;
  }
  if (error) {
    return (
      <>
        <StatusMessage type="error">{error.message}</StatusMessage>
        <p>
          <Link to="/my-pokemon">Back to My Pokemon</Link>
        </p>
      </>
    );
  }
  // The key gives the form fresh initial values when another Pokemon is opened.
  return <EditForm key={pokemon.id} pokemon={pokemon} />;
}

function EditForm({ pokemon }) {
  const navigate = useNavigate();
  const [localizedName, setLocalizedName] = useState(pokemon.localizedName ?? '');
  const [region, setRegion] = useState(pokemon.region ?? '');
  const [tags, setTags] = useState(pokemon.tags.join(', '));
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await updateLocalPokemon(pokemon.id, {
        localizedName: localizedName.trim() || null,
        region: region.trim() || null,
        tags: tags.split(',').map((tag) => tag.trim()).filter(Boolean),
      });
      navigate('/my-pokemon');
    } catch (saveError) {
      setError(saveError);
      setSaving(false);
    }
  };

  const errors = fieldErrors(error);
  const tagsError = Object.entries(errors).find(([field]) => field.startsWith('tags'))?.[1];
  const hasFieldErrors = Object.keys(errors).length > 0;

  return (
    <>
      <p>
        <Link to="/my-pokemon">← Back to My Pokemon</Link>
      </p>
      <h1 className="capitalize">Edit {pokemon.name}</h1>
      <section className="panel">
        <h2>PokeAPI data</h2>
        {pokemon.spriteUrl && <img src={pokemon.spriteUrl} alt={pokemon.name} width="96" height="96" />}
        <TypeList types={pokemon.types} />
        <p>
          #{pokemon.pokeApiId} · {pokemon.heightM} m · {pokemon.weightKg} kg
        </p>
        <p className="hint">This data is a copy from PokeAPI and cannot be edited.</p>
      </section>

      <h2>Your data</h2>
      <form className="form panel" onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label htmlFor="localizedName">Localized name</label>
          <input
            id="localizedName"
            value={localizedName}
            onChange={(event) => setLocalizedName(event.target.value)}
            maxLength={100}
            aria-invalid={Boolean(errors.localizedName)}
          />
          {errors.localizedName && <span className="field-error">{errors.localizedName}</span>}
        </div>
        <div className="field">
          <label htmlFor="region">Region</label>
          <input
            id="region"
            value={region}
            onChange={(event) => setRegion(event.target.value)}
            maxLength={100}
            aria-invalid={Boolean(errors.region)}
          />
          {errors.region && <span className="field-error">{errors.region}</span>}
        </div>
        <div className="field">
          <label htmlFor="tags">Tags</label>
          <input
            id="tags"
            value={tags}
            onChange={(event) => setTags(event.target.value)}
            aria-describedby="tags-hint"
            aria-invalid={Boolean(tagsError)}
          />
          <span id="tags-hint" className="hint">
            Separated by commas. Up to 10 tags, each with letters, digits or hyphens.
          </span>
          {tagsError && <span className="field-error">{tagsError}</span>}
        </div>
        {error && !hasFieldErrors && <StatusMessage type="error">{error.message}</StatusMessage>}
        <button type="submit" className="button" disabled={saving}>
          {saving ? 'Saving...' : 'Save'}
        </button>
      </form>
    </>
  );
}

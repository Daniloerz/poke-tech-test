import { Link } from 'react-router';
import TypeList from './TypeList.jsx';
import styles from './PokemonCard.module.css';

const VISIBLE_MOVES = 4;

export default function PokemonCard({ pokemon }) {
  const { id, name, spriteUrl, types, weightKg, moves } = pokemon;
  const hiddenMoves = moves.length - VISIBLE_MOVES;

  return (
    <li className={styles.card}>
      <Link to={`/pokemon/${name}`} className={styles.link}>
        {spriteUrl ? (
          <img src={spriteUrl} alt={name} width="96" height="96" loading="lazy" />
        ) : (
          <div className={styles.noImage}>No image</div>
        )}
        <p className={styles.number}>#{id}</p>
        <h2 className={styles.name}>{name}</h2>
      </Link>
      <TypeList types={types} />
      <p className={styles.weight}>{weightKg} kg</p>
      <p className={styles.moves}>
        {moves.slice(0, VISIBLE_MOVES).join(', ')}
        {hiddenMoves > 0 && ` +${hiddenMoves} more`}
      </p>
    </li>
  );
}

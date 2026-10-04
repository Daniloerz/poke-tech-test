import { Link } from 'react-router';
import styles from './EvolutionTree.module.css';

/** Draws one stage and, under it, the stages it evolves into. A stage can have several branches. */
export default function EvolutionTree({ node }) {
  return (
    <div className={styles.node}>
      <Link to={`/pokemon/${node.name}`} className={styles.stage}>
        <img src={node.imageUrl} alt={node.name} width="72" height="72" loading="lazy" />
        <span>{node.name}</span>
      </Link>
      {node.evolvesTo.length > 0 && (
        <div className={styles.children}>
          {node.evolvesTo.map((child) => (
            <EvolutionTree key={child.id} node={child} />
          ))}
        </div>
      )}
    </div>
  );
}

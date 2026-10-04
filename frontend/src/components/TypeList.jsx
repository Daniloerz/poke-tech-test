export default function TypeList({ types }) {
  return (
    <ul className="tag-list" aria-label="Types">
      {types.map((type) => (
        <li key={type} className="tag type">
          {type}
        </li>
      ))}
    </ul>
  );
}

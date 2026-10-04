/** Loading, empty or error message. Errors use role="alert" so screen readers announce them. */
export default function StatusMessage({ type = 'info', children }) {
  const role = type === 'error' ? 'alert' : 'status';
  return (
    <p role={role} className={`status-message ${type}`}>
      {children}
    </p>
  );
}

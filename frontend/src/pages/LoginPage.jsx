import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../auth/AuthContext.jsx';
import StatusMessage from '../components/StatusMessage.jsx';

export default function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const nextPage = location.state?.from ?? '/my-pokemon';

  if (isAuthenticated) {
    return <Navigate to={nextPage} replace />;
  }

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login(username, password);
      navigate(nextPage, { replace: true });
    } catch (loginError) {
      setError(loginError);
      setSubmitting(false);
    }
  };

  return (
    <>
      <h1>Log in</h1>
      <form className="form panel" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            required
          />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            required
          />
        </div>
        {error && <StatusMessage type="error">{error.message}</StatusMessage>}
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? 'Logging in...' : 'Log in'}
        </button>
        <p className="hint">
          Demo users: <strong>ash</strong> / pikachu123 and <strong>misty</strong> / starmie123.
        </p>
        <p>
          No account yet? <Link to="/register">Create one</Link>
        </p>
      </form>
    </>
  );
}

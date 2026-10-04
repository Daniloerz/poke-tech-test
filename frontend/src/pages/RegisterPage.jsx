import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router';
import { register } from '../api/authApi.js';
import { fieldErrors } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';
import StatusMessage from '../components/StatusMessage.jsx';

export default function RegisterPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/my-pokemon" replace />;
  }

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await register(username, password);
      await login(username, password);
      navigate('/my-pokemon', { replace: true });
    } catch (registerError) {
      setError(registerError);
      setSubmitting(false);
    }
  };

  const errors = fieldErrors(error);
  const hasFieldErrors = Object.keys(errors).length > 0;

  return (
    <>
      <h1>Create an account</h1>
      <form className="form panel" onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            aria-describedby="username-hint"
            aria-invalid={Boolean(errors.username)}
          />
          <span id="username-hint" className="hint">
            3 to 30 letters, digits, &quot;.&quot;, &quot;_&quot; or &quot;-&quot;.
          </span>
          {errors.username && <span className="field-error">{errors.username}</span>}
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="new-password"
            aria-describedby="password-hint"
            aria-invalid={Boolean(errors.password)}
          />
          <span id="password-hint" className="hint">
            At least 8 characters.
          </span>
          {errors.password && <span className="field-error">{errors.password}</span>}
        </div>
        {error && !hasFieldErrors && <StatusMessage type="error">{error.message}</StatusMessage>}
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? 'Creating account...' : 'Create account'}
        </button>
        <p>
          Already registered? <Link to="/login">Log in</Link>
        </p>
      </form>
    </>
  );
}

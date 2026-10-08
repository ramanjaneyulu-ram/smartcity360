import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { IconHome } from '../components/Icons';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not log in. Check your details and try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-panel">
        <div className="auth-brand">
          <span className="auth-brand-mark"><IconHome aria-hidden="true" /></span>
          <span className="auth-brand-copy">
            <strong>SmartCity 360</strong>
            <small>Citizen grievance and service management</small>
          </span>
        </div>
        <div className="auth-card">
          <h1>Welcome back</h1>
          <p>Log in to SmartCity 360 to report or track civic issues.</p>
          {error && <div className="auth-error">{error}</div>}
          <form onSubmit={onSubmit}>
            <div className="field">
              <label htmlFor="login-email">Email</label>
              <input
                id="login-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="asha@example.com"
                autoComplete="username"
                required
              />
            </div>
            <div className="field">
              <label htmlFor="login-password">Password</label>
              <input
                id="login-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                autoComplete="current-password"
                required
              />
            </div>
            <button className="btn btn-primary auth-btn" disabled={loading}>
              {loading ? 'Logging in…' : 'Log in'}
            </button>
          </form>
          <div className="auth-switch">
            No account? <Link to="/register">Register</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

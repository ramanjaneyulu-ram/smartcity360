import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { IconHome } from '../components/Icons';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function update(k, v) { setForm((f) => ({ ...f, [k]: v })); }

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register(form);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not create your account.');
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
          <h1>Create an account</h1>
          <p>Create a citizen account to report and track city issues.</p>
          {error && <div className="auth-error">{error}</div>}
          <form onSubmit={onSubmit}>
            <div className="field">
              <label htmlFor="register-name">Full name</label>
              <input id="register-name" type="text" value={form.name} onChange={(e) => update('name', e.target.value)} autoComplete="name" required />
            </div>
            <div className="field">
              <label htmlFor="register-email">Email</label>
              <input id="register-email" type="email" value={form.email} onChange={(e) => update('email', e.target.value)} autoComplete="email" required />
            </div>
            <div className="field">
              <label htmlFor="register-password">Password</label>
              <input id="register-password" type="password" value={form.password} onChange={(e) => update('password', e.target.value)} autoComplete="new-password" required />
            </div>
            <button className="btn btn-primary auth-btn" disabled={loading}>
              {loading ? 'Creating account…' : 'Create account'}
            </button>
          </form>
          <div className="auth-switch">
            Already have an account? <Link to="/login">Log in</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

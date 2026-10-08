import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import Chip from '../components/Chip';
import { IconPlus, IconBuilding } from '../components/Icons';

export default function CitizenDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    client.get('/complaints/mine')
      .then((res) => setComplaints(res.data))
      .catch((err) => {
        if (!err.response) setError('Cannot connect to the backend. Check that it is running on port 8080.');
        else if (err.response.status === 401 || err.response.status === 403) {
          setError('Your session cannot access citizen reports. Log in with a citizen account.');
        } else setError(`Could not load reports (HTTP ${err.response.status}).`);
      })
      .finally(() => setLoading(false));
  }, []);

  const active = complaints.filter((c) => !['RESOLVED', 'VERIFIED'].includes(c.status)).length;
  const resolved = complaints.filter((c) => ['RESOLVED', 'VERIFIED'].includes(c.status)).length;
  const firstName = user?.name?.split(' ')[0] || 'there';
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';

  return (
    <>
      <div className="page-head">
        <div>
          <h1>{greeting}, {firstName}</h1>
          <p>Here's what's happening with the problems you've reported.</p>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/report')}>
          <IconPlus /> Report a problem
        </button>
      </div>

      <div className="stat-row">
        <div className="stat"><div className="num">{loading || error ? '—' : active}</div><div className="lbl">Active reports</div></div>
        <div className="stat"><div className="num">{loading || error ? '—' : resolved}</div><div className="lbl">Resolved</div></div>
        <div className="stat"><div className="num">{loading || error ? '—' : complaints.length}</div><div className="lbl">Total reports filed</div></div>
      </div>

      <div className="home-grid">
        <div>
          <div className="cta-card">
            <h2>Seen a pothole, a dark street, or an overflowing bin?</h2>
            <p>Snap a photo and drop a pin — SmartCity 360 routes it to the right department automatically.</p>
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/report')}>Start a report</button>
          </div>

          <div className="card list-card">
            <div className="list-head">
              <h3>Your recent reports</h3>
              <a onClick={() => navigate('/track')}>View all</a>
            </div>
            {loading && <div style={{ padding: 18, fontSize: 13, color: 'var(--ink-soft)' }}>Loading…</div>}
            {error && <div style={{ padding: 18, fontSize: 13, color: 'var(--red-500)' }}>{error}</div>}
            {!loading && !error && complaints.length === 0 && (
              <div style={{ padding: 18, fontSize: 13, color: 'var(--ink-soft)' }}>
                You haven't reported anything yet.
              </div>
            )}
            {complaints.slice(0, 5).map((c) => (
              <div key={c.id} className="complaint-row" onClick={() => navigate(`/track?id=${c.id}`)}>
                <div className="cicon"><IconBuilding width="16" height="16" /></div>
                <div className="ctext">
                  <div className="t">{c.description.slice(0, 60)}{c.description.length > 60 ? '…' : ''}</div>
                  <div className="s">{c.publicId} · {c.category || 'Classifying…'}</div>
                </div>
                <Chip value={c.status} />
              </div>
            ))}
          </div>
        </div>

        <div>
          <div className="card side-card">
            <h3>Tips for a fast resolution</h3>
            <p style={{ margin: 0, fontSize: 12.5, color: 'var(--ink-soft)', lineHeight: 1.6 }}>
              Reports with a clear description, a photo, and an accurate location are classified with higher
              confidence and typically reach the right department faster.
            </p>
          </div>
        </div>
      </div>
    </>
  );
}

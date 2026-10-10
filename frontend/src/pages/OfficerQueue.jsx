import { useEffect, useState } from 'react';
import client from '../api/client';
import { formatApiDateTime } from '../utils/dateTime';
import { useAuth } from '../context/AuthContext';
import Chip from '../components/Chip';
import { IconRefresh, IconUpload } from '../components/Icons';

export default function OfficerQueue() {
  const { user } = useAuth();
  const [complaints, setComplaints] = useState([]);
  const [officers, setOfficers] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [filter, setFilter] = useState('All');
  const [officerId, setOfficerId] = useState('');
  const [status, setStatus] = useState('IN_PROGRESS');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [photoSrc, setPhotoSrc] = useState('');

  function load() {
    setLoading(true);
    client.get('/officer/queue', { params: { department: user?.department } })
      .then((res) => {
        setComplaints(res.data);
        if (res.data.length && !selectedId) setSelectedId(res.data[0].id);
      })
      .finally(() => setLoading(false));

    client.get('/admin/officers').then((res) => setOfficers(res.data)).catch(() => {});
  }

  useEffect(load, []); // eslint-disable-line react-hooks/exhaustive-deps

  const filtered = complaints.filter((c) => {
    if (filter === 'All') return true;
    if (filter === 'High priority') return c.priority === 'HIGH';
    if (filter === 'Unassigned') return !c.officerName;
    if (filter === 'In progress') return c.status === 'IN_PROGRESS';
    if (filter === 'Overdue') return c.overdue;
    return true;
  });

  const selected = complaints.find((c) => c.id === selectedId);

  useEffect(() => {
    let objectUrl;
    let active = true;
    setPhotoSrc('');
    if (selected?.photoUrl) {
      client.get(selected.photoUrl, { responseType: 'blob' }).then(({ data }) => {
        objectUrl = URL.createObjectURL(data);
        if (active) setPhotoSrc(objectUrl);
        else URL.revokeObjectURL(objectUrl);
      }).catch(() => {});
    }
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [selected?.photoUrl]);

  async function assign() {
    if (!selected || !officerId) return;
    setBusy(true);
    try {
      await client.post(`/officer/complaints/${selected.id}/assign`, { officerId: Number(officerId) });
      window.dispatchEvent(new CustomEvent('notification-updated'));
      load();
    } catch (err) {
      alert(err?.response?.data?.error || 'Could not assign officer.');
    } finally {
      setBusy(false);
    }
  }

  async function saveStatus() {
    if (!selected) return;
    setBusy(true);
    try {
      await client.patch(`/officer/complaints/${selected.id}/status`, { status });
      window.dispatchEvent(new CustomEvent('notification-updated'));
      load();
    } catch (err) {
      alert(err?.response?.data?.error || 'Could not update status.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Priority queue</h1>
          <p>{user?.department || 'All departments'} · {complaints.length} open reports</p>
        </div>
        <button className="btn btn-ghost" onClick={load}><IconRefresh /> Refresh</button>
      </div>

      <div className="filters">
        {['All', 'High priority', 'Unassigned', 'In progress', 'Overdue'].map((f) => (
          <button key={f} className={`filter-chip${filter === f ? ' active' : ''}`} onClick={() => setFilter(f)}>{f}</button>
        ))}
      </div>

      <div className="officer-layout">
        <div className="card" style={{ padding: '14px 0 4px' }}>
          {loading && <div style={{ padding: 18, fontSize: 13, color: 'var(--ink-soft)' }}>Loading…</div>}
          <table className="qtable">
            <thead><tr><th>Report</th><th>Category</th><th>Priority</th><th>Status</th><th>SLA</th></tr></thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id} className={c.id === selectedId ? 'sel' : ''} onClick={() => setSelectedId(c.id)}>
                  <td><div style={{ fontWeight: 600 }}>{c.description.slice(0, 34)}{c.description.length > 34 ? '…' : ''}</div><div className="qid">{c.publicId}</div></td>
                  <td>{c.category || '—'}</td>
                  <td><Chip value={c.priority} /></td>
                  <td><Chip value={c.status} /></td>
                  <td><span className={`sla-indicator${c.overdue ? ' overdue' : ''}`}>
                    {c.overdue ? 'Overdue' : 'Due'}<br />{formatSla(c.slaDueAt)}
                  </span></td>
                </tr>
              ))}
              {!loading && filtered.length === 0 && (
                <tr><td colSpan="5" style={{ padding: 18, color: 'var(--ink-soft)' }}>No reports match this filter.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {selected && (
          <div className="card detail-panel">
            <h3>{selected.description.slice(0, 60)}{selected.description.length > 60 ? '…' : ''}</h3>
            <div className="sub">{selected.publicId} · {selected.category} · <Chip value={selected.priority} /></div>
            <div className="field">
              <label>SLA deadline</label>
              <div className={`sla-indicator${selected.overdue ? ' overdue' : ''}`}>
                {selected.overdue ? 'Overdue · ' : 'Due · '}{formatSla(selected.slaDueAt)}
              </div>
            </div>
            <div className="field"><label>Location</label><div>{selected.location || 'Not provided'}</div></div>
            {selected.photoUrl && (
              <div className="field">
                <label>Report image</label>
                {photoSrc ? <img className="report-photo" src={photoSrc} alt={`Image for report ${selected.publicId}`} /> : <div className="sub">Loading image…</div>}
              </div>
            )}

            <div className="field">
              <label>Assign field officer</label>
              <select value={officerId} onChange={(e) => setOfficerId(e.target.value)}>
                <option value="">Select an officer…</option>
                {officers.map((o) => <option key={o.id} value={o.id}>{o.name} — {o.department}</option>)}
              </select>
            </div>

            <div className="field">
              <label>Update status</label>
              <select value={status} onChange={(e) => setStatus(e.target.value)}>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="RESOLVED">Resolved — awaiting citizen verification</option>
              </select>
            </div>

            <div className="action-stack">
              <button className="btn btn-primary" style={{ justifyContent: 'center' }} disabled={busy || !officerId} onClick={assign}>
                <IconUpload /> Assign officer
              </button>
              <button className="btn btn-dark" style={{ justifyContent: 'center' }} disabled={busy} onClick={saveStatus}>
                Save status
              </button>
            </div>
          </div>
        )}
      </div>
    </>
  );
}

function formatSla(value) {
  return value ? formatApiDateTime(value) : 'Not set';
}

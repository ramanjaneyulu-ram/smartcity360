import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import client from '../api/client';
import { formatApiDateTime, parseApiDate } from '../utils/dateTime';
import Chip from '../components/Chip';

const STEPS = ['SUBMITTED', 'CLASSIFIED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED'];
const STEP_LABELS = ['Submitted', 'Classified', 'Assigned', 'In Progress', 'Resolved', 'Verified'];

export default function TrackReports() {
  const [params] = useSearchParams();
  const [complaints, setComplaints] = useState([]);
  const [selectedId, setSelectedId] = useState(params.get('id') ? Number(params.get('id')) : null);
  const [loading, setLoading] = useState(true);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [submittingFeedback, setSubmittingFeedback] = useState(false);
  const [complaintNotifications, setComplaintNotifications] = useState([]);
  const [history, setHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  function load() {
    setLoading(true);
    client.get('/complaints/mine').then((res) => {
      setComplaints(res.data);
      if (!selectedId && res.data.length) setSelectedId(res.data[0].id);
    }).finally(() => setLoading(false));
  }

  useEffect(load, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!selectedId) return;
    setHistoryLoading(true);
    client.get(`/complaints/${selectedId}/history`)
      .then((res) => setHistory(res.data))
      .catch(() => setHistory([]))
      .finally(() => setHistoryLoading(false));
    client.get(`/notifications/complaint/${selectedId}`)
      .then((res) => setComplaintNotifications(res.data))
      .catch(() => setComplaintNotifications([]));
  }, [selectedId]);

  const selected = complaints.find((c) => c.id === selectedId);
  const currentStepIndex = selected ? STEPS.indexOf(selected.status) : -1;

  async function submitFeedback() {
    if (!selected) return;
    setSubmittingFeedback(true);
    try {
      await client.post(`/complaints/${selected.id}/feedback`, { rating, comment });
      window.dispatchEvent(new CustomEvent('notification-updated'));
      load();
      client.get(`/notifications/complaint/${selected.id}`)
        .then((res) => setComplaintNotifications(res.data))
        .catch(() => {});
    } catch (err) {
      alert(err?.response?.data?.error || 'Could not submit feedback.');
    } finally {
      setSubmittingFeedback(false);
    }
  }

  if (loading) return <p style={{ color: 'var(--ink-soft)' }}>Loading your reports…</p>;

  if (!complaints.length) {
    return (
      <div className="page-head">
        <div>
          <h1>Track my reports</h1>
          <p>You haven't submitted any reports yet.</p>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Track my reports</h1>
          <p>{complaints.length} report{complaints.length === 1 ? '' : 's'} on file</p>
        </div>
      </div>

      <div className="officer-layout">
        <div className="card" style={{ padding: '6px 0' }}>
          {complaints.map((c) => (
            <div key={c.id} className={`complaint-row${c.id === selectedId ? ' sel' : ''}`} onClick={() => setSelectedId(c.id)}>
              <div className="ctext">
                <div className="t">{c.description.slice(0, 50)}{c.description.length > 50 ? '…' : ''}</div>
                <div className="s">{c.publicId} · {c.category || 'Classifying…'}</div>
              </div>
              <Chip value={c.status} />
            </div>
          ))}
        </div>

        {selected && (
          <div>
            <div className="track-head" style={{ marginBottom: 14, flexDirection: 'column', alignItems: 'flex-start' }}>
              <h1 style={{ fontSize: 19 }}>{selected.description}</h1>
              <div className="meta">{selected.publicId} · Submitted {parseApiDate(selected.createdAt)?.toLocaleDateString()}</div>
            </div>

            <div className="card" style={{ padding: '6px 14px 14px' }}>
              <div className="stepper">
                {STEPS.map((s, i) => (
                  <div key={s} className={`step ${i < currentStepIndex ? 'done' : i === currentStepIndex ? 'current' : ''}`}>
                    {i > 0 && <div className="line" />}
                    <div className="dot">{i < currentStepIndex ? '✓' : i + 1}</div>
                    <div className="lbl">{STEP_LABELS[i]}</div>
                  </div>
                ))}
              </div>
            </div>

            <div className="detail-card card" style={{ marginTop: 16 }}>
              <h3>Report details</h3>
              <div className="kv"><span className="k">Category</span><span className="v">{selected.category || '—'}</span></div>
              <div className="kv"><span className="k">Department</span><span className="v">{selected.department || '—'}</span></div>
              <div className="kv"><span className="k">Priority</span><span className="v"><Chip value={selected.priority} /></span></div>
              <div className="kv"><span className="k">Location</span><span className="v">{selected.location || '—'}</span></div>
              <div className="kv"><span className="k">Assigned officer</span><span className="v">{selected.officerName || 'Not yet assigned'}</span></div>
              <div className="kv"><span className="k">SLA deadline</span><span className={`v${selected.overdue ? ' sla-text-overdue' : ''}`}>
                {selected.slaDueAt ? formatDateTime(selected.slaDueAt) : 'Not set'}{selected.overdue ? ' · Overdue' : ''}
              </span></div>
            </div>

            <div className="detail-card card" style={{ marginTop: 16 }}>
              <h3>Status timeline</h3>
              {historyLoading ? (
                <div className="sub">Loading status history…</div>
              ) : history.length === 0 ? (
                <div className="sub">No status history recorded yet.</div>
              ) : (
                <div className="status-timeline">
                  {history.map((entry, index) => (
                    <div className="status-timeline-item" key={`${entry.status}-${entry.createdAt}-${index}`}>
                      <span className="status-timeline-dot" />
                      <div>
                        <div className="status-timeline-heading">
                          <strong>{entry.status.replaceAll('_', ' ')}</strong>
                          <time>{formatDateTime(entry.createdAt)}</time>
                        </div>
                        <div className="status-timeline-message">{entry.message}</div>
                        {entry.actorName && <div className="status-timeline-actor">Updated by {entry.actorName}</div>}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="detail-card card" style={{ marginTop: 16 }}>
              <h3>Activity & Notification Log</h3>
              {complaintNotifications.length === 0 ? (
                <div style={{ fontSize: 13, color: 'var(--ink-soft)' }}>
                  No updates or notification events recorded yet for this report.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {complaintNotifications.map((n) => (
                    <div key={n.id} style={{ borderLeft: '3px solid var(--teal-700)', paddingLeft: 10 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontWeight: 600, fontSize: 13, color: 'var(--ink)' }}>{n.title}</span>
                        <span style={{ fontSize: 11, color: 'var(--ink-soft)' }}>
                          {formatApiDateTime(n.createdAt)}
                        </span>
                      </div>
                      <div style={{ fontSize: 12.5, color: 'var(--ink-soft)', marginTop: 2 }}>{n.message}</div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="detail-card card" style={{ marginTop: 16 }}>
              <h3>Confirm resolution</h3>
              {selected.status === 'RESOLVED' ? (
                <>
                  <div className="field">
                    <label>Rating</label>
                    <select value={rating} onChange={(e) => setRating(Number(e.target.value))}>
                      {[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n} / 5</option>)}
                    </select>
                  </div>
                  <div className="field">
                    <label>Comment (optional)</label>
                    <textarea rows="2" value={comment} onChange={(e) => setComment(e.target.value)} />
                  </div>
                  <button className="btn btn-primary btn-sm" disabled={submittingFeedback} onClick={submitFeedback}>
                    {submittingFeedback ? 'Submitting…' : 'Confirm resolved & give feedback'}
                  </button>
                </>
              ) : (
                <button className="btn btn-ghost btn-sm" disabled>
                  Available once the report is marked resolved
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </>
  );
}

function formatDateTime(value) {
  return formatApiDateTime(value);
}

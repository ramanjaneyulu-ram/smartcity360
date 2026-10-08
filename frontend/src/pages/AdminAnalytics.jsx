import { useEffect, useState } from 'react';
import { Bar, Line } from 'react-chartjs-2';
import {
  Chart as ChartJS, CategoryScale, LinearScale, BarElement, LineElement, PointElement, Tooltip, Legend,
} from 'chart.js';
import client from '../api/client';
import { IconRefresh } from '../components/Icons';

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, Tooltip, Legend);

export default function AdminAnalytics() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [refreshRequest, setRefreshRequest] = useState(0);
  const [officers, setOfficers] = useState([]);
  const [officerForm, setOfficerForm] = useState({ name: '', email: '', password: '', department: '', ward: '' });
  const [officerMessage, setOfficerMessage] = useState('');
  const [officerError, setOfficerError] = useState('');
  const [creatingOfficer, setCreatingOfficer] = useState(false);

  useEffect(() => {
    client.get('/admin/officers')
      .then((res) => setOfficers(res.data))
      .catch(() => setOfficerError('Could not load officer accounts.'));
  }, []);

  useEffect(() => {
    let active = true;
    function refresh() {
      setRefreshing(true);
      client.get('/admin/analytics')
        .then((res) => {
          if (!active) return;
          setData(res.data);
          setError('');
        })
        .catch(() => {
          if (active) setError('Could not load analytics. Is the backend running and are you logged in as admin?');
        })
        .finally(() => {
          if (active) setRefreshing(false);
        });
    }

    function refreshWhenVisible() {
      if (document.visibilityState === 'visible') refresh();
    }

    refresh();
    const interval = window.setInterval(refresh, 30000);
    window.addEventListener('focus', refresh);
    document.addEventListener('visibilitychange', refreshWhenVisible);
    return () => {
      active = false;
      window.clearInterval(interval);
      window.removeEventListener('focus', refresh);
      document.removeEventListener('visibilitychange', refreshWhenVisible);
    };
  }, [refreshRequest]);

  async function createOfficer(event) {
    event.preventDefault();
    setOfficerError('');
    setOfficerMessage('');
    setCreatingOfficer(true);
    try {
      const { data: officer } = await client.post('/admin/officers', officerForm);
      setOfficers((current) => [officer, ...current]);
      setOfficerForm({ name: '', email: '', password: '', department: '', ward: '' });
      setOfficerMessage(`Officer account created for ${officer.name}.`);
    } catch (err) {
      setOfficerError(err?.response?.data?.error || 'Could not create officer account.');
    } finally {
      setCreatingOfficer(false);
    }
  }

  if (error && !data) return <p style={{ color: 'var(--red-500)' }}>{error}</p>;
  if (!data) return <p style={{ color: 'var(--ink-soft)' }}>Loading analytics…</p>;

  const catLabels = Object.keys(data.complaintsByCategory);
  const catValues = Object.values(data.complaintsByCategory);
  const deptLabels = Object.keys(data.complaintsByDepartment);
  const deptValues = Object.values(data.complaintsByDepartment);
  const months = data.monthlyTrend.map((m) => m.month);

  const barOpts = {
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { x: { grid: { display: false } }, y: { beginAtZero: true } },
  };
  const hbarOpts = {
    indexAxis: 'y', responsive: true, maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { x: { beginAtZero: true }, y: { grid: { display: false } } },
  };
  const lineOpts = {
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, usePointStyle: true } } },
    scales: { x: { grid: { display: false } }, y: { beginAtZero: true } },
  };

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Analytics dashboard</h1>
          <p>City-wide grievance activity</p>
        </div>
        <button className="btn btn-ghost" onClick={() => setRefreshRequest((count) => count + 1)} disabled={refreshing}>
          <IconRefresh /> {refreshing ? 'Refreshing…' : 'Refresh'}
        </button>
      </div>

      <div className="kpi-row">
        <div className="kpi"><div className="top"><span className="lbl">Total reports</span></div><div className="num">{data.totalComplaints}</div></div>
        <div className="kpi"><div className="top"><span className="lbl">Avg. resolution time</span></div><div className="num">{data.avgResolutionDays} days</div></div>
        <div className="kpi"><div className="top"><span className="lbl">Resolved</span></div><div className="num">{data.resolvedCount}</div></div>
        <div className="kpi"><div className="top"><span className="lbl">In progress</span></div><div className="num">{data.inProgressCount}</div></div>
      </div>

      <div className="chart-grid">
        <div className="chart-card">
          <h3>Complaints by category</h3>
          <div className="sub">All reports on file</div>
          <div className="chart-wrap">
            <Bar options={barOpts} data={{
              labels: catLabels,
              datasets: [{ data: catValues, backgroundColor: '#D98E3F', borderRadius: 5, maxBarThickness: 34 }],
            }} />
          </div>
        </div>
        <div className="chart-card">
          <h3>Department workload</h3>
          <div className="sub">Open reports by department</div>
          <div className="chart-wrap">
            <Bar options={hbarOpts} data={{
              labels: deptLabels,
              datasets: [{ data: deptValues, backgroundColor: '#245951', borderRadius: 5, maxBarThickness: 18 }],
            }} />
          </div>
        </div>
      </div>

      <div className="chart-grid">
        <div className="chart-card">
          <h3>Monthly trend</h3>
          <div className="sub">Reports filed vs. resolved</div>
          <div className="chart-wrap short">
            <Line options={lineOpts} data={{
              labels: months,
              datasets: [
                { label: 'Filed', data: data.monthlyTrend.map((m) => m.filed), borderColor: '#D98E3F', backgroundColor: 'rgba(217,142,63,0.12)', tension: 0.35, fill: true, pointRadius: 3 },
                { label: 'Resolved', data: data.monthlyTrend.map((m) => m.resolved), borderColor: '#5F8E71', backgroundColor: 'rgba(95,142,113,0.12)', tension: 0.35, fill: true, pointRadius: 3 },
              ],
            }} />
          </div>
        </div>
        <div className="chart-card">
          <h3>Status breakdown</h3>
          <div className="sub">Where every report currently stands</div>
          <div style={{ marginTop: 4 }}>
            <div className="hotspot-row"><span className="area">Resolved</span><div className="track"><div className="fill" style={{ width: `${pct(data.resolvedCount, data.totalComplaints)}%` }} /></div><span className="n">{data.resolvedCount}</span></div>
            <div className="hotspot-row"><span className="area">In progress</span><div className="track"><div className="fill" style={{ width: `${pct(data.inProgressCount, data.totalComplaints)}%` }} /></div><span className="n">{data.inProgressCount}</span></div>
            <div className="hotspot-row"><span className="area">Unassigned</span><div className="track"><div className="fill" style={{ width: `${pct(data.unassignedCount, data.totalComplaints)}%` }} /></div><span className="n">{data.unassignedCount}</span></div>
          </div>
        </div>
      </div>

      <div className="chart-grid">
        <section className="chart-card">
          <h3>Create officer account</h3>
          <div className="sub">Only administrators can add officers.</div>
          <form onSubmit={createOfficer}>
            <div className="field">
              <label>Full name</label>
              <input required value={officerForm.name} onChange={(event) => setOfficerForm({ ...officerForm, name: event.target.value })} />
            </div>
            <div className="field">
              <label>Email</label>
              <input required type="email" value={officerForm.email} onChange={(event) => setOfficerForm({ ...officerForm, email: event.target.value })} />
            </div>
            <div className="field">
              <label>Temporary password</label>
              <input required type="password" minLength="8" value={officerForm.password} onChange={(event) => setOfficerForm({ ...officerForm, password: event.target.value })} />
            </div>
            <div className="field">
              <label>Department</label>
              <input required value={officerForm.department} onChange={(event) => setOfficerForm({ ...officerForm, department: event.target.value })} />
            </div>
            <div className="field">
              <label>Ward</label>
              <input value={officerForm.ward} onChange={(event) => setOfficerForm({ ...officerForm, ward: event.target.value })} />
            </div>
            {officerError && <p style={{ color: 'var(--red-500)' }}>{officerError}</p>}
            {officerMessage && <p>{officerMessage}</p>}
            <button className="btn btn-primary" disabled={creatingOfficer}>
              {creatingOfficer ? 'Creating…' : 'Create officer'}
            </button>
          </form>
        </section>
        <section className="chart-card">
          <h3>Officer accounts</h3>
          <div className="sub">{officers.length} registered</div>
          {officers.length ? officers.map((officer) => (
            <div className="hotspot-row" key={officer.id}>
              <span className="area">{officer.name}</span>
              <span>{officer.department}{officer.ward ? ` · ${officer.ward}` : ''}</span>
            </div>
          )) : <p className="sub">No officer accounts yet.</p>}
        </section>
      </div>
    </>
  );
}

function pct(n, total) {
  if (!total) return 0;
  return Math.round((n / total) * 100);
}

import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';
import Chip from '../components/Chip';
import { IconUpload, IconCamera, IconPin, IconSpark } from '../components/Icons';

export default function ReportProblem() {
  const navigate = useNavigate();
  const [description, setDescription] = useState(
    "The street light near our college has not been working for over a week and it's very dark at night."
  );
  const [location, setLocation] = useState('Near Sri Venkateswara College Gate, Ring Road');
  const [ai, setAi] = useState({ category: 'Street Light', department: 'Electrical', priority: 'Medium', confidence: 92 });
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(null);
  const [locating, setLocating] = useState(false);
  const [locationError, setLocationError] = useState('');
  const [coordinates, setCoordinates] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const fileInputRef = useRef(null);
  const cameraInputRef = useRef(null);
  const debounceRef = useRef(null);

  useEffect(() => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      if (!description.trim()) return;
      client.post('/complaints/classify', { description })
        .then((res) => setAi(res.data))
        .catch(() => {/* silently ignore — preview is best-effort */});
    }, 400);
    return () => clearTimeout(debounceRef.current);
  }, [description]);

  async function onSubmit() {
    setSubmitting(true);
    try {
      let photoUrl;
      if (selectedFile) {
        const formData = new FormData();
        formData.append('file', selectedFile);
        const upload = await client.post('/complaints/photos', formData);
        photoUrl = upload.data.photoUrl;
      }
      const { data } = await client.post('/complaints', { description, location, photoUrl });
      window.dispatchEvent(new CustomEvent('notification-updated'));
      setSubmitted(data);
    } catch (err) {
      alert(err?.response?.data?.error || 'Could not submit your report. Please log in as a citizen and try again.');
    } finally {
      setSubmitting(false);
    }
  }

  function useCurrentLocation() {
    if (!navigator.geolocation) {
      setLocationError('Location is not supported by this browser.');
      return;
    }

    setLocating(true);
    setLocationError('');
    navigator.geolocation.getCurrentPosition(
      async ({ coords }) => {
        const latitude = coords.latitude.toFixed(6);
        const longitude = coords.longitude.toFixed(6);
        setCoordinates({ latitude: coords.latitude, longitude: coords.longitude });
        const coordinateText = `(${latitude}, ${longitude})`;

        try {
          const response = await fetch(
            `https://nominatim.openstreetmap.org/reverse?format=jsonv2&addressdetails=1&lat=${coords.latitude}&lon=${coords.longitude}`,
            { headers: { 'Accept-Language': 'en' } },
          );
          if (!response.ok) throw new Error('Reverse geocoding failed');
          const data = await response.json();
          const address = data.address || {};
          const readableParts = [
            address.road,
            address.neighbourhood || address.suburb || address.hamlet || address.city_district,
            address.city || address.town || address.village,
            address.county || address.state_district,
            address.state,
          ].filter((part, index, parts) => part && parts.indexOf(part) === index);
          setLocation(`${readableParts.join(', ') || data.display_name || 'Current location'} ${coordinateText}`);
        } catch {
          setLocation(`Current location ${coordinateText}`);
        } finally {
          setLocating(false);
        }
      },
      (error) => {
        setLocationError(error.code === error.PERMISSION_DENIED
          ? 'Location permission was denied. Allow access and try again.'
          : 'Could not detect your location. Please try again.');
        setLocating(false);
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
    );
  }

  if (submitted) {
    return (
      <>
        <div className="page-head">
          <div>
            <h1>Report submitted</h1>
            <p>Thanks — {submitted.publicId} has been classified and routed to {submitted.department}.</p>
          </div>
        </div>
        <div className="card detail-card" style={{ maxWidth: 480 }}>
          <div className="kv"><span className="k">Report ID</span><span className="v">{submitted.publicId}</span></div>
          <div className="kv"><span className="k">Category</span><span className="v">{submitted.category}</span></div>
          <div className="kv"><span className="k">Department</span><span className="v">{submitted.department}</span></div>
          <div className="kv"><span className="k">Status</span><span className="v"><Chip value={submitted.status} /></span></div>
          <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
            <button className="btn btn-primary" onClick={() => navigate('/track')}>Track this report</button>
            <button className="btn btn-ghost" onClick={() => { setSubmitted(null); setDescription(''); }}>Report another</button>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Report a problem</h1>
          <p>Describe what you're seeing — SmartCity 360 classifies and routes it for you.</p>
        </div>
      </div>

      <div className="form-grid">
        <div className="card form-card">
          <div className="field">
            <label>What's the problem?</label>
            <textarea rows="4" value={description} onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. The street light near the college gate hasn't worked for a week and it's very dark at night." />
          </div>
          <div className="field">
            <label>Photo or video</label>
            <input
              ref={cameraInputRef}
              className="file-input"
              type="file"
              accept="image/*"
              capture="environment"
              onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
            />
            <input
              ref={fileInputRef}
              className="file-input"
              type="file"
              accept="image/jpeg,image/png,image/gif"
              onChange={(event) => setSelectedFile(event.target.files?.[0] || null)}
            />
            <div className="upload-box" role="button" tabIndex="0"
              onClick={() => fileInputRef.current?.click()}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') fileInputRef.current?.click();
              }}>
              <IconUpload />
              <div>{selectedFile ? selectedFile.name : 'Drag a photo here, or click to upload'}</div>
            </div>
            <button className="btn btn-ghost btn-sm" type="button" style={{ marginTop: 8 }}
              onClick={() => cameraInputRef.current?.click()} disabled={submitting}>
              <IconCamera aria-hidden="true" /> Take photo
            </button>
          </div>
          <div className="field">
            <label>Location</label>
            <div className="loc-row">
              <input type="text" value={location} onChange={(e) => setLocation(e.target.value)} />
              <button className="btn btn-ghost btn-sm" type="button" onClick={useCurrentLocation} disabled={locating}>
                <IconPin /> {locating ? 'Detecting…' : 'Use my location'}
              </button>
            </div>
            {locationError && <div className="location-error">{locationError}</div>}
            <div className={`map-box${coordinates ? ' has-location' : ''}`}>
              {coordinates ? (
                <iframe
                  title="Current location map"
                  loading="lazy"
                  src={`https://www.openstreetmap.org/export/embed.html?bbox=${coordinates.longitude - 0.0015}%2C${coordinates.latitude - 0.0005}%2C${coordinates.longitude + 0.0015}%2C${coordinates.latitude + 0.0005}&layer=mapnik&marker=${coordinates.latitude}%2C${coordinates.longitude}`}
                />
              ) : (
                <svg className="pin" viewBox="0 0 24 24" fill="#BC5340" stroke="#fff" strokeWidth="1">
                  <path d="M12 2C7.6 2 4 5.6 4 10c0 6 8 12 8 12s8-6 8-12c0-4.4-3.6-8-8-8z" />
                </svg>
              )}
            </div>
          </div>
          <button className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }}
            disabled={submitting || !description.trim()} onClick={onSubmit}>
            {submitting ? 'Submitting…' : 'Submit report'}
          </button>
        </div>

        <div>
          <div className="ai-card">
            <div className="ai-head"><IconSpark /><h3>AI classification preview</h3></div>
            <div className="ai-row"><span className="k">Category</span><span className="v">{ai.category}</span></div>
            <div className="ai-row"><span className="k">Department</span><span className="v">{ai.department}</span></div>
            <div className="ai-row"><span className="k">Priority</span><span className="v"><Chip value={ai.priority} /></span></div>
            <div className="ai-row"><span className="k">Confidence</span><span className="v">{ai.confidence}%</span></div>
            <p className="ai-hint">
              This updates as you type, calling the same classifier the backend uses on submission — try mentioning
              "garbage" or "pothole" to see it re-classify.
            </p>
          </div>
        </div>
      </div>
    </>
  );
}

import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  IconHome, IconPlus, IconTrack, IconQueue, IconChart, IconLogout, IconBuilding, IconMoon, IconSun,
} from './Icons';
import NotificationCenter from './NotificationCenter';

const crumbFor = {
  '/': 'Citizen <b>/ Dashboard</b>',
  '/report': 'Citizen <b>/ Report a Problem</b>',
  '/track': 'Citizen <b>/ Track My Reports</b>',
  '/officer': 'Officer <b>/ Priority Queue</b>',
  '/admin': 'Admin <b>/ Analytics Dashboard</b>',
};

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('sc360_theme') === 'dark');
  const [showLogoutModal, setShowLogoutModal] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [swipeDirection, setSwipeDirection] = useState('');
  const swipeStart = useRef(null);

  useEffect(() => {
    localStorage.setItem('sc360_theme', darkMode ? 'dark' : 'light');
  }, [darkMode]);

  useEffect(() => {
    if (!showProfile) return undefined;
    function closeOnEscape(event) {
      if (event.key === 'Escape') setShowProfile(false);
    }
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [showProfile]);

  function handleLogout() {
    setShowLogoutModal(true);
  }

  function confirmLogout() {
    logout();
    setShowLogoutModal(false);
    navigate('/login');
  }

  const accountPages = user?.role === 'CITIZEN'
    ? ['/', '/report', '/track']
    : user?.role === 'ADMIN'
      ? ['/admin', '/officer']
      : ['/officer'];

  function startSwipe(event) {
    if (showLogoutModal) return;
    if (!event.isPrimary || (event.pointerType === 'mouse' && event.button !== 0)) return;
    if (event.target instanceof Element && event.target.closest('button,a,input,textarea,select,[role="button"],[contenteditable="true"]')) return;
    swipeStart.current = { x: event.clientX, y: event.clientY };
  }

  function finishSwipe(event) {
    const start = swipeStart.current;
    swipeStart.current = null;
    if (!start) return;

    const deltaX = event.clientX - start.x;
    const deltaY = event.clientY - start.y;
    if (Math.abs(deltaX) < 50 || Math.abs(deltaX) < Math.abs(deltaY) * 1.25) return;

    const currentIndex = accountPages.indexOf(location.pathname);
    if (currentIndex < 0) return;
    const nextIndex = deltaX > 0 ? currentIndex + 1 : currentIndex - 1;
    if (nextIndex < 0 || nextIndex >= accountPages.length) return;

    setSwipeDirection(deltaX > 0 ? 'right' : 'left');
    navigate(accountPages[nextIndex]);
  }

  useEffect(() => {
    if (!swipeDirection) return undefined;
    const timeout = window.setTimeout(() => setSwipeDirection(''), 300);
    return () => window.clearTimeout(timeout);
  }, [swipeDirection, location.pathname]);

  const crumb = crumbFor[location.pathname] || 'SmartCity 360';
  const initials = user?.name ? user.name.split(' ').map((p) => p[0]).slice(0, 2).join('') : '?';

  return (
    <div
      className={`shell${darkMode ? ' theme-dark' : ''}`}
      onPointerDown={startSwipe}
      onPointerUp={finishSwipe}
      onPointerCancel={() => { swipeStart.current = null; }}
    >
      <nav className="sidebar">
        <div className="brand">
          <div className="mark"><IconBuilding width="19" height="19" /></div>
          <div className="name">SmartCity 360<small>MUNICIPAL SERVICE PLATFORM</small></div>
        </div>

        {user?.role === 'CITIZEN' && (
          <div className="nav-group">
            <div className="nav-label">CITIZEN</div>
            <NavLink to="/" end className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
              <IconHome /> Dashboard
            </NavLink>
            <NavLink to="/report" className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
              <IconPlus /> Report a Problem
            </NavLink>
            <NavLink to="/track" className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
              <IconTrack /> Track My Reports
            </NavLink>
          </div>
        )}

        {(user?.role === 'OFFICER' || user?.role === 'ADMIN') && (
          <div className="nav-group">
            <div className="nav-label">OFFICER</div>
            <NavLink to="/officer" className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
              <IconQueue /> Priority Queue
            </NavLink>
          </div>
        )}

        {user?.role === 'ADMIN' && (
          <div className="nav-group">
            <div className="nav-label">ADMIN</div>
            <NavLink to="/admin" className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
              <IconChart /> Analytics Dashboard
            </NavLink>
          </div>
        )}

        <div className="sidebar-foot">
          <button className="role-pill" onClick={() => setShowProfile(true)} aria-haspopup="dialog" aria-expanded={showProfile}>
            <div className="avatar">{initials}</div>
            <div className="who">
              <b>{user?.name}</b>
              <span>{user?.role === 'CITIZEN' ? 'Citizen account' : user?.role === 'OFFICER' ? `Officer · ${user?.department || ''}` : 'Administrator'}</span>
            </div>
          </button>
        </div>
      </nav>

      <div className="main">
        <div className="topbar">
          <div className="crumb" dangerouslySetInnerHTML={{ __html: crumb }} />
          <div className="topbar-actions">
            <NotificationCenter />
            <button className="icon-btn" onClick={() => setDarkMode((enabled) => !enabled)} aria-label={darkMode ? 'Switch to light theme' : 'Switch to dark theme'}>
              {darkMode ? <IconSun /> : <IconMoon />}
            </button>
            <button className="icon-btn" onClick={handleLogout} aria-label="Log out">
              <IconLogout />
            </button>
          </div>
        </div>
        <div
          className={`view${swipeDirection ? ` swipe-enter-${swipeDirection}` : ''}`}
        >
          {children}
        </div>
      </div>

      {showProfile && (
        <div className="modal-overlay" onClick={() => setShowProfile(false)}>
          <section className="profile-modal" role="dialog" aria-modal="true" aria-labelledby="profile-title" onClick={(event) => event.stopPropagation()}>
            <div className="profile-modal-head">
              <div className="avatar">{initials}</div>
              <div>
                <h3 id="profile-title">Your profile</h3>
                <p>{user?.name}</p>
              </div>
            </div>
            <div className="profile-details">
              <div><span>Email</span><strong>{user?.email || 'Email not available'}</strong></div>
              <div><span>Account type</span><strong>{user?.role === 'CITIZEN' ? 'Citizen' : user?.role === 'OFFICER' ? 'Officer' : 'Administrator'}</strong></div>
              {user?.role === 'OFFICER' && user?.department && (
                <div><span>Department</span><strong>{user.department}</strong></div>
              )}
            </div>
            <div className="modal-actions">
              <button className="btn btn-ghost" onClick={() => setShowProfile(false)}>Close</button>
            </div>
          </section>
        </div>
      )}

      {showLogoutModal && (
        <div className="modal-overlay" onClick={() => setShowLogoutModal(false)}>
          <div className="logout-modal" onClick={(event) => event.stopPropagation()}>
            <div className="logout-modal-icon">
              <IconLogout />
            </div>
            <h3>Sign Out</h3>
            <p>Are you sure you want to sign out of <strong>SmartCity360?</strong></p>
            <div className="modal-actions logout-actions">
              <button className="btn btn-ghost" onClick={() => setShowLogoutModal(false)}>Cancel</button>
              <button className="btn btn-danger" onClick={confirmLogout}>Sign Out</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

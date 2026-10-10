import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import { IconBell } from './Icons';
import { formatApiDateTime } from '../utils/dateTime';

export default function NotificationCenter() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef(null);

  const fetchUnreadCount = async () => {
    try {
      const res = await client.get('/notifications/unread-count');
      setUnreadCount(res.data.unreadCount || 0);
    } catch {
      // silently ignore if offline or not logged in
    }
  };

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const res = await client.get('/notifications');
      setNotifications(res.data || []);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 25000);
    return () => clearInterval(interval);
  }, []);

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setOpen(false);
      }
    }
    if (open) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  const toggleDropdown = () => {
    const nextState = !open;
    setOpen(nextState);
    if (nextState) {
      fetchNotifications();
    }
  };

  const handleMarkAsRead = async (notification) => {
    if (!notification.read) {
      try {
        await client.patch(`/notifications/${notification.id}/read`);
        setNotifications((prev) =>
          prev.map((n) => (n.id === notification.id ? { ...n, read: true } : n))
        );
        setUnreadCount((c) => Math.max(0, c - 1));
      } catch {
        // ignore
      }
    }

    if (notification.complaintId) {
      setOpen(false);
      if (user?.role === 'CITIZEN') {
        navigate(`/track?id=${notification.complaintId}`);
      } else {
        navigate('/officer');
      }
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await client.patch('/notifications/read-all');
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch {
      // ignore
    }
  };

  return (
    <div className="notif-wrapper" ref={dropdownRef}>
      <button
        className="icon-btn notif-bell-btn"
        onClick={toggleDropdown}
        aria-label="View notifications"
        title="Notifications"
      >
        <IconBell />
        {unreadCount > 0 && (
          <span className="notif-badge">{unreadCount > 9 ? '9+' : unreadCount}</span>
        )}
      </button>

      {open && (
        <div className="notif-popover">
          <div className="notif-header">
            <div className="notif-title">
              Notifications {unreadCount > 0 && <span className="notif-unread-count">({unreadCount} new)</span>}
            </div>
            {notifications.some((n) => !n.read) && (
              <button className="notif-mark-all" onClick={handleMarkAllRead}>
                Mark all read
              </button>
            )}
          </div>

          <div className="notif-list">
            {loading && <div className="notif-empty">Loading notifications…</div>}
            {!loading && notifications.length === 0 && (
              <div className="notif-empty">No notifications yet.</div>
            )}
            {!loading &&
              notifications.map((n) => (
                <div
                  key={n.id}
                  className={`notif-item${n.read ? '' : ' unread'}`}
                  onClick={() => handleMarkAsRead(n)}
                >
                  <div className="notif-item-top">
                    <span className="notif-item-title">{n.title}</span>
                    <span className="notif-time">
                      {formatApiDateTime(n.createdAt, { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  <div className="notif-item-msg">{n.message}</div>
                  {n.complaintPublicId && (
                    <div className="notif-item-tag">{n.complaintPublicId}</div>
                  )}
                </div>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}

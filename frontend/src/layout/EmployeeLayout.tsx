import React, { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../security/AuthContext'
import {
  connectSocket,
  disconnectSocket,
  subscribeToBranch,
  type NotificationMessage,
} from '../api/NotificationSocket'
import {
  getUnreadCountApi,
  getNotificationsApi,
  markNotificationReadApi,
  clearNotificationsApi,
  type Notification,
} from '../api/NotificationApi'

const EmployeeLayout: React.FC = () => {
  const { employee, logout, token } = useAuth()
  const navigate = useNavigate()
  const [unreadCount, setUnreadCount] = useState(0)
  const [showNotifications, setShowNotifications] = useState(false)
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [toasts, setToasts] = useState<Array<{ id: string; title: string; body?: string }>>([])

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  const refreshUnread = async () => {
    try {
      const res = await getUnreadCountApi()
      setUnreadCount(res.data.count)
    } catch {
      // ignore
    }
  }

  const loadNotifications = async () => {
    try {
      const res = await getNotificationsApi()
      setNotifications(res.data)
    } catch {
      // ignore
    }
  }

  useEffect(() => {
    void refreshUnread()
    // connect to WebSocket for live notifications
    if (employee && token) {
      connectSocket(token, (msg: NotificationMessage) => {
        // When a notification arrives, refresh unread count and optionally append
        void refreshUnread()
        // If panel is open, fetch latest notifications
        if (showNotifications) {
          void loadNotifications()
        }

        // show a transient toast
        const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
        const title = msg.type ?? 'Notification'
        const body = msg.reason ?? msg.message ?? (msg.date ? `${msg.date} ${msg.shiftType ?? ''}` : '')
        setToasts((t) => [{ id, title, body }, ...t])
        // auto-dismiss
        setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 6000)
      })

      // If HR, subscribe to branch topic
      if (employee.isHRManager && employee.branchId != null) {
        subscribeToBranch(employee.branchId, (msg) => {
          void refreshUnread()
          if (showNotifications) void loadNotifications()
        })
      }
    }

    return () => {
      disconnectSocket()
    }
  }, [])

  const toggleNotifications = () => {
    const next = !showNotifications
    setShowNotifications(next)
    if (next) {
      void loadNotifications()
    }
  }

  const handleClearNotifications = async () => {
    try {
      await clearNotificationsApi()
      // reload from server to ensure UI matches persisted state
      await loadNotifications()
      void refreshUnread()
    } catch {
      // ignore
    }
  }

  const handleMarkRead = async (id: number) => {
    try {
      await markNotificationReadApi(id)
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n)),
      )
      void refreshUnread()
    } catch {
      // ignore
    }
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header-left">
          <h1 className="app-title">Employee workspace</h1>
          {employee && (
            <span className="app-subtitle">Welcome, {employee.name}</span>
          )}
        </div>

        <div className="app-header-right">
          <button
            type="button"
            className="notification-bell"
            onClick={toggleNotifications}
          >
            🔔
            {unreadCount > 0 && (
              <span className="notification-badge">{unreadCount}</span>
            )}
          </button>

          <button type="button" className="link-button" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

        {/* Toasts */}
        <div style={{ position: 'fixed', right: 16, top: 72, zIndex: 1200 }}>
          {toasts.map((t) => (
            <div key={t.id} style={{ background: '#0f1724', color: '#fff', padding: '0.6rem 0.9rem', borderRadius: 8, marginBottom: 8, boxShadow: '0 6px 18px rgba(2,6,23,0.6)' }}>
              <div style={{ fontWeight: 600 }}>{t.title}</div>
              {t.body && <div style={{ fontSize: '0.9rem', opacity: 0.85 }}>{t.body}</div>}
            </div>
          ))}
        </div>

      {showNotifications && (
        <div className="notification-panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2>Notifications</h2>
            <div>
              <button type="button" className="link-button" onClick={() => void loadNotifications()}>
                Refresh
              </button>
              <button type="button" className="link-button" onClick={handleClearNotifications}>
                Clear all
              </button>
            </div>
          </div>
          {notifications.length === 0 && <p>No notifications yet.</p>}
          <ul>
            {notifications.map((n) => (
              <li key={n.id} className={n.read ? 'read' : 'unread'} title={n.body}>
                <div className="notification-title">{n.title}</div>
                <div className="notification-body" title={n.body}>{n.body}</div>
                <div className="notification-meta">
                  <span>{new Date(n.createdAt).toLocaleString()}</span>
                  {!n.read && (
                    <button
                      type="button"
                      className="link-button"
                      onClick={() => void handleMarkRead(n.id)}
                    >
                      Mark as read
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="app-body">
        <nav className="app-nav">
          <NavLink
            to="/me"
            end
            className={({ isActive }) =>
              isActive ? 'app-nav-link active' : 'app-nav-link'
            }
          >
            My schedule
          </NavLink>
          <NavLink
            to="/me/availability"
            className={({ isActive }) =>
              isActive ? 'app-nav-link active' : 'app-nav-link'
            }
          >
            My availability
          </NavLink>
          <NavLink
            to="/me/profile"
            className={({ isActive }) =>
              isActive ? 'app-nav-link active' : 'app-nav-link'
            }
          >
            My profile
          </NavLink>
          <NavLink
            to="/me/requests"
            className={({ isActive }) =>
              isActive ? 'app-nav-link active' : 'app-nav-link'
            }
          >
            Requests to HR
          </NavLink>
        </nav>

        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default EmployeeLayout

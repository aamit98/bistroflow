// src/layout/HrLayout.tsx
import React, { useEffect, useState, useRef } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../security/AuthContext'
import { useBranch } from '../context/BranchContext'
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
import '../styles/premium.css'

const HrLayout: React.FC = () => {
  const { employee, logout, token } = useAuth()
  const navigate = useNavigate()
  
  // Use the shared branch context
  const { branches, selectedBranchId, selectedBranch, setSelectedBranchId } = useBranch()

  const [unreadCount, setUnreadCount] = useState(0)
  const [showNotifications, setShowNotifications] = useState(false)
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [toasts, setToasts] = useState<Array<{ id: string; title: string; body?: string }>>([])
  const notificationPanelRef = useRef<HTMLDivElement>(null)

  const handleLogout = () => {
    logout()
    navigate('/login')
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

  const handleBranchChange = (branchId: number) => {
    setSelectedBranchId(branchId)
    // Navigate to the new branch's schedule page
    navigate(`/hr/branches/${branchId}/schedule`)
  }

  useEffect(() => {
    void refreshUnread()
    if (employee && token) {
      connectSocket(token, (msg: NotificationMessage) => {
        void refreshUnread()
        if (showNotifications) void loadNotifications()

        const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
        const title = msg.type ?? 'Notification'
        const body = msg.reason ?? msg.message ?? ''
        setToasts((t) => [{ id, title, body }, ...t])
        setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 6000)
      })

      if (employee.isHRManager && selectedBranchId != null) {
        subscribeToBranch(selectedBranchId, () => {
          void refreshUnread()
          if (showNotifications) void loadNotifications()
        })
      }
    }

    return () => {
      disconnectSocket()
    }
  }, [employee, token, selectedBranchId])

  // Close notification panel on outside click
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (notificationPanelRef.current && !notificationPanelRef.current.contains(event.target as Node)) {
        setShowNotifications(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const toggleNotifications = () => {
    const next = !showNotifications
    setShowNotifications(next)
    if (next) void loadNotifications()
  }

  const handleMarkRead = async (id: number) => {
    try {
      await markNotificationReadApi(id)
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)))
      void refreshUnread()
    } catch {
      // ignore
    }
  }

  const handleClearNotifications = async () => {
    try {
      await clearNotificationsApi()
      await loadNotifications()
      void refreshUnread()
    } catch {
      // ignore
    }
  }

  // Use selected branch or fall back to employee's branch
  const activeBranchId = selectedBranchId ?? employee?.branchId
  const initials = employee?.name?.split(' ').map(n => n[0]).join('').toUpperCase() || 'HR'

  return (
    <div className="bf-layout bf-theme-hr">
      {/* Sidebar */}
      <aside className="bf-sidebar">
        <div className="bf-sidebar-header">
          <div className="bf-logo">
            <img 
              src="/assets/bistro-flow-logo.svg" 
              alt="BistroFlow" 
              className="bf-logo-icon"
              style={{ background: 'transparent', boxShadow: 'none' }}
            />
            <div className="bf-logo-text">
              <h1>BistroFlow</h1>
              <span>HR Manager</span>
            </div>
          </div>
        </div>

        <nav className="bf-nav">
          {/* Branch Selector */}
          {branches.length > 0 && (
            <div className="bf-nav-section">
              <div className="bf-nav-section-title">Branch</div>
              <div style={{ padding: '0 6px' }}>
                <select 
                  className="bf-form-select"
                  value={selectedBranchId ?? ''} 
                  onChange={(e) => handleBranchChange(Number(e.target.value))}
                >
                  {branches.map(branch => (
                    <option key={branch.id} value={branch.id}>
                      {branch.name} ({branch.city})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          )}

          <div className="bf-nav-section">
            <div className="bf-nav-section-title">Overview</div>
            <NavLink to="/hr" end className={({ isActive }) => `bf-nav-link ${isActive ? 'active' : ''}`}>
              <span className="bf-nav-icon">📊</span>
              Dashboard
            </NavLink>
          </div>

          <div className="bf-nav-section">
            <div className="bf-nav-section-title">Workforce</div>
            <NavLink to={`/hr/branches/${activeBranchId}/employees`} className={({ isActive }) => `bf-nav-link ${isActive ? 'active' : ''}`}>
              <span className="bf-nav-icon">👥</span>
              Employees
            </NavLink>
            <NavLink to={`/hr/branches/${activeBranchId}/schedule`} className={({ isActive }) => `bf-nav-link ${isActive ? 'active' : ''}`}>
              <span className="bf-nav-icon">📅</span>
              Weekly Schedule
            </NavLink>
            <NavLink to={`/hr/branches/${activeBranchId}/time-off`} className={({ isActive }) => `bf-nav-link ${isActive ? 'active' : ''}`}>
              <span className="bf-nav-icon">🏖️</span>
              Time-off Requests
              {unreadCount > 0 && <span className="bf-nav-badge">{unreadCount}</span>}
            </NavLink>
          </div>

          <div className="bf-nav-section">
            <div className="bf-nav-section-title">Operations</div>
            <NavLink to={`/hr/branches/${activeBranchId}/inventory`} className={({ isActive }) => `bf-nav-link ${isActive ? 'active' : ''}`}>
              <span className="bf-nav-icon">📦</span>
              Inventory
            </NavLink>
            <NavLink to="/hr/branch-settings" className={({ isActive }) => `bf-nav-link ${isActive ? 'active' : ''}`}>
              <span className="bf-nav-icon">⚙️</span>
              Branch Settings
            </NavLink>
          </div>
        </nav>

        {/* User Footer */}
        <div className="bf-sidebar-footer">
          <div className="bf-user-card">
            <div className="bf-user-avatar">{initials}</div>
            <div className="bf-user-info">
              <div className="bf-user-name">{employee?.name || 'HR Manager'}</div>
              <div className="bf-user-role">{selectedBranch?.name || 'HR Portal'}</div>
            </div>
          </div>
          <button className="bf-logout-btn-full" onClick={handleLogout}>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16,17 21,12 16,7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="bf-main">
        {/* Header */}
        <header className="bf-header">
          <div className="bf-header-left">
            <h1>HR Management</h1>
            <p>{selectedBranch?.name || 'Select a branch'} • {new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</p>
          </div>
          <div className="bf-header-right">
            <button 
              className="bf-notification-btn" 
              onClick={toggleNotifications}
            >
              🔔
              {unreadCount > 0 && <span className="bf-notification-badge">{unreadCount}</span>}
            </button>
          </div>
        </header>

        {/* Toast Notifications */}
        <div className="bf-toast-container">
          {toasts.map((t) => (
            <div key={t.id} className="bf-toast">
              <div className="bf-toast-title">{t.title}</div>
              {t.body && <div className="bf-toast-body">{t.body}</div>}
            </div>
          ))}
        </div>

        {/* Notification Panel */}
        {showNotifications && (
          <div className="bf-notification-panel" ref={notificationPanelRef}>
            <div className="bf-notification-header">
              <h3>Notifications</h3>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button className="bf-btn bf-btn-ghost bf-btn-sm" onClick={() => void loadNotifications()}>
                  Refresh
                </button>
                <button className="bf-btn bf-btn-ghost bf-btn-sm" onClick={handleClearNotifications}>
                  Clear all
                </button>
              </div>
            </div>
            <div className="bf-notification-list">
              {notifications.length === 0 ? (
                <div className="bf-empty-state" style={{ padding: '40px 20px' }}>
                  <div className="bf-empty-icon">🔔</div>
                  <h3>No notifications</h3>
                  <p>You're all caught up!</p>
                </div>
              ) : (
                notifications.map(n => (
                  <div 
                    key={n.id} 
                    className={`bf-notification-item ${!n.read ? 'unread' : ''}`}
                    onClick={() => !n.read && void handleMarkRead(n.id)}
                  >
                    <div className="bf-notification-title">{n.title}</div>
                    <div className="bf-notification-body">{n.body}</div>
                    <div className="bf-notification-time">
                      {new Date(n.createdAt).toLocaleString()}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {/* Page Content */}
        <div className="bf-content">
          <Outlet />
        </div>
      </main>
    </div>
  )
}

export default HrLayout

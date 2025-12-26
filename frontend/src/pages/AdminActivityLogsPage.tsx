import React, { useState } from 'react'
import './admin/Admin.css'

interface ActivityLog {
  id: number
  timestamp: string
  user: string
  action: string
  target: string
  details: string
  type: 'info' | 'success' | 'warning' | 'error'
}

// Mock activity logs for demo
const mockLogs: ActivityLog[] = [
  { id: 1, timestamp: new Date().toISOString(), user: 'System', action: 'Application Started', target: 'System', details: 'BistroFlow server started successfully', type: 'info' },
  { id: 2, timestamp: new Date(Date.now() - 1000 * 60 * 5).toISOString(), user: 'admin', action: 'Login', target: 'Auth', details: 'Super Admin logged in', type: 'success' },
  { id: 3, timestamp: new Date(Date.now() - 1000 * 60 * 15).toISOString(), user: 'Sarah Cohen', action: 'Schedule Published', target: 'Downtown TLV', details: 'Published week schedule for Dec 8-14', type: 'success' },
  { id: 4, timestamp: new Date(Date.now() - 1000 * 60 * 30).toISOString(), user: 'Sarah Cohen', action: 'Time-Off Approved', target: 'Employee #3', details: 'Approved request for Dec 10', type: 'info' },
  { id: 5, timestamp: new Date(Date.now() - 1000 * 60 * 60).toISOString(), user: 'Miriam Katz', action: 'Time-Off Request', target: 'HR', details: 'Requested Dec 10 morning off - Doctor appointment', type: 'info' },
  { id: 6, timestamp: new Date(Date.now() - 1000 * 60 * 90).toISOString(), user: 'System', action: 'Low Stock Alert', target: 'Downtown TLV', details: 'Ground Beef below threshold (5 kg remaining)', type: 'warning' },
  { id: 7, timestamp: new Date(Date.now() - 1000 * 60 * 120).toISOString(), user: 'Yossi Levy', action: 'Shift Confirmed', target: 'Schedule', details: 'Confirmed morning shift on Dec 9', type: 'success' },
  { id: 8, timestamp: new Date(Date.now() - 1000 * 60 * 180).toISOString(), user: 'admin', action: 'HR Manager Created', target: 'Users', details: 'Created HR manager account for Sarah Cohen', type: 'success' },
  { id: 9, timestamp: new Date(Date.now() - 1000 * 60 * 240).toISOString(), user: 'admin', action: 'Branch Created', target: 'Organization', details: 'Created Downtown TLV branch', type: 'success' },
  { id: 10, timestamp: new Date(Date.now() - 1000 * 60 * 300).toISOString(), user: 'System', action: 'Demo Data Seeded', target: 'Database', details: 'Initial demo data populated successfully', type: 'info' },
]

const AdminActivityLogsPage: React.FC = () => {
  const [filterType, setFilterType] = useState<'all' | 'info' | 'success' | 'warning' | 'error'>('all')
  const [searchTerm, setSearchTerm] = useState('')

  const filteredLogs = mockLogs.filter(log => {
    if (filterType !== 'all' && log.type !== filterType) return false
    if (searchTerm) {
      const lower = searchTerm.toLowerCase()
      return log.user.toLowerCase().includes(lower) ||
             log.action.toLowerCase().includes(lower) ||
             log.target.toLowerCase().includes(lower) ||
             log.details.toLowerCase().includes(lower)
    }
    return true
  })

  const formatTime = (iso: string) => {
    const date = new Date(iso)
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(minutes / 60)
    
    if (minutes < 1) return 'Just now'
    if (minutes < 60) return `${minutes}m ago`
    if (hours < 24) return `${hours}h ago`
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
  }

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'success': return '✅'
      case 'warning': return '⚠️'
      case 'error': return '❌'
      default: return 'ℹ️'
    }
  }

  const getTypeClass = (type: string) => {
    switch (type) {
      case 'success': return 'log-type-success'
      case 'warning': return 'log-type-warning'
      case 'error': return 'log-type-error'
      default: return 'log-type-info'
    }
  }

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">
            <span className="admin-page-icon">📋</span>
            Activity Logs
          </h1>
          <p className="admin-page-subtitle">Monitor system activity and user actions</p>
        </div>
      </div>

      {/* Filters */}
      <div className="admin-card" style={{ marginBottom: '1.5rem' }}>
        <div className="admin-card-body" style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: '200px' }}>
            <input
              type="text"
              placeholder="Search logs..."
              className="admin-input"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <select
            className="admin-select"
            value={filterType}
            onChange={(e) => setFilterType(e.target.value as any)}
            style={{ width: 'auto' }}
          >
            <option value="all">All Types</option>
            <option value="info">ℹ️ Info</option>
            <option value="success">✅ Success</option>
            <option value="warning">⚠️ Warning</option>
            <option value="error">❌ Error</option>
          </select>
          <button className="admin-btn admin-btn-secondary">
            Export Logs
          </button>
        </div>
      </div>

      {/* Logs List */}
      <div className="admin-card">
        <div className="admin-card-body" style={{ padding: 0 }}>
          <div className="activity-logs-list">
            {filteredLogs.length === 0 ? (
              <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>📭</div>
                <p>No logs match your filters</p>
              </div>
            ) : (
              filteredLogs.map(log => (
                <div key={log.id} className={`activity-log-item ${getTypeClass(log.type)}`}>
                  <div className="activity-log-icon">{getTypeIcon(log.type)}</div>
                  <div className="activity-log-content">
                    <div className="activity-log-header">
                      <span className="activity-log-action">{log.action}</span>
                      <span className="activity-log-target">→ {log.target}</span>
                    </div>
                    <p className="activity-log-details">{log.details}</p>
                    <div className="activity-log-meta">
                      <span className="activity-log-user">👤 {log.user}</span>
                      <span className="activity-log-time">🕐 {formatTime(log.timestamp)}</span>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <style>{`
        .activity-logs-list {
          max-height: 600px;
          overflow-y: auto;
        }
        .activity-log-item {
          display: flex;
          gap: 1rem;
          padding: 1rem 1.5rem;
          border-bottom: 1px solid var(--border-color, rgba(255,255,255,0.1));
          transition: background-color 0.2s;
        }
        .activity-log-item:hover {
          background-color: rgba(255,255,255,0.03);
        }
        .activity-log-item:last-child {
          border-bottom: none;
        }
        .activity-log-icon {
          font-size: 1.25rem;
          width: 2rem;
          text-align: center;
        }
        .activity-log-content {
          flex: 1;
        }
        .activity-log-header {
          display: flex;
          gap: 0.5rem;
          align-items: center;
          margin-bottom: 0.25rem;
        }
        .activity-log-action {
          font-weight: 600;
          color: var(--text-primary, #fff);
        }
        .activity-log-target {
          color: var(--text-muted, #9ca3af);
          font-size: 0.875rem;
        }
        .activity-log-details {
          color: var(--text-secondary, #d1d5db);
          margin: 0.25rem 0;
          font-size: 0.9rem;
        }
        .activity-log-meta {
          display: flex;
          gap: 1rem;
          font-size: 0.75rem;
          color: var(--text-muted, #9ca3af);
          margin-top: 0.5rem;
        }
        .log-type-success .activity-log-icon { color: #10b981; }
        .log-type-warning .activity-log-icon { color: #f59e0b; }
        .log-type-error .activity-log-icon { color: #ef4444; }
        .log-type-info .activity-log-icon { color: #3b82f6; }
      `}</style>
    </div>
  )
}

export default AdminActivityLogsPage

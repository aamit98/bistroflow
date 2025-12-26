import React from 'react'
import './admin/Admin.css'

const AdminSettingsPage: React.FC = () => {
  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">
            <span className="admin-page-icon">⚙️</span>
            System Settings
          </h1>
          <p className="admin-page-subtitle">Configure system-wide settings and preferences</p>
        </div>
      </div>

      <div className="admin-grid admin-grid-2">
        {/* General Settings */}
        <div className="admin-card">
          <div className="admin-card-header">
            <h3 className="admin-card-title">General Settings</h3>
          </div>
          <div className="admin-card-body">
            <div className="settings-group">
              <label className="settings-label">
                <span>System Name</span>
                <input 
                  type="text" 
                  className="admin-input"
                  defaultValue="BistroFlow"
                  disabled
                />
              </label>
              <label className="settings-label">
                <span>Default Timezone</span>
                <select className="admin-select" defaultValue="Asia/Jerusalem" disabled>
                  <option value="Asia/Jerusalem">Asia/Jerusalem (GMT+2)</option>
                  <option value="Europe/London">Europe/London (GMT)</option>
                  <option value="America/New_York">America/New York (GMT-5)</option>
                </select>
              </label>
              <label className="settings-label">
                <span>Week Starts On</span>
                <select className="admin-select" defaultValue="sunday" disabled>
                  <option value="sunday">Sunday</option>
                  <option value="monday">Monday</option>
                </select>
              </label>
            </div>
            <p className="settings-note">⚠️ Settings are read-only in this demo version</p>
          </div>
        </div>

        {/* Schedule Settings */}
        <div className="admin-card">
          <div className="admin-card-header">
            <h3 className="admin-card-title">Schedule Defaults</h3>
          </div>
          <div className="admin-card-body">
            <div className="settings-group">
              <label className="settings-label">
                <span>Morning Shift Start</span>
                <input 
                  type="time" 
                  className="admin-input"
                  defaultValue="07:00"
                  disabled
                />
              </label>
              <label className="settings-label">
                <span>Morning Shift End</span>
                <input 
                  type="time" 
                  className="admin-input"
                  defaultValue="15:00"
                  disabled
                />
              </label>
              <label className="settings-label">
                <span>Evening Shift Start</span>
                <input 
                  type="time" 
                  className="admin-input"
                  defaultValue="15:00"
                  disabled
                />
              </label>
              <label className="settings-label">
                <span>Evening Shift End</span>
                <input 
                  type="time" 
                  className="admin-input"
                  defaultValue="23:00"
                  disabled
                />
              </label>
            </div>
          </div>
        </div>

        {/* Security Settings */}
        <div className="admin-card">
          <div className="admin-card-header">
            <h3 className="admin-card-title">Security</h3>
          </div>
          <div className="admin-card-body">
            <div className="settings-group">
              <label className="settings-label settings-checkbox">
                <input type="checkbox" defaultChecked disabled />
                <span>Require strong passwords</span>
              </label>
              <label className="settings-label settings-checkbox">
                <input type="checkbox" defaultChecked disabled />
                <span>Enable session timeout (30 min)</span>
              </label>
              <label className="settings-label settings-checkbox">
                <input type="checkbox" disabled />
                <span>Two-factor authentication</span>
              </label>
            </div>
          </div>
        </div>

        {/* Notifications Settings */}
        <div className="admin-card">
          <div className="admin-card-header">
            <h3 className="admin-card-title">Notifications</h3>
          </div>
          <div className="admin-card-body">
            <div className="settings-group">
              <label className="settings-label settings-checkbox">
                <input type="checkbox" defaultChecked disabled />
                <span>Email notifications</span>
              </label>
              <label className="settings-label settings-checkbox">
                <input type="checkbox" defaultChecked disabled />
                <span>Time-off request alerts</span>
              </label>
              <label className="settings-label settings-checkbox">
                <input type="checkbox" defaultChecked disabled />
                <span>Low inventory alerts</span>
              </label>
              <label className="settings-label settings-checkbox">
                <input type="checkbox" disabled />
                <span>SMS notifications</span>
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default AdminSettingsPage

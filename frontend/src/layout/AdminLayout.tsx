import React from 'react'
import { Outlet, NavLink, useNavigate, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../security/AuthContext'
import { IconCommand, IconManager, IconBranch, IconBuilding, IconChart, IconActivity, IconLogout } from '../components/Icons'
import '../pages/admin/Admin.css'

const AdminLayout: React.FC = () => {
  const { employee, logout, isSuperAdmin } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // Only super admins can access this layout
  if (!isSuperAdmin) {
    return <Navigate to="/" replace />
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const navLinks = [
    { path: '/admin', label: 'Command Center', icon: <IconCommand size={18} />, exact: true },
    { path: '/admin/hr-managers', label: 'HR Managers', icon: <IconManager size={18} /> },
    { path: '/admin/restaurants', label: 'Restaurant Chains', icon: <IconBuilding size={18} /> },
    { path: '/admin/branches', label: 'Branches', icon: <IconBranch size={18} /> },
    { path: '/admin/analytics', label: 'Analytics', icon: <IconChart size={18} /> },
  ]

  const isActive = (path: string, exact?: boolean) => {
    if (exact) return location.pathname === path
    return location.pathname.startsWith(path)
  }

  return (
    <div className="admin-layout">
      {/* Premium Sidebar */}
      <aside className="admin-sidebar">
        <div className="admin-sidebar-header">
          <div className="admin-logo">
            <img 
              src="/assets/bistro-flow-logo.svg" 
              alt="BistroFlow" 
              className="admin-logo-icon"
              style={{ width: '44px', height: '44px' }}
            />
            <div className="admin-logo-text">
              <h1>BistroFlow</h1>
              <span>Admin Portal</span>
            </div>
          </div>
        </div>

        <nav className="admin-nav">
          <div className="admin-nav-section">
            <div className="admin-nav-section-title">Main Menu</div>
            {navLinks.map((link) => (
              <NavLink
                key={link.path}
                to={link.path}
                end={link.exact}
                className={`admin-nav-link ${isActive(link.path, link.exact) ? 'active' : ''}`}
              >
                <span className="admin-nav-link-icon">{link.icon}</span>
                {link.label}
              </NavLink>
            ))}
          </div>

          <div className="admin-nav-section">
            <div className="admin-nav-section-title">System</div>
            <NavLink
              to="/admin/logs"
              className={`admin-nav-link ${isActive('/admin/logs') ? 'active' : ''}`}
            >
              <span className="admin-nav-link-icon"><IconActivity size={18} /></span>
              Activity Logs
            </NavLink>
          </div>
        </nav>

        <div className="admin-sidebar-footer">
          <div className="admin-user">
            <div className="admin-user-avatar">
              {employee?.name?.charAt(0)?.toUpperCase() || 'A'}
            </div>
            <div className="admin-user-info">
              <div className="admin-user-name">{employee?.name || 'Super Admin'}</div>
              <div className="admin-user-role">● Super Administrator</div>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="admin-logout-btn-full"
            title="Sign out"
          >
            <IconLogout size={18} />
            <span className="logout-text">Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  )
}

export default AdminLayout

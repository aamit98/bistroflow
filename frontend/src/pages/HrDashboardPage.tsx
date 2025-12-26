// src/pages/HrDashboardPage.tsx
import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useBranch } from '../context/BranchContext'
import { apiClient } from '../api/ApiClient'

interface RoleCoverageData {
  required: number
  assigned: number
  percentage: number
}

interface BackendDashboard {
  branchId: number
  weekStart: string
  weekEnd: string
  employeeCount: number
  hrManagerCount: number
  availabilitySubmitted: number
  assignmentsScheduled: number
  weeklyRequirement: number
  pendingTimeOff: number
  approvedTimeOff: number
  schedulePublished: boolean
  publishedAt: string | null
  shiftsWithIssues: number
  roleCoverage: Record<string, RoleCoverageData>
  lowStockItems: number
  openOrders: number
  todayShiftCount: number
}

interface RoleCoverage {
  role: string
  required: number
  assigned: number
  coverage: number
}

interface BranchDashboard {
  branchId: number
  employeeCount: number
  availabilitySubmitted: number
  pendingTimeOff: number
  shiftsToday: number
  shiftsWithIssues: number
  lowStockItems: number
  openOrders: number
  roleCoverage: RoleCoverage[]
  schedulePublished: boolean
}

function getCurrentSundayISO(): string {
  const today = new Date()
  const day = today.getDay() // 0 = Sunday
  const diff = day // days since Sunday
  const sunday = new Date(today)
  sunday.setDate(today.getDate() - diff)
  return sunday.toISOString().slice(0, 10)
}

const HrDashboardPage: React.FC = () => {
  const { selectedBranchId, branches } = useBranch()
  const [loading, setLoading] = useState(true)
  const [dashboardData, setDashboardData] = useState<BranchDashboard | null>(null)

  useEffect(() => {
    if (!selectedBranchId) {
      setLoading(false)
      return
    }

    const loadDashboard = async () => {
      setLoading(true)
      try {
        const weekStart = getCurrentSundayISO()
        const res = await apiClient.get<BackendDashboard>(
          `/hr/branches/${selectedBranchId}/dashboard`,
          { params: { weekStart } }
        )
        
        // Transform backend response to frontend format
        const backend = res.data
        const roleCoverageArray: RoleCoverage[] = Object.entries(backend.roleCoverage || {}).map(
          ([role, data]) => ({
            role,
            required: data.required,
            assigned: data.assigned,
            coverage: data.percentage
          })
        )

        setDashboardData({
          branchId: backend.branchId,
          employeeCount: backend.employeeCount,
          availabilitySubmitted: backend.availabilitySubmitted,
          pendingTimeOff: backend.pendingTimeOff,
          shiftsToday: backend.todayShiftCount,
          shiftsWithIssues: backend.shiftsWithIssues,
          lowStockItems: backend.lowStockItems,
          openOrders: backend.openOrders,
          roleCoverage: roleCoverageArray,
          schedulePublished: backend.schedulePublished
        })
      } catch (e: unknown) {
        console.error('Failed to load dashboard:', e)
        // Fall back to empty data
        setDashboardData({
          branchId: selectedBranchId ?? 0,
          employeeCount: 0,
          availabilitySubmitted: 0,
          pendingTimeOff: 0,
          shiftsToday: 0,
          shiftsWithIssues: 0,
          lowStockItems: 0,
          openOrders: 0,
          roleCoverage: [],
          schedulePublished: false
        })
      } finally {
        setLoading(false)
      }
    }

    loadDashboard()
  }, [selectedBranchId])

  // Show prompt to select a branch if none selected
  if (!selectedBranchId && !loading) {
    return (
      <div className="bf-empty-state" style={{ padding: '60px 20px', textAlign: 'center' }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>🏢</div>
        <h2 style={{ marginBottom: '12px', color: 'var(--bf-text-primary)' }}>Select a Branch</h2>
        <p style={{ color: 'var(--bf-text-secondary)', marginBottom: '24px' }}>
          {branches.length > 0 
            ? 'Please select a branch from the dropdown above to view the dashboard.'
            : 'No branches available. Please contact an administrator.'}
        </p>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="bf-loading">
        <div className="bf-spinner"></div>
        <p>Loading dashboard...</p>
      </div>
    )
  }

  const data = dashboardData

  return (
    <div>
      {/* Stats Grid */}
      <div className="bf-stats-grid">
        <div className="bf-stat-card" style={{ '--stat-color': '#22c55e' } as React.CSSProperties}>
          <div className="bf-stat-icon">👥</div>
          <p className="bf-stat-value">{data?.employeeCount ?? 0}</p>
          <p className="bf-stat-label">Total Employees</p>
        </div>

        <div className="bf-stat-card" style={{ '--stat-color': '#0ea5e9' } as React.CSSProperties}>
          <div className="bf-stat-icon">📋</div>
          <p className="bf-stat-value">{data?.availabilitySubmitted ?? 0}</p>
          <p className="bf-stat-label">Availability Submitted</p>
        </div>

        <div className="bf-stat-card" style={{ '--stat-color': '#f59e0b' } as React.CSSProperties}>
          <div className="bf-stat-icon">🏖️</div>
          <p className="bf-stat-value">{data?.pendingTimeOff ?? 0}</p>
          <p className="bf-stat-label">Pending Time-Off</p>
        </div>

        <div className="bf-stat-card" style={{ '--stat-color': '#8b5cf6' } as React.CSSProperties}>
          <div className="bf-stat-icon">📅</div>
          <p className="bf-stat-value">{data?.shiftsToday ?? 0}</p>
          <p className="bf-stat-label">Shifts Today</p>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bf-quick-actions">
        <Link to={`/hr/branches/${selectedBranchId}/schedule`} className="bf-quick-action">
          <div className="bf-quick-action-icon">📅</div>
          <div className="bf-quick-action-text">
            <h3>Weekly Schedule</h3>
            <p>Manage shift assignments</p>
          </div>
        </Link>

        <Link to={`/hr/branches/${selectedBranchId}/employees`} className="bf-quick-action">
          <div className="bf-quick-action-icon">👥</div>
          <div className="bf-quick-action-text">
            <h3>Employees</h3>
            <p>View and manage staff</p>
          </div>
        </Link>

        <Link to={`/hr/branches/${selectedBranchId}/time-off`} className="bf-quick-action">
          <div className="bf-quick-action-icon">🏖️</div>
          <div className="bf-quick-action-text">
            <h3>Time-Off Requests</h3>
            <p>{data?.pendingTimeOff || 0} pending requests</p>
          </div>
        </Link>

        <Link to="/hr/branch-settings" className="bf-quick-action">
          <div className="bf-quick-action-icon">⚙️</div>
          <div className="bf-quick-action-text">
            <h3>Branch Settings</h3>
            <p>Configure your branch</p>
          </div>
        </Link>
      </div>

      {/* Main Content Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '20px' }}>
        {/* Role Coverage Card */}
        <div className="bf-card">
          <div className="bf-card-header">
            <div className="bf-card-title">
              <h2>📊 Role Coverage</h2>
              <span className="bf-card-badge">This Week</span>
            </div>
          </div>
          <div className="bf-card-body">
            {data?.roleCoverage && data.roleCoverage.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {data.roleCoverage.map((role) => (
                  <div key={role.role}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <span style={{ fontWeight: 500 }}>{role.role}</span>
                      <span className={`bf-badge ${role.coverage >= 90 ? 'bf-badge-success' : role.coverage >= 70 ? 'bf-badge-warning' : 'bf-badge-danger'}`}>
                        {role.assigned}/{role.required} ({role.coverage}%)
                      </span>
                    </div>
                    <div style={{
                      height: '8px',
                      background: 'rgba(255,255,255,0.08)',
                      borderRadius: '4px',
                      overflow: 'hidden'
                    }}>
                      <div style={{
                        height: '100%',
                        width: `${Math.min(role.coverage, 100)}%`,
                        background: role.coverage >= 90 ? '#22c55e' : role.coverage >= 70 ? '#f59e0b' : '#ef4444',
                        borderRadius: '4px',
                        transition: 'width 0.3s ease'
                      }} />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="bf-empty-state" style={{ padding: '20px' }}>
                <p>No role coverage data available</p>
              </div>
            )}
          </div>
          <div className="bf-card-footer">
            <Link to={`/hr/branches/${selectedBranchId}/schedule`} className="bf-btn bf-btn-primary bf-btn-sm">
              View Full Schedule →
            </Link>
          </div>
        </div>

        {/* Alerts Card */}
        <div className="bf-card">
          <div className="bf-card-header">
            <div className="bf-card-title">
              <h2>⚠️ Alerts & Issues</h2>
            </div>
          </div>
          <div className="bf-card-body">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {(data?.pendingTimeOff ?? 0) > 0 && (
                <div style={{
                  padding: '14px 16px',
                  background: 'rgba(245, 158, 11, 0.1)',
                  borderRadius: '10px',
                  border: '1px solid rgba(245, 158, 11, 0.2)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}>
                  <span>📋 {data?.pendingTimeOff} pending time-off request(s)</span>
                  <Link to={`/hr/branches/${selectedBranchId}/time-off`} className="bf-btn bf-btn-sm bf-btn-secondary">
                    Review
                  </Link>
                </div>
              )}

              {(data?.shiftsWithIssues ?? 0) > 0 && (
                <div style={{
                  padding: '14px 16px',
                  background: 'rgba(239, 68, 68, 0.1)',
                  borderRadius: '10px',
                  border: '1px solid rgba(239, 68, 68, 0.2)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}>
                  <span>🚨 {data?.shiftsWithIssues} shift(s) with coverage issues</span>
                  <Link to={`/hr/branches/${selectedBranchId}/schedule`} className="bf-btn bf-btn-sm bf-btn-secondary">
                    Fix
                  </Link>
                </div>
              )}

              {(data?.lowStockItems ?? 0) > 0 && (
                <div style={{
                  padding: '14px 16px',
                  background: 'rgba(234, 179, 8, 0.1)',
                  borderRadius: '10px',
                  border: '1px solid rgba(234, 179, 8, 0.2)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}>
                  <span>📦 {data?.lowStockItems} low-stock item(s)</span>
                  <button className="bf-btn bf-btn-sm bf-btn-secondary" disabled>View</button>
                </div>
              )}

              {(data?.pendingTimeOff ?? 0) === 0 && 
               (data?.shiftsWithIssues ?? 0) === 0 && 
               (data?.lowStockItems ?? 0) === 0 && (
                <div style={{
                  padding: '32px',
                  textAlign: 'center',
                  color: 'var(--bf-success)'
                }}>
                  <div style={{ fontSize: '32px', marginBottom: '8px' }}>✅</div>
                  <p style={{ fontWeight: 600 }}>All clear! No issues to address.</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Inventory Status Card */}
        <div className="bf-card">
          <div className="bf-card-header">
            <div className="bf-card-title">
              <h2>📦 Inventory Status</h2>
            </div>
          </div>
          <div className="bf-card-body">
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '20px' }}>
              <div style={{
                padding: '20px',
                background: 'var(--bf-surface-light)',
                borderRadius: '12px',
                textAlign: 'center'
              }}>
                <div style={{ fontSize: '28px', fontWeight: 700, color: '#f59e0b' }}>
                  {data?.lowStockItems ?? 0}
                </div>
                <div style={{ fontSize: '13px', color: 'var(--bf-text-secondary)', marginTop: '4px' }}>Low Stock Items</div>
              </div>
              <div style={{
                padding: '20px',
                background: 'var(--bf-surface-light)',
                borderRadius: '12px',
                textAlign: 'center'
              }}>
                <div style={{ fontSize: '28px', fontWeight: 700, color: '#0ea5e9' }}>
                  {data?.openOrders ?? 0}
                </div>
                <div style={{ fontSize: '13px', color: 'var(--bf-text-secondary)', marginTop: '4px' }}>Open Orders</div>
              </div>
            </div>
            <Link to={`/hr/branches/${selectedBranchId}/inventory`} className="bf-btn bf-btn-secondary" style={{ width: '100%' }}>
              Open Inventory Module
            </Link>
          </div>
        </div>

        {/* Schedule Status Card */}
        <div className="bf-card">
          <div className="bf-card-header">
            <div className="bf-card-title">
              <h2>📆 Schedule Status</h2>
            </div>
          </div>
          <div className="bf-card-body">
            <div style={{
              padding: '24px',
              textAlign: 'center',
              background: data?.schedulePublished ? 'rgba(34, 197, 94, 0.1)' : 'rgba(245, 158, 11, 0.1)',
              borderRadius: '12px',
              border: `1px solid ${data?.schedulePublished ? 'rgba(34, 197, 94, 0.2)' : 'rgba(245, 158, 11, 0.2)'}`
            }}>
              <div style={{ fontSize: '40px', marginBottom: '12px' }}>
                {data?.schedulePublished ? '✅' : '⏳'}
              </div>
              <div style={{ 
                fontWeight: 600, 
                fontSize: '16px',
                color: data?.schedulePublished ? '#22c55e' : '#f59e0b'
              }}>
                {data?.schedulePublished ? 'Schedule Published' : 'Schedule Draft'}
              </div>
              <p style={{ color: 'var(--bf-text-secondary)', fontSize: '13px', marginTop: '8px' }}>
                {data?.schedulePublished 
                  ? 'Employees can view their shifts'
                  : 'Review and publish when ready'}
              </p>
            </div>
          </div>
          <div className="bf-card-footer">
            <Link to={`/hr/branches/${selectedBranchId}/schedule`} className="bf-btn bf-btn-primary bf-btn-sm">
              {data?.schedulePublished ? 'View Schedule' : 'Complete & Publish'}
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default HrDashboardPage

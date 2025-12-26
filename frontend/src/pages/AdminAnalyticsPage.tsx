import React, { useEffect, useState, useMemo } from 'react'
import {
  getAllBranchesApi,
  getAllHrManagersApi,
  getAllRestaurantsApi,
  getRestaurantBranchesApi,
  type BranchSummary,
  type HrManager,
  type Restaurant,
} from '../api/AdminApi'
import {
  IconChart,
  IconBuilding,
  IconBranch,
  IconUsers,
  IconManager,
} from '../components/Icons'
import './admin/Admin.css'

// Fake historical data generator for demo purposes
function generateTrendData(baseValue: number, months: number = 6): number[] {
  const data: number[] = []
  let current = baseValue * 0.7
  for (let i = 0; i < months; i++) {
    current = current + (Math.random() * 0.15 * baseValue) - (Math.random() * 0.05 * baseValue)
    data.push(Math.round(current))
  }
  data.push(baseValue) // Current value is last
  return data
}

const AdminAnalyticsPage: React.FC = () => {
  const [branches, setBranches] = useState<BranchSummary[]>([])
  const [restaurants, setRestaurants] = useState<Restaurant[]>([])
  const [hrManagers, setHrManagers] = useState<HrManager[]>([])
  const [branchesByRestaurant, setBranchesByRestaurant] = useState<Record<number, BranchSummary[]>>({})
  const [loading, setLoading] = useState(true)
  const [selectedTimeframe, setSelectedTimeframe] = useState<'7d' | '30d' | '90d' | '1y'>('30d')

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    try {
      const [branchRes, hrRes, restaurantRes] = await Promise.all([
        getAllBranchesApi(),
        getAllHrManagersApi(),
        getAllRestaurantsApi(),
      ])
      setBranches(branchRes.data)
      setHrManagers(hrRes.data)
      setRestaurants(restaurantRes.data)

      // Load branches for each restaurant
      const branchesMap: Record<number, BranchSummary[]> = {}
      await Promise.all(
        restaurantRes.data.map(async (restaurant: Restaurant) => {
          try {
            const branchRes = await getRestaurantBranchesApi(restaurant.id)
            branchesMap[restaurant.id] = branchRes.data
          } catch {
            branchesMap[restaurant.id] = []
          }
        })
      )
      setBranchesByRestaurant(branchesMap)
    } catch (err) {
      console.error('Failed to load analytics:', err)
    } finally {
      setLoading(false)
    }
  }

  const analytics = useMemo(() => {
    const totalEmployees = branches.reduce((sum, b) => sum + b.employeeCount, 0)
    const avgEmployeesPerBranch = branches.length > 0 
      ? Math.round(totalEmployees / branches.length) 
      : 0
    
    const branchesWithRestaurant = branches.filter(b => b.restaurantName).length
    const restaurantCoverage = branches.length > 0 
      ? Math.round((branchesWithRestaurant / branches.length) * 100) 
      : 0

    const sortedByEmployees = [...branches].sort((a, b) => b.employeeCount - a.employeeCount)
    const topBranches = sortedByEmployees.slice(0, 5)
    const bottomBranches = sortedByEmployees.slice(-3).reverse()

    // Restaurant chain metrics
    const chainMetrics = restaurants.map(r => {
      const chainBranches = branchesByRestaurant[r.id] || []
      const chainEmployees = chainBranches.reduce((sum, b) => sum + b.employeeCount, 0)
      return {
        ...r,
        branches: chainBranches,
        totalEmployees: chainEmployees,
        avgEmployeesPerBranch: chainBranches.length > 0 
          ? Math.round(chainEmployees / chainBranches.length) 
          : 0,
        activeBranches: chainBranches.filter(b => b.active).length,
      }
    })

    // Simulated trends (in real app, this would come from backend)
    const employeeTrend = generateTrendData(totalEmployees)
    const branchTrend = generateTrendData(branches.length)
    const employeeGrowth = totalEmployees > 0 && employeeTrend[0] > 0
      ? Math.round(((totalEmployees - employeeTrend[0]) / employeeTrend[0]) * 100)
      : 0
    const branchGrowth = branches.length > 0 && branchTrend[0] > 0
      ? Math.round(((branches.length - branchTrend[0]) / branchTrend[0]) * 100)
      : 0

    return {
      totalRestaurants: restaurants.length,
      activeRestaurants: restaurants.filter(r => r.active).length,
      totalBranches: branches.length,
      activeBranches: branches.filter(b => b.active).length,
      totalHrManagers: hrManagers.length,
      totalEmployees,
      avgEmployeesPerBranch,
      restaurantCoverage,
      unassignedManagers: hrManagers.filter(hr => !hr.restaurantId).length,
      topBranches,
      bottomBranches,
      chainMetrics,
      employeeTrend,
      branchTrend,
      employeeGrowth,
      branchGrowth,
    }
  }, [branches, hrManagers, restaurants, branchesByRestaurant])

  // Simple bar chart renderer
  const renderMiniChart = (data: number[], color: string) => {
    const max = Math.max(...data)
    return (
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: 40 }}>
        {data.map((value, i) => (
          <div
            key={i}
            style={{
              width: 8,
              height: `${(value / max) * 100}%`,
              background: i === data.length - 1 ? color : `${color}66`,
              borderRadius: 2,
              transition: 'height 0.3s ease',
            }}
          />
        ))}
      </div>
    )
  }

  if (loading) {
    return (
      <div className="bf-loading-container">
        <div className="bf-spinner" />
        <p className="bf-loading-text">Loading analytics...</p>
      </div>
    )
  }

  return (
    <div className="bf-page">
      {/* Header */}
      <div className="bf-page-header">
        <div>
          <h1 className="bf-page-title">
            <IconChart size={28} /> Analytics & Insights
          </h1>
          <p className="bf-page-subtitle">System-wide performance metrics and business intelligence</p>
        </div>
        <div className="bf-header-actions">
          <select
            className="bf-select"
            value={selectedTimeframe}
            onChange={(e) => setSelectedTimeframe(e.target.value as typeof selectedTimeframe)}
            style={{ width: 'auto', minWidth: 140 }}
          >
            <option value="7d">Last 7 days</option>
            <option value="30d">Last 30 days</option>
            <option value="90d">Last 90 days</option>
            <option value="1y">Last year</option>
          </select>
        </div>
      </div>

      {/* Key Performance Indicators */}
      <div className="bf-stats-grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
        <div className="bf-stat-card bf-stat-primary">
          <div className="bf-stat-header">
            <div className="bf-stat-icon"><IconBuilding size={24} /></div>
            {analytics.employeeGrowth !== 0 && (
              <span className={`bf-trend ${analytics.employeeGrowth > 0 ? 'bf-trend-up' : 'bf-trend-down'}`}>
                {analytics.employeeGrowth > 0 ? '↗' : '↘'}
                {Math.abs(analytics.employeeGrowth)}%
              </span>
            )}
          </div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{analytics.totalRestaurants}</p>
            <p className="bf-stat-label">Restaurant Chains</p>
          </div>
          <div className="bf-stat-chart">
            {renderMiniChart(analytics.branchTrend, 'var(--bf-primary)')}
          </div>
        </div>

        <div className="bf-stat-card bf-stat-secondary">
          <div className="bf-stat-header">
            <div className="bf-stat-icon"><IconBranch size={24} /></div>
            {analytics.branchGrowth !== 0 && (
              <span className={`bf-trend ${analytics.branchGrowth > 0 ? 'bf-trend-up' : 'bf-trend-down'}`}>
                {analytics.branchGrowth > 0 ? '↗' : '↘'}
                {Math.abs(analytics.branchGrowth)}%
              </span>
            )}
          </div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{analytics.totalBranches}</p>
            <p className="bf-stat-label">Total Branches</p>
          </div>
          <div className="bf-stat-footer">
            <span className="bf-stat-subtext">{analytics.activeBranches} active</span>
          </div>
        </div>

        <div className="bf-stat-card bf-stat-accent">
          <div className="bf-stat-header">
            <div className="bf-stat-icon"><IconUsers size={24} /></div>
            {analytics.employeeGrowth !== 0 && (
              <span className={`bf-trend ${analytics.employeeGrowth > 0 ? 'bf-trend-up' : 'bf-trend-down'}`}>
                {analytics.employeeGrowth > 0 ? '↗' : '↘'}
                {Math.abs(analytics.employeeGrowth)}%
              </span>
            )}
          </div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{analytics.totalEmployees}</p>
            <p className="bf-stat-label">Total Workforce</p>
          </div>
          <div className="bf-stat-chart">
            {renderMiniChart(analytics.employeeTrend, 'var(--bf-accent)')}
          </div>
        </div>

        <div className="bf-stat-card bf-stat-success">
          <div className="bf-stat-icon"><IconManager size={24} /></div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{analytics.avgEmployeesPerBranch}</p>
            <p className="bf-stat-label">Avg Staff/Branch</p>
          </div>
          <div className="bf-stat-footer">
            <span className="bf-stat-subtext">{analytics.totalHrManagers} HR managers</span>
          </div>
        </div>
      </div>

      {/* Restaurant Chain Performance */}
      <div className="bf-card">
        <div className="bf-card-header">
          <div className="bf-card-title-group">
            <h2 className="bf-card-title">📊 Restaurant Chain Performance</h2>
            <span className="bf-badge bf-badge-info">{restaurants.length} chains</span>
          </div>
        </div>
        <div className="bf-table-wrapper">
          <table className="bf-table">
            <thead>
              <tr>
                <th>Restaurant Chain</th>
                <th>Status</th>
                <th>Branches</th>
                <th>Total Staff</th>
                <th>Avg/Branch</th>
                <th>HR Manager</th>
                <th>Performance</th>
              </tr>
            </thead>
            <tbody>
              {analytics.chainMetrics.map((chain) => {
                // Calculate performance score (0-100)
                const hasHr = chain.hrManagerId ? 25 : 0
                const branchScore = Math.min(chain.branches.length * 10, 25)
                const staffScore = Math.min(chain.totalEmployees * 2, 25)
                const activeScore = chain.active ? 25 : 0
                const performanceScore = hasHr + branchScore + staffScore + activeScore
                const performanceColor = performanceScore >= 75 ? 'var(--bf-success)' 
                  : performanceScore >= 50 ? 'var(--bf-warning)' 
                  : 'var(--bf-danger)'

                return (
                  <tr key={chain.id}>
                    <td>
                      <div className="bf-table-user">
                        <div className="bf-avatar bf-avatar-primary">
                          <IconBuilding size={16} />
                        </div>
                        <div>
                          <span className="bf-table-name">{chain.name}</span>
                          {chain.businessId && (
                            <span className="bf-table-meta">{chain.businessId}</span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className={`bf-badge ${chain.active ? 'bf-badge-success' : 'bf-badge-danger'}`}>
                        {chain.active ? '● Active' : '○ Inactive'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ fontWeight: 600 }}>{chain.branches.length}</span>
                        <span className="bf-table-muted">({chain.activeBranches} active)</span>
                      </div>
                    </td>
                    <td>
                      <span style={{ fontWeight: 600 }}>{chain.totalEmployees}</span>
                    </td>
                    <td>
                      <span className="bf-badge bf-badge-info">{chain.avgEmployeesPerBranch}</span>
                    </td>
                    <td>
                      {chain.hrManagerName ? (
                        <span className="bf-badge bf-badge-success">
                          <IconManager size={12} /> {chain.hrManagerName}
                        </span>
                      ) : (
                        <span className="bf-badge bf-badge-warning">⚠️ None</span>
                      )}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <div style={{ 
                          width: 60, 
                          height: 8, 
                          background: 'var(--bf-surface-hover)', 
                          borderRadius: 4,
                          overflow: 'hidden'
                        }}>
                          <div style={{ 
                            width: `${performanceScore}%`, 
                            height: '100%', 
                            background: performanceColor,
                            borderRadius: 4,
                            transition: 'width 0.3s ease'
                          }} />
                        </div>
                        <span style={{ fontSize: 12, fontWeight: 600, color: performanceColor }}>
                          {performanceScore}%
                        </span>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Two-column layout for detailed analytics */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        {/* Organization Health */}
        <div className="bf-card">
          <div className="bf-card-header">
            <h2 className="bf-card-title">🏥 Organization Health</h2>
          </div>
          <div style={{ padding: 24 }}>
            {/* Health Metrics */}
            <div style={{ marginBottom: 24 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span style={{ color: 'var(--bf-text-muted)' }}>Branch Assignment Rate</span>
                <span style={{ fontWeight: 600 }}>{analytics.restaurantCoverage}%</span>
              </div>
              <div style={{ 
                height: 10, 
                background: 'var(--bf-surface-hover)', 
                borderRadius: 5,
                overflow: 'hidden'
              }}>
                <div style={{ 
                  width: `${analytics.restaurantCoverage}%`, 
                  height: '100%', 
                  background: analytics.restaurantCoverage === 100 ? 'var(--bf-success)' : 'var(--bf-warning)',
                  borderRadius: 5,
                  transition: 'width 0.5s ease'
                }} />
              </div>
            </div>

            <div style={{ marginBottom: 24 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span style={{ color: 'var(--bf-text-muted)' }}>Active Branches</span>
                <span style={{ fontWeight: 600 }}>
                  {analytics.totalBranches > 0 
                    ? Math.round((analytics.activeBranches / analytics.totalBranches) * 100)
                    : 0}%
                </span>
              </div>
              <div style={{ 
                height: 10, 
                background: 'var(--bf-surface-hover)', 
                borderRadius: 5,
                overflow: 'hidden'
              }}>
                <div style={{ 
                  width: `${analytics.totalBranches > 0 ? (analytics.activeBranches / analytics.totalBranches) * 100 : 0}%`, 
                  height: '100%', 
                  background: 'var(--bf-success)',
                  borderRadius: 5,
                  transition: 'width 0.5s ease'
                }} />
              </div>
            </div>

            <div style={{ marginBottom: 24 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span style={{ color: 'var(--bf-text-muted)' }}>HR Manager Coverage</span>
                <span style={{ fontWeight: 600 }}>
                  {analytics.totalHrManagers > 0 
                    ? Math.round(((analytics.totalHrManagers - analytics.unassignedManagers) / analytics.totalHrManagers) * 100)
                    : 0}%
                </span>
              </div>
              <div style={{ 
                height: 10, 
                background: 'var(--bf-surface-hover)', 
                borderRadius: 5,
                overflow: 'hidden'
              }}>
                <div style={{ 
                  width: `${analytics.totalHrManagers > 0 ? ((analytics.totalHrManagers - analytics.unassignedManagers) / analytics.totalHrManagers) * 100 : 0}%`, 
                  height: '100%', 
                  background: analytics.unassignedManagers === 0 ? 'var(--bf-success)' : 'var(--bf-warning)',
                  borderRadius: 5,
                  transition: 'width 0.5s ease'
                }} />
              </div>
            </div>

            {/* Summary Cards */}
            <div style={{ 
              display: 'grid', 
              gridTemplateColumns: 'repeat(2, 1fr)', 
              gap: 16,
              marginTop: 24,
              padding: 16,
              background: 'var(--bf-surface-hover)',
              borderRadius: 12
            }}>
              <div style={{ textAlign: 'center' }}>
                <p style={{ margin: 0, fontSize: 28, fontWeight: 700, color: 'var(--bf-primary)' }}>
                  {analytics.totalHrManagers}
                </p>
                <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--bf-text-muted)' }}>HR Managers</p>
              </div>
              <div style={{ textAlign: 'center' }}>
                <p style={{ 
                  margin: 0, 
                  fontSize: 28, 
                  fontWeight: 700, 
                  color: analytics.unassignedManagers > 0 ? 'var(--bf-warning)' : 'var(--bf-success)' 
                }}>
                  {analytics.unassignedManagers}
                </p>
                <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--bf-text-muted)' }}>Unassigned</p>
              </div>
            </div>
          </div>
        </div>

        {/* Top Performing Branches */}
        <div className="bf-card">
          <div className="bf-card-header">
            <h2 className="bf-card-title">🏆 Branch Rankings</h2>
          </div>
          <div style={{ padding: 24 }}>
            <h4 style={{ margin: '0 0 16px', color: 'var(--bf-text-muted)', fontSize: 12, textTransform: 'uppercase', letterSpacing: 1 }}>
              Top Branches by Staff Size
            </h4>
            {analytics.topBranches.map((branch, idx) => (
              <div
                key={branch.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  padding: '12px 16px',
                  background: idx === 0 ? 'rgba(16, 185, 129, 0.1)' : 'var(--bf-surface-hover)',
                  borderRadius: 10,
                  marginBottom: 10,
                  border: idx === 0 ? '1px solid rgba(16, 185, 129, 0.3)' : '1px solid transparent',
                }}
              >
                <span style={{ 
                  fontSize: 20, 
                  width: 32, 
                  textAlign: 'center' 
                }}>
                  {idx === 0 ? '🥇' : idx === 1 ? '🥈' : idx === 2 ? '🥉' : `#${idx + 1}`}
                </span>
                <div style={{ flex: 1 }}>
                  <p style={{ margin: 0, fontWeight: 600 }}>{branch.name}</p>
                  <p style={{ margin: '2px 0 0', fontSize: 12, color: 'var(--bf-text-muted)' }}>
                    {branch.restaurantName || 'Unassigned'} • {branch.city}
                  </p>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <p style={{ margin: 0, fontSize: 20, fontWeight: 700, color: 'var(--bf-primary)' }}>
                    {branch.employeeCount}
                  </p>
                  <p style={{ margin: 0, fontSize: 11, color: 'var(--bf-text-muted)' }}>staff</p>
                </div>
              </div>
            ))}

            {analytics.bottomBranches.length > 0 && analytics.bottomBranches[0] !== analytics.topBranches[analytics.topBranches.length - 1] && (
              <>
                <h4 style={{ margin: '24px 0 16px', color: 'var(--bf-text-muted)', fontSize: 12, textTransform: 'uppercase', letterSpacing: 1 }}>
                  Needs Attention
                </h4>
                {analytics.bottomBranches.map((branch) => (
                  <div
                    key={branch.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 12,
                      padding: '12px 16px',
                      background: 'rgba(239, 68, 68, 0.05)',
                      borderRadius: 10,
                      marginBottom: 10,
                      border: '1px solid rgba(239, 68, 68, 0.2)',
                    }}
                  >
                    <span style={{ fontSize: 20 }}>⚠️</span>
                    <div style={{ flex: 1 }}>
                      <p style={{ margin: 0, fontWeight: 600 }}>{branch.name}</p>
                      <p style={{ margin: '2px 0 0', fontSize: 12, color: 'var(--bf-text-muted)' }}>
                        {branch.restaurantName || 'Unassigned'} • {branch.city}
                      </p>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <p style={{ margin: 0, fontSize: 20, fontWeight: 700, color: 'var(--bf-warning)' }}>
                        {branch.employeeCount}
                      </p>
                      <p style={{ margin: 0, fontSize: 11, color: 'var(--bf-text-muted)' }}>staff</p>
                    </div>
                  </div>
                ))}
              </>
            )}
          </div>
        </div>
      </div>

      {/* Staff Distribution Chart */}
      <div className="bf-card">
        <div className="bf-card-header">
          <h2 className="bf-card-title">📈 Staff Distribution by Branch</h2>
        </div>
        <div style={{ padding: 24 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {branches.slice(0, 10).map((branch, idx) => {
              const percentage = analytics.totalEmployees > 0 
                ? (branch.employeeCount / analytics.totalEmployees) * 100 
                : 0
              const colors = [
                'var(--bf-primary)', 'var(--bf-secondary)', 'var(--bf-accent)', 
                'var(--bf-success)', '#f59e0b', '#ec4899', '#8b5cf6',
                '#06b6d4', '#84cc16', '#f97316'
              ]
              return (
                <div key={branch.id}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{ fontWeight: 500 }}>{branch.name}</span>
                      {branch.restaurantName && (
                        <span className="bf-badge bf-badge-sm bf-badge-info">{branch.restaurantName}</span>
                      )}
                    </div>
                    <span style={{ color: 'var(--bf-text-muted)', fontSize: 13 }}>
                      {branch.employeeCount} ({percentage.toFixed(1)}%)
                    </span>
                  </div>
                  <div style={{ 
                    height: 24, 
                    background: 'var(--bf-surface-hover)', 
                    borderRadius: 6,
                    overflow: 'hidden',
                    display: 'flex',
                    alignItems: 'center',
                  }}>
                    <div style={{ 
                      width: `${percentage}%`, 
                      minWidth: percentage > 0 ? 4 : 0,
                      height: '100%', 
                      background: colors[idx % colors.length],
                      borderRadius: 6,
                      transition: 'width 0.5s ease',
                      display: 'flex',
                      alignItems: 'center',
                      paddingLeft: percentage > 15 ? 10 : 0,
                    }}>
                      {percentage > 15 && (
                        <span style={{ color: 'white', fontSize: 12, fontWeight: 600 }}>
                          {branch.employeeCount}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </div>
  )
}

export default AdminAnalyticsPage

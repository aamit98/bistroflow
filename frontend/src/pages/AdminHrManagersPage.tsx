import React, { useEffect, useState, useMemo } from 'react'
import {
  getAllHrManagersApi,
  getAllRestaurantsApi,
  createHrManagerApi,
  updateHrManagerRestaurantApi,
  removeHrManagerApi,
  type HrManager,
  type Restaurant,
} from '../api/AdminApi'
import { useToast } from '../components/ToastContext'
import './admin/Admin.css'

const AdminHrManagersPage: React.FC = () => {
  const { addToast } = useToast()
  const [hrManagers, setHrManagers] = useState<HrManager[]>([])
  const [restaurants, setRestaurants] = useState<Restaurant[]>([])
  const [loading, setLoading] = useState(true)
  const [showAddModal, setShowAddModal] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterRestaurant, setFilterRestaurant] = useState<string>('all')

  // Form state
  const [formData, setFormData] = useState({
    id: '',
    name: '',
    password: '',
    restaurantId: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [actionLoading, setActionLoading] = useState<number | null>(null)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    try {
      const [hrRes, restRes] = await Promise.all([
        getAllHrManagersApi(),
        getAllRestaurantsApi(),
      ])
      setHrManagers(hrRes.data)
      setRestaurants(restRes.data)
    } catch (err: any) {
      console.error('Failed to load data:', err)
      addToast('Failed to load HR managers', 'error')
    } finally {
      setLoading(false)
    }
  }

  const filteredManagers = useMemo(() => {
    let result = hrManagers

    // Filter by search term
    if (searchTerm) {
      const lower = searchTerm.toLowerCase()
      result = result.filter(
        hr => hr.name.toLowerCase().includes(lower) ||
              hr.id.toString().includes(lower) ||
              (hr.restaurantName && hr.restaurantName.toLowerCase().includes(lower))
      )
    }

    // Filter by restaurant
    if (filterRestaurant !== 'all') {
      if (filterRestaurant === 'unassigned') {
        result = result.filter(hr => !hr.restaurantId)
      } else {
        result = result.filter(hr => hr.restaurantId === parseInt(filterRestaurant))
      }
    }

    return result
  }, [hrManagers, searchTerm, filterRestaurant])

  const handleAddHrManager = async (e: React.FormEvent) => {
    e.preventDefault()
    setSubmitting(true)

    try {
      await createHrManagerApi({
        id: parseInt(formData.id),
        name: formData.name,
        password: formData.password,
        restaurantId: formData.restaurantId ? parseInt(formData.restaurantId) : undefined,
      })
      addToast('HR Manager created successfully!', 'success')
      setShowAddModal(false)
      setFormData({ id: '', name: '', password: '', restaurantId: '' })
      loadData()
    } catch (err: any) {
      console.error('Failed to create HR manager:', err)
      addToast(err.response?.data?.error || 'Failed to create HR manager', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  const handleChangeRestaurant = async (hrId: number, newRestaurantId: number | null) => {
    setActionLoading(hrId)
    try {
      await updateHrManagerRestaurantApi(hrId, newRestaurantId)
      addToast('Restaurant assignment updated!', 'success')
      loadData()
    } catch (err: any) {
      console.error('Failed to update restaurant:', err)
      addToast('Failed to update restaurant assignment', 'error')
    } finally {
      setActionLoading(null)
    }
  }

  const handleRemoveHrManager = async (hrId: number, name: string) => {
    if (!confirm(`Are you sure you want to remove ${name} as HR Manager? They will be demoted to a regular employee.`)) {
      return
    }

    setActionLoading(hrId)
    try {
      await removeHrManagerApi(hrId)
      addToast('HR Manager removed', 'success')
      loadData()
    } catch (err: any) {
      console.error('Failed to remove HR manager:', err)
      addToast('Failed to remove HR manager', 'error')
    } finally {
      setActionLoading(null)
    }
  }

  if (loading) {
    return (
      <div className="admin-loading">
        <div className="admin-spinner" />
        <p style={{ color: 'var(--admin-text-muted)', marginTop: 16 }}>Loading HR managers...</p>
      </div>
    )
  }

  return (
    <>
      {/* Header */}
      <div className="admin-header">
        <div className="admin-header-content">
          <div>
            <h1>👔 HR Managers</h1>
            <p>Complete control over all HR managers across your restaurant chains</p>
          </div>
          <div className="admin-header-actions">
            <button 
              className="admin-btn admin-btn-primary"
              onClick={() => setShowAddModal(true)}
            >
              ➕ Add HR Manager
            </button>
          </div>
        </div>
      </div>

      <div className="admin-content">
        {/* Stats Row */}
        <div className="admin-stats-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
          <div className="admin-stat-card" style={{ '--stat-color': '#6c5ce7' } as React.CSSProperties}>
            <div className="admin-stat-card-icon">👔</div>
            <p className="admin-stat-card-value">{hrManagers.length}</p>
            <p className="admin-stat-card-label">Total HR Managers</p>
          </div>
          <div className="admin-stat-card" style={{ '--stat-color': '#00b894' } as React.CSSProperties}>
            <div className="admin-stat-card-icon">✅</div>
            <p className="admin-stat-card-value">{hrManagers.filter(hr => hr.restaurantId).length}</p>
            <p className="admin-stat-card-label">Assigned to Restaurants</p>
          </div>
          <div className="admin-stat-card" style={{ '--stat-color': hrManagers.filter(hr => !hr.restaurantId).length > 0 ? '#fdcb6e' : '#00b894' } as React.CSSProperties}>
            <div className="admin-stat-card-icon">{hrManagers.filter(hr => !hr.restaurantId).length > 0 ? '⚠️' : '✓'}</div>
            <p className="admin-stat-card-value">{hrManagers.filter(hr => !hr.restaurantId).length}</p>
            <p className="admin-stat-card-label">Unassigned</p>
          </div>
        </div>

        {/* Main Table */}
        <div className="admin-table-container">
          <div className="admin-table-header">
            <div className="admin-table-title">
              <h2>All HR Managers</h2>
              <span className="admin-table-title-badge">{filteredManagers.length} of {hrManagers.length}</span>
            </div>
            <div className="admin-table-actions">
              <div className="admin-table-search">
                <span>🔍</span>
                <input
                  type="text"
                  placeholder="Search by name, ID..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <select
                className="admin-form-select"
                value={filterRestaurant}
                onChange={(e) => setFilterRestaurant(e.target.value)}
                style={{ width: 'auto', minWidth: 200 }}
              >
                <option value="all">All Restaurant Chains</option>
                <option value="unassigned">⚠️ Unassigned Only</option>
                {restaurants.map(r => (
                  <option key={r.id} value={r.id}>{r.name}</option>
                ))}
              </select>
            </div>
          </div>

          {filteredManagers.length === 0 ? (
            <div className="admin-empty-state">
              <div className="admin-empty-state-icon">👔</div>
              <h3>{searchTerm || filterRestaurant !== 'all' ? 'No matching managers' : 'No HR Managers'}</h3>
              <p>{searchTerm || filterRestaurant !== 'all' ? 'Try adjusting your filters' : 'Create your first HR manager to get started'}</p>
              {!searchTerm && filterRestaurant === 'all' && (
                <button 
                  className="admin-btn admin-btn-primary"
                  onClick={() => setShowAddModal(true)}
                >
                  ➕ Add First HR Manager
                </button>
              )}
            </div>
          ) : (
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Employee ID</th>
                  <th>Name</th>
                  <th>Restaurant Chain</th>
                  <th>Branches</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredManagers.map((hr) => (
                  <tr key={hr.id}>
                    <td>
                      <code style={{ 
                        background: 'var(--admin-surface-light)', 
                        padding: '4px 10px', 
                        borderRadius: 6,
                        fontSize: 13,
                        fontFamily: 'monospace'
                      }}>
                        #{hr.id}
                      </code>
                    </td>
                    <td>
                      <div className="admin-table-cell-main">{hr.name}</div>
                      {hr.email && (
                        <div className="admin-table-cell-sub">{hr.email}</div>
                      )}
                    </td>
                    <td>
                      <select
                        value={hr.restaurantId || ''}
                        onChange={(e) => handleChangeRestaurant(hr.id, e.target.value ? parseInt(e.target.value) : null)}
                        disabled={actionLoading === hr.id}
                        className="admin-form-select"
                        style={{ width: 'auto', minWidth: 220 }}
                      >
                        <option value="">-- Not Assigned --</option>
                        {restaurants.map(r => (
                          <option key={r.id} value={r.id}>
                            {r.name} {r.hrManagerName && r.hrManagerName !== hr.name ? `(${r.hrManagerName})` : ''}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <span className="admin-badge info">
                        🏪 {hr.branchCount} branches
                      </span>
                    </td>
                    <td>
                      {hr.restaurantId ? (
                        <span className="admin-badge success">
                          ✓ Active
                        </span>
                      ) : (
                        <span className="admin-badge warning">
                          ⚠ Unassigned
                        </span>
                      )}
                    </td>
                    <td>
                      <div className="admin-actions">
                        <button
                          className="admin-btn admin-btn-danger admin-btn-sm"
                          onClick={() => handleRemoveHrManager(hr.id, hr.name)}
                          disabled={actionLoading === hr.id}
                          title="Remove HR Role"
                        >
                          {actionLoading === hr.id ? '...' : '🗑️ Remove'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Add HR Manager Modal */}
      {showAddModal && (
        <div className="admin-modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="admin-modal" onClick={e => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2>➕ Add New HR Manager</h2>
              <button className="admin-modal-close" onClick={() => setShowAddModal(false)}>×</button>
            </div>
            <form onSubmit={handleAddHrManager}>
              <div className="admin-modal-body">
                <div className="admin-form-group">
                  <label className="admin-form-label">Employee ID (9 digits)</label>
                  <input
                    type="text"
                    className="admin-form-input"
                    placeholder="e.g., 123456789"
                    pattern="\d{9}"
                    value={formData.id}
                    onChange={(e) => setFormData({...formData, id: e.target.value})}
                    required
                  />
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Full Name</label>
                  <input
                    type="text"
                    className="admin-form-input"
                    placeholder="e.g., John Smith"
                    value={formData.name}
                    onChange={(e) => setFormData({...formData, name: e.target.value})}
                    required
                  />
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Password</label>
                  <input
                    type="password"
                    className="admin-form-input"
                    placeholder="Set a secure password"
                    minLength={6}
                    value={formData.password}
                    onChange={(e) => setFormData({...formData, password: e.target.value})}
                    required
                  />
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Assign to Restaurant Chain</label>
                  <select
                    className="admin-form-select"
                    value={formData.restaurantId}
                    onChange={(e) => setFormData({...formData, restaurantId: e.target.value})}
                  >
                    <option value="">-- Assign Later --</option>
                    {restaurants.map(r => (
                      <option key={r.id} value={r.id}>
                        {r.name} {r.hrManagerName ? `(Current: ${r.hrManagerName})` : '(No Manager)'}
                      </option>
                    ))}
                  </select>
                  <small style={{ color: 'var(--admin-text-muted)', marginTop: 4, display: 'block' }}>
                    HR Managers own an entire restaurant chain with all its branches
                  </small>
                </div>
              </div>
              <div className="admin-modal-footer">
                <button 
                  type="button" 
                  className="admin-btn admin-btn-secondary"
                  onClick={() => setShowAddModal(false)}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="admin-btn admin-btn-primary"
                  disabled={submitting}
                >
                  {submitting ? 'Creating...' : 'Create HR Manager'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  )
}

export default AdminHrManagersPage

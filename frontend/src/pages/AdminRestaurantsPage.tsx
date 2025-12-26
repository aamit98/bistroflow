import React, { useEffect, useState, useMemo } from 'react'
import {
  getAllRestaurantsApi,
  createRestaurantApi,
  activateRestaurantApi,
  deactivateRestaurantApi,
  getRestaurantBranchesApi,
  getAllHrManagersApi,
  updateHrManagerRestaurantApi,
  type Restaurant,
  type BranchSummary,
  type HrManager,
} from '../api/AdminApi'
import { useToast } from '../components/ToastContext'
import {
  IconBuilding,
  IconBranch,
  IconManager,
  IconUsers,
  IconPlus,
  IconCheck,
  IconClose,
} from '../components/Icons'
import './admin/Admin.css'

const AdminRestaurantsPage: React.FC = () => {
  const { addToast } = useToast()
  const [restaurants, setRestaurants] = useState<Restaurant[]>([])
  const [branchesByRestaurant, setBranchesByRestaurant] = useState<Record<number, BranchSummary[]>>({})
  const [hrManagers, setHrManagers] = useState<HrManager[]>([])
  const [loading, setLoading] = useState(true)
  const [showAddModal, setShowAddModal] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterStatus, setFilterStatus] = useState<'all' | 'active' | 'inactive'>('all')
  const [expandedRestaurants, setExpandedRestaurants] = useState<Set<number>>(new Set())

  // Form state for new restaurant
  const [formData, setFormData] = useState({
    name: '',
    businessId: '',
    contactEmail: '',
    contactPhone: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [actionLoading, setActionLoading] = useState<number | null>(null)

  // HR Manager assignment state
  const [assigningHrManager, setAssigningHrManager] = useState<number | null>(null)
  const [selectedHrManagerId, setSelectedHrManagerId] = useState<number | null>(null)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    try {
      const [restaurantRes, hrRes] = await Promise.all([
        getAllRestaurantsApi(),
        getAllHrManagersApi(),
      ])
      setRestaurants(restaurantRes.data)
      setHrManagers(hrRes.data)

      // Load branches for each restaurant
      const branchesMap: Record<number, BranchSummary[]> = {}
      await Promise.all(
        restaurantRes.data.map(async (restaurant: Restaurant) => {
          try {
            const branchRes = await getRestaurantBranchesApi(restaurant.id)
            branchesMap[restaurant.id] = branchRes.data
          } catch (err) {
            console.error(`Failed to load branches for restaurant ${restaurant.id}`, err)
            branchesMap[restaurant.id] = []
          }
        })
      )
      setBranchesByRestaurant(branchesMap)
    } catch (err: any) {
      console.error('Failed to load restaurants:', err)
      addToast('Failed to load restaurants', 'error')
    } finally {
      setLoading(false)
    }
  }

  const filteredRestaurants = useMemo(() => {
    let result = restaurants

    // Filter by search term
    if (searchTerm) {
      const lower = searchTerm.toLowerCase()
      result = result.filter(
        r => r.name.toLowerCase().includes(lower) ||
             (r.businessId && r.businessId.toLowerCase().includes(lower)) ||
             (r.hrManagerName && r.hrManagerName.toLowerCase().includes(lower))
      )
    }

    // Filter by status
    if (filterStatus !== 'all') {
      result = result.filter(r => filterStatus === 'active' ? r.active : !r.active)
    }

    return result
  }, [restaurants, searchTerm, filterStatus])

  const stats = useMemo(() => ({
    total: restaurants.length,
    active: restaurants.filter(r => r.active).length,
    inactive: restaurants.filter(r => !r.active).length,
    withHrManager: restaurants.filter(r => r.hrManagerId).length,
    totalBranches: restaurants.reduce((sum, r) => sum + r.branchCount, 0),
    totalEmployees: restaurants.reduce((sum, r) => sum + r.employeeCount, 0),
  }), [restaurants])

  const unassignedHrManagers = useMemo(() => 
    hrManagers.filter(hr => !hr.restaurantId),
  [hrManagers])

  const toggleExpanded = (restaurantId: number) => {
    setExpandedRestaurants(prev => {
      const next = new Set(prev)
      if (next.has(restaurantId)) {
        next.delete(restaurantId)
      } else {
        next.add(restaurantId)
      }
      return next
    })
  }

  const handleAddRestaurant = async (e: React.FormEvent) => {
    e.preventDefault()
    setSubmitting(true)

    try {
      await createRestaurantApi({
        name: formData.name,
        businessId: formData.businessId || undefined,
        contactEmail: formData.contactEmail || undefined,
        contactPhone: formData.contactPhone || undefined,
      })
      addToast('Restaurant chain created successfully!', 'success')
      setShowAddModal(false)
      setFormData({ name: '', businessId: '', contactEmail: '', contactPhone: '' })
      loadData()
    } catch (err: any) {
      console.error('Failed to create restaurant:', err)
      addToast(err.response?.data?.message || 'Failed to create restaurant', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  const handleToggleStatus = async (restaurant: Restaurant) => {
    setActionLoading(restaurant.id)
    try {
      if (restaurant.active) {
        await deactivateRestaurantApi(restaurant.id)
        addToast(`${restaurant.name} and all its branches deactivated`, 'success')
      } else {
        await activateRestaurantApi(restaurant.id)
        addToast(`${restaurant.name} activated`, 'success')
      }
      loadData()
    } catch (err: any) {
      console.error('Failed to toggle status:', err)
      addToast('Failed to update restaurant status', 'error')
    } finally {
      setActionLoading(null)
    }
  }

  const handleAssignHrManager = async (restaurantId: number) => {
    if (selectedHrManagerId === null) return

    try {
      await updateHrManagerRestaurantApi(selectedHrManagerId, restaurantId)
      addToast('HR Manager assigned successfully!', 'success')
      setAssigningHrManager(null)
      setSelectedHrManagerId(null)
      loadData()
    } catch (err: any) {
      console.error('Failed to assign HR manager:', err)
      addToast('Failed to assign HR manager', 'error')
    }
  }

  if (loading) {
    return (
      <div className="bf-loading-container">
        <div className="bf-spinner" />
        <p className="bf-loading-text">Loading restaurant chains...</p>
      </div>
    )
  }

  return (
    <div className="bf-page">
      {/* Header */}
      <div className="bf-page-header">
        <div>
          <h1 className="bf-page-title">
            <IconBuilding size={28} /> Restaurant Chains
          </h1>
          <p className="bf-page-subtitle">Manage restaurant chains and their HR assignments</p>
        </div>
        <div className="bf-header-actions">
          <button 
            className="bf-btn bf-btn-primary"
            onClick={() => setShowAddModal(true)}
          >
            <IconPlus size={18} /> Add Restaurant Chain
          </button>
        </div>
      </div>

      {/* Stats */}
      <div className="bf-stats-grid">
        <div className="bf-stat-card bf-stat-primary">
          <div className="bf-stat-icon"><IconBuilding size={28} /></div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats.total}</p>
            <p className="bf-stat-label">Restaurant Chains</p>
          </div>
        </div>
        <div className="bf-stat-card bf-stat-secondary">
          <div className="bf-stat-icon"><IconBranch size={28} /></div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats.totalBranches}</p>
            <p className="bf-stat-label">Total Branches</p>
          </div>
        </div>
        <div className="bf-stat-card bf-stat-accent">
          <div className="bf-stat-icon"><IconManager size={28} /></div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats.withHrManager}</p>
            <p className="bf-stat-label">With HR Manager</p>
          </div>
        </div>
        <div className="bf-stat-card bf-stat-success">
          <div className="bf-stat-icon"><IconUsers size={28} /></div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats.totalEmployees}</p>
            <p className="bf-stat-label">Total Staff</p>
          </div>
        </div>
      </div>

      {/* Controls */}
      <div className="bf-card bf-card-table">
        <div className="bf-card-header">
          <div className="bf-card-title-group">
            <h2 className="bf-card-title">All Restaurant Chains</h2>
            <span className="bf-badge bf-badge-info">{filteredRestaurants.length} of {restaurants.length}</span>
          </div>
          <div className="bf-header-actions" style={{ gap: 12 }}>
            <div className="bf-search-box">
              <span>🔍</span>
              <input
                type="text"
                placeholder="Search chains..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <select
              className="bf-select"
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value as any)}
              style={{ width: 'auto', minWidth: 140 }}
            >
              <option value="all">All Status</option>
              <option value="active">✅ Active</option>
              <option value="inactive">⚪ Inactive</option>
            </select>
          </div>
        </div>

        {filteredRestaurants.length === 0 ? (
          <div className="bf-empty-state">
            <div className="bf-empty-icon"><IconBuilding size={48} /></div>
            <h3>{searchTerm || filterStatus !== 'all' ? 'No matching restaurant chains' : 'No Restaurant Chains'}</h3>
            <p>{searchTerm || filterStatus !== 'all' ? 'Try adjusting your filters' : 'Create your first restaurant chain to get started'}</p>
            {!searchTerm && filterStatus === 'all' && (
              <button 
                className="bf-btn bf-btn-primary"
                onClick={() => setShowAddModal(true)}
              >
                <IconPlus size={16} /> Add First Restaurant Chain
              </button>
            )}
          </div>
        ) : (
          <div className="bf-accordion-list">
            {filteredRestaurants.map((restaurant) => {
              const branches = branchesByRestaurant[restaurant.id] || []
              const isExpanded = expandedRestaurants.has(restaurant.id)
              
              return (
                <div key={restaurant.id} className="bf-accordion-item">
                  {/* Restaurant Header */}
                  <div 
                    className="bf-accordion-header"
                    onClick={() => toggleExpanded(restaurant.id)}
                  >
                    <div className="bf-accordion-left">
                      <div className="bf-accordion-icon bf-icon-primary">
                        <IconBuilding size={24} />
                      </div>
                      <div className="bf-accordion-info">
                        <div className="bf-accordion-title-row">
                          <h3>{restaurant.name}</h3>
                          <span className={`bf-badge bf-badge-sm ${restaurant.active ? "bf-badge-success" : "bf-badge-danger"}`}>
                            {restaurant.active ? "Active" : "Inactive"}
                          </span>
                        </div>
                        <div className="bf-accordion-meta">
                          <span><IconBranch size={14} /> {branches.length} branch{branches.length !== 1 ? "es" : ""}</span>
                          <span><IconUsers size={14} /> {restaurant.employeeCount} employees</span>
                          {restaurant.businessId && <span>📋 {restaurant.businessId}</span>}
                        </div>
                      </div>
                    </div>
                    <div className="bf-accordion-right">
                      {/* HR Manager Display/Assignment */}
                      {assigningHrManager === restaurant.id ? (
                        <div className="bf-inline-edit" onClick={e => e.stopPropagation()}>
                          <select
                            value={selectedHrManagerId ?? ""}
                            onChange={(e) => setSelectedHrManagerId(e.target.value ? parseInt(e.target.value) : null)}
                            className="bf-select bf-select-sm"
                          >
                            <option value="">Select HR Manager</option>
                            {/* Show current HR manager if assigned */}
                            {restaurant.hrManagerId && (
                              <option value={restaurant.hrManagerId}>
                                {restaurant.hrManagerName} (current)
                              </option>
                            )}
                            {/* Show unassigned HR managers */}
                            {unassignedHrManagers.map((hr) => (
                              <option key={hr.id} value={hr.id}>
                                {hr.name}
                              </option>
                            ))}
                          </select>
                          <button
                            onClick={() => handleAssignHrManager(restaurant.id)}
                            className="bf-btn bf-btn-icon bf-btn-success"
                            disabled={!selectedHrManagerId}
                          >
                            <IconCheck size={16} />
                          </button>
                          <button
                            onClick={() => {
                              setAssigningHrManager(null)
                              setSelectedHrManagerId(null)
                            }}
                            className="bf-btn bf-btn-icon bf-btn-ghost"
                          >
                            <IconClose size={16} />
                          </button>
                        </div>
                      ) : (
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }} onClick={e => e.stopPropagation()}>
                          {restaurant.hrManagerName ? (
                            <span className="bf-badge bf-badge-success">
                              <IconManager size={14} /> {restaurant.hrManagerName}
                            </span>
                          ) : (
                            <span className="bf-badge bf-badge-warning">
                              ⚠️ No HR Manager
                            </span>
                          )}
                          <button
                            onClick={() => {
                              setAssigningHrManager(restaurant.id)
                              setSelectedHrManagerId(restaurant.hrManagerId ?? null)
                            }}
                            className="bf-btn bf-btn-sm bf-btn-outline"
                          >
                            {restaurant.hrManagerId ? "Change" : "Assign"}
                          </button>
                        </div>
                      )}
                      <span className={`bf-accordion-chevron ${isExpanded ? "expanded" : ""}`}>▼</span>
                    </div>
                  </div>

                  {/* Expanded Content */}
                  {isExpanded && (
                    <div className="bf-accordion-content">
                      {/* Restaurant Details */}
                      <div style={{ 
                        display: 'grid', 
                        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', 
                        gap: 16, 
                        marginBottom: 20,
                        padding: 16,
                        background: 'var(--bf-surface-hover)',
                        borderRadius: 8
                      }}>
                        <div>
                          <p style={{ margin: 0, fontSize: 12, color: 'var(--bf-text-muted)' }}>Business ID</p>
                          <p style={{ margin: '4px 0 0', fontWeight: 500 }}>{restaurant.businessId || '-'}</p>
                        </div>
                        <div>
                          <p style={{ margin: 0, fontSize: 12, color: 'var(--bf-text-muted)' }}>Contact Email</p>
                          <p style={{ margin: '4px 0 0', fontWeight: 500 }}>{restaurant.contactEmail || '-'}</p>
                        </div>
                        <div>
                          <p style={{ margin: 0, fontSize: 12, color: 'var(--bf-text-muted)' }}>Contact Phone</p>
                          <p style={{ margin: '4px 0 0', fontWeight: 500 }}>{restaurant.contactPhone || '-'}</p>
                        </div>
                        <div>
                          <p style={{ margin: 0, fontSize: 12, color: 'var(--bf-text-muted)' }}>Created</p>
                          <p style={{ margin: '4px 0 0', fontWeight: 500 }}>
                            {restaurant.createdAt ? new Date(restaurant.createdAt).toLocaleDateString() : '-'}
                          </p>
                        </div>
                      </div>

                      {/* Branches List */}
                      <h4 style={{ margin: '0 0 12px', display: 'flex', alignItems: 'center', gap: 8 }}>
                        <IconBranch size={18} /> Branches ({branches.length})
                      </h4>
                      
                      {branches.length > 0 ? (
                        <div className="bf-branch-list">
                          {branches.map((branch) => (
                            <div key={branch.id} className="bf-branch-item">
                              <div className="bf-branch-left">
                                <IconBranch size={18} className="bf-branch-icon" />
                                <div>
                                  <p className="bf-branch-name">{branch.name}</p>
                                  <p className="bf-branch-meta">
                                    📍 {branch.address}, {branch.city} • {branch.employeeCount} employees
                                  </p>
                                </div>
                              </div>
                              <span className={`bf-badge bf-badge-sm ${branch.active ? "bf-badge-success" : "bf-badge-danger"}`}>
                                {branch.active ? "Active" : "Inactive"}
                              </span>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <div className="bf-branch-empty">
                          <p>No branches yet</p>
                          <a href="/admin/branches">+ Add branch</a>
                        </div>
                      )}

                      {/* Actions */}
                      <div style={{ display: 'flex', gap: 12, marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--bf-border)' }}>
                        <button
                          className={`bf-btn bf-btn-sm ${restaurant.active ? 'bf-btn-danger' : 'bf-btn-primary'}`}
                          onClick={(e) => {
                            e.stopPropagation()
                            handleToggleStatus(restaurant)
                          }}
                          disabled={actionLoading === restaurant.id}
                        >
                          {actionLoading === restaurant.id 
                            ? 'Processing...' 
                            : restaurant.active 
                              ? '⏸️ Deactivate Chain' 
                              : '▶️ Activate Chain'
                          }
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Add Restaurant Modal */}
      {showAddModal && (
        <div className="bf-modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="bf-modal" onClick={e => e.stopPropagation()}>
            <div className="bf-modal-header">
              <div className="bf-modal-title-group">
                <div className="bf-modal-icon">
                  <IconBuilding size={24} />
                </div>
                <div>
                  <h2>Add Restaurant Chain</h2>
                  <p>Create a new restaurant chain organization</p>
                </div>
              </div>
              <button className="bf-modal-close" onClick={() => setShowAddModal(false)}>
                <IconClose size={20} />
              </button>
            </div>
            <form onSubmit={handleAddRestaurant}>
              <div className="bf-modal-body">
                <div className="bf-form-group">
                  <label className="bf-label">
                    Chain Name <span className="bf-required">*</span>
                  </label>
                  <input
                    type="text"
                    className="bf-input"
                    placeholder="e.g., BistroFlow Tel Aviv"
                    value={formData.name}
                    onChange={(e) => setFormData({...formData, name: e.target.value})}
                    required
                  />
                  <p className="bf-form-hint">The name of your restaurant chain or franchise</p>
                </div>
                <div className="bf-form-group">
                  <label className="bf-label">Business ID</label>
                  <input
                    type="text"
                    className="bf-input"
                    placeholder="e.g., IL-123456789"
                    value={formData.businessId}
                    onChange={(e) => setFormData({...formData, businessId: e.target.value})}
                  />
                  <p className="bf-form-hint">Official business registration number</p>
                </div>
                <div className="bf-form-row">
                  <div className="bf-form-group">
                    <label className="bf-label">Contact Email</label>
                    <input
                      type="email"
                      className="bf-input"
                      placeholder="contact@restaurant.com"
                      value={formData.contactEmail}
                      onChange={(e) => setFormData({...formData, contactEmail: e.target.value})}
                    />
                  </div>
                  <div className="bf-form-group">
                    <label className="bf-label">Contact Phone</label>
                    <input
                      type="tel"
                      className="bf-input"
                      placeholder="+972-3-123-4567"
                      value={formData.contactPhone}
                      onChange={(e) => setFormData({...formData, contactPhone: e.target.value})}
                    />
                  </div>
                </div>
              </div>
              <div className="bf-modal-footer">
                <button 
                  type="button" 
                  className="bf-btn bf-btn-secondary"
                  onClick={() => setShowAddModal(false)}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="bf-btn bf-btn-primary"
                  disabled={submitting}
                >
                  {submitting ? 'Creating...' : 'Create Restaurant Chain'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminRestaurantsPage

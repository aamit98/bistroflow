import React, { useEffect, useState, useMemo } from 'react'
import {
  getAllBranchesApi,
  getAllRestaurantsApi,
  createBranchApi,
  activateBranchApi,
  deactivateBranchApi,
  type BranchSummary,
  type Restaurant,
} from '../api/AdminApi'
import { useToast } from '../components/ToastContext'
import {
  IconBranch,
  IconBuilding,
  IconUsers,
  IconPlus,
  IconClose,
} from '../components/Icons'
import './admin/Admin.css'

const AdminBranchesPage: React.FC = () => {
  const { addToast } = useToast()
  const [branches, setBranches] = useState<BranchSummary[]>([])
  const [restaurants, setRestaurants] = useState<Restaurant[]>([])
  const [loading, setLoading] = useState(true)
  const [showAddModal, setShowAddModal] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterStatus, setFilterStatus] = useState<'all' | 'active' | 'inactive'>('all')
  const [filterRestaurant, setFilterRestaurant] = useState<number | 'all'>('all')
  const [viewMode, setViewMode] = useState<'grid' | 'table'>('grid')

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    address: '',
    city: '',
    restaurantId: '' as string | number,
  })
  const [submitting, setSubmitting] = useState(false)
  const [actionLoading, setActionLoading] = useState<number | null>(null)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    try {
      const [branchRes, restaurantRes] = await Promise.all([
        getAllBranchesApi(),
        getAllRestaurantsApi(),
      ])
      setBranches(branchRes.data)
      setRestaurants(restaurantRes.data)
    } catch (err) {
      console.error('Failed to load branches:', err)
      addToast('Failed to load branches', 'error')
    } finally {
      setLoading(false)
    }
  }

  const filteredBranches = useMemo(() => {
    let result = branches

    // Filter by search term
    if (searchTerm) {
      const lower = searchTerm.toLowerCase()
      result = result.filter(
        b => b.name.toLowerCase().includes(lower) ||
             b.address.toLowerCase().includes(lower) ||
             (b.restaurantName && b.restaurantName.toLowerCase().includes(lower))
      )
    }

    // Filter by status
    if (filterStatus !== 'all') {
      result = result.filter(b => filterStatus === 'active' ? b.active : !b.active)
    }

    // Filter by restaurant
    if (filterRestaurant !== 'all') {
      result = result.filter(b => b.restaurantId === filterRestaurant)
    }

    return result
  }, [branches, searchTerm, filterStatus, filterRestaurant])

  const stats = useMemo(() => ({
    total: branches.length,
    active: branches.filter(b => b.active).length,
    inactive: branches.filter(b => !b.active).length,
    withRestaurant: branches.filter(b => b.restaurantName).length,
    totalEmployees: branches.reduce((sum, b) => sum + b.employeeCount, 0),
  }), [branches])

  const handleAddBranch = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!formData.restaurantId) {
      addToast('Please select a restaurant chain for this branch', 'error')
      return
    }
    
    setSubmitting(true)

    try {
      await createBranchApi({
        name: formData.name,
        address: formData.address,
        city: formData.city || undefined,
        restaurantId: typeof formData.restaurantId === 'string' 
          ? parseInt(formData.restaurantId) 
          : formData.restaurantId,
      })
      addToast('Branch created successfully!', 'success')
      setShowAddModal(false)
      setFormData({ name: '', address: '', city: '', restaurantId: '' })
      loadData()
    } catch (err) {
      console.error('Failed to create branch:', err)
      addToast('Failed to create branch', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  const handleToggleStatus = async (branch: BranchSummary) => {
    setActionLoading(branch.id)
    try {
      if (branch.active) {
        await deactivateBranchApi(branch.id)
        addToast(`${branch.name} deactivated`, 'success')
      } else {
        await activateBranchApi(branch.id)
        addToast(`${branch.name} activated`, 'success')
      }
      loadData()
    } catch (err) {
      console.error('Failed to toggle status:', err)
      addToast('Failed to update branch status', 'error')
    } finally {
      setActionLoading(null)
    }
  }

  if (loading) {
    return (
      <div className="bf-loading-container">
        <div className="bf-spinner" />
        <p className="bf-loading-text">Loading branches...</p>
      </div>
    )
  }

  return (
    <div className="bf-page">
      {/* Header */}
      <div className="bf-page-header">
        <div>
          <h1 className="bf-page-title">
            <IconBranch size={28} /> Branches
          </h1>
          <p className="bf-page-subtitle">Manage physical branch locations across restaurant chains</p>
        </div>
        <div className="bf-header-actions">
          <button 
            className="bf-btn bf-btn-primary"
            onClick={() => setShowAddModal(true)}
          >
            <IconPlus size={18} /> Add Branch
          </button>
        </div>
      </div>

      {/* Stats */}
      <div className="bf-stats-grid">
        <div className="bf-stat-card bf-stat-primary">
          <div className="bf-stat-icon"><IconBranch size={28} /></div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats.total}</p>
            <p className="bf-stat-label">Total Branches</p>
          </div>
        </div>
        <div className="bf-stat-card bf-stat-secondary">
          <div className="bf-stat-icon">✅</div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats.active}</p>
            <p className="bf-stat-label">Active</p>
          </div>
        </div>
        <div className="bf-stat-card bf-stat-accent">
          <div className="bf-stat-icon"><IconUsers size={28} /></div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats.totalEmployees}</p>
            <p className="bf-stat-label">Total Staff</p>
          </div>
        </div>
        <div className="bf-stat-card bf-stat-success">
          <div className="bf-stat-icon"><IconBuilding size={28} /></div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats.withRestaurant}</p>
            <p className="bf-stat-label">Assigned to Chain</p>
          </div>
        </div>
      </div>

      {/* Controls */}
      <div className="bf-card bf-card-table">
        <div className="bf-card-header">
          <div className="bf-card-title-group">
            <h2 className="bf-card-title">All Branches</h2>
            <span className="bf-badge bf-badge-info">{filteredBranches.length} of {branches.length}</span>
          </div>
          <div className="bf-header-actions" style={{ gap: 12 }}>
            <div className="bf-search-box">
              <span>🔍</span>
              <input
                type="text"
                placeholder="Search branches..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <select
              className="bf-select"
              value={filterRestaurant}
              onChange={(e) => setFilterRestaurant(e.target.value === 'all' ? 'all' : parseInt(e.target.value))}
              style={{ width: 'auto', minWidth: 180 }}
            >
              <option value="all">All Restaurant Chains</option>
              {restaurants.map(r => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </select>
            <select
              className="bf-select"
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value as 'all' | 'active' | 'inactive')}
              style={{ width: 'auto', minWidth: 140 }}
            >
              <option value="all">All Status</option>
              <option value="active">✅ Active</option>
              <option value="inactive">⚪ Inactive</option>
            </select>
            <div style={{ display: 'flex', gap: 4 }}>
              <button
                className={`bf-btn bf-btn-icon ${viewMode === 'grid' ? 'bf-btn-primary' : 'bf-btn-secondary'}`}
                onClick={() => setViewMode('grid')}
                title="Grid View"
              >
                ⊞
              </button>
              <button
                className={`bf-btn bf-btn-icon ${viewMode === 'table' ? 'bf-btn-primary' : 'bf-btn-secondary'}`}
                onClick={() => setViewMode('table')}
                title="Table View"
              >
                ☰
              </button>
            </div>
          </div>
        </div>

        {filteredBranches.length === 0 ? (
          <div className="bf-empty-state">
            <div className="bf-empty-icon"><IconBranch size={48} /></div>
            <h3>{searchTerm || filterStatus !== 'all' || filterRestaurant !== 'all' ? 'No matching branches' : 'No Branches'}</h3>
            <p>{searchTerm || filterStatus !== 'all' ? 'Try adjusting your filters' : 'Create your first branch to get started'}</p>
            {!searchTerm && filterStatus === 'all' && (
              <button 
                className="bf-btn bf-btn-primary"
                onClick={() => setShowAddModal(true)}
              >
                <IconPlus size={16} /> Add First Branch
              </button>
            )}
          </div>
        ) : viewMode === 'table' ? (
          <div className="bf-table-wrapper">
            <table className="bf-table">
              <thead>
                <tr>
                  <th>Branch</th>
                  <th>Location</th>
                  <th>Restaurant Chain</th>
                  <th>Staff</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredBranches.map((branch) => (
                  <tr key={branch.id}>
                    <td>
                      <div className="bf-table-user">
                        <div className="bf-avatar bf-avatar-secondary">
                          <IconBranch size={16} />
                        </div>
                        <span className="bf-table-name">{branch.name}</span>
                      </div>
                    </td>
                    <td className="bf-table-muted">📍 {branch.address}, {branch.city}</td>
                    <td>
                      {branch.restaurantName ? (
                        <span className="bf-badge bf-badge-success">
                          <IconBuilding size={12} /> {branch.restaurantName}
                        </span>
                      ) : (
                        <span className="bf-badge bf-badge-warning">⚠ Not Assigned</span>
                      )}
                    </td>
                    <td>
                      <span className="bf-badge bf-badge-info">{branch.employeeCount} staff</span>
                    </td>
                    <td>
                      <span className={`bf-badge ${branch.active ? 'bf-badge-success' : 'bf-badge-danger'}`}>
                        {branch.active ? '● Active' : '○ Inactive'}
                      </span>
                    </td>
                    <td>
                      <button
                        className={`bf-btn bf-btn-sm ${branch.active ? 'bf-btn-danger' : 'bf-btn-secondary'}`}
                        onClick={() => handleToggleStatus(branch)}
                        disabled={actionLoading === branch.id}
                      >
                        {actionLoading === branch.id ? '...' : branch.active ? 'Deactivate' : 'Activate'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', 
            gap: 20,
            padding: 24
          }}>
            {filteredBranches.map((branch) => (
              <div
                key={branch.id}
                className="bf-card"
                style={{
                  opacity: branch.active ? 1 : 0.7,
                  transition: 'all 0.2s',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
                  <div>
                    <h3 style={{ margin: 0, fontSize: 18, display: 'flex', alignItems: 'center', gap: 8 }}>
                      <IconBranch size={20} /> {branch.name}
                    </h3>
                    <p style={{ margin: '6px 0 0', color: 'var(--bf-text-muted)', fontSize: 13 }}>
                      📍 {branch.address}, {branch.city}
                    </p>
                  </div>
                  <span className={`bf-badge ${branch.active ? 'bf-badge-success' : 'bf-badge-danger'}`}>
                    {branch.active ? '● Active' : '○ Inactive'}
                  </span>
                </div>

                <div style={{ 
                  display: 'grid', 
                  gridTemplateColumns: '1fr 1fr', 
                  gap: 16, 
                  marginBottom: 20,
                  padding: 16,
                  background: 'var(--bf-surface-hover)',
                  borderRadius: 12
                }}>
                  <div>
                    <p style={{ margin: 0, fontSize: 28, fontWeight: 700, color: 'var(--bf-primary)' }}>
                      {branch.employeeCount}
                    </p>
                    <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--bf-text-muted)' }}>Staff Members</p>
                  </div>
                  <div>
                    {branch.restaurantName ? (
                      <>
                        <p style={{ margin: 0, fontSize: 14, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 4 }}>
                          <IconBuilding size={14} /> {branch.restaurantName}
                        </p>
                        <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--bf-success)' }}>Restaurant Chain</p>
                      </>
                    ) : (
                      <>
                        <p style={{ margin: 0, fontSize: 14, fontWeight: 500, color: 'var(--bf-warning)' }}>
                          ⚠️ Not Assigned
                        </p>
                        <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--bf-text-muted)' }}>Restaurant Chain</p>
                      </>
                    )}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 10 }}>
                  <button
                    className={`bf-btn bf-btn-sm ${branch.active ? 'bf-btn-danger' : 'bf-btn-primary'}`}
                    style={{ flex: 1 }}
                    onClick={() => handleToggleStatus(branch)}
                    disabled={actionLoading === branch.id}
                  >
                    {actionLoading === branch.id ? 'Processing...' : branch.active ? '⏸️ Deactivate' : '▶️ Activate'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add Branch Modal */}
      {showAddModal && (
        <div className="bf-modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="bf-modal" onClick={e => e.stopPropagation()}>
            <div className="bf-modal-header">
              <div className="bf-modal-title-group">
                <div className="bf-modal-icon">
                  <IconBranch size={24} />
                </div>
                <div>
                  <h2>Add New Branch</h2>
                  <p>Create a new branch location</p>
                </div>
              </div>
              <button className="bf-modal-close" onClick={() => setShowAddModal(false)}>
                <IconClose size={20} />
              </button>
            </div>
            <form onSubmit={handleAddBranch}>
              <div className="bf-modal-body">
                <div className="bf-form-group">
                  <label className="bf-label">
                    Restaurant Chain <span className="bf-required">*</span>
                  </label>
                  <select
                    className="bf-select"
                    value={formData.restaurantId}
                    onChange={(e) => setFormData({...formData, restaurantId: e.target.value})}
                    required
                  >
                    <option value="">-- Select Restaurant Chain --</option>
                    {restaurants.map(r => (
                      <option key={r.id} value={r.id}>{r.name}</option>
                    ))}
                  </select>
                  <p className="bf-form-hint">The restaurant chain this branch belongs to</p>
                </div>
                <div className="bf-form-group">
                  <label className="bf-label">
                    Branch Name <span className="bf-required">*</span>
                  </label>
                  <input
                    type="text"
                    className="bf-input"
                    placeholder="e.g., Downtown Branch"
                    value={formData.name}
                    onChange={(e) => setFormData({...formData, name: e.target.value})}
                    required
                  />
                </div>
                <div className="bf-form-row">
                  <div className="bf-form-group">
                    <label className="bf-label">
                      Address <span className="bf-required">*</span>
                    </label>
                    <input
                      type="text"
                      className="bf-input"
                      placeholder="e.g., 123 Main St"
                      value={formData.address}
                      onChange={(e) => setFormData({...formData, address: e.target.value})}
                      required
                    />
                  </div>
                  <div className="bf-form-group">
                    <label className="bf-label">City</label>
                    <input
                      type="text"
                      className="bf-input"
                      placeholder="e.g., Tel Aviv"
                      value={formData.city}
                      onChange={(e) => setFormData({...formData, city: e.target.value})}
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
                  disabled={submitting || !formData.restaurantId}
                >
                  {submitting ? 'Creating...' : 'Create Branch'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminBranchesPage

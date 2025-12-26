import React, { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import type { BranchRole, CreateRolePayload } from '../api/BranchRoleApi'
import {
  getBranchRolesApi,
  createBranchRoleApi,
  updateBranchRoleApi,
  deleteBranchRoleApi,
  agorotToNis,
  nisToAgorot,
} from '../api/BranchRoleApi'
import BranchApi from '../api/BranchApi'
import './RoleManagerPage.css'

const DEFAULT_COLORS = [
  '#69558bff', // Purple
  '#EF4444', // Red
  '#F59E0B', // Orange
  '#10B981', // Green
  '#3B82F6', // Blue
  '#EC4899', // Pink
  '#6B7280', // Gray
  '#14B8A6', // Teal
]

interface EditingRole {
  id?: number
  code: string
  displayName: string
  description: string
  color: string
  icon: string
  baseHourlyRateNis: string // store as string for input
  requiresCertification: boolean
  canSupervise: boolean
  sortOrder: number
  active: boolean
}

const emptyRole: EditingRole = {
  code: '',
  displayName: '',
  description: '',
  color: '#6B7280',
  icon: '',
  baseHourlyRateNis: '33.50',
  requiresCertification: false,
  canSupervise: false,
  sortOrder: 0,
  active: true,
}

const RoleManagerPage: React.FC = () => {
  const { branchId } = useParams<{ branchId: string }>()
  const [branchName, setBranchName] = useState('')
  const [roles, setRoles] = useState<BranchRole[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editingRole, setEditingRole] = useState<EditingRole | null>(null)
  const [saving, setSaving] = useState(false)
  const [showInactive, setShowInactive] = useState(false)

  const numericBranchId = parseInt(branchId || '0', 10)

  const loadData = async () => {
    if (!numericBranchId) return
    setLoading(true)
    setError(null)
    try {
      const [rolesData, branchData] = await Promise.all([
        getBranchRolesApi(numericBranchId, !showInactive),
        BranchApi.getById(numericBranchId),
      ])
      setRoles(rolesData.data)
      setBranchName(branchData.name)
    } catch (e: any) {
      console.error(e)
      setError(e.response?.data?.error || 'Failed to load roles')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [numericBranchId, showInactive])

  const handleNewRole = () => {
    // Suggest next sort order
    const maxSort = roles.reduce((max, r) => Math.max(max, r.sortOrder), 0)
    setEditingRole({
      ...emptyRole,
      sortOrder: maxSort + 1,
      color: DEFAULT_COLORS[roles.length % DEFAULT_COLORS.length],
    })
  }

  const handleEditRole = (role: BranchRole) => {
    setEditingRole({
      id: role.id,
      code: role.code,
      displayName: role.displayName,
      description: role.description || '',
      color: role.color,
      icon: role.icon || '',
      baseHourlyRateNis: (role.baseHourlyRate / 100).toFixed(2),
      requiresCertification: role.requiresCertification,
      canSupervise: role.canSupervise,
      sortOrder: role.sortOrder,
      active: role.active,
    })
  }

  const handleSaveRole = async () => {
    if (!editingRole) return

    // Validation
    const rateNis = parseFloat(editingRole.baseHourlyRateNis)
    if (isNaN(rateNis) || rateNis < 33.50) {
      setError('Hourly rate must be at least ₪33.50 (minimum wage)')
      return
    }

    if (!editingRole.code.trim()) {
      setError('Role code is required')
      return
    }

    if (!editingRole.displayName.trim()) {
      setError('Display name is required')
      return
    }

    setSaving(true)
    setError(null)

    try {
      const rateAgorot = nisToAgorot(rateNis)

      if (editingRole.id) {
        // Update existing
        await updateBranchRoleApi(numericBranchId, editingRole.id, {
          displayName: editingRole.displayName,
          description: editingRole.description || undefined,
          color: editingRole.color,
          icon: editingRole.icon || undefined,
          baseHourlyRate: rateAgorot,
          requiresCertification: editingRole.requiresCertification,
          canSupervise: editingRole.canSupervise,
          sortOrder: editingRole.sortOrder,
          active: editingRole.active,
        })
      } else {
        // Create new
        const payload: CreateRolePayload = {
          code: editingRole.code.toUpperCase().replace(/\s+/g, '_'),
          displayName: editingRole.displayName,
          description: editingRole.description || undefined,
          color: editingRole.color,
          icon: editingRole.icon || undefined,
          baseHourlyRate: rateAgorot,
          requiresCertification: editingRole.requiresCertification,
          canSupervise: editingRole.canSupervise,
          sortOrder: editingRole.sortOrder,
        }
        await createBranchRoleApi(numericBranchId, payload)
      }

      setEditingRole(null)
      await loadData()
    } catch (e: any) {
      console.error(e)
      setError(e.response?.data?.error || 'Failed to save role')
    } finally {
      setSaving(false)
    }
  }

  const handleDeleteRole = async (role: BranchRole) => {
    if (!confirm(`Deactivate "${role.displayName}"? This role will no longer be available for assignments.`)) {
      return
    }

    try {
      await deleteBranchRoleApi(numericBranchId, role.id)
      await loadData()
    } catch (e: any) {
      console.error(e)
      setError(e.response?.data?.error || 'Failed to deactivate role')
    }
  }

  const handleReactivateRole = async (role: BranchRole) => {
    try {
      await updateBranchRoleApi(numericBranchId, role.id, { active: true })
      await loadData()
    } catch (e: any) {
      console.error(e)
      setError(e.response?.data?.error || 'Failed to reactivate role')
    }
  }

  if (loading) {
    return (
      <div className="page">
        <div className="page-header">
          <h2 className="page-title">Role Manager</h2>
        </div>
        <div className="card">Loading...</div>
      </div>
    )
  }

  return (
    <div className="page role-manager-page">
      <div className="page-header">
        <div>
          <div className="breadcrumb">
            <Link to="/hr/branches">← Branch Settings</Link>
          </div>
          <h2 className="page-title">Role Manager</h2>
          <p className="page-subtitle">
            Configure roles for <strong>{branchName}</strong>
          </p>
        </div>
        <button className="btn btn-primary" onClick={handleNewRole}>
          + Add Role
        </button>
      </div>

      {error && (
        <div className="form-error" style={{ marginBottom: '1rem' }}>
          {error}
          <button
            onClick={() => setError(null)}
            style={{ marginLeft: '1rem', background: 'transparent', border: 'none', color: 'inherit', cursor: 'pointer' }}
          >
            ✕
          </button>
        </div>
      )}

      <div className="controls-bar">
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={showInactive}
            onChange={(e) => setShowInactive(e.target.checked)}
          />
          Show inactive roles
        </label>
      </div>

      {/* Role Edit Modal */}
      {editingRole && (
        <div className="modal-overlay">
          <div className="card modal-card">
            <h3 style={{ marginTop: 0 }}>
              {editingRole.id ? 'Edit Role' : 'New Role'}
            </h3>

            <div className="form-row">
              <div className="form-group">
                <label>Role Code</label>
                <input
                  type="text"
                  value={editingRole.code}
                  onChange={(e) =>
                    setEditingRole({
                      ...editingRole,
                      code: e.target.value.toUpperCase().replace(/\s+/g, '_'),
                    })
                  }
                  placeholder="GRILL_MASTER"
                  disabled={!!editingRole.id}
                  style={{ fontFamily: 'monospace' }}
                />
                {editingRole.id && (
                  <small className="form-hint">Code cannot be changed after creation</small>
                )}
              </div>

              <div className="form-group">
                <label>Display Name</label>
                <input
                  type="text"
                  value={editingRole.displayName}
                  onChange={(e) =>
                    setEditingRole({ ...editingRole, displayName: e.target.value })
                  }
                  placeholder="Grill Master"
                />
              </div>
            </div>

            <div className="form-group">
              <label>Description (optional)</label>
              <textarea
                value={editingRole.description}
                onChange={(e) =>
                  setEditingRole({ ...editingRole, description: e.target.value })
                }
                placeholder="Operates the grill station, prepares burgers and meat dishes"
                rows={2}
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label>Hourly Rate (NIS)</label>
                <div className="input-with-prefix">
                  <span className="input-prefix">₪</span>
                  <input
                    type="number"
                    step="0.50"
                    min="33.50"
                    value={editingRole.baseHourlyRateNis}
                    onChange={(e) =>
                      setEditingRole({ ...editingRole, baseHourlyRateNis: e.target.value })
                    }
                  />
                </div>
                <small className="form-hint">Minimum wage: ₪33.50/hr</small>
              </div>

              <div className="form-group">
                <label>Sort Order</label>
                <input
                  type="number"
                  value={editingRole.sortOrder}
                  onChange={(e) =>
                    setEditingRole({
                      ...editingRole,
                      sortOrder: parseInt(e.target.value, 10) || 0,
                    })
                  }
                />
              </div>
            </div>

            <div className="form-group">
              <label>Color</label>
              <div className="color-picker">
                {DEFAULT_COLORS.map((color) => (
                  <button
                    key={color}
                    type="button"
                    className={`color-swatch ${editingRole.color === color ? 'selected' : ''}`}
                    style={{ backgroundColor: color }}
                    onClick={() => setEditingRole({ ...editingRole, color })}
                  />
                ))}
                <input
                  type="color"
                  value={editingRole.color}
                  onChange={(e) =>
                    setEditingRole({ ...editingRole, color: e.target.value })
                  }
                  className="color-input"
                />
              </div>
            </div>

            <div className="form-row checkbox-row">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={editingRole.canSupervise}
                  onChange={(e) =>
                    setEditingRole({ ...editingRole, canSupervise: e.target.checked })
                  }
                />
                Can supervise others
              </label>

              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={editingRole.requiresCertification}
                  onChange={(e) =>
                    setEditingRole({
                      ...editingRole,
                      requiresCertification: e.target.checked,
                    })
                  }
                />
                Requires certification
              </label>
            </div>

            {editingRole.id && (
              <div className="form-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={editingRole.active}
                    onChange={(e) =>
                      setEditingRole({ ...editingRole, active: e.target.checked })
                    }
                  />
                  Active
                </label>
                <small className="form-hint">
                  Inactive roles won't appear when assigning employees
                </small>
              </div>
            )}

            <div className="modal-actions">
              <button
                className="btn btn-primary"
                onClick={handleSaveRole}
                disabled={saving}
              >
                {saving ? 'Saving...' : 'Save Role'}
              </button>
              <button className="btn" onClick={() => setEditingRole(null)}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Roles List */}
      <div className="roles-list">
        {roles.length === 0 ? (
          <div className="card empty-state">
            <p>No roles configured for this branch.</p>
            <p>
              Click <strong>+ Add Role</strong> to create your first role.
            </p>
          </div>
        ) : (
          <div className="roles-grid">
            {roles.map((role) => (
              <div
                key={role.id}
                className={`role-card ${!role.active ? 'inactive' : ''}`}
              >
                <div className="role-header">
                  <div
                    className="role-color-badge"
                    style={{ backgroundColor: role.color }}
                  />
                  <div className="role-title">
                    <h4>{role.displayName}</h4>
                    <code className="role-code">{role.code}</code>
                  </div>
                  <div className="role-badges">
                    {role.canSupervise && (
                      <span className="badge badge-purple" title="Can supervise">
                        👑
                      </span>
                    )}
                    {role.requiresCertification && (
                      <span className="badge badge-yellow" title="Requires certification">
                        📜
                      </span>
                    )}
                    {!role.active && (
                      <span className="badge badge-red">Inactive</span>
                    )}
                  </div>
                </div>

                {role.description && (
                  <p className="role-description">{role.description}</p>
                )}

                <div className="role-rate">
                  <span className="rate-label">Hourly Rate:</span>
                  <span className="rate-value">{agorotToNis(role.baseHourlyRate)}</span>
                </div>

                <div className="role-actions">
                  <button className="btn btn-sm" onClick={() => handleEditRole(role)}>
                    Edit
                  </button>
                  {role.active ? (
                    <button
                      className="btn btn-sm btn-danger"
                      onClick={() => handleDeleteRole(role)}
                    >
                      Deactivate
                    </button>
                  ) : (
                    <button
                      className="btn btn-sm btn-success"
                      onClick={() => handleReactivateRole(role)}
                    >
                      Reactivate
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default RoleManagerPage

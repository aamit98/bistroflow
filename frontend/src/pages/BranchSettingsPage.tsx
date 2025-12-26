import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import BranchApi, { type Branch } from '../api/BranchApi'

interface EditingBranch {
  id?: number
  name: string
  address: string
  city: string
  phone: string
  timezone: string
  active: boolean
}

const emptyBranch: EditingBranch = {
  name: '',
  address: '',
  city: '',
  phone: '',
  timezone: 'Asia/Jerusalem',
  active: true,
}

const BranchSettingsPage: React.FC = () => {
  const [branches, setBranches] = useState<Branch[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editingBranch, setEditingBranch] = useState<EditingBranch | null>(null)
  const [saving, setSaving] = useState(false)
  const [expandedBranch, setExpandedBranch] = useState<number | null>(null)

  const loadBranches = async () => {
    setLoading(true)
    setError(null)
    try {
      // Use activeOnly=true to get only branches the HR manager has access to
      const data = await BranchApi.getAll(true)
      setBranches(data)
    } catch (e) {
      console.error(e)
      setError('Failed to load branches')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadBranches()
  }, [])

  const handleNewBranch = () => {
    setEditingBranch({ ...emptyBranch })
  }

  const handleEditBranch = (branch: Branch) => {
    setEditingBranch({
      id: branch.id,
      name: branch.name,
      address: branch.address,
      city: branch.city,
      phone: branch.phone ?? '',
      timezone: branch.timezone,
      active: branch.active,
    })
  }

  const handleSaveBranch = async () => {
    if (!editingBranch) return

    setSaving(true)
    setError(null)
    try {
      if (editingBranch.id) {
        // Update existing
        await BranchApi.update(editingBranch.id, {
          name: editingBranch.name,
          address: editingBranch.address,
          city: editingBranch.city,
          phone: editingBranch.phone || undefined,
          timezone: editingBranch.timezone,
          active: editingBranch.active,
        })
      } else {
        // Create new
        await BranchApi.create({
          name: editingBranch.name,
          address: editingBranch.address,
          city: editingBranch.city,
          phone: editingBranch.phone || undefined,
          timezone: editingBranch.timezone,
        })
      }
      setEditingBranch(null)
      await loadBranches()
    } catch (e: any) {
      console.error(e)
      setError(e.response?.data?.message || 'Failed to save branch')
    } finally {
      setSaving(false)
    }
  }

  const handleCancelEdit = () => {
    setEditingBranch(null)
  }

  const toggleExpand = (branchId: number) => {
    setExpandedBranch(expandedBranch === branchId ? null : branchId)
  }

  const formatTime = (time: string) => {
    const [hours, minutes] = time.split(':')
    const h = parseInt(hours, 10)
    const ampm = h >= 12 ? 'PM' : 'AM'
    const h12 = h % 12 || 12
    return `${h12}:${minutes} ${ampm}`
  }

  if (loading) {
    return (
      <div className="page">
        <div className="page-header">
          <h2 className="page-title">Branch Settings</h2>
        </div>
        <div className="card">Loading...</div>
      </div>
    )
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Branch Settings</h2>
          <p className="page-subtitle">
            Manage restaurant branches and their shift schedules
          </p>
        </div>
        <button className="btn btn-primary" onClick={handleNewBranch}>
          + Add Branch
        </button>
      </div>

      {error && <div className="form-error" style={{ marginBottom: '1rem' }}>{error}</div>}

      {/* Branch Edit Modal */}
      {editingBranch && (
        <div className="modal-overlay" style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
        }}>
          <div className="card" style={{ 
            width: '100%', 
            maxWidth: '500px',
            maxHeight: '90vh',
            overflow: 'auto'
          }}>
            <h3 style={{ marginTop: 0 }}>
              {editingBranch.id ? 'Edit Branch' : 'New Branch'}
            </h3>

            <div className="form-group">
              <label>Branch Name</label>
              <input
                type="text"
                value={editingBranch.name}
                onChange={(e) => setEditingBranch({ ...editingBranch, name: e.target.value })}
                placeholder="Downtown"
              />
            </div>

            <div className="form-group">
              <label>Address</label>
              <input
                type="text"
                value={editingBranch.address}
                onChange={(e) => setEditingBranch({ ...editingBranch, address: e.target.value })}
                placeholder="123 Main St"
              />
            </div>

            <div className="form-group">
              <label>City</label>
              <input
                type="text"
                value={editingBranch.city}
                onChange={(e) => setEditingBranch({ ...editingBranch, city: e.target.value })}
                placeholder="Tel Aviv"
              />
            </div>

            <div className="form-group">
              <label>Phone (optional)</label>
              <input
                type="tel"
                value={editingBranch.phone}
                onChange={(e) => setEditingBranch({ ...editingBranch, phone: e.target.value })}
                placeholder="+972-3-123-4567"
              />
            </div>

            <div className="form-group">
              <label>Timezone</label>
              <select
                value={editingBranch.timezone}
                onChange={(e) => setEditingBranch({ ...editingBranch, timezone: e.target.value })}
              >
                <option value="Asia/Jerusalem">Asia/Jerusalem (IST)</option>
                <option value="UTC">UTC</option>
                <option value="America/New_York">America/New_York (EST)</option>
                <option value="Europe/London">Europe/London (GMT)</option>
              </select>
            </div>

            {editingBranch.id && (
              <div className="form-group">
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <input
                    type="checkbox"
                    checked={editingBranch.active}
                    onChange={(e) => setEditingBranch({ ...editingBranch, active: e.target.checked })}
                  />
                  Active
                </label>
                <small style={{ color: '#888' }}>
                  Inactive branches won't appear in the branch selector
                </small>
              </div>
            )}

            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1.5rem' }}>
              <button 
                className="btn btn-primary" 
                onClick={handleSaveBranch}
                disabled={saving || !editingBranch.name || !editingBranch.address || !editingBranch.city}
              >
                {saving ? 'Saving...' : 'Save'}
              </button>
              <button className="btn" onClick={handleCancelEdit}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Branches List */}
      <div className="branches-list">
        {branches.length === 0 ? (
          <div className="card">
            <p style={{ textAlign: 'center', color: '#888' }}>
              No branches configured. Add your first branch to get started.
            </p>
          </div>
        ) : (
          branches.map((branch) => (
            <div key={branch.id} className="card" style={{ marginBottom: '1rem' }}>
              <div 
                style={{ 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  alignItems: 'flex-start',
                  cursor: 'pointer'
                }}
                onClick={() => toggleExpand(branch.id)}
              >
                <div>
                  <h3 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {branch.name}
                    {!branch.active && (
                      <span style={{
                        fontSize: '0.75rem',
                        padding: '0.2rem 0.5rem',
                        borderRadius: '4px',
                        background: 'rgba(255,0,0,0.1)',
                        color: '#ff6b6b'
                      }}>
                        Inactive
                      </span>
                    )}
                  </h3>
                  <p style={{ margin: '0.25rem 0', color: '#888' }}>
                    📍 {branch.address}, {branch.city}
                  </p>
                  {branch.phone && (
                    <p style={{ margin: '0.25rem 0', color: '#888' }}>
                      📞 {branch.phone}
                    </p>
                  )}
                </div>
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <button 
                    className="btn btn-sm"
                    onClick={(e) => {
                      e.stopPropagation()
                      handleEditBranch(branch)
                    }}
                  >
                    Edit
                  </button>
                  <span style={{ color: '#666' }}>
                    {expandedBranch === branch.id ? '▼' : '▶'}
                  </span>
                </div>
              </div>

              {/* Expanded Details - Shift Templates */}
              {expandedBranch === branch.id && (
                <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid #333' }}>
                  {/* Quick Links */}
                  <div style={{ marginBottom: '1.25rem' }}>
                    <Link 
                      to={`/hr/branches/${branch.id}/roles`}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.5rem',
                        padding: '0.5rem 1rem',
                        background: 'rgba(124, 58, 237, 0.15)',
                        color: '#a78bfa',
                        borderRadius: '6px',
                        textDecoration: 'none',
                        fontSize: '0.9rem'
                      }}
                    >
                      👥 Manage Roles
                    </Link>
                  </div>

                  <h4 style={{ margin: '0 0 0.75rem 0' }}>Shift Templates</h4>
                  {branch.shiftTemplates.length === 0 ? (
                    <p style={{ color: '#888', fontSize: '0.9rem' }}>
                      No shift templates configured for this branch.
                    </p>
                  ) : (
                    <div style={{ display: 'grid', gap: '0.5rem' }}>
                      {branch.shiftTemplates.map((template, idx) => (
                        <div 
                          key={idx}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '1rem',
                            padding: '0.75rem',
                            background: 'rgba(255,255,255,0.03)',
                            borderRadius: '6px'
                          }}
                        >
                          <span style={{
                            padding: '0.25rem 0.5rem',
                            borderRadius: '4px',
                            fontSize: '0.85rem',
                            background: template.shiftType === 'MORNING' 
                              ? 'rgba(255, 193, 7, 0.15)' 
                              : 'rgba(103, 58, 183, 0.15)',
                            color: template.shiftType === 'MORNING' ? '#f57c00' : '#7c4dff'
                          }}>
                            {template.shiftType === 'MORNING' ? '🌅 Morning' : '🌙 Evening'}
                          </span>
                          <span style={{ color: '#ccc' }}>
                            {formatTime(template.startTime)} - {formatTime(template.endTime)}
                          </span>
                          <span style={{ color: '#888', fontSize: '0.9rem' }}>
                            ({template.shiftHours}h)
                          </span>
                        </div>
                      ))}
                    </div>
                  )}

                  <div style={{ marginTop: '1rem' }}>
                    <p style={{ fontSize: '0.85rem', color: '#666' }}>
                      🌐 Timezone: {branch.timezone}
                    </p>
                  </div>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export default BranchSettingsPage

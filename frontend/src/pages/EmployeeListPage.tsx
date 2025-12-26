import React, { useEffect, useState, useMemo, useCallback } from 'react'
import { useAuth } from '../security/AuthContext'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getEmployeesInBranchApi,
  type Employee,
  createEmployeeApi,
  type CreateEmployeePayload,
  deleteEmployeeApi,
} from '../api/HrApiService'
import {
  getBranchRolesApi,
  type BranchRole,
} from '../api/BranchRoleApi'
import { getApiErrorMessage } from '../utils/apiError'

// Israeli minimum wage: ₪33.50/hr = 3350 agorot
const MIN_WAGE_AGOROT = 3350
const PAGE_SIZE_OPTIONS = [10, 25, 50]

/**
 * Validates an Israeli ID (Teudat Zehut) using the Luhn-like algorithm
 * @param id - 9-digit ID string (will be zero-padded if shorter)
 * @returns true if valid
 */
const validateIsraeliId = (id: string): boolean => {
  // Remove any non-digit characters
  const cleaned = id.replace(/\D/g, '')
  
  // Must be 1-9 digits
  if (cleaned.length === 0 || cleaned.length > 9) return false
  
  // Pad to 9 digits
  const padded = cleaned.padStart(9, '0')
  
  // Luhn-like algorithm for Israeli ID
  let sum = 0
  for (let i = 0; i < 9; i++) {
    let digit = parseInt(padded[i], 10)
    // Multiply odd positions (1-indexed) by 1, even by 2
    if ((i + 1) % 2 === 0) {
      digit *= 2
      if (digit > 9) digit -= 9
    }
    sum += digit
  }
  
  return sum % 10 === 0
}

const EmployeeListPage: React.FC = () => {
  const { branchId } = useParams<{ branchId: string }>()
  const navigate = useNavigate()
  const [employees, setEmployees] = useState<Employee[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [createSuccess, setCreateSuccess] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [showAddModal, setShowAddModal] = useState(false)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(25)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  
  // Dynamic roles from branch
  const [branchRoles, setBranchRoles] = useState<BranchRole[]>([])
  const [rolesLoading, setRolesLoading] = useState(false)
  
  // Validation states
  const [idError, setIdError] = useState<string | null>(null)
  const [wageError, setWageError] = useState<string | null>(null)

  const [newEmployee, setNewEmployee] = useState<Partial<CreateEmployeePayload>>(
    {
      id: 0,
      name: '',
      termsOfEmployment: '',
      startDate: Number(
        new Date().toISOString().slice(0, 10).replace(/-/g, ''),
      ),
      bankCode: 0,
      bankBranchCode: 0,
      bankAccount: 0,
      hourlyRate: 0,
      monthlyRate: 0,
      roles: [],
      password: '',
    },
  )

  const { employee } = useAuth()

  const loadEmployees = useCallback(async () => {
    if (!branchId) return
    setLoading(true)
    setError(null)
    try {
      const res = await getEmployeesInBranchApi(Number(branchId), page, size)
      const payload = res.data
      if (payload.totalPages > 0 && page >= payload.totalPages) {
        setPage(Math.max(payload.totalPages - 1, 0))
        return
      }
      if (payload.totalPages === 0 && page > 0) {
        setPage(0)
        return
      }
      setEmployees(payload.content)
      setTotalElements(payload.totalElements)
      setTotalPages(payload.totalPages)
    } catch (err: unknown) {
      console.error(err)
      setError(getApiErrorMessage(err, 'Unable to load employees right now'))
    } finally {
      setLoading(false)
    }
  }, [branchId, page, size])

  const loadBranchRoles = useCallback(async () => {
    if (!branchId) return
    setRolesLoading(true)
    try {
      // Only get active roles for employee assignment
      const res = await getBranchRolesApi(Number(branchId), true)
      setBranchRoles(res.data)
    } catch (err) {
      console.error('Failed to load branch roles:', err)
    } finally {
      setRolesLoading(false)
    }
  }, [branchId])

  // Get active roles sorted by sortOrder for display
  const activeRoles = useMemo(() => {
    return [...branchRoles].sort((a, b) => a.sortOrder - b.sortOrder)
  }, [branchRoles])

  useEffect(() => {
    // Wait for auth to be available and user to be HR before loading
    if (!employee) return
    if (!employee.isHRManager) {
      setError('Access denied: not an HR manager')
      return
    }
    void loadEmployees()
    void loadBranchRoles()
  }, [employee, loadEmployees, loadBranchRoles])

  const handleNextPage = () => {
    if (totalPages === 0 || page + 1 >= totalPages) return
    setPage((prev) => prev + 1)
  }

  const handlePrevPage = () => {
    if (page === 0) return
    setPage((prev) => Math.max(prev - 1, 0))
  }

  const handlePageSizeChange = (value: number) => {
    if (value === size) return
    setSize(value)
    setPage(0)
  }

  const displayTotalPages = totalPages > 0 ? totalPages : (totalElements > 0 ? 1 : 0)
  const displayPageNumber = displayTotalPages === 0 ? 0 : Math.min(page + 1, displayTotalPages)
  const disableNext = loading || totalPages === 0 || page + 1 >= totalPages
  const disablePrev = loading || page === 0

  const handleViewDetails = (id: number) => {
    navigate(`/hr/employees/${id}`)
  }

  // Validate Israeli ID on blur
  const handleIdBlur = () => {
    const idStr = String(newEmployee.id ?? '')
    if (idStr && idStr !== '0') {
      if (!validateIsraeliId(idStr)) {
        setIdError('Invalid Israeli ID (Teudat Zehut) - check digit failed')
      } else {
        setIdError(null)
      }
    } else {
      setIdError(null)
    }
  }

  // Validate hourly rate on change (minimum wage check)
  // Note: hourlyRateNis is stored in state as NIS, converted to agorot on submit
  const [hourlyRateNis, setHourlyRateNis] = useState<number>(0)
  const [monthlyRateNis, setMonthlyRateNis] = useState<number>(0)
  
  const MIN_WAGE_NIS = MIN_WAGE_AGOROT / 100 // ₪33.50
  
  const handleHourlyRateChange = (value: number) => {
    setHourlyRateNis(value)
    if (value > 0 && value < MIN_WAGE_NIS) {
      setWageError(`Hourly rate must be at least ₪${MIN_WAGE_NIS.toFixed(2)} (minimum wage)`)
    } else {
      setWageError(null)
    }
  }

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId) return
    
    // Validate ID
    const idStr = String(newEmployee.id ?? '')
    if (!validateIsraeliId(idStr)) {
      setCreateError('Invalid Israeli ID (Teudat Zehut)')
      return
    }
    
    // Validate minimum wage (hourlyRateNis is in NIS)
    if (hourlyRateNis > 0 && hourlyRateNis < MIN_WAGE_NIS) {
      setCreateError(`Hourly rate must be at least ₪${MIN_WAGE_NIS.toFixed(2)} (minimum wage)`)
      return
    }
    
    setCreating(true)
    setCreateError(null)
    try {
      const payload: CreateEmployeePayload = {
        id: newEmployee.id ?? 0,
        branchId: Number(branchId),
        name: newEmployee.name ?? '',
        termsOfEmployment: newEmployee.termsOfEmployment ?? '',
        startDate: newEmployee.startDate ?? 0,
        bankCode: newEmployee.bankCode ?? 0,
        bankBranchCode: newEmployee.bankBranchCode ?? 0,
        bankAccount: newEmployee.bankAccount ?? 0,
        hourlyRate: Math.round(hourlyRateNis * 100), // Convert NIS to agorot
        monthlyRate: Math.round(monthlyRateNis * 100), // Convert NIS to agorot
        roles: (newEmployee.roles as string[]) ?? [],
        password: newEmployee.password ?? '',
      }
      await createEmployeeApi(Number(branchId), payload)
      setCreateSuccess('Employee created successfully')
      await loadEmployees()
      setShowAddModal(false)
      // Reset form
      setNewEmployee((prev) => ({
        ...prev,
        id: 0,
        name: '',
        password: '',
        roles: [],
      }))
      setHourlyRateNis(0)
      setMonthlyRateNis(0)
      setWageError(null)
      setIdError(null)
    } catch (err) {
      console.error(err)
      setCreateError(getApiErrorMessage(err, 'Failed to create employee'))
    } finally {
      setCreating(false)
    }
  }

  const handleDelete = async (empId: number, empName: string) => {
    if (!branchId) return
    if (!window.confirm(`Are you sure you want to delete employee "${empName}" (#${empId})? This action cannot be undone.`)) {
      return
    }
    setDeletingId(empId)
    try {
      await deleteEmployeeApi(Number(branchId), empId)
      await loadEmployees()
    } catch (err) {
      console.error(err)
      alert(`Delete failed: ${getApiErrorMessage(err, 'Failed to delete employee')}`)
    } finally {
      setDeletingId(null)
    }
  }

  // Helper to get role display info
  const getRoleDisplay = (roleCode: string) => {
    const role = branchRoles.find(r => r.code === roleCode)
    return role ? { name: role.displayName, color: role.color } : { name: roleCode, color: '#666' }
  }

  if (!branchId) {
    return <p>Branch ID is missing in the URL.</p>
  }

  return (
    <div>
      {/* Header with Add Button */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <span className="bf-badge bf-badge-info">{totalElements.toLocaleString()} employees</span>
        </div>
        <button className="bf-btn bf-btn-primary" onClick={() => setShowAddModal(true)}>
          + Add Employee
        </button>
      </div>

      {loading && (
        <div className="bf-loading">
          <div className="bf-spinner"></div>
          <p>Loading employees...</p>
        </div>
      )}
      
      {error && (
        <div style={{
          padding: '16px',
          background: 'rgba(239, 68, 68, 0.1)',
          border: '1px solid rgba(239, 68, 68, 0.25)',
          borderRadius: '10px',
          color: '#ef4444',
          marginBottom: '20px'
        }}>
          {error}
        </div>
      )}

      {/* Employee Grid */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', 
        gap: '20px',
        marginBottom: '32px'
      }}>
        {employees.map((emp) => (
          <div 
            key={emp.id}
            className="bf-card"
            style={{ cursor: 'pointer', transition: 'all 0.2s' }}
            onClick={() => handleViewDetails(emp.id)}
          >
            <div className="bf-card-body">
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginBottom: '16px' }}>
                <div style={{
                  width: '48px',
                  height: '48px',
                  background: 'linear-gradient(135deg, #0ea5e9 0%, #22c55e 100%)',
                  borderRadius: '12px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '18px',
                  fontWeight: '700',
                  color: 'white'
                }}>
                  {emp.name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)}
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '16px', fontWeight: '600', color: 'var(--bf-text)' }}>
                    {emp.name}
                  </h3>
                  <p style={{ margin: '2px 0 0', fontSize: '13px', color: 'var(--bf-text-secondary)' }}>
                    ID: {emp.id}
                  </p>
                </div>
              </div>
              
              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '16px' }}>
                {emp.roles.length > 0 ? emp.roles.map((roleCode) => {
                  const { name, color } = getRoleDisplay(roleCode)
                  return (
                    <span 
                      key={roleCode}
                      style={{
                        display: 'inline-block',
                        padding: '4px 10px',
                        borderRadius: '6px',
                        background: `${color}20`,
                        border: `1px solid ${color}40`,
                        fontSize: '12px',
                        fontWeight: 500,
                        color: color
                      }}
                    >
                      {name}
                    </span>
                  )
                }) : (
                  <span className="bf-badge bf-badge-neutral">No roles assigned</span>
                )}
              </div>

              <div style={{ display: 'flex', gap: '8px' }}>
                <button 
                  className="bf-btn bf-btn-secondary bf-btn-sm"
                  style={{ flex: 1 }}
                  onClick={(e) => {
                    e.stopPropagation()
                    handleViewDetails(emp.id)
                  }}
                >
                  View Details
                </button>
                <button
                  className="bf-btn bf-btn-danger bf-btn-sm"
                  disabled={deletingId === emp.id}
                  onClick={(e) => {
                    e.stopPropagation()
                    handleDelete(emp.id, emp.name)
                  }}
                >
                  {deletingId === emp.id ? '...' : '🗑️'}
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: '12px',
        marginBottom: '32px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button
            className="bf-btn bf-btn-secondary bf-btn-sm"
            onClick={handlePrevPage}
            disabled={disablePrev}
          >
            ‹ Prev
          </button>
          <span style={{ fontSize: '14px', color: 'var(--bf-text-secondary)' }}>
            Page {displayPageNumber} of {displayTotalPages}
          </span>
          <button
            className="bf-btn bf-btn-secondary bf-btn-sm"
            onClick={handleNextPage}
            disabled={disableNext}
          >
            Next ›
          </button>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '14px', color: 'var(--bf-text-secondary)' }}>Rows per page</span>
          <select
            className="bf-form-input"
            value={size}
            onChange={(e) => handlePageSizeChange(Number(e.target.value))}
            style={{ width: '100px' }}
          >
            {PAGE_SIZE_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Add Employee Modal */}
      {showAddModal && (
        <div className="bf-modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="bf-modal" style={{ maxWidth: '600px' }} onClick={e => e.stopPropagation()}>
            <div className="bf-modal-header">
              <h2>Add New Employee</h2>
              <button className="bf-modal-close" onClick={() => setShowAddModal(false)}>×</button>
            </div>
            <form onSubmit={handleCreate}>
              <div className="bf-modal-body">
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                  <div className="bf-form-group">
                    <label className="bf-form-label">Employee ID (Teudat Zehut)</label>
                    <input
                      className="bf-form-input"
                      type="number"
                      value={newEmployee.id ?? 0}
                      onChange={(e) =>
                        setNewEmployee((prev) => ({
                          ...prev,
                          id: Number(e.target.value),
                        }))
                      }
                      onBlur={handleIdBlur}
                      required
                      style={idError ? { borderColor: '#ef4444' } : undefined}
                    />
                    {idError && <span style={{ color: '#ef4444', fontSize: '12px' }}>{idError}</span>}
                  </div>

                  <div className="bf-form-group">
                    <label className="bf-form-label">Full Name</label>
                    <input
                      className="bf-form-input"
                      type="text"
                      value={newEmployee.name ?? ''}
                      onChange={(e) =>
                        setNewEmployee((prev) => ({ ...prev, name: e.target.value }))
                      }
                      placeholder="שם מלא / Full Name"
                      required
                    />
                  </div>

                  <div className="bf-form-group">
                    <label className="bf-form-label">Hourly Rate (₪)</label>
                    <input
                      className="bf-form-input"
                      type="number"
                      step="0.01"
                      value={hourlyRateNis || ''}
                      onChange={(e) => handleHourlyRateChange(parseFloat(e.target.value) || 0)}
                      min={0}
                      placeholder={`Min ₪${MIN_WAGE_NIS.toFixed(2)}/hr`}
                      style={wageError ? { borderColor: '#ef4444' } : undefined}
                    />
                    {wageError && <span style={{ color: '#ef4444', fontSize: '12px' }}>{wageError}</span>}
                  </div>

                  <div className="bf-form-group">
                    <label className="bf-form-label">Monthly Rate (₪)</label>
                    <input
                      className="bf-form-input"
                      type="number"
                      step="0.01"
                      value={monthlyRateNis || ''}
                      onChange={(e) => setMonthlyRateNis(parseFloat(e.target.value) || 0)}
                      min={0}
                      placeholder="e.g. 6000"
                    />
                  </div>

                  <div className="bf-form-group">
                    <label className="bf-form-label">Bank Code</label>
                    <input
                      className="bf-form-input"
                      type="number"
                      value={newEmployee.bankCode ?? 0}
                      onChange={(e) =>
                        setNewEmployee((prev) => ({
                          ...prev,
                          bankCode: Number(e.target.value),
                        }))
                      }
                      placeholder="e.g. 12 (Hapoalim)"
                    />
                  </div>

                  <div className="bf-form-group">
                    <label className="bf-form-label">Bank Branch</label>
                    <input
                      className="bf-form-input"
                      type="number"
                      value={newEmployee.bankBranchCode ?? 0}
                      onChange={(e) =>
                        setNewEmployee((prev) => ({
                          ...prev,
                          bankBranchCode: Number(e.target.value),
                        }))
                      }
                    />
                  </div>

                  <div className="bf-form-group">
                    <label className="bf-form-label">Bank Account</label>
                    <input
                      className="bf-form-input"
                      type="number"
                      value={newEmployee.bankAccount ?? 0}
                      onChange={(e) =>
                        setNewEmployee((prev) => ({
                          ...prev,
                          bankAccount: Number(e.target.value),
                        }))
                      }
                    />
                  </div>

                  <div className="bf-form-group">
                    <label className="bf-form-label">Password</label>
                    <input
                      className="bf-form-input"
                      type="password"
                      value={newEmployee.password ?? ''}
                      onChange={(e) =>
                        setNewEmployee((prev) => ({
                          ...prev,
                          password: e.target.value,
                        }))
                      }
                      required
                      minLength={4}
                    />
                  </div>
                </div>

                <div className="bf-form-group" style={{ marginTop: '16px' }}>
                  <label className="bf-form-label">Roles</label>
                  {rolesLoading ? (
                    <p style={{ fontSize: '13px', color: 'var(--bf-text-secondary)' }}>Loading roles...</p>
                  ) : activeRoles.length === 0 ? (
                    <p style={{ fontSize: '13px', color: '#ef4444' }}>
                      No active roles defined for this branch.
                    </p>
                  ) : (
                    <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                      {activeRoles.map((role) => {
                        const isSelected = (newEmployee.roles as string[])?.includes(role.code) ?? false
                        return (
                          <label 
                            key={role.id} 
                            style={{ 
                              display: 'flex', 
                              alignItems: 'center', 
                              gap: '8px',
                              padding: '10px 14px',
                              borderRadius: '10px',
                              background: isSelected ? `${role.color}15` : 'var(--bf-surface-light)',
                              border: `2px solid ${isSelected ? role.color : 'var(--bf-border)'}`,
                              cursor: 'pointer',
                              transition: 'all 0.2s',
                            }}
                          >
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={(e) => {
                                setNewEmployee((prev) => {
                                  const currentRoles = (prev.roles as string[]) ?? []
                                  if (e.target.checked) {
                                    return { ...prev, roles: [...currentRoles, role.code] }
                                  } else {
                                    return { ...prev, roles: currentRoles.filter((r) => r !== role.code) }
                                  }
                                })
                              }}
                              style={{ accentColor: role.color }}
                            />
                            <span 
                              style={{ 
                                width: '10px',
                                height: '10px',
                                borderRadius: '50%',
                                background: role.color,
                              }} 
                            />
                            <span style={{ fontWeight: isSelected ? 600 : 400, fontSize: '13px' }}>
                              {role.displayName}
                            </span>
                          </label>
                        )
                      })}
                    </div>
                  )}
                </div>

                {createError && (
                  <div style={{
                    padding: '12px',
                    background: 'rgba(239, 68, 68, 0.1)',
                    border: '1px solid rgba(239, 68, 68, 0.25)',
                    borderRadius: '8px',
                    color: '#ef4444',
                    fontSize: '13px',
                    marginTop: '16px'
                  }}>
                    {createError}
                  </div>
                )}
              </div>
              <div className="bf-modal-footer">
                <button type="button" className="bf-btn bf-btn-secondary" onClick={() => setShowAddModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="bf-btn bf-btn-primary" disabled={creating || activeRoles.length === 0}>
                  {creating ? 'Creating...' : 'Create Employee'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Success Toast */}
      {createSuccess && (
        <div className="bf-toast-container">
          <div className="bf-toast" style={{ borderLeft: '3px solid #22c55e' }}>
            <div className="bf-toast-title">✅ Success</div>
            <div className="bf-toast-body">{createSuccess}</div>
          </div>
        </div>
      )}
    </div>
  )
}

export default EmployeeListPage

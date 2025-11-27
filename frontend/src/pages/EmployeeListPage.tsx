import React, { useEffect, useState } from 'react'
import { useAuth } from '../security/AuthContext'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getEmployeesInBranchApi,
  type Employee,
  createEmployeeApi,
  type CreateEmployeePayload,
} from '../api/HrApiService'

const EmployeeListPage: React.FC = () => {
  const { branchId } = useParams<{ branchId: string }>()
  const navigate = useNavigate()
  const [employees, setEmployees] = useState<Employee[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [createSuccess, setCreateSuccess] = useState<string | null>(null)

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

  const loadEmployees = async () => {
    if (!branchId) return
    setLoading(true)
    setError(null)
    try {
      const res = await getEmployeesInBranchApi(Number(branchId))
      setEmployees(res.data)
    } catch (err: any) {
      console.error(err)
      // show richer error info for debugging
      const status = err?.response?.status
      const body = err?.response?.data
      setError(status ? `Error ${status}: ${JSON.stringify(body)}` : (err?.message ?? 'Failed to load employees'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    // Wait for auth to be available and user to be HR before loading
    if (!employee) return
    if (!employee.isHRManager) {
      setError('Access denied: not an HR manager')
      return
    }
    void loadEmployees()
  }, [branchId, employee])

  const handleViewDetails = (id: number) => {
    navigate(`/hr/employees/${id}`)
  }

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId) return
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
        hourlyRate: newEmployee.hourlyRate ?? 0,
        monthlyRate: newEmployee.monthlyRate ?? 0,
        roles: (newEmployee.roles as string[]) ?? [],
        password: newEmployee.password ?? '',
      }
      await createEmployeeApi(Number(branchId), payload)
      setCreateSuccess('Employee created successfully')
      await loadEmployees()
      setNewEmployee((prev) => ({
        ...prev,
        id: 0,
        name: '',
        password: '',
      }))
    } catch (err: any) {
      console.error(err)
      setCreateError(err?.response?.data?.error ?? 'Failed to create employee')
    } finally {
      setCreating(false)
    }
  }

  if (!branchId) {
    return <p>Branch ID is missing in the URL.</p>
  }

  return (
    <div className="page">
      <h2>Employees in branch {branchId}</h2>
      {loading && <p>Loading employees…</p>}
      {error && <p className="form-error">{error}</p>}

      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', 
        gap: '1.5rem',
        marginBottom: '3rem'
      }}>
        {employees.map((emp) => (
          <div 
            key={emp.id}
            className="card"
            style={{ cursor: 'pointer', transition: 'transform 0.2s' }}
            onClick={() => handleViewDetails(emp.id)}
          >
            <h4 style={{ margin: '0 0 0.5rem 0' }}>{emp.name}</h4>
            <p style={{ margin: '0.25rem 0', fontSize: '0.9rem', color: '#999' }}>
              Employee #{emp.id}
            </p>
            <div style={{ marginTop: '0.75rem' }}>
              <p style={{ margin: '0.25rem 0', fontSize: '0.85rem' }}>
                <strong>Roles:</strong> {emp.roles.length > 0 ? emp.roles.join(', ') : '—'}
              </p>
              <p style={{ margin: '0.25rem 0', fontSize: '0.85rem' }}>
                <strong>Branch:</strong> {emp.branchId}
              </p>
            </div>
            <button 
              type="button" 
              style={{ marginTop: '0.75rem', width: '100%' }}
              onClick={(e) => {
                e.stopPropagation()
                handleViewDetails(emp.id)
              }}
            >
              View Details
            </button>
          </div>
        ))}
      </div>

      <section className="card" style={{ marginTop: '2rem' }}>
        <h3 className="card-title">Add employee</h3>
        <p className="card-subtitle">
          Quick form for HR to add a new employee to this branch. In a real
          system this would be a multi-step wizard, but this gives you the main
          fields.
        </p>

        <form onSubmit={handleCreate} className="form">
          <div className="form-grid">
            <label className="form-field">
              <span>Employee ID</span>
              <input
                type="number"
                value={newEmployee.id ?? 0}
                onChange={(e) =>
                  setNewEmployee((prev) => ({
                    ...prev,
                    id: Number(e.target.value),
                  }))
                }
                required
              />
            </label>

            <label className="form-field">
              <span>Name</span>
              <input
                type="text"
                value={newEmployee.name ?? ''}
                onChange={(e) =>
                  setNewEmployee((prev) => ({ ...prev, name: e.target.value }))
                }
                required
              />
            </label>

            <label className="form-field">
              <span>Hourly rate</span>
              <input
                type="number"
                value={newEmployee.hourlyRate ?? 0}
                onChange={(e) =>
                  setNewEmployee((prev) => ({
                    ...prev,
                    hourlyRate: Number(e.target.value),
                  }))
                }
              />
            </label>

            <label className="form-field">
              <span>Monthly rate</span>
              <input
                type="number"
                value={newEmployee.monthlyRate ?? 0}
                onChange={(e) =>
                  setNewEmployee((prev) => ({
                    ...prev,
                    monthlyRate: Number(e.target.value),
                  }))
                }
              />
            </label>

            <label className="form-field">
              <span>Bank code</span>
              <input
                type="number"
                value={newEmployee.bankCode ?? 0}
                onChange={(e) =>
                  setNewEmployee((prev) => ({
                    ...prev,
                    bankCode: Number(e.target.value),
                  }))
                }
              />
            </label>

            <label className="form-field">
              <span>Bank branch</span>
              <input
                type="number"
                value={newEmployee.bankBranchCode ?? 0}
                onChange={(e) =>
                  setNewEmployee((prev) => ({
                    ...prev,
                    bankBranchCode: Number(e.target.value),
                  }))
                }
              />
            </label>

            <label className="form-field">
              <span>Bank account</span>
              <input
                type="number"
                value={newEmployee.bankAccount ?? 0}
                onChange={(e) =>
                  setNewEmployee((prev) => ({
                    ...prev,
                    bankAccount: Number(e.target.value),
                  }))
                }
              />
            </label>

            <label className="form-field">
              <span>Password</span>
              <input
                type="password"
                value={newEmployee.password ?? ''}
                onChange={(e) =>
                  setNewEmployee((prev) => ({
                    ...prev,
                    password: e.target.value,
                  }))
                }
                required
              />
            </label>

            <label className="form-field" style={{ gridColumn: '1 / -1' }}>
              <span>Roles (select all that apply)</span>
              <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', marginTop: '0.5rem' }}>
                {['MANAGER', 'CASHIER', 'STOREKEEPER'].map((role) => (
                  <label key={role} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <input
                      type="checkbox"
                      checked={(newEmployee.roles as string[])?.includes(role) ?? false}
                      onChange={(e) => {
                        setNewEmployee((prev) => {
                          const currentRoles = (prev.roles as string[]) ?? []
                          if (e.target.checked) {
                            return {
                              ...prev,
                              roles: [...currentRoles, role],
                            }
                          } else {
                            return {
                              ...prev,
                              roles: currentRoles.filter((r) => r !== role),
                            }
                          }
                        })
                      }}
                    />
                    {role}
                  </label>
                ))}
              </div>
            </label>
          </div>

          {createError && <p className="form-error">{createError}</p>}
          {createSuccess && <p className="form-success">{createSuccess}</p>}

          <button type="submit" disabled={creating}>
            {creating ? 'Creating…' : 'Create employee'}
          </button>
        </form>
      </section>
    </div>
  )
}

export default EmployeeListPage

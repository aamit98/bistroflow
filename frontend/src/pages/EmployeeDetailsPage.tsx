// src/pages/EmployeeDetailsPage.tsx
import React, { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  getEmployeeDetailsApi,
  type EmployeeProfileResponse,
} from '../api/HrApiService'
import {
  formatWeekRange,
  getCurrentSundayISO,
  shiftWeek,
} from '../api/ScheduleApi'
import {
  getBranchRolesApi,
  agorotToNis,
  type BranchRole,
} from '../api/BranchRoleApi'
import './EmployeeDetailsPage.css'

const DAYS = [
  { code: 'SUNDAY', label: 'Sun' },
  { code: 'MONDAY', label: 'Mon' },
  { code: 'TUESDAY', label: 'Tue' },
  { code: 'WEDNESDAY', label: 'Wed' },
  { code: 'THURSDAY', label: 'Thu' },
  { code: 'FRIDAY', label: 'Fri' },
  { code: 'SATURDAY', label: 'Sat' },
] as const

const SHIFTS = ['MORNING', 'EVENING'] as const

const EmployeeDetailsPage: React.FC = () => {
  const { employeeId } = useParams<{ employeeId: string }>()
  const numericId = employeeId ? Number(employeeId) : NaN

  const [weekStart, setWeekStart] = useState(getCurrentSundayISO())
  const [profile, setProfile] = useState<EmployeeProfileResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [branchRoles, setBranchRoles] = useState<BranchRole[]>([])

  // Helper to get role display info
  const getRoleDisplay = (roleCode: string) => {
    const role = branchRoles.find(r => r.code === roleCode)
    return role ? { 
      name: role.displayName, 
      color: role.color,
      baseRate: role.baseHourlyRate,
      canSupervise: role.canSupervise,
      requiresCert: role.requiresCertification,
    } : { 
      name: roleCode, 
      color: '#666',
      baseRate: 0,
      canSupervise: false,
      requiresCert: false,
    }
  }

  useEffect(() => {
    if (!numericId) return

    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const response = await getEmployeeDetailsApi(numericId, weekStart)
        setProfile(response.data)
        
        // Load branch roles for display
        if (response.data.profile.branchId) {
          try {
            const rolesRes = await getBranchRolesApi(response.data.profile.branchId)
            setBranchRoles(rolesRes.data)
          } catch (err) {
            console.warn('Could not load branch roles', err)
          }
        }
      } catch (e: any) {
        console.error(e)
        const message = e?.response?.data?.error ?? 'Failed to load employee details'
        setError(message)
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [numericId, weekStart])

  const availabilityMap = useMemo(() => {
    const map = new Map<string, Set<string>>()
    profile?.availability.slots.forEach((slot) => {
      if (!slot.available) return
      const set = map.get(slot.dayOfWeek) ?? new Set<string>()
      set.add(slot.shiftType)
      map.set(slot.dayOfWeek, set)
    })
    return map
  }, [profile])

  const shiftsByDay = useMemo(() => {
    const grouped = new Map<string, EmployeeProfileResponse['schedule']['shifts']>()
    profile?.schedule.shifts.forEach((shift) => {
      const list = grouped.get(shift.shiftDate) ?? []
      list.push(shift)
      grouped.set(shift.shiftDate, list)
    })
    return Array.from(grouped.entries()).sort((a, b) => a[0].localeCompare(b[0]))
  }, [profile])

  const handleWeekShift = (delta: number) => {
    setWeekStart(shiftWeek(weekStart, delta))
  }

  if (!employeeId) {
    return <div className="page">Employee not found</div>
  }

  return (
    <div className="page employee-details-page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Employee #{employeeId}</h2>
          {profile && (
            <p className="page-subtitle">
              {profile.profile.name} · Branch {profile.profile.branchId}
            </p>
          )}
        </div>
        <div className="week-range">
          <button className="btn-outline" onClick={() => handleWeekShift(-1)}>
            ← Prev
          </button>
          <span>{formatWeekRange(weekStart)}</span>
          <button className="btn-outline" onClick={() => handleWeekShift(1)}>
            Next →
          </button>
        </div>
      </div>

      {loading && <p>Loading profile…</p>}
      {error && <p className="form-error">{error}</p>}

      {profile && !loading && !error && (
        <>
          <section className="card profile-hero">
            <div className="hero-main">
              <h3>{profile.profile.name}</h3>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginTop: '0.5rem' }}>
                {profile.profile.roles.length > 0 ? profile.profile.roles.map((roleCode) => {
                  const roleInfo = getRoleDisplay(roleCode)
                  return (
                    <span 
                      key={roleCode}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.35rem',
                        padding: '0.25rem 0.6rem',
                        borderRadius: '0.35rem',
                        background: `${roleInfo.color}25`,
                        border: `1.5px solid ${roleInfo.color}`,
                        fontSize: '0.85rem',
                        fontWeight: 500,
                      }}
                    >
                      <span 
                        style={{ 
                          width: '8px', 
                          height: '8px', 
                          borderRadius: '50%', 
                          background: roleInfo.color 
                        }} 
                      />
                      {roleInfo.name}
                      {roleInfo.canSupervise && <span title="Supervisor role">👑</span>}
                      {roleInfo.requiresCert && <span title="Requires certification">📜</span>}
                    </span>
                  )
                }) : (
                  <span style={{ color: '#888', fontStyle: 'italic' }}>No roles assigned</span>
                )}
              </div>
            </div>
            <div className="hero-stats">
              <div>
                <span className="stat-label">Availability submitted</span>
                <span className="stat-value">
                  {profile.availability.submitted ? 'Yes' : 'No'}
                </span>
              </div>
              <div>
                <span className="stat-label">Shifts this week</span>
                <span className="stat-value">{profile.schedule.shifts.length}</span>
              </div>
              <div>
                <span className="stat-label">Hourly rate</span>
                <span className="stat-value">₪{profile.profile.hourlyRate}</span>
              </div>
            </div>
          </section>

          <section className="profile-panels">
            <div className="card">
              <h3 className="card-title">Employment</h3>
              <ul>
                <li>Terms: {profile.profile.termsOfEmployment || '—'}</li>
                <li>Monthly rate: ₪{profile.profile.monthlyRate}</li>
                <li>Start date: {profile.profile.startDate}</li>
              </ul>
            </div>
            <div className="card">
              <h3 className="card-title">Bank details</h3>
              <ul>
                <li>Bank: {profile.profile.bankCode}</li>
                <li>Branch: {profile.profile.bankBranchCode}</li>
                <li>Account: {profile.profile.bankAccount}</li>
              </ul>
            </div>
            <div className="card">
              <h3 className="card-title">Status</h3>
              <ul>
                <li>HR Manager: {profile.profile.hrManager ? 'Yes' : 'No'}</li>
                <li>Employee ID: {profile.profile.employeeId}</li>
                <li>Branch ID: {profile.profile.branchId}</li>
              </ul>
            </div>
            <div className="card">
              <h3 className="card-title">Assigned Roles</h3>
              {profile.profile.roles.length === 0 ? (
                <p style={{ color: '#888', fontStyle: 'italic' }}>No roles assigned</p>
              ) : (
                <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                  {profile.profile.roles.map((roleCode) => {
                    const roleInfo = getRoleDisplay(roleCode)
                    return (
                      <li 
                        key={roleCode}
                        style={{ 
                          display: 'flex', 
                          alignItems: 'center', 
                          gap: '0.5rem',
                          marginBottom: '0.5rem',
                          padding: '0.5rem',
                          background: `${roleInfo.color}15`,
                          borderRadius: '0.35rem',
                          borderLeft: `3px solid ${roleInfo.color}`,
                        }}
                      >
                        <span style={{ fontWeight: 500 }}>{roleInfo.name}</span>
                        {roleInfo.baseRate > 0 && (
                          <span style={{ fontSize: '0.8rem', color: '#888' }}>
                            Base: {agorotToNis(roleInfo.baseRate)}/hr
                          </span>
                        )}
                        {roleInfo.canSupervise && (
                          <span title="Supervisor" style={{ fontSize: '0.8rem' }}>👑</span>
                        )}
                        {roleInfo.requiresCert && (
                          <span title="Certification required" style={{ fontSize: '0.8rem' }}>📜</span>
                        )}
                      </li>
                    )
                  })}
                </ul>
              )}
            </div>
          </section>

          <section className="card">
            <div className="card-header-row">
              <div>
                <h3 className="card-title">Weekly availability</h3>
                <p className="card-subtitle">
                  {formatWeekRange(weekStart)} ·{' '}
                  {profile.availability.submitted
                    ? 'Submitted by employee'
                    : 'No availability submitted for this week'}
                </p>
              </div>
            </div>
            <div className="availability-grid">
              <table>
                <thead>
                  <tr>
                    <th>Shift</th>
                    {DAYS.map((day) => (
                      <th key={day.code}>{day.label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {SHIFTS.map((shift) => (
                    <tr key={shift}>
                      <td className="shift-label">{shift}</td>
                      {DAYS.map((day) => {
                        const available = availabilityMap
                          .get(day.code)
                          ?.has(shift)
                        return (
                          <td
                            key={`${day.code}-${shift}`}
                            className={available ? 'avail-yes' : 'avail-no'}
                          >
                            {available ? '✓' : '—'}
                          </td>
                        )
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="card">
            <div className="card-header-row">
              <div>
                <h3 className="card-title">Scheduled shifts</h3>
                <p className="card-subtitle">
                  {profile.schedule.shifts.length > 0
                    ? 'Assignments generated for this week'
                    : 'No shifts assigned this week'}
                </p>
              </div>
            </div>
            <div className="shift-list">
              {shiftsByDay.length === 0 && (
                <p className="muted-text">No shifts scheduled for this week.</p>
              )}
              {shiftsByDay.map(([date, shifts]) => (
                <div key={date} className="shift-day">
                  <h4>{new Date(date + 'T00:00:00').toLocaleDateString()}</h4>
                  <div className="shift-chips">
                    {shifts.map((shift) => (
                      <span
                        key={shift.assignmentId ?? `${shift.shiftDate}-${shift.shiftType}`}
                        className={`shift-chip status-${shift.status.toLowerCase()}`}
                      >
                        {shift.shiftType} · {shift.status}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  )
}

export default EmployeeDetailsPage

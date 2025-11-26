// src/pages/EmployeeDetailsPage.tsx
import React, { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  getEmployeeDetailsApi,
  getEmployeeAvailabilityApi,
  updateEmployeeAvailabilityApi,
  type Employee,
  type DayCode,
  type EmployeeAvailability,
} from '../api/HrApiService'

type TabKey = 'profile' | 'availability' | 'schedule'

interface DayAvailabilityState {
  day: DayCode
  label: string
  enabled: boolean
  startTime: string
  endTime: string
}

const dayOrder: { day: DayCode; label: string }[] = [
  { day: 'MONDAY', label: 'Mon' },
  { day: 'TUESDAY', label: 'Tue' },
  { day: 'WEDNESDAY', label: 'Wed' },
  { day: 'THURSDAY', label: 'Thu' },
  { day: 'FRIDAY', label: 'Fri' },
  { day: 'SATURDAY', label: 'Sat' },
  { day: 'SUNDAY', label: 'Sun' },
]

function getMondayOfWeek(date: Date): Date {
  const d = new Date(date)
  const day = (d.getDay() + 6) % 7 // 0=Sun -> 6, 1=Mon -> 0, ...
  d.setDate(d.getDate() - day)
  d.setHours(0, 0, 0, 0)
  return d
}

function toIsoDate(d: Date): string {
  return d.toISOString().slice(0, 10)
}

const EmployeeDetailsPage: React.FC = () => {
  const { employeeId } = useParams<{ employeeId: string }>()
  const numericId = employeeId ? Number(employeeId) : NaN

  const [activeTab, setActiveTab] = useState<TabKey>('profile')

  const [employee, setEmployee] = useState<Employee | null>(null)
  const [loadingEmployee, setLoadingEmployee] = useState(true)
  const [employeeError, setEmployeeError] = useState<string | null>(null)

  const [weekStart, setWeekStart] = useState<Date>(() =>
    getMondayOfWeek(new Date()),
  )

  const [availabilityRows, setAvailabilityRows] = useState<DayAvailabilityState[]>(
    () =>
      dayOrder.map(({ day, label }) => ({
        day,
        label,
        enabled: false,
        startTime: '09:00',
        endTime: '17:00',
      })),
  )
  const [loadingAvailability, setLoadingAvailability] = useState(false)
  const [availabilityError, setAvailabilityError] = useState<string | null>(null)
  const [savingAvailability, setSavingAvailability] = useState(false)
  const [saveMessage, setSaveMessage] = useState<string | null>(null)

  const weekStartIso = useMemo(() => toIsoDate(weekStart), [weekStart])

  // Load employee profile
  useEffect(() => {
    if (!numericId) return

    const load = async () => {
      setLoadingEmployee(true)
      setEmployeeError(null)
      try {
        const response = await getEmployeeDetailsApi(numericId)
        setEmployee(response.data)
      } catch (e) {
        console.error(e)
        setEmployeeError('Failed to load employee details')
      } finally {
        setLoadingEmployee(false)
      }
    }

    load()
  }, [numericId])

  // Load availability for current week
  useEffect(() => {
    if (!numericId) return

    const load = async () => {
      setLoadingAvailability(true)
      setAvailabilityError(null)
      setSaveMessage(null)

      try {
        const response = await getEmployeeAvailabilityApi(
          numericId,
          weekStartIso,
        )
        const data: EmployeeAvailability = response.data

        const byDay = new Map<DayCode, { startTime: string; endTime: string }>()
        data.slots.forEach((slot) => {
          byDay.set(slot.dayOfWeek, {
            startTime: slot.startTime,
            endTime: slot.endTime,
          })
        })

        setAvailabilityRows(
          dayOrder.map(({ day, label }) => {
            const slot = byDay.get(day)
            if (!slot) {
              return {
                day,
                label,
                enabled: false,
                startTime: '09:00',
                endTime: '17:00',
              }
            }
            return {
              day,
              label,
              enabled: true,
              startTime: slot.startTime,
              endTime: slot.endTime,
            }
          }),
        )
      } catch (e) {
        console.error(e)
        setAvailabilityError('No availability found for this week')
        // keep default rows, but mark as not enabled
        setAvailabilityRows(
          dayOrder.map(({ day, label }) => ({
            day,
            label,
            enabled: false,
            startTime: '09:00',
            endTime: '17:00',
          })),
        )
      } finally {
        setLoadingAvailability(false)
      }
    }

    load()
  }, [numericId, weekStartIso])

  const handleWeekChange = (deltaWeeks: number) => {
    const next = new Date(weekStart)
    next.setDate(next.getDate() + deltaWeeks * 7)
    setWeekStart(getMondayOfWeek(next))
  }

  const handleRowChange = (
    idx: number,
    partial: Partial<DayAvailabilityState>,
  ) => {
    setAvailabilityRows((prev) =>
      prev.map((row, i) => (i === idx ? { ...row, ...partial } : row)),
    )
  }

  const handleSaveAvailability = async () => {
    if (!numericId) return

    setSavingAvailability(true)
    setAvailabilityError(null)
    setSaveMessage(null)

    const payload: EmployeeAvailability = {
      employeeId: numericId,
      weekStart: weekStartIso,
      slots: availabilityRows
        .filter((r) => r.enabled)
        .map((r) => ({
          dayOfWeek: r.day,
          startTime: r.startTime,
          endTime: r.endTime,
        })),
    }

    try {
      await updateEmployeeAvailabilityApi(numericId, payload)
      setSaveMessage('Availability saved successfully')
    } catch (e) {
      console.error(e)
      setAvailabilityError('Failed to save availability')
    } finally {
      setSavingAvailability(false)
    }
  }

  const formattedWeekRange = useMemo(() => {
    const end = new Date(weekStart)
    end.setDate(end.getDate() + 6)
    return `${weekStartIso} → ${toIsoDate(end)}`
  }, [weekStart, weekStartIso])

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2 className="page-title">
            Employee {employee ? `– ${employee.name}` : `#${employeeId}`}
          </h2>
          {employee && (
            <p className="page-subtitle">
              Branch #{employee.branchId} ·{' '}
              {employee.roles && employee.roles.length > 0
                ? employee.roles.join(', ')
                : 'No roles assigned'}
            </p>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="tabs">
        <button
          className={`tab ${activeTab === 'profile' ? 'tab-active' : ''}`}
          onClick={() => setActiveTab('profile')}
        >
          Profile
        </button>
        <button
          className={`tab ${activeTab === 'availability' ? 'tab-active' : ''}`}
          onClick={() => setActiveTab('availability')}
        >
          Availability
        </button>
        <button
          className={`tab ${activeTab === 'schedule' ? 'tab-active' : ''}`}
          onClick={() => setActiveTab('schedule')}
        >
          Schedule
        </button>
      </div>

      {/* Tab contents */}
      {activeTab === 'profile' && (
        <section className="card">
          {loadingEmployee && <p>Loading employee…</p>}
          {employeeError && (
            <p className="error-text">{employeeError}</p>
          )}
          {employee && !loadingEmployee && !employeeError && (
            <div className="profile-grid">
              <div>
                <h3 className="card-title">Basic info</h3>
                <p>
                  <strong>ID:</strong> {employee.id}
                </p>
                <p>
                  <strong>Branch:</strong> {employee.branchId}
                </p>
                <p>
                  <strong>HR manager:</strong>{' '}
                  {employee.isHRManager ? 'Yes' : 'No'}
                </p>
                <p>
                  <strong>Roles:</strong>{' '}
                  {employee.roles && employee.roles.length > 0
                    ? employee.roles.join(', ')
                    : '—'}
                </p>
                <p>
                  <strong>Start date:</strong> {employee.startDate ?? '—'}
                </p>
              </div>

              <div>
                <h3 className="card-title">Employment</h3>
                <p>
                  <strong>Hourly rate:</strong>{' '}
                  {employee.hourlyRate?.toFixed(2) ?? '—'}
                </p>
                <p>
                  <strong>Monthly rate:</strong>{' '}
                  {employee.monthlyRate?.toFixed(2) ?? '—'}
                </p>
                <p>
                  <strong>Terms:</strong>{' '}
                  {employee.termsOfEmployment ?? '—'}
                </p>
              </div>

              <div>
                <h3 className="card-title">Bank</h3>
                <p>
                  <strong>Bank code:</strong> {employee.bankCode}
                </p>
                <p>
                  <strong>Branch code:</strong> {employee.bankBranchCode}
                </p>
                <p>
                  <strong>Account:</strong> {employee.bankAccount}
                </p>
              </div>
            </div>
          )}
        </section>
      )}

      {activeTab === 'availability' && (
        <section className="card">
          <div className="card-header-row">
            <div>
              <h3 className="card-title">Weekly availability</h3>
              <p className="card-subtitle">{formattedWeekRange}</p>
            </div>
            <div className="week-switcher">
              <button
                className="btn-outline"
                onClick={() => handleWeekChange(-1)}
              >
                ← Previous week
              </button>
              <button
                className="btn-outline"
                onClick={() => handleWeekChange(1)}
              >
                Next week →
              </button>
            </div>
          </div>

          {loadingAvailability && <p>Loading availability…</p>}
          {availabilityError && (
            <p className="error-text">{availabilityError}</p>
          )}
          {saveMessage && (
            <p className="success-text">{saveMessage}</p>
          )}

          <div className="availability-table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Day</th>
                  <th>Available</th>
                  <th>From</th>
                  <th>To</th>
                </tr>
              </thead>
              <tbody>
                {availabilityRows.map((row, idx) => (
                  <tr key={row.day}>
                    <td>{row.label}</td>
                    <td>
                      <input
                        type="checkbox"
                        checked={row.enabled}
                        onChange={(e) =>
                          handleRowChange(idx, { enabled: e.target.checked })
                        }
                      />
                    </td>
                    <td>
                      <input
                        type="time"
                        className="time-input"
                        value={row.startTime}
                        onChange={(e) =>
                          handleRowChange(idx, { startTime: e.target.value })
                        }
                        disabled={!row.enabled}
                      />
                    </td>
                    <td>
                      <input
                        type="time"
                        className="time-input"
                        value={row.endTime}
                        onChange={(e) =>
                          handleRowChange(idx, { endTime: e.target.value })
                        }
                        disabled={!row.enabled}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="card-footer-row">
            <button
              className="btn-primary"
              onClick={handleSaveAvailability}
              disabled={savingAvailability}
            >
              {savingAvailability ? 'Saving…' : 'Save availability'}
            </button>
          </div>
        </section>
      )}

      {activeTab === 'schedule' && (
        <section className="card">
          <h3 className="card-title">Schedule</h3>
          <p className="card-subtitle">
            Coming next – this tab will show the actual assigned shifts for this
            employee, based on the weekly branch schedule.
          </p>
        </section>
      )}
    </div>
  )
}

export default EmployeeDetailsPage

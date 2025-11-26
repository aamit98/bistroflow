import React, { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import {
  getBranchSchedule,
  publishBranchSchedule,
  type BranchSchedule,
  type ShiftCell,
  type DayOfWeekCode,
  type ShiftType,
  getNextSundayISO,
  formatWeekRange,
  shiftWeek,
} from '../api/ScheduleApi'

const DAYS: DayOfWeekCode[] = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
]

const SHIFTS: ShiftType[] = ['MORNING', 'EVENING']

const HrBranchSchedulePage: React.FC = () => {
  const { branchId } = useParams<{ branchId: string }>()
  const [searchParams, setSearchParams] = useSearchParams()
  
  const weekStartParam = searchParams.get('weekStart')
  const [weekStart, setWeekStart] = useState(weekStartParam || getNextSundayISO())
  
  const [schedule, setSchedule] = useState<BranchSchedule | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [publishing, setPublishing] = useState(false)
  const [publishSuccess, setPublishSuccess] = useState<string | null>(null)

  // Update URL when week changes
  useEffect(() => {
    setSearchParams({ weekStart })
  }, [weekStart, setSearchParams])

  useEffect(() => {
    if (!branchId) return

    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const res = await getBranchSchedule(parseInt(branchId), weekStart)
        setSchedule(res.data)
      } catch (e: any) {
        console.error(e)
        const errorMsg = e.response?.data?.error || 'Failed to load schedule'
        setError(errorMsg)
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [branchId, weekStart])

  const goToNextWeek = () => setWeekStart(shiftWeek(weekStart, 1))
  const goToPrevWeek = () => setWeekStart(shiftWeek(weekStart, -1))
  const goToDefaultWeek = () => setWeekStart(getNextSundayISO())

  const handlePublish = async () => {
    if (!branchId || schedule?.published) return
    
    const confirmed = window.confirm(
      `Are you sure you want to publish the schedule for ${formatWeekRange(weekStart)}?\n\nThis will lock employee availability for this week.`
    )
    if (!confirmed) return

    setPublishing(true)
    setPublishSuccess(null)
    try {
      const res = await publishBranchSchedule(parseInt(branchId), weekStart)
      setPublishSuccess(`Schedule published at ${new Date(res.data.publishedAt).toLocaleString()}`)
      // Reload schedule to get updated status
      const updated = await getBranchSchedule(parseInt(branchId), weekStart)
      setSchedule(updated.data)
    } catch (e: any) {
      console.error(e)
      const errorMsg = e.response?.data?.error || 'Failed to publish schedule'
      setError(errorMsg)
    } finally {
      setPublishing(false)
    }
  }

  // Get shift cell for a specific day and shift type
  const getShiftCell = (day: DayOfWeekCode, shift: ShiftType): ShiftCell | undefined => {
    return schedule?.shifts.find(
      (s) => s.dayOfWeek === day && s.shiftType === shift
    )
  }

  // Render a single shift cell
  const renderShiftCell = (day: DayOfWeekCode, shift: ShiftType) => {
    const cell = getShiftCell(day, shift)
    if (!cell) {
      return <div className="shift-cell shift-cell-empty">No data</div>
    }

    const { totalRequired, totalAssigned, roleConstraints, assignedEmployees } = cell
    const isFull = totalAssigned >= totalRequired
    const isOverstaffed = totalAssigned > totalRequired
    const isUnderstaffed = totalAssigned < totalRequired

    return (
      <div
        className={`shift-cell ${
          isOverstaffed
            ? 'shift-cell-over'
            : isUnderstaffed
            ? 'shift-cell-under'
            : 'shift-cell-ok'
        }`}
      >
        {/* Staffing summary */}
        <div className="shift-summary">
          <span className="shift-count">
            {totalAssigned}/{totalRequired}
          </span>
          <span className="shift-status">
            {isOverstaffed && '⚠️ Over'}
            {isUnderstaffed && '⚠️ Under'}
            {isFull && !isOverstaffed && '✓ Full'}
          </span>
        </div>

        {/* Role breakdown */}
        <div className="shift-roles">
          {roleConstraints.map((rc) => (
            <div
              key={rc.role}
              className={`role-badge ${
                rc.assignedCount >= rc.requiredCount
                  ? 'role-badge-ok'
                  : 'role-badge-need'
              }`}
            >
              <span className="role-name">{rc.role}</span>
              <span className="role-count">
                {rc.assignedCount}/{rc.requiredCount}
              </span>
            </div>
          ))}
        </div>

        {/* Assigned employees */}
        {assignedEmployees.length > 0 && (
          <div className="shift-employees">
            {assignedEmployees.map((emp) => (
              <div key={`${emp.employeeId}-${emp.role}`} className="employee-tag">
                {emp.name}
                <span className="employee-role">({emp.role})</span>
              </div>
            ))}
          </div>
        )}
      </div>
    )
  }

  if (!branchId) {
    return <div className="page">Invalid branch ID</div>
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Branch {branchId} Schedule</h2>
          <p className="page-subtitle">
            View and manage shift assignments for the week.
          </p>
        </div>
      </div>

      {/* Week navigation */}
      <div className="card">
        <div className="week-nav">
          <button className="btn-outline" onClick={goToPrevWeek}>
            ← Previous
          </button>
          <div className="week-nav-center">
            <span className="week-label">Week of</span>
            <strong>{formatWeekRange(weekStart)}</strong>
            {schedule?.published && (
              <span className="published-badge">✓ Published</span>
            )}
          </div>
          <button className="btn-outline" onClick={goToNextWeek}>
            Next →
          </button>
        </div>
        <div style={{ textAlign: 'center', marginTop: '0.5rem' }}>
          <button
            className="btn-link"
            onClick={goToDefaultWeek}
            style={{ fontSize: '0.85rem' }}
          >
            Go to next week
          </button>
        </div>
      </div>

      {/* Status and actions */}
      <div className="card">
        <div className="card-header-row">
          <div>
            {schedule?.published ? (
              <p className="success-text">
                ✓ Schedule published
                {schedule.publishedAt && (
                  <> on {new Date(schedule.publishedAt).toLocaleDateString()}</>
                )}
              </p>
            ) : (
              <p style={{ color: '#9ca3af', fontSize: '0.9rem' }}>
                Schedule not yet published. Employees can still edit availability.
              </p>
            )}
          </div>
          <div>
            {!schedule?.published && (
              <button
                className="btn-primary"
                onClick={handlePublish}
                disabled={publishing || loading}
              >
                {publishing ? 'Publishing…' : 'Publish Schedule'}
              </button>
            )}
          </div>
        </div>
        {publishSuccess && (
          <p className="success-text" style={{ marginTop: '0.5rem' }}>
            {publishSuccess}
          </p>
        )}
      </div>

      {/* Schedule grid */}
      <div className="card" style={{ marginTop: '1rem' }}>
        {loading && <p>Loading schedule…</p>}
        {error && <p className="form-error">{error}</p>}

        {!loading && schedule && (
          <div className="schedule-grid-wrapper">
            <table className="schedule-grid">
              <thead>
                <tr>
                  <th>Shift</th>
                  {DAYS.map((day) => (
                    <th key={day}>{day.slice(0, 3)}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {SHIFTS.map((shift) => (
                  <tr key={shift}>
                    <td className="shift-label">{shift}</td>
                    {DAYS.map((day) => (
                      <td key={day}>{renderShiftCell(day, shift)}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Legend */}
      <div className="card" style={{ marginTop: '1rem' }}>
        <h3 className="card-title">Legend</h3>
        <div className="legend-grid">
          <div className="legend-item">
            <span className="legend-color legend-ok"></span>
            <span>Fully staffed</span>
          </div>
          <div className="legend-item">
            <span className="legend-color legend-under"></span>
            <span>Understaffed</span>
          </div>
          <div className="legend-item">
            <span className="legend-color legend-over"></span>
            <span>Overstaffed</span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default HrBranchSchedulePage

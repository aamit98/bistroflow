import React, { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  getBranchSchedule,
  publishBranchSchedule,
  deleteAssignment,
  type BranchSchedule,
  type ShiftCell,
  type DayOfWeekCode,
  type ShiftType,
  getNextSundayISO,
  formatWeekRange,
  shiftWeek,
  normalizeToSundayISO,
  getShiftCandidates,
  createScheduleAssignment,
  type ShiftAssignmentCandidate
} from '../api/ScheduleApi'
import ScheduleBuilder from '../components/ScheduleBuilder'
import ScheduleGenerator from '../components/ScheduleGenerator'
import './HrBranchSchedulePage.css'

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
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  
  const weekStartParam = searchParams.get('weekStart')
  const [weekStart, setWeekStart] = useState(() =>
    normalizeToSundayISO(weekStartParam || getNextSundayISO()),
  )
  const [activeTab, setActiveTab] = useState<'setup' | 'generate' | 'view'>('view')
  
  const [schedule, setSchedule] = useState<BranchSchedule | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [publishing, setPublishing] = useState(false)
  const [publishSuccess, setPublishSuccess] = useState<string | null>(null)

  const [selectedShift, setSelectedShift] = useState<{
    day: DayOfWeekCode
    shiftType: ShiftType
    shiftDate: string
  } | null>(null)

  const [candidates, setCandidates] = useState<ShiftAssignmentCandidate[] | null>(null)
  const [loadingCandidates, setLoadingCandidates] = useState(false)
  const [assignError, setAssignError] = useState<string | null>(null)
  const [assigningId, setAssigningId] = useState<number | null>(null)
  const [removingId, setRemovingId] = useState<number | null>(null)
  const [generatorRefreshTrigger, setGeneratorRefreshTrigger] = useState(0)

  const toErrorMessage = (err: unknown): string => {
    if (err && typeof err === 'object' && 'response' in err) {
      const response = (err as { response?: { data?: { error?: string } } }).response
      if (response?.data?.error) return String(response.data.error)
    }
    if (err instanceof Error) return err.message
    return 'Something went wrong'
  }
  const reloadSchedule = async () => {
    if (!branchId) return
    setLoading(true)
    setError(null)
    try {
      const res = await getBranchSchedule(parseInt(branchId, 10), weekStart)
      setSchedule(res.data)
    } catch (err: unknown) {
      console.error(err)
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { error?: string } } }).response
        if (response?.data?.error) {
          setError(response.data.error)
          return
        }
      }
      setError('Failed to load schedule')
    } finally {
      setLoading(false)
    }
  }
  // Update URL when week changes
  useEffect(() => {
    setSearchParams({ weekStart })
  }, [weekStart, setSearchParams])
  useEffect(() => {
    void reloadSchedule()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branchId, weekStart])

  const handleWeekChange = (nextIso: string) => {
    setWeekStart(normalizeToSundayISO(nextIso))
  }

  const goToNextWeek = () => setWeekStart(shiftWeek(weekStart, 1))
  const goToPrevWeek = () => setWeekStart(shiftWeek(weekStart, -1))
  const goToDefaultWeek = () => setWeekStart(normalizeToSundayISO(getNextSundayISO()))

  const handlePublish = async () => {
    if (!branchId || schedule?.published) return
    
    const confirmed = window.confirm(
      `Are you sure you want to publish the schedule for ${formatWeekRange(weekStart)}?\n\nThis will lock employee availability for this week.`
    )
    if (!confirmed) return

    setPublishing(true)
    setPublishSuccess(null)
    try {
      const res = await publishBranchSchedule(parseInt(branchId, 10), weekStart)
      setPublishSuccess(`Schedule published at ${new Date(res.data.publishedAt).toLocaleString()}`)
      // Reload schedule to get updated status
      await reloadSchedule()
    } catch (err: unknown) {
      console.error(err)
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { error?: string } } }).response
        if (response?.data?.error) {
          setError(response.data.error)
          return
        }
      }
      setError('Failed to publish schedule')
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


    const openShiftAssignment = async (day: DayOfWeekCode, shiftType: ShiftType) => {
    if (!branchId || !schedule) return

    const cell = getShiftCell(day, shiftType)
    if (!cell) return

    setSelectedShift({
      day,
      shiftType,
      shiftDate: cell.shiftDate,
    })
    setLoadingCandidates(true)
    setAssignError(null)

    try {
      const res = await getShiftCandidates(
        parseInt(branchId, 10),
        cell.shiftDate,
        shiftType
      )
      setCandidates(res.data)
    } catch (err) {
      setAssignError(toErrorMessage(err))
      setCandidates(null)
    } finally {
      setLoadingCandidates(false)
    }
  }

  const handleAssign = async (employeeId: number) => {
    if (!branchId || !selectedShift || !schedule) return

    const cell = getShiftCell(selectedShift.day, selectedShift.shiftType)
    if (!cell) return

    setAssigningId(employeeId)
    setAssignError(null)

    try {
      const res = await createScheduleAssignment(parseInt(branchId, 10), {
        employeeId,
        shiftDate: cell.shiftDate,
        shiftType: selectedShift.shiftType,
      })

      console.debug('[Schedule] assignment created', res.data)

      // רענון הטבלה אחרי שיבוץ
      const updated = await getBranchSchedule(parseInt(branchId, 10), schedule.weekStart)
      setSchedule(updated.data)

      // ריענון המועמדים לאותה משמרת
      const candRes = await getShiftCandidates(
        parseInt(branchId, 10),
        cell.shiftDate,
        selectedShift.shiftType
      )
      setCandidates(candRes.data)
    } catch (err) {
      setAssignError(toErrorMessage(err))
    } finally {
      setAssigningId(null)
    }
  }

  const handleRemoveAssignment = async (assignmentId: number, employeeName: string) => {
    if (!branchId || schedule?.published) return
    
    const confirmed = window.confirm(
      `Remove ${employeeName} from this shift?`
    )
    if (!confirmed) return

    setRemovingId(assignmentId)
    try {
      await deleteAssignment(assignmentId)
      // Refresh the schedule
      await reloadSchedule()
      
      // Also refresh candidates if drawer is open (to update conflict flags)
      if (selectedShift) {
        const cell = getShiftCell(selectedShift.day, selectedShift.shiftType)
        if (cell) {
          const candRes = await getShiftCandidates(
            parseInt(branchId, 10),
            cell.shiftDate,
            selectedShift.shiftType
          )
          setCandidates(candRes.data)
        }
      }
    } catch (err) {
      console.error('Failed to remove assignment:', err)
      setError(toErrorMessage(err))
    } finally {
      setRemovingId(null)
    }
  }



  // Render a single shift cell
  const renderShiftCell = (day: DayOfWeekCode, shift: ShiftType) => {
    const cell = getShiftCell(day, shift)
    if (!cell) {
      return <div className="shift-cell shift-cell-empty">No data</div>
    }

    const { totalRequired, totalAssigned, roleConstraints, assignedEmployees } = cell
    
    // Handle case when no constraints are defined
    const hasConstraints = totalRequired > 0
    const isOverstaffed = hasConstraints && totalAssigned > totalRequired
    const isUnderstaffed = hasConstraints && totalAssigned < totalRequired
    const isNoConstraints = !hasConstraints && totalAssigned === 0

    const handleClick = () => {
      if (schedule?.published) return // לא מאפשרים שינוי אחרי פרסום
      void openShiftAssignment(day, shift)
    }

    // Determine status text
    let statusText = ''
    if (!hasConstraints && totalAssigned === 0) {
      statusText = 'Setup needed'
    } else if (!hasConstraints && totalAssigned > 0) {
      statusText = 'No requirements set'
    } else if (isOverstaffed) {
      statusText = 'Overstaffed'
    } else if (isUnderstaffed) {
      statusText = 'Understaffed'
    } else {
      statusText = 'Balanced'
    }

    return (
      <div
        className={`shift-cell ${
          isNoConstraints
            ? 'shift-cell-setup'
            : isOverstaffed
            ? 'shift-cell-over'
            : isUnderstaffed
            ? 'shift-cell-under'
            : 'shift-cell-ok'
        }`}
        style={{ cursor: schedule?.published ? 'default' : 'pointer' }}
        onClick={handleClick}
      >
        {/* Staffing summary */}
        <div className="shift-summary">
          {hasConstraints ? (
            <span className="shift-count">
              {totalAssigned}/{totalRequired}
            </span>
          ) : totalAssigned > 0 ? (
            <span className="shift-count">
              {totalAssigned} assigned
            </span>
          ) : (
            <span className="shift-count shift-count-empty">
              —
            </span>
          )}
          <span className={`shift-status ${isNoConstraints ? 'shift-status-warning' : ''}`}>
            {statusText}
          </span>
        </div>

        {/* Roles - only show if there are constraints */}
        {roleConstraints.length > 0 && (
          <div className="shift-roles">
            {roleConstraints.map((rc) => (
              <span key={rc.role} className="shift-role-pill">
                {rc.role}: {rc.assignedCount}/{rc.requiredCount}
              </span>
            ))}
          </div>
        )}

        {/* No constraints hint */}
        {!hasConstraints && roleConstraints.length === 0 && (
          <div className="shift-hint">
            <small>Click to assign or go to Setup tab</small>
          </div>
        )}

        {/* Assigned employees */}
        {assignedEmployees.length > 0 && (
          <div className="shift-assigned">
            {assignedEmployees.map((emp, idx) => (
              <div key={emp.assignmentId} className="shift-employee-pill">
                <span className="shift-employee-name">
                  {idx === 0 ? 'Lead: ' : ''}
                  {emp.name} ({emp.role})
                </span>
                {!schedule?.published && emp.assignmentId && (
                  <button
                    className="shift-employee-remove"
                    onClick={(e) => {
                      e.stopPropagation()
                      void handleRemoveAssignment(emp.assignmentId!, emp.name)
                    }}
                    disabled={removingId === emp.assignmentId}
                    title="Remove from shift"
                  >
                    {removingId === emp.assignmentId ? '...' : '×'}
                  </button>
                )}
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
    <div className="page schedule-page">
      <div className="page-header">
        <div>
          <h2 className="page-title">🍽️ Schedule Management - Branch {branchId}</h2>
          <p className="page-subtitle">
            Define staffing rules, generate optimal schedules, and publish to your team.
          </p>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="tab-navigation">
        <button
          className={`tab-btn ${activeTab === 'setup' ? 'active' : ''}`}
          onClick={() => setActiveTab('setup')}
        >
          ⚙️ Setup
        </button>
        <button
          className={`tab-btn ${activeTab === 'generate' ? 'active' : ''}`}
          onClick={() => {
            setActiveTab('generate')
            setGeneratorRefreshTrigger(prev => prev + 1)
          }}
        >
          ⚡ Generate
        </button>
        <button
          className={`tab-btn ${activeTab === 'view' ? 'active' : ''}`}
          onClick={() => setActiveTab('view')}
        >
          📅 View
        </button>
      </div>

      {activeTab === 'setup' && branchId && (
        <ScheduleBuilder
          branchId={parseInt(branchId, 10)}
          weekStart={weekStart}
          onWeekChange={handleWeekChange}
          onConstraintsSaved={() => { 
            void reloadSchedule()
            setGeneratorRefreshTrigger(prev => prev + 1)
          }}
        />
      )}

      {/* Generate Tab - Create Schedule */}
      {activeTab === 'generate' && branchId && (
        <ScheduleGenerator
          branchId={parseInt(branchId, 10)}
          weekStart={weekStart}
          onWeekChange={handleWeekChange}
          refreshTrigger={generatorRefreshTrigger}
          onScheduleApplied={() => {
            // Refresh schedule view after applying
            void reloadSchedule()
          }}
        />
      )}

      {/* View Tab - Display and Publish */}
      {activeTab === 'view' && (
        <>
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

        {selectedShift && (
          <div className="card" style={{ marginTop: '1rem' }}>
            <div className="card-header-row">
              <div>
                <h3 style={{ margin: 0 }}>
                  Assign shift – {selectedShift.day} {selectedShift.shiftType}
                </h3>
                <small>
                  Date: {selectedShift.shiftDate}
                </small>
              </div>
              <button
                className="btn-link"
                onClick={() => {
                  setSelectedShift(null)
                  setCandidates(null)
                  setAssignError(null)
                }}
              >
                ✕ Close
              </button>
            </div>

            {loadingCandidates && <p>Loading candidates...</p>}

            {assignError && (
              <p className="error-text">{assignError}</p>
            )}

            {!loadingCandidates && candidates && candidates.length === 0 && (
              <p>No candidates available for this shift.</p>
            )}

            {!loadingCandidates && candidates && candidates.length > 0 && (
              <div className="assign-shift-panel">
                <div className="assign-shift-headings">
                  <span>Employee</span>
                  <span>Availability &amp; conflicts</span>
                  <span>Suggested role</span>
                  <span></span>
                </div>
                <div className="assign-shift-list">
                  {candidates.map((c) => {
                    const conflicts: string[] = []

                    if (c.timeOffConflict) conflicts.push('Time-off')
                    if (!c.availabilitySubmitted) conflicts.push('No availability form')
                    if (c.availabilitySubmitted && !c.availableForShift)
                      conflicts.push('Marked unavailable')
                    if (c.alreadyAssignedThisShift) conflicts.push('Already in this shift')
                    if (c.alreadyAssignedThisDay) conflicts.push('Other shift this day')
                      
                    const hasConflicts = conflicts.length > 0
                    const disabled = !!schedule?.published

                    const availabilityLabel = (() => {
                      if (!c.availabilitySubmitted) return 'No availability form'
                      if (!c.availableForShift) return 'Unavailable'
                      if (conflicts.length > 0) return 'Conflicted'
                      return 'Available'
                    })()

                    const availabilityState = (() => {
                      if (!c.availabilitySubmitted) return 'flex'
                      if (!c.availableForShift) return 'unavailable'
                      if (conflicts.length > 0) return 'conflict'
                      return 'available'
                    })()

                    const rowClass = [
                      'assign-shift-row',
                      hasConflicts ? 'has-conflicts' : '',
                      disabled ? 'is-disabled' : '',
                    ]
                      .filter(Boolean)
                      .join(' ')

                    return (
                      <div key={c.id} className={rowClass}>
                        <div className="assign-col assign-col-employee">
                          <button
                            type="button"
                            className="candidate-name-link"
                            onClick={() => navigate(`/hr/employees/${c.id}`)}
                          >
                            {c.name}
                          </button>
                          <div className="candidate-meta">
                            {c.roles && c.roles.length > 0 ? (
                              c.roles.map((role) => (
                                <span key={role} className="candidate-role-pill">
                                  {role}
                                </span>
                              ))
                            ) : (
                              <span className="candidate-role-pill muted">No role tags</span>
                            )}
                            <span className="candidate-stat">Weekly shifts: {c.weeklyAssignments}</span>
                          </div>
                        </div>

                        <div className="assign-col assign-col-status">
                          <span className={`availability-chip ${availabilityState}`}>
                            {availabilityLabel}
                          </span>
                          <div className="conflict-chip-list">
                            {conflicts.length > 0 ? (
                              conflicts.map((x) => (
                                <span key={x} className="conflict-chip">
                                  {x}
                                </span>
                              ))
                            ) : (
                              <span className="conflict-chip ok">No conflicts</span>
                            )}
                          </div>
                        </div>

                        <div className="assign-col assign-col-suggested">
                          <span className="role-chip">{c.suggestedRole ?? '—'}</span>
                        </div>

                        <div className="assign-col assign-col-action">
                          <button
                            className="btn-primary assign-btn"
                            disabled={disabled || assigningId === c.id}
                            onClick={() => {
                              if (hasConflicts) {
                                const ok = window.confirm(
                                  'This employee has conflicts for this shift:\n' +
                                  conflicts.join('\n') +
                                  '\n\nAssign anyway?'
                                )
                                if (!ok) return
                              }

                              void handleAssign(c.id)
                            }}
                          >
                            {assigningId === c.id
                              ? 'Assigning…'
                              : hasConflicts
                              ? 'Force assign'
                              : 'Assign shift'}
                          </button>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}
          </div>
        )}

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
            <div className="legend-item">
              <span className="legend-color legend-setup"></span>
              <span>Setup needed (no requirements defined)</span>
            </div>
          </div>
        </div>
        </>
      )}
    </div>
  )
}

export default HrBranchSchedulePage

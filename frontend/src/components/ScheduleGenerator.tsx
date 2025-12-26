import React, { useMemo, useState, useEffect } from 'react'
import type { CSSProperties } from 'react'
import * as ScheduleApi from '../api/ScheduleApi'
import { useToast } from './ToastContext'
import { addDaysISO, parseISODateToLocal } from '../utils/date'
import './ScheduleGenerator.css'

interface GenerationState {
  loading: boolean
  generated: boolean
  result: ScheduleApi.ScheduleGenerationResult | null
  error: string | null
}

interface PreviewStats {
  hasConstraints: boolean
  totalConstraints: number
  employeesWithAvailability: number
  totalEmployees: number
  loading: boolean
}

interface ScheduleGeneratorProps {
  branchId: number
  weekStart: string
  onWeekChange: (nextWeekStart: string) => void
  refreshTrigger?: number // Increment to force refresh of stats
  onScheduleApplied?: () => void // Callback when schedule is successfully applied
}

const ScheduleGenerator: React.FC<ScheduleGeneratorProps> = ({ branchId, weekStart, onWeekChange, refreshTrigger, onScheduleApplied }) => {
  const [state, setState] = useState<GenerationState>({
    loading: false,
    generated: false,
    result: null,
    error: null,
  })
  const [previewStats, setPreviewStats] = useState<PreviewStats>({
    hasConstraints: false,
    totalConstraints: 0,
    employeesWithAvailability: 0,
    totalEmployees: 0,
    loading: true,
  })
  const [applyLoading, setApplyLoading] = useState(false)
  const [publishLoading, setPublishLoading] = useState(false)
  const { addToast } = useToast()

  const weekEndStr = useMemo(() => addDaysISO(weekStart, 6), [weekStart])

  // Load preview stats when week changes or refreshTrigger changes
  useEffect(() => {
    if (!branchId || !weekStart) return
    void loadPreviewStats()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branchId, weekStart, refreshTrigger])

  const loadPreviewStats = async () => {
    setPreviewStats(s => ({ ...s, loading: true }))
    try {
      // Load constraints and availability in parallel
      const [constraintsRes, availabilityRes] = await Promise.all([
        ScheduleApi.getScheduleConstraints(branchId, weekStart),
        ScheduleApi.getBranchAvailability(branchId, weekStart),
      ])
      
      const constraints = constraintsRes.data || []
      const availability = availabilityRes.data
      
      // Count employees with at least some availability
      const employeesWithAvailability = availability.employees.filter(emp => {
        const slots = Object.values(emp.availability)
        return slots.some(avail => avail)
      }).length
      
      setPreviewStats({
        hasConstraints: constraints.length > 0,
        totalConstraints: constraints.length,
        employeesWithAvailability,
        totalEmployees: availability.employees.length,
        loading: false,
      })
    } catch (err) {
      console.error('Failed to load preview stats:', err)
      setPreviewStats(s => ({ ...s, loading: false }))
    }
  }

  const generateSchedule = async () => {
    if (branchId == null || !weekStart) return

    try {
      setState(s => ({ ...s, loading: true, error: null }))
      const result = await ScheduleApi.generateSchedule(branchId, weekStart)

      setState(s => ({
        ...s,
        loading: false,
        generated: true,
        result: result.data,
      }))

      if (result.data.hasViolations) {
        addToast(
          `⚠️ Schedule generated with ${result.data.totalViolations} staffing issue(s)`,
          'warning'
        )
      } else {
        addToast('✅ Schedule generated successfully!', 'success')
      }
    } catch (err: unknown) {
      console.error(err)
      const errMsg = err instanceof Error ? err.message : 'Failed to generate schedule'
      setState(s => ({ ...s, loading: false, error: errMsg }))
      addToast(errMsg, 'error')
    }
  }

  const applySchedule = async (publishAfter: boolean) => {
    if (!branchId || !weekStart || !state.result) return

    if (state.result.hasViolations) {
      const shouldContinue = window.confirm(
        'Some shifts are still understaffed. Apply the generated assignments anyway?'
      )
      if (!shouldContinue) {
        return
      }
    }

    try {
      setApplyLoading(true)
      const applyRes = await ScheduleApi.applyGeneratedSchedule(branchId, weekStart)
      addToast(applyRes.data.message, applyRes.data.hasViolations ? 'warning' : 'success')

      // Trigger refresh of schedule view
      if (onScheduleApplied) {
        onScheduleApplied()
      }

      if (publishAfter) {
        setPublishLoading(true)
        const publishRes = await ScheduleApi.publishBranchSchedule(branchId, weekStart)
        addToast(publishRes.data.message ?? 'Schedule published to employees', 'success')
        // Refresh again after publishing
        if (onScheduleApplied) {
          onScheduleApplied()
        }
      }
    } catch (err) {
      const errMsg = err instanceof Error ? err.message : 'Failed to apply schedule'
      addToast(errMsg, 'error')
    } finally {
      setApplyLoading(false)
      setPublishLoading(false)
    }
  }

  const formatDate = (dateStr: string) => {
    const parsed = parseISODateToLocal(dateStr)
    if (!parsed) return dateStr
    return parsed.toLocaleDateString('en-US', { month: 'short', day: 'numeric', weekday: 'short' })
  }

  const shiftColor = (shift: string) => {
    return shift === 'MORNING' ? '#fbbf24' : '#60a5fa'
  }

  // Group assignments by date and shift for better display
  const groupedAssignments = useMemo(() => {
    const assignments = state.result?.assignments
    if (!assignments) return []
    
    const groups: Map<string, typeof assignments> = new Map()
    
    for (const a of assignments) {
      const key = `${a.shiftDate}-${a.shiftType}`
      if (!groups.has(key)) {
        groups.set(key, [])
      }
      groups.get(key)!.push(a)
    }
    
    return Array.from(groups.entries()).map(([key, assignmentList]) => ({
      key,
      shiftDate: assignmentList[0].shiftDate,
      shiftType: assignmentList[0].shiftType,
      assignments: assignmentList,
    })).sort((a, b) => {
      if (a.shiftDate !== b.shiftDate) return a.shiftDate.localeCompare(b.shiftDate)
      return a.shiftType === 'MORNING' ? -1 : 1
    })
  }, [state.result])

  return (
    <div className="schedule-generator">
      <div className="generator-card">
        <h3>🤖 AI Schedule Generator</h3>
        <p className="subtitle">
          The algorithm analyzes employee availability, time-off requests, and staffing constraints 
          to create an optimal schedule that balances workload fairly.
        </p>

        <div className="generator-controls">
          <div className="control-group">
            <label>Week Starting:</label>
            <input
              type="date"
              value={weekStart}
              onChange={(e) => onWeekChange(e.target.value)}
              disabled={state.loading}
            />
          </div>
          <div className="control-group">
            <label>Week Ending:</label>
            <input
              type="date"
              value={weekEndStr}
              disabled
            />
          </div>
        </div>

        {/* Pre-generation checklist */}
        <div className="generator-checklist">
          <h4>📋 Pre-Generation Checklist</h4>
          {previewStats.loading ? (
            <div className="checklist-loading">Loading...</div>
          ) : (
            <div className="checklist-items">
              <div className={`checklist-item ${previewStats.hasConstraints ? 'checklist-ok' : 'checklist-warn'}`}>
                <span className="checklist-icon">{previewStats.hasConstraints ? '✓' : '⚠️'}</span>
                <span className="checklist-text">
                  {previewStats.hasConstraints 
                    ? `${previewStats.totalConstraints} staffing constraints defined`
                    : 'No constraints defined - go to Setup tab first!'}
                </span>
              </div>
              <div className={`checklist-item ${previewStats.employeesWithAvailability > 0 ? 'checklist-ok' : 'checklist-warn'}`}>
                <span className="checklist-icon">{previewStats.employeesWithAvailability > 0 ? '✓' : '⚠️'}</span>
                <span className="checklist-text">
                  {previewStats.employeesWithAvailability}/{previewStats.totalEmployees} employees submitted availability
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Algorithm explanation */}
        <div className="generator-algorithm-info">
          <h4>🧠 How the Algorithm Works</h4>
          <div className="algorithm-steps">
            <div className="algorithm-step">
              <span className="step-number">1</span>
              <span className="step-text">Analyzes staffing requirements for each shift</span>
            </div>
            <div className="algorithm-step">
              <span className="step-number">2</span>
              <span className="step-text">Filters employees by role, availability, and time-off</span>
            </div>
            <div className="algorithm-step">
              <span className="step-number">3</span>
              <span className="step-text">Balances shifts fairly across employees</span>
            </div>
            <div className="algorithm-step">
              <span className="step-number">4</span>
              <span className="step-text">Reports conflicts and staffing gaps</span>
            </div>
          </div>
        </div>

        {state.error && <div className="generator-error">{state.error}</div>}

        <button
          className="btn btn-generate"
          onClick={generateSchedule}
          disabled={state.loading || !previewStats.hasConstraints}
          title={!previewStats.hasConstraints ? 'Define staffing constraints in Setup tab first' : ''}
        >
          {state.loading ? '⏳ Generating...' : '⚡ Generate Schedule'}
        </button>

        {!previewStats.hasConstraints && !previewStats.loading && (
          <p className="generator-hint">
            💡 Tip: Go to the <strong>Setup</strong> tab to define how many staff you need per shift before generating.
          </p>
        )}

        {state.generated && state.result && (
          <div className="generation-results">
            <div className="generation-actions">
              <button
                className="btn btn-apply"
                onClick={() => applySchedule(false)}
                disabled={applyLoading || publishLoading}
              >
                {applyLoading && !publishLoading ? '🛠 Applying…' : '📥 Apply to Draft Schedule'}
              </button>
              <button
                className="btn btn-publish"
                onClick={() => applySchedule(true)}
                disabled={publishLoading || applyLoading}
              >
                {publishLoading ? '🚀 Publishing…' : '🚀 Apply & Publish'}
              </button>
            </div>
            <div className="results-header">
              <div className="stat">
                <div className="stat-value">{state.result.totalAssignments}</div>
                <div className="stat-label">Total Shifts Assigned</div>
              </div>
              <div className="stat">
                <div className="stat-value" style={{ color: state.result.hasViolations ? '#ef4444' : '#10b981' }}>
                  {state.result.totalViolations}
                </div>
                <div className="stat-label">Staffing Issues</div>
              </div>
              <div className="stat">
                <div className="stat-value">{groupedAssignments.length}</div>
                <div className="stat-label">Shifts Covered</div>
              </div>
            </div>

            {state.result.hasViolations && (
              <div className="violations-section">
                <h4>⚠️ Staffing Issues (Action Required)</h4>
                <p className="violations-intro">
                  The following shifts couldn't be fully staffed. You can manually assign employees in the View tab.
                </p>
                <div className="violations-list">
                  {state.result.violations.map((v, idx) => (
                    <div key={idx} className="violation-item">
                      <div className="violation-icon">⚠️</div>
                      <div className="violation-content">
                        <div className="violation-message">{v.message}</div>
                        {v.shiftDate && (
                          <div className="violation-details">
                            📅 {formatDate(v.shiftDate)} · {v.shiftType === 'MORNING' ? '☀️' : '🌙'} {v.shiftType} 
                            <br />
                            👥 Required: <strong>{v.requiredCount}</strong>, Assigned: <strong>{v.actualCount}</strong>
                            <span className="violation-gap"> (need {v.requiredCount - v.actualCount} more)</span>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {!state.result.hasViolations && (
              <div className="success-section">
                <div className="success-message">
                  <span className="success-icon">🎉</span>
                  <div>
                    <strong>Schedule Generated Successfully!</strong>
                    <p>All shifts are properly staffed according to your constraints.</p>
                  </div>
                </div>
              </div>
            )}

            <div className="assignments-section">
              <h4>📅 Generated Assignments by Shift</h4>
              <div className="assignments-by-shift">
                {groupedAssignments.map((group) => (
                  <div
                    key={group.key}
                    className="shift-group"
                    style={{ '--accent-color': shiftColor(group.shiftType) } as CSSProperties}
                  >
                    <div className="shift-group-header">
                      <span className="shift-group-date">{formatDate(group.shiftDate)}</span>
                      <span className="shift-group-type">
                        {group.shiftType === 'MORNING' ? '☀️' : '🌙'} {group.shiftType}
                      </span>
                      <span className="shift-group-count">{group.assignments.length} assigned</span>
                    </div>
                    <div className="shift-group-employees">
                      {group.assignments.map((a, idx) => (
                        <span key={idx} className="employee-chip">
                          Employee #{a.employeeId}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="next-steps">
              <h4>🎯 Next Steps</h4>
              <ol className="next-steps-list">
                <li>Review the schedule in the <strong>View</strong> tab</li>
                <li>Manually adjust any understaffed shifts if needed</li>
                <li>Click <strong>Publish Schedule</strong> when ready to notify employees</li>
              </ol>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default ScheduleGenerator

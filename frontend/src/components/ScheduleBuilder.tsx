import React, { useEffect, useState, useMemo } from 'react'
import * as ScheduleApi from '../api/ScheduleApi'
import { getBranchRolesApi, type BranchRole } from '../api/BranchRoleApi'
import { useToast } from './ToastContext'
import './ScheduleBuilder.css'

const SHIFTS: Array<'MORNING' | 'EVENING'> = ['MORNING', 'EVENING']
const FALLBACK_ROLES = ['MANAGER', 'CASHIER', 'STOREKEEPER']

interface ConstraintRow {
  shiftType: 'MORNING' | 'EVENING'
  roleRequired: string
  minRequired: number
  idealCount: number
}

interface ScheduleBuilderProps {
  branchId: number
  weekStart: string
  onWeekChange: (nextWeekStart: string) => void
  // חדש – שנדע להריץ רענון של ה־View אחרי שמירה
  onConstraintsSaved?: () => void
}

const ScheduleBuilder: React.FC<ScheduleBuilderProps> = ({
  branchId,
  weekStart,
  onWeekChange,
  onConstraintsSaved,
}) => {
  const [constraints, setConstraints] = useState<ConstraintRow[]>([])
  const [branchRoles, setBranchRoles] = useState<BranchRole[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { addToast } = useToast()

  // Get role codes from the fetched branch roles, or use fallback
  const availableRoles = useMemo(() => {
    if (branchRoles.length > 0) {
      return branchRoles.filter(r => r.active).map(r => r.code)
    }
    return FALLBACK_ROLES
  }, [branchRoles])

  // Generate default constraints dynamically based on available roles
  const generateDefaultConstraints = (): ConstraintRow[] => {
    const defaults: ConstraintRow[] = []
    for (const shift of SHIFTS) {
      for (const role of availableRoles) {
        // Smart defaults based on role type
        let minRequired = 1
        let idealCount = 1
        
        if (role.includes('CASHIER') || role.includes('SERVER') || role.includes('WAITER')) {
          minRequired = 2
          idealCount = 3
        } else if (role.includes('STOREKEEPER') || role.includes('COOK')) {
          minRequired = shift === 'MORNING' ? 1 : 0
          idealCount = 1
        }
        
        defaults.push({
          shiftType: shift,
          roleRequired: role,
          minRequired,
          idealCount,
        })
      }
    }
    return defaults
  }

  // Load branch roles when branch changes
  useEffect(() => {
    if (!branchId) return
    void loadBranchRoles()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branchId])

  // Load constraints when branch or week changes
  useEffect(() => {
    if (!branchId || !weekStart) return
    void loadConstraints()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branchId, weekStart])

  const loadBranchRoles = async () => {
    try {
      const res = await getBranchRolesApi(branchId, true) // active only
      setBranchRoles(res.data || [])
    } catch (err) {
      console.warn('Could not load branch roles, using fallback:', err)
      setBranchRoles([])
    }
  }

  const loadConstraints = async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await ScheduleApi.getScheduleConstraints(branchId, weekStart)
      setConstraints(res.data || []) // לא מכריח ברירות מחדל – זה רק אם תלחץ על הכפתור
    } catch (err) {
      console.error(err)
      setError('Failed to load constraints')
    } finally {
      setLoading(false)
    }
  }

  const addConstraintRow = () => {
    setConstraints(prev => [
      ...prev,
      {
        shiftType: 'MORNING',
        roleRequired: availableRoles[0] || 'MANAGER',
        minRequired: 0,
        idealCount: 1,
      },
    ])
  }

  // Load default template with dynamic roles
  const loadDefaultTemplate = () => {
    setConstraints(generateDefaultConstraints())
  }

  const updateConstraint = (index: number, field: keyof ConstraintRow, value: string | number) => {
    setConstraints(prev => {
      const updated = [...prev]
      updated[index] = { ...updated[index], [field]: value }
      return updated
    })
  }

  const removeConstraint = (index: number) => {
    setConstraints(prev => prev.filter((_, i) => i !== index))
  }

  const saveConstraints = async () => {
    try {
      setLoading(true)
      await ScheduleApi.setScheduleConstraints(
        branchId,
        weekStart,
        constraints.map(c => ({
          shiftType: c.shiftType,
          roleRequired: c.roleRequired,
          minRequired: c.minRequired,
          idealCount: c.idealCount,
        })),
      )
      addToast('Constraints saved successfully!', 'success')
      // לקרוא להורה (HrBranchSchedulePage) שירענן את ה־View
      onConstraintsSaved?.()
    } catch (err: unknown) {
      console.error(err)
      const message = err instanceof Error ? err.message : 'Failed to save constraints'
      addToast(message, 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="schedule-builder">
      <div className="builder-card">
        <h3>⚙️ Staffing Requirements</h3>
        <p className="subtitle">
          Define the minimum and ideal number of staff by role for each shift (applied to all days of this week).
        </p>

        <div className="builder-week-controls">
          <div>
            <strong>Week:</strong> {ScheduleApi.formatWeekRange(weekStart)}
          </div>
          <div className="builder-week-actions">
            <button
              className="btn-outline"
              onClick={() => onWeekChange(ScheduleApi.shiftWeek(weekStart, -1))}
              disabled={loading}
            >
              ← Previous
            </button>
            <button
              className="btn-outline"
              onClick={() => onWeekChange(ScheduleApi.shiftWeek(weekStart, 1))}
              disabled={loading}
            >
              Next →
            </button>
          </div>
        </div>

        {error && <div className="builder-error">{error}</div>}

        <div className="constraints-table-wrapper">
          <table className="constraints-table">
            <thead>
              <tr>
                <th>Shift</th>
                <th>Role Required</th>
                <th>Minimum</th>
                <th>Ideal</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {constraints.map((c, idx) => (
                <tr key={idx}>
                  <td>
                    <select
                      value={c.shiftType}
                      onChange={(e) =>
                        updateConstraint(idx, 'shiftType', e.target.value as ConstraintRow['shiftType'])
                      }
                    >
                      {SHIFTS.map((s) => (
                        <option key={s} value={s}>
                          {s}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <select
                      value={c.roleRequired}
                      onChange={(e) => updateConstraint(idx, 'roleRequired', e.target.value)}
                    >
                      {availableRoles.map((r) => (
                        <option key={r} value={r}>
                          {branchRoles.find(br => br.code === r)?.displayName || r}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <input
                      type="number"
                      min="0"
                      value={c.minRequired}
                      onChange={(e) =>
                        updateConstraint(
                          idx,
                          'minRequired',
                          Math.max(
                            0,
                            Number.isNaN(Number(e.target.value)) ? 0 : Number(e.target.value),
                          ),
                        )
                      }
                    />
                  </td>
                  <td>
                    <input
                      type="number"
                      min="0"
                      value={c.idealCount}
                      onChange={(e) =>
                        updateConstraint(
                          idx,
                          'idealCount',
                          Math.max(
                            0,
                            Number.isNaN(Number(e.target.value)) ? 0 : Number(e.target.value),
                          ),
                        )
                      }
                    />
                  </td>
                  <td>
                    <button
                      className="btn-remove"
                      onClick={() => removeConstraint(idx)}
                      title="Remove constraint"
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
              {constraints.length === 0 && (
                <tr>
                  <td
                    colSpan={5}
                    style={{ textAlign: 'center', padding: '1rem', color: '#9ca3af' }}
                  >
                    No constraints defined for this week yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="builder-actions">
          <button className="btn btn-secondary" onClick={addConstraintRow} disabled={loading}>
            + Add Constraint
          </button>
          <button
            className="btn btn-outline"
            type="button"
            onClick={loadDefaultTemplate}
            disabled={loading}
            style={{ marginLeft: '0.5rem' }}
          >
            Use default template
          </button>
          <button
            className="btn btn-primary"
            onClick={saveConstraints}
            disabled={loading || constraints.length === 0}
            style={{ marginLeft: 'auto' }}
          >
            {loading ? 'Saving...' : '💾 Save Constraints'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default ScheduleBuilder

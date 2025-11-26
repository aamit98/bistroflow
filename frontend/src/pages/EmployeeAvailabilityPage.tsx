import React, { useEffect, useState } from 'react'
import { useAuth } from '../security/AuthContext'
import {
  getEmployeeAvailability,
  updateEmployeeAvailability,
  type AvailabilitySlot,
  type WeekAvailability,
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

const EmployeeAvailabilityPage: React.FC = () => {
  const { employee } = useAuth()
  // Default to NEXT Sunday (the week for which availability should be submitted)
  const [weekStart, setWeekStart] = useState(getNextSundayISO)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)
  const [slots, setSlots] = useState<AvailabilitySlot[]>([])
  
  // Status from backend
  const [editable, setEditable] = useState(true)
  const [editableReason, setEditableReason] = useState('')
  const [schedulePublished, setSchedulePublished] = useState(false)
  const [publishedAt, setPublishedAt] = useState<string | null>(null)

  // Ensure we always have a full grid
  function ensureGrid(current: AvailabilitySlot[]): AvailabilitySlot[] {
    const map = new Map<string, AvailabilitySlot>()
    for (const s of current) {
      map.set(`${s.dayOfWeek}-${s.shiftType}`, s)
    }

    const result: AvailabilitySlot[] = []
    for (const d of DAYS) {
      for (const shift of SHIFTS) {
        const key = `${d}-${shift}`
        if (map.has(key)) {
          result.push(map.get(key)!)
        } else {
          result.push({ dayOfWeek: d, shiftType: shift, available: false })
        }
      }
    }
    return result
  }

  useEffect(() => {
    if (!employee) return

    const load = async () => {
      setLoading(true)
      setError(null)
      setSuccessMsg(null)
      try {
        const res = await getEmployeeAvailability(employee.id, weekStart)
        setSlots(ensureGrid(res.data.slots ?? []))
        setEditable(res.data.editable)
        setEditableReason(res.data.editableReason)
        setSchedulePublished(res.data.schedulePublished)
        setPublishedAt(res.data.publishedAt)
      } catch (e: any) {
        console.error(e)
        // if no data yet, start from empty grid
        setSlots(ensureGrid([]))
        setEditable(true) // Assume editable if we can't fetch status
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [employee, weekStart])

  if (!employee) return null

  const toggleSlot = (day: DayOfWeekCode, shift: ShiftType) => {
    if (!editable) return
    setSlots((prev) =>
      prev.map((s) =>
        s.dayOfWeek === day && s.shiftType === shift
          ? { ...s, available: !s.available }
          : s
      )
    )
  }

  const handleSave = async () => {
    if (!employee || !editable) return
    setSaving(true)
    setError(null)
    setSuccessMsg(null)
    try {
      const body: WeekAvailability = {
        employeeId: employee.id,
        weekStart,
        slots,
      }
      await updateEmployeeAvailability(employee.id, weekStart, body)
      setSuccessMsg('Availability saved successfully!')
    } catch (e: any) {
      console.error(e)
      const errorData = e.response?.data
      if (errorData?.error) {
        setError(errorData.error)
      } else {
        setError('Failed to save availability')
      }
    } finally {
      setSaving(false)
    }
  }

  const goToNextWeek = () => setWeekStart(shiftWeek(weekStart, 1))
  const goToPrevWeek = () => setWeekStart(shiftWeek(weekStart, -1))
  const goToDefaultWeek = () => setWeekStart(getNextSundayISO())

  // Status banner component
  const StatusBanner = () => {
    if (schedulePublished) {
      return (
        <div className="status-banner status-published">
          <strong>📋 Schedule Published</strong>
          <p>
            The schedule for this week has been published
            {publishedAt && ` on ${new Date(publishedAt).toLocaleDateString()}`}.
            Availability is now locked.
          </p>
        </div>
      )
    }

    if (!editable) {
      return (
        <div className="status-banner status-locked">
          <strong>🔒 Editing Disabled</strong>
          <p>{editableReason}</p>
        </div>
      )
    }

    return (
      <div className="status-banner status-open">
        <strong>✏️ Submissions Open</strong>
        <p>You can edit your availability for this week.</p>
      </div>
    )
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2 className="page-title">My Availability</h2>
          <p className="page-subtitle">
            Submit your availability for upcoming weeks.
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
            Go to next week (default submission week)
          </button>
        </div>
      </div>

      {/* Status banner */}
      <StatusBanner />

      {/* Availability grid */}
      <div className="card">
        {loading && <p>Loading…</p>}
        {error && <p className="form-error">{error}</p>}
        {successMsg && <p className="success-text">{successMsg}</p>}

        <div className="availability-grid">
          <table className="data-table">
            <thead>
              <tr>
                <th>Day</th>
                {SHIFTS.map((s) => (
                  <th key={s}>{s}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {DAYS.map((day) => (
                <tr key={day}>
                  <td>{day.charAt(0) + day.slice(1).toLowerCase()}</td>
                  {SHIFTS.map((shift) => {
                    const slot = slots.find(
                      (s) => s.dayOfWeek === day && s.shiftType === shift
                    )
                    const available = slot?.available ?? false
                    return (
                      <td key={shift}>
                        <button
                          type="button"
                          className={`btn-chip ${
                            available ? 'btn-chip-on' : 'btn-chip-off'
                          } ${!editable ? 'btn-chip-disabled' : ''}`}
                          onClick={() => toggleSlot(day, shift)}
                          disabled={!editable}
                        >
                          {available ? '✓ Available' : '✗ Unavailable'}
                        </button>
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {editable && (
          <div style={{ marginTop: '1rem' }}>
            <button
              className="btn-primary"
              onClick={handleSave}
              disabled={saving}
            >
              {saving ? 'Saving…' : 'Save Availability'}
            </button>
          </div>
        )}
      </div>

      {/* Instructions */}
      <div className="card" style={{ marginTop: '1rem' }}>
        <h3 className="card-title">📌 How it works</h3>
        <ul style={{ fontSize: '0.9rem', color: '#9ca3af', paddingLeft: '1.1rem' }}>
          <li>Submit your availability for <strong>next week</strong> (starting Sunday).</li>
          <li>Deadline: <strong>Thursday 23:59</strong> of the current week.</li>
          <li>Once the schedule is published by HR, availability is locked.</li>
          <li>Past weeks are always read-only.</li>
        </ul>
      </div>
    </div>
  )
}

export default EmployeeAvailabilityPage

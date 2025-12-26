import React, { useEffect, useState } from 'react'
import { useAuth } from '../security/AuthContext'
import { 
  getEmployeeSchedule, 
  confirmShift, 
  confirmWeekShifts,
  type Shift 
} from '../api/ScheduleApi'

function getCurrentSundayISO() {
  const today = new Date()
  const day = today.getDay() // 0 = Sunday
  const diff = day // days since Sunday
  const sunday = new Date(today)
  sunday.setDate(today.getDate() - diff)
  return sunday.toISOString().slice(0, 10)
}

const EmployeeSchedulePage: React.FC = () => {
  const { employee } = useAuth()
  const [weekStart, setWeekStart] = useState(getCurrentSundayISO)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [shifts, setShifts] = useState<Shift[]>([])
  const [published, setPublished] = useState(false)
  const [allConfirmed, setAllConfirmed] = useState(false)
  const [scheduleMessage, setScheduleMessage] = useState<string | null>(null)
  const [confirmingId, setConfirmingId] = useState<number | null>(null)
  const [confirmingAll, setConfirmingAll] = useState(false)

  const loadSchedule = async () => {
    if (!employee) return
    setLoading(true)
    setError(null)
    try {
      const res = await getEmployeeSchedule(employee.id, weekStart)
      setShifts(res.data.shifts)
      setPublished(res.data.published)
      setAllConfirmed(res.data.allConfirmed)
      setScheduleMessage(res.data.message)
    } catch (e: any) {
      console.error(e)
      setError('Failed to load schedule')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSchedule()
  }, [employee, weekStart])

  const handleConfirmShift = async (shiftId: number) => {
    if (!employee) return
    setConfirmingId(shiftId)
    try {
      await confirmShift(employee.id, shiftId)
      // Refresh to get updated confirmation status
      await loadSchedule()
    } catch (e: any) {
      console.error(e)
      setError('Failed to confirm shift')
    } finally {
      setConfirmingId(null)
    }
  }

  const handleConfirmAll = async () => {
    if (!employee) return
    setConfirmingAll(true)
    try {
      await confirmWeekShifts(employee.id, weekStart)
      await loadSchedule()
    } catch (e: any) {
      console.error(e)
      setError('Failed to confirm shifts')
    } finally {
      setConfirmingAll(false)
    }
  }

  if (!employee) return null

  const unconfirmedCount = shifts.filter(s => !s.confirmed).length

  return (
    <div>
      {/* Week Navigation */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <input
            type="date"
            className="bf-form-input"
            style={{ width: 'auto' }}
            value={weekStart}
            onChange={(e) => setWeekStart(e.target.value)}
          />
        </div>
        {published && unconfirmedCount > 0 && (
          <button 
            className="bf-btn bf-btn-primary"
            onClick={handleConfirmAll}
            disabled={confirmingAll}
          >
            {confirmingAll ? 'Confirming...' : `✓ Confirm All (${unconfirmedCount})`}
          </button>
        )}
      </div>

      {/* Confirmation Status Banner */}
      {published && shifts.length > 0 && (
        <div style={{ 
          padding: '18px 22px', 
          marginBottom: '24px',
          borderRadius: '14px',
          background: allConfirmed 
            ? 'rgba(34, 197, 94, 0.1)' 
            : 'rgba(245, 158, 11, 0.1)',
          border: `1px solid ${allConfirmed 
            ? 'rgba(34, 197, 94, 0.25)' 
            : 'rgba(245, 158, 11, 0.25)'}`,
          display: 'flex',
          alignItems: 'center',
          gap: '14px'
        }}>
          <span style={{ fontSize: '28px' }}>
            {allConfirmed ? '✅' : '⚠️'}
          </span>
          <div>
            <strong style={{ 
              color: allConfirmed ? '#22c55e' : '#f59e0b',
              fontSize: '15px'
            }}>
              {allConfirmed 
                ? 'All shifts confirmed!' 
                : `${unconfirmedCount} shift(s) pending confirmation`}
            </strong>
            {!allConfirmed && (
              <p style={{ margin: '4px 0 0 0', fontSize: '13px', color: 'var(--bf-text-secondary)' }}>
                Please confirm your shifts to let your manager know you've seen the schedule.
              </p>
            )}
          </div>
        </div>
      )}

      {/* Loading State */}
      {loading && (
        <div className="bf-loading">
          <div className="bf-spinner"></div>
          <p>Loading schedule...</p>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div style={{
          padding: '16px',
          background: 'rgba(239, 68, 68, 0.1)',
          border: '1px solid rgba(239, 68, 68, 0.25)',
          borderRadius: '10px',
          color: '#ef4444'
        }}>
          {error}
        </div>
      )}

      {/* Not Published State */}
      {!loading && !error && !published && (
        <div className="bf-card">
          <div className="bf-empty-state">
            <div className="bf-empty-icon">⏳</div>
            <h3>{scheduleMessage || 'Schedule not published yet'}</h3>
            <p>Check back later or contact your HR manager.</p>
          </div>
        </div>
      )}

      {/* No Shifts State */}
      {!loading && !error && published && shifts.length === 0 && (
        <div className="bf-card">
          <div className="bf-empty-state">
            <div className="bf-empty-icon">📅</div>
            <h3>No shifts scheduled</h3>
            <p>You don't have any shifts scheduled for this week.</p>
          </div>
        </div>
      )}

      {/* Shifts Table */}
      {!loading && !error && published && shifts.length > 0 && (
        <div className="bf-card">
          <table className="bf-table">
            <thead>
              <tr>
                <th>Day</th>
                <th>Date</th>
                <th>Shift</th>
                <th>Role</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {shifts.map((s, idx) => (
                <tr key={idx}>
                  <td>
                    <span style={{ fontWeight: 600 }}>{s.day}</span>
                  </td>
                  <td>{s.date}</td>
                  <td>
                    <span className={`bf-badge ${s.shiftType === 'MORNING' ? 'bf-badge-warning' : 'bf-badge-primary'}`}>
                      {s.shiftType === 'MORNING' ? '🌅 Morning' : '🌙 Evening'}
                    </span>
                  </td>
                  <td>{s.role || '—'}</td>
                  <td>
                    {s.confirmed ? (
                      <span className="bf-badge bf-badge-success">✓ Confirmed</span>
                    ) : (
                      <span className="bf-badge bf-badge-warning">Pending</span>
                    )}
                  </td>
                  <td>
                    {!s.confirmed && s.id && (
                      <button
                        className="bf-btn bf-btn-success bf-btn-sm"
                        onClick={() => handleConfirmShift(s.id!)}
                        disabled={confirmingId === s.id}
                      >
                        {confirmingId === s.id ? '...' : 'Confirm'}
                      </button>
                    )}
                    {s.confirmed && (
                      <span style={{ color: 'var(--bf-text-muted)', fontSize: '12px' }}>
                        {s.confirmedAt 
                          ? new Date(s.confirmedAt).toLocaleDateString() 
                          : '—'}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export default EmployeeSchedulePage

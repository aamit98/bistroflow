import React, { useEffect, useState } from 'react'
import { useAuth } from '../security/AuthContext'
import { getEmployeeSchedule, type Shift } from '../api/ScheduleApi'

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

  useEffect(() => {
    if (!employee) return

    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const res = await getEmployeeSchedule(employee.id, weekStart)
        setShifts(res.data.shifts)
      } catch (e: any) {
        console.error(e)
        setError('Failed to load schedule')
      } finally {
        setLoading(false)
      }
    }

    load()
  }, [employee, weekStart])

  if (!employee) return null

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2 className="page-title">My schedule</h2>
          <p className="page-subtitle">
            Week starting on <strong>{weekStart}</strong>.
          </p>
        </div>
        <div>
          <input
            type="date"
            value={weekStart}
            onChange={(e) => setWeekStart(e.target.value)}
          />
        </div>
      </div>

      <div className="card">
        {loading && <p>Loading…</p>}
        {error && <p className="form-error">{error}</p>}

        {!loading && !error && shifts.length === 0 && (
          <p>No shifts scheduled for this week yet.</p>
        )}

        {!loading && !error && shifts.length > 0 && (
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Shift</th>
                <th>Branch</th>
                <th>Role</th>
              </tr>
            </thead>
            <tbody>
              {shifts.map((s, idx) => (
                <tr key={idx}>
                  <td>{s.date}</td>
                  <td>{s.shiftType}</td>
                  <td>{s.branchId}</td>
                  <td>{s.role}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default EmployeeSchedulePage

import React, { useState } from 'react'
import { useAuth } from '../security/AuthContext'
import { createTimeOffRequestApi } from '../api/TimeOffApi'

type ShiftType = 'MORNING' | 'EVENING'

const EmployeeRequestsPage: React.FC = () => {
  const { employee } = useAuth()
  const [date, setDate] = useState('')
  const [shiftType, setShiftType] = useState<ShiftType>('MORNING')
  const [reason, setReason] = useState('')
  const [status, setStatus] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!employee) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setStatus(null)
    setError(null)

    if (!date) {
      setError('Please choose a date')
      return
    }

    try {
      setSubmitting(true)
      await createTimeOffRequestApi(employee.id, {
        date,
        shiftType,
        reason,
      })
      setStatus('Request sent to your HR manager.')
      setReason('')
    } catch (err: any) {
      console.error(err)
      setError(err?.response?.data?.error ?? 'Failed to send request')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page">
      <h2>Requests to HR</h2>
      <section className="card">
        <h3 className="card-title">Emergency / time-off request</h3>
        <p className="card-subtitle">
          Use this form if you have a special situation and cannot work on a
          specific day or shift. Your HR manager will review and either approve
          or reject it, and you&apos;ll get a notification here in the system.
        </p>

        <form onSubmit={handleSubmit} className="form">
          <label className="form-field">
            <span>Date</span>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              required
            />
          </label>

          <label className="form-field">
            <span>Shift</span>
            <select
              value={shiftType}
              onChange={(e) => setShiftType(e.target.value as ShiftType)}
            >
              <option value="MORNING">Morning</option>
              <option value="EVENING">Evening</option>
            </select>
          </label>

          <label className="form-field">
            <span>Reason / details</span>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Describe what you need – emergency, study day, family event, etc."
              rows={4}
            />
          </label>

          {error && <p className="form-error">{error}</p>}
          {status && <p className="form-success">{status}</p>}

          <button type="submit" disabled={submitting}>
            {submitting ? 'Sending…' : 'Send request'}
          </button>
        </form>
      </section>
    </div>
  )
}

export default EmployeeRequestsPage

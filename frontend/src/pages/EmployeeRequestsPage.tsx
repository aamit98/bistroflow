import React, { useState, useEffect } from 'react'
import { useAuth } from '../security/AuthContext'
import { createTimeOffRequestApi, getEmployeeTimeOffRequestsApi, type TimeOffRequest } from '../api/TimeOffApi'

type ShiftType = 'MORNING' | 'EVENING'

const EmployeeRequestsPage: React.FC = () => {
  const { employee } = useAuth()
  const [date, setDate] = useState('')
  const [shiftType, setShiftType] = useState<ShiftType>('MORNING')
  const [reason, setReason] = useState('')
  const [status, setStatus] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  
  // Request history state
  const [requests, setRequests] = useState<TimeOffRequest[]>([])
  const [loadingRequests, setLoadingRequests] = useState(false)
  const PAGE_SIZE = 10
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  useEffect(() => {
    if (employee) {
      void loadRequests(0)
    }
  }, [employee])

  const loadRequests = async (targetPage = page) => {
    if (!employee) return
    setLoadingRequests(true)
    try {
      const res = await getEmployeeTimeOffRequestsApi(employee.id, targetPage, PAGE_SIZE)
      setRequests(res.data.content)
      setPage(res.data.page)
      setTotalPages(res.data.totalPages)
      setTotalElements(res.data.totalElements)
    } catch (err) {
      console.error('Failed to load requests:', err)
    } finally {
      setLoadingRequests(false)
    }
  }

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
      setDate('')
      // Refresh the requests list
      await loadRequests(0)
    } catch (err: any) {
      console.error(err)
      setError(err?.response?.data?.error ?? 'Failed to send request')
    } finally {
      setSubmitting(false)
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'APPROVED': return '#10b981'
      case 'REJECTED': return '#ef4444'
      case 'PENDING': return '#f59e0b'
      default: return '#6b7280'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'APPROVED': return '✓'
      case 'REJECTED': return '✗'
      case 'PENDING': return '⏳'
      default: return '?'
    }
  }

  return (
    <div className="page">
      <h2>Time-Off Requests</h2>
      
      {/* New Request Form */}
      <section className="card">
        <h3 className="card-title">Request Time Off</h3>
        <p className="card-subtitle">
          Use this form if you have an emergency or need time off. Your HR manager
          will review and respond to your request.
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

      {/* Request History */}
      <section className="card" style={{ marginTop: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h3 className="card-title" style={{ margin: 0 }}>My Request History</h3>
          <button 
            type="button" 
            className="btn-outline" 
            onClick={() => void loadRequests(page)}
            disabled={loadingRequests}
            style={{ padding: '0.25rem 0.75rem', fontSize: '0.875rem' }}
          >
            {loadingRequests ? 'Loading...' : 'Refresh'}
          </button>
        </div>

        {totalElements > 0 && requests.length > 0 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', fontSize: '0.9rem', color: '#9ca3af' }}>
            <span>
              {(() => {
                const start = page * PAGE_SIZE + 1
                const end = Math.min(totalElements, page * PAGE_SIZE + requests.length)
                return `Showing ${start} – ${end} of ${totalElements}`
              })()}
            </span>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                type="button"
                className="btn-outline"
                onClick={() => void loadRequests(page - 1)}
                disabled={page === 0 || loadingRequests}
              >
                Previous
              </button>
              <button
                type="button"
                className="btn-outline"
                onClick={() => void loadRequests(page + 1)}
                disabled={page + 1 >= totalPages || loadingRequests}
              >
                Next
              </button>
            </div>
          </div>
        )}

        {loadingRequests && requests.length === 0 && (
          <p style={{ color: '#9ca3af' }}>Loading your requests...</p>
        )}

        {!loadingRequests && requests.length === 0 && (
          <p style={{ color: '#9ca3af' }}>You haven't made any time-off requests yet.</p>
        )}

        {requests.length > 0 && (
          <div style={{ overflowX: 'auto' }}>
            <table className="table" style={{ width: '100%' }}>
              <thead>
                <tr>
                  <th style={{ textAlign: 'left', padding: '0.75rem 0.5rem' }}>Date</th>
                  <th style={{ textAlign: 'left', padding: '0.75rem 0.5rem' }}>Shift</th>
                  <th style={{ textAlign: 'left', padding: '0.75rem 0.5rem' }}>Status</th>
                  <th style={{ textAlign: 'left', padding: '0.75rem 0.5rem' }}>Reason</th>
                  <th style={{ textAlign: 'left', padding: '0.75rem 0.5rem' }}>Response</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((req) => (
                  <tr key={req.id} style={{ borderBottom: '1px solid #374151' }}>
                    <td style={{ padding: '0.75rem 0.5rem' }}>{req.date}</td>
                    <td style={{ padding: '0.75rem 0.5rem' }}>
                      <span style={{ 
                        display: 'inline-block',
                        padding: '0.25rem 0.5rem',
                        borderRadius: '0.25rem',
                        fontSize: '0.75rem',
                        backgroundColor: req.shiftType === 'MORNING' ? '#fef3c7' : '#dbeafe',
                        color: req.shiftType === 'MORNING' ? '#92400e' : '#1e40af'
                      }}>
                        {req.shiftType === 'MORNING' ? '☀️' : '🌙'} {req.shiftType}
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem' }}>
                      <span style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.25rem',
                        padding: '0.25rem 0.5rem',
                        borderRadius: '0.25rem',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        backgroundColor: `${getStatusColor(req.status)}20`,
                        color: getStatusColor(req.status)
                      }}>
                        {getStatusIcon(req.status)} {req.status}
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {req.reason || <em style={{ color: '#6b7280' }}>No reason provided</em>}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {req.decisionComment || (req.status === 'PENDING' ? <em style={{ color: '#6b7280' }}>Awaiting review</em> : '-')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.75rem' }}>
            <span style={{ color: '#9ca3af', fontSize: '0.9rem' }}>
              Page {page + 1} of {totalPages}
            </span>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                type="button"
                className="btn-outline"
                onClick={() => void loadRequests(page - 1)}
                disabled={page === 0 || loadingRequests}
              >
                Previous
              </button>
              <button
                type="button"
                className="btn-outline"
                onClick={() => void loadRequests(page + 1)}
                disabled={page + 1 >= totalPages || loadingRequests}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </section>
    </div>
  )
}

export default EmployeeRequestsPage

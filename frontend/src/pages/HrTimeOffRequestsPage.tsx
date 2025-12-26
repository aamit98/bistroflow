import React, { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '../security/AuthContext'
import { useToast } from '../components/ToastContext'
import {
  getBranchTimeOffRequestsApi,
  decideTimeOffRequestApi,
  type TimeOffRequest,
} from '../api/TimeOffApi'
import { apiClient } from '../api/ApiClient'
import { subscribeToBranch, type NotificationMessage } from '../api/NotificationSocket'
import { getApiErrorMessage } from '../utils/apiError'
import './HrTimeOffRequestsPage.css'

interface TriageResult {
  category: string
  priority: string
  priorityScore: number
  confidence: number
  detectedKeywords: string[]
  suggestion: string
}

const HrTimeOffRequestsPage: React.FC = () => {
  const { branchId } = useParams<{ branchId: string }>()
  const [requests, setRequests] = useState<TimeOffRequest[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<
    'PENDING' | 'APPROVED' | 'REJECTED'
  >('PENDING')
  const PAGE_SIZE = 20
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [triageResults, setTriageResults] = useState<Record<number, TriageResult>>({})
  const [analyzingId, setAnalyzingId] = useState<number | null>(null)

  const { employee } = useAuth()
  const { addToast } = useToast()

  const analyzeRequest = useCallback(async (request: TimeOffRequest) => {
    if (triageResults[request.id]) return // Already analyzed
    
    setAnalyzingId(request.id)
    try {
      // Send context for real staffing analysis
      const res = await apiClient.post<TriageResult>('/ai/triage', { 
        reason: request.reason,
        employeeId: request.employeeId,
        branchId: request.branchId,
        date: request.date,
        shiftType: request.shiftType
      })
      setTriageResults(prev => ({ ...prev, [request.id]: res.data }))
    } catch (e: unknown) {
      console.error('Failed to analyze request:', e)
    } finally {
      setAnalyzingId(null)
    }
  }, [triageResults])

  const load = useCallback(async (targetPage: number) => {
    if (!branchId) return
    setLoading(true)
    setError(null)
    try {
      const res = await getBranchTimeOffRequestsApi(
        Number(branchId),
        statusFilter,
        targetPage,
        PAGE_SIZE,
      )
      setRequests(res.data.content)
      setPage(res.data.page)
      setTotalPages(res.data.totalPages)
      setTotalElements(res.data.totalElements)
      
    } catch (err: unknown) {
      console.error(err)
      setError(getApiErrorMessage(err, 'Failed to load requests'))
    } finally {
      setLoading(false)
    }
  }, [branchId, statusFilter, PAGE_SIZE])

  useEffect(() => {
    if (!employee) return
    if (!employee.isHRManager) {
      setError('Access denied: not an HR manager')
      return
    }
    void load(0)
  }, [branchId, statusFilter, employee, load])

  // Subscribe to notifications independently (only when branchId changes)
  useEffect(() => {
    if (!branchId || !employee || !employee.isHRManager) return

    const handleBranchNotification = (msg: NotificationMessage) => {
      if (msg.type === 'TIME_OFF_REQUEST') {
        addToast(`Employee #${msg.employeeId} submitted a time-off request!`, 'success')
        void load(0)
      }
    }
    subscribeToBranch(Number(branchId), handleBranchNotification)
  }, [branchId, employee, addToast, load])

  useEffect(() => {
    if (statusFilter !== 'PENDING') return
    for (const req of requests) {
      if (req.reason && !triageResults[req.id]) {
        analyzeRequest(req)
      }
    }
  }, [requests, statusFilter, triageResults, analyzeRequest])

  const handleDecision = async (id: number, approve: boolean) => {
    try {
      await decideTimeOffRequestApi(id, approve)
      void load(page)
    } catch (err: unknown) {
      console.error(err)
      setError(getApiErrorMessage(err, 'Failed to update request'))
    }
  }

  if (!branchId) return <p>Missing branch ID in URL.</p>

  return (
    <div className="page timeoff-page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Time-off requests</h2>
          <p className="page-subtitle">Branch {branchId}</p>
        </div>
        <div className="timeoff-summary">
          <span className="summary-chip">Filter: {statusFilter}</span>
          <span className="summary-chip summary-chip-ok">
            Showing {requests.length === 0 ? 0 : page * PAGE_SIZE + 1} –
            {requests.length === 0
              ? 0
              : Math.min(totalElements, page * PAGE_SIZE + requests.length)}{' '}
            of {totalElements}
          </span>
        </div>
      </div>

      <div className="card timeoff-controls">
        <div className="control-group">
          <label htmlFor="status-filter">Status</label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(
                e.target.value as 'PENDING' | 'APPROVED' | 'REJECTED',
              )
            }
          >
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>
        <button type="button" className="btn-outline" onClick={() => void load(page)}>
          Refresh
        </button>
      </div>

      <div className="card">
        {loading && <p>Loading…</p>}
        {error && <p className="form-error">{error}</p>}

        {!loading && requests.length === 0 && (
          <p className="empty-state">No requests for this filter.</p>
        )}

        {!loading && requests.length > 0 && (
          <div className="timeoff-table-wrapper">
            <table className="timeoff-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Employee</th>
                  <th>Requested date</th>
                  <th>Shift</th>
                  <th>Reason</th>
                  <th>🤖 AI Analysis</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((r) => {
                  const triage = triageResults[r.id]
                  return (
                    <tr key={r.id}>
                      <td>#{r.id}</td>
                      <td>Employee #{r.employeeId}</td>
                      <td>{r.date}</td>
                      <td>
                        <span className="shift-pill">{r.shiftType}</span>
                      </td>
                      <td>{r.reason || <em className="muted-text">No reason</em>}</td>
                      <td>
                        {analyzingId === r.id ? (
                          <span className="ai-analyzing">🔄 Analyzing...</span>
                        ) : triage ? (
                          <div className="ai-triage">
                            <span className={`priority-badge priority-${triage.priority.toLowerCase()}`}>
                              {triage.priority}
                            </span>
                            <span className="category-badge">{triage.category}</span>
                            {triage.confidence > 0.5 && (
                              <div className="ai-suggestion" title={triage.suggestion}>
                                💡 {triage.suggestion.slice(0, 40)}...
                              </div>
                            )}
                          </div>
                        ) : r.reason ? (
                          <button 
                            type="button" 
                            className="btn-sm"
                            onClick={() => analyzeRequest(r)}
                          >
                            🤖 Analyze
                          </button>
                        ) : (
                          <em className="muted-text">-</em>
                        )}
                      </td>
                      <td>
                        <span className={`status-pill status-${r.status.toLowerCase()}`}>
                          {r.status}
                        </span>
                      </td>
                      <td>
                        {r.status === 'PENDING' ? (
                          <div className="action-buttons">
                            <button
                              type="button"
                              className="btn-approve"
                              onClick={() => void handleDecision(r.id, true)}
                            >
                              ✓ Approve
                            </button>
                            <button
                              type="button"
                              className="btn-reject"
                              onClick={() => void handleDecision(r.id, false)}
                            >
                              ✗ Reject
                            </button>
                          </div>
                        ) : (
                          <em className="muted-text">Reviewed</em>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
            <button
              type="button"
              className="btn-outline"
              onClick={() => void load(page - 1)}
              disabled={page === 0 || loading}
            >
              Previous
            </button>
            <span className="page-indicator">
              Page {page + 1} of {totalPages}
            </span>
            <button
              type="button"
              className="btn-outline"
              onClick={() => void load(page + 1)}
              disabled={page + 1 >= totalPages || loading}
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default HrTimeOffRequestsPage

import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '../security/AuthContext'
import {
  getBranchTimeOffRequestsApi,
  decideTimeOffRequestApi,
  type TimeOffRequest,
} from '../api/TimeOffApi'
import { subscribeToBranch, type NotificationMessage } from '../api/NotificationSocket'

const HrTimeOffRequestsPage: React.FC = () => {
  const { branchId } = useParams<{ branchId: string }>()
  const [requests, setRequests] = useState<TimeOffRequest[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<
    'PENDING' | 'APPROVED' | 'REJECTED'
  >('PENDING')

  const { employee } = useAuth()

  const load = async () => {
    if (!branchId) return
    setLoading(true)
    setError(null)
    try {
      const res = await getBranchTimeOffRequestsApi(
        Number(branchId),
        statusFilter,
      )
      setRequests(res.data)
    } catch (err: any) {
      console.error(err)
      const status = err?.response?.status
      const body = err?.response?.data
      setError(status ? `Error ${status}: ${JSON.stringify(body)}` : (err?.message ?? 'Failed to load requests'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!employee) return
    if (!employee.isHRManager) {
      setError('Access denied: not an HR manager')
      return
    }
    void load()

    // Subscribe to real-time branch notifications
    const handleBranchNotification = (msg: NotificationMessage) => {
      if (msg.type === 'TIME_OFF_REQUEST') {
        // A new time-off request was submitted, refresh the list
        console.log('[HrTimeOff] Received new time-off request notification, refreshing...')
        void load()
      }
    }
    subscribeToBranch(Number(branchId), handleBranchNotification)
  }, [branchId, statusFilter, employee])

  const handleDecision = async (id: number, approve: boolean) => {
    try {
      await decideTimeOffRequestApi(id, approve)
      void load()
    } catch (err: any) {
      console.error(err)
      setError(err?.response?.data?.error ?? 'Failed to update request')
    }
  }

  if (!branchId) return <p>Missing branch ID in URL.</p>

  return (
    <div className="page">
      <h2>Time-off requests for branch {branchId}</h2>

      <div className="toolbar">
        <label>
          Status:&nbsp;
          <select
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
        </label>
        <button type="button" onClick={() => void load()}>
          Refresh
        </button>
      </div>

      {loading && <p>Loading…</p>}
      {error && <p className="form-error">{error}</p>}

      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Employee</th>
            <th>Date</th>
            <th>Shift</th>
            <th>Reason</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {requests.map((r) => (
            <tr key={r.id}>
              <td>{r.id}</td>
              <td>#{r.employeeId}</td>
              <td>{r.date}</td>
              <td>{r.shiftType}</td>
              <td>{r.reason}</td>
              <td>{r.status}</td>
              <td>
                {r.status === 'PENDING' ? (
                  <>
                    <button
                      type="button"
                      onClick={() => void handleDecision(r.id, true)}
                    >
                      Approve
                    </button>
                    <button
                      type="button"
                      onClick={() => void handleDecision(r.id, false)}
                    >
                      Reject
                    </button>
                  </>
                ) : (
                  <em>Reviewed</em>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default HrTimeOffRequestsPage

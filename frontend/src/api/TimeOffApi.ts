// src/api/TimeOffApi.ts
import { apiClient } from './ApiClient'

export type ShiftType = 'MORNING' | 'EVENING'

export interface TimeOffRequest {
  id: number
  employeeId: number
  branchId: number
  date: string
  shiftType: ShiftType
  reason: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt: string
  reviewedByEmployeeId?: number
  reviewedAt?: string
  decisionComment?: string
}

export interface CreateTimeOffRequestInput {
  date: string // YYYY-MM-DD
  shiftType: ShiftType
  reason: string
}

export const createTimeOffRequestApi = (
  employeeId: number,
  payload: CreateTimeOffRequestInput,
) =>
  apiClient.post<TimeOffRequest>(
    `/employees/${employeeId}/time-off-requests`,
    { reason: payload.reason },
    {
      params: {
        date: payload.date,
        shiftType: payload.shiftType,
      },
    },
  )

export const getBranchTimeOffRequestsApi = (
  branchId: number,
  status: 'PENDING' | 'APPROVED' | 'REJECTED' = 'PENDING',
) =>
  apiClient.get<TimeOffRequest[]>(
    `/hr/branches/${branchId}/time-off-requests`,
    {
      params: { status },
    },
  )

export const decideTimeOffRequestApi = (
  id: number,
  approve: boolean,
  comment?: string,
) =>
  apiClient.post<TimeOffRequest>(
    `/hr/time-off-requests/${id}/decision`,
    comment ? { comment } : {},
    {
      params: { approve },
    },
  )

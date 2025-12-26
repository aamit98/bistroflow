// src/api/HrApiService.ts
import { apiClient } from "./ApiClient";
import type { PagedResponse } from "./types";

export interface Employee {
  id: number
  name: string
  branchId: number | null  // null for super admin (all branches)
  bankCode: number
  bankBranchCode: number
  bankAccount: number
  hourlyRate: number
  monthlyRate: number
  isHRManager: boolean
  isSuperAdmin: boolean
  roles: string[]
  startDate: string
  termsOfEmployment: string
}

export interface LoginRequest {
  employeeId: number
  password: string
}

export interface LoginResponse {
  token: string
  employee: Employee
}

export interface EmployeeProfileSummary {
  employeeId: number
  name: string
  branchId: number
  roles: string[]
  hrManager: boolean
  hourlyRate: number
  monthlyRate: number
  termsOfEmployment: string
  startDate: string
  bankCode: number
  bankBranchCode: number
  bankAccount: number
}

export interface EmployeeAvailabilitySlot {
  dayOfWeek: string
  shiftType: string
  available: boolean
}

export interface EmployeeAvailabilitySection {
  weekStart: string
  slots: EmployeeAvailabilitySlot[]
  submitted: boolean
}

export interface EmployeeShiftView {
  assignmentId: number | null
  shiftDate: string
  shiftType: string
  status: string
  branchId: number
}

export interface EmployeeScheduleSection {
  weekStart: string
  weekEnd: string
  shifts: EmployeeShiftView[]
}

export interface EmployeeProfileResponse {
  profile: EmployeeProfileSummary
  availability: EmployeeAvailabilitySection
  schedule: EmployeeScheduleSection
}

export const loginApi = (payload: LoginRequest) =>
  apiClient.post<LoginResponse>('/auth/login', payload)

export const logoutApi = (employeeId: number) =>
  apiClient.post('/auth/logout', { employeeId })

// HR: get employees in a branch (enriched profiles)
export const getEmployeesInBranchApi = (branchId: number, page = 0, size = 25) =>
  apiClient.get<PagedResponse<Employee>>(`/hr/branches/${branchId}/employees`, {
    params: { page, size },
  })

// HR: get single employee profile (with availability + schedule)
export const getEmployeeDetailsApi = (employeeId: number, weekStart: string) =>
  apiClient.get<EmployeeProfileResponse>(`/employees/${employeeId}`, {
    params: { weekStart },
  })

// HR: add a new employee to a branch
export interface CreateEmployeePayload {
  id: number
  branchId?: number
  name: string
  termsOfEmployment?: string
  startDate: number // yyyymmdd
  bankCode: number
  bankBranchCode: number
  bankAccount: number
  hourlyRate: number
  monthlyRate: number
  roles: string[]
  password: string
}

export const createEmployeeApi = (branchId: number, payload: CreateEmployeePayload) =>
  apiClient.post(`/hr/branches/${branchId}/employees`, payload)

// HR: delete an employee from a branch
export const deleteEmployeeApi = (branchId: number, employeeId: number) =>
  apiClient.delete(`/hr/branches/${branchId}/employees/${employeeId}`)

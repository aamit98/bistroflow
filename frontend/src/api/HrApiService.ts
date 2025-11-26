// src/api/HrApiService.ts
import { apiClient } from "./ApiClient";

export interface Employee {
  id: number
  name: string
  branchId: number
  bankCode: number
  bankBranchCode: number
  bankAccount: number
  hourlyRate: number
  monthlyRate: number
  isHRManager: boolean
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

export interface EmployeeAvailability {
  weekStart: string
  slots: { day: string; shift: string }[]
}

export const loginApi = (payload: LoginRequest) =>
  apiClient.post<LoginResponse>('/auth/login', payload)

export const logoutApi = (employeeId: number) =>
  apiClient.post('/auth/logout', { employeeId })

// HR: get employees in a branch (enriched profiles)
export const getEmployeesInBranchApi = (branchId: number) =>
  apiClient.get<Employee[]>(`/hr/branches/${branchId}/employees`)

// HR: get single employee details (profile, roles, bank, rates, etc.)
export const getEmployeeDetailsApi = (employeeId: number) =>
  apiClient.get<Employee>(`/employees/${employeeId}`)

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

// HR: get employee weekly availability (manager view)
export const getEmployeeAvailabilityApi = (employeeId: number, weekStart: string) =>
  apiClient.get<EmployeeAvailability>(`/employees/${employeeId}/availability`, {
    params: { weekStart },
  })

export const updateEmployeeAvailabilityApi = (
  employeeId: number,
  availability: EmployeeAvailability,
) =>
  apiClient.put<EmployeeAvailability>(
    `/employees/${employeeId}/availability`,
    availability,
  )

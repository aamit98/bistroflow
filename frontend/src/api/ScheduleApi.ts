import { apiClient } from "./ApiClient"

export type ShiftType = "MORNING" | "EVENING"
export type DayOfWeekCode =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY"

export interface AvailabilitySlot {
  dayOfWeek: DayOfWeekCode
  shiftType: ShiftType
  available: boolean
}

export interface WeekAvailability {
  employeeId: number
  weekStart: string // "YYYY-MM-DD"
  slots: AvailabilitySlot[]
}

// Extended response from backend with status info
export interface AvailabilityResponse {
  employeeId: number
  weekStart: string
  slots: AvailabilitySlot[]
  editable: boolean
  editableReason: string
  schedulePublished: boolean
  publishedAt: string | null
}

export interface Shift {
  date: string
  shiftType: ShiftType
  branchId: number
  role: string
}

export interface WeekSchedule {
  employeeId: number
  weekStart: string
  shifts: Shift[]
}

// ===================== HR Schedule Types =====================

export interface RoleConstraint {
  role: string
  requiredCount: number
  assignedCount: number
}

export interface AssignedEmployee {
  employeeId: number
  name: string
  role: string
}

export interface ShiftCell {
  dayOfWeek: DayOfWeekCode
  shiftType: ShiftType
  roleConstraints: RoleConstraint[]
  assignedEmployees: AssignedEmployee[]
  totalRequired: number
  totalAssigned: number
}

export interface BranchSchedule {
  branchId: number
  weekStart: string
  shifts: ShiftCell[]
  published: boolean
  publishedAt: string | null
}

export interface EmployeeAvailabilityOverview {
  employeeId: number
  name: string
  roles: string[]
  availability: Record<string, boolean> // "SUNDAY-MORNING" -> true/false
}

export interface BranchAvailability {
  branchId: number
  weekStart: string
  employees: EmployeeAvailabilityOverview[]
}

// ===================== Employee Endpoints =====================

export function getEmployeeAvailability(
  employeeId: number,
  weekStart: string
) {
  return apiClient.get<AvailabilityResponse>(
    `/employees/${employeeId}/availability`,
    { params: { weekStart } }
  )
}

export function updateEmployeeAvailability(
  employeeId: number,
  weekStart: string,
  body: WeekAvailability
) {
  return apiClient.put(
    `/employees/${employeeId}/availability`,
    body,
    { params: { weekStart } }
  )
}

export function getEmployeeSchedule(employeeId: number, weekStart: string) {
  return apiClient.get<WeekSchedule>(
    `/employees/${employeeId}/schedule`,
    { params: { weekStart } }
  )
}

export function getNextEditableWeek(employeeId: number) {
  return apiClient.get<{ nextWeekStart: string; message: string }>(
    `/employees/${employeeId}/availability/next-week`
  )
}

// ===================== HR Endpoints =====================

export function getBranchSchedule(branchId: number, weekStart: string) {
  return apiClient.get<BranchSchedule>(
    `/hr/branches/${branchId}/schedule`,
    { params: { weekStart } }
  )
}

export function getBranchAvailability(branchId: number, weekStart: string) {
  return apiClient.get<BranchAvailability>(
    `/hr/branches/${branchId}/availability`,
    { params: { weekStart } }
  )
}

export function publishBranchSchedule(branchId: number, weekStart: string) {
  return apiClient.post<{ message: string; weekStart: string; publishedAt: string }>(
    `/hr/branches/${branchId}/schedule/publish`,
    null,
    { params: { weekStart } }
  )
}

// ===================== Helpers =====================

/**
 * Get next Sunday's date (for default availability week)
 */
export function getNextSundayISO(): string {
  const today = new Date()
  const dayOfWeek = today.getDay() // 0 = Sunday
  const daysUntilNextSunday = dayOfWeek === 0 ? 7 : 7 - dayOfWeek
  const nextSunday = new Date(today)
  nextSunday.setDate(today.getDate() + daysUntilNextSunday)
  return nextSunday.toISOString().slice(0, 10)
}

/**
 * Get current week's Sunday
 */
export function getCurrentSundayISO(): string {
  const today = new Date()
  const day = today.getDay() // 0 = Sunday
  const diff = day // days since Sunday
  const sunday = new Date(today)
  sunday.setDate(today.getDate() - diff)
  return sunday.toISOString().slice(0, 10)
}

/**
 * Format date for display
 */
export function formatWeekRange(weekStartISO: string): string {
  const start = new Date(weekStartISO)
  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  
  const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' }
  return `${start.toLocaleDateString('en-US', options)} - ${end.toLocaleDateString('en-US', options)}`
}

/**
 * Navigate weeks
 */
export function shiftWeek(weekStartISO: string, weeks: number): string {
  const date = new Date(weekStartISO)
  date.setDate(date.getDate() + weeks * 7)
  return date.toISOString().slice(0, 10)
}

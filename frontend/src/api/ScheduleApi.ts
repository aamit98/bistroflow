import { apiClient } from "./ApiClient"
import { addDaysISO, formatDateToISO, parseISODateToLocal } from "../utils/date"

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
  id: number | null     // Shift assignment ID for confirmation
  date: string
  day: string        // e.g. "Sunday", "Monday"
  shiftType: ShiftType
  branchId: number
  role: string
  confirmed: boolean
  confirmedAt: string | null
}

export interface WeekSchedule {
  employeeId: number
  weekStart: string
  shifts: Shift[]
  published: boolean
  allConfirmed: boolean
  message: string | null
}

export interface ConfirmationResponse {
  success: boolean
  message: string
}

// ===================== HR Schedule Types =====================

export interface RoleConstraint {
  role: string
  requiredCount: number
  assignedCount: number
}

export interface AssignedEmployee {
  assignmentId: number | null
  employeeId: number
  name: string
  role: string
  status: 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED'
  shiftDate: string
}

export interface ShiftCell {
  dayOfWeek: DayOfWeekCode
  shiftDate: string
  shiftType: ShiftType
  roleConstraints: RoleConstraint[]
  assignedEmployees: AssignedEmployee[]
  totalRequired: number
  totalAssigned: number
}

export interface BranchSchedule {
  branchId: number
  weekStart: string
  weekEnd: string
  shifts: ShiftCell[]
  published: boolean
  publishedAt: string | null
}

export interface ShiftAssignmentCandidate {
  id: number
  name: string
  roles: string[]
  hasRequiredRole: boolean
  availabilitySubmitted: boolean
  availableForShift: boolean
  timeOffConflict: boolean
  alreadyAssignedThisShift: boolean
  alreadyAssignedThisDay: boolean
  weeklyAssignments: number
  eligible: boolean
  blockingReasons: string[]
  suggestedRole: string | null
}

export interface CreateAssignmentRequest {
  employeeId: number
  shiftDate: string // YYYY-MM-DD
  shiftType: ShiftType
}

export interface CreateAssignmentResponse {
  assignmentId: number
  status: string
  shiftDate: string
  shiftType: ShiftType
  message: string
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

// ===================== Schedule Constraint Types =====================

export interface ScheduleConstraint {
  id?: number
  branchId: number
  shiftType: ShiftType
  roleRequired: string
  minRequired: number
  idealCount: number
}

export interface ScheduleAssignment {
  id: number
  branchId: number
  employeeId: number
  shiftDate: string
  shiftType: ShiftType
  status: 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED'
  createdAt: string
  updatedAt: string
}

export interface ScheduleConstraintViolation {
  branchId: number | null
  shiftDate: string | null
  shiftType: string | null
  roleRequired: string | null
  requiredCount: number
  actualCount: number
  message: string
}

export interface ScheduleGenerationResult {
  assignments: ScheduleAssignment[]
  violations: ScheduleConstraintViolation[]
  hasViolations: boolean
  totalAssignments: number
  totalViolations: number
}

export interface ShiftAssignmentCandidate {
  id: number
  name: string
  roles: string[]
  hasRequiredRole: boolean
  availabilitySubmitted: boolean
  availableForShift: boolean
  timeOffConflict: boolean
  alreadyAssignedThisShift: boolean
  alreadyAssignedThisDay: boolean
  weeklyAssignments: number
  eligible: boolean
  blockingReasons: string[]
  suggestedRole: string | null
}

export interface CreateAssignmentRequest {
  employeeId: number
  shiftDate: string      // 'YYYY-MM-DD'
  shiftType: ShiftType   // 'MORNING' | 'EVENING'
}
// ===================== Schedule Constraint Endpoints =====================

export function getScheduleConstraints(branchId: number, weekStart: string) {
  return apiClient.get<ScheduleConstraint[]>(
    `/hr/branches/${branchId}/schedule-constraints`,
    { params: { weekStart } }
  )
}

export function setScheduleConstraints(
  branchId: number,
  weekStart: string,
  constraints: Omit<ScheduleConstraint, 'id' | 'branchId'>[]
) {
  return apiClient.post(
    `/hr/branches/${branchId}/schedule-constraints`,
    constraints,
    { params: { weekStart } }
  )
}



export function updateAssignment(assignmentId: number, status: 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED') {
  return apiClient.post(
    `/hr/schedule-assignments/${assignmentId}`,
    { status }
  )
}

export function deleteAssignment(assignmentId: number) {
  return apiClient.delete(
    `/hr/schedule-assignments/${assignmentId}`
  )
}



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

/**
 * Confirm a single shift
 */
export function confirmShift(employeeId: number, shiftId: number) {
  return apiClient.post<ConfirmationResponse>(
    `/employees/${employeeId}/shifts/${shiftId}/confirm`
  )
}

/**
 * Confirm all shifts for a week at once
 */
export function confirmWeekShifts(employeeId: number, weekStart: string) {
  return apiClient.post<ConfirmationResponse>(
    `/employees/${employeeId}/weeks/${weekStart}/confirm-all`
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

/**
 * Generate an optimal schedule for the given week (Sun–Sat).
 * Uses constraints, availability, time-off and tries to balance workload.
 */
export function generateSchedule(branchId: number, weekStart: string) {
  return apiClient.get<ScheduleGenerationResult>(
    `/hr/branches/${branchId}/generate-schedule`,
    { params: { weekStart } }
  )
}

export function applyGeneratedSchedule(branchId: number, weekStart: string) {
  return apiClient.post<{ message: string; assignmentsApplied: number; hasViolations: boolean; totalViolations: number }>(
    `/hr/branches/${branchId}/schedule/apply`,
    null,
    { params: { weekStart } }
  )
}

/**
 * Get candidates for a specific shift (used by the HR “click cell to assign” panel)
 */
export function getShiftCandidates(
  branchId: number,
  shiftDate: string,
  shiftType: ShiftType
) {
  return apiClient.get<ShiftAssignmentCandidate[]>(
    `/hr/branches/${branchId}/schedule/candidates`,
    { params: { shiftDate, shiftType } }
  )
}

/**
 * Create a single assignment for a shift (HR manually assigns someone)
 */
export function createScheduleAssignment(
  branchId: number,
  body: CreateAssignmentRequest
) {
  return apiClient.post<ScheduleAssignment>(
    `/hr/branches/${branchId}/schedule/assignments`,
    body
  )
}
// ===================== Helpers =====================

/**
 * Get next Sunday's date (for default availability week)
 */
export function getNextSundayISO(): string {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const dayOfWeek = today.getDay() // 0 = Sunday
  const daysUntilNextSunday = dayOfWeek === 0 ? 7 : 7 - dayOfWeek
  today.setDate(today.getDate() + daysUntilNextSunday)
  return formatDateToISO(today)
}

/**
 * Get current week's Sunday
 */
export function getCurrentSundayISO(): string {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const day = today.getDay() // 0 = Sunday
  today.setDate(today.getDate() - day)
  return formatDateToISO(today)
}

/**
 * Format date for display
 */
export function formatWeekRange(weekStartISO: string): string {
  const start = parseISODateToLocal(weekStartISO)
  if (!start) return weekStartISO
  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  
  const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' }
  return `${start.toLocaleDateString('en-US', options)} - ${end.toLocaleDateString('en-US', options)}`
}

/**
 * Navigate weeks
 */
export function shiftWeek(weekStartISO: string, weeks: number): string {
  return addDaysISO(weekStartISO, weeks * 7)
}

export { normalizeToSundayISO } from "../utils/date"

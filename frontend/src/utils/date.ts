export function parseISODateToLocal(iso: string): Date | null {
  if (!iso) return null
  const parts = iso.split('-')
  if (parts.length !== 3) return null
  const [yearStr, monthStr, dayStr] = parts
  const year = Number(yearStr)
  const month = Number(monthStr)
  const day = Number(dayStr)
  if ([year, month, day].some((value) => Number.isNaN(value))) {
    return null
  }
  const date = new Date(year, month - 1, day)
  date.setHours(0, 0, 0, 0)
  return date
}

export function formatDateToISO(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function normalizeToSundayISO(iso: string): string {
  const date = parseISODateToLocal(iso)
  if (!date) return iso
  const day = date.getDay()
  date.setDate(date.getDate() - day)
  return formatDateToISO(date)
}

export function addDaysISO(iso: string, days: number): string {
  const date = parseISODateToLocal(iso)
  if (!date) return iso
  date.setDate(date.getDate() + days)
  return formatDateToISO(date)
}

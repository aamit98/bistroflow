import type { AxiosError } from 'axios'

type ApiErrorPayload = {
  error?: string
  message?: string
  detail?: string
}

export function getApiErrorMessage(error: unknown, fallback = 'Something went wrong'): string {
  const axiosError = error as AxiosError<ApiErrorPayload>

  const payloadMessage = axiosError?.response?.data?.error
    ?? axiosError?.response?.data?.message
    ?? axiosError?.response?.data?.detail

  if (payloadMessage && typeof payloadMessage === 'string') {
    return payloadMessage
  }

  if (axiosError?.message) {
    return axiosError.message
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

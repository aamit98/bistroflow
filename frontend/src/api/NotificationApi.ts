// src/api/NotificationApi.ts
import { apiClient } from './ApiClient'

export interface Notification {
  id: number
  employeeId: number
  title: string
  body: string
  read: boolean
  type: string
  createdAt: string
}

export const getNotificationsApi = () =>
  apiClient.get<Notification[]>('/notifications')

export const getUnreadCountApi = () =>
  apiClient.get<{ count: number }>('/notifications/unread-count')

export const markNotificationReadApi = (id: number) =>
  apiClient.post(`/notifications/${id}/read`)

export const clearNotificationsApi = () =>
  apiClient.post(`/notifications/clear`)

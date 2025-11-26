// src/api/ApiClient.ts
import axios from "axios"

export const apiClient = axios.create({
  baseURL: "/api",
})

export function setAuthToken(token: string | null) {
  if (token) {
    console.debug('[ApiClient] setAuthToken: setting token')
    apiClient.defaults.headers.common["Authorization"] = `Bearer ${token}`
  } else {
    console.debug('[ApiClient] setAuthToken: clearing token')
    delete apiClient.defaults.headers.common["Authorization"]
  }
}

// Debug: log outgoing requests (only in development) to help diagnose 403s
if (process.env.NODE_ENV === 'development') {
  apiClient.interceptors.request.use((cfg) => {
    try {
      if (cfg.url && cfg.url.includes('/hr/')) {
        console.debug('[ApiClient] outgoing HR request', cfg.method, cfg.url, 'Authorization=', cfg.headers?.Authorization)
      }
    } catch (e) {
      // ignore
    }
    return cfg
  })
}

import {
  createContext,
  useContext,
  useState,
  useEffect,
  type ReactNode,
} from 'react'
import {
  loginApi,
  logoutApi,
  type Employee,
  type LoginResponse,
} from '../api/HrApiService'
import { setAuthToken } from '../api/ApiClient'

interface AuthContextValue {
  isAuthenticated: boolean
  employee: Employee | null
  token: string | null
  login: (employeeId: number, password: string) => Promise<Employee>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}

const STORAGE_KEY = 'bistroflow-auth'

export default function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setAuthenticated] = useState(false)
  const [employee, setEmployee] = useState<Employee | null>(null)
  const [token, setToken] = useState<string | null>(null)

  // 🔄 Rehydrate from localStorage on first load
  useEffect(() => {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return

    try {
      const parsed = JSON.parse(raw) as {
        token: string
        employee: Employee
      }
      setAuthenticated(true)
      setEmployee(parsed.employee)
      setToken(parsed.token)
      setAuthToken(parsed.token)
    } catch (e) {
      console.warn('Failed to parse stored auth, clearing it', e)
      localStorage.removeItem(STORAGE_KEY)
    }
  }, [])

  async function login(employeeId: number, password: string): Promise<Employee> {
    const response = await loginApi({ employeeId, password })
    const data: LoginResponse = response.data

    setAuthenticated(true)
    setEmployee(data.employee)
    setToken(data.token)
    setAuthToken(data.token)

    // persist
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        token: data.token,
        employee: data.employee,
      })
    )

    return data.employee
  }

  function logout() {
    ;(async () => {
      if (employee) {
        try {
          await logoutApi(employee.id)
        } catch (e) {
          console.error('Logout API failed (ignoring):', e)
        }
      }
      setAuthenticated(false)
      setEmployee(null)
      setToken(null)
      setAuthToken(null)
      localStorage.removeItem(STORAGE_KEY)
    })()
  }

  return (
    <AuthContext.Provider
      value={{ isAuthenticated, employee, token, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}

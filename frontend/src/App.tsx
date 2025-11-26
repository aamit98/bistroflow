import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './security/AuthContext'
import DebugPanel from './components/DebugPanel'

import HrLayout from './layout/HrLayout'
import EmployeeLayout from './layout/EmployeeLayout'

import LoginPage from './pages/LoginPage'
import HrDashboardPage from './pages/HrDashboardPage'
import EmployeeListPage from './pages/EmployeeListPage'
import EmployeeDetailsPage from './pages/EmployeeDetailsPage'
import EmployeeAvailabilityPage from './pages/EmployeeAvailabilityPage'
import EmployeeSchedulePage from './pages/EmployeeSchedulePage'
import HrBranchSchedulePage from './pages/HrBranchSchedulePage'
import EmployeeProfilePage from './pages/EmployeeProfilePage'
import EmployeeRequestsPage from './pages/EmployeeRequestsPage'
import HrTimeOffRequestsPage from './pages/HrTimeOffRequestsPage'

const App: React.FC = () => {
  const { isAuthenticated, employee } = useAuth()

  return (
    <>
      <DebugPanel />
      <Routes>
        {/* Login is always available */}
        <Route path="/login" element={<LoginPage />} />

        {/* HR area (same idea as before, just with new routes) */}
        <Route path="/hr" element={<HrLayout />}>
          <Route index element={<HrDashboardPage />} />
          <Route
            path="branches/:branchId/schedule"
            element={<HrBranchSchedulePage />}
          />
          <Route
            path="branches/:branchId/employees"
            element={<EmployeeListPage />}
          />
          <Route
            path="branches/:branchId/time-off"
            element={<HrTimeOffRequestsPage />}
          />
          <Route path="employees/:employeeId" element={<EmployeeDetailsPage />} />
        </Route>

        {/* Employee area */}
        <Route path="/me" element={<EmployeeLayout />}>
          <Route index element={<EmployeeSchedulePage />} />
          <Route path="availability" element={<EmployeeAvailabilityPage />} />
          <Route path="profile" element={<EmployeeProfilePage />} />
          <Route path="requests" element={<EmployeeRequestsPage />} />
        </Route>

        {/* Fallback – send user to right home depending on auth + role */}
        <Route
          path="*"
          element={
            isAuthenticated
              ? employee?.isHRManager
                ? <Navigate to="/hr" replace />
                : <Navigate to="/me" replace />
              : <Navigate to="/login" replace />
          }
        />
      </Routes>
    </>
  )
}

export default App

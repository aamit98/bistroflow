import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './security/AuthContext'
import { ToastProvider } from './components/ToastContext'
import { BranchProvider } from './context/BranchContext'
import ToastContainer from './components/ToastContainer'
import DebugPanel from './components/DebugPanel'

import HrLayout from './layout/HrLayout'
import EmployeeLayout from './layout/EmployeeLayout'
import AdminLayout from './layout/AdminLayout'

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
import BranchSettingsPage from './pages/BranchSettingsPage'
import InventoryPage from './pages/InventoryPage'
import RoleManagerPage from './pages/RoleManagerPage'
import AdminDashboardPage from './pages/AdminDashboardPage'
import AdminHrManagersPage from './pages/AdminHrManagersPage'
import AdminRestaurantsPage from './pages/AdminRestaurantsPage'
import AdminBranchesPage from './pages/AdminBranchesPage'
import AdminAnalyticsPage from './pages/AdminAnalyticsPage'
import AdminSettingsPage from './pages/AdminSettingsPage'
import AdminActivityLogsPage from './pages/AdminActivityLogsPage'

// Wrapper component for HR routes with branch context
const HrRoutesWrapper: React.FC = () => (
  <BranchProvider>
    <HrLayout />
  </BranchProvider>
)

const App: React.FC = () => {
  const { isAuthenticated, isSuperAdmin, isHrManager } = useAuth()

  return (
    <ToastProvider>
      <>
        <DebugPanel />
        <ToastContainer />
        <Routes>
        {/* Login is always available */}
        <Route path="/login" element={<LoginPage />} />

        {/* Super Admin area */}
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<AdminDashboardPage />} />
          <Route path="hr-managers" element={<AdminHrManagersPage />} />
          <Route path="restaurants" element={<AdminRestaurantsPage />} />
          <Route path="branches" element={<AdminBranchesPage />} />
          <Route path="analytics" element={<AdminAnalyticsPage />} />
          <Route path="settings" element={<AdminSettingsPage />} />
          <Route path="logs" element={<AdminActivityLogsPage />} />
        </Route>

        {/* HR area (same idea as before, just with new routes) */}
        <Route path="/hr" element={<HrRoutesWrapper />}>
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
          <Route path="branch-settings" element={<BranchSettingsPage />} />
          <Route path="branches/:branchId/roles" element={<RoleManagerPage />} />
          <Route path="branches/:branchId/inventory" element={<InventoryPage />} />
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
              ? isSuperAdmin
                ? <Navigate to="/admin" replace />
                : isHrManager
                  ? <Navigate to="/hr" replace />
                  : <Navigate to="/me" replace />
              : <Navigate to="/login" replace />
          }
        />
      </Routes>
      </>
    </ToastProvider>
  )
}

export default App

import React from 'react'
import { useAuth } from '../security/AuthContext'

const EmployeeProfilePage: React.FC = () => {
  const { employee } = useAuth()

  if (!employee) return null

  return (
    <div className="page">
      <h2>My profile</h2>
      <section className="card">
        <h3 className="card-title">Personal info</h3>
        <p>
          <strong>Name:</strong> {employee.name}
        </p>
        <p>
          <strong>Employee ID:</strong> {employee.id}
        </p>
        <p>
          <strong>Branch:</strong> {employee.branchId}
        </p>
        <p>
          <strong>Roles:</strong> {employee.roles.join(', ') || '—'}
        </p>
      </section>

      <section className="card">
        <h3 className="card-title">Employment</h3>
        <p>
          <strong>Terms of employment:</strong> {employee.termsOfEmployment || '—'}
        </p>
        <p>
          <strong>Hourly rate:</strong> ₪{employee.hourlyRate}
        </p>
        <p>
          <strong>Monthly rate:</strong> ₪{employee.monthlyRate}
        </p>
        <p>
          <strong>Start date:</strong> {employee.startDate}
        </p>
      </section>

      <section className="card">
        <h3 className="card-title">Bank details</h3>
        <p>
          <strong>Bank:</strong> {employee.bankCode}
        </p>
        <p>
          <strong>Branch:</strong> {employee.bankBranchCode}
        </p>
        <p>
          <strong>Account:</strong> {employee.bankAccount}
        </p>
      </section>
    </div>
  )
}

export default EmployeeProfilePage

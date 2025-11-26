// src/pages/HrDashboardPage.tsx
import React from 'react'
import { useAuth } from '../security/AuthContext'

const HrDashboardPage: React.FC = () => {
  const { employee } = useAuth()

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2 className="page-title">HR overview</h2>
          <p className="page-subtitle">
            Welcome{employee ? `, ${employee.name}` : ''}. Manage people,
            schedules and inventory across your branches.
          </p>
        </div>
      </div>

      {/* Top metrics row */}
      <section className="card">
        <div className="profile-grid">
          <div>
            <h3 className="card-title">Branch snapshot</h3>
            <p className="card-subtitle">
              Quick overview of your main branch.
            </p>
            <ul style={{ marginTop: '0.6rem', paddingLeft: '1.1rem' }}>
              <li>Employees: <strong>12</strong> (5 full-time, 7 part-time)</li>
              <li>Open positions: <strong>2</strong> (cashier, cook)</li>
              <li>Active shifts this week: <strong>24</strong></li>
            </ul>
          </div>

          <div>
            <h3 className="card-title">Scheduling</h3>
            <p className="card-subtitle">
              Availability vs actual coverage for this week.
            </p>
            <ul style={{ marginTop: '0.6rem', paddingLeft: '1.1rem' }}>
              <li>Morning shifts covered: <strong>90%</strong></li>
              <li>Evening shifts covered: <strong>82%</strong></li>
              <li>Conflicts to review: <strong>3</strong></li>
            </ul>
          </div>

          <div>
            <h3 className="card-title">Inventory health</h3>
            <p className="card-subtitle">
              Based on branch stock and reorder thresholds.
            </p>
            <ul style={{ marginTop: '0.6rem', paddingLeft: '1.1rem' }}>
              <li>Low-stock items: <strong>5</strong></li>
              <li>Open supplier orders: <strong>2</strong></li>
              <li>Expiring discounts: <strong>1</strong></li>
            </ul>
          </div>
        </div>
      </section>

      {/* Quick actions row */}
      <section className="card" style={{ marginTop: '1rem' }}>
        <div className="card-header-row">
          <div>
            <h3 className="card-title">Quick actions</h3>
            <p className="card-subtitle">
              Common tasks you&apos;ll handle every shift.
            </p>
          </div>
        </div>

        <div className="profile-grid" style={{ marginTop: '0.75rem' }}>
          <div>
            <h4 className="card-title">People</h4>
            <ul style={{ paddingLeft: '1.1rem', fontSize: '0.9rem' }}>
              <li>Review employees in branch 1</li>
              <li>Open an employee profile &amp; availability</li>
              <li>Onboard a new hire (coming soon)</li>
            </ul>
          </div>

          <div>
            <h4 className="card-title">Schedule</h4>
            <ul style={{ paddingLeft: '1.1rem', fontSize: '0.9rem' }}>
              <li>Review weekly shift coverage</li>
              <li>Resolve conflicts vs availability</li>
              <li>Publish the branch schedule</li>
            </ul>
          </div>

          <div>
            <h4 className="card-title">Inventory</h4>
            <ul style={{ paddingLeft: '1.1rem', fontSize: '0.9rem' }}>
              <li>Check low-stock ingredients</li>
              <li>Create a supplier order</li>
              <li>Adjust branch discounts</li>
            </ul>
          </div>
        </div>
      </section>
    </div>
  )
}

export default HrDashboardPage

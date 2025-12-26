import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../security/AuthContext'
import '../styles/premium.css'

const LoginPage: React.FC = () => {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [employeeId, setEmployeeId] = useState('1')
  const [password, setPassword] = useState('hrManager')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    
    // Validate employee ID
    const empIdNum = Number(employeeId)
    if (!employeeId || employeeId.trim() === '' || isNaN(empIdNum) || empIdNum <= 0) {
      setError('Employee ID is required and must be a valid number')
      return
    }
    
    // Validate password
    if (!password || password.trim() === '') {
      setError('Password is required')
      return
    }
    
    setLoading(true)
    try {
      const emp = await login(empIdNum, password)

      // Redirect based on role - Super Admin first, then HR, then regular employee
      if (emp.isSuperAdmin) {
        navigate('/admin', { replace: true })
      } else if (emp.isHRManager) {
        navigate('/hr', { replace: true })
      } else {
        navigate('/me', { replace: true })
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message || 'Invalid employee ID or password')
      } else {
        setError('Invalid employee ID or password')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0a0a1a 0%, #1a2a3e 50%, #0f0f23 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px'
    }}>
      <div style={{
        width: '100%',
        maxWidth: '420px'
      }}>
        {/* Logo */}
        <div style={{
          textAlign: 'center',
          marginBottom: '40px'
        }}>
          <img 
            src="/assets/bistro-flow-logo.svg" 
            alt="BistroFlow Logo"
            style={{
              width: '80px',
              height: '80px',
              marginBottom: '20px'
            }}
          />
          <h1 style={{
            margin: '0',
            fontSize: '32px',
            fontWeight: '700',
            color: '#fff',
            letterSpacing: '-0.5px'
          }}>
            <span style={{ color: '#3D5A80' }}>Bistro</span>
            <span style={{ color: '#98C1A9' }}>Flow</span>
          </h1>
          <p style={{
            margin: '8px 0 0',
            color: 'rgba(255, 255, 255, 0.6)',
            fontSize: '15px'
          }}>
            Restaurant Management System
          </p>
        </div>

        {/* Login Card */}
        <form 
          onSubmit={handleSubmit}
          noValidate
          autoComplete="off"
          style={{
            background: 'rgba(26, 26, 46, 0.8)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: '20px',
            padding: '36px',
            boxShadow: '0 25px 50px rgba(0, 0, 0, 0.4)'
          }}
        >
          <h2 style={{
            margin: '0 0 8px',
            fontSize: '22px',
            fontWeight: '600',
            color: '#fff'
          }}>
            Welcome back
          </h2>
          <p style={{
            margin: '0 0 28px',
            color: 'rgba(255, 255, 255, 0.5)',
            fontSize: '14px'
          }}>
            Sign in with your employee credentials
          </p>

          <div className="bf-form-group">
            <label className="bf-form-label">Employee ID</label>
            <input
              className="bf-form-input"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              name="employeeId"
              autoComplete="off"
              value={employeeId}
              onChange={(e) => setEmployeeId(e.target.value.replace(/\D/g, ''))}
              placeholder="Enter your employee ID"
            />
          </div>

          <div className="bf-form-group">
            <label className="bf-form-label">Password</label>
            <input
              className="bf-form-input"
              type="password"
              name="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
            />
          </div>

          {error && (
            <div style={{
              padding: '12px 16px',
              background: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.25)',
              borderRadius: '10px',
              color: '#ef4444',
              fontSize: '14px',
              marginBottom: '18px'
            }}>
              {error}
            </div>
          )}

          <button 
            className="bf-btn bf-btn-primary" 
            type="submit" 
            disabled={loading}
            style={{ 
              width: '100%',
              padding: '14px',
              fontSize: '15px',
              marginTop: '8px'
            }}
          >
            {loading ? (
              <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }}>
                <span style={{
                  width: '18px',
                  height: '18px',
                  border: '2px solid rgba(255,255,255,0.3)',
                  borderTopColor: '#fff',
                  borderRadius: '50%',
                  animation: 'spin 0.8s linear infinite'
                }}></span>
                Signing in...
              </span>
            ) : (
              'Sign In'
            )}
          </button>

          {/* Demo Credentials */}
          <div style={{
            marginTop: '28px',
            padding: '18px',
            background: 'rgba(61, 90, 128, 0.15)',
            borderRadius: '12px',
            border: '1px solid rgba(61, 90, 128, 0.25)'
          }}>
            <p style={{
              margin: '0 0 12px',
              fontSize: '12px',
              fontWeight: '600',
              color: '#7aa3d1',
              textTransform: 'uppercase',
              letterSpacing: '1px'
            }}>
              📋 Demo Accounts
            </p>
            <div style={{
              display: 'grid',
              gap: '6px',
              fontSize: '12px',
              color: 'rgba(255, 255, 255, 0.75)'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>🔑 HR Manager (Recommended):</span>
                <span style={{ fontFamily: 'monospace', color: '#98C1A9', fontWeight: '600' }}>1 / hrManager</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>👤 Super Admin:</span>
                <span style={{ fontFamily: 'monospace', color: '#fff' }}>999999999 / admin123</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '4px', paddingTop: '8px', borderTop: '1px solid rgba(255,255,255,0.1)' }}>
                <span>🏢 Branch Managers:</span>
                <span style={{ fontFamily: 'monospace', color: '#fff' }}>2, 6 / password</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>👥 All Employees:</span>
                <span style={{ fontFamily: 'monospace', color: '#fff' }}>2-15 / password</span>
              </div>
              <div style={{ 
                marginTop: '8px', 
                paddingTop: '8px', 
                borderTop: '1px solid rgba(255,255,255,0.1)',
                fontSize: '11px',
                color: 'rgba(255, 255, 255, 0.5)',
                fontStyle: 'italic'
              }}>
                💡 Login as HR Manager (ID: 1) to see full demo features
              </div>
            </div>
          </div>
        </form>

        {/* Footer */}
        <p style={{
          textAlign: 'center',
          marginTop: '24px',
          color: 'rgba(255, 255, 255, 0.4)',
          fontSize: '13px'
        }}>
          © 2025 BistroFlow • Restaurant ERP
        </p>
      </div>
    </div>
  )
}

export default LoginPage

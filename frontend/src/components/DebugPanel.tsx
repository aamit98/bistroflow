import React, { useState } from 'react'
import { useAuth } from '../security/AuthContext'

const DebugPanel: React.FC = () => {
  const { employee, token, isAuthenticated } = useAuth()
  const [isExpanded, setIsExpanded] = useState(false)

  return (
    <div style={{ position: 'fixed', bottom: 0, right: 0, zIndex: 9999 }}>
      <button
        type="button"
        onClick={() => setIsExpanded(!isExpanded)}
        style={{
          padding: '8px 12px',
          background: '#333',
          color: '#fff',
          border: 'none',
          cursor: 'pointer',
          borderRadius: '4px 4px 0 0',
        }}
      >
        {isExpanded ? '▼' : '▲'} Debug
      </button>
      
      {isExpanded && (
        <div
          style={{
            background: '#f0f0f0',
            border: '1px solid #999',
            padding: '12px',
            width: '350px',
            maxHeight: '300px',
            overflowY: 'auto',
            fontFamily: 'monospace',
            fontSize: '12px',
            lineHeight: '1.5',
          }}
        >
          <p><strong>Auth Debug Info</strong></p>
          <p>Authenticated: {isAuthenticated ? 'YES' : 'NO'}</p>
          <p>Employee ID: {employee?.id ?? 'N/A'}</p>
          <p>HR Manager: {employee?.isHRManager ? 'YES' : 'NO'}</p>
          <p>Branch ID: {employee?.branchId ?? 'N/A'}</p>
          <p>
            Token:
            <br />
            {token ? token.substring(0, 50) + '...' : 'N/A'}
          </p>
          <p>
            LocalStorage auth:
            <br />
            {(() => {
              try {
                const stored = localStorage.getItem('bistroflow-auth')
                if (!stored) return '(empty)'
                const parsed = JSON.parse(stored)
                return `emp=${parsed.employee?.id}, hasToken=${!!parsed.token}`
              } catch (e) {
                return '(parse error)'
              }
            })()}
          </p>
          <button
            type="button"
            onClick={() => {
              console.log('Auth state:', { isAuthenticated, employee, token })
              console.log('localStorage bistroflow-auth:', localStorage.getItem('bistroflow-auth'))
            }}
            style={{ marginTop: '8px', padding: '4px 8px' }}
          >
            Log to console
          </button>
        </div>
      )}
    </div>
  )
}

export default DebugPanel

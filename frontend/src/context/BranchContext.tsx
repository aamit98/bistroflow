// src/context/BranchContext.tsx
// Provides shared branch selection state for HR pages

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import BranchApi, { type Branch } from '../api/BranchApi'
import { useAuth } from '../security/AuthContext'

interface BranchContextType {
  branches: Branch[]
  selectedBranchId: number | null
  selectedBranch: Branch | null
  setSelectedBranchId: (id: number) => void
  loading: boolean
  error: string | null
  refresh: () => Promise<void>
}

const BranchContext = createContext<BranchContextType | undefined>(undefined)

export function BranchProvider({ children }: { children: ReactNode }) {
  const { employee } = useAuth()
  const [branches, setBranches] = useState<Branch[]>([])
  const [selectedBranchId, setSelectedBranchIdState] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadBranches = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const branchList = await BranchApi.getAll(true) // active branches only
      setBranches(branchList)
      
      // If no branch selected yet, default to employee's branch or first branch
      setSelectedBranchIdState(prev => {
        if (!prev) {
          if (employee?.branchId && branchList.some(b => b.id === employee.branchId)) {
            return employee.branchId
          } else if (branchList.length > 0) {
            return branchList[0].id
          }
        }
        return prev
      })
    } catch (e: unknown) {
      console.error('Failed to load branches:', e)
      const msg = e instanceof Error ? e.message : 'Failed to load branches'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [employee?.branchId])

  useEffect(() => {
    loadBranches()
  }, [loadBranches])

  // Initialize with employee's branch when it becomes available
  useEffect(() => {
    if (employee?.branchId && branches.length > 0) {
      setSelectedBranchIdState(prev => {
        if (!prev && branches.some(b => b.id === employee.branchId)) {
          return employee.branchId!
        }
        return prev
      })
    }
  }, [employee?.branchId, branches])

  const selectedBranch = branches.find(b => b.id === selectedBranchId) || null

  const setSelectedBranchId = useCallback((id: number) => {
    setSelectedBranchIdState(id)
  }, [])

  const value: BranchContextType = {
    branches,
    selectedBranchId,
    selectedBranch,
    setSelectedBranchId,
    loading,
    error,
    refresh: loadBranches
  }

  return (
    <BranchContext.Provider value={value}>
      {children}
    </BranchContext.Provider>
  )
}

export function useBranch(): BranchContextType {
  const context = useContext(BranchContext)
  if (context === undefined) {
    throw new Error('useBranch must be used within a BranchProvider')
  }
  return context
}

export default BranchContext

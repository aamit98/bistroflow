// src/api/BranchRoleApi.ts
import { apiClient } from "./ApiClient";

/**
 * Branch role - custom roles defined per branch.
 * baseHourlyRate is in agorot (100 agorot = 1 NIS)
 */
export interface BranchRole {
  id: number;
  code: string;
  displayName: string;
  description?: string;
  color: string;
  icon?: string;
  baseHourlyRate: number; // in agorot (3350 = ₪33.50)
  requiresCertification: boolean;
  canSupervise: boolean;
  sortOrder: number;
  active: boolean;
}

export interface CreateRolePayload {
  code: string;
  displayName: string;
  description?: string;
  color?: string;
  icon?: string;
  baseHourlyRate?: number;
  requiresCertification?: boolean;
  canSupervise?: boolean;
  sortOrder?: number;
}

export interface UpdateRolePayload {
  displayName?: string;
  description?: string;
  color?: string;
  icon?: string;
  baseHourlyRate?: number;
  requiresCertification?: boolean;
  canSupervise?: boolean;
  sortOrder?: number;
  active?: boolean;
}

/**
 * Get all roles for a branch
 * @param branchId - The branch ID
 * @param activeOnly - If true, only return active roles
 */
export const getBranchRolesApi = (branchId: number, activeOnly = false) =>
  apiClient.get<BranchRole[]>(`/branches/${branchId}/roles`, {
    params: activeOnly ? { active: true } : undefined,
  });

/**
 * Get a single role
 */
export const getBranchRoleApi = (branchId: number, roleId: number) =>
  apiClient.get<BranchRole>(`/branches/${branchId}/roles/${roleId}`);

/**
 * Create a new role for a branch
 */
export const createBranchRoleApi = (branchId: number, payload: CreateRolePayload) =>
  apiClient.post<BranchRole>(`/branches/${branchId}/roles`, payload);

/**
 * Update an existing role
 */
export const updateBranchRoleApi = (branchId: number, roleId: number, payload: UpdateRolePayload) =>
  apiClient.put<BranchRole>(`/branches/${branchId}/roles/${roleId}`, payload);

/**
 * Delete (soft-delete / deactivate) a role
 */
export const deleteBranchRoleApi = (branchId: number, roleId: number) =>
  apiClient.delete(`/branches/${branchId}/roles/${roleId}`);

/**
 * Helper: Convert agorot to NIS for display
 */
export const agorotToNis = (agorot: number): string => {
  return `₪${(agorot / 100).toFixed(2)}`;
};

/**
 * Helper: Convert NIS to agorot for storage
 */
export const nisToAgorot = (nis: number): number => {
  return Math.round(nis * 100);
};

// src/api/AdminApi.ts
import { apiClient } from "./ApiClient";

// ===== RESTAURANT TYPES =====

export interface Restaurant {
  id: number;
  name: string;
  businessId: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  active: boolean;
  hrManagerId: number | null;
  hrManagerName: string | null;
  branchCount: number;
  employeeCount: number;
  createdAt: string | null;
}

export interface CreateRestaurantPayload {
  name: string;
  businessId?: string;
  contactEmail?: string;
  contactPhone?: string;
}

// ===== BRANCH TYPES =====

export interface BranchSummary {
  id: number;
  name: string;
  address: string;
  city: string;
  active: boolean;
  restaurantId: number | null;
  restaurantName: string | null;
  employeeCount: number;
}

export interface CreateBranchPayload {
  name: string;
  address: string;
  city?: string;
  restaurantId?: number;
}

// ===== HR MANAGER TYPES =====

export interface HrManager {
  id: number;
  name: string;
  restaurantId: number | null;  // HR managers are assigned to restaurants, not branches
  restaurantName: string | null;
  branchCount: number;          // Number of branches under their restaurant
  email?: string;
  createdAt?: string;
}

export interface CreateHrManagerPayload {
  id: number;
  name: string;
  password: string;
  restaurantId?: number;  // Assign to restaurant, not branch
}

// ===== STATS TYPES =====

export interface SystemStats {
  totalRestaurants: number;
  activeRestaurants: number;
  totalBranches: number;
  activeBranches: number;
  totalHrManagers: number;
  totalEmployees: number;
}

// ===== RESTAURANT API =====

export const getAllRestaurantsApi = () =>
  apiClient.get<Restaurant[]>('/admin/restaurants');

export const getRestaurantApi = (id: number) =>
  apiClient.get<Restaurant>(`/admin/restaurants/${id}`);

export const createRestaurantApi = (payload: CreateRestaurantPayload) =>
  apiClient.post<Restaurant>('/admin/restaurants', payload);

export const updateRestaurantApi = (id: number, payload: Partial<CreateRestaurantPayload>) =>
  apiClient.put<Restaurant>(`/admin/restaurants/${id}`, payload);

export const deactivateRestaurantApi = (id: number) =>
  apiClient.put<Restaurant>(`/admin/restaurants/${id}/deactivate`, {});

export const activateRestaurantApi = (id: number) =>
  apiClient.put<Restaurant>(`/admin/restaurants/${id}/activate`, {});

export const getRestaurantBranchesApi = (restaurantId: number) =>
  apiClient.get<BranchSummary[]>(`/admin/restaurants/${restaurantId}/branches`);

// ===== BRANCH API =====

export const getAllBranchesApi = () =>
  apiClient.get<BranchSummary[]>('/admin/branches');

export const createBranchApi = (payload: CreateBranchPayload) =>
  apiClient.post<BranchSummary>('/admin/branches', payload);

export const deactivateBranchApi = (branchId: number) =>
  apiClient.put<BranchSummary>(`/admin/branches/${branchId}/deactivate`, {});

export const activateBranchApi = (branchId: number) =>
  apiClient.put<BranchSummary>(`/admin/branches/${branchId}/activate`, {});

// ===== HR MANAGER API =====

export const getAllHrManagersApi = () =>
  apiClient.get<HrManager[]>('/admin/hr-managers');

export const createHrManagerApi = (payload: CreateHrManagerPayload) =>
  apiClient.post<HrManager>('/admin/hr-managers', payload);

// Update restaurant assignment for HR manager
export const updateHrManagerRestaurantApi = (hrManagerId: number, restaurantId: number | null) =>
  apiClient.put<HrManager>(`/admin/hr-managers/${hrManagerId}/restaurant`, { restaurantId });

export const removeHrManagerApi = (hrManagerId: number) =>
  apiClient.delete(`/admin/hr-managers/${hrManagerId}`);

// ===== STATS API =====

export const getSystemStatsApi = () =>
  apiClient.get<SystemStats>('/admin/stats');

/**
 * BistroFlow Security Tests - Data Isolation Validation
 * 
 * This focused test suite specifically checks for the data isolation bug
 * where HR managers could see other restaurants' data.
 * 
 * Run with: npx playwright test e2e/security.spec.ts
 */

import { test, expect, Page } from '@playwright/test';

interface AuthResponse {
  token: string;
  employeeId: number;
  isHrManager: boolean;
  isSuperAdmin: boolean;
  restaurantId?: number;
}

// API-level security tests are faster and more reliable
const API_BASE = 'http://localhost:8080/api';

async function getAuthToken(employeeId: string, password: string): Promise<AuthResponse | null> {
  try {
    const response = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ employeeId: parseInt(employeeId), password }),
    });
    if (response.ok) {
      return await response.json();
    }
    return null;
  } catch {
    return null;
  }
}

async function apiGet(path: string, token: string): Promise<{ status: number; data: any }> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Authorization': `Bearer ${token}` },
  });
  return {
    status: response.status,
    data: response.ok ? await response.json().catch(() => null) : null,
  };
}

// Branch IDs for testing
// HR Manager 1 (employeeId: 1) -> restaurant 129 -> branch 129
// HR Manager 2 (employeeId: 208278986) -> restaurant 97 -> branch 161

test.describe('API Security - Data Isolation', () => {
  let hr1Token: string;
  let hr2Token: string;

  test.beforeAll(async () => {
    // Get tokens for both HR managers
    const hr1Auth = await getAuthToken('1', 'hrManager');
    const hr2Auth = await getAuthToken('208278986', 'hrManager');
    
    hr1Token = hr1Auth?.token || '';
    hr2Token = hr2Auth?.token || '';
  });

  test('HR Manager 1 can access their own branch (129)', async () => {
    test.skip(!hr1Token, 'HR Manager 1 login failed');
    
    const result = await apiGet('/hr/branches/129/dashboard', hr1Token);
    expect(result.status).toBe(200);
  });

  test('HR Manager 1 CANNOT access HR Manager 2 branch (161)', async () => {
    test.skip(!hr1Token, 'HR Manager 1 login failed');
    
    const result = await apiGet('/hr/branches/161/dashboard', hr1Token);
    // Should get 403 Forbidden, not 200
    expect(result.status).toBe(403);
  });

  test('HR Manager 2 can access their own branch (161)', async () => {
    test.skip(!hr2Token, 'HR Manager 2 login failed');
    
    const result = await apiGet('/hr/branches/161/dashboard', hr2Token);
    expect(result.status).toBe(200);
  });

  test('HR Manager 2 CANNOT access HR Manager 1 branch (129)', async () => {
    test.skip(!hr2Token, 'HR Manager 2 login failed');
    
    const result = await apiGet('/hr/branches/129/dashboard', hr2Token);
    // Should get 403 Forbidden, not 200
    expect(result.status).toBe(403);
  });

  test('HR Manager 1 CANNOT access employees from another branch', async () => {
    test.skip(!hr1Token, 'HR Manager 1 login failed');
    
    const result = await apiGet('/hr/branches/161/employees', hr1Token);
    expect(result.status).toBe(403);
  });

  test('HR Manager 1 CANNOT view schedule from another branch', async () => {
    test.skip(!hr1Token, 'HR Manager 1 login failed');
    
    const result = await apiGet('/hr/branches/161/schedule', hr1Token);
    expect(result.status).toBe(403);
  });

  test('HR Manager 1 CANNOT view time-off requests from another branch', async () => {
    test.skip(!hr1Token, 'HR Manager 1 login failed');
    
    const result = await apiGet('/hr/branches/161/time-off-requests', hr1Token);
    expect(result.status).toBe(403);
  });
});

test.describe('API Security - Branch Settings', () => {
  let hr1Token: string;

  test.beforeAll(async () => {
    const hr1Auth = await getAuthToken('1', 'hrManager');
    hr1Token = hr1Auth?.token || '';
  });

  test('HR Manager 1 CANNOT update another branch settings', async () => {
    test.skip(!hr1Token, 'HR Manager 1 login failed');
    
    const response = await fetch(`${API_BASE}/branches/161`, {
      method: 'PUT',
      headers: { 
        'Authorization': `Bearer ${hr1Token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name: 'Hacked Branch Name' }),
    });
    
    expect(response.status).toBe(403);
  });

  test('HR Manager 1 CANNOT delete roles from another branch', async () => {
    test.skip(!hr1Token, 'HR Manager 1 login failed');
    
    const response = await fetch(`${API_BASE}/branches/161/roles/999`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${hr1Token}` },
    });
    
    // Should be 403 (or 404 if role doesn't exist, but NOT 200)
    expect([403, 404]).toContain(response.status);
  });

  test('HR Manager 1 CANNOT create roles in another branch', async () => {
    test.skip(!hr1Token, 'HR Manager 1 login failed');
    
    const response = await fetch(`${API_BASE}/branches/161/roles`, {
      method: 'POST',
      headers: { 
        'Authorization': `Bearer ${hr1Token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name: 'Hacked Role', hourlyWage: 100 }),
    });
    
    expect(response.status).toBe(403);
  });
});

test.describe('API Security - Active Branches Filter', () => {
  let hr1Token: string;

  test.beforeAll(async () => {
    const hr1Auth = await getAuthToken('1', 'hrManager');
    hr1Token = hr1Auth?.token || '';
  });

  test('HR Manager only sees branches from their restaurant', async () => {
    test.skip(!hr1Token, 'HR Manager 1 login failed');
    
    const result = await apiGet('/branches/active', hr1Token);
    expect(result.status).toBe(200);
    
    // All returned branches should belong to restaurant 129
    if (result.data && Array.isArray(result.data)) {
      const branchIds = result.data.map((b: any) => b.id);
      console.log('HR Manager 1 sees branches:', branchIds);
      
      // Branch 161 should NOT be in the list (it belongs to restaurant 97)
      expect(branchIds).not.toContain(161);
    }
  });
});

test.describe('API Security - Super Admin Access', () => {
  let adminToken: string;

  test.beforeAll(async () => {
    const adminAuth = await getAuthToken('999999999', 'admin123');
    adminToken = adminAuth?.token || '';
  });

  test('Super Admin CAN access any branch', async () => {
    test.skip(!adminToken, 'Super Admin login failed');
    
    // Can access branch 129
    const result1 = await apiGet('/hr/branches/129/dashboard', adminToken);
    expect(result1.status).toBe(200);
    
    // Can also access branch 161
    const result2 = await apiGet('/hr/branches/161/dashboard', adminToken);
    expect(result2.status).toBe(200);
  });
});

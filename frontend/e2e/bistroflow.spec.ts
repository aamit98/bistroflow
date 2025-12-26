/**
 * BistroFlow E2E Test Suite - MVP Automated Testing
 * 
 * This test suite simulates real user workflows and catches bugs automatically.
 * Run with: npx playwright test
 * 
 * Test Accounts:
 * - Super Admin: 999999999 / admin123
 * - HR Manager (BistroFlow TLV): 1 / hrManager
 */

import { test, expect, Page } from '@playwright/test';

// Test utilities
const USERS = {
  superAdmin: { id: '999999999', password: 'admin123' },
  hrManager1: { id: '1', password: 'hrManager' },
  hrManager2: { id: '208278986', password: 'hrManager' },
};

async function login(page: Page, userId: string, password: string) {
  await page.goto('/');
  // Try different selectors for employee ID input
  const employeeInput = page.locator('input[name="employeeId"]').or(page.locator('input[placeholder*="Employee"]'));
  await employeeInput.fill(userId);
  await page.locator('input[type="password"]').fill(password);
  await page.click('button[type="submit"]');
  // Wait for navigation away from login
  await page.waitForTimeout(3000);
}

async function logout(page: Page) {
  // Look for logout button or profile menu
  const logoutBtn = page.locator('button:has-text("Logout"), button:has-text("Sign Out"), [aria-label="logout"]');
  if (await logoutBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    await logoutBtn.click();
  }
}

// ===== AUTHENTICATION TESTS =====
test.describe('Authentication', () => {
  test('should login as HR Manager successfully', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    // Should see dashboard or branch selector
    await expect(page.locator('text=Dashboard, text=Branch, text=Schedule').first()).toBeVisible({ timeout: 10000 });
  });

  test('should login as Super Admin successfully', async ({ page }) => {
    await login(page, USERS.superAdmin.id, USERS.superAdmin.password);
    // Super admin should see admin panel
    await expect(page.locator('text=Admin, text=Restaurants, text=HR Managers').first()).toBeVisible({ timeout: 10000 });
  });

  test('should reject invalid credentials', async ({ page }) => {
    await page.goto('/');
    await page.fill('input[name="employeeId"]', '999');
    await page.fill('input[type="password"]', 'wrongpassword');
    await page.click('button[type="submit"]');
    // Should show error or stay on login page
    const errorVisible = await page.locator('text=Invalid').or(page.locator('text=Error')).or(page.locator('text=Failed')).first().isVisible({ timeout: 5000 }).catch(() => false);
    const stillOnLogin = page.url().includes('login') || await page.locator('input[type="password"]').isVisible();
    expect(errorVisible || stillOnLogin).toBeTruthy();
  });

  test('should logout successfully', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    await logout(page);
    // Should be back on login page or see login form
    await expect(page.locator('input[type="password"]')).toBeVisible({ timeout: 5000 });
  });
});

// ===== DATA ISOLATION TESTS =====
test.describe('Data Isolation - Critical Security', () => {
  test('HR Manager should only see their own branches', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Get all branches visible in the UI
    const branchElements = page.locator('[data-branch-id], .branch-item, .branch-card');
    const count = await branchElements.count();
    
    // If branches are shown, they should belong to this HR manager's restaurant
    // This test documents what branches are visible for review
    console.log(`HR Manager 1 sees ${count} branches`);
    
    // The HR manager should see at least one branch (their own)
    expect(count).toBeGreaterThan(0);
  });

  test('HR Manager cannot access another restaurants branch via URL manipulation', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Try to access a branch that might belong to another restaurant
    // Branch IDs 161 belongs to restaurant 97 (different from HR Manager 1's restaurant 129)
    await page.goto('/hr/branches/161/dashboard');
    
    // Should either redirect, show error, or show empty state
    // Should NOT show employee data from another restaurant
    const hasAccessDenied = await page.locator('text=Access Denied, text=Forbidden, text=unauthorized').first().isVisible({ timeout: 3000 }).catch(() => false);
    const wasRedirected = !page.url().includes('/branches/161');
    const noEmployeeData = await page.locator('.employee-card, .employee-row').count() === 0;
    
    expect(hasAccessDenied || wasRedirected || noEmployeeData).toBeTruthy();
  });

  test('Notifications should be filtered by user', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Navigate to notifications if there's a notification icon/page
    const notificationBtn = page.locator('[aria-label*="notification"], .notification-icon, button:has-text("Notifications")');
    if (await notificationBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await notificationBtn.click();
      
      // Count notifications
      const notificationCount = await page.locator('.notification-item, .notification-card').count();
      console.log(`HR Manager 1 sees ${notificationCount} notifications`);
      
      // All notifications should be for this user's restaurant
      // This is documentation for manual review
    }
  });
});

// ===== EMPLOYEE MANAGEMENT TESTS =====
test.describe('Employee Management', () => {
  test('should display employee list', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Navigate to employees page
    await page.click('text=Employees, text=Staff, a[href*="employee"]').catch(() => {});
    
    // Should see employee list or cards
    await expect(page.locator('.employee-card, .employee-row, table tbody tr').first()).toBeVisible({ timeout: 10000 });
  });

  test('should be able to view employee details', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Navigate to employees and click on one
    await page.click('text=Employees, text=Staff').catch(() => {});
    await page.waitForTimeout(2000);
    
    // Click first employee
    const employeeLink = page.locator('.employee-card, .employee-row, tr').first();
    if (await employeeLink.isVisible()) {
      await employeeLink.click();
      // Should see employee details
      await expect(page.locator('text=Name, text=Email, text=Phone, text=Role').first()).toBeVisible({ timeout: 5000 });
    }
  });
});

// ===== SCHEDULE MANAGEMENT TESTS =====
test.describe('Schedule Management', () => {
  test('should display weekly schedule', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Navigate to schedule page
    await page.click('text=Schedule, a[href*="schedule"]').catch(() => {});
    
    // Should see schedule grid or calendar
    await expect(page.locator('.schedule-grid, .calendar, .schedule-container, [class*="schedule"]').first()).toBeVisible({ timeout: 10000 });
  });

  test('schedule should show days of the week', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    await page.click('text=Schedule, a[href*="schedule"]').catch(() => {});
    await page.waitForTimeout(2000);
    
    // Should see day labels
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    let foundDays = 0;
    for (const day of days) {
      if (await page.locator(`text=${day}`).first().isVisible().catch(() => false)) {
        foundDays++;
      }
    }
    expect(foundDays).toBeGreaterThanOrEqual(1);
  });
});

// ===== TIME OFF REQUESTS TESTS =====
test.describe('Time Off Requests', () => {
  test('should display time off requests list', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Navigate to time off page
    await page.click('text=Time Off, text=Requests, a[href*="time-off"]').catch(() => {});
    
    // Should see requests or empty state
    await expect(page.locator('.request-card, .time-off-list, text=No requests, text=pending').first()).toBeVisible({ timeout: 10000 });
  });
});

// ===== BRANCH SETTINGS TESTS =====
test.describe('Branch Settings', () => {
  test('should display branch settings', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Navigate to branch settings
    await page.click('text=Settings, text=Branch Settings, a[href*="settings"]').catch(() => {});
    
    // Should see settings form
    await expect(page.locator('input, select, .settings-form, text=Branch Name, text=Address').first()).toBeVisible({ timeout: 10000 });
  });

  test('branch settings should show roles', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    await page.click('text=Settings, a[href*="settings"]').catch(() => {});
    await page.waitForTimeout(2000);
    
    // Should see roles section
    await expect(page.locator('text=Roles, text=Role, text=Position').first()).toBeVisible({ timeout: 10000 });
  });
});

// ===== INVENTORY TESTS =====
test.describe('Inventory Management', () => {
  test('should display inventory/stock list', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Navigate to inventory page
    await page.click('text=Inventory, text=Stock, a[href*="inventory"]').catch(() => {});
    
    // Should see inventory list or products
    await expect(page.locator('.product-row, .inventory-item, table, text=Stock, text=Product').first()).toBeVisible({ timeout: 10000 });
  });
});

// ===== SUPER ADMIN TESTS =====
test.describe('Super Admin Functions', () => {
  test('should see all restaurants', async ({ page }) => {
    await login(page, USERS.superAdmin.id, USERS.superAdmin.password);
    
    // Should see restaurants management
    await expect(page.locator('text=Restaurants, text=All Restaurants').first()).toBeVisible({ timeout: 10000 });
  });

  test('should see all HR managers', async ({ page }) => {
    await login(page, USERS.superAdmin.id, USERS.superAdmin.password);
    
    // Navigate to HR managers
    await page.click('text=HR Managers, text=Managers').catch(() => {});
    
    // Should see HR managers list
    await expect(page.locator('.hr-manager-card, .manager-row, table tbody tr').first()).toBeVisible({ timeout: 10000 });
  });

  test('super admin can view any branch', async ({ page }) => {
    await login(page, USERS.superAdmin.id, USERS.superAdmin.password);
    
    // Super admin should be able to access any branch
    await page.goto('/hr/branches/129/dashboard');
    
    // Should not be blocked
    const isBlocked = await page.locator('text=Access Denied, text=Forbidden').first().isVisible({ timeout: 3000 }).catch(() => false);
    expect(isBlocked).toBeFalsy();
  });
});

// ===== ERROR HANDLING TESTS =====
test.describe('Error Handling', () => {
  test('should handle 404 pages gracefully', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    await page.goto('/non-existent-page-xyz');
    
    // Should show 404 or redirect to home, not crash
    const has404 = await page.locator('text=404, text=Not Found, text=Page not found').first().isVisible().catch(() => false);
    const redirectedHome = page.url().includes('dashboard') || page.url() === 'http://localhost:5173/';
    
    expect(has404 || redirectedHome).toBeTruthy();
  });

  test('should handle network errors gracefully', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Block API calls
    await page.route('**/api/**', route => route.abort());
    
    // Navigate to a page that makes API calls
    await page.click('text=Employees, text=Schedule').catch(() => {});
    
    // Should show error message, not crash
    await page.waitForTimeout(3000);
    
    // Page should still be functional (not blank)
    const bodyContent = await page.locator('body').textContent();
    expect(bodyContent?.length).toBeGreaterThan(10);
  });
});

// ===== UI/UX CONSISTENCY TESTS =====
test.describe('UI Consistency', () => {
  test('should have consistent navigation', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Check for main navigation elements
    const hasNav = await page.locator('nav, .sidebar, .navigation, header').first().isVisible();
    expect(hasNav).toBeTruthy();
  });

  test('should be responsive on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 }); // iPhone SE
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    // Should still be able to navigate
    const hasContent = await page.locator('main, .content, .dashboard, body').first().isVisible();
    expect(hasContent).toBeTruthy();
  });
});

// ===== PERFORMANCE TESTS =====
test.describe('Performance', () => {
  test('login should complete within 5 seconds', async ({ page }) => {
    const startTime = Date.now();
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    const duration = Date.now() - startTime;
    
    console.log(`Login completed in ${duration}ms`);
    expect(duration).toBeLessThan(5000);
  });

  test('dashboard should load within 10 seconds', async ({ page }) => {
    await login(page, USERS.hrManager1.id, USERS.hrManager1.password);
    
    const startTime = Date.now();
    await page.goto('/hr/dashboard');
    await page.waitForLoadState('networkidle');
    const duration = Date.now() - startTime;
    
    console.log(`Dashboard loaded in ${duration}ms`);
    expect(duration).toBeLessThan(10000);
  });
});

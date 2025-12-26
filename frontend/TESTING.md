# BistroFlow Automated Testing Guide

## Quick Start

### 1. Install Playwright (one-time setup)
```bash
cd frontend
npm install -D @playwright/test
npx playwright install chromium
```

### 2. Run Tests

**Run all tests:**
```bash
npm test
```

**Run tests with visual browser (headed mode):**
```bash
npm run test:headed
```

**Run only security tests:**
```bash
npm run test:security
```

**Run tests with Playwright UI (interactive):**
```bash
npm run test:ui
```

**View test report:**
```bash
npm run test:report
```

## Test Suites

### 1. `e2e/bistroflow.spec.ts` - Full MVP Testing
Tests the complete user workflow:
- ✅ Authentication (login, logout, invalid credentials)
- ✅ Data isolation between restaurants
- ✅ Employee management
- ✅ Schedule management
- ✅ Time-off requests
- ✅ Branch settings
- ✅ Inventory management
- ✅ Super admin functions
- ✅ Error handling (404, network errors)
- ✅ UI consistency
- ✅ Performance benchmarks

### 2. `e2e/security.spec.ts` - Security-Focused Tests
API-level tests for data isolation:
- ✅ HR Manager can only access their own branches
- ✅ HR Manager cannot access other restaurants' data via URL manipulation
- ✅ Branch settings are protected
- ✅ Active branches are filtered correctly
- ✅ Super Admin has full access

## Test Accounts

| Role | Employee ID | Password |
|------|-------------|----------|
| Super Admin | 999999999 | admin123 |
| HR Manager (BistroFlow TLV) | 1 | hrManager |
| HR Manager (Other Restaurant) | 208278986 | hrManager |

## Requirements

Before running tests, ensure:
1. **Backend is running** on port 8080
2. **Frontend is running** on port 5173 (or let Playwright start it)

## Test Results

After running tests, you'll see:
- Pass/fail status in terminal
- Screenshots of failures in `test-results/` folder
- HTML report (run `npm run test:report` to view)

## Recommended Extensions

For AI-powered testing enhancements, install these VS Code extensions:

1. **Playwright Test for VS Code** (`ms-playwright.playwright`) - Run tests from VS Code
2. **TestDriver** (`testdriver.testdriver`) - AI computer-vision testing
3. **Keploy** (`keploy.keployio`) - Auto-generate API tests from traffic

## CI/CD Integration

Add to your GitHub Actions workflow:
```yaml
- name: Run E2E Tests
  run: |
    cd frontend
    npm ci
    npx playwright install --with-deps
    npm test
```

## Troubleshooting

**Tests fail to start:** Make sure backend is running on port 8080

**Login tests fail:** Verify test account credentials are correct

**Timeout errors:** Increase timeout in `playwright.config.ts`

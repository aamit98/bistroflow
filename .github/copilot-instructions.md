<!-- Copilot instructions for AI coding agents working in this repo -->
# Quick onboard: updated-project

This file contains concise, actionable guidance for AI coding agents working in this repository. Focus on reproducible, repository-specific details rather than generic advice.

- **Project layout:**
  - **Backend:** `backend/adss-backend` — a Spring Boot (Maven) Java 17 service. Key files: `pom.xml`, `mvnw` / `mvnw.cmd`, `src/main/java/...` (domain, DataAccessLayer, ServiceLayer). Database file `InventoryHR.db` is present in the backend folder.
  - **Frontend:** `frontend` — Vite + React + TypeScript. Key files: `package.json`, `vite.config.ts`, `src/` (notably `src/api` and `src/security/AuthContext.tsx`).

- **High-level architecture & data flow:**
  - Frontend talks to backend over REST under `/api`. The front-end `ApiClient` sets `baseURL: "/api"` (see `frontend/src/api/ApiClient.ts`).
  - Vite dev server proxies `/api` to `http://localhost:8080` (see `frontend/vite.config.ts`), so in development the backend should run on port `8080`.
  - Backend uses Spring Boot with Spring MVC, Spring Security and JWT (`io.jsonwebtoken`) for authentication. Data access is a mix of in-repo SQLite and H2 runtime dependencies.

- **Important conventions & gotchas (do not change lightly):**
  - The HELP file notes the package name uses `com.gitProjects.adss_backend` (underscore) rather than the invalid `com.gitProjects.adss-backend`. Avoid renaming packages unless you update all references.
  - Auth is persisted client-side under localStorage key `bistroflow-auth`; `AuthProvider` rehydrates it and calls `setAuthToken` (see `frontend/src/security/AuthContext.tsx`). Keep this shape when changing auth flows.
  - `ApiClient.setAuthToken(token)` updates `axios` default `Authorization` header. Use this when adding secured API calls.

- **Build / run / test workflows (explicit commands):**
  - Backend (Windows PowerShell):
    - Install/build: `cd backend\adss-backend; .\mvnw.cmd package`
    - Run for development: `cd backend\adss-backend; .\mvnw.cmd spring-boot:run`
    - Run tests: `cd backend\adss-backend; .\mvnw.cmd test`
  - Frontend (PowerShell / CMD):
    - Install deps: `cd frontend; npm install`
    - Start dev server: `cd frontend; npm run dev` (uses Vite HMR; proxies `/api` to backend)
    - Build for production: `cd frontend; npm run build`

- **Where to look when adding features or endpoints:**
  - Backend controllers and DTOs: `backend/adss-backend/src/main/java/DataAccessLayer` and `.../DomainLayer` — follow existing controller/DTO naming and package layout.
  - Frontend API clients: `frontend/src/api/*` — add new endpoint methods there (follow patterns in `HrApiService.ts` and `InventoryApi.ts`), and ensure `setAuthToken` is used for secured calls.
  - Auth integration: If changing login/logout, update both backend JWT handling (Spring Security config) and `AuthProvider` + persisted shape in the frontend.

- **Testing & debugging hints:**
  - Backend logs: standard Spring Boot logging; `spring-boot:run` exposes logs in console.
  - If frontend cannot reach backend in dev, confirm `vite.config.ts` proxy target (`localhost:8080`) and that backend is running on that port.

- **Safe change guidelines for AI agents:**
  - Prefer additive changes (new endpoints, new DTOs) over renaming or moving packages/files.
  - If updating a public API or DTO shape, change both backend DTO and all frontend callers in `frontend/src/api` in the same PR.
  - When updating auth/JWT code, include a short integration test or manual verification steps (login -> call protected endpoint -> logout) in the PR description.

If anything in these instructions is unclear or you want more detail on a specific area (examples: database schema, controller patterns, or build CI), tell me which part and I will expand or refine these instructions.

# Smart Society Connect Frontend

React + Vite frontend for the Smart Society Connect Spring Boot API.

For complete installation on another Windows computer, including MySQL,
Eclipse environment variables, Lombok, Gmail OTP, and Razorpay, see
[`SETUP_ON_NEW_SYSTEM.md`](SETUP_ON_NEW_SYSTEM.md).

## Local development

1. Configure and start the backend on `http://localhost:8080`.

   Required backend environment variables include `DB_URL`, `DB_USERNAME`,
   `DB_PASSWORD`, `APP_JWT_SECRET`, `MAIL_USERNAME`, and `MAIL_PASSWORD`.

2. Confirm the backend is ready:

   ```text
   http://localhost:8080/api/health
   ```

   It should return:

   ```json
   { "status": "UP" }
   ```

3. Install and run the frontend:

   ```bat
   npm ci
   npm run dev
   ```

4. Open `http://localhost:5173`.

The frontend sends requests to `/api`; Vite proxies them to the backend target
defined by `VITE_BACKEND_TARGET` (default `http://localhost:8080`).

## Roles

The backend JWT roles are:

```text
ADMIN
RESIDENT
SECURITY
SECRETARY
ACCOUNTANT
```

## Docker deployment

The production image serves the frontend via Nginx and proxies `/api/*` to
`BACKEND_URL`. It defaults to `http://host.docker.internal:8080` for local
Docker Desktop use. Override it in production, for example:

```text
BACKEND_URL=https://api.example.com
```

## Current API integration

The frontend uses the backend's role-specific dashboard endpoints, resident,
visitor, complaint, billing, payment, meeting, OTP, profile, and health APIs.
See `API_CONTRACT.md` for the detailed route and payload reference.

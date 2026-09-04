# Smart Society Connect API Contract

Backend base URL: `http://localhost:8080/api`.

All protected routes require:

```http
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

Successful resource responses may be wrapped as:

```json
{ "success": true, "message": "...", "data": {} }
```

## Public routes

| Method | Route | Request / response |
| --- | --- | --- |
| GET | `/health` | Returns `{ "status": "UP" }`. |
| POST | `/auth/register` | `{ firstName, lastName, email, mobile, password }`. This creates a user account only; it does not create a resident or a flat. |
| POST | `/auth/login` | `{ username, password }`; returns `{ token, userId, fullName, email, roleName }`. |
| POST | `/auth/verify-otp` | `{ email, otpCode }` |
| POST | `/auth/resend-otp` | Plain-text registered email in request body. |

Roles emitted by the backend are `ADMIN`, `RESIDENT`, `SECURITY`, `SECRETARY`, and `ACCOUNTANT`.

## Authenticated profile

| Method | Route | Request / response |
| --- | --- | --- |
| GET | `/auth/profile` | Returns `{ id, name, email, mobile }`. |
| PUT | `/auth/profile` | `{ name, mobile }`; email changes require a separate verified-email workflow. |
| POST | `/auth/logout` | Raw token string in request body. |

## Dashboard

The backend uses a separate response type per role:

| Role | Route |
| --- | --- |
| ADMIN | `GET /dashboard/admin` |
| RESIDENT | `GET /dashboard/resident` |
| SECURITY | `GET /dashboard/security` |
| ACCOUNTANT | `GET /dashboard/accountant` |

There is no `/dashboard/summary` endpoint. The frontend maps the role-specific statistics into its common dashboard display.

## Residents

Resident onboarding is a two-stage process: the user registers and verifies an
account, an administrator approves it and selects its role, then an
administrator creates the resident record by linking that approved `userId` to
an existing `flatId`. Create the flat first if it does not exist.

| Method | Route |
| --- | --- |
| GET / POST | `/residents` |
| GET / PUT / DELETE | `/residents/{residentId}` |
| GET | `/residents/user/{userId}` |

Create request:

```json
{
  "userId": 12,
  "flatId": 5,
  "residentType": "OWNER",
  "occupation": "Engineer",
  "emergencyContact": "9876543210",
  "moveInDate": "2026-07-22",
  "isPrimaryMember": true
}
```

`residentType` is `OWNER` or `TENANT`.

## Flats

| Method | Route |
| --- | --- |
| GET / POST | `/flats` |
| GET / PUT / DELETE | `/flats/{flatId}` |

Create request requires `flatNumber`, `wing`, `floorNumber`, and `flatType`.

## Visitors

| Method | Route |
| --- | --- |
| GET / POST | `/visitors` |
| PUT | `/visitors/{visitorId}/approve` |
| PUT | `/visitors/{visitorId}/check-in` |
| PUT | `/visitors/{visitorId}/check-out` |
| DELETE | `/visitors/{visitorId}` |

Create requires `residentId`, `visitorName`, `mobile`, `gender`, `age`, `address`, `idProofType`, `idProofNumber`, `purpose`, and `visitDate`. Status flow: `REQUESTED → APPROVED/REJECTED → CHECKED_IN → CHECKED_OUT`.

## Complaints

| Method | Route |
| --- | --- |
| GET / POST | `/complaints` |
| PUT / DELETE | `/complaints/{complaintId}` |
| PATCH | `/complaints/{complaintId}/status` |

Create request requires `residentId`, `title`, `description`, `category`, and `priority`. Categories use uppercase enum values such as `ELECTRICAL`, `PLUMBING`, `WATER`, and `OTHER`. Status updates accept `status`, optional `assignedTo`, and optional `resolutionRemarks`.

## Billing and payments

| Method | Route |
| --- | --- |
| GET / POST | `/billing` |
| GET / PUT / DELETE | `/billing/{billingId}` |
| POST | `/payments` |

Bill creation uses `residentId`, `billingMonth`, `billingYear`, charge amounts, `dueDate`, and optional `remarks`. Payment request:

```json
{
  "billingId": "uuid",
  "amountPaid": 3650,
  "paymentMode": "UPI",
  "transactionReference": "REFERENCE-123",
  "remarks": null
}
```

Payment modes: `UPI`, `CARD`, `BANK_TRANSFER`, `CHEQUE`, `CASH`.

## Meetings

| Method | Route |
| --- | --- |
| GET / POST | `/meetings` |
| PUT / DELETE | `/meetings/{meetingUuid}` |
| PATCH | `/meetings/{meetingUuid}/status?status=COMPLETED` |

Create/update uses `title`, `description`, `agenda`, `meetingDate`, `startTime`, `endTime`, and `venue`.

## Deployment

Development uses Vite’s `/api` proxy. Docker/Nginx uses `BACKEND_URL` to proxy `/api/*` to Spring Boot. External hosting must allow the frontend origin and the `Authorization` header if requests bypass Nginx.

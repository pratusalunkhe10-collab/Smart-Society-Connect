# 🏢 Smart Society Connect

### Integrated Resident Engagement Platform

**Smart Society Connect** is a full-stack residential society management platform that centralizes day-to-day society operations through secure, role-based workflows for administrators, secretaries, residents, security personnel, and accountants.

> **Status:** Completed Full-Stack Academic Project — Cloud Deployed  
> **Program:** PGCP-AC — Post Graduate Certificate Programme in Advanced Computing  
> **Team Size:** 5

## 🌐 Live Demo

- **Live Application:** https://smart-society-connect-frontend.onrender.com
- **Backend API:** https://smart-society-connect-backend.onrender.com
- **Swagger / OpenAPI:** https://smart-society-connect-backend.onrender.com/swagger-ui/index.html

> **Demo note:** The application is hosted for project demonstration. Registration/OTP delivery depends on the configured external email provider. Free hosting instances may also require a short warm-up on the first request after inactivity.

---

## ✨ Key Features

- 🔐 JWT authentication and role-based authorization
- ✉️ Email OTP verification and administrator user approval
- 🏠 Resident, flat, family-member and document management
- 🚪 Visitor pre-approval, check-in and check-out workflows
- 📝 Complaint creation, tracking and resolution
- 💳 Billing, payment history, dues and Razorpay integration
- 📊 Role-specific dashboards
- 📅 Meeting scheduling, attendance and minutes
- 📢 Announcements and notifications
- 📂 Society and staff document management
- 📚 Swagger / OpenAPI documentation
- 🌗 Light and dark UI themes

---

## 👥 User Roles

| Role | Main Responsibilities |
|---|---|
| **ADMIN** | User approvals, society administration, residents, complaints, meetings and announcements |
| **SECRETARY** | Society operations, meetings, announcements and resident coordination |
| **RESIDENT** | Profile, visitors, complaints, bills, meetings and documents |
| **SECURITY** | Visitor approval workflow, check-in and check-out |
| **ACCOUNTANT** | Billing, payments, dues and financial dashboard |

---

## 🛠️ Technology Stack

**Frontend:** React.js, Vite, JavaScript, Tailwind CSS, Axios, React Router  
**Backend:** Java 17+, Spring Boot 3, Spring Security, Spring Data JPA, Hibernate, JWT, Maven  
**Database:** MySQL 8  
**Integrations:** Resend HTTP API for OTP email, Razorpay  
**DevOps / Deployment:** Git, GitHub, Docker, Render, Aiven MySQL

---

## 🏗️ System Architecture

```text
React + Vite Frontend
        │
        │ REST API / JSON
        ▼
Spring Security + JWT + OTP + CORS
        │
        ▼
Spring Boot Controllers
        │
        ▼
Business / Service Layer
        │
        ▼
Spring Data JPA / Repository Layer
        │
        ▼
MySQL 8
```

---

## 📦 Main Modules

1. **Authentication & User Management** — registration, OTP, login, JWT, profiles, roles and approvals.
2. **Resident & Flat Management** — residents, flats, ownership/tenant data, family members and documents.
3. **Visitor Management** — requests, approvals, check-in/check-out and security workflows.
4. **Complaint Management** — complaint creation, priority/category tracking, status updates and resolution.
5. **Billing & Payments** — bill generation, payment history, dues, payment status and Razorpay.
6. **Dashboard** — role-specific KPIs and operational summaries.
7. **Meeting Management** — meetings, scheduling, attendance and minutes.

Additional functionality includes announcements, notifications, society documents and staff documents.

---

## 📁 Repository Structure

```text
Smart-Society-Connect/
├── backend/
│   ├── src/
│   ├── database/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── mvnw
│   └── mvnw.cmd
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── vite.config.js
│   ├── Dockerfile
│   └── nginx.conf
├── docs/
│   └── screenshots/
│       ├── White/
│       └── Black/
├── .env.example
├── docker-compose.yml
├── .gitignore
└── README.md
```

---

# 📸 Project Screenshots

The application supports both **Light (White)** and **Dark (Black)** themes.

## ☀️ Light / White Theme

<table>
<tr><td width="50%"><b>Accountant</b><br><img src="docs/screenshots/White/Accountant.png" width="100%"></td><td width="50%"><b>Admin</b><br><img src="docs/screenshots/White/Admin.png" width="100%"></td></tr>
<tr><td width="50%"><b>Announcements</b><br><img src="docs/screenshots/White/Announcements.png" width="100%"></td><td width="50%"><b>Billing</b><br><img src="docs/screenshots/White/Billing.png" width="100%"></td></tr>
<tr><td width="50%"><b>Complaint</b><br><img src="docs/screenshots/White/Complaint.png" width="100%"></td><td width="50%"><b>Documents</b><br><img src="docs/screenshots/White/Documents.png" width="100%"></td></tr>
<tr><td width="50%"><b>Login</b><br><img src="docs/screenshots/White/Login.png" width="100%"></td><td width="50%"><b>Meetings</b><br><img src="docs/screenshots/White/Meetings.png" width="100%"></td></tr>
<tr><td width="50%"><b>My Profile</b><br><img src="docs/screenshots/White/My%20Profile.png" width="100%"></td><td width="50%"><b>Registration</b><br><img src="docs/screenshots/White/Registration.png" width="100%"></td></tr>
<tr><td width="50%"><b>Resident & Flats</b><br><img src="docs/screenshots/White/Resident%20&%20Flats.png" width="100%"></td><td width="50%"><b>Residents</b><br><img src="docs/screenshots/White/Residents.png" width="100%"></td></tr>
<tr><td width="50%"><b>Secretary</b><br><img src="docs/screenshots/White/Secretary.png" width="100%"></td><td width="50%"><b>Security Guard</b><br><img src="docs/screenshots/White/Security%20Guard.png" width="100%"></td></tr>
<tr><td width="50%"><b>User Approvals</b><br><img src="docs/screenshots/White/User%20Approvals.png" width="100%"></td><td width="50%"><b>Visitors</b><br><img src="docs/screenshots/White/Visitors.png" width="100%"></td></tr>
</table>

## 🌙 Dark / Black Theme

<table>
<tr><td width="50%"><b>Accountant</b><br><img src="docs/screenshots/Black/Accountant.png" width="100%"></td><td width="50%"><b>Admin</b><br><img src="docs/screenshots/Black/Admin.png" width="100%"></td></tr>
<tr><td width="50%"><b>Announcement</b><br><img src="docs/screenshots/Black/Announcement.png" width="100%"></td><td width="50%"><b>Billing</b><br><img src="docs/screenshots/Black/Billing.png" width="100%"></td></tr>
<tr><td width="50%"><b>Complaints</b><br><img src="docs/screenshots/Black/Complaints.png" width="100%"></td><td width="50%"><b>Documents</b><br><img src="docs/screenshots/Black/Documents.png" width="100%"></td></tr>
<tr><td width="50%"><b>Login</b><br><img src="docs/screenshots/Black/Login.png" width="100%"></td><td width="50%"><b>Meetings</b><br><img src="docs/screenshots/Black/Meetings.png" width="100%"></td></tr>
<tr><td width="50%"><b>My Profile</b><br><img src="docs/screenshots/Black/My%20Profile.png" width="100%"></td><td width="50%"><b>Registration</b><br><img src="docs/screenshots/Black/Registration.png" width="100%"></td></tr>
<tr><td width="50%"><b>Resident & Flats</b><br><img src="docs/screenshots/Black/Resident%20&%20Flats.png" width="100%"></td><td width="50%"><b>Resident</b><br><img src="docs/screenshots/Black/Resident.png" width="100%"></td></tr>
<tr><td width="50%"><b>Secretary</b><br><img src="docs/screenshots/Black/Secretary.png" width="100%"></td><td width="50%"><b>Security Guard</b><br><img src="docs/screenshots/Black/Security%20Guard.png" width="100%"></td></tr>
<tr><td width="50%"><b>User Approvals</b><br><img src="docs/screenshots/Black/User%20Approvals.png" width="100%"></td><td width="50%"><b>Visitors</b><br><img src="docs/screenshots/Black/Visitors.png" width="100%"></td></tr>
</table>

---

## ⚙️ Environment Configuration

This repository does **not** contain production credentials. After cloning, create your own environment configuration from `.env.example` and provide your own database, JWT, email and payment credentials.

Important variables include:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
APP_JWT_SECRET
APP_CORS_ALLOWED_ORIGINS
RESEND_API_KEY
RESEND_FROM_EMAIL
RAZORPAY_KEY_ID
RAZORPAY_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET
```

Frontend configuration is documented in `frontend/.env.example`.

> **Security:** Never commit real `.env` files, database passwords, Resend API keys, JWT secrets, Razorpay secrets or private keys.

---

## ▶️ Run Locally After Cloning

### Prerequisites

Java 17+, Node.js/npm, MySQL 8 and Git.

### 1. Clone

```bash
git clone https://github.com/Nadeer-Ansari/Smart-Society-Connect.git
cd Smart-Society-Connect
```

### 2. Database

Create the database and import the supplied fresh-install schema:

```sql
CREATE DATABASE IF NOT EXISTS smart_society_connect_db;
```

Then import:

```text
backend/database/fresh_install_schema.sql
```

### 3. Configure environment

Copy `.env.example` to `.env` when using Docker, or set the equivalent environment variables in your IDE/terminal. Replace every placeholder with your own values.

For OTP email, create your own Resend API key and configure `RESEND_API_KEY`. For unrestricted recipients, configure `RESEND_FROM_EMAIL` with a sender/domain verified in your own Resend account.

### 4. Backend

```bash
cd backend
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Backend: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 5. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

The frontend template defaults to local development. For another deployed backend, set `VITE_API_BASE_URL` according to `frontend/.env.example`.

---

## 🗄️ Database

Schema and migrations are stored under `backend/database/`. The default local database is `smart_society_connect_db`.

Cloud deployment does not lock the repository to Aiven: another developer can provide their own MySQL connection through `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

---

## 🔒 Security

- BCrypt password hashing
- JWT authentication
- OTP email verification
- Role-based authorization
- Administrator user approval
- CORS configuration
- Protected REST endpoints
- Environment-based secret management

---

## 🐳 Deployment Architecture

```text
GitHub
   ↓
Docker / Containerization
   ↓
Aiven MySQL
   ↓
Render Spring Boot Backend
   ↓
Render React Frontend
   ↓
Production CORS + API Configuration
   ↓
Live Demo
```

The repository remains portable: Render/Aiven-specific credentials are environment variables rather than hard-coded project values.

---

## 👨‍💻 Project Team

**Team Leader:** Nadeer Ansari

**Team Members**
- Nadeer Ansari
- Riddhi Shinde
- Pratiksha
- Nikhil
- *Add the fifth team member's exact name*

---

## 🎓 Academic Project

Developed as part of the **PGCP-AC (Post Graduate Certificate Programme in Advanced Computing)** academic project by a **5-member team**.

Smart Society Connect aims to provide a centralized, secure and user-friendly digital platform for residential society operations and communication.

---

## 📌 Project Status

✅ Backend implemented  
✅ Frontend implemented  
✅ Role-based workflows integrated  
✅ Database schema available  
✅ Payment integration implemented  
✅ Dockerized backend  
✅ Cloud MySQL deployed  
✅ Backend deployed on Render  
✅ Frontend deployed on Render  
✅ Swagger / API documentation deployed  
⚠️ OTP email delivery depends on external Resend configuration/sender verification

---

## 📄 License

This repository is intended primarily for educational, academic and demonstration purposes.

---

## ⭐ Smart Society Connect

If you find the project useful, consider starring the repository.

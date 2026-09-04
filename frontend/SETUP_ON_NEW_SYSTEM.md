# Smart Society Connect — Setup on a New System

This guide sets up both applications on another Windows computer:

- Backend: `Smart-Society-Connect/smart-society-connect`
- Frontend: `Smart-Society-Connect-Frontend`

Do not copy private passwords, API secrets, `target`, `node_modules`, or `dist`
from the original computer. Each computer should use its own local configuration.

## 1. Prerequisites

Install these before importing the projects:

| Software | Required version | Check command |
| --- | --- | --- |
| JDK | Java 17 | `java -version` |
| Eclipse IDE or Spring Tool Suite | Current version with Maven support | Open Eclipse |
| MySQL Server and MySQL Workbench/Shell | MySQL 8 or newer | `mysql --version` |
| Node.js | 22.12 or newer, below 27 | `node --version` |
| npm | 10 or newer | `npm --version` |
| Git | Optional, recommended | `git --version` |

The backend contains `mvnw.cmd`, so a separate Maven installation is not
required. Internet access is required during the first Maven and npm build.

## 2. Copy the Projects

Copy these two source folders to the new computer:

```text
Smart-Society-Connect/smart-society-connect
Smart-Society-Connect-Frontend
```

The following generated/private folders and files should not be transferred:

```text
backend/target
frontend/node_modules
frontend/dist
frontend/.env
backend/run-local.ps1
```

The `uploads` folder is application data. Copy it only when existing uploaded
documents must also be migrated.

## 3. Create the MySQL Database

Start MySQL, then open MySQL Workbench or the MySQL command-line client.

For a completely new system, run the complete schema file once:

```sql
SOURCE C:/path/to/Smart-Society-Connect/smart-society-connect/database/fresh_install_schema.sql;
```

Use forward slashes in the `SOURCE` path. The file creates:

```text
smart_society_connect_db
```

It also creates every current table, foreign key, required role, announcement
table, document tables, Razorpay order table, and Cash/Cheque audit fields. Do
**not** run V2, V3, or V4 after `fresh_install_schema.sql`; those migrations are
only for upgrading an older database.

Confirm the installation:

```sql
USE smart_society_connect_db;
SHOW TABLES;
SELECT role_id, role_name FROM roles;
```

## 4. Prepare Gmail OTP

The sender Gmail account must have two-step verification enabled.

1. Open the Google Account security settings.
2. Enable two-step verification.
3. Create a Gmail App Password.
4. Save the generated 16-character App Password.
5. Use the App Password as `MAIL_PASSWORD`; do not use the normal Gmail password.

The registered user can use any email address. OTP messages are sent from
`MAIL_USERNAME` to the email entered during registration.

## 5. Backend Environment Variables in Eclipse

### Import the backend

1. Open Eclipse.
2. Select **File → Import → Maven → Existing Maven Projects**.
3. Select the `smart-society-connect` backend directory.
4. Make sure `pom.xml` is selected and finish the import.
5. Right-click the project → **Maven → Update Project**.
6. Enable **Force Update of Snapshots/Releases** and select **OK**.

### Configure Java 17

1. Open **Window → Preferences → Java → Installed JREs**.
2. Add the installed JDK 17 directory if it is missing.
3. Select JDK 17 as the default.
4. Open **Project → Properties → Java Compiler** and use Java 17.

### Configure Lombok in Eclipse

Maven already contains Lombok and its annotation processor. Eclipse must also
understand Lombok-generated constructors, getters, builders, and log fields.

1. Download `lombok.jar` from the official Lombok website.
2. Close Eclipse.
3. Run:

   ```bat
   java -jar lombok.jar
   ```

4. Select the installed `eclipse.exe` or STS executable.
5. Choose **Install/Update**.
6. Restart Eclipse.
7. Run **Maven → Update Project**, then **Project → Clean**.

If Eclipse reports errors such as `log cannot be resolved` or “blank final
field may not have been initialized” while Maven compiles successfully, Lombok
is not installed or active in Eclipse. Do not rewrite the working source code
to solve an IDE-only Lombok configuration problem.

### Add environment variables

1. Open **Run → Run Configurations**.
2. Select the Spring Boot application configuration. If it does not exist,
   first run the main Spring Boot class once using **Run As → Spring Boot App**.
3. Open the **Environment** tab.
4. Select **New** and enter every variable below.
5. Apply the configuration.

Required variables:

```text
DB_URL=jdbc:mysql://localhost:3306/smart_society_connect_db
DB_USERNAME=root
DB_PASSWORD=YOUR_MYSQL_PASSWORD
MAIL_USERNAME=YOUR_GMAIL_ADDRESS
MAIL_PASSWORD=YOUR_16_CHARACTER_GMAIL_APP_PASSWORD
APP_JWT_SECRET=YOUR_UNIQUE_RANDOM_SECRET_OF_AT_LEAST_32_CHARACTERS
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
JPA_DDL_AUTO=validate
```

Recommended upload setting:

```text
APP_UPLOAD_DIR=C:/path/to/smart-society-connect/uploads/documents
```

Optional server settings:

```text
SERVER_PORT=8080
JWT_EXPIRATION_MS=86400000
JPA_SHOW_SQL=false
```

### Razorpay Test Mode

Add these variables only when online bill payment is required:

```text
RAZORPAY_KEY_ID=rzp_test_REPLACE_WITH_YOUR_KEY
RAZORPAY_KEY_SECRET=REPLACE_WITH_YOUR_TEST_SECRET
RAZORPAY_WEBHOOK_SECRET=REPLACE_WITH_YOUR_PRIVATE_WEBHOOK_SECRET
```

Keep the Key Secret and Webhook Secret only in the backend. Never put them in
the React `.env` file or source control.

For a localhost demonstration, Checkout and immediate server verification work
without ngrok. A public HTTPS webhook is required for production reliability.
The webhook endpoint is:

```text
https://YOUR_PUBLIC_BACKEND/api/razorpay/webhook
```

Subscribe it to:

```text
payment.captured
payment.failed
```

## 6. Build and Start the Backend

Before running through Eclipse, verify the backend from Command Prompt or
PowerShell inside the backend directory:

```bat
mvnw.cmd clean compile
```

Then start it from the Eclipse Run Configuration that contains the environment
variables.

Verify:

```text
Health:  http://localhost:8080/api/health
Swagger: http://localhost:8080/swagger-ui.html
```

The health endpoint should return an `UP` response.

## 7. Configure and Start the Frontend

In the frontend directory, copy `.env.example` to `.env` and use:

```text
VITE_API_BASE_URL=/api
VITE_BACKEND_TARGET=http://localhost:8080
VITE_USE_MOCK_API=false
```

Do not add database, Gmail, JWT, or Razorpay secrets to this file. Variables
whose names start with `VITE_` are delivered to the browser.

Install exact dependencies and start Vite:

```bat
npm ci
npm run dev
```

Open:

```text
http://localhost:5173
```

Run these checks before a demonstration:

```bat
npm run lint
npm run build
```

## 8. Correct First-User and Approval Flow

On a fresh database:

1. The first registered account becomes the bootstrap `ADMIN` account and is
   automatically approved because no administrator exists yet.
2. The first account must still verify its OTP before login.
3. Every later registration receives the base `RESIDENT` role and remains
   `PENDING` after OTP verification.
4. Admin approval activates that resident account; it does not add another role.
5. Admin can later add `SECRETARY`, `ACCOUNTANT`, `SECURITY`, or `ADMIN` as an
   optional second role.
6. The base `RESIDENT` role remains assigned.

## 9. Startup Order

Always start services in this order:

1. MySQL Server
2. Spring Boot backend on port 8080
3. React/Vite frontend on port 5173
4. Open the browser and sign in

## 10. Common Problems

### `Schema-validation: missing table ...`

The complete schema was not imported or the backend is connected to the wrong
database. Run `fresh_install_schema.sql` for a fresh database and verify
`DB_URL`. Keep `JPA_DDL_AUTO=validate` so schema mistakes are reported clearly.

### Lombok errors in Eclipse

Run `mvnw.cmd clean compile`. If Maven succeeds but Eclipse shows missing
constructors, builders, getters, or `log`, install Lombok into that Eclipse
installation, update the Maven project, and clean the project.

### Backend returns 403 after changing a role

Sign out and sign in again so a fresh JWT contains the current authorities.

### OTP email times out or is not delivered

Confirm that the Gmail App Password is correct, two-step verification is on,
internet access is available, and outbound SMTP port 587 is not blocked.

### Frontend cannot reach the backend

Confirm the backend health endpoint works, `.env` uses port 8080, and
`APP_CORS_ALLOWED_ORIGINS` includes `http://localhost:5173`. Restart Vite after
changing `.env`.

### Razorpay Checkout does not open

Confirm Test Mode credentials are set in the backend Run Configuration, the
bill is unpaid, internet access is available, and the user selected **Pay
online → Pay securely with Razorpay**.

### Uploaded files cannot be opened

Use an absolute writable `APP_UPLOAD_DIR`. Preserve that directory when moving
existing uploaded documents to another computer.

### Port already in use

Stop the old process or change `SERVER_PORT` and update
`VITE_BACKEND_TARGET` to the same backend port.

## 11. Security Checklist

- Never commit `run-local.ps1`, frontend `.env`, Gmail App Passwords, database
  passwords, JWT secrets, Razorpay secrets, or ngrok tokens.
- Use Razorpay Test Mode for demonstrations.
- Generate different JWT and webhook secrets on each installation.
- Keep Windows Security and antivirus protection enabled.
- Use HTTPS and a real secret manager before production deployment.

## 12. Final Verification

Before handing the project to another person, verify:

```text
[ ] Java 17 is active
[ ] Lombok is installed in Eclipse
[ ] MySQL schema imported successfully
[ ] Eclipse environment variables added
[ ] Backend health endpoint returns UP
[ ] Swagger UI opens
[ ] npm ci completed
[ ] Frontend opens on port 5173
[ ] First account can verify OTP and log in as Admin
[ ] Later account remains Resident after approval
[ ] Online Razorpay Test payment opens for an unpaid bill
[ ] Document upload and download work
```

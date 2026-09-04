# Local backend setup

1. Copy `run-local.ps1.example` to `run-local.ps1`.
2. Enter your MySQL password, Gmail address, Gmail App Password, and a long JWT secret in `run-local.ps1`.
3. Run the following in PowerShell from the backend directory:

   ```powershell
   .\run-local.ps1
   ```

4. Start the Spring Boot application from that same PowerShell window.

`run-local.ps1` is deliberately not committed: it holds your private credentials.

## What each setting means

| Setting | Required value |
| --- | --- |
| `DB_URL` | Your MySQL JDBC URL. Default database: `smart_society_connect_db`. |
| `DB_USERNAME` | Your MySQL user, commonly `root` locally. |
| `DB_PASSWORD` | Password for that MySQL user; use an empty string only if it has no password. |
| `MAIL_USERNAME` | The Gmail address that sends OTP messages. |
| `MAIL_PASSWORD` | A Gmail App Password, not your normal Gmail password. |
| `APP_JWT_SECRET` | A unique, private string with at least 32 characters. |
| `APP_CORS_ALLOWED_ORIGINS` | Frontend addresses permitted to call this API. |
| `RAZORPAY_KEY_ID` | Razorpay Test Mode key ID, such as `rzp_test_...`. |
| `RAZORPAY_KEY_SECRET` | Razorpay Test Mode key secret. Never expose this to React. |
| `RAZORPAY_WEBHOOK_SECRET` | Secret configured for the Razorpay webhook endpoint. |

Users register with their own email address. The application sends their OTP *from* `MAIL_USERNAME` *to* the email they entered.

## Razorpay test setup

1. For a new database, run `database/fresh_install_schema.sql` once; Razorpay
   and offline-payment audit tables are already included. For an older database,
   run only the missing migrations (`V3` for Razorpay and `V4` for Cash/Cheque
   audit fields).
2. Create Test Mode API keys in the Razorpay Dashboard.
3. Add the three Razorpay variables to your private `run-local.ps1`.
4. Enable automatic payment capture in the Razorpay Dashboard.
5. For a publicly reachable backend, configure this webhook URL:
   `https://YOUR_BACKEND/api/razorpay/webhook`
6. Subscribe the webhook to `payment.captured` and `payment.failed`.

For localhost demos the checkout and immediate server verification work without
a public webhook. The webhook is required before production deployment.

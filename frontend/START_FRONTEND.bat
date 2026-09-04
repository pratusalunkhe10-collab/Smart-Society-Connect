@echo off
setlocal
cd /d "%~dp0"
echo Checking backend...
powershell -NoProfile -Command "try { $r=Invoke-RestMethod -Uri 'http://localhost:8080/api/health' -TimeoutSec 4; Write-Host ('Backend status: ' + $r.status) -ForegroundColor Green } catch { Write-Host 'Backend is not reachable on http://localhost:8080/api/health' -ForegroundColor Red; Write-Host 'Start Spring Boot first, then run this file again.'; pause; exit 1 }"
if not exist node_modules (
  echo Installing frontend dependencies...
  call npm ci --no-audit --no-fund
  if errorlevel 1 exit /b 1
)
echo Starting frontend on http://localhost:5173
call npm run dev

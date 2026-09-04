@echo off
setlocal
cd /d "%~dp0"

echo ================================================
echo Smart Society Connect - Frontend Setup
echo ================================================
where node >nul 2>&1 || (
  echo ERROR: Node.js is not installed or is not in PATH.
  echo Install the current Node.js LTS version, reopen Command Prompt, then run this file again.
  pause
  exit /b 1
)
where npm >nul 2>&1 || (
  echo ERROR: npm is not available in PATH.
  pause
  exit /b 1
)

echo Node version:
node -v
echo npm version:
npm -v

echo.
echo Cleaning old installation files...
if exist node_modules rmdir /s /q node_modules
if exist "%LOCALAPPDATA%\npm-cache\_cacache" rmdir /s /q "%LOCALAPPDATA%\npm-cache\_cacache"

echo.
echo Verifying npm cache...
call npm cache verify
if errorlevel 1 (
  echo npm cache verification failed. Resetting cache...
  call npm cache clean --force
)

echo.
echo Installing dependencies from the public npm registry...
call npm ci --registry=https://registry.npmjs.org/ --no-audit --no-fund
if errorlevel 1 (
  echo.
  echo npm ci failed. Trying npm install...
  call npm install --registry=https://registry.npmjs.org/ --no-audit --no-fund
)
if errorlevel 1 (
  echo.
  echo INSTALLATION FAILED.
  echo Run: node -v
  echo Run: npm -v
  echo Then share the newest log from %%LOCALAPPDATA%%\npm-cache\_logs
  pause
  exit /b 1
)

echo.
echo Installation completed successfully.
echo Start the frontend with: npm run dev
pause

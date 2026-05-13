@echo off
cd /d "%~dp0"

where mvn >nul 2>&1
if %ERRORLEVEL% == 0 (
    set "MVN=mvn"
) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd" (
    set "MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd"
) else (
    echo Maven not found. Please install Maven or add it to PATH.
    exit /b 1
)

call "%MVN%" clean package -q
if %ERRORLEVEL% == 0 (
    echo BUILD_OK
) else (
    echo BUILD_FAILED
    call "%MVN%" clean compile
)

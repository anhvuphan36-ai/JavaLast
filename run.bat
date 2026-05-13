@echo off
echo Building...

where mvn >nul 2>&1
if %ERRORLEVEL% == 0 (
    set "MVN=mvn"
) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd" (
    set "MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd"
) else (
    echo Maven not found. Please install Maven or add it to PATH.
    pause
    exit /b 1
)

call "%MVN%" clean package -q -f "%~dp0pom.xml"
if not %ERRORLEVEL% == 0 (
    echo BUILD_FAILED
    pause
    exit /b 1
)

echo Starting app...
java -jar "%~dp0target\JudgeSystem-1.0-SNAPSHOT.jar"
pause

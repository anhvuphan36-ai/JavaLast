@echo off
echo ========================================
echo  AI-Powered CP Judge System
echo ========================================
echo.

echo [1/3] Kiem tra Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo LOI: Khong tim thay Java. Cai JDK 17+ tu https://adoptium.net
    pause
    exit /b 1
)
echo OK: Java da san sang.
echo.

echo [2/3] Kiem tra MySQL...
docker ps --filter "name=judge-mysql" --format "{{.Names}}" 2>nul | findstr "judge-mysql" >nul
if errorlevel 1 (
    echo MySQL chua chay. Dang khoi dong...
    docker-compose up -d
    if errorlevel 1 (
        echo LOI: Khong the khoi dong MySQL. Hay chay: docker-compose up -d
        pause
        exit /b 1
    )
    echo Dang cho MySQL san sang...
    timeout /t 10 /nobreak >nul
)
echo OK: MySQL da san sang.
echo.

echo [3/3] Khoi chay chuong trinh...
echo.
java -jar target\JudgeSystem-1.0-SNAPSHOT.jar

pause

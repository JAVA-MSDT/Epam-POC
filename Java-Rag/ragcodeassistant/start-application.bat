@echo off
REM Startup script for RAG Code Assistant

echo ========================================
echo    RAG Code Assistant Startup
echo ========================================
echo.

echo Step 1: Starting Qdrant Vector Database...
call scripts\run_qdrant.bat
if errorlevel 1 (
    echo Failed to start Qdrant. Exiting...
    pause
    exit /b 1
)

echo.
echo Step 2: Starting Ollama LLM Service...
call scripts\run_ollama.bat
if errorlevel 1 (
    echo Failed to start Ollama. Exiting...
    pause
    exit /b 1
)

echo.
echo Step 3: Starting Spring Boot Application...
echo Please wait while the application starts...
mvn spring-boot:run

pause
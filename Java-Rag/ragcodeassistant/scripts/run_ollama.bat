@echo off
REM Script to start Ollama locally and pull the mistral model

echo Setting up Ollama local LLM...

REM Check if Ollama is installed
where ollama >nul 2>&1
if errorlevel 1 (
    echo Ollama is not installed. Please install Ollama manually.
    echo Download from: https://ollama.ai
    echo For Windows, download the installer from the website.
    pause
    exit /b 1
)

REM Check if Ollama service is running
tasklist /FI "IMAGENAME eq ollama.exe" 2>NUL | find /I /N "ollama.exe" >nul
if errorlevel 1 (
    echo Starting Ollama service...
    start /B ollama serve
    echo Ollama service started
    
    REM Wait for service to be ready
    echo Waiting for Ollama service to be ready...
    timeout /t 5 /nobreak >nul
) else (
    echo Ollama service is already running
)

REM Wait for Ollama to be ready
echo Checking Ollama availability...
set /a counter=0
:check_loop
set /a counter+=1
curl -s http://localhost:11434/api/tags >nul 2>&1
if not errorlevel 1 (
    echo ✅ Ollama is ready!
    goto :ollama_ready
)
if %counter% geq 30 (
    echo ❌ Ollama failed to start after 30 attempts
    pause
    exit /b 1
)
echo Waiting for Ollama... (%counter%/30)
timeout /t 2 /nobreak >nul
goto :check_loop

:ollama_ready
REM Check if mistral model is already available
ollama list | find "mistral" >nul
if not errorlevel 1 (
    echo ✅ Mistral model is already available
) else (
    echo Pulling mistral model (this may take a while)...
    ollama pull mistral
    
    if not errorlevel 1 (
        echo ✅ Mistral model pulled successfully!
    ) else (
        echo ❌ Failed to pull mistral model
        pause
        exit /b 1
    )
)

REM Test the model
echo Testing Ollama with mistral model...
echo Hello, respond with just 'OK' if you're working | ollama run mistral >temp_response.txt 2>&1
findstr /C:"OK" temp_response.txt >nul
if not errorlevel 1 (
    echo ✅ Ollama and mistral model are working correctly!
) else (
    echo ⚠️  Ollama is running but model test failed. This might be normal on first run.
)
del temp_response.txt >nul 2>&1

echo.
echo Ollama Setup Complete!
echo    - API endpoint: http://localhost:11434
echo.
echo Usage:
echo    - Chat: ollama run mistral
echo    - API: curl http://localhost:11434/api/generate -d "{\"model\":\"mistral\",\"prompt\":\"Hello\"}"
echo.
echo To stop Ollama: taskkill /F /IM ollama.exe

pause
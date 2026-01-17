@echo off
echo Testing RAG Code Assistant compilation...

echo.
echo 1. Cleaning project...
call mvn clean

echo.
echo 2. Compiling project...
call mvn compile

echo.
echo 3. Running tests (if any)...
call mvn test

echo.
echo 4. Packaging application...
call mvn package -DskipTests

echo.
echo Compilation test complete!
pause
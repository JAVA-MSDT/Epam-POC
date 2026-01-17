@echo off
REM Test script for RAG Code Assistant API endpoints

echo ========================================
echo    RAG Code Assistant API Tests
echo ========================================
echo.

set BASE_URL=http://localhost:8080/api

echo Testing API endpoints...
echo.

echo 1. Testing health check (GET /documents)...
curl -s %BASE_URL%/documents
echo.
echo.

echo 2. Adding a sample document...
curl -X POST %BASE_URL%/documents ^
  -H "Content-Type: application/json" ^
  -d "{\"fileName\":\"HelloWorld.java\",\"filePath\":\"/src/HelloWorld.java\",\"content\":\"public class HelloWorld { public static void main(String[] args) { System.out.println(\\\"Hello World\\\"); } }\",\"language\":\"java\",\"className\":\"HelloWorld\"}"
echo.
echo.

echo 3. Searching for documents...
curl -s "%BASE_URL%/search?query=main method&limit=5"
echo.
echo.

echo 4. Testing code review...
curl -X POST %BASE_URL%/review ^
  -H "Content-Type: application/json" ^
  -d "{\"code\":\"public class Test { public static void main(String[] args) { System.out.println(\\\"test\\\"); } }\",\"language\":\"java\"}"
echo.
echo.

echo 5. Testing refactoring suggestions...
curl -X POST %BASE_URL%/refactor ^
  -H "Content-Type: application/json" ^
  -d "{\"code\":\"public class Test { public static void main(String[] args) { System.out.println(\\\"test\\\"); } }\",\"language\":\"java\"}"
echo.
echo.

echo API tests completed!
pause
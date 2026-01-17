@echo off
echo Testing Java RAG Code Review System
echo ====================================

echo.
echo Testing BadCodeExample.java...
java -cp target/classes com.epam.Main samples/BadCodeExample.java src/main/resources/checkstyle.xml src/main/resources/pmd-ruleset.xml src/main/resources/knowledgebase index

echo.
echo ====================================
echo Testing AnotherBadExample.java...
java -cp target/classes com.epam.Main samples/AnotherBadExample.java src/main/resources/checkstyle.xml src/main/resources/pmd-ruleset.xml src/main/resources/knowledgebase index

echo.
echo ====================================
echo Testing GoodCodeExample.java...
java -cp target/classes com.epam.Main samples/GoodCodeExample.java src/main/resources/checkstyle.xml src/main/resources/pmd-ruleset.xml src/main/resources/knowledgebase index

echo.
echo Testing completed!
pause
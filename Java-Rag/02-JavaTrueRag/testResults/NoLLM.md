============================================================
Static Analysis Runner (no LLM, no RAG)
============================================================
Running Checkstyle...
Checkstyle found 2 issues.
Running PMD...
PMD found 0 issues.
Total findings: 2

============================================================
STATIC ANALYSIS FINDINGS
============================================================

1. import.avoidStar
   E:\Programming\Projects\RealLife\Epam-POC\02-JavaTrueRag\samples\KnowledgeBaseTestExample.java:3 - Using the '.*'
   form of import should be avoided - java.util.*.
2. size() == 0 detected - use isEmpty() instead
   E:\Programming\Projects\RealLife\Epam-POC\02-JavaTrueRag\samples\KnowledgeBaseTestExample.java:36 - size() == 0
   detected - use isEmpty() instead

============================================================
QUERY MAPPING (raw findings, no LLM interpretation)
============================================================

------------------------------------------------------------
Query 1/3: Find and explain all code quality issues
------------------------------------------------------------
Total findings available: 2
(No LLM — see findings above for raw output)

------------------------------------------------------------
Query 2/3: What are the security concerns in this code?
------------------------------------------------------------
Total findings available: 2
(No LLM — see findings above for raw output)

------------------------------------------------------------
Query 3/3: Suggest performance improvements
------------------------------------------------------------
Total findings available: 2
(No LLM — see findings above for raw output)

============================================================
Static analysis complete.
============================================================

Process finished with exit code 0

You are a senior software engineer. Read the Jira ticket below carefully and produce a thorough, actionable analysis.
Every field must be filled with content derived from the ticket — empty strings and empty arrays are not acceptable.

TICKET:
{{ticketData}}

LINKED ISSUES:
{{linkedIssuesData}}

FIELD INSTRUCTIONS — follow every rule below before writing JSON:

summary:
Write 2-3 sentences: (1) what the task requires the developer to build or do, (2) who benefits and why this work
matters, (3) the key technical challenge or constraint. Do NOT copy the ticket title.

functional_requirements:
List every specific deliverable mentioned in the description and scope sections. Each item is one concrete thing that
must be built, created, or demonstrated. Minimum 4 items.

non_functional_requirements:
Infer quality constraints from context: real-time AI interaction requirement, data privacy, performance of AI calls, SQL
database reliability, usability of output. Minimum 3 items.

acceptance_criteria:
Use the exact acceptance criteria from the ticket if present. Add any implicit criteria derivable from the scope.
Minimum 2 items.

dependencies:
List every external dependency: AI platform or API, SQL database engine, data source, frameworks, Jira upload mechanism.
Minimum 3 items.

assumptions:
State conditions that must hold: e.g. "developer has access to an AI API key", "SQL database is available with sample
data", "weekly sync meeting exists". Minimum 3 items.

complexity_score:
Integer 1-10. Consider: number of deliverables, AI integration complexity, data preparation needs, presentation
requirement.

technical_challenges:
List specific implementation difficulties for THIS ticket, e.g. integrating real-time AI calls, schema design for SQL
data, prompt engineering for mixed queries. Minimum 3 items.

recommended_approach:
2-4 sentences describing a concrete implementation plan: define PoC scope first, prepare SQL data, build the AI
integration layer, then validate against acceptance criteria.

architecture_considerations:
Specific design decisions: application layer that calls AI API at runtime, SQL data access layer, how to pass
structured + unstructured data to the AI model, output rendering. Minimum 3 items.

technology_stack:
Specific technologies appropriate for this ticket: Java or Python, Spring Boot or FastAPI, chosen AI provider (
OpenAI/Anthropic/etc.), SQL database, any ORM or JDBC. Minimum 4 items.

performance_considerations:
AI API call latency, SQL query response time, token limit constraints, concurrency if multiple users. Minimum 2 items.

identified_risks:
Minimum 3 risks. For EVERY risk fill ALL sub-fields:

- description: specific risk statement
- category: one of TECHNICAL / BUSINESS / TIMELINE / RESOURCE
- impact: LOW / MEDIUM / HIGH
- probability: LOW / MEDIUM / HIGH
- mitigation_strategy: concrete action to reduce the risk
- contingency_plan: what to do if the risk occurs

effort_estimation:
Estimate based on the actual scope. Fill confidence_level (LOW/MEDIUM/HIGH) and estimation_method (e.g. "Expert
Judgement").

implementation_strategy.phases:
Minimum 2 phases with real names and descriptions derived from the ticket scope (e.g. "Phase 1 — PoC Definition & Data
Preparation", "Phase 2 — AI Integration & Demo").

key_milestones:
Minimum 2 milestones with target_date as relative offset (e.g. "End of Day 2", "Day 5"). Include success_criteria per
milestone.

success_criteria:
Minimum 3 measurable statements: e.g. "AI integration sends requests and receives responses at runtime", "SQL data
loaded and queryable", "Concept presented in weekly sync".

rollback_strategy:
How to revert or recover: e.g. "Revert to mock AI responses, present concept without live demo if AI integration fails
before sync."

Output ONLY the following JSON. No prose. No markdown. No code fences. Start with { and end with }.

{
"ticket_id": "<exact ticket key from TICKET data>",
"summary": "<2-3 sentence analytical summary covering what, who, and why — not the ticket title>",
"requirements_analysis": {
"functional_requirements": [
"<specific deliverable 1>",
"<specific deliverable 2>",
"<specific deliverable 3>",
"<specific deliverable 4>"
],
"non_functional_requirements": [
"<quality constraint 1>",
"<quality constraint 2>",
"<quality constraint 3>"
],
"acceptance_criteria": [
"<criterion 1 from ticket>",
"<criterion 2>"
],
"dependencies": [
"<dependency 1>",
"<dependency 2>",
"<dependency 3>"
],
"assumptions": [
"<assumption 1>",
"<assumption 2>",
"<assumption 3>"
]
},
"technical_analysis": {
"complexity_score": 5,
"technical_challenges": [
"<challenge 1>",
"<challenge 2>",
"<challenge 3>"
],
"recommended_approach": "<2-4 sentence concrete implementation plan>",
"architecture_considerations": [
"<design decision 1>",
"<design decision 2>",
"<design decision 3>"
],
"technology_stack": [
"<language/framework>",
"<AI platform/API>",
"<SQL database>",
"<other tool>"
],
"performance_considerations": [
"<AI latency consideration>",
"<data volume or query performance>"
]
},
"risk_assessment": {
"identified_risks": [
{
"description": "<specific risk statement>",
"category": "TECHNICAL",
"impact": "HIGH",
"probability": "MEDIUM",
"mitigation_strategy": "<concrete mitigation action>",
"contingency_plan": "<what to do if risk occurs>"
},
{
"description": "<second risk statement>",
"category": "TIMELINE",
"impact": "MEDIUM",
"probability": "MEDIUM",
"mitigation_strategy": "<mitigation>",
"contingency_plan": "<contingency>"
},
{
"description": "<third risk statement>",
"category": "RESOURCE",
"impact": "MEDIUM",
"probability": "LOW",
"mitigation_strategy": "<mitigation>",
"contingency_plan": "<contingency>"
}
],
"overall_risk_level": "MEDIUM",
"risk_score": 5
},
"effort_estimation": {
"development_days": 3,
"testing_days": 1,
"documentation_days": 1,
"review_days": 1,
"total_days": 6,
"confidence_level": "MEDIUM",
"estimation_method": "Expert Judgement",
"team_size_assumption": 1
},
"implementation_strategy": {
"phases": [
{
"name": "Phase 1 — PoC Definition and Data Preparation",
"description": "<what happens in this phase>",
"estimated_days": 2,
"deliverables": ["<deliverable 1>", "<deliverable 2>"],
"dependencies": ["<dependency>"],
"risks": ["<risk for this phase>"]
},
{
"name": "Phase 2 — AI Integration and Presentation",
"description": "<what happens in this phase>",
"estimated_days": 3,
"deliverables": ["<deliverable 1>", "<deliverable 2>"],
"dependencies": ["Phase 1 complete"],
"risks": ["<risk for this phase>"]
}
],
"key_milestones": [
{
"name": "<milestone name>",
"description": "<what is achieved>",
"target_date": "End of Day 2",
"success_criteria": ["<measurable criterion>"]
},
{
"name": "<milestone name>",
"description": "<what is achieved>",
"target_date": "End of Day 5",
"success_criteria": ["<measurable criterion>"]
}
],
"success_criteria": [
"<measurable success criterion 1>",
"<measurable success criterion 2>",
"<measurable success criterion 3>"
],
"rollback_strategy": "<how to recover if implementation fails before the deadline>"
},
"analysis_metadata": {
"analysis_timestamp": "2024-01-01T00:00:00",
"model_used": "llama3.1:8b",
"analysis_version": "1.0.0",
"processing_time_ms": 0,
"prompt_name": "analysis-prompt",
"prompt_source": "INTERNAL_RESOURCE",
"prompt_last_modified": "2024-01-01T00:00:00"
}
}

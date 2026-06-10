Perform a risk assessment of the Jira ticket below and output the JSON response.

TICKET:
{{ticketData}}

LINKED ISSUES:
{{linkedIssuesData}}

Risk categories: TECHNICAL (complexity, unknowns), BUSINESS (requirements, stakeholders), TIMELINE (dependencies),
RESOURCE (skills, staffing).

Output ONLY the following JSON. No prose. No markdown. No code fences. Start with { and end with }.

{
"ticket_id": "<ticket key from TICKET data>",
"summary": "<one-sentence summary>",
"requirements_analysis": {
"functional_requirements": ["<item>"],
"non_functional_requirements": ["<item>"],
"acceptance_criteria": ["<item>"],
"dependencies": ["<item>"],
"assumptions": ["<item>"]
},
"technical_analysis": {
"complexity_score": 5,
"technical_challenges": ["<item>"],
"recommended_approach": "<approach>",
"architecture_considerations": ["<item>"],
"technology_stack": ["<item>"],
"performance_considerations": ["<item>"]
},
"risk_assessment": {
"identified_risks": [
{
"description": "<risk>",
"category": "TECHNICAL",
"impact": "MEDIUM",
"probability": "MEDIUM",
"mitigation_strategy": "<strategy>",
"contingency_plan": "<plan>"
},
{
"description": "<business or timeline risk>",
"category": "BUSINESS",
"impact": "LOW",
"probability": "MEDIUM",
"mitigation_strategy": "<strategy>",
"contingency_plan": "<plan>"
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
"estimation_method": "Story Points",
"team_size_assumption": 2
},
"implementation_strategy": {
"phases": [
{
"name": "Risk Mitigation",
"description": "<description>",
"estimated_days": 3,
"deliverables": ["<item>"],
"dependencies": ["<item>"],
"risks": ["<item>"]
}
],
"key_milestones": [
{
"name": "Risk Review Checkpoint",
"description": "<description>",
"target_date": "<date>",
"success_criteria": ["<item>"]
}
],
"success_criteria": ["<item>"],
"rollback_strategy": "<strategy>"
},
"analysis_metadata": {
"analysis_timestamp": "2024-01-01T00:00:00",
"model_used": "llama3.1:8b",
"analysis_version": "1.0.0",
"processing_time_ms": 0,
"prompt_name": "risk-assessment-prompt",
"prompt_source": "INTERNAL_RESOURCE",
"prompt_last_modified": "2024-01-01T00:00:00"
}
}

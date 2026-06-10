Perform a security analysis of the Jira ticket below and output the JSON response.

TICKET:
{{ticketData}}

LINKED ISSUES:
{{linkedIssuesData}}

Evaluate: OWASP Top 10 vulnerabilities, PII/data privacy, auth & authorization, compliance (GDPR/PCI-DSS), dependency
risks, secrets management.

Output ONLY the following JSON. No prose. No markdown. No code fences. Start with { and end with }.

{
"ticket_id": "<ticket key from TICKET data>",
"summary": "<one-sentence security-focused summary>",
"requirements_analysis": {
"functional_requirements": ["<item>"],
"non_functional_requirements": ["<item>"],
"acceptance_criteria": ["<item>"],
"dependencies": ["<item>"],
"assumptions": ["<item>"]
},
"technical_analysis": {
"complexity_score": 5,
"technical_challenges": ["<security challenge>"],
"recommended_approach": "<security-first approach>",
"architecture_considerations": ["<item>"],
"technology_stack": ["<item>"],
"performance_considerations": ["<item>"]
},
"risk_assessment": {
"identified_risks": [
{
"description": "<OWASP/security vulnerability>",
"category": "SECURITY",
"impact": "HIGH",
"probability": "MEDIUM",
"mitigation_strategy": "<strategy>",
"contingency_plan": "<plan>"
}
],
"overall_risk_level": "HIGH",
"risk_score": 7
},
"effort_estimation": {
"development_days": 3,
"testing_days": 2,
"documentation_days": 1,
"review_days": 1,
"total_days": 7,
"confidence_level": "MEDIUM",
"estimation_method": "Story Points",
"team_size_assumption": 2
},
"implementation_strategy": {
"phases": [
{
"name": "Security Review",
"description": "<description>",
"estimated_days": 2,
"deliverables": ["<item>"],
"dependencies": ["<item>"],
"risks": ["<item>"]
}
],
"key_milestones": [
{
"name": "Penetration Test",
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
"prompt_name": "security-analysis-prompt",
"prompt_source": "INTERNAL_RESOURCE",
"prompt_last_modified": "2024-01-01T00:00:00"
}
}

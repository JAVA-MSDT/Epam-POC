# Skill: fetch_requirements

Fetch and parse requirements from any source: JIRA ticket, URL, or pasted text.

## Source Detection

| Input                                   | Type        | Method        |
|-----------------------------------------|-------------|---------------|
| Matches `[A-Z]+-\d+` (e.g., `PROJ-123`) | JIRA ID     | REST API call |
| Starts with `http://` or `https://`     | URL         | HTTP fetch    |
| Anything else                           | Pasted text | Use directly  |

## JIRA API Fetch

Required environment variables:

- `JIRA_URL` — e.g., `https://yourcompany.atlassian.net`
- `JIRA_USERNAME` — Atlassian account email
- `JIRA_API_KEY` — API token from https://id.atlassian.com/manage-profile/security/api-tokens

```bash
curl -s -u "$JIRA_USERNAME:$JIRA_API_KEY" \
  "$JIRA_URL/rest/api/3/issue/$TICKET_ID" \
  -H "Accept: application/json"
```

Parse response fields:

- `fields.summary` → title
- `fields.description` → description (Atlassian Document Format — walk the content nodes and extract text)
- `fields.issuetype.name` → type
- `fields.labels` → labels
- `fields.issuelinks` → linked issues

## URL Fetch

Fetch the page content and extract:

- Look for section headings: "Description", "Requirements", "Acceptance Criteria", "Definition of Done"
- Extract the text content under those sections
- Strip HTML tags and navigation elements

## Error Handling

- JIRA 401: Invalid credentials — tell user to check `JIRA_URL`, `JIRA_USERNAME`, `JIRA_API_KEY`
- JIRA 404: Ticket not found — verify the ID format and project key
- Network failure on URL: Fall back to asking user to paste content
- Ambiguous content: Note it as an open question — do not guess

## Fallback

If any fetch fails, ask:
> "Could not retrieve the requirements automatically. Please paste the ticket description and acceptance criteria here."

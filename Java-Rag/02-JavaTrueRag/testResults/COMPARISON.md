# Approach Comparison: Static Analysis vs LLM-Only vs RAG

## Results

| Aspect                | NoLLM                                    | LLM-Only                                                            | RAG                                                   |
|-----------------------|------------------------------------------|---------------------------------------------------------------------|-------------------------------------------------------|
| Output per query      | Identical raw findings for all 3 queries | Query-aware explanation (security / perf framing)                   | Query-aware + KB-grounded explanation                 |
| Code examples         | None                                     | LLM-generated (may hallucinate)                                     | Sourced from KB entry (`size_vs_isempty.json`)        |
| KB references         | None                                     | **Hallucinated** (`import_bestpractice`, `collection_bestpractice`) | Real retrieved entries, correctly cited               |
| Performance rationale | None                                     | Generic ("size() might be slow")                                    | Specific: Vector doesn't track its own size — from KB |

## Where RAG Shines

- **Grounded answers** — the LLM can only cite what was actually retrieved; hallucinated references are structurally
  impossible.
- **Domain knowledge injection** — KB entries carry curated rationale (e.g. *why* Vector's `size()` is slower) that a
  general-purpose model won't reliably reproduce.
- **Consistent sourcing across queries** — the same KB entry surfaces correctly whether the user asks about quality,
  security, or performance, because retrieval is query-independent (driven by findings).

## Key Findings

**LLM-Only hallucinated KB references.** In Query 2, the model invented `import_bestpractice` and
`collection_bestpractice` as if they were real KB entries. RAG made this impossible by design — the model only sees what
Lucene retrieved.

**RAG coverage is bounded by the KB.** `import.avoidStar` returned 0 KB matches, so for that finding RAG and LLM-Only
produced equivalent output. RAG only adds value where the KB has relevant content.

**Static analysis is the shared foundation.** All three approaches start from the same 2 Checkstyle findings. The
LLM/RAG layer adds interpretation, not discovery — the tools find the issues, the LLM explains them.

**Model artifacts leaked in LLM-Only output.** The `<｜begin▁of▁sentence｜>` token appeared twice in LLM-Only responses,
indicating the model's internal prompt boundary bled into the output. RAG responses did not exhibit this, likely because
the longer structured prompt kept the model on track.

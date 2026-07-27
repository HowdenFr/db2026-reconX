# ReconX ADR Prompt Template

Use this template whenever creating a new ADR with Claude. Commit both the
prompt and the ADR output in the same PR.

## Prompt Template

You are an enterprise software architect. Write an Architecture Decision Record
(ADR) in the Michael Nygard format (Title, Status, Context, Decision,
Consequences) for the following decision.

System: ReconX, a near-prod trade reconciliation platform.
Stack: PostgreSQL 16, Spring Boot 3, Kafka, React.
Scale: ~50,000 trades/day, 5-year retention, 10 concurrent recon analysts.

Decision to record: <ONE LINE DESCRIBING THE DECISION>

Alternatives we considered: <LIST 2-3>

Constraints / forces: <LIST 2-3>

Format: Markdown, Nygard 5-section template, no fluff. Keep under 300 words.
Include a "Status: Accepted | Date: <YYYY-MM-DD>" line.

## Process Expectations

- Save generated ADRs as `docs/adr/000X-<slug>.md`.
- Save the instantiated prompt as `docs/adr/prompts/000X-<slug>-prompt.md`.
- Review and tighten output so it names ReconX-specific scale, alternatives,
  and constraints before committing.
# TICKET-ADV016 - Day 1 Project Board Setup (Jira or GitHub Projects)

This artifact is an implementation package for creating the Day 1 board.
It includes epic structure, status workflow, field schema, and card payload.

## Tool Choice

- Preferred: GitHub Projects (close to repo workflow).
- Alternative: Jira board with equivalent fields.

## Epics

- RECONX-E1 - Day 1: Architecture and Setup (TICKET-ADV001 to TICKET-ADV005)
- RECONX-E2 - Day 1: Schema and Analytics (TICKET-ADV006 to TICKET-ADV011)
- RECONX-E3 - Day 1: Liquibase and Tooling (TICKET-ADV012 to TICKET-ADV017)

## Columns (Status)

- Backlog
- To Do
- In Progress
- In Review
- Done

## Field Schema

fields:
  - name: Exercise ID
    type: text
  - name: Estimate
    type: single_select
    options: [1, 2, 3, 5, 8]
  - name: Owner
    type: assignee
  - name: Linked PR
    type: text
  - name: Status
    type: single_select
    options: [Backlog, To Do, In Progress, In Review, Done]

## Card Data Source

- Import-ready card file: [docs/project-board/day1-cards.csv](docs/project-board/day1-cards.csv)
- Acceptance criteria source: [student-guides/day1/README.md](student-guides/day1/README.md)

## Important Note on ADV005

The Day 1 guide declares TICKET-ADV001 through TICKET-ADV017, but no
TICKET-ADV005 section exists in the guide or codebase TODO blocks. Card ADV005
is included as a placeholder to keep the requested one-card-per-ID sequence,
and should be clarified with trainer/instructor.

## End-to-End Flow Evidence

Card TICKET-ADV015 is set to Done in the seed data and marked as having flowed
through Backlog -> To Do -> In Progress -> In Review -> Done.

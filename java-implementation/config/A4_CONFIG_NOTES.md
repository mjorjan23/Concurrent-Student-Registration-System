# A4 Failure and Recovery Configuration

The A4 validation configurations use lower camel case keys:

- `failApproval`: inject deterministic approval-stage failures.
- `failBilling`: inject deterministic billing-stage failures.
- `failAfterEvents`: begin injecting configured failures after this many matching stage events; `-1` means immediately.
- `failStudent`: limit failure injection to a student name or ID; blank means all students.
- `maxRetries`: maximum retries after the initial attempt.

Legacy uppercase keys remain supported by `AppConfig` so older configurations still run.

## Concurrent Duplicate Race

`a4_concurrent_duplicate_race.properties` enables `concurrentDuplicateRace=true`.
For each logical registration, two producer threads wait on the same start latch and then
race to publish equivalent approval events. Worker scheduling determines which event wins.
The idempotency control must allow exactly one side effect and reject the competing event.
This is a concurrency-driven duplicate race rather than a sequential duplicate injection.

## Explicit Workflow State Machine

The asynchronous tracker enforces these legal transitions:

`untracked -> REGISTERED -> APPROVED -> BILLED`

A workflow may also transition from `REGISTERED` or `APPROVED` to `DEAD_LETTERED`
after retry exhaustion. `BILLED` and `DEAD_LETTERED` are terminal states. Duplicate and
out-of-order events are counted without replacing the last valid workflow state.

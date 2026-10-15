# Individual Integrity Packet

## Project Summary

Assignment 4 extends the asynchronous event-driven Student Registration System developed throughout the semester by adding validation, failure injection, retry and recovery mechanisms, Dead Letter Queue (DLQ) processing, metrics collection, and repeatable stress testing. Rather than redesigning the architecture from Assignment 3, my goal was to show that the existing system could continue to operate correctly under concurrent execution, different failure conditions, and simulated failures. The complete architecture, testing methodology, and results are documented in the accompanying README.

---

# Final Design Recommendation

My final recommendation is to keep the asynchronous event-driven architecture introduced in Assignment 3 and extended in Assignment 4. Thread-safe queues, dedicated workers, centralized status tracking, retry limits, DLQ processing, and validation controls allow the system to handle concurrency and failures while keeping the workflow organized and maintainable. The complete architecture and design rationale are documented in Sections 2–5 of the README.

---

# Assumptions and Risks

The following assumptions guided the design and implementation of Assignment 4:

- Registration requests may execute concurrently.
- Workflow stages communicate through asynchronous events.
- Validation occurs automatically after each experimental execution.
- Recoverable failures should be retried within configurable limits.
- Unrecoverable failures should be isolated through the Dead Letter Queue.
- AI-generated recommendations should be independently reviewed before implementation.

During development, I considered the following engineering risks:

- Race conditions
- Duplicate event processing
- Out-of-order event execution
- Retry loops
- Deadlock-like behavior
- Missed worker notifications
- Premature workflow completion
- Lost workflow state
- Behavioral differences between the Java and Python implementations

---

# Engineering Validation

I treated validation as part of the system design rather than something performed only after implementation. Evidence included automated validation, repeated stress testing, benchmark summaries, execution logs, runtime metrics, Java/Python comparisons, and failure injection scenarios.

Both the Java and Python validation suites completed successfully with **6 of 6 validation tests passing**. Each implementation also executed 13 experimental scenarios five times, resulting in 65 executions per implementation and 130 experimental executions across both languages.

The complete validation methodology, scenarios, benchmark summaries, stress-test results, and comparative analysis are documented in the accompanying README.

---

# AI Usage and Independent Verification

I used AI as an engineering assistant for discussing architecture, reviewing concurrent and asynchronous design decisions, debugging, evaluating tradeoffs, and reviewing documentation. I independently reviewed AI recommendations, modified them when necessary, and verified them through implementation and repeated testing before incorporating them into the project.

## Final AI Review

AI helped identify potential risks involving duplicate processing, out-of-order execution, retry behavior, failure containment, and documentation consistency. I incorporated recommendations that were supported by testing and deferred larger architectural suggestions that were outside the scope of Assignment 4. I independently verified the final system through execution logs, metrics, validation tests, and repeated stress testing. AI review could identify potential concurrency risks, but it could not determine the actual timing-dependent behavior of the running system without direct execution and repeated testing.

---

# Ownership

I am responsible for the final architecture, implementation, validation, documentation, and conclusions presented in Assignment 4.

---

# System Limits and Larger-Scale Failure

The architecture works well for the scope of Assignment 4, but it has limitations at a larger scale. Events are stored in in-memory queues, workers run within a single process, and worker counts are fixed. A process failure could cause queued events to be lost, while larger workloads could create queue or worker bottlenecks. A larger-scale implementation would need durable messaging, distributed state management, and more flexible worker scaling, as discussed further in Section 10 of the README.

---

# Escalation Process

When I encountered unexpected behavior during development, I followed this process:

1. Reproduce the behavior using a repeatable experimental scenario.
2. Review execution logs, validation results, workflow state, and benchmark data.
3. Determine whether the problem was architectural or implementation-specific.
4. Modify the implementation after identifying the underlying cause.
5. Re-run the validation framework and stress tests.
6. Verify that the change fixed the problem without introducing regressions.
7. Document the results before considering the issue resolved.

This process helped ensure that my implementation decisions were based on repeatable evidence rather than assumptions.

---

# Reflection

Assignment 4 shifted my focus from building an asynchronous system to proving that it could remain correct under concurrency and controlled failures. Implementing the same overall architecture in Java and Python also reinforced that reliable concurrent software depends more on good architecture, observability, repeatable testing, and validation than on the specific programming language being used.
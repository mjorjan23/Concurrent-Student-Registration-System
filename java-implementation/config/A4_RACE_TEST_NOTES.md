# Natural Concurrent Duplicate Race Test

`a4_concurrent_duplicate_race.properties` enables a natural producer race with:

```properties
concurrentDuplicateRace=true
approvalWorkers=4
billingWorkers=2
randomDelay=true
```

For each logical registration, two producer threads wait on the same start latch and then publish competing approval events at nearly the same time. The test does not use configured failure injection. Normal thread scheduling determines which event reaches an approval worker first.

The idempotency control must allow only one event to produce side effects. A passing run has:

- 3 tracked logical operations
- 3 approvals
- 3 invoices
- 3 detected duplicate events
- no failed or dead-lettered operations

Run only this scenario with:

```bash
./run_all_tests.sh
```

Run the complete A4 validation suite with:

```bash
./run_all_tests.sh
```

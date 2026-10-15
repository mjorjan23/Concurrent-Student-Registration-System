public class ValidationRunner
{
    private interface ResultValidator
    {
        void validate(AsyncRunResult result);
    }

    private static class ValidationTest
    {
        private final String name;
        private final String configFile;
        private final ResultValidator validator;

        private ValidationTest(String name, String configFile, ResultValidator validator)
        {
            this.name = name;
            this.configFile = configFile;
            this.validator = validator;
        }
    }

    private static final ValidationTest[] TESTS =
    {
        new ValidationTest("SUCCESS TEST", "config/a4_success.properties", result ->
        {
            require(result.getSubmitted() == 3, "expected 3 submitted operations");
            require(result.getCompleted() == 3, "all submitted operations should complete");
            require(result.getFailed() == 0, "no operation should fail");
            require(result.getRetries() == 0, "no retries should be needed");
            require(result.getDeadLetters() == 0, "dead-letter queue should be empty");
        }),
        new ValidationTest("CONCURRENT TEST", "config/a4_concurrent.properties", result ->
        {
            require(result.getSubmitted() == 250, "expected 250 submitted operations");
            require(result.getCompleted() == 250, "all concurrent operations should complete");
            require(result.getFailed() == 0, "no concurrent operation should fail");
            require(result.getDeadLetters() == 0, "dead-letter queue should be empty");
            require(result.getTrackedOperations() == 250, "all operations should be tracked");
            require(result.getBilledStateCount() == 250, "all operations should finish in BILLED state");
        }),
        new ValidationTest("ASYNC DUPLICATE-CONTROL TEST", "config/a4_duplicate_control.properties", result ->
        {
            require(result.getSubmitted() == 3, "expected 3 submitted operations");
            require(result.getCompleted() == 3, "duplicate injection must not prevent completion");
            require(result.getDuplicateEvents() == 3, "one duplicate approval event should be ignored per operation");
            require(result.getFailed() == 0, "duplicates should not create failed operations");
            require(result.getDeadLetters() == 0, "duplicates should not reach the dead-letter queue");
        }),
        new ValidationTest("CONCURRENT DUPLICATE RACE TEST", "config/a4_concurrent_duplicate_race.properties", result ->
        {
            require(result.getSubmitted() == 3, "expected 3 logical operations");
            require(result.getCompleted() == 3, "all logical operations should complete once");
            require(result.getDuplicateEvents() == 3, "one concurrently published duplicate should be ignored per operation");
            require(result.getApprovalCount() == 3, "concurrent duplicates must not create duplicate approvals");
            require(result.getInvoiceCount() == 3, "concurrent duplicates must not create duplicate invoices");
            require(result.getBilledStateCount() == 3, "all workflows should finish in BILLED state");
        }),
        new ValidationTest("FAILURE / DEAD-LETTER TEST", "config/a4_dead_letter.properties", result ->
        {
            require(result.getSubmitted() == 3, "expected 3 submitted operations");
            require(result.getCompleted() == 2, "two unaffected operations should complete");
            require(result.getFailed() == 1, "the targeted operation should fail");
            require(result.getRetries() == 3, "the failed operation should use all three retries");
            require(result.getDeadLetters() == 1, "one event should be dead-lettered");
            require(result.getDeadLetteredStateCount() == 1, "one workflow should finish in DEAD_LETTERED state");
        }),
        new ValidationTest("RECOVERY TEST", "config/a4_recovery.properties", result ->
        {
            require(result.getSubmitted() == 3, "expected 3 submitted operations");
            require(result.getCompleted() == 3, "all transient failures should recover");
            require(result.getFailed() == 0, "no operation should remain failed");
            require(result.getRetries() == 3, "each operation should retry once");
            require(result.getDeadLetters() == 0, "recovered operations should not be dead-lettered");
        })
    };

    public static void main(String[] args)
    {
        Logger.log("========================================");
        Logger.log("A4 JAVA VALIDATION RUNNER");
        Logger.log("========================================");

        int passed = 0;
        for(ValidationTest test : TESTS)
        {
            Logger.log("\n>>>>>>>> " + test.name + " <<<<<<<<");
            try
            {
                AsyncRunResult result = AsyncMain.run(new String[] {test.configFile});
                test.validator.validate(result);
                passed++;
                Logger.log("VALIDATION RESULT: PASS - " + test.name);
            }
            catch(Exception e)
            {
                Logger.log("VALIDATION RESULT: FAIL - " + test.name + " - " + e.getMessage());
            }
        }

        Logger.log("========================================");
        Logger.log("VALIDATION SUMMARY: " + passed + " of " + TESTS.length + " tests passed.");
        Logger.log("========================================");

        if(passed != TESTS.length)
            throw new IllegalStateException("One or more A4 validation tests failed.");
    }

    private static void require(boolean condition, String message)
    {
        if(!condition)
            throw new IllegalStateException(message);
    }
}

public class AsyncRunResult
{
    private final int submitted;
    private final int completed;
    private final int failed;
    private final int retries;
    private final int deadLetters;
    private final int duplicateEvents;
    private final int outOfOrderEvents;
    private final int trackedOperations;
    private final int billedStateCount;
    private final int deadLetteredStateCount;
    private final int enrollmentCount;
    private final int approvalCount;
    private final int invoiceCount;
    private final int gradeHoldCount;
    private final long elapsedMs;

    public AsyncRunResult(MetricsCollector metrics,
                          AsyncStatusTracker tracker,
                          Course course,
                          ProfessorApprovalWorkflow approvalWorkflow,
                          BillingService billingService,
                          GradeHoldService gradeHoldService)
    {
        submitted = metrics.getSubmitted();
        completed = metrics.getCompleted();
        failed = metrics.getFailed();
        retries = metrics.getRetries();
        deadLetters = metrics.getDeadLetters();
        duplicateEvents = tracker.getDuplicateEvents();
        outOfOrderEvents = tracker.getOutOfOrderEvents();
        trackedOperations = tracker.getTrackedOperationCount();
        billedStateCount = tracker.countOperationsInState(AsyncOperationStatus.BILLED);
        deadLetteredStateCount = tracker.countOperationsInState(AsyncOperationStatus.DEAD_LETTERED);
        enrollmentCount = course.getEnrollmentCount();
        approvalCount = approvalWorkflow.getApprovalCount();
        invoiceCount = billingService.getInvoices().size();
        gradeHoldCount = gradeHoldService.getHoldCount();
        elapsedMs = metrics.getElapsedMs();
    }

    public int getSubmitted() { return submitted; }
    public int getCompleted() { return completed; }
    public int getFailed() { return failed; }
    public int getRetries() { return retries; }
    public int getDeadLetters() { return deadLetters; }
    public int getDuplicateEvents() { return duplicateEvents; }
    public int getOutOfOrderEvents() { return outOfOrderEvents; }
    public int getTrackedOperations() { return trackedOperations; }
    public int getBilledStateCount() { return billedStateCount; }
    public int getDeadLetteredStateCount() { return deadLetteredStateCount; }
    public int getEnrollmentCount() { return enrollmentCount; }
    public int getApprovalCount() { return approvalCount; }
    public int getInvoiceCount() { return invoiceCount; }
    public int getGradeHoldCount() { return gradeHoldCount; }
    public long getElapsedMs() { return elapsedMs; }
}

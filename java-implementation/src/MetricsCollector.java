import java.util.concurrent.atomic.AtomicInteger;

public class MetricsCollector
{
    private final AtomicInteger submitted = new AtomicInteger();
    private final AtomicInteger completed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger retries = new AtomicInteger();
    private final AtomicInteger deadLetters = new AtomicInteger();
    private long elapsedMs;

    public void recordSubmitted() { submitted.incrementAndGet(); }
    public void recordCompleted() { completed.incrementAndGet(); }
    public void recordFailed() { failed.incrementAndGet(); }
    public void recordRetry() { retries.incrementAndGet(); }
    public void recordDeadLetter() { deadLetters.incrementAndGet(); }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }

    public int getSubmitted() { return submitted.get(); }
    public int getCompleted() { return completed.get(); }
    public int getFailed() { return failed.get(); }
    public int getRetries() { return retries.get(); }
    public int getDeadLetters() { return deadLetters.get(); }
    public long getElapsedMs() { return elapsedMs; }

    public void printFinalReport(AsyncStatusTracker tracker)
    {
        Logger.log("========= FINAL REPORT =========");
        Logger.log("Submitted: " + getSubmitted());
        Logger.log("Completed: " + getCompleted());
        Logger.log("Failed: " + getFailed());
        Logger.log("Retries: " + getRetries());
        Logger.log("Dead Letters: " + getDeadLetters());
        Logger.log("Duplicate Events Ignored: " + tracker.getDuplicateEvents());
        Logger.log(String.format("Runtime: %.2f sec", elapsedMs / 1000.0));
    }
}

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncBillingWorker implements Runnable
{
    private final BlockingQueue<AsyncApprovalEvent> billingQueue;
    private final BillingWorkflow billingWorkflow;
    private final AsyncStatusTracker tracker;
    private final FailureInjector failureInjector;
    private final DeadLetterQueue deadLetterQueue;
    private final MetricsCollector metrics;
    private final AtomicInteger pendingBillingEvents;
    private final int billingDelayMs;
    private final boolean randomDelay;
    private final boolean controlsEnabled;
    private final int maxRetries;

    public AsyncBillingWorker(BlockingQueue<AsyncApprovalEvent> billingQueue,
                              BillingWorkflow billingWorkflow,
                              AsyncStatusTracker tracker,
                              FailureInjector failureInjector,
                              DeadLetterQueue deadLetterQueue,
                              MetricsCollector metrics,
                              AtomicInteger pendingBillingEvents,
                              int billingDelayMs,
                              boolean randomDelay,
                              boolean controlsEnabled,
                              int maxRetries)
    {
        this.billingQueue = billingQueue;
        this.billingWorkflow = billingWorkflow;
        this.tracker = tracker;
        this.failureInjector = failureInjector;
        this.deadLetterQueue = deadLetterQueue;
        this.metrics = metrics;
        this.pendingBillingEvents = pendingBillingEvents;
        this.billingDelayMs = billingDelayMs;
        this.randomDelay = randomDelay;
        this.controlsEnabled = controlsEnabled;
        this.maxRetries = maxRetries;
    }

    @Override
    public void run()
    {
        try
        {
            while(true)
            {
                AsyncApprovalEvent event = billingQueue.take();
                if(event == AsyncApprovalEvent.POISON_PILL)
                {
                    Logger.log(Thread.currentThread().getName() + " received async stop signal.");
                    break;
                }

                try
                {
                    process(event);
                }
                finally
                {
                    pendingBillingEvents.decrementAndGet();
                }
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private void process(AsyncApprovalEvent event) throws InterruptedException
    {
        Logger.log("[op=" + event.getOperationId() + "][event=" + event.getEventId() +
                "] billing worker consumed event for " + event.getStudent().getName() +
                " (attempt " + (event.getAttempt() + 1) + ").");

        String idempotencyKey = event.getIdempotencyKey();
        if(failureInjector.isEnabled())
            idempotencyKey += ":attempt:" + event.getAttempt();

        if(controlsEnabled)
        {
            if(!tracker.shouldProcess(idempotencyKey))
                return;

            AsyncOperationStatus currentStatus = tracker.getStatus(event.getOperationId());
            if(currentStatus != AsyncOperationStatus.APPROVED)
            {
                tracker.recordOutOfOrder(event.getOperationId(),
                        "billing event blocked because operation status was " + currentStatus + ", not APPROVED.");
                return;
            }
        }
        else if(event.getSequence() < 2 || tracker.getStatus(event.getOperationId()) != AsyncOperationStatus.APPROVED)
        {
            tracker.recordOutOfOrder(event.getOperationId(), "billing processed before approval guard was checked.");
        }
        else if(event.getAttempt() > 0)
        {
            tracker.recordDuplicateFailure(event.getIdempotencyKey());
        }

        DelayHelper.pause("Async billing", billingDelayMs, randomDelay);

        if(failureInjector.shouldFailBilling(event.getStudent()))
        {
            handleFailure(event, "deterministic billing failure injected by FailureInjector");
            return;
        }

        billingWorkflow.generateBillAndCheckPayment(event.getStudent(), event.getCourse(), event.doesStudentPay());
        if(tracker.transitionTo(event.getOperationId(), AsyncOperationStatus.BILLED))
            metrics.recordCompleted();
        else
            tracker.recordOutOfOrder(event.getOperationId(),
                    "billing side effect completed but the BILLED transition was rejected.");
    }

    private void handleFailure(AsyncApprovalEvent event, String reason) throws InterruptedException
    {
        if(event.getAttempt() < maxRetries)
        {
            tracker.recordRetry(event.getOperationId());
            metrics.recordRetry();
            int nextAttempt = event.getAttempt() + 1;
            pendingBillingEvents.incrementAndGet();
            billingQueue.put(new AsyncApprovalEvent(
                    event.getOperationId(),
                    event.getEventId() + "-retry" + nextAttempt,
                    event.getStudent(),
                    event.getCourse(),
                    event.doesStudentPay(),
                    event.getSequence(),
                    nextAttempt));
            Logger.log("[op=" + event.getOperationId() + "] billing retry " + nextAttempt +
                    " of " + maxRetries + " queued.");
        }
        else
        {
            tracker.transitionTo(event.getOperationId(), AsyncOperationStatus.DEAD_LETTERED);
            metrics.recordFailed();
            metrics.recordDeadLetter();
            deadLetterQueue.add(new DeadLetterEvent(
                    event.getOperationId(), event.getEventId(), "BILLING",
                    event.getStudent().getName(), event.getAttempt() + 1, reason));
        }
    }
}

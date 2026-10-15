import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncApprovalWorker implements Runnable
{
    private final BlockingQueue<AsyncEnrollmentEvent> approvalQueue;
    private final BlockingQueue<AsyncApprovalEvent> billingQueue;
    private final ProfessorApprovalWorkflow approvalWorkflow;
    private final AsyncStatusTracker tracker;
    private final FailureInjector failureInjector;
    private final DeadLetterQueue deadLetterQueue;
    private final MetricsCollector metrics;
    private final int approvalDelayMs;
    private final boolean randomDelay;
    private final boolean controlsEnabled;
    private final boolean transientRetryMode;
    private final AtomicInteger pendingApprovalEvents;
    private final AtomicInteger pendingBillingEvents;
    private final int maxRetries;

    public AsyncApprovalWorker(BlockingQueue<AsyncEnrollmentEvent> approvalQueue,
                               BlockingQueue<AsyncApprovalEvent> billingQueue,
                               ProfessorApprovalWorkflow approvalWorkflow,
                               AsyncStatusTracker tracker,
                               FailureInjector failureInjector,
                               DeadLetterQueue deadLetterQueue,
                               MetricsCollector metrics,
                               int approvalDelayMs,
                               boolean randomDelay,
                               boolean controlsEnabled,
                               boolean transientRetryMode,
                               AtomicInteger pendingApprovalEvents,
                               AtomicInteger pendingBillingEvents,
                               int maxRetries)
    {
        this.approvalQueue = approvalQueue;
        this.billingQueue = billingQueue;
        this.approvalWorkflow = approvalWorkflow;
        this.tracker = tracker;
        this.failureInjector = failureInjector;
        this.deadLetterQueue = deadLetterQueue;
        this.metrics = metrics;
        this.approvalDelayMs = approvalDelayMs;
        this.randomDelay = randomDelay;
        this.controlsEnabled = controlsEnabled;
        this.transientRetryMode = transientRetryMode;
        this.pendingApprovalEvents = pendingApprovalEvents;
        this.pendingBillingEvents = pendingBillingEvents;
        this.maxRetries = maxRetries;
    }

    @Override
    public void run()
    {
        try
        {
            while(true)
            {
                AsyncEnrollmentEvent event = approvalQueue.take();
                if(event == AsyncEnrollmentEvent.POISON_PILL)
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
                    pendingApprovalEvents.decrementAndGet();
                }
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private void process(AsyncEnrollmentEvent event) throws InterruptedException
    {
        Logger.log("[op=" + event.getOperationId() + "][event=" + event.getEventId() +
                "] approval worker consumed event for " + event.getStudent().getName() +
                " (attempt " + (event.getAttempt() + 1) + ").");

        String idempotencyKey = event.getIdempotencyKey();
        if(transientRetryMode || failureInjector.isEnabled())
            idempotencyKey += ":attempt:" + event.getAttempt();

        if(controlsEnabled && !tracker.shouldProcess(idempotencyKey))
            return;
        else if(!controlsEnabled && event.getAttempt() > 0)
            tracker.recordDuplicateFailure(event.getIdempotencyKey());

        DelayHelper.pause("Async approval", approvalDelayMs, randomDelay);

        boolean transientFailure = transientRetryMode && event.getAttempt() == 0;
        boolean injectedFailure = failureInjector.shouldFailApproval(event.getStudent());
        if(transientFailure || injectedFailure)
        {
            String reason = transientFailure
                    ? "deterministic transient approval failure"
                    : "deterministic approval failure injected by FailureInjector";
            handleFailure(event, reason);
            return;
        }

        ApprovalStatus status = approvalWorkflow.approveStudent(event.getStudent(), event.getCourse());
        if(status == ApprovalStatus.APPROVED || (!controlsEnabled && status == ApprovalStatus.DUPLICATE))
        {
            if(!tracker.transitionTo(event.getOperationId(), AsyncOperationStatus.APPROVED))
            {
                tracker.recordOutOfOrder(event.getOperationId(),
                        "approval completion was rejected by the workflow state machine.");
                return;
            }
            pendingBillingEvents.incrementAndGet();
            billingQueue.put(new AsyncApprovalEvent(
                    event.getOperationId(),
                    event.getEventId() + "-billing",
                    event.getStudent(),
                    event.getCourse(),
                    event.doesStudentPay(),
                    2,
                    0));
            Logger.log("[op=" + event.getOperationId() + "] approval published billing event.");
        }
        else
        {
            handleFailure(event, "approval workflow returned " + status);
        }
    }

    private void handleFailure(AsyncEnrollmentEvent event, String reason) throws InterruptedException
    {
        if(event.getAttempt() < maxRetries)
        {
            tracker.recordRetry(event.getOperationId());
            metrics.recordRetry();
            int nextAttempt = event.getAttempt() + 1;
            pendingApprovalEvents.incrementAndGet();
            approvalQueue.put(new AsyncEnrollmentEvent(
                    event.getOperationId(),
                    event.getEventId() + "-retry" + nextAttempt,
                    event.getStudent(),
                    event.getCourse(),
                    event.doesStudentPay(),
                    event.getSequence(),
                    nextAttempt));
            Logger.log("[op=" + event.getOperationId() + "] approval retry " + nextAttempt +
                    " of " + maxRetries + " queued.");
        }
        else
        {
            tracker.transitionTo(event.getOperationId(), AsyncOperationStatus.DEAD_LETTERED);
            metrics.recordFailed();
            metrics.recordDeadLetter();
            deadLetterQueue.add(new DeadLetterEvent(
                    event.getOperationId(), event.getEventId(), "APPROVAL",
                    event.getStudent().getName(), event.getAttempt() + 1, reason));
        }
    }
}

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncMain
{
    public static void main(String[] args)
    {
        run(args);
    }

    public static AsyncRunResult run(String[] args)
    {
        String configFile = args.length > 0 ? args[0] : "config/normal.properties";
        AppConfig config = AppConfig.load(configFile);
        String mode = args.length > 1 ? args[1] : config.getAsyncMode();
        boolean controlsEnabled = mode.toLowerCase().startsWith("safe");
        boolean duplicateFailure = mode.equalsIgnoreCase("duplicate") ||
                mode.equalsIgnoreCase("unsafe_all") || mode.equalsIgnoreCase("safe_duplicate");
        boolean outOfOrderFailure = mode.equalsIgnoreCase("out_of_order") ||
                mode.equalsIgnoreCase("unsafe_all") || mode.equalsIgnoreCase("safe_out_of_order");
        boolean transientRetryMode = mode.equalsIgnoreCase("safe_retry");

        Teacher teacher = new Teacher(config.getTeacherId(), config.getTeacherName());
        Course course = new Course(config.getCourseId(), config.getCourseName(), teacher, config.getCapacity());
        WaitList waitList = new WaitList();
        RegistrationCoordinator coordinator = new RegistrationCoordinator(waitList, config);
        LicenseService licenseService = new LicenseService();
        ProfessorApprovalWorkflow approvalWorkflow = new ProfessorApprovalWorkflow(teacher, licenseService, config);
        BillingService billingService = new BillingService();
        GradeHoldService gradeHoldService = new GradeHoldService();
        BillingWorkflow billingWorkflow = new BillingWorkflow(billingService, gradeHoldService, config);
        AsyncStatusTracker tracker = new AsyncStatusTracker();
        FailureInjector failureInjector = new FailureInjector(config);
        DeadLetterQueue deadLetterQueue = new DeadLetterQueue();
        MetricsCollector metrics = new MetricsCollector();

        BlockingQueue<AsyncEnrollmentEvent> approvalQueue = new LinkedBlockingQueue<AsyncEnrollmentEvent>();
        BlockingQueue<AsyncApprovalEvent> billingQueue = new LinkedBlockingQueue<AsyncApprovalEvent>();
        AtomicInteger pendingApprovalEvents = new AtomicInteger(0);
        AtomicInteger pendingBillingEvents = new AtomicInteger(0);

        Thread[] approvalThreads = new Thread[config.getApprovalWorkers()];
        Thread[] billingThreads = new Thread[config.getBillingWorkers()];

        for(int i = 0; i < approvalThreads.length; i++)
        {
            approvalThreads[i] = new Thread(new AsyncApprovalWorker(
                    approvalQueue, billingQueue, approvalWorkflow, tracker,
                    failureInjector, deadLetterQueue, metrics,
                    config.getApprovalDelayMs(), config.isRandomDelay(), controlsEnabled,
                    transientRetryMode, pendingApprovalEvents, pendingBillingEvents,
                    config.getMaxRetries()));
            approvalThreads[i].setName("Async-Approval-Worker-" + (i + 1));
            approvalThreads[i].start();
        }

        for(int i = 0; i < billingThreads.length; i++)
        {
            billingThreads[i] = new Thread(new AsyncBillingWorker(
                    billingQueue, billingWorkflow, tracker,
                    failureInjector, deadLetterQueue, metrics, pendingBillingEvents,
                    config.getBillingDelayMs(), config.isRandomDelay(), controlsEnabled,
                    config.getMaxRetries()));
            billingThreads[i].setName("Async-Billing-Worker-" + (i + 1));
            billingThreads[i].start();
        }

        ArrayList<StudentInput> students = StudentLoader.load(config.getStudentFile());
        long startTime = System.currentTimeMillis();

        Logger.log("==============================");
        Logger.log("A4 ASYNC MODE: " + mode);
        Logger.log("Controls enabled: " + controlsEnabled);
        Logger.log("Failure injector: " + failureInjector.describe());
        Logger.log("Maximum retries: " + config.getMaxRetries());
        Logger.log("Registration submits events and returns without waiting for approval or billing.");
        Logger.log("==============================");

        int operationNumber = 0;
        for(StudentInput input : students)
        {
            Student student = input.getStudent();
            RegistrationResult result = coordinator.processRegistration(student, course);
            if(result == RegistrationResult.ENROLLED)
            {
                operationNumber++;
                metrics.recordSubmitted();
                String operationId = "op-" + operationNumber + "-student-" + student.getStudentId();
                if(!tracker.transitionTo(operationId, AsyncOperationStatus.REGISTERED))
                    throw new IllegalStateException("Could not initialize workflow state for " + operationId);

                AsyncEnrollmentEvent event = new AsyncEnrollmentEvent(
                        operationId, operationId + "-approval-1", student, course, input.getPays(), 1, 0);

                if(config.isConcurrentDuplicateRace())
                {
                    publishConcurrentDuplicateRace(approvalQueue, pendingApprovalEvents, event);
                    Logger.log("[op=" + operationId + "] two producer threads raced to publish the same logical approval event.");
                }
                else
                {
                    pendingApprovalEvents.incrementAndGet();
                    approvalQueue.add(event);
                    Logger.log("[op=" + operationId + "] registration published async approval event and returned.");
                }

                if(duplicateFailure)
                {
                    pendingApprovalEvents.incrementAndGet();
                    approvalQueue.add(new AsyncEnrollmentEvent(
                            operationId, operationId + "-approval-duplicate", student, course,
                            input.getPays(), 1, 1));
                    Logger.log("[op=" + operationId + "] TEST INJECTION: duplicate approval event published.");
                }

                if(outOfOrderFailure)
                {
                    pendingBillingEvents.incrementAndGet();
                    billingQueue.add(new AsyncApprovalEvent(
                            operationId, operationId + "-billing-too-early", student, course,
                            input.getPays(), 0, 0));
                    Logger.log("[op=" + operationId + "] TEST INJECTION: out-of-order billing event published before approval.");
                }
            }
        }

        waitForWorkToFinish(pendingApprovalEvents, pendingBillingEvents);
        stopWorkers(approvalQueue, billingQueue, approvalThreads, billingThreads);

        long elapsed = System.currentTimeMillis() - startTime;
        metrics.setElapsedMs(elapsed);

        Logger.log("==============================");
        Logger.log("A4 ASYNC FINAL STATUS SUMMARY");
        Logger.log("==============================");
        Logger.log("Tracked operations: " + tracker.getTrackedOperationCount());
        Logger.log("Enrollment count: " + course.getEnrollmentCount());
        Logger.log("Approval count: " + approvalWorkflow.getApprovalCount());
        Logger.log("License count: " + licenseService.getLicenseCount());
        Logger.log("Invoice count: " + billingService.getInvoices().size());
        Logger.log("Grade hold count: " + gradeHoldService.getHoldCount());
        Logger.log("Duplicate events detected: " + tracker.getDuplicateEvents());
        Logger.log("Out-of-order events detected: " + tracker.getOutOfOrderEvents());
        Logger.log("Retried events: " + tracker.getRetriedEvents());
        deadLetterQueue.printSummary();
        metrics.printFinalReport(tracker);

        return new AsyncRunResult(metrics, tracker, course, approvalWorkflow,
                billingService, gradeHoldService);
    }


    private static void publishConcurrentDuplicateRace(
            BlockingQueue<AsyncEnrollmentEvent> approvalQueue,
            AtomicInteger pendingApprovalEvents,
            AsyncEnrollmentEvent event)
    {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Thread[] producers = new Thread[2];

        for(int i = 0; i < producers.length; i++)
        {
            final int producerNumber = i + 1;
            producers[i] = new Thread(() ->
            {
                try
                {
                    ready.countDown();
                    start.await();
                    pendingApprovalEvents.incrementAndGet();
                    approvalQueue.put(new AsyncEnrollmentEvent(
                            event.getOperationId(),
                            event.getEventId() + "-producer-" + producerNumber,
                            event.getStudent(), event.getCourse(), event.doesStudentPay(),
                            event.getSequence(), event.getAttempt()));
                }
                catch(InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }, "Race-Producer-" + producerNumber);
            producers[i].start();
        }

        try
        {
            ready.await();
            start.countDown();
            for(Thread producer : producers)
                producer.join();
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent race publisher was interrupted.", e);
        }
    }

    private static void waitForWorkToFinish(AtomicInteger pendingApprovalEvents,
                                            AtomicInteger pendingBillingEvents)
    {
        try
        {
            while(pendingApprovalEvents.get() > 0 || pendingBillingEvents.get() > 0)
                Thread.sleep(5);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static void stopWorkers(BlockingQueue<AsyncEnrollmentEvent> approvalQueue,
                                    BlockingQueue<AsyncApprovalEvent> billingQueue,
                                    Thread[] approvalThreads,
                                    Thread[] billingThreads)
    {
        try
        {
            for(int i = 0; i < approvalThreads.length; i++)
                approvalQueue.put(AsyncEnrollmentEvent.POISON_PILL);
            for(Thread thread : approvalThreads)
                thread.join();

            for(int i = 0; i < billingThreads.length; i++)
                billingQueue.put(AsyncApprovalEvent.POISON_PILL);
            for(Thread thread : billingThreads)
                thread.join();
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}

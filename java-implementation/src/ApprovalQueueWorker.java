import java.util.concurrent.BlockingQueue;

public class ApprovalQueueWorker implements Runnable
{
    private final BlockingQueue<EnrollmentEvent> approvalQueue;
    private final BlockingQueue<ApprovalEvent> billingQueue;
    private final ProfessorApprovalWorkflow approvalWorkflow;
    private final int approvalDelayMs;
    private final boolean randomDelay;
    private final AppConfig config;

    public ApprovalQueueWorker(BlockingQueue<EnrollmentEvent> approvalQueue,
                               BlockingQueue<ApprovalEvent> billingQueue,
                               ProfessorApprovalWorkflow approvalWorkflow,
                               int approvalDelayMs,
                               boolean randomDelay,
                               AppConfig config)
    {
        this.approvalQueue = approvalQueue;
        this.billingQueue = billingQueue;
        this.approvalWorkflow = approvalWorkflow;
        this.approvalDelayMs = approvalDelayMs;
        this.randomDelay = randomDelay;
        this.config = config;
    }

    @Override
    public void run()
    {
        try
        {
            while(true)
            {
                EnrollmentEvent event = approvalQueue.take();

                if(event == EnrollmentEvent.POISON_PILL)
                {
                    Logger.log(Thread.currentThread().getName() + " received stop signal.");
                    break;
                }

                Student student = event.getStudent();
                Course course = event.getCourse();

                Logger.log(Thread.currentThread().getName()
                        + " consumed enrollment event for " + student.getName());

                DelayHelper.pause("Workflow 2", approvalDelayMs, randomDelay);

                ApprovalStatus status = approvalWorkflow.approveStudent(student, course);

                if(status == ApprovalStatus.APPROVED)
                {
                    DelayHelper.pause("Workflow 2 pre-billing-publish", config.getPreBillingPublishDelayMs(), config.isRandomDelay());
                    billingQueue.put(new ApprovalEvent(student, course, event.doesStudentPay()));
                    Logger.log(Thread.currentThread().getName()
                            + " published billing event for " + student.getName());
                }
                else
                {
                    Logger.log(Thread.currentThread().getName()
                            + " did not publish billing event for "
                            + student.getName() + " because status was " + status + ".");
                }
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}

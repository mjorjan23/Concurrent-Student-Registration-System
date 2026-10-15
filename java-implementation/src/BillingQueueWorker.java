import java.util.concurrent.BlockingQueue;

public class BillingQueueWorker implements Runnable
{
    private final BlockingQueue<ApprovalEvent> billingQueue;
    private final BillingWorkflow billingWorkflow;
    private final int billingDelayMs;
    private final boolean randomDelay;

    public BillingQueueWorker(BlockingQueue<ApprovalEvent> billingQueue,
                              BillingWorkflow billingWorkflow,
                              int billingDelayMs,
                              boolean randomDelay)
    {
        this.billingQueue = billingQueue;
        this.billingWorkflow = billingWorkflow;
        this.billingDelayMs = billingDelayMs;
        this.randomDelay = randomDelay;
    }

    @Override
    public void run()
    {
        try
        {
            while(true)
            {
                ApprovalEvent event = billingQueue.take();

                if(event == ApprovalEvent.POISON_PILL)
                {
                    Logger.log(Thread.currentThread().getName() + " received stop signal.");
                    break;
                }

                Student student = event.getStudent();
                Course course = event.getCourse();

                Logger.log(Thread.currentThread().getName()
                        + " consumed approval event for " + student.getName());

                DelayHelper.pause("Workflow 3", billingDelayMs, randomDelay);

                billingWorkflow.generateBillAndCheckPayment(student, course, event.doesStudentPay());
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}

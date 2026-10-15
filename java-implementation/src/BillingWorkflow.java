public class BillingWorkflow
{
    private BillingService billingService;
    private GradeHoldService gradeHoldService;
    private AppConfig config;

    public BillingWorkflow(BillingService billingService, GradeHoldService gradeHoldService, AppConfig config)
    {
        this.billingService = billingService;
        this.gradeHoldService = gradeHoldService;
        this.config = config;
    }

    public void generateBillAndCheckPayment(Student student, Course course, boolean studentPays)
    {
        Logger.log("BILLING WORKFLOW STARTED for " + student.getName());

        try
        {
            DelayHelper.pause("Workflow 3 pre-billing", config.getPreBillingDelayMs(), config.isRandomDelay());
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return;
        }

        if(!student.hasCourse(course))
        {
            Logger.log("No bill generated because " + student.getName()
                    + " is not enrolled in " + course.getCourseName() + ".");
            Logger.log("BILLING WORKFLOW ENDED for " + student.getName());
            return;
        }

        try
        {
            DelayHelper.pause("Workflow 3 inside billing step", config.getInsideBillingDelayMs(), config.isRandomDelay());
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return;
        }

        billingService.generateInvoice(student, course);

        if(studentPays)
        {
            billingService.processPayment(student, course);
            gradeHoldService.releaseHold(student);
        }
        else
        {
            Logger.log(student.getName() + " has not paid the invoice.");
            gradeHoldService.placeHold(student);
        }

        try
        {
            DelayHelper.pause("Workflow 3 post-billing", config.getPostBillingDelayMs(), config.isRandomDelay());
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return;
        }

        Logger.log("BILLING WORKFLOW ENDED for " + student.getName());
    }

    public BillingService getBillingService()
    {
        return billingService;
    }

    public GradeHoldService getGradeHoldService()
    {
        return gradeHoldService;
    }
}

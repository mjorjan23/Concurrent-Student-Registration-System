public class RegistrationCoordinator
{
    private final EnrollmentChecker checker;
    private final NotificationService notificationService;
    private final WaitList waitList;
    private final AppConfig config;

    public RegistrationCoordinator(WaitList waitList, AppConfig config)
    {
        checker = new EnrollmentChecker();
        notificationService = new NotificationService();
        this.waitList = waitList;
        this.config = config;
    }

    public RegistrationResult processRegistration(Student student, Course course)
    {
        Logger.log("WORKFLOW 1 STARTED for " + student.getName());

        try
        {
            DelayHelper.pause("Workflow 1 pre-validation", config.getPreValidationDelayMs(), config.isRandomDelay());
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return RegistrationResult.REJECTED;
        }

        // Stage 1: Validate student.
        if(!checker.validate(student))
        {
            Logger.log("VALIDATION FAILED for " + student.getName());
            notificationService.notifyStudent(student, course, RegistrationResult.REJECTED);
            Logger.log("WORKFLOW 1 ENDED for " + student.getName());
            return RegistrationResult.REJECTED;
        }

        Logger.log("VALIDATION PASSED for " + student.getName());

        try
        {
            DelayHelper.pause("Workflow 1 post-validation", config.getPostValidationDelayMs(), config.isRandomDelay());
            DelayHelper.pause("Workflow 1 pre-enrollment", config.getPreEnrollmentDelayMs(), config.isRandomDelay());
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return RegistrationResult.REJECTED;
        }

        // Stage 2: Course owns enrollment state and protects enrollment invariants.
        EnrollmentStatus enrollmentStatus = course.enrollStudent(
                student,
                config.getInsideEnrollmentDelayMs(),
                config.isRandomDelay());

        try
        {
            DelayHelper.pause("Workflow 1 post-enrollment", config.getPostEnrollmentDelayMs(), config.isRandomDelay());
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return RegistrationResult.REJECTED;
        }

        RegistrationResult result;

        if(enrollmentStatus == EnrollmentStatus.ENROLLED)
        {
            result = RegistrationResult.ENROLLED;
        }
        else if(enrollmentStatus == EnrollmentStatus.FULL)
        {
            // Stage 3: WaitList owns waitlist state separately from Course.
            boolean addedToWaitList = waitList.addStudent(student);

            if(addedToWaitList)
                result = RegistrationResult.WAITLISTED;
            else
                result = RegistrationResult.REJECTED;
        }
        else
        {
            result = RegistrationResult.REJECTED;
        }

        // Stage 4: Notify student.
        notificationService.notifyStudent(student, course, result);

        Logger.log("WORKFLOW 1 ENDED for " + student.getName());
        return result;
    }

    public int getWaitListCount()
    {
        return waitList.size();
    }

    public WaitList getWaitList()
    {
        return waitList;
    }
}

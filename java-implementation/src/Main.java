import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main
{
    public static void main(String[] args)
    {
        String configFile = "config/normal.properties";

        if(args.length > 0)
            configFile = args[0];

        AppConfig config = AppConfig.load(configFile);

        Teacher teacher = new Teacher(config.getTeacherId(), config.getTeacherName());
        Course course = new Course(config.getCourseId(), config.getCourseName(), teacher, config.getCapacity());

        WaitList waitList = new WaitList();
        RegistrationCoordinator coordinator = new RegistrationCoordinator(waitList, config);
        LicenseService licenseService = new LicenseService();
        ProfessorApprovalWorkflow approvalWorkflow = new ProfessorApprovalWorkflow(teacher, licenseService, config);
        BillingService billingService = new BillingService();
        GradeHoldService gradeHoldService = new GradeHoldService();
        BillingWorkflow billingWorkflow = new BillingWorkflow(billingService, gradeHoldService, config);

        BlockingQueue<EnrollmentEvent> approvalQueue = new LinkedBlockingQueue<EnrollmentEvent>();
        BlockingQueue<ApprovalEvent> billingQueue = new LinkedBlockingQueue<ApprovalEvent>();

        int approvalWorkerCount = config.getApprovalWorkers();
        int billingWorkerCount = config.getBillingWorkers();

        Thread[] approvalWorkerThreads = new Thread[approvalWorkerCount];
        Thread[] billingWorkerThreads = new Thread[billingWorkerCount];

        for(int i = 0; i < approvalWorkerCount; i++)
        {
            approvalWorkerThreads[i] = new Thread(
                    new ApprovalQueueWorker(approvalQueue, billingQueue, approvalWorkflow,
                            config.getApprovalDelayMs(), config.isRandomDelay(), config));
            approvalWorkerThreads[i].setName("Workflow-2-Approval-Queue-Worker-" + (i + 1));
            approvalWorkerThreads[i].start();
        }

        for(int i = 0; i < billingWorkerCount; i++)
        {
            billingWorkerThreads[i] = new Thread(
                    new BillingQueueWorker(billingQueue, billingWorkflow,
                            config.getBillingDelayMs(), config.isRandomDelay()));
            billingWorkerThreads[i].setName("Workflow-3-Billing-Queue-Worker-" + (i + 1));
            billingWorkerThreads[i].start();
        }

        ArrayList<StudentInput> students = StudentLoader.load(config.getStudentFile());

        Logger.log("==============================");
        Logger.log("WORKFLOW 1: CONCURRENT REGISTRATION");
        Logger.log("Successful registrations publish events to the approval queue.");
        Logger.log("==============================");

        int repeatStudentSubmissions = config.getRepeatStudentSubmissions();
        int totalRegistrationAttempts = students.size() * repeatStudentSubmissions;

        Logger.log("Student records loaded: " + students.size());
        Logger.log("Repeat submissions per student record: " + repeatStudentSubmissions);
        Logger.log("Total registration attempts: " + totalRegistrationAttempts);

        Thread[] registrationThreads = new Thread[totalRegistrationAttempts];

        for(int i = 0; i < totalRegistrationAttempts; i++)
        {
            StudentInput currentInput = students.get(i % students.size());
            Student currentStudent = currentInput.getStudent();
            boolean studentPays = currentInput.getPays();

            registrationThreads[i] = new Thread(() ->
            {
                try
                {
                    DelayHelper.pause("Workflow 1", config.getRegistrationDelayMs(), config.isRandomDelay());
                }
                catch(InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return;
                }

                RegistrationResult result = coordinator.processRegistration(currentStudent, course);

                if(result == RegistrationResult.ENROLLED)
                {
                    try
                    {
                        DelayHelper.pause("Workflow 1 pre-queue-publish", config.getPreQueuePublishDelayMs(), config.isRandomDelay());
                        approvalQueue.put(new EnrollmentEvent(currentStudent, course, studentPays));
                        Logger.log("Workflow 1 published approval event for " + currentStudent.getName());
                    }
                    catch(InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
                else
                {
                    Logger.log("Workflow 1 did not publish approval event for "
                            + currentStudent.getName() + " because result was " + result + ".");
                }
            });

            registrationThreads[i].setName("Registration-Thread-" + (i + 1));
        }

        startAndJoin(registrationThreads);

        try
        {
            for(int i = 0; i < approvalWorkerCount; i++)
                approvalQueue.put(EnrollmentEvent.POISON_PILL);

            for(Thread approvalWorkerThread : approvalWorkerThreads)
                approvalWorkerThread.join();

            for(int i = 0; i < billingWorkerCount; i++)
                billingQueue.put(ApprovalEvent.POISON_PILL);

            for(Thread billingWorkerThread : billingWorkerThreads)
                billingWorkerThread.join();
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        Logger.log("==============================");
        Logger.log("REGISTRATION SUMMARY");
        Logger.log("==============================");
        printRegistrationSummary(course, coordinator);

        Logger.log("==============================");
        Logger.log("WORKFLOW 2 SUMMARY: QUEUED PROFESSOR APPROVAL AND SOFTWARE LICENSE (" + approvalWorkerCount + " WORKERS)");
        Logger.log("==============================");
        printApprovalSummary(approvalWorkflow, licenseService);

        Logger.log("==============================");
        Logger.log("WORKFLOW 3 SUMMARY: QUEUED BILLING, PAYMENT, AND GRADE HOLD (" + billingWorkerCount + " WORKERS)");
        Logger.log("==============================");
        printBillingSummary(billingService, gradeHoldService);

        Logger.log("==============================");
        Logger.log("FINAL SYSTEM CHECK");
        Logger.log("==============================");

        if(course.getEnrollmentCount() > course.getCapacity())
            Logger.log("Warning: Capacity exceeded");
        else
            Logger.log("Capacity was not exceeded.");

        Logger.log("Normal A2 workflow completed using registration threads, queues, "
                + approvalWorkerCount + " approval worker(s), "
                + billingWorkerCount + " billing worker(s), and "
                + repeatStudentSubmissions + " repeat submission(s) per student record.");
    }

    private static void startAndJoin(Thread[] threads)
    {
        for(Thread t : threads)
            t.start();

        for(Thread t : threads)
        {
            try
            {
                t.join();
            }
            catch(InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void printRegistrationSummary(Course course, RegistrationCoordinator coordinator)
    {
        Logger.log("Class Information: ");
        Logger.log("Mode: SAFE WITH QUEUE COORDINATION");
        Logger.log("Teacher: " + course.getTeacher().getName());
        Logger.log("Class Name: " + course.getCourseName());
        Logger.log("Class Capacity: " + course.getCapacity());
        Logger.log("Actual Enrollment Count: " + course.getEnrollmentCount());
        Logger.log("Waitlist Count: " + coordinator.getWaitListCount());

        Logger.log("Roster:");
        for(Student s : course.getEnrolledStudents())
            Logger.log("  -" + s.getName() + " | ID: " + s.getStudentId() + " | GPA: " + s.getGpa());

        Logger.log("Waitlist:");
        for(Student s : coordinator.getWaitList().getStudents())
            Logger.log("  -" + s.getName() + " | ID: " + s.getStudentId() + " | GPA: " + s.getGpa());
    }

    private static void printApprovalSummary(ProfessorApprovalWorkflow approvalWorkflow,
                                             LicenseService licenseService)
    {
        Logger.log("Approved Students: " + approvalWorkflow.getApprovalCount());
        Logger.log("Software Licenses Issued: " + licenseService.getLicenseCount());

        for(SoftwareLicense license : licenseService.getIssuedLicenses())
        {
            Logger.log("  -" + license.getStudent().getName()
                    + " | License Key: " + license.getLicenseKey());
        }
    }

    private static void printBillingSummary(BillingService billingService,
                                            GradeHoldService gradeHoldService)
    {
        Logger.log("Invoices:");
        for(Invoice invoice : billingService.getInvoices())
        {
            Logger.log("  -" + invoice.getStudent().getName()
                    + " | Amount: $" + invoice.getAmount()
                    + " | Paid: " + invoice.isPaid()
                    + " | Grade Hold: " + gradeHoldService.hasHold(invoice.getStudent()));
        }

        Logger.log("Grade Hold Count: " + gradeHoldService.getHoldCount());
    }
}

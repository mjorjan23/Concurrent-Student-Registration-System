import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class CoordinationFailureDemo
{
    private static final double COURSE_FEE = 300.00;
    private static final double SOFTWARE_FEE = 75.00;

    public static void main(String[] args)
    {
        Logger.log("==============================");
        Logger.log("COORDINATION FAILURE DEMO");
        Logger.log("These unsafe demos are separate from the normal A2 workflow.");
        Logger.log("Artificial sleeps widen the race window so the failure is repeatable.");
        Logger.log("==============================");

        Teacher teacher = new Teacher(1, "Mr. Jan");
        Course course = new Course(101, "AP Computer Science", teacher, 3);
        Student leah = new Student(10000003, "Leah", 3.8);

        course.enrollStudent(leah);

        runUnsafeWorkflow2DuplicateLicenseDemo(teacher, course, leah);
        runUnsafeWorkflow3DuplicateInvoiceDemo(course, leah);
    }

    private static void runUnsafeWorkflow2DuplicateLicenseDemo(Teacher teacher, Course course, Student leah)
    {
        Logger.log("==============================");
        Logger.log("FAILURE DEMO 1: Workflow 2 Duplicate Completion");
        Logger.log("Expected: one approval and one license for Leah.");
        Logger.log("Unsafe behavior: two approval threads approve Leah at the same time.");
        Logger.log("==============================");

        UnsafeApprovalAndLicenseDemo unsafeApproval = new UnsafeApprovalAndLicenseDemo(teacher);

        Thread t1 = new Thread(() -> unsafeApproval.approveStudentUnsafely(leah, course));
        Thread t2 = new Thread(() -> unsafeApproval.approveStudentUnsafely(leah, course));

        t1.setName("Unsafe-Approval-Thread-1");
        t2.setName("Unsafe-Approval-Thread-2");

        startAndJoin(t1, t2);

        Logger.log("Expected approvals for Leah: 1");
        Logger.log("Actual unsafe approvals for Leah: " + unsafeApproval.getApprovalCount());
        Logger.log("Expected licenses for Leah: 1");
        Logger.log("Actual unsafe licenses for Leah: " + unsafeApproval.getLicenseCount());

        if(unsafeApproval.getLicenseCount() > 1)
            Logger.log("RESULT: COORDINATION FAILURE DEMONSTRATED - duplicate completion.");
        else
            Logger.log("RESULT: Failure did not reproduce on this run.");
    }

    private static void runUnsafeWorkflow3DuplicateInvoiceDemo(Course course, Student leah)
    {
        Logger.log("==============================");
        Logger.log("FAILURE DEMO 2: Workflow 3 Duplicate Invoice");
        Logger.log("Expected: one invoice for Leah in this course.");
        Logger.log("Unsafe behavior: two billing threads generate Leah's invoice at the same time.");
        Logger.log("==============================");

        UnsafeBillingDemo unsafeBilling = new UnsafeBillingDemo();

        Thread t1 = new Thread(() -> unsafeBilling.generateInvoiceUnsafely(leah, course));
        Thread t2 = new Thread(() -> unsafeBilling.generateInvoiceUnsafely(leah, course));

        t1.setName("Unsafe-Billing-Thread-1");
        t2.setName("Unsafe-Billing-Thread-2");

        startAndJoin(t1, t2);

        Logger.log("Expected invoices for Leah: 1");
        Logger.log("Actual unsafe invoices for Leah: " + unsafeBilling.getInvoiceCount());

        if(unsafeBilling.getInvoiceCount() > 1)
            Logger.log("RESULT: COORDINATION FAILURE DEMONSTRATED - duplicate invoice completion.");
        else
            Logger.log("RESULT: Failure did not reproduce on this run.");
    }

    private static void startAndJoin(Thread t1, Thread t2)
    {
        t1.start();
        t2.start();

        try
        {
            t1.join();
            t2.join();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static class UnsafeApprovalAndLicenseDemo
    {
        private final Teacher professor;
        private final ArrayList<Integer> approvedStudentIds;
        private final ArrayList<SoftwareLicense> issuedLicenses;
        private final AtomicInteger approvalCompletions;
        private final AtomicInteger licenseCompletions;

        public UnsafeApprovalAndLicenseDemo(Teacher professor)
        {
            this.professor = professor;
            approvedStudentIds = new ArrayList<Integer>();
            issuedLicenses = new ArrayList<SoftwareLicense>();
            approvalCompletions = new AtomicInteger(0);
            licenseCompletions = new AtomicInteger(0);
        }

        public void approveStudentUnsafely(Student student, Course course)
        {
            Logger.log(Thread.currentThread().getName() + " checking approval for " + student.getName());

            if(!hasApprovedStudent(student))
            {
                sleep(100);

                approvedStudentIds.add(student.getStudentId());
                approvalCompletions.incrementAndGet();
                Logger.log(professor.getName() + " approved " + student.getName() + " UNSAFELY.");

                issueLicenseUnsafely(student, course);
            }
            else
            {
                Logger.log(student.getName() + " was already approved.");
            }
        }

        private boolean hasApprovedStudent(Student student)
        {
            for(Integer id : approvedStudentIds)
            {
                if(id == student.getStudentId())
                    return true;
            }

            return false;
        }

        private void issueLicenseUnsafely(Student student, Course course)
        {
            Logger.log(Thread.currentThread().getName() + " checking license for " + student.getName());

            if(findLicense(student, course) == null)
            {
                sleep(100);

                String key = "UNSAFE-LIC-" + course.getCourseName().replace(" ", "-").toUpperCase()
                        + "-" + student.getStudentId()
                        + "-" + Thread.currentThread().getName();

                SoftwareLicense license = new SoftwareLicense(key, student, course);
                issuedLicenses.add(license);
                licenseCompletions.incrementAndGet();
                Logger.log("Unsafe software license issued to " + student.getName()
                        + ": " + license.getLicenseKey());
            }
        }

        private SoftwareLicense findLicense(Student student, Course course)
        {
            for(SoftwareLicense license : issuedLicenses)
            {
                if(license.getStudent().getStudentId() == student.getStudentId()
                        && license.getCourse().getCourseName().equals(course.getCourseName()))
                    return license;
            }

            return null;
        }

        public int getApprovalCount()
        {
            return approvalCompletions.get();
        }

        public int getLicenseCount()
        {
            return licenseCompletions.get();
        }
    }

    private static class UnsafeBillingDemo
    {
        private final ArrayList<Invoice> invoices;

        public UnsafeBillingDemo()
        {
            invoices = new ArrayList<Invoice>();
        }

        public void generateInvoiceUnsafely(Student student, Course course)
        {
            Logger.log(Thread.currentThread().getName() + " checking invoice for " + student.getName());

            if(findInvoice(student, course) == null)
            {
                sleep(100);

                Invoice invoice = new Invoice(student, course, COURSE_FEE + SOFTWARE_FEE);
                invoices.add(invoice);

                Logger.log("Unsafe invoice generated for " + student.getName()
                        + " in the amount of $" + invoice.getAmount() + ".");
            }
            else
            {
                Logger.log("Invoice already exists for " + student.getName() + ".");
            }
        }

        private Invoice findInvoice(Student student, Course course)
        {
            for(Invoice invoice : invoices)
            {
                if(invoice.getStudent().getStudentId() == student.getStudentId()
                        && invoice.getCourse().getCourseName().equals(course.getCourseName()))
                    return invoice;
            }

            return null;
        }

        public int getInvoiceCount()
        {
            return invoices.size();
        }
    }

    private static void sleep(int milliseconds)
    {
        try
        {
            Thread.sleep(milliseconds);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}

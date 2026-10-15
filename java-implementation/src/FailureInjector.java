import java.util.concurrent.atomic.AtomicInteger;

public class FailureInjector
{
    private final boolean failApproval;
    private final boolean failBilling;
    private final int failAfterEvents;
    private final String failStudent;
    private final AtomicInteger approvalEventsSeen = new AtomicInteger();
    private final AtomicInteger billingEventsSeen = new AtomicInteger();

    public FailureInjector(AppConfig config)
    {
        this.failApproval = config.isFailApproval();
        this.failBilling = config.isFailBilling();
        this.failAfterEvents = config.getFailAfterEvents();
        this.failStudent = config.getFailStudent();
    }

    public boolean isEnabled()
    {
        return failApproval || failBilling || failAfterEvents >= 0 || !failStudent.isEmpty();
    }

    public boolean shouldFailApproval(Student student)
    {
        int seen = approvalEventsSeen.incrementAndGet();
        return failApproval && matchesStudent(student) && reachedFailureThreshold(seen);
    }

    public boolean shouldFailBilling(Student student)
    {
        int seen = billingEventsSeen.incrementAndGet();
        return failBilling && matchesStudent(student) && reachedFailureThreshold(seen);
    }

    private boolean matchesStudent(Student student)
    {
        if(failStudent.isEmpty())
            return true;

        return failStudent.equalsIgnoreCase(student.getName()) ||
                failStudent.equals(String.valueOf(student.getStudentId()));
    }

    private boolean reachedFailureThreshold(int eventsSeen)
    {
        return failAfterEvents < 0 || eventsSeen > failAfterEvents;
    }

    public String describe()
    {
        return "FAIL_APPROVAL=" + failApproval +
                ", FAIL_BILLING=" + failBilling +
                ", failAfterEvents=" + failAfterEvents +
                ", FAIL_STUDENT=" + (failStudent.isEmpty() ? "<all>" : failStudent);
    }
}

public class AsyncApprovalEvent
{
    public static final AsyncApprovalEvent POISON_PILL = new AsyncApprovalEvent("STOP", "STOP", null, null, false, 0, 0);

    private final String operationId;
    private final String eventId;
    private final Student student;
    private final Course course;
    private final boolean studentPays;
    private final int sequence;
    private final int attempt;

    public AsyncApprovalEvent(String operationId, String eventId, Student student, Course course, boolean studentPays, int sequence, int attempt)
    {
        this.operationId = operationId;
        this.eventId = eventId;
        this.student = student;
        this.course = course;
        this.studentPays = studentPays;
        this.sequence = sequence;
        this.attempt = attempt;
    }

    public String getOperationId() { return operationId; }
    public String getEventId() { return eventId; }
    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public boolean doesStudentPay() { return studentPays; }
    public int getSequence() { return sequence; }
    public int getAttempt() { return attempt; }
    public String getIdempotencyKey() { return operationId + ":billing:" + sequence; }
}

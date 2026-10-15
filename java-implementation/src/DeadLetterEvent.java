public class DeadLetterEvent
{
    private final String operationId;
    private final String eventId;
    private final String stage;
    private final String studentName;
    private final int attempts;
    private final String reason;

    public DeadLetterEvent(String operationId, String eventId, String stage,
                           String studentName, int attempts, String reason)
    {
        this.operationId = operationId;
        this.eventId = eventId;
        this.stage = stage;
        this.studentName = studentName;
        this.attempts = attempts;
        this.reason = reason;
    }

    public String getOperationId() { return operationId; }
    public String getEventId() { return eventId; }
    public String getStage() { return stage; }
    public String getStudentName() { return studentName; }
    public int getAttempts() { return attempts; }
    public String getReason() { return reason; }
}

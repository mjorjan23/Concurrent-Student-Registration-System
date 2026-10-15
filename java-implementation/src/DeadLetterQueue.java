import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeadLetterQueue
{
    private final List<DeadLetterEvent> failedEvents =
            Collections.synchronizedList(new ArrayList<DeadLetterEvent>());

    public void add(DeadLetterEvent event)
    {
        failedEvents.add(event);
        Logger.log("[op=" + event.getOperationId() + "] DEAD LETTER: " +
                event.getStage() + " event moved to DeadLetterQueue after " +
                event.getAttempts() + " attempt(s). Reason: " + event.getReason());
    }

    public int size()
    {
        return failedEvents.size();
    }

    public List<DeadLetterEvent> getFailedEvents()
    {
        synchronized(failedEvents)
        {
            return new ArrayList<DeadLetterEvent>(failedEvents);
        }
    }

    public void printSummary()
    {
        Logger.log("========= DEAD LETTER QUEUE =========");
        Logger.log("Dead-lettered events: " + size());
        for(DeadLetterEvent event : getFailedEvents())
        {
            Logger.log("[op=" + event.getOperationId() + "][event=" + event.getEventId() +
                    "] stage=" + event.getStage() +
                    ", student=" + event.getStudentName() +
                    ", attempts=" + event.getAttempts() +
                    ", reason=" + event.getReason());
        }
    }
}

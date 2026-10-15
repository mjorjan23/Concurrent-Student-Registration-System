import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AsyncStatusTracker
{
    private final Map<String, AsyncOperationStatus> statuses;
    private final Set<String> processedKeys;
    private int duplicateEvents;
    private int outOfOrderEvents;
    private int retriedEvents;

    public AsyncStatusTracker()
    {
        statuses = new HashMap<String, AsyncOperationStatus>();
        processedKeys = new HashSet<String>();
    }

    /**
     * Records the latest observed workflow status. This is intentionally a
     * lightweight status tracker rather than a formal finite-state machine.
     */
    public synchronized boolean transitionTo(String operationId, AsyncOperationStatus nextStatus)
    {
        statuses.put(operationId, nextStatus);
        Logger.log("[op=" + operationId + "] STATUS -> " + nextStatus);
        return true;
    }

    public synchronized AsyncOperationStatus getStatus(String operationId)
    {
        return statuses.get(operationId);
    }

    public synchronized boolean shouldProcess(String idempotencyKey)
    {
        if(processedKeys.contains(idempotencyKey))
        {
            duplicateEvents++;
            Logger.log("ASYNC CONTROL: duplicate event skipped for idempotency key " + idempotencyKey);
            return false;
        }

        processedKeys.add(idempotencyKey);
        return true;
    }

    public synchronized void recordDuplicateFailure(String idempotencyKey)
    {
        duplicateEvents++;
        Logger.log("ASYNC FAILURE: duplicate event processed for idempotency key " + idempotencyKey);
    }

    public synchronized void recordOutOfOrder(String operationId, String message)
    {
        outOfOrderEvents++;
        Logger.log("[op=" + operationId + "] ASYNC FAILURE: " + message);
    }

    public synchronized void recordRetry(String operationId)
    {
        retriedEvents++;
        Logger.log("[op=" + operationId + "] retry scheduled.");
    }

    public synchronized int getDuplicateEvents() { return duplicateEvents; }
    public synchronized int getOutOfOrderEvents() { return outOfOrderEvents; }
    public synchronized int getRetriedEvents() { return retriedEvents; }
    public synchronized int getTrackedOperationCount() { return statuses.size(); }

    public synchronized int countOperationsInState(AsyncOperationStatus status)
    {
        int count = 0;
        for(AsyncOperationStatus current : statuses.values())
        {
            if(current == status)
                count++;
        }
        return count;
    }
}

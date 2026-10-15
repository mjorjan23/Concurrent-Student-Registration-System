import threading
from enums import AsyncOperationStatus

class AsyncStatusTracker:
    def __init__(self):
        self._statuses={}; self._processed=set(); self._lock=threading.RLock()
        self.duplicate_events=0; self.out_of_order_events=0; self.retried_events=0
    def transition_to(self, op, status):
        with self._lock:
            current=self._statuses.get(op)
            allowed=(current is None and status is AsyncOperationStatus.REGISTERED) or                     (current is AsyncOperationStatus.REGISTERED and status in (AsyncOperationStatus.APPROVED, AsyncOperationStatus.DEAD_LETTERED)) or                     (current is AsyncOperationStatus.APPROVED and status in (AsyncOperationStatus.BILLED, AsyncOperationStatus.DEAD_LETTERED)) or                     current is status
            if allowed: self._statuses[op]=status; return True
            return False
    def should_process(self, key):
        with self._lock:
            if key in self._processed: self.duplicate_events+=1; return False
            self._processed.add(key); return True
    def record_out_of_order(self, op, reason=""):
        with self._lock: self.out_of_order_events+=1
    def record_retry(self, op):
        with self._lock: self.retried_events+=1
    def record_duplicate_failure(self, key):
        with self._lock: self.duplicate_events+=1
    def count_status(self, status):
        with self._lock: return sum(v is status for v in self._statuses.values())
    def get_tracked_operation_count(self):
        with self._lock: return len(self._statuses)

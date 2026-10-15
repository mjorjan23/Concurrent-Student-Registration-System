import threading
from logger import Logger
from enums import AsyncOperationStatus
class MetricsCollector:
    def __init__(self): self.submitted=0; self.failed=0; self.retries=0; self.dead_letters=0; self.elapsed_ms=0; self._lock=threading.Lock()
    def _inc(self, name):
        with self._lock: setattr(self,name,getattr(self,name)+1)
    def record_submitted(self): self._inc("submitted")
    def record_failed(self): self._inc("failed")
    def record_retry(self): self._inc("retries")
    def record_dead_letter(self): self._inc("dead_letters")
    def print_final_report(self, tracker):
        completed=tracker.count_status(AsyncOperationStatus.BILLED)
        Logger.log("========= FINAL REPORT =========")
        Logger.log(f"Submitted: {self.submitted}"); Logger.log(f"Completed: {completed}"); Logger.log(f"Failed: {self.failed}")
        Logger.log(f"Retries: {self.retries}"); Logger.log(f"Dead Letters: {self.dead_letters}"); Logger.log(f"Runtime: {self.elapsed_ms} ms")

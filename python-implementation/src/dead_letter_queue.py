import threading
from logger import Logger
class DeadLetterQueue:
    def __init__(self): self._events=[]; self._lock=threading.Lock()
    def add(self, event):
        with self._lock: self._events.append(event)
        Logger.log(f"DEAD LETTER: op={event.operation_id} stage={event.stage} reason={event.reason}")
    def size(self):
        with self._lock: return len(self._events)
    def print_summary(self): Logger.log(f"Dead-letter count: {self.size()}")

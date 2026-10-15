import threading
class FailureInjector:
    def __init__(self, config): self.config=config; self._approval=0; self._billing=0; self._lock=threading.Lock()
    def is_enabled(self): return self.config.fail_approval or self.config.fail_billing
    def _target(self, student): return not self.config.fail_student or self.config.fail_student.lower() in student.name.lower()
    def should_fail_approval(self, student):
        if not self.config.fail_approval or not self._target(student): return False
        with self._lock: self._approval+=1; return self.config.fail_after_events < 0 or self._approval > self.config.fail_after_events
    def should_fail_billing(self, student):
        if not self.config.fail_billing or not self._target(student): return False
        with self._lock: self._billing+=1; return self.config.fail_after_events < 0 or self._billing > self.config.fail_after_events
    def describe(self): return f"approval={self.config.fail_approval}, billing={self.config.fail_billing}, student={self.config.fail_student or 'ANY'}"

import threading
class GradeHoldService:
    def __init__(self): self._holds=set(); self._lock=threading.Lock()
    def place_hold(self, student):
        with self._lock: self._holds.add(student.student_id)
    def release_hold(self, student):
        with self._lock: self._holds.discard(student.student_id)
    def get_hold_count(self):
        with self._lock: return len(self._holds)

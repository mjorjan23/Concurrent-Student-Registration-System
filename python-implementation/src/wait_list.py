import threading
from logger import Logger

class WaitList:
    def __init__(self):
        self._students = []
        self._lock = threading.Lock()
    def add_student(self, student):
        with self._lock:
            if any(s.student_id == student.student_id for s in self._students): return False
            self._students.append(student); Logger.log(f"{student.name} added to the waitlist."); return True
    def size(self):
        with self._lock: return len(self._students)

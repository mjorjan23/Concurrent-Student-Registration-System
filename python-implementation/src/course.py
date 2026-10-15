import threading
from delay_helper import DelayHelper
from enums import EnrollmentStatus
from logger import Logger

class Course:
    def __init__(self, course_id, course_name, teacher, capacity):
        self.course_id = course_id
        self.course_name = course_name
        self.teacher = teacher
        self.capacity = capacity
        self._students = []
        self._lock = threading.RLock()

    def enroll_student(self, student, inside_delay_ms=0, random_delay=False):
        with self._lock:
            Logger.log(f"{threading.current_thread().name} attempting to enroll {student.name}")
            DelayHelper.pause("Inside Course enrollment critical section", inside_delay_ms, random_delay)
            if any(s.student_id == student.student_id for s in self._students):
                return EnrollmentStatus.DUPLICATE
            if len(self._students) >= self.capacity:
                return EnrollmentStatus.FULL
            self._students.append(student)
            student.add_course(self)
            Logger.log(f"{student.name} enrolled successfully.")
            Logger.log(f"Enrollment Count: {len(self._students)}")
            return EnrollmentStatus.ENROLLED

    def get_enrollment_count(self):
        with self._lock: return len(self._students)

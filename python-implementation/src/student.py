import threading

class Student:
    def __init__(self, student_id: int, name: str, gpa: float):
        self.student_id = student_id
        self.name = name
        self.gpa = gpa
        self._courses = []
        self._lock = threading.Lock()

    def add_course(self, course) -> None:
        with self._lock:
            if course not in self._courses:
                self._courses.append(course)

    def has_course(self, course) -> bool:
        with self._lock:
            return course in self._courses

    def __str__(self):
        return f"{self.name} (ID: {self.student_id}, GPA: {self.gpa})"

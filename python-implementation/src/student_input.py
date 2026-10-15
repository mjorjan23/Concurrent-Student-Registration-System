from dataclasses import dataclass
from student import Student

@dataclass(frozen=True)
class StudentInput:
    student: Student
    pays: bool

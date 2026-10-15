import csv
from student import Student
from student_input import StudentInput

class StudentLoader:
    @staticmethod
    def load(path):
        result=[]
        with open(path, newline="", encoding="utf-8") as f:
            for row in csv.DictReader(f):
                result.append(StudentInput(Student(int(row["id"]), row["name"].strip(), float(row["gpa"])), row["pays"].strip().lower()=="true"))
        return result

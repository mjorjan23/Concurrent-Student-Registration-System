import threading
from software_license import SoftwareLicense

class LicenseService:
    def __init__(self): self._licenses=[]; self._lock=threading.Lock()
    def issue_license(self, student, course):
        with self._lock:
            for x in self._licenses:
                if x.student.student_id==student.student_id and x.course.course_name==course.course_name: return x
            lic=SoftwareLicense(f"LIC-{course.course_name.replace(' ','-').upper()}-{student.student_id}", student, course)
            self._licenses.append(lic); return lic
    def get_license_count(self):
        with self._lock: return len(self._licenses)

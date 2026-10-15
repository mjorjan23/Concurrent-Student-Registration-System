import threading
from delay_helper import DelayHelper
from enums import ApprovalStatus

class ProfessorApprovalWorkflow:
    def __init__(self, professor, license_service, config):
        self.professor, self.license_service, self.config = professor, license_service, config
        self._approved=set(); self._lock=threading.RLock()
    def approve_student(self, student, course):
        with self._lock:
            DelayHelper.pause("Workflow 2 pre-approval", self.config.pre_approval_delay_ms, self.config.random_delay)
            if not student.has_course(course): return ApprovalStatus.NOT_ENROLLED
            if student.student_id in self._approved: return ApprovalStatus.DUPLICATE
            DelayHelper.pause("Inside approval critical section", self.config.inside_approval_delay_ms, self.config.random_delay)
            self._approved.add(student.student_id)
            self.license_service.issue_license(student, course)
            DelayHelper.pause("Workflow 2 post-approval", self.config.post_approval_delay_ms, self.config.random_delay)
            return ApprovalStatus.APPROVED
    def get_approval_count(self):
        with self._lock: return len(self._approved)

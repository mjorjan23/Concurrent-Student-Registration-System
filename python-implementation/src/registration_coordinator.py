from delay_helper import DelayHelper
from enums import EnrollmentStatus, RegistrationResult

class RegistrationCoordinator:
    def __init__(self, wait_list, config): self.wait_list, self.config = wait_list, config
    def process_registration(self, student, course):
        DelayHelper.pause("Registration", self.config.registration_delay_ms, self.config.random_delay)
        status=course.enroll_student(student, self.config.inside_enrollment_delay_ms, self.config.random_delay)
        if status is EnrollmentStatus.ENROLLED: return RegistrationResult.ENROLLED
        if status is EnrollmentStatus.FULL:
            self.wait_list.add_student(student); return RegistrationResult.WAITLISTED
        return RegistrationResult.REJECTED

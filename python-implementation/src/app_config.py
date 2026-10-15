from dataclasses import dataclass
from pathlib import Path

@dataclass(frozen=True)
class AppConfig:
    teacher_id: int
    teacher_name: str
    course_id: int
    course_name: str
    capacity: int
    approval_workers: int
    billing_workers: int
    student_file: str
    registration_delay_ms: int = 0
    approval_delay_ms: int = 0
    billing_delay_ms: int = 0
    random_delay: bool = False
    repeat_student_submissions: int = 1
    async_mode: str = "safe"
    fail_approval: bool = False
    fail_billing: bool = False
    fail_after_events: int = -1
    fail_student: str = ""
    max_retries: int = 3
    concurrent_duplicate_race: bool = False
    pre_validation_delay_ms: int = 0
    post_validation_delay_ms: int = 0
    pre_enrollment_delay_ms: int = 0
    inside_enrollment_delay_ms: int = 0
    post_enrollment_delay_ms: int = 0
    pre_queue_publish_delay_ms: int = 0
    pre_approval_delay_ms: int = 0
    inside_approval_delay_ms: int = 0
    post_approval_delay_ms: int = 0
    pre_billing_publish_delay_ms: int = 0
    pre_billing_delay_ms: int = 0
    inside_billing_delay_ms: int = 0
    post_billing_delay_ms: int = 0

    @staticmethod
    def load(path: str) -> "AppConfig":
        props = {}
        for raw in Path(path).read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
        def s(k, default=None, legacy=None):
            value = props.get(k)
            if (value is None or value == "") and legacy:
                value = props.get(legacy)
            if value is None or value == "":
                if default is None: raise ValueError(f"Missing config value: {k}")
                return default
            return value
        def i(k, default=None, legacy=None): return int(s(k, default, legacy))
        def b(k, default=False, legacy=None): return s(k, str(default).lower(), legacy).lower() == "true"
        return AppConfig(
            i("teacherId"), s("teacherName"), i("courseId"), s("courseName"), i("capacity"),
            i("approvalWorkers"), i("billingWorkers"), s("studentFile"),
            i("registrationDelayMs", 0), i("approvalDelayMs", 0), i("billingDelayMs", 0),
            b("randomDelay", False), i("repeatStudentSubmissions", 1), s("async.mode", "safe"),
            b("failApproval", False, "FAIL_APPROVAL"), b("failBilling", False, "FAIL_BILLING"),
            i("failAfterEvents", -1, "TIMEOUT_AFTER"), s("failStudent", "", "FAIL_STUDENT"),
            i("maxRetries", 3, "MAX_RETRIES"), b("concurrentDuplicateRace", False),
            i("preValidationDelayMs", 0), i("postValidationDelayMs", 0), i("preEnrollmentDelayMs", 0),
            i("insideEnrollmentDelayMs", 0), i("postEnrollmentDelayMs", 0), i("preQueuePublishDelayMs", 0),
            i("preApprovalDelayMs", 0), i("insideApprovalDelayMs", 0), i("postApprovalDelayMs", 0),
            i("preBillingPublishDelayMs", 0), i("preBillingDelayMs", 0), i("insideBillingDelayMs", 0),
            i("postBillingDelayMs", 0))

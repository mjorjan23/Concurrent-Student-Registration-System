from dataclasses import dataclass
@dataclass(frozen=True)
class AsyncApprovalEvent:
    operation_id: str
    event_id: str
    student: object
    course: object
    student_pays: bool
    sequence: int
    attempt: int
    @property
    def idempotency_key(self): return f"{self.operation_id}:billing:{self.sequence}"
POISON_PILL=object()

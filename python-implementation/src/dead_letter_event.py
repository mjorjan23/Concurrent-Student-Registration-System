from dataclasses import dataclass
@dataclass(frozen=True)
class DeadLetterEvent:
    operation_id: str
    event_id: str
    stage: str
    student_name: str
    attempts: int
    reason: str

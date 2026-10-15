from enum import Enum, auto

class EnrollmentStatus(Enum):
    ENROLLED = auto()
    DUPLICATE = auto()
    FULL = auto()

class ApprovalStatus(Enum):
    APPROVED = auto()
    DUPLICATE = auto()
    NOT_ENROLLED = auto()

class RegistrationResult(Enum):
    ENROLLED = auto()
    WAITLISTED = auto()
    REJECTED = auto()

class AsyncOperationStatus(Enum):
    REGISTERED = auto()
    APPROVED = auto()
    BILLED = auto()
    DEAD_LETTERED = auto()

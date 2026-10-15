from dataclasses import dataclass
@dataclass(frozen=True)
class SoftwareLicense:
    license_key: str
    student: object
    course: object

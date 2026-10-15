from dataclasses import dataclass
@dataclass
class Invoice:
    student: object
    course: object
    amount: float
    paid: bool=False

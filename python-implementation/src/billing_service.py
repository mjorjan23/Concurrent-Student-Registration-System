import threading
from invoice import Invoice

class BillingService:
    def __init__(self): self._invoices=[]; self._lock=threading.RLock()
    def generate_invoice(self, student, course):
        with self._lock:
            for inv in self._invoices:
                if inv.student.student_id==student.student_id and inv.course.course_name==course.course_name: return inv
            inv=Invoice(student, course, 375.0); self._invoices.append(inv); return inv
    def process_payment(self, student, course):
        with self._lock:
            inv=self.generate_invoice(student, course); inv.paid=True; return True
    def get_invoice_count(self):
        with self._lock: return len(self._invoices)

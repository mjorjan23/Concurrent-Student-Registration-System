from delay_helper import DelayHelper
class BillingWorkflow:
    def __init__(self, billing_service, grade_hold_service, config): self.billing_service,self.grade_hold_service,self.config=billing_service,grade_hold_service,config
    def generate_bill_and_check_payment(self, student, course, student_pays):
        DelayHelper.pause("Workflow 3 pre-billing", self.config.pre_billing_delay_ms, self.config.random_delay)
        if not student.has_course(course): return False
        DelayHelper.pause("Workflow 3 inside billing step", self.config.inside_billing_delay_ms, self.config.random_delay)
        self.billing_service.generate_invoice(student, course)
        if student_pays:
            self.billing_service.process_payment(student, course); self.grade_hold_service.release_hold(student)
        else: self.grade_hold_service.place_hold(student)
        DelayHelper.pause("Workflow 3 post-billing", self.config.post_billing_delay_ms, self.config.random_delay)
        return True

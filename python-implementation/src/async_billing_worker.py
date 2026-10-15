from async_approval_event import AsyncApprovalEvent, POISON_PILL
from dead_letter_event import DeadLetterEvent
from delay_helper import DelayHelper
from enums import AsyncOperationStatus

class AsyncBillingWorker:
    def __init__(self,bq,workflow,tracker,injector,dlq,metrics,config,controls,pending): self.bq,self.workflow,self.tracker,self.injector,self.dlq,self.metrics,self.config,self.controls,self.pending=bq,workflow,tracker,injector,dlq,metrics,config,controls,pending
    def run(self):
        while True:
            event=self.bq.get()
            if event is POISON_PILL: break
            try: self.process(event)
            finally: self.pending.decrement(); self.bq.task_done()
    def process(self,event):
        key=event.idempotency_key + (f":attempt:{event.attempt}" if self.injector.is_enabled() else "")
        if self.controls and not self.tracker.should_process(key): return
        DelayHelper.pause("Async billing",self.config.billing_delay_ms,self.config.random_delay)
        if self.injector.should_fail_billing(event.student): self.handle_failure(event,"deterministic billing failure"); return
        if not self.tracker.transition_to(event.operation_id,AsyncOperationStatus.BILLED):
            self.tracker.record_out_of_order(event.operation_id)
            if not self.controls:
                self.workflow.generate_bill_and_check_payment(event.student,event.course,event.student_pays)
            return
        self.workflow.generate_bill_and_check_payment(event.student,event.course,event.student_pays)
    def handle_failure(self,event,reason):
        if event.attempt < self.config.max_retries:
            self.tracker.record_retry(event.operation_id); self.metrics.record_retry(); nxt=event.attempt+1; self.pending.increment()
            self.bq.put(AsyncApprovalEvent(event.operation_id,event.event_id+f"-retry{nxt}",event.student,event.course,event.student_pays,event.sequence,nxt))
        else:
            self.tracker.transition_to(event.operation_id,AsyncOperationStatus.DEAD_LETTERED); self.metrics.record_failed(); self.metrics.record_dead_letter(); self.dlq.add(DeadLetterEvent(event.operation_id,event.event_id,"BILLING",event.student.name,event.attempt+1,reason))

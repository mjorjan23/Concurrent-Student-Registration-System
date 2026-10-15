import threading
from async_approval_event import AsyncApprovalEvent
from async_enrollment_event import AsyncEnrollmentEvent, POISON_PILL
from dead_letter_event import DeadLetterEvent
from delay_helper import DelayHelper
from enums import ApprovalStatus, AsyncOperationStatus
from logger import Logger

class AsyncApprovalWorker:
    def __init__(self, approval_queue, billing_queue, workflow, tracker, injector, dlq, metrics, config, controls, transient, pending_a, pending_b):
        self.aq,self.bq,self.workflow,self.tracker,self.injector,self.dlq,self.metrics,self.config,self.controls,self.transient,self.pending_a,self.pending_b=approval_queue,billing_queue,workflow,tracker,injector,dlq,metrics,config,controls,transient,pending_a,pending_b
    def run(self):
        while True:
            event=self.aq.get()
            if event is POISON_PILL: break
            try: self.process(event)
            finally: self.pending_a.decrement(); self.aq.task_done()
    def process(self,event):
        key=event.idempotency_key + (f":attempt:{event.attempt}" if self.transient or self.injector.is_enabled() else "")
        if self.controls and not self.tracker.should_process(key): return
        if not self.controls and event.attempt>0: self.tracker.record_duplicate_failure(event.idempotency_key)
        DelayHelper.pause("Async approval", self.config.approval_delay_ms, self.config.random_delay)
        if (self.transient and event.attempt==0) or self.injector.should_fail_approval(event.student):
            self.handle_failure(event, "deterministic approval failure"); return
        status=self.workflow.approve_student(event.student,event.course)
        if status is ApprovalStatus.APPROVED or (not self.controls and status is ApprovalStatus.DUPLICATE):
            if not self.tracker.transition_to(event.operation_id, AsyncOperationStatus.APPROVED):
                self.tracker.record_out_of_order(event.operation_id); return
            self.pending_b.increment(); self.bq.put(AsyncApprovalEvent(event.operation_id,event.event_id+"-billing",event.student,event.course,event.student_pays,2,0))
        else: self.handle_failure(event, f"approval workflow returned {status.name}")
    def handle_failure(self,event,reason):
        if event.attempt < self.config.max_retries:
            self.tracker.record_retry(event.operation_id); self.metrics.record_retry(); nxt=event.attempt+1; self.pending_a.increment()
            self.aq.put(AsyncEnrollmentEvent(event.operation_id,event.event_id+f"-retry{nxt}",event.student,event.course,event.student_pays,event.sequence,nxt))
        else:
            self.tracker.transition_to(event.operation_id,AsyncOperationStatus.DEAD_LETTERED); self.metrics.record_failed(); self.metrics.record_dead_letter(); self.dlq.add(DeadLetterEvent(event.operation_id,event.event_id,"APPROVAL",event.student.name,event.attempt+1,reason))

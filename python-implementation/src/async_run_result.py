from dataclasses import dataclass
from enums import AsyncOperationStatus
@dataclass
class AsyncRunResult:
    metrics: object; tracker: object; course: object; approval_workflow: object; billing_service: object; grade_hold_service: object
    @property
    def submitted(self): return self.metrics.submitted
    @property
    def completed(self): return self.tracker.count_status(AsyncOperationStatus.BILLED)
    @property
    def failed(self): return self.metrics.failed
    @property
    def retries(self): return self.metrics.retries
    @property
    def dead_letters(self): return self.metrics.dead_letters
    @property
    def duplicate_events(self): return self.tracker.duplicate_events
    @property
    def tracked_operations(self): return self.tracker.get_tracked_operation_count()
    @property
    def billed_state_count(self): return self.tracker.count_status(AsyncOperationStatus.BILLED)
    @property
    def dead_lettered_state_count(self): return self.tracker.count_status(AsyncOperationStatus.DEAD_LETTERED)
    @property
    def approval_count(self): return self.approval_workflow.get_approval_count()
    @property
    def invoice_count(self): return self.billing_service.get_invoice_count()

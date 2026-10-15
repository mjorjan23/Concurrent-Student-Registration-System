import queue, sys, threading, time
from app_config import AppConfig
from async_approval_event import AsyncApprovalEvent, POISON_PILL as BILLING_STOP
from async_approval_worker import AsyncApprovalWorker
from async_billing_worker import AsyncBillingWorker
from async_enrollment_event import AsyncEnrollmentEvent, POISON_PILL as APPROVAL_STOP
from async_run_result import AsyncRunResult
from async_status_tracker import AsyncStatusTracker
from atomic_counter import AtomicCounter
from billing_service import BillingService
from billing_workflow import BillingWorkflow
from course import Course
from dead_letter_queue import DeadLetterQueue
from enums import AsyncOperationStatus, RegistrationResult
from failure_injector import FailureInjector
from grade_hold_service import GradeHoldService
from license_service import LicenseService
from logger import Logger
from metrics_collector import MetricsCollector
from professor_approval_workflow import ProfessorApprovalWorkflow
from registration_coordinator import RegistrationCoordinator
from student_loader import StudentLoader
from teacher import Teacher
from wait_list import WaitList

class AsyncMain:
 @staticmethod
 def run(args=None):
    args=args or []
    config=AppConfig.load(args[0] if args else "config/normal.properties")
    mode=args[1] if len(args)>1 else config.async_mode
    controls=mode.lower().startswith("safe")
    duplicate=mode.lower() in {"duplicate","unsafe_all","safe_duplicate"}
    out_of_order=mode.lower() in {"out_of_order","unsafe_all","safe_out_of_order"}
    transient=mode.lower()=="safe_retry"
    teacher=Teacher(config.teacher_id,config.teacher_name); course=Course(config.course_id,config.course_name,teacher,config.capacity)
    wait=WaitList(); coordinator=RegistrationCoordinator(wait,config); licenses=LicenseService(); approvals=ProfessorApprovalWorkflow(teacher,licenses,config)
    billing=BillingService(); holds=GradeHoldService(); billing_workflow=BillingWorkflow(billing,holds,config)
    tracker=AsyncStatusTracker(); injector=FailureInjector(config); dlq=DeadLetterQueue(); metrics=MetricsCollector()
    aq=queue.Queue(); bq=queue.Queue(); pending_a=AtomicCounter(); pending_b=AtomicCounter(); threads=[]
    for i in range(config.approval_workers):
        w=AsyncApprovalWorker(aq,bq,approvals,tracker,injector,dlq,metrics,config,controls,transient,pending_a,pending_b); t=threading.Thread(target=w.run,name=f"Async-Approval-Worker-{i+1}"); t.start(); threads.append(t)
    billing_threads=[]
    for i in range(config.billing_workers):
        w=AsyncBillingWorker(bq,billing_workflow,tracker,injector,dlq,metrics,config,controls,pending_b); t=threading.Thread(target=w.run,name=f"Async-Billing-Worker-{i+1}"); t.start(); billing_threads.append(t)
    start=time.monotonic(); op=0
    for item in StudentLoader.load(config.student_file):
        if coordinator.process_registration(item.student,course) is RegistrationResult.ENROLLED:
            op+=1; metrics.record_submitted(); opid=f"op-{op}-student-{item.student.student_id}"; tracker.transition_to(opid,AsyncOperationStatus.REGISTERED)
            event=AsyncEnrollmentEvent(opid,opid+"-approval-1",item.student,course,item.pays,1,0)
            if config.concurrent_duplicate_race:
                barrier=threading.Barrier(2)
                def publish(n): barrier.wait(); pending_a.increment(); aq.put(AsyncEnrollmentEvent(opid,event.event_id+f"-producer-{n}",event.student,event.course,event.student_pays,event.sequence,event.attempt))
                ps=[threading.Thread(target=publish,args=(n,)) for n in (1,2)]
                [p.start() for p in ps]; [p.join() for p in ps]
            else: pending_a.increment(); aq.put(event)
            if duplicate:
                pending_a.increment(); aq.put(AsyncEnrollmentEvent(opid,opid+"-approval-duplicate",item.student,course,item.pays,1,1))
            if out_of_order:
                pending_b.increment(); bq.put(AsyncApprovalEvent(opid,opid+"-billing-too-early",item.student,course,item.pays,0,0))
    while pending_a.get()>0 or pending_b.get()>0: time.sleep(.005)
    for _ in range(config.approval_workers): aq.put(APPROVAL_STOP)
    for t in threads: t.join()
    for _ in range(config.billing_workers): bq.put(BILLING_STOP)
    for t in billing_threads: t.join()
    metrics.elapsed_ms=int((time.monotonic()-start)*1000)
    Logger.log("=============================="); Logger.log("A4 PYTHON ASYNC FINAL STATUS SUMMARY"); Logger.log("==============================")
    Logger.log(f"Tracked operations: {tracker.get_tracked_operation_count()}"); Logger.log(f"Enrollment count: {course.get_enrollment_count()}")
    Logger.log(f"Approval count: {approvals.get_approval_count()}"); Logger.log(f"License count: {licenses.get_license_count()}"); Logger.log(f"Invoice count: {billing.get_invoice_count()}")
    Logger.log(f"Grade hold count: {holds.get_hold_count()}"); Logger.log(f"Duplicate events detected: {tracker.duplicate_events}"); Logger.log(f"Out-of-order events detected: {tracker.out_of_order_events}")
    Logger.log(f"Retried events: {tracker.retried_events}"); Logger.log(f"Elapsed ms: {metrics.elapsed_ms}"); dlq.print_summary(); metrics.print_final_report(tracker)
    return AsyncRunResult(metrics,tracker,course,approvals,billing,holds)

if __name__=="__main__": AsyncMain.run(sys.argv[1:])

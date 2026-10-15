from async_main import AsyncMain
from logger import Logger

def require(condition,message):
    if not condition: raise AssertionError(message)

tests=[
("SUCCESS TEST","config/a4_success.properties",lambda r:(require(r.submitted==3,"submitted"),require(r.completed==3,"completed"),require(r.failed==0,"failed"),require(r.retries==0,"retries"),require(r.dead_letters==0,"dead letters"))),
("CONCURRENT TEST","config/a4_concurrent.properties",lambda r:(require(r.submitted==250,"submitted"),require(r.completed==250,"completed"),require(r.failed==0,"failed"),require(r.tracked_operations==250,"tracked"),require(r.billed_state_count==250,"billed"))),
("ASYNC DUPLICATE-CONTROL TEST","config/a4_duplicate_control.properties",lambda r:(require(r.submitted==3,"submitted"),require(r.completed==3,"completed"),require(r.duplicate_events==3,"duplicates"),require(r.failed==0,"failed"))),
("CONCURRENT DUPLICATE RACE TEST","config/a4_concurrent_duplicate_race.properties",lambda r:(require(r.submitted==3,"submitted"),require(r.completed==3,"completed"),require(r.duplicate_events==3,"duplicates"),require(r.approval_count==3,"approvals"),require(r.invoice_count==3,"invoices"))),
("FAILURE / DEAD-LETTER TEST","config/a4_dead_letter.properties",lambda r:(require(r.submitted==3,"submitted"),require(r.completed==2,"completed"),require(r.failed==1,"failed"),require(r.retries==3,"retries"),require(r.dead_letters==1,"dead letters"),require(r.dead_lettered_state_count==1,"state"))),
("RECOVERY TEST","config/a4_recovery.properties",lambda r:(require(r.submitted==3,"submitted"),require(r.completed==3,"completed"),require(r.failed==0,"failed"),require(r.retries==3,"retries"),require(r.dead_letters==0,"dead letters"))),]

if __name__=="__main__":
    Logger.log("A4 PYTHON VALIDATION RUNNER"); passed=0
    for name,config,validator in tests:
        try: validator(AsyncMain.run([config])); passed+=1; Logger.log(f"VALIDATION RESULT: PASS - {name}")
        except Exception as e: Logger.log(f"VALIDATION RESULT: FAIL - {name} - {e}")
    Logger.log(f"VALIDATION SUMMARY: {passed} of {len(tests)} tests passed.")
    if passed != len(tests): raise SystemExit(1)

#!/bin/bash

# Runs the complete A4 Java test suite from one entry point.
#
# Phase 1: assertion-based A4 validation scenarios (including the natural race test)
# Phase 2: all configuration-driven concurrency and stress scenarios
#
# Usage:
#   ./run_all_tests.sh       # 5 runs per configuration-driven test (recommended default)
#   ./run_all_tests.sh 10    # 10 runs per configuration-driven test
#   ./run_all_tests.sh 1     # 1 run per configuration-driven test

set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

REPEATS="${1:-5}"
case "$REPEATS" in
  ''|*[!0-9]*)
    echo "Repeat count must be a positive integer."
    exit 1
    ;;
esac
if [ "$REPEATS" -lt 1 ]; then
  echo "Repeat count must be at least 1."
  exit 1
fi

BUILD_DIR="out/repeated_tests"
RUN_STAMP="$(date +%Y%m%d_%H%M%S)"
RESULT_DIR="masslogs/run_$RUN_STAMP"
DETAIL_CSV="$RESULT_DIR/per_run_summary.csv"
AGGREGATE_CSV="$RESULT_DIR/aggregate_summary.csv"
SUMMARY_TXT="$RESULT_DIR/summary.txt"

mkdir -p "$BUILD_DIR" "$RESULT_DIR"

printf '%s\n' \
  "====================================" \
  "A4 COMPLETE TEST SUITE" \
  "Compiling once for all tests" \
  "===================================="

if ! javac -d "$BUILD_DIR" src/*.java; then
  echo "Compilation failed. No tests were run."
  exit 1
fi

echo "Compilation successful."
echo
printf '%s\n' \
  "====================================" \
  "Phase 1: Assertion-Based Validation" \
  "===================================="

VALIDATION_LOG="$RESULT_DIR/validation.log"
if java -cp "$BUILD_DIR" ValidationRunner | tee "$VALIDATION_LOG"; then
  if ! grep -q "VALIDATION SUMMARY: 6 of 6 tests passed." "$VALIDATION_LOG"; then
    echo "ValidationRunner completed, but the expected 6-of-6 pass summary was not found."
    exit 1
  fi
else
  echo "Assertion-based validation failed."
  exit 1
fi

echo
echo "Assertion-based validation passed."
echo "Validation log: $VALIDATION_LOG"
echo
printf '%s\n' \
  "====================================" \
  "Phase 2: Configuration and Stress Tests" \
  "===================================="
echo "Runs per stress/configuration test: $REPEATS"
echo "Assertion-based validation runs once before the repeated stress suite."
echo "Results folder: $RESULT_DIR"
echo

# Test definition format:
# number|short_name|display_name|config|mode|timeout_seconds
TESTS=$(cat <<'TEST_LIST'
01|safe_duplicate|Safe Duplicate|config/01_safe_duplicate_status_overwrite.properties|safe_duplicate|120
02|safe_out_of_order_250|Safe Out-of-Order 250|config/02_safe_out_of_order_race_250.properties|safe_out_of_order|120
03|safe_out_of_order_1000|Safe Out-of-Order 1000|config/03_safe_out_of_order_race_1000.properties|safe_out_of_order|180
04|duplicate_storm|Safe Duplicate Storm|config/04_safe_duplicate_storm.properties|safe_duplicate|120
05|unsafe_stress|Unsafe Combined Stress|config/05_unsafe_all_stress.properties|unsafe_all|180
06|out_of_order_execution|Out-of-Order Execution|config/06_out_of_order_execution.properties|unsafe_all|180
07|duplicate_churn|Livelock-Like Duplicate Churn|config/07_livelock_like_duplicate_churn.properties|safe_duplicate|180
08|missed_signal|Missed-Signal Simulation|config/08_missed_signal_simulation.properties|safe|120
09|lock_stall|Deadlock-Like Lock Stall|config/09_deadlock_like_lock_stall.properties|safe|8
10|duplicate_completion|Duplicate Completion|config/10_duplicate_completion.properties|duplicate|120
11|premature_finalization|Premature Finalization|config/11_premature_finalization.properties|safe|120
12|transient_retry|Transient Failure Retry|config/12_transient_failure_retry.properties|safe_retry|120
13|concurrent_duplicate_race|Concurrent Duplicate Producer Race|config/a4_concurrent_duplicate_race.properties|safe|120
TEST_LIST
)

# CSV header. One row is written for every test run.
echo 'test_number,test_key,test_name,run,status,exit_code,tracked_operations,enrollment_count,approval_count,license_count,invoice_count,grade_hold_count,duplicate_events,out_of_order_events,retried_events,elapsed_ms,async_failures,duplicate_failure_markers,out_of_order_failure_markers,timeout_marker,log_file' > "$DETAIL_CSV"

csv_escape() {
  # Escape a value for CSV output.
  printf '"%s"' "$(printf '%s' "$1" | sed 's/"/""/g')"
}

extract_metric() {
  label="$1"
  logfile="$2"
  value=$(grep -F "$label" "$logfile" 2>/dev/null | tail -n 1 | sed -E "s/.*${label}[[:space:]]*//" || true)
  if [ -z "$value" ]; then
    printf ''
  else
    printf '%s' "$value" | tr -d '\r'
  fi
}

count_marker() {
  marker="$1"
  logfile="$2"
  count=$(grep -F -c "$marker" "$logfile" 2>/dev/null || true)
  printf '%s' "${count:-0}"
}

run_with_timeout() {
  timeout_seconds="$1"
  logfile="$2"
  config="$3"
  mode="$4"

  java -cp "$BUILD_DIR" AsyncMain "$config" "$mode" > "$logfile" 2>&1 &
  pid=$!
  waited=0

  while kill -0 "$pid" 2>/dev/null; do
    if [ "$waited" -ge "$timeout_seconds" ]; then
      echo "TEST HARNESS: Process was still running after ${timeout_seconds} seconds; terminating this run." >> "$logfile"
      kill "$pid" 2>/dev/null || true
      sleep 1
      kill -9 "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
      return 124
    fi
    sleep 1
    waited=$((waited + 1))
  done

  wait "$pid"
  return $?
}

TOTAL_TESTS=13
CURRENT_TEST=0

while IFS='|' read -r number key name config mode timeout_seconds; do
  [ -z "$number" ] && continue
  CURRENT_TEST=$((CURRENT_TEST + 1))

  test_dir="$RESULT_DIR/${number}_${key}"
  mkdir -p "$test_dir"

  printf '%s\n' \
    "====================================" \
    "Test $CURRENT_TEST/$TOTAL_TESTS: $name" \
    "Config: $config" \
    "Mode: $mode" \
    "===================================="

  if [ ! -f "$config" ]; then
    echo "WARNING: Missing config: $config"
  fi

  run=1
  while [ "$run" -le "$REPEATS" ]; do
    logfile="$test_dir/run_$(printf '%02d' "$run").log"
    echo "  Run $run/$REPEATS..."

    if [ ! -f "$config" ]; then
      status="missing_config"
      exit_code=2
      echo "TEST HARNESS: Missing configuration file $config" > "$logfile"
    else
      run_with_timeout "$timeout_seconds" "$logfile" "$config" "$mode"
      exit_code=$?
      if [ "$exit_code" -eq 0 ]; then
        status="completed"
      elif [ "$exit_code" -eq 124 ]; then
        status="timeout"
      else
        status="exit_$exit_code"
      fi
    fi

    tracked=$(extract_metric 'Tracked operations:' "$logfile")
    enrollment=$(extract_metric 'Enrollment count:' "$logfile")
    approval=$(extract_metric 'Approval count:' "$logfile")
    license=$(extract_metric 'License count:' "$logfile")
    invoice=$(extract_metric 'Invoice count:' "$logfile")
    grade_hold=$(extract_metric 'Grade hold count:' "$logfile")
    duplicates=$(extract_metric 'Duplicate events detected:' "$logfile")
    out_of_order=$(extract_metric 'Out-of-order events detected:' "$logfile")
    retried=$(extract_metric 'Retried events:' "$logfile")
    elapsed=$(extract_metric 'Elapsed ms:' "$logfile")

    async_failures=$(count_marker 'ASYNC FAILURE:' "$logfile")
    duplicate_failures=$(count_marker 'ASYNC FAILURE: duplicate event processed' "$logfile")
    ordering_failures=$(count_marker 'ASYNC FAILURE: billing processed before approval guard was checked' "$logfile")
    timeout_marker=$(count_marker 'TEST HARNESS: Process was still running' "$logfile")

    {
      printf '%s,%s,' "$number" "$key"
      csv_escape "$name"
      printf ',%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,' \
        "$run" "$status" "$exit_code" "${tracked:-}" "${enrollment:-}" \
        "${approval:-}" "${license:-}" "${invoice:-}" "${grade_hold:-}" \
        "${duplicates:-}" "${out_of_order:-}" "${retried:-}" "${elapsed:-}" \
        "$async_failures" "$duplicate_failures" "$ordering_failures" "$timeout_marker"
      csv_escape "$logfile"
      printf '\n'
    } >> "$DETAIL_CSV"

    echo "    Status: $status | enrollment=${enrollment:-NA} approval=${approval:-NA} invoice=${invoice:-NA} async_failures=$async_failures"
    run=$((run + 1))
  done
  echo
done <<EOF_TESTS
$TESTS
EOF_TESTS

# Build aggregate CSV and readable report with Python's standard library.
# The per-run CSV remains available even if Python 3 is unavailable.
if command -v python3 >/dev/null 2>&1; then
  python3 - "$DETAIL_CSV" "$AGGREGATE_CSV" "$SUMMARY_TXT" "$REPEATS" <<'PY'
import csv
import statistics
import sys
from collections import defaultdict
from pathlib import Path

input_path, aggregate_path, text_path, repeats = sys.argv[1:]
repeats = int(repeats)

numeric_fields = [
    "tracked_operations", "enrollment_count", "approval_count", "license_count",
    "invoice_count", "grade_hold_count", "duplicate_events",
    "out_of_order_events", "retried_events", "elapsed_ms", "async_failures",
    "duplicate_failure_markers", "out_of_order_failure_markers", "timeout_marker",
]

with open(input_path, newline="", encoding="utf-8") as f:
    rows = list(csv.DictReader(f))

groups = defaultdict(list)
for row in rows:
    groups[(row["test_number"], row["test_key"], row["test_name"])].append(row)

def numbers(group, field):
    result = []
    for row in group:
        value = (row.get(field) or "").strip()
        if value:
            try:
                result.append(float(value))
            except ValueError:
                pass
    return result

def fmt(value):
    if value is None:
        return ""
    if abs(value - round(value)) < 1e-9:
        return str(int(round(value)))
    return f"{value:.2f}"

aggregate_fields = [
    "test_number", "test_key", "test_name", "runs", "completed", "timeouts",
    "other_failures", "runs_with_async_failures", "runs_with_duplicate_failures",
    "runs_with_out_of_order_failures", "avg_enrollment", "min_enrollment",
    "max_enrollment", "avg_approval", "avg_invoice", "avg_duplicates",
    "avg_out_of_order", "avg_elapsed_ms", "min_elapsed_ms", "max_elapsed_ms",
]

aggregate_rows = []
for identity in sorted(groups):
    group = groups[identity]
    statuses = [r["status"] for r in group]
    async_counts = numbers(group, "async_failures")
    duplicate_counts = numbers(group, "duplicate_failure_markers")
    ordering_counts = numbers(group, "out_of_order_failure_markers")

    def avg(field):
        vals = numbers(group, field)
        return statistics.mean(vals) if vals else None

    def minimum(field):
        vals = numbers(group, field)
        return min(vals) if vals else None

    def maximum(field):
        vals = numbers(group, field)
        return max(vals) if vals else None

    aggregate_rows.append({
        "test_number": identity[0],
        "test_key": identity[1],
        "test_name": identity[2],
        "runs": len(group),
        "completed": statuses.count("completed"),
        "timeouts": statuses.count("timeout"),
        "other_failures": sum(1 for s in statuses if s not in {"completed", "timeout"}),
        "runs_with_async_failures": sum(1 for x in async_counts if x > 0),
        "runs_with_duplicate_failures": sum(1 for x in duplicate_counts if x > 0),
        "runs_with_out_of_order_failures": sum(1 for x in ordering_counts if x > 0),
        "avg_enrollment": fmt(avg("enrollment_count")),
        "min_enrollment": fmt(minimum("enrollment_count")),
        "max_enrollment": fmt(maximum("enrollment_count")),
        "avg_approval": fmt(avg("approval_count")),
        "avg_invoice": fmt(avg("invoice_count")),
        "avg_duplicates": fmt(avg("duplicate_events")),
        "avg_out_of_order": fmt(avg("out_of_order_events")),
        "avg_elapsed_ms": fmt(avg("elapsed_ms")),
        "min_elapsed_ms": fmt(minimum("elapsed_ms")),
        "max_elapsed_ms": fmt(maximum("elapsed_ms")),
    })

with open(aggregate_path, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=aggregate_fields)
    writer.writeheader()
    writer.writerows(aggregate_rows)

lines = []
lines.append("A4 COMPLETE TEST SUMMARY")
lines.append("=" * 80)
lines.append("Assertion-based validation repetitions: 1")
lines.append(f"Configuration/stress repetitions per test: {repeats}")
lines.append(f"Total executions: {len(rows)}")
lines.append("")

for item in aggregate_rows:
    lines.append(f"{item['test_number']}. {item['test_name']}")
    lines.append(
        f"   Runs: {item['runs']} | Completed: {item['completed']} | "
        f"Timeouts: {item['timeouts']} | Other failures: {item['other_failures']}"
    )
    lines.append(
        f"   Async failure observed in {item['runs_with_async_failures']}/{item['runs']} runs; "
        f"duplicate failure in {item['runs_with_duplicate_failures']}/{item['runs']}; "
        f"out-of-order failure in {item['runs_with_out_of_order_failures']}/{item['runs']}"
    )
    lines.append(
        f"   Average counts: enrollment={item['avg_enrollment'] or 'NA'}, "
        f"approval={item['avg_approval'] or 'NA'}, invoice={item['avg_invoice'] or 'NA'}, "
        f"duplicates={item['avg_duplicates'] or 'NA'}, "
        f"out-of-order={item['avg_out_of_order'] or 'NA'}"
    )
    lines.append(
        f"   Elapsed ms: avg={item['avg_elapsed_ms'] or 'NA'}, "
        f"min={item['min_elapsed_ms'] or 'NA'}, max={item['max_elapsed_ms'] or 'NA'}"
    )
    lines.append("")

lines.append("INTERPRETATION GUIDE")
lines.append("- A timeout in Test 09 is expected evidence of the lock-stall demonstration.")
lines.append("- Async failures in unsafe tests are expected evidence of broken coordination.")
lines.append("- Safe tests should complete without ASYNC FAILURE markers.")
lines.append("- Enrollment/approval/invoice mismatches can indicate abandoned or premature work.")
lines.append("- Variation between repeated runs is evidence of timing-dependent concurrency behavior.")

Path(text_path).write_text("\n".join(lines) + "\n", encoding="utf-8")
print("\n".join(lines))
PY
else
  echo "Python 3 was not found; per-run CSV was created, but aggregate reports were skipped." | tee "$SUMMARY_TXT"
fi

printf '%s\n' \
  "====================================" \
  "All A4 tests complete" \
  "====================================" \
  "Validation log:  $VALIDATION_LOG" \
  "Individual logs: $RESULT_DIR/<test>/run_XX.log" \
  "Per-run CSV:      $DETAIL_CSV" \
  "Aggregate CSV:    $AGGREGATE_CSV" \
  "Readable summary: $SUMMARY_TXT"
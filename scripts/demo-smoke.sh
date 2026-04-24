#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
RUN_TIMEOUT_SECONDS="${RUN_TIMEOUT_SECONDS:-40}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 1
fi

wait_for_run() {
  local run_id="$1"
  local deadline=$((SECONDS + RUN_TIMEOUT_SECONDS))
  local summary
  while [ "$SECONDS" -lt "$deadline" ]; do
    summary="$(curl -fsS "${API_BASE_URL}/api/runs/${run_id}")"
    local run_status
    run_status="$(printf '%s' "$summary" | jq -r '.status')"
    if [ "$run_status" = "COMPLETED" ] || [ "$run_status" = "FAILED" ]; then
      printf '%s\n' "$summary"
      return 0
    fi
    sleep 1
  done

  echo "Run ${run_id} did not finish within ${RUN_TIMEOUT_SECONDS}s" >&2
  return 1
}

run_experiment() {
  local strategy="$1"
  local name="$2"
  local payload
  payload="$(jq -n \
    --arg name "$name" \
    --arg strategy "$strategy" \
    '{
      name: $name,
      scenarioType: "CONCERT_BOOKING",
      strategyType: $strategy,
      concurrentUsers: 80,
      totalRequests: 120,
      targetSeatCount: 1,
      hotspotMode: true,
      artificialDelayMs: 40
    }')"

  local experiment_id
  experiment_id="$(curl -fsS -H 'Content-Type: application/json' -d "$payload" "${API_BASE_URL}/api/experiments" | jq -r '.id')"

  local run_id
  run_id="$(curl -fsS -X POST "${API_BASE_URL}/api/experiments/${experiment_id}/runs" | jq -r '.runId')"

  wait_for_run "$run_id"
}

echo "TrafficLab smoke demo against ${API_BASE_URL}"

unsafe_summary="$(run_experiment "UNSAFE" "Smoke demo - Unsafe hotspot")"
redis_summary="$(run_experiment "REDIS_LOCK" "Smoke demo - Redis hotspot")"

unsafe_duplicates="$(printf '%s' "$unsafe_summary" | jq -r '.duplicateReservationCount')"
redis_duplicates="$(printf '%s' "$redis_summary" | jq -r '.duplicateReservationCount')"

echo
echo "Unsafe result"
printf '%s\n' "$unsafe_summary" | jq '{id,status,totalRequests,successCount,failureCount,duplicateReservationCount,invariantViolationCount,p95LatencyMs,throughput}'

echo
echo "Redis lock result"
printf '%s\n' "$redis_summary" | jq '{id,status,totalRequests,successCount,failureCount,duplicateReservationCount,invariantViolationCount,p95LatencyMs,throughput}'

if [ "$unsafe_duplicates" -le 0 ]; then
  echo "Expected UNSAFE to produce duplicate reservations, got ${unsafe_duplicates}" >&2
  exit 1
fi

if [ "$redis_duplicates" -ne 0 ]; then
  echo "Expected REDIS_LOCK duplicateReservationCount to be 0, got ${redis_duplicates}" >&2
  exit 1
fi

echo
echo "Smoke demo passed: unsafe duplicated reservations, Redis lock preserved the invariant."

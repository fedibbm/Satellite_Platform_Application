# Workflow Data Optimization Report
## Buffered Writes for Workflow Logs/Events (Batch Flush by Size/Time)

Date: 2026-04-10

---

## 1) Objective

Implement a **low-risk data optimization** for workflow execution persistence by introducing:

- **Buffered writes** for workflow execution logs/events
- **Batch flush by size and by time**

Primary goal:

- Reduce database write pressure caused by per-node save calls during workflow execution
- Preserve execution correctness and final-state durability

---

## 2) Baseline Before Optimization

Previously, workflow execution persistence behavior included:

1. Append logs during node execution
2. Persist `WorkflowExecution` frequently (including per-node save path)
3. Persist final state at completion/failure

Impact under heavy workflows:

- High write amplification (many saves per workflow run)
- Increased MongoDB load during parallel execution bursts
- Lower throughput under load

---

## 3) Implemented Optimization

### 3.1 Strategy

Implemented an in-memory buffering layer in workflow execution service:

- Keep appending logs/events in memory on the execution object
- Track pending buffered events per execution
- Flush to database when:
	- **Size threshold reached** (batch by count)
	- **Time threshold reached** (periodic flush)
- Force flush on terminal states (success/failure)

### 3.2 Flush Policy

- `LOG_BUFFER_FLUSH_SIZE = 10`
- `LOG_BUFFER_FLUSH_INTERVAL_MS = 2000`

Meaning:

- Save when 10 buffered events accumulate, or
- Save every ~2 seconds if events are pending

### 3.3 Safety Rules

- Force flush before marking execution terminal state persisted
- Remove execution from buffer tracking on completion/failure
- Periodic scheduler flushes all pending buffered executions

---

## 4) Code-Level Changes

### 4.1 Workflow execution write buffering

Updated file:

- `Backend/src/main/java/com/enit/satellite_platform/modules/workflow/services/WorkflowExecutionService.java`

Key additions:

- `BufferedExecutionState` internal state holder
- `bufferedExecutions` concurrent map (executionId -> state)
- `@Scheduled` periodic flush method
- Helper methods:
	- `registerBufferedExecution(...)`
	- `unregisterBufferedExecution(...)`
	- `markBufferedEvent(...)`
	- `forceFlushExecution(...)`

Behavioral changes:

- Removed per-node immediate save path
- Log append now marks buffered event
- Final/failure paths force flush then unregister

### 4.2 No schema migration required

- No DB schema changes
- No API contract changes
- No frontend changes required for this optimization

---

## 5) Performance Feasibility Assessment

### 5.1 Feasibility

**High feasibility** for this codebase because:

- Workflow execution already centralized in one service
- Logs/events naturally accumulate during run
- Terminal state already has explicit finalize logic

### 5.2 Expected Gains

For medium/large workflows, expected improvements:

- Lower write operation count
- Better throughput under concurrent workflow runs
- Lower tail latency during execution spikes

### 5.3 Trade-offs

- Slight delay before intermediate logs are persisted (up to flush interval)
- If process crashes before next flush, very recent buffered logs may be lost
	- Mitigated by frequent interval flush + terminal force flush

---

## 6) Risk Analysis

### Low-risk characteristics

- Final state durability preserved through forced flush
- Existing execution orchestration remains unchanged
- No changes to node execution semantics

### Residual risks

1. **Crash window data loss** for last unflushed log events
2. **In-memory buffer growth** if extremely high execution concurrency

Mitigations:

- Small flush interval
- Size-triggered flush
- Unregister on terminal states

---

## 7) Verification Plan

### Functional checks

1. Execute a small workflow and confirm logs still appear in execution output.
2. Execute a workflow that fails on purpose; verify:
	 - status = FAILED
	 - failure log persists
3. Execute a larger workflow with many nodes; verify all final logs persisted.

### Load checks

1. Run N concurrent workflows (e.g., 20+)
2. Compare:
	 - DB write ops/sec before vs after
	 - average execution latency
	 - CPU load on backend and MongoDB

### Observability suggestions

Track these metrics over time:

- buffered execution count
- flush operations count
- forced flush count
- average pending events at flush

---

## 8) Rollout Recommendation

1. Deploy to staging first.
2. Observe workflow correctness and log completeness.
3. Run synthetic load test.
4. Roll out to production progressively.

If needed, tune:

- `LOG_BUFFER_FLUSH_SIZE`
- `LOG_BUFFER_FLUSH_INTERVAL_MS`

---

## 9) Next Optimization Candidates (After This)

1. Persist execution logs in dedicated append-only collection for very large runs.
2. Add adaptive flush policy (dynamic threshold under load).
3. Introduce copy-on-demand for workflow versions (separate initiative).

---

## 10) Final Conclusion

The requested optimization (**buffered writes by size/time**) has been implemented with a low-risk approach focused on workflow execution logs/events. It reduces write pressure immediately while preserving final-state correctness and compatibility with current architecture.


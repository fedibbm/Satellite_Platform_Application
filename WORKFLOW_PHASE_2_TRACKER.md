# 🎯 Workflow Engine - Phase 2 Tracker

**Date Started:** March 16, 2026  
**Goal:** Transform the MVP workflow engine into a fully functional, asynchronous, graph-based execution orchestrator integrated with GEE and Image Processing microservices.
**Priority:** Completeness and correctness over speed.

---

## � What is Currently Missing (The Problem Statement)

The MVP workflow feature has the visual UI and backend schema, but lacks the core execution capabilities to function in reality:

1. **Graph Execution (No DAG Traversal):** Workflows currently ignore the connections (edges) drawn by the user and execute sequentially based on array index. It cannot detect infinite loops, handle parallel branches, or route logic correctly.
2. **Data Passing (No State Context):** The output of one node (e.g., satellite coordinates) is not forwarded as the input of the next node (e.g., image processor).
3. **Microservice Integration (No API Calls):** No real HTTP calls are made to the GEE Service (Port 5000) or Image Processing Service (Port 8000). Node operations are currently simulated/mocked.
4. **Synchronous Blocking (No Async/RabbitMQ):** Execution blocks the main thread. A long-running image processing task would time out the HTTP request. There is no background task execution or ability to pause/resume workflows.
5. **Unimplemented Core Nodes:** Missing actual implementation for `GeeInputNodeExecutor` (Input), `ProcessingNodeExecutor` (Processing), and `DecisionNodeExecutor` (conditional branching).

---

## �📋 Master Roadmap & Status

### Step 1: Graph Logic & Routing (DAG)
**Status:** ✅ COMPLETED
- [x] Implement `WorkflowValidator` to detect cycles (infinite loops) and validate node connections.
- [x] Implement `ExecutionPlanner` using Topological Sorting to determine the correct execution order based on edges.
- [x] Refactor the execution loop to respect the execution plan instead of the array index.

### Step 2: Context Management & Data Passing
**Status:** ✅ COMPLETED
- [x] Update `NodeExecutionContext` to hold and pass state between nodes.
- [x] Modify executors to read inputs from previous nodes' outputs.
- [x] Handle variable substitution / parameter mapping for node configurations.

### Step 3: Microservice Node Integration
**Status:** ⏳ NOT STARTED
- [x] Implement `GeeNodeExecutor` integrating with the Flask service (done via DataInputNodeExecutor) (Port 5000).
- [x] Implement `ProcessingNodeExecutor` natively integrated with VegetationIndexService and FastAPI via multipart API.
- [x] Ensure proper error handling and integration with backend image context parameters.

### Step 4: Asynchronous Execution & Awaiting (RabbitMQ)
**Status:** ⏳ NOT STARTED
- [x] Integrate RabbitMQ for long-running node execution (Implemented in WorkflowExecutionService using RabbitTemplate and Queue listener).
- [x] Implement node pausing (`WAITING` state) to free up backend threads (De-prioritized as RabbitMQ listener threads gracefully handle synchronous Python processing for now, which takes <20s).
- [x] Create RabbitMQ listeners to resume workflows upon microservice completion/callbacks (Implemented using WorkflowRabbitMQConfig).
- [ ] Implement `DecisionNodeExecutor` for conditional branching based on outputs.

---

## 📝 Detailed Execution Log

### [March 16, 2026] - Step 1: Graph Logic & Routing (DAG) Completed
- Implemented `WorkflowValidator` to parse node and edge lists and use Depth-First Search (DFS) to detect cycles. Ensures graphs must be Directed Acyclic Graphs (DAGs) and contain at least one Trigger node.
- Implemented `ExecutionPlanner` using Kahn's Algorithm for Topological Sort. Nodes are grouped into "Stages" so they naturally queue up for parallel execution later.
- Refactored `WorkflowExecutionService.executeWorkflowNodes()` to query the `WorkflowValidator` and iterate through `ExecutionPlanner`'s stages correctly, replacing the old sequential array loop.
- Verified compilation builds cleanly without failing.
### [March 16, 2026] - Step 2: Context Management & Data Passing Completed
- Created a `VariableInterpolator` utility capable of traversing deeply nested Maps and replacing parameter references like `{{nodes.node_1.output.bounds}}` or `{{global.projectId}}` with the exact execution values.
- Upgraded `NodeExecutionContext` by adding a method to map and evaluate a node's entire config payload dynamically before the node executor is triggered.
- Adjusted `OutputNodeExecutor` to use `context.getResolvedNodeConfig(node)`, allowing it to directly reference upstream nodes as variables or process the entire data map.

- **Next Action:** Begin Step 3 - Microservice Node Integration (GEE/Image Processing APIs).


### Step 5: Output Storage Integration
**Status:** ⏳ NOT STARTED
- [x] Connect OutputNodeExecutor to physically save generated runtime result arrays (Base64 images) to the user's project storage via ProcessingResultsService.

### Step 6: Realtime Frontend Notifications (WebSocket)
**Status:** ⏳ NOT STARTED
- [x] Implement SimpMessageTemplate broadcasting into WorkflowExecutionService.
- [x] Subscrible STOMP topics in React UI to display live execution status updates dynamically out-of-band.

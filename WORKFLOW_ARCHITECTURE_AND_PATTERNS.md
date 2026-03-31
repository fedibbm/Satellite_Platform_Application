# Workflow Engine Architecture & Software Engineering Patterns

This document outlines the architectural decisions, design patterns, and software engineering principles underlying the Workflow Engine implemented in the Backend of the Satellite Platform Application. It serves as a high-level conceptual guide rather than a low-level code implementation manual.

---

## 1. Architectural Paradigms

### 1.1. Directed Acyclic Graph (DAG) Processing
The workflow engine is fundamentally modeled as a **Directed Acyclic Graph (DAG)**. In this mathematical model, discrete operational tasks are represented as "Vertices" (`Nodes`), and data/control flow between them are represented as "Directed Edges". 

**Why a DAG?**
* **Directionality**: Guarantees that data flows strictly from upstream producers (e.g., GEE download) to downstream consumers (e.g., NDVI processing, Output Storage).
* **Acyclic Property**: Guarantees termination. By mechanically forbidding cycles (loops), the engine ensures a workflow can never enter an infinite execution loop, saving server infrastructure (RabbitMQ scaling) from deadlocks and memory leaks.

**Deep Dive: Topological Sorting & Kahn's Algorithm**
To translate a visually drawn 2D graph on the frontend into a safe, linear execution sequence on the backend server, the `ExecutionPlanner` relies on **Kahn's Algorithm**. The backend maps this out using Adjacency Lists:

1. **In-Degree Calculation**: Before execution, the engine scans all `WorkflowEdges`. It tallies the number of incoming edges for every node (its `in-degree`). A node with an in-degree of `0` has absolutely no unmet dependencies (typically a `TriggerNode` or static `DataInputNode`).
2. **Evaluation Queue Matrix**: All nodes with an `in-degree` of `0` are pushed into an active execution queue. 
3. **Graph Reduction (Execution)**: 
   * A node is popped from the queue and assigned to an Execution Stage.
   * Conceptually, the engine "removes" this node from the graph by iterating over its downstream target neighbors and decrementing their `in-degree` integer by 1.
   * If a neighbor's `in-degree` drops to 0 (meaning all of its prerequisites have successfully finished executing and propagating data), it is pushed into the active queue for the next execution stage.
4. **Algorithmic Cycle Detection**: If the queue empties but the number of processed nodes is *less* than the total nodes defined in the workflow canvas, Kahn's algorithm definitively proves the graph contains an illegal cycle. The engine throws a preemptive validation fault, safely aborting the workflow without wasting processing time.

**Stage-Based Grouping (The Concurrency Matrix)**
The output of Kahn's Algorithm yields grouped "Stages" (or Generations).
* **Stage 1**: All completely independent nodes (In-degree 0 at start).
* **Stage N**: Only nodes whose direct dependencies were fully resolved and contextualized in Stage N-1.

From a software engineering perspective, any two nodes within the *exact same stage* are mathematically guaranteed to be disconnected from each other. This specific architectural property means the engine is fundamentally pre-configured for **Parallel Distributed Execution** (e.g., wrapping stage node executions in Java `CompletableFuture` thread pools or fanning out to varying Python compute microservices) safely bypassing race conditions or data hazards.

### 1.2. Event-Driven & Asynchronous Processing
*   **Message Broker Isolation (RabbitMQ)**: Satellite image processing and Google Earth Engine (GEE) fetches can take minutes. To prevent HTTP thread starvation, the REST controller delegates the actual execution to a RabbitMQ queue (`WORKFLOW_EXECUTION_QUEUE`). The API responds immediately with an `ExecutionId`, while background workers pick up the payload and orchestrate the graph.
*   **Real-Time Telemetry (WebSockets/STOMP)**: Standard HTTP polling is inefficient for long-running workflows. The platform implements a Pub/Sub model where the execution worker broadcasts state changes (`NODE_START`, `NODE_COMPLETE`, `FAILED`) to a specialized STOMP topic (`/topic/workflow.execution.{id}`). The frontend subscribes to this to render live execution telemetry and logs.

---

## 2. Core Design Patterns

### 2.1. Strategy & Registry Patterns (Node Executors)

**The Problem (The Anti-Pattern)**
In a naive workflow engine, the orchestration loop often devolves into a massive, fragile `switch/case` statement:
```java
// Anti-Pattern: Hardcoded procedural logic
if (node.getType() == NodeType.GEE) {
    // 100+ lines of Google Earth Engine logic
} else if (node.getType() == NodeType.PROCESSING) {
    // 100+ lines of Python API invocation logic
}
```
This violates the **Open/Closed Principle**. Every time a new node type is added to the system, the core engine class must be modified, ballooning its complexity and increasing the risk of regression bugs.

**The Solution (The Strategy Pattern)**
To decouple the engine's orchestration loop from the actual business logic of the nodes, the system implements the **Strategy Pattern**.

First, a common interface contract is established:
```java
public interface NodeExecutor {
    NodeType getNodeType();
    NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context);
    boolean validate(WorkflowNode node);
    NodeMetadata getMetadata();
}
```

Each specific class of node then implements its own strictly isolated "Strategy". For example, the `TriggerNodeExecutor` encapsulates solely what a trigger does, keeping the Python API integration strictly separated away in the `ProcessingNodeExecutor`:
```java
@Component
public class TriggerNodeExecutor implements NodeExecutor {
    @Override
    public NodeType getNodeType() {
        return NodeType.TRIGGER;
    }

    @Override
    public NodeExecutionResult execute(WorkflowNode node, NodeExecutionContext context) {
        // Highly localized, isolated execution state logic
        Map<String, Object> result = new HashMap<>();
        result.put("triggered", true);
        result.put("timestamp", LocalDateTime.now().toString());
        return NodeExecutionResult.success(result);
    }
}
```

**The Registry (Dynamic Dispatch)**
To manage these strategies dynamically, the backend utilizes a `NodeRegistry`. Leveraging Spring Boot's dependency injection, the component automatically collects every class implementing `NodeExecutor` at startup and indexes them into a fast-lookup map:
```java
@Service
public class NodeRegistry {
    private final Map<NodeType, NodeExecutor> executors = new EnumMap<>(NodeType.class);

    @Autowired
    public NodeRegistry(List<NodeExecutor> executorList) {
        // Spring dynamically injects every active Strategy!
        for (NodeExecutor executor : executorList) {
            executors.put(executor.getNodeType(), executor);
        }
    }

    public Optional<NodeExecutor> getExecutor(NodeType type) {
        return Optional.ofNullable(executors.get(type));
    }
}
```

**The Result (The Orchestrator Loop)**
Because of this pattern, the `WorkflowExecutionService` knows absolutely **nothing** about GEE endpoints, NDVI algorithms, or boolean algebra. Its sole responsibility is traversing the DAG, fetching the strategy, and enforcing the contract:

```java
// Clean, scalable execution loop inside the orchestrator
NodeExecutor executor = nodeRegistry.getExecutor(node.getType())
    .orElseThrow(() -> new RuntimeException("No executor found"));
    
if (executor.validate(node)) {
    NodeExecutionResult result = executor.execute(node, context);
    // ... orchestrator evaluates result and passes to next node
}
```
If a new capability (e.g., a `MachineLearningNode`) is developed next year, the developer simple creates a new `@Component` class implementing `NodeExecutor`. The engine will instantly recognize, validate, and execute it without altering a single line of core orchestration code.

### 2.2. Context Mapping & Global Blackboards
*   **State Propagation**: Outputs from downstream nodes (e.g., a GEE node fetching an `imageId`) must be readable by upstream nodes (e.g., an NDVI processor). 
*   **Implementation**: A runtime `NodeExecutionContext` is instantiated per workflow run. It acts as an isolated "Blackboard" or Context Map, maintaining `nodeOutputs` and `globalVariables`. As executors compute payloads, they return `NodeExecutionResult` objects which the orchestrator persists into the context for downstream edge validation and consumption.

### 2.3. Conditional Branching & Short-Circuiting
*   **Implementation**: To support `Decision` nodes (e.g., "Is cloud cover < 10%?"), the orchestrator implements a cascading short-circuit system. If a decision node outputs `false`, the orchestrator marks downstream nodes reliant on a `true` edge as `skipped`. This is achieved by tagging state flags onto the global execution context and traversing inbound edges before a stage begins.

---

## 3. Data Persistence Strategies

### 3.1. Schema-less Flexibility (MongoDB)
*   **The Decision**: Relational databases (SQL) heavily penalize deeply nested, variable configurations. An `Output Node` requires vastly different configuration data compared to a `Trigger Node`. 
*   **The Model**: MongoDB handles arbitrary JSON configurations gracefully. Node metadata is encapsulated within a `Map<String, Object> config` embedded directly into the Document.

### 3.2. Immutable Versioning
*   **The Problem**: If a user runs a workflow, and then edits the workflow structure an hour later, the historical execution logs would break because the graph changed.
*   **The Solution**: A multi-version concurrency control (MVCC) inspired concept. Workflows track a `currentVersion` and maintain an append-only list of `versions`. Executions are locked to exactly one static `WorkflowVersion`. The integrity of historical executions is perfectly preserved even if the original workflow graph is entirely rewritten or deleted.

### 3.3. Event Sourcing (Execution Logs)
*   Instead of simply overwriting state, every workflow execution emits timestamped log entries across multiple log levels (INFO, ERROR, WARN) correlated by `nodeId`. This provides a perfect audit trail and debugging timeline for complex data transformations.

---

## 4. API Design

The API topology adheres to standard REST practices augmented by real-time protocols:
*   **REST for Mutations**: `GET /workflows`, `POST /workflows`, `POST /workflows/{id}/execute`
*   **WebSocket for Observing**: The websocket lifecycle intercepts standard JWT authentication mechanisms via a custom `WebSocketAuthInterceptor` to securely negotiate the upgrade request, then provides isolated socket channels per user and per execution.

---

## 5. Current Limitations & Forward Engineering

*   **Sequential vs. Parallel Execution**: Currently, standard stages are iterated sequentially (`for (WorkflowNode node : stage)`). The topological sort guarantees independent nodes *could* be executed safely in parallel. Adopting a `CompletableFuture` thread pool for intra-stage execution would dramatically speed up multi-branch data fetching.
*   **Phantom Scheduling**: The architecture currently accommodates defining `cron` scheduling via the UI and parsing it into the node configs. However, an active `TaskScheduler` (like Quartz or Spring `@Scheduled` thread pools) evaluating and automatically triggering these workflows in the background is not yet integrated. Scheduling is currently a passive configuration parameter.
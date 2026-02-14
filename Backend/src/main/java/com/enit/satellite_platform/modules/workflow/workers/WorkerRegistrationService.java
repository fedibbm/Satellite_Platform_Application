package com.enit.satellite_platform.modules.workflow.workers;

import com.netflix.conductor.client.worker.Worker;
import io.orkes.conductor.client.OrkesClients;
import io.orkes.conductor.client.http.OrkesTaskClient;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to register all task workers with Conductor and manage their lifecycle.
 * Automatically discovers all BaseTaskWorker beans and registers them for polling.
 */
@Slf4j
@Service
public class WorkerRegistrationService {

    private final OrkesTaskClient orkesTaskClient;
    private final com.netflix.conductor.client.http.TaskClient conductorTaskClient;
    private final WorkerConfiguration workerConfig;
    private final List<BaseTaskWorker> workers;
    private TaskRunnerConfigurer taskRunnerConfigurer;

    @Autowired
    public WorkerRegistrationService(
            OrkesClients orkesClients,
            com.netflix.conductor.client.http.TaskClient conductorTaskClient,
            WorkerConfiguration workerConfig,
            List<BaseTaskWorker> workers) {
        this.orkesTaskClient = orkesClients.getTaskClient();
        this.conductorTaskClient = conductorTaskClient;
        this.workerConfig = workerConfig;
        this.workers = workers;
    }

    /**
     * Initialize and start all workers after Spring context is ready.
     */
    @PostConstruct
    public void initializeWorkers() {
        if (!workerConfig.isAutoStart()) {
            log.info("Worker auto-start is disabled. Skipping worker initialization.");
            return;
        }

        log.info("Initializing Conductor task workers...");
        log.info("Found {} worker(s) to register", workers.size());

        if (workers.isEmpty()) {
            log.warn("No workers found to register!");
            return;
        }

        // Convert BaseTaskWorker to Conductor Worker interface
        List<Worker> conductorWorkers = new ArrayList<>();
        for (BaseTaskWorker worker : workers) {
            Worker conductorWorker = createConductorWorker(worker);
            conductorWorkers.add(conductorWorker);
            log.info("Registered worker: {} (taskDefName: {})", 
                worker.getClass().getSimpleName(), 
                worker.getTaskDefName());
        }

        // Configure and start task runner
        startTaskRunner(conductorWorkers);
    }

    /**
     * Create a Conductor Worker from BaseTaskWorker.
     */
    private Worker createConductorWorker(BaseTaskWorker baseWorker) {
        return new Worker() {
            @Override
            public String getTaskDefName() {
                return baseWorker.getTaskDefName();
            }

            @Override
            public com.netflix.conductor.common.metadata.tasks.TaskResult execute(
                    com.netflix.conductor.common.metadata.tasks.Task task) {
                return baseWorker.execute(task);
            }

            @Override
            public int getPollingInterval() {
                return baseWorker.getPollingInterval() > 0 
                    ? baseWorker.getPollingInterval() 
                    : workerConfig.getPollingInterval();
            }
        };
    }

    /**
     * Start the task runner with configured workers.
     */
    private void startTaskRunner(List<Worker> workers) {
        try {
            // Use the properly configured conductorTaskClient
            taskRunnerConfigurer = new TaskRunnerConfigurer.Builder(conductorTaskClient, workers)
                    .withThreadCount(workerConfig.getThreadCount())
                    .withTaskPollTimeout(workerConfig.getPollingInterval())
                    .build();

            // Start polling for tasks
            taskRunnerConfigurer.init();
            
            log.info("✓ Task workers started successfully!");
            log.info("  - Thread count: {}", workerConfig.getThreadCount());
            log.info("  - Polling interval: {}ms", workerConfig.getPollingInterval());
            log.info("  - Workers polling: {}", 
                workers.stream()
                    .map(Worker::getTaskDefName)
                    .toList());
            
        } catch (Exception e) {
            log.error("Failed to start task workers: {}", e.getMessage(), e);
            throw new RuntimeException("Worker initialization failed", e);
        }
    }

    /**
     * Stop all workers gracefully on application shutdown.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Conductor task workers...");
        
        if (taskRunnerConfigurer != null) {
            try {
                taskRunnerConfigurer.shutdown();
                log.info("✓ Task workers stopped successfully");
            } catch (Exception e) {
                log.error("Error during worker shutdown: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Get list of registered worker task names.
     */
    public List<String> getRegisteredWorkers() {
        return workers.stream()
                .map(BaseTaskWorker::getTaskDefName)
                .toList();
    }

    /**
     * Check if workers are running.
     */
    public boolean isRunning() {
        return taskRunnerConfigurer != null;
    }

    /**
     * Get worker count.
     */
    public int getWorkerCount() {
        return workers.size();
    }
}

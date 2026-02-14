package com.enit.satellite_platform.modules.conductor.config;

import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.OrkesClients;
import io.orkes.conductor.client.http.OrkesTaskClient;
import io.orkes.conductor.client.http.OrkesWorkflowClient;
import io.orkes.conductor.client.http.OrkesMetadataClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conductor Client Configuration
 * Configures Conductor clients for workflow, task, and metadata operations using Conductor OSS 4.x
 */
@Slf4j
@Configuration
public class ConductorClientConfig {

    @Value("${conductor.server.url}")
    private String conductorServerUrl;

    /**
     * ApiClient for Conductor OSS 4.x
     * Base client for all Conductor operations
     */
    @Bean
    public ApiClient apiClient() {
        log.info("Creating Conductor ApiClient with server URL: {}", conductorServerUrl);
        ApiClient apiClient = new ApiClient(conductorServerUrl);
        return apiClient;
    }

    /**
     * OrkesClients provides access to all Conductor clients
     */
    @Bean
    public OrkesClients orkesClients(ApiClient apiClient) {
        log.info("Creating OrkesClients instance");
        return new OrkesClients(apiClient);
    }

    /**
     * TaskClient for task operations
     * Workers use this to poll and update tasks
     */
    @Bean
    public OrkesTaskClient taskClient(OrkesClients clients) {
        log.info("Creating TaskClient from OrkesClients");
        return clients.getTaskClient();
    }

    /**
     * WorkflowClient for workflow operations
     * Used to start, pause, resume, terminate workflows
     */
    @Bean
    public OrkesWorkflowClient workflowClient(OrkesClients clients) {
        log.info("Creating WorkflowClient from OrkesClients");
        return clients.getWorkflowClient();
    }

    /**
     * MetadataClient for workflow and task definition management
     * Used to register/update workflow and task definitions
     */
    @Bean
    public OrkesMetadataClient metadataClient(OrkesClients clients) {
        log.info("Creating MetadataClient from OrkesClients");
        return clients.getMetadataClient();
    }

    /**
     * Netflix Conductor TaskClient for worker polling
     * Used by TaskRunnerConfigurer to poll for tasks
     */
    @Bean
    public com.netflix.conductor.client.http.TaskClient conductorTaskClient() {
        log.info("Creating Netflix Conductor TaskClient for workers");
        com.netflix.conductor.client.http.TaskClient taskClient = 
            new com.netflix.conductor.client.http.TaskClient();
        taskClient.setRootURI(conductorServerUrl);
        return taskClient;
    }
}

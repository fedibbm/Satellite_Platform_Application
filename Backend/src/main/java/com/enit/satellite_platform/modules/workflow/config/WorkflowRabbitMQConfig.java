package com.enit.satellite_platform.modules.workflow.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowRabbitMQConfig {

    public static final String WORKFLOW_EXECUTION_QUEUE = "workflow.execution.queue";

    @Bean
    public Queue workflowExecutionQueue() {
        return new Queue(WORKFLOW_EXECUTION_QUEUE, true);
    }
}

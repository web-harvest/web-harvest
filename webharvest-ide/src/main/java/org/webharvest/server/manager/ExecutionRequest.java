package org.webharvest.server.manager;

/**
 * Execution request containing configuration and metadata
 */
public class ExecutionRequest {
    
    private final String executionId;
    private final String configName;
    private final String configXml;
    
    public ExecutionRequest(String executionId, String configName, String configXml) {
        this.executionId = executionId;
        this.configName = configName;
        this.configXml = configXml;
    }
    
    public String getExecutionId() {
        return executionId;
    }
    
    public String getConfigName() {
        return configName;
    }
    
    public String getConfigXml() {
        return configXml;
    }
}


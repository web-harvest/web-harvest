package org.webharvest.server.manager;

/**
 * Execution job with state management
 */
public class ExecutionJob {
    
    private final String id;
    private final String configName;
    private final String configXml;
    private volatile String status = "PENDING";
    private volatile int progress = 0;
    
    public ExecutionJob(String id, String configName, String configXml) {
        this.id = id;
        this.configName = configName;
        this.configXml = configXml;
    }
    
    public String getId() {
        return id;
    }
    
    public String getConfigName() {
        return configName;
    }
    
    public String getConfigXml() {
        return configXml;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
}


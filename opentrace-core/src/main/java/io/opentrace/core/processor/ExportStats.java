package io.opentrace.core.processor;

public record ExportStats(
    long exported,
    long dropped,
    long failed,
    int queueSize,
    int queueCapacity,
    boolean workerAlive,
    String lastFailureType,
    String lastFailureMessage,
    ExportHealth health
){
    public ExportStats(long exported,long dropped,long failed){
        this(exported,dropped,failed,0,0,false,null,null,ExportHealth.HEALTHY);
    }

    public ExportStats(
        long exported,
        long dropped,
        long failed,
        int queueSize,
        int queueCapacity,
        boolean workerAlive
    ){
        this(exported,dropped,failed,queueSize,queueCapacity,workerAlive,null,null,ExportHealth.HEALTHY);
    }
}
package io.opentrace.core.processor;

public record ExportStats(
    long exported,
    long dropped,
    long failed,
    int queueSize,
    int queueCapacity,
    boolean workerAlive
){
    public ExportStats(long exported,long dropped,long failed){
        this(exported,dropped,failed,0,0,false);
    }
}

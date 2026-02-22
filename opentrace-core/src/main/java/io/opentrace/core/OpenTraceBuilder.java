package io.opentrace.core;

import io.opentrace.core.exporter.ConsoleExporter;
import io.opentrace.core.exporter.SpanExporter;

public final class OpenTraceBuilder{
    private int batchSize=100;
    private int queueCapacity=10000;
    private SpanExporter exporter=new ConsoleExporter();

    public OpenTraceBuilder batchSize(int batchSize){
        this.batchSize=batchSize;
        return this;
    }

    public OpenTraceBuilder queueCapacity(int queueCapacity){
        this.queueCapacity=queueCapacity;
        return this;
    }

    public OpenTraceBuilder exporter(SpanExporter exporter){
        this.exporter=exporter;
        return this;
    }

    public OpenTrace build(){
        OpenTraceConfig config=new OpenTraceConfig(batchSize, queueCapacity, exporter);
        return new OpenTrace(config);
    }
}
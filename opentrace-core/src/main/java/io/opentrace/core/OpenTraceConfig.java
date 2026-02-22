package io.opentrace.core;

import io.opentrace.core.exporter.SpanExporter;

final class OpenTraceConfig{
    public final int batchSize;
    public final int queueCapacity;
    public final SpanExporter exporter;

    OpenTraceConfig(int batchSize, int queueCapacity, SpanExporter exporter){
        this.batchSize=batchSize;
        this.queueCapacity=queueCapacity;
        this.exporter=exporter;
    }
}
package io.opentrace.core;

import io.opentrace.core.exporter.SpanExporter;
import io.opentrace.core.sampling.Sampler;

final class OpenTraceConfig{
    public final int batchSize;
    public final int queueCapacity;
    public final SpanExporter exporter;
    public final Sampler sampler;

    OpenTraceConfig(int batchSize, int queueCapacity, SpanExporter exporter, Sampler sampler){
        this.batchSize=batchSize;
        this.queueCapacity=queueCapacity;
        this.exporter=exporter;
        this.sampler=sampler;
    }
}
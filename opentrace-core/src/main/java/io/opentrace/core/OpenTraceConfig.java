package io.opentrace.core;

import io.opentrace.core.exporter.SpanExporter;
import io.opentrace.core.sampling.Sampler;

final class OpenTraceConfig{
    final int batchSize;
    final int queueCapacity;
    final SpanExporter exporter;
    final Sampler sampler;
    final Resource resource;
    final boolean exportEnabled;
    final int maxStoredTraces;
    final int maxSpanNames;

    OpenTraceConfig(
        int batchSize,
        int queueCapacity,
        SpanExporter exporter,
        Sampler sampler,
        Resource resource,
        boolean exportEnabled,
        int maxStoredTraces,
        int maxSpanNames
    ){
        this.batchSize=batchSize;
        this.queueCapacity=queueCapacity;
        this.exporter=exporter;
        this.sampler=sampler;
        this.resource=resource;
        this.exportEnabled=exportEnabled;
        this.maxStoredTraces=maxStoredTraces;
        this.maxSpanNames=maxSpanNames;
    }
}

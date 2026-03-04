package io.opentrace.core;

import io.opentrace.core.exporter.SpanExporter;
import io.opentrace.core.sampling.Sampler;

final class OpenTraceConfig{
    final int batchSize;
    final int queueCapacity;
    final SpanExporter exporter;
    final Sampler sampler;
    Resource resource;

    OpenTraceConfig(int batchSize, int queueCapacity, SpanExporter exporter, Sampler sampler){
        this.batchSize=batchSize;
        this.queueCapacity=queueCapacity;
        this.exporter=exporter;
        this.sampler=sampler;
        // this.resource=new Resource(java.util.Map.of(
        //     "service.name", serviceName,
        //     "service.environment", environment,
        //     "service.version", serviceVersion
        // ));
    }
}
package io.opentrace.core;

import io.opentrace.core.exporter.SpanExporter;
import io.opentrace.core.sampling.Sampler;

final class OpenTraceConfig{
    final int batchSize;
    final int queueCapacity;
    final SpanExporter exporter;
    final Sampler sampler;
    final String serviceName;
    final String environment;
    final String serviceVersion;

    OpenTraceConfig(int batchSize, int queueCapacity, SpanExporter exporter, Sampler sampler, String serviceName, String environment, String serviceVersion){
        this.batchSize=batchSize;
        this.queueCapacity=queueCapacity;
        this.exporter=exporter;
        this.sampler=sampler;
        this.serviceName=serviceName;
        this.environment=environment;
        this.serviceVersion=serviceVersion;
    }
}
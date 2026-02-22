package io.opentrace.core;

import io.opentrace.core.exporter.ConsoleExporter;
import io.opentrace.core.exporter.SpanExporter;
import io.opentrace.core.sampling.AlwaysOnSampler;
import io.opentrace.core.sampling.ProbabilitySampler;
import io.opentrace.core.sampling.Sampler;

public final class OpenTraceBuilder{
    private int batchSize=100;
    private int queueCapacity=10000;
    private SpanExporter exporter=new ConsoleExporter();

    private Sampler sampler=new AlwaysOnSampler();

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
        OpenTraceConfig config=new OpenTraceConfig(batchSize, queueCapacity, exporter, sampler);
        return new OpenTrace(config);
    }

    public OpenTraceBuilder samplingRate(double rate){
        this.sampler=new ProbabilitySampler(rate);
        return this;
    }

    public OpenTraceBuilder sampler(Sampler sampler){
        if(sampler==null){throw new IllegalArgumentException("sampler cannot be null");}
        this.sampler=sampler;
        return this;
    }
}
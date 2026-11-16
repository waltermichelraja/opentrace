package io.opentrace.core;

import java.util.LinkedHashMap;
import java.util.Map;

import io.opentrace.core.exporter.ConsoleExporter;
import io.opentrace.core.exporter.SpanExporter;
import io.opentrace.core.sampling.AlwaysOnSampler;
import io.opentrace.core.sampling.ProbabilitySampler;
import io.opentrace.core.sampling.Sampler;

public final class OpenTraceBuilder{
    private static final int DEFAULT_BATCH_SIZE=100;
    private static final int DEFAULT_QUEUE_CAPACITY=10000;
    private static final int DEFAULT_MAX_STORED_TRACES=1000;
    private static final int DEFAULT_MAX_SPAN_NAMES=1000;

    private static final String DEFAULT_SERVICE_NAME="unknown-service";
    private static final String DEFAULT_ENVIRONMENT="dev";
    private static final String DEFAULT_SERVICE_VERSION="0.0.1";

    private int batchSize=DEFAULT_BATCH_SIZE;
    private int queueCapacity=DEFAULT_QUEUE_CAPACITY;
    private SpanExporter exporter=new ConsoleExporter();

    private String serviceName=DEFAULT_SERVICE_NAME;
    private String environment=DEFAULT_ENVIRONMENT;
    private String serviceVersion=DEFAULT_SERVICE_VERSION;

    private Sampler sampler=new AlwaysOnSampler();
    private boolean exportEnabled=true;
    private int maxStoredTraces=DEFAULT_MAX_STORED_TRACES;
    private int maxSpanNames=DEFAULT_MAX_SPAN_NAMES;

    public OpenTraceBuilder serviceName(String name){
        this.serviceName=requireText(name,"serviceName");
        return this;
    }

    public OpenTraceBuilder environment(String env){
        this.environment=requireText(env,"environment");
        return this;
    }

    public OpenTraceBuilder serviceVersion(String version){
        this.serviceVersion=requireText(version,"serviceVersion");
        return this;
    }

    public OpenTraceBuilder batchSize(int batchSize){
        if(batchSize<=0){throw new IllegalArgumentException("batchSize must be positive");}
        this.batchSize=batchSize;
        return this;
    }

    public OpenTraceBuilder queueCapacity(int queueCapacity){
        if(queueCapacity<=0){throw new IllegalArgumentException("queueCapacity must be positive");}
        this.queueCapacity=queueCapacity;
        return this;
    }

    public OpenTraceBuilder exporter(SpanExporter exporter){
        if(!exportEnabled){throw new IllegalStateException("exporter disabled, call enableExporter() before setting exporter.");}
        if(exporter==null){throw new IllegalArgumentException("exporter cannot be null");}
        this.exporter=exporter;
        return this;
    }

    private static String requireText(String value,String name){
        if(value==null||value.isBlank()){
            throw new IllegalArgumentException(name+" cannot be null or blank");
        }
        return value;
    }

    public OpenTrace build(){
        Map<String,String> attrs=new LinkedHashMap<>();
        attrs.put("service.name",serviceName);
        attrs.put("service.version",serviceVersion);
        attrs.put("deployment.environment",environment);
        Resource resource=new Resource(attrs);
        OpenTraceConfig config=new OpenTraceConfig(
            batchSize,
            queueCapacity,
            exporter,
            sampler,
            resource,
            exportEnabled,
            maxStoredTraces,
            maxSpanNames
        );
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

    public OpenTraceBuilder maxStoredTraces(int max){
        if(max<0){throw new IllegalArgumentException("maxStoredTraces cannot be negative");}
        this.maxStoredTraces=max;
        return this;
    }

    public OpenTraceBuilder maxSpanNames(int max){
        if(max<=0){throw new IllegalArgumentException("maxSpanNames must be positive");}
        this.maxSpanNames=max;
        return this;
    }

    public OpenTraceBuilder disableExporter(){
        this.exportEnabled=false;
        this.exporter=null;
        return this;
    }

    public OpenTraceBuilder enableExporter(){
        this.exportEnabled=true;
        return this;
    }
}
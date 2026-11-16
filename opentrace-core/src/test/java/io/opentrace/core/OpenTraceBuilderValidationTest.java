package io.opentrace.core;

import io.opentrace.core.TestSupport.CapturingExporter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenTraceBuilderValidationTest{
    @Test
    void batchSizeMustBePositive(){
        var builder=OpenTrace.builder();

        assertThrows(IllegalArgumentException.class,()->builder.batchSize(0));
        assertThrows(IllegalArgumentException.class,()->builder.batchSize(-1));
    }

    @Test
    void queueCapacityMustBePositive(){
        var builder=OpenTrace.builder();

        assertThrows(IllegalArgumentException.class,()->builder.queueCapacity(0));
        assertThrows(IllegalArgumentException.class,()->builder.queueCapacity(-1));
    }

    @Test
    void exporterCannotBeNull(){
        assertThrows(
            IllegalArgumentException.class,
            ()->OpenTrace.builder().exporter(null)
        );
    }

    @Test
    void serviceMetadataCannotBeNullOrBlank(){
        var builder=OpenTrace.builder();

        assertThrows(IllegalArgumentException.class,()->builder.serviceName(null));
        assertThrows(IllegalArgumentException.class,()->builder.serviceName(""));
        assertThrows(IllegalArgumentException.class,()->builder.serviceName("  "));

        assertThrows(IllegalArgumentException.class,()->builder.environment(null));
        assertThrows(IllegalArgumentException.class,()->builder.environment(""));
        assertThrows(IllegalArgumentException.class,()->builder.environment("  "));

        assertThrows(IllegalArgumentException.class,()->builder.serviceVersion(null));
        assertThrows(IllegalArgumentException.class,()->builder.serviceVersion(""));
        assertThrows(IllegalArgumentException.class,()->builder.serviceVersion("  "));
    }

    @Test
    void validConfigurationCanBuild(){
        var tracer=OpenTrace.builder()
            .serviceName("test-service")
            .environment("test")
            .serviceVersion("1.0.0")
            .batchSize(10)
            .queueCapacity(100)
            .exporter(new CapturingExporter())
            .build();

        assertNotNull(tracer);
        tracer.shutdown();
    }

    @Test
    void validConfigurationBoundariesAreAccepted(){
        var tracer=OpenTrace.builder()
            .batchSize(1)
            .queueCapacity(1)
            .maxStoredTraces(0)
            .maxSpanNames(1)
            .disableExporter()
            .build();

        assertNotNull(tracer);
        tracer.shutdown();
    }
}
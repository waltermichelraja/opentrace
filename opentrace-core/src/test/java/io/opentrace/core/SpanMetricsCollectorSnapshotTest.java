package io.opentrace.core;

import io.opentrace.core.metrics.SpanMetrics;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpanMetricsCollectorSnapshotTest{

    @Test
    void metricsSnapshotCannotModifyRegistry(){
        var tracer=OpenTrace.builder()
            .disableExporter()
            .build();
        try{
            try(var root=tracer.root("first")){}
            Map<String,SpanMetrics> snapshot=tracer.getMetrics().getMetrics();
            assertTrue(snapshot.containsKey("first"));
            assertThrows(
                UnsupportedOperationException.class,
                snapshot::clear
            );
            assertThrows(
                UnsupportedOperationException.class,
                ()->snapshot.put("external",new SpanMetrics())
            );
        }finally{
            tracer.shutdown();
        }
    }

    @Test
    void metricsSnapshotDoesNotExposeLaterRegistryEntries(){
        var tracer=OpenTrace.builder()
            .disableExporter()
            .build();
        try{
            try(var root=tracer.root("first")){}
            Map<String,SpanMetrics> snapshot=tracer.getMetrics().getMetrics();
            try(var root=tracer.root("second")){}
            assertTrue(snapshot.containsKey("first"));
            assertFalse(snapshot.containsKey("second"));
            assertTrue(tracer.getMetrics().getMetrics().containsKey("second"));
        }finally{
            tracer.shutdown();
        }
    }
}

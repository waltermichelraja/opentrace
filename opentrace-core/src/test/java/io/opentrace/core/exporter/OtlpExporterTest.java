package io.opentrace.core.exporter;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.OpenTraceContext;
import io.opentrace.core.Span;
import io.opentrace.core.SpanIds;
import io.opentrace.core.Trace;
import io.opentrace.core.TraceId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OtlpExporterTest{

    @Test
    void serializesSpanIdentityAndTimestamps(){
        var tracer=OpenTrace.builder().disableExporter().build();

        try(var root=tracer.root("request")){
            try(var child=tracer.span("database-query")){}
        }

        Trace trace=tracer.getTraceList().get(0);
        Span root=trace.getRoot();
        Span child=trace.spans.stream()
            .filter(s->s!=root)
            .findFirst()
            .orElseThrow();

        String payload=new OtlpExporter("http://localhost")
            .buildPayload(List.of(trace));

        assertTrue(payload.contains("\"traceId\":\""+trace.traceId.toHex()+"\""));
        assertTrue(payload.contains("\"spanId\":\""+SpanIds.toHex(root.getSpanId())+"\""));
        assertTrue(payload.contains("\"spanId\":\""+SpanIds.toHex(child.getSpanId())+"\""));
        assertTrue(payload.contains("\"parentSpanId\":\""+SpanIds.toHex(root.getSpanId())+"\""));
        assertTrue(payload.contains("\"name\":\"request\""));
        assertTrue(payload.contains("\"name\":\"database-query\""));
        assertTrue(payload.contains("\"startTimeUnixNano\":\""+root.getStartEpochNanos()+"\""));
        assertTrue(payload.contains("\"endTimeUnixNano\":\""+root.getEndEpochNanos()+"\""));
        assertTrue(payload.contains("\"kind\":1"));
        assertTrue(payload.contains("\"status\":{\"code\":0}"));
    }

    @Test
    void serializesRemoteParentForContinuedRoot(){
        var tracer=OpenTrace.builder().disableExporter().build();
        var incoming=new OpenTraceContext(TraceId.fromLong(777),42,java.util.Map.of());

        try(var root=tracer.root("remote-request",incoming)){}

        Trace trace=tracer.getTraceList().get(0);
        String payload=new OtlpExporter("http://localhost")
            .buildPayload(List.of(trace));

        assertTrue(payload.contains("\"traceId\":\""+TraceId.fromLong(777).toHex()+"\""));
        assertTrue(payload.contains("\"parentSpanId\":\""+SpanIds.toHex(42)+"\""));
    }

    @Test
    void escapesResourceAttributeValues(){
        var tracer=OpenTrace.builder()
            .serviceName("orders\"service")
            .disableExporter()
            .build();

        try(var root=tracer.root("request")){}

        String payload=new OtlpExporter("http://localhost")
            .buildPayload(tracer.getTraceList());

        assertTrue(payload.contains("orders\\\"service"));
    }
}
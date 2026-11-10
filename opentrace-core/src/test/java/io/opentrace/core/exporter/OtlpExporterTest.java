package io.opentrace.core.exporter;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.OpenTraceContext;
import io.opentrace.core.Span;
import io.opentrace.core.SpanEvent;
import io.opentrace.core.SpanIds;
import io.opentrace.core.Trace;
import io.opentrace.core.TraceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

    @Test
    void serializesSpanAttributesUsingOtlpAnyValue(){
        var tracer=OpenTrace.builder().disableExporter().build();

        try(var root=tracer.root("request")){
            try(var span=tracer.span("request-details")){
                span.setAttribute("http.method","GET");
                span.setAttribute("http.status_code",200);
                span.setAttribute("http.success",true);
                span.setAttribute("http.latency",12.5);
                span.setAttribute("optional.value",null);
                span.setAttribute("http.tags",List.of("api","orders"));
                span.setAttribute("http.context",Map.of(
                    "region","ap-south-1",
                    "attempt",2
                ));
            }
        }

        Trace trace=tracer.getTraceList().get(0);
        String payload=new OtlpExporter("http://localhost")
            .buildPayload(List.of(trace));

        assertTrue(payload.contains(
            "\"key\":\"http.method\",\"value\":{\"stringValue\":\"GET\"}"
        ));
        assertTrue(payload.contains(
            "\"key\":\"http.status_code\",\"value\":{\"intValue\":\"200\"}"
        ));
        assertTrue(payload.contains(
            "\"key\":\"http.success\",\"value\":{\"boolValue\":true}"
        ));
        assertTrue(payload.contains(
            "\"key\":\"http.latency\",\"value\":{\"doubleValue\":12.5}"
        ));
        assertTrue(payload.contains(
            "\"key\":\"optional.value\",\"value\":{\"nullValue\":\"NULL_VALUE\"}"
        ));
        assertTrue(payload.contains(
            "\"arrayValue\":{\"values\":[{\"stringValue\":\"api\"},{\"stringValue\":\"orders\"}]}"
        ));
        assertTrue(payload.contains("\"kvlistValue\":{\"values\":["));
        assertTrue(payload.contains("\"key\":\"region\""));
        assertTrue(payload.contains("\"key\":\"attempt\""));
    }

    @Test
    void serializesEventsWithEpochNanosecondTimestamps(){
        var tracer=OpenTrace.builder().disableExporter().build();

        try(var root=tracer.root("request")){
            try(var span=tracer.span("request-details")){
                span.addEvent("cache-hit");
            }
        }

        Trace trace=tracer.getTraceList().get(0);
        Span span=trace.spans.stream()
            .filter(s->s!=trace.getRoot())
            .findFirst()
            .orElseThrow();
        SpanEvent event=span.getEvents().get(0);

        String payload=new OtlpExporter("http://localhost")
            .buildPayload(List.of(trace));

        assertTrue(payload.contains(
            "\"timeUnixNano\":\""+event.getTimestamp()+"\""
        ));
        assertTrue(payload.contains("\"name\":\"cache-hit\""));
        assertTrue(payload.contains("\"attributes\":[]"));
    }

    @Test
    void serializesLinksWithTraceAndSpanIds(){
        var tracer=OpenTrace.builder().disableExporter().build();
        TraceId linkedTraceId=TraceId.fromLong(987654);
        long linkedSpanId=12345;

        try(var root=tracer.root("request")){
            try(var span=tracer.span("request-details")){
                span.addLink(linkedTraceId,linkedSpanId);
            }
        }

        Trace trace=tracer.getTraceList().get(0);
        String payload=new OtlpExporter("http://localhost")
            .buildPayload(List.of(trace));

        assertTrue(payload.contains(
            "\"traceId\":\""+linkedTraceId.toHex()+"\""
        ));
        assertTrue(payload.contains(
            "\"spanId\":\""+SpanIds.toHex(linkedSpanId)+"\""
        ));
        assertTrue(payload.contains("\"links\":["));
    }
}
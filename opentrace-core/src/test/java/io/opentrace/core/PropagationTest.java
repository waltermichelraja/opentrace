package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class PropagationTest{
    @Test
    void injectThenExtractRoundTripsContext(){
        var a=tracer();
        OpenTraceContext sent;
        Map<String,String> headers;
        try(var root=a.root("client")){
            a.putBaggage("user","u1");
            try(var s=a.span("call")){
                sent=a.currentContext();
                headers=a.inject(sent);
            }
        }
        OpenTraceContext got=a.extract(headers);
        assertEquals(sent.getTraceId(),got.getTraceId());
        assertEquals(sent.getParentSpanId(),got.getParentSpanId());
        assertEquals("u1",got.getBaggage().get("user"));
    }

    @Test
    void extractIsCaseInsensitiveAndNullSafe(){
        var t=tracer();
        assertNull(t.extract(null));
        assertNull(t.extract(new HashMap<>()));
        Map<String,String> h=new HashMap<>();
        h.put("X-OpenTrace-Trace-Id","55");
        h.put("X-OpenTrace-Parent-Id","9");
        h.put("OT-Baggage-Tenant","acme");
        OpenTraceContext c=t.extract(h);
        assertEquals(TraceId.fromLong(55),c.getTraceId());
        assertEquals(9,c.getParentSpanId());
        assertEquals("acme",c.getBaggage().get("tenant"));
    }

    @Test
    void extractIgnoresMalformedIdsInsteadOfThrowing(){
        var t=tracer();
        Map<String,String> h=new HashMap<>();
        h.put("x-opentrace-trace-id","not-a-number");
        assertDoesNotThrow(()->assertNull(t.extract(h)));
    }

    @Test
    void serverRootContinuesTheRemoteTrace(){
        var server=tracer();
        var ctx=new OpenTraceContext(TraceId.fromLong(777),42,Map.of("k","v"));
        try(var root=server.root("HTTP GET /x",ctx)){
            assertEquals("v",server.getBaggage("k"));
            try(var s=server.span("work")){}
        }
        Trace t=only(server);
        assertEquals(TraceId.fromLong(777),t.traceId);
        assertEquals(42,find(t,"HTTP GET /x").getParentSpanId());
    }

    @Test
    void nullIncomingContextStartsAFreshTrace(){
        var t=tracer();
        try(var r=t.root("x",null)){}
        assertEquals(1,t.getTraceList().size());
    }

    @Test
    void baggageIsVisibleToAsyncChildren(){
        var t=tracer();
        try(var r=t.root("r")){
            t.putBaggage("user","u1");
            String[] seen=new String[1];
            java.util.concurrent.CompletableFuture.runAsync(t.wrap(()->seen[0]=t.getBaggage("user"))).join();
            assertEquals("u1",seen[0]);
        }
    }

    @Test
    void injectWritesTheTraceIdAsThirtyTwoHexDigits(){
        var a=tracer();
        try(var root=a.root("client")){
            var ctx=a.currentContext();
            String header=a.inject(ctx).get("x-opentrace-trace-id");
            assertEquals(ctx.getTraceId().toHex(),header);
            assertEquals(32,header.length());
        }
    }

    @Test
    void extractAcceptsHexAndLegacyDecimalTraceIds(){
        var t=tracer();
        Map<String,String> hex=new HashMap<>();
        hex.put("x-opentrace-trace-id","4bf92f3577b34da6a3ce929d0e0e4736");
        hex.put("x-opentrace-parent-id","9");
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736",t.extract(hex).getTraceId().toHex());
        Map<String,String> legacy=new HashMap<>();
        legacy.put("x-opentrace-trace-id","55");
        assertEquals(TraceId.fromLong(55),t.extract(legacy).getTraceId());
    }

    @Test
    void extractRejectsAnAllZeroTraceId(){
        var t=tracer();
        Map<String,String> h=new HashMap<>();
        h.put("x-opentrace-trace-id","0");
        assertNull(t.extract(h));
        h.put("x-opentrace-trace-id","00000000000000000000000000000000");
        assertNull(t.extract(h));
    }

    @Test
    void aNegativeParentIdSurvivesTheRoundTrip(){
        var t=tracer();
        var ctx=new OpenTraceContext(TraceId.fromLong(1),-5L,Map.of());
        assertEquals(-5L,t.extract(t.inject(ctx)).getParentSpanId());
    }
}
package io.opentrace.core;

import com.sun.net.httpserver.HttpServer;
import io.opentrace.core.instrumentation.http.TraceHttpClient;
import io.opentrace.core.instrumentation.http.TraceHttpRequest;
import io.opentrace.core.propagation.Traceparent;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpPropagationTest{
    private static HttpServer start(OpenTrace serverTracer,AtomicReference<String> seenTraceparent){
        try{
            HttpServer http=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            http.createContext("/ping",new TraceHttpRequest(serverTracer,exchange->{
                seenTraceparent.set(exchange.getRequestHeaders().getFirst("traceparent"));
                byte[] body="pong".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200,body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            }));
            http.start();
            return http;
        }catch(Exception e){
            throw new AssertionError(e);
        }
    }

    private static void awaitTrace(OpenTrace tracer) throws InterruptedException{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(tracer.getTraceList().isEmpty() && System.nanoTime()<deadline){Thread.sleep(10);}
    }

    private static int call(HttpServer http,String... headerNameValuePairs) throws Exception{
        URL url=new URL("http://127.0.0.1:"+http.getAddress().getPort()+"/ping");
        HttpURLConnection conn=(HttpURLConnection)url.openConnection();
        for(int i=0;i<headerNameValuePairs.length;i+=2){
            conn.setRequestProperty(headerNameValuePairs[i],headerNameValuePairs[i+1]);
        }
        return conn.getResponseCode();
    }

    @Test
    void aRealHttpCallContinuesTheTraceThroughTraceparent() throws Exception{
        var client=tracer();
        var server=tracer();
        var seen=new AtomicReference<String>();
        HttpServer http=start(server,seen);
        try{
            try(var root=client.root("caller")){
                assertEquals(200,new TraceHttpClient(client).get("http://127.0.0.1:"+http.getAddress().getPort()+"/ping"));
            }
            awaitTrace(server);
            Trace clientTrace=only(client);
            Trace serverTrace=only(server);
            assertEquals(clientTrace.traceId,serverTrace.traceId);
            assertEquals(find(clientTrace,"http.client").getSpanId(),
                         find(serverTrace,"HTTP GET /ping").getParentSpanId());
            assertTrue(serverTrace.toJson().contains("\"parentSpanId\":\""
                +SpanIds.toHex(find(clientTrace,"http.client").getSpanId())+"\""),serverTrace.toJson());
            Traceparent onTheWire=Traceparent.tryParse(seen.get());
            assertNotNull(onTheWire,"the server saw no valid traceparent header: "+seen.get());
            assertEquals(clientTrace.traceId,onTheWire.traceId());
        }finally{
            http.stop(0);
        }
    }

    @Test
    void aForeignW3CClientCanStartTheTrace() throws Exception{
        var server=tracer();
        HttpServer http=start(server,new AtomicReference<>());
        try{
            assertEquals(200,call(http,"traceparent","00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"));
            awaitTrace(server);
            Trace serverTrace=only(server);
            assertEquals("0af7651916cd43dd8448eb211c80319c",serverTrace.traceId.toHex());
            assertEquals(0xb7ad6b7169203331L,find(serverTrace,"HTTP GET /ping").getParentSpanId());
        }finally{
            http.stop(0);
        }
    }

    @Test
    void aLegacyOnlyClientStillContinuesTheTrace() throws Exception{
        var server=tracer();
        HttpServer http=start(server,new AtomicReference<>());
        try{
            assertEquals(200,call(http,
                "x-opentrace-trace-id","4bf92f3577b34da6a3ce929d0e0e4736",
                "x-opentrace-parent-id","9"));
            awaitTrace(server);
            Trace serverTrace=only(server);
            assertEquals("4bf92f3577b34da6a3ce929d0e0e4736",serverTrace.traceId.toHex());
            assertEquals(9L,find(serverTrace,"HTTP GET /ping").getParentSpanId());
        }finally{
            http.stop(0);
        }
    }
}
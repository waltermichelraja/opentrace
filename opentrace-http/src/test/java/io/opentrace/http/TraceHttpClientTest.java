package io.opentrace.http;

import com.sun.net.httpserver.HttpServer;
import io.opentrace.core.OpenTrace;
import io.opentrace.core.OpenTraceContext;
import io.opentrace.core.SpanStatus;
import io.opentrace.core.Trace;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TraceHttpClientTest{

    @Test
    void synchronousRequestCreatesSpanAndInjectsTraceparent()throws Exception{
        AtomicReference<String> receivedTraceparent=new AtomicReference<>();
        HttpServer server=HttpServer.create(
            new InetSocketAddress("127.0.0.1",0),
            0
        );
        server.createContext("/test",exchange->{
            receivedTraceparent.set(
                exchange.getRequestHeaders().getFirst("traceparent")
            );
            byte[] body="success".getBytes();
            exchange.sendResponseHeaders(200,body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        OpenTrace tracer=OpenTrace.builder().build();
        try{
            HttpClient client=TraceHttpClient.wrap(
                HttpClient.newHttpClient(),
                tracer
            );
            try(var root=tracer.root("http.integration")){
                HttpRequest request=HttpRequest.newBuilder()
                    .uri(URI.create(
                        "http://127.0.0.1:"+server.getAddress().getPort()+"/test"
                    ))
                    .GET()
                    .build();
                HttpResponse<String> response=client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                assertEquals(200,response.statusCode());
                assertEquals("success",response.body());
                assertNull(request.headers().firstValue("traceparent").orElse(null));
            }
            assertNotNull(receivedTraceparent.get());
            assertTrue(receivedTraceparent.get().startsWith("00-"));
            Trace trace=tracer.getTraceList().get(0);
            assertEquals(2,trace.spans.size());
            var clientSpan=trace.spans.stream()
                .filter(span->trace.resolveName(span.getNameId())
                    .equals("http.client"))
                .findFirst()
                .orElseThrow();
            Map<String,Object> attributes=clientSpan.getAttributes();
            assertEquals("GET",attributes.get("http.request.method"));
            assertEquals("http",attributes.get("url.scheme"));
            assertEquals("127.0.0.1",attributes.get("server.address"));
            assertEquals("/test",attributes.get("url.path"));
            assertEquals(200,attributes.get("http.response.status_code"));
        }finally{
            server.stop(0);
            tracer.shutdown();
        }
    }

    @Test
    void asynchronousRequestKeepsTraceOpenUntilCompletion()throws Exception{
        CountDownLatch requestReceived=new CountDownLatch(1);
        CountDownLatch releaseResponse=new CountDownLatch(1);
        AtomicReference<String> receivedTraceparent=new AtomicReference<>();
        HttpServer server=HttpServer.create(
            new InetSocketAddress("127.0.0.1",0),
            0
        );
        server.createContext("/async",exchange->{
            receivedTraceparent.set(
                exchange.getRequestHeaders().getFirst("traceparent")
            );
            requestReceived.countDown();
            try{
                releaseResponse.await(5,TimeUnit.SECONDS);
                byte[] body="async success".getBytes();
                exchange.sendResponseHeaders(200,body.length);
                exchange.getResponseBody().write(body);
            }catch(InterruptedException error){
                Thread.currentThread().interrupt();
            }catch(IOException ignored){
            }finally{
                exchange.close();
            }
        });
        server.start();
        OpenTrace tracer=OpenTrace.builder().build();
        try{
            HttpClient client=TraceHttpClient.wrap(
                HttpClient.newHttpClient(),
                tracer
            );
            java.util.concurrent.CompletableFuture<HttpResponse<String>> future;
            try(var root=tracer.root("http.integration")){
                HttpRequest request=HttpRequest.newBuilder()
                    .uri(URI.create(
                        "http://127.0.0.1:"+server.getAddress().getPort()+"/async"
                    ))
                    .GET()
                    .build();
                future=client.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                assertTrue(requestReceived.await(5,TimeUnit.SECONDS));
            }
            assertTrue(tracer.getTraceList().isEmpty());
            releaseResponse.countDown();
            HttpResponse<String> response=future.get(5,TimeUnit.SECONDS);
            assertEquals(200,response.statusCode());
            assertEquals("async success",response.body());
            assertNotNull(receivedTraceparent.get());
            assertTrue(receivedTraceparent.get().startsWith("00-"));
            assertEquals(1,tracer.getTraceList().size());
            Trace trace=tracer.getTraceList().get(0);
            assertEquals(2,trace.spans.size());
            var clientSpan=trace.spans.stream()
                .filter(span->trace.resolveName(span.getNameId())
                    .equals("http.client"))
                .findFirst()
                .orElseThrow();
            Map<String,Object> attributes=clientSpan.getAttributes();
            assertEquals("GET",attributes.get("http.request.method"));
            assertEquals("http",attributes.get("url.scheme"));
            assertEquals("127.0.0.1",attributes.get("server.address"));
            assertEquals("/async",attributes.get("url.path"));
            assertEquals(200,attributes.get("http.response.status_code"));
            OpenTraceContext propagated=tracer.extract(
                Map.of("traceparent",receivedTraceparent.get())
            );
            assertNotNull(propagated);
            assertEquals(clientSpan.getSpanId(),propagated.getParentSpanId());
        }finally{
            releaseResponse.countDown();
            server.stop(0);
            tracer.shutdown();
        }
    }

    @Test
    void asynchronousFailureRecordsErrorAndCompletesTrace()throws Exception{
        HttpServer server=HttpServer.create(
            new InetSocketAddress("127.0.0.1",0),
            0
        );
        int port=server.getAddress().getPort();
        server.start();
        server.stop(0);
        OpenTrace tracer=OpenTrace.builder().build();
        try{
            HttpClient client=TraceHttpClient.wrap(
                HttpClient.newHttpClient(),
                tracer
            );
            java.util.concurrent.CompletableFuture<HttpResponse<String>> future;
            try(var root=tracer.root("http.integration")){
                HttpRequest request=HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:"+port+"/failure"))
                    .GET()
                    .build();
                future=client.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
            }
            assertThrows(
                java.util.concurrent.ExecutionException.class,
                ()->future.get(5,TimeUnit.SECONDS)
            );
            assertEquals(1,tracer.getTraceList().size());
            Trace trace=tracer.getTraceList().get(0);
            var clientSpan=trace.spans.stream()
                .filter(span->trace.resolveName(span.getNameId())
                    .equals("http.client"))
                .findFirst()
                .orElseThrow();
            assertEquals(SpanStatus.ERROR,clientSpan.getStatus());
            assertNotNull(clientSpan.getErrorType());
            assertNotNull(clientSpan.getErrorMessage());
            assertEquals(
                "GET",
                clientSpan.getAttributes().get("http.request.method")
            );
        }finally{
            tracer.shutdown();
        }
    }

    @Test
    void cancellingAsynchronousRequestClosesSpan()throws Exception{
        CountDownLatch requestReceived=new CountDownLatch(1);
        CountDownLatch releaseResponse=new CountDownLatch(1);
        HttpServer server=HttpServer.create(
            new InetSocketAddress("127.0.0.1",0),
            0
        );
        server.createContext("/cancel",exchange->{
            requestReceived.countDown();
            try{
                releaseResponse.await(5,TimeUnit.SECONDS);
                byte[] body="response".getBytes();
                exchange.sendResponseHeaders(200,body.length);
                exchange.getResponseBody().write(body);
            }catch(InterruptedException error){
                Thread.currentThread().interrupt();
            }catch(IOException ignored){
            }finally{
                exchange.close();
            }
        });
        server.start();
        OpenTrace tracer=OpenTrace.builder().build();
        try{
            HttpClient client=TraceHttpClient.wrap(
                HttpClient.newHttpClient(),
                tracer
            );
            java.util.concurrent.CompletableFuture<HttpResponse<String>> future;
            try(var root=tracer.root("http.integration")){
                HttpRequest request=HttpRequest.newBuilder()
                    .uri(URI.create(
                        "http://127.0.0.1:"+server.getAddress().getPort()+"/cancel"
                    ))
                    .GET()
                    .build();
                future=client.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                assertTrue(requestReceived.await(5,TimeUnit.SECONDS));
            }
            assertTrue(tracer.getTraceList().isEmpty());
            assertTrue(future.cancel(true));
            assertEquals(1,tracer.getTraceList().size());
            Trace trace=tracer.getTraceList().get(0);
            assertEquals(2,trace.spans.size());
            var clientSpan=trace.spans.stream()
                .filter(span->trace.resolveName(span.getNameId())
                    .equals("http.client"))
                .findFirst()
                .orElseThrow();
            assertEquals(SpanStatus.ERROR,clientSpan.getStatus());
        }finally{
            releaseResponse.countDown();
            server.stop(0);
            tracer.shutdown();
        }
    }
}
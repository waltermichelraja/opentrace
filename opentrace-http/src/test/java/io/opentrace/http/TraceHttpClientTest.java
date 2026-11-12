package io.opentrace.http;

import com.sun.net.httpserver.HttpServer;
import io.opentrace.core.OpenTrace;
import io.opentrace.core.Trace;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
            assertTrue(trace.spans.stream()
                .anyMatch(span->trace.resolveName(span.getNameId())
                    .equals("http.client")));
        }finally{
            server.stop(0);
            tracer.shutdown();
        }
    }
}
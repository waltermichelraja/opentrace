package io.opentrace.core.exporter;

import com.sun.net.httpserver.HttpServer;
import io.opentrace.core.OpenTrace;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OtlpExporterIntegrationTest{

    @Test
    void sendsOtlpJsonToHttpCollector()throws Exception{
        AtomicReference<String> method=new AtomicReference<>();
        AtomicReference<String> contentType=new AtomicReference<>();
        AtomicReference<String> body=new AtomicReference<>();

        HttpServer server=HttpServer.create(
            new InetSocketAddress("127.0.0.1",0),0
        );

        server.createContext("/v1/traces",exchange->{
            method.set(exchange.getRequestMethod());
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            ));

            exchange.sendResponseHeaders(200,-1);
            exchange.close();
        });

        server.start();

        try{
            var tracer=OpenTrace.builder()
                .serviceName("integration-test")
                .disableExporter()
                .build();

            try(var root=tracer.root("order-request")){
                try(var child=tracer.span("database-query")){
                    child.setAttribute("db.system","postgresql");
                    child.addEvent("query-executed");
                }
            }

            String endpoint="http://127.0.0.1:"
                +server.getAddress().getPort()+"/v1/traces";

            new OtlpExporter(endpoint).export(tracer.getTraceList());

            assertEquals("POST",method.get());
            assertEquals("application/json",contentType.get());

            String payload=body.get();
            assertNotNull(payload);
            assertTrue(payload.contains("\"resourceSpans\":["));
            assertTrue(payload.contains("\"service.name\""));
            assertTrue(payload.contains("integration-test"));
            assertTrue(payload.contains("\"name\":\"order-request\""));
            assertTrue(payload.contains("\"name\":\"database-query\""));
            assertTrue(payload.contains("\"key\":\"db.system\""));
            assertTrue(payload.contains("\"name\":\"query-executed\""));
        }finally{
            server.stop(0);
        }
    }
}
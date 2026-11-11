package io.opentrace.core.exporter;

import com.sun.net.httpserver.HttpServer;
import io.opentrace.core.OpenTrace;
import io.opentrace.core.processor.ExportStats;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    @Test
    void rejectsHttp400And500Responses()throws Exception{
        for(int statusCode:List.of(400,500)){
            HttpServer server=createRejectingServer(statusCode);
            server.start();
            try{
                String endpoint="http://127.0.0.1:"
                    +server.getAddress().getPort()+"/v1/traces";
                IllegalStateException failure=assertThrows(
                    IllegalStateException.class,
                    ()->new OtlpExporter(endpoint).export(List.of())
                );
                assertEquals(
                    "OTLP export failed with HTTP status "+statusCode,
                    failure.getMessage()
                );
            }finally{
                server.stop(0);
            }
        }
    }

    @Test
    void reportsConnectionFailures()throws Exception{
        int port;
        try(ServerSocket socket=new ServerSocket(0)){
            port=socket.getLocalPort();
        }
        String endpoint="http://127.0.0.1:"+port+"/v1/traces";
        IllegalStateException failure=assertThrows(
            IllegalStateException.class,
            ()->new OtlpExporter(endpoint).export(List.of())
        );
        assertEquals("OTLP export request failed",failure.getMessage());
        assertNotNull(failure.getCause());
    }

    @Test
    void countsHttpRejectionsAsFailedExports()throws Exception{
        HttpServer server=createRejectingServer(500);
        server.start();
        try{
            String endpoint="http://127.0.0.1:"
                +server.getAddress().getPort()+"/v1/traces";
            var tracer=OpenTrace.builder()
                .exporter(new OtlpExporter(endpoint))
                .batchSize(1)
                .build();
            try(var root=tracer.root("rejected-request")){}
            tracer.shutdown();
            ExportStats stats=tracer.getExportStats();
            assertEquals(0,stats.exported());
            assertEquals(1,stats.failed());
            assertEquals(0,stats.dropped());
        }finally{
            server.stop(0);
        }
    }

    private HttpServer createRejectingServer(int statusCode)throws IOException{
        HttpServer server=HttpServer.create(
            new InetSocketAddress("127.0.0.1",0),0
        );
        server.createContext("/v1/traces",exchange->{
            byte[] response="collector rejected trace"
                .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode,response.length);
            try(var output=exchange.getResponseBody()){
                output.write(response);
            }
        });
        return server;
    }
}
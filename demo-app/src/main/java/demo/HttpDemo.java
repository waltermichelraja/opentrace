package demo;

import com.sun.net.httpserver.HttpServer;
import io.opentrace.core.OpenTrace;
import io.opentrace.core.processor.LoggingSpanProcessor;
import io.opentrace.http.TraceHttpClient;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class HttpDemo{

    public static void main(String[] args)throws Exception{
        OpenTrace tracer=OpenTrace.builder()
            .serviceName("http-demo")
            .serviceVersion("1.0.0")
            .environment("dev")
            .build();
        tracer.addProcessor(new LoggingSpanProcessor());
        AtomicReference<String> receivedTraceparent=new AtomicReference<>();
        HttpServer server=HttpServer.create(
            new InetSocketAddress("127.0.0.1",0),
            0
        );
        server.createContext("/hello",exchange->{
            receivedTraceparent.set(
                exchange.getRequestHeaders().getFirst("traceparent")
            );
            byte[] response="hello from local server"
                .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,response.length);
            try(OutputStream output=exchange.getResponseBody()){
                output.write(response);
            }
        });
        server.start();
        try{
            HttpClient client=TraceHttpClient.wrap(
                HttpClient.newHttpClient(),
                tracer
            );
            URI endpoint=URI.create(
                "http://127.0.0.1:"+server.getAddress().getPort()+"/hello"
            );
            // synchronous HTTP request
            try(var root=tracer.root("http-demo.sync")){
                HttpRequest request=HttpRequest.newBuilder()
                    .uri(endpoint)
                    .GET()
                    .build();
                HttpResponse<String> response=client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                System.out.println("Sync status: "+response.statusCode());
                System.out.println("Sync response: "+response.body());
            }
            System.out.println(
                "Received traceparent: "+receivedTraceparent.get()
            );
            // asynchronous HTTP request
            CompletableFuture<HttpResponse<String>> future;
            try(var root=tracer.root("http-demo.async")){
                HttpRequest request=HttpRequest.newBuilder()
                    .uri(endpoint)
                    .GET()
                    .build();
                future=client.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
            }
            HttpResponse<String> response=future.get();
            System.out.println("Async status: "+response.statusCode());
            System.out.println("Async response: "+response.body());
        }finally{
            server.stop(0);
            tracer.shutdown();
        }
    }
}

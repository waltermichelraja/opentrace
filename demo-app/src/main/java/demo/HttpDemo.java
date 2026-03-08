package demo;

import com.sun.net.httpserver.HttpServer;
import io.opentrace.core.OpenTrace;
import io.opentrace.core.instrumentation.http.TraceHttpClient;
import io.opentrace.core.instrumentation.http.TraceHttpRequest;

import java.io.OutputStream;
import java.net.InetSocketAddress;

public class HttpDemo{
    public static void main(String[] args)throws Exception{
        OpenTrace tracer=OpenTrace.builder()
                .serviceName("demo-http")
                .environment("dev")
                .serviceVersion("1.0")
                .build();

        HttpServer server=HttpServer.create(new InetSocketAddress(8080),0);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));

        TraceHttpClient client=new TraceHttpClient(tracer);

        server.createContext("/hello",
            new TraceHttpRequest(tracer,exchange->{
                String response="hello world";
                exchange.sendResponseHeaders(200,response.length());
                try(OutputStream os=exchange.getResponseBody()){
                    os.write(response.getBytes());
                }

            })
        );

        server.createContext("/call",
            new TraceHttpRequest(tracer,exchange->{
                try{
                    int status=client.get("http://localhost:8080/hello");
                    String response="call returned "+status;
                    exchange.sendResponseHeaders(200,response.length());
                    try(OutputStream os=exchange.getResponseBody()){
                        os.write(response.getBytes());
                    }
                }catch(Exception e){
                    exchange.sendResponseHeaders(500,0);
                    exchange.close();
                }
            })
        );

        server.start();

        System.out.println("server running:");
        System.out.println("http://localhost:8080/hello");
        System.out.println("http://localhost:8080/call");

    }
}
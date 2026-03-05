package io.opentrace.core.instrumentation.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.opentrace.core.OpenTrace;

import java.io.IOException;

public final class TraceHttpRequest implements HttpHandler{
    private final HttpHandler delegate;
    private final OpenTrace tracer;

    public TraceHttpRequest(OpenTrace tracer, HttpHandler delegate){
        this.tracer=tracer;
        this.delegate=delegate;
    }

    @Override
    public void handle(HttpExchange exchange)throws IOException{
        String method=exchange.getRequestMethod();
        String path=exchange.getRequestURI().getPath();
        try(var root=tracer.root("HTTP "+method+" "+path)){
            try(var span=tracer.span("http.request")){
                span.setAttribute("http.method", method);
                span.setAttribute("http.path", path);
                delegate.handle(exchange);
                span.setAttribute("http.status_code", exchange.getResponseCode());
            }
        }
    }
}
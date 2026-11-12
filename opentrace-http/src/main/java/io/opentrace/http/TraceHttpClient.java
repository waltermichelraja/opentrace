package io.opentrace.http;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.OpenTraceContext;
import io.opentrace.core.SpanScope;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public final class TraceHttpClient extends HttpClient{

    private final HttpClient delegate;
    private final OpenTrace tracer;

    private TraceHttpClient(HttpClient delegate,OpenTrace tracer){
        this.delegate=Objects.requireNonNull(delegate,"delegate");
        this.tracer=Objects.requireNonNull(tracer,"tracer");
    }

    public static HttpClient wrap(HttpClient client,OpenTrace tracer){
        return new TraceHttpClient(client,tracer);
    }

    @Override
    public Optional<CookieHandler> cookieHandler(){
        return delegate.cookieHandler();
    }

    @Override
    public Optional<Duration> connectTimeout(){
        return delegate.connectTimeout();
    }

    @Override
    public Redirect followRedirects(){
        return delegate.followRedirects();
    }

    @Override
    public Optional<ProxySelector> proxy(){
        return delegate.proxy();
    }

    @Override
    public SSLContext sslContext(){
        return delegate.sslContext();
    }

    @Override
    public SSLParameters sslParameters(){
        return delegate.sslParameters();
    }

    @Override
    public Optional<Authenticator> authenticator(){
        return delegate.authenticator();
    }

    @Override
    public Version version(){
        return delegate.version();
    }

    @Override
    public Optional<Executor> executor(){
        return delegate.executor();
    }

    @Override
    public <T> HttpResponse<T> send(
        HttpRequest request,
        HttpResponse.BodyHandler<T> responseBodyHandler
    )throws IOException,InterruptedException{

        try(SpanScope span=tracer.span("http.client")){
            HttpRequest tracedRequest=injectTraceparent(request);
            return delegate.send(tracedRequest,responseBodyHandler);
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request,
        HttpResponse.BodyHandler<T> responseBodyHandler
    ){
        return delegate.sendAsync(request,responseBodyHandler);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request,
        HttpResponse.BodyHandler<T> responseBodyHandler,
        HttpResponse.PushPromiseHandler<T> pushPromiseHandler
    ){
        return delegate.sendAsync(
            request,
            responseBodyHandler,
            pushPromiseHandler
        );
    }

    private HttpRequest injectTraceparent(HttpRequest request){
        OpenTraceContext context=tracer.currentContext();

        if(context==null){
            return request;
        }

        Map<String,String> headers=tracer.inject(context);
        String traceparent=headers.get("traceparent");

        if(traceparent==null){
            return request;
        }

        return HttpRequest.newBuilder(request,(name,value)->true)
            .setHeader("traceparent",traceparent)
            .build();
    }
}
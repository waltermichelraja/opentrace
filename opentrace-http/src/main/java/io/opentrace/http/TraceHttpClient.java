package io.opentrace.http;

import io.opentrace.core.AsyncSpanScope;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
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
            HttpRequest tracedRequest=injectTraceparent(
                request,
                tracer.currentContext()
            );
            return delegate.send(tracedRequest,responseBodyHandler);
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request,
        HttpResponse.BodyHandler<T> responseBodyHandler
    ){
        return sendAsyncInternal(
            request,
            tracedRequest->delegate.sendAsync(
                tracedRequest,
                responseBodyHandler
            )
        );
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request,
        HttpResponse.BodyHandler<T> responseBodyHandler,
        HttpResponse.PushPromiseHandler<T> pushPromiseHandler
    ){
        return sendAsyncInternal(
            request,
            tracedRequest->delegate.sendAsync(
                tracedRequest,
                responseBodyHandler,
                pushPromiseHandler
            )
        );
    }

    private <T> CompletableFuture<HttpResponse<T>> sendAsyncInternal(
        HttpRequest request,
        Function<HttpRequest,CompletableFuture<HttpResponse<T>>> requestFunction
    ){
        AsyncSpanScope span=tracer.asyncSpan("http.client");
        try{
            HttpRequest tracedRequest=injectTraceparent(
                request,
                span.context()
            );
            CompletableFuture<HttpResponse<T>> source=
                requestFunction.apply(tracedRequest);
            return trackAsync(source,span);
        }catch(RuntimeException|Error error){
            span.error(error);
            span.close();
            throw error;
        }
    }

    private <T> CompletableFuture<HttpResponse<T>> trackAsync(
        CompletableFuture<HttpResponse<T>> source,
        AsyncSpanScope span
    ){
        CompletableFuture<HttpResponse<T>> result=new CompletableFuture<>(){
            @Override
            public boolean cancel(boolean mayInterruptIfRunning){
                boolean cancelled=super.cancel(mayInterruptIfRunning);
                if(cancelled){
                    span.error(
                        new CancellationException("HTTP request was cancelled")
                    );
                    span.close();
                    source.cancel(mayInterruptIfRunning);
                }
                return cancelled;
            }
        };
        source.whenComplete((response,error)->{
            if(error!=null){
                span.error(unwrap(error));
            }
            span.close();
            if(error!=null){
                result.completeExceptionally(error);
            }else{
                result.complete(response);
            }
        });
        return result;
    }

    private Throwable unwrap(Throwable error){
        if((error instanceof CompletionException
            ||error instanceof ExecutionException)
            &&error.getCause()!=null){
            return error.getCause();
        }
        return error;
    }

    private HttpRequest injectTraceparent(
        HttpRequest request,
        OpenTraceContext context
    ){
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

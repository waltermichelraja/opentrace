package io.opentrace.servlet;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.OpenTraceContext;
import io.opentrace.core.RootScope;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class OpenTraceFilter implements Filter{

    private static final String SCOPE_ATTRIBUTE=
        OpenTraceFilter.class.getName()+".ROOT_SCOPE";

    private final OpenTrace tracer;

    public OpenTraceFilter(OpenTrace tracer){
        this.tracer=Objects.requireNonNull(tracer,"tracer");
    }

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain
    )throws IOException,ServletException{
        if(!(request instanceof HttpServletRequest httpRequest)
            ||!(response instanceof HttpServletResponse httpResponse)){
            chain.doFilter(request,response);
            return;
        }
        RootScope existing=(RootScope)httpRequest.getAttribute(SCOPE_ATTRIBUTE);
        if(existing!=null){
            existing.makeCurrent();
            try{
                chain.doFilter(request,response);
            }catch(IOException|ServletException|RuntimeException|Error error){
                existing.error(error);
                throw error;
            }finally{
                existing.detach();
            }
            return;
        }
        OpenTraceContext incoming=tracer.extract(extractHeaders(httpRequest));
        RootScope span=tracer.root("http.server",incoming);
        httpRequest.setAttribute(SCOPE_ATTRIBUTE,span);
        span.setAttribute("http.request.method",httpRequest.getMethod());

        String path=httpRequest.getRequestURI();
        if(path!=null&&!path.isEmpty()){
            span.setAttribute("url.path",path);
        }
        boolean listenerRegistered=false;
        try{
            chain.doFilter(request,response);
        }catch(IOException|ServletException|RuntimeException|Error error){
            span.error(error);
            throw error;
        }finally{
            if(httpRequest.isAsyncStarted()){
                try{
                    AsyncContext asyncContext=httpRequest.getAsyncContext();
                    asyncContext.addListener(
                        new OpenTraceAsyncListener(
                            span,
                            httpRequest,
                            httpResponse
                        ),
                        httpRequest,
                        httpResponse
                    );
                    listenerRegistered=true;
                    span.detach();
                }catch(IllegalStateException error){
                    span.error(error);
                }
            }
            if(!listenerRegistered){
                span.setAttribute(
                    "http.response.status_code",
                    httpResponse.getStatus()
                );
                span.close();
                httpRequest.removeAttribute(SCOPE_ATTRIBUTE);
            }
        }
    }

    private Map<String,String> extractHeaders(HttpServletRequest request){
        Map<String,String> headers=new LinkedHashMap<>();
        Enumeration<String> names=request.getHeaderNames();
        if(names==null){
            return headers;
        }
        for(String name:Collections.list(names)){
            headers.put(name,request.getHeader(name));
        }
        return headers;
    }

    private static final class OpenTraceAsyncListener implements AsyncListener{

        private final RootScope span;
        private final HttpServletRequest request;
        private final HttpServletResponse response;

        private OpenTraceAsyncListener(
            RootScope span,
            HttpServletRequest request,
            HttpServletResponse response
        ){
            this.span=span;
            this.request=request;
            this.response=response;
        }

        @Override
        public void onComplete(AsyncEvent event){
            span.setAttribute(
                "http.response.status_code",
                response.getStatus()
            );
            span.close();
            request.removeAttribute(SCOPE_ATTRIBUTE);
        }

        @Override
        public void onTimeout(AsyncEvent event){
            span.error(new TimeoutException("Servlet async request timed out"));
        }

        @Override
        public void onError(AsyncEvent event){
            Throwable error=event.getThrowable();
            if(error!=null){
                span.error(error);
            }
        }

        @Override
        public void onStartAsync(AsyncEvent event){
            event.getAsyncContext().addListener(this,request,response);
        }
    }
}

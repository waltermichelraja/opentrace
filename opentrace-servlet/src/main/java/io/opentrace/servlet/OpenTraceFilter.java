package io.opentrace.servlet;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.OpenTraceContext;
import io.opentrace.core.RootScope;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class OpenTraceFilter implements Filter{

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

        OpenTraceContext incoming=tracer.extract(extractHeaders(httpRequest));

        try(RootScope span=tracer.root("http.server",incoming)){
            span.setAttribute("http.request.method",httpRequest.getMethod());

            String path=httpRequest.getRequestURI();
            if(path!=null&&!path.isEmpty()){
                span.setAttribute("url.path",path);
            }

            try{
                chain.doFilter(request,response);
            }catch(IOException|ServletException|RuntimeException|Error error){
                span.error(error);
                throw error;
            }finally{
                span.setAttribute(
                    "http.response.status_code",
                    httpResponse.getStatus()
                );
            }
        }
    }

    private Map<String,String> extractHeaders(HttpServletRequest request){
        Map<String,String> headers=new LinkedHashMap<>();
        for(String name:Collections.list(request.getHeaderNames())){
            headers.put(name,request.getHeader(name));
        }
        return headers;
    }
}

package io.opentrace.core.instrumentation.http;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.OpenTraceContext;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public final class TraceHttpClient{
    private final OpenTrace tracer;

    public TraceHttpClient(OpenTrace tracer){
        this.tracer=tracer;
    }

    public int get(String urlStr)throws Exception{
        URL url=new URL(urlStr);
        try(var span=tracer.span("http.client")){
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.url", urlStr);
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            OpenTraceContext ctx=tracer.currentContext();
            Map<String,String> headers=tracer.inject(ctx);
            for(Map.Entry<String,String> e:headers.entrySet()){
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
            int status=conn.getResponseCode();
            span.setAttribute("http.status_code", status);
            try(InputStream is=conn.getInputStream()){
                while(is.read()!=-1){}
            }
            return status;
        }
    }
}
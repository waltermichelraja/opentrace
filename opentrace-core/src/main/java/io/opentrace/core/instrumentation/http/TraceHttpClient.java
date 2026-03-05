package io.opentrace.core.instrumentation.http;

import io.opentrace.core.OpenTrace;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
            int status=conn.getResponseCode();
            span.setAttribute("http.status_code", status);
            try(InputStream is=conn.getInputStream()){
                while(is.read()!=-1){}
            }
            return status;
        }
    }
}
package io.opentrace.core.exporter;

import io.opentrace.core.Trace;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public final class OtlpExporter implements SpanExporter{
    private final String endpoint;

    public OtlpExporter(String endpoint){
        this.endpoint=endpoint;
    }

    @Override
    public void export(List<Trace> traces){
        try{
            String payload=buildPayload(traces);
            URL url=new URL(endpoint);
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try(OutputStream os=conn.getOutputStream()){
                os.write(payload.getBytes());
            }
            conn.getResponseCode();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private String buildPayload(List<Trace> traces){
        StringBuilder sb=new StringBuilder();
        sb.append("{\"resourceSpans\":[");
        boolean first=true;
        for(Trace trace: traces){
            if(!first) sb.append(",");
            first=false;
            sb.append("{");
            sb.append("\"resource\":{");
            sb.append("\"attributes\":[");
            trace.resource.getAttributes().forEach((k,v)->{
                sb.append("{\"key\":\"").append(k).append("\",");
                sb.append("\"value\":{\"stringValue\":\"").append(v).append("\"}},");
            });
            if(sb.charAt(sb.length()-1)==','){sb.deleteCharAt(sb.length()-1);}
            sb.append("]},");
            sb.append("\"scopeSpans\":[]");
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
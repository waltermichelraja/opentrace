package io.opentrace.core.exporter;

import io.opentrace.core.Json;
import io.opentrace.core.Span;
import io.opentrace.core.SpanStatus;
import io.opentrace.core.Trace;
import io.opentrace.core.SpanIds;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
            conn.setRequestProperty("Content-Type","application/json");
            try(OutputStream os=conn.getOutputStream()){
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
            conn.disconnect();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    String buildPayload(List<Trace> traces){
        StringBuilder sb=new StringBuilder();
        sb.append("{\"resourceSpans\":[");

        boolean firstTrace=true;
        for(Trace trace:traces){
            if(!firstTrace){sb.append(",");}
            firstTrace=false;

            sb.append("{\"resource\":{\"attributes\":[");
            boolean firstAttribute=true;
            for(Map.Entry<String,String> attribute:trace.resource.getAttributes().entrySet()){
                if(!firstAttribute){sb.append(",");}
                firstAttribute=false;

                sb.append("{\"key\":").append(Json.quote(attribute.getKey()));
                sb.append(",\"value\":{\"stringValue\":")
                    .append(Json.quote(attribute.getValue())).append("}}");
            }

            sb.append("]},\"scopeSpans\":[");
            sb.append("{\"scope\":{\"name\":\"opentrace\"},\"spans\":[");

            boolean firstSpan=true;
            for(Span span:trace.spans){
                if(!firstSpan){sb.append(",");}
                firstSpan=false;
                appendSpan(sb,trace,span);
            }

            sb.append("]}]}");
        }

        sb.append("]}");
        return sb.toString();
    }

    private void appendSpan(StringBuilder sb,Trace trace,Span span){
        sb.append("{\"traceId\":").append(Json.quote(trace.traceId.toHex()));
        sb.append(",\"spanId\":").append(Json.quote(SpanIds.toHex(span.getSpanId())));

        if(span.getParentSpanId()!=0){
            sb.append(",\"parentSpanId\":")
                .append(Json.quote(SpanIds.toHex(span.getParentSpanId())));
        }

        sb.append(",\"name\":").append(Json.quote(trace.resolveName(span.getNameId())));
        sb.append(",\"kind\":1");
        sb.append(",\"startTimeUnixNano\":")
            .append(Json.quote(Long.toString(span.getStartEpochNanos())));
        sb.append(",\"endTimeUnixNano\":")
            .append(Json.quote(Long.toString(span.getEndEpochNanos())));
        sb.append(",\"status\":{\"code\":").append(statusCode(span.getStatus()));

        if(span.getStatus()==SpanStatus.ERROR && span.getErrorMessage()!=null){
            sb.append(",\"message\":").append(Json.quote(span.getErrorMessage()));
        }

        sb.append("}}");
    }

    private int statusCode(SpanStatus status){
        return switch(status){
            case UNSET->0;
            case OK->1;
            case ERROR->2;
        };
    }
}
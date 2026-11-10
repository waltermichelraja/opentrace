package io.opentrace.core.exporter;

import io.opentrace.core.Json;
import io.opentrace.core.Span;
import io.opentrace.core.SpanEvent;
import io.opentrace.core.SpanIds;
import io.opentrace.core.SpanLink;
import io.opentrace.core.SpanStatus;
import io.opentrace.core.Trace;

import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigInteger;
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

        sb.append("}");

        appendAttributes(sb,span.getAttributes());
        appendEvents(sb,span.getEvents());
        appendLinks(sb,span.getLinks());

        sb.append("}");
    }

    private void appendAttributes(StringBuilder sb,Map<String,Object> attributes){
        if(attributes.isEmpty()){return;}

        sb.append(",\"attributes\":[");
        boolean first=true;

        for(Map.Entry<String,Object> attribute:attributes.entrySet()){
            if(!first){sb.append(",");}
            first=false;

            sb.append("{\"key\":").append(Json.quote(attribute.getKey()));
            sb.append(",\"value\":");
            appendAnyValue(sb,attribute.getValue());
            sb.append("}");
        }

        sb.append("]");
    }

    private void appendAnyValue(StringBuilder sb,Object value){
        if(value==null){
            sb.append("{\"nullValue\":\"NULL_VALUE\"}");
        }else if(value instanceof String || value instanceof Character){
            sb.append("{\"stringValue\":")
                .append(Json.quote(value.toString())).append("}");
        }else if(value instanceof Boolean){
            sb.append("{\"boolValue\":").append(value).append("}");
        }else if(value instanceof Byte || value instanceof Short
            || value instanceof Integer || value instanceof Long
            || value instanceof BigInteger){
            sb.append("{\"intValue\":")
                .append(Json.quote(value.toString())).append("}");
        }else if(value instanceof Number){
            double number=((Number)value).doubleValue();
            sb.append("{\"doubleValue\":");
            if(Double.isNaN(number)){
                sb.append(Json.quote("NaN"));
            }else if(number==Double.POSITIVE_INFINITY){
                sb.append(Json.quote("Infinity"));
            }else if(number==Double.NEGATIVE_INFINITY){
                sb.append(Json.quote("-Infinity"));
            }else{
                sb.append(number);
            }
            sb.append("}");
        }else if(value instanceof Map<?,?> map){
            sb.append("{\"kvlistValue\":{\"values\":[");
            boolean first=true;

            for(Map.Entry<?,?> entry:map.entrySet()){
                if(!first){sb.append(",");}
                first=false;

                sb.append("{\"key\":")
                    .append(Json.quote(String.valueOf(entry.getKey())));
                sb.append(",\"value\":");
                appendAnyValue(sb,entry.getValue());
                sb.append("}");
            }

            sb.append("]}}");
        }else if(value instanceof Iterable<?> iterable){
            sb.append("{\"arrayValue\":{\"values\":[");
            boolean first=true;

            for(Object item:iterable){
                if(!first){sb.append(",");}
                first=false;
                appendAnyValue(sb,item);
            }

            sb.append("]}}");
        }else if(value.getClass().isArray()){
            sb.append("{\"arrayValue\":{\"values\":[");
            int length=Array.getLength(value);

            for(int i=0;i<length;i++){
                if(i>0){sb.append(",");}
                appendAnyValue(sb,Array.get(value,i));
            }

            sb.append("]}}");
        }else{
            sb.append("{\"stringValue\":")
                .append(Json.quote(value.toString())).append("}");
        }
    }

    private void appendEvents(StringBuilder sb,List<SpanEvent> events){
        synchronized(events){
            if(events.isEmpty()){return;}

            sb.append(",\"events\":[");
            boolean first=true;

            for(SpanEvent event:events){
                if(!first){sb.append(",");}
                first=false;

                sb.append("{\"timeUnixNano\":")
                    .append(Json.quote(Long.toString(event.getTimestamp())));
                sb.append(",\"name\":").append(Json.quote(event.getName()));
                sb.append(",\"attributes\":[]}");
            }

            sb.append("]");
        }
    }

    private void appendLinks(StringBuilder sb,List<SpanLink> links){
        synchronized(links){
            if(links.isEmpty()){return;}

            sb.append(",\"links\":[");
            boolean first=true;

            for(SpanLink link:links){
                if(!first){sb.append(",");}
                first=false;

                sb.append("{\"traceId\":")
                    .append(Json.quote(link.getTraceId().toHex()));
                sb.append(",\"spanId\":")
                    .append(Json.quote(SpanIds.toHex(link.getSpanId())));
                sb.append(",\"attributes\":[]}");
            }

            sb.append("]");
        }
    }

    private int statusCode(SpanStatus status){
        return switch(status){
            case UNSET->0;
            case OK->1;
            case ERROR->2;
        };
    }
}
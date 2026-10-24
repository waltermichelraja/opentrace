package io.opentrace.core;

import io.opentrace.core.exporter.ConsoleExporter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonOutputTest{
    private static Trace traceWithNastyStrings(){
        var tracer=tracer();
        try(var r=tracer.root("root \"quoted\"")){
            try(var s=tracer.span("db\\query")){
                s.setAttribute("sql","SELECT \"x\" FROM t\nWHERE a='b'");
                s.error(new RuntimeException("line1\nline2 \"q\""));
                s.addEvent("evt");
                s.addLink(1,2);
            }
        }
        return only(tracer);
    }

    @Test
    void toJsonEscapesQuotesBackslashesAndNewlines(){
        String json=traceWithNastyStrings().toJson();
        assertTrue(json.contains("root \\\"quoted\\\""),json);
        assertTrue(json.contains("db\\\\query"),json);
        assertTrue(json.contains("\\n"),json);
        assertFalse(json.contains("\n"),"raw newline leaked into JSON");
    }

    @Test
    void toJsonIsStructurallyBalanced(){
        String json=traceWithNastyStrings().toJson();
        assertTrue(isBalanced(json),json);
    }

    @Test
    void toJsonIncludesEventsAndLinks(){
        String json=traceWithNastyStrings().toJson();
        assertTrue(json.contains("\"events\""),json);
        assertTrue(json.contains("\"links\""),json);
    }

    @Test
    void consoleExporterOutputMatchesToJsonAndIsBalanced(){
        Trace t=traceWithNastyStrings();
        var buf=new ByteArrayOutputStream();
        PrintStream old=System.out;
        System.setOut(new PrintStream(buf));
        try{new ConsoleExporter().export(java.util.List.of(t));}
        finally{System.setOut(old);}
        String out=buf.toString().trim();
        assertTrue(isBalanced(out),out);
        assertFalse(out.contains("\n"),"console JSON should be one line");
    }

    @Test
    void metricsJsonEscapesNames(){
        var tracer=tracer();
        try(var r=tracer.root("a\"b")){}
        assertTrue(isBalanced(tracer.getMetrics().toString()));
        assertTrue(tracer.getMetrics().toString().contains("a\\\"b"));
    }

    @Test
    void collectionJsonIsBalanced(){
        var tracer=tracer();
        for(int i=0;i<3;i++){try(var r=tracer.root("r\"")){}}
        assertTrue(isBalanced(tracer.getTraces().toString()));
    }

    @Test
    void continuedRemoteTraceStillHasARoot(){
        var server=tracer();
        try(var root=server.root("HTTP GET /x",new OpenTraceContext(777,42,java.util.Map.of()))){
            try(var s=server.span("http.request")){}
        }
        Trace t=only(server);
        assertNotNull(t.getRoot());
        assertEquals("HTTP GET /x",t.resolveName(t.getRoot().getNameId()));
        assertTrue(t.toJson().contains("\"name\":\"HTTP GET /x\""),t.toJson());
        assertTrue(t.toJson().contains("http.request"),t.toJson());
    }

    @Test
    void loggingProcessorPrintsContinuedRemoteTraces(){
        var server=tracer();
        var buf=new ByteArrayOutputStream();
        PrintStream old=System.out;
        System.setOut(new PrintStream(buf));
        try{
            server.addProcessor(new io.opentrace.core.processor.LoggingSpanProcessor());
            try(var root=server.root("HTTP GET /x",new OpenTraceContext(5,9,java.util.Map.of()))){
                try(var s=server.span("child")){}
            }
        }finally{System.setOut(old);}
        assertTrue(buf.toString().contains("TRACE HTTP GET /x"),buf.toString());
        assertTrue(buf.toString().contains("child"),buf.toString());
    }

    private static boolean isBalanced(String s){
        int brace=0,bracket=0;boolean inStr=false;
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            if(inStr){
                if(c=='\\'){i++;}
                else if(c=='"'){inStr=false;}
                continue;
            }
            switch(c){
                case '"'->inStr=true;
                case '{'->brace++;
                case '}'->brace--;
                case '['->bracket++;
                case ']'->bracket--;
                default->{}
            }
            if(brace<0||bracket<0){return false;}
        }
        return !inStr && brace==0 && bracket==0;
    }
}
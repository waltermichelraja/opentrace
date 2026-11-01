package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.opentrace.core.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class NameRegistryTest{
    @Test
    void sameNameGetsTheSameIdAndResolvesBack(){
        var r=new NameRegistry(10);
        int a=r.id("alpha");
        assertEquals(a,r.id("alpha"));
        assertNotEquals(a,r.id("beta"));
        assertEquals("alpha",r.name(a));
    }

    @Test
    void namesBeyondTheCapShareOneOverflowEntry(){
        var r=new NameRegistry(3);
        int a=r.id("a"),b=r.id("b"),c=r.id("c");
        int d=r.id("d"),e=r.id("e");
        assertEquals(3,List.of(a,b,c).stream().distinct().count());
        assertEquals(d,e);
        assertEquals("_other",r.name(d));
        assertNotEquals(a,d);
    }

    @Test
    void knownNamesKeepResolvingAfterTheCapIsReached(){
        var r=new NameRegistry(2);
        int a=r.id("a"),b=r.id("b");
        r.id("c");r.id("d");
        assertEquals(a,r.id("a"));
        assertEquals("a",r.name(a));
        assertEquals("b",r.name(b));
    }

    @Test
    void warnsExactlyOnceWhenTheCapIsHit(){
        var captured=new ByteArrayOutputStream();
        PrintStream old=System.err;
        System.setErr(new PrintStream(captured));
        try{
            var r=new NameRegistry(2);
            for(int i=0;i<100;i++){r.id("n"+i);}
        }finally{System.setErr(old);}
        long warnings=captured.toString().lines().filter(l->l.contains("distinct span names")).count();
        assertEquals(1,warnings);
    }

    @Test
    void invalidCapIsRejected(){
        assertThrows(IllegalArgumentException.class,()->new NameRegistry(0));
        assertThrows(IllegalArgumentException.class,()->OpenTrace.builder().maxSpanNames(0));
    }

    @Test
    void tracerFoldsExcessNamesIntoOther(){
        var tracer=OpenTrace.builder().disableExporter().maxSpanNames(2).build();
        try(var root=tracer.root("a")){
            try(var b=tracer.span("b")){}
            try(var c=tracer.span("c")){}
            try(var d=tracer.span("d")){}
        }
        Trace t=only(tracer);
        List<String> names=new ArrayList<>();
        for(Span s: t.spans){names.add(t.resolveName(s.getNameId()));}
        assertTrue(names.contains("a"));
        assertTrue(names.contains("b"));
        assertEquals(2,names.stream().filter("_other"::equals).count());
    }

    @Test
    void concurrentRegistrationStaysNearTheCap() throws Exception{
        var r=new NameRegistry(1000);
        Set<Integer> ids=ConcurrentHashMap.newKeySet();
        List<Thread> threads=new ArrayList<>();
        for(int t=0;t<8;t++){
            final int tid=t;
            Thread th=new Thread(()->{
                for(int i=0;i<5000;i++){ids.add(r.id("t"+tid+"-"+i));}
            });
            threads.add(th);th.start();
        }
        for(Thread th: threads){th.join();}
        assertTrue(ids.size()<=1000+8+1,"distinct ids="+ids.size());
        for(int id: ids){assertNotNull(r.name(id));}
    }
}
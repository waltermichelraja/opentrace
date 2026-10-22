package io.opentrace.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpanConcurrencyTest{
    @Test
    void freshSpanHasEmptyNonNullAttributes(){
        assertNotNull(new Span().getAttributes());
        assertTrue(new Span().getAttributes().isEmpty());
    }

    @Test
    void attributesSnapshotIsReadOnly(){
        Span s=new Span();
        s.putAttribute("a",1);
        assertThrows(UnsupportedOperationException.class,()->s.getAttributes().put("b",2));
    }

    @Test
    void concurrentAttributeWritesAreNotLost() throws Exception{
        Span s=new Span();
        List<Thread> threads=new ArrayList<>();
        for(int t=0;t<8;t++){
            final int id=t;
            Thread th=new Thread(()->{for(int i=0;i<500;i++){s.putAttribute("k"+id+"-"+i,i);}});
            threads.add(th);th.start();
        }
        for(Thread th:threads){th.join();}
        assertEquals(4000,s.getAttributes().size());
    }

    @Test
    void concurrentLinksAndEventsAreNotLost() throws Exception{
        Span s=new Span();
        List<Thread> threads=new ArrayList<>();
        for(int t=0;t<8;t++){
            Thread th=new Thread(()->{for(int i=0;i<250;i++){s.addLink(1,i);s.addEvent("e");}});
            threads.add(th);th.start();
        }
        for(Thread th:threads){th.join();}
        assertEquals(2000,s.getLinks().size());
        assertEquals(2000,s.getEvents().size());
    }
}
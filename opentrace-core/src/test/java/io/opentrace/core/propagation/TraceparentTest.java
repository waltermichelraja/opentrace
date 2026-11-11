package io.opentrace.core.propagation;

import io.opentrace.core.TraceId;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TraceparentTest{
    private static final String TRACE="4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN="00f067aa0ba902b7";
    private static final String SAMPLED="00-"+TRACE+"-"+SPAN+"-01";

    @Test
    void parsesTheW3CExample(){
        Traceparent tp=Traceparent.tryParse(SAMPLED);
        assertNotNull(tp);
        assertEquals(TRACE,tp.traceId().toHex());
        assertEquals(0x00f067aa0ba902b7L,tp.parentSpanId());
        assertEquals(1,tp.flags());
        assertTrue(tp.isSampled());
    }

    @Test
    void sampledIsBitZeroOfTheFlagsAndOtherBitsAreIgnored(){
        assertFalse(Traceparent.tryParse("00-"+TRACE+"-"+SPAN+"-00").isSampled());
        assertTrue(Traceparent.tryParse("00-"+TRACE+"-"+SPAN+"-ff").isSampled());
        assertFalse(Traceparent.tryParse("00-"+TRACE+"-"+SPAN+"-fe").isSampled());
        assertEquals(0xfe,Traceparent.tryParse("00-"+TRACE+"-"+SPAN+"-fe").flags());
    }

    @Test
    void formatsAsVersionZeroWithLowercaseHex(){
        var id=TraceId.fromHex(TRACE);
        assertEquals(SAMPLED,Traceparent.of(id,0x00f067aa0ba902b7L,true).toHeader());
        assertEquals("00-"+TRACE+"-"+SPAN+"-00",Traceparent.of(id,0x00f067aa0ba902b7L,false).toHeader());
    }

    @Test
    void formatThenParseRoundTrips(){
        var id=new TraceId(0xFEDCBA9876543210L,0x0123456789ABCDEFL);
        var original=Traceparent.of(id,-42L,true);
        var parsed=Traceparent.tryParse(original.toHeader());
        assertEquals(original,parsed);
        assertEquals(id,parsed.traceId());
        assertEquals(-42L,parsed.parentSpanId());
    }

    @Test
    void malformedHeadersAreRejected(){
        String[] bad={
            null,
            "",
            "00-"+TRACE+"-"+SPAN,                                   // flags missing
            "00-"+TRACE+"-"+SPAN+"-0",                              // flags too short
            "ff-"+TRACE+"-"+SPAN+"-01",                             // version ff is reserved
            "0A-"+TRACE+"-"+SPAN+"-01",                             // uppercase version
            "zz-"+TRACE+"-"+SPAN+"-01",                             // version not hex
            "00-4BF92F3577B34DA6A3CE929D0E0E4736-"+SPAN+"-01",      // uppercase trace id
            "00-"+TRACE+"-00F067AA0BA902B7-01",                     // uppercase span id
            "00-"+TRACE+"-"+SPAN+"-0A",                             // uppercase flags
            "00-"+TRACE+"-"+SPAN+"-zz",                             // flags not hex
            "00-00000000000000000000000000000000-"+SPAN+"-01",      // all-zero trace id
            "00-"+TRACE+"-0000000000000000-01",                     // all-zero span id
            "00_"+TRACE+"-"+SPAN+"-01",                             // wrong separators
            "00-"+TRACE+"_"+SPAN+"-01",
            "00-"+TRACE+"-"+SPAN+"_01",
            "00-"+TRACE.substring(1)+"-"+SPAN+"-01",                // trace id too short
            "00-"+TRACE+"0-"+SPAN+"-01",                            // trace id too long
            "00-"+TRACE+"-"+SPAN+"-01-extra",                       // version 00 must be exactly 55 chars
            SAMPLED+" ",                                            // trailing whitespace
            " "+SAMPLED                                             // leading whitespace
        };
        for(String s: bad){
            assertNull(Traceparent.tryParse(s),"should be rejected: ["+s+"]");
        }
    }

    @Test
    void futureVersionsAreParsedByTheirFirstFourFields(){
        assertNotNull(Traceparent.tryParse("01-"+TRACE+"-"+SPAN+"-01"));
        assertNotNull(Traceparent.tryParse("cc-"+TRACE+"-"+SPAN+"-01-and-more-fields"));
        assertEquals(TRACE,Traceparent.tryParse("cc-"+TRACE+"-"+SPAN+"-01-x").traceId().toHex());
        assertNull(Traceparent.tryParse("01-"+TRACE+"-"+SPAN+"-01extra"));   // extra data must start with '-'
        assertNull(Traceparent.tryParse("ff-"+TRACE+"-"+SPAN+"-01-x"));
    }

    @Test
    void ofRejectsInvalidInputs(){
        var id=TraceId.fromHex(TRACE);
        assertThrows(IllegalArgumentException.class,()->Traceparent.of(TraceId.INVALID,1L,true));
        assertThrows(IllegalArgumentException.class,()->Traceparent.of(null,1L,true));
        assertThrows(IllegalArgumentException.class,()->Traceparent.of(id,0L,true));
    }

    @Test
    void equalsHashCodeAndToString(){
        var id=TraceId.fromHex(TRACE);
        var a=Traceparent.of(id,5L,true);
        var b=Traceparent.of(id,5L,true);
        assertEquals(a,b);
        assertEquals(a.hashCode(),b.hashCode());
        assertNotEquals(a,Traceparent.of(id,5L,false));
        assertNotEquals(a,Traceparent.of(id,6L,true));
        Set<Traceparent> set=new HashSet<>();
        set.add(a);set.add(b);
        assertEquals(1,set.size());
        assertEquals(a.toHeader(),a.toString());
    }
}
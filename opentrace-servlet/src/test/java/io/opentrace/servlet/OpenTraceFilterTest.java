package io.opentrace.servlet;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.Span;
import io.opentrace.core.SpanStatus;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OpenTraceFilterTest{

    private HttpServletRequest request(){
        HttpServletRequest request=mock(HttpServletRequest.class);
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        when(request.isAsyncStarted()).thenReturn(false);
        return request;
    }

    private HttpServletResponse response(){
        HttpServletResponse response=mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);
        return response;
    }

    @Test
    void recordsSynchronousRequest(){
        OpenTrace tracer=OpenTrace.builder().build();
        OpenTraceFilter filter=new OpenTraceFilter(tracer);
        HttpServletRequest request=request();
        HttpServletResponse response=response();

        assertDoesNotThrow(()->filter.doFilter(
            request,
            response,
            (req,res)->{}
        ));

        assertEquals(1,tracer.getTraceList().size());

        Span root=tracer.getTraceList().get(0).getRoot();
        assertNotNull(root);
        assertEquals("http.server",tracer.getTraceList().get(0).resolveName(root.getNameId()));
        assertEquals("GET",root.getAttributes().get("http.request.method"));
        assertEquals("/test",root.getAttributes().get("url.path"));
        assertEquals(200,root.getAttributes().get("http.response.status_code"));
assertEquals(SpanStatus.UNSET,root.getStatus());
    }

    @Test
    void recordsThrownServletExceptionAndRethrowsIt(){
        OpenTrace tracer=OpenTrace.builder().build();
        OpenTraceFilter filter=new OpenTraceFilter(tracer);
        HttpServletRequest request=request();
        HttpServletResponse response=response();
        ServletException expected=new ServletException("request failed");

        ServletException actual=assertThrows(
            ServletException.class,
            ()->filter.doFilter(
                request,
                response,
                (req,res)->{throw expected;}
            )
        );

        assertSame(expected,actual);
        assertEquals(1,tracer.getTraceList().size());

        Span root=tracer.getTraceList().get(0).getRoot();
        assertEquals(SpanStatus.ERROR,root.getStatus());
        assertEquals("ServletException",root.getErrorType());
    }

    @Test
    void keepsAsyncRequestOpenUntilCompletion()throws Exception{
        OpenTrace tracer=OpenTrace.builder().build();
        OpenTraceFilter filter=new OpenTraceFilter(tracer);
        HttpServletRequest request=request();
        HttpServletResponse response=response();
        AsyncContext asyncContext=mock(AsyncContext.class);

        when(request.isAsyncStarted()).thenReturn(true);
        when(request.getAsyncContext()).thenReturn(asyncContext);

        filter.doFilter(request,response,(req,res)->{});

        ArgumentCaptor<AsyncListener> listenerCaptor=
            ArgumentCaptor.forClass(AsyncListener.class);

        verify(asyncContext).addListener(
            listenerCaptor.capture(),
            eq(request),
            eq(response)
        );

        assertTrue(tracer.getTraceList().isEmpty());

        AsyncListener listener=listenerCaptor.getValue();
        listener.onComplete(new AsyncEvent(asyncContext,request,response));

        assertEquals(1,tracer.getTraceList().size());

        Span root=tracer.getTraceList().get(0).getRoot();
        assertEquals(200,root.getAttributes().get("http.response.status_code"));
        assertEquals(SpanStatus.UNSET,root.getStatus());
    }

    @Test
    void recordsAsyncTimeoutAsError()throws Exception{
        OpenTrace tracer=OpenTrace.builder().build();
        OpenTraceFilter filter=new OpenTraceFilter(tracer);
        HttpServletRequest request=request();
        HttpServletResponse response=response();
        AsyncContext asyncContext=mock(AsyncContext.class);

        when(request.isAsyncStarted()).thenReturn(true);
        when(request.getAsyncContext()).thenReturn(asyncContext);

        filter.doFilter(request,response,(req,res)->{});

        ArgumentCaptor<AsyncListener> listenerCaptor=
            ArgumentCaptor.forClass(AsyncListener.class);

        verify(asyncContext).addListener(
            listenerCaptor.capture(),
            eq(request),
            eq(response)
        );

        AsyncListener listener=listenerCaptor.getValue();
        listener.onTimeout(new AsyncEvent(asyncContext,request,response));
        listener.onComplete(new AsyncEvent(asyncContext,request,response));

        assertEquals(1,tracer.getTraceList().size());

        Span root=tracer.getTraceList().get(0).getRoot();
        assertEquals(SpanStatus.ERROR,root.getStatus());
        assertEquals("TimeoutException",root.getErrorType());
    }
}

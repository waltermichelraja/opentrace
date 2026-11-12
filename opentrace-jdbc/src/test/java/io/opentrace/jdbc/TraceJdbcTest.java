package io.opentrace.jdbc;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.Span;
import io.opentrace.core.SpanStatus;
import io.opentrace.core.Trace;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TraceJdbcTest{

    @Test
    void getConnectionReturnsDelegatedConnectionWrapper()throws Exception{
        AtomicInteger connectionRequests=new AtomicInteger();
        AtomicInteger closeCalls=new AtomicInteger();

        Connection original=proxy(Connection.class,(method,args)->{
            if(method.getName().equals("close")){
                closeCalls.incrementAndGet();
                return null;
            }
            if(method.getName().equals("isClosed")){return false;}
            return defaultValue(method.getReturnType());
        });

        DataSource originalDataSource=dataSource((method,args)->{
            if(method.getName().equals("getConnection")){
                connectionRequests.incrementAndGet();
                return original;
            }
            return defaultValue(method.getReturnType());
        });

        DataSource traced=TraceJdbc.wrap(originalDataSource,tracer());
        Connection connection=traced.getConnection();

        assertNotNull(connection);
        assertNotSame(original,connection);
        assertEquals(1,connectionRequests.get());

        connection.close();

        assertEquals(1,closeCalls.get());
        assertFalse(connection.isClosed());
    }

    @Test
    void getConnectionPreservesOriginalSqlException()throws Exception{
        SQLException expected=new SQLException("Connection failed","08001");

        DataSource original=dataSource((method,args)->{
            if(method.getName().equals("getConnection")){throw expected;}
            return defaultValue(method.getReturnType());
        });

        DataSource traced=TraceJdbc.wrap(original,tracer());

        SQLException actual=assertThrows(SQLException.class,traced::getConnection);

        assertSame(expected,actual);
        assertEquals("08001",actual.getSQLState());
    }

    @Test
    void statementExecutionCreatesSpan()throws Exception{
        AtomicInteger executions=new AtomicInteger();

        Statement statement=proxy(Statement.class,(method,args)->{
            if(method.getName().equals("executeQuery")){
                executions.incrementAndGet();
                return null;
            }
            return defaultValue(method.getReturnType());
        });

        OpenTrace tracer=tracer();
        DataSource traced=tracedDataSource(tracer,statement);

        try(var root=tracer.root("test")){
            traced.getConnection().createStatement().executeQuery("SELECT 1");
        }

        assertEquals(1,executions.get());

        Trace trace=onlyTrace(tracer);
        assertNotNull(findSpan(trace,"db.query"));
        assertEquals(2,trace.spans.size());
    }

    @Test
    void preparedAndCallableStatementsAreWrapped()throws Exception{
        AtomicInteger preparedExecutions=new AtomicInteger();
        AtomicInteger callableExecutions=new AtomicInteger();

        PreparedStatement prepared=proxy(PreparedStatement.class,(method,args)->{
            if(method.getName().equals("executeUpdate")){
                preparedExecutions.incrementAndGet();
                return 1;
            }
            return defaultValue(method.getReturnType());
        });

        CallableStatement callable=proxy(CallableStatement.class,(method,args)->{
            if(method.getName().equals("execute")){
                callableExecutions.incrementAndGet();
                return true;
            }
            return defaultValue(method.getReturnType());
        });

        Connection connection=proxy(Connection.class,(method,args)->{
            if(method.getName().equals("prepareStatement")){return prepared;}
            if(method.getName().equals("prepareCall")){return callable;}
            return defaultValue(method.getReturnType());
        });

        DataSource source=dataSource((method,args)->{
            if(method.getName().equals("getConnection")){return connection;}
            return defaultValue(method.getReturnType());
        });

        OpenTrace tracer=tracer();
        DataSource traced=TraceJdbc.wrap(source,tracer);

        try(var root=tracer.root("test")){
            traced.getConnection().prepareStatement("UPDATE items SET active=true").executeUpdate();
            traced.getConnection().prepareCall("{call refresh_items()}").execute();
        }

        assertEquals(1,preparedExecutions.get());
        assertEquals(1,callableExecutions.get());

        Trace trace=onlyTrace(tracer);
        assertEquals(3,trace.spans.size());
        assertEquals(2,trace.spans.stream()
            .filter(span->trace.resolveName(span.getNameId()).equals("db.query"))
            .count());
    }

    @Test
    void batchExecutionCreatesSpan()throws Exception{
        AtomicInteger batchExecutions=new AtomicInteger();

        Statement statement=proxy(Statement.class,(method,args)->{
            if(method.getName().equals("executeBatch")){
                batchExecutions.incrementAndGet();
                return new int[]{1,2};
            }
            return defaultValue(method.getReturnType());
        });

        OpenTrace tracer=tracer();
        DataSource traced=tracedDataSource(tracer,statement);

        try(var root=tracer.root("test")){
            int[] result=traced.getConnection().createStatement().executeBatch();
            assertArrayEquals(new int[]{1,2},result);
        }

        assertEquals(1,batchExecutions.get());

        Trace trace=onlyTrace(tracer);
        assertNotNull(findSpan(trace,"db.query"));
        assertEquals(2,trace.spans.size());
    }

    @Test
    void failedExecutionPreservesOriginalSQLExceptionAndMarksSpanError()throws Exception{
        SQLException expected=new SQLException("Query failed","42000");

        Statement statement=proxy(Statement.class,(method,args)->{
            if(method.getName().equals("executeUpdate")){throw expected;}
            return defaultValue(method.getReturnType());
        });

        OpenTrace tracer=tracer();
        DataSource traced=tracedDataSource(tracer,statement);

        try(var root=tracer.root("test")){
            SQLException actual=assertThrows(
                SQLException.class,
                ()->traced.getConnection().createStatement().executeUpdate("INVALID SQL")
            );
            assertSame(expected,actual);
        }

        Trace trace=onlyTrace(tracer);
        Span span=findSpan(trace,"db.query");

        assertEquals(SpanStatus.ERROR,span.getStatus());
        assertEquals("SQLException",span.getErrorType());
    }

    private static OpenTrace tracer(){
        return OpenTrace.builder().build();
    }

    private static Trace onlyTrace(OpenTrace tracer){
        assertEquals(1,tracer.getTraceList().size());
        return tracer.getTraceList().get(0);
    }

    private static Span findSpan(Trace trace,String name){
        return trace.spans.stream()
            .filter(span->trace.resolveName(span.getNameId()).equals(name))
            .findFirst()
            .orElseThrow();
    }

    private static DataSource tracedDataSource(OpenTrace tracer,Statement statement){
        Connection connection=proxy(Connection.class,(method,args)->{
            if(method.getName().equals("createStatement")){return statement;}
            return defaultValue(method.getReturnType());
        });

        DataSource source=dataSource((method,args)->{
            if(method.getName().equals("getConnection")){return connection;}
            return defaultValue(method.getReturnType());
        });

        return TraceJdbc.wrap(source,tracer);
    }

    private static DataSource dataSource(Invocation invocation){
        return proxy(DataSource.class,invocation);
    }

    private static <T>T proxy(Class<T> type,Invocation invocation){
        return type.cast(Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            (proxy,method,args)->invocation.call(method,args)
        ));
    }

    private static Object defaultValue(Class<?> type){
        if(!type.isPrimitive()){return null;}
        if(type==boolean.class){return false;}
        if(type==char.class){return '\0';}
        if(type==byte.class){return (byte)0;}
        if(type==short.class){return (short)0;}
        if(type==int.class){return 0;}
        if(type==long.class){return 0L;}
        if(type==float.class){return 0F;}
        if(type==double.class){return 0D;}
        return null;
    }

    @FunctionalInterface
    private interface Invocation{
        Object call(Method method,Object[] args)throws Throwable;
    }
}

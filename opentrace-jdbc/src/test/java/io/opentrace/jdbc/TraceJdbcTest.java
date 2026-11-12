package io.opentrace.jdbc;

import io.opentrace.core.OpenTrace;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TraceJdbcTest{

    @Test
    void getConnectionReturnsDelegatedConnectionWrapper()throws Exception{
        AtomicInteger connectionRequests=new AtomicInteger();
        AtomicInteger closeCalls=new AtomicInteger();

        Connection original=(Connection)Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy,method,args)->{
                if(method.getName().equals("close")){
                    closeCalls.incrementAndGet();
                    return null;
                }
                if(method.getName().equals("isClosed")){
                    return false;
                }
                return defaultValue(method.getReturnType());
            }
        );

        DataSource originalDataSource=(DataSource)Proxy.newProxyInstance(
            DataSource.class.getClassLoader(),
            new Class<?>[]{DataSource.class},
            (proxy,method,args)->{
                if(method.getName().equals("getConnection")){
                    connectionRequests.incrementAndGet();
                    return original;
                }
                return defaultValue(method.getReturnType());
            }
        );

        OpenTrace tracer=OpenTrace.builder().build();
        DataSource traced=TraceJdbc.wrap(originalDataSource,tracer);

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

        DataSource original=(DataSource)Proxy.newProxyInstance(
            DataSource.class.getClassLoader(),
            new Class<?>[]{DataSource.class},
            (proxy,method,args)->{
                if(method.getName().equals("getConnection")){
                    throw expected;
                }
                return defaultValue(method.getReturnType());
            }
        );

        DataSource traced=TraceJdbc.wrap(original,OpenTrace.builder().build());

        SQLException actual=assertThrows(SQLException.class,traced::getConnection);

        assertSame(expected,actual);
        assertEquals("08001",actual.getSQLState());
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
}

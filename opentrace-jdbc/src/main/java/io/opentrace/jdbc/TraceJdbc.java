package io.opentrace.jdbc;

import io.opentrace.core.OpenTrace;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Objects;

public final class TraceJdbc{

    private TraceJdbc(){}

    public static DataSource wrap(DataSource dataSource,OpenTrace tracer){
        Objects.requireNonNull(dataSource,"dataSource");
        Objects.requireNonNull(tracer,"tracer");

        InvocationHandler handler=(proxy,method,args)->{
            Object result=invoke(dataSource,method,args);

            if(method.getName().equals("getConnection")&&result instanceof Connection){
                return wrapConnection((Connection)result);
            }

            return result;
        };

        return (DataSource)Proxy.newProxyInstance(
            DataSource.class.getClassLoader(),
            new Class<?>[]{DataSource.class},
            handler
        );
    }

    private static Connection wrapConnection(Connection connection){
        InvocationHandler handler=(proxy,method,args)->invoke(connection,method,args);

        return (Connection)Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            handler
        );
    }

    private static Object invoke(Object target,Method method,Object[] args)throws Throwable{
        try{
            return method.invoke(target,args);
        }catch(InvocationTargetException e){
            throw e.getCause();
        }
    }
}

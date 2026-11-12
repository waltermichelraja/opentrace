package io.opentrace.jdbc;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.SpanScope;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;

public final class TraceJdbc{

    private TraceJdbc(){}

    public static DataSource wrap(DataSource dataSource,OpenTrace tracer){
        Objects.requireNonNull(dataSource,"dataSource");
        Objects.requireNonNull(tracer,"tracer");

        InvocationHandler handler=(proxy,method,args)->{
            Object result=invoke(dataSource,method,args);

            if(method.getName().equals("getConnection")&&result instanceof Connection){
                return wrapConnection((Connection)result,tracer);
            }

            return result;
        };

        return (DataSource)Proxy.newProxyInstance(
            DataSource.class.getClassLoader(),
            new Class<?>[]{DataSource.class},
            handler
        );
    }

    private static Connection wrapConnection(Connection connection,OpenTrace tracer){
        InvocationHandler handler=(proxy,method,args)->{
            Object result=invoke(connection,method,args);

            if(result instanceof CallableStatement){
                return wrapStatement((CallableStatement)result,tracer);
            }
            if(result instanceof PreparedStatement){
                return wrapStatement((PreparedStatement)result,tracer);
            }
            if(result instanceof Statement){
                return wrapStatement((Statement)result,tracer);
            }

            return result;
        };

        return (Connection)Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            handler
        );
    }

    private static <T extends Statement>T wrapStatement(T statement,OpenTrace tracer){
        Class<?> statementType;
        if(statement instanceof CallableStatement){
            statementType=CallableStatement.class;
        }else if(statement instanceof PreparedStatement){
            statementType=PreparedStatement.class;
        }else{
            statementType=Statement.class;
        }

        InvocationHandler handler=(proxy,method,args)->{
            if(!method.getName().startsWith("execute")){
                return invoke(statement,method,args);
            }

            try(SpanScope span=tracer.span("db.query")){
                try{
                    return invoke(statement,method,args);
                }catch(Throwable error){
                    span.error(error);
                    throw error;
                }
            }
        };

        Object proxy=Proxy.newProxyInstance(
            statementType.getClassLoader(),
            new Class<?>[]{statementType},
            handler
        );

        @SuppressWarnings("unchecked")
        T wrapped=(T)proxy;
        return wrapped;
    }

    private static Object invoke(Object target,Method method,Object[] args)throws Throwable{
        try{
            return method.invoke(target,args);
        }catch(InvocationTargetException e){
            throw e.getCause();
        }
    }
}

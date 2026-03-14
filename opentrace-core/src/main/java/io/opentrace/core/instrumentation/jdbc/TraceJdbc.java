package io.opentrace.core.instrumentation.jdbc;

import io.opentrace.core.OpenTrace;

import java.lang.reflect.*;
import java.sql.*;

public final class TraceJdbc{

    private TraceJdbc(){}

    public static Connection wrap(Connection connection,OpenTrace tracer){

        InvocationHandler handler=(proxy,method,args)->{

            String name=method.getName();

            if(name.equals("createStatement")){
                Statement stmt=(Statement)method.invoke(connection,args);
                return wrapStatement(stmt,tracer);
            }

            if(name.equals("prepareStatement")){
                PreparedStatement stmt=(PreparedStatement)method.invoke(connection,args);
                return wrapPreparedStatement(stmt,tracer);
            }

            return method.invoke(connection,args);
        };

        return (Connection)Proxy.newProxyInstance(
            connection.getClass().getClassLoader(),
            new Class[]{Connection.class},
            handler
        );
    }

    private static Statement wrapStatement(Statement stmt,OpenTrace tracer){

        InvocationHandler handler=(proxy,method,args)->{

            String name=method.getName();

            if(name.equals("execute")||
               name.equals("executeQuery")||
               name.equals("executeUpdate")){

                return tracer.trace("db.query",()->{
                    try{
                        return method.invoke(stmt,args);
                    }catch(Throwable t){
                        throw new RuntimeException(t);
                    }
                });
            }

            return method.invoke(stmt,args);
        };

        return (Statement)Proxy.newProxyInstance(
            stmt.getClass().getClassLoader(),
            new Class[]{Statement.class},
            handler
        );
    }

    private static PreparedStatement wrapPreparedStatement(
        PreparedStatement stmt,
        OpenTrace tracer
    ){

        InvocationHandler handler=(proxy,method,args)->{

            String name=method.getName();

            if(name.equals("execute")||
               name.equals("executeQuery")||
               name.equals("executeUpdate")){

                return tracer.trace("db.query",()->{
                    try{
                        return method.invoke(stmt,args);
                    }catch(Throwable t){
                        throw new RuntimeException(t);
                    }
                });
            }

            return method.invoke(stmt,args);
        };

        return (PreparedStatement)Proxy.newProxyInstance(
            stmt.getClass().getClassLoader(),
            new Class[]{PreparedStatement.class},
            handler
        );
    }
}
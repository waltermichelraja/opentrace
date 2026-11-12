
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;

public final class TraceJdbc{

    private TraceJdbc(){}

    public static DataSource wrap(DataSource dataSource,OpenTrace tracer){
        return wrap(dataSource,tracer,false);
    }

    public static DataSource wrap(DataSource dataSource,OpenTrace tracer,boolean captureSql){
        Objects.requireNonNull(dataSource,"dataSource");
        Objects.requireNonNull(tracer,"tracer");

        InvocationHandler handler=(proxy,method,args)->{
            Object result=invoke(dataSource,method,args);

            if(method.getName().equals("getConnection")&&result instanceof Connection){
                return wrapConnection((Connection)result,tracer,captureSql);
            }

            return result;
        };

        return (DataSource)Proxy.newProxyInstance(
            DataSource.class.getClassLoader(),
            new Class<?>[]{DataSource.class},
            handler
        );
    }

    private static Connection wrapConnection(Connection connection,OpenTrace tracer,boolean captureSql){
        InvocationHandler handler=(proxy,method,args)->{
            Object result=invoke(connection,method,args);

            String name=method.getName();
            String sql=null;

            if((name.equals("prepareStatement")||name.equals("prepareCall"))
                &&args!=null&&args.length>0&&args[0] instanceof String){
                sql=(String)args[0];
            }

            if(result instanceof CallableStatement){
                return wrapStatement((CallableStatement)result,tracer,captureSql,sql);
            }
            if(result instanceof PreparedStatement){
                return wrapStatement((PreparedStatement)result,tracer,captureSql,sql);
            }
            if(result instanceof Statement){
                return wrapStatement((Statement)result,tracer,captureSql,null);
            }

            return result;
        };

        return (Connection)Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            handler
        );
    }

    private static <T extends Statement>T wrapStatement(
        T statement,OpenTrace tracer,boolean captureSql,String preparedSql
    ){
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

            String sql=preparedSql;
            if(args!=null&&args.length>0&&args[0] instanceof String){
                sql=(String)args[0];
            }

            try(SpanScope span=tracer.span("db.query")){
                span.setAttribute("db.system","jdbc");
                span.setAttribute("db.operation.name",operation(sql,method.getName()));

                if(captureSql&&sql!=null){
                    span.setAttribute("db.statement",sql);
                }

                try{
                    return invoke(statement,method,args);
                }catch(Throwable error){
                    span.error(error);
                    span.setAttribute("error.type",error.getClass().getName());

                    if(error instanceof SQLException){
                        SQLException sqlError=(SQLException)error;

                        if(sqlError.getSQLState()!=null){
                            span.setAttribute("db.response.sqlstate",sqlError.getSQLState());
                        }

                        span.setAttribute("db.response.vendor_code",sqlError.getErrorCode());
                    }

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

    private static String operation(String sql,String methodName){
        if(sql==null||sql.isBlank()){
            if(methodName.equals("executeBatch")||methodName.equals("executeLargeBatch")){
                return "BATCH";
            }
            return "UNKNOWN";
        }

        String trimmed=sql.stripLeading();
        int end=0;

        while(end<trimmed.length()&&Character.isLetter(trimmed.charAt(end))){
            end++;
        }

        if(end==0){return "UNKNOWN";}

        String keyword=trimmed.substring(0,end).toUpperCase(Locale.ROOT);

        return switch(keyword){
            case "SELECT","INSERT","UPDATE","DELETE","MERGE",
                 "CREATE","ALTER","DROP","TRUNCATE","CALL"->keyword;
            default->"UNKNOWN";
        };
    }

    private static Object invoke(Object target,Method method,Object[] args)throws Throwable{
        try{
            return method.invoke(target,args);
        }catch(InvocationTargetException e){
            throw e.getCause();
        }
    }
}

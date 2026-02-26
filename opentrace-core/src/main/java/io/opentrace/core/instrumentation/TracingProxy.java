package io.opentrace.core.instrumentation;

import io.opentrace.core.OpenTrace;

import java.lang.reflect.*;

public final class TracingProxy{
    private TracingProxy(){}

    @SuppressWarnings("unchecked")
    public static <T>T create(T target, OpenTrace tracer, Class<T> interfaceType){
    InvocationHandler handler=(proxy, method, args)->{
        Trace traceAnnotation=method.getAnnotation(Trace.class);
        if(traceAnnotation==null){
            Method targetMethod=target.getClass().getMethod(method.getName(), method.getParameterTypes());
            traceAnnotation=targetMethod.getAnnotation(Trace.class);
        }
        
        if(traceAnnotation==null){return method.invoke(target,args);}
        String spanName=traceAnnotation.value().isEmpty()?method.getName():traceAnnotation.value();
        return tracer.trace(spanName, ()->{
            try{return method.invoke(target,args);}
            catch(InvocationTargetException e){
                Throwable cause=e.getTargetException();
                if(cause instanceof RuntimeException){throw(RuntimeException)cause;}
                if(cause instanceof Error){throw (Error)cause;}
                throw new RuntimeException(cause);
            }catch(IllegalAccessException e){throw new RuntimeException(e);}});
        };

        return (T)Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[]{interfaceType}, handler);
    }
}
package io.opentrace.core.instrumentation;

import io.opentrace.core.OpenTrace;

import java.lang.reflect.*;

public final class TracingProxy{
    private TracingProxy(){}

    @SuppressWarnings("unchecked")
    public static <T>T create(T target, OpenTrace tracer, Class<T> interfaceType){
        InvocationHandler handler=(proxy,method, args)->{
            if(method.getDeclaringClass()==Object.class){return method.invoke(target, args);}
            Trace methodTrace=method.getAnnotation(Trace.class);
            if(methodTrace==null){
                try{
                    Method targetMethod=target.getClass().getMethod(method.getName(), method.getParameterTypes());
                    methodTrace=targetMethod.getAnnotation(Trace.class);
                }catch(NoSuchMethodException ignored){}
            }
            Trace interfaceTrace=interfaceType.getAnnotation(Trace.class);
            Trace classTrace=target.getClass().getAnnotation(Trace.class);
            if(methodTrace==null && interfaceTrace==null && classTrace==null){return method.invoke(target, args);}
            String spanName;
            if(methodTrace!=null && !methodTrace.value().isEmpty()){spanName=methodTrace.value();}
            else{spanName=method.getName();}
            return tracer.trace(spanName,()->{
                try{return method.invoke(target, args);}
                catch(InvocationTargetException e){
                    Throwable cause=e.getTargetException();
                    if(cause instanceof RuntimeException){throw (RuntimeException)cause;}
                    if(cause instanceof Error){throw (Error)cause;}
                    throw new RuntimeException(cause);
                }catch(IllegalAccessException e){throw new RuntimeException(e);}
            });
        };
        T proxy=(T)Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[]{interfaceType},handler);
        if(target instanceof SelfAware){((SelfAware<T>)target).setSelf(proxy);}
        return proxy;
    }
}
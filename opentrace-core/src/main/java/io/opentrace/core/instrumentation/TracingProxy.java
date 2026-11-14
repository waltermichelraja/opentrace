package io.opentrace.core.instrumentation;

import io.opentrace.core.OpenTrace;
import io.opentrace.core.SpanScope;

import java.lang.reflect.*;

public final class TracingProxy{
    private TracingProxy(){}

    @SuppressWarnings("unchecked")
    public static <T>T create(T target,OpenTrace tracer,Class<T> interfaceType){
        InvocationHandler handler=(proxy,method,args)->{
            try{method.setAccessible(true);}catch(RuntimeException ignored){}
            if(method.getDeclaringClass()==Object.class){
                return invokeTarget(target,method,args);
            }
            Method targetMethod=null;
            try{
                targetMethod=target.getClass().getMethod(method.getName(),method.getParameterTypes());
            }catch(NoSuchMethodException ignored){}
            Trace interfaceMethodTrace=method.getAnnotation(Trace.class);
            Trace implementationMethodTrace=targetMethod==null
                ?null
                :targetMethod.getAnnotation(Trace.class);
            Trace selectedMethodTrace=implementationMethodTrace!=null
                ?implementationMethodTrace
                :interfaceMethodTrace;
            Trace interfaceTrace=interfaceType.getAnnotation(Trace.class);
            Trace classTrace=target.getClass().getAnnotation(Trace.class);
            if(selectedMethodTrace==null&&interfaceTrace==null&&classTrace==null){
                return invokeTarget(target,method,args);
            }
            String spanName=selectedMethodTrace!=null&&!selectedMethodTrace.value().isEmpty()
                ?selectedMethodTrace.value()
                :method.getName();
            try(SpanScope scope=tracer.span(spanName)){
                try{
                    return invokeTarget(target,method,args);
                }catch(Throwable t){
                    scope.error(t);
                    throw t;
                }
            }
        };
        T proxy=(T)Proxy.newProxyInstance(
            interfaceType.getClassLoader(),
            new Class<?>[]{interfaceType},
            handler
        );
        if(target instanceof SelfAware){
            ((SelfAware<T>)target).setSelf(proxy);
        }

        return proxy;
    }

    private static Object invokeTarget(Object target,Method method,Object[] args)throws Throwable{
        try{
            return method.invoke(target,args);
        }catch(InvocationTargetException e){
            throw e.getTargetException();
        }
    }
}
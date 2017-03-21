package io.anyway.hera.concurrent;

import io.anyway.hera.context.MetricTraceContext;
import io.anyway.hera.context.MetricTraceContextHolder;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by yangzz on 17/3/13.
 */
public class ThreadPoolWrapper extends ThreadPoolTaskExecutor{

    private final ThreadPoolTaskExecutor delegate;

    private final int queueCapacity;

    public ThreadPoolWrapper(ThreadPoolTaskExecutor delegate){
        this.delegate= delegate;
        Field field= ReflectionUtils.findField(ThreadPoolTaskExecutor.class,"queueCapacity");
        ReflectionUtils.makeAccessible(field);
        queueCapacity= (Integer) ReflectionUtils.getField(field,delegate);
    }

    @Override
    public int getCorePoolSize(){
        return delegate.getCorePoolSize();
    }

    @Override
    public int getActiveCount(){
        return delegate.getActiveCount();
    }

    @Override
    public int getKeepAliveSeconds(){
        return delegate.getKeepAliveSeconds();
    }

    @Override
    public int getMaxPoolSize(){
        return delegate.getMaxPoolSize();
    }

    @Override
    public ThreadGroup getThreadGroup(){
        return delegate.getThreadGroup();
    }

    @Override
    public String getThreadNamePrefix(){
        return delegate.getThreadNamePrefix();
    }

    @Override
    public int getThreadPriority(){
        return delegate.getThreadPriority();
    }

    @Override
    public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException{
        return delegate.getThreadPoolExecutor();
    }

    public int getQueueCapacity(){
        return queueCapacity;
    }

    @Override
    public void execute(final Runnable task){
        MetricTraceContext ctx= MetricTraceContextHolder.getMetricTraceContext();
        if(ctx!= null){
            final String traceId= ctx.getTraceId();
            final String parentId= ctx.getTraceStack().peek();
            final String remote= ctx.getRemote();
            delegate.execute(new Runnable() {
                @Override
                public void run() {
                    try{
                        deliverMetricsContext(traceId,parentId,remote);
                        task.run();
                    }finally {
                        clearMetricsContext();
                    }
                }
            });
        }
        else{
            delegate.execute(task);
        }
    }

    @Override
    public Future<?> submit(final Runnable task){
        MetricTraceContext ctx= MetricTraceContextHolder.getMetricTraceContext();
        if(ctx!= null){
            final String traceId= ctx.getTraceId();
            final String parentId= ctx.getTraceStack().peek();
            final String remote= ctx.getRemote();
            return delegate.submit(new Runnable() {
                @Override
                public void run() {
                    try{
                        deliverMetricsContext(traceId,parentId,remote);
                        task.run();
                    }finally {
                        clearMetricsContext();
                    }
                }
            });
        }
        else{
            return delegate.submit(task);
        }
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task){
        MetricTraceContext ctx= MetricTraceContextHolder.getMetricTraceContext();
        if(ctx!= null){
            final String traceId= ctx.getTraceId();
            final String parentId= ctx.getTraceStack().peek();
            final String remote= ctx.getRemote();
            return delegate.submit(new Callable() {
                @Override
                public T call()throws Exception {
                    try{
                        deliverMetricsContext(traceId,parentId,remote);
                        return task.call();
                    }finally {
                        clearMetricsContext();
                    }
                }
            });
        }
        else{
            return delegate.submit(task);
        }
    }

    private void deliverMetricsContext(String traceId,String parentId,String remote){
        MetricTraceContext ctx= new MetricTraceContext();
        ctx.setTraceId(traceId);
        Stack<String> stack= new Stack<String>();
        stack.push(parentId);
        ctx.setTraceStack(stack);
        ctx.setRemote(remote);
        MetricTraceContextHolder.setMetricTraceContext(ctx);
        MDC.put("traceId",traceId);
    }

    private void clearMetricsContext(){
        MetricTraceContextHolder.clear();
        MDC.remove("traceId");
    }

}

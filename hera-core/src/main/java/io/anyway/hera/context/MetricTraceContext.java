package io.anyway.hera.context;

import org.slf4j.MDC;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Created by yangzz on 16/8/16.
 */
public class MetricTraceContext {

    /**
     * 调用链标识
     */
    private String traceId;

    /**
     * 调用链栈
     */
    private Stack<String> traceStack;

    /**
     * 调用者信息
     */
    private String remote;

    private Set<Throwable> exceptions= null;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
        MDC.put("traceId",traceId);
    }

    public Stack<String> getTraceStack() {
        return traceStack;
    }

    public void setTraceStack(Stack<String> traceStack) {
        this.traceStack = traceStack;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public synchronized boolean containException(Throwable e){
        if(exceptions== null){
            return false;
        }
        return exceptions.contains(e);
    }

    public synchronized void addException(Throwable e){
        if(exceptions== null){
            exceptions= new LinkedHashSet<Throwable>();
        }
        exceptions.add(e);
    }

}

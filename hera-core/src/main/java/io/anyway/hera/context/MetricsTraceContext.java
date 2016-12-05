package io.anyway.hera.context;

import java.util.Stack;

/**
 * Created by yangzz on 16/8/16.
 */
public class MetricsTraceContext {

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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
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

}

package io.anyway.hera.context;

import java.util.Stack;

/**
 * Created by yangzz on 16/8/16.
 */
public class MetricsContext {

    /**
     * 调用链标识
     */
    private String transactionId;

    /**
     * 调用链栈
     */
    private Stack<String> transactionTrace;

    /**
     * 调用者信息
     */
    private String remote;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Stack<String> getTransactionTrace() {
        return transactionTrace;
    }

    public void setTransactionTrace(Stack<String> transactionTrace) {
        this.transactionTrace = transactionTrace;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }
}

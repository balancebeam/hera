package io.anyway.hera.context;

/**
 * Created by yangzz on 16/8/16.
 */
final public class MetricsTraceContextHolder {

    private MetricsTraceContextHolder(){}

    private static ThreadLocal<MetricsTraceContext> holder= new ThreadLocal<MetricsTraceContext>();

    /**
     * 获取调用链上下文
     * @return
     */
    public static MetricsTraceContext getMetricsTraceContext(){
        return holder.get();
    }

    public static void setMetricsTraceContext(MetricsTraceContext ctx){
        holder.set(ctx);
    }

    /**
     * 清空调用链上下文
     */
    public static void clear(){
        holder.set(null);
    }
}

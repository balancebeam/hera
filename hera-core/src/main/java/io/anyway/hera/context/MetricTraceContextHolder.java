package io.anyway.hera.context;

/**
 * Created by yangzz on 16/8/16.
 */
final public class MetricTraceContextHolder {

    private MetricTraceContextHolder(){}

    private static ThreadLocal<MetricTraceContext> holder= new ThreadLocal<MetricTraceContext>();

    /**
     * 获取调用链上下文
     * @return
     */
    public static MetricTraceContext getMetricTraceContext(){
        return holder.get();
    }

    public static void setMetricTraceContext(MetricTraceContext ctx){
        holder.set(ctx);
    }

    /**
     * 清空调用链上下文
     */
    public static void clear(){
        holder.remove();
    }
}

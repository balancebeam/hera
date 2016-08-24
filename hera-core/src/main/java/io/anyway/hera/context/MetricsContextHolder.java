package io.anyway.hera.context;

/**
 * Created by yangzz on 16/8/16.
 */
final public class MetricsContextHolder {

    private MetricsContextHolder(){}

    private static ThreadLocal<MetricsContext> holder= new ThreadLocal<MetricsContext>();

    /**
     * 获取调用链上下文
     * @return
     */
    public static MetricsContext getMetricsContext(){
        return holder.get();
    }

    public static void setMetricsContext(MetricsContext ctx){
        holder.set(ctx);
    }

    /**
     * 清空调用链上下文
     */
    public static void clear(){
        holder.set(null);
    }
}

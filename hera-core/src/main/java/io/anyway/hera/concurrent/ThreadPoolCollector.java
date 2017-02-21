package io.anyway.hera.concurrent;

import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.BlockingStackTraceCollector;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.collector.MetricsCollector;
import io.anyway.hera.spring.BeanPostProcessorWrapper;
import io.anyway.hera.spring.BeanPreProcessorWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.pool.ConnPoolControl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by yangzz on 16/8/19.
 */
public class ThreadPoolCollector implements BeanPostProcessorWrapper,BeanPreProcessorWrapper,MetricsCollector {

    private MetricsHandler handler;

    private Log logger= LogFactory.getLog(ThreadPoolCollector.class);

    private Map<String,ThreadPoolTaskExecutor> threadPools= new LinkedHashMap<String,ThreadPoolTaskExecutor>();

    private Set<String> excludedThreadPools= Collections.emptySet();

    private BlockingStackTraceCollector blockingStackTraceCollector;

    public void setBlockingStackTraceCollector(BlockingStackTraceCollector blockingStackTraceCollector) {
        this.blockingStackTraceCollector = blockingStackTraceCollector;
    }

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    @Override
    public boolean interest(Object bean) {
        return bean instanceof ThreadPoolTaskExecutor;
    }

    @Override
    public Object preWrapBean(Object bean, String ctxId, String beanName) {
        ((ThreadPoolTaskExecutor)bean).setThreadGroupName(beanName+"-Group");
        return bean;
    }

    @Override
    public synchronized Object wrapBean(Object bean,String appId, String beanName) {
        if(!excludedThreadPools.contains(beanName)){
            threadPools.put((StringUtils.isEmpty(appId)?"":appId+":")+beanName,(ThreadPoolTaskExecutor)bean);

            logger.info("Monitor thread pool: "+beanName);
        }
        return bean;
    }

    @Override
    public synchronized void destroyWrapper(String appId, String beanName) {
        threadPools.remove((StringUtils.isEmpty(appId)?"":appId+":")+beanName);
    }

    public void setExcludedThreadPools(Set<String> excludedThreadPools) {
        this.excludedThreadPools = excludedThreadPools;
        logger.info("ExcludedThreadPools: "+excludedThreadPools);
    }

    @Override
    public void doCollect() {
        //工作线程池收集
        for (Map.Entry<String,ThreadPoolTaskExecutor> each: threadPools.entrySet()){
            ThreadPoolTaskExecutor executor= each.getValue();
            Map<String,String> tags= new LinkedHashMap<String,String>();
            Map<String,Object> props= new LinkedHashMap<String, Object>();
            //工作线程池的名称
            tags.put("threadPoolName",each.getKey());
            //最大线程池数
            props.put("maxPoolSize",executor.getMaxPoolSize());
            //活跃的线程数
            props.put("activeCount",executor.getActiveCount());
            //核心的线程数
            props.put("corePoolSize",executor.getPoolSize());
            //超时时间
            props.put("keepAliveSeconds",executor.getKeepAliveSeconds());
            Field f= ReflectionUtils.findField(ThreadPoolTaskExecutor.class,"queueCapacity");
            ReflectionUtils.makeAccessible(f);
            int queueCapacity= (Integer) ReflectionUtils.getField(f,executor);
            //最大队列数
            props.put("queueCapacity",queueCapacity);
            //任务数包括队列和正在执行的任务
            props.put("taskCount",executor.getThreadPoolExecutor().getTaskCount()-executor.getThreadPoolExecutor().getCompletedTaskCount());
            //发送监控数据
            handler.handle(MetricsQuota.THREAD,tags,props);

            //当线程池被占满需要打印堆栈
            if(executor.getMaxPoolSize()== executor.getActiveCount()){
                ThreadGroup threadGroup= executor.getThreadGroup();
                Thread[] threads= new Thread[threadGroup.activeCount()];
                threadGroup.enumerate(threads);

                List<StackTraceElement[]> stackTraces= new LinkedList<StackTraceElement[]>();
                for(Thread thread: threads){
                    if(thread!= null){
                        stackTraces.add(thread.getStackTrace());
                    }
                }
                blockingStackTraceCollector.collect(MetricsQuota.THREAD,each.getKey(),stackTraces);
            }
        }

    }
}

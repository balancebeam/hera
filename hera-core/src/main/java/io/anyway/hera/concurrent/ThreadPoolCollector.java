package io.anyway.hera.concurrent;

import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.collector.MetricsCollector;
import io.anyway.hera.spring.BeanPostProcessorWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by yangzz on 16/8/19.
 */
public class ThreadPoolCollector implements BeanPostProcessorWrapper,MetricsCollector {

    private MetricsHandler handler;

    private Log logger= LogFactory.getLog(ThreadPoolCollector.class);

    private Map<String,ThreadPoolTaskExecutor> threadPools= new LinkedHashMap<String,ThreadPoolTaskExecutor>();

    private Set<String> excludedThreadPools= Collections.emptySet();

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    @Override
    public boolean interest(Object bean) {
        return bean instanceof ThreadPoolTaskExecutor;
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
            props.put("timestamp",System.currentTimeMillis());
            //发送监控数据
            handler.handle(MetricsQuota.WORKTHREAD,tags,props);
        }
        //系统线程池收集
        ThreadMXBean instance = ManagementFactory.getThreadMXBean();
        Map<String,Object> props= new LinkedHashMap<String, Object>();
        //线程总数
        props.put("threadCount",instance.getThreadCount());
        //峰值活动线程数
        props.put("peakThreadCount",instance.getPeakThreadCount());
        //守护线程总数
        props.put("daemonThreadCount",instance.getDaemonThreadCount());
        //当前线程cpu执行时间
        props.put("currentThreadCpuTime",instance.getCurrentThreadCpuTime());
        //当前线程用户模式中的cpu执行时间
        props.put("currentThreadUserTime",instance.getCurrentThreadUserTime());
        props.put("timestamp",System.currentTimeMillis());
        //发送监控数据
        handler.handle(MetricsQuota.SYSTHREAD,null,props);
    }
}

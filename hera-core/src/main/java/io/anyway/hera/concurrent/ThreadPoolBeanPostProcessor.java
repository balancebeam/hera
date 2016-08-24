package io.anyway.hera.concurrent;

import io.anyway.hera.common.MetricsCollector;
import io.anyway.hera.scheduler.MetricsProcessor;
import io.anyway.hera.spring.BeanPostProcessorWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by yangzz on 16/8/19.
 */
public class ThreadPoolBeanPostProcessor implements BeanPostProcessorWrapper,MetricsProcessor {

    private Log logger= LogFactory.getLog(ThreadPoolBeanPostProcessor.class);

    private final static Map<String,ThreadPoolTaskExecutor> threadPools= new LinkedHashMap<String,ThreadPoolTaskExecutor>();

    private Set<String> excludedThreadPools= Collections.emptySet();

    @Override
    public boolean interest(Object bean) {
        return bean instanceof ThreadPoolTaskExecutor;
    }

    @Override
    public Object wrapBean(Object bean, String beanName) {
        if(!excludedThreadPools.contains(beanName)){
            threadPools.put(beanName,(ThreadPoolTaskExecutor)bean);
            logger.info("Monitor thread pool: "+beanName);
        }
        return bean;
    }

    public void setExcludedThreadPools(Set<String> excludedThreadPools) {
        this.excludedThreadPools = excludedThreadPools;
        logger.info("ExcludedThreadPools: "+excludedThreadPools);
    }

    public static Map<String,ThreadPoolTaskExecutor> getThreadPools(){
        return threadPools;
    }

    @Override
    public void doMonitor() {
        //工作线程池收集
        for (Map.Entry<String,ThreadPoolTaskExecutor> each: threadPools.entrySet()){
            ThreadPoolTaskExecutor executor= each.getValue();
            Map<String,Object> payload= new LinkedHashMap<String, Object>();
            //设置工作线程标识
            payload.put("category","workThread");
            //工作线程池的名称
            payload.put("name",each.getKey());
            //最大线程池数
            payload.put("maxPoolSize",executor.getMaxPoolSize());
            //活跃的线程数
            payload.put("activeCount",executor.getActiveCount());
            //核心的线程数
            payload.put("poolSize",executor.getPoolSize());
            //超时时间
            payload.put("keepAliveSeconds",executor.getKeepAliveSeconds());
            Field f= ReflectionUtils.findField(ThreadPoolTaskExecutor.class,"queueCapacity");
            ReflectionUtils.makeAccessible(f);
            int queueCapacity= (Integer) ReflectionUtils.getField(f,executor);
            //最大队列数
            payload.put("queueCapacity",queueCapacity);
            //任务数包括队列和正在执行的任务
            payload.put("taskCount",executor.getThreadPoolExecutor().getTaskCount());
            //采集的当前时间
            payload.put("timestamp",System.currentTimeMillis());
            //发送监控数据
            MetricsCollector.collect(payload);
        }
        //系统线程池收集
        ThreadMXBean instance = ManagementFactory.getThreadMXBean();
        Map<String,Object> payload= new LinkedHashMap<String, Object>();
        payload.put("category","sysThread");
        //线程总数
        payload.put("threadCount",instance.getThreadCount());
        //峰值活动线程数
        payload.put("peakThreadCount",instance.getPeakThreadCount());
        //守护线程总数
        payload.put("daemonThreadCount",instance.getDaemonThreadCount());
        //当前线程cpu执行时间
        payload.put("currentThreadCpuTime",instance.getCurrentThreadCpuTime());
        //当前线程用户模式中的cpu执行时间
        payload.put("currentThreadUserTime",instance.getCurrentThreadUserTime());
        //采集的当前时间
        payload.put("timestamp",System.currentTimeMillis());
        //发送监控数据
        MetricsCollector.collect(payload);
    }
}

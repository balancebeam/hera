package io.anyway.hera.jvm;

import io.anyway.hera.common.MetricsCollector;
import io.anyway.hera.scheduler.MetricsProcessor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public class MemoryMetricsProcessor implements MetricsProcessor {

    @Override
    public void doMonitor() {

        Map<String,Object> payload= new LinkedHashMap<String,Object>();
        //设置内存类别
        payload.put("category","memory");
        //虚拟机最大内存
        payload.put("maxMemory",Runtime.getRuntime().maxMemory());
        //已经使用的虚机内存
        payload.put("usedMemory",Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        //获取永久带内存
        MemoryPoolMXBean permGenMemoryPool= getPermGenMemoryPool();
        if (permGenMemoryPool != null) {
            MemoryUsage usage = permGenMemoryPool.getUsage();
            //最大永久带内存
            payload.put("maxPermGen",usage.getMax());
            //已使用的久带内存
            payload.put("usedPerGen",usage.getUsed());
        }
        else{
            payload.put("maxPermGen",-1);
            payload.put("usedPerGen",-1);
        }
        //加载的类数量
        payload.put("loadedClassesCount",ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
        //得到非堆内存对象
        MemoryUsage memoryUsage= ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        //最大非对内存
        payload.put("maxNonHeapMemory",memoryUsage.getMax());
        //使用的非对内存
        payload.put("usedNonHeapMemory",memoryUsage.getUsed());
        //获取堆内存对象
        memoryUsage= ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        //最大堆内存
        payload.put("maxHeapMemory",memoryUsage.getMax());
        //已使用的久带内存
        payload.put("usedHeapMemory",memoryUsage.getUsed());
        //获取操作系统对象
        OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        if (isSunOsMBean(operatingSystem)) {
            com.sun.management.OperatingSystemMXBean sunOperatingSystem= (com.sun.management.OperatingSystemMXBean)operatingSystem;
            //最大物理内存
            payload.put("totalPhysicalMemory",sunOperatingSystem.getTotalPhysicalMemorySize());
            //空闲物理内存
            payload.put("freePhysicalMemory",sunOperatingSystem.getFreePhysicalMemorySize());
            //最大交换空间
            payload.put("totalSwapSpace",sunOperatingSystem.getTotalSwapSpaceSize());
            //空闲交换空间
            payload.put("freeSwapSpace",sunOperatingSystem.getFreeSwapSpaceSize());
        } else {
            payload.put("totalPhysicalMemory",-1);
            payload.put("freePhysicalMemory",-1);
            payload.put("totalSwapSpace",-1);
            payload.put("freeSwapSpace",-1);
        }
        //采集时间
        payload.put("timestamp",System.currentTimeMillis());
        //发送采集信息
        MetricsCollector.collect(payload);
    }

    private static MemoryPoolMXBean getPermGenMemoryPool() {
        for (final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool.getName().endsWith("Perm Gen")) {
                return memoryPool;
            }
        }
        return null;
    }

    private boolean isSunOsMBean(OperatingSystemMXBean operatingSystem) {
        String className = operatingSystem.getClass().getName();
        return "com.sun.management.OperatingSystem".equals(className)
                || "com.sun.management.UnixOperatingSystem".equals(className)
                || "sun.management.OperatingSystemImpl".equals(className);
    }
}

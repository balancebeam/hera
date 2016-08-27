package io.anyway.hera.jvm;

import io.anyway.hera.common.MetricsType;
import io.anyway.hera.common.MetricsManager;
import io.anyway.hera.common.MetricsCollector;

import java.lang.management.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public class MemoryCollector implements MetricsCollector {

    @Override
    public void doCollect() {

        Map<String,Object> payload= new LinkedHashMap<String,Object>();
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
        payload.put("timestamp",MetricsManager.toLocalDate(System.currentTimeMillis()));
        //发送采集信息
        MetricsManager.collect(MetricsType.MEMORY,payload);

        //采集内存垃圾回收信息
        for(GarbageCollectorMXBean each: ManagementFactory.getGarbageCollectorMXBeans()){
            payload= new LinkedHashMap<String,Object>();
            //内存区名称
            payload.put("name",each.getName());
            //垃圾回收的次数
            payload.put("collectionCount",each.getCollectionCount());
            //垃圾回收持续的时间
            payload.put("collectionTime",each.getCollectionTime());
            //内存池名称
            payload.put("MemoryPoolNames", Arrays.asList(each.getMemoryPoolNames()).toString());
            //采集时间
            payload.put("timestamp",MetricsManager.toLocalDate(System.currentTimeMillis()));
            //发送采集信息
            MetricsManager.collect(MetricsType.MEMORYGARBAGE,payload);
        }
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
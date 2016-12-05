package io.anyway.hera.jvm;

import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.collector.MetricsCollector;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.management.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public class MemoryCollector implements MetricsCollector {

    private long m_unit= 1024*1024;

    private MetricsHandler handler;

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    @Override
    public void doCollect() {

        Map<String,Object> props= new LinkedHashMap<String,Object>();
        //虚拟机最大内存
        props.put("maxMemory",b2m(Runtime.getRuntime().maxMemory()));
        //已经使用的虚机内存
        props.put("usedMemory",b2m(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        //获取永久带内存
        MemoryPoolMXBean permGenMemoryPool= getPermGenMemoryPool();
        if (permGenMemoryPool != null) {
            MemoryUsage usage = permGenMemoryPool.getUsage();
            //最大永久带内存
            props.put("maxPermGen",b2m(usage.getMax()));
            //已使用的久带内存
            props.put("usedPerGen",b2m(usage.getUsed()));
        }
        else{
            props.put("maxPermGen",-1);
            props.put("usedPerGen",-1);
        }
        //加载的类数量
        props.put("loadedClassesCount",ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
        //得到非堆内存对象
        MemoryUsage memoryUsage= ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        //最大非对内存
        props.put("maxNonHeapMemory",b2m(memoryUsage.getMax()));
        //使用的非对内存
        props.put("usedNonHeapMemory",b2m(memoryUsage.getUsed()));
        //获取堆内存对象
        memoryUsage= ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        //最大堆内存
        props.put("maxHeapMemory",b2m(memoryUsage.getMax()));
        //已使用的久带内存
        props.put("usedHeapMemory",b2m(memoryUsage.getUsed()));
        //获取操作系统对象
        OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        String[] sysoprops= {
            "totalPhysicalMemorySize", //最大物理内存
            "freePhysicalMemorySize", //空闲物理内存
            "totalSwapSpaceSize", //最大交换空间
            "freeSwapSpaceSize" //空闲交换空间
        };
        //如果是Sun的JDK
        if (isSunOsMBean(operatingSystem)) {
            for(String each: sysoprops){
                try{
                    props.put(each,b2m(Long.parseLong(BeanUtils.getProperty(operatingSystem,each))));
                }catch (Exception e){
                    props.put(each, -1);
                }
            }
        } else {
           for(String each: sysoprops){
               props.put(each, -1);
           }
        }
        //发送采集信息
        props.put("timestamp",System.currentTimeMillis());
        handler.handle(MetricsQuota.MEMORY,null,props);

        //采集内存垃圾回收信息
        for(GarbageCollectorMXBean each: ManagementFactory.getGarbageCollectorMXBeans()){
            props= new LinkedHashMap<String,Object>();
            Map<String,String> tags= new LinkedHashMap<String, String>();
            //内存区名称
            tags.put("name",each.getName());
            //垃圾回收的次数
            props.put("collectionCount",each.getCollectionCount());
            //垃圾回收持续的时间
            props.put("collectionTime",each.getCollectionTime());
            //内存池名称
            props.put("MemoryPoolNames", Arrays.asList(each.getMemoryPoolNames()).toString());
            //发送采集信息
            props.put("timestamp",System.currentTimeMillis());
            handler.handle(MetricsQuota.GC,tags,props);
        }
    }

    private MemoryPoolMXBean getPermGenMemoryPool() {
        for (final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
            //java 8 use "Metaspace" instead of "Perm Gen"
            if (memoryPool.getName().matches(".*Perm\\sGen|Metaspace")) {
                return memoryPool;
            }
        }
        return null;
    }

    private double b2m(long val){
        if(val<0){
            return val;
        }
        BigDecimal decimal= new BigDecimal(val/m_unit);
        return decimal.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private boolean isSunOsMBean(OperatingSystemMXBean operatingSystem) {
        String className = operatingSystem.getClass().getName();
        return "com.sun.management.OperatingSystem".equals(className)
                || "com.sun.management.UnixOperatingSystem".equals(className)
                || "sun.management.OperatingSystemImpl".equals(className);
    }
}

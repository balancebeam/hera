package io.anyway.hera.jvm;

import io.anyway.hera.collector.MetricsHandler;
import io.anyway.hera.common.MetricsQuota;
import io.anyway.hera.collector.MetricsCollector;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yangzz on 16/8/24.
 */
public class CpuCollector implements MetricsCollector {

    private MetricsHandler handler;

    public void setHandler(MetricsHandler handler){
        this.handler= handler;
    }

    @Override
    public void doCollect() {
        Map<String,Object> props= new LinkedHashMap<String,Object>();
        //获取操作系统
        OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        //获取处理器核数
        props.put("availableProcessors",operatingSystem.getAvailableProcessors());
        //操作系统名称
        props.put("name",operatingSystem.getName());
        //操作系统版本号
        props.put("version",operatingSystem.getVersion());
        //cpu负载值
        double loadedAverage= operatingSystem.getSystemLoadAverage();
        props.put("systemLoadAverage",loadedAverage);
        //windows获取的cpu负载值为-1
        if(loadedAverage== -1.0 && isSunOsMBean(operatingSystem)){
            try {
                loadedAverage= Double.parseDouble(BeanUtils.getProperty(operatingSystem,"systemCpuLoad"));
                props.put("systemLoadAverage", loadedAverage);
            } catch (Exception e) {}
        }
        props.put("timestamp",System.currentTimeMillis());
        //发送采集信息
        handler.handle(MetricsQuota.CPU,null,props);
    }

    private boolean isSunOsMBean(OperatingSystemMXBean operatingSystem) {
        String className = operatingSystem.getClass().getName();
        return "com.sun.management.OperatingSystem".equals(className)
                || "com.sun.management.UnixOperatingSystem".equals(className)
                || "sun.management.OperatingSystemImpl".equals(className);
    }
}
